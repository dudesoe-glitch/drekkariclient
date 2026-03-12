---
name: extinventory-features-research
description: Research on 4 EnderWiggin features to port to Hurricane: repeat-action-for-group, ItemType metadata, WItem badge rendering, and new Action.java entries
type: project
---

# Research: EnderWiggin Feature Port Candidates

## 1. Repeat-Action-for-Group in ExtInventory

### How it works in EnderWiggin

The feature is powered by a **reactive event bus** (`Reactor.FLOWER_CHOICE`) that publishes every flower menu selection globally. `ItemsGroup` (the list row widget inside ExtInventory's panel) subscribes to this bus on construction, and unsubscribes automatically on dispose.

The toggle is `chb_repeat` (an `ICheckBox` labeled "Toggle repeat mode") in the ExtInventory toolbar.

When the user right-clicks any item and picks a flower menu option:

1. `FlowerMenu.choose()` fires: it publishes a `FlowerMenu.Choice(opt, target, forced)` to `Reactor.FLOWER_CHOICE`
2. Each `ItemsGroup.flowerChoice()` receives it
3. The group checks:
   - `extInventory.chb_repeat.a` — repeat mode is on
   - `!choice.forced` — the user actually picked the option (not auto-selected by a bot)
   - `choice.opt != null` — a real option was chosen
   - `Targets.item(choice.target) == sample` — the item that was right-clicked is this group's representative (the highest-quality item, `items.get(0)`)
4. If all checks pass: it unsubscribes from FLOWER_CHOICE to avoid processing the cascade it's about to trigger, collects all other items in the group (filtering out `sample`), and calls `Actions.selectFlowerOnItems(ui.gui, choice.opt, targets)`
5. `Actions.selectFlowerOnItems` creates `ItemTarget` for each, runs a `Bot.process` pipeline: `ITarget::rclick` then `BotUtil.selectFlower(option)` — i.e., right-clicks each item then auto-selects the same flower option

### Key classes and files
- `src/haven/ExtInventory.java` — `ItemsGroup.flowerChoice()` (subscriber method, lines ~388-397)
- `src/haven/rx/Reactor.java` — `FLOWER_CHOICE` static `PublishSubject<FlowerMenu.Choice>`
- `src/haven/FlowerMenu.java` — publishes `Reactor.FLOWER_CHOICE.onNext(choice)` in `choose()`; `Choice` class has `opt`, `target`, `forced` fields
- `src/auto/Actions.java` — `selectFlowerOnItems(GameUI, String option, List<WItem>)` runs the bot sequence

### What Hurricane needs to add to port this

Hurricane does NOT have:
- `Reactor` / reactive event bus (RxJava is not in Hurricane's dependencies)
- `FlowerMenu.Choice` class
- `ITarget` / `Bot.process` pipeline framework

**What Hurricane DOES have** that is equivalent:
- `AutoRepeatFlowerMenuScript` — already tracks the last flower option clicked (via `FlowerMenu.lastFlowerMenuOption`/`lastFlowerMenuItemRes`)
- `ExtInventoryWindow` — has groups via `GroupData`, items via `ItemRow`

### Realistic Hurricane implementation without RxJava

Instead of a reactive bus, use a direct callback. Hurricane's `FlowerMenu` already has a static `setNextSelection()` mechanism. A simpler approach:

1. Add a static `FlowerMenu.lastChosenOption` field (String, thread-safe) that `FlowerMenu.choose()` sets when an item (not a gob) is the target and `!forceChosen`
2. `ExtInventoryWindow` has a "Repeat" checkbox toggle
3. When the user right-clicks an item row (its `ItemRow.mousedown`), if repeat mode is on, after the first right-click completes, queue up right-clicks on the rest of the group items using `FlowerMenu.setNextSelection(lastChosenOption)` before each

The timing issue is that you can't right-click all items instantly — you must wait for each flower menu to open, auto-select, wait for action, then proceed. This is effectively a bot loop.

**Simplest viable approach**: A "Repeat last action for group" button that, when clicked after a flower action was performed on one group item, runs a bot loop over the remaining items using `FlowerMenu.setNextSelection` + right-click pattern (same as FarmingBot's Harvest loop).

**Complexity: Medium** — 3-4 hours. Needs a bot loop and a way to know which group the "sample" item belongs to.

---

## 2. ItemType Metadata Enrichment

### What EnderWiggin has in ExtInventory

EnderWiggin's ExtInventory uses a **private inner class** called `ItemType` (not a global enum) that captures:
- `name` — display name from `WItem.name`
- `resname` — resource path from `WItem.item.resname()`
- `quality` — optional Double (bucketed or exact, based on Grouping mode)
- `matches` — whether the item passes the active `ItemFilter`
- `alchemyMatches` — whether the item passes the alchemy filter
- `color` — the `olcol` color overlay (from `GItem.ColorInfo`, e.g., filter highlight color)
- `state` — `Pipe.Op` for color-masked rendering
- `cacheId` — `"resname@name"` string used as icon cache key
- `loading` — true if name starts with "???"

The `compareTo` on `ItemType` sorts groups by: filter matches first → alchemy matches → overlay color → name → resname → quality descending.

### What Hurricane's `ItemType` enum already has

Hurricane's `ItemType.java` (Session 11): `Food, Armor, Curiosity, Tool, Seed, Container, Material, Weapon, Unknown` — with `classify(GItem)`, `color()`, `label()`.

Hurricane's `WItem.ItemCategory` (inner enum in WItem.java): `Food, Armor, Curiosity, Tool, Weapon, Seed, Container, Material` — with inline `AttrCache`-based detection, used for badge dots.

### What EnderWiggin adds that Hurricane doesn't have

1. **Quality bucketing modes** in grouping: `NONE/Q/Q1/Q5/Q10` — group items together if their quality is within the same 1/5/10 unit bucket. Hurricane's `ItemGrouping` does `BY_QUALITY` (exact quality as group key). Adding `BY_Q5` and `BY_Q10` grouping modes to `ExtInventoryWindow` is straightforward.

2. **`olcol` color grouping**: items with different item filter highlight colors appear as separate groups in the list, and the color is rendered as an overlay on the icon. This requires `WItem.olcol` AttrCache (Hurricane doesn't have this — it's the color from `GItem.ColorInfo` which comes from a server-pushed overlay color). This is a low-priority item since it depends on server protocol data.

3. **Filter match ordering**: groups whose items match the active filter are sorted to the top of the list. Hurricane doesn't do this in ExtInventoryWindow yet.

4. **Stack unpacking**: items that are stacks get their contents unpacked into the group list (if a stack contains multiple item types, each appears as a separate group row). Controlled by `CFG.UI_STACK_EXT_INV_UNPACK`. Hurricane doesn't have this.

5. **Curiosity info in list row**: `DisplayType.Info` renders `lph: X mw: Y` for curiosities in the list view. Hurricane's ExtInventoryWindow doesn't do per-category info rendering in rows.

### Complexity
- Q5/Q10 grouping: **Low** — 30 min (add two `ItemGrouping` enum values, bucket the quality key)
- Filter-match ordering: **Low** — 1 hour (add a sort key based on `GItem.filter != null && filter.matches(item.info())`)
- Curiosity info row: **Medium** — 2 hours (detect Curiosity info type, render lph/mw in the row label)
- Stack unpacking: **Medium-High** — 3 hours (recurse into `GItem.contents` children)

---

## 3. WItem Badge Display Comparison

### EnderWiggin WItem draw() pipeline

EnderWiggin's `WItem.draw()` calls these helper methods in order:
1. `drawmain(g, spr)` — draws the sprite
2. Check `item.matches()` → magenta rect border (active filter highlight)
3. Check `item.alchemyMatches()` → alchemy mark image (bottom-left corner)
4. `itemols.get()` → draws `GItem.InfoOverlay` renders (server-pushed overlays)
5. `drawbars(g, sz)` — left-edge wear bar (4px wide, height proportional to durability)
6. `drawnum(g, sz)` — bottom-right text: chain of `heurnum` → `armor` → `durability` (first non-null wins)
7. `drawmeter(g, sz)` — pie chart for curiosity meter OR study-time countdown text
8. `drawq(g)` — top-right quality number from `QualityList`

Key AttrCaches in EnderWiggin WItem (not in Hurricane WItem):
- `olcol` — `Color` from `GItem.ColorInfo` (server-pushed tint)
- `fullness` — `Level` (fullness level for food dishes)
- `itemmeter` — `Double` from `GItem.MeterInfo`
- `armor` — `Tex` with "hard/soft" armor values (shown in bottom-right)
- `durability` — `Tex` with remaining durability number
- `wear` — `Pair<Double, Color>` for the wear bar
- `curio` — `Curiosity` for LPH/MW display
- `study` — `Pair<String,String>` remaining study time tip
- `quantity` — `Float` item count (for stacks)

### Hurricane WItem draw() — what it already has

Hurricane has (from WItem.java read earlier):
- `heurnum` AttrCache (quantity/count text)
- `wear` AttrCache (durability bar colors)
- Category badges (ItemCategory enum, inline AttrCache, drawn as colored dots)
- Quality number display (via `GItem.getQBuff()`)
- Filter highlight (pulsing blue border via `searchItemColorValue`)
- Study time countdown (via `study` AttrCache)

### What EnderWiggin does differently

1. **Armor display**: EnderWiggin shows `hard/soft` armor values as text in the bottom-right corner (where durability number also goes), using an `AttrCache<Tex>` with an opt-in `CFG.SHOW_ITEM_ARMOR`. Hurricane does not show armor values on items.

2. **Durability number**: EnderWiggin shows the remaining durability as a number in the bottom-right, with a dedicated `DURABILITY_COLOR` (pale cyan). Hurricane shows a colored left-edge bar but no number.

3. **Wear bar style**: EnderWiggin draws a 4px-wide colored bar on the LEFT edge of the item (not bottom). Hurricane draws a bar on the bottom (check WItem ~line 75-80). Different convention.

4. **`SWAP_NUM_AND_Q` setting**: EnderWiggin has a setting to swap quality (top-right) and count (bottom-right) positions. Hurricane doesn't have this toggle.

5. **Progress number**: EnderWiggin can show `X%` text centered on the item instead of the pie meter, controlled by `CFG.PROGRESS_NUMBER`.

6. **`onRClick` listener list**: EnderWiggin adds a `List<Action3<WItem,Coord,Integer>> rClickListeners` so other systems can intercept right-clicks without subclassing. Hurricane doesn't have this hook.

7. **Category badges**: Hurricane's colored-dot badge system is UNIQUE to Hurricane — EnderWiggin has no equivalent item category badge display. This is a Hurricane original.

### Worthwhile ports for Hurricane
- **Armor value display** on items: **Low** — 2 hours (add `armor` AttrCache, draw in `drawnum`, add settings checkbox)
- **Durability number** on items: **Low** — 1 hour (add `durability` AttrCache, chain into `drawnum`)
- **SWAP_NUM_AND_Q setting**: **Low** — 1 hour (a pref toggle + conditional in draw())
- **`onRClick` listener**: **Low** — 30 min (useful for the repeat-group feature above)

---

## 4. New Action.java Entries (Not Yet in Hurricane)

EnderWiggin's `Action.java` has several entries Hurricane's `Actions` class doesn't have:

### Camera controls (all new)
```
CAM_ZOOM_IN / CAM_ZOOM_OUT — gui.map.zoomCamera(±1)
CAM_ROTATE_LEFT/RIGHT/UP/DOWN — gui.map.rotateCamera(Coord.left/right/up/down)
CAM_SNAP_WEST/EAST/NORTH/SOUTH — gui.map.snapCamera*()
CAM_RESET — gui.map.resetCamera()
```
These bind camera controls to KeyBindings. Very useful for players who want keyboard camera control. Hurricane has some camera controls but they may not all be keybound.

### Equipment quick-swap actions (partially new)
```
EQUIP_BOW — Equip.twoHanded(gui, Equip.BOW)
EQUIP_SPEAR — Equip.twoHanded(gui, Equip.SPEAR)
EQUIP_SWORD_N_BOARD — Equip.twoItems(gui, Equip.SHIELD, Equip.SWORD)
```
Hurricane already has "EquipFromBelt" keybinds (17 of them, Session 3). The `Equip` class in EnderWiggin is more sophisticated — it handles the case where both hands are full (puts one item back to belt first). Worth checking if Hurricane's version handles this edge case.

### Quick automation actions (partially new)
```
BOT_MOUNT_HORSE — whistle nearest domestic horse then mount
BOT_OPEN_GATE — right-click nearest gate within 3 tiles
TOGGLE_INSPECT — toggle "inspect mode" cursor
TRACK_OBJECT — toggle "tracking mode" cursor
```
`BOT_MOUNT_HORSE` is interesting — it whistle-summons the horse if far away (>20 units), waits for it to approach, then mounts. Hurricane has no equivalent.

`BOT_OPEN_GATE` is a dead-simple one-liner useful for automation.

### Gob info toggle actions (new category)
```
TOGGLE_GOB_INFO_PLANTS / TREE_GROWTH / TREE_CONTENT / ANIMAL_FLEECE / HEALTH / BARREL / SIGN / CHEESE / QUALITY / TIMER
```
These are individual toggles for each `GobInfoOpts.InfoPart`. Hurricane currently has `TOGGLE_GOB_INFO` as a single on/off. EnderWiggin's approach lets users enable only the specific overlays they want.

### Fueling actions
```
FUEL_SMELTER_9 / FUEL_SMELTER_12 — fuelGob with "Coal", 9 or 12 units
FUEL_OVEN_4 — fuelGob with "Branch", 4 units
```
These use `Actions.fuelGob()` which Hurricane's `Actions.java` already has. The Action enum entries just bind them to keybinds/menu. Easy to add.

### Complexity estimates for Action ports
- Camera controls: **Low** — 2 hours (add keybinds + check which `MapView` methods exist in Hurricane)
- BOT_OPEN_GATE: **Low** — 1 hour
- BOT_MOUNT_HORSE: **Medium** — 3 hours (whistle logic + approach-wait loop)
- Per-part gob info toggles: **Medium** — 3 hours (requires splitting the single `DISPLAY_GOB_INFO` pref into per-part prefs, then wiring into `GobGrowthInfo`/`GobReadyForHarvestInfo`)
- Fuel quick-actions as keybinds: **Low** — 30 min

---

## 5. Other Notable Features Not Yet Researched

### `GobDamageInfo` (completely new to Hurricane)
Tracks damage dealt by the player vs. others to each `Gob`, rendered as a floating overlay above the gob showing SHP/HHP/Armor values. Uses `ConcurrentHashMap<Long, DamageVO>` keyed by gob ID. Data comes from `FightView` events. **Complexity: High** (6+ hours) — useful for PvP/combat players.

### `GobTimerData` (completely new to Hurricane)
Tracks smelting timers. When you right-click a smelter gob, the interaction is captured via `Reactor.GOB_INTERACT`. When the smelter window opens, it's linked to the gob. The remaining time (from item meter) is rendered as a countdown above the smelter on the map. **Complexity: Medium** (3-4 hours).

### `ContainerInfo` (JSON-driven, completely new to Hurricane)
`containers.json5` file lists gob resource names with their `full` and `empty` sdt bitmask values. `ContainerInfo.get(resname)` returns a `Container` with `isFull(sdt)` and `isEmpty(sdt)`. Used by `GobTag` to classify containers as FULL/EMPTY without hardcoding bit logic. This makes the gob tagging system data-driven and extensible without recompiling. **Complexity: Low-Medium** (2 hours + data entry).

### `GeneralGobInfo` (partially comparable to Hurricane's `GobGrowthInfo`)
EnderWiggin's single class handles: plant growth, tree growth (with %-from-start normalization), tree contents (seeds/leaves/bark/bough from overlay names), animal fleece status, health, barrel contents, quality number, cheese rack overlay, and a smelting timer. Hurricane has `GobGrowthInfo` and `GobReadyForHarvestInfo` which are narrower in scope. The richer version shows quality numbers on objects by parsing tooltip strings. **Worth studying** for adding quality display on world objects.

### `WindowDetector` + `WindowX`/`DecoX` (architecture, completely new)
EnderWiggin has a sophisticated window typing system — `WindowDetector.isWindowType(wdg, WND_STUDY, WND_SMELTER, ...)` checks parent window captions against known strings. `WindowX` and `DecoX` are extended Window classes that support per-window configuration (CFG stored per-window-type), `addtwdg()` for toolbar widgets, etc. This is EnderWiggin's way of attaching ExtInventory behavior to specific windows. Hurricane doesn't need this unless porting the full ExtInventory attachment mechanism.

### `Reactor` / RxJava (infrastructure, deep dependency)
EnderWiggin uses RxJava's `PublishSubject` for event buses: `FLOWER`, `FLOWER_CHOICE`, `WINDOW`, `EVENTS`, `GOB_INTERACT`, `EMSG`, `IMSG`, `PLAYER`. Hurricane would need to either add RxJava as a dependency or implement a simpler custom event bus (which is only ~30 lines for a basic pub/sub). The repeat-group feature specifically depends on `FLOWER_CHOICE`.

---

## Priority Summary

| Feature | File(s) | Complexity | Value |
|---------|---------|-----------|-------|
| Repeat-action for group in ExtInventory | ExtInventoryWindow.java | Medium (3-4h) | High |
| Q5/Q10 grouping modes | ExtInventoryWindow.java, ItemGrouping.java | Low (30min) | Medium |
| Filter-match group ordering | ExtInventoryWindow.java | Low (1h) | Medium |
| Armor/durability text on WItem | WItem.java | Low (2h) | Medium |
| SWAP_NUM_AND_Q setting | WItem.java, OptWnd | Low (1h) | Low |
| Camera keybinds | Action.java equivalents | Low (2h) | Medium |
| BOT_OPEN_GATE action | Actions.java | Low (1h) | Low |
| Fuel-smelter keybinds | Actions.java + keybinds | Low (30min) | Low |
| BOT_MOUNT_HORSE | Actions.java | Medium (3h) | Medium |
| Curiosity lph/mw in ExtInv rows | ExtInventoryWindow.java | Medium (2h) | Medium |
| GobTimerData (smelter countdown) | New class | Medium (3-4h) | Medium |
| ContainerInfo JSON system | New class + data | Low-Medium (2h) | Medium |
| GobDamageInfo (combat floaters) | New class | High (6h+) | Low-Medium |
| Per-part gob info toggles | GobGrowthInfo + prefs + OptWnd | Medium (3h) | Medium |
