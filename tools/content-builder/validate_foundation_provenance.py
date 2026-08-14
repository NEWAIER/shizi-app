#!/usr/bin/env python3
"""Validate that every foundation record has traceable source metadata."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from compile_layered_content import LayerError


def read_json(path: Path):
    try:
        with path.open(encoding="utf-8") as file:
            return json.load(file)
    except (OSError, json.JSONDecodeError) as exc:
        raise LayerError(f"无法读取来源文件 {path}: {exc}") from exc


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--records", type=Path, required=True)
    parser.add_argument("--provenance", type=Path, required=True)
    parser.add_argument("--allow-draft", action="store_true")
    args = parser.parse_args()
    records = read_json(args.records)
    provenance = read_json(args.provenance)
    if isinstance(records, dict):
        records = records.get("records")
    if not isinstance(records, list) or not records:
        raise LayerError("基础记录必须是非空数组")
    required = ("sourceId", "sourceName", "license", "snapshotId", "snapshotSha256", "status")
    missing = [field for field in required if not provenance.get(field)]
    if missing:
        raise LayerError(f"来源元数据缺少字段: {', '.join(missing)}")
    if provenance["status"] != "VERIFIED" and not args.allow_draft:
        raise LayerError("来源状态不是 VERIFIED；候选源请使用 --allow-draft")
    if len(provenance["snapshotSha256"]) != 64 or not all(char in "0123456789abcdef" for char in provenance["snapshotSha256"].lower()):
        raise LayerError("snapshotSha256 必须是 64 位十六进制摘要")
    for index, record in enumerate(records):
        for field in ("source", "sourceLicense", "sourceSnapshot"):
            if not record.get(field):
                raise LayerError(f"第 {index + 1} 条基础记录缺少 {field}")
        if record["source"] != provenance["sourceId"]:
            raise LayerError(f"{record.get('id')} 的 source 与来源清单不一致")
        if record["sourceLicense"] != provenance["license"]:
            raise LayerError(f"{record.get('id')} 的 sourceLicense 与来源清单不一致")
        if record["sourceSnapshot"] != provenance["snapshotId"]:
            raise LayerError(f"{record.get('id')} 的 sourceSnapshot 与来源清单不一致")
    print(f"Foundation provenance validation: OK ({len(records)} records; status={provenance['status']})")


if __name__ == "__main__":
    try:
        main()
    except LayerError as exc:
        raise SystemExit(f"PROVENANCE ERROR: {exc}")
