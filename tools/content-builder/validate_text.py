#!/usr/bin/env python3
"""Validate frozen PR-03A CSV text without changing it."""
from __future__ import annotations
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "starter-thirty-v1"

def read(name: str):
    with (SOURCE / name).open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))

def main() -> None:
    chars, readings, words, sentences = read("characters.csv"), read("readings.csv"), read("words.csv"), read("sentences.csv")
    assert len(chars) == 25 and len({r['id'] for r in chars}) == 25
    assert all(r['id'].startswith('char_u') for r in chars)
    assert all(12 <= len(r['meaning_for_child']) <= 35 and len(r['teaching_prompt']) <= 45 for r in chars)
    char_by_id = {r['id']: r['character'] for r in chars}
    assert all(r['character_id'] in char_by_id and char_by_id[r['character_id']] in r['text'] for r in words)
    assert all(sum(w['character_id'] == c['id'] for w in words) == 3 for c in chars)
    assert all(c['character'] in next(s['text'] for s in sentences if s['character_id'] == c['id']) for c in chars)
    assert all(int(r['tone_number']) in range(1, 5) for r in readings)
    print('PR-03A frozen text validation: OK')

if __name__ == '__main__': main()
