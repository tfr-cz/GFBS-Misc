#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Null-safety red lines for the Java sources.

Built after a crash: a worn-set lookup keyed with ``List.of(...)`` called
``contains(null)`` to detect empty armour slots. Both halves throw
``NullPointerException`` - the factory rejects null elements, and an immutable
list throws from ``contains(null)`` - so every attack on a player crashed a
server tick.

The two rules below are mechanical and have no false positives, so they run as a
gate rather than living in a coding-standards document:

1. ``<expr>.contains(null)`` / ``.indexOf(null)`` - a JDK immutable collection
   throws on a null argument, and on a mutable one the check is nearly always a
   mistake about whether emptiness is possible.
2. ``List.of(...)`` / ``Set.of(...)`` / ``Map.of(...)`` with a literal ``null``
   argument - the factory throws immediately.

Collections that genuinely hold nulls are fine; they just need a mutable or
array-backed implementation (``Arrays.asList``, ``HashMap``), which keeps them
out of these rules.

Usage (the project root is inferred from this file's location):

    python tools/null_safety_check.py
    python tools/null_safety_check.py --root path/to/tree   # for trying it out

Exit code: 0 when everything is OK, 1 when any rule fires.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = TOOLS_DIR.parent
JAVA_ROOT = PROJECT_ROOT / "src" / "main" / "java"

CONTAINS_NULL = re.compile(r"\.\s*(?:contains|indexOf)\s*\(\s*null\s*\)")
FACTORY_NULL = re.compile(r"\b(?:List|Set|Map)\.of\s*\([^)]*\bnull\b")

RULES = (
    ("contains(null)", CONTAINS_NULL,
     "an immutable collection throws NPE on a null argument; check emptiness instead"),
    ("List.of/Set.of/Map.of(null)", FACTORY_NULL,
     "the immutable factories reject null elements; use a record, Arrays.asList or a HashMap"),
)


def strip_comments(text: str) -> str:
    """Blanks out // and /* */ comments so prose about the rules cannot trip them."""
    out = []
    in_block = False
    for line in text.splitlines():
        if in_block:
            end = line.find("*/")
            if end < 0:
                out.append("")
                continue
            line = " " * (end + 2) + line[end + 2:]
            in_block = False
        start = line.find("/*")
        if start >= 0 and line.find("*/", start) < 0:
            line = line[:start]
            in_block = True
        line = re.sub(r"//.*$", "", line)
        out.append(line)
    return "\n".join(out)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Null-safety red lines for Java sources.")
    parser.add_argument("--root", type=Path, default=None,
                        help="tree to scan (defaults to the project root)")
    args = parser.parse_args(argv)

    java_root = (args.root / "src" / "main" / "java") if args.root else JAVA_ROOT
    if not java_root.is_dir():
        print("no Java sources at %s" % java_root)
        print("RESULT: FAIL")
        return 1

    failed = 0
    scanned = 0
    for path in sorted(java_root.rglob("*.java")):
        scanned += 1
        lines = strip_comments(path.read_text(encoding="utf-8")).splitlines()
        for number, line in enumerate(lines, start=1):
            for label, pattern, hint in RULES:
                if pattern.search(line):
                    failed += 1
                    print("[FAIL] %s:%d  %s\n       %s\n       %s"
                          % (path.relative_to(java_root), number, label,
                             line.strip(), hint))

    if failed == 0:
        print("[OK] no null-hostile collection use in %d Java file(s)" % scanned)
        print("RESULT: OK")
        return 0

    print("RESULT: FAIL (%d violation(s) in %d file(s))" % (failed, scanned))
    return 1


if __name__ == "__main__":
    sys.exit(main())
