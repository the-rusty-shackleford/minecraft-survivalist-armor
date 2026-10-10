"""recipes.py: write the kits' recipes and names (D-0001, nfx's §6: scrap and hardware, not treasure).

    uv run --no-project python devtools/data/recipes.py

Per kit: its crafting recipe (one kit), its duplication (the kit and its scrap make two, as a trim
template and its material do), and its smithing recipe (the kit, any piece of armour, and its scrap
as the cost). One removal recipe: a kitted piece and shears. And the English names. The tables
below are the whole of it; the files are written from them.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/survivalistarmor/recipe"
LANG = ROOT / "src/main/resources/assets/survivalistarmor/lang/en_us.json"


def item(i: str) -> dict:
    return {"tag": i[1:]} if i.startswith("#") else {"item": i}


# kit: (title, pattern, key, the scrap the smithing table takes as the cost, its name, duplication scrap)
KITS = {
    "duster": ("Duster", ["LSL", "WDW", "LSL"],
               {"L": "minecraft:leather", "S": "minecraft:string", "W": "#minecraft:wool", "D": "minecraft:dried_kelp"},
               "minecraft:string", "String", "minecraft:leather"),
    "scrapper": ("Scrapper", ["NCN", "TCT", "NCN"],
                 {"N": "minecraft:iron_nugget", "C": "minecraft:chain", "T": "minecraft:tripwire_hook"},
                 "minecraft:chain", "Chain", "minecraft:chain"),
    "roadplate": ("Roadplate", ["NSN", "NDN", "NIN"],
                  {"N": "minecraft:iron_nugget", "S": "#minecraft:signs", "D": "minecraft:white_dye", "I": "minecraft:iron_ingot"},
                  "minecraft:iron_nugget", "Iron Nugget", "minecraft:iron_ingot"),
    "magpie": ("Magpie", ["GCG", "CRC", "GCG"],
               {"G": "minecraft:gold_nugget", "C": "minecraft:copper_ingot", "R": "minecraft:lightning_rod"},
               "minecraft:copper_ingot", "Copper Ingot", "minecraft:copper_ingot"),
    "bluecoat": ("Bluecoat", ["LGL", "PLP", "LGL"],
                 {"L": "minecraft:leather", "G": "minecraft:glass", "P": "minecraft:prismarine_shard"},
                 "minecraft:leather", "Leather", "minecraft:prismarine_shard"),
    "blacklung": ("Blacklung", ["BCB", "PBP", "BCB"],
                  {"B": "minecraft:blackstone", "C": "#minecraft:coals", "P": "minecraft:blaze_powder"},
                  "#minecraft:coals", "Coal", "minecraft:blackstone"),
    "hazmat": ("Hazmat", ["YLY", "HGH", "YLY"],
               {"Y": "minecraft:yellow_dye", "L": "minecraft:leather", "H": "minecraft:honeycomb", "G": "minecraft:glass"},
               "minecraft:honeycomb", "Honeycomb", "minecraft:honeycomb"),
}


def write(name: str, body: dict) -> None:
    (DATA / f"{name}.json").write_text(json.dumps(body, indent=2) + "\n")


def main() -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    lang = {
        "itemGroup.survivalistarmor": "Survivalist Armor",
        "upgrade.survivalistarmor.kit": "Survivalist Kit",
        "item.survivalistarmor.kit.applies_to": "Armor",
        "item.survivalistarmor.kit.base_slot_description": "Add a piece of armor",
    }
    for kit, (title, pattern, key, cost, cost_name, dup) in KITS.items():
        result = f"survivalistarmor:{kit}_kit"
        write(f"{kit}_kit", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                             "key": {k: item(v) for k, v in key.items()}, "result": {"id": result, "count": 1}})
        write(f"{kit}_kit_duplicate", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SKS"],
                                       "key": {"S": item(dup), "K": item(result)}, "result": {"id": result, "count": 2}})
        write(f"{kit}_smithing", {"type": "survivalistarmor:kit_smithing", "kit": f"survivalistarmor:{kit}",
                                  "template": item(result), "base": item("#minecraft:trimmable_armor"), "addition": item(cost)})
        lang[f"item.survivalistarmor.{kit}_kit"] = f"{title} Kit"
        lang[f"item.survivalistarmor.{kit}_kit.ingredients"] = cost_name
        lang[f"item.survivalistarmor.{kit}_kit.additions_slot_description"] = f"Add {cost_name.lower()}"
        lang[f"kit.survivalistarmor.{kit}"] = f"{title} Kit"
    write("kit_removal", {"type": "survivalistarmor:kit_removal", "category": "equipment"})
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(json.dumps(lang, indent=2) + "\n")
    print(f"wrote {len(KITS) * 3 + 1} recipes and {len(lang)} names")


if __name__ == "__main__":
    main()
