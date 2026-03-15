package haven.automated.pathfinder;


import haven.*;
import haven.automated.helpers.HitBoxes;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static haven.OCache.posres;

public class Pathfinder implements Runnable {
    private OCache oc;
    private MCache map;
    private MapView mv;
    private Coord dest;
    public volatile boolean terminate = false;
    public volatile boolean moveinterupted = false;
    private int meshid;
    private int clickb;
    private Gob gob;
    private String action;
    public Coord mc;
    private int modflags;
    private Coord clickMc; // Original click position for gob interactions (e.g., door on a house)
    private int interruptedRetries = 8;
    private static final int RESPONSE_TIMEOUT = 800;
    private long avgOverrun = 0;
    public final List<Coord2d> pathWaypoints = new CopyOnWriteArrayList<>();

    public Pathfinder(MapView mv, Coord dest, String action) {
        this.dest = dest;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    public Pathfinder(MapView mv, Coord dest, Gob gob, int meshid, int clickb, int modflags, String action) {
        this.dest = dest;
        this.meshid = meshid;
        this.clickb = clickb;
        this.gob = gob;
        this.modflags = modflags;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    public Pathfinder(MapView mv, Coord dest, Gob gob, Coord2d clickPos, int meshid, int clickb, int modflags, String action) {
        this(mv, dest, gob, meshid, clickb, modflags, action);
        if (clickPos != null)
            this.clickMc = clickPos.floor(posres);
    }

    private final Set<PFListener> listeners = new CopyOnWriteArraySet<PFListener>();
    public final void addListener(final PFListener listener) {
        listeners.add(listener);
    }

    public final void removeListener(final PFListener listener) {
        listeners.remove(listener);
    }

    private final void notifyListeners() {
        for (PFListener listener : listeners) {
            listener.pfDone(this);
        }
    }

    @Override
    public void run() {
        try {
            do {
                moveinterupted = false;
                Gob player = mv.player();
                if (player == null) { terminate = true; break; }
                pathfind(player.rc.floor());
            } while (moveinterupted && !terminate);
        } catch (Loading e) {
            System.err.println("Pathfinder: map/resource not loaded, aborting. " + e.getMessage());
            terminate = true;
        } catch (Exception e) {
            System.err.println("Pathfinder error: " + e.getMessage());
            e.printStackTrace();
            terminate = true;
        }
        notifyListeners();
    }

    public void pathfind(Coord src) {
        long starttotal = System.nanoTime();
        Gob player = mv.player();
        if (player == null) {
            terminate = true;
            return;
        }

        // Adjust hitbox for mounted players (horses have larger collision)
        // or players carrying objects (wider effective collision)
        if (player.occupiedGobID != null) {
            Map.setPlayerBBox(4); // slightly wider hitbox for horse/carry
        } else {
            Map.setPlayerBBox(3); // default player hitbox
        }

        // Collect gob IDs that follow the player (carried objects, passengers)
        // so we can skip their hitboxes during pathfinding
        long plgobId = player.id;

        Map m = new Map(src, dest, map);

        long start = System.nanoTime();
        synchronized (oc) {
            for (Gob gob : oc) {
                if (gob.isPlgob(this.mv.ui.gui))
                    continue;
                if (this.gob != null && this.gob.id == gob.id)
                    continue;
                // Skip the horse/mount gob when player is mounted — otherwise
                // the mount's hitbox blocks the player's own starting position
                if (player.occupiedGobID != null && gob.id == player.occupiedGobID)
                    continue;
                // Skip gobs following the player (carried objects) — their hitbox
                // is at the player's position and would block pathfinding
                Moving moving = gob.getattr(Moving.class);
                if (moving instanceof Following && ((Following) moving).tgt == plgobId)
                    continue;
                Resource gobRes = null;
                try { gobRes = gob.getres(); } catch (Loading ignored) {}
                if (gobRes != null && isInsideBoundBox(gob.rc.floor(), gob.a, gobRes.name, player.rc.floor())) {
                    HitBoxes.CollisionBoxSecondary[] collisionBoxes = HitBoxes.collisionBoxMap.get(gobRes.name);
                    if (collisionBoxes != null) {
                        for (HitBoxes.CollisionBoxSecondary collisionBox : collisionBoxes) {
                            if (collisionBox.hitAble && collisionBox.coords.length > 2) {
                                double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                                double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                                for (Coord2d coord : collisionBox.coords) {
                                    minX = Math.min(minX, coord.x);
                                    minY = Math.min(minY, coord.y);
                                    maxX = Math.max(maxX, coord.x);
                                    maxY = Math.max(maxY, coord.y);
                                }
                                Coord2d topLeft = new Coord2d(minX, minY);
                                Coord2d bottomRight = new Coord2d(maxX, maxY);
                                m.excludeGob(topLeft.floor(), bottomRight.floor(), gob);
                            }
                        }
                    }
                }
                m.analyzeGobHitBoxes(gob);
            }
        }

        if (m.isOriginBlocked()) {
            Pair<Integer, Integer> freeloc = m.getFreeLocation();
            if (freeloc == null) {
                terminate = true;
                m.dbgdump();
                return;
            }
            mc = new Coord2d(src.x + freeloc.a - Map.origin, src.y + freeloc.b - Map.origin).floor(posres);
            mv.wdgmsg("click", Coord.z, mc, 1, 0);
            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {}
            moveinterupted = true;
            m.dbgdump();
            return;
        }

        if (this.gob != null) {
            Resource targetRes = null;
            try { targetRes = this.gob.getres(); } catch (Loading ignored) {}
            if (targetRes != null) {
                HitBoxes.CollisionBoxSecondary[] collisionBoxes = HitBoxes.collisionBoxMap.get(targetRes.name);
                if (collisionBoxes != null) {
                    for (HitBoxes.CollisionBoxSecondary collisionBox : collisionBoxes) {
                        if (collisionBox.hitAble && collisionBox.coords.length > 2) {
                            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                            for (Coord2d coord : collisionBox.coords) {
                                minX = Math.min(minX, coord.x);
                                minY = Math.min(minY, coord.y);
                                maxX = Math.max(maxX, coord.x);
                                maxY = Math.max(maxY, coord.y);
                            }
                            Coord2d topLeft = new Coord2d(minX, minY);
                            Coord2d bottomRight = new Coord2d(maxX, maxY);
                            m.excludeGob(topLeft.floor(), bottomRight.floor(), this.gob);
                        }
                    }
                }
            }
        }

        if (Map.DEBUG_TIMINGS)
            System.out.println("      Gobs Processing: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        Iterable<Edge> path = m.main();

        if (Map.DEBUG_TIMINGS)
            System.out.println("--------------- Total: " + (double) (System.nanoTime() - starttotal) / 1000000.0 + " ms.");

        m.dbgdump();

        pathWaypoints.clear();
        pathWaypoints.add(new Coord2d(player.rc.x, player.rc.y));
        for (Edge e : path) {
            pathWaypoints.add(new Coord2d(src.x + e.dest.x - Map.origin, src.y + e.dest.y - Map.origin));
        }

        Iterator<Edge> it = path.iterator();
        boolean firstSegment = true;
        while (it.hasNext() && !moveinterupted && !terminate) {
            Edge e = it.next();
            mc = new Coord2d(src.x + e.dest.x - Map.origin, src.y + e.dest.y - Map.origin).floor(posres);
            boolean isLastSegment = !it.hasNext();

            if (action != null && isLastSegment)
                mv.ui.gui.act(action);

            if (gob != null && isLastSegment) {
                Coord finalMc = (clickMc != null) ? clickMc : mc;
                mv.wdgmsg("click", Coord.z, finalMc, clickb, modflags, 0, (int) gob.id, gob.rc.floor(posres), 0, meshid);
            }
            else
                mv.wdgmsg("click", Coord.z, mc, 1, 0);

            // Only wait for movement to start on the first segment.
            // Subsequent segments are pre-clicked while still moving — no need to wait.
            if (firstSegment) {
                long moveWaitStart = System.currentTimeMillis();
                while (!player.isMoving() && !terminate) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e1) {
                        return;
                    }
                    if (System.currentTimeMillis() - moveWaitStart > RESPONSE_TIMEOUT)
                        return;
                }
                firstSegment = false;
            }

            Coord2d destWorld = mc.mul(posres);
            long segmentStart = System.currentTimeMillis();
            long estimate = estimateTravelTimeWorld(player.rc, destWorld, player);

            if (isLastSegment) {
                // Last segment: precise arrival — wait until fully stopped
                long lead = Math.min(50, (long) (estimate * 0.05));
                long wait = Math.max(0, estimate - lead - avgOverrun);
                if (wait > 0) {
                    try { Thread.sleep(wait); } catch (InterruptedException e1) { return; }
                }
                while (!moveinterupted && !terminate) {
                    if (!player.isMoving()) break;
                    try { Thread.sleep(25); } catch (InterruptedException e1) { return; }
                }
                long actual = System.currentTimeMillis() - segmentStart;
                long overrun = actual - estimate;
                avgOverrun = (avgOverrun + overrun) / 2;
            } else {
                // Intermediate segment: pre-click next waypoint 30% before arriving
                // This creates smooth chained movement instead of stop-and-go
                long lead = Math.max(150, (long) (estimate * 0.30));
                long wait = Math.max(0, estimate - lead);
                if (wait > 0) {
                    try { Thread.sleep(wait); } catch (InterruptedException e1) { return; }
                }
                // Don't wait for stop — proceed directly to click next waypoint
            }

            if (pathWaypoints.size() > 1) {
                pathWaypoints.remove(1);
                pathWaypoints.set(0, new Coord2d(player.rc.x, player.rc.y));
            }

            if (moveinterupted) {
                interruptedRetries--;
                if (interruptedRetries == 0)
                    terminate = true;
                m.dbgdump();
                return;
            }
        }
        terminate = true;
    }

    private long estimateTravelTimeWorld(Coord2d curPos, Coord2d destPos, Gob player) {
        LinMove lm = player.getLinMove();
        double speed = (lm != null) ? lm.getv() : 0; // world units per second
        if (speed <= 0) {
            return RESPONSE_TIMEOUT;
        }
        double dist = curPos.dist(destPos);
        return (long)((dist / speed) * 1000.0);
    }

    public static boolean isInsideBoundBox(Coord gobRc, double gobA, String resName, Coord point) {
        if (HitBoxes.collisionBoxMap.get(resName) != null) {
            HitBoxes.CollisionBoxSecondary[] collisionBoxes = HitBoxes.collisionBoxMap.get(resName);
            for (HitBoxes.CollisionBoxSecondary collisionBox : collisionBoxes) {
                if (collisionBox.hitAble) {
                    if (collisionBox.coords.length > 3) {
                        double minX = Double.MAX_VALUE;
                        double minY = Double.MAX_VALUE;
                        double maxX = -Double.MAX_VALUE;
                        double maxY = -Double.MAX_VALUE;

                        for (Coord2d coord : collisionBox.coords) {
                            minX = Math.min(minX, coord.x);
                            minY = Math.min(minY, coord.y);
                            maxX = Math.max(maxX, coord.x);
                            maxY = Math.max(maxY, coord.y);
                        }
                        Coord2d topLeft = new Coord2d(minX, minY);
                        Coord2d bottomRight = new Coord2d(maxX, maxY);

                        final Coordf relative = new Coordf(point.sub(gobRc)).rotate(-gobA);
                        if (relative.x >= topLeft.x && relative.x <= bottomRight.x &&
                                relative.y >= topLeft.y && relative.y <= bottomRight.y) {
                            return true;
                        }

                    }
                    if (collisionBox.coords.length == 3) {
                        double minX = Double.MAX_VALUE;
                        double minY = Double.MAX_VALUE;
                        double maxX = -Double.MAX_VALUE;
                        double maxY = -Double.MAX_VALUE;

                        for (Coord2d coord : collisionBox.coords) {
                            if (coord.x < minX) {
                                minX = coord.x;
                            }
                            if (coord.y < minY) {
                                minY = coord.y;
                            }
                            if (coord.x > maxX) {
                                maxX = coord.x;
                            }
                            if (coord.y > maxY) {
                                maxY = coord.y;
                            }
                        }
                        Coord2d topLeft = new Coord2d(minX, minY);
                        Coord2d bottomRight = new Coord2d(maxX, maxY);
                        final Coordf relative = new Coordf(point.sub(gobRc)).rotate(-gobA);
                        if (relative.x >= topLeft.x && relative.x <= bottomRight.x &&
                                relative.y >= topLeft.y && relative.y <= bottomRight.y) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
