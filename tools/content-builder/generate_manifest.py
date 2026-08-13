#!/usr/bin/env python3
"""Create a deterministic manifest only after validate_media.py succeeds."""
from __future__ import annotations
import hashlib, json
from pathlib import Path
from validate_media import main as validate_media

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / 'app' / 'src' / 'main' / 'assets' / 'content' / 'packs' / 'starter-thirty-v1' / '2.0.0'

def main() -> None:
    validate_media()
    resources = []
    for path in sorted((p for p in PACK.rglob('*') if p.is_file() and p.name not in {'manifest.json', 'content.json', 'pack.json'}), key=lambda p: p.as_posix()):
        relative = path.relative_to(PACK).as_posix()
        resources.append({'path': relative, 'sizeBytes': path.stat().st_size, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
    (PACK / 'manifest.json').write_text(json.dumps({'version': '2.0.0', 'resources': resources}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'Wrote {len(resources)} real resources to manifest.json')

if __name__ == '__main__': main()
