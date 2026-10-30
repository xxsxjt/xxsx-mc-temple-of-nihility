"""Generate 16x16 spawn egg textures for the five Nihility creature tiers."""

import os
from PIL import Image

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "src", "main", "resources", "assets", "templenihility", "textures", "item",
)

# tier: (base, shade, deep shade, speckle, highlight)
PALETTES = {
    "nihility_believer": ("#2f3646", "#232935", "#171b23", "#59627a", "#98a1b4"),
    "nihility_follower": ("#374055", "#2a3143", "#1c212c", "#6b7590", "#a9b2c4"),
    "nihility_envoy": ("#414f6b", "#323d53", "#222a39", "#7d8dab", "#bcc9dc"),
    "nihility_grand_envoy": ("#4d6488", "#3a4c68", "#28354a", "#93a8c6", "#d2dded"),
    "nihility_saint": ("#5d77a0", "#475a78", "#32405a", "#a7bfdc", "#e8f2ff"),
}

SHAPE = [
    "................",
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "....########....",
    ".....######.....",
    "................",
]

# Each tier gets a distinct speckle layout so the eggs are told apart at a glance.
SPECKLES = {
    "nihility_believer": [[(6, 5), (7, 5)], [(9, 8)], [(5, 9)], [(8, 11), (9, 11)]],
    "nihility_follower": [[(5, 6), (6, 6)], [(9, 5)], [(7, 8)], [(10, 9)], [(6, 11)]],
    "nihility_envoy": [[(7, 4), (8, 4)], [(5, 7)], [(10, 7)], [(6, 10)], [(9, 11), (10, 11)]],
    "nihility_grand_envoy": [[(5, 5)], [(9, 4), (10, 4)], [(4, 8)], [(8, 8)], [(11, 9)], [(7, 11)]],
    "nihility_saint": [[(7, 3)], [(6, 6), (7, 6)], [(10, 6)], [(5, 9)], [(9, 10)], [(8, 12)]],
}


def hex_to_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def build(name, base, shade, deep, speckle, highlight):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    base_rgb, shade_rgb, deep_rgb = hex_to_rgb(base), hex_to_rgb(shade), hex_to_rgb(deep)
    speckle_rgb, hi_rgb = hex_to_rgb(speckle), hex_to_rgb(highlight)

    for y, row in enumerate(SHAPE):
        for x, ch in enumerate(row):
            if ch == "#":
                px[x, y] = (*base_rgb, 255)

    # Rounding shade on the right half, deepening toward the bottom.
    for y, row in enumerate(SHAPE):
        for x, ch in enumerate(row):
            if ch != "#":
                continue
            if x >= 10:
                px[x, y] = (*shade_rgb, 255)
            if x >= 11 and y >= 8:
                px[x, y] = (*deep_rgb, 255)
            if y >= 11 and x >= 9:
                px[x, y] = (*shade_rgb, 255)

    for cluster in SPECKLES[name]:
        for (x, y) in cluster:
            if px[x, y][3] != 0:
                px[x, y] = (*speckle_rgb, 255)

    # Small diagonal specular glint, upper left.
    for (x, y) in [(6, 3), (7, 3), (5, 4)]:
        if px[x, y][3] != 0:
            px[x, y] = (*hi_rgb, 255)

    img.save(os.path.join(OUT_DIR, name + "_spawn_egg.png"))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, palette in PALETTES.items():
        build(name, *palette)
        print("wrote", name + "_spawn_egg.png")


if __name__ == "__main__":
    main()
