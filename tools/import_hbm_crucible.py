"""Imports HBM's Crucible (the "Ep"-era endgame sword, 裁决剑) out of the NTM jar.

The source is one OBJ with four objects - GuardRight, Blade, GuardLeft, Hilt - authored in the same
units HBM rendered it in (y -1.63 .. 11.38). NTM binds the three textures in code rather than through
the OBJ, so this script adds the missing `usemtl` lines and an MTL with one material per texture:

    Hilt                 -> crucible_hilt.png   (pommel, grip, crossguard bar)
    GuardLeft/GuardRight -> crucible_guard.png  (the two folding plates)
    Blade                -> crucible_blade.png  (the glowing red blade)

The inventory icon is HBM's 16x16 item texture; the item itself is drawn from the mesh.

Usage: python tools/import_hbm_crucible.py [path-to-NTM.jar]
"""

import os
import shutil
import sys
import zipfile

JAR = sys.argv[1] if len(sys.argv) > 1 else \
    r"G:\1\1\.minecraft\versions\1.7.10-Forge_10.13.4.1614\mods\HBM-NTM-.1.0.27_X5778_H261.jar"
ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "gfbs_doors")
OUT_MODELS = os.path.join(ROOT, "models", "weapons")
OUT_TEXTURES = os.path.join(OUT_MODELS, "tex")
OUT_ICONS = os.path.join(ROOT, "textures", "item")

SOURCE_OBJ = "assets/hbm/models/weapons/crucible.obj"

# Object -> material, in the order they appear in the source file.
GROUPS = {
    "GuardRight": "guard",
    "Blade": "blade",
    "GuardLeft": "guard",
    "Hilt": "hilt",
}

MATERIALS = {
    "hilt": "crucible_hilt.png",
    "guard": "crucible_guard.png",
    "blade": "crucible_blade.png",
}

ICON = "assets/hbm/textures/items/crucible.png"


def split_obj(text):
    """Splits the source into vertex lines and one face list per object."""
    vertices, objects, current = [], {}, None
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith(("v ", "vt ", "vn ")):
            vertices.append(stripped)
        elif stripped.startswith("o "):
            current = stripped.split(None, 1)[1].strip()
            objects[current] = []
        elif stripped.startswith("f ") and current is not None:
            objects[current].append(stripped)
    return vertices, objects


def write_obj(vertices, objects):
    os.makedirs(OUT_MODELS, exist_ok=True)
    os.makedirs(OUT_TEXTURES, exist_ok=True)

    lines = [
        "# HBM's Crucible, imported from assets/hbm/models/weapons/crucible.obj in the NTM jar.",
        "# Geometry is untouched; only the usemtl lines HBM supplied in code were added.",
        "mtllib crucible.mtl",
    ]
    for group, material in GROUPS.items():
        if group not in objects:
            raise SystemExit("missing object in the source OBJ: " + group)
        lines.append("o " + group)
        lines.append("usemtl " + material)
        lines.extend(objects[group])

    obj_path = os.path.join(OUT_MODELS, "crucible.obj")
    with open(obj_path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(vertices + lines) + "\n")

    mtl = []
    for material, texture in MATERIALS.items():
        mtl.extend([
            "newmtl " + material,
            "Ka 0.0 0.0 0.0",
            "Kd 1.0 1.0 1.0",
            "Ks 0.0 0.0 0.0",
            "d 1.0",
            "illum 1",
            "map_Kd tex/" + texture,
            "",
        ])
    with open(os.path.join(OUT_MODELS, "crucible.mtl"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(mtl))

    return obj_path


def main():
    os.makedirs(OUT_MODELS, exist_ok=True)
    os.makedirs(OUT_TEXTURES, exist_ok=True)
    os.makedirs(OUT_ICONS, exist_ok=True)
    with zipfile.ZipFile(JAR) as jar:
        vertices, objects = split_obj(jar.read(SOURCE_OBJ).decode("utf-8"))
        for texture in MATERIALS.values():
            target = os.path.join(OUT_TEXTURES, texture)
            with jar.open("assets/hbm/textures/models/weapons/" + texture) as src, \
                    open(target, "wb") as dst:
                shutil.copyfileobj(src, dst)
        os.makedirs(OUT_ICONS, exist_ok=True)
        with jar.open(ICON) as src, open(os.path.join(OUT_ICONS, "crucible.png"), "wb") as dst:
            shutil.copyfileobj(src, dst)

    obj_path = write_obj(vertices, objects)
    total_faces = sum(len(faces) for faces in objects.values())
    print("wrote", obj_path, "(%d vertices, %d faces)" % (len(vertices), total_faces))
    for group in GROUPS:
        print("  %-11s %4d faces" % (group, len(objects[group])))


if __name__ == "__main__":
    main()
