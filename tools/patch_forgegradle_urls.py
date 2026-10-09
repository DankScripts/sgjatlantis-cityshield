#!/usr/bin/env python3
"""Redirect ForgeGradle's hard-coded metadata URLs to the local build mirror."""

from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import os
import sys

OLD_META = b"https://piston-meta.mojang.com/mc/game/version_manifest.json"
OLD_META_LOCAL = b"http://127.0.0.1:48123/piston0/mc/game/version_manifest.json"
NEW_META = b"https://127.0.0.1:48123/piston/mc/game/version_manifest.json"
OLD_LIBS = b"https://libraries.minecraft.net/"
OLD_LIBS_LOCAL = b"http://127.0.0.1:48123/library0/"
NEW_LIBS = b"https://127.0.0.1:48123/library/"

assert len(OLD_META) == len(NEW_META)
assert len(OLD_LIBS) == len(NEW_LIBS)


def patch_jar(path: Path) -> bool:
    temporary = path.with_suffix(".patched.jar")
    changed = False
    with ZipFile(path, "r") as source, ZipFile(temporary, "w", ZIP_DEFLATED) as destination:
        for info in source.infolist():
            data = source.read(info.filename)
            patched = (data.replace(OLD_META, NEW_META).replace(OLD_META_LOCAL, NEW_META)
                       .replace(OLD_LIBS, NEW_LIBS).replace(OLD_LIBS_LOCAL, NEW_LIBS))
            changed |= patched != data
            destination.writestr(info, patched)
    if changed:
        os.replace(temporary, path)
    else:
        temporary.unlink(missing_ok=True)
    return changed


if __name__ == "__main__":
    root = Path(sys.argv[1])
    jars = list(root.rglob("*ForgeGradle*.jar")) + list(root.rglob("*forgegradle*.jar"))
    seen = set()
    for jar in jars:
        if jar in seen:
            continue
        seen.add(jar)
        if patch_jar(jar):
            print(f"patched {jar}")
