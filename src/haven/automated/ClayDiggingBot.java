package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ClayDiggingBot extends BotBase {
	private final Set<Long> blacklisted = new HashSet<>();

	public ClayDiggingBot(GameUI gui) {
		super(gui, UI.scale(220, 80), "Clay Digging Bot");

		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, 10));

		activeButton = new Button(UI.scale(80), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					blacklisted.clear();
					this.change("Stop");
					statusLabel.settext("Running...");
				} else {
					idlePlayer();
					this.change("Start");
					statusLabel.settext("Stopped");
				}
			}
		};
		add(activeButton, UI.scale(65, 45));
	}

	@Override
	protected void tick() throws InterruptedException {
		if (!checkVitals()) return;

		if (gui.prog != null) {
			setStatus("Digging...");
			waitForProgressBar(30000);
			return;
		}

		Gob clay = findNearestClay();
		if (clay == null) {
			if (!blacklisted.isEmpty()) {
				blacklisted.clear();
				clay = findNearestClay();
			}
			if (clay == null) {
				setStatus("No clay patches found");
				deactivate();
				return;
			}
		}

		// Capture vhand to local (TOCTOU fix)
		WItem vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("transfer", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}

		setStatus("Walking to clay...");
		gui.map.pfLeftClick(clay.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			blacklisted.add(clay.id);
			Actions.unstuck(gui);
			return;
		}
		if (stop) return;

		Gob player = gui.map.player();
		if (player == null) return;
		if (clay.rc.dist(player.rc) > 11 * 5) {
			blacklisted.add(clay.id);
			setStatus("Too far, skipping...");
			return;
		}

		// Verify clay still exists after pathfinding
		if (gui.map.glob.oc.getgob(clay.id) == null) {
			setStatus("Clay gone, moving on...");
			return;
		}

		setStatus("Digging clay...");
		FlowerMenu.setNextSelection("Dig");
		gui.map.wdgmsg("click", Coord.z, clay.rc.floor(posres), 3, 0, 0,
			(int) clay.id, clay.rc.floor(posres), 0, -1);
		Thread.sleep(50);
		waitForProgressBar(30000);

		// Clear stale FlowerMenu selection
		FlowerMenu.setNextSelection(null);

		// Transfer clay to inventory instead of dropping on ground
		vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("transfer", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
	}

	private Gob findNearestClay() {
		Gob closest = null;
		double closestDist = Double.MAX_VALUE;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = player.rc;

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					if (blacklisted.contains(gob.id)) continue;
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.contains("gfx/terobjs/clay")) continue;
					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (dist < closestDist) { closestDist = dist; closest = gob; }
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return closest;
	}

	@Override
	protected String windowPrefKey() { return "wndc-clayDiggingBotWindow"; }

	@Override
	protected void onCleanup() { gui.clayDiggingBot = null; }
}
