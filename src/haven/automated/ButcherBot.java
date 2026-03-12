package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ButcherBot extends Window implements Runnable {
    private static final double MAX_SEARCH_DIST = 550.0; // ~50 tiles
    private final GameUI gui;
    public volatile boolean stop;
    private volatile boolean active;
    private Button activeButton;
    private Label statusLabel;

    private boolean doLargeGame;
    private boolean doLivestock;
    private boolean doPredators;
    private boolean doSmallGame;

    // Large game: big wild animals hunted for meat
    private static final Set<String> LARGE_GAME = new HashSet<>(Arrays.asList(
        "gfx/kritter/boar/boar",
        "gfx/kritter/moose/moose",
        "gfx/kritter/reddeer/reddeer",
        "gfx/kritter/reindeer/reindeer",
        "gfx/kritter/roedeer/roedeer",
        "gfx/kritter/mammoth/mammoth",
        "gfx/kritter/walrus/walrus",
        "gfx/kritter/greyseal/greyseal",
        "gfx/kritter/troll/troll"
    ));

    // Livestock: domestic farm animals
    private static final Set<String> LIVESTOCK = new HashSet<>(Arrays.asList(
        "gfx/kritter/cattle/cattle",
        "gfx/kritter/cattle/calf",
        "gfx/kritter/sheep/sheep",
        "gfx/kritter/sheep/lamb",
        "gfx/kritter/goat/wildgoat",
        "gfx/kritter/goat/nanny",
        "gfx/kritter/goat/billy",
        "gfx/kritter/goat/kid",
        "gfx/kritter/pig/hog",
        "gfx/kritter/pig/sow",
        "gfx/kritter/pig/piglet",
        "gfx/kritter/horse/horse",
        "gfx/kritter/horse/foal",
        "gfx/kritter/chicken/chicken",
        "gfx/kritter/chicken/hen",
        "gfx/kritter/chicken/rooster",
        "gfx/kritter/chicken/chick"
    ));

    // Predators: dangerous wild animals
    private static final Set<String> PREDATORS = new HashSet<>(Arrays.asList(
        "gfx/kritter/bear/bear",
        "gfx/kritter/bear/polarbear",
        "gfx/kritter/wolf/wolf",
        "gfx/kritter/lynx/lynx",
        "gfx/kritter/wolverine/wolverine",
        "gfx/kritter/adder/adder",
        "gfx/kritter/caveangler/caveangler",
        "gfx/kritter/cavelouse/cavelouse",
        "gfx/kritter/orca/orca",
        "gfx/kritter/nidbane/nidbane"
    ));

    // Small game: small critters
    private static final Set<String> SMALL_GAME = new HashSet<>(Arrays.asList(
        "gfx/kritter/fox/fox",
        "gfx/kritter/badger/badger",
        "gfx/kritter/beaver/beaver",
        "gfx/kritter/otter/otter",
        "gfx/kritter/stoat/stoat",
        "gfx/kritter/swan/swan",
        "gfx/kritter/pelican/pelican",
        "gfx/kritter/eagleowl/eagleowl",
        "gfx/kritter/goldeneagle/goldeneagle",
        "gfx/kritter/woodgrouse/woodgrouse-m",
        "gfx/kritter/garefowl/garefowl",
        "gfx/kritter/goshawk/goshawk",
        "gfx/kritter/crane/crane",
        "gfx/kritter/mallard/mallard",
        "gfx/kritter/chasmconch/chasmconch",
        "gfx/kritter/ooze/greenooze"
    ));

    public ButcherBot(GameUI gui) {
        super(UI.scale(220, 145), "Butcher Bot");
        this.gui = gui;
        this.stop = false;
        this.active = false;

        doLargeGame = Utils.getprefb("butcherBot_largeGame", true);
        doLivestock = Utils.getprefb("butcherBot_livestock", true);
        doPredators = Utils.getprefb("butcherBot_predators", true);
        doSmallGame = Utils.getprefb("butcherBot_smallGame", true);

        add(new CheckBox("Large Game") {
            { a = doLargeGame; }
            public void set(boolean val) {
                doLargeGame = val;
                a = val;
                Utils.setprefb("butcherBot_largeGame", val);
            }
        }, UI.scale(10, 10));

        add(new CheckBox("Livestock") {
            { a = doLivestock; }
            public void set(boolean val) {
                doLivestock = val;
                a = val;
                Utils.setprefb("butcherBot_livestock", val);
            }
        }, UI.scale(10, 30));

        add(new CheckBox("Predators") {
            { a = doPredators; }
            public void set(boolean val) {
                doPredators = val;
                a = val;
                Utils.setprefb("butcherBot_predators", val);
            }
        }, UI.scale(110, 10));

        add(new CheckBox("Small Game") {
            { a = doSmallGame; }
            public void set(boolean val) {
                doSmallGame = val;
                a = val;
                Utils.setprefb("butcherBot_smallGame", val);
            }
        }, UI.scale(110, 30));

        statusLabel = new Label("Idle");
        add(statusLabel, UI.scale(10, 60));

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
        add(activeButton, UI.scale(65, 105));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (active && (doLargeGame || doLivestock || doPredators || doSmallGame)) {
                    // HP check
                    if (gui.getmeters("hp").get(1).a < 0.02) {
                        statusLabel.settext("Low HP! Hearthing...");
                        gui.act("travel", "hearth");
                        Thread.sleep(8000);
                        continue;
                    }
                    // Energy check
                    if (gui.getmeter("nrj", 0).a < 0.25) {
                        gui.error("Butcher Bot: Low on energy, stopping.");
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
                        gui.error("Butcher Bot: Inventory full, stopping.");
                        active = false;
                        activeButton.change("Start");
                        statusLabel.settext("Inventory full");
                        Thread.sleep(2000);
                        continue;
                    }

                    // Check if already working (progress bar active)
                    if (gui.prog != null) {
                        statusLabel.settext("Working...");
                        waitForProgressBar(10000);
                        Thread.sleep(500);
                        continue;
                    }

                    // Find nearest knocked animal
                    Gob animal = findNearestKnockedAnimal();
                    if (animal == null) {
                        statusLabel.settext("No knocked animals found");
                        Thread.sleep(3000);
                        continue;
                    }

                    // Clear hand if holding something
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(500);
                    }

                    String animalName = "animal";
                    try {
                        Resource res = animal.getres();
                        if (res != null) {
                            String name = res.name;
                            animalName = name.substring(name.lastIndexOf('/') + 1);
                        }
                    } catch (Loading ignored) {}
                    statusLabel.settext("Walking to " + animalName);

                    // Pathfind to animal
                    gui.map.pfLeftClick(animal.rc.floor().add(2, 0), null);
                    if (!AUtils.waitPf(gui)) {
                        AUtils.unstuck(gui);
                        Thread.sleep(1000);
                        continue;
                    }

                    // Check range
                    if (animal.rc.dist(gui.map.player().rc) > 11 * 5) {
                        statusLabel.settext("Too far, retrying...");
                        Thread.sleep(1000);
                        continue;
                    }

                    // Butcher: set flower menu selection then right-click
                    statusLabel.settext("Butchering " + animalName);
                    FlowerMenu.setNextSelection("Butcher");
                    gui.map.wdgmsg("click", Coord.z, animal.rc.floor(posres), 3, 0, 0,
                        (int) animal.id, animal.rc.floor(posres), 0, -1);

                    // Wait a moment for the flower menu to appear and auto-select
                    Thread.sleep(1000);

                    // Wait for progress bar (butchering action)
                    waitForProgressBar(30000);

                    // Clear hand if butchering dropped something on cursor
                    Thread.sleep(500);
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(500);
                    }

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

    private void waitForProgressBar(int timeout) throws InterruptedException {
        int elapsed = 0;
        int hz = 100;
        while (gui.prog != null && gui.prog.prog != -1 && elapsed < timeout) {
            if (stop || !active) break;
            elapsed += hz;
            Thread.sleep(hz);
        }
    }

    private Gob findNearestKnockedAnimal() {
        Gob closest = null;
        Gob player = gui.map.player();
        if (player == null) return null;
        Coord2d playerPos = player.rc;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res == null) continue;

                    // Must be knocked (dead)
                    if (!gob.getPoses().contains("knock")) continue;

                    // Check if this animal type is enabled
                    if (!isAnimalEnabled(res.name)) continue;

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

    private boolean isAnimalEnabled(String resName) {
        if (doLargeGame && LARGE_GAME.contains(resName)) return true;
        if (doLivestock && LIVESTOCK.contains(resName)) return true;
        if (doPredators && PREDATORS.contains(resName)) return true;
        if (doSmallGame && SMALL_GAME.contains(resName)) return true;
        return false;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (java.util.Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.butcherBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        Gob player = ui.gui.map.player();
        if (player != null)
            ui.gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        if (gui.butcherBotThread != null) {
            gui.butcherBotThread.interrupt();
            gui.butcherBotThread = null;
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-butcherBotWindow", this.c);
        super.reqdestroy();
    }
}
