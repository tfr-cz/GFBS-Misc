#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Collects the game's runtime evidence into the repo so an agent can read it directly.

Until now every iteration that hit a runtime problem ended with "send me a screenshot / the crash
report". Minecraft already writes all of it - `logs/latest.log` and `crash-reports/crash-*.txt` - but
the instance lives outside the repo, so the agent had no way in.

The instance path is deliberately not hard-coded (the repo is public): it comes from `--instance`,
then `GFBS_INSTANCE`, then `.scratch/runtime.env`. Missing means it tells you exactly what to write.

Usage:

    python tools/pull_runtime.py                 # newest crash + last 400 log lines
    python tools/pull_runtime.py --tail 2000     # more log
    python tools/pull_runtime.py --debug         # also the debug.log tail (large)

Exit code: 0 when something was collected, 1 when the instance could not be read.
"""

from __future__ import annotations

import argparse
import os
import shutil
import sys
from datetime import datetime
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = TOOLS_DIR.parent
SCRATCH = PROJECT_ROOT / ".scratch"
ENV_FILE = SCRATCH / "runtime.env"
ENV_KEY = "GFBS_INSTANCE"


def read_env_file() -> str | None:
    if not ENV_FILE.is_file():
        return None
    for line in ENV_FILE.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        if key.strip() == ENV_KEY:
            return value.strip().strip('"').strip("'")
    return None


def resolve_instance(explicit: str | None) -> Path | None:
    for candidate in (explicit, os.environ.get(ENV_KEY), read_env_file()):
        if candidate:
            path = Path(candidate)
            if path.is_dir():
                return path
            print("instance not found: %s" % path)
            return None
    print("no instance configured.")
    print("  pass --instance PATH, or set %s, or create %s with:" % (ENV_KEY, ENV_FILE))
    print("    %s=G:/path/to/.minecraft/versions/<instance>" % ENV_KEY)
    return None


def tail_lines(path: Path, count: int) -> str:
    """Last `count` lines without loading a multi-megabyte log twice."""
    with path.open("rb") as handle:
        handle.seek(0, os.SEEK_END)
        size = handle.tell()
        block = 64 * 1024
        data = b""
        while size > 0 and data.count(b"\n") <= count:
            step = min(block, size)
            size -= step
            handle.seek(size)
            data = handle.read(step) + data
    text = data.decode("utf-8", "replace").splitlines()
    return "\n".join(text[-count:]) + "\n"


def newest_crash(instance: Path) -> Path | None:
    reports = sorted((instance / "crash-reports").glob("crash-*.txt"),
                     key=lambda p: p.stat().st_mtime, reverse=True)
    return reports[0] if reports else None


def summarise(stamp_dir: Path, latest: Path | None, crash: Path | None) -> str:
    lines = ["# runtime evidence %s" % stamp_dir.name, ""]
    if crash:
        lines.append("crash report: %s" % crash)
        for line in crash.read_text(encoding="utf-8", errors="replace").splitlines()[:12]:
            if line.strip():
                lines.append("  " + line)
        lines.append("")
    if latest and latest.is_file():
        text = latest.read_text(encoding="utf-8", errors="replace")
        lines.append("latest.log: null-pointer exceptions=%d, lines mentioning gfbs=%d"
                     % (text.count("NullPointerException"), text.count("gfbs")))
        interesting = [line for line in text.splitlines()
                       if ("gfbs" in line.lower() and ("error" in line.lower() or "exception" in line.lower()))]
        lines.append("gfbs errors/warnings: %d" % len(interesting))
        for line in interesting[-5:]:
            lines.append("  " + line[:200])
    return "\n".join(lines) + "\n"


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Collect Minecraft logs and crash reports.")
    parser.add_argument("--instance", default=None, help="instance directory (the one holding logs/)")
    parser.add_argument("--out", type=Path, default=None, help="output directory (default .scratch/runtime/<stamp>)")
    parser.add_argument("--tail", type=int, default=400, help="lines of latest.log to keep (default 400)")
    parser.add_argument("--debug", action="store_true", help="also keep the debug.log tail")
    args = parser.parse_args(argv)

    instance = resolve_instance(args.instance)
    if instance is None:
        return 1

    logs = instance / "logs"
    latest = logs / "latest.log"
    if not latest.is_file():
        print("no logs/latest.log under %s" % instance)
        return 1

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    stamp_dir = args.out or (SCRATCH / "runtime" / stamp)
    stamp_dir.mkdir(parents=True, exist_ok=True)

    crash = newest_crash(instance)
    if crash:
        shutil.copy2(crash, stamp_dir / crash.name)
    (stamp_dir / "latest.tail.log").write_text(tail_lines(latest, args.tail), encoding="utf-8")
    if args.debug and (logs / "debug.log").is_file():
        (stamp_dir / "debug.tail.log").write_text(tail_lines(logs / "debug.log", args.tail), encoding="utf-8")

    (stamp_dir / "summary.txt").write_text(summarise(stamp_dir, latest, crash), encoding="utf-8")

    print("collected into %s" % stamp_dir)
    for path in sorted(stamp_dir.iterdir()):
        print("  %-22s %8d B" % (path.name, path.stat().st_size))
    print()
    print((stamp_dir / "summary.txt").read_text(encoding="utf-8"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
