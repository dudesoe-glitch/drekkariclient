package haven.automated;

import haven.*;

import java.util.*;
import java.util.function.Predicate;

/**
 * Shared game object utility methods for bot automation.
 * All methods are static, null-safe, and handle Loading exceptions gracefully.
 */
public class GobHelper {

    // Gob type resource path prefixes
    public static final String TREE_PREFIX = "gfx/terobjs/trees/";
    public static final String BUSH_PREFIX = "gfx/terobjs/bushes/";
    public static final String HERB_PREFIX = "gfx/terobjs/herbs/";
    public static final String PLANT_PREFIX = "gfx/terobjs/plants/";
    public static final String ANIMAL_PREFIX = "gfx/kritter/";
    public static final String ROCK_PREFIX = "gfx/terobjs/bumlings/";

    /**
     * Get the gob's resource name, or null if not loaded or gob is null.
     */
    public static String getResName(Gob gob) {
        if (gob == null) return null;
        try {
            Resource res = gob.getres();
            if (res != null) {
                return res.name;
            }
        } catch (Loading ignored) {}
        return null;
    }

    /**
     * Check if gob is a living tree (not a stump, log, or old trunk).
     */
    public static boolean isTree(Gob gob) {
        String name = getResName(gob);
        if (name == null) return false;
        return name.startsWith(TREE_PREFIX)
            && !name.endsWith("stump")
            && !name.endsWith("log")
            && !name.endsWith("oldtrunk");
    }

    /**
     * Check if gob is a bush.
     */
    public static boolean isBush(Gob gob) {
        String name = getResName(gob);
        if (name == null) return false;
        return name.startsWith(BUSH_PREFIX);
    }

    /**
     * Check if gob is a harvestable herb (overworld forageable).
     */
    public static boolean isHerb(Gob gob) {
        String name = getResName(gob);
        if (name == null) return false;
        return name.startsWith(HERB_PREFIX);
    }

    /**
     * Check if gob is a crop plant (growing in a field or on a trellis).
     */
    public static boolean isCrop(Gob gob) {
        String name = getResName(gob);
        if (name == null) return false;
        return name.startsWith(PLANT_PREFIX);
    }

    /**
     * Check if gob is an animal (critter/kritter).
     */
    public static boolean isAnimal(Gob gob) {
        String name = getResName(gob);
        if (name == null) return false;
        return name.startsWith(ANIMAL_PREFIX);
    }

    /**
     * Check if gob is a rock/boulder (bumling).
     */
    public static boolean isRock(Gob gob) {
        String name = getResName(gob);
        if (name == null) return false;
        return name.startsWith(ROCK_PREFIX);
    }

    /**
     * Check if a crop or trellis plant is mature (at its maximum growth stage).
     * Uses sprite kind detection and growth stage comparison from mesh layer IDs.
     *
     * @return true if the gob is a mature crop, false otherwise or if state cannot be determined
     */
    public static boolean isMature(Gob gob) {
        if (gob == null) return false;
        try {
            // Must be a growing plant or trellis plant
            if (!Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")) {
                return false;
            }

            // Get max growth stage from mesh layers
            int maxStage = 0;
            for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
                if (layer.id / 10 > maxStage) {
                    maxStage = layer.id / 10;
                }
            }

            // Get current growth stage from drawable data
            Drawable dr = gob.getattr(Drawable.class);
            ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
            if (d == null) return false;

            Message data = d.sdt.clone();
            if (data == null) return false;

            int stage = data.uint8();
            if (stage > maxStage) stage = maxStage;

            return stage == maxStage;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Find the nearest gob matching a predicate, within maxDist of the player.
     * Uses the synchronized gui.map.glob.oc iteration pattern with player null guard.
     *
     * @param gui     the GameUI instance
     * @param maxDist maximum distance from player (in game units), or <= 0 for unlimited
     * @param filter  predicate to test each gob against
     * @return the nearest matching gob, or null
     */
    public static Gob findNearest(GameUI gui, double maxDist, Predicate<Gob> filter) {
        if (gui == null || gui.map == null || gui.map.glob == null) return null;
        Gob player = gui.map.player();
        if (player == null) return null;
        Coord2d playerPos = player.rc;

        Gob closest = null;
        double closestDist = Double.MAX_VALUE;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    if (gob.id == gui.map.plgob) continue;
                    if (filter != null && !filter.test(gob)) continue;

                    double dist = gob.rc.dist(playerPos);
                    if (maxDist > 0 && dist > maxDist) continue;

                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = gob;
                    }
                } catch (Loading | NullPointerException ignored) {}
            }
        }
        return closest;
    }

    /**
     * Find all gobs matching a predicate, within maxDist of the player.
     *
     * @param gui     the GameUI instance
     * @param maxDist maximum distance from player (in game units), or <= 0 for unlimited
     * @param filter  predicate to test each gob against
     * @return list of matching gobs, possibly empty
     */
    public static List<Gob> findAll(GameUI gui, double maxDist, Predicate<Gob> filter) {
        List<Gob> result = new ArrayList<>();
        if (gui == null || gui.map == null || gui.map.glob == null) return result;
        Gob player = gui.map.player();
        if (player == null) return result;
        Coord2d playerPos = player.rc;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    if (gob.id == gui.map.plgob) continue;
                    if (filter != null && !filter.test(gob)) continue;

                    double dist = gob.rc.dist(playerPos);
                    if (maxDist > 0 && dist > maxDist) continue;

                    result.add(gob);
                } catch (Loading | NullPointerException ignored) {}
            }
        }
        return result;
    }

    /**
     * Get the distance from the player to a gob.
     *
     * @return the distance, or Double.MAX_VALUE if player or gob is null
     */
    public static double distToPlayer(GameUI gui, Gob gob) {
        if (gui == null || gob == null || gui.map == null) return Double.MAX_VALUE;
        Gob player = gui.map.player();
        if (player == null) return Double.MAX_VALUE;
        return gob.rc.dist(player.rc);
    }

    /**
     * Check if a gob is a knocked/dead animal.
     * Knocked state is determined by the gob's pose containing "knock", "dead",
     * "waterdead", "banzai", or "carried" (set in Gob.updPose).
     *
     * @return true if the gob has been knocked/killed, false if alive or state unknown
     */
    public static boolean isKnocked(Gob gob) {
        if (gob == null) return false;
        // Gob.knocked is a Boolean (nullable): null = pose not yet loaded,
        // true = knocked/dead, false = alive
        return gob.knocked != null && gob.knocked;
    }
}
