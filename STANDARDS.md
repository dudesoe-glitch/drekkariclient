# Hurricane Coding Standards

## Upstream Sync Protocol

When the game updates and the client crashes or can't connect:

1. **Pull official source:**
   ```bash
   git remote add upstream git://sh.seatribe.se/hafen-client 2>/dev/null
   git fetch upstream
   ```

2. **Diff critical files against Hurricane's copies:**
   - `Session.java` — server protocol (MUST match or client can't connect)
   - `Resource.java` — asset loading format (MUST match or resources fail)
   - `Widget.java` — UI message protocol
   - `Config.java` — configuration and version tracking
   - `JOGLPanel.java` — rendering loop

3. **Merge upstream changes** into Hurricane's versions of these files, preserving Hurricane's customizations

4. **Force re-download dependencies** if needed:
   ```bash
   rm lib/ext/jogl/has-jogl    # Forces JOGL re-download
   ant clean && ant run         # Full rebuild
   ```

5. **Test:** Verify connection to official servers works

**Dependencies** auto-download to `lib/ext/` on first build and are cached. They won't be re-checked unless you delete the marker files or run `ant clean`.

---

## Java Conventions

### Naming
- **Classes:** PascalCase (`FarmingBot`, `AutoDropManagerWindow`)
- **Methods:** camelCase (`findClosestGob`, `processTransfer`)
- **Fields:** camelCase, `public` for GUI-accessible state, `private` for internal
- **Constants:** UPPER_SNAKE_CASE (`ITEM_COMPARATOR_ASC`, `PLAYER_INVENTORY_NAMES`)
- **Thread fields:** `<botName>Thread` (e.g., `farmingBotThread`)
- **Bot window fields:** `<botName>` without suffix (e.g., `farmingBot`)

### Package Structure
- Core client: `haven`
- Automation: `haven.automated`
- Bot helpers: `haven.automated.helpers`
- Pathfinding: `haven.automated.pathfinder`
- Food database: `haven.automated.cookbook`

---

## Bot Development Standards

### Bot Class Template
Every bot MUST follow this structure:

```java
package haven.automated;

import haven.*;
import java.util.Objects;
import static haven.OCache.posres;

public class MyBot extends Window implements Runnable {
    private final GameUI gui;
    public boolean stop = false;
    private boolean active = false;
    private Button startButton;

    public MyBot(GameUI gui) {
        super(UI.scale(WIDTH, HEIGHT), "My Bot");
        this.gui = gui;
        // Add UI controls (CheckBoxes, Buttons, Labels)
        startButton = add(new Button(UI.scale(80), "Start") {
            @Override
            public void click() {
                active = !active;
                this.change(active ? "Stop" : "Start");
            }
        }, UI.scale(X, Y));
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                if (active) {
                    // Bot logic here
                }
                Thread.sleep(TICK_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == this && Objects.equals(msg, "close")) {
            stop();
            reqdestroy();
            gui.myBot = null;          // Clear GameUI reference
            gui.myBotThread = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        stop = true;
        if (gui.myBotThread != null) {
            gui.myBotThread.interrupt();
            gui.myBotThread = null;
        }
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-myBotWindow", this.c);  // Save window position
        super.reqdestroy();
    }
}
```

### Required Bot Registration (GameUI.java)
1. Add fields: `public MyBot myBot;` and `public Thread myBotThread;`
2. Add keybind or menu entry to create and start the bot
3. Bot window stores position via `Utils.setprefc` / `Utils.getprefc`

### Bot Safety Checks
Every bot loop MUST include:
- **HP check**: `gui.getmeters("hp").get(1).a < threshold` → hearth travel
- **Energy check**: `gui.getmeter("nrj", 0).a < 0.25` → stop with error
- **Stamina check**: `gui.getmeter("stam", 0).a < 0.40` → drink via `AUtils.drinkTillFull()`
- **Interrupt handling**: `catch (InterruptedException)` → break loop cleanly

### Thread Safety
- **Always `synchronized (gui.map.glob.oc)`** when iterating game objects
- **Handle `Loading` exceptions** — resources load lazily, wrap in try-catch
- **Handle `NullPointerException`** — gobs can despawn between check and use
- **Never block the UI thread** — all bot work runs on its own thread
- **Use `Thread.sleep()` between iterations** — minimum 10ms, typical 500-2000ms

---

## Inventory Operations

### Accessing Items
```java
// Player main inventory
Inventory inv = gui.maininv;

// All items in inventory
List<WItem> items = inv.getAllItems();

// Find by exact name
WItem item = inv.getItemPrecise("gfx/invobjs/seed-turnip");

// Find by partial name
WItem item = inv.getItemPartial("seed-");

// All inventories (including open containers)
List<Inventory> allInvs = gui.getAllInventories();
```

### Quality Access
```java
QBuff qbuff = item.item.getQBuff();
double quality = (qbuff != null) ? qbuff.q : 0;
```

### Item Operations
```java
// Transfer (shift-click behavior)
item.item.wdgmsg("transfer", Coord.z);

// Drop on ground
item.item.wdgmsg("drop", Coord.z);

// Pick up to hand cursor
item.item.wdgmsg("take", Coord.z);

// Right-click item for flower menu
item.item.wdgmsg("iact", Coord.z, gui.ui.modflags());
```

### Sorting
```java
// Existing comparators (quality only)
Collections.sort(items, Inventory.ITEM_COMPARATOR_ASC);   // low → high
Collections.sort(items, Inventory.ITEM_COMPARATOR_DESC);  // high → low
```

---

## Game Object (Gob) Operations

### Finding Gobs
```java
// Find all gobs of a type
ArrayList<Gob> crops = AUtils.getGobs("gfx/terobjs/plants/turnip", gui);

// Find nearest matching gob
synchronized (gui.map.glob.oc) {
    for (Gob gob : gui.map.glob.oc) {
        try {
            Resource res = gob.getres();
            if (res != null && res.name.contains("plants/")) {
                // Found a plant
            }
        } catch (Loading | NullPointerException ignored) {}
    }
}
```

### Interacting with Gobs
```java
// Walk to gob
gui.map.pfLeftClick(gob.rc.floor().add(20, 0), null);
AUtils.waitPf(gui);

// Right-click gob (opens flower menu)
gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0,
    (int) gob.id, gob.rc.floor(posres), 0, -1);

// Auto-select flower menu option
FlowerMenu.setNextSelection("Harvest");
// Then right-click the gob...

// Right-click and auto-select (combined)
AUtils.rightClickGobAndSelectOption(gui, gob, optionIndex);
```

### Player State
```java
Gob player = gui.map.player();
Coord2d playerPos = player.rc;
double distToGob = gob.rc.dist(playerPos);
boolean isWorking = gui.prog != null;
```

---

## Resource Name Conventions

| Category | Pattern | Example |
|----------|---------|---------|
| Inventory items | `gfx/invobjs/<name>` | `gfx/invobjs/seed-turnip` |
| Plants/crops | `gfx/terobjs/plants/<name>` | `gfx/terobjs/plants/turnip` |
| Trees | `gfx/terobjs/trees/<name>` | `gfx/terobjs/trees/birch` |
| Bushes | `gfx/terobjs/bushes/<name>` | `gfx/terobjs/bushes/blackberry` |
| Animals | `gfx/kritter/<name>/<name>` | `gfx/kritter/boar/boar` |
| Players | `gfx/borka/body` | — |
| Rocks | `gfx/terobjs/bumlings/<name>` | `gfx/terobjs/bumlings/basite` |
| Structures | `gfx/terobjs/<name>` | `gfx/terobjs/pow` (fireplace) |

---

## Settings Persistence

```java
// Save/load preferences
Utils.setprefb("myBot_optionName", value);     // boolean
boolean val = Utils.getprefb("myBot_optionName", defaultValue);

Utils.setprefi("myBot_threshold", value);       // int
int val = Utils.getprefi("myBot_threshold", defaultValue);

Utils.setprefc("wndc-myBotWindow", this.c);     // window position (Coord)
Coord pos = Utils.getprefc("wndc-myBotWindow", defaultPos);
```

---

## Code Quality Rules

### CRITICAL (must fix)
- Always `synchronized` on `glob.oc` when iterating gobs
- Always handle `Loading` exceptions on resource access
- Never block the UI thread with long operations
- Always null-check `gob.getres()` before accessing `.name`
- Always null-check `item.getQBuff()` before accessing `.q`
- Always check `gui.map.player()` is not null

### REQUIRED (should fix)
- Use `AUtils` helpers instead of reimplementing gob-finding
- Bot loops must have HP/energy/stamina safety checks
- Save window position in `reqdestroy()`
- Clean up GameUI fields in `wdgmsg("close")`
- Use `UI.scale()` for all pixel coordinates

### PREFERRED (nice to have)
- Use `final` on fields assigned once (constructor params)
- Error messages via `gui.error()`, info via `gui.msg()`
- Sleep durations as named constants, not magic numbers
- Resource name strings as constants or fields, not inline literals
