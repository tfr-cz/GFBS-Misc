"""Audits HazmatArmorModel for the two geometry faults that do not show up in the shaped preview.

1. Coplanar faces: two boxes in the same mesh with a face on exactly the same plane and an
   overlapping footprint. Both faces get drawn at the same depth, so the GPU picks one per pixel
   per frame - that is the "flickering" a player sees.
2. Penetration depth: how far a moving piece (arm, leg, head) reaches into the static neighbour it
   rotates against. Some overlap is unavoidable and hidden, but deep overlap means the moving part
   cuts through the neighbour's outer face as soon as it rotates.

Usage: python audit_hazmat.py
"""

import io
import os
import re
import sys

JAVA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java", "org", "lytharalab", "gfbs", "doors", "client", "armor", "HazmatArmorModel.java")

EPS = 0.02          # planes within this distance are treated as coincident
NUM = r"-?\d+(?:\.\d+)?"

METHOD_RE = re.compile(r"^\s+(?:public|private) static (?:LayerDefinition|void) (\w+)\(")
BOX_RE = re.compile(r"\.addBox\(")
GROUP_RE = re.compile(r"boxes\(\s*([A-Z]+)_U\s*,\s*([A-Z]+)_V\s*\)")
PART_RE = re.compile(r'getChild\("(head|hat|body|right_arm|left_arm|right_leg|left_leg)"\)')
CHILD_RE = re.compile(r'addOrReplaceChild\("(\w+)"')
MX_RE = re.compile(r"mx\(\s*(" + NUM + r")F?\s*,\s*(" + NUM + r")F?\s*,\s*(\w+)\s*\)")
NUMBER_RE = re.compile(r"^(-?[\d.]+)F$")


def split_top_level(args):
    parts, depth, current = [], 0, ""
    for char in args:
        if char == "," and depth == 0:
            parts.append(current.strip())
            current = ""
            continue
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        current += char
    parts.append(current.strip())
    return parts


def calls(line, callee):
    """Yields the argument text of every callee(...) call on the line, nested parentheses included."""
    needle, index = callee + "(", 0
    while True:
        start = line.find(needle, index)
        if start < 0:
            return
        depth, position = 0, start + len(needle) - 1
        while position < len(line):
            if line[position] == "(":
                depth += 1
            elif line[position] == ")":
                depth -= 1
                if depth == 0:
                    break
            position += 1
        yield line[start + len(needle):position]
        index = position + 1


def number(token):
    match = NUMBER_RE.match(token.strip())
    return float(match.group(1)) if match else None


def method_bodies(text):
    """Yields (method, body) for the model's top-level static methods."""
    lines = text.split("\n")
    current, buffer = None, []
    for line in lines:
        match = METHOD_RE.match(line)
        if match:
            if current:
                yield current, "\n".join(buffer)
            current, buffer = match.group(1), [line]
            continue
        if current:
            buffer.append(line)
    if current:
        yield current, "\n".join(buffer)


def boxes_of(body, default_part, mirrored):
    # mx(x, width, mirrored) collapses to its x; the audit compares boxes inside one part, and
    # the mirror only flips the interval, which leaves every plane-distance test unchanged.
    body = re.sub(r"mx\(\s*(-?[\d.]+)F?\s*,\s*-?[\d.]+F?\s*,\s*\w+\s*\)", r"F", body)
    out, part, role, child = [], default_part, "primary", "?"
    for line in body.split("\n"):
        if PART_RE.search(line):
            part = PART_RE.search(line).group(1)
        child_match = CHILD_RE.search(line)
        if child_match:
            child = child_match.group(1)
        for group in GROUP_RE.finditer(line):
            role = group.group(1).lower()
        for args in calls(line, ".addBox"):
            values = [number(v) for v in split_top_level(args)]
            if len(values) != 6 or any(v is None for v in values):
                print("  ! could not read addBox args:", args)
                continue
            x, y, z, w, h, d = values
            out.append(dict(part=part, child=child, role=role,
                            box=(x, y, z, x + w, y + h, z + d)))
    return out


def collect():
    text = io.open(JAVA, encoding="utf-8").read()
    slots = {}
    for name, body in method_bodies(text):
        if name in ("helmet", "chest", "legs", "boots"):
            slots[name] = boxes_of(body, "body" if name in ("chest", "legs", "boots") else "head", False)
        elif name == "arm":
            slots.setdefault("chest", []).extend(boxes_of(body, "right_arm", False))
            slots.setdefault("chest", []).extend(boxes_of(body, "left_arm", True))
        elif name == "legBoxes":
            template = boxes_of(body, "right_leg", False)
            mirrored = [dict(entry, part="left_leg") for entry in template]
            slots.setdefault("legs", []).extend(template + mirrored)
        elif name == "bootBoxes":
            template = boxes_of(body, "right_leg", False)
            mirrored = [dict(entry, part="left_leg") for entry in template]
            slots.setdefault("boots", []).extend(template + mirrored)
    return slots


def coplanar(a, b):
    """Faces on the same plane with an overlapping footprint."""
    axes = ((0, 3), (1, 4), (2, 5))
    hits = []
    for axis, (lo, hi) in enumerate(axes):
        other = [i for i in range(3) if i != axis]
        for value_a, label_a in ((a[lo], "lo"), (a[hi], "hi")):
            for value_b, label_b in ((b[lo], "lo"), (b[hi], "hi")):
                if abs(value_a - value_b) > EPS:
                    continue
                if label_a != label_b:
                    # Back-to-back planes: only the camera-facing side survives back-face culling,
                    # so these never flicker.
                    continue
                if all(a[other[0]] + EPS < b[other[0] + 3] and b[other[0]] + EPS < a[other[0] + 3]
                       and a[other[1]] + EPS < b[other[1] + 3] and b[other[1]] + EPS < a[other[1] + 3]
                       for _ in (0,)):
                    hits.append(("xyz"[axis], label_a, label_b, value_a))
    return hits


def penetration(moving, static):
    """How far the moving box reaches inside the static box, per axis (0 when they do not meet)."""
    depth = []
    for axis in range(3):
        overlap = min(moving[axis + 3], static[axis + 3]) - max(moving[axis], static[axis])
        depth.append(max(0.0, overlap))
    return depth


def main():
    slots = collect()
    print("boxes per slot:", {name: len(entries) for name, entries in slots.items()})
    problems = 0

    print("\n== coplanar faces inside one mesh (flicker) ==")
    for slot, entries in slots.items():
        # Only boxes that share a part keep a fixed relative pose, so only those can z-fight.
        for i in range(len(entries)):
            for j in range(i + 1, len(entries)):
                a, b = entries[i], entries[j]
                if a["part"] != b["part"]:
                    continue
                hits = coplanar(a["box"], b["box"])
                if hits:
                    problems += 1
                    for axis, la, lb, value in hits:
                        print(" %-6s %-10s vs %-10s  %s %s/%s at %.2f   %s %s"
                              % (slot, a["child"], b["child"], axis, la, lb, value, a["role"], b["role"]))
    if problems == 0:
        print(" none")

    print("\n== static overlaps worth knowing (same part, one box inside another) ==")
    for slot, entries in slots.items():
        for i in range(len(entries)):
            for j in range(i + 1, len(entries)):
                a, b = entries[i], entries[j]
                if a["part"] != b["part"] or a["child"] == b["child"]:
                    continue
                depth = penetration(a["box"], b["box"])
                if all(d > 0 for d in depth):
                    pass  # full 3D overlap, normal for stacked shells
    print(" (informational only - stacked shells are expected to interpenetrate)")

    print("\n== moving piece vs. the body it rotates into ==")
    pairs = [("chest", "right_arm", "chest", "body"), ("chest", "left_arm", "chest", "body"),
             ("legs", "right_leg", "legs", "body"), ("legs", "left_leg", "legs", "body")]
    for moving_slot, moving_part, static_slot, static_part in pairs:
        moving = [e for e in slots[moving_slot] if e["part"] == moving_part]
        static = [e for e in slots[static_slot] if e["part"] == static_part]
        worst = None
        for m in moving:
            for s in static:
                depth = penetration(m["box"], s["box"])
                if all(d > 0 for d in depth):
                    if worst is None or max(depth) > max(worst[0]):
                        worst = (depth, m["child"], s["child"])
        if worst:
            print(" %-6s %-12s <- %-6s %-8s  x %.2f  y %.2f  z %.2f"
                  % (moving_part, worst[1], static_part, worst[2], *worst[0]))

    print("\nproblems:", problems)
    return 0 if problems == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
