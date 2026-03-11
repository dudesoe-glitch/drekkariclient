# CLAUDE.md — Hurricane (Haven & Hearth Custom Client)

## Project Overview

**Hurricane** is a custom game client for **Haven & Hearth**, a multiplayer survival/crafting MMO. It's a fork/mod of the official H&H client with extensive quality-of-life features, automation scripts, and UI improvements.

- **Language:** Java 15+ (target 15, supports up to 24)
- **Build System:** Apache Ant (`ant run` to build and launch)
- **Rendering:** OpenGL via JOGL / LWJGL
- **UI Framework:** Custom widget tree hierarchy (Widget base class)
- **Networking:** Custom socket protocol with resource caching
- **Storage:** SQLite for persistent data (routes, preferences)
- **Distribution:** GitHub releases + Steam Workshop

---

## Project Structure

```
src/
├── haven/                        # Main client code (380+ classes)
│   ├── automated/                # Bot scripts and automation
│   │   ├── AUtils.java           # Shared automation utilities
│   │   ├── *Bot.java             # Bot window classes (extend Window + Runnable)
│   │   ├── *Script.java          # One-shot automation scripts
│   │   ├── cookbook/              # Food/recipe database
│   │   ├── helpers/              # Hit boxes, targeting helpers
│   │   ├── mapper/               # World mapping
│   │   └── pathfinder/           # A* pathfinding
│   ├── render/                   # OpenGL rendering pipeline
│   ├── res/                      # Resource loading/caching
│   └── *.java                    # Core client classes
├── dolda/                        # Common utility framework
├── com/jcraft/                   # SSH/transport library
└── org/json/                     # JSON processing
```

### Key Files

| File | Purpose |
|------|---------|
| `GameUI.java` | Main game controller — manages all panels, inventory, chat, combat |
| `Inventory.java` | Grid-based inventory widget with drag-drop and quality sorting |
| `GItem.java` | Core item representation (quality, durability, sprites) |
| `WItem.java` | Visual item widget in inventory |
| `FlowerMenu.java` | Radial context menu (right-click actions like Harvest, Plant) |
| `Gob.java` | Game object (players, mobs, trees, crops, structures) |
| `MapView.java` | World map rendering and interaction |
| `OptWnd.java` | Options/settings window (massive — 5000+ lines) |
| `GobGrowthInfo.java` | Crop/tree growth stage tracking |
| `GobReadyForHarvestInfo.java` | Harvest readiness detection |
| `AutoDropManagerWindow.java` | Auto-drop items by quality threshold |
| `FlowerMenuAutoSelectManagerWindow.java` | Auto-select flower menu options |
| `CheckpointManager.java` | Route/waypoint automation |

---

## Architecture Patterns

### Widget System
- All UI elements extend `Widget`
- Linked-list tree: `child`, `lchild`, `next`, `prev`, `parent`
- Factory pattern via `@RName` annotation
- Event propagation: pointer, keyboard, query events flow through tree
- `wdgmsg()` sends messages to server (actions, interactions)

### Bot/Automation Pattern
- Bots extend `Window implements Runnable`
- Run on separate threads via `new Thread(bot).start()`
- GUI stores thread reference (e.g., `gui.cleanupBotThread`)
- Use `GameUI` reference for world access
- Stop via `stop = true` flag + thread interrupt
- `AUtils` provides shared helpers (find nearest gob, get player position, etc.)
- `FlowerMenu.setNextSelection("Action")` queues auto-selection before right-clicking

### Inventory System
- Grid-based: `Coord isz` defines slot grid size, `sqsz = 33x33` pixels
- Items tracked via `HashMap<GItem, WItem> wmap`
- Quality accessed via `item.getQBuff()` → `QBuff.q` (double)
- Item name via `item.res.get().name` (resource path like `gfx/invobjs/seed-turnip`)
- Transfer items: `item.wdgmsg("transfer", Coord.z)`
- Drop items: `item.wdgmsg("drop", Coord.z)`
- Existing comparators: `ITEM_COMPARATOR_ASC/DESC` (sort by quality)

### Game Object (Gob) System
- `Gob` = any world entity (player, animal, crop, tree, rock, building)
- Identified by resource name (e.g., `gfx/terobjs/plants/turnip`)
- Position via `gob.rc` (Coord2d)
- Growth info via `GobGrowthInfo` overlay
- Access all gobs: `gui.map.glob.oc` (OCache, synchronized)
- Interact: `gui.map.wdgmsg("click", ...)` with gob ID and position

---

## Building & Running

```bash
# Build and run
ant run

# Build only (staged distribution)
ant bin

# Clean build artifacts
ant clean
```

**Requirements:** Java 15+ (JDK), Apache Ant, internet connection (downloads game resources on first build)

---

## Coding Conventions

- Package: `haven` for core, `haven.automated` for bots/scripts
- Bot classes: `<Name>Bot extends Window implements Runnable`
- Script classes: `<Name>Script implements Runnable`
- UI scaling: `UI.scale(x, y)` for all pixel coordinates
- Thread safety: `synchronized` on shared collections (especially `glob.oc`)
- Resource names: `gfx/invobjs/<item>`, `gfx/terobjs/plants/<crop>`, `gfx/kritter/<animal>`
- Settings persistence: `Utils.setprefb/getprefb()` for booleans, `Utils.setprefi/getprefi()` for ints

---

## Development Guidelines

### Before Writing Code
1. Check existing patterns in `src/haven/automated/` — follow the established bot structure
2. Use `AUtils` helpers instead of reimplementing gob-finding, player-position, etc.
3. All inventory operations must handle `Loading` exceptions (resources load lazily)
4. Thread bot operations — never block the UI thread
5. Use `FlowerMenu.setNextSelection()` for automated right-click actions
6. Access quality via `GItem.getQBuff()` — may return null (check first)

### Key APIs
- **Find items in inventory:** `inventory.getItemsExact("name")` / `getItemsPartial("name")`
- **Get player inventory:** `gui.maininv` (main inventory widget)
- **Get all nearby gobs:** iterate `gui.map.glob.oc` (synchronized)
- **Player position:** `gui.map.player().rc`
- **Right-click gob:** `gui.map.wdgmsg("click", screenCoord, gobPos, button, modifiers, clickType, gobId, gobPos, overlayId, meshId)`
- **Wait for action:** `Thread.sleep()` in bot loops with interrupt checks
- **Check gob resource:** `gob.getres()` → `Resource.name`

---

## Upstream Sync (CRITICAL)

### Official H&H Source Code

The official client source is the foundation for all custom clients. When the game updates, Hurricane MUST be synced or it will crash.

- **Official source:** `git://sh.seatribe.se/hafen-client`
- **Source readme:** https://www.havenandhearth.com/portal/client-src-readme
- **Key files to watch for changes:** `Session.java` (protocol), `Resource.java` (asset loading), `Widget.java` (UI base), `Config.java` (config), `JOGLPanel.java` (rendering)
- **Dependencies:** Auto-downloaded to `lib/ext/` on first build. Force re-download: delete `lib/ext/jogl/has-jogl` or run `ant clean`

### Update Process

When H&H pushes a game update:
1. Pull the official source: `git pull git://sh.seatribe.se/hafen-client`
2. Diff against Hurricane's `src/haven/` core files (Session, Resource, Widget, Config, MainFrame, JOGLPanel)
3. Merge upstream changes into Hurricane — protocol and resource changes are mandatory
4. Rebuild: `ant clean && ant run`
5. Test: verify connection to official servers works

### Reference Client: EnderWiggin/hafen-client

Another custom H&H client with features worth studying and porting:

- **Repo:** https://github.com/EnderWiggin/hafen-client
- **Key differences from Hurricane:**

| Feature | Hurricane | EnderWiggin | Port Priority |
|---------|-----------|-------------|---------------|
| Bot framework | `Window+Runnable`, per-bot threads | `Bot` class + `Defer.Callable`, target-action pipeline | Study |
| Inventory sorting | Quality-only comparator | Multi-criteria: name → resource → sort value → quality (reversed) | **HIGH** |
| Extended inventory | None | `ExtInventory` — grouping, filtering, sort button, item type lists | HIGH |
| Item filtering | Name search only | `ItemFilter` — regex-based: quality, FEP, armor, curiosity, contents, food values | HIGH |
| Quality display | Single quality | `QualityList` — multiple quality types (All, Mean, Max), colored rendering | Medium |
| Auto-drop | Category + quality threshold | Per-item toggle via JSON config (`item_drop.json`) | Medium |
| Inventory helpers | `AUtils` (mixed gob+inv) | Separate `InvHelper` — `findFirstMatching`, `contains`, `isDrinkContainer`, `canBeFilledWith` | Study |
| Gob helpers | Manual `glob.oc` iteration | `GobHelper` + `GobTag` enum-based tagging | Study |
| Actions framework | Inline in each bot | `Actions` class — reusable `fuelGob`, `pickup`, `saltFood`, `refillDrinks` | Study |

**Key files to reference in EnderWiggin:**
- `src/auto/InventorySorter.java` — Multi-criteria sort with grid placement
- `src/auto/Bot.java` — Target-action bot framework with setup/cleanup phases
- `src/auto/Actions.java` — Reusable automation actions (fuelGob, pickup, saltFood, refillDrinks)
- `src/auto/InvHelper.java` — Inventory query helpers (findFirstMatching, contains, isDrinkContainer)
- `src/haven/ExtInventory.java` — Extended inventory UI with grouping (by Type, Quality, Q-brackets), repeat mode, sort button
- `src/haven/ItemFilter.java` — Powerful regex item search: `q>10`, `fep:str>1`, `armor:hard>1`, `has:water>2`, `lp>100`
- `src/haven/QualityList.java` — Multi-type quality tracking (RMS mean, average, min/max), colored rendering
- `src/haven/ItemAutoDrop.java` — Per-item JSON config auto-drop with drag-drop UI and "Filter Respect" toggle
- `src/haven/Action.java` — 50+ preset quick-actions enum (auto-equip, auto-pickup, gate toggle, etc.)
- `src/haven/PathQueue.java` — Waypoint movement queue with vehicle detection (auto-clears in boats/wagons)

**EnderWiggin does NOT have** (opportunities for Hurricane to be unique):
- No farming automation (no crop/seed/harvest code at all)
- No quality-based seed selection
- No macro/scripting system
- No persistent craft queues (only batch multiplier)

---

## Feature Roadmap (Custom Additions)

### Planned Features
- [ ] **Inventory sort by type + quality** — Multi-criteria comparator: group by item name, then sort by quality within groups. Reference: EnderWiggin's `InventorySorter.ITEM_COMPARATOR` (name → resource → sortValue → quality reversed)
- [ ] **Auto-farming bot** — Harvest mature crops, replant with highest quality seeds from inventory
- [ ] **Seed quality picker** — When replanting, automatically select the highest quality seed of the same type
- [ ] **Item filter search** — Port EnderWiggin's `ItemFilter` pattern: `q>10`, `fep:str>1`, `has:water`, `armor:hard>1`
- [ ] **Extended inventory panel** — Port EnderWiggin's `ExtInventory`: grouping by type, sort button, item list view
- [ ] **Per-item auto-drop** — Port EnderWiggin's JSON-config auto-drop (more flexible than Hurricane's category-based system)

---

## Reference: Existing Automation Features

| Feature | Location | What It Does |
|---------|----------|-------------|
| Auto-drop | `AutoDropManagerWindow.java` | Drop items below quality threshold during mining |
| Flower menu auto-select | `FlowerMenuAutoSelectManagerWindow.java` | Auto-pick flower menu options |
| Cleanup bot | `CleanupBot.java` | Chop bushes/trees, chip rocks, destroy stumps |
| Fishing bot | `FishingBot.java` | Automated fishing (569 lines, most complex bot) |
| Cellar digging | `CellarDiggingBot.java` | Automated cellar expansion |
| Roasting spit | `RoastingSpitBot.java` | Automated cooking on spit |
| Stack/Unstack | `StackAllItems.java`, `UnstackAllItems.java` | Inventory organization |
| Grub grub | `GrubGrubBot.java` | Automated foraging |
| Checkpoint routing | `CheckpointManager.java` | Waypoint-based auto-walking |
| Tar kiln cleaner | `TarKilnCleanerBot.java` | Automated tar kiln management |
| Refill water | `RefillWaterContainers.java` | Auto-refill water containers |

---

*This is a living document. Update as development progresses.*
