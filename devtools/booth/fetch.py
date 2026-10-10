# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
"""Fill a booth's game directory with what the pack plays with where it touches armour.

    uv run --no-project python devtools/booth/fetch.py [--gpu] [pack-url]

- run/booth (the default) is the automated booth, in software on Xephyr: shaders off, Pixlli left out
  (an atlas that size is past llvmpipe).
- run/gpubooth (--gpu) is the photographs', on the 4070: Iris and Complementary with Rusty's own
  settings (LabPBR), Pixlli 128x, the light the kits are judged in (D-0002).

Both carry the mods that fight over armour and hands:
- EMF, ETF, Armored Legacy and its no-tassets patch (the worn armour every un-kitted piece keeps);
- Modefite, 3D Default (the item models a kitted item must win over, D-0003);
- Not Enough Animations, Hold My Items, player-animation-lib (first person);
- Set It Down (armour set down on a stand of its own), Serfdom (villagers in armour).

Mods and packs come from the published pack on the Mod Hub, checked against its sha1s; override jars
and folders from its overrides. Fresh Animations and its player extension are Rusty's own additions,
not the pack's, and are copied from his Prism instance, for the booth alone. Then the options and
Iris's settings are written, the resource packs in the order Rusty's game has them.
"""
import hashlib
import io
import json
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

HUB = "http://192.168.0.203:8080/pack.mrpack"
REPO = Path(__file__).resolve().parents[2]
PRISM = Path.home() / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/Minecraft/minecraft"
MODS = ("iris-neoforge", "sodium-neoforge", "entity_model_features", "entity_texture_features", "modefite",
        "notenoughanimations", "holdmyitems", "player-animation-lib", "setitdown", "serfdom")
SHADER = "ComplementaryUnbound_r5.8.1.zip"
# Lowest priority first, as options.txt lists them.
PACKS = ["3D Default 1.20+ v1.15.0.zip", "Armored Legacy 1.5.1.zip", "Armored-Legacy-No-Tassets"]
PIXLLI = "Pixlli V57 26.3-1.13 128x.zip"
LOCAL = ["FreshAnimations_v1.10.4.zip", "FA+Player-v1.1.zip"]


def fetch(url):
    with urllib.request.urlopen(url, timeout=300) as r:
        return r.read()


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    gpu = "--gpu" in sys.argv
    booth = REPO / "run" / ("gpubooth" if gpu else "booth")
    packs = ([PIXLLI] if gpu else []) + PACKS
    pack = zipfile.ZipFile(io.BytesIO(fetch(args[0] if args else HUB)))
    index = json.loads(pack.read("modrinth.index.json"))
    for sub in ("mods", "resourcepacks", "shaderpacks", "config"):
        (booth / sub).mkdir(parents=True, exist_ok=True)
    for f in index["files"]:
        name, folder = f["path"].split("/")[-1], f["path"].split("/")[0]
        if not ((folder == "mods" and name.startswith(MODS)) or (folder == "resourcepacks" and name in packs)
                or (folder == "shaderpacks" and name == SHADER)):
            continue
        target = booth / f["path"]
        if target.exists() and hashlib.sha1(target.read_bytes()).hexdigest() == f["hashes"]["sha1"]:
            print("have", f["path"])
            continue
        data = fetch(f["downloads"][0])
        if hashlib.sha1(data).hexdigest() != f["hashes"]["sha1"]:
            sys.exit(f"sha1 mismatch for {f['path']}")
        target.write_bytes(data)
        print("got ", f["path"], len(data))
    for member in pack.namelist():
        if not member.startswith("overrides/") or member.endswith("/"):
            continue
        rel = member[len("overrides/"):]
        folder, name = rel.split("/")[0], rel.split("/")[1] if "/" in rel else ""
        if (folder == "mods" and name.startswith(MODS)) or (folder == "resourcepacks" and name in packs):
            out = booth / rel
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(pack.read(member))
    for name in LOCAL:
        shutil.copyfile(PRISM / "resourcepacks" / name, booth / "resourcepacks" / name)
        print("copied", name)
    if gpu:
        shutil.copyfile(PRISM / "shaderpacks" / (SHADER + ".txt"), booth / "shaderpacks" / (SHADER + ".txt"))
    missing = [p for p in packs + LOCAL if not (booth / "resourcepacks" / p).exists()]
    have = {p.name for p in (booth / "mods").iterdir()}
    missing += [m for m in MODS if not any(h.startswith(m) for h in have)]
    if missing:
        sys.exit(f"not in the pack: {missing}")
    (booth / "config" / "iris.properties").write_text(
        f"enableShaders={'true' if gpu else 'false'}\nshaderPack={SHADER}\ndisableUpdateMessage=true\nmaxShadowRenderDistance=8\n")
    order = ["vanilla", "mod_resources"] + ["file/" + p for p in packs + LOCAL]
    listed = ",".join(json.dumps(p) for p in order)
    (booth / "options.txt").write_text(
        "onboardAccessibility:false\nnarrator:0\nsoundCategory_master:0.0\nrenderDistance:4\nsimulationDistance:5\n"
        "graphicsMode:1\nmaxFps:60\nguiScale:2\nentityShadows:true\nfov:0.0\nbobView:false\ntutorialStep:none\n"
        f"resourcePacks:[{listed}]\nincompatibleResourcePacks:[]\n")
    print(f"wrote {booth}/options.txt and config/iris.properties (shaders {'on' if gpu else 'off'})")


main()
