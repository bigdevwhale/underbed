"""Writes the launcher icon vectors: the edge of a bedspread and two yellow eyes in the dark under it.

    python tools/icon.py
then `./gradlew testDebugUnitTest -Prender --tests '*RenderArtIcon*'` draws docs/logo.png from them.
"""
from pathlib import Path

DRAW = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"
HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n')


def oval(cx, cy, rx, ry):
    return f"M{cx - rx:.2f},{cy:.2f}A{rx},{ry} 0,1 1,{cx + rx:.2f},{cy:.2f}A{rx},{ry} 0,1 1,{cx - rx:.2f},{cy:.2f}Z"


def main():
    body = []
    # The bedspread hanging down, scalloped, with its fringe.
    d = "M0,0H108V36"
    x = 108.0
    while x > 0:
        d += f"Q{x - 6:.1f},40.5 {x - 12:.1f},36"
        x -= 12
    body.append(("#FFEFE6D2", d + "Z"))
    fringe = "".join(f"M{x:.1f},37.5L{x + .4:.1f},43" for x in [i * 3.4 + 1.5 for i in range(32)])
    body.append(("stroke:#FFEFE6D2:1.1", fringe))
    body.append(("#FF4A4038", "M0,44h108v4h-108z"))
    # Two eyes with their glow.
    for cx in (42, 66):
        body.append(("#33F2C14E", oval(cx, 66, 11, 11)))
        body.append(("#FFF2C14E", oval(cx, 66, 5.4, 6.4)))
        body.append(("#FF0D0B0A", oval(cx, 66, 1.2, 5)))
    out = []
    for c, p in body:
        if c.startswith("stroke:"):
            _, col, w = c.split(":")
            out.append(f'    <path android:strokeColor="{col}" android:strokeWidth="{w}" android:pathData="{p}" />')
        else:
            out.append(f'    <path android:fillColor="{c}" android:pathData="{p}" />')
    (DRAW / "ic_launcher_foreground.xml").write_text(HEAD + "\n".join(out) + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(HEAD + '    <path android:fillColor="#FF0D0B0A" android:pathData="M0,0h108v108h-108z" />\n</vector>\n', encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
