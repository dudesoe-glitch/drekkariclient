---
name: EnderWiggin Inventory Sort Research
description: Full analysis of EnderWiggin's InventorySorter.java — comparator chain, grid placement, trigger mechanism, and what Hurricane needs to add
type: project
---

# EnderWiggin Inventory Sort — Research Findings

## Files Studied
- `src/auto/InventorySorter.java` — the sort engine
- `src/haven/WItem.java` — `sortName()`, `sortValue()`, `quality()` methods
- `src/haven/GItem.java` — `resname()`, `name` AttrCache, `quality()` method

---

## The Comparator Chain

```java
public static final Comparator<WItem> ITEM_COMPARATOR = Comparator.comparing(WItem::sortName)
    .thenComparing(w -> w.item.resname())
    .thenComparing(WItem::sortValue)
    .thenComparing(WItem::quality, Comparator.reverseOrder());
```

**Sort priority (highest to lowest):**
1. `WItem.sortName()` — display name (human-readable string), ascending
2. `GItem.resname()` — resource path (e.g. `gfx/invobjs/seed-turnip`), ascending. Disambiguates items with identical display names but different resources (rare, mainly gemstones with variants).
3. `WItem.sortValue()` — an int, normally 0. Only non-zero for `Gemstone` sprites, which override it for sub-type ordering within a gem family.
4. `WItem.quality()` — double, **reversed** (highest quality first within each group).

---

## How Each Field Is Obtained

### `WItem.sortName()`
```java
public String sortName() {
    if(lspr instanceof Gemstone) {
        return ((Gemstone)lspr).sortName(); // gem-specific name like "Ruby (Large)"
    }
    return item.name.get(item.resname()); // display name from ItemInfo.Name, fallback to resource path
}
```
- `item.name` is an `AttrCache<String>` on `GItem` that extracts `ItemInfo.Name.original` from the item's info list, with stack/content name handling.
- Fallback is `item.resname()` (the resource path string) so unloaded items sort stably.

### `GItem.resname()`
```java
public String resname() {
    try {
        Resource res = resource();
        if(res != null) { return res.name; }
    } catch (Loading ignore) {}
    return "";
}
```
- Returns the full resource path, e.g. `gfx/invobjs/seeds/turnip`.
- Returns `""` safely if resource is still loading — no crash.

### `WItem.sortValue()`
```java
public int sortValue() {
    if(lspr instanceof Gemstone) {
        return ((Gemstone)lspr).sortValue(); // encodes gem size/tier as int
    }
    return 0; // ALL non-gem items return 0
}
```
- This is a Gemstone-only concern. For general inventory items, this field is always 0 and has no effect.

### `WItem.quality()` / `GItem.quality()`
```java
// WItem delegates:
public double quality() { return item.quality(); }

// GItem:
public double quality() {
    QualityList ql = itemq.get();
    return (ql != null && !ql.isEmpty()) ? ql.single().value : 0;
}
```
- Uses EnderWiggin's `QualityList` system (not present in Hurricane).
- **Hurricane equivalent:** `item.getQBuff() != null ? item.getQBuff().q : 0`

---

## What Hurricane Currently Has vs. What It Needs

### Hurricane already has:
- `Inventory.ITEM_COMPARATOR_ASC/DESC` — quality-only sort (single criterion)
- `Inventory.sqmask` field — already exists, used for masked slots
- `GItem.getQBuff()` returning a `QBuff` with `.q` double — the quality value

### Hurricane is MISSING (needed to port the comparator):
1. **`GItem.resname()` method** — Hurricane has `res.get().name` accessible via `gob.getres()` pattern but no clean `resname()` helper on GItem that handles `Loading` gracefully. Need to add:
   ```java
   public String resname() {
       try {
           Resource r = res.get();
           return r != null ? r.name : "";
       } catch (Loading ignore) {}
       return "";
   }
   ```
2. **`GItem.name` AttrCache** — Hurricane uses `getname()` which returns error strings on failure (`"it's null"`, `"exception"`). EnderWiggin's approach uses a cached, null-safe `AttrCache<String>`. Hurricane's `getname()` is NOT safe to use in a comparator.
3. **`WItem.sortName()` method** — needs to be added to Hurricane's `WItem`. For Hurricane (no Gemstone class), this simplifies to:
   ```java
   public String sortName() {
       try {
           return item.getname(); // but only if getname() is made null-safe
       } catch (Exception e) {
           return item.res.get().name; // fallback to resource path
       }
   }
   ```
   Better: add a null-safe name getter to GItem first, then delegate.
4. **`WItem.quality()` method** — needs to be added to Hurricane's `WItem`:
   ```java
   public double quality() {
       QBuff q = item.getQBuff();
       return q != null ? q.q : 0;
   }
   ```

---

## Grid Placement Algorithm

The `doSort(Inventory inv)` method:

### Phase 1 — Build occupied grid
```java
boolean[][] grid = new boolean[inv.isz.x][inv.isz.y];
// Mark slots blocked by sqmask (pre-blocked slots like belt buckle holes)
// Mark slots occupied by multi-slot items (size > 1x1) — these are EXCLUDED from sorting
```

### Phase 2 — Collect sortable items
Only **1x1 items** (`lsz.x * lsz.y == 1`) are sorted. Multi-slot items (e.g. 2x1 boards, 1x2 buckets) stay in place and just block their grid cells.

### Phase 3 — Sort and assign target positions
Each item gets: `[witem, currentCoord, targetCoord]`. Items are sorted by `ITEM_COMPARATOR`, then assigned target slots left-to-right, top-to-bottom, skipping blocked cells.

### Phase 4 — Move items (cycle detection)
```java
for each item where currentPos != targetPos:
    take() the item (picks it up on cursor)
    follow the displacement chain:
        drop at target slot
        if something was at that target slot, it becomes the next item to place
        repeat until chain ends
```
This is a **chain-following swap** to avoid needing a free slot. It takes one item, drops it at its target (displacing whatever was there), then places that displaced item at its target, etc. It's more efficient than needing a staging area.

---

## Sort Trigger — How It's Invoked

### Single inventory sort
```java
public static void sort(Inventory inv) {
    if(invalidCursor(inv.ui)) { return; } // blocks if cursor is not default (item on cursor)
    start(new InventorySorter(Collections.singletonList(inv)), inv.ui.gui);
}
```
Called from a **sort button** added to each `ExtInventory` panel header.

### Sort all open inventories
```java
public static void sortAll(GameUI gui) { ... }
```
Iterates all open `ExtInventory` windows, excludes those in `EXCLUDE` list (Character Sheet, Study, Cauldron, etc.), collects their `Inventory` objects, runs sort on all of them.

### Async execution via `Defer`
```java
task = Defer.later(this); // runs call() on a background thread
task.callback(() -> callback.accept(task.cancelled() ? "cancelled" : "complete"));
```
- The sort runs off the UI thread.
- Plays an SFX clip (`sfx/hud/on`) on completion.
- Global lock (`synchronized (lock)`) prevents two sorts running simultaneously.
- `cancel()` can abort mid-sort (e.g. if another sort starts, or inventory closes).

### Cursor guard
Sort refuses to start if the cursor is non-default (i.e., player is holding an item). Shows an error message `"Need to have default cursor active to sort inventory!"`.

---

## EXCLUDE List
These inventory window titles are skipped by `sortAll()`:
```
"Character Sheet", "Study",
"Chicken Coop", "Belt", "Pouch", "Purse",
"Cauldron", "Finery Forge", "Fireplace", "Frame",
"Herbalist Table", "Kiln", "Ore Smelter", "Smith's Smelter",
"Oven", "Pane mold", "Rack", "Smoke shed",
"Stack Furnace", "Steelbox", "Tub"
```
These are crafting stations/equipment slots where sorting would be destructive or meaningless.

---

## Porting Checklist for Hurricane

To implement a matching `InventorySorter` in Hurricane:

1. **Add `GItem.resname()`** — null-safe resource name getter (handles `Loading`)
2. **Fix or add a null-safe `GItem.name` getter** — `getname()` is not suitable for sorting; add a safe variant that returns `""` on failure
3. **Add `WItem.sortName()`** — returns safe display name with resource path fallback
4. **Add `WItem.quality()`** — delegates to `item.getQBuff()` → `.q`, returns 0 if null
5. **Build `ITEM_COMPARATOR`** in `Inventory.java` or a new `InventorySorter.java`:
   ```java
   Comparator.comparing(WItem::sortName)
       .thenComparing(w -> w.item.resname())
       // skip sortValue — not needed without Gemstone class
       .thenComparing(WItem::quality, Comparator.reverseOrder())
   ```
6. **Port `doSort()` grid placement logic** — already compatible since Hurricane has `inv.isz`, `inv.sqmask`, `WItem.lsz`, `WItem.c`, `Inventory.sqsz`
7. **Port `Defer.later()` async execution** — check if Hurricane has `Defer` class; if not, use `new Thread(runnable).start()` pattern consistent with other bots
8. **Add sort button** to inventory window header (or keybind via `OptWnd`)
9. **Define EXCLUDE list** appropriate for Hurricane's window titles

---

## Key Difference: `Defer` vs Hurricane Threading
EnderWiggin uses `Defer.later(this)` which is a task-queue executor. Hurricane bots use `new Thread(bot, "name").start()`. The sort logic itself is thread-safe as written — it just needs to run off the UI thread. Hurricane can use a plain `new Thread(sorter).start()` pattern without needing to port `Defer`.
