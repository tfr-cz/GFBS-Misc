import json
import os
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NTM = Path(os.environ.get("NTM_ASSETS", r"G:/qoder/work/ntm/src/main/resources/assets/hbm"))
RES = ROOT / "src/main/resources/assets/gfbs_doors"
MODELS = RES / "models"
DOOR_MODELS = MODELS / "doors"
DOOR_TEX = DOOR_MODELS / "tex"
MACHINE_MODELS = MODELS / "machines"
MACHINE_TEX = MACHINE_MODELS / "tex"
ITEM_TEX = RES / "textures/item"
SOUNDS = RES / "sounds"
DATA = ROOT / "src/main/resources/data"

DOORS = [
    dict(id="transition_seal", model=None, skins=["transition_seal"], labels=[], parts=["frame"]),
    dict(id="vault_door", model="pheodoors/vault_door.obj",
         skins=["vault_door_3", "vault_door_3", "vault_door_3", "vault_door_4", "vault_door_4", "vault_door_s", "vault_door_s"],
         labels=["label_101", "label_87", "label_106", "label_81", "label_111", "label_2", "label_99"]),
    dict(id="sliding_blast_door", model="pheodoors/blast_door.obj",
         skins=["blast_door", "blast_door_industrial"], labels=[]),
    dict(id="fire_door", model="pheodoors/fire_door.obj",
         skins=["fire_door", "fire_door_black", "fire_door_orange", "fire_door_yellow", "fire_door_trefoil"], labels=[]),
    dict(id="silo_hatch", model="doors/silo_hatch.obj", skins=["silo_hatch"], labels=[]),
    dict(id="silo_hatch_large", model="doors/silo_hatch_large.obj", skins=["silo_hatch_large"], labels=[]),
    dict(id="secure_access_door", model="pheodoors/secure_door.obj",
         skins=["secure_door", "secure_door_grey", "secure_door_black", "secure_door_yellow"], labels=[]),
    dict(id="large_vehicle_door", model="pheodoors/vehicle_door.obj",
         skins=["vehicle_door", "vehicle_door_black", "vehicle_door_blue", "vehicle_door_red"], labels=[]),
    dict(id="qe_containment", model="pheodoors/containment_door.obj",
         skins=["containment_door", "containment_door_trefoil", "containment_door_trefoil_yellow"], labels=[]),
    dict(id="qe_sliding_door", model="pheodoors/sliding_door.obj",
         skins=["sliding_door", "sliding_door_black", "sliding_door_grid", "sliding_door_plate", "sliding_door_white"], labels=[]),
    dict(id="round_airlock_door", model="pheodoors/airlock_door.obj",
         skins=["airlock_door", "airlock_door_clean", "airlock_door_green"], labels=[]),
    dict(id="sliding_seal_door", model="pheodoors/seal_door.obj",
         skins=["seal_door", "seal_door_clean", "seal_door_trefoil"], labels=[]),
    dict(id="water_door", model="pheodoors/water_door.obj",
         skins=["water_door", "water_door_clean"], labels=[]),
    dict(id="cargo_door", model="pheodoors/cargo_door.obj", skins=["cargo_door"], labels=[]),
    dict(id="blast_door", model=None, skins=["blast_door_base"], labels=[]),
]

NAMES_EN = {
    "transition_seal": "Transition Seal",
    "vault_door": "Vault-Tec Blast Door",
    "sliding_blast_door": "Sliding Blast Door",
    "fire_door": "Fire Door",
    "silo_hatch": "Silo Hatch",
    "silo_hatch_large": "Large Silo Hatch",
    "secure_access_door": "Secure Access Door",
    "large_vehicle_door": "Large Vehicle Door",
    "qe_containment": "QE Containment Door",
    "qe_sliding_door": "QE Sliding Door",
    "round_airlock_door": "Round Airlock Door",
    "sliding_seal_door": "Sliding Seal Door",
    "water_door": "Water Door",
    "cargo_door": "Cargo Door",
    "blast_door": "Blast Door",
    "cargo_elevator": "Cargo Elevator",
}
NAMES_ZH = {
    "transition_seal": "巨型安全门",
    "vault_door": "避难所防爆门",
    "sliding_blast_door": "滑动防爆门",
    "fire_door": "防火门",
    "silo_hatch": "发射井舱门",
    "silo_hatch_large": "大型发射井舱门",
    "secure_access_door": "安全检修门",
    "large_vehicle_door": "大型机械门",
    "qe_containment": "QE安全门",
    "qe_sliding_door": "QE滑动门",
    "round_airlock_door": "圆形气闸门",
    "sliding_seal_door": "滑动密封门",
    "water_door": "水闸门",
    "cargo_door": "货运大门",
    "blast_door": "防爆闸门",
    "cargo_elevator": "货梯",
}

SOUND_FILES = {
    "door.transition_seal_open": ("door/transition_seal_open", "block/door/transition_seal_open.ogg", True),
    "door.alarm6": ("door/alarm6", "block/door/alarm6.ogg", True),
    "door.lever": ("door/lever", "block/door/lever1.ogg", False),
    "door.sliding_door_opening": ("door/sliding_door_opening", "block/door/sliding_door_opening.ogg", False),
    "door.sliding_door_opened": ("door/sliding_door_opened", "block/door/sliding_door_opened.ogg", False),
    "door.sliding_door_shut": ("door/sliding_door_shut", "block/door/sliding_door_shut.ogg", False),
    "door.sliding_seal_open": ("door/sliding_seal_open", "block/door/doormove2.ogg", False),
    "door.sliding_seal_stop": ("door/sliding_seal_stop", "block/door/metal_stop1.ogg", False),
    "door.garage_move": ("door/garage_move", "block/door/garage_move.ogg", False),
    "door.garage_stop": ("door/garage_stop", "block/door/garage_stop.ogg", False),
    "door.qe_sliding_opening": ("door/qe_sliding_opening", "block/door/doorslide_opening1.ogg", False),
    "door.qe_sliding_opened": ("door/qe_sliding_opened", "block/door/doorslide_opened1.ogg", False),
    "door.qe_sliding_shut": ("door/qe_sliding_shut", "block/door/doorshut_1.ogg", False),
    "door.wgh_start": ("door/wgh_start", "block/door/wgh_start.ogg", False),
    "door.wgh_stop": ("door/wgh_stop", "block/door/wgh_stop.ogg", False),
    "door.wgh_big_start": ("door/wgh_big_start", "block/door/door_wgh_big_start.ogg", False),
    "door.wgh_big_stop": ("door/wgh_big_stop", "block/door/door_wgh_big_stop.ogg", False),
    "block.reactor_start": ("block/reactor_start", "block/reactorStart.ogg", False),
    "block.reactor_stop": ("block/reactor_stop", "block/reactorStop.ogg", False),
    "block.vault_scrape_new": ("block/vault_scrape_new", "block/vaultScrapeNew.ogg", False),
    "block.vault_thud_new": ("block/vault_thud_new", "block/vaultThudNew.ogg", False),
}

RECIPES = {
    "transition_seal": (["IOI", "BRB", "IOI"], {"I": "minecraft:iron_ingot", "O": "minecraft:obsidian", "B": "minecraft:iron_block", "R": "minecraft:redstone_block"}, 1),
    "vault_door": (["IBI", "BRB", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "R": "minecraft:redstone_block"}, 1),
    "sliding_blast_door": (["III", "BRB", "III"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "R": "minecraft:redstone"}, 2),
    "fire_door": (["IRI", "III", "IRI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 2),
    "silo_hatch": (["III", "IRI", "III"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 1),
    "silo_hatch_large": (["IBI", "GRG", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "G": "minecraft:glass", "R": "minecraft:redstone"}, 1),
    "secure_access_door": (["IRI", "IBI", "IRI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "R": "minecraft:redstone"}, 2),
    "large_vehicle_door": (["IBI", "BRB", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "R": "minecraft:redstone"}, 1),
    "qe_containment": (["IRI", "GBG", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "B": "minecraft:iron_block", "R": "minecraft:redstone"}, 1),
    "qe_sliding_door": (["IRI", "III", "IRI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 2),
    "round_airlock_door": (["ICI", "GRG", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "G": "minecraft:glass", "R": "minecraft:redstone"}, 1),
    "sliding_seal_door": (["IBI", "IRI", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "R": "minecraft:redstone"}, 1),
    "water_door": (["III", "GRG", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "R": "minecraft:redstone"}, 2),
    "cargo_door": (["III", "BRB", "III"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_block", "R": "minecraft:redstone"}, 2),
    "cargo_elevator": (["III", "IPI", "III"], {"I": "minecraft:iron_ingot", "P": "minecraft:piston"}, 3),
    "blast_door": (["IOI", "IRI", "IOI"], {"I": "minecraft:iron_ingot", "O": "minecraft:obsidian", "R": "minecraft:redstone"}, 1),
}


def merge_legacy_blast_door():
    models = [
        ("Base", "blast_door_base.obj", "blast_door_base"),
        ("Block", "blast_door_block.obj", "blast_door_block"),
        ("Tooth", "blast_door_tooth.obj", "blast_door_tooth"),
        ("Slider", "blast_door_slider.obj", "blast_door_slider"),
    ]
    lines = ["mtllib blast_door_legacy.mtl"]
    positions = texcoords = normals = 0
    for group, filename, material in models:
        source = NTM / "models" / filename
        raw = source.read_text(encoding="utf-8", errors="ignore").splitlines()
        v_lines = [line for line in raw if line.strip().startswith("v ")]
        vt_lines = [line for line in raw if line.strip().startswith("vt ")]
        vn_lines = [line for line in raw if line.strip().startswith("vn ")]
        faces = [line for line in raw if line.strip().startswith("f ")]
        lines.extend(v_lines)
        lines.extend(vt_lines)
        lines.extend(vn_lines)
        lines.append(f"o {group}")
        lines.append(f"usemtl {material}")
        for face in faces:
            parts = face.split()
            converted = ["f"]
            for token in parts[1:]:
                bits = token.split("/")
                v = int(bits[0]); v = v + positions if v > 0 else positions + v + 1
                out = str(v)
                if len(bits) > 1 and bits[1]:
                    t = int(bits[1]); t = t + texcoords if t > 0 else texcoords + t + 1
                    out += "/" + str(t)
                elif len(bits) > 1:
                    out += "/"
                if len(bits) > 2 and bits[2]:
                    n = int(bits[2]); n = n + normals if n > 0 else normals + n + 1
                    out += "/" + str(n)
                converted.append(out)
            lines.append(" ".join(converted))
        positions += len(v_lines)
        texcoords += len(vt_lines)
        normals += len(vn_lines)
    dst = DOOR_MODELS / "blast_door_legacy.obj"
    dst.write_text("\n".join(lines) + "\n", encoding="utf-8")
    for _, _, texture in models:
        copy_texture(texture, DOOR_TEX)
    write_mtl(DOOR_MODELS / "blast_door_legacy.mtl", [m[2] for m in models])


def material_name(texture):
    return re.sub(r"[^a-z0-9_]", "_", texture.lower())


def source_texture(texture):
    for candidate in (NTM / "textures/models/pheodoors" / f"{texture}.png",
                      NTM / "textures/models/pheodoors/vault" / f"{texture}.png",
                      NTM / "textures/models/doors" / f"{texture}.png",
                      NTM / "textures/models/machines" / f"{texture}.png",
                      NTM / "textures/models" / f"{texture}.png"):
        if candidate.exists():
            return candidate
    raise FileNotFoundError(texture)


def copy_texture(texture, target_dir):
    target_dir.mkdir(parents=True, exist_ok=True)
    target = target_dir / f"{texture}.png"
    shutil.copyfile(source_texture(texture), target)
    return target


def rewrite_obj(src, dst, skins, labels, default_texture):
    lines = src.read_text(encoding="utf-8", errors="ignore").splitlines()
    out = [f"mtllib {dst.stem}.mtl"]
    group = "default"
    wrote_material = set()
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("mtllib ") or stripped.startswith("usemtl "):
            continue
        parts = stripped.split()
        if parts and parts[0] in ("o", "g"):
            group = " ".join(parts[1:]) or "default"
            out.append(line)
            continue
        if parts and parts[0] == "f":
            if group not in wrote_material:
                if group == "Label" and labels:
                    mat = material_name(labels[0])
                else:
                    mat = material_name(default_texture)
                out.append(f"usemtl {mat}")
                wrote_material.add(group)
            out.append(line)
            continue
        out.append(line)
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text("\n".join(out) + "\n", encoding="utf-8")


def write_mtl(dst, textures):
    lines = []
    for texture in dict.fromkeys(textures):
        mat = material_name(texture)
        lines.extend([
            f"newmtl {mat}",
            "Ka 0.0 0.0 0.0",
            "Kd 1.0 1.0 1.0",
            "Ks 0.0 0.0 0.0",
            "d 1.0",
            "illum 1",
            f"map_Kd tex/{texture}.png",
            "",
        ])
    dst.write_text("\n".join(lines), encoding="utf-8")


def block_model(door_id):
    return {"textures": {"particle": f"gfbs_doors:item/{door_id}"}, "elements": []}


def item_model(door_id):
    return {"parent": "builtin/entity"}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    DOOR_MODELS.mkdir(parents=True, exist_ok=True)
    DOOR_TEX.mkdir(parents=True, exist_ok=True)
    ITEM_TEX.mkdir(parents=True, exist_ok=True)

    for door in DOORS:
        door_id = door["id"]
        textures = list(dict.fromkeys(door["skins"] + door["labels"]))
        for texture in textures:
            copy_texture(texture, DOOR_TEX)
        shutil.copyfile(source_texture(door["skins"][0]), ITEM_TEX / f"{door_id}.png")

        if door["model"] is not None:
            src = NTM / "models" / door["model"]
            dst = DOOR_MODELS / src.name
            rewrite_obj(src, dst, door["skins"], door["labels"], door["skins"][0])
            write_mtl(DOOR_MODELS / f"{dst.stem}.mtl", textures)

        variants = {f"facing={facing},core={core}": {"model": f"gfbs_doors:block/{door_id}"}
                    for facing in ("north", "south", "west", "east")
                    for core in ("true", "false")}
        write_json(RES / "blockstates" / f"{door_id}.json", {"variants": variants})
        write_json(MODELS / "block" / f"{door_id}.json", block_model(door_id))
        write_json(MODELS / "item" / f"{door_id}.json", item_model(door_id))

    merge_legacy_blast_door()

    elevator_tex = "elevator"
    copy_texture(elevator_tex, MACHINE_TEX)
    shutil.copyfile(source_texture(elevator_tex), ITEM_TEX / "cargo_elevator.png")
    src = NTM / "models/machines/elevator.obj"
    dst = MACHINE_MODELS / "elevator.obj"
    rewrite_obj(src, dst, [elevator_tex], [], elevator_tex)
    write_mtl(MACHINE_MODELS / "elevator.mtl", [elevator_tex])
    write_json(RES / "blockstates/cargo_elevator.json",
               {"variants": {"core=false": {"model": "gfbs_doors:block/cargo_elevator"},
                             "core=true": {"model": "gfbs_doors:block/cargo_elevator"}}})
    write_json(MODELS / "block/cargo_elevator.json", block_model("cargo_elevator"))
    write_json(MODELS / "item/cargo_elevator.json", item_model("cargo_elevator"))

    sounds_json = {}
    for key, (path, source, stream) in SOUND_FILES.items():
        src = NTM / "sounds" / source
        dst = SOUNDS / f"{path}.ogg"
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dst)
        entry = {"name": f"gfbs_doors:{path}"}
        if stream:
            entry["stream"] = True
        sounds_json[key] = {"category": "block", "sounds": [entry]}
    write_json(RES / "sounds.json", sounds_json)

    for lang, names in (("en_us", NAMES_EN), ("zh_cn", NAMES_ZH)):
        data = {
            "itemGroup.gfbs_doors.main": "GFBS: Doors" if lang == "en_us" else "GFBS：门类",
            "tooltip.gfbs_doors.change_skin": "Shift + Right-click to change skin (Creative only)" if lang == "en_us" else "潜行 + 右键切换外观（仅创造模式）",
            "tooltip.gfbs_doors.redstone": "Right-click to open or close" if lang == "en_us" else "右键开启 / 关闭",
            "tooltip.gfbs_doors.elevator": "Right-click to move; hold another elevator and right-click to extend" if lang == "en_us" else "右键升降；手持货梯右键可加高一节",
        }
        for key, name in names.items():
            data[f"block.gfbs_doors.{key}"] = name
            data[f"item.gfbs_doors.{key}"] = name
        write_json(RES / "lang" / f"{lang}.json", data)

    pickaxe = [f"gfbs_doors:{door['id']}" for door in DOORS] + ["gfbs_doors:cargo_elevator"]
    write_json(DATA / "minecraft/tags/blocks/mineable/pickaxe.json", {"replace": False, "values": pickaxe})

    for door_id, (pattern, keys, count) in RECIPES.items():
        recipe = {
            "type": "minecraft:crafting_shaped",
            "pattern": pattern,
            "key": {key: {"item": item} for key, item in keys.items()},
            "result": {"item": f"gfbs_doors:{door_id}", "count": count},
        }
        write_json(DATA / "gfbs_doors/recipes" / f"{door_id}.json", recipe)

    print("assets written")


if __name__ == "__main__":
    main()
