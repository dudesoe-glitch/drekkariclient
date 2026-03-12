package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class MiningBot extends Window implements Runnable {
    private final GameUI gui;
    public volatile boolean stop;
    private volatile boolean active;
    private Button activeButton;
    private Label statusLabel;
    private Label targetLabel;
    private Coord2d targetPos;
    public boolean settingTarget;

    public MiningBot(GameUI gui) {
        super(UI.scale(250, 120), "Mining Bot");
        this.gui = gui;
        this.stop = false;
        this.active = false;
        this.targetPos = null;
        this.settingTarget = false;

        statusLabel = new Label("Idle");
        add(statusLabel, UI.scale(10, 10));

        Button setTargetButton = new Button(UI.scale(100), "Set Target") {
            @Override
            public void click() {
                settingTarget = true;
                statusLabel.settext("Click a mine wall...");
            }
        };
        add(setTargetButton, UI.scale(10, 35));

        targetLabel = new Label("Target: not set");
        add(targetLabel, UI.scale(10, 58));

        activeButton = new Button(UI.scale(80), "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    if (targetPos == null) {
                        active = false;
                        statusLabel.settext("Set target first!");
                        return;
                    }
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
        add(activeButton, UI.scale(80, 75));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (!active || targetPos == null) {
                    Thread.sleep(500);
                    continue;
                }

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
                    gui.error("Mining Bot: Low on energy, stopping.");
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
                    gui.error("Mining Bot: Inventory full, pausing.");
                    active = false;
                    activeButton.change("Start");
                    statusLabel.settext("Inventory full");
                    Thread.sleep(2000);
                    continue;
                }

                // If currently mining (progress bar active), wait for completion
                if (gui.prog != null) {
                    statusLabel.settext("Mining...");
                    waitForProgressBar(60000);
                    Thread.sleep(500);
                    // Drop cursor items
                    if (gui.vhand != null) {
                        gui.vhand.item.wdgmsg("drop", Coord.z);
                        Thread.sleep(500);
                    }
                    continue;
                }

                // Click the mine wall
                statusLabel.settext("Mining...");
                gui.map.wdgmsg("click", Coord.z, targetPos.floor(posres), 1, 0);
                Thread.sleep(1000);

                // Wait for mining to start
                for (int i = 0; i < 30; i++) {
                    if (gui.prog != null) break;
                    if (stop || !active) break;
                    Thread.sleep(200);
                }

                Thread.sleep(500);
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

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (java.util.Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.miningBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void setTarget(Coord2d mc) {
        targetPos = mc;
        settingTarget = false;
        targetLabel.settext("Target: " + mc.floor().x + ", " + mc.floor().y);
        statusLabel.settext("Target set");
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
        if (gui.miningBotThread != null) {
            gui.miningBotThread.interrupt();
            gui.miningBotThread = null;
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-miningBotWindow", this.c);
        super.reqdestroy();
    }
}
