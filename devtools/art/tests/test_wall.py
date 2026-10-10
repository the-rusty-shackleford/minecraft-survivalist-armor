"""The clean-room wall (D-0002): Pixlli is measured into numbers, and nothing that paints a kit ever reads it.

measure.py alone opens Pixlli (Rusty's own copy, for personal use as its license allows) and writes
pixlli-style.json; the painter reads only that file. These tests hold the wall from both sides: no art
module imports the measurer or names a pack file, and the style file holds statistics, never pixels.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import ast
import json
from pathlib import Path

ART = Path(__file__).resolve().parents[1]
PAINTING = sorted([*ART.glob("kitlib/*.py"), *ART.glob("kits/*.py"), ART / "build.py", ART / "look.py"])


def test_nothing_that_paints_imports_the_measurer():
    for path in PAINTING:
        tree = ast.parse(path.read_text(), filename=str(path))
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                names = [a.name for a in node.names]
            elif isinstance(node, ast.ImportFrom):
                names = [node.module or ""]
            else:
                continue
            assert not any(n.split(".")[-1] == "measure" for n in names), f"{path.name} imports the measurer"
            assert not any(n.split(".")[0] in ("zipfile", "tarfile") for n in names), f"{path.name} opens archives"


def test_nothing_that_paints_names_a_pack_file():
    # A string naming a zip, a pack's texture folder or a path through Pixlli would be a door through the wall; the
    # style file is the one Pixlli name allowed.
    for path in PAINTING:
        for node in ast.walk(ast.parse(path.read_text(), filename=str(path))):
            if isinstance(node, ast.Constant) and isinstance(node.value, str):
                s = node.value.lower()
                if "pixlli-style.json" in s:
                    continue
                assert ".zip" not in s and "assets/minecraft/textures" not in s and not ("pixlli" in s and "/" in s), \
                    f"{path.name}: {node.value[:60]!r}"


def test_the_style_file_holds_numbers_not_pixels():
    data = json.loads((ART / "pixlli-style.json").read_text())
    leaves = []

    def walk(x, path):
        if isinstance(x, dict):
            for k, v in x.items():
                walk(v, f"{path}.{k}")
        elif isinstance(x, list):
            assert len(x) <= 16, f"{path} holds {len(x)} entries: an image or a strip of one, not a statistic"
            for i, v in enumerate(x):
                walk(v, f"{path}[{i}]")
        else:
            leaves.append((path, x))

    walk(data, "style")
    for path, v in leaves:
        if isinstance(v, str):
            assert len(v) <= 80, f"{path} is a long string: {v[:40]}..."
    # A palette is eight colours at most per class; everything else is a scalar or a short range.
    assert len(leaves) < 1500, f"{len(leaves)} values: more than a measurement needs"
    for name, c in data["classes"].items():
        assert len(c.get("palette", [])) <= 8, name
