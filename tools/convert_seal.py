import struct
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
NTM = Path(r"G:/qoder/work/ntm/src/main/resources/assets/hbm")
SOURCE = NTM / "models/doors/seal.dae"
DEST = ROOT / "src/main/resources/assets/gfbs_doors/models/doors"
FPS = 20.0
NS = {"c": "http://www.collada.org/2005/11/COLLADASchema"}


def math_matrix(raw):
    return np.array(raw, dtype=float).reshape(4, 4)


def node_matrix(node):
    matrix = node.find("c:matrix", NS)
    if matrix is None:
        return np.eye(4)
    return math_matrix([float(x) for x in matrix.text.split()])


def read_geometries(root):
    geoms = {}
    for geometry in root.findall(".//c:library_geometries/c:geometry", NS):
        mesh = geometry.find("c:mesh", NS)
        if mesh is None:
            continue
        sources = {}
        for source in mesh.findall("c:source", NS):
            array = source.find("c:float_array", NS)
            if array is not None and array.text:
                sources[source.get("id")] = np.array([float(x) for x in array.text.split()])
        vertices = mesh.find("c:vertices", NS)
        position_input = vertices.find('c:input[@semantic="POSITION"]', NS)
        position_source = position_input.get("source")[1:]
        primitives = []
        for triangles in mesh.findall("c:triangles", NS):
            inputs = {i.get("semantic"): (i.get("source")[1:], int(i.get("offset")))
                      for i in triangles.findall("c:input", NS)}
            data = (triangles.find("c:p", NS).text or "").split()
            stride = max(offset for _, offset in inputs.values()) + 1
            indices = np.array([int(x) for x in data], dtype=int).reshape(-1, stride)
            primitives.append((inputs, indices))
        if primitives and position_source in sources:
            geoms[geometry.get("id")] = (sources, position_source, primitives)
    return geoms


def read_animations(root):
    animations = {}
    source_by_id = {s.get("id"): s for s in root.findall(".//c:library_animations//c:source", NS)}

    def floats(source_id):
        array = source_by_id[source_id].find("c:float_array", NS)
        return np.array([float(x) for x in array.text.split()])

    for animation in root.findall(".//c:library_animations//c:animation", NS):
        channel = animation.find("c:channel", NS)
        if channel is None:
            continue
        target = channel.get("target").split("/")[0]
        sampler = animation.find(f'c:sampler[@id="{channel.get("source")[1:]}"]', NS)
        if sampler is None:
            continue
        times = floats(sampler.find('c:input[@semantic="INPUT"]', NS).get("source")[1:])
        values = floats(sampler.find('c:input[@semantic="OUTPUT"]', NS).get("source")[1:])
        matrices = values.reshape(-1, 16)
        animations[target] = (times, matrices)
    return animations


def normalize(vector):
    length = np.linalg.norm(vector)
    return vector / length if length > 1e-9 else vector


def main():
    root = ET.parse(SOURCE).getroot()
    scene = root.find(".//c:visual_scene", NS)
    nodes = scene.findall("c:node", NS)
    geometries = read_geometries(root)
    animations = read_animations(root)

    groups = {}
    for node in nodes:
        instance = node.find("c:instance_geometry", NS)
        if instance is None:
            continue
        geometry_id = instance.get("url")[1:]
        if geometry_id not in geometries:
            continue
        name = node.get("id")
        animated = name in animations
        matrix = np.eye(4) if animated else node_matrix(node)
        groups[name] = (geometry_id, matrix, animated)

    positions = []
    texcoords = []
    normals = []
    faces = []
    group_faces = {}

    def vertex_index(position, normal, uv):
        key = (tuple(np.round(position, 6)), tuple(np.round(normal, 6)), tuple(np.round(uv, 6)))
        existing = vertex_index.cache.get(key)
        if existing is not None:
            return existing
        positions.append(position)
        normals.append(normal)
        texcoords.append(uv)
        index = len(positions)
        vertex_index.cache[key] = index
        return index

    vertex_index.cache = {}

    def add_group(group_name, matrix, geometry_id, bake):
        sources, position_source, primitives = geometries[geometry_id]
        group_faces.setdefault(group_name, [])
        rotation = matrix[:3, :3]
        normal_matrix = np.linalg.inv(rotation).T if bake else np.eye(3)
        for inputs, indices in primitives:
            pos_offset = inputs["VERTEX"][1]
            normal_offset = inputs["NORMAL"][1]
            uv_offset = inputs["TEXCOORD"][1]
            pos_values = sources[position_source].reshape(-1, 3)
            normal_values = sources[inputs["NORMAL"][0]].reshape(-1, 3)
            uv_values = sources[inputs["TEXCOORD"][0]].reshape(-1, 2)
            for row in range(len(indices) // 3):
                face = []
                for corner in range(3):
                    entry = indices[row * 3 + corner]
                    position = pos_values[entry[pos_offset]]
                    normal = normalize(normal_values[entry[normal_offset]])
                    uv = uv_values[entry[uv_offset]].copy()
                    if bake:
                        position = rotation @ position + matrix[:3, 3]
                        normal = normalize(normal_matrix @ normal)
                    face.append(vertex_index(position, normal, uv))
                group_faces[group_name].append(face)

    frame_parts = []
    for name, (geometry_id, matrix, animated) in groups.items():
        if animated:
            continue
        frame_parts.append((geometry_id, matrix))
    for geometry_id, matrix in frame_parts:
        add_group("frame", matrix, geometry_id, True)

    animated_order = []
    for name in animations:
        if name in groups and groups[name][2]:
            animated_order.append(name)
            geometry_id, matrix, _ = groups[name]
            add_group(name, np.eye(4), geometry_id, False)

    DEST.mkdir(parents=True, exist_ok=True)
    obj = ["mtllib transition_seal.mtl"]
    for position in positions:
        obj.append(f"v {position[0]:.6f} {position[1]:.6f} {position[2]:.6f}")
    for uv in texcoords:
        obj.append(f"vt {uv[0]:.6f} {uv[1]:.6f}")
    for normal in normals:
        obj.append(f"vn {normal[0]:.6f} {normal[1]:.6f} {normal[2]:.6f}")
    for group_name, group in group_faces.items():
        if not group:
            continue
        obj.append(f"o {group_name}")
        obj.append("usemtl transition_seal")
        for face in group:
            obj.append("f " + " ".join(f"{i}/{i}/{i}" for i in face))
    (DEST / "transition_seal.obj").write_text("\n".join(obj) + "\n", encoding="utf-8")

    (DEST / "transition_seal.mtl").write_text(
        "newmtl transition_seal\nKa 0 0 0\nKd 1 1 1\nKs 0 0 0\nd 1\nillum 1\nmap_Kd tex/transition_seal.png\n",
        encoding="utf-8")

    duration = max(float(times[-1]) for times, _ in animations.values())
    frame_count = int(round(duration * FPS)) + 1
    sample_times = np.linspace(0.0, duration, frame_count)
    names = animated_order
    matrices = np.zeros((frame_count, len(names), 4, 4), dtype=float)

    for node_index, name in enumerate(names):
        times, values = animations[name]
        for frame, time in enumerate(sample_times):
            if time <= times[0]:
                matrix = math_matrix(values[0])
            elif time >= times[-1]:
                matrix = math_matrix(values[-1])
            else:
                right = int(np.searchsorted(times, time))
                left = right - 1
                span = times[right] - times[left]
                blend = 0.0 if span <= 0 else (time - times[left]) / span
                m0 = math_matrix(values[left])
                m1 = math_matrix(values[right])
                matrix = m0 + (m1 - m0) * blend
            matrices[frame, node_index] = matrix

    with (DEST / "transition_seal.anim").open("wb") as stream:
        stream.write(struct.pack(">iiifi", 0x47464244, 1, len(names), duration, frame_count))
        for name in names:
            encoded = name.encode("utf-8")
            stream.write(struct.pack(">H", len(encoded)))
            stream.write(encoded)
        for time in sample_times:
            stream.write(struct.pack(">f", float(time)))
        for frame in range(frame_count):
            for node_index in range(len(names)):
                stream.write(struct.pack(">16f", *matrices[frame, node_index].flatten()))

    print("wrote", len(positions), "vertices,", len(animated_order), "animated nodes,", frame_count, "frames")


if __name__ == "__main__":
    main()
