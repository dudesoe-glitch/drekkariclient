package haven.automated;

import haven.*;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;

public class CleanupBot extends BotBase {
	private volatile boolean chopBushes;
	private volatile boolean chopTrees;
	private volatile boolean chipRocks;
	private volatile boolean destroyStumps;
	private volatile boolean destroySoil;

	public CleanupBot(GameUI gui) {
		super(gui, UI.scale(220, 105), "Cleanup Bot");
		chopBushes = false; chopTrees = false; chipRocks = false; destroyStumps = false; destroySoil = false;

		add(new CheckBox("Bushes") {{ a = chopBushes; } public void set(boolean val) { chopBushes = val; a = val; }}, UI.scale(10, 10));
		add(new CheckBox("Trees") {{ a = chopTrees; } public void set(boolean val) { chopTrees = val; a = val; }}, UI.scale(10, 30));
		add(new CheckBox("Rocks") {{ a = chipRocks; } public void set(boolean val) { chipRocks = val; a = val; }}, UI.scale(10, 50));
		add(new CheckBox("Stumps") {{ a = destroyStumps; } public void set(boolean val) { destroyStumps = val; a = val; }}, UI.scale(10, 70));
		add(new CheckBox("Soil") {{ a = destroySoil; } public void set(boolean val) { destroySoil = val; a = val; }}, UI.scale(90, 10));

		activeButton = new Button(UI.scale(50), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) { this.change("Stop"); }
				else { idlePlayer(); this.change("Start"); }
			}
		};
		add(activeButton, UI.scale(120, 70));
	}

	@Override
	protected void tick() throws InterruptedException {
		if (!(chopBushes || chopTrees || destroyStumps || chipRocks || destroySoil)) { sleep(500); return; }
		if (!checkVitals()) return;
		Gob gob = findClosestGob();
		if (chipRocks) dropStones();
		destroyGob(gob);
	}

	private void destroyGob(Gob gob) throws InterruptedException {
		Gob player = gui.map.player();
		if (player != null && gui.prog != null && (player.getPoses().contains("pickan") || player.getPoses().contains("treechop") || player.getPoses().contains("chopping") || player.getPoses().contains("shoveldig") || player.getPoses().contains("drinkan"))) {
			waitWhileWorking(30000);
		} else {
			if (gob != null) {
				// Check gob still exists before walking to it
				if (gui.map.glob.oc.getgob(gob.id) == null) return;
				gui.map.pfLeftClick(gob.rc.floor().add(20, 0), null);
				if (!Actions.waitPf(gui)) Actions.unstuck(gui);
				player = gui.map.player();
				if (player == null) return;
				if (gob.rc.dist(player.rc) < 11 * 5) {
					Resource res;
					try { res = gob.getres(); } catch (Loading l) { return; }
					if (res == null) return;
					Actions.clearhand(gui);
					if ((res.name.contains("/trees/") && !res.name.endsWith("stump") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk")) || res.name.contains("/bushes/")) {
						FlowerMenu.setNextSelection("Chop");
						gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
						waitWhileWorking(30000);
						// Log goes to cursor after tree chop — drop it on ground
						WItem vh = gui.vhand;
						if (vh != null) {
							vh.item.wdgmsg("drop", Coord.z);
							Actions.waitForEmptyHand(gui, 1000, "");
						}
					} else if (res.name.contains("/bumlings/")) {
						FlowerMenu.setNextSelection("Chip");
						gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
						waitWhileWorking(30000);
					} else if (res.name.endsWith("stump") || res.name.endsWith("/stockpile-soil")) {
						gui.act("destroy");
						gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
						gui.map.wdgmsg("click", Coord.z, Coord.z, 3, 0);
						waitWhileWorking(30000);
					}
				}
			} else {
				gui.errorsilent("Cleanup Bot: Nothing left to destroy.");
				deactivate();
			}
		}
	}

	private void waitWhileWorking(int timeout) throws InterruptedException {
		sleep(200);
		int hz = 50; int time = 0;
		while (gui.prog != null && gui.prog.prog != -1 && time < timeout) { time += hz; sleep(hz); }
	}

	private void dropStones() {
		for (WItem wItem : gui.maininv.getAllItems()) {
			try {
				GItem gitem = wItem.item;
				if (Config.stoneItemBaseNames.contains(gitem.resource().basename())) gitem.wdgmsg("drop", new Coord(wItem.item.sz.x / 2, wItem.item.sz.y / 2));
			} catch (Loading ignored) {}
		}
	}

	private Gob findClosestGob() {
		Gob closestGob = null;
		double closestDist = Double.MAX_VALUE;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d plc = new Coord2d(player.rc.x, player.rc.y);
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					boolean selected = (res.name.contains("/bumlings/") && chipRocks) || (res.name.endsWith("stump") && destroyStumps)
						|| ((res.name.contains("/trees/") && !res.name.endsWith("stump")) && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk") && chopTrees)
						|| (res.name.contains("/bushes/") && chopBushes) || (res.name.endsWith("/stockpile-soil") && destroySoil);
					if (selected) {
						double dist = gob.rc.dist(plc);
						if (dist > MAX_SEARCH_DIST) continue;
						if (dist < closestDist) { closestDist = dist; closestGob = gob; }
					}
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return closestGob;
	}

	@Override protected String windowPrefKey() { return "wndc-cleanupBotWindow"; }
	@Override protected void onCleanup() { gui.cleanupBot = null; }
}
