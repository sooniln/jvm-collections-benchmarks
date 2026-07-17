#!/usr/bin/env python3
"""
Re-run benchmarks listed in a candidates CSV and update libraries.csv.

Usage:
    python rerun.py [candidates.csv] [output.csv]

Defaults to rerun_candidates.csv and libraries.csv.  Also accepts rerun_missing.csv (which has no
cov_pct column): missing rows are always inserted; existing rows are only replaced
when the new CoV is lower than the original.

Groups candidates by (gradle_task, library, order) to minimise JMH invocations.
Writes libraries.csv after each group so partial progress is preserved.
"""

import csv
import json
import os
import subprocess
import statistics
import sys
from collections import defaultdict
from pathlib import Path

RESULTS_DIR = Path(__file__).parent.parent / "benchmark-results"
CANDIDATES_CSV = RESULTS_DIR / "rerun_candidates.csv"
LIBRARIES_CSV  = RESULTS_DIR / "libraries.csv"
JMH_JSON       = Path(__file__).parent.parent / "build" / "results" / "jmh" / "results.json"
GRADLEW        = Path(__file__).parent.parent / "gradlew.bat"


# ---------------------------------------------------------------------------
# Helpers shared with parse_results.py
# ---------------------------------------------------------------------------

def stats_of_raw_data(raw_data: list[list[float]]) -> tuple[float, float, float]:
    flat = [v for fork in raw_data for v in fork]
    if not flat:
        raise ValueError("rawData is empty")
    return (
        statistics.median(flat),
        statistics.mean(flat),
        statistics.stdev(flat) if len(flat) > 1 else 0.0,
    )


def cov(mean: float, stdev: float) -> float:
    return (stdev / mean * 100) if mean else float("inf")


# ---------------------------------------------------------------------------
# Phase 1: load and group candidates
# ---------------------------------------------------------------------------

def load_candidates(path: Path) -> dict[tuple, float | None]:
    """Return {(benchmark, library, order, size) → orig_cov_pct}.

    cov_pct is None when the column is absent (e.g. rerun_missing.csv).
    """
    result = {}
    with path.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            key = (row["benchmark"], row["library"], row.get("order", ""), int(row["size"]))
            cov_str = row.get("cov_pct", "")
            result[key] = float(cov_str) if cov_str else None
    return result


def group_candidates(orig_cov: dict[tuple, float]) -> list[dict]:
    """Group by (benchmark_prefix, library, order), collecting methods and sizes.

    Always uses the base 'jmh' Gradle task rather than named tasks (jmhIntMap
    etc.) because named tasks override includes with a hardcoded class pattern,
    making jmhIncludes ineffective.  The base 'jmh' task honours jmhIncludes,
    so only the candidate methods are benchmarked.
    """
    groups: dict[tuple, dict] = defaultdict(lambda: {"methods": set(), "sizes": set()})
    for (benchmark, library, order, size), _ in orig_cov.items():
        prefix = benchmark.split(".")[0]   # e.g. "IntMap"
        method = benchmark.split(".")[1]   # e.g. "getHit"
        key    = (prefix, library, order)
        groups[key]["methods"].add(method)
        groups[key]["sizes"].add(size)

    # For each prefix, collect the non-empty orders that appear in candidates.
    # Used to restrict empty-order groups so FullState benchmarks (which carry
    # an order @Param) don't run "even"/"partition" that are absent from the
    # original libraries.csv.
    prefix_valid_orders: dict[str, set[str]] = defaultdict(set)
    for (prefix, library, order) in groups:
        if order:
            prefix_valid_orders[prefix].add(order)

    result = []
    for (prefix, library, order), data in sorted(groups.items()):
        methods   = sorted(data["methods"])
        sizes     = sorted(data["sizes"])
        method_re = f"{prefix}Benchmark\\.({'|'.join(methods)})"
        restrict_order = (
            ",".join(sorted(prefix_valid_orders[prefix]))
            if (not order and prefix_valid_orders[prefix])
            else ""
        )
        result.append({
            "task":           "jmh",
            "prefix":         prefix,
            "library":        library,
            "order":          order,
            "methods":        methods,
            "sizes":          sizes,
            "method_re":      method_re,
            "restrict_order": restrict_order,
        })
    return result


# ---------------------------------------------------------------------------
# Phase 2: run a group and parse results
# ---------------------------------------------------------------------------

def run_group(group: dict) -> list[dict] | None:
    """Invoke gradle and return parsed rows, or None on failure."""
    sizes_arg = ",".join(str(s) for s in group["sizes"])

    # Pass Gradle project properties via environment variables (ORG_GRADLE_PROJECT_*)
    # rather than -P flags so that special regex characters (|, (), \) are never
    # interpreted by cmd.exe's shell parser.
    env = os.environ.copy()
    env["ORG_GRADLE_PROJECT_jmhType"]     = group["library"]
    env["ORG_GRADLE_PROJECT_jmhSize"]     = sizes_arg
    env["ORG_GRADLE_PROJECT_jmhIncludes"] = group["method_re"]
    if group["order"]:
        env["ORG_GRADLE_PROJECT_jmhOrder"] = group["order"]
    elif group["restrict_order"]:
        env["ORG_GRADLE_PROJECT_jmhOrder"] = group["restrict_order"]

    cmd = [str(GRADLEW), group["task"]]

    print(f"\n{'='*70}")
    print(f"Task    : {group['task']}  (prefix: {group['prefix']})")
    print(f"Library : {group['library']}")
    print(f"Order   : {group['order'] or '(none)'}")
    print(f"Methods : {', '.join(group['methods'])}")
    print(f"Sizes   : {len(group['sizes'])} sizes")
    print(f"Command : {' '.join(cmd)}  [+env vars for jmhType/Size/Includes/Order]")

    proc = subprocess.run(cmd, env=env, cwd=GRADLEW.parent)
    if proc.returncode != 0:
        print(f"[WARN] gradle exited {proc.returncode} — skipping group, originals kept")
        return None

    if not JMH_JSON.exists():
        print(f"[WARN] {JMH_JSON} not found after run — skipping group")
        return None

    return parse_json(JMH_JSON)


def parse_json(json_path: Path) -> list[dict]:
    with json_path.open(encoding="utf-8") as f:
        data = json.load(f)

    rows = []
    for entry in data:
        full_name: str = entry["benchmark"]
        class_name, method = full_name.rsplit(".", 1)
        prefix = class_name.rsplit(".", 1)[-1].removesuffix("Benchmark")  # e.g. "IntMap"

        params  = entry.get("params", {})
        median, mean, stdev = stats_of_raw_data(entry["primaryMetric"]["rawData"])
        rows.append({
            "benchmark": f"{prefix}.{method}",
            "library":   params["type"],
            "order":     params.get("order", ""),
            "size":      int(params["size"]),
            "median":    median,
            "mean":      mean,
            "stdev":     stdev,
            "unit":      entry["primaryMetric"]["scoreUnit"],
            "cov_pct":   cov(mean, stdev),
        })
    return rows


# ---------------------------------------------------------------------------
# Phase 3: compare and update libraries.csv
# ---------------------------------------------------------------------------

def load_libraries(path: Path) -> tuple[list[str], list[dict]]:
    """Return (fieldnames, rows) from the libraries CSV."""
    with path.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)
    return fieldnames, rows


def write_libraries(path: Path, fieldnames: list[str], rows: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def row_key(row: dict) -> tuple:
    return (row["benchmark"], row["library"], row.get("order", ""), int(row["size"]))


def apply_improvements(
    fieldnames: list[str],
    lib_rows: list[dict],
    new_results: list[dict],
    orig_cov: dict[tuple, float | None],
) -> tuple[list[dict], int]:
    """Replace or append rows from new_results into lib_rows.

    - orig_cov[key] is None  → row was missing; always insert.
    - orig_cov[key] is float → row exists; only replace if new CoV is lower.
    Returns (updated_rows, count).
    """
    accepted: dict[tuple, dict] = {}
    for result in new_results:
        key = row_key(result)
        if key not in orig_cov:
            continue  # wasn't a candidate — shouldn't happen, but skip
        orig = orig_cov[key]
        if orig is None or result["cov_pct"] < orig:
            accepted[key] = result

    if not accepted:
        return lib_rows, 0

    def as_lib_row(r: dict) -> dict:
        return {
            "benchmark": r["benchmark"],
            "library":   r["library"],
            "order":     r["order"],
            "size":      r["size"],
            "median":    r["median"],
            "mean":      r["mean"],
            "stdev":     r["stdev"],
            "unit":      r["unit"],
        }

    existing_keys = {row_key(r) for r in lib_rows}

    updated = []
    for row in lib_rows:
        key = row_key(row)
        updated.append(as_lib_row(accepted[key]) if key in accepted else row)

    for key, r in accepted.items():
        if key not in existing_keys:
            updated.append(as_lib_row(r))

    return updated, len(accepted)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    candidates_path = Path(sys.argv[1]) if len(sys.argv) > 1 else CANDIDATES_CSV
    output_path     = Path(sys.argv[2]) if len(sys.argv) > 2 else LIBRARIES_CSV
    if not candidates_path.exists():
        sys.exit(f"Not found: {candidates_path}")
    if not output_path.exists():
        sys.exit(f"Not found: {output_path}")

    orig_cov = load_candidates(candidates_path)
    groups   = group_candidates(orig_cov)
    print(f"Candidates : {len(orig_cov)} rows")
    print(f"Groups     : {len(groups)} JMH invocations")

    fieldnames, lib_rows = load_libraries(output_path)

    total_improved = 0
    for i, group in enumerate(groups, 1):
        print(f"\n[{i}/{len(groups)}]", end="")
        new_results = run_group(group)
        if not new_results:
            continue

        lib_rows, n = apply_improvements(fieldnames, lib_rows, new_results, orig_cov)
        total_improved += n
        write_libraries(output_path, fieldnames, lib_rows)
        print(f"[{i}/{len(groups)}] Improved {n} row(s) this group  (total so far: {total_improved})")

    print(f"\nDone. {total_improved} row(s) replaced in {output_path.name}.")


if __name__ == "__main__":
    main()
