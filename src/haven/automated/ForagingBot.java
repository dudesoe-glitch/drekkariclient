package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ForagingBot extends BotBase {
	private CheckBox alsoPickGroundItemsCB;
	private volatile boolean alsoPickGroundItems;

	private static final Set<String> PICKABLE_ITEMS = new HashSet<>(Arrays.asList(
		"adder", "arrow", "bat", "swan", "goshawk", "precioussnowflake",
		"truffle-black0", "truffle-black1", "truffle-black2", "truffle-black3",
		"truffle-white0", "truffle-white1", "truffle-white2", "truffle-white3",
		"gemstone", "boarspear"
	));

	public ForagingBot(GameUI gui) {
		super(gui, UI.scale(250, 100), "Foraging Bot");
		this.alsoPickGroundItems = Utils.getprefb("foragingBotPickGround", false);

		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, 10));

		alsoPickGroundItemsCB = new CheckBox("Also pick ground items") {
			@Override
			public void changed(boolean val) {
				alsoPickGroundItems = val;
				Utils.setprefb("foragingBotPickGround", val);
			}
		};
		alsoPickGroundItemsCB.a = alsoPickGroundItems;
		add(alsoPickGroundItemsCB, UI.scale(10, 30));

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
		add(activeButton, UI.scale(80, 55));
	}

	@Override
	protected void tick() throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (!checkVitals()) return;

		if (gui.prog != null) {
			setStatus("Working...");
			Actions.waitProgBar(gui);
			if (stop) return;
			return;
		}

		Gob herb = findNearestForageable();
		if (herb == null) {
			setStatus("No forageables found");
			deactivate();
			return;
		}

		WItem vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
		if (stop) return;

		setStatus("Walking to forageable...");
		Coord2d herbPos = new Coord2d(herb.rc.x, herb.rc.y);
		gui.map.pfLeftClick(herbPos.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			Actions.unstuck(gui);
			return;
		}
		if (stop) return;

		player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (herbPos.dist(new Coord2d(player.rc.x, player.rc.y)) > 11 * 5) {
			setStatus("Too far, retrying...");
			return;
		}

		if (gui.map.glob.oc.getgob(herb.id) == null) return;

		setStatus("Picking...");
		try {
			Resource res = herb.getres();
			if (res != null && res.name.startsWith("gfx/terobjs/herbs")) {
				FlowerMenu.setNextSelection("Pick");
			}
		} catch (Loading ignored) {}
		Coord2d clickPos = new Coord2d(herb.rc.x, herb.rc.y);
		gui.map.wdgmsg("click", Coord.z, clickPos.floor(posres), 3, 0, 0,
			(int) herb.id, clickPos.floor(posres), 0, -1);

		// Wait for the pick to complete: progress bar, gob disappearing, or item on cursor
		long herbId = herb.id;
		int waited = 0;
		while (waited < 10000 && !stop && active) {
			// Progress bar active — wait for it
			GameUI.Progress p = gui.prog;
			if (p != null && p.prog >= 0) {
				Actions.waitProgBar(gui);
				break;
			}
			// Herb gone — pick succeeded
			if (gui.map.glob.oc.getgob(herbId) == null) break;
			// Item appeared on cursor — pick succeeded
			if (gui.vhand != null) break;
			Thread.sleep(50);
			waited += 50;
		}
		FlowerMenu.setNextSelection(null);
		if (stop) return;

		vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
	}

	private Gob findNearestForageable() {
		Gob closest = null;
		double closestDist = Double.MAX_VALUE;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					boolean isHerb = res.name.startsWith("gfx/terobjs/herbs");
					boolean isPickable = alsoPickGroundItems && PICKABLE_ITEMS.contains(res.basename());
					if (!isHerb && !isPickable) continue;
					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (dist < closestDist) {
						closest = gob;
						closestDist = dist;
					}
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return closest;
	}

	@Override
	protected String windowPrefKey() { return "wndc-foragingBotWindow"; }

	@Override
	protected void onCleanup() { gui.foragingBot = null; }
}
