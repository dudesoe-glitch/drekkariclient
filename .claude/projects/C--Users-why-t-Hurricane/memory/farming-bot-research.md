---
name: farming-bot-research
description: Complete research for building an auto-farming bot — crop growth detection, harvest mechanics, planting mechanics, inventory seed search, existing bot patterns, resource names, AUtils helpers, FlowerMenu usage, GameUI registration
type: project
---

# Farming Bot Research Report

## 1. Crop Growth Detection

### How GobGrowthInfo Works

Crops are identified by `Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")`. For crops only (not trees/bushes), the growth stage is read from the gob's `ResDrawable.sdt` stream:

```java
// Extract stage from drawable data
Drawable dr = gob.getattr(Drawable.class);
ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
Message data = d.sdt.clone();
int stage = data.uint8();  // First byte is the growth stage
```

The max stage is determined by iterating the resource's `FastMesh.MeshRes` layers: `layer.id / 10` gives the stage index; `maxStage` is the highest.

### Mature Stage Detection by Crop Type

There are three maturity rules hardcoded in `GobGrowthInfo`:

| Crop | Mature/Harvestable at |
|------|-----------------------|
| carrot | `stage == maxStage` (FINAL_STAGE_DOT = red dot) |
| turnip, leek | `stage == maxStage` (FINAL_STAGE_DOT = red dot) |
| all other GrowingPlants | `stage == maxStage` |

**Key insight:** For ALL regular crops, `stage == maxStage` means fully grown and ready to harvest. The SEEDS_STAGE_DOT (blue dot) shown for carrot at `maxStage - 1` and turnip/leek at `maxStage - 2` is just a visual warning, not the harvest trigger.

### Gob `sdt()` Helper

`Gob.sdt()` returns `dw.sdtnum()` — this is the first integer of the sdt stream, which for crops IS the growth stage. So `gob.sdt()` gives the current stage directly.

### Utility Method for Maturity Check

A clean helper for the bot:
```java
public static boolean isCropMature(Gob gob) {
    if (!Utils.isSpriteKind(gob, "GrowingPlant")) return false;
    Drawable dr = gob.getattr(Drawable.class);
    if (!(dr instanceof ResDrawable)) return false;
    Message data = ((ResDrawable) dr).sdt.clone();
    int stage = data.uint8();
    int maxStage = 0;
    for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
        if (layer.id / 10 > maxStage) maxStage = layer.id / 10;
    }
    return stage >= maxStage;
}
```

Note: Trellis plants (wine, pepper, hops, peas, cucumber) use `TrellisPlant` kind, not `GrowingPlant`. The bot should focus on `GrowingPlant` for field crops.

## 2. GobReadyForHarvestInfo (Trees/Bushes Only)

`GobReadyForHarvestInfo` is for **trees and bushes only** (NOT crops). It shows icons for when fruit/seeds/leaves are ready. It does NOT apply to field crops (`gfx/terobjs/plants/`).

For trees: `gob.sdt() & 1` — if bit 0 is NOT set (i.e., `(sdt & 1) != 1`), the tree has seeds ready. This is not needed for the farming bot.

## 3. Existing Bot Patterns

### CleanupBot Pattern (Primary Template)
`src/haven/automated/CleanupBot.java` — most relevant template for a farming bot.

Key pattern:
1. `findClosestGob()` — iterates `gui.map.glob.oc` synchronized, finds nearest matching gob
2. Walk to it: `gui.map.pfLeftClick(gob.rc.floor().add(20, 0), null)` then `AUtils.waitPf(gui)`
3. Check proximity: `gob.rc.dist(gui.map.player().rc) < 11 * 5`
4. Interact: `FlowerMenu.setNextSelection("Harvest")` + right-click wdgmsg
5. Wait while working: poll `gui.prog`
6. Stop/close: `stop = true` flag, `gui.cleanupBot = null`, `reqdestroy()`

### HarvestNearestDreamcatcher Pattern (Harvest with FlowerMenu)
`src/haven/automated/HarvestNearestDreamcatcher.java` — exact harvest mechanic:

```java
FlowerMenu.setNextSelection("Harvest");
gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0,
    (int) gob.id, gob.rc.floor(posres), 0, -1);
// Wait for inventory to gain an item
int initialSpace = gui.maininv.getFreeSpace();
long now = System.currentTimeMillis();
while (initialSpace <= gui.maininv.getFreeSpace() && System.currentTimeMillis() - now < 500) {
    Thread.sleep(40);
}
```

### CloverScript Pattern (Item-in-Hand + itemact on Ground/Gob)
`src/haven/automated/CloverScript.java` — perfect template for PLANTING (pick up seed, use on ground):

```java
// 1. Find seed in inventory
WItem seedWidget = inv.getItemPrecise("Turnip Seed");
GItem seed = seedWidget.item;

// 2. Pick it up to cursor
seed.wdgmsg("take", new Coord(seed.sz.x / 2, seed.sz.y / 2));
// Wait for hand to be occupied
while (gui.hand.isEmpty() || gui.vhand == null) { Thread.sleep(8); }

// 3. Use item on world position (planting = itemact on ground tile)
gui.map.wdgmsg("itemact", Coord.z, targetPos.floor(posres), 0, 0, ...);
// OR for planting on an empty tile (no gob):
gui.map.wdgmsg("itemact", Coord.z, targetPos.floor(posres), 0);
```

### DestroyNearestTrellisPlantScript Pattern
Minimal single-shot: find nearest gob, interact with it, nullify thread. Good for the "one-shot per call" pattern.

## 4. Crop Resource Names

### Field Crops (gfx/terobjs/plants/*)
All use `GrowingPlant` sprite kind. Resource path format: `gfx/terobjs/plants/<cropname>`

Known from codebase and game:
- `gfx/terobjs/plants/turnip`
- `gfx/terobjs/plants/carrot`
- `gfx/terobjs/plants/leek`
- `gfx/terobjs/plants/onion`
- `gfx/terobjs/plants/wheat`
- `gfx/terobjs/plants/rye`
- `gfx/terobjs/plants/barley`
- `gfx/terobjs/plants/poppy` (probably)
- `gfx/terobjs/plants/hemp`
- `gfx/terobjs/plants/beet`

Trellis plants (different sprite kind `TrellisPlant`, NOT for auto-farming):
- `gfx/terobjs/plants/wine`, `pepper`, `hops`, `peas`, `cucumber`

Giant variants exist: `gfx/terobjs/plants/giantturnip` — the game checks `!res.name.endsWith("giantturnip")` for hiding purposes.

### How to Detect ANY Farmable Crop
```java
res.name.startsWith("gfx/terobjs/plants/")
    && !res.name.endsWith("trellis")
    && !res.name.endsWith("giantturnip")
    && Utils.isSpriteKind(gob, "GrowingPlant")
```

### Extracting Crop Basename for Seed Lookup
`gob.getres().basename()` — e.g., `"turnip"` from `"gfx/terobjs/plants/turnip"`.
Then seed resource: `"gfx/invobjs/seed-" + basename` (e.g., `"gfx/invobjs/seed-turnip"`).

## 5. Seed Inventory — Finding Seeds by Resource Name

**IMPORTANT:** `getItemsPartial()` and `getItemsExact()` search by DISPLAY NAME (item.getname()), NOT resource name. For seeds, searching by display name "Seed" or "Turnip Seed" is simplest.

For resource-name-based search, use `AUtils.findItemInInv()`:
```java
WItem seed = AUtils.findItemInInv(gui.maininv, "gfx/invobjs/seed-turnip");
```
or iterate directly:
```java
for (WItem wi : gui.maininv.getAllItems()) {
    try {
        if (wi.item.resource().name.startsWith("gfx/invobjs/seed-")) { ... }
    } catch (Loading ignored) {}
}
```

### Best Seed Selection (Quality Picker)
To find the highest quality seed of a specific type:
```java
WItem bestSeed = null;
double bestQ = -1;
for (WItem wi : gui.maininv.getAllItems()) {
    try {
        if (wi.item.resource().name.equals("gfx/invobjs/seed-" + cropBasename)) {
            QBuff qb = wi.item.getQBuff();
            double q = (qb != null) ? qb.q : 0;
            if (q > bestQ) { bestQ = q; bestSeed = wi; }
        }
    } catch (Loading ignored) {}
}
```

## 6. Planting Mechanics

### How Planting Works
Planting in H&H is: pick up seed item to cursor, then left-click (or `itemact`) on the ground tile where you want to plant.

From `RefillWaterContainers` and other scripts, the `itemact` for ground tiles (no gob):
```java
gui.map.wdgmsg("itemact", Coord.z, targetGroundPos.floor(posres), 0);
```

The exact coordinates for replanting would be the same position where the harvested crop was (`harvestedGob.rc`), since crops grow in fixed field tiles.

**Critical:** The bot must:
1. `FlowerMenu.setNextSelection("Harvest")` + right-click the mature crop
2. Wait for harvest to complete (inventory space decreases, or `gui.prog` finishes)
3. Record the crop's `rc` position before it disappears
4. Find the best matching seed from inventory
5. Pick up seed: `seed.wdgmsg("take", Coord.z)`
6. Wait for hand occupied: `AUtils.waitForOccupiedHand(gui, 2000, "...")`
7. Plant: `gui.map.wdgmsg("itemact", Coord.z, harvestedPos.floor(posres), 0)`
8. Wait for empty hand: `AUtils.waitForEmptyHand(gui, 2000, "...")`

## 7. AUtils Helpers Relevant to Farming Bot

| Method | Use |
|--------|-----|
| `AUtils.getGobs(String name, GameUI gui)` | Get all gobs matching exact resource name |
| `AUtils.waitPf(GameUI gui)` | Wait for pathfinding to complete |
| `AUtils.unstuck(GameUI gui)` | Random clicks to unstuck player |
| `AUtils.waitProgBar(GameUI gui)` | Wait for progress bar (harvest animation) |
| `AUtils.waitForOccupiedHand(gui, timeout, err)` | Wait until cursor holds item |
| `AUtils.waitForEmptyHand(gui, timeout, err)` | Wait until cursor is empty |
| `AUtils.drinkTillFull(gui, threshold, stopLevel)` | Restore stamina |
| `AUtils.rightClick(GameUI gui)` | Cancel/deselect (right-click at player pos) |
| `AUtils.findItemInInv(Inventory inv, String resName)` | Find item by exact resource name |
| `AUtils.findItemByPrefixInAllInventories(gui, prefix)` | Find first item matching res prefix |

**Note:** There is NO `AUtils.getGobs(prefix)` — `getGobs()` does exact match only. For prefix-matching crops (`gfx/terobjs/plants/`), the bot must iterate `glob.oc` manually (like CleanupBot does).

## 8. FlowerMenu.setNextSelection()

Located at `src/haven/FlowerMenu.java` line 324:
```java
public static void setNextSelection(String name) {
    nextAutoSel = name;
}
```

`tryAutoSelect()` is called in `added()` (when the FlowerMenu widget is created). It searches `options[]` for an exact match with `nextAutoSel`, then calls `choose(getPetalFromName(option))`.

**CRITICAL:** `setNextSelection` must be called BEFORE the right-click that opens the FlowerMenu, because `tryAutoSelect()` fires immediately when the menu appears.

**Usage pattern:**
```java
FlowerMenu.setNextSelection("Harvest");
gui.map.wdgmsg("click", Coord.z, crop.rc.floor(posres), 3, 0, 0,
    (int) crop.id, crop.rc.floor(posres), 0, -1);
```

If the option is NOT found, `choose(null)` is called (dismisses menu), and `nextAutoSel` is cleared.

**Exact flower menu option names for crops:**
- Harvest: `"Harvest"` (standard for crops)
- Pick: `"Pick"` (for forageables, herbs — NOT for planted crops)
- Plant: crops are planted by holding seed and left-clicking, NOT via flower menu

## 9. GameUI Bot Registration Pattern

### Existing Bot Fields (lines 177-191 in GameUI.java)
```java
// Bot Threads
public OceanScoutBot OceanScoutBot;
public Thread oceanScoutBotThread;
public TarKilnCleanerBot tarKilnCleanerBot;
public Thread tarKilnCleanerThread;
public CleanupBot cleanupBot;
public Thread cleanupThread;
public GrubGrubBot grubGrubBot;
public Thread grubGrubThread;
public CellarDiggingBot cellarDiggingBot;
public Thread cellarDiggingThread;
public RoastingSpitBot roastingSpitBot;
public Thread roastingSpitThread;
public FishingBot fishingBot;
public Thread fishingThread;
```

**For farming bot, add:**
```java
public FarmingBot farmingBot;
public Thread farmingBotThread;
```

### Bot Window Registration (wdgmsg "close" handler in the Bot)
```java
@Override
public void wdgmsg(Widget sender, String msg, Object... args) {
    if ((sender == this) && (Objects.equals(msg, "close"))) {
        stop();
        reqdestroy();
        gui.farmingBot = null;      // null the GameUI reference
    } else {
        super.wdgmsg(sender, msg, args);
    }
}
```

### stop() Method Pattern
```java
public void stop() {
    stop = true;
    if (ui.gui.map.pfthread != null) {
        ui.gui.map.pfthread.interrupt();
    }
    if (gui.farmingBotThread != null) {
        gui.farmingBotThread.interrupt();
        gui.farmingBotThread = null;
    }
    this.destroy();
}
```

### Window Position Persistence
```java
@Override
public void reqdestroy() {
    Utils.setprefc("wndc-farmingBotWindow", this.c);
    super.reqdestroy();
}
```

### How Bots Are Started (in GameUI, wherever the button/menu is wired)
```java
gui.farmingBot = new FarmingBot(this);
add(gui.farmingBot, Utils.getprefc("wndc-farmingBotWindow", new Coord(200, 200)));
gui.farmingBotThread = new Thread(gui.farmingBot, "FarmingBot");
gui.farmingBotThread.start();
```

## 10. Complete Farming Bot Loop Design

Based on all research, the farming bot loop should be:

```
Phase 1: HARVEST
  while mature crops exist in glob.oc:
    find nearest mature GrowingPlant
    pfLeftClick(crop.rc) + waitPf
    check stamina, drink if needed
    record crop.rc (for replanting)
    record crop.getres().basename() (for seed match)
    FlowerMenu.setNextSelection("Harvest")
    right-click crop
    waitProgBar(gui)  // harvest animation
    sleep(300)        // brief settle

Phase 2: REPLANT
  for each harvested position (stored list):
    if replantEnabled && have matching seed in inventory:
      find best quality seed (gfx/invobjs/seed-<basename>)
      pfLeftClick(position) + waitPf
      seed.wdgmsg("take", Coord.z)
      waitForOccupiedHand(gui, 2000, ...)
      gui.map.wdgmsg("itemact", Coord.z, position.floor(posres), 0)
      waitForEmptyHand(gui, 2000, ...)
      sleep(200)
```

### Key Design Decision: Crop Position After Harvest
When a crop is harvested, the `Gob` is removed from `glob.oc`. Therefore, `gob.rc` must be captured BEFORE issuing the harvest command.

### Energy/Stamina Safety Checks
From CleanupBot: check `gui.getmeter("nrj", 0).a < 0.25` (energy) and `gui.getmeter("stam", 0).a < 0.40` (stamina). Use `AUtils.drinkTillFull()` when stamina is low.

## 11. Relevant Source File Locations

| File | Path |
|------|------|
| GobGrowthInfo | `src/haven/GobGrowthInfo.java` |
| GobReadyForHarvestInfo | `src/haven/GobReadyForHarvestInfo.java` |
| AUtils | `src/haven/automated/AUtils.java` |
| CleanupBot (template) | `src/haven/automated/CleanupBot.java` |
| HarvestNearestDreamcatcher | `src/haven/automated/HarvestNearestDreamcatcher.java` |
| CloverScript (planting template) | `src/haven/automated/CloverScript.java` |
| DestroyNearestTrellisPlantScript | `src/haven/automated/DestroyNearestTrellisPlantScript.java` |
| FlowerMenu | `src/haven/FlowerMenu.java` |
| GameUI (bot fields ~line 177) | `src/haven/GameUI.java` |
| Inventory (getItems*, getFreeSpace) | `src/haven/Inventory.java` |
| GItem (getQBuff, resource, take) | `src/haven/GItem.java` |
| GrowingPlant sprite | `src/haven/res/lib/plants/GrowingPlant.java` |
