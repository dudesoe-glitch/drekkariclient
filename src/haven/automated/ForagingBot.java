package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ForagingBot extends Window implements Runnable {
    private static final double MAX_SEARCH_DIST = 550.0; // ~50 tiles
    private final GameUI gui;
    public volatile boolean stop;
    private volatile boolean active;
    private Button activeButton;
    private Label statusLabel;
    private CheckBox alsoPickGroundItemsCB;
    private boolean alsoPickGroundItems;

    private static final Set<String> PICKABLE_ITEMS = new HashSet<>(Arrays.asList(
        "adder", "arrow", "bat", "swan", "goshawk", "precioussnowflake",
        "truffle-black0", "truffle-black1", "truffle-black2", "truffle-black3",
        "truffle-white0", "truffle-white1", "truffle-white2", "truffle-white3",
        "gemstone", "boarspear"
    ));

    public ForagingBot(GameUI gui) {
        super(UI.scale(250, 100), "Foraging Bot");
        this.gui = gui;
        this.stop = false;
        this.active = false;
        this.alsoPickGroundItems = Utils.getprefb("foragingBotPickGround", false);

        statusLabel = new Label("Idle");
        add(statusLabel, UI.scale(10, 10));

        alsoPickGroundItemsCB = new CheckBox("Also pick ground items") {
            @Override
            public void changed(boolean val) {
                alsoPickGroundItems = val;
                Utils.setprefb("foragingBotPickGround", val);
            }
        };
        alsoPickGroundItemsCB.a = alsoPickGroundItems;
        add(alsoPickGroundItemsCB, UI.scale(10, 30));

        activeButton = new Button(UI.scale(80), "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    this.change("Stop");
                    statusLabel.settext("Running...");
                } else {
                    Gob player = ui.gui.map.player();
                    if (player != null)
                        ui.gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 1, 0);
                    this.change("Start");
                    statusLabel.settext("Stopped");
                }
            }
        };
        add(activeButton, UI.scale(80, 55));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (active) {
                    Gob player = gui.map.player();
                    if (player == null) {
                        Thread.sleep(500);
                        continue;
                    }

                    // HP check
                    if (gui.getmeters("hp").get(1).a < 0.02) {
                        statusLabel.settext("Low HP! Hearthing...");
                        gui.act("travel", "hearth");
                        Thread.sleep(8000);
                        continue;
                    }
                    // Energy check
                    if (gui.getmeter("nrj", 0).a < 0.25) {
                        gui.error("Foraging Bot: Low on energy, stopping.");
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
                    // Inventory full check
                    if (gui.maininv.getFreeSpace() < 2) {
                        gui.error("Foraging Bot: Inventory full, stopping.");
                        active = false;
                        activeButton.change("Start");
                        statusLabel.settext("Inventory full");
                        Thread.sleep(2000);
                        continue;
                    }

                    // If progress bar active, wait
                    if (gui.prog != null) {
                        statusLabel.settext("Working...");
                        Thread.sleep(500);
                        continue;
                    }

                    // Find nearest forageable
                    Gob herb = findNearestForageable();
                    if (herb == null) {
                        statusLabel.settext("No forageables found");
                        Thread.sleep(3000);
                        continue;
                    }

                    // Clear cursor
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(300);
                    }

                    // Pathfind to herb
                    statusLabel.settext("Walking to forageable...");
                    gui.map.pfLeftClick(herb.rc.floor().add(2, 0), null);
                    if (!AUtils.waitPf(gui)) {
                        AUtils.unstuck(gui);
                        Thread.sleep(1000);
                        continue;
                    }

                    // Range check
                    player = gui.map.player();
                    if (player == null) {
                        Thread.sleep(500);
                        continue;
                    }
                    if (herb.rc.dist(player.rc) > 11 * 5) {
                        statusLabel.settext("Too far, retrying...");
                        Thread.sleep(1000);
                        continue;
                    }

                    // Pick the herb (only herbs use "Pick" flower menu; ground items are just right-clicked)
                    statusLabel.settext("Picking...");
                    try {
                        Resource res = herb.getres();
                        if (res != null && res.name.startsWith("gfx/terobjs/herbs")) {
                            FlowerMenu.setNextSelection("Pick");
                        }
                    } catch (Loading ignored) {}
                    gui.map.wdgmsg("click", Coord.z, herb.rc.floor(posres), 3, 0, 0,
                        (int) herb.id, herb.rc.floor(posres), 0, -1);
                    Thread.sleep(1500);

                    // Drop cursor item to inventory if any
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(300);
                    }

                } else {
                    Thread.sleep(500);
                    continue;
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Gob findNearestForageable() {
        Gob closest = null;
        Gob player = gui.map.player();
        if (player == null) return null;
        Coord2d playerPos = player.rc;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res == null) continue;

                    boolean isHerb = res.name.startsWith("gfx/terobjs/herbs");
                    boolean isPickable = alsoPickGroundItems && PICKABLE_ITEMS.contains(res.basename());
                    if (!isHerb && !isPickable) continue;

                    double dist = gob.rc.dist(playerPos);
                    if (dist > MAX_SEARCH_DIST) continue;
                    if (closest == null || dist < closest.rc.dist(playerPos)) {
                        closest = gob;
                    }
                } catch (Loading | NullPointerException ignored) {
                }
            }
        }
        return closest;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (java.util.Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.foragingBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        try {
            Gob player = ui.gui.map.player();
            if (player != null)
                ui.gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 1, 0);
        } catch (Exception ignored) {}
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        if (gui.foragingBotThread != null) {
            gui.foragingBotThread.interrupt();
            gui.foragingBotThread = null;
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-foragingBotWindow", this.c);
        super.reqdestroy();
    }
}
