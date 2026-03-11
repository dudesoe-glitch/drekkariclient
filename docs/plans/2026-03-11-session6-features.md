# Session 6 Features Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement 4 features: polish list view (icons + click-to-find), flower menu repeat mode toolbar button, type category badges on inventory items, and two new bots (Foraging + Mining).

**Architecture:** Each feature is independent and can be built in parallel. List view and category badges modify existing UI rendering. Repeat mode adds a toolbar button wired to existing AutoRepeatFlowerMenuScript. Bots follow established Window+Runnable pattern with GameUI/MenuGrid registration.

**Tech Stack:** Java 15+, Haven custom UI framework (Widget tree), OpenGL rendering via GOut, SQLite for persistent data.

---

### Task 1: Polish Inventory List View — Item Icons in Rows

**Files:**
- Modify: `src/haven/InventoryListWindow.java`

**What to change:**
In `refreshList()`, each row widget currently renders only text. Add a small item icon (11x11 px) to the left of each row, loaded from the item's resource `imgc` layer — same technique used in `Inventory.drawGroupHeaders()`.

**Step 1: Store resource reference in itemStats aggregation**

In `refreshList()`, add a `Map<String, Resource>` to store the representative resource for each item group. When building `itemStats`, also store the `Resource` from `wi.item.resource()`:

```java
Map<String, Resource> itemResources = new LinkedHashMap<>();
// Inside the loop, after itemResNames.put:
try { itemResources.put(name, wi.item.resource()); } catch (Exception ignored) {}
```

**Step 2: Draw item icon in each row's draw() method**

In the row Widget's `draw(GOut g)` method, before drawing the text, draw the item icon:

```java
int iconSz = UI.scale(14);
int iconPad = iconSz + UI.scale(3);
// Load and draw icon
Resource itemRes = itemResources.get(itemName);
if (itemRes != null) {
    try {
        Resource.Image img = itemRes.layer(Resource.imgc);
        if (img != null) {
            g.image(img.tex(), new Coord(UI.scale(2), 1), new Coord(iconSz, iconSz));
        }
    } catch (Exception ignored) {}
}
// Shift text right by iconPad
g.image(tex, new Coord(iconPad, 1));
```

**Step 3: Update row width to account for icon**

Adjust the row widget width and text x-offset to accommodate the icon.

---

### Task 2: Polish Inventory List View — Click-to-Find in Grid

**Files:**
- Modify: `src/haven/InventoryListWindow.java`
- Modify: `src/haven/Inventory.java` (add `highlightItemByName` field)
- Modify: `src/haven/WItem.java` (draw highlight overlay)

**What to change:**
When clicking a row in the list view, highlight matching items in the actual inventory grid with a pulsing blue border.

**Step 1: Add highlight field to Inventory**

In `Inventory.java`, add:
```java
public String highlightItemName = null;
```

**Step 2: Set highlight on list row click**

In `InventoryListWindow.java`, in the row mousedown handler, after toggling `highlightedName`, also set:
```java
if (gui.maininv != null) {
    gui.maininv.highlightItemName = highlightedName;
}
```

**Step 3: Draw highlight overlay in WItem**

In `WItem.java` `draw()`, after drawing overlays, check if this item's sort name matches the inventory's `highlightItemName`. If so, draw a pulsing blue border:

```java
if (parent instanceof Inventory) {
    Inventory inv = (Inventory) parent;
    if (inv.highlightItemName != null) {
        try {
            if (sortName().equals(inv.highlightItemName)) {
                int alpha = (int)(80 + 60 * Math.sin(Utils.rtime() * 4));
                g.chcolor(80, 120, 255, alpha);
                g.rect(Coord.z, sz, 2);
                g.chcolor();
            }
        } catch (Exception ignored) {}
    }
}
```

---

### Task 3: Flower Menu Repeat Mode — Toolbar Button

**Files:**
- Modify: `src/haven/GameUI.java` (add Repeat button to inventory toolbar)
- Modify: `src/haven/FlowerMenu.java` (track last selected option + source resource)

**What to change:**
Add a "Repeat" toggle button to the inventory toolbar. When enabled, after any flower menu selection, the chosen action is automatically repeated on all similar items in inventory (using existing `AutoRepeatFlowerMenuScript`).

**Step 1: Track last flower menu choice in FlowerMenu**

In `FlowerMenu.java`, add static fields:
```java
public static String lastChosenOption = null;
public static String lastChosenResource = null;
```

In `choose(Petal option)`, when option is not null, store the choice:
```java
if (option != null) {
    lastChosenOption = option.name;
    // Try to get the resource name of the item that triggered this menu
    // This is available via the widget that triggered iact
}
```

**Step 2: Add "Repeat" button to inventory toolbar**

In `GameUI.java` inventory toolbar section (after the List button), add:
```java
Button repeatBtn = add(new Button(UI.scale(55), "Repeat") {
    public void click() {
        if (FlowerMenu.lastChosenOption != null && FlowerMenu.lastChosenResource != null) {
            if (autoRepeatFlowerMenuScriptThread != null) {
                autoRepeatFlowerMenuScriptThread.interrupt();
                autoRepeatFlowerMenuScriptThread = null;
            }
            autoRepeatFlowerMenuScriptThread = new Thread(
                new AutoRepeatFlowerMenuScript(GameUI.this, FlowerMenu.lastChosenResource),
                "autoRepeatFlowerMenu"
            );
            AutoRepeatFlowerMenuScript.option = FlowerMenu.lastChosenOption;
            autoRepeatFlowerMenuScriptThread.start();
        } else {
            error("No flower menu action to repeat. Right-click an item first.");
        }
    }
}, listBtn.pos("ur").adds(4, 0));
repeatBtn.settip("Repeat last flower menu action on all similar items in inventory");
```

**Step 3: Capture source resource in FlowerMenu**

The tricky part is knowing which item resource triggered the flower menu. In `choose()`:
```java
if (option != null) {
    lastChosenOption = option.name;
    // The flower menu's parent context comes from the widget that sent "iact"
    // Store the resource from the item that was right-clicked
    if (ui != null && ui.gui != null) {
        // Track via the last interacted item in GameUI
        lastChosenResource = ui.gui.lastFlowerMenuItemRes;
    }
}
```

In `GameUI.java`, add:
```java
public String lastFlowerMenuItemRes = null;
```

In `WItem.java` mousedown for button 3 (right-click), before `item.wdgmsg("iact", ...)`:
```java
try {
    ui.gui.lastFlowerMenuItemRes = item.getres().name;
} catch (Loading ignored) {}
```

**Step 4: Update itemCountLabel position**

Move `itemCountLabel` to be after the repeat button instead of listBtn:
```java
itemCountLabel = add(new Label(""), repeatBtn.pos("ur").adds(8, 3));
```

---

### Task 4: Type Category Badges on Inventory Grid Items

**Files:**
- Modify: `src/haven/WItem.java` (add category badge drawing in `draw()`)

**What to change:**
Draw a small colored dot/badge in the bottom-left corner of each inventory item indicating its type category. Categories are detected by checking the item's `ItemInfo` list for known types.

**Step 1: Add category detection method to WItem**

```java
public enum ItemCategory {
    FOOD(new Color(50, 200, 50, 200)),      // Green
    ARMOR(new Color(80, 130, 255, 200)),     // Blue
    CURIOSITY(new Color(200, 100, 255, 200)),// Purple
    TOOL(new Color(255, 200, 50, 200)),      // Yellow/Gold
    NONE(null);

    public final Color color;
    ItemCategory(Color c) { this.color = c; }
}

private ItemCategory cachedCategory = null;
private boolean categoryChecked = false;

public ItemCategory getCategory() {
    if (categoryChecked) return cachedCategory;
    categoryChecked = true;
    try {
        List<ItemInfo> infos = item.info();
        for (ItemInfo inf : infos) {
            if (inf instanceof haven.resutil.FoodInfo) {
                cachedCategory = ItemCategory.FOOD;
                return cachedCategory;
            }
            if (inf instanceof haven.res.ui.tt.armor.Armor) {
                cachedCategory = ItemCategory.ARMOR;
                return cachedCategory;
            }
            if (inf instanceof haven.resutil.Curiosity) {
                cachedCategory = ItemCategory.CURIOSITY;
                return cachedCategory;
            }
        }
        // Check if it's a tool by resource path
        String resname = item.resname();
        if (resname.contains("/tools/") || resname.contains("pickaxe") || resname.contains("shovel")
            || resname.contains("axe") || resname.contains("saw") || resname.contains("scythe")) {
            cachedCategory = ItemCategory.TOOL;
            return cachedCategory;
        }
    } catch (Exception ignored) {}
    cachedCategory = ItemCategory.NONE;
    return cachedCategory;
}
```

**Step 2: Draw category badge in draw() method**

In `WItem.draw()`, after `drawDurabilityBars()`, add (only when OptWnd setting enabled):

```java
if (OptWnd.showItemCategoryBadgesCheckBox != null && OptWnd.showItemCategoryBadgesCheckBox.a) {
    ItemCategory cat = getCategory();
    if (cat != null && cat.color != null) {
        g.chcolor(cat.color);
        g.fcircle(UI.scale(5), sz.y - UI.scale(5), UI.scale(4), 10);
        g.chcolor();
    }
}
```

**Step 3: Add setting checkbox in OptWnd**

In `OptWnd.java`, in the Display tab section, add:
```java
public static CheckBox showItemCategoryBadgesCheckBox;
// Initialize:
showItemCategoryBadgesCheckBox = new CheckBox("Show Item Category Badges") {
    { a = Utils.getprefb("showItemCategoryBadges", false); }
    public void changed(boolean val) { Utils.setprefb("showItemCategoryBadges", val); }
};
```

**Step 4: Reset cache when item info changes**

In `WItem.tick()` or when info is reloaded, reset the cache:
```java
// In the info AttrCache invalidation flow, or simply make categoryChecked reset periodically
// Simplest: use AttrCache pattern like other WItem caches
```

Actually, better approach — use `AttrCache` like existing pattern:
```java
public final AttrCache<ItemCategory> itemCategory = new AttrCache<>(this::info, info -> {
    ItemCategory cat = ItemCategory.NONE;
    for (ItemInfo inf : info) {
        if (inf instanceof haven.resutil.FoodInfo) return () -> ItemCategory.FOOD;
        if (inf instanceof haven.res.ui.tt.armor.Armor) return () -> ItemCategory.ARMOR;
        if (inf instanceof haven.resutil.Curiosity) return () -> ItemCategory.CURIOSITY;
    }
    // Tool detection by resource name
    try {
        String resname = item.resname();
        if (resname.contains("/tools/") || resname.contains("pickaxe") || resname.contains("shovel")
            || resname.contains("axe") || resname.contains("saw") || resname.contains("scythe")) {
            return () -> ItemCategory.TOOL;
        }
    } catch (Exception ignored) {}
    return () -> cat;
});
```

Then in draw: `ItemCategory cat = itemCategory.get();`

---

### Task 5: Foraging Bot

**Files:**
- Create: `src/haven/automated/ForagingBot.java`
- Modify: `src/haven/GameUI.java` (add fields)
- Modify: `src/haven/MenuGrid.java` (add registration + handler)
- Create: `res/customclient/menugrid/Bots/ForagingBot.res` (copy from existing .res)

**What to change:**
Bot that automatically finds and picks nearby forageable herbs/items (`gfx/terobjs/herbs/*`), pathfinds to them, right-clicks with "Pick" flower menu selection, then moves to the next.

**Bot Logic:**
```
loop:
  1. Check HP < 2% → hearth
  2. Check energy < 2% → stop
  3. Check stamina < 40% → auto-drink
  4. Check inventory full → stop with message
  5. Find nearest herb (gfx/terobjs/herbs/*) within range
  6. If none found → expand search / wait / stop
  7. Pathfind to herb
  8. FlowerMenu.setNextSelection("Pick")
  9. Right-click the herb gob
  10. Wait for flower menu to process
  11. Repeat
```

**Key patterns to follow (from ClayDiggingBot):**
- `extends Window implements Runnable`
- Start/Stop button toggle
- Status label
- `stop` boolean flag
- `findNearestHerb()` — iterate `gui.map.glob.oc` synchronized, filter by `res.name.startsWith("gfx/terobjs/herbs")`
- `gui.map.pfLeftClick()` for pathfinding
- `AUtils.waitPf()` for path completion
- `AUtils.drinkTillFull()` for auto-drink
- `FlowerMenu.setNextSelection("Pick")` before right-click
- Right-click pattern: `gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int)gob.id, gob.rc.floor(posres), 0, -1)`

**Config options:**
- Checkbox: also pick items from `otherPickableObjects` set (truffles, etc.)
- Range slider or label showing search radius

**Registration (GameUI.java):**
```java
public ForagingBot foragingBot;
public Thread foragingBotThread;
```

**Registration (MenuGrid.java):**
```java
makeLocal("customclient/menugrid/Bots/ForagingBot");
// Handler block following ButcherBot/ClayDiggingBot pattern
```

---

### Task 6: Mining Bot

**Files:**
- Create: `src/haven/automated/MiningBot.java`
- Modify: `src/haven/GameUI.java` (add fields)
- Modify: `src/haven/MenuGrid.java` (add registration + handler)
- Create: `res/customclient/menugrid/Bots/MiningBot.res` (copy from existing .res)

**What to change:**
Bot that automates mining by repeatedly clicking a designated mine wall tile. Unlike other bots that find targets automatically, mining requires the user to click the target tile first, then the bot repeats the mining action.

**Bot Logic:**
```
setup:
  1. User clicks "Set Target" button, then clicks a mine wall tile
  2. Bot stores the target coordinate

loop:
  1. Check HP < 2% → hearth
  2. Check energy < 2% → stop
  3. Check stamina < 40% → auto-drink
  4. Check inventory full → pause with message
  5. If currently mining (progress bar active) → wait
  6. Click the stored mine wall target to start mining
  7. Wait for mining progress to complete
  8. Check for ore/stone drops on cursor → drop to inventory
  9. Repeat
```

**Key implementation details:**
- The mining action is a simple left-click on a map tile (not a gob interaction)
- Target is a `Coord` on the map
- Mining progress detected via `gui.prog != null`
- Dropped items on cursor: `gui.vhand != null` → `gui.vhand.item.wdgmsg("drop", Coord.z)`
- CellarDiggingBot already has mining pose detection: `gui.map.player().getPoses().contains("pickan")`
- Auto-drink: `AUtils.drinkTillFull(gui, 0.99, 0.99)`

**Config options:**
- "Set Target" button — user clicks map tile, stores coord
- Status label showing current action
- Auto-drop low quality stones option (integrate with ItemAutoDrop)

**Registration follows same pattern as Task 5.**

**Bot class structure:**
```java
public class MiningBot extends Window implements Runnable {
    private final GameUI gui;
    public boolean stop;
    private boolean active;
    private Coord2d targetPos;
    private boolean settingTarget;
    private Button activeButton;
    private Label statusLabel;
    // ...
}
```

**Target setting mechanism:**
Override `MapView` or use a callback. Simplest approach: add a flag `gui.miningBotSettingTarget` that, when true, captures the next map click coordinate and passes it to the bot.

In `MapView.java`, in the click handler, check:
```java
if (gui.miningBot != null && gui.miningBot.settingTarget) {
    gui.miningBot.setTarget(mc); // mc = map coordinate clicked
    gui.miningBot.settingTarget = false;
    return;
}
```

---

## Execution Notes

- **Tasks 1-2** can be combined (both modify InventoryListWindow)
- **Tasks 3-4** are independent of each other and of 1-2
- **Tasks 5-6** are independent bots, can be built in parallel
- All tasks need a final build verification: `ant bin`
- Bot `.res` files can be binary-copied from existing ones (e.g., `ClayDiggingBot.res`)

## Build Command
```bash
JAVA_HOME="/c/Program Files/Java/jdk-17" /c/tools/apache-ant-1.10.15/bin/ant bin
```
