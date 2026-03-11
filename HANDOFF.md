# HANDOFF — Session 3 (Equipment Swap, Auto-Drop, Crafting, Filters, Inventory UI)

## Resumption Prompt
Implemented 5 new features: equipment quick-swap hotkeys (17 keybindings), per-item auto-drop (JSON config + UI), batch crafting "Craft N", expanded ItemFilter (FEP/armor/LP/wear/contents), and inventory toolbar (sort button + item count). Also verified 3 features already existed (minesweeper, drink refill, parasite auto-drop). All research doc priorities complete. Build clean with zero errors.

## Goal
Work through research document priorities + HANDOFF priorities, verifying what already exists before building.

## Completed

### New Features
1. **Equipment quick-swap hotkeys** (`GameUI.java`, `OptWnd.java`)
   - 17 KeyBinding entries (kb_equipB12, kb_equipCutblade, etc.), all default to unbound
   - globtype() handlers launch EquipFromBelt threads on hotkey press
   - OptWnd BindingPanel section: "Equipment Quick-Swap (from Belt)" with color-coded rows
     - Red = weapons (B12, Cutblade, Boar Spear, Giant Needle, swords+shields, bows)
     - Blue = tools (Pickaxe, Sledgehammer, Scythe, shovels)
     - Green = utility (Traveller's Sacks, Wanderer's Bindles)

2. **Per-item auto-drop** (`ItemAutoDrop.java`, `ItemAutoDropWindow.java`, `GItem.java`, `OptWnd.java`)
   - ItemAutoDrop: ConcurrentHashMap<String, Integer> per-item thresholds, JSON in prefs
   - ItemAutoDropWindow: scrollable list, add by name or "Add from Cursor", remove, edit threshold
   - GItem.checkAutoDropItem(): per-item check runs BEFORE category-based check (priority)
   - Button in OptWnd: "Per-Item Auto-Drop Config" below existing "Auto-Drop Manager"

3. **Batch crafting "Craft N"** (`Makewindow.java`)
   - TextEntry for count (persisted as "craftNCount" pref)
   - "Craft N" button starts daemon thread: sends wdgmsg("make", 0) N times with 650ms delay
   - "Stop" button interrupts the thread
   - Count persists across craft windows

4. **Expanded ItemFilter** (`ItemFilter.java`)
   - New patterns: fep, fep:TYPE, armor, armor:hard/soft, lp, lph, has:SUBSTANCE, wear
   - All support comparison operators: >, >=, <, <=, =, !=
   - FEP types match by prefix: str, agi, int, con, per, cha, dex, wil, psy
   - Backed by: FoodInfo.evs, Armor.hard/soft, Curiosity.exp/lph, Contents.content, Wear.percentage
   - Refactored compare() helper and ALL_PATTERNS array for hasNameFilter()/getNamePart()

5. **Inventory toolbar** (`GameUI.java`)
   - Sort button (calls maininv.sortInventory())
   - Search button (toggles InventorySearchWindow)
   - Live item count label (updated in tick()): "count/capacity" with color coding
     - White = normal, Yellow = >80% full, Red = completely full

### Verification (already existed)
6. **Minesweeper display** — MineSweeper toggle + duration config + menu grid button
7. **Drink refill** — RefillWaterContainers.java + auto-drink on low stamina
8. **Parasite auto-drop** — autoDropLeeches + autoDropTicks in Equipory.tick()

## In Progress
Nothing — all planned work complete.

## Next Priorities
1. **Test all new features in-game** — especially equipment hotkeys, per-item auto-drop, Craft N, item filters
2. **Batch animal butchering bot** — next forum priority (TIER 1 remaining)
3. **Flat terrain toggle** — TIER 2 forum feature
4. **FEP modifier tooltips** — TIER 2 forum feature
5. **OptWnd refactor** — extract 5600-line god class into separate Settings objects
6. **Extended inventory: grouping modes** — add By Name, By Quality brackets grouping to inventory

## Audit Findings (remaining, not yet fixed)
- **MEDIUM**: OptWnd 5600-line god class (begin extracting Settings objects)
- **MEDIUM**: `String ==` comparison used pervasively (fragile but intentional for interned protocol strings)
- **LOW**: Mixed tabs/spaces, 400+ lines of commented-out code in GameUI

## Files Modified
| File | Changes |
|------|---------|
| `src/haven/GameUI.java` | 17 equipment KB definitions, globtype() handlers, inventory toolbar |
| `src/haven/OptWnd.java` | itemAutoDropWindow field+init, BindingPanel equipment section, Per-Item Auto-Drop button |
| `src/haven/Makewindow.java` | craftNEntry, Craft N button, Stop button, craftNThread |
| `src/haven/ItemFilter.java` | Full rewrite: 8 new filter types (FEP, armor, LP, contents, wear) |
| `src/haven/GItem.java` | Per-item auto-drop check (ItemAutoDrop.shouldDrop) before category check |
| `src/haven/ItemAutoDrop.java` | NEW — JSON config manager for per-item drop thresholds |
| `src/haven/ItemAutoDropWindow.java` | NEW — UI window for per-item auto-drop configuration |

## Files Not Modified (verified already implemented)
| Feature | Existing Files |
|---------|---------------|
| Minesweeper | MiningSafetyAssistant.java, OptWnd.java, MenuGrid.java |
| Drink refill | RefillWaterContainers.java, GameUI.java |
| Parasite auto-drop | Equipory.java |
