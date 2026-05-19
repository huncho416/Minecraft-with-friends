# MythicPvP Resource Pack — Designer Handoff

The pack is now functionally rebranded. Everything that could be automated has been done. This document lists the remaining graphic-design work that needs a human in Aseprite / Photoshop / GIMP.

## What's already done (automated)

✅ Stripped namespaces: `workshop_six`, `dungeons`, `gens`, `factories` (~4 MB saved)
✅ Pack icon `pack.png` regenerated from `../Mythic Graphics/middle_logo.png` (repo-root `Mythic Graphics/`; 128×128, aspect-preserved bicubic scale, RGBA transparency intact)
✅ `pack.mcmeta` description rebranded to `"§dMythicPvP §6Resource Pack"`; all 8 overlay declarations preserved (1.21.2 → 26.1 protocol support)
✅ All 117 lang files: `EnchantedMC` → `MythicPvP` find/replace (`menu.returnToGame`, etc.)
✅ 30 brand-specific PNGs renamed:
   - `betterranks/`: `enchantedmc*` → `mythicpvp*`, `enchanted{dust,glow,image,rarity,plusrarity}` → `mythic*`, `rank_enchanted{,plus,pluswhite,plus2-13}` → `rank_mythic*`
   - `menus/`: `enchantedcell*` → `mythiccell*`, `enchantedwizard` → `mythicwizard`
   - `custom_esc/`: `enchantedmc.png` → `mythicpvp.png`
   - `other/`: `enchantedkey.{png,json}` → `mythickey.{png,json}`
✅ All JSON references (font/default.json, font/uniform.json, model JSONs, atlas JSONs) updated to point at the renamed PNGs across base + 8 overlay directories
✅ Vanilla "enchanted_" item names left alone (`enchanted_book`, `enchanted_glint`, `enchanted_hit` particle)
✅ Existing custom Mythic content merged in (`assets/mythic/`: breach, density, mace + the 157 custom files in `assets/minecraft/`)

**Final pack size:** 27 MB (down from 31 MB).

---

## What still needs designer work

### 1. Chat rank tag artwork — `assets/betterranks/textures/rank_mythic*.png`

These are tiny pixel-art images (59×7 px or 65×7 px) with rank text baked into the pixels — they currently still say "ENCHANTED" and "ENCHANTED+" in EnchantedMC's pixel art. The PNGs were *renamed* to `rank_mythic*` so the references work, but the actual pixels still show the old text.

I deliberately did **not** auto-overlay text via Java AWT — at 7-pixel height the result looks amateur next to hand-drawn pixel art. This needs a pixel artist.

**Files to redraw** (15 PNGs, all 7px tall to match Minecraft chat font height):

| File | Current size | Suggested Mythic rank to depict |
|------|-------------:|---------------------------------|
| `rank_mythic.png` | 59×7 | `[INITIATE]` (base donator tier) |
| `rank_mythicplus.png` | 65×7 | `[CHAMPION]` |
| `rank_mythicplus2.png` | 65×7 | `[ELITE]` |
| `rank_mythicplus3.png` | 65×7 | `[LEGEND]` |
| `rank_mythicplus4.png` | 65×7 | `[TITAN]` |
| `rank_mythicplus5.png` | 65×7 | `[MYTHIC]` (top donator) |
| `rank_mythicplus6.png` | 65×7 | `[HELPER]` |
| `rank_mythicplus7.png` | 65×7 | `[MOD]` |
| `rank_mythicplus8.png` | 65×7 | `[SRMOD]` |
| `rank_mythicplus9.png` | 65×7 | `[ADMIN]` |
| `rank_mythicplus10.png` | 65×7 | `[SRADMIN]` |
| `rank_mythicplus11.png` | 65×7 | `[MANAGER]` |
| `rank_mythicplus12.png` | 65×7 | `[DEVELOPER]` |
| `rank_mythicplus13.png` | 65×7 | `[OWNER]` |
| `rank_mythicpluswhite.png` | 65×7 | Generic white-text variant template |

There are also pre-existing **"MYTHICAL" tier** PNGs from EnchantedMC's own pack that you may want to repurpose:

| File | Size | Notes |
|------|-----:|-------|
| `mythical.png` | 53×7 | Generic MYTHICAL chat tag |
| `mythicalbook.png` | 16×16 | MYTHICAL book icon |
| `mythicalrarity.png` | 59×11 | Item rarity tag |
| `mythicalplusrarity.png` | 66×11 | Item rarity+ tag |

**Width tip:** the visible text is 5-pixel-wide-per-letter at this scale. Width budget: 59px = ~10-letter rank, 65px = ~11-letter rank. Anything longer needs a new wider canvas.

**Color tip:** keep each tier in its Mythic-palette color (verify against `mythic-core/src/main/resources/ranks.yml` — each rank has a `color: "#RRGGBB"` field). Default tier colors:
- INITIATE/CHAMPION/ELITE: cool palette (`#9CC3FF`, `#9CFF9C`, `#FFEC8A`)
- LEGEND/TITAN/MYTHIC: bold palette (`#FFD700`, `#FF1493`, `#F529BE`)
- Staff: red→purple gradient (`#FF8A8A` → `#F529BE`)

### 2. Splash banners — `assets/betterranks/textures/mythicpvp.png` + `mythicpvp1.png`

Both are **6080×592** PNGs — almost certainly full-width splash banners that say "ENCHANTEDMC" in giant letters across the screen. Currently still showing EnchantedMC artwork.

**Action:** open in Photoshop, redraw "MYTHICPVP" using the same letter style, save back. Or replace entirely with a scaled-up render of `Mythic Graphics/middle_logo.png` if you prefer the new branding direction.

### 3. EnchantedMC-specific menu overlays — `assets/menus/textures/`

5 GUI overlays were renamed but their pixel content is still EnchantedMC artwork:

- `mythiccellcreation.png`
- `mythiccellcreationchoice.png`
- `mythiccelltop.png`
- `mythiccelltoppoint.png`
- `global/mythicwizard.png`

These are full menu backgrounds rendered behind inventory GUIs. Audit each in an image editor and replace any visible EnchantedMC text/logos.

There are also **120 other menu PNGs** in `assets/menus/textures/` that were *not* renamed — most are content-neutral (auction GUI, friend menu, splash screens) but may have an EnchantedMC banner strip somewhere. Spot-check the obvious candidates first.

### 4. Custom escape art — `assets/custom_esc/textures/mythicpvp.png`

Single 2.4 MB texture, likely a large branded background/splash. Audit and redraw.

### 5. Other namespace items

- `assets/other/textures/mythickey.png` — custom key item. Probably a small 16×16 icon. Verify it doesn't have EnchantedMC text.

---

## Things to verify (non-blocking)

- **Color palette match:** EnchantedMC's pink ≈ Mythic's `#F529BE` but not exactly. If pixel-perfect brand matching matters, batch hue-shift the renamed PNGs from EnchantedMC pink → Mythic pink. ImageMagick `-modulate 100,100,98` or similar can do this once you've installed it.
- **Content packs you might still strip:** `easter2024set`, `valentineset`, `easter_cos`, all `lanshan_*`, `crystal_creations` are seasonal — enable only during their event windows or strip entirely if you're not running those events.
- **Resource pack hash:** once final art is in, regenerate the pack zip, compute SHA-1, update wherever the resource-pack URL lives in `mythic-core` (search for `setBedrockPackInfo` / `setResourcePack`).

---

## Folder layout after rebrand

```
TexturePack/
├── pack.mcmeta              ← rebranded, overlays preserved
├── pack.png                 ← Mythic logo, 128×128
├── README.txt               ← (already correct)
├── credits.txt              ← (already correct)
├── DESIGNER_TODO.md         ← this file
├── REBRAND_INVENTORY.md     ← original planning doc
├── assets/                  ← 36 namespaces (was 41, stripped 4 + merged Mythic custom)
│   ├── minecraft/           ← vanilla overrides + lang (rebranded)
│   ├── betterranks/         ← chat tags (renamed, pixels TODO)
│   ├── menus/               ← GUI overlays (renamed, pixels TODO)
│   ├── mythic/              ← our existing custom items (breach, density, mace)
│   └── …                    ← armor, cosmetics, etc.
├── ia_overlay_*/            ← 8 multi-version overlays (refs updated)
├── overlay_*/               ← 2 modern overlays (refs updated)
└── Texture Pack Examples/   ← original reference base (gitignored — third-party EnchantedMC pack)
    └── enchantedmc/
```

Source logos used to generate `pack.png` live at the repo root under `Mythic Graphics/` (not inside `TexturePack/`).
```

**Total Mythic pack size:** 27 MB. **Files needing pixel-art work:** ~25 (listed above).
