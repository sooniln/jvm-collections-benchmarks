#!/usr/bin/env python3
"""
Parse the latest JMH benchmark result from benchmark-results/ and emit a CSV.

Columns: benchmark, library, [order,] size, median, mean, stdev, unit
  - benchmark : operation qualified by benchmark class (e.g. IntMap.getHit, LongMap.putMiss)
  - library   : the implementation under test, from params.type (e.g. FastCollect, Fastutil, JRE)
  - order     : key-distribution pattern (e.g. sequential, random, highBits);
                omitted from the CSV entirely if no entry in the file has an order param
  - size      : number of elements (e.g. 3000, 192000)
  - median    : median of the raw per-iteration measurements
  - mean      : arithmetic mean of the raw per-iteration measurements
  - stdev     : sample standard deviation of the raw per-iteration measurements
  - unit      : timing unit from the JSON (e.g. ns/op, us/op)

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


def stats_of_raw_data(raw_data: list[list[float]]) -> tuple[float, float, float]:
    """rawData is [[iter, iter, …], …] (forks × iterations). Return (median, mean, stdev)."""
    flat = [v for fork in raw_data for v in fork]
    if not flat:
        raise ValueError("rawData is empty")
    return (
        statistics.median(flat),
        statistics.mean(flat),
        statistics.stdev(flat) if len(flat) > 1 else 0.0,
    )


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
        median, mean, stdev = stats_of_raw_data(entry["primaryMetric"]["rawData"])
        rows.append({
            "library":    params["type"],
            "benchmark":  f"{class_label}.{method}",
            "order":      params.get("order"),   # None when absent
            "size":       int(params["size"]),
            "median":     median,
            "mean":       mean,
            "stdev":      stdev,
            "unit":       entry["primaryMetric"]["scoreUnit"],
        })

    # Stable sort: benchmark → library → (order) → size
    rows.sort(key=lambda r: (r["benchmark"], r["library"], r.get("order", ""), r["size"]))
    return rows


def write_csv(rows: list[dict], out_path: Path) -> None:
    has_order = any(r["order"] is not None for r in rows)
    fieldnames = ["benchmark", "library"] + (["order"] if has_order else []) + ["size", "median", "mean", "stdev", "unit"]
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
