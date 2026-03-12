package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ForagingBot extends BotBase {
	private CheckBox alsoPickGroundItemsCB;
	private boolean alsoPickGroundItems;

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
		if (player == null) { Thread.sleep(500); return; }
		if (!checkVitals()) return;

		if (gui.prog != null) {
			setStatus("Working...");
			Thread.sleep(500);
			return;
		}

		Gob herb = findNearestForageable();
		if (herb == null) {
			setStatus("No forageables found");
			Thread.sleep(3000);
			return;
		}

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Thread.sleep(300);
		}

		setStatus("Walking to forageable...");
		gui.map.pfLeftClick(herb.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			Actions.unstuck(gui);
			Thread.sleep(1000);
			return;
		}

		player = gui.map.player();
		if (player == null) { Thread.sleep(500); return; }
		if (herb.rc.dist(player.rc) > 11 * 5) {
			setStatus("Too far, retrying...");
			Thread.sleep(1000);
			return;
		}

		setStatus("Picking...");
		try {
			Resource res = herb.getres();
			if (res != null && res.name.startsWith("gfx/terobjs/herbs")) {
				FlowerMenu.setNextSelection("Pick");
			}
		} catch (Loading ignored) {}
		gui.map.wdgmsg("click", Coord.z, herb.rc.floor(posres), 3, 0, 0,
			(int) herb.id, herb.rc.floor(posres), 0, -1);
		Thread.sleep(1500);

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Thread.sleep(300);
		}
		Thread.sleep(500);
	}

	private Gob findNearestForageable() {
		Gob closest = null;
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
					if (closest == null || dist < closest.rc.dist(playerPos)) closest = gob;
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
