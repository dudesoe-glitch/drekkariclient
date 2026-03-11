---
name: EnderWiggin InventorySorter — comparator, grid placement, trigger
description: Full details of EnderWiggin's multi-criteria inventory sort: comparator chain (sortName → resname → sortValue → quality desc), 1x1-only grid placement, Defer async execution, sort button trigger, EXCLUDE list, and porting checklist for Hurricane
type: project
---

# EnderWiggin Inventory Sort Research

## Comparator Chain (exact)

```java
Comparator.comparing(WItem::sortName)
    .thenComparing(w -> w.item.resname())
    .thenComparing(WItem::sortValue)
    .thenComparing(WItem::quality, Comparator.reverseOrder());
```

1. `sortName()` — display name string, ascending. Falls back to resource path if not loaded.
2. `resname()` — full resource path (e.g. `gfx/invobjs/seed-turnip`), ascending. Secondary disambiguation.
3. `sortValue()` — int, always 0 except for Gemstone sprites (irrelevant for Hurricane port).
4. `quality()` — double, **reversed** (highest quality first within each name group).

## How Fields Are Obtained

- `WItem.sortName()`: calls `item.name.get(item.resname())` — uses an `AttrCache<String>` on GItem that extracts `ItemInfo.Name.original`, falls back to resname on failure.
- `GItem.resname()`: `res.get().name`, wrapped in try/catch Loading, returns `""` safely.
- `WItem.quality()`: delegates to `GItem.quality()` → `itemq.get().single().value`, returns 0 if null.

## What Hurricane Is Missing

1. `GItem.resname()` — no null-safe version exists. Need to add: `try { return res.get().name; } catch (Loading e) { return ""; }`
2. `WItem.sortName()` — doesn't exist. Need to add: returns `item.getname()` but `getname()` returns error strings on failure; must wrap safely.
3. `WItem.quality()` — doesn't exist. Need to add: `QBuff q = item.getQBuff(); return q != null ? q.q : 0;`
4. Hurricane already has `ITEM_COMPARATOR_ASC/DESC` (quality-only). The multi-criteria comparator is NEW.

## Grid Placement Algorithm

- Only sorts **1x1 items** (`lsz.x * lsz.y == 1`). Multi-slot items stay put, block their grid cells.
- Builds `boolean[][] grid` from `inv.sqmask` (pre-blocked slots) + multi-slot item positions.
- Assigns target positions left-to-right, top-to-bottom through unblocked cells.
- Moves items via **chain-following swaps**: take item, drop at target, that displaces another item which becomes the next in chain. No free staging slot needed.

## Trigger Mechanism

- **Sort button** on each `ExtInventory` panel header — calls `InventorySorter.sort(inv)`.
- **Sort all** keybind — `InventorySorter.sortAll(gui)` iterates all open `ExtInventory` windows.
- **Cursor guard** — refuses to sort if cursor is non-default (player holding item on cursor).
- **Async** — runs via `Defer.later(this)`. For Hurricane port: use `new Thread(sorter).start()` instead.
- **SFX** on completion: plays `sfx/hud/on` clip.
- **Global lock** — only one sort at a time; new sort cancels previous.

## EXCLUDE List (windows skipped by sortAll)

```
"Character Sheet", "Study",
"Chicken Coop", "Belt", "Pouch", "Purse",
"Cauldron", "Finery Forge", "Fireplace", "Frame",
"Herbalist Table", "Kiln", "Ore Smelter", "Smith's Smelter",
"Oven", "Pane mold", "Rack", "Smoke shed",
"Stack Furnace", "Steelbox", "Tub"
```

## Hurricane Porting Checklist

1. Add `GItem.resname()` — Loading-safe resource name getter
2. Add null-safe name getter on GItem (fix `getname()` or add new method)
3. Add `WItem.sortName()` — display name with resname fallback
4. Add `WItem.quality()` — delegates to `getQBuff().q`, returns 0 if null
5. Build `ITEM_COMPARATOR` (3-criterion, skip sortValue unless Gemstone support added)
6. Port `doSort()` grid placement — already compatible (Hurricane has `inv.isz`, `sqmask`, `WItem.lsz`, `WItem.c`, `Inventory.sqsz`)
7. Add sort button to inventory header or keybind via OptWnd
8. Define EXCLUDE list for Hurricane's window titles

## Full Research File

Full details at: `C:\Users\why_t\Hurricane\.claude\projects\C--Users-why-t-Hurricane\memory\enderwiggin-sort-research.md`
