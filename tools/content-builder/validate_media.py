#!/usr/bin/env python3
"""Reject missing or fake candidate media. Requires ffprobe for final validation."""
from __future__ import annotations
import csv, shutil, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'content-source' / 'starter-thirty-v1' / 'generated' / 'media_requirements.csv'
PACK = ROOT / 'app' / 'src' / 'main' / 'assets' / 'content' / 'packs' / 'starter-thirty-v1' / '2.0.0'

def main() -> None:
    if not SOURCE.exists(): sys.exit('Run build_starter_thirty.py first.')
    missing = []
    with SOURCE.open(encoding='utf-8', newline='') as f:
        for row in csv.DictReader(f):
            file = PACK / row['asset_path']
            if not file.is_file() or file.stat().st_size == 0: missing.append(row['asset_path'])
            elif file.suffix == '.mp3' and file.read_bytes()[:3] not in (b'ID3', b'\xff\xfb'):
                sys.exit(f'Not a real MP3: {file}')
            elif file.suffix == '.webp' and file.read_bytes()[:4] != b'RIFF':
                sys.exit(f'Not a real WebP: {file}')
    if missing: sys.exit('Missing real candidate media:\n' + '\n'.join(missing))
    if shutil.which('ffprobe') is None: sys.exit('ffprobe is required for final MP3 metadata validation.')
    print('Candidate media file existence/type validation: OK')

if __name__ == '__main__': main()
