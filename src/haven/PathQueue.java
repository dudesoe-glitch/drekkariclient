package haven;

import static haven.OCache.posres;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Shift-click waypoint queue for map movement.
 * Allows queuing multiple destinations by shift-clicking on the map.
 * When the player reaches each waypoint, the next one is automatically started.
 *
 * Adapted from EnderWiggin's hafen-client PathQueue concept.
 */
public class PathQueue {
    private final MapView map;
    private final Deque<Coord2d> queue = new ConcurrentLinkedDeque<>();
    private volatile boolean enabled = true;

    /* Passenger seat bone names — passengers can't steer */
    private static final Set<String> PASSENGER_SEATS = Set.of(
        "Deck1", "Deck2", "Deck3",     // snekkja / knarr passenger seats
        "Seat1", "Seat2",              // rowboat / wagon passenger seats
        "Port", "Starboard"            // spark seats
    );

    public PathQueue(MapView map) {
        this.map = map;
    }

    /** Add a waypoint to the end of the queue. Returns true if movement was started. */
    public boolean add(Coord2d dest) {
        if (!enabled) return false;
        boolean wasEmpty = queue.isEmpty();
        queue.addLast(dest);
        if (wasEmpty) {
            walkTo(dest);
            return true;
        }
        return false;
    }

    /** Replace the queue with a single destination. */
    public void start(Coord2d dest) {
        queue.clear();
        queue.addLast(dest);
        walkTo(dest);
    }

    /** Clear the queue. */
    public void clear() {
        queue.clear();
    }

    /** Get current queue size. */
    public int size() {
        return queue.size();
    }

    /** Check if queue is empty. */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Called when the player's Moving attribute changes.
     * When the player stops moving (to == null), advance to the next waypoint.
     * When the player starts Following or Homing, clear the queue (interacting with something).
     */
    public void movementChange(GAttrib from, GAttrib to) {
        if (to == null && from instanceof Moving) {
            // Player stopped moving — advance to next waypoint
            queue.pollFirst(); // Remove the waypoint we just reached
            Coord2d next = queue.peekFirst();
            if (next != null) {
                walkTo(next);
            }
        } else if (to instanceof Homing || to instanceof Following) {
            // Player is interacting with something or following — clear queue
            if (to instanceof Following) {
                Following f = (Following) to;
                if (isPassenger(f)) {
                    // Player is a passenger, can't steer
                    clear();
                    return;
                }
            }
            clear();
        }
    }

    /** Check if the Following attachment is a passenger seat. */
    private boolean isPassenger(Following f) {
        return f.xfname != null && PASSENGER_SEATS.contains(f.xfname);
    }

    /** Send click message to walk to a map coordinate. */
    private void walkTo(Coord2d dest) {
        map.wdgmsg("click", Coord.z, dest.floor(posres), 1, 0);
    }

    /** Enable or disable path queuing. Clears queue when disabled. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get line segments from player to each queued waypoint.
     * Returns pairs of (from, to) coordinates for rendering on the map/minimap.
     */
    public List<Coord2d[]> getSegments() {
        Gob player = map.player();
        if (player == null || queue.isEmpty()) return Collections.emptyList();

        List<Coord2d[]> segments = new ArrayList<>();
        Coord2d prev = player.rc;
        for (Coord2d wp : queue) {
            segments.add(new Coord2d[]{prev, wp});
            prev = wp;
        }
        return segments;
    }
}
