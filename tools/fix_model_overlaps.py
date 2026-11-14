"""Removes coplanar faces from HazmatArmorModel by nudging the smaller box of each colliding pair.

Two faces on exactly the same plane that both point the same way are both rasterised, and which one
wins is decided per pixel per frame - that is the flicker a player sees at the wrist, where the glove
was built with the same x/z extents as the sleeve.

The fix is deliberately tiny: the decorative box's offending face moves inward by GAP units (0.06 of
a pixel-block, far below anything visible), which leaves the silhouette untouched but puts the two
faces on different planes. Pairs whose faces point opposite ways are left alone: back-face culling
means only the camera-facing side is ever drawn.

Run after any geometry edit; audit_hazmat.py should then report no problems.
"""

import io
import os
import re
import sys

JAVA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "java", "org", "lytharalab", "gfbs", "doors", "client", "armor", "HazmatArmorModel.java")

GAP = 0.06
EPS = 0.02
NUM = r"-?\d+(?:\.\d+)?"
NUMBER_RE = re.compile(r"^(-?[\d.]+)F$")
METHOD_RE = re.compile(r"^\s+(?:public|private) static (?:LayerDefinition|void) (\w+)\(")
PART_RE = re.compile(r'getChild\("(head|hat|body|right_arm|left_arm|right_leg|left_leg)"\)')
CHILD_RE = re.compile(r'addOrReplaceChild\("(\w+)"')
MX_RE = re.compile(r"mx\(\s*(" + NUM + r")F?\s*,\s*(" + NUM + r")F?\s*,\s*(\w+)\s*\)")


def fmt(value):
    text = "%.2f" % value
    if text.endswith(".00"):
        return text[:-1]
    if text.endswith("0"):
        return text[:-1]
    return text


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


def call_spans(line, callee):
    """Yields (args_start, args_end) for each callee(...) on the line."""
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
        yield start + len(needle), position
        index = position + 1


def parse():
    lines = io.open(JAVA, encoding="utf-8").read().split("\n")
    boxes, method, part, child = [], None, "body", "?"
    for index, line in enumerate(lines):
        match = METHOD_RE.match(line)
        if match:
            method = match.group(1)
            # Boxes under getChild(part) carry no literal part name, so scope them to the method:
            # arm() and legBoxes()/bootBoxes() are single groups of geometry.
            part = method
            continue
        if PART_RE.search(line):
            part = PART_RE.search(line).group(1)
        child_match = CHILD_RE.search(line)
        if child_match:
            child = child_match.group(1)
        for start, end in call_spans(line, ".addBox"):
            args = line[start:end]
            if "mx(" in args:
                mx = MX_RE.search(args)
                xs, ws, flag = mx.group(1), mx.group(2), mx.group(3)
                rest = split_top_level(args)[1:]
                values = [float(xs), float(ws)] + [float(NUMBER_RE.match(v).group(1)) for v in rest]
                # x, y, z, w, h, d with x/width coming from the mx() helper
                x, y, z, w, h, d = values[0], values[2], values[3], values[4], values[5], values[6]
                boxes.append(dict(line=index, start=start, end=end, method=method, part=part,
                                  child=child, has_mx=True, flag=flag,
                                  x=x, y=y, z=z, w=w, h=h, d=d))
            else:
                values = [float(NUMBER_RE.match(v).group(1)) for v in split_top_level(args)]
                x, y, z, w, h, d = values
                boxes.append(dict(line=index, start=start, end=end, method=method, part=part,
                                  child=child, has_mx=False, flag=None,
                                  x=x, y=y, z=z, w=w, h=h, d=d))
    return lines, boxes


def extent(box):
    return box["x"] + box["w"], box["y"] + box["h"], box["z"] + box["d"]


def coincidences(boxes):
    """Same-direction face pairs inside one part, which is a fixed relative pose."""
    out = []
    for i in range(len(boxes)):
        for j in range(i + 1, len(boxes)):
            a, b = boxes[i], boxes[j]
            if a["part"] != b["part"]:
                continue
            a_lo = (a["x"], a["y"], a["z"])
            b_lo = (b["x"], b["y"], b["z"])
            a_hi, b_hi = extent(a), extent(b)
            for axis in range(3):
                others = [k for k in range(3) if k != axis]
                if not all(min(a_hi[k], b_hi[k]) - max(a_lo[k], b_lo[k]) > EPS for k in others):
                    continue
                for side in ("lo", "hi"):
                    av = a_lo[axis] if side == "lo" else a_hi[axis]
                    bv = b_lo[axis] if side == "lo" else b_hi[axis]
                    if abs(av - bv) <= EPS:
                        out.append((i, j, axis, side))
    return out


def nudge(box, axis, side, amount):
    key = "xyz"[axis]
    size = box["whd"[axis]]
    if size - amount < 0.05:
        return False
    if side == "lo":
        box[key] += amount
    box["whd"[axis]] = size - amount
    return True


def volume(box):
    return box["w"] * box["h"] * box["d"]


def apply(lines, boxes, edits):
    for index in sorted(edits):
        box = boxes[index]
        line = lines[box["line"]]
        x1, y1, z1 = extent(box)
        tail = ", ".join(fmt(v) + "F" for v in (box["y"], box["z"], box["w"], box["h"], box["d"]))
        if box["has_mx"]:
            head = "mx(%sF, %sF, %s)" % (fmt(box["x"]), fmt(box["w"]), box["flag"])
            args = head + ", " + tail
        else:
            args = ", ".join(fmt(v) + "F" for v in (box["x"], box["y"], box["z"],
                                                    box["w"], box["h"], box["d"]))
        lines[box["line"]] = line[:box["start"]] + args + line[box["end"]:]
        # Rewriting one call can shift the spans of later calls on the same line.
        shift = len(args) - (box["end"] - box["start"])
        for other in boxes:
            if other["line"] == box["line"] and other["start"] > box["start"]:
                other["start"] += shift
                other["end"] += shift


def main():
    lines, boxes = parse()
    print("boxes:", len(boxes))
    for round_index in range(12):
        # Re-parse every round: applying a nudge shifts the source offsets, so stale spans would
        # write one box's numbers into another's line.
        lines, boxes = parse()
        pairs = coincidences(boxes)
        if not pairs:
            print("round %d: clean" % round_index)
            break
        touched = {}
        for i, j, axis, side in pairs:
            a, b = boxes[i], boxes[j]
            target = i if volume(a) < volume(b) - 1e-6 else (j if volume(b) < volume(a) - 1e-6 else max(i, j))
            if not nudge(boxes[target], axis, side, GAP):
                other = j if target == i else i
                nudge(boxes[other], axis, side, GAP)
                target = other
            touched[target] = True
        print("round %d: %d coplanar pairs, nudged %d boxes" % (round_index, len(pairs), len(touched)))
        apply(lines, boxes, touched)
    io.open(JAVA, "w", encoding="utf-8", newline="\n").write("\n".join(lines))


if __name__ == "__main__":
    sys.exit(main() or 0)
