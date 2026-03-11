# HANDOFF — Session 6 (List View Polish, Repeat Mode, Category Badges, New Bots)

## Resumption Prompt
Implemented 6 sub-features across 4 areas: list view icons + click-to-find highlighting, flower menu repeat toolbar button, item type category badges, ForagingBot, and MiningBot. Build clean with zero errors.

## Goal
Work through all priorities from session 5 HANDOFF except testing.

## Completed

### Inventory List View Polish
1. **Item icons in rows** (`InventoryListWindow.java`)
   - Each row now shows a 14x14 item icon to the left of the text
   - Icon loaded from item's `Resource.imgc` layer, same as group headers
   - Row height increased from 16 to 18px to accommodate icons
   - Resource cached per item group for efficiency

2. **Click-to-find grid highlighting** (`InventoryListWindow.java`, `Inventory.java`, `WItem.java`)
   - Clicking a row in list view highlights matching items in inventory grid
   - `Inventory.highlightItemName` synced from list view's `highlightedName`
   - WItem draws pulsing blue 2px border (alpha oscillates via `Math.sin(Utils.rtime() * 5)`)

### Flower Menu Repeat Mode
3. **Repeat button in inventory toolbar** (`GameUI.java`, `FlowerMenu.java`, `WItem.java`)
   - "Repeat" button added after "List" button in inventory toolbar
   - Tracks last chosen flower menu option and source item resource
   - `FlowerMenu.lastChosenOption` / `lastChosenResource` static fields
   - `GameUI.lastFlowerMenuItemRes` set on right-click before `iact` message
   - Click "Repeat" → launches `AutoRepeatFlowerMenuScript` with stored values
   - Shows error if no previous action to repeat

### Type Category Badges
4. **Colored dot badges on inventory items** (`WItem.java`, `OptWnd.java`)
   - `ItemCategory` enum: FOOD (green), ARMOR (blue), CURIOSITY (purple), TOOL (yellow)
   - Detection via `AttrCache<ItemCategory>` checking `FoodInfo`, `Armor`, `Curiosity` info types
   - Tool detection by resource name patterns (`/tools/`, `pickaxe`, `shovel`, etc.)
   - Small filled circle drawn at bottom-left of each item
   - Setting: "Show Item Category Badges" checkbox in Display/Quality panel (default: off)

### New Bots
5. **ForagingBot** (`src/haven/automated/ForagingBot.java`, ~200 lines)
   - Auto-finds and picks nearby herbs (`gfx/terobjs/herbs/*`)
   - "Also pick ground items" checkbox for truffles, gemstones, etc.
   - Pathfinds to target, `FlowerMenu.setNextSelection("Pick")`, right-clicks
   - Safety: HP/energy/stamina checks, inventory full detection, auto-drink
   - Registered in GameUI + MenuGrid

6. **MiningBot** (`src/haven/automated/MiningBot.java`, ~200 lines)
   - "Mine Here" button stores player's current position as mining target
   - Repeatedly left-clicks target coordinate to mine
   - Waits for progress bar completion, drops cursor items
   - Safety: HP/energy/stamina checks, inventory full detection, auto-drink
   - Target coordinate shown in UI
   - Registered in GameUI + MenuGrid

## In Progress
Nothing — all planned work complete.

## Next Priorities
1. **Test all features in-game** — especially:
   - List view icons render correctly
   - Click-to-find highlighting matches correct items
   - Repeat button works with flower menu actions
   - Category badges show correct colors for food/armor/curios/tools
   - ForagingBot flower menu "Pick" action works
   - MiningBot "Mine Here" targeting + repeated mining
2. **Polish** — adjust badge size/position, refine tool detection heuristics
3. **More bots** — consider additional automation (fishing improvements, etc.)
4. **OptWnd further extraction** — still 5000+ lines, more panels could be extracted

## Files Modified
| File | Changes |
|------|---------|
| `src/haven/InventoryListWindow.java` | Item icons in rows, increased row height, resource caching |
| `src/haven/Inventory.java` | Added `highlightItemName` field |
| `src/haven/WItem.java` | Click-to-find blue border, `ItemCategory` enum + AttrCache, badge drawing, `lastFlowerMenuItemRes` tracking |
| `src/haven/FlowerMenu.java` | `lastChosenOption` + `lastChosenResource` static fields, tracking in `choose()` |
| `src/haven/GameUI.java` | Repeat button in toolbar, `lastFlowerMenuItemRes` field, ForagingBot/MiningBot fields |
| `src/haven/OptWnd.java` | `showItemCategoryBadgesCheckBox` declaration + initialization |
| `src/haven/MenuGrid.java` | ForagingBot + MiningBot registration + handlers |
| `src/haven/automated/ForagingBot.java` | NEW — auto-herb picking bot |
| `src/haven/automated/MiningBot.java` | NEW — auto-mining bot |
| `res/customclient/menugrid/Bots/ForagingBot.res` | NEW — menu icon |
| `res/customclient/menugrid/Bots/MiningBot.res` | NEW — menu icon |
