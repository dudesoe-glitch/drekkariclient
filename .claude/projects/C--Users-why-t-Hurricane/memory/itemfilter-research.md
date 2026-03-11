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

| Tag | Inner class | Examples |
|-----|------------|---------|
| (none) | `Text` — name substring match | `turnip` |
| `txt:` | `Text` — full tooltip text search | `txt:Well mined` |
| `q` / `q:` | `Q` — quality value | `q>10`, `q+21`, `q<12`, `q:5` |
| `has:` | `Has` — container contents | `has:water`, `has:water>2`, `has:water+3` |
| `lp`, `lph`, `xp`, `mw` | `XP` — curiosity stats | `lp>100`, `lp+200`, `lph>50`, `mw<5` |
| `fep:` | `FEP` — food event points by stat name | `fep:str>1`, `fep:agi+2`, `fep:cha<3` |
| `energy`/`nrg`, `hunger`/`hng` | `Food` — food fill values | `nrg>50`, `nrg<120`, `hng+200` |
| `armor:` | `Armor` — hard/soft armor values | `armor:hard>1`, `armor:soft<2`, `armor:>4` |
| `symb:`/`gast:` | `Gastronomy` — symbel bonuses | `symb:fep>2`, `symb:hunger<3` |
| `attr:` | `Attribute` — equipment/gilding attr bonuses | `attr:str>2`, `attr:survival` |
| `use:`/`uses:` | `Inputs` — crafting input requirements | `use:snow`, `use:iron>2` |
| `eff:`/`effect:` | `Effects` — alchemy ingredient effects | `eff:lore`, `eff:jelly` |

**Sign operators:**
- `>` = GREATER (strictly more than)
- `<` = LESS (less than or equal)
- `=` = EQUAL (exact match)
- `+` = GREQUAL (at least / greater-or-equal)
- `~` = WAVE (used for quality type selection)
- (none) = DEFAULT (nonzero / "has any")

Multiple terms in one query form a `Compound` filter — AND logic, all sub-filters must match.

---

## 2. Architecture — Class Hierarchy

```
ItemFilter (abstract base)
├── matches(List<ItemInfo>) — iterates info list calling match() on each
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
```java
// GItem.java
private static ItemFilter filter;
private static long lastFilter = 0;

public static void setFilter(ItemFilter filter) {
    GItem.filter = filter;
    lastFilter = System.currentTimeMillis();
}
```

**Step 2 — Per-tick lazy evaluation in `GItem.tick()`:**
```java
public void tick(double dt) {
    super.tick(dt);
    // ... spr tick ...
    testMatch();
}

public void testMatch() {
    try {
        if(filtered < lastFilter && spr != null) {
            matches = filter != null && filter.matches(info());
            filtered = lastFilter;
            // notify listeners
        }
    } catch (Loading ignored) {}
}
```
Only re-evaluates when `lastFilter` advances (i.e., when filter changed), not every single tick.

**Step 3 — Highlight in `WItem.draw()`:**
```java
if(item.matches()) {
    g.chcolor(MATCH_COLOR);
    g.rect(Coord.z, sz);
    g.chcolor();
}
```
A semi-transparent colored rect is drawn behind the item sprite for matching items. Non-matching items get no overlay (appear dim by contrast).

**Step 4 — Clear on hide:**
`FilterWnd.hide()` calls `GItem.setFilter(null)` — all items revert to unfiltered display.

---

## 4. FilterWnd — The UI Window

`FilterWnd extends GameUI.Hidewnd` (a show/hide toggle panel, not a standalone Window):
- Single `TextEntry` field, auto-focuses when shown
- Calls `ItemFilter.create(text)` → `GItem.setFilter()` on every keystroke (when length >= 2)
- ESC key clears the text entry and clears the filter without closing the window
- Help button → `ItemFilter.showHelp(ui, FILTER_HELP)` opens a text window with all syntax
- `GameUI.toggleFilter()` creates/shows the window on first call, then just toggles visibility

In `GameUI`:
```java
public FilterWnd filter;

public void toggleFilter() {
    if(filter == null) {
        filter = add(new FilterWnd(), ClientUtils.getScreenCenter(ui));
    }
    filter.toggle();
}
```

---

## 5. Dependencies — What Hurricane Has vs. What It Lacks

| Dependency | Hurricane Status | Notes |
|-----------|-----------------|-------|
| `QualityList` + `SingleType` | **MISSING** | EnderWiggin's multi-quality system. Hurricane uses `QBuff` (single quality via `GItem.getQBuff()`). The `Q` filter needs adaptation. |
| `ItemInfo.getContent()` static | **PARTIAL** — has `ItemInfo.Contents.content()` instance method and `Content.EMPTY`, but no static `getContent(List)` wrapper | Trivial one-liner to add |
| `ItemInfo.getArmor()` | **YES** — Hurricane has this at `ItemInfo.java:619`, same `Pair<Integer,Integer>` return | Direct use |
| `ItemInfo.getBonuses()` | **DIFFERENT signature** — Hurricane returns `Map<Entry, String>` (attrmod.Entry), EnderWiggin returns `Map<Resource, Integer>`. | Attribute filter needs rewrite |
| `ItemInfo.getInputs()` | **MISSING** | Full port from EnderWiggin needed if wanted |
| `Curiosity` (resutil) | **YES** — same fields: `exp`, `mw`, `enc`, `time`, `lph` | Direct use |
| `FoodInfo` (resutil) | **YES** — same fields: `end`, `glut`, `evs[]`, each `ev.nm` | Direct use |
| `Gast` (symb) | **UNKNOWN** — not verified | Check `haven.res.ui.tt.gast` package |
| `ItemInfo.AttrCache` | **YES** — Hurricane has it | Compatible |
| `me.ender.ClientUtils` | **MISSING** | Replace `ClientUtils.round()` with `Math.round()` |
| `me.ender.Reflect` | **YES** — Hurricane has `haven.Reflect` used widely | Check method parity |
| `GItem.matches()` / `GItem.setFilter()` | **MISSING** — Hurricane GItem has no static filter or matches() | Must add |
| `GItem.testMatch()` called from `tick()` | **MISSING** | Must add |
| `GameUI.Hidewnd` | **UNKNOWN** | FilterWnd may need to extend Window instead |

---

## 6. Hurricane's Current InventorySearchWindow

`src/haven/InventorySearchWindow.java` (38 lines):
- Stores a **static** `String inventorySearchString`
- `TextEntry` updates the string on change via `setSearchValue()`
- Close handler sets string to `""` and nulls `gui.inventorySearchWindow`
- Toggle keybind `kb_searchInventoriesButton` in `GameUI` at line ~1957

**How `WItem` currently uses it** (lines 234-261 of Hurricane `WItem.java`):
```java
String searchKeyword = InventorySearchWindow.inventorySearchString.toLowerCase();
if (searchKeyword.length() > 1) {
    if (Fuzzy.fuzzyContains(itemName, searchKeyword)) {
        // Animated pulsing brightness overlay cycling 0-255 at fps-based speed
        g.usestate(new ColorMask(new Color(value, value, value, value)));
    }
}
```
- Uses **fuzzy** matching (not exact substring)
- Animated pulse effect (brightness oscillates)
- Only checks the item name — no ItemInfo queries at all

---

## 7. Porting Strategy

### Option A — Upgrade InventorySearchWindow (recommended for minimal disruption)
1. Keep existing toggle keybind and window
2. Replace `TextEntry` change handler to call `ItemFilter.create(text)` → `GItem.setFilter(filter)`
3. Add to `GItem`: `static ItemFilter filter`, `static long lastFilter`, `setFilter()`, `boolean matches`, `matches()`, `testMatch()` called from `tick()`
4. In `WItem.draw()`: replace the fuzzy animation block with `item.matches()` highlight check
5. Port inner filter classes as capacity allows (see priority order below)

### Option B — Add FilterWnd alongside InventorySearchWindow
- Keeps existing fuzzy search for name-only use
- Adds a separate advanced filter window
- Results in two filter systems — messier

### Inner class port priority (easiest to hardest)
1. `Text` — pure name string match, zero dependencies, drop-in replacement for current fuzzy search
2. `XP` — curiosity: `Curiosity` class already compatible in Hurricane
3. `FEP` / `Food` — food: `FoodInfo` already compatible in Hurricane
4. `Has` — containers: add one-line static `ItemInfo.getContent(List)` wrapper
5. `Armor` — armor: `ItemInfo.getArmor()` already compatible
6. `Q` — quality: adapt to use `QBuff` instead of `QualityList` (see below)
7. `Attribute` — attr bonuses: `getBonuses()` signature differs, requires rewrite around `Entry.attr.name()`
8. `Inputs` — crafting inputs: Hurricane missing `ItemInfo.getInputs()` entirely
9. `Gastronomy` / `Effects` — verify `Gast` and `Effect` classes exist in Hurricane

### Q filter adaptation for Hurricane (QBuff instead of QualityList)
EnderWiggin Q filter uses `QualityList.make(infos)`. Hurricane adaptation:
```java
// In the Q filter's match/matches override
QBuff qBuff = ItemInfo.find(QBuff.class, infos);
if (qBuff != null) {
    return test(qBuff.q);
}
return false;
```

### Attribute filter adaptation for Hurricane
EnderWiggin: `Map<Resource, Integer> bonuses = ItemInfo.getBonuses(infos, null)` — keyed by Resource.
Hurricane: `Map<Entry, String> bonuses = ItemInfo.getBonuses(infos)` — keyed by `attrmod.Entry`.
Adaptation:
```java
Map<Entry, String> bonuses = ItemInfo.getBonuses(infos);
for (Entry entry : bonuses.keySet()) {
    if (entry.attr.name().toLowerCase().startsWith(text)) {
        // parse numeric value from bonuses.get(entry) string (it's already formatted)
        // or use Reflect to get the numeric field from the entry directly
        return test(parsed_value);
    }
}
```

---

## 8. Integration Points in Hurricane

| File | Change Needed |
|------|--------------|
| `GItem.java` | Add `static ItemFilter filter`, `static long lastFilter`, `static void setFilter()`, `boolean matches`, `boolean matches()`, `void testMatch()` called from `tick()` |
| `WItem.java` | In `draw()`: replace/extend the fuzzy animation block; check `item.matches()` for highlight |
| `InventorySearchWindow.java` | Upgrade `TextEntry` handler to call `ItemFilter.create()` + `GItem.setFilter()` instead of storing raw string. Add help button optional. |
| `GameUI.java` | No new field needed if reusing `inventorySearchWindow`; just upgrade the handler. Or add `FilterWnd filter` field for full EnderWiggin parity. |
| `ItemInfo.java` | Add static `getContent(List<ItemInfo>)` wrapper (one-liner delegating to `Contents.content()`) |

---

## 9. Key Differences Summary

| Aspect | EnderWiggin | Hurricane (current) |
|--------|-------------|-------------------|
| Filter architecture | `GItem.setFilter()` static global, per-tick lazy `testMatch()`, `matches()` | Static string `InventorySearchWindow.inventorySearchString` |
| Match highlight | Semi-transparent solid color rect | Animated pulsing brightness overlay |
| Match check site | `GItem.matches()` called from `WItem.draw()` | String read directly in `WItem.draw()` |
| Text matching | Exact substring (`contains`) | Fuzzy (`Fuzzy.fuzzyContains`) |
| Quality filtering | Full (QualityList multi-type) | Not supported |
| Food/FEP filtering | Full | Not supported |
| Armor/attr filtering | Full | Not supported |
| Curiosity filtering | Full | Not supported |
| Container filtering | Full | Not supported |
| Help text | Built-in syntax help window | None |
