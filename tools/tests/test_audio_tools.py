from __future__ import annotations

import argparse
import asyncio
import csv
import hashlib
import importlib.util
import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, patch

ROOT = Path(__file__).resolve().parents[2]


def load_module(name: str, relative: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / relative)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


tts = load_module("generate_edge_tts", "tools/audio-generator/generate_edge_tts.py")
media = load_module("validate_media", "tools/content-builder/validate_media.py")
batches = load_module("build_tts_batches", "tools/content-builder/build_tts_batches.py")
starter_builder = load_module("build_starter_thirty", "tools/content-builder/build_starter_thirty.py")


def options(output_dir: Path, force: bool = False) -> argparse.Namespace:
    return argparse.Namespace(
        output_dir=output_dir, voice="zh-CN-XiaoyiNeural", rate="-8%", volume="+0%", pitch="+0Hz",
        force=force, normalize_existing=False, manifest=None, index=None,
    )


class EdgeTtsGeneratorTests(unittest.TestCase):
    def write_csv(self, directory: Path, rows: list[list[str]]) -> Path:
        source = directory / "input.csv"
        with source.open("w", newline="", encoding="utf-8") as file:
            writer = csv.writer(file)
            writer.writerow(["id", "text", "filename"])
            writer.writerows(rows)
        return source

    def test_csv_rejects_duplicate_id_filename_and_escape_paths(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for rows, expected in [
                ([["same", "一", "a.mp3"], ["same", "上", "b.mp3"]], "duplicate CSV id"),
                ([["a", "一", "same.mp3"], ["b", "上", "same.mp3"]], "duplicate CSV filename"),
                ([["a", "一", "../escape.mp3"]], "escapes output directory"),
                ([["a", "一", "C:/escape.mp3"]], "escapes output directory"),
                ([["a", "一", "sub\\escape.mp3"]], "relative POSIX path"),
            ]:
                with self.subTest(expected=expected):
                    with self.assertRaisesRegex(ValueError, expected):
                        tts.load_rows(self.write_csv(directory, rows))

    def test_existing_file_reuse_requires_matching_fingerprint_and_hash(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            target = directory / "a.mp3"
            target.write_bytes(b"verified bytes")
            row = {"id": "a", "text": "一", "filename": "a.mp3"}
            current = options(directory)
            entry = {"inputFingerprint": tts.provenance(row, current), "sha256": tts.sha256(target)}
            with patch.object(tts, "validate_mp3", return_value=1000):
                self.assertTrue(tts.can_reuse(entry, row, target, current))
            changed_text = {**row, "text": "上"}
            with patch.object(tts, "validate_mp3", return_value=1000):
                self.assertFalse(tts.can_reuse(entry, changed_text, target, current))
            target.write_bytes(b"changed bytes")
            with patch.object(tts, "validate_mp3", return_value=1000):
                self.assertFalse(tts.can_reuse(entry, row, target, current))

    def test_mismatched_existing_file_is_preserved_without_force(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            target = directory / "a.mp3"
            original = b"must not be overwritten"
            target.write_bytes(original)
            row = {"id": "a", "text": "一", "filename": "a.mp3"}
            result = asyncio.run(tts.generate_one(row, options(directory), {"sha256": "wrong"}))
            self.assertEqual("FAILED", result["generationStatus"])
            self.assertEqual(original, target.read_bytes())
            self.assertIn("--force", result["error"])

    def test_reused_report_preserves_generated_at_and_uses_posix_path(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "voice"
            directory.mkdir()
            target = directory / "nested" / "a.mp3"
            target.parent.mkdir()
            target.write_bytes(b"x")
            row = {"id": "a", "text": "一", "filename": "nested/a.mp3"}
            current = options(directory)
            existing = {"inputFingerprint": tts.provenance(row, current), "sha256": tts.sha256(target), "generatedAt": "2026-01-01T00:00:00+00:00"}
            with patch.object(tts, "validate_mp3", return_value=1000):
                result = asyncio.run(tts.generate_one(row, current, existing))
            self.assertEqual("2026-01-01T00:00:00+00:00", result["generatedAt"])
            self.assertNotIn("\\", result["outputPath"])
            self.assertEqual("REUSED_VERIFIED", result["generationStatus"])

    def test_batch_failure_preserves_old_manifest_and_writes_failed_items(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            source = self.write_csv(directory, [["a", "一", "a.mp3"]])
            manifest = directory / "audition_manifest.json"
            original = "[{\"old\": true}]\n"
            manifest.write_text(original, encoding="utf-8")
            current = options(directory)
            current.input, current.manifest, current.index = source, manifest, directory / "AUDITION_INDEX.md"
            failed = {"id": "a", "generationStatus": "FAILED", "error": "intentional"}
            with patch.object(tts, "args", return_value=current), patch.object(tts, "generate_one", AsyncMock(return_value=failed)):
                self.assertEqual(1, asyncio.run(tts.main()))
            self.assertEqual(original, manifest.read_text(encoding="utf-8"))
            self.assertEqual([failed], json.loads((directory / "failed_items.json").read_text(encoding="utf-8")))


class MediaValidatorTests(unittest.TestCase):
    def test_rejects_path_escape(self) -> None:
        for value in ("../x.mp3", "/x.mp3", "C:/x.mp3", "audio\\x.mp3"):
            with self.subTest(value=value):
                with self.assertRaises(media.MediaValidationError):
                    media.safe_asset_path(value)

    def test_uses_probe_and_decoder_instead_of_header_checks(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "forged.mp3"
            path.write_bytes(b"ID3" + b"x" * 2048)
            calls: list[list[str]] = []

            def fake_which(name: str) -> str:
                return name

            def fake_run(command, **_kwargs):
                calls.append(command)
                if command[0] == "ffprobe":
                    return type("Result", (), {"returncode": 1, "stdout": "", "stderr": "invalid data"})()
                return type("Result", (), {"returncode": 0, "stdout": "", "stderr": ""})()

            with patch.object(media.shutil, "which", fake_which), patch.object(media.subprocess, "run", fake_run):
                with self.assertRaisesRegex(media.MediaValidationError, "ffprobe rejected"):
                    media.validate_file(path)
            self.assertEqual("ffprobe", calls[0][0])
            self.assertEqual(1, len(calls), "decoder must not run after a failed probe")

    def test_valid_probe_requires_decoder(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "real.mp3"
            path.write_bytes(b"not a placeholder" * 100)
            calls: list[list[str]] = []

            def fake_run(command, **_kwargs):
                calls.append(command)
                if command[0] == "ffprobe":
                    return type("Result", (), {"returncode": 0, "stdout": json.dumps({"format": {"duration": "1.2", "bit_rate": "96000"}, "streams": [{"codec_type": "audio", "codec_name": "mp3", "channels": 1, "sample_rate": "44100", "bit_rate": "96000"}]}), "stderr": ""})()
                volume = "mean_volume: -20.0 dB" if "volumedetect" in command else ""
                return type("Result", (), {"returncode": 0, "stdout": volume, "stderr": ""})()

            with patch.object(media.shutil, "which", lambda name: name), patch.object(media.subprocess, "run", fake_run):
                media.validate_file(path)
            self.assertEqual(["ffprobe", "ffmpeg", "ffmpeg"], [call[0] for call in calls])


@unittest.skipUnless(shutil.which("ffmpeg") and shutil.which("ffprobe"), "FFmpeg is required for real-media tests")
class ActualMediaValidationTests(unittest.TestCase):
    def ffmpeg(self, *arguments: str) -> None:
        subprocess.run([shutil.which("ffmpeg"), "-y", *arguments], check=True, capture_output=True)

    def test_real_mp3_rejects_silence_truncation_channels_and_rate(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            valid, silent, stereo, low_rate, broken = (root / name for name in ("valid.mp3", "silent.mp3", "stereo.mp3", "low.mp3", "broken.mp3"))
            self.ffmpeg("-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-ac", "1", "-ar", "44100", "-b:a", "96k", str(valid))
            self.ffmpeg("-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono", "-t", "1", "-ac", "1", "-ar", "44100", "-b:a", "96k", str(silent))
            self.ffmpeg("-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-ac", "2", "-ar", "44100", "-b:a", "96k", str(stereo))
            self.ffmpeg("-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-ac", "1", "-ar", "22050", "-b:a", "96k", str(low_rate))
            broken.write_bytes(valid.read_bytes()[:100])
            media.validate_mp3(valid)
            for invalid in (silent, stereo, low_rate, broken):
                with self.subTest(invalid=invalid.name), self.assertRaises(media.MediaValidationError):
                    media.validate_mp3(invalid)

    def test_real_webp_rejects_fake_wrong_size_and_oversize(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            wrong, fake, oversized = root / "wrong.webp", root / "fake.webp", root / "large.webp"
            self.ffmpeg("-f", "lavfi", "-i", "color=c=red:s=64x64", "-frames:v", "1", "-c:v", "libwebp", str(wrong))
            fake.write_bytes(b"RIFF" + b"not-a-webp" * 200)
            oversized.write_bytes(wrong.read_bytes() + b"x" * (media.MAX_WEBP_BYTES + 1))
            for invalid in (wrong, fake, oversized):
                with self.subTest(invalid=invalid.name), self.assertRaises(media.MediaValidationError):
                    media.validate_webp(invalid)


class ProductionBatchTests(unittest.TestCase):
    def test_real_source_creates_five_correct_30_item_batches_stably(self) -> None:
        built = batches.build(batches.load())
        self.assertEqual(5, len(built)); self.assertTrue(all(len(batch) == 30 for batch in built))
        source_by_id = {row["id"]: row["character_id"] for row in batches.load()}
        for index, characters in enumerate(batches.BATCHES):
            self.assertEqual({batches.CHARACTER_IDS[value] for value in characters}, {source_by_id[row["id"]] for row in built[index]})
        with tempfile.TemporaryDirectory() as temp:
            target = Path(temp); batches.write(target, built)
            first = (target / "all-150.csv").read_bytes(); batches.write(target, built)
            self.assertEqual(first, (target / "all-150.csv").read_bytes())

    def test_source_rejects_duplicate_missing_empty_and_escape_values(self) -> None:
        header = list(batches.FIELDS)
        good = ["a", "char_u4e00", "character", "一", "audio/a.mp3"]
        cases = [
            (header, [good, good], "duplicate id or output_file"),
            (header[:-1], [good[:-1]], "source fields"),
            (header, [["a", "char_u4e00", "character", "", "audio/a.mp3"]], "id and text"),
            (header, [["a", "char_u4e00", "character", "一", "../a.mp3"]], "invalid output_file"),
        ]
        with tempfile.TemporaryDirectory() as temp:
            for fields, rows, message in cases:
                path = Path(temp) / "bad.csv"
                with path.open("w", newline="", encoding="utf-8") as file:
                    writer = csv.writer(file); writer.writerow(fields); writer.writerows((rows * 150)[:150])
                with self.subTest(message=message), self.assertRaisesRegex(ValueError, message):
                    batches.load(path)

    def test_ci_jobs_and_gradlew_mode_are_committed(self) -> None:
        workflow = (ROOT / ".github/workflows/android-quality.yml").read_text(encoding="utf-8")
        self.assertIn("audio-tool-quality:", workflow); self.assertIn("android-quality:", workflow)
        mode = subprocess.check_output(["git", "ls-files", "--stage", "gradlew"], cwd=ROOT, text=True)
        self.assertTrue(mode.startswith("100755 "))


class QuestionMatrixTests(unittest.TestCase):
    def test_starter_thirty_question_matrix_is_complete_and_frozen(self) -> None:
        source = ROOT / "content-source" / "starter-thirty-v1"
        with (source / "options.csv").open(encoding="utf-8", newline="") as file: options = {row["option_id"]: row for row in csv.DictReader(file)}
        with (source / "questions.csv").open(encoding="utf-8", newline="") as file: questions = list(csv.DictReader(file))
        with (source / "characters.csv").open(encoding="utf-8", newline="") as file: characters = list(csv.DictReader(file))
        self.assertEqual(25, len(characters)); self.assertEqual(125, len(questions)); self.assertEqual(125, len({row["question_id"] for row in questions})); self.assertEqual(len(options), len(set(options)))
        expected = {"LISTEN_CHOOSE_CHARACTER", "CHARACTER_CHOOSE_IMAGE", "CHARACTER_CHOOSE_AUDIO", "SHAPE_RECOGNITION", "LIFE_WORD_CONTEXT"}
        for character in characters:
            current = [row for row in questions if row["character_id"] == character["id"]]
            self.assertEqual(expected, {row["question_type"] for row in current})
            for question in current:
                option_ids = json.loads(question["option_ids"])
                self.assertEqual(1, option_ids.count(question["correct_option_id"])); self.assertTrue(all(value in options for value in option_ids))
                self.assertEqual(question["evidence_category"], starter_builder.EVIDENCE[question["question_type"]])
                self.assertTrue(1 <= int(question["min_learned_count"]) <= 25); self.assertEqual("PENDING", question["parent_review_status"])
        self.assertTrue(all(".." not in row["asset_path"] and "\\" not in row["asset_path"] for row in options.values()))


if __name__ == "__main__":
    unittest.main()
