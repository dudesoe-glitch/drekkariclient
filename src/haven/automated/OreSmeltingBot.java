package haven.automated;

import haven.*;

import java.util.*;
import java.util.List;

import static haven.OCache.posres;

public class OreSmeltingBot extends Window implements Runnable {
    private final GameUI gui;
    public boolean stop;
    private boolean active;
    private Button activeButton;
    private Label statusLabel;
    private Label smelterCountLabel;

    // Ore type toggles
    private boolean doCopperOre;
    private boolean doTinOre;
    private boolean doIronOre;
    private boolean doGoldOre;
    private boolean doSilverOre;
    private boolean doLeadOre;
    private boolean doZincOre;

    // Settings
    private boolean doCollectOutput;
    private int fuelPerLoad;

    // Smelter resource name
    private static final String SMELTER_RES = "gfx/terobjs/smelter";

    // Ore item names (display names as returned by getname())
    private static final Map<String, String> ORE_TYPES = new LinkedHashMap<String, String>() {{
        put("Copper Ore", "oreSmeltingBot_copper");
        put("Tin Ore", "oreSmeltingBot_tin");
        put("Iron Ore", "oreSmeltingBot_iron");
        put("Gold Ore", "oreSmeltingBot_gold");
        put("Silver Ore", "oreSmeltingBot_silver");
        put("Lead Ore", "oreSmeltingBot_lead");
        put("Zinc Ore", "oreSmeltingBot_zinc");
    }};

    // Fuel item name patterns
    private static final String[] FUEL_NAMES = {"Coal", "Charcoal"};

    // Interaction distances and timeouts
    private static final double MAX_INTERACT_DIST = 11 * 5;
    private static final int HAND_TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;

    public OreSmeltingBot(GameUI gui) {
        super(UI.scale(250, 260), "Ore Smelting Bot");
        this.gui = gui;
        this.stop = false;
        this.active = false;

        // Load preferences
        doCopperOre = Utils.getprefb("oreSmeltingBot_copper", true);
        doTinOre = Utils.getprefb("oreSmeltingBot_tin", true);
        doIronOre = Utils.getprefb("oreSmeltingBot_iron", true);
        doGoldOre = Utils.getprefb("oreSmeltingBot_gold", false);
        doSilverOre = Utils.getprefb("oreSmeltingBot_silver", false);
        doLeadOre = Utils.getprefb("oreSmeltingBot_lead", false);
        doZincOre = Utils.getprefb("oreSmeltingBot_zinc", false);
        doCollectOutput = Utils.getprefb("oreSmeltingBot_collect", true);
        fuelPerLoad = Utils.getprefi("oreSmeltingBot_fuelCount", 9);

        int y = 10;

        // Ore type checkboxes - left column
        add(new CheckBox("Copper Ore") {
            { a = doCopperOre; }
            public void set(boolean val) {
                doCopperOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_copper", val);
            }
        }, UI.scale(10, y));

        add(new CheckBox("Gold Ore") {
            { a = doGoldOre; }
            public void set(boolean val) {
                doGoldOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_gold", val);
            }
        }, UI.scale(130, y));
        y += 20;

        add(new CheckBox("Tin Ore") {
            { a = doTinOre; }
            public void set(boolean val) {
                doTinOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_tin", val);
            }
        }, UI.scale(10, y));

        add(new CheckBox("Silver Ore") {
            { a = doSilverOre; }
            public void set(boolean val) {
                doSilverOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_silver", val);
            }
        }, UI.scale(130, y));
        y += 20;

        add(new CheckBox("Iron Ore") {
            { a = doIronOre; }
            public void set(boolean val) {
                doIronOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_iron", val);
            }
        }, UI.scale(10, y));

        add(new CheckBox("Lead Ore") {
            { a = doLeadOre; }
            public void set(boolean val) {
                doLeadOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_lead", val);
            }
        }, UI.scale(130, y));
        y += 20;

        add(new CheckBox("Zinc Ore") {
            { a = doZincOre; }
            public void set(boolean val) {
                doZincOre = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_zinc", val);
            }
        }, UI.scale(10, y));
        y += 25;

        // Collect output checkbox
        add(new CheckBox("Collect output bars") {
            { a = doCollectOutput; }
            public void set(boolean val) {
                doCollectOutput = val;
                a = val;
                Utils.setprefb("oreSmeltingBot_collect", val);
            }
        }, UI.scale(10, y));
        y += 25;

        // Fuel count
        add(new Label("Fuel per smelter:"), UI.scale(10, y));
        TextEntry fuelEntry = new TextEntry(UI.scale(30), String.valueOf(fuelPerLoad)) {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                boolean ret = super.keydown(ev);
                try {
                    int val = Integer.parseInt(text());
                    if (val > 0 && val <= 20) {
                        fuelPerLoad = val;
                        Utils.setprefi("oreSmeltingBot_fuelCount", val);
                    }
                } catch (NumberFormatException ignored) {}
                return ret;
            }
        };
        add(fuelEntry, UI.scale(120, y - 2));
        y += 25;

        // Status
        statusLabel = new Label("Idle");
        add(statusLabel, UI.scale(10, y));
        y += 15;

        smelterCountLabel = new Label("");
        add(smelterCountLabel, UI.scale(10, y));
        y += 20;

        // Start/Stop button
        activeButton = new Button(UI.scale(80), "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    this.change("Stop");
                    statusLabel.settext("Running...");
                } else {
                    ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
                    this.change("Start");
                    statusLabel.settext("Stopped");
                }
            }
        };
        add(activeButton, UI.scale(80, y));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (active && hasAnyOreSelected()) {
                    // HP check
                    if (gui.getmeters("hp").get(1).a < 0.02) {
                        statusLabel.settext("Low HP! Hearthing...");
                        gui.act("travel", "hearth");
                        Thread.sleep(8000);
                        continue;
                    }
                    // Energy check
                    if (gui.getmeter("nrj", 0).a < 0.25) {
                        gui.error("Ore Smelting Bot: Low on energy, stopping.");
                        active = false;
                        activeButton.change("Start");
                        statusLabel.settext("Low energy");
                        Thread.sleep(2000);
                        continue;
                    }
                    // Stamina check
                    if (gui.getmeter("stam", 0).a < 0.40) {
                        statusLabel.settext("Drinking...");
                        AUtils.drinkTillFull(gui, 0.99, 0.99);
                    }

                    // Check if already working
                    if (gui.prog != null) {
                        statusLabel.settext("Working...");
                        waitForProgressBar(10000);
                        Thread.sleep(500);
                        continue;
                    }

                    // Find nearest smelter
                    Gob smelter = findNearestSmelter();
                    if (smelter == null) {
                        statusLabel.settext("No smelters found nearby");
                        smelterCountLabel.settext("");
                        Thread.sleep(3000);
                        continue;
                    }

                    // Count available smelters
                    int smelterCount = countSmelters();
                    smelterCountLabel.settext("Smelters nearby: " + smelterCount);

                    // Clear hand if holding something
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        AUtils.waitForEmptyHand(gui, 1000, "Ore Smelting Bot: Couldn't clear hand");
                    }

                    // Check if we have ore to smelt
                    WItem oreItem = findOreInInventory();
                    if (oreItem == null) {
                        // No ore - try to collect output if enabled
                        if (doCollectOutput) {
                            statusLabel.settext("No ore found. Collecting output...");
                            collectOutputFromSmelter(smelter);
                        } else {
                            statusLabel.settext("No ore in inventory");
                            active = false;
                            activeButton.change("Start");
                        }
                        Thread.sleep(2000);
                        continue;
                    }

                    // Check if we have fuel
                    WItem fuelItem = findFuelInInventory();
                    if (fuelItem == null) {
                        statusLabel.settext("No fuel (Coal/Charcoal) in inventory");
                        active = false;
                        activeButton.change("Start");
                        Thread.sleep(2000);
                        continue;
                    }

                    // Process the smelter
                    processSmelter(smelter);

                } else {
                    Thread.sleep(1000);
                    continue;
                }
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processSmelter(Gob smelter) throws InterruptedException {
        // Step 1: Pathfind to smelter
        statusLabel.settext("Walking to smelter...");
        gui.map.pfLeftClick(smelter.rc.floor().add(2, 0), null);
        if (!AUtils.waitPf(gui)) {
            AUtils.unstuck(gui);
            Thread.sleep(1000);
            return;
        }

        // Check range
        if (smelter.rc.dist(gui.map.player().rc) > MAX_INTERACT_DIST) {
            statusLabel.settext("Too far from smelter, retrying...");
            Thread.sleep(1000);
            return;
        }

        // Step 2: Load ore into smelter
        statusLabel.settext("Loading ore...");
        int oreLoaded = loadItemsIntoSmelter(smelter, true);
        if (oreLoaded == 0) {
            statusLabel.settext("Failed to load ore");
            Thread.sleep(1000);
            return;
        }
        statusLabel.settext("Loaded " + oreLoaded + " ore");
        Thread.sleep(300);

        // Step 3: Load fuel into smelter
        statusLabel.settext("Loading fuel...");
        int fuelLoaded = loadFuelIntoSmelter(smelter);
        if (fuelLoaded == 0) {
            statusLabel.settext("Failed to load fuel");
            Thread.sleep(1000);
            return;
        }
        statusLabel.settext("Loaded " + fuelLoaded + " fuel");
        Thread.sleep(300);

        // Step 4: Light the smelter
        statusLabel.settext("Lighting smelter...");
        FlowerMenu.setNextSelection("Light");
        gui.map.wdgmsg("click", Coord.z, smelter.rc.floor(posres), 3, 0, 0,
            (int) smelter.id, smelter.rc.floor(posres), 0, -1);

        // Wait for flower menu to appear and auto-select
        Thread.sleep(1500);

        // Step 5: Wait for smelting to complete
        statusLabel.settext("Smelting in progress...");

        // Smelting takes a while - poll for completion
        // The smelter changes its visual state when done
        // We wait up to 5 minutes, checking periodically
        int waitTime = 0;
        int maxWait = 300000; // 5 minutes
        while (waitTime < maxWait && active && !stop) {
            // Check if progress bar is active
            if (gui.prog != null) {
                waitForProgressBar(60000);
                Thread.sleep(500);
            }

            // Check stamina while waiting
            if (gui.getmeter("stam", 0).a < 0.40) {
                AUtils.drinkTillFull(gui, 0.99, 0.99);
            }

            // Check if smelter has changed state (no longer active/lit)
            // Smelters have drawable state data indicating activity
            try {
                ResDrawable rd = smelter.getattr(ResDrawable.class);
                if (rd != null) {
                    int state = rd.sdt.checkrbuf(0);
                    // State 0 typically means idle/done, non-zero means active
                    // After lighting, smelter state changes; when it goes back to idle, smelting is done
                    if (waitTime > 5000 && state == 0) {
                        statusLabel.settext("Smelting complete!");
                        break;
                    }
                }
            } catch (Exception ignored) {}

            Thread.sleep(3000);
            waitTime += 3000;
            int minutes = waitTime / 60000;
            int seconds = (waitTime % 60000) / 1000;
            statusLabel.settext("Smelting... " + minutes + "m " + seconds + "s");
        }

        Thread.sleep(1000);

        // Step 6: Collect output
        if (doCollectOutput && active && !stop) {
            statusLabel.settext("Collecting output...");
            collectOutputFromSmelter(smelter);
        }
    }

    /**
     * Load ore items from player inventory into the smelter.
     * Uses the pick-up-and-click pattern from AddCoalToSmelter.
     * Returns the number of ore items loaded.
     */
    private int loadItemsIntoSmelter(Gob smelter, boolean isOre) throws InterruptedException {
        int loaded = 0;
        List<WItem> items = isOre ? findAllOreInInventory() : new ArrayList<>();

        for (WItem witem : items) {
            if (stop || !active) break;

            try {
                GItem item = witem.item;
                // Pick up the item
                item.wdgmsg("take", new Coord(item.sz.x / 2, item.sz.y / 2));

                // Wait for item to be in hand
                if (!waitForHand(true)) {
                    continue;
                }

                GItem handItem = gui.vhand.item;

                // Click on smelter with the item (shift-click to transfer stack)
                gui.map.wdgmsg("itemact", Coord.z, smelter.rc.floor(posres), 1, 0,
                    (int) smelter.id, smelter.rc.floor(posres), 0, -1);

                Thread.sleep(300);

                // Wait for hand to be empty (item deposited) or changed (next from stack)
                int timeout = 0;
                while (timeout < HAND_TIMEOUT) {
                    WItem handNow = gui.vhand;
                    if (handNow == null) {
                        // Item deposited, hand is empty
                        break;
                    } else if (handNow.item != handItem) {
                        // Stack - a new item replaced the old one; drop it back
                        handNow.item.wdgmsg("drop", Coord.z);
                        AUtils.waitForEmptyHand(gui, 1000, "");
                        break;
                    }
                    timeout += HAND_DELAY;
                    Thread.sleep(HAND_DELAY);
                }

                loaded++;
                Thread.sleep(100);
            } catch (Loading ignored) {}
        }

        // Make sure hand is clear after loading
        if (gui.vhand != null) {
            gui.vhand.item.wdgmsg("drop", Coord.z);
            AUtils.waitForEmptyHand(gui, 1000, "");
        }

        return loaded;
    }

    /**
     * Load fuel (coal/charcoal) into the smelter.
     * Follows the AddCoalToSmelter pattern closely.
     */
    private int loadFuelIntoSmelter(Gob smelter) throws InterruptedException {
        WItem fuelWItem = findFuelInInventory();
        if (fuelWItem == null) {
            return 0;
        }

        GItem fuel = fuelWItem.item;
        // Pick up the fuel
        fuel.wdgmsg("take", new Coord(fuel.sz.x / 2, fuel.sz.y / 2));

        if (!waitForHand(true)) {
            return 0;
        }

        fuel = gui.vhand.item;
        int loaded = 0;

        for (int i = 0; i < fuelPerLoad && active && !stop; i++) {
            // Use shift-click (modifier 1) for all but the last piece
            int modifier = (i < fuelPerLoad - 1) ? 1 : 0;
            gui.map.wdgmsg("itemact", Coord.z, smelter.rc.floor(posres), modifier, 0,
                (int) smelter.id, smelter.rc.floor(posres), 0, -1);

            int timeout = 0;
            boolean done = false;
            while (timeout < HAND_TIMEOUT) {
                WItem newFuel = gui.vhand;
                if (newFuel != null && newFuel.item != fuel) {
                    // New item from stack
                    fuel = newFuel.item;
                    loaded++;
                    break;
                } else if (newFuel == null) {
                    // Hand empty - no more fuel or last piece deposited
                    loaded++;
                    done = true;
                    break;
                }
                timeout += HAND_DELAY;
                Thread.sleep(HAND_DELAY);
            }

            if (timeout >= HAND_TIMEOUT) {
                // Timed out - smelter might be full
                break;
            }

            if (done) {
                break;
            }
        }

        // Clear hand if still holding fuel
        if (gui.vhand != null) {
            gui.vhand.item.wdgmsg("drop", Coord.z);
            AUtils.waitForEmptyHand(gui, 1000, "");
        }

        return loaded;
    }

    /**
     * Collect output bars/products from a smelter.
     * Right-clicks the smelter to open its inventory, then transfers items out.
     */
    private void collectOutputFromSmelter(Gob smelter) throws InterruptedException {
        // Walk to smelter if needed
        if (smelter.rc.dist(gui.map.player().rc) > MAX_INTERACT_DIST) {
            gui.map.pfLeftClick(smelter.rc.floor().add(2, 0), null);
            if (!AUtils.waitPf(gui)) {
                return;
            }
        }

        // Right-click smelter to open it (button 3 = right click)
        gui.map.wdgmsg("click", Coord.z, smelter.rc.floor(posres), 3, 0, 0,
            (int) smelter.id, smelter.rc.floor(posres), 0, -1);
        Thread.sleep(1500);

        // The smelter should now have a window open
        // Look for items in non-player inventories and transfer them
        // We use Ctrl+A (transfer all) by shift-clicking items in the smelter window
        List<Inventory> allInvs = gui.getAllInventories();
        for (Inventory inv : allInvs) {
            // Skip the main player inventory
            if (inv == gui.maininv) continue;

            // Check if this inventory is in a window that looks like a smelter
            if (inv.parent instanceof Window) {
                String cap = ((Window) inv.parent).cap;
                if (cap != null && (cap.contains("Smelter") || cap.contains("smelter"))) {
                    // Transfer all items from this inventory to player inventory
                    List<WItem> items = inv.getAllItems();
                    for (WItem item : items) {
                        if (stop || !active) break;
                        try {
                            item.item.wdgmsg("transfer", Coord.z);
                            Thread.sleep(100);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        Thread.sleep(500);
    }

    private Gob findNearestSmelter() {
        Gob closest = null;
        Coord2d playerPos = gui.map.player().rc;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res == null) continue;

                    if (res.name.contains("smelter")) {
                        if (closest == null || gob.rc.dist(playerPos) < closest.rc.dist(playerPos)) {
                            closest = gob;
                        }
                    }
                } catch (Loading | NullPointerException ignored) {}
            }
        }
        return closest;
    }

    private int countSmelters() {
        int count = 0;
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && res.name.contains("smelter")) {
                        count++;
                    }
                } catch (Loading | NullPointerException ignored) {}
            }
        }
        return count;
    }

    /**
     * Find a single ore item in the player's main inventory that matches enabled ore types.
     */
    private WItem findOreInInventory() {
        for (Widget wdg = gui.maininv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    String name = wi.item.getname();
                    if (isOreEnabled(name)) {
                        return wi;
                    }
                } catch (Loading ignored) {}
            }
        }
        return null;
    }

    /**
     * Find all ore items in the player's main inventory that match enabled ore types.
     */
    private List<WItem> findAllOreInInventory() {
        List<WItem> ores = new ArrayList<>();
        for (Widget wdg = gui.maininv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    String name = wi.item.getname();
                    if (isOreEnabled(name)) {
                        ores.add(wi);
                    }
                } catch (Loading ignored) {}
            }
        }
        return ores;
    }

    /**
     * Find fuel (Coal or Charcoal) in the player's inventory.
     */
    private WItem findFuelInInventory() {
        for (Widget wdg = gui.maininv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    String name = wi.item.getname();
                    if (name != null && !name.contains("stack of")) {
                        for (String fuelName : FUEL_NAMES) {
                            if (name.contains(fuelName)) {
                                return wi;
                            }
                        }
                    }
                } catch (Loading ignored) {}
            }
        }
        return null;
    }

    /**
     * Check if an ore name corresponds to an enabled ore type.
     */
    private boolean isOreEnabled(String itemName) {
        if (itemName == null) return false;
        if (doCopperOre && itemName.contains("Copper") && itemName.contains("Ore")) return true;
        if (doTinOre && itemName.contains("Tin") && itemName.contains("Ore")) return true;
        if (doIronOre && itemName.contains("Iron") && itemName.contains("Ore")) return true;
        if (doGoldOre && itemName.contains("Gold") && itemName.contains("Ore")) return true;
        if (doSilverOre && itemName.contains("Silver") && itemName.contains("Ore")) return true;
        if (doLeadOre && itemName.contains("Lead") && itemName.contains("Ore")) return true;
        if (doZincOre && itemName.contains("Zinc") && itemName.contains("Ore")) return true;
        return false;
    }

    private boolean hasAnyOreSelected() {
        return doCopperOre || doTinOre || doIronOre || doGoldOre
            || doSilverOre || doLeadOre || doZincOre;
    }

    /**
     * Wait for the player's hand to become occupied or empty.
     * @param occupied true to wait for hand occupied, false for empty
     * @return true if condition met within timeout
     */
    private boolean waitForHand(boolean occupied) throws InterruptedException {
        int timeout = 0;
        while (timeout < HAND_TIMEOUT) {
            if (occupied && gui.vhand != null) return true;
            if (!occupied && gui.vhand == null) return true;
            timeout += HAND_DELAY;
            Thread.sleep(HAND_DELAY);
        }
        return false;
    }

    private void waitForProgressBar(int timeout) throws InterruptedException {
        int elapsed = 0;
        int hz = 100;
        while (gui.prog != null && gui.prog.prog != -1 && elapsed < timeout) {
            elapsed += hz;
            Thread.sleep(hz);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.oreSmeltingBot = null;
            gui.oreSmeltingBotThread = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        if (gui.oreSmeltingBotThread != null) {
            gui.oreSmeltingBotThread.interrupt();
            gui.oreSmeltingBotThread = null;
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-oreSmeltingBotWindow", this.c);
        super.reqdestroy();
    }
}
