#!/usr/bin/env python3
"""
For a given benchmark (and optional order filter), print a Markdown table
with one column per library, holding the geometric mean of medians across
all matching sizes.

Usage:
    python scripts/geomean_table.py <benchmark> [--order ORDER] [file.csv]

Examples:
    python scripts/geomean_table.py IntMap.getHit
    python scripts/geomean_table.py IntMap.getHit --order random
    python scripts/geomean_table.py IntMap.getHit --order random benchmark-results/2026-07-17_10-31-54.csv

Defaults to benchmark-results/libraries.csv if no file is given.
"""

import argparse
import csv
import math
from pathlib import Path

RESULTS_DIR   = Path(__file__).parent.parent / "benchmark-results"
LIBRARIES_CSV = RESULTS_DIR / "libraries.csv"


def geomean(values: list[float]) -> float:
    if len(values) == 0:
        return -1
    else:
        return math.exp(sum(math.log(v) for v in values) / len(values))


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("benchmark", help="benchmark to summarize, e.g. IntMap.getHit")
    parser.add_argument("--order", default=None, help="restrict to a single order value (e.g. random)")
    parser.add_argument(
        "file", type=Path, nargs="?", default=LIBRARIES_CSV,
        help="input CSV (default: benchmark-results/libraries.csv)",
    )
    args = parser.parse_args()

    if not args.file.exists():
        raise FileNotFoundError(f"Not found: {args.file}")

    with args.file.open(encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))

    matching = [r for r in rows if r["benchmark"] == args.benchmark]
    if args.order is not None:
        matching = [r for r in matching if r.get("order", "") == args.order]

    if not matching:
        raise SystemExit(f"No rows found for benchmark={args.benchmark!r} order={args.order!r} in {args.file}")

    unit = matching[0]["unit"]
    libraries = list(dict.fromkeys(r["library"] for r in matching))

    medians_by_library: dict[str, list[float]] = {lib: [] for lib in libraries}
    for r in matching:
        try:
            median = float(r["median"])
        except ValueError:
            continue
        medians_by_library[r["library"]].append(median)

    values = [geomean(medians_by_library[lib]) for lib in libraries]

    title = f"## {args.benchmark}" + (f" (order={args.order})" if args.order else "") + f" ({unit})\n"
    header = "| " + " | ".join(libraries) + " |"
    sep    = "| " + " | ".join(["---:"] * len(libraries)) + " |"
    row    = "| " + " | ".join(f"{v:.3f}" for v in values) + " |"

    print(title)
    print(header)
    print(sep)
    print(row)


if __name__ == "__main__":
    main()
