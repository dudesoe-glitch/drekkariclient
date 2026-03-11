package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ClayDiggingBot extends Window implements Runnable {
    private final GameUI gui;
    public boolean stop;
    private boolean active;
    private Button activeButton;
    private Label statusLabel;

    public ClayDiggingBot(GameUI gui) {
        super(UI.scale(220, 80), "Clay Digging Bot");
        this.gui = gui;
        this.stop = false;
        this.active = false;

        statusLabel = new Label("Idle");
        add(statusLabel, UI.scale(10, 10));

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
        add(activeButton, UI.scale(65, 45));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (active) {
                    // HP check
                    if (gui.getmeters("hp").get(1).a < 0.02) {
                        statusLabel.settext("Low HP! Hearthing...");
                        gui.act("travel", "hearth");
                        Thread.sleep(8000);
                        continue;
                    }
                    // Energy check
                    if (gui.getmeter("nrj", 0).a < 0.02) {
                        gui.error("Clay Digging Bot: Low on energy, stopping.");
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

                    // Check if already working (progress bar active)
                    if (gui.prog != null) {
                        statusLabel.settext("Digging...");
                        waitForProgressBar(30000);
                        Thread.sleep(500);
                        continue;
                    }

                    // Find nearest clay patch
                    Gob clay = findNearestClay();
                    if (clay == null) {
                        statusLabel.settext("No clay patches found");
                        Thread.sleep(3000);
                        continue;
                    }

                    // Clear hand if holding something
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(500);
                    }

                    statusLabel.settext("Walking to clay...");

                    // Pathfind to clay patch
                    gui.map.pfLeftClick(clay.rc.floor().add(2, 0), null);
                    if (!AUtils.waitPf(gui)) {
                        AUtils.unstuck(gui);
                        Thread.sleep(1000);
                        continue;
                    }

                    // Check range
                    if (clay.rc.dist(gui.map.player().rc) > 11 * 5) {
                        statusLabel.settext("Too far, retrying...");
                        Thread.sleep(1000);
                        continue;
                    }

                    // Dig: set flower menu selection then right-click
                    statusLabel.settext("Digging clay...");
                    FlowerMenu.setNextSelection("Dig");
                    gui.map.wdgmsg("click", Coord.z, clay.rc.floor(posres), 3, 0, 0,
                        (int) clay.id, clay.rc.floor(posres), 0, -1);

                    // Wait a moment for the flower menu to appear and auto-select
                    Thread.sleep(1000);

                    // Wait for progress bar (digging action)
                    waitForProgressBar(30000);

                    // Clear hand if digging dropped something on cursor
                    Thread.sleep(500);
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(500);
                    }

                } else {
                    Thread.sleep(1000);
                    continue;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void waitForProgressBar(int timeout) throws InterruptedException {
        int elapsed = 0;
        int hz = 100;
        while (gui.prog != null && gui.prog.prog != -1 && elapsed < timeout) {
            elapsed += hz;
            Thread.sleep(hz);
        }
    }

    private Gob findNearestClay() {
        Gob closest = null;
        Coord2d playerPos = gui.map.player().rc;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res == null) continue;

                    if (!res.name.contains("gfx/terobjs/clay")) continue;

                    if (closest == null || gob.rc.dist(playerPos) < closest.rc.dist(playerPos)) {
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
            gui.clayDiggingBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        if (gui.clayDiggingBotThread != null) {
            gui.clayDiggingBotThread.interrupt();
            gui.clayDiggingBotThread = null;
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-clayDiggingBotWindow", this.c);
        super.reqdestroy();
    }
}
