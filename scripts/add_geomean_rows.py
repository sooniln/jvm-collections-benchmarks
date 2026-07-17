#!/usr/bin/env python3
"""
Add per-library geometric-mean summary rows to benchmark-results/libraries.csv.

For each (library, order, size) group, combining IntMap and LongMap together,
adds:
  - Map.readGM    : geomean of getHit, getMiss
  - Map.writeGM   : geomean of putHit, putMiss, removeAndPutMiss
  - Map.forEachGM : geomean of forEach

Orderings are kept separate (never combined across order values). If a group
is missing one of its constituent (class, method) rows, the geomean is
computed from whichever subset is present and a warning is printed.

mean/median are geomeans of the constituent rows' mean/median values. stdev
is left blank since geomean-of-stdevs is not a meaningful statistic.

Re-running is idempotent: any existing *.readGM / *.writeGM rows are dropped
and recomputed from scratch each time.

Usage:
    python scripts/add_geomean_rows.py [libraries.csv]
"""

import csv
import math
import sys
from collections import defaultdict
from pathlib import Path

RESULTS_DIR   = Path(__file__).parent.parent / "benchmark-results"
LIBRARIES_CSV = RESULTS_DIR / "libraries.csv"

READ_METHODS    = {"getHit", "getMiss"}
WRITE_METHODS   = {"putHit", "putMiss", "removeAndPutMiss"}
FOREACH_METHODS = {"forEach"}


def geomean(values: list[float]) -> float:
    return math.exp(sum(math.log(v) for v in values) / len(values))


def is_gm_row(benchmark: str) -> bool:
    return benchmark.endswith(".readGM") or benchmark.endswith(".writeGM") or benchmark.endswith(".forEachGM")


def main() -> None:
    libraries_csv = Path(sys.argv[1]) if len(sys.argv) > 1 else LIBRARIES_CSV
    if not libraries_csv.exists():
        raise FileNotFoundError(f"Not found: {libraries_csv}")

    with libraries_csv.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames)
        lib_rows = list(reader)

    base_rows = [r for r in lib_rows if not is_gm_row(r["benchmark"])]

    # All classes seen anywhere, used to detect groups missing a whole class's data.
    classes = sorted({r["benchmark"].split(".")[0] for r in base_rows})

    # Group by (library, order, size) -> list of (class, method, row), combining
    # IntMap and LongMap together.
    groups: dict[tuple, list[tuple]] = defaultdict(list)
    for row in base_rows:
        cls, method = row["benchmark"].split(".")
        if method in READ_METHODS or method in WRITE_METHODS or method in FOREACH_METHODS:
            key = (row["library"], row["order"], row["size"])
            groups[key].append((cls, method, row))

    new_rows = []
    incomplete = []
    for (library, order, size), entries in sorted(groups.items()):
        for label, full_set in (
            ("readGM", READ_METHODS),
            ("writeGM", WRITE_METHODS),
            ("forEachGM", FOREACH_METHODS),
        ):
            present = [(cls, method, row) for cls, method, row in entries if method in full_set]
            if not present:
                continue

            expected = {(cls, m) for cls in classes for m in full_set}
            actual = {(cls, method) for cls, method, _ in present}
            if actual != expected:
                incomplete.append((library, order, size, label, sorted(actual)))

            rows = [row for _, _, row in present]
            unit = rows[0]["unit"]
            new_rows.append({
                "benchmark": f"Map.{label}",
                "library":   library,
                "order":     order,
                "size":      size,
                "median":    geomean([float(r["median"]) for r in rows]),
                "mean":      geomean([float(r["mean"]) for r in rows]),
                "stdev":     "",
                "unit":      unit,
            })

    if incomplete:
        print(f"Incomplete groups ({len(incomplete)}):")
        for library, order, size, label, present in incomplete:
            print(f"  Map.{label}  {library}  {order}  {size}  -> only {present}")

    updated = base_rows + new_rows

    with libraries_csv.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(updated)

    print(f"Removed old GM rows: {len(lib_rows) - len(base_rows)}")
    print(f"New GM rows added  : {len(new_rows)}")
    print(f"Output             : {libraries_csv}  ({len(updated)} rows total)")


if __name__ == "__main__":
    main()
