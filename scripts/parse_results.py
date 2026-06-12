#!/usr/bin/env python3
"""
Parse the latest JMH benchmark result from benchmark-results/ and emit a CSV.

Columns: benchmark, library, [loadFactor, order, pow2,] size, score, unit
  - benchmark : operation qualified by benchmark class (e.g. Map32.getHit, Map64.putMiss)
  - library   : the implementation under test, from params.type (e.g. FastCollect, Fastutil, JRE)
  - order     : key-distribution pattern (e.g. sequential, random, highBits);
                omitted from the CSV entirely if no entry in the file has an order param
  - size      : number of elements (e.g. 3000, 192000)
  - score     : median of the raw per-iteration measurements
  - unit      : score unit from the JSON (e.g. ns/op, us/op)

Usage:
    python scripts/parse_results.py                        # latest JSON → <stem>.csv next to it
    python scripts/parse_results.py path/to/results.json   # explicit JSON input
    python scripts/parse_results.py results.json --out /tmp/out.csv
"""

import argparse
import csv
import json
import statistics
from pathlib import Path


def median_of_raw_data(raw_data: list[list[float]]) -> float:
    """rawData is [[iter, iter, …], …] (forks × iterations). Return the median."""
    flat = [v for fork in raw_data for v in fork]
    if not flat:
        raise ValueError("rawData is empty")
    return statistics.median(flat)


def find_latest_json(results_dir: Path) -> Path:
    files = sorted(results_dir.glob("*.json"))
    if not files:
        raise FileNotFoundError(f"No JSON files found in {results_dir}")
    return files[-1]   # lexicographic == chronological for YYYY-MM-DD_HH-MM-SS names


def parse(json_path: Path) -> list[dict]:
    with json_path.open(encoding="utf-8") as f:
        data = json.load(f)

    rows = []
    for entry in data:
        # e.g. "io.github.sooniln.fastcollect.Map32Benchmark.getHit"
        full_name: str = entry["benchmark"]
        class_name, method = full_name.rsplit(".", 1)
        class_label = class_name.rsplit(".", 1)[-1].removesuffix("Benchmark")  # "Map32"

        params = entry.get("params", {})
        rows.append({
            "library":    params["type"],
            "benchmark":  f"{class_label}.{method}",
            "loadFactor": float(params["loadFactor"]) if "loadFactor" in params else None,
            "order":      params.get("order"),   # None when absent
            "pow2":       int(params["pow2"]) if "pow2" in params else None,
            "size":       int(params["size"] if "size" in params else (2 ** int(params["pow2"]) * float(params["loadFactor"]))),
            "score":      median_of_raw_data(entry["primaryMetric"]["rawData"]),
            "unit":       entry["primaryMetric"]["scoreUnit"],
        })

    # Stable sort: benchmark → library → (order) → size
    rows.sort(key=lambda r: (r["benchmark"], r["library"], r.get("order", ""), r["size"]))
    return rows


def write_csv(rows: list[dict], out_path: Path) -> None:
    has_order = any(r["order"] is not None for r in rows)
    has_loadFactor = any(r["loadFactor"] is not None for r in rows)
    has_pow2 = any(r["pow2"] is not None for r in rows)

    fieldnames = ["benchmark", "library"] + (["order"] if has_order else []) + (["loadFactor"] if has_loadFactor else []) + (["pow2"] if has_pow2 else []) + ["size", "score", "unit"]
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    parent_dir  = Path(__file__).parent.parent
    results_dir = parent_dir / "benchmark-results"

    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "file", type=Path, nargs="?", default=None,
        help="benchmark JSON to parse (default: latest file in benchmark-results/)",
    )
    parser.add_argument(
        "--out", type=Path, default=None,
        help="output CSV path (default: <json-stem>.csv next to the JSON)",
    )
    args = parser.parse_args()

    json_path = args.file if args.file else find_latest_json(results_dir)
    out_path  = args.out  if args.out  else json_path.with_suffix(".csv")

    print(f"Input : {json_path}")
    rows = parse(json_path)
    write_csv(rows, out_path)
    print(f"Output: {out_path}  ({len(rows)} rows)")


if __name__ == "__main__":
    main()
