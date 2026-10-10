# Survivalist Armor

Craftable cosmetic armour kits for NeoForge 1.21.1, both sides. A kit is a smithing template:
applied at the smithing table to any armour piece, as a trim is, it changes how the piece is drawn,
to a remodelled scavenger's version of itself (straps, plates, pouches, respirators, all real
geometry with 128x LabPBR textures). It never changes what the piece does: armour points,
durability, enchantments, name, dye and trim are kept, and a trim still shows on top. From nfx's
spec (`knowledge/sources/ARMOR-MOD-SPEC.md`); the design is in `knowledge/decisions/`, the state of
the work in `knowledge/PROJECT.md`.

## The seven kits

| Kit | Idea (nfx's §6) | Designed toward | Smithing scrap |
|---|---|---|---|
| Roadplate | a breastplate cut from a road sign, bolted over padding; welding visor | iron | iron nugget |
| Magpie | brass busbar and copper from a substation on a lineman's coat; hard hat | gold | copper ingot |
| Bluecoat | a faded teal hardsuit under scavenged leather; tank and hose; lit visor | diamond | leather |
| Blacklung | black lames, a gas mask with lit orange eyes, hazard shoulders | netherite | coal |
| Hazmat | a quilted yellow chemical suit, boxy hood, side filter, bedroll, coiled hoses | iron | honeycomb |
| Duster | a canvas coat over hide, a deep hood, face rag and welding goggles | leather | string |
| Scrapper | chain-link fence cut into panels and wired onto a jacket; twin cartridges | chainmail | chain |

Each kit is crafted (a 3x3 shaped recipe of scrap and hardware) and copied (the kit between two of
its copying item makes two). The recipes are written by `devtools/data/recipes.py` from one table.

## How it works

- **The component.** A kit on a piece is one data component, `survivalistarmor:kit`, holding the
  kit's id (D-0001): persistent, synced, nothing more. Applying another kit replaces it; a kitted
  piece and shears in a crafting grid give the piece back without it (`kit_removal`; the shears
  lose a point). Uninstalling the mod leaves every kitted piece as the plain piece: the game loads
  a stack naming an unknown component with only that entry dropped (S1, `UninstallGameTests`).
- **The recipe.** `survivalistarmor:kit_smithing`: the kit item, any `#minecraft:trimmable_armor`
  piece that is an `ArmorItem` and not refused by the server's blacklist, and the kit's scrap.
  `consumeTemplate=false` keeps the kit (a mixin on `SmithingMenu.onTake`).
- **Worn.** One hook at the head of `HumanoidArmorLayer.renderArmorPiece` draws a kitted piece and
  cancels the game's: players, mobs and their babies, armour stands, the smithing screen's preview,
  Set It Down's displays and Serfdom's villagers. Un-kitted pieces keep Armored Legacy. A trim is
  drawn on the kit's cloth and plates by projecting each face onto vanilla's armour box (D-0001);
  hardware (lenses, hoses, pouches, straps) takes none. Glint and dye as the game draws them.
- **First person.** The kit's sleeve on your arm, both hands, after the skin's sleeve: the arm's cubes
  that keep within 0.5 of it, so no bracer, glove, tape or shoulder stands in the view down your arm
  (`KitMesh.sleeve`, D-0007). Client config `firstPersonArms`.
- **As an item.** A kitted piece is 3D wherever an item is drawn (inventory, hand, ground, frames):
  a model baked at runtime from the kit's own geometry, drawn around Modefite through MixinExtras
  `@WrapMethod` on `ItemRenderer` (D-0003), with the trim's real pattern.
- **Material.** A kit's metal parts take the piece's material through `palettes.json` (armour
  material id → palette, `neutral` for anything unnamed): cut steel on iron, brass on gold, teal
  composite on diamond, dark alloy on netherite, wire steel on chainmail, hide on leather (which a
  dye tints), scute on turtle, and nfx's galestone and voidsteel (D-0005).
- **Textures.** Every kit's garment and one material sheet per palette, each with `_n` and `_s`,
  are `single` sprites on the block atlas (one copy for worn, first person and items; Complementary
  reads their maps there, spike S4).
- **EMI.** Each kit's smithing recipe and the removal, the piece and the kitted piece turning
  together (D-0006). Compiled against EMI's API only.

## Kits as data

A kit is `assets/<ns>/survivalistarmor/kits/<kit>.bbmodel`, one Blockbench project holding all four
pieces: top folder the piece (`helmet`, `chestplate`, `leggings`, `boots`), the next the body part
(`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`). A cube's name says its material
(`plate.breast`, `strap.bandolier`, `lens_lit.eye_r`): trims show on `plate`, `shell`, `lames`,
`sheet`, `panel`, `canvas`, `cloth`, `coat`, `hide`, `leather`, `padding`, `quilt`, `suit`, `hood`;
`lens_lit` glows; `mesh` (chain-link) hides nothing behind it. The project's two textures are named
`garment` and `material`. A resource pack can add or replace kits; F3+T reloads them.

## Art (devtools/art)

The kits are written in Python (`kits/<kit>.py`) over `kitlib`: geometry in the Blockbench frame,
the shared vocabulary at nfx's depths (`vocab.py`), packing at eight pixels a unit, procedural
painting (`paint.py`), palettes, the writer of projects, textures and LabPBR maps, previews and the
template icons.

```
uv run --no-project --with numpy --with pillow python -I devtools/art/build.py [kit ...]
uv run --no-project --with numpy --with pillow python -I devtools/art/look.py <kit> <yaw> <pitch> <x,y,z> [scale] [palette]
```

**The clean-room wall (D-0002).** Pixlli's licence forbids sharing derivatives, so the art matches
it by measurement only: `measure.py` reads Rusty's own copy and writes numbers to
`pixlli-style.json`; nothing that paints reads Pixlli. `tests/test_wall.py` holds the wall.

**The art tests** (`devtools/art/tests`, run by `./gradlew check` as `artTests`) hold every kit to
the interfaces that let pieces of different kits mix (D-0004, moved in by D-0007), one envelope of
how far anything may stand off the body (D-0007: sleeves 0.5, bracers 1.0, the shoulders, 3.0 before
and behind the torso, nothing where the arms swing; the constants in `kitlib/vocab.py`), nfx's shared
vocabulary, a heavy left shoulder, a budget of 40 cubes a piece, no cube buried, and the textures to
LabPBR's channels.

## Configuration

- `survivalistarmor-server.toml` (synced): `blacklist` (a mod id refuses its every piece, an item id
  that piece; refused pieces take no kit and a kit already on one is not drawn; Create's backtanks
  by default, D-0005), `consumeTemplate` (default `true`).
- `survivalistarmor-client.toml`: `firstPersonArms` (default `true`).

## Building and testing

```
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew clean build
```

Gradle runs on JDK 25; the game is compiled and run on Java 21, which Gradle's foojay resolver
fetches. `check` runs:

- **JUnit** on the domain layer (the Blockbench reader, rest pose, trim projection, mesh culling,
  palettes, blacklist);
- **the art tests**, under uv;
- **GameTests** on a real server: applying, swapping and refusing kits, removal, cosmetic-only,
  uninstalling;
- **the booth** (`runPhotoBooth`, software rendering, `run/booth` filled by
  `devtools/booth/fetch.py` from the published pack): 25 checks of what was drawn, by capturing
  the render, for every wearer, items in four contexts, first person, trims, Set It Down's displays
  and EMI. `runGpuBooth` (`run/gpubooth`, `--gpu`) is the same booth photographed on the GPU with
  Pixlli and Complementary, plus the seven kits in a row and a cave. `-PboothKit=<id>`
  films another kit.

## Licence

AGPL-3.0-or-later. Copyright 2026 Rusty Shackleford and nfx.
