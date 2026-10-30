"""Pixel-art texture atlas for the Nihility creatures.

Style rules taken from docs/ASPECT_DESIGN.md: black/white/grey base, hard-edged
notches, erasable-looking borders, layered highlights. No soft gradients -- every
pixel is a deliberate colour from a small palette, the way hand-drawn MC art
works. UV slots come from tools/model_layout.py.
"""

import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model_layout import ATLAS, BOXES  # noqa: E402

OUT = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "assets", "templenihility",
    "textures", "entity",
)

# Ordered dark -> light. Five steps is enough to read as pixel art at this size.
PALETTES = {
    "nihility_believer": ["#0a0c11", "#141922", "#212936", "#333d4f", "#4c5970", "#6f7f99"],
    "nihility_follower": ["#0b0e14", "#161c27", "#242d3c", "#384458", "#54637c", "#78899f"],
    "nihility_envoy": ["#0d1118", "#1a2230", "#2b374a", "#43536b", "#637694", "#8ba0bd"],
    "nihility_grand_envoy": ["#0f141d", "#1e2738", "#324055", "#4d6079", "#7188a6", "#9db4d1"],
    "nihility_saint": ["#121822", "#232f42", "#3a4b63", "#5a708c", "#8098b8", "#b3c8e0"],
}


def rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def face_rects(uv, size):
    u, v = uv
    w, h, d = size
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (*color, 255))


def paint_face(img, rect, palette, style, rng, face):
    """Paint one face with hard-edged pixel art in the given style."""
    x0, y0, w, h = (int(round(v)) for v in rect)
    if w <= 0 or h <= 0:
        return

    # Base tone: top faces read lighter, bottom faces darker.
    base_idx = {"up": 4, "down": 1, "east": 3, "west": 2, "north": 3, "south": 2}[face]

    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            put(img, x, y, rgb(palette[base_idx]))

    if style == "cloth":
        # Vertical fold every 3px, with a darker seam and a lit ridge.
        for x in range(x0, x0 + w):
            if (x - x0) % 3 == 0:
                for y in range(y0, y0 + h):
                    put(img, x, y, rgb(palette[max(0, base_idx - 1)]))
            elif (x - x0) % 3 == 1:
                for y in range(y0, y0 + h):
                    put(img, x, y, rgb(palette[min(5, base_idx + 1)]))
        # Hem: darken the last row so the garment reads as ending.
        for x in range(x0, x0 + w):
            put(img, x, y0 + h - 1, rgb(palette[0]))

    elif style == "armor":
        # Plate borders and a single rivet per plate.
        for x in range(x0, x0 + w):
            put(img, x, y0, rgb(palette[min(5, base_idx + 2)]))
            put(img, x, y0 + h - 1, rgb(palette[0]))
        for y in range(y0, y0 + h):
            put(img, x0, y, rgb(palette[max(0, base_idx - 1)]))
            put(img, x0 + w - 1, y, rgb(palette[0]))
        if w >= 3 and h >= 3:
            cx, cy = x0 + w // 2, y0 + h // 2
            put(img, cx, cy, rgb(palette[5]))

    elif style == "skin":
        # Stipple with a few darker pores so the surface is not flat.
        for _ in range(max(1, (w * h) // 8)):
            px = x0 + rng.randrange(w)
            py = y0 + rng.randrange(h)
            put(img, px, py, rgb(palette[max(0, base_idx - 1)]))

    elif style == "void":
        # Diagonal streaks that read as flowing emptiness.
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if (x + y) % 4 == 0:
                    put(img, x, y, rgb(palette[min(5, base_idx + 1)]))
                elif (x + y) % 7 == 0:
                    put(img, x, y, rgb(palette[max(0, base_idx - 1)]))


def paint_mask(img, uv, size, palette):
    """Void mask on the head front: recessed plate, hard rim, two lit slits."""
    u, v = uv
    w, h, d = size
    x0, y0 = int(u + d), int(v + d)
    x1, y1 = x0 + int(w), y0 + int(h)

    # Recessed plate.
    for y in range(y0 + 1, y1 - 1):
        for x in range(x0 + 1, x1 - 1):
            put(img, x, y, rgb(palette[0]))

    # Hard rim: lit on top/left, shadowed on bottom/right.
    for x in range(x0 + 1, x1 - 1):
        put(img, x, y0, rgb(palette[5]))
        put(img, x, y1 - 1, rgb(palette[0]))
    for y in range(y0 + 1, y1 - 1):
        put(img, x0, y, rgb(palette[4]))
        put(img, x1 - 1, y, rgb(palette[1]))

    # Two eye slits, bright white-blue.
    eye_y = y0 + 2
    slit = max(2, (x1 - x0) // 4)
    for ex in (x0 + 2, x1 - slit - 2):
        for x in range(ex, min(ex + slit, x1 - 1)):
            put(img, x, eye_y, rgb(palette[5]))
            put(img, x, eye_y + 1, rgb(palette[4]))

    # Crack down the centre.
    cx = (x0 + x1) // 2
    for i in range(3, (y1 - y0) - 1):
        put(img, cx + (1 if i % 3 == 0 else 0), y0 + i, rgb(palette[0]))


STYLE = {
    "skull": "skin", "jaw": "skin", "sheen": "void", "mask": "armor",
    "chest": "cloth", "waist": "cloth", "hips": "cloth", "tabard": "cloth",
    "upper": "cloth", "fore": "cloth", "hand": "skin",
    "thigh": "cloth", "shin": "cloth", "foot": "armor",
    "plate": "armor", "ridge": "armor",
    "seg1": "void", "seg2": "void", "seg3": "void",
    "body": "armor", "tip": "armor",
    "bar": "armor", "horn": "armor", "spike": "armor",
}


def build(name, palette):
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    rng = random.Random(sum(ord(c) for c in name))

    for _part, label, uv, size in BOXES:
        style = STYLE.get(label, "cloth")
        for face, rect in face_rects(uv, size).items():
            paint_face(img, rect, palette, style, rng, face)

    head_uv = next(uv for part, label, uv, _s in BOXES if part == "head" and label == "skull")
    head_size = next(s for part, label, _u, s in BOXES if part == "head" and label == "skull")
    paint_mask(img, head_uv, head_size, palette)

    img.save(os.path.join(OUT, name + ".png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, palette in PALETTES.items():
        build(name, palette)
        print("wrote", name + ".png")


if __name__ == "__main__":
    main()
