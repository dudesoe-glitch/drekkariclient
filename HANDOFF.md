# HANDOFF — Session 15 (Minimap Fix, Combat Rotation, Weapon Tuning)

## Resumption Prompt
Fixed minimap click-to-move (cross-segment fallback using dloc+player.rc). Created CombatRotationBot (configurable skill sequences with loop/cooldown). Updated all weapon distances with Ring of Brodgar wiki multipliers (BASE_MELEE_DIST * range). Fixed CombatDistanceTool double-destroy bug. Researched Ardennes/Amber clients. Bot audit running. Build clean.

## Goal
Work through HANDOFF priorities: minimap click fix, combat rotation, weapon tuning, bot audit.

## Completed

### Bug Fixes
1. **Minimap click-to-move** — Root cause: `mvclick()` silently dropped clicks when `sessloc.seg != loc.seg`. Fix: compute world position from `dloc` (display center) + `player.rc` as fallback. Also relaxed `MapWnd.clickloc` segment check. Backwards-compatible: same-segment clicks unchanged.
2. **CombatDistanceTool double-destroy** — `stop()` called `this.destroy()` AND `wdgmsg` handler called `reqdestroy()`. Fixed: removed `destroy()` from `stop()`, added null safety for `gui.map`.

### New Features
3. **CombatRotationBot** — Configurable combat skill sequence executor:
   - Define rotation steps: action bar slot (1-10) + repeat count (up to 8 steps)
   - Loop rotation and wait-for-cooldown options
   - Sends moves via Fightsess "use"/"rel" messages with opponent targeting
   - Persists rotation to preferences
   - Slot key reference label (1-3=1/2/3, 4=R, 5=F, etc.)
   - Thread-safe step list (synchronized)
   - Registered in MenuGrid under OtherScriptsAndTools

4. **Weapon distance overhaul** — Updated all melee weapon distances using Ring of Brodgar wiki range multipliers:
   - `BASE_MELEE_DIST = 13.5` × wiki multiplier (1.0-1.6)
   - Added 10+ new weapons: knives, cleaver, scythe, sling, metal axe, throwing axe, obsidian dagger
   - One constant to tune all melee weapons at once

## In Progress
- **Ardennes/Amber client research** — background agent researching features and repos
- **Full bot audit** — background agent reviewing all 12+ bots for edge cases

## Next Priorities
1. **Test all changes in-game** — 15 sessions of untested features!
2. **Apply bot audit findings** — fix issues identified by audit
3. **Review Ardennes/Amber research** — decide what to port
4. **Tune BASE_MELEE_DIST** — needs in-game calibration (currently 13.5)
5. **Consider CombatDistanceTool → BotBase migration**
6. **QoL lessons document** — patterns applicable to future projects

## Files Modified
| File | Changes |
|------|---------|
| `src/haven/MiniMap.java` | Cross-segment click fallback using dloc + player.rc |
| `src/haven/MapWnd.java` | Relaxed clickloc segment check |
| `src/haven/GameUI.java` | Added combatRotationBot + combatRotationBotThread fields |
| `src/haven/MenuGrid.java` | CombatRotation menu entry + handler |
| `src/haven/automated/CombatRotationBot.java` | **NEW** — Combat skill rotation bot |
| `src/haven/automated/CombatDistanceTool.java` | Wiki-based weapon distances, double-destroy fix, null safety |
| `res/customclient/menugrid/OtherScriptsAndTools/CombatRotation.res` | **NEW** — Menu icon |
| `Diary/2026-03-12-session15.md` | **NEW** — Session diary |
