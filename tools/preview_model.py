"""Offscreen previewer for Minecraft-style box models.

Renders textured boxes so geometry and UV layout can be judged without
launching the game.

Model JSON:
{
  "texture": "atlas.png",
  "textureSize": [256, 256],
  "parts": [
    {"name": "head", "pivot": [0,-2,0], "rotation": [0,0,0],
     "cubes": [{"origin": [-3,-7,-3], "size": [6,5,6], "uv": [0,0], "inflate": 0.0}]}
  ]
}

Model space matches Minecraft: +X right, +Y down, +Z toward the viewer, origin
at the feet. Screen space is +X right, +Y down, so only Y needs negating when
projecting. Cube origins are absolute; the pivot is only the rotation centre.
"""

import json
import math
import sys

from PIL import Image

FACE_SHADE = {
    "up": 1.10,
    "down": 0.60,
    "east": 0.95,
    "west": 0.75,
    "north": 1.00,
    "south": 0.85,
}


def mat_identity():
    return [[1.0 if r == c else 0.0 for c in range(4)] for r in range(4)]


def mat_mul(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(4)) for c in range(4)] for r in range(4)]


def mat_translate(x, y, z):
    m = mat_identity()
    m[0][3], m[1][3], m[2][3] = x, y, z
    return m


def mat_rot(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = mat_identity()
    if axis == "x":
        m[1][1], m[1][2], m[2][1], m[2][2] = c, -s, s, c
    elif axis == "y":
        m[0][0], m[0][2], m[2][0], m[2][2] = c, s, -s, c
    else:
        m[0][0], m[0][1], m[1][0], m[1][1] = c, -s, s, c
    return m


def xform(m, p):
    x, y, z = p
    return (
        m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
        m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
        m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3],
    )


def cube_faces(origin, size, inflate):
    x, y, z = origin
    w, h, d = size
    x0, y0, z0 = x - inflate, y - inflate, z - inflate
    x1, y1, z1 = x + w + inflate, y + h + inflate, z + d + inflate
    # Corner order is (top-left, top-right, bottom-right, bottom-left) as seen
    # from outside, matching face_uvs(). +Y is down, +Z toward the viewer.
    return {
        "up": [(x0, y0, z0), (x0, y0, z1), (x1, y0, z1), (x1, y0, z0)],
        "down": [(x0, y1, z1), (x0, y1, z0), (x1, y1, z0), (x1, y1, z1)],
        "north": [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
        "south": [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)],
        "east": [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)],
        "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
    }


def uv_rect(uv, size, face):
    u, v = uv
    w, h, d = size
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }[face]


def face_uvs(uv, size, face):
    u, v, w, h = uv_rect(uv, size, face)
    return [(u, v), (u + w, v), (u + w, v + h), (u, v + h)]


# Outward normals in model space (+Y down, +Z toward the viewer).
FACE_NORMAL = {
    "up": (0.0, -1.0, 0.0),
    "down": (0.0, 1.0, 0.0),
    "north": (0.0, 0.0, -1.0),
    "south": (0.0, 0.0, 1.0),
    "east": (1.0, 0.0, 0.0),
    "west": (-1.0, 0.0, 0.0),
}


def xform_dir(m, v):
    """Rotate a direction vector (no translation)."""
    x, y, z = v
    return (
        m[0][0] * x + m[0][1] * y + m[0][2] * z,
        m[1][0] * x + m[1][1] * y + m[1][2] * z,
        m[2][0] * x + m[2][1] * y + m[2][2] * z,
    )

def face_normal(face, world):
    """Outward normal of a face in view space, from its world-space corners."""
    # Corners are ordered counter-clockwise when seen from outside.
    a, b, c = world[0], world[1], world[2]
    u = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
    v = (c[0] - a[0], c[1] - a[1], c[2] - a[2])
    n = (
        u[1] * v[2] - u[2] * v[1],
        u[2] * v[0] - u[0] * v[2],
        u[0] * v[1] - u[1] * v[0],
    )
    length = math.sqrt(n[0] ** 2 + n[1] ** 2 + n[2] ** 2)
    if length < 1e-9:
        return (0.0, 0.0, 0.0)
    return (n[0] / length, n[1] / length, n[2] / length)

def part_matrix(part):
    px, py, pz = part.get("pivot", [0, 0, 0])
    rx, ry, rz = part.get("rotation", [0, 0, 0])
    rot = mat_mul(mat_mul(mat_rot("z", rz), mat_rot("y", ry)), mat_rot("x", rx))
    return mat_mul(mat_mul(mat_translate(px, py, pz), rot), mat_translate(-px, -py, -pz))


def gather(model, scale, yaw, pitch, canvas_size):
    """Project every visible face; returns polygons sorted back-to-front."""
    view = mat_mul(mat_rot("x", pitch), mat_rot("y", yaw))
    out = []
    for part in model["parts"]:
        pm = part_matrix(part)
        for cube in part["cubes"]:
            faces = cube_faces(cube["origin"], cube["size"], cube.get("inflate", 0.0))
            for face, corners in faces.items():
                world = [xform(pm, c) for c in corners]
                world_view = [xform(view, p) for p in world]
                view_pts = world_view
                # Screen: Minecraft +Y is down and screen +Y is down, so no flip.
                screen = [(p[0] * scale, p[1] * scale) for p in view_pts]
                depth = sum(p[2] for p in view_pts) / 4.0

                # Face normals are fixed by the face name; this avoids relying
                # on the corner winding being consistent.
                n = xform_dir(view, FACE_NORMAL[face])
                # Cull only faces pointing away from the camera. With a
                # z-buffer, keeping the rest is harmless and avoids holes.
                if n[2] <= -1e-6 and False:
                    continue

                out.append({
                    "depth": depth,
                    "face": face,
                    "uv": cube["uv"],
                    "size": tuple(cube["size"]),
                    "screen": screen,
                    "depths": [p[2] for p in view_pts],
                })
    out.sort(key=lambda p: p["depth"])
    return out


def fit_offset(polys, canvas_size):
    if not polys:
        return (0.0, 0.0)
    xs = [p[0] for poly in polys for p in poly["screen"]]
    ys = [p[1] for poly in polys for p in poly["screen"]]
    return (
        canvas_size[0] / 2.0 - (min(xs) + max(xs)) / 2.0,
        canvas_size[1] / 2.0 - (min(ys) + max(ys)) / 2.0,
    )


def raster(canvas, tex, screen, uvs, depths, shade, zbuf):
    xs = [p[0] for p in screen]
    ys = [p[1] for p in screen]
    min_x = max(0, int(math.floor(min(xs))))
    max_x = min(canvas.width - 1, int(math.ceil(max(xs))))
    min_y = max(0, int(math.floor(min(ys))))
    max_y = min(canvas.height - 1, int(math.ceil(max(ys))))
    if max_x < min_x or max_y < min_y:
        return

    for tri in ((0, 1, 2), (0, 2, 3)):
        p0, p1, p2 = (screen[i] for i in tri)
        t0, t1, t2 = (uvs[i] for i in tri)
        d0, d1, d2 = (depths[i] for i in tri)
        denom = (p1[1] - p2[1]) * (p0[0] - p2[0]) + (p2[0] - p1[0]) * (p0[1] - p2[1])
        if abs(denom) < 1e-9:
            continue
        lo_x = max(min_x, int(math.floor(min(p0[0], p1[0], p2[0]))))
        hi_x = min(max_x, int(math.ceil(max(p0[0], p1[0], p2[0]))))
        lo_y = max(min_y, int(math.floor(min(p0[1], p1[1], p2[1]))))
        hi_y = min(max_y, int(math.ceil(max(p0[1], p1[1], p2[1]))))
        for y in range(lo_y, hi_y + 1):
            py = y + 0.5
            for x in range(lo_x, hi_x + 1):
                px = x + 0.5
                w0 = ((p1[1] - p2[1]) * (px - p2[0]) + (p2[0] - p1[0]) * (py - p2[1])) / denom
                w1 = ((p2[1] - p0[1]) * (px - p2[0]) + (p0[0] - p2[0]) * (py - p2[1])) / denom
                w2 = 1.0 - w0 - w1
                if w0 < -0.002 or w1 < -0.002 or w2 < -0.002:
                    continue
                u = w0 * t0[0] + w1 * t1[0] + w2 * t2[0]
                v = w0 * t0[1] + w1 * t1[1] + w2 * t2[1]
                tx = min(tex.width - 1, max(0, int(u)))
                ty = min(tex.height - 1, max(0, int(v)))
                r, g, b, a = tex.getpixel((tx, ty))
                if a == 0:
                    continue
                depth = w0 * d0 + w1 * d1 + w2 * d2
                if depth <= zbuf[y * canvas.width + x]:
                    continue
                zbuf[y * canvas.width + x] = depth
                canvas.putpixel(
                    (x, y),
                    (min(255, int(r * shade)), min(255, int(g * shade)),
                     min(255, int(b * shade)), a),
                )


def render(model, out_path, scale=12, yaw=0.0, pitch=-6.0, canvas_size=(420, 640)):
    tex = Image.open(model["texture"]).convert("RGBA")
    target = tuple(model.get("textureSize", tex.size))
    if tex.size != target:
        tex = tex.resize(target, Image.NEAREST)

    canvas = Image.new("RGBA", canvas_size, (26, 27, 32, 255))
    polys = gather(model, scale, yaw, pitch, canvas_size)
    dx, dy = fit_offset(polys, canvas_size)
    zbuf = [-1e9] * (canvas.width * canvas.height)
    for poly in polys:
        screen = [(p[0] + dx, p[1] + dy) for p in poly["screen"]]
        raster(canvas, tex, screen,
               face_uvs(poly["uv"], poly["size"], poly["face"]),
               poly["depths"], FACE_SHADE[poly["face"]], zbuf)
    canvas.save(out_path)
    return out_path


def main():
    if len(sys.argv) < 3:
        sys.stderr.write("usage: preview_model.py <model.json> <out.png> [scale] [yaw] [pitch]\n")
        return 2
    model = json.load(open(sys.argv[1], encoding="utf-8"))
    scale = int(sys.argv[3]) if len(sys.argv) > 3 else 12
    yaw = float(sys.argv[4]) if len(sys.argv) > 4 else 0.0
    pitch = float(sys.argv[5]) if len(sys.argv) > 5 else -6.0
    print("wrote", render(model, sys.argv[2], scale, yaw, pitch))
    return 0


if __name__ == "__main__":
    sys.exit(main())
