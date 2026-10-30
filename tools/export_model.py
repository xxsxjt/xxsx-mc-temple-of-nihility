"""Export NihilityCreatureModel geometry to the previewer's JSON format."""

import json
import os
import re

SRC = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "java", "com", "templenihility", "client", "model",
    "NihilityCreatureModel.java",
)

PART_RE = re.compile(
    r'root\.addOrReplaceChild\(\s*"([a-z_]+)",\s*CubeListBuilder\.create\(\)\s*'
    r'((?:\.texOffs\(\d+,\s*\d+\)[^;]*?)+)\s*,\s*PartPose\.(offset\(([^)]*)\)|ZERO)\s*\)',
    re.S,
)
CUBE_RE = re.compile(
    r'\.texOffs\((\d+),\s*(\d+)\)\.addBox\(([^)]*)\)',
    re.S,
)


def parse_floats(text):
    out = []
    for token in text.split(","):
        token = token.strip().rstrip("f")
        if not token:
            continue
        try:
            out.append(float(token))
        except ValueError:
            # Named arguments such as `fluid` or `new CubeDeformation(0.04f)`.
            inner = re.search(r"CubeDeformation\(\s*([-\d.]+)f?\s*\)", token)
            if inner:
                out.append(float(inner.group(1)))
    return out


def main():
    src = open(SRC, encoding="utf-8").read()
    parts = []
    for m in PART_RE.finditer(src):
        name = m.group(1)
        cubes_src = m.group(2)
        pivot = [0.0, 0.0, 0.0] if m.group(3) == "ZERO" else parse_floats(m.group(4))
        cubes = []
        for c in CUBE_RE.finditer(cubes_src):
            u, v = int(c.group(1)), int(c.group(2))
            nums = parse_floats(c.group(3))
            origin = nums[0:3]
            size = nums[3:6]
            inflate = nums[6] if len(nums) > 6 else 0.0
            cubes.append({
                "origin": origin,
                "size": size,
                "uv": [u, v],
                "inflate": inflate,
            })
        if cubes:
            parts.append({"name": name, "pivot": pivot, "rotation": [0, 0, 0], "cubes": cubes})

    model = {
        "texture": os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "src", "main", "resources", "assets", "templenihility",
            "textures", "entity", "nihility_believer.png",
        ),
        "textureSize": [128, 128],
        "parts": parts,
    }
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "creature-model.json")
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("parts:", len(parts), "->", out)


if __name__ == "__main__":
    main()
