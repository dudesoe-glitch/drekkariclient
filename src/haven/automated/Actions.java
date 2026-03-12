package haven.automated;

import haven.*;

import java.util.Optional;
import java.util.Random;

import static haven.OCache.posres;

public class Actions {

    public static void attackGob(GameUI gui, Gob gob) {
        if (gob != null && gui != null && gui.map != null) {
            gui.act("aggro");
            gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
            rightClick(gui);
        }
    }

    public static void rightClick(GameUI gui) {
        if (gui == null || gui.map == null) return;
        Gob player = gui.map.player();
        if (player == null) return;
        gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 3, 0);
    }

    public static void clearhand(GameUI gui) {
        if (gui.vhand != null) {
            gui.vhand.item.wdgmsg("drop", Coord.z);
        }
        rightClick(gui);
    }

    public static boolean waitForEmptyHand(final GameUI gui, final int timeout, final String error) throws InterruptedException {
        int t = 0;
        while (gui.vhand != null) {
            t += 5;
            if (t >= timeout) {
                gui.error(error);
                return false;
            }
            try {
                Thread.sleep(5L);
            }
            catch (InterruptedException ie) {
                throw ie;
            }
        }
        return true;
    }

    public static boolean waitForOccupiedHand(final GameUI gui, final int timeout, final String error) throws InterruptedException {
        int t = 0;
        while (gui.vhand == null) {
            t += 5;
            if (t >= timeout) {
                gui.error(error);
                return false;
            }
            try {
                Thread.sleep(5L);
            }
            catch (InterruptedException ie) {
                throw ie;
            }
        }
        return true;
    }

    public static boolean waitPf(GameUI gui) throws InterruptedException {
        if(gui.map.pfthread == null){
            return false;
        }
        int time = 0;
        boolean moved = false;
        Thread.sleep(300);
        Gob player = gui.map.player();
        if (player == null) return false;
        while (gui.map.pfthread.isAlive() || player.getv() > 0) {
            time += 70;
            Thread.sleep(70);
            player = gui.map.player();
            if (player == null) return false;
            if (player.getv() > 0) {
                time = 0;
                moved = true;
            }
            if (time > 2000 && moved == false) {
                System.out.println("TRYING UNSTUCK");
                return false;
            } else if (time > 20000) {
                return false;
            }
        }
        return true;
    }

    public static void waitProgBar(GameUI gui) throws InterruptedException {
        while (gui.prog != null && gui.prog.prog >= 0) {
            Thread.sleep(40);
        }
    }

    public static void drinkTillFull(GameUI gui, double threshold, double stoplevel) throws InterruptedException {
        while (gui.drink(threshold)) {
            Thread.sleep(490);
            do {
                Thread.sleep(10);
                IMeter.Meter stam = gui.getmeter("stam", 0);
                if (stam.a >= stoplevel)
                    break;
            } while (gui.prog != null && gui.prog.prog >= 0);
        }
    }

    public static void unstuck(GameUI gui) throws InterruptedException {
        Gob player = gui.map.player();
        if (player == null) return;
        Coord2d pc = player.rc;
        Random r = new Random();
        for (int i = 0; i < 5; i++) {
            int xAdd = r.nextInt(500) - 250;
            int yAdd = r.nextInt(500) - 250;
            gui.map.wdgmsg("click", Coord.z, pc.floor(posres).add(xAdd, yAdd), 1, 0);
            Thread.sleep(100);
        }
    }

    public static void rightClickShiftCtrl(GameUI gui, Gob gob) {
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 3, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static void rightClickGobAndSelectOption(GameUI gui, Gob gob, int index) {
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
        gui.ui.rcvr.rcvmsg(gui.ui.lastWidgetID+1, "cl", index, gui.ui.modflags());
    }

    public static boolean rightClickGobOverlayWithItem(GameUI gui, Gob gob, String overlayResName) {
        if (gob != null && !gob.ols.isEmpty()) {
            Optional<Gob.Overlay> foundOverlay = gob.ols.stream()
                    .filter(ol -> ol != null && ol.spr != null && ol.spr.res != null && overlayResName.equals(ol.spr.res.name))
                    .map(ol -> (Gob.Overlay) ol)
                    .findFirst();

            if (foundOverlay.isPresent()) {
                Gob.Overlay gobOverlay = foundOverlay.get();
                gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 0, 1, (int) gob.id, gob.rc.floor(posres), gobOverlay.id, -1);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean rightClickGobOverlayAndSelectOption(GameUI gui, Gob gob, int index, String overlayResName) {
        if (gob != null && !gob.ols.isEmpty()) {
            Optional<Gob.Overlay> foundOverlay = gob.ols.stream()
                    .filter(ol -> ol != null && ol.spr != null && ol.spr.res != null && overlayResName.equals(ol.spr.res.name))
                    .map(ol -> (Gob.Overlay) ol)
                    .findFirst();

            if (foundOverlay.isPresent()) {
                Gob.Overlay gobOverlay = foundOverlay.get();
                gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 1, (int) gob.id, gob.rc.floor(posres), gobOverlay.id, -1);;
                gui.ui.rcvr.rcvmsg(gui.ui.lastWidgetID+1, "cl", index, gui.ui.modflags());
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static void clickWItemAndSelectOption(GameUI gui, WItem wItem, int index) {
        wItem.item.wdgmsg("iact", Coord.z, gui.ui.modflags());
        gui.ui.rcvr.rcvmsg(gui.ui.lastWidgetID+1, "cl", index, gui.ui.modflags());
    }

    /**
     * Toggle the nearest gate (open/close) within ~3 tiles.
     * Works with all gate types (palisade, brick, pole, drystone).
     */
    public static void toggleNearestGate(GameUI gui) {
        if (gui == null || gui.map == null) return;
        Gob player = gui.map.player();
        if (player == null) return;

        double maxDist = 36.0; // ~3 tiles
        Gob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    if (gob.getres() == null) continue;
                    String basename = gob.getres().basename();
                    if (InteractWithNearestObject.smallGates.contains(basename) ||
                        InteractWithNearestObject.reinforcedGates.contains(basename)) {
                        double dist = gob.rc.dist(player.rc);
                        if (dist < maxDist && dist < nearestDist) {
                            nearestDist = dist;
                            nearest = gob;
                        }
                    }
                } catch (Loading ignored) {}
            }
        }

        if (nearest != null) {
            FlowerMenu.setNextSelection("Open");
            gui.map.wdgmsg("click", Coord.z, nearest.rc.floor(posres), 3, 0, 0, (int) nearest.id, nearest.rc.floor(posres), 0, -1);
        }
    }

    /**
     * Pick up the nearest item on the ground within ~2 tiles.
     */
    public static void pickupNearest(GameUI gui) {
        if (gui == null || gui.map == null) return;
        Gob player = gui.map.player();
        if (player == null) return;

        double maxDist = 24.0; // ~2 tiles
        Gob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    if (gob.getres() == null) continue;
                    String name = gob.getres().name;
                    // Ground items are in gfx/terobjs/items/
                    if (name.contains("gfx/terobjs/items/")) {
                        double dist = gob.rc.dist(player.rc);
                        if (dist < maxDist && dist < nearestDist) {
                            nearestDist = dist;
                            nearest = gob;
                        }
                    }
                } catch (Loading ignored) {}
            }
        }

        if (nearest != null) {
            gui.map.wdgmsg("click", Coord.z, nearest.rc.floor(posres), 3, 0, 0, (int) nearest.id, nearest.rc.floor(posres), 0, -1);
        }
    }
}
