package haven.automated;


import haven.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static haven.OCache.posres;

public class InteractWithCursorNearest implements Runnable {
    private GameUI gui;

    public InteractWithCursorNearest(GameUI gui) {
        this.gui = gui;
    }


    double maxDistance = 12 * 11;
    @Override
    public void run() {
        gui.map.new Hittest(gui.map.currentCursorLocation) {
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                Gob player = gui.map.player();
                if (player == null)
                    return; // player is null, possibly taking a road, don't bother trying to do any of the below
                if (inf != null) {
                    Long gobid = new Long((Integer) inf.clickargs()[1]);
                    Gob clickedGob = gui.map.glob.oc.getgob(gobid);
                    if (clickedGob != null) {
                        Resource res = null;
                        try {
                            res = clickedGob.getres();
                        } catch (Loading l) {
                        }
                        if (res != null) {
                            // Open nearby gates, but not visitor gates
                            boolean isGate = InteractWithNearestObject.gates.contains(res.basename());
                            try {
                                if (isGate) {
                                    for (Gob.Overlay ol : clickedGob.ols) {
                                        String oname = ol.spr.res.name;
                                        if (oname.equals("gfx/fx/eq"))
                                            isGate = false;
                                    }
                                }
                            } catch (NullPointerException ignored) {}
                            if ((isGate && Utils.getprefb("clickNearestObject_NonVisitorGates", true))
                            || ((res.name.startsWith("gfx/terobjs/herbs") || InteractWithNearestObject.otherPickableObjects.contains(res.basename())) && Utils.getprefb("clickNearestObject_Forageables", true))
                            || (Arrays.stream(Config.critterResPaths).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) && Utils.getprefb("clickNearestObject_Critters", true)
                            || (InteractWithNearestObject.caves.contains(res.name) && Utils.getprefb("clickNearestObject_Caves", false))
                            || (InteractWithNearestObject.mines.contains(res.name) && Utils.getprefb("clickNearestObject_MineholesAndLadders", false))) {
                                if (res.name.startsWith("gfx/terobjs/herbs")) FlowerMenu.setNextSelection("Pick"); // ND: Set the flower menu option to "pick" only for these particular ones.
                                if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
                                    gui.map.switchBunnySlippersAndPlateBoots(clickedGob);
                                }
                                gui.map.wdgmsg("click", Coord.z, clickedGob.rc.floor(posres), 3, 0, 0, (int) clickedGob.id, clickedGob.rc.floor(posres), 0, -1);
                                if (gui.interactWithNearestObjectThread != null) {
                                    gui.interactWithNearestObjectThread.interrupt();
                                    gui.interactWithNearestObjectThread = null;
                                }
                                return;
                            }
                        }
                    }
                }
                Gob theObject = null;
                for (Gob gob : Utils.getAllGobs(gui)) {
                    double distFromPlayer = gob.rc.dist(mc);
                    if (gob.id == gui.map.plgob || distFromPlayer >= maxDistance)
                        continue;
                    Resource res = null;
                    try {
                        res = gob.getres();
                    } catch (Loading l) {
                    }
                    if (res != null) {
                        // Open nearby gates, but not visitor gates
                        boolean isGate = InteractWithNearestObject.gates.contains(res.basename());
                        try {
                            if (isGate) {
                                for (Gob.Overlay ol : gob.ols) {
                                    String oname = ol.spr.res.name;
                                    if (oname.equals("gfx/fx/eq"))
                                        isGate = false;
                                }
                            }
                        } catch (NullPointerException ignored) {}
                        if ((isGate && Utils.getprefb("clickNearestObject_NonVisitorGates", true))
                                || ((res.name.startsWith("gfx/terobjs/herbs") || InteractWithNearestObject.otherPickableObjects.contains(res.basename())) && Utils.getprefb("clickNearestObject_Forageables", true))
                                || (Arrays.stream(Config.critterResPaths).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) && Utils.getprefb("clickNearestObject_Critters", true)
                                || (InteractWithNearestObject.caves.contains(res.name) && Utils.getprefb("clickNearestObject_Caves", false))
                                || (InteractWithNearestObject.mines.contains(res.name) && Utils.getprefb("clickNearestObject_MineholesAndLadders", false))) {
                            if (distFromPlayer < maxDistance && (theObject == null || distFromPlayer < theObject.rc.dist(mc))) {
                                theObject = gob;
                                if (res.name.startsWith("gfx/terobjs/herbs")) FlowerMenu.setNextSelection("Pick"); // ND: Set the flower menu option to "pick" only for these particular ones.
                            }
                        }
                    }
                }
                if (theObject == null)
                    return;
                if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
                    gui.map.switchBunnySlippersAndPlateBoots(theObject);
                }
                gui.map.wdgmsg("click", Coord.z, theObject.rc.floor(posres), 3, 0, 0, (int) theObject.id, theObject.rc.floor(posres), 0, -1);
                if (gui.interactWithNearestObjectThread != null) {
                    gui.interactWithNearestObjectThread.interrupt();
                    gui.interactWithNearestObjectThread = null;
                }
            }
        }.run();
    }
}
