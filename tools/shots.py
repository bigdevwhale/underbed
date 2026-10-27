"""Turns screen renders or phone captures into the README screenshots.

    ./gradlew testDebugUnitTest -Prender --tests '*RenderArtScreens*'
    python tools/shots.py app/build/art
reads <dir>/screens-<lang>/*.png (or <dir>/<lang>/*.png from `adb exec-out screencap -p`) and writes
docs/screenshots/<lang>/<name>.jpg at phone width.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent


def main(raw: str):
    for lang in ("en", "ru"):
        src = Path(raw) / f"screens-{lang}"
        if not src.exists():
            src = Path(raw) / lang
        out = ROOT / f"docs/screenshots/{lang}"
        out.mkdir(parents=True, exist_ok=True)
        for f in sorted(src.glob("*.png")):
            img = Image.open(f).convert("RGB")
            if img.width > 1080:
                img = img.resize((1080, round(img.height * 1080 / img.width)), Image.LANCZOS)
            img.save(out / f"{f.stem}.jpg", quality=86, optimize=True)


if __name__ == "__main__":
    main(sys.argv[1])
