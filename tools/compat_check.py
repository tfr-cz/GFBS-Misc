#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""GFBS-Misc compatibility red-line regression check.

This tool is deliberately dependency-free: it only uses the Python 3 standard
library, never invokes Gradle, never opens a network connection, and never
starts Minecraft.

Usage (the project root is inferred from this file's location):

    python tools/compat_check.py
    python tools/compat_check.py --write-baseline

Default mode compares the current tree against ``tools/compat_baseline.json``.
``--write-baseline`` refreshes that file, but only when every hard red-line
check passes (so a broken tree cannot silently become the new baseline).

Exit code: 0 when everything is OK, 1 when any check fails.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple


# ---------------------------------------------------------------------------
# Paths.  Everything is derived from this file, never from a hard-coded drive.
# ---------------------------------------------------------------------------

TOOLS_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = TOOLS_DIR.parent
BASELINE_PATH = TOOLS_DIR / "compat_baseline.json"

JAVA_ROOT = PROJECT_ROOT / "src" / "main" / "java"
RESOURCES_ROOT = PROJECT_ROOT / "src" / "main" / "resources"
ASSETS_ROOT = RESOURCES_ROOT / "assets"
DATA_ROOT = RESOURCES_ROOT / "data"

DOOR_SPECS_FILE = (
    JAVA_ROOT
    / "org"
    / "lytharalab"
    / "gfbs"
    / "doors"
    / "content"
    / "DoorSpecs.java"
)
PICKAXE_TAG_FILE = (
    DATA_ROOT / "minecraft" / "tags" / "blocks" / "mineable" / "pickaxe.json"
)
GFBS_DOORS_BLOCKSTATES_DIR = ASSETS_ROOT / "gfbs_doors" / "blockstates"
GFBS_DOORS_RECIPES_DIR = DATA_ROOT / "gfbs_doors" / "recipes"

SCHEMA_VERSION = 1
PROJECT_NAME = "GFBS-Misc"

# Namespaces used by ResourceLocation constructor calls.  ``forge`` is a
# vanilla/loader namespace and is explicitly tolerated; anything else new must
# be reviewed before the baseline is refreshed.
CORE_JAVA_NAMESPACES = {"gfbs_misc", "gfbs_doors", "minecraft"}
VANILLA_JAVA_NAMESPACES = {"forge"}
ALLOWED_JAVA_NAMESPACES = CORE_JAVA_NAMESPACES | VANILLA_JAVA_NAMESPACES

# Resource roots are stricter: only the two legacy content namespaces may own
# assets; data/minecraft is the single allowed vanilla data root (tags).
ALLOWED_ASSETS_NAMESPACES = {"gfbs_doors"}
ALLOWED_DATA_NAMESPACES = {"gfbs_doors", "minecraft"}

# Recipes under data/gfbs_doors may produce door ids, plus these non-door
# items.  door_frame / door_frame_tone are normal blocks that are not DoorSpecs.
KNOWN_RECIPE_ITEMS = {"door_configurator", "door_frame", "door_frame_tone",
                       "hazmat_helmet", "hazmat_chest", "hazmat_legs", "hazmat_boots"}

# The doors-only line has no cabin elevator; the constant is kept so the pickaxe
# completeness check keeps working without a second code path.
REQUIRED_CABIN_ELEVATOR_BLOCKS = set()

# The two legacy namespace holders, plus the bundle's own namespace holder.
NAMESPACE_CONSTANT_FILES = (
    (
        "gfbs_doors",
        "src/main/java/org/lytharalab/gfbs/doors/GFBSDoors.java",
        "gfbs_doors",
    ),
    (
        "gfbs_misc",
        "src/main/java/org/lytharalab/gfbs/misc/GFBSMisc.java",
        "gfbs_misc",
    ),
)

REQUIRED_BASELINE_KEYS = {
    "schema_version",
    "project",
    "java_namespaces",
    "resource_roots",
    "door_ids",
    "door_id_count",
    "namespace_constants",
    "known_recipe_items",
    "recipe_result_ids",
    "pickaxe_entries",
    "pickaxe_entry_count",
    "required_pickaxe_entries",
    "counts",
}


# ---------------------------------------------------------------------------
# Small utility helpers
# ---------------------------------------------------------------------------


def rel(path: Path) -> str:
    """Return a POSIX-style path relative to the project root when possible."""
    try:
        return path.resolve().relative_to(PROJECT_ROOT).as_posix()
    except ValueError:
        return str(path)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def read_json(path: Path):
    return json.loads(read_text(path))


def fmt(value, limit: int = 14) -> str:
    """Compact, deterministic rendering for check output."""
    if isinstance(value, (set, frozenset)):
        value = sorted(value)
    if isinstance(value, (list, tuple)):
        items = [str(item) for item in value]
        if len(items) > limit:
            head = ", ".join(items[:limit])
            return "[" + head + ", ... (+%d more)]" % (len(items) - limit)
        return "[" + ", ".join(items) + "]"
    return str(value)


def describe_diff(expected: Sequence, actual: Sequence, limit: int = 14) -> List[str]:
    """Return human-readable expected/actual plus added/removed lines."""
    expected_list = list(expected)
    actual_list = list(actual)
    expected_set = set(expected_list)
    actual_set = set(actual_list)
    added = sorted(actual_set - expected_set)
    removed = sorted(expected_set - actual_set)
    lines = [
        "expected: %s" % fmt(expected_list, limit),
        "actual:   %s" % fmt(actual_list, limit),
    ]
    if added:
        lines.append("added:    %s" % fmt(added, limit))
    if removed:
        lines.append("removed:  %s" % fmt(removed, limit))
    return lines


class Check:
    """One named result in the final OK/FAIL list."""

    def __init__(self, name: str) -> None:
        self.name = name
        self.notes: List[str] = []
        self.failures: List[str] = []

    @property
    def ok(self) -> bool:
        return not self.failures

    def note(self, message: str) -> None:
        self.notes.append(str(message))

    def fail(self, message: str) -> None:
        self.failures.append(str(message))

    def fail_lines(self, lines: Iterable[str]) -> None:
        for line in lines:
            self.fail(line)


def print_check(check: Check) -> None:
    print("[%s] %s" % ("OK" if check.ok else "FAIL", check.name))
    for line in check.notes:
        for part in str(line).splitlines():
            print("       %s" % part)
    for line in check.failures:
        for part in str(line).splitlines():
            print("       %s" % part)


# ---------------------------------------------------------------------------
# Java source scanning
# ---------------------------------------------------------------------------


def mask_java(text: str, keep_strings: bool = False) -> str:
    """Mask Java comments (and optionally string contents) without moving text.

    The returned string has exactly the same length as ``text``; offsets and
    line numbers therefore remain valid.  Newlines are preserved so that line
    numbers can be computed from the masked text as well.
    """
    out: List[str] = []
    i = 0
    n = len(text)
    state = "code"
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ""

        if state == "code":
            if ch == "/" and nxt == "/":
                out.append("  ")
                i += 2
                state = "line_comment"
                continue
            if ch == "/" and nxt == "*":
                out.append("  ")
                i += 2
                state = "block_comment"
                continue
            if ch == '"' and text.startswith('"""', i):
                out.append("   ")
                i += 3
                state = "text_block"
                continue
            if ch == '"':
                out.append(ch)
                i += 1
                state = "string"
                continue
            if ch == "'":
                out.append(ch)
                i += 1
                state = "char"
                continue
            out.append(ch)
            i += 1

        elif state == "line_comment":
            if ch == "\n":
                out.append(ch)
                state = "code"
            else:
                out.append(" ")
            i += 1

        elif state == "block_comment":
            if ch == "*" and nxt == "/":
                out.append("  ")
                i += 2
                state = "code"
            else:
                out.append("\n" if ch == "\n" else " ")
                i += 1

        elif state == "string":
            if keep_strings:
                out.append(ch)
            else:
                out.append("\n" if ch == "\n" else " ")
            if ch == "\\" and i + 1 < n:
                if keep_strings:
                    out.append(text[i + 1])
                else:
                    out.append("\n" if text[i + 1] == "\n" else " ")
                i += 2
                continue
            if ch == '"':
                state = "code"
            i += 1

        elif state == "char":
            if keep_strings:
                out.append(ch)
            else:
                out.append("\n" if ch == "\n" else " ")
            if ch == "\\" and i + 1 < n:
                if keep_strings:
                    out.append(text[i + 1])
                else:
                    out.append("\n" if text[i + 1] == "\n" else " ")
                i += 2
                continue
            if ch == "'":
                state = "code"
            i += 1

        elif state == "text_block":
            if text.startswith('"""', i):
                out.append("   ")
                i += 3
                state = "code"
            else:
                out.append("\n" if ch == "\n" else " ")
                i += 1

    return "".join(out)


def java_unescape(value: str) -> str:
    """Decode the small set of Java escapes that can appear in ids/paths."""
    mapping = {
        "n": "\n",
        "r": "\r",
        "t": "\t",
        '"': '"',
        "'": "'",
        "\\": "\\",
    }
    out: List[str] = []
    i = 0
    while i < len(value):
        if value[i] == "\\" and i + 1 < len(value):
            out.append(mapping.get(value[i + 1], value[i + 1]))
            i += 2
        else:
            out.append(value[i])
            i += 1
    return "".join(out)


def collect_java_constants(
    files: Sequence[Path],
) -> Tuple[Dict[str, str], Dict[Path, Dict[str, str]]]:
    """Collect ``static final String`` values, keyed by Class.FIELD and file."""
    global_constants: Dict[str, str] = {}
    local_constants: Dict[Path, Dict[str, str]] = {}

    for path in files:
        try:
            raw = read_text(path)
        except OSError:
            continue
        text = mask_java(raw, keep_strings=True)
        structure = mask_java(raw, keep_strings=False)

        package_match = re.search(
            r"\bpackage\s+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)\s*;",
            structure,
        )
        package = package_match.group(1) if package_match else ""
        class_match = re.search(
            r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)",
            structure,
        )
        class_name = class_match.group(1) if class_match else ""
        local: Dict[str, str] = {}

        pattern = re.compile(
            r'\bstatic\s+final\s+String\s+([A-Za-z_$][\w$]*)\s*=\s*'
            r'"((?:\\.|[^"\\])*)"\s*;'
        )
        for match in pattern.finditer(text):
            field = match.group(1)
            value = java_unescape(match.group(2))
            local[field] = value
            if class_name:
                global_constants["%s.%s" % (class_name, field)] = value
                if package:
                    global_constants[
                        "%s.%s.%s" % (package, class_name, field)
                    ] = value

        local_constants[path] = local

    return global_constants, local_constants


def extract_paren_content(text: str, open_index: int) -> Tuple[Optional[str], int]:
    """Return the text between ``text[open_index] == '('`` and its matching ')'."""
    depth = 0
    start = open_index + 1
    i = open_index
    n = len(text)
    in_string = False
    in_char = False
    in_text_block = False

    while i < n:
        ch = text[i]

        if in_text_block:
            if text.startswith('"""', i):
                in_text_block = False
                i += 3
                continue
            i += 1
            continue

        if in_string:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_string = False
            i += 1
            continue

        if in_char:
            if ch == "\\":
                i += 2
                continue
            if ch == "'":
                in_char = False
            i += 1
            continue

        if text.startswith('"""', i):
            in_text_block = True
            i += 3
            continue
        if ch == '"':
            in_string = True
            i += 1
            continue
        if ch == "'":
            in_char = True
            i += 1
            continue

        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[start:i], i
        i += 1

    return None, open_index


def split_top_level_args(content: str) -> List[str]:
    """Split constructor arguments on commas that are not nested or quoted."""
    if not content:
        return []

    args: List[str] = []
    start = 0
    depth = 0
    i = 0
    n = len(content)
    in_string = False
    in_char = False
    in_text_block = False

    while i < n:
        ch = content[i]

        if in_text_block:
            if content.startswith('"""', i):
                in_text_block = False
                i += 3
                continue
            i += 1
            continue

        if in_string:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_string = False
            i += 1
            continue

        if in_char:
            if ch == "\\":
                i += 2
                continue
            if ch == "'":
                in_char = False
            i += 1
            continue

        if content.startswith('"""', i):
            in_text_block = True
            i += 3
            continue
        if ch == '"':
            in_string = True
            i += 1
            continue
        if ch == "'":
            in_char = True
            i += 1
            continue

        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        elif ch == "," and depth == 0:
            part = content[start:i].strip()
            if part:
                args.append(part)
            start = i + 1
        i += 1

    tail = content[start:].strip()
    if tail:
        args.append(tail)
    return args


def resolve_namespace(
    expression: str,
    path: Path,
    global_constants: Dict[str, str],
    local_constants: Dict[Path, Dict[str, str]],
    has_second_arg: bool,
) -> Optional[str]:
    """Resolve the namespace part of a ResourceLocation first argument.

    Handles string literals, ``MODID`` constants, and expressions beginning
    with a constant such as ``GFBSDoors.MODID``.  For the one-argument form a
    literal without ``:`` is a path in the default ``minecraft`` namespace; for
    the two-argument form it is the namespace itself.
    """
    expr = expression.strip()
    # Peel a few redundant outer parentheses.
    while expr.startswith("(") and expr.endswith(")"):
        expr = expr[1:-1].strip()

    literal_match = re.match(r'^"((?:\\.|[^"\\])*)"', expr)
    if literal_match:
        value = java_unescape(literal_match.group(1))
        rest = expr[literal_match.end():].strip()
        if ":" in value:
            # "ns:path" + anything still starts in a known namespace.
            return value.split(":", 1)[0]
        if has_second_arg and rest:
            # e.g. new ResourceLocation("gfbs_doors" + suffix, path): the
            # actual namespace cannot be determined statically.
            return None
        return value if has_second_arg else "minecraft"

    ref_match = re.match(
        r"^([A-Za-z_$][\w$]*(?:\s*\.\s*[A-Za-z_$][\w$]*)*)", expr
    )
    if not ref_match:
        return None
    if expr[ref_match.end():].strip():
        # Reject MODID + "_suffix" and similar dynamic namespace expressions
        # rather than silently treating them as MODID itself.
        return None

    ref = re.sub(r"\s+", "", ref_match.group(1))
    parts = ref.split(".")
    candidates = [ref]
    if len(parts) >= 2:
        candidates.append(".".join(parts[-2:]))
    if len(parts) >= 3:
        candidates.append(".".join(parts[-3:]))

    resolved: Optional[str] = None
    for candidate in candidates:
        if candidate in global_constants:
            resolved = global_constants[candidate]
            break
    if resolved is None:
        short = parts[-1]
        resolved = local_constants.get(path, {}).get(short)
    if resolved is None:
        return None

    if ":" in resolved:
        return resolved.split(":", 1)[0]
    return resolved if has_second_arg else "minecraft"


RL_CALL_RE = re.compile(
    r"(?<![\w$])(?:new\s+)?(?:[A-Za-z_$][\w$]*\s*\.\s*)*ResourceLocation\s*\("
)


def collect_resource_location_uses(
    files: Sequence[Path],
) -> List[dict]:
    """Return every ResourceLocation(...) call with its resolved namespace."""
    global_constants, local_constants = collect_java_constants(files)
    uses: List[dict] = []

    for path in files:
        try:
            raw = read_text(path)
        except OSError:
            continue

        # Comments are blanked in both masks; strings are blanked only in the
        # structural mask so that "ResourceLocation(" inside text is ignored.
        structure = mask_java(raw, keep_strings=False)
        with_strings = mask_java(raw, keep_strings=True)

        for match in RL_CALL_RE.finditer(structure):
            open_index = match.end() - 1
            content, _ = extract_paren_content(with_strings, open_index)
            args = split_top_level_args(content) if content is not None else []
            namespace = None
            if args:
                namespace = resolve_namespace(
                    args[0],
                    path,
                    global_constants,
                    local_constants,
                    len(args) >= 2,
                )
            line = raw.count("\n", 0, match.start()) + 1
            first_arg = args[0] if args else ""
            uses.append(
                {
                    "path": path,
                    "line": line,
                    "namespace": namespace,
                    "first_arg": first_arg,
                }
            )

    return uses


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------


def check_baseline_metadata(baseline: dict) -> Check:
    check = Check("baseline metadata")
    missing = sorted(REQUIRED_BASELINE_KEYS - set(baseline.keys()))
    if missing:
        check.fail("expected: baseline keys %s" % fmt(sorted(REQUIRED_BASELINE_KEYS)))
        check.fail("actual:   missing %s" % fmt(missing))
    if baseline.get("schema_version") != SCHEMA_VERSION:
        check.fail(
            "expected schema_version: %r" % SCHEMA_VERSION
        )
        check.fail("actual schema_version:   %r" % baseline.get("schema_version"))
    if baseline.get("project") != PROJECT_NAME:
        check.fail("expected project: %r" % PROJECT_NAME)
        check.fail("actual project:   %r" % baseline.get("project"))
    expected_known = sorted(KNOWN_RECIPE_ITEMS)
    actual_known = baseline.get("known_recipe_items")
    if not isinstance(actual_known, list) or sorted(actual_known) != expected_known:
        check.fail("expected known_recipe_items: %s" % fmt(expected_known))
        check.fail("actual known_recipe_items:   %s" % fmt(actual_known))
    if not check.failures:
        check.note(
            "schema=%s project=%s known_recipe_items=%s"
            % (SCHEMA_VERSION, PROJECT_NAME, fmt(expected_known))
        )
    return check


def check_java_namespaces(baseline: Optional[dict]) -> Tuple[Check, List[str], List[dict]]:
    check = Check("Java ResourceLocation namespaces")
    files = sorted(JAVA_ROOT.rglob("*.java")) if JAVA_ROOT.is_dir() else []
    if not files:
        check.fail("expected: Java sources under %s" % rel(JAVA_ROOT))
        check.fail("actual:   no .java files found")
        return check, [], []

    uses = collect_resource_location_uses(files)
    namespaces = sorted({u["namespace"] for u in uses if u["namespace"]})
    unresolved = [u for u in uses if not u["namespace"]]

    if not uses:
        check.fail("expected: at least one ResourceLocation(...) call")
        check.fail("actual:   0 calls")
    if unresolved:
        check.fail(
            "expected: resolvable namespace constant/literal in every ResourceLocation call"
        )
        for use in unresolved:
            check.fail(
                "actual:   %s:%d cannot resolve %r"
                % (rel(use["path"]), use["line"], use["first_arg"])
            )

    unknown = sorted(set(namespaces) - ALLOWED_JAVA_NAMESPACES)
    if unknown:
        check.fail(
            "expected namespace(s): %s (plus vanilla 'forge')"
            % fmt(sorted(ALLOWED_JAVA_NAMESPACES))
        )
        check.fail("actual: new/unknown namespace(s): %s" % fmt(unknown))

    check.note("calls: %d; namespaces: %s" % (len(uses), fmt(namespaces)))

    if baseline is not None:
        expected = baseline.get("java_namespaces")
        if not isinstance(expected, list):
            check.fail("baseline 'java_namespaces' is missing or not a list")
        elif namespaces != sorted(expected):
            check.fail_lines(describe_diff(expected, namespaces))

    return check, namespaces, uses


def direct_subdirs(root: Path) -> List[str]:
    if not root.is_dir():
        return []
    return sorted(
        child.name
        for child in root.iterdir()
        if child.is_dir() and not child.name.startswith(".")
    )


def check_resource_roots(baseline: Optional[dict]) -> Tuple[Check, Dict[str, List[str]]]:
    check = Check("resource root namespaces (assets/ and data/)")
    assets = direct_subdirs(ASSETS_ROOT)
    data = direct_subdirs(DATA_ROOT)
    roots = {"assets": assets, "data": data}

    if not ASSETS_ROOT.is_dir():
        check.fail("expected: %s exists" % rel(ASSETS_ROOT))
        check.fail("actual:   missing")
    if not DATA_ROOT.is_dir():
        check.fail("expected: %s exists" % rel(DATA_ROOT))
        check.fail("actual:   missing")

    for required in ("gfbs_doors",):
        if required not in assets:
            check.fail("expected: %s/" % rel(ASSETS_ROOT / required))
            check.fail("actual:   missing")
        if required not in data:
            check.fail("expected: %s/" % rel(DATA_ROOT / required))
            check.fail("actual:   missing")

    bad_assets = sorted(set(assets) - ALLOWED_ASSETS_NAMESPACES)
    bad_data = sorted(set(data) - ALLOWED_DATA_NAMESPACES)
    if bad_assets:
        check.fail(
            "expected assets namespaces: %s" % fmt(sorted(ALLOWED_ASSETS_NAMESPACES))
        )
        check.fail("actual: unexpected assets namespace(s): %s" % fmt(bad_assets))
    if bad_data:
        check.fail(
            "expected data namespaces: %s (data/minecraft is the only vanilla exception)"
            % fmt(sorted(ALLOWED_DATA_NAMESPACES))
        )
        check.fail("actual: unexpected data namespace(s): %s" % fmt(bad_data))

    check.note("assets: %s" % fmt(assets))
    check.note("data:   %s" % fmt(data))

    if baseline is not None:
        expected_roots = baseline.get("resource_roots")
        if not isinstance(expected_roots, dict):
            check.fail("baseline 'resource_roots' is missing or not an object")
        else:
            for key, actual in (("assets", assets), ("data", data)):
                expected = expected_roots.get(key)
                if not isinstance(expected, list):
                    check.fail(
                        "baseline 'resource_roots.%s' is missing or not a list" % key
                    )
                elif actual != sorted(expected):
                    check.fail("resource_roots.%s" % key)
                    check.fail_lines(describe_diff(expected, actual))

    return check, roots


def parse_door_ids() -> Tuple[List[str], Optional[str]]:
    if not DOOR_SPECS_FILE.is_file():
        return [], "missing %s" % rel(DOOR_SPECS_FILE)
    try:
        raw = read_text(DOOR_SPECS_FILE)
    except OSError as exc:
        return [], "cannot read %s: %s" % (rel(DOOR_SPECS_FILE), exc)

    text = mask_java(raw, keep_strings=True)

    # Constant name -> spec id, e.g. VAULT_DOOR = DoorSpec.builder("vault_door").
    constants: Dict[str, str] = {}
    for match in re.finditer(
        r'\b(\w+)\s*=\s*DoorSpec\s*\.\s*builder\s*\(\s*"((?:\\.|[^"\\])*)"\s*\)', text
    ):
        constants[match.group(1)] = java_unescape(match.group(2))

    ids = list(constants.values())

    # Enlarged variants, e.g. VAULT_DOOR_LARGE = large(VAULT_DOOR, 1, 1).
    for match in re.finditer(r'\b(\w+)\s*=\s*large\s*\(\s*(\w+)\s*,', text):
        base = constants.get(match.group(2))
        if base is not None:
            ids.append(base + "_large")
    return ids, None


def check_door_ids(baseline: Optional[dict]) -> Tuple[Check, List[str]]:
    check = Check("door ids and per-id resources")
    ids_raw, error = parse_door_ids()
    if error:
        check.fail("expected: parse DoorSpec.builder ids from %s" % rel(DOOR_SPECS_FILE))
        check.fail("actual:   %s" % error)
        return check, []

    ids = sorted(ids_raw)
    if not ids:
        check.fail("expected: at least one DoorSpec.builder(\"id\") entry")
        check.fail("actual:   0 entries")
        return check, []

    duplicates = sorted({item for item in ids_raw if ids_raw.count(item) > 1})
    if duplicates:
        check.fail("expected: unique DoorSpec ids")
        check.fail("actual: duplicate id(s): %s" % fmt(duplicates))

    missing_blockstates: List[str] = []
    missing_recipes: List[str] = []
    for door_id in ids:
        blockstate = GFBS_DOORS_BLOCKSTATES_DIR / ("%s.json" % door_id)
        recipe = GFBS_DOORS_RECIPES_DIR / ("%s.json" % door_id)
        if not blockstate.is_file():
            missing_blockstates.append(rel(blockstate))
        if not recipe.is_file():
            missing_recipes.append(rel(recipe))

    if missing_blockstates:
        check.fail(
            "expected: blockstate JSON for every door id (%d files)" % len(ids)
        )
        check.fail("actual:   missing %s" % fmt(missing_blockstates, 30))
    if missing_recipes:
        check.fail("expected: recipe JSON for every door id (%d files)" % len(ids))
        check.fail("actual:   missing %s" % fmt(missing_recipes, 30))

    # Blocks that are not DoorSpecs but still ship as first-class content: the
    # decorative frames.  The doors-only line has no cargo elevator.
    for extra in ("door_frame", "door_frame_tone"):
        blockstate = GFBS_DOORS_BLOCKSTATES_DIR / ("%s.json" % extra)
        recipe = GFBS_DOORS_RECIPES_DIR / ("%s.json" % extra)
        if not blockstate.is_file():
            check.fail("expected: %s" % rel(blockstate))
            check.fail("actual:   missing (%s blockstate)" % extra)
        if not recipe.is_file():
            check.fail("expected: %s" % rel(recipe))
            check.fail("actual:   missing (%s recipe)" % extra)

    check.note("door ids (%d): %s" % (len(ids), fmt(ids, 30)))

    if baseline is not None:
        expected = baseline.get("door_ids")
        if not isinstance(expected, list):
            check.fail("baseline 'door_ids' is missing or not a list")
        elif ids != sorted(expected):
            check.fail_lines(describe_diff(expected, ids, 30))
        if baseline.get("door_id_count") != len(ids):
            check.fail(
                "expected door_id_count: %r" % baseline.get("door_id_count")
            )
            check.fail("actual door_id_count:   %d" % len(ids))

    return check, ids


def extract_result_item(data) -> Optional[str]:
    if not isinstance(data, dict):
        return None
    result = data.get("result")
    if isinstance(result, str):
        return result
    if isinstance(result, dict):
        item = result.get("item")
        if isinstance(item, str):
            return item
        item_id = result.get("id")
        if isinstance(item_id, str):
            return item_id
    return None


def check_recipe_results(
    baseline: Optional[dict], door_ids: Sequence[str]
) -> Tuple[Check, List[str], int]:
    check = Check("recipe results stay in whitelisted ids")
    allowed = set(door_ids) | KNOWN_RECIPE_ITEMS
    result_ids: List[str] = []
    recipe_files: List[Path] = []

    if not GFBS_DOORS_RECIPES_DIR.is_dir():
        check.fail("expected: %s exists" % rel(GFBS_DOORS_RECIPES_DIR))
        check.fail("actual:   missing")
        return check, [], 0

    recipe_files = sorted(GFBS_DOORS_RECIPES_DIR.glob("*.json"))
    for path in recipe_files:
        try:
            data = read_json(path)
        except (OSError, UnicodeError, json.JSONDecodeError) as exc:
            check.fail("expected: valid UTF-8 JSON in %s" % rel(path))
            check.fail("actual:   %s" % exc)
            continue

        item = extract_result_item(data)
        if not item:
            check.fail("expected: result.item in %s" % rel(path))
            check.fail("actual:   missing or unsupported result shape")
            continue

        if item.startswith("gfbs_doors:"):
            result_id = item.split(":", 1)[1]
            if result_id not in allowed:
                check.fail(
                    "expected result id in door_ids or known items %s"
                    % fmt(sorted(allowed), 24)
                )
                check.fail("actual:   %s -> %s" % (rel(path), item))
            result_ids.append(item)

    check.note(
        "recipe files: %d; gfbs_doors result ids: %d"
        % (len(recipe_files), len(result_ids))
    )

    if baseline is not None:
        expected = baseline.get("recipe_result_ids")
        actual = sorted(set(result_ids))
        if not isinstance(expected, list):
            check.fail("baseline 'recipe_result_ids' is missing or not a list")
        elif actual != sorted(expected):
            check.fail_lines(describe_diff(expected, actual, 24))

        counts = baseline.get("counts")
        expected_recipe_count = (
            counts.get("recipe_files") if isinstance(counts, dict) else None
        )
        if expected_recipe_count != len(recipe_files):
            check.fail("expected recipe_files: %r" % expected_recipe_count)
            check.fail("actual recipe_files:   %d" % len(recipe_files))

    return check, sorted(set(result_ids)), len(recipe_files)


def check_pickaxe(
    baseline: Optional[dict], door_ids: Sequence[str]
) -> Tuple[Check, List[str], List[str]]:
    check = Check("mineable/pickaxe tag completeness")
    required = (
        {"gfbs_doors:%s" % door_id for door_id in door_ids}
        | REQUIRED_CABIN_ELEVATOR_BLOCKS
    )
    required_sorted = sorted(required)

    if not PICKAXE_TAG_FILE.is_file():
        check.fail("expected: %s exists" % rel(PICKAXE_TAG_FILE))
        check.fail("actual:   missing")
        return check, [], required_sorted

    try:
        data = read_json(PICKAXE_TAG_FILE)
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        check.fail("expected: valid UTF-8 JSON in %s" % rel(PICKAXE_TAG_FILE))
        check.fail("actual:   %s" % exc)
        return check, [], required_sorted

    values = data.get("values") if isinstance(data, dict) else None
    if not isinstance(values, list):
        check.fail("expected: top-level 'values' array in %s" % rel(PICKAXE_TAG_FILE))
        check.fail("actual:   %r" % type(values).__name__)
        return check, [], required_sorted

    entries: List[str] = []
    for index, value in enumerate(values):
        if isinstance(value, str):
            entries.append(value)
        elif isinstance(value, dict) and isinstance(value.get("id"), str):
            entries.append(value["id"])
        else:
            check.fail(
                "expected: string or {'id': ...} at %s values[%d]"
                % (rel(PICKAXE_TAG_FILE), index)
            )
            check.fail("actual:   %r" % (value,))

    unique_entries = sorted(set(entries))
    missing = sorted(required - set(entries))
    if missing:
        check.fail(
            "expected: all %d required entries present (%d door ids + door_frame + door_frame_tone)"
            % (len(required), len(door_ids))
        )
        check.fail("actual:   missing %d: %s" % (len(missing), fmt(missing, 30)))
        for item in missing:
            check.fail("          - %s" % item)

    check.note(
        "entries: %d (unique: %d); required: %d"
        % (len(entries), len(unique_entries), len(required))
    )

    if baseline is not None:
        expected_entries = baseline.get("pickaxe_entries")
        if not isinstance(expected_entries, list):
            check.fail("baseline 'pickaxe_entries' is missing or not a list")
        elif unique_entries != sorted(expected_entries):
            check.fail_lines(describe_diff(expected_entries, unique_entries, 30))

        expected_required = baseline.get("required_pickaxe_entries")
        if not isinstance(expected_required, list):
            check.fail("baseline 'required_pickaxe_entries' is missing or not a list")
        elif required_sorted != sorted(expected_required):
            check.fail("required_pickaxe_entries")
            check.fail_lines(describe_diff(expected_required, required_sorted, 30))

        expected_count = baseline.get("pickaxe_entry_count")
        if expected_count != len(unique_entries):
            check.fail("expected pickaxe_entry_count: %r" % expected_count)
            check.fail("actual pickaxe_entry_count:   %d" % len(unique_entries))

    return check, unique_entries, required_sorted


def check_single_channel_and_namespaces(
    baseline: Optional[dict],
) -> Tuple[Check, Dict[str, int], Dict[str, dict]]:
    check = Check("single channel / single @Mod / namespace holders")
    files = sorted(JAVA_ROOT.rglob("*.java")) if JAVA_ROOT.is_dir() else []

    channel_count = 0
    mod_count = 0
    channel_files: List[str] = []
    mod_files: List[str] = []

    for path in files:
        try:
            raw = read_text(path)
        except OSError:
            continue
        code = mask_java(raw, keep_strings=False)

        channels_here = len(
            re.findall(r"\bNetworkRegistry\s*\.\s*newSimpleChannel\s*\(", code)
        )
        mods_here = len(re.findall(r"@Mod\s*\(", code))
        channel_count += channels_here
        mod_count += mods_here
        if channels_here:
            channel_files.append("%s (%d)" % (rel(path), channels_here))
        if mods_here:
            mod_files.append("%s (%d)" % (rel(path), mods_here))

    if channel_count != 1:
        check.fail("expected: exactly 1 NetworkRegistry.newSimpleChannel(...)")
        check.fail("actual:   %d in %s" % (channel_count, fmt(channel_files, 12)))
    if mod_count != 1:
        check.fail("expected: exactly 1 @Mod(...) annotation")
        check.fail("actual:   %d in %s" % (mod_count, fmt(mod_files, 12)))

    constants: Dict[str, dict] = {}
    for key, relative, expected_value in NAMESPACE_CONSTANT_FILES:
        path = PROJECT_ROOT / relative
        if not path.is_file():
            check.fail("expected: %s (%s namespace holder)" % (relative, key))
            check.fail("actual:   missing")
            continue
        try:
            raw = read_text(path)
        except OSError as exc:
            check.fail("expected: readable %s" % relative)
            check.fail("actual:   %s" % exc)
            continue
        code = mask_java(raw, keep_strings=True)
        match = re.search(r'\bMODID\s*=\s*"((?:\\.|[^"\\])*)"\s*;', code)
        if not match:
            check.fail("expected: %s contains MODID = \"%s\"" % (relative, expected_value))
            check.fail("actual:   MODID assignment not found")
            continue
        actual_value = java_unescape(match.group(1))
        if actual_value != expected_value:
            check.fail("expected: %s MODID = %r" % (relative, expected_value))
            check.fail("actual:   MODID = %r" % actual_value)
            continue
        constants[key] = {"file": relative, "value": actual_value}

    check.note(
        "newSimpleChannel: %d; @Mod(: %d; namespace constants: %s"
        % (channel_count, mod_count, fmt(sorted(constants.keys())))
    )
    for key in sorted(constants):
        check.note("  %s: %s -> %s" % (key, constants[key]["file"], constants[key]["value"]))

    if baseline is not None:
        counts = baseline.get("counts", {})
        if not isinstance(counts, dict):
            check.fail("baseline 'counts' is missing or not an object")
        else:
            for key, actual in (
                ("network_channels", channel_count),
                ("mod_annotations", mod_count),
            ):
                expected = counts.get(key)
                if expected != actual:
                    check.fail("expected counts.%s: %r" % (key, expected))
                    check.fail("actual counts.%s:   %r" % (key, actual))

        expected_constants = baseline.get("namespace_constants")
        if not isinstance(expected_constants, dict):
            check.fail("baseline 'namespace_constants' is missing or not an object")
        elif expected_constants != constants:
            check.fail(
                "expected namespace_constants: %s" % json.dumps(expected_constants, ensure_ascii=False, sort_keys=True)
            )
            check.fail(
                "actual namespace_constants:   %s" % json.dumps(constants, ensure_ascii=False, sort_keys=True)
            )

    return check, {"channels": channel_count, "mods": mod_count}, constants


# ---------------------------------------------------------------------------
# Baseline snapshot / main
# ---------------------------------------------------------------------------


def count_json_files(directory: Path) -> int:
    if not directory.is_dir():
        return 0
    return sum(1 for path in directory.glob("*.json") if path.is_file())


def build_snapshot(
    java_namespaces: Sequence[str],
    rl_uses: Sequence[dict],
    roots: Dict[str, List[str]],
    door_ids: Sequence[str],
    recipe_result_ids: Sequence[str],
    recipe_files: int,
    pickaxe_entries: Sequence[str],
    required_pickaxe: Sequence[str],
    channel_info: Dict[str, int],
    namespace_constants: Dict[str, dict],
) -> dict:
    door_blockstates = count_json_files(GFBS_DOORS_BLOCKSTATES_DIR)
    door_recipes = count_json_files(GFBS_DOORS_RECIPES_DIR)

    counts = {
        "java_resource_location_calls": len(rl_uses),
        "java_namespaces": len(java_namespaces),
        "assets_namespaces": len(roots["assets"]),
        "data_namespaces": len(roots["data"]),
        "door_ids": len(door_ids),
        "door_blockstates": door_blockstates,
        "door_recipes": door_recipes,
        "recipe_files": recipe_files,
        "recipe_gfbs_doors_results": len(recipe_result_ids),
        "pickaxe_entries": len(pickaxe_entries),
        "required_pickaxe_entries": len(required_pickaxe),
        "network_channels": channel_info["channels"],
        "mod_annotations": channel_info["mods"],
        "namespace_constants": len(namespace_constants),
    }

    return {
        "schema_version": SCHEMA_VERSION,
        "project": PROJECT_NAME,
        "generated_by": "tools/compat_check.py --write-baseline",
        "java_namespaces": sorted(java_namespaces),
        "resource_roots": {
            "assets": sorted(roots["assets"]),
            "data": sorted(roots["data"]),
        },
        "door_ids": sorted(door_ids),
        "door_id_count": len(door_ids),
        "namespace_constants": namespace_constants,
        "known_recipe_items": sorted(KNOWN_RECIPE_ITEMS),
        "recipe_result_ids": sorted(recipe_result_ids),
        "pickaxe_entries": sorted(pickaxe_entries),
        "pickaxe_entry_count": len(pickaxe_entries),
        "required_pickaxe_entries": sorted(required_pickaxe),
        "counts": counts,
    }


def load_baseline() -> Tuple[Optional[dict], Optional[str]]:
    if not BASELINE_PATH.is_file():
        return None, (
            "baseline not found: %s\n"
            "       run: python tools/compat_check.py --write-baseline" % rel(BASELINE_PATH)
        )
    try:
        data = read_json(BASELINE_PATH)
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        return None, "cannot read %s: %s" % (rel(BASELINE_PATH), exc)
    if not isinstance(data, dict):
        return None, "baseline root is not a JSON object: %s" % rel(BASELINE_PATH)
    if data.get("schema_version") != SCHEMA_VERSION:
        return None, (
            "baseline schema_version is %r, expected %r; refresh the baseline"
            % (data.get("schema_version"), SCHEMA_VERSION)
        )
    return data, None


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        description=(
            "GFBS-Misc compatibility red-line check: Java/RESOURCE namespaces, "
            "door ids, pickaxe tag and single-channel invariants."
        )
    )
    parser.add_argument(
        "--write-baseline",
        action="store_true",
        help="write tools/compat_baseline.json from the current tree",
    )
    args = parser.parse_args(argv)

    # Keep the console's native encoding (so Chinese paths render correctly in
    # cmd/PowerShell) while avoiding a hard crash on unencodable characters.
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(errors="replace")
        except Exception:
            pass

    baseline: Optional[dict] = None
    if not args.write_baseline:
        baseline, baseline_error = load_baseline()
        if baseline_error:
            print("=== GFBS-Misc compatibility check ===")
            print("project: %s" % PROJECT_ROOT)
            print("mode:    verify baseline")
            print()
            print("[FAIL] baseline file")
            for line in baseline_error.splitlines():
                print("       %s" % line)
            print()
            print("RESULT: FAIL (1 check failed)")
            return 1

    checks: List[Check] = []

    if baseline is not None:
        checks.append(check_baseline_metadata(baseline))

    java_check, java_namespaces, rl_uses = check_java_namespaces(baseline)
    checks.append(java_check)

    roots_check, roots = check_resource_roots(baseline)
    checks.append(roots_check)

    door_check, door_ids = check_door_ids(baseline)
    checks.append(door_check)

    recipe_check, recipe_result_ids, recipe_file_count = check_recipe_results(
        baseline, door_ids
    )
    checks.append(recipe_check)

    pickaxe_check, pickaxe_entries, required_pickaxe = check_pickaxe(baseline, door_ids)
    checks.append(pickaxe_check)

    channel_check, channel_info, namespace_constants = (
        check_single_channel_and_namespaces(baseline)
    )
    checks.append(channel_check)

    snapshot = build_snapshot(
        java_namespaces=java_namespaces,
        rl_uses=rl_uses,
        roots=roots,
        door_ids=door_ids,
        recipe_result_ids=recipe_result_ids,
        recipe_files=recipe_file_count,
        pickaxe_entries=pickaxe_entries,
        required_pickaxe=required_pickaxe,
        channel_info=channel_info,
        namespace_constants=namespace_constants,
    )

    print("=== GFBS-Misc compatibility check ===")
    print("project: %s" % PROJECT_ROOT)
    print("mode:    %s" % ("write baseline" if args.write_baseline else "verify baseline"))
    print()
    for check in checks:
        print_check(check)

    failed = [check for check in checks if not check.ok]
    print()

    if args.write_baseline:
        if failed:
            print(
                "RESULT: FAIL (%d check(s) failed); baseline was NOT written"
                % len(failed)
            )
            return 1
        text = json.dumps(
            snapshot,
            ensure_ascii=False,
            indent=2,
            sort_keys=True,
        ) + "\n"
        try:
            with BASELINE_PATH.open("w", encoding="utf-8", newline="\n") as handle:
                handle.write(text)
        except OSError as exc:
            print("RESULT: FAIL; cannot write %s: %s" % (rel(BASELINE_PATH), exc))
            return 1
        print("baseline written: %s" % rel(BASELINE_PATH))
        print(
            "recorded: door_ids=%d, namespaces=%d, pickaxe_entries=%d, "
            "recipe_results=%d"
            % (
                snapshot["counts"]["door_ids"],
                snapshot["counts"]["java_namespaces"],
                snapshot["counts"]["pickaxe_entries"],
                snapshot["counts"]["recipe_gfbs_doors_results"],
            )
        )
        print("RESULT: OK")
        return 0

    if failed:
        print("RESULT: FAIL (%d check(s) failed)" % len(failed))
        return 1

    print("RESULT: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
