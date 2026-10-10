# Survivalist Armor — Craftable Cosmetic Armor Kits

**Handoff spec.** Target: NeoForge **1.21.1** (neo_version 21.1.248, Java 21) — same as both
of my live packs.

**Reference images, in this folder — look at them before reading anything else:**
- `reference-crew.webp` — **the primary reference.** Four figures, desert ruins. This is the target.
- `reference.webp` — secondary, from an earlier pass (teal and black figures).

---

## 0. One paragraph

A set of **craftable cosmetic armor kits** that you apply to a piece of armor at a **smithing
table**, exactly the way vanilla armor trims work. Applying a kit doesn't change the armor's
stats, name or behaviour — it changes what it *looks* like, swapping in a fully remodelled,
retextured survivalist version of that piece. Scavenged post-collapse gear: respirators, bolted
plate, bandoliers, pockets, canvas, tape, dust. Fallout and S.T.A.L.K.E.R., not Skyrim.

The armor you haven't kitted stays exactly as it is today. The new look is something you craft
and apply, piece by piece.

---

## 1. How the system works

This is the part that changed, so read it before anything else. The art direction (§3–§9) is
unchanged, but it now gets delivered through kits rather than by replacing everything wholesale.

### 1.1 The kit items

A **kit** is the cosmetic style — its own model, its own texture, its own construction. There are
seven (§6), and they're craftable items.

Mirror vanilla trim templates as closely as you can, because people already understand them:

- Each kit is a **template item** that sits in the smithing table's template slot.
- **Craftable**, not loot-gated. These are scavenger kits; you make them out of junk. Recipes in
  §6 are proposals — the feel I want is "cobbled together from scrap", not "rare treasure".
- **Duplicable at a crafting table**, like trim templates (template + the material it's made of),
  so having one is enough to keep outfitting.
- The template is **consumed** on use, same as a trim template. If you'd rather make it reusable,
  that's a config toggle, not a redesign.

### 1.2 Applying one — the smithing table

Three slots, mapped the same way vanilla trims map:

| Slot | Takes | Role |
|---|---|---|
| **Template** | A Survivalist Kit | Which style you're applying |
| **Base** | Any armor piece | What you're applying it to |
| **Addition** | Scrap material | The cost, consumed |

Output is **the same armor piece**, with a cosmetic component added. It keeps its enchantments,
durability, custom name, existing trim and everything else — identical item, new appearance.

Implementation is a custom `SmithingRecipe` with its own serializer. Don't try to bend
`SmithingTrimRecipe` into this; it's hardcoded around the trim component.

**What the addition slot controls.** My recommendation: it's purely the cost, and the **palette
comes from the base armor's own material** — so the Roadplate kit on an iron chestplate shows
cut steel, and the same kit on a diamond chestplate shows teal composite where the plate is
exposed. That keeps "the armor still subtly reads as its material" (§2) working, and it avoids a
third combinatorial axis in the item models (§11). The alternative — addition material sets the
accent colour, a true pattern × material system like trims — is in §18 as an open question.

### 1.3 Removing and swapping

- **Swapping** is just applying a different kit. The new one replaces the old.
- **Removing** returns the piece to its vanilla look. Do this at the **smithing table** too, with
  an empty-ish addition, or as a shapeless crafting recipe — your call, but it must **preserve
  enchantments, durability and name.**
- **Not the grindstone.** The obvious-looking choice is wrong: the grindstone strips enchantments,
  so anyone de-kitting a good chestplate would lose the enchants with it.

### 1.4 Kits coexist with vanilla trims

A piece can carry **both** a vanilla armor trim and a survivalist kit, and both must render. Our
component and the vanilla trim component are independent.

This is the main constraint on the kit models: **a kit's geometry has to take a trim correctly.**
See §10 — the answer is keeping the kit model's UVs inside the vanilla armor UV layout so the
existing trim sheet maps straight onto it.

### 1.5 What a kit does *not* do

Nothing. It's cosmetic, start to finish — see §2.

---

## 2. Strictly cosmetic

**No kit changes gameplay in any way.** This is a visual system and nothing else.

- **Armor stats are untouched.** Armor value, toughness, knockback resistance, durability,
  enchantability, repair material, equip sound — a kitted piece is numerically identical to an
  un-kitted one. No set bonuses, no hidden perks, no downsides.
- **Armor keeps its name and identity.** An iron chestplate is still `minecraft:iron_chestplate`
  and still reads "Iron Chestplate". The kit shows in the tooltip the way a trim does. The kit
  names in §6 (Roadplate, Magpie, Bluecoat…) are the **kit items'** names — they never rename the
  armor itself.
- **Base armor recipes are untouched.** We add recipes for the kit items and one smithing recipe.
  We don't modify, remove or override a single vanilla recipe.
- **Un-kitted armor is 100% vanilla.** If you never craft a kit, the mod changes nothing you can
  see. This is a deliberate difference from the original plan — the survivalist look is opt-in,
  per piece.
- **No new armor materials, no new armor pieces.** Only the kit items themselves are new.

**Gameplay footprint, stated plainly, since this version does add things:** a handful of new
template items, their crafting recipes, one custom smithing recipe, and one data component on
armor stacks. That's the whole surface area.

**Uninstalling.** Needs to degrade gracefully: pull the mod out and every armor piece must survive
intact and render vanilla again, with enchantments and durability untouched. **Verify what 1.21.1
actually does with an unregistered data component on load** — the acceptable outcome is the kit
silently disappearing; losing the item is not acceptable. Test this early, it's cheap to check and
expensive to discover late.

**This version needs to be on the server as well as the client** — items, recipes and components
are server-side concepts. The earlier client-only idea doesn't survive the redesign, which is
fine, but it means everyone on a shared world needs the mod.

---

## 3. Reading the reference (`reference-crew.webp`)

What that screenshot is actually doing, ranked by how much it matters:

1. **Bulky, layered silhouette.** The armor is visibly wider than the player model, and the bulk
   is *stepped* — you can count the layers. Slab shoulders, thick chest, boots that flare. It
   reads as stuff piled on a body, not as a texture painted on a body.
2. **Big shapes, few of them.** Almost nothing in that image is small detail. What looks detailed
   is big shapes meeting other big shapes along hard edges.
3. **Hard dark lines between parts.** Nearly every component is separated from its neighbour by a
   dark line. That near-outlining is most of why it reads instantly at gameplay distance, and it's
   what makes the depth and seams land.
4. **One dusty tone over everything.** Warm tan dust sits over all four figures. It's the main
   reason a yellow suit and a black one look like they come from the same world.
5. **Nobody has a face.** Every single one is masked — respirator, goggles, hood, filter. This
   does more "survivalist" work than any other single detail. Keep it universal.
6. **Almost no saturation anywhere** except the yellow suit, which earns it by being the *whole*
   garment rather than an accent.

### The four figures, individually

| | What it is | Steal this |
|---|---|---|
| **Yellow** | Sealed hazmat / firefighter suit. Quilted horizontal panelling, full-face mask with a round side filter, brown leather belt and shoulder straps over the top, bedroll and canister on the back, red and blue hose coiled at the shoulder, heavy gloves, boxy hood. | The suit *is* the armor and the straps sit over it. Hoses and a back rig breaking the silhouette. |
| **Grey / white** | Bolted sheet-metal plate over a padded undersuit. Boxy welded helmet, twin-cartridge respirator, stained white-grey panels, visible rivets and patch plates, dark bandolier right-shoulder-to-left-hip, mismatched pauldrons. | Patch plates over padding. The bandolier. Asymmetric shoulders. Grime pooling in the seams. |
| **Brown** | Layered coat and hood. Goggle-eyed respirator, chest pouch, pack straps crossing the chest, big turned collar, skirted coat over the legs. | Cloth layering, hood pulled over the mask, pouches that hang and overlap. |
| **Dark** | Scrap and mesh. Chain-link / wire weave across the chest, dark hood, filter mask, uneven dark plates. | Mesh as a real material, not drawn rings. Near-black that still has readable form. |

---

## 4. The two art rules I care most about

### 4.1 Every kit is its own design. Not one chassis recoloured seven times.

The seven kits must not be palette swaps of each other. Same cubes, same strap placement, same
silhouette with the hue rotated is exactly what we're **not** building — it's what vanilla armor
already does and it's why vanilla armor looks flat.

Each kit gets its **own construction logic**: what the scavenger found, how they attached it, what
it protects, how it sits on the body. The Duster and the Roadplate shouldn't share a single cube.
Look at the reference — four completely different ideas of what armor even is: a sealed chemical
suit with a tank, bolted sheet plate over a padded undersuit, a layered coat with a hood, mesh over
scrap. That's the bar.

**What ties them together is vocabulary, not geometry** — §5.

One-line test: if someone can look at two kits and say *"that one's just the orange version of
that one"*, it's wrong, redo it.

**Note on how this interacts with the kit system.** Because any kit can go on any armor material,
the base material supplies the palette for the exposed-material regions (§1.2). That's fine and
it's how trims work — but it's *not* a licence to let the kits themselves become one chassis in
seven colours. The variety comes from seven genuinely different builds; the material tint is on
top of that, not instead of it.

### 4.2 The material still shows, but it isn't the garment

A kitted piece should still *read* as the material underneath it, subtly. The material shows as
**specific salvaged parts** — a plate, a mesh, a strip of busbar — bolted, wired or strapped onto a
garment made of something else entirely. Rough budget: **the base material covers 20–35% of the
silhouette.** The rest is canvas, hide, rubber, webbing, tape and hardware.

### 4.3 Depth is geometry. Straps, bags and seams get modelled, not painted.

The other non-negotiable, and the reason this is a mod and not a resource pack. **I don't want flat
pixels painted on a surface to suggest a bag, a strap or a layer.** If a thing would stick out,
hang off, wrap around or sit on top of something in the real world, it's **cubes**. It should catch
light on its top face, cast a shadow onto the surface beneath it, and visibly break the silhouette
when you look at the player from the side.

Bandolier and its cases, shoulder and chest straps, webbing, pouches, buckles, plate edges,
overlapping layers, hose runs, canisters, collar and hood lips, tape edges, knee and shin pads.
Painted versions of any of these are the fastest way to make this look like every other Minecraft
armor retexture. Full breakdown in **§8.1**.

---

## 5. The shared vocabulary (what makes seven different builds one set)

Since the kits don't share geometry, these are what hold them together. **Every kit gets all of
them**, modelled and drawn in its own way:

- **A respirator or sealed mask.** Varies per kit: rag and goggles on the scrappiest, sealed
  multi-filter rig on the heaviest. No visible faces anywhere in the set.
- **Lenses.** Dead flat black on the crude kits, lit / emissive on the high-tech ones.
- **Hazard yellow**, `#C6A42E` over near-black `#1E1B16`, 45°. One or two placements per kit,
  always peeling, torn or scuffed. The one colour constant across all seven.
- **A bandolier**, always right shoulder to left hip. 4–6 cases, clustered rather than evenly
  spaced. The strap *and each case* are modelled (§8.1).
- **Pockets and pouches** that genuinely hang off the surface and break the silhouette. Modelled,
  never painted. At least 2 per chest piece, 1 per leg piece.
- **A filter canister and a ribbed hose** somewhere, even if small. Both modelled.
- **One sprayed stencil mark** per kit — a number, a letter, an arrow. This one is texture.
- **Straps and webbing** crossing at least two panel seams, so they read as holding things on.
  Modelled, standing off the surface, casting a shadow on what they cross.
- **The dust pass**, applied last over everything (§7).

**Clutter budget per kit, so it stays scavenged rather than cosplay:** 2 tape placements · 1
stencil · 4–6 bandolier cases · 2–4 pouches · 1 hose run · 0 visible faces.

### Deliberate asymmetry

Nothing mirrors. Heavy left shoulder, light right. Bracers don't match. Tape on one side only.
Vanilla's armor UV *does* mirror the arms and legs by default, so breaking that is a real
modelling cost — it's a decision, not a freebie (§18).

---

## 6. The seven kits

Each is a four-piece style (helmet, chestplate, leggings, boots) and one craftable template.
Recipes are proposals — push back. The feel I want is scrap and hardware, not treasure.

| Kit | The idea | Suggested recipe feel |
|---|---|---|
| **Duster** | Canvas work coat over cut hide, big collar, face rag and welding goggles. The brown figure in the reference. | Leather, string, dried kelp or wool |
| **Scrapper** | Chain-link fence cut into panels and wired onto a work jacket. **Diamond weave, not rings** — that's what stops it reading medieval. Half-face twin-cartridge respirator. | Iron nuggets, chains, tripwire hooks |
| **Roadplate** | Breastplate cut out of a road sign, with enough lettering surviving to read part of it. Welding visor. The grey figure in the reference. | Iron ingots, a sign, white dye |
| **Magpie** | Brass busbar, copper pipe and contacts stripped out of a substation, mounted on a worker's coat. Soft, so it **bends and dents** rather than chipping. Heavy verdigris and tarnish. | Copper, gold nuggets, lightning rod |
| **Bluecoat** | A pre-collapse hardsuit — pale institutional teal, scuffed toward grey — with leather, webbing and wood slats lashed over it by whoever scavenged it. Tank and hose over the shoulder. | Leather, glass, prismarine or diamond |
| **Blacklung** | Black lames and scrap, lit orange lenses, full gas mask, hazard shoulders, ammo bandolier. The dark figure in the reference. | Blackstone, netherite scrap, blaze powder |
| **Hazmat** | Sealed quilted chemical suit, boxy hood, round side filter, bedroll and canister on the back, coiled hoses. The yellow figure in the reference. | Yellow dye, leather, honeycomb, glass |

**On gold and diamond, which started this whole project.** Vanilla's are cartoonishly coloured,
and the kits are the fix: **Magpie** and **Bluecoat** exist specifically for them. Both work by
treating the material as a *found object* rather than a paint colour — gold becomes brass hardware
on a work coat, diamond becomes a faded blue-grey hardsuit under scavenged leather. Both should
look like the **least** saturated things in the lineup at a glance, and only reveal their material
when you look properly. They're also the two kits I most want to see early (§17).

**A note on where the vanilla tier archetypes went.** In the earlier design each tier *was* a
style. Now the kits are free-floating — nothing stops a Magpie kit on a netherite helmet, and that
should look good. But the pairings above are still the ones I'd design toward first, and the
material tint (§1.2) means the "natural" pairings will come out especially well.

**Turtle shell** is a helmet-only oddity: scutes on a rubber dive hood, brass porthole lens,
rebreather hose, **wet instead of dusty**. Worth doing as a kit at some point; not in the first
seven.

---

## 7. Texture spec

- **128×** to match Pixlli — **512×256** for the armor layer textures (vanilla is 64×32, so 8×).
  Item icons likewise at 8× vanilla.
- **LabPBR** throughout: albedo plus `_n` and `_s` sidecars (§9).
- **One material per contiguous region.** If two materials share a run under 2px, merge them — the
  roughness channel can't resolve it and it'll sparkle under shaders.
- **Layer, don't paint.** Base garment → attached material → strapped-on kit. Three readable depth
  bands, each with its own brightness range and its own edge in the silhouette.
- **Wear lives on geometry.** Bright on high edges, dark and greasy in recesses. Nothing wears in
  the middle of a flat panel.
- **Only draw what you could model** — and if it should have depth, **model it instead of drawing
  it** (§8.1). Texture handles surface: grain, grime, stains, stencils, wear. It doesn't handle
  things that stick out. No fine per-pixel noise either; it turns to mud and bakes a filthy normal
  map.
- **No painted drop shadows.** If a strap needs a shadow under it, the strap is a cube and the
  shadow is real. Painted contact shadows fight the shader's own lighting and look wrong the moment
  the light moves.
- **Progression is hue and value, never saturation.** Everything stays under ~35% saturation except
  the hazard yellow and the lens glow.
- **The dust pass goes on last, over everything.** Warm tan, light at the shoulders, heavier toward
  the boots, extra film on upward-facing surfaces. The single biggest reason the reference reads as
  one world. **Dust must break metalness where it's heavy** (§9) — dusted metal that keeps its F0
  renders as dirty chrome, and that's the mistake everyone makes.

### Don't

- Fine noise, dithering or per-pixel grain.
- Mirror-symmetric shoulders or matching bracers.
- A third saturated colour anywhere in the set.
- Clean tape. It's always peeling, torn at a corner or lifted at one end.
- Evenly spaced anything — bandolier cases, rivets, bolts. Cluster them.
- Pure black or pure white. Floor around `#14120E`, ceiling around `#E2DAC8`.
- **Flat painted straps, bags, buckles or seams.** The thing I'm most likely to reject a piece for.

---

## 8. Model spec

Real 3D geometry for every piece of every kit. The bulk and layering in the reference are geometry,
not texture — that's the whole reason this is a mod.

### Geometry rules

- **Build on top of the vanilla `HumanoidModel` part tree.** Custom parts parent to
  `head` / `body` / `rightArm` / `leftArm` / `rightLeg` / `leftLeg`, so they inherit all vanilla
  animation for free — walking, swimming, riding, sneaking, elytra, armor stands, mob poses. Don't
  build a free-floating rig.
- **Budget: roughly 20–40 cubes per piece.** Enough for pouches, strap crossings, canisters and
  stepped layers; not so much it tanks framerate when ten kitted pillagers show up.
- **Layering is cubes, not texture** — §8.1, the point of the whole project.
- **Keep it blocky.** No sub-pixel chamfering everywhere — this is still Minecraft. Chamfer where it
  reads (plate edges, helmet corners) and stay square elsewhere.
- **Don't poke through the player.** Boots overlap leggings and leggings overlap the chest in
  vanilla; preserve that ordering or you get z-fighting at the waist and ankle.
- **Non-player humanoids matter.** The same models render on zombies, skeletons, husks, drowned,
  piglins, villagers, armor stands and **baby** variants — anything that can be given kitted armor.
  Baby zombies scale the head part, so anything parented to `head` scales with it.
- **Mixing kits across slots has to work.** Nothing stops a player wearing a Duster helmet with
  Roadplate boots. It doesn't need to look designed, but it mustn't clip badly — keep the waist,
  neck and ankle interfaces at consistent volumes across all seven kits.

### 8.1 What must be geometry, and what may stay flat

The rule: **if it would stick out, hang off, wrap around, or sit on top of something, it's cubes.**
Not a painted highlight, not a drawn drop shadow.

| Must be real geometry | May stay in the texture |
|---|---|
| Bandolier, and **every case on it** as its own box | Fabric weave, canvas grain, mesh pattern |
| Shoulder straps, chest straps, webbing, belts | Stitching lines and thread |
| Pouches, pockets, mag carriers, bedrolls | Stains, grime, rust bloom, dust |
| Buckles, clasps, D-rings, press studs | Stencil marks, lettering, decals |
| Overlapping plate edges and layer steps | Scratches and scuffs |
| Hose runs and filter canisters | Paint chipping and wear patterns |
| Collar lips, hood edges, cuffs, boot flares | Colour variation between panels |
| Knee pads, shin plates, elbow caps | Faded printing, hazard-tape *colour* |
| Respirator body, filters, lens housings | Lens tint and glow |
| Rivet and bolt heads (where they read at size) | Hazard-tape *edge grime* |

**Depth values**, in 16-unit model space — enough to read at gameplay distance without looking
inflated:

- Straps and webbing: **0.5–0.75** off the surface
- Pouches, bandolier cases, canisters: **1.0–2.0**
- Plate edges and layer steps: **0.5–1.0**
- Hose runs: **1.0–1.5**, round-ish via a stepped 3-box cross-section if it's a hero piece
- Rivets and bolts: **0.25** — the one thing that may be faked if cube count is tight

**Seams specifically.** A seam is where two layers meet, so it's a *step in the geometry* — the
upper layer's edge standing proud of the lower one, with the dark separation line falling in the
gap. A dark line painted on a flat surface isn't a seam, and under shaders it reads as a smudge.

**Where cube budget gets tight, drop count, not depth.** Four chunky bandolier cases that actually
stand off the chest beat eight flat ones every time.

### Source format

Blockbench is fine and is what I use for everything else. Whatever you pick, the mod should load
models **from JSON at runtime**, not from hardcoded Java — so I can iterate on a kit without a
recompile, and so a new kit is mostly a data drop.

---

## 9. PBR and Pixlli cohesion

Both my packs run **Pixlli 128× (LabPBR)** with shaders on, so this has to sit in that world or it
will look pasted in.

- **Ship `_n` and `_s` beside every texture** — armor layers *and* item icons.
- **`_n`:** RG = normal, B = AO, A = height. Real height from the modelled geometry where there is
  any; shallow honest relief elsewhere.
- **`_s`:** R = perceptual smoothness, G = F0 / metal, B = porosity / SSS, A = emission.
- **Emission** (`_s` alpha) on lenses and lit seams only. Nothing else emits.
- **Rough smoothness guide:** bare steel `0.60–0.75` · painted metal `0.35–0.50` · hazard tape
  `0.22–0.32` · rubber `0.12–0.25` · lens glass `0.90–0.96` · canvas / webbing `0.05–0.20` ·
  cartridge brass `0.72–0.84` · tarnished gold `0.45–0.60` · diamond composite `0.50–0.65`.
- **Dust is a modifier on the maps, not a zone.** Where dust is heavy: drop toward dielectric F0,
  cut smoothness, flatten the normal. Heavy dust over metal must kill the metalness or the shader
  renders dirty chrome.
- **Match Pixlli's value range.** Pixlli's materials sit in a fairly tight mid band. If our armor is
  noticeably darker or brighter than the blocks around it, it reads as a different pack. Check in
  game against stone and oak planks, not in an image viewer.
- **The kit item icons need the same treatment** — they're new items and they'll sit in an inventory
  next to Pixlli's own art.
- **Verify the shader actually reads entity PBR maps.** Not every shader treats entity and armor
  textures the way it treats blocks. Confirm early; it changes how much the `_s` map is worth.
- Derive surfaces from the Pixlli source art rather than drawing material from scratch, same as my
  other patches, so materials genuinely match. I have the extracted sources and can point you at
  them.

---

## 10. Armor trims must keep working on kitted armor

A piece can have a trim and a kit at once (§1.4), and both must render. Facts I've already verified
on 1.21.1, so you don't have to rediscover them:

- Worn trims resolve `trims/models/armor/<pattern>_<trim material>` — keyed by the **trim** material,
  not the armor material. So **no new trim art is needed**, for any kit.
  (`overrideArmorMaterials` exists only so iron-on-iron gets the `_darker` palette.)
- `trim_type` is registered with `ItemProperties.registerGeneric`, not per item — so the predicate
  works on any armor item.
- Slot item tags (`#minecraft:head_armor` and friends, composited into `#trimmable_armor`) are what
  enable trimming. Already correct for vanilla, so **we touch nothing** (§2).

### Getting trims onto kit geometry — two options

**Option A (preferred): keep the kit model's UVs inside the vanilla armor UV layout.** Every cube
that should be trimmable takes its UVs from the vanilla 64×32 armor layer regions. The existing trim
texture then maps straight onto our geometry with no extra rendering work, and the trim lands roughly
where it would on vanilla armor. Genuinely new geometry — canisters, hose, pouches — gets mapped into
atlas space that's blank in the trim sheet, so it takes no trim, which is also the right look.

**Option B (fallback): inflated trim shell.** Render a second copy of the trimmable geometry scaled
slightly outward, UV'd to the trim sprite. This is what Armored Legacy does for its item icons, so
there's known-good precedent, but it doubles worn geometry — use only where A is impossible.

---

## 11. Item models: 3D models and sprites, across every display context

Two sets of item art are needed: **the kit template items**, and **the armor pieces themselves once
kitted** (since a kitted chestplate should look kitted in your hand and in your inventory).

**The 3D model is what renders anywhere the item exists in the world — no exceptions.** A dropped
item on the ground is 3D. An item in an item frame is 3D. An item set down with Set It Down is 3D.
An item on an armor stand or in a hand is 3D. None may fall back to a flat extruded sprite — that's
exactly the look we're getting rid of.

**Ship both, with a clear division of labour:**

- **A real 3D item model** for every kitted armor piece, matching the worn armor's design so the two
  read as the same object. Used in every world and GUI context — ground, item frames, both hands,
  head slot, inventory icon, Set It Down.
- **An updated flat sprite** as a **fallback only**, for flat-2D surfaces that can't render a model:
  recipe viewers, tooltips, advancement icons. Never what you see in the world.
- **The kit templates** follow vanilla trim-template convention — a flat sprite is fine for those,
  and they should be visually distinguishable from each other at icon size.

**Tune every `display` context**, not just `gui`:

| Context | Where it shows | Watch for |
|---|---|---|
| `gui` | Inventory, hotbar, containers | Renders the 3D model at a slight angle so depth reads; legible at icon size |
| `fixed` | **Item frames** | **3D with real depth standing off the frame.** Centred, not poking through or floating |
| `ground` | **Dropped items** | **3D and solid, with thickness** — not a flat plane. Spins and bobs, so it must read from every angle |
| `thirdperson_righthand` / `_lefthand` | Held, seen by others | |
| `firstperson_righthand` / `_lefthand` | Held, your own view | Distinct from §13, which is armor *worn* on the arm |
| `head` | Worn in the head slot | |

### 11.1 Model permutations — plan for this early

A kitted piece's item model depends on **both** the kit and the trim. Seven kits × ten trim
materials × four pieces is a few hundred generated model JSONs, on top of the un-kitted vanilla
case. That's fine — they're tiny and scriptable, and I've generated trim model permutations before
on another mod, so there's a working precedent I can share. But **write the generator first**;
hand-authoring these is not viable.

In 1.21.1, an override entry can carry multiple predicates, so one model per (kit, trim) pair with
both `trim_type` and our own kit predicate works. Register the kit predicate with
`ItemProperties.registerGeneric` reading our component, same pattern as `trim_type`.

### 11.2 Set It Down compatibility — read before choosing how to build item models

Set It Down is my own mod. It may or may not be on the shared server, so don't depend on it, but
don't break it either. Two things about how it works:

1. **It measures an item's real bounds from the baked model's quads**, using the `FIXED` transform,
   so it can push the item clear of the surface. **An item model with no quads — anything rendered
   through a `BlockEntityWithoutLevelRenderer` instead of a baked JSON model — falls back to a flat
   one-block square and will clip or float.** So: **ship real baked JSON item models with actual
   geometry. Don't custom-render the item through a BEWLR.** The most important compat note here.
2. **For armor it ignores the item model entirely** — it equips the piece onto an invisible,
   never-ticked, client-side `ArmorStand` and renders that, so a set-down chestplate looks like the
   real worn piece. Which means **our worn armor layer must render correctly on an off-world
   ArmorStand driven directly by `EntityRenderDispatcher.render`.** Don't gate the render on the
   entity being a real player, being in a loaded world, or having ticked.

---

## 12. Coverage: modded armor

The kit system makes this much simpler than the old design. A kit applies to **any armor piece**,
so modded armor is covered as long as the smithing recipe accepts it.

- **Accept anything in the vanilla armor slot tags** (`#minecraft:head_armor` and friends). Most
  modded armor is tagged correctly; what isn't, we leave alone rather than retagging (§2).
- **Palette for the exposed-material regions** (§1.2) comes from a lookup table keyed by armor
  material, with a sensible neutral default. Hand-tune entries for materials worth the effort and
  let everything else fall to the default — a reasonable default is much better than nothing, and
  nothing here can look broken the way the old auto-derivation could.
- **My own `galestone` and `voidsteel` armor** are in the pack and worth palette entries.
- **A per-mod config blacklist**, in case some modded armor has geometry that fights the kits.

---

## 13. First-person hands

**A kitted chestplate must render on your own arm in first person.** Vanilla renders only the
player's arm and sleeve — `ItemInHandRenderer#renderPlayerArm` → `PlayerRenderer#renderRightHand` —
with no armor layer at all.

- **The chestplate drives it.** Layer 1 covers helmet, chest *and arms*, so arm armor belongs to the
  chestplate. Boots, leggings and helmet are irrelevant here.
- **Same geometry and textures as the third-person piece.** What you see on your arm is what
  everyone else sees on you. Don't author a separate first-person set.
- **Trims must show on it too.**
- **Both hands**, for the off-hand and left-handed cases.
- **Nothing renders** when the chestplate has no kit — un-kitted armor stays vanilla, which in first
  person means no armor on the arm at all, same as today.
- Hook point: NeoForge's hand render event, or a mixin on `renderPlayerArm` / `renderRightHand` /
  `renderLeftHand` — draw the armor arm part after the sleeve, using the same pose stack so it tracks
  the swing.
- **Conflict risk.** Mods that re-render hands will fight this. My "The Boys" pack runs **Fresh
  Animations**, which has already caused arm-pose problems with other mods, so test there and give
  this a config toggle.

---

## 14. Implementation notes

Collected so they're in one place:

- **One data component** holding the kit id, registered on armor stacks. It syncs to the client with
  the stack, so rendering can read it directly.
- **Custom `SmithingRecipe` + serializer** (§1.2). Don't reuse `SmithingTrimRecipe`.
- **Preserve every other component** on application and removal — enchantments, damage, custom name,
  trim, attribute modifiers, everything. Copy the stack and add/remove one component; don't rebuild it.
- **Tooltip line** showing the applied kit, styled like the trim line.
- **Kit predicate** via `ItemProperties.registerGeneric` (§11.1).
- **Worn rendering** keys off the component, in a render layer that replaces or supplements
  `HumanoidArmorLayer`.
- **Creative tab** for the kit items.
- **Config:** per-mod blacklist, template consumed on use, first-person toggle.

---

## 15. Compatibility checklist

- **Pixlli 128× plus shaders** — the main target. Check against blocks, in daylight and in a cave.
- **Fresh Animations / EMF / entity model mods** — highest-risk conflict, especially §13.
- **Armored Legacy** — I run it, and it also does 3D armor. Decide whether kitted armor overrides it
  cleanly or whether AL has to come out. Un-kitted armor should still get AL's treatment if it stays.
- **Other armor mods** — kits should apply to their pieces (§12).
- **Elytra** — occupies the chest slot, so no chestplate renders. Make sure that path is clean.
- **Mobs and armor stands**, including baby variants.
- **Dyed leather** — the dye must still do something sensible on a kitted leather piece. Decide
  whether the kit takes the dye or ignores it; I'd lean ignore, with the dye showing in whatever
  leather the kit exposes.
- **Item frames, dropped items and armor stands** (§11) — **confirm all three are genuinely 3D with
  real thickness**, not flat extruded sprites.
- **Set It Down**, if present (§11.2) — set a kitted chestplate down and confirm it renders as the
  worn 3D piece without clipping.
- **Uninstall** (§2) — pull the mod from a played-in world and confirm no item loss.
- **Multiplayer** — kit state is server-side and must sync; check a second player sees your kit.

---

## 16. Traps worth knowing up front

From earlier texture work in this pack:

1. **Dusted metal that keeps its metalness renders as dirty chrome.** Dust has to break F0.
2. **Transparent pixels hold black.** Blurring or scaling a cutout drags black into every visible
   edge. Flood the opaque colours outward first, then filter.
3. **Lifting the black floor kills detail.** A tone curve that raised the floor from 0.23 to 0.48
   once flattened every strap and buckle into the plate — "you can see where there should be a belt
   buckle, but it's far too light." Keep the dark floor; the dark separation lines do the structural
   work.
4. **Sharpening amplifies source dithering** into checkerboard noise. Gate the unsharp term.
5. **Judge at 8× upscale, side by side with the source.** Eyeballing a 64×32 plate lies.
6. **Minecraft holds loaded pack zips open.** Write a new filename, don't replace in place.
7. **Grey is not metal.** Any automatic "this is hardware" detector will flag grey cloth.
8. **A colour match alone never justifies a material.** Name plus palette plus pattern, or nothing.

---

## 17. Suggested order of work

1. **The system, with one kit, end to end** — template item, recipe, smithing application, component,
   worn render, 3D item model and sprite, first person, one trim on top. **Roadplate** is the one I'd
   build first: it's directly in the reference and it exercises everything.
2. **Magpie and Bluecoat** next — the gold and diamond answers, and the whole reason the project
   started. If those two land, the approach is sound.
3. **Blacklung and Hazmat** — the two most striking figures in the reference.
4. **Duster and Scrapper.**
5. **Material palette table and modded coverage** (§12), once the kits have settled.

A first checkpoint I can actually review: a Roadplate chestplate and helmet — crafted, applied at the
smithing table, worn, in an item frame, in first person, with one trim on top.

**Send it as a side-on screenshot with shaders on**, so the strap, bandolier and layer depth (§8.1)
is visible. Straight-on and shaderless hides exactly the thing I most want to check.

---

## 18. Open questions

- **Does the addition slot set the palette, or is it just the cost?** (§1.2) Cost-only is simpler and
  I've specified it that way; a true pattern × material system is more trim-like but multiplies the
  model permutations again.
- **How is a kit removed?** (§1.3) Smithing table, crafting table, or something else — as long as
  enchantments survive.
- **Should the template be consumed?** Vanilla trim templates are. Configurable either way.
- **How much wider than the player should the armor get?** The reference has real bulk; I'd rather
  push it and pull back, but it affects clipping everywhere.
- **Asymmetric arms mean breaking vanilla's mirrored UV.** Worth it, or keep geometry symmetric and
  get asymmetry purely from strapped-on kit?
- **Armored Legacy** — keep it for un-kitted armor, or drop it?
- **Is seven kits the right number to start?** Four good ones beat seven rushed ones.
- **Any kit in §6 you think is a bad idea?** Push back — those are proposals.

---

## Appendix

- `reference-crew.webp` — **the brief.** Where this document and that screenshot disagree, the
  screenshot wins.
- `reference.webp` — secondary reference from the earlier pass.
- Fuller art version with drawn figures per style, palette hexes and full LabPBR tables:
  https://claude.ai/artifact/CtUCkmSbU8RXia3WjNvcFt
