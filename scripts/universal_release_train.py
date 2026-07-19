#!/usr/bin/env python3
"""Plan and track synchronized universal Android/database releases."""

from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import subprocess
from pathlib import Path
from typing import Any

APP_EXCLUDES = (
    ".release/universal-state.json",
    "RELEASE_STATUS.md",
    "updates/app-update*.json",
    "releases/**",
)
DATABASE_EXCLUDES = ("public/**",)

DEFAULT_STATE: dict[str, Any] = {
    "schema_version": 1,
    "version_name": "0.2.2",
    "version_code": 4,
    "channel": "unreleased",
    "app_content_hash": "",
    "database_content_hash": "",
}


def git_files(repo: Path) -> list[str]:
    output = subprocess.check_output(["git", "-C", str(repo), "ls-files", "-z"])
    return sorted(item.decode("utf-8") for item in output.split(b"\0") if item)


def excluded(path: str, patterns: tuple[str, ...]) -> bool:
    return any(fnmatch.fnmatch(path, pattern) for pattern in patterns)


def content_hash(repo: Path, patterns: tuple[str, ...]) -> str:
    digest = hashlib.sha256()
    for relative in git_files(repo):
        if excluded(relative, patterns):
            continue
        file_path = repo / relative
        if not file_path.is_file():
            continue
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(file_path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def load_state(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return dict(DEFAULT_STATE)
    loaded = json.loads(path.read_text(encoding="utf-8"))
    return {**DEFAULT_STATE, **loaded}


def save_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def bump_minor(version: str) -> str:
    numeric = version.split("-", 1)[0].split("+")[0]
    parts = numeric.split(".")
    if len(parts) < 2:
        raise ValueError(f"Version must contain at least major.minor: {version}")
    major = int(parts[0])
    minor = int(parts[1])
    return f"{major}.{minor + 1}.0"


def make_plan(app_repo: Path, database_repo: Path, action: str) -> dict[str, Any]:
    state_path = app_repo / ".release/universal-state.json"
    state = load_state(state_path)
    app_hash = content_hash(app_repo, APP_EXCLUDES)
    database_hash = content_hash(database_repo, DATABASE_EXCLUDES)
    app_changed = app_hash != state["app_content_hash"]
    database_changed = database_hash != state["database_content_hash"]
    source_changed = app_changed or database_changed

    target_version = bump_minor(state["version_name"]) if source_changed else state["version_name"]
    target_version_code = int(state["version_code"]) + 1
    current_channel = str(state["channel"])

    promote_only = action == "publish" and not source_changed and current_channel == "prerelease"
    no_op = (
        action == "prerelease" and not source_changed
    ) or (
        action == "publish" and not source_changed and current_channel != "prerelease"
    )

    build_data = action == "build-only" or source_changed
    build_app = action == "build-only" or source_changed or promote_only
    data_release_needed = action in {"prerelease", "publish"} and source_changed
    app_release_needed = action in {"prerelease", "publish"} and (source_changed or promote_only)
    target_channel = "stable" if action == "publish" else "prerelease"

    return {
        "action": action,
        "current_version": state["version_name"],
        "current_version_code": int(state["version_code"]),
        "current_channel": current_channel,
        "target_version": target_version,
        "target_version_code": target_version_code,
        "target_channel": target_channel,
        "app_content_hash": app_hash,
        "database_content_hash": database_hash,
        "app_changed": app_changed,
        "database_changed": database_changed,
        "source_changed": source_changed,
        "promote_only": promote_only,
        "no_op": no_op,
        "build_data": build_data,
        "build_app": build_app,
        "data_release_needed": data_release_needed,
        "app_release_needed": app_release_needed,
    }


def render_status(plan: dict[str, Any]) -> str:
    changed = "Yes" if plan["source_changed"] else "No"
    app_changed = "Yes" if plan["app_changed"] else "No"
    database_changed = "Yes" if plan["database_changed"] else "No"

    if plan["source_changed"]:
        version_note = (
            f"The next visible version will be **{plan['target_version']}** "
            f"(a +0.1 minor bump from {plan['current_version']})."
        )
    elif plan["current_channel"] == "prerelease":
        version_note = (
            f"No source change was detected. Stable publish will promote "
            f"**{plan['current_version']}** without changing the visible version."
        )
    else:
        version_note = (
            f"No source change was detected, so the visible version remains "
            f"**{plan['current_version']}**."
        )

    return f"""# Universal release status

This file is generated from the tracked Android and database source content.
Generated manifests, release state and public release output are excluded from change detection.

| Item | Value |
|---|---|
| Current visible version | `{plan['current_version']}` |
| Current Android version code | `{plan['current_version_code']}` |
| Current channel | `{plan['current_channel']}` |
| Source changes detected | **{changed}** |
| Android source changed | {app_changed} |
| Database source changed | {database_changed} |
| Planned visible version | **`{plan['target_version']}`** |
| Planned Android version code | `{plan['target_version_code']}` |

{version_note}

## One universal application

The APK always supports both user data modes:

- **Minimalist** downloads and displays the text database only.
- **Images** downloads the same text database plus the optional verified image package.

Theme selection remains independent from the data mode.

## Manual workflow choices

- **build-only** builds the universal debug APK and database artifacts without reserving the version.
- **prerelease** publishes the paired APK and database as GitHub prereleases when source changes exist.
- **publish** publishes a stable pair when source changes exist, or promotes the current prerelease without a visible-version bump.

The Android version code can increase during prerelease-to-stable promotion because Android requires a higher internal build number for an installable update. The user-facing version name increases only when tracked source content changes.
"""


def command_plan(args: argparse.Namespace) -> None:
    plan = make_plan(Path(args.app), Path(args.database), args.action)
    save_json(Path(args.output), plan)
    Path(args.markdown).write_text(render_status(plan), encoding="utf-8")


def command_status(args: argparse.Namespace) -> None:
    plan = make_plan(Path(args.app), Path(args.database), "build-only")
    Path(args.output).write_text(render_status(plan), encoding="utf-8")


def command_update_state(args: argparse.Namespace) -> None:
    state = {
        "schema_version": 1,
        "version_name": args.version,
        "version_code": args.version_code,
        "channel": args.channel,
        "app_content_hash": args.app_hash,
        "database_content_hash": args.database_hash,
    }
    save_json(Path(args.state), state)


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    commands = root.add_subparsers(dest="command", required=True)

    plan = commands.add_parser("plan")
    plan.add_argument("--app", required=True)
    plan.add_argument("--database", required=True)
    plan.add_argument("--action", choices=("build-only", "prerelease", "publish"), required=True)
    plan.add_argument("--output", required=True)
    plan.add_argument("--markdown", required=True)
    plan.set_defaults(func=command_plan)

    status = commands.add_parser("status")
    status.add_argument("--app", required=True)
    status.add_argument("--database", required=True)
    status.add_argument("--output", required=True)
    status.set_defaults(func=command_status)

    update = commands.add_parser("update-state")
    update.add_argument("--state", required=True)
    update.add_argument("--version", required=True)
    update.add_argument("--version-code", type=int, required=True)
    update.add_argument("--channel", choices=("prerelease", "stable"), required=True)
    update.add_argument("--app-hash", required=True)
    update.add_argument("--database-hash", required=True)
    update.set_defaults(func=command_update_state)

    return root


def main() -> None:
    args = parser().parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
