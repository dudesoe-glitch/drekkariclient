# HANDOFF — Session 16 (BotBase Migration, Bot Audit, Client Research)

## Resumption Prompt
Migrated 4 remaining Window+Runnable tools to BotBase (CombatDistanceTool, CombatRotationBot, MiningSafetyAssistant, OreAndStoneCounter). Removed 4 Thread fields from GameUI. Fixed bot audit findings across 6 bots (null safety, ui.gui→gui, Loading try-catch). Fixed volatile stop in AutoRepeatFlowerMenuScript and CheckpointManager. Researched 8 H&H custom clients — confirmed Hurricane already has every major feature they offer. Build clean.

## Goal
Work through HANDOFF priorities: bot audit, client research comparison, BotBase migration, cleanup.

## Completed

### BotBase Migration (4 tools)
1. **CombatDistanceTool** → BotBase — fixed non-volatile stop, standardized lifecycle, skip idlePlayer in combat
2. **CombatRotationBot** → BotBase — mapped activeButton/statusLabel to BotBase fields, skip idlePlayer
3. **MiningSafetyAssistant** → BotBase — fixed non-volatile stop, standardized lifecycle
4. **OreAndStoneCounter** → BotBase — fixed non-volatile stop, removed duplicate sleep()/stop()/reqdestroy()
5. **GameUI** — removed 4 Thread fields (combatDistanceToolThread, combatRotationBotThread, miningSafetyAssistantThread, oreAndStoneCounterThread)
6. **MenuGrid** — all 4 tools now use startThread() pattern, simplified toggle-off

### Bot Audit Fixes (6 bots)
7. **CellarDiggingBot** — ui.gui.map → gui.map, player null check
8. **CleanupBot** — cached player in findClosestGob(), null guard
9. **FishingBot** — ui.gui.getmeter → gui.getmeter, getmeters null/size check
10. **OceanScoutBot** — ui.gui.map → gui.map throughout, Loading try-catch in isGobCollision/isDangerZone/isVeryDangerZone
11. **TarKilnCleanerBot** — ui.gui → gui throughout, player null check
12. **OreSmeltingBot** — gui.vhand null guard before item access

### Volatile Stop Fixes
13. **AutoRepeatFlowerMenuScript** — `private boolean stop` → `private volatile boolean stop` (scheduler thread writes)
14. **CheckpointManager** — `final boolean stop = false` → `volatile boolean stop` (was dead code, now functional)

### Client Research (8 clients compared)
15. Researched ArdClient, Amber, Purus-Pasta, Paragon, Kami, Minion, Nurgling, Yoink-Pasta
16. Confirmed Hurricane already has every major feature (farming, mining, fishing, foraging, butchering, clay, ore smelting, tar kiln, trellis, cellar, inventory sort/filter/grouping, quality display, auto-drop, pathfinding, combat tools, alarms, equipment swap, batch crafting, map markers, etc.)
17. Hurricane has unique features no other client offers (CombatRotationBot, CombatDistanceTool with RoB weapon ranges, BotBase framework, PathQueue with gold lines, ItemType enum, inventory list view, NotepadWindow)

## Next Priorities
1. **Test all changes in-game** — 16 sessions of untested features
2. **Livestock manager UI** — we have animal data overlays (GobQualityInfo, GobFoodWaterInfo) but no table/list view like other clients
3. **Tune BASE_MELEE_DIST** — needs in-game calibration (currently 13.5)
4. **Disable animations option** — FPS boost for low-end machines (other clients have this)
5. **LP/H display in study tooltip** — Curiosity.java has data, could show in curio tooltip
6. **Consider scripting API** — big effort but ArdClient's PBot was popular

## Files Modified
| File | Changes |
|------|---------|
| `src/haven/GameUI.java` | Removed 4 Thread fields |
| `src/haven/MenuGrid.java` | startThread() pattern for 4 tools, simplified toggle-off |
| `src/haven/CheckpointManager.java` | volatile stop fix |
| `src/haven/automated/CombatDistanceTool.java` | BotBase migration |
| `src/haven/automated/CombatRotationBot.java` | BotBase migration |
| `src/haven/automated/MiningSafetyAssistant.java` | BotBase migration |
| `src/haven/automated/OreAndStoneCounter.java` | BotBase migration |
| `src/haven/automated/AutoRepeatFlowerMenuScript.java` | volatile stop fix |
| `src/haven/automated/CellarDiggingBot.java` | Null safety, ui.gui→gui |
| `src/haven/automated/CleanupBot.java` | Player null guard in findClosestGob |
| `src/haven/automated/FishingBot.java` | Null safety, ui.gui→gui |
| `src/haven/automated/OceanScoutBot.java` | Null safety, ui.gui→gui, Loading try-catch |
| `src/haven/automated/TarKilnCleanerBot.java` | Null safety, ui.gui→gui |
| `src/haven/automated/OreSmeltingBot.java` | gui.vhand null guard |
