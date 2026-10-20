#!/usr/bin/env python3
"""Check that the Android renderer dispatches every JSONC schema component type."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
schema = root.parent / "megalodonte-declarative/megalodonte-jsonc-ui/schema/view.schema.json"
renderer = root / "android-bridge/src/main/java/megalodonte/android/bridge/AndroidJsoncScreen.java"
declared = set(json.loads(schema.read_text(encoding="utf-8"))["$defs"]["node"]["properties"]["type"]["enum"])
source = renderer.read_text(encoding="utf-8")
handled = set()
for labels in re.findall(r'case\s+((?:"[^"]+"\s*,?\s*)+)\s*->', source):
    handled.update(re.findall(r'"([^"]+)"', labels))
missing = declared - handled
if missing:
    raise SystemExit("Android JSONC renderer is missing: " + ", ".join(sorted(missing)))
print(f"Android JSONC renderer covers all {len(declared)} schema component types")
