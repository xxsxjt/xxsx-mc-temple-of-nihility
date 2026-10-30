"""Creature model geometry, generated from a parametric skeleton.

Building the boxes from shared anchor values keeps neighbouring parts flush, so
the silhouette reads as one body instead of scattered blocks. All coordinates
are absolute model space with +Y down and the origin at the feet.

Skeleton (y):
    0  crown spikes
    3  top of skull
    9  jaw bottom / neck top
    9..15  chest
    15..20 waist
    20..23 hips
    23..33 thigh
    33..38 shin
    38..40 foot
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model_layout import ATLAS, BOXES  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "creature-model.json")
TEX = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "assets", "templenihility",
    "textures", "entity", "nihility_saint.png",
)

# Shared anchors.
HEAD_TOP = 3.0
HEAD_BOT = 9.0
NECK = 9.0
CHEST_BOT = 15.0
WAIST_BOT = 20.0
HIP_BOT = 23.0
THIGH_BOT = 33.0
SHIN_BOT = 38.0
FOOT_BOT = 40.0

HALF_CHEST = 4.0
HALF_WAIST = 2.8
HALF_HIP = 3.4
ARM_X = 5.2          # distance from centre to the inner face of the arm
ARM_W = 3.2
LEG_X = 1.8          # centre of each leg
LEG_W = 3.2


def box(x0, y0, z0, w, h, d):
    return [x0, y0, z0], [w, h, d]


PARTS = {
    "head": ((0, HEAD_TOP, 0), {
        "skull": box(-3.0, HEAD_TOP, -3.0, 6, 4.4, 6),
        "jaw": box(-2.6, HEAD_TOP + 4.4, -2.8, 5.2, 1.6, 5.6),
        "mask": box(-2.4, HEAD_TOP + 0.6, -3.5, 4.8, 3.4, 1.0),
    }),
    "head_sheen": ((0, HEAD_TOP, 0), {
        "sheen": box(-3.3, HEAD_TOP - 0.3, -3.3, 6.6, 5.0, 6.6),
    }),
    "body": ((0, CHEST_BOT, 0), {
        "chest": box(-HALF_CHEST, NECK, -2.2, HALF_CHEST * 2, CHEST_BOT - NECK, 4.4),
        "waist": box(-HALF_WAIST, CHEST_BOT, -1.7, HALF_WAIST * 2, WAIST_BOT - CHEST_BOT, 3.4),
        "hips": box(-HALF_HIP, WAIST_BOT, -1.8, HALF_HIP * 2, HIP_BOT - WAIST_BOT, 3.6),
        "tabard": box(-HALF_WAIST, CHEST_BOT, 1.7, HALF_WAIST * 2, 12.0, 1.0),
    }),
    "right_arm": ((-ARM_X, NECK + 1.0, 0), {
        "upper": box(-ARM_X - ARM_W, NECK + 1.0, -1.7, ARM_W, 5.0, 3.4),
        "fore": box(-ARM_X - ARM_W + 0.2, NECK + 6.0, -1.5, ARM_W - 0.2, 5.0, 3.0),
        "hand": box(-ARM_X - ARM_W + 0.4, NECK + 11.0, -1.2, 2.4, 2.6, 2.4),
    }),
    "left_arm": ((ARM_X, NECK + 1.0, 0), {
        "upper": box(ARM_X, NECK + 1.0, -1.7, ARM_W, 5.0, 3.4),
        "fore": box(ARM_X, NECK + 6.0, -1.5, ARM_W - 0.2, 5.0, 3.0),
        "hand": box(ARM_X + 0.4, NECK + 11.0, -1.2, 2.4, 2.6, 2.4),
    }),
    "right_leg": ((-LEG_X, HIP_BOT, 0), {
        "thigh": box(-LEG_X - LEG_W / 2, HIP_BOT, -1.7, LEG_W, THIGH_BOT - HIP_BOT, 3.4),
        "shin": box(-LEG_X - LEG_W / 2 + 0.1, THIGH_BOT, -1.5, LEG_W - 0.2, SHIN_BOT - THIGH_BOT, 3.0),
        "foot": box(-LEG_X - LEG_W / 2, SHIN_BOT, -2.4, LEG_W, FOOT_BOT - SHIN_BOT, 4.8),
    }),
    "left_leg": ((LEG_X, HIP_BOT, 0), {
        "thigh": box(LEG_X - LEG_W / 2, HIP_BOT, -1.7, LEG_W, THIGH_BOT - HIP_BOT, 3.4),
        "shin": box(LEG_X - LEG_W / 2 + 0.1, THIGH_BOT, -1.5, LEG_W - 0.2, SHIN_BOT - THIGH_BOT, 3.0),
        "foot": box(LEG_X - LEG_W / 2, SHIN_BOT, -2.4, LEG_W, FOOT_BOT - SHIN_BOT, 4.8),
    }),
    "left_shoulder": ((ARM_X, NECK, 0), {
        "plate": box(ARM_X - 0.8, NECK - 0.6, -3.0, ARM_W + 1.6, 2.2, 6.0),
        "ridge": box(ARM_X - 0.2, NECK + 1.6, -2.4, ARM_W + 0.4, 1.8, 4.8),
    }),
    "right_shoulder": ((-ARM_X, NECK, 0), {
        "plate": box(-ARM_X - ARM_W - 0.8, NECK - 0.6, -3.0, ARM_W + 1.6, 2.2, 6.0),
        "ridge": box(-ARM_X - ARM_W - 0.2, NECK + 1.6, -2.4, ARM_W + 0.4, 1.8, 4.8),
    }),
    "left_flow": ((-ARM_X, NECK, 2.6), {
        "seg1": box(-ARM_X - 1.1, NECK, 2.2, 2.2, 5.0, 1.0),
        "seg2": box(-ARM_X - 1.0, NECK + 5.0, 2.25, 2.2, 5.0, 1.0),
        "seg3": box(-ARM_X - 0.9, NECK + 10.0, 2.3, 2.2, 5.0, 1.0),
    }),
    "right_flow": ((ARM_X, NECK, 2.6), {
        "seg1": box(ARM_X - 1.1, NECK, 2.2, 2.2, 5.0, 1.0),
        "seg2": box(ARM_X - 1.0, NECK + 5.0, 2.25, 2.2, 5.0, 1.0),
        "seg3": box(ARM_X - 0.9, NECK + 10.0, 2.3, 2.2, 5.0, 1.0),
    }),
    "lower_flow_left": ((-LEG_X, HIP_BOT, 2.2), {
        "seg1": box(-LEG_X - 1.0, HIP_BOT, 1.8, 2.0, 4.4, 1.0),
        "seg2": box(-LEG_X - 0.9, HIP_BOT + 4.4, 1.85, 2.0, 4.4, 1.0),
        "seg3": box(-LEG_X - 0.8, HIP_BOT + 8.8, 1.9, 2.0, 4.4, 1.0),
    }),
    "lower_flow_right": ((LEG_X, HIP_BOT, 2.2), {
        "seg1": box(LEG_X - 1.0, HIP_BOT, 1.8, 2.0, 4.4, 1.0),
        "seg2": box(LEG_X - 0.9, HIP_BOT + 4.4, 1.85, 2.0, 4.4, 1.0),
        "seg3": box(LEG_X - 0.8, HIP_BOT + 8.8, 1.9, 2.0, 4.4, 1.0),
    }),
    "seal_left": ((-ARM_X - 2.4, CHEST_BOT, 1.2), {
        "body": box(-ARM_X - 2.9, CHEST_BOT - 1.5, 0.7, 1.2, 3.2, 1.2),
        "tip": box(-ARM_X - 2.9, CHEST_BOT + 1.7, 0.7, 1.2, 2.2, 1.2),
    }),
    "seal_right": ((ARM_X + 2.4, CHEST_BOT, 1.2), {
        "body": box(ARM_X + 1.7, CHEST_BOT - 1.5, 0.7, 1.2, 3.2, 1.2),
        "tip": box(ARM_X + 1.7, CHEST_BOT + 1.7, 0.7, 1.2, 2.2, 1.2),
    }),
    "crown_left": ((0, HEAD_TOP, 0), {
        "bar": box(-5.2, HEAD_TOP - 1.4, -0.6, 3.2, 1.2, 1.2),
        "horn": box(-6.0, HEAD_TOP - 3.4, -0.6, 1.2, 2.0, 1.2),
    }),
    "crown_right": ((0, HEAD_TOP, 0), {
        "bar": box(2.0, HEAD_TOP - 1.4, -0.6, 3.2, 1.2, 1.2),
        "horn": box(4.8, HEAD_TOP - 3.4, -0.6, 1.2, 2.0, 1.2),
    }),
    "crown_top": ((0, HEAD_TOP, 0), {
        "spike": box(-0.6, HEAD_TOP - 4.2, -0.6, 1.2, 2.8, 1.2),
    }),
}


def main():
    uv_by_key = {(part, label): uv for part, label, uv, _s in BOXES}
    parts = []
    for part_name, (pivot, cubes) in PARTS.items():
        entries = []
        for label, (origin, size) in cubes.items():
            entries.append({
                "origin": [round(v, 2) for v in origin],
                "size": [round(v, 2) for v in size],
                "uv": list(uv_by_key[(part_name, label)]),
                "inflate": 0.0,
            })
        parts.append({
            "name": part_name,
            "pivot": list(pivot),
            "rotation": [0, 0, 0],
            "cubes": entries,
        })

    model = {"texture": TEX, "textureSize": [ATLAS, ATLAS], "parts": parts}
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("parts:", len(parts), "cubes:", sum(len(p["cubes"]) for p in parts))


if __name__ == "__main__":
    main()
