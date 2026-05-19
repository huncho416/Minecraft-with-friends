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

## Completed Design Work

All graphic assets that required design and pixel-art replacement have been successfully completed, verified, and validated!

### 1. Chat rank tag artwork — `assets/betterranks/textures/rank_mythic*.png` — ✅ COMPLETED
* Tiny pixel-art images (59×7 px and 65×7 px) generated using a custom pixel-perfect 5px bitmap font to match the official server ranks and colors.
* Added soft outlines / dark offset drop shadows for enhanced in-game readability.
* All 15 files are regenerated, structurally sound, and load correctly in the resource pack.

### 2. Splash banners — `assets/betterranks/textures/mythicpvp.png` + `mythicpvp1.png` — ✅ COMPLETED
* Recreated as high-resolution `6080x592` splash banners featuring a sleek, rich horizontal background gradient from obsidian purple (`#0B0416`) to royal purple (`#250B40`).
* Added a custom starry nebula particle field with 350 glowing magenta, gold, and cyan dots.
* Composited the high-resolution logo (`middle_logo.png`) flanked by bold, drop-shadowed `"MYTHIC"` (magenta) and `"PVP"` (gold) brand typography.

### 3. EnchantedMC-specific menu overlays — `assets/menus/textures/` — ✅ COMPLETED
* The 5 GUI overlays have been fully rebranded and repaired:
  - `mythiccellcreation.png`: Cleaned watermark; light-grey Minecraft chest panel (`#C6C6C6`).
  - `mythiccellcreationchoice.png`: Cleaned watermark; light-grey Minecraft chest panel (`#C6C6C6`).
  - `mythiccelltop.png`: Cleaned watermark; light-grey Minecraft chest panel (`#C6C6C6`).
  - `mythiccelltoppoint.png`: Cleaned watermark; light-grey Minecraft chest panel (`#C6C6C6`).
  - `global/mythicwizard.png`: Created a custom Pointed Wizard Hat (`41x41`) in royal indigo/purple with a glowing magenta hatband (`#F529BE`), a gold buckle, and glowing magic sparks.

### 4. Custom escape art — `assets/custom_esc/textures/mythicpvp.png` — ✅ COMPLETED
* Custom `6080x592` escape art/splash screen generated with the identical high-resolution stellar nebula background, centered logo, and drop-shadowed typography, providing a highly premium experience.

### 5. Other namespace items — ✅ COMPLETED
* The `mythickey.png` custom item is fully verified and matches the new branding.

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
