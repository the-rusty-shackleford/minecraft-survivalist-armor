---
title: Survivalist Armor — decisions log
type: index
layer: store
tags: [index]
---

# Survivalist Armor — decisions log

**Append-only.** Never edit an entry's rationale; supersede via a new entry with
`supersedes: D-NNNN`. Every entry has a status: `Active` / `Superseded` / `Rejected`.

| Id | Topic |
|----|-------|
| D-0001 | A kit is a smithing template applied by our own recipe type; on the piece it is one component holding the kit's id, so the piece keeps everything else and an uninstall drops only the kit; shears take it off; drawn by one hook on the armour layer, the arm in first person, a trim by projection onto vanilla's armour box |
| D-0002 | The art is a clean-room match to Pixlli: measured numbers cross, Pixlli's pixels never do |
| D-0003 | Kitted item models are baked at runtime from the kit's own Blockbench project and drawn around Modefite, not generated as JSON permutations |
| D-0004 | Every kit keeps the same envelopes at the seams, so any pieces mix; the art tests hold them, the vocabulary, the budget and no cube buried, in `check`; the choices made building the seven |
| D-0005 | Modded armour takes kits by its slot tags; galestone and voidsteel have palettes; Create's backtanks are refused by default (their tank is drawn by a layer of its own) |
| D-0006 | EMI shows each kit's smithing recipe and the shears' removal, the piece and the result turning together |
| D-0007 | The bulk pass: one envelope every kit keeps (sleeves 0.5, bracers 1.0, shoulders and torsos pulled in, nothing where the arms swing), held by the art tests; D-0004's seams moved in with it; first person draws the kit's sleeve alone |
| D-0008 | The kits are stitched on an atlas of their own: on the block atlas, beside Pixlli's 128x, they doubled it to 16384x8192 (measured); a kitted item's particle icon stays its armour's own on the block atlas |
