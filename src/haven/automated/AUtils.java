package haven.automated;

import haven.*;
import haven.Composite;
import haven.Window;

import java.awt.*;
import java.util.*;
import java.util.List;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class AUtils {

    private static final HashSet<String> _aggroTargets = new HashSet<String>() {{ // ND: Probably still missing dungeon ants, dungeon bees, dungeon beavers, dungeon bats?
        add("gfx/borka/body");
        add("gfx/kritter/adder/adder");
        add("gfx/kritter/ants/ants");
//        add("gfx/kritter/cattle/cattle"); // ND: Aurochs are handled differently in the method below!
        add("gfx/kritter/badger/badger");
        add("gfx/kritter/bat/bat");
        add("gfx/kritter/bear/bear");
        add("gfx/kritter/bear/polarbear");
        add("gfx/kritter/beaver/beaver");
        add("gfx/kritter/boar/boar");
        add("gfx/kritter/boreworm/boreworm");
        add("gfx/kritter/caveangler/caveangler");
        add("gfx/kritter/cavelouse/cavelouse");
        add("gfx/kritter/chasmconch/chasmconch"); // ND: I even added this one
        add("gfx/kritter/eagleowl/eagleowl");
        add("gfx/kritter/fox/fox");
        add("gfx/kritter/goat/wildgoat");
        add("gfx/kritter/goldeneagle/goldeneagle");
        add("gfx/kritter/greyseal/greyseal");
        add("gfx/kritter/horse/horse");
        add("gfx/kritter/lynx/lynx");
        add("gfx/kritter/mammoth/mammoth");
        add("gfx/kritter/moose/moose");
//        add("gfx/kritter/sheep/sheep"); // ND: Mouflons are handled differently in the method below!
        add("gfx/kritter/nidbane/nidbane");
        add("gfx/kritter/ooze/greenooze");
        add("gfx/kritter/orca/orca");
        add("gfx/kritter/otter/otter");
        add("gfx/kritter/pelican/pelican");
        add("gfx/kritter/rat/caverat");
        add("gfx/kritter/reddeer/reddeer");
        add("gfx/kritter/reindeer/reindeer");
        add("gfx/kritter/roedeer/roedeer");
        add("gfx/kritter/spermwhale/spermwhale");
        add("gfx/kritter/stoat/stoat");
        add("gfx/kritter/swan/swan");
        add("gfx/kritter/troll/troll");
        add("gfx/kritter/walrus/walrus");
        add("gfx/kritter/wolf/wolf");
        add("gfx/kritter/wolverine/wolverine");
        add("gfx/kritter/woodgrouse/woodgrouse-m");
        add("gfx/kritter/garefowl/garefowl");
        add("gfx/kritter/goshawk/goshawk");
        add("gfx/kritter/narwhal/narwhal");
        add("gfx/kritter/crane/crane");
        add("gfx/kritter/woodscorpion/woodscorpion");

        add("gfx/kritter/ants/queenant");
        add("gfx/kritter/ants/royalguardant");
        add("gfx/kritter/ants/warriorant");
        add("gfx/kritter/ants/redants");

        add("gfx/kritter/beaver/beaverking");
        add("gfx/kritter/beaver/oldbeaver");
        add("gfx/kritter/beaver/grizzlybeaver");

        add("gfx/kritter/bees/warriordrone");
        add("gfx/kritter/bees/queenbee");
        add("gfx/kritter/bees/sentinelbee");
        add("gfx/kritter/bees/vulturebee");
        add("gfx/kritter/bees/honeybee");
        add("gfx/kritter/bees/beelarva");
        add("gfx/kritter/wildbees/beeswarm");

        add("gfx/kritter/bat/nightqueen");
        add("gfx/kritter/bat/vampire");
        add("gfx/kritter/bat/bloodstalker");
        add("gfx/kritter/bat/denmother");
        add("gfx/kritter/bat/fatbat");

        add("gfx/kritter/rat/ratking");
        add("gfx/kritter/rat/fatrat");
        add("gfx/kritter/rat/blackrat");

    }
    };
    public static final Set<String> potentialAggroTargets = Collections.unmodifiableSet(_aggroTargets);

    public static HashMap<Long, Gob> getAllAttackableMap(GameUI gui) {
        HashMap<Long, Gob> gobs = new HashMap<>();
        if (gui.map.plgob == -1) {
            return gobs;
        }
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                Resource res = gob.getres();
                if (res != null && res.name != null){
                    if (gob.id != gui.map.plgob) {
                        if (potentialAggroTargets.contains(res.name)){
                            gobs.put(gob.id, gob);
                        } else if (res.name.equals("gfx/kritter/cattle/cattle")) { // ND: Special case for Aurochs
                            for (GAttrib g : gob.attr.values()) {
                                if (g instanceof Drawable) {
                                    if (g instanceof Composite) {
                                        Composite c = (Composite) g;
                                        if (c.comp.cmod.size() > 0) {
                                            for (Composited.MD item : c.comp.cmod) {
                                                if (item.mod.get().basename().equals("aurochs")){
                                                    gobs.put(gob.id, gob);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (res.name.equals("gfx/kritter/sheep/sheep")) { // ND: Special case for Mouflon
                            for (GAttrib g : gob.attr.values()) {
                                if (g instanceof Drawable) {
                                    if (g instanceof Composite) {
                                        Composite c = (Composite) g;
                                        if (c.comp.cmod.size() > 0) {
                                            for (Composited.MD item : c.comp.cmod) {
                                                if (item.mod.get().basename().equals("mouflon")){
                                                    gobs.put(gob.id, gob);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
                } catch (Loading ignored) {}
            }
        }
        return gobs;
    }

    public static HashMap<Long, Gob> getAllAttackablePlayersMap(GameUI gui) {
        HashMap<Long, Gob> gobs = new HashMap<>();
        if (gui.map.plgob == -1) {
            return gobs;
        }
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && res.name != null){
                        if (gob.id != gui.map.plgob) {
                            if (res.name.equals("gfx/borka/body")){
                                gobs.put(gob.id, gob);
                            }
                        }
                    }
                } catch (Loading ignored) {}
            }
        }
        return gobs;
    }

    public static void getGridHeightAvg(GameUI gui){
        try {
            Gob player = gui.map.player();
            if (player == null) return;
            Coord playerCoord = player.rc.floor(tilesz);
            MCache.Grid grid = gui.ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
            float wholeGridHeight = 0;
            float[] quarterHeights = new float[4];
            int gridSize = 100;
            int halfGridSize = gridSize / 2;
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    wholeGridHeight += grid.z[i * gridSize + j];
                    int quarterIndex;
                    if(i < halfGridSize) {
                        quarterIndex = (j < halfGridSize) ? 0 : 1;
                    } else {
                        quarterIndex = (j < halfGridSize) ? 2 : 3;
                    }
                    quarterHeights[quarterIndex] += grid.z[i * gridSize + j];
                }
            }
            String[] quarterNames = {"N-W", "N-E", "S-W", "S-E"};
            StringBuilder message = new StringBuilder("Whole grid average height is: " + wholeGridHeight / 10000 + ", ");
            for (int i = 0; i < 4; i++) {
                message.append("\n").append(quarterNames[i]).append(" quarter average height is: ").append(quarterHeights[i] / 2500).append(", ");
            }
            gui.msg(message.toString(), Color.WHITE);
        } catch (Loading ignored) {}
    }

}
