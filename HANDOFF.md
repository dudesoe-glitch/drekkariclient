# HANDOFF — Session 11 (Code Review Fixes, AUtils Cleanup, ItemType, ExtInventory Context Menu)

## Resumption Prompt
Committed session 10's work, then ran comprehensive code review (6 critical, 11 major issues found) and research on EnderWiggin port candidates. Fixed all critical/major bugs in parallel: NPE guards in Actions, BotBase stop/destroy ordering, CleanupBot null checks, OptWndAutoLootPanel pref key, RoastingSpitBot Loading/Exception safety, aggro scripts gui.fv null dereference. Cleaned up AUtils (deleted dead code, deprecated 5 superseded methods). Added GobHelper.isPlayer/hasOverlay/findAllSupports, InvHelper.findFirstByNameInAllInventories. Migrated FillCheeseTray, MiningSafetyAssistant, 4 aggro scripts, ButcherBot to use helpers. Fixed Tex leaks in ExtInventoryWindow/Inventory. Added ItemType enum with classify(), BY_TYPE grouping, right-click context menu (Transfer All/Drop All). Build clean.

## Goal
Code review + bug fixes, AUtils cleanup, EnderWiggin feature ports.

## Completed

### Critical Bug Fixes
1. **Actions.java** — NPE guards on player() in rightClick(), waitPf(), unstuck(); added clearhand() method
2. **BotBase.java** — Removed premature this.destroy() from stop() (was tearing down widget before reqdestroy could save position)
3. **CleanupBot.java** — Fixed null dereference on getres() in both destroyGob() and findClosestGob(); migrated clearhand to Actions
4. **OptWndAutoLootPanel.java** — Fixed Pouches checkbox reading/writing "autoLootCloakRobe" instead of "autoLootPouches"
5. **RoastingSpitBot.java** — Added Loading try-catch in findSpitroastableItems(); wrapped readyToRoast()/isCooked() unsafe render tree casts in try-catch

### Aggro/Combat Fixes
6. **AggroNearestTarget/Player/EveryoneInRange** — Fixed gui.fv null dereference before synchronized block
7. **All 4 aggro scripts** — Replaced private isPlayer() with GobHelper.isPlayer()
8. **ButcherBot.java** — Replaced inline knock pose check with GobHelper.isKnocked()

### AUtils Cleanup
9. **AUtils.java** — Deleted 2 zero-caller methods (getAllItemsFromAllInventoriesAndStacksExcludeBeltAndKeyring, isBeltOrKeyring); deprecated 5 superseded methods with @Deprecated + javadoc pointers
10. **GobHelper.java** — Added isPlayer(), hasOverlay(), findAllSupports()
11. **InvHelper.java** — Added findFirstByNameInAllInventories()
12. **FillCheeseTray.java** — Migrated from AUtils to InvHelper
13. **MiningSafetyAssistant.java** — Migrated from AUtils to GobHelper
14. **CellarDiggingBot.java** — Migrated clearhand to Actions

### UI Improvements
15. **ExtInventoryWindow.java** — Fixed Tex leak (icon cache), static Color constants, right-click context menu (Transfer All/Drop All)
16. **Inventory.java** — Fixed Tex leak in drawGroupHeaders (icon cache)
17. **ItemType.java** — NEW — Centralized item classification enum (9 types: Food/Armor/Curiosity/Tool/Seed/Container/Material/Weapon/Unknown) with classify(), color(), label()
18. **ItemGrouping.java** — Added BY_TYPE grouping mode using ItemType.classify()

## In Progress
Nothing — all planned work complete.

## Next Priorities
1. **Test everything in-game** — 11 sessions of features untested!
2. **Remaining code review items** — FishingBot putOn* method deduplication, BotBase.checkVitals() broad catch, sortInventory() untracked thread
3. **More EnderWiggin ports** — repeat-action-for-group in ExtInventory, ItemType metadata enrichment
4. **WItem badge refactor** — Replace WItem's inline badge detection with ItemType.classify()

## Files Modified/Created
| File | Changes |
|------|---------|
| `src/haven/automated/Actions.java` | NPE guards + clearhand() |
| `src/haven/automated/BotBase.java` | Remove premature destroy() in stop() |
| `src/haven/automated/CleanupBot.java` | Null checks + clearhand migration |
| `src/haven/automated/CellarDiggingBot.java` | clearhand migration |
| `src/haven/OptWndAutoLootPanel.java` | Fix Pouches pref key |
| `src/haven/automated/RoastingSpitBot.java` | Loading + Exception safety |
| `src/haven/automated/AggroEveryoneInRange.java` | gui.fv null fix + isPlayer migration |
| `src/haven/automated/AggroNearestPlayer.java` | gui.fv null fix + isPlayer migration |
| `src/haven/automated/AggroNearestTarget.java` | gui.fv null fix + isPlayer migration |
| `src/haven/automated/AggroOrTargetCursorNearest.java` | isPlayer migration |
| `src/haven/automated/ButcherBot.java` | GobHelper.isKnocked() |
| `src/haven/automated/AUtils.java` | Delete dead code, deprecate 5 methods |
| `src/haven/automated/GobHelper.java` | isPlayer + hasOverlay + findAllSupports |
| `src/haven/automated/InvHelper.java` | findFirstByNameInAllInventories |
| `src/haven/automated/FillCheeseTray.java` | Migrate to InvHelper |
| `src/haven/automated/MiningSafetyAssistant.java` | Migrate to GobHelper |
| `src/haven/ExtInventoryWindow.java` | Tex cache + Color constants + context menu |
| `src/haven/Inventory.java` | Icon Tex cache in drawGroupHeaders |
| `src/haven/ItemType.java` | NEW — Item classification enum |
| `src/haven/ItemGrouping.java` | BY_TYPE grouping mode |
