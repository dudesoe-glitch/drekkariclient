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
		gui.map.pfLeftClick(herb.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			Actions.unstuck(gui);
			return;
		}
		if (stop) return;

		player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (herb.rc.dist(player.rc) > 11 * 5) {
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
		gui.map.wdgmsg("click", Coord.z, herb.rc.floor(posres), 3, 0, 0,
			(int) herb.id, herb.rc.floor(posres), 0, -1);
		Thread.sleep(50);
		Actions.waitProgBar(gui);
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
		Coord2d playerPos = player.rc;

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
