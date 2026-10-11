---
title: Survivalist Armor — project
type: overview
layer: store
tags: [overview]
---

# Survivalist Armor

## What this is

Craftable cosmetic armour kits, from nfx's handoff of 2026-10-09 (`knowledge/sources/ARMOR-MOD-SPEC.md`).
A kit is a smithing template. Applied at the smithing table to any armour piece, it swaps how the
piece is drawn for a remodelled, retextured scavenger's version (D-0001). The piece keeps its item,
stats, name, enchantments and trim. Chunkworks, AGPL-3.0-or-later, `com.chunkworks.survivalistarmor`,
both sides.

- **The brief.** nfx's two reference images are kept outside this public repo, in
  `minecraft mods/tools/reference/survivalist-armor/`. `reference-crew.webp` is the brief; where it
  and the spec disagree, the image wins (Hazmat's strong yellow and wrapped forearms, Duster's
  hood with the face in shadow, Scrapper's diamond weave).
- **The plan.** `~/.claude/plans/my-buddy-wrote-a-valiant-nebula.md`, approved by Rusty 2026-10-09.
  Rusty then said "Keep going, I want you to take this to the end": every phase was built through
  without stopping at the checkpoints.
- **The art is a clean-room match to Pixlli** (D-0002). Kitted item models are baked at runtime and
  drawn around Modefite (D-0003).

## Status: 0.2.0 (the bulk pass, D-0007; the kits' own atlas, D-0008) built and gated 2026-10-10, to ship in pack 1.82.0; 0.1.0 released in pack 1.80.0

**0.2.0** answers a friend's "arms look really chunky and blocky when equipped in first person and third person
... clips into player body" (2026-10-10; Rusty: "Give it a shot"). One envelope every kit keeps, held by the art
tests: sleeves 0.5, bracers 1.0, the shoulders pulled down and in, torsos 3.0 deep, nothing on the body where the
arms swing; D-0004's seams moved in with it. First person draws the kit's sleeve alone (`KitMesh.sleeve`). The kits'
sprites went from 10.67 to 9.57 Mpx. The booth's Hector creeper shot went with the pack's Hector (pack 1.81.0).

**The kits' own atlas (D-0008).** Rusty's block atlas went from 8192x8192 to 16384x8192 on pack 1.80.0. Measured on
a copy of his client: the kits alone did it, and the pack without them has under 1.73 Mpx of the block atlas free,
so no palette cut or tighter sheet could fit them back. Rusty chose to move them: `KitAtlas`
(`survivalistarmor:textures/atlas/kits.png`, 4096x4096, listed in `assets/survivalistarmor/atlases/kits.json`).
His client with 0.2.0 stitches the block atlas at 8192x8192 again.

The gate green on 2026-10-10 (`clean build --no-build-cache`, the software booth): 55 JUnit, 134 art tests, 26
GameTests, 27 booth checks (the build's count); the GPU booth's 27 the same, under Pixlli and Complementary
(`run/review/0.2.0-d0008/`). To ship on Rusty's go of 2026-10-10 ("Release the 2026-10-10 batch and Survivalist
Armor 0.2.0. This is my go."); the friend has not seen 0.2.0.

### 0.1.0

Released on Rusty's "Release that shit, 2 min server warning" / "Release everything": tag `v0.1.0`
at `adea51c`, the gate green on that commit (52 JUnit, 111 art tests, 26 GameTests, 25 booth
checks), jar `2113341e` on GitHub and on the server, with Set It Down 1.1.1. The server repo's
`knowledge/releases/pack-1.80.0.md` is the deployment record. Nobody has worn a kit on the box yet.

## What 0.1.0 is (built 2026-10-09)

- **Phase 0, the four spikes, green.**
  - **S1, uninstalling.** `UninstallGameTests`, 9 cases. A kitted piece loses only the kit and keeps
    every other component wherever it is saved: on its own, in a shulker or bundle, in a chest,
    frame or dropped item, on a stand or a zombie, in a player's inventory. A mutant expecting the
    kit kept failed all 9.
  - **S2, worn.** The hook draws every wearer with EMF, Armored Legacy and Fresh Animations loaded.
  - **S3, items.** The `@WrapMethod` wrap beats Modefite, Armored Legacy and 3D Default.
  - **S4, shaders.** Complementary (RP_MODE 3) reads the kits' `_n`/`_s` from the block atlas.
- **The system** (D-0001, D-0003): the component, the recipe and removal, the worn hook, trims by
  projection, first person, runtime item models, palettes, blacklist, config, the template icons,
  the EMI plugin (D-0006).
- **The seven kits** (D-0004), written in `devtools/art/kits/`: Roadplate, Magpie, Bluecoat,
  Blacklung, Hazmat, Duster, Scrapper: 62 to 77 cubes a kit, 40 at most a piece. Each previewed in its designed-toward
  armour and photographed in game under Pixlli and Complementary on the 4070, in a row as nfx's
  reference stands its crew.
- **Modded armour** (D-0005): galestone and voidsteel palettes; Create's backtanks refused by
  default; the pack's other armour takes kits by its slot tags.
- **Gates.** JUnit 52; art tests 111 (interfaces, vocabulary, budget, buried cubes, the wall,
  LabPBR channels); GameTests 26; booth 25 checks. All green on 2026-10-09 (`clean build
  --no-build-cache`, the software booth; the GPU booth's 25 the same). The release record will
  name the gate at the tag.
- **Set It Down 1.1.1** (its D-0008): a piece's armour box measured per item and components, so a
  kitted chestplate set down rests as itself. The booth's check fails on 1.1.0 and passes on 1.1.1.

## Not done, and why

- **Not seen by Rusty or nfx.** The booth photographs are the review; nfx's §17 checkpoint asked
  for side-on shader shots. With Pixlli, for review only: `run/review/booth-lineup*.png`,
  `booth-side-lit*.png`, `booth-cave*.png`. Without Pixlli (the wiki's): `run/wikishots/`.
- **Display transforms** (`assets/survivalistarmor/survivalistarmor/display/*.json`) were set
  once by photo for the placeholder and not recalibrated per kit: the frames and ground shots look
  right for Roadplate; the other kits' bulk may want their own `display` overrides.
- **Modded pieces kitted on a stand**: Myths of the Sea's and Block Factory's armour have not been
  seen kitted (D-0005).
- **A second player seeing a kit** can only be checked on the box: one client runs at a time here.
- **The 20-zombie FPS scene** from the plan's verification was not run.

## Building

- Gradle 9.2.1 runs on JDK 25: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew …`. The
  default `java` is Oracle JDK 26.
- The Java 21 toolchain that compiles and runs the game is Adoptium 21.0.12, fetched by foojay into
  `~/.gradle/jdks` (the system's JDK 21 is gone, found 2026-10-09).
- `./gradlew clean build` is the gate (README, "Building and testing"). `artTests` finds uv on the
  PATH or in `~/.local/bin`.
- The booths need a display. Xephyr was removed by an OS upgrade on 2026-10-09 (the desktop is now
  Wayland): a rootful `Xwayland :7 -geometry 1280x720 -decorate -ac -noreset` stands in for it.
  The software booth runs there with the mesa variables; the GPU booth on the same display with
  them unset gets the 4070 (its log's `OpenGL Renderer` names the GeForce).
- `devtools/booth/fetch.py [--gpu]` fills `run/booth` (`run/gpubooth`) from the published pack, by
  sha1.
