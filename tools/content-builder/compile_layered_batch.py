#!/usr/bin/env python3
"""Compile a directory of layered records into one candidate content package."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from compile_layered_content import LayerError, compile_package, validate_layers


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = ROOT / "content-source" / "content-strategy-v1" / "batch"


def records(path: Path) -> list[dict]:
    try:
        with path.open(encoding="utf-8") as file:
            value = json.load(file)
    except (OSError, json.JSONDecodeError) as exc:
        raise LayerError(f"无法读取 JSON: {path}: {exc}") from exc
    if isinstance(value, dict) and isinstance(value.get("records"), list):
        value = value["records"]
    if not isinstance(value, list) or not value or not all(isinstance(item, dict) for item in value):
        raise LayerError(f"{path} 必须是非空对象数组，或包含 records 数组")
    return value


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", default="2.0.0-candidate")
    parser.add_argument("--allow-draft", action="store_true")
    args = parser.parse_args()

    bases = records(args.source / "character_base.json")
    children = {item.get("characterId"): item for item in records(args.source / "child_content.json")}
    experiences = {item.get("characterId"): item for item in records(args.source / "experience.json")}
    if len(children) != len(records(args.source / "child_content.json")):
        raise LayerError("child_content.json 存在重复 characterId")
    if len(experiences) != len(records(args.source / "experience.json")):
        raise LayerError("experience.json 存在重复 characterId")

    compiled = []
    for base in bases:
        character_id = base.get("id")
        if character_id not in children or character_id not in experiences:
            raise LayerError(f"{character_id} 缺少儿童加工层或体验层")
        child, experience = children[character_id], experiences[character_id]
        validate_layers(base, child, experience, args.allow_draft)
        compiled.append(compile_package(base, child, experience, args.version))

    characters = [item["characters"][0] for item in compiled]
    characters.sort(key=lambda item: item["order"])
    if [item["order"] for item in characters] != list(range(1, len(characters) + 1)):
        raise LayerError("批量体验层 learningOrder 必须从 1 连续编号")
    options = [option for item in characters for option in [
        {"id": f"text_{item['id']}", "kind": "TEXT", "characterId": item["id"], "text": item["character"]}
    ]]
    output = {
        "schemaVersion": 2,
        "contentVersion": args.version,
        "layers": {
            "foundationVersion": "batch-source",
            "childContentVersion": "child-content-v1",
            "experienceVersion": "experience-v1",
        },
        "childLearningPack": {
            "schemaVersion": 1,
            "packVersion": "child-content-v1",
            "characters": [entry for item in compiled for entry in item["childLearningPack"]["characters"]],
        },
        "course": {"stageTestThreshold": 3, "badgeMilestones": []},
        "learningOrder": [item["id"] for item in characters],
        "reviewOffsetsDays": [1, 3, 7, 14, 30, 60],
        "optionCatalog": options,
        "characters": characters,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Layered batch candidate pack written: {args.output} ({len(characters)} characters)")


if __name__ == "__main__":
    try:
        main()
    except LayerError as exc:
        raise SystemExit(f"CONTENT ERROR: {exc}")
