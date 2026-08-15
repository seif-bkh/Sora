#!/usr/bin/env python3
"""
extract_glyph.py — convert a font glyph into VectorDrawable `pathData`.

WHY THIS EXISTS
---------------
The Sora brand SVGs in `branding/` render the kanji 空 with SVG `<text>`
elements. Android's VectorDrawable format has **no text support at all** — the
only drawing primitive is `<path android:pathData="...">`. So every glyph has
to be converted to an outline path before it can ship in `app/src/main/res/`.

This script does that conversion deterministically, so the generated assets in
res/ can be regenerated from the branding SVGs at any time rather than being
opaque blobs someone hand-traced once.

FONT
----
Noto Serif JP (SIL Open Font License 1.1), the family named in the brand SVGs'
`font-family`. Fetched from the @fontsource/noto-serif-jp npm package. The font
itself is NOT vendored or shipped in the APK — it is used at build-asset
generation time only, and the resulting outline paths are what get committed.

COORDINATE TRANSFORM
--------------------
Fonts use a y-up coordinate system with the origin on the baseline; SVG and
VectorDrawable use y-down with the origin at the top-left of the viewport. For
a glyph drawn as `<text x=X y=Y font-size=S text-anchor=middle>`:

    scale   = S / unitsPerEm
    originX = X - (advanceWidth * scale) / 2      # middle anchor
    originY = Y                                    # baseline

    svg_x = originX + glyph_x * scale
    svg_y = originY - glyph_y * scale              # note the sign flip

Quadratic curves from TrueType outlines are emitted as `q`/`Q` commands, which
VectorDrawable supports natively — no conversion to cubics needed.

USAGE
    python3 extract_glyph.py --char 空 --font <path.woff> \\
        --size 38 --x 54 --y 50 --anchor middle
"""

from __future__ import annotations

import argparse
import sys

from fontTools.pens.basePen import BasePen
from fontTools.ttLib import TTFont


class VectorDrawablePen(BasePen):
    """A FontTools pen that emits SVG/VectorDrawable path syntax.

    Applies the font-space -> viewport-space transform described in the module
    docstring as it walks the outline, so no post-processing pass is required.
    """

    def __init__(self, glyph_set, scale: float, origin_x: float, origin_y: float, precision: int = 2):
        super().__init__(glyph_set)
        self._scale = scale
        self._origin_x = origin_x
        self._origin_y = origin_y
        self._precision = precision
        self.commands: list[str] = []

    def _fmt(self, value: float) -> str:
        """Trim trailing zeros so the emitted pathData stays compact."""
        text = f"{value:.{self._precision}f}".rstrip("0").rstrip(".")
        return "0" if text in ("", "-0") else text

    def _pt(self, pt) -> str:
        x = self._origin_x + pt[0] * self._scale
        # Sign flip: font y-up -> viewport y-down.
        y = self._origin_y - pt[1] * self._scale
        return f"{self._fmt(x)},{self._fmt(y)}"

    def _moveTo(self, pt):
        self.commands.append(f"M{self._pt(pt)}")

    def _lineTo(self, pt):
        self.commands.append(f"L{self._pt(pt)}")

    def _curveToOne(self, pt1, pt2, pt3):
        self.commands.append(f"C{self._pt(pt1)} {self._pt(pt2)} {self._pt(pt3)}")

    def _qCurveToOne(self, pt1, pt2):
        self.commands.append(f"Q{self._pt(pt1)} {self._pt(pt2)}")

    def _closePath(self):
        self.commands.append("Z")

    def path_data(self) -> str:
        return "".join(self.commands)


def extract(font_path: str, char: str, size: float, x: float, y: float, anchor: str) -> str:
    font = TTFont(font_path)
    upem = font["head"].unitsPerEm
    cmap = font.getBestCmap()
    codepoint = ord(char)

    if codepoint not in cmap:
        raise SystemExit(f"error: {char!r} (U+{codepoint:04X}) is not in {font_path}")

    glyph_name = cmap[codepoint]
    advance_width = font["hmtx"][glyph_name][0]
    scale = size / upem

    if anchor == "middle":
        origin_x = x - (advance_width * scale) / 2.0
    elif anchor == "end":
        origin_x = x - (advance_width * scale)
    else:  # "start"
        origin_x = x

    pen = VectorDrawablePen(font.getGlyphSet(), scale, origin_x, y)
    font.getGlyphSet()[glyph_name].draw(pen)
    return pen.path_data()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--font", required=True, help="Path to the .woff/.ttf/.otf font file")
    parser.add_argument("--char", required=True, help="The character to outline")
    parser.add_argument("--size", type=float, required=True, help="SVG font-size")
    parser.add_argument("--x", type=float, required=True, help="SVG text x")
    parser.add_argument("--y", type=float, required=True, help="SVG text y (baseline)")
    parser.add_argument("--anchor", default="start", choices=["start", "middle", "end"])
    args = parser.parse_args()

    print(extract(args.font, args.char, args.size, args.x, args.y, args.anchor))
    return 0


if __name__ == "__main__":
    sys.exit(main())
