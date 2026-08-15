#!/usr/bin/env python3
"""
preview.py — render generated VectorDrawables back to PNG for visual checking.

This is a DEVELOPMENT AID ONLY. It reverses the VectorDrawable XML into a plain
SVG and rasterises it, so the generated drawables can be eyeballed without
Android Studio or a device. Nothing here feeds the build.

It also composites the adaptive icon the way a launcher does (background layer
+ foreground layer, masked to a circle) so the mark's safe-zone behaviour and
the absence of the old gradient seam can be verified directly.

    python3 tools/svg2vd/preview.py
"""

from __future__ import annotations

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DRAWABLE = REPO_ROOT / "app" / "src" / "main" / "res" / "drawable"
OUT = Path("/tmp/icon_preview")
A = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"


def vd_to_svg(path: Path) -> str:
    """Translate a VectorDrawable into an equivalent standalone SVG string."""
    root = ET.parse(path).getroot()
    vw = root.get(f"{A}viewportWidth", "108")
    vh = root.get(f"{A}viewportHeight", "108")

    defs: list[str] = []
    body: list[str] = []

    for index, node in enumerate(root):
        if not node.tag.endswith("path"):
            continue

        data = node.get(f"{A}pathData", "")
        fill = node.get(f"{A}fillColor")
        fill_alpha = node.get(f"{A}fillAlpha")
        stroke = node.get(f"{A}strokeColor")
        stroke_width = node.get(f"{A}strokeWidth")
        stroke_alpha = node.get(f"{A}strokeAlpha")
        cap = node.get(f"{A}strokeLineCap")

        # A nested <aapt:attr name="android:fillColor"><gradient> becomes an
        # SVG <linearGradient> in <defs>.
        gradient = node.find(f"{AAPT}attr/gradient")
        if gradient is not None:
            gid = f"grad{index}"
            x1, y1 = gradient.get(f"{A}startX", "0"), gradient.get(f"{A}startY", "0")
            x2, y2 = gradient.get(f"{A}endX", "0"), gradient.get(f"{A}endY", "0")
            stops = []
            for item in gradient:
                offset = item.get(f"{A}offset", "0")
                colour = item.get(f"{A}color", "#000000").lstrip("#")
                # VectorDrawable uses #AARRGGBB; SVG needs #RRGGBB + opacity.
                if len(colour) == 8:
                    opacity = int(colour[:2], 16) / 255.0
                    rgb = f"#{colour[2:]}"
                else:
                    opacity, rgb = 1.0, f"#{colour}"
                stops.append(
                    f'<stop offset="{offset}" stop-color="{rgb}" stop-opacity="{opacity:.3f}"/>'
                )
            defs.append(
                f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" '
                f'x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{"".join(stops)}</linearGradient>'
            )
            fill = f"url(#{gid})"

        attrs = [f'd="{data}"']
        attrs.append(f'fill="{fill}"' if fill else 'fill="none"')
        if fill_alpha:
            attrs.append(f'fill-opacity="{fill_alpha}"')
        if stroke:
            attrs.append(f'stroke="{stroke}"')
            attrs.append(f'stroke-width="{stroke_width or 1}"')
            if stroke_alpha:
                attrs.append(f'stroke-opacity="{stroke_alpha}"')
            if cap:
                attrs.append(f'stroke-linecap="{cap}"')
        body.append(f"<path {' '.join(attrs)}/>")

    defs_block = f"<defs>{''.join(defs)}</defs>" if defs else ""
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {vw} {vh}" '
        f'width="{vw}" height="{vh}">{defs_block}{"".join(body)}</svg>'
    )


# Rasterised with resvg via node. ImageMagick in a bare container usually has
# no SVG delegate (it shells out to rsvg-convert, which may not be installed),
# so resvg-js is the more reliable renderer here. Preview-only dependency.
RENDER_JS = Path("/tmp/render.js")


def rasterise(svg_text: str, out_png: Path, size: int = 432) -> bool:
    tmp_svg = out_png.with_suffix(".svg")
    tmp_svg.write_text(svg_text, encoding="utf-8")
    result = subprocess.run(
        ["node", str(RENDER_JS), str(tmp_svg), str(out_png), str(size)],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        print(f"  ! rasterise failed for {out_png.name}: {result.stderr.strip()[:200]}")
        return False
    return True


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)

    for name in ("ic_launcher_background", "ic_launcher_foreground",
                 "ic_launcher_monochrome", "ic_splash", "ic_notification"):
        src = DRAWABLE / f"{name}.xml"
        if not src.exists():
            continue
        size = 192 if name == "ic_notification" else 432
        if rasterise(vd_to_svg(src), OUT / f"{name}.png", size):
            print(f"  rendered {name}.png")

    # Composite the adaptive icon the way a launcher does: background layer,
    # foreground layer on top, then a circular mask.
    bg, fg = OUT / "ic_launcher_background.png", OUT / "ic_launcher_foreground.png"
    if bg.exists() and fg.exists():
        flat = OUT / "adaptive_flat.png"
        subprocess.run(["convert", str(bg), str(fg), "-composite", str(flat)], check=False)

        mask = OUT / "circle_mask.png"
        subprocess.run(
            ["convert", "-size", "432x432", "xc:none", "-fill", "white",
             "-draw", "circle 216,216 216,0", str(mask)], check=False,
        )
        subprocess.run(
            ["convert", str(flat), str(mask), "-alpha", "off",
             "-compose", "CopyOpacity", "-composite", str(OUT / "adaptive_circle.png")],
            check=False,
        )
        print("  rendered adaptive_flat.png / adaptive_circle.png")

    print(f"Previews in {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
