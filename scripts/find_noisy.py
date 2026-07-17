#!/usr/bin/env python3
"""
Identify noisy benchmark rows (CoV > threshold%) from a results CSV.
Outputs benchmark-results/rerun_candidates.csv.

Usage:
    python find_noisy.py [results.csv]

Defaults to the latest CSV in benchmark-results/ if no argument is given.

CoV = stdev / mean * 100  (coefficient of variation, as a percentage)
"""

import csv
import sys
from pathlib import Path

THRESHOLD_PCT = 15.0


def main():
    results_dir = Path(__file__).parent.parent / "benchmark-results"
    if len(sys.argv) > 1:
        latest = Path(sys.argv[1])
        if not latest.exists():
            sys.exit(f"Not found: {latest}")
    else:
        csvs = sorted(results_dir.glob("*.csv"))
        if not csvs:
            raise FileNotFoundError("No CSV files found in benchmark-results/")
        latest = csvs[-1]
    print(f"Input : {latest}")

    noisy = []
    total = 0
    with latest.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            total += 1
            mean = float(row["mean"])
            stdev = float(row["stdev"])
            if mean == 0:
                continue
            cov = stdev / mean * 100
            if cov > THRESHOLD_PCT:
                prefix = row["benchmark"].split(".")[0]  # e.g. "IntMap"
                noisy.append({
                    "gradle_task": f"jmh{prefix}",
                    "benchmark":   row["benchmark"],
                    "library":     row["library"],
                    "order":       row.get("order", ""),
                    "size":        int(row["size"]),
                    "cov_pct":     round(cov, 2),
                })

    noisy.sort(key=lambda r: (-r["cov_pct"], r["benchmark"], r["library"], r["size"]))
    print(f"Noisy : {len(noisy)} / {total} rows exceed {THRESHOLD_PCT}% CoV")

    out_path = results_dir / "rerun_candidates.csv"
    fieldnames = ["gradle_task", "benchmark", "library", "order", "size", "cov_pct"]
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(noisy)
    print(f"Output: {out_path}  ({len(noisy)} rows)")


if __name__ == "__main__":
    main()
