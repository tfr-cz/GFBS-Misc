#!/usr/bin/env python3
"""Push local commits to GitHub through the Git Data API.

The git CLI cannot push from this machine (schannel fails with
CRYPT_E_NO_REVOCATION_CHECK and the openssl backend has no CA bundle), so the
objects are uploaded directly. Blobs and trees are content addressed, so
rebuilding them this way yields byte-identical object ids and the new remote
commit keeps the same sha as the local one.

Usage:  python tools/push_gitdata.py [--remote origin] [--branch master] [--dry-run]
"""

from __future__ import annotations

import argparse
import base64
import json
import subprocess
import sys


def git(*args: str, binary: bool = False):
    out = subprocess.run(["git", *args], capture_output=True, check=True).stdout
    return out if binary else out.decode("utf-8", "replace")


def gh(*args: str, payload: dict | None = None, method: str | None = None):
    cmd = ["gh", "api", *args]
    if method:
        cmd += ["-X", method]
    if payload is not None:
        cmd += ["--input", "-"]
    proc = subprocess.run(
        cmd,
        input=json.dumps(payload).encode() if payload is not None else None,
        capture_output=True,
    )
    if proc.returncode != 0:
        raise SystemExit(
            f"gh api {' '.join(args)} failed:\n{proc.stderr.decode('utf-8', 'replace')}"
        )
    text = proc.stdout.decode("utf-8", "replace").strip()
    return json.loads(text) if text else {}


def repo_slug(remote: str) -> str:
    url = git("remote", "get-url", remote).strip()
    for prefix in ("https://github.com/", "git@github.com:"):
        if url.startswith(prefix):
            return url[len(prefix) :].removesuffix(".git")
    raise SystemExit(f"cannot derive a github repo from remote url: {url}")


def iso_date(value: str) -> str:
    # 2026-11-15T12:34:56+08:00 is already what the API wants.
    return value.strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--remote", default="origin")
    ap.add_argument("--branch", default="master")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    slug = repo_slug(args.remote)
    local = git("rev-parse", "HEAD").strip()
    ref = gh(f"repos/{slug}/git/ref/heads/{args.branch}")
    remote = ref["object"]["sha"]

    if local == remote:
        print(f"up to date: {args.branch} is {local[:7]}")
        return 0

    ahead = git("rev-list", "--count", f"{remote}..{local}").strip()
    behind = git("rev-list", "--count", f"{local}..{remote}").strip()
    if behind != "0":
        raise SystemExit(
            f"remote {args.branch} has {behind} commit(s) the local branch lacks; "
            "reconcile by hand, this script only fast-forwards"
        )
    print(f"{slug}: {remote[:7]} -> {local[:7]} ({ahead} commit(s))")

    # Everything reachable from local but not from remote: exactly the new blobs,
    # trees and commits. Trees come out with their paths, blobs too.
    listing = git("rev-list", "--objects", f"{remote}..{local}")
    objects: list[tuple[str, str]] = []
    for line in listing.splitlines():
        sha, _, path = line.partition(" ")
        objects.append((sha, path))

    kinds: dict[str, list[tuple[str, str]]] = {"blob": [], "tree": [], "commit": []}
    for sha, path in objects:
        kinds[git("cat-file", "-t", sha).strip()].append((sha, path))

    print(
        f"objects: {len(kinds['blob'])} blob(s), {len(kinds['tree'])} tree(s), "
        f"{len(kinds['commit'])} commit(s)"
    )
    if args.dry_run:
        for kind in ("blob", "tree", "commit"):
            for sha, path in kinds[kind]:
                print(f"  {kind:6} {sha[:12]} {path}")
        return 0

    for sha, path in kinds["blob"]:
        data = git("cat-file", "blob", sha, binary=True)
        made = gh(
            f"repos/{slug}/git/blobs",
            method="POST",
            payload={
                "content": base64.b64encode(data).decode("ascii"),
                "encoding": "base64",
            },
        )
        if made["sha"] != sha:
            raise SystemExit(
                f"blob {path} came back as {made['sha']} instead of {sha}; "
                "the content github stored differs from the local object"
            )
        print(f"  blob {sha[:10]} {path}")

    # Deepest trees first: a tree may only reference trees that already exist.
    for sha, path in sorted(kinds["tree"], key=lambda kv: -kv[1].count("/")):
        entries = []
        for line in git("ls-tree", sha).splitlines():
            meta, _, name = line.partition("\t")
            mode, kind, child = meta.split()
            entries.append(
                {"path": name, "mode": mode, "type": kind, "sha": child}
            )
        made = gh(
            f"repos/{slug}/git/trees",
            method="POST",
            payload={"tree": entries},
        )
        if made["sha"] != sha:
            raise SystemExit(
                f"tree {path or '.'} came back as {made['sha']} instead of {sha}"
            )
        print(f"  tree {sha[:10]} {path or '.'} ({len(entries)} entries)")

    # Commits are replayed in ancestry order so each parent already exists.
    for sha, _ in sorts_commits(kinds["commit"], local):
        raw = git("cat-file", "commit", sha)
        head, _, message = raw.partition("\n\n")
        tree = ""
        parents: list[str] = []
        author = committer = ""
        for line in head.splitlines():
            if line.startswith("tree "):
                tree = line[5:]
            elif line.startswith("parent "):
                parents.append(line[7:])
            elif line.startswith("author "):
                author = line[7:]
            elif line.startswith("committer "):
                committer = line[10:]
        payload = {
            "message": message,
            "tree": tree,
            "parents": parents,
            "author": split_ident(author),
            "committer": split_ident(committer),
        }
        made = gh(f"repos/{slug}/git/commits", method="POST", payload=payload)
        if made["sha"] != sha:
            raise SystemExit(
                f"commit came back as {made['sha']} instead of {sha}"
            )
        print(f"  commit {sha[:10]} {message.splitlines()[0][:60]}")

    gh(
        f"repos/{slug}/git/refs/heads/{args.branch}",
        method="PATCH",
        payload={"sha": local, "force": False},
    )
    print(f"pushed: {slug} {args.branch} is now {local[:7]}")
    return 0


def split_ident(value: str) -> dict[str, str]:
    # "Name <mail> 1763201234 +0800"
    name, _, rest = value.partition(" <")
    mail, _, dates = rest.partition("> ")
    stamp, _, offset = dates.strip().partition(" ")
    from datetime import datetime, timedelta, timezone

    tz = timezone(timedelta(hours=int(offset[:3]))) if offset else timezone.utc
    moment = datetime.fromtimestamp(int(stamp), tz)
    return {
        "name": name,
        "email": mail,
        "date": iso_date(moment.isoformat()),
    }


def sorts_commits(commits: list[tuple[str, str]], head: str):
    """Order commits oldest-first along the chain ending at head."""
    wanted = {sha for sha, _ in commits}
    order: list[str] = []
    stack = [head]
    while stack:
        sha = stack.pop()
        if sha not in wanted or sha in order:
            continue
        for line in git("cat-file", "commit", sha).splitlines():
            if line.startswith("parent "):
                stack.append(line[7:])
        order.append(sha)
    return [(sha, "") for sha in reversed(order)]


if __name__ == "__main__":
    sys.exit(main())
