#!/usr/bin/env python3
"""
Upsert rows from a benchmark CSV into libraries.csv.

For each row in the input CSV, replaces the matching row in libraries.csv
(matched on benchmark + library + order + size) or appends it if absent.

Usage:
    python scripts/upsert_results.py <path-to-csv>
"""

import csv
import sys
from pathlib import Path

RESULTS_DIR   = Path(__file__).parent.parent / "benchmark-results"
LIBRARIES_CSV = RESULTS_DIR / "libraries.csv"


def row_key(row: dict) -> tuple:
    return (row["benchmark"], row["library"], row.get("order", ""), int(row["size"]))


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(f"Usage: {sys.argv[0]} <path-to-csv>")

    input_path = Path(sys.argv[1])
    if not input_path.exists():
        sys.exit(f"Not found: {input_path}")
    if not LIBRARIES_CSV.exists():
        sys.exit(f"Not found: {LIBRARIES_CSV}")

    with input_path.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        incoming = {row_key(row): row for row in reader}

    with LIBRARIES_CSV.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames)
        lib_rows = list(reader)

    replaced = 0
    added = 0
    updated = []
    seen_keys: set[tuple] = set()

    for row in lib_rows:
        key = row_key(row)
        seen_keys.add(key)
        if key in incoming:
            updated.append(incoming[key])
            replaced += 1
        else:
            updated.append(row)

    for key, row in incoming.items():
        if key not in seen_keys:
            updated.append(row)
            added += 1

    with LIBRARIES_CSV.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(updated)

    print(f"Input     : {input_path}  ({len(incoming)} rows)")
    print(f"Replaced  : {replaced}")
    print(f"Added     : {added}")
    print(f"Output    : {LIBRARIES_CSV}  ({len(updated)} rows total)")


if __name__ == "__main__":
    main()