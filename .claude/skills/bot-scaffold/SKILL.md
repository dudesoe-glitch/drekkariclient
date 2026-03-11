---
name: bot-scaffold
description: "Create a new bot class from the established Window+Runnable template with GameUI registration"
argument-hint: "<BotName>"
disable-model-invocation: true
---

# Bot Scaffold Generator

Create a new bot following Hurricane's established bot pattern.

## Steps

1. **Parse `$ARGUMENTS`** as the bot name (e.g., `FarmingBot`, `AutoPlanterBot`)
   - If no argument, ask the user what the bot should do

2. **Ask the user for bot configuration:**
   - What does the bot do? (one-sentence description)
   - What UI controls does it need? (checkboxes, dropdowns, labels)
   - What game objects does it interact with? (crops, animals, structures)
   - What flower menu actions does it use? (Harvest, Plant, Pick, etc.)
   - Does it need inventory access? (finding items, quality checks)

3. **Create the bot class** at `src/haven/automated/<BotName>.java`:
   - Extend `Window implements Runnable`
   - Follow the template from STANDARDS.md § Bot Class Template exactly
   - Include HP/energy/stamina safety checks in the run loop
   - Include `wdgmsg("close")` cleanup
   - Include `reqdestroy()` position save
   - Include `stop()` with thread interrupt and pathfinder cleanup

4. **Register in GameUI.java:**
   - Add `public <BotName> <botFieldName>;` field
   - Add `public Thread <botFieldName>Thread;` field
   - Show user where to add keybind/menu entry (don't auto-modify GameUI — it's 5000+ lines)

5. **Verify compilation:** Run `ant run` in dry mode or check syntax

6. **Report:** Created files, registration instructions, next steps

## Example

```
/bot-scaffold FarmingBot

Created: src/haven/automated/FarmingBot.java

Register in GameUI.java:
  Line ~190: public FarmingBot farmingBot;
             public Thread farmingBotThread;

  In keybind handler, add:
    farmingBot = new FarmingBot(this);
    farmingBot.show();
    add(farmingBot, Utils.getprefc("wndc-farmingBotWindow", UI.scale(200, 200)));
    farmingBotThread = new Thread(farmingBot, "FarmingBot");
    farmingBotThread.start();
```
