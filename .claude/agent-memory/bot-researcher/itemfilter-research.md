---
name: itemfilter-research
description: EnderWiggin ItemFilter deep dive — query syntax, inner filter classes, integration via GItem.setFilter/FilterWnd, comparison with Hurricane's InventorySearchWindow
type: project
---

# EnderWiggin ItemFilter Research

## Source files examined
- `src/haven/ItemFilter.java` (666 lines)
- `src/haven/FilterWnd.java` (79 lines)
- `src/haven/GItem.java` — filter application and `matches()` method
- `src/haven/WItem.java` — highlight rendering via `item.matches()`
- Hurricane `src/haven/InventorySearchWindow.java` — what currently exists
- Hurricane `src/haven/WItem.java` — fuzzy-match highlight with pulsing color

---

## 1. Query Syntax Supported

The master regex pattern that drives parsing:
```
(?:(\w+))?(?:^|:)([\w\p{L}*]+)?(?:([<>=+~])(\d+(?:\.\d+)?)?([<>=+~])?)?
```

Group breakdown: `tag`, `text`, `sign`, `value`, `opt`

| Tag | Inner classes | Examples |
|-----|--------------|---------|
| (none) | `Text` — name substring match | `turnip` |
| `txt:` | `Text` — full tooltip text search | `txt:Well mined` |
| `q` / `q:` | `Q` — quality value | `q>10`, `q+21`, `q<12`, `q:5` |
| `has:` | `Has` — container contents | `has:water`, `has:water>2`, `has:water+3` |
| `lp`, `lph`, `xp`, `mw` | `XP` — curiosity stats | `lp>100`, `lp+200`, `lph>50`, `mw<5` |
| `fep:` | `FEP` — food event points by name | `fep:str>1`, `fep:agi+2`, `fep:cha<3` |
| `energy`/`nrg`, `hunger`/`hng` | `Food` — food fill values | `nrg>50`, `nrg<120`, `hng+200` |
| `armor:` | `Armor` — hard/soft armor values | `armor:hard>1`, `armor:soft<2`, `armor:>4` |
| `symb:`/`gast:` | `Gastronomy` — symbel bonuses | `symb:fep>2`, `symb:hunger<3` |
| `attr:` | `Attribute` — equipment/gilding attr bonuses | `attr:str>2`, `attr:survival` |
| `use:`/`uses:` | `Inputs` — crafting input requirements | `use:snow`, `use:iron>2` |
| `eff:`/`effect:` | `Effects` — alchemy ingredient effects | `eff:lore`, `eff:jelly` |

**Sign operators:**
- `>` = GREATER (strictly more than)
- `<` = LESS (less than or equal to — note: not strictly less)
- `=` = EQUAL
- `+` = GREQUAL (at least / greater-or-equal)
- `~` = WAVE (used for quality type selection, not numeric compare)
- (none) = DEFAULT (nonzero / "has any")

Multiple terms in a single query form a `Compound` filter (AND logic) — all sub-filters must match.

---

## 2. Architecture — Class Hierarchy

```
ItemFilter (abstract base)
├── matches(List<ItemInfo>) — top-level entry, iterates info list calling match()
├── matches(MenuGrid.Pagina) — for menu/crafting grid use
└── match(ItemInfo) — single-info check (default returns false)

ItemFilter.Compound        — AND-chains multiple filters
ItemFilter.Text            — name substring or full-text (adhoc, coinage) match
ItemFilter.Complex (base)  — sign+value numeric tests; subtypes:
    ├── Has               — container contents (name + count)
    ├── Q                 — quality (delegates to QualityList, supports SingleType)
    ├── XP                — curiosity (lp/lph/xp/mw)
    ├── FEP               — FoodInfo event points by stat name (3-char prefix match)
    ├── Food              — FoodInfo energy/hunger values
    ├── Armor             — hard/soft armor via ItemInfo.getArmor()
    ├── Gastronomy        — Gast (symbel) fep+hunger bonuses
    ├── Attribute         — equipment attr mods via ItemInfo.getBonuses()
    ├── Inputs            — crafting inputs via ItemInfo.getInputs()
    └── Effects           — alchemy ingredient effects
```

---

## 3. Filter Application Pipeline (EnderWiggin)

**Step 1 — Set filter globally:**
`GItem.setFilter(ItemFilter filter)` — stores in `static ItemFilter filter` field, bumps `lastFilter` timestamp.

**Step 2 — Per-tick evaluation in GItem.tick():**
```java
public void tick(double dt) {
    super.tick(dt);
    ...
    testMatch();
}

public void testMatch() {
    if(filtered < lastFilter && spr != null) {
        matches = filter != null && filter.matches(info());
        filtered = lastFilter;
        // notify listeners
    }
}
```
This is lazy: only re-evaluates when the filter timestamp advances (filter changed), not every tick.

**Step 3 — Highlight in WItem.draw():**
```java
if(item.matches()) {
    g.chcolor(MATCH_COLOR);
    g.rect(Coord.z, sz);
    g.chcolor();
}
```
Non-matching items get no overlay (they appear dim by contrast). `MATCH_COLOR` is a semi-transparent color overlay drawn as a filled rect behind the item sprite.

**Step 4 — Clear on hide:**
`FilterWnd.hide()` calls `GItem.setFilter(null)` — all items revert to normal.

---

## 4. FilterWnd — The UI Window

`FilterWnd extends GameUI.Hidewnd` (not a standalone Window — it uses a show/hide toggle panel pattern):

- Single `TextEntry` field, auto-focuses on show
- Fires `ItemFilter.create(text)` → `GItem.setFilter()` whenever text changes (length >= 2)
- ESC key clears the text entry (and clears filter) without closing the window
- Has a help button → `ItemFilter.showHelp(ui, FILTER_HELP)` showing all query syntax
- `GameUI.toggleFilter()` creates/shows the window on first call, then just toggles show/hide

In `GameUI`:
```java
public FilterWnd filter;
// keybind triggers:
public void toggleFilter() {
    if(filter == null) {
        filter = add(new FilterWnd(), ClientUtils.getScreenCenter(ui));
    }
    filter.toggle();
}
```

---

## 5. Dependencies EnderWiggin Has That Hurricane May Lack

| Dependency | Status in Hurricane | Notes |
|-----------|-------------------|-------|
| `QualityList` + `SingleType` | **MISSING** | EnderWiggin's multi-quality system. Hurricane uses `QBuff` (single quality from `GItem.getQBuff()`). The `Q` filter inner class needs this. |
| `ItemInfo.getContent()` | **PARTIAL** — Hurricane has `ItemInfo.Contents.content()` instance method and `Content.EMPTY` but no static `ItemInfo.getContent(List)` wrapper | Easy to add |
| `ItemInfo.getArmor()` | **YES** — Hurricane has this at line 619 in ItemInfo.java | Compatible |
| `ItemInfo.getBonuses()` | **DIFFERENT signature** — Hurricane returns `Map<Entry, String>` (attrmod.Entry), EnderWiggin returns `Map<Resource, Integer>`. Attribute filter will need rewriting. | Medium rewrite |
| `ItemInfo.getInputs()` | **MISSING** in Hurricane — not found | Full port needed |
| `Curiosity` (resutil) | **YES** — Hurricane has it at `src/haven/resutil/Curiosity.java` with same fields: `exp`, `mw`, `enc`, `time`, `lph` | Direct use |
| `FoodInfo` (resutil) | **YES** — Hurricane has it with same fields: `end`, `glut`, `evs[]`, each `ev.nm` | Direct use |
| `Gast` (symb) | **UNKNOWN** — not checked | Check `haven.res.ui.tt.gast` |
| `ItemInfo.AttrCache` | **YES** — Hurricane has this at ItemInfo.java line 657 | Compatible |
| `me.ender.ClientUtils` | **MISSING** — EnderWiggin-specific package | Replace `ClientUtils.round()` with `Math.round()` |
| `me.ender.Reflect` | **Hurricane has its own `Reflect.java`** — already used widely | Check method parity |
| `GItem.matches()` / `GItem.setFilter()` | **MISSING** — Hurricane GItem has no static filter or matches() method | Must add |
| `GItem.testMatch()` | **MISSING** | Must add |
| `GameUI.Hidewnd` | **UNKNOWN** — not checked in Hurricane | May not exist; FilterWnd would extend Window instead |

---

## 6. Hurricane's Current InventorySearchWindow

`src/haven/InventorySearchWindow.java` — 38 lines total:
- Stores a **static** `String inventorySearchString`
- Has a `TextEntry` that updates the string on change
- Close sets string to `""` and nulls `gui.inventorySearchWindow`
- Toggle keybind in `GameUI` at line ~1957 (`kb_searchInventoriesButton`)

**How WItem uses it** (in `draw()`, lines 234-261 of Hurricane WItem.java):
```java
String searchKeyword = InventorySearchWindow.inventorySearchString.toLowerCase();
if (searchKeyword.length() > 1) {
    if (Fuzzy.fuzzyContains(itemName, searchKeyword)) {
        // Animated pulsing color overlay (brightness cycles 0-255 at ~fps-based speed)
        g.usestate(new ColorMask(new Color(value, value, value, value)));
    }
}
```
This does **fuzzy** name matching with an animated pulse effect. Only checks the item name, not any ItemInfo.

---

## 7. Porting Strategy for Hurricane

### Option A — Replace InventorySearchWindow with ItemFilter (recommended)
1. Keep the existing `InventorySearchWindow` toggle keybind and window
2. Replace the `TextEntry` change handler to call `ItemFilter.create(text)` → `GItem.setFilter(filter)`
3. Add `static ItemFilter filter` + `setFilter()` + `matches()` + `testMatch()` to `GItem`
4. In `WItem.draw()`, replace the fuzzy-match animation with `item.matches()` color overlay
5. Port subset of inner filter classes that are feasible

### Option B — Create FilterWnd alongside InventorySearchWindow (parallel)
- Keeps the existing simple fuzzy search intact
- Adds a separate advanced filter window
- More code duplication, less clean

### Recommended inner class port order (easiest to hardest)
1. `Text` — pure name string match, zero dependencies
2. `XP` — curiosity: `Curiosity` class already exists in Hurricane with same fields
3. `FEP` / `Food` — food: `FoodInfo` already exists with same fields
4. `Has` — containers: needs `ItemInfo.getContent()` static wrapper (trivial to add)
5. `Armor` — armor: `ItemInfo.getArmor()` already exists and is compatible
6. `Q` — quality: **requires QualityList or adaptation to use QBuff** (Hurricane uses `QBuff`, single quality only — adapt to test `item.getQBuff().q` directly)
7. `Attribute` — attr bonuses: `getBonuses()` signature differs (Entry vs Resource); requires rewrite around `Entry.attr.name()`
8. `Inputs` — crafting inputs: Hurricane is missing `ItemInfo.getInputs()` entirely; would need full port
9. `Gastronomy` (symb) / `Effects` (alchemy) — game-specific; verify `Gast` and `Effect` classes exist

### Q filter adaptation for Hurricane (QBuff instead of QualityList)
Instead of `QualityList.make(infos)`, use:
```java
QBuff qBuff = ItemInfo.find(QBuff.class, infos);
if (qBuff != null) { return test(qBuff.q); }
```

### Attribute filter adaptation for Hurricane
Hurricane's `getBonuses()` returns `Map<Entry, String>` where `Entry.attr.name()` gives the attr name.
Replace EnderWiggin's `Resource`-keyed lookup with:
```java
Map<Entry, String> bonuses = ItemInfo.getBonuses(infos);
for (Entry entry : bonuses.keySet()) {
    if (entry.attr.name().toLowerCase().startsWith(text)) {
        // parse value from bonuses.get(entry) string
        return test(parsed_value);
    }
}
```

---

## 8. Integration Points in Hurricane

- `GItem.java` — add `static ItemFilter filter`, `static long lastFilter`, `setFilter()`, `matches()`, `testMatch()` called from `tick()`
- `WItem.java` — in `draw()`, replace/extend the fuzzy animation block with `item.matches()` highlight check
- `InventorySearchWindow.java` — upgrade to call `ItemFilter.create()` instead of storing raw string, or replace with a `FilterWnd`-style implementation
- `GameUI.java` — no new field needed if reusing `inventorySearchWindow`, just upgrade the handler

---

## 9. Key Differences Summary

| Aspect | EnderWiggin | Hurricane (current) |
|--------|-------------|-------------------|
| Filter architecture | `GItem.setFilter()` static, per-tick `testMatch()`, `matches()` | Static string `InventorySearchWindow.inventorySearchString` |
| Match highlight | Semi-transparent color rect behind sprite | Animated pulsing brightness overlay |
| Match check point | `GItem.matches()` called from `WItem.draw()` | `InventorySearchWindow.inventorySearchString` read directly in `WItem.draw()` |
| Text matching | Exact substring (`contains`) | Fuzzy (`Fuzzy.fuzzyContains`) |
| Quality filtering | QualityList multi-type | Not supported |
| Food/FEP filtering | Full support | Not supported |
| Armor/attr filtering | Full support | Not supported |
| Curiosity filtering | Full support | Not supported |
| Container filtering | Full support | Not supported |
