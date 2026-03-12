package haven.automated;

import haven.*;

import static haven.OCache.posres;

public class ClayDiggingBot extends BotBase {

	public ClayDiggingBot(GameUI gui) {
		super(gui, UI.scale(220, 80), "Clay Digging Bot");

		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, 10));

		activeButton = new Button(UI.scale(80), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
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
			Thread.sleep(500);
			return;
		}

		Gob clay = findNearestClay();
		if (clay == null) {
			setStatus("No clay patches found");
			Thread.sleep(3000);
			return;
		}

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Thread.sleep(500);
		}

		setStatus("Walking to clay...");
		gui.map.pfLeftClick(clay.rc.floor().add(2, 0), null);
		if (!AUtils.waitPf(gui)) {
			AUtils.unstuck(gui);
			Thread.sleep(1000);
			return;
		}

		if (clay.rc.dist(gui.map.player().rc) > 11 * 5) {
			setStatus("Too far, retrying...");
			Thread.sleep(1000);
			return;
		}

		setStatus("Digging clay...");
		FlowerMenu.setNextSelection("Dig");
		gui.map.wdgmsg("click", Coord.z, clay.rc.floor(posres), 3, 0, 0,
			(int) clay.id, clay.rc.floor(posres), 0, -1);
		Thread.sleep(1000);
		waitForProgressBar(30000);
		Thread.sleep(500);
		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Thread.sleep(500);
		}
		Thread.sleep(1000);
	}

	private Gob findNearestClay() {
		Gob closest = null;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = player.rc;

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.contains("gfx/terobjs/clay")) continue;
					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (closest == null || dist < closest.rc.dist(playerPos)) closest = gob;
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
