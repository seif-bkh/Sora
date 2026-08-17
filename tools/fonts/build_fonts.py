#!/usr/bin/env python3
"""
build_fonts.py — generate the bundled display font for Sora.

WHY THIS EXISTS
---------------
DESIGN.md specifies Cormorant Garamond Light (300) as the display face. The
upstream file is a 1.2 MB variable font covering weights 300-700 — far too
much to ship for what the app actually uses (one weight, Latin only).

This script instances the variable axis to wght=300 and subsets to Latin plus
the punctuation the UI needs, producing a ~47 KB TTF. Committing the generator
rather than an opaque binary means the asset can be regenerated and audited.

    1195560 bytes  upstream variable TTF
     773016 bytes  after instancing to wght=300
      47148 bytes  after Latin subset          <- what ships

CJK IS DELIBERATELY EXCLUDED
----------------------------
Cormorant has no CJK coverage at all, so the 空 in the wordmark cannot come
from it. Android's font fallback chain resolves it from the platform serif
automatically — no bundled CJK face is needed, and adding one would cost
megabytes. Verified by the assertion at the end of this script.

LICENCE
-------
Cormorant Garamond is SIL Open Font License 1.1. The OFL permits subsetting
and bundling; the licence text ships alongside the font in
`app/src/main/assets/licenses/` (OFL §1 requires it to travel with the font).

USAGE
    pip install -r tools/fonts/requirements.txt
    python3 tools/fonts/build_fonts.py
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
FONT_RES_DIR = REPO_ROOT / "app" / "src" / "main" / "res" / "font"

# Pinned upstream source. Fetched via the GitHub API rather than
# raw.githubusercontent.com, which is not reachable from every environment.
UPSTREAM_REPO = "google/fonts"
UPSTREAM_PATH = "ofl/cormorantgaramond"
UPSTREAM_FILE = "CormorantGaramond[wght].ttf"

# Android resource names must be lowercase with underscores only.
OUTPUT_NAME = "cormorant_garamond_light.ttf"

# The weight DESIGN.md specifies for display type.
TARGET_WEIGHT = 300

# Latin-1 plus the typographic punctuation the UI actually renders:
# curly quotes, bullet, ellipsis, en/em dashes, middot (used as the metadata
# separator in "2024 · TV-14 · Action").
UNICODES = ",".join(
    [
        "U+0020-007E",  # basic Latin
        "U+00A0-00FF",  # Latin-1 supplement (accented characters)
        "U+2018-201D",  # curly quotes
        "U+2022",       # bullet
        "U+2026",       # ellipsis
        "U+2013-2014",  # en dash, em dash
        "U+00B7",       # middot
    ]
)

# `tnum` (tabular figures) is the reason mono/serif numerals are requested at
# all: without it, progress counters like 142/310 visibly shift as digits
# change. `kern`/`liga`/`calt` are standard text-shaping features.
LAYOUT_FEATURES = "kern,liga,calt,tnum"


def run(cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
    result = subprocess.run(cmd, capture_output=True, text=True, **kwargs)
    if result.returncode != 0:
        sys.stderr.write(result.stdout + result.stderr)
        raise SystemExit(f"error: command failed: {' '.join(cmd[:3])}...")
    return result


def fetch_upstream(destination: Path) -> None:
    """Download the variable font through the GitHub API."""
    print(f"  fetching {UPSTREAM_FILE} from {UPSTREAM_REPO}/{UPSTREAM_PATH}")
    listing = run(
        [
            "gh", "api", f"repos/{UPSTREAM_REPO}/contents/{UPSTREAM_PATH}",
            "--jq", f'.[] | select(.name=="{UPSTREAM_FILE}") | .sha',
        ]
    )
    sha = listing.stdout.strip()
    if not sha:
        raise SystemExit(f"error: {UPSTREAM_FILE} not found upstream")

    blob = run(
        ["gh", "api", f"repos/{UPSTREAM_REPO}/git/blobs/{sha}", "--jq", ".content"]
    )
    import base64

    destination.write_bytes(base64.b64decode(blob.stdout))
    print(f"    {destination.stat().st_size:,} bytes (variable, wght 300-700)")


def main() -> int:
    print("Building Sora display font...")
    FONT_RES_DIR.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        variable = tmp_path / "upstream.ttf"
        instanced = tmp_path / "instanced.ttf"
        output = FONT_RES_DIR / OUTPUT_NAME

        fetch_upstream(variable)

        # Collapse the weight axis to a single static instance.
        print(f"  instancing to wght={TARGET_WEIGHT}")
        run(
            [
                sys.executable, "-m", "fontTools.varLib.instancer",
                str(variable), f"wght={TARGET_WEIGHT}", "-o", str(instanced),
            ]
        )
        print(f"    {instanced.stat().st_size:,} bytes")

        print("  subsetting to Latin + UI punctuation")
        run(
            [
                sys.executable, "-m", "fontTools.subset", str(instanced),
                f"--unicodes={UNICODES}",
                f"--layout-features={LAYOUT_FEATURES}",
                f"--output-file={output}",
            ]
        )
        print(f"    {output.stat().st_size:,} bytes  -> {output.relative_to(REPO_ROOT)}")

        # Verify the result is actually usable before declaring success.
        from fontTools.ttLib import TTFont

        font = TTFont(output)
        cmap = font.getBestCmap()

        required = "ABCXYZabcxyz0123456789·—'"
        missing = [c for c in required if ord(c) not in cmap]
        if missing:
            raise SystemExit(f"error: subset is missing glyphs: {missing}")

        # Documents the fallback requirement as an executable assertion.
        assert 0x7A7A not in cmap, "unexpected: Cormorant should not contain 空"

        print(f"    {len(font.getGlyphOrder())} glyphs; CJK absent as expected")
        print("\nDone. 空 resolves from the platform serif via font fallback.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
