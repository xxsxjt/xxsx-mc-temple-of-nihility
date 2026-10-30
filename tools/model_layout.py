"""Single source of truth for the Nihility creature model layout.

Both the texture generator and the Java model are written against this table so
UV slots can never drift apart again.

Box layout: (name, uv, size) where size is (width, height, depth) in model px.
The atlas is 256x256 and every box reserves d+2w by d+h at its offset.
"""

# (part, cube label, uv, size)
BOXES = [
    ("head", "skull", (0, 0), (6, 5, 6)),
    ("head", "jaw", (0, 32), (5, 2, 6)),
    ("head", "mask", (40, 0), (5, 4, 1)),
    ("head_sheen", "sheen", (24, 0), (7, 6, 7)),
    ("body", "chest", (16, 24), (7, 5, 4)),
    ("body", "waist", (16, 48), (5, 7, 3)),
    ("body", "hips", (44, 48), (6, 3, 3)),
    ("body", "tabard", (48, 24), (5, 8, 1)),
    ("right_arm", "upper", (0, 64), (3, 4, 3)),
    ("right_arm", "fore", (16, 64), (3, 5, 3)),
    ("right_arm", "hand", (32, 64), (2, 3, 2)),
    ("left_arm", "upper", (48, 64), (3, 4, 3)),
    ("left_arm", "fore", (64, 64), (3, 5, 3)),
    ("left_arm", "hand", (80, 64), (2, 3, 2)),
    ("right_leg", "thigh", (0, 96), (3, 5, 3)),
    ("right_leg", "shin", (16, 96), (3, 5, 3)),
    ("right_leg", "foot", (32, 96), (3, 2, 4)),
    ("left_leg", "thigh", (48, 96), (3, 5, 3)),
    ("left_leg", "shin", (64, 96), (3, 5, 3)),
    ("left_leg", "foot", (80, 96), (3, 2, 4)),
    ("left_shoulder", "plate", (0, 128), (4, 2, 5)),
    ("left_shoulder", "ridge", (24, 128), (3, 2, 4)),
    ("right_shoulder", "plate", (48, 128), (4, 2, 5)),
    ("right_shoulder", "ridge", (72, 128), (3, 2, 4)),
    ("left_flow", "seg1", (0, 160), (2, 5, 1)),
    ("left_flow", "seg2", (16, 160), (2, 5, 1)),
    ("left_flow", "seg3", (32, 160), (2, 5, 1)),
    ("right_flow", "seg1", (48, 160), (2, 5, 1)),
    ("right_flow", "seg2", (64, 160), (2, 5, 1)),
    ("right_flow", "seg3", (80, 160), (2, 5, 1)),
    ("lower_flow_left", "seg1", (0, 192), (2, 4, 1)),
    ("lower_flow_left", "seg2", (16, 192), (2, 4, 1)),
    ("lower_flow_left", "seg3", (32, 192), (2, 4, 1)),
    ("lower_flow_right", "seg1", (48, 192), (2, 4, 1)),
    ("lower_flow_right", "seg2", (64, 192), (2, 4, 1)),
    ("lower_flow_right", "seg3", (80, 192), (2, 4, 1)),
    ("seal_left", "body", (0, 224), (1, 3, 1)),
    ("seal_left", "tip", (8, 224), (1, 2, 1)),
    ("seal_right", "body", (16, 224), (1, 3, 1)),
    ("seal_right", "tip", (24, 224), (1, 2, 1)),
    ("crown_left", "bar", (32, 224), (3, 1, 1)),
    ("crown_left", "horn", (48, 224), (1, 2, 1)),
    ("crown_right", "bar", (56, 224), (3, 1, 1)),
    ("crown_right", "horn", (72, 224), (1, 2, 1)),
    ("crown_top", "spike", (80, 224), (1, 3, 1)),
]

ATLAS = 256
