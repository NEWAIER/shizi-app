#!/usr/bin/env python3
"""Validate layered source records before compilation."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from compile_layered_content import LayerError, load_json, validate_layers


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = ROOT / "content-source" / "content-strategy-v1"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--allow-draft", action="store_true")
    args = parser.parse_args()
    base = load_json(args.source / "character_base.sample.json")
    child = load_json(args.source / "child_content.sample.json")
    experience = load_json(args.source / "experience.sample.json")
    validate_layers(base, child, experience, args.allow_draft)
    print(f"Layered source validation: OK ({base['id']})")


if __name__ == "__main__":
    try:
        main()
    except LayerError as exc:
        raise SystemExit(f"CONTENT ERROR: {exc}")
