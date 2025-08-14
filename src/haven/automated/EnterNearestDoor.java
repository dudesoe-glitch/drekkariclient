package haven.automated;

import haven.*;
import java.util.*;
import static haven.OCache.posres;

public class EnterNearestDoor implements Runnable {
    private final GameUI gui;

    public EnterNearestDoor(GameUI gui) { this.gui = gui; }

    @Override
    public void run() {
        Gob player = gui.map.player();
        if (player == null) return;
        Coord2d plc = player.rc;

        Target fromTable = nearestFromBuildings(40 * 11);
        Gob suffixGob = nearestBySuffix(40 * 11);
        Target fromSuffix = (suffixGob == null) ? null : new Target(suffixGob.rc, Coord.z, suffixGob.id, -1);

        Target best = minByDist(plc, fromTable, fromSuffix);
        if (best == null) return;

        try {
            gui.map.wdgmsg("click", best.s, best.c.floor(posres), 3, 0, 0,
                    (int) best.g, best.c.floor(posres), 0, best.m);
        } catch (Exception ignored) {}
    }

    private Target nearestFromBuildings(double r) {
        if (r <= 0) r = 1024.0;
        Coord2d plc = gui.map.player().rc;
        Target best = null;
        for (Gob gob : Utils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();
                if (res == null) continue;
                List<Door> doors = BUILDINGS.get(res.name);
                if (doors == null) continue;
                for (Door d : doors) {
                    Coord2d c = gob.rc.add(d.rc.rotate(gob.a));
                    if (c.dist(plc) < r && (best == null || c.dist(plc) < best.c.dist(plc)))
                        best = new Target(c, Coord.z, gob.id, d.id);
                }
            } catch (Loading ignored) {}
        }
        return best;
    }

    private Gob nearestBySuffix(double r) {
        if (r <= 0) r = 1024.0;
        Coord2d plc = gui.map.player().rc;
        Gob best = null;
        double bestd = Double.MAX_VALUE;

        for (Gob gob : Utils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();
                if (res == null) continue;
                if ("gfx/terobjs/arch/greathall-door".equals(res.name)) continue;
                boolean match = false;
                for (String s : SUFFIXES) { if (res.name.endsWith(s)) { match = true; break; } }
                if (!match) continue;
                double d = gob.rc.dist(plc);
                if (d < r && d < bestd) { best = gob; bestd = d; }
            } catch (Loading ignored) {}
        }
        return best;
    }

    private static Target minByDist(Coord2d plc, Target... ts) {
        Target best = null;
        for (Target t : ts) if (t != null && (best == null || t.c.dist(plc) < best.c.dist(plc))) best = t;
        return best;
    }

    public static class Door {
        public final Coord2d rc;
        public final int id;
        public Door(Coord2d rc, int id) { this.rc = rc; this.id = id; }
    }

    public static class Target {
        public final Coord2d c;
        public final Coord s;
        public final long g;
        public final int m;
        public Target(Coord2d c, Coord s, long g, int m) { this.c = c; this.s = s; this.g = g; this.m = m; }
    }

    private static final List<String> SUFFIXES = Arrays.asList(
            "-door","ladder","upstairs","downstairs","cellarstairs","cavein","caveout"
    );

    private static final Map<String, List<Door>> BUILDINGS = new HashMap<>();
    static {
        BUILDINGS.put("gfx/terobjs/arch/logcabin", Arrays.asList(new Door(new Coord2d(22, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/timberhouse", Arrays.asList(new Door(new Coord2d(33, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/stonestead", Arrays.asList(new Door(new Coord2d(44, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/stonemansion", Arrays.asList(new Door(new Coord2d(48, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/greathall", Arrays.asList(
                new Door(new Coord2d(77, -28), 18),
                new Door(new Coord2d(77, 0), 17),
                new Door(new Coord2d(77, 28), 16)
        ));
        BUILDINGS.put("gfx/terobjs/arch/stonetower", Arrays.asList(new Door(new Coord2d(36, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/windmill", Arrays.asList(new Door(new Coord2d(0, 28), 16)));
        BUILDINGS.put("gfx/terobjs/arch/greathall-door", Arrays.asList(
                new Door(new Coord2d(0, -30), 18),
                new Door(new Coord2d(0, 0), 17),
                new Door(new Coord2d(0, 30), 16)
        ));
    }
}
