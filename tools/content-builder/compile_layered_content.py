#!/usr/bin/env python3
"""Compile the three content layers into a legacy-compatible candidate pack.

This compiler is intentionally deterministic and offline. It creates a candidate
runtime-shaped JSON file, but never registers it in the Android asset catalog.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = ROOT / "content-source" / "content-strategy-v1"


class LayerError(ValueError):
    pass


def load_json(path: Path) -> dict:
    try:
        with path.open(encoding="utf-8") as file:
            value = json.load(file)
    except (OSError, json.JSONDecodeError) as exc:
        raise LayerError(f"无法读取 JSON: {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise LayerError(f"顶层必须是对象: {path}")
    return value


def require(value: object, field: str) -> object:
    if value is None or value == "":
        raise LayerError(f"缺少字段: {field}")
    return value


def validate_layers(base: dict, child: dict, experience: dict, allow_draft: bool) -> None:
    base_id = require(base.get("id"), "character_base.id")
    child_id = require(child.get("characterId"), "child_content.characterId")
    experience_id = require(experience.get("characterId"), "experience.characterId")
    if not (base_id == child_id == experience_id):
        raise LayerError(f"三层 characterId 不一致: {base_id}, {child_id}, {experience_id}")
    if not re.fullmatch(r"char_u[0-9a-f]+", str(base_id)):
        raise LayerError(f"非法 characterId: {base_id}")
    character = require(base.get("character"), "character_base.character")
    if len(character) != 1:
        raise LayerError("character_base.character 必须是一个汉字")
    if base.get("unicode") != f"U+{ord(character):X}":
        raise LayerError(f"Unicode 与 character 不一致: {base.get('unicode')} != U+{ord(character):X}")
    tone = base.get("tone")
    if not isinstance(tone, int) or tone not in range(1, 5):
        raise LayerError("character_base.tone 必须是 1-4")
    if not isinstance(base.get("strokeCount"), int) or base["strokeCount"] < 1:
        raise LayerError("character_base.strokeCount 必须是正整数")
    words = child.get("words")
    if not isinstance(words, list) or not 1 <= len(words) <= 3 or not all(isinstance(word, str) and word for word in words):
        raise LayerError("child_content.words 必须包含 1-3 个非空词语")
    if child.get("reviewStatus") != "ACTIVE" and not allow_draft:
        raise LayerError("只有 ACTIVE 儿童内容可以编译；候选包请使用 --allow-draft")
    if not isinstance(experience.get("learningOrder"), int) or experience["learningOrder"] < 1:
        raise LayerError("experience.learningOrder 必须是正整数")
    if not isinstance(experience.get("questionSeeds"), list) or not experience["questionSeeds"]:
        raise LayerError("experience.questionSeeds 不能为空")


def slug(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-") or "word"


def compile_package(base: dict, child: dict, experience: dict, version: str) -> dict:
    character_id = base["id"]
    character = base["character"]
    suffix = character_id.removeprefix("char_")
    text_option_id = f"text_{character_id}"
    options = [{"id": text_option_id, "kind": "TEXT", "characterId": character_id, "text": character}]
    question_seeds = []
    type_map = {
        "listen_choose_character": ("LISTEN_CHOOSE_CHARACTER", "SOUND_TO_SHAPE"),
        "character_choose_image": ("CHARACTER_CHOOSE_IMAGE", "SHAPE_TO_MEANING"),
        "find_same_character": ("SHAPE_RECOGNITION", "SHAPE"),
    }
    for index, seed in enumerate(experience["questionSeeds"], start=1):
        if seed not in type_map:
            raise LayerError(f"不支持的 question seed: {seed}")
        question_type, evidence = type_map[seed]
        qid = f"q_{suffix}_{slug(seed)}"
        question_seeds.append({
            "id": qid,
            "type": question_type,
            "promptAudio": f"audio/prompts/prompt_{slug(seed)}_v1.mp3",
            "correctOptionId": text_option_id,
            "optionIds": [text_option_id],
            "minLearnedCount": max(0, experience["learningOrder"] - 1),
            "evidenceCategory": evidence,
        })
    words = [
        {"text": word, "audioAsset": f"audio/words/{suffix}_{index}_{slug(word)}_v1.mp3"}
        for index, word in enumerate(child["words"], start=1)
    ]
    return {
        "schemaVersion": 2,
        "contentVersion": version,
        "layers": {
            "foundationVersion": base.get("sourceSnapshot", "unknown"),
            "childContentVersion": "child-content-v1",
            "experienceVersion": "experience-v1",
        },
        "childLearningPack": {
            "schemaVersion": 1,
            "packVersion": "child-content-v1",
            "characters": [{
                "characterId": character_id,
                "meaningForChild": child["meaningForChild"],
                "words": child["words"],
                "sentence": child["sentence"],
                "learningGoal": child["learningGoal"],
                "imageRequest": child["imageRequest"],
                "audioRequest": child["audioRequest"],
            }],
        },
        "course": {"stageTestThreshold": 3, "badgeMilestones": []},
        "learningOrder": [character_id],
        "reviewOffsetsDays": [1, 3, 7, 14, 30, 60],
        "optionCatalog": options,
        "characters": [{
            "id": character_id,
            "character": character,
            "pinyin": base["pinyin"],
            "toneNumber": base["tone"],
            "order": experience["learningOrder"],
            "meaningForChild": child["meaningForChild"],
            "imageAsset": f"images/characters/{character_id}_main_v1.webp",
            "imageAlt": child["imageTheme"],
            "words": words,
            "sentence": {"text": child["sentence"], "audioAsset": f"audio/sentences/{suffix}_v1.mp3"},
            "audio": {"character": f"audio/characters/{character_id}_v1.mp3", "meaning": f"audio/meanings/meaning_{suffix}_v1.mp3"},
            "teachingPrompt": child["teachingPrompt"],
            "confusableRestrictions": [],
            "misconceptions": child.get("misconceptions", []),
            "questionSeeds": question_seeds,
            "contentReview": {
                "textReviewed": child["reviewStatus"] in {"TEXT_REVIEWED", "MEDIA_REVIEWED", "PARENT_REVIEWED", "ACTIVE"},
                "assetReviewedByDeveloper": child["reviewStatus"] in {"MEDIA_REVIEWED", "PARENT_REVIEWED", "ACTIVE"},
                "assetReviewedByParent": child["reviewStatus"] in {"PARENT_REVIEWED", "ACTIVE"},
                "blockedReason": None if child["reviewStatus"] == "ACTIVE" else "candidate-layer-pack",
            },
        }],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", default="2.0.0-candidate")
    parser.add_argument("--allow-draft", action="store_true")
    args = parser.parse_args()
    source = args.source
    base = load_json(source / "character_base.sample.json")
    child = load_json(source / "child_content.sample.json")
    experience = load_json(source / "experience.sample.json")
    validate_layers(base, child, experience, args.allow_draft)
    compiled = compile_package(base, child, experience, args.version)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(compiled, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Layered candidate pack written: {args.output}")


if __name__ == "__main__":
    try:
        main()
    except LayerError as exc:
        raise SystemExit(f"CONTENT ERROR: {exc}")
