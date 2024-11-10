package haven.automated;


import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class InteractWithNearestObject implements Runnable {
    private GameUI gui;

    public InteractWithNearestObject(GameUI gui) {
        this.gui = gui;
    }

    public final static HashSet<String> gates = new HashSet<String>(Arrays.asList(
            "brickwallgate",
            "brickbiggate",
            "drystonewallgate",
            "drystonewallbiggate",
            "palisadegate",
            "palisadebiggate",
            "polegate",
            "polebiggate"
    ));

    public final static Set<String> otherPickableObjects = new HashSet<String>(Arrays.asList( // ND: Pretty much any ground item can be added here
            "adder",
            "arrow",
            "bat",
            "precioussnowflake",
            "truffle-black0",
            "truffle-black1",
            "truffle-black2",
            "truffle-black3",
            "truffle-white0",
            "truffle-white1",
            "truffle-white2",
            "truffle-white3",
            "gemstone"
    ));

    public final static HashSet<String> mines = new HashSet<String>(Arrays.asList(
            "gfx/terobjs/ladder",
            "gfx/terobjs/minehole"
    ));

    public final static HashSet<String> caves = new HashSet<String>(Arrays.asList(
            "gfx/tiles/ridges/cavein",
            "gfx/tiles/ridges/caveout"
    ));

    double maxDistance = 12 * 11;
    @Override
    public void run() {
        Gob theObject = null;
        Gob player = gui.map.player();
        Coord2d plc = player.rc;
        if (player == null)
            return; // player is null, possibly taking a road, don't bother trying to do any of the below
        for (Gob gob : Utils.getAllGobs(gui)) {
            double distFromPlayer = gob.rc.dist(plc);
            if (gob.id == gui.map.plgob || distFromPlayer >= maxDistance)
                continue;
            Resource res = null;
            try {
                res = gob.getres();
            } catch (Loading l) {
            }
            if (res != null) {
                // Open nearby gates, but not visitor gates
                boolean isGate = gates.contains(res.basename());
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
                || ((res.name.startsWith("gfx/terobjs/herbs") || otherPickableObjects.contains(res.basename())) && Utils.getprefb("clickNearestObject_Forageables", true))
                || (Arrays.stream(Config.critterResPaths).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) && Utils.getprefb("clickNearestObject_Critters", true)
                || (caves.contains(res.name) && Utils.getprefb("clickNearestObject_Caves", false))
                || (mines.contains(res.name) && Utils.getprefb("clickNearestObject_MineholesAndLadders", false))) {
                    if (distFromPlayer < maxDistance && (theObject == null || distFromPlayer < theObject.rc.dist(plc))) {
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
}
