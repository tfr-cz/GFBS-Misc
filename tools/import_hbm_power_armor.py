"""Imports HBM's T-51b power armour out of the NTM jar into this mod's glTF assets.

The source model is a single OBJ with per-biped-part objects (Helmet, Chest, LeftArm, RightArm,
LeftLeg, RightLeg, LeftBoot, RightBoot) authored in vanilla biped pixels: x -10.23..10.23,
y -8.65..24.24, z -6.21..5.31. That is exactly the space HazmatArmorModel/our glTF renderer work in,
so no vertex rewriting is needed - the model is split into one OBJ per armour slot and the renderer
applies the matching biped part transform per object.

Textures come from assets/hbm/textures/armor/t51_{helmet,chest,arm,leg}.png; HBM binds them per part
(chest for the torso, arm for both arms), which the slot MTLs reproduce with two materials.

Usage: python tools/import_hbm_power_armor.py [path-to-NTM.jar]
"""

import os
import sys
import zipfile

from PIL import Image, ImageDraw

JAR = sys.argv[1] if len(sys.argv) > 1 else \
    r"G:\1\1\.minecraft\versions\1.7.10-Forge_10.13.4.1614\mods\HBM-NTM-.1.0.27_X5778_H261.jar"
ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "gfbs_doors")
OUT_MODELS = os.path.join(ROOT, "models", "armor", "power")
OUT_ICONS = os.path.join(ROOT, "textures", "item")

SOURCE_OBJ = "assets/hbm/models/armor/t51.obj"

SLOTS = {
    "helmet": {
        "groups": {"Helmet": "power_helmet"},
        "materials": {"power_helmet": "t51_helmet.png"},
    },
    "chest": {
        "groups": {"Chest": "power_chest", "LeftArm": "power_arm", "RightArm": "power_arm"},
        "materials": {"power_chest": "t51_chest.png", "power_arm": "t51_arm.png"},
    },
    "legs": {
        "groups": {"LeftLeg": "power_leg", "RightLeg": "power_leg"},
        "materials": {"power_leg": "t51_leg.png"},
    },
    "boots": {
        "groups": {"LeftBoot": "power_leg", "RightBoot": "power_leg"},
        "materials": {"power_leg": "t51_leg.png"},
    },
}


def read_obj(text):
    """Splits the OBJ into its raw vertex-like lines and its objects (name -> face lines)."""
    vertices, objects, current = [], {}, None
    for line in text.splitlines():
        if line.startswith(("v ", "vt ", "vn ")):
            vertices.append(line)
        elif line.startswith("o "):
            current = line.split(None, 1)[1].strip()
            objects[current] = []
        elif line.startswith("f ") and current is not None:
            objects[current].append(line)
    return vertices, objects


def write_slot(name, vertices, objects, definition, textures):
    obj_path = os.path.join(OUT_MODELS, name + ".obj")
    mtl_path = os.path.join(OUT_MODELS, name + ".mtl")
    lines = [
        "# HBM T-51b power armour, split from assets/hbm/models/armor/t51.obj in the NTM jar.",
        "# Vertices stay in vanilla biped pixels; PowerArmorLayer supplies the per-part transform.",
        "mtllib " + name + ".mtl",
    ]
    for group, material in definition["groups"].items():
        lines.append("o " + group)
        lines.append("usemtl " + material)
        lines.extend(objects[group])
    with open(obj_path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(vertices + lines) + "\n")

    mtl = []
    for material, texture in definition["materials"].items():
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
    with open(mtl_path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(mtl))

    return obj_path, mtl_path


def dominant_colours(image, count=4):
    pixels = [pixel for pixel in image.convert("RGBA").getdata() if pixel[3] > 32]
    if not pixels:
        return [(128, 128, 128, 255)] * count
    buckets = {}
    for red, green, blue, _alpha in pixels:
        key = (red // 32, green // 32, blue // 32)
        bucket = buckets.setdefault(key, [0, 0, 0, 0])
        bucket[0] += red
        bucket[1] += green
        bucket[2] += blue
        bucket[3] += 1
    ordered = sorted(buckets.values(), key=lambda bucket: -bucket[3])[:count]
    return [(b[0] // b[3], b[1] // b[3], b[2] // b[3], 255) for b in ordered]


def write_icon(name, textures):
    """16x16 inventory icon from the set's own palette, so the item matches the suit it puts on."""
    colours = []
    for texture in textures:
        colours.extend(dominant_colours(Image.open(texture)))
    main = colours[0]
    dark = colours[-1]
    mid = colours[len(colours) // 2]
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    if name == "helmet":
        d.rectangle((3, 2, 12, 12), fill=main)
        d.rectangle((3, 2, 12, 4), fill=dark)
        d.rectangle((5, 5, 10, 9), fill=mid)
        d.rectangle((7, 1, 8, 1), fill=dark)
    elif name == "chest":
        d.rectangle((3, 3, 12, 13), fill=main)
        d.rectangle((0, 3, 3, 7), fill=mid)
        d.rectangle((12, 3, 15, 7), fill=mid)
        d.rectangle((5, 5, 10, 9), fill=dark)
        d.rectangle((3, 12, 12, 13), fill=dark)
    elif name == "legs":
        d.rectangle((3, 1, 12, 3), fill=dark)
        d.rectangle((3, 3, 6, 13), fill=main)
        d.rectangle((9, 3, 12, 13), fill=main)
        d.rectangle((4, 8, 6, 10), fill=mid)
        d.rectangle((9, 8, 11, 10), fill=mid)
    else:
        d.rectangle((3, 4, 6, 11), fill=main)
        d.rectangle((9, 4, 12, 11), fill=main)
        d.rectangle((3, 4, 6, 5), fill=mid)
        d.rectangle((9, 4, 12, 5), fill=mid)
        d.rectangle((2, 11, 7, 13), fill=dark)
        d.rectangle((8, 11, 13, 13), fill=dark)
    path = os.path.join(OUT_ICONS, "power_" + name + ".png")
    os.makedirs(OUT_ICONS, exist_ok=True)
    image.save(path)
    return path


def main():
    os.makedirs(os.path.join(OUT_MODELS, "tex"), exist_ok=True)
    with zipfile.ZipFile(JAR) as jar:
        vertices, objects = read_obj(jar.read(SOURCE_OBJ).decode("utf-8", "ignore"))
        print("source groups:", {name: len(faces) for name, faces in objects.items()})

        for slot, definition in SLOTS.items():
            copies = {}
            for texture in definition["materials"].values():
                target = os.path.join(OUT_MODELS, "tex", texture)
                if not os.path.exists(target):
                    with open(target, "wb") as handle:
                        handle.write(jar.read("assets/hbm/textures/armor/" + texture))
                copies[texture] = target
            obj_path, mtl_path = write_slot(slot, vertices, objects, definition, copies)
            print("wrote", os.path.basename(obj_path), "and", os.path.basename(mtl_path))

        icon_sources = {
            "helmet": ["t51_helmet.png"],
            "chest": ["t51_chest.png", "t51_arm.png"],
            "legs": ["t51_leg.png"],
            "boots": ["t51_leg.png"],
        }
        for slot, _definition in SLOTS.items():
            paths = [os.path.join(OUT_MODELS, "tex", texture) for texture in icon_sources[slot]]
            print("icon", write_icon(slot, paths))


if __name__ == "__main__":
    main()
