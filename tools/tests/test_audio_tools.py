from __future__ import annotations

import argparse
import asyncio
import csv
import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]


def load_module(name: str, relative: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / relative)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


tts = load_module("generate_edge_tts", "tools/audio-generator/generate_edge_tts.py")
media = load_module("validate_media", "tools/content-builder/validate_media.py")


def options(output_dir: Path, force: bool = False) -> argparse.Namespace:
    return argparse.Namespace(
        output_dir=output_dir, voice="zh-CN-XiaoyiNeural", rate="-8%", volume="+0%", pitch="+0Hz",
        force=force, manifest=None, index=None,
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
            self.assertTrue(tts.can_reuse(entry, row, target, current))
            changed_text = {**row, "text": "上"}
            self.assertFalse(tts.can_reuse(entry, changed_text, target, current))
            target.write_bytes(b"changed bytes")
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
            self.assertEqual("GENERATED", result["generationStatus"])


class MediaValidatorTests(unittest.TestCase):
    def test_rejects_path_escape(self) -> None:
        for value in ("../x.mp3", "/x.mp3", "C:/x.mp3", "audio\\x.mp3"):
            with self.subTest(value=value):
                with self.assertRaises(media.MediaValidationError):
                    media.safe_asset_path(value)

    def test_uses_probe_and_decoder_instead_of_header_checks(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "forged.mp3"
            path.write_bytes(b"ID3 this is garbage, not media")
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
            path.write_bytes(b"not a placeholder")
            calls: list[list[str]] = []

            def fake_run(command, **_kwargs):
                calls.append(command)
                if command[0] == "ffprobe":
                    return type("Result", (), {"returncode": 0, "stdout": json.dumps({"format": {"duration": "1.2"}, "streams": [{"codec_type": "audio", "codec_name": "mp3"}]}), "stderr": ""})()
                return type("Result", (), {"returncode": 0, "stdout": "", "stderr": ""})()

            with patch.object(media.shutil, "which", lambda name: name), patch.object(media.subprocess, "run", fake_run):
                media.validate_file(path)
            self.assertEqual(["ffprobe", "ffmpeg"], [call[0] for call in calls])


if __name__ == "__main__":
    unittest.main()
