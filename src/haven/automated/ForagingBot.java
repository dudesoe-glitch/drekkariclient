package haven.automated;

import haven.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static haven.OCache.posres;
import static haven.automated.AUtils.potentialAggroTargets;
import static haven.automated.CombatDistanceTool.animalDistances;

public class ForagingBot extends BotBase {
	private CheckBox alsoPickGroundItemsCB;
	private CheckBox wanderCB;
	private TextEntry filterEntry;
	private volatile boolean alsoPickGroundItems;
	private volatile boolean wanderWhenEmpty;
	private volatile String herbFilter = "";
	private static final double DANGER_MARGIN = 1.5; // multiplier on aggro distance for safety
	private final Set<Long> blacklisted = ConcurrentHashMap.newKeySet();
	private Coord2d lastWanderPos;
	private final Random random = new Random();

	private static final Set<String> PICKABLE_ITEMS = new HashSet<>(Arrays.asList(
		"adder", "arrow", "bat", "swan", "goshawk", "precioussnowflake",
		"truffle-black0", "truffle-black1", "truffle-black2", "truffle-black3",
		"truffle-white0", "truffle-white1", "truffle-white2", "truffle-white3",
		"gemstone", "boarspear"
	));

	private static final int PATHFIND_OFFSET = 12; // Larger offset for horse compatibility

	public ForagingBot(GameUI gui) {
		super(gui, UI.scale(250, 150), "Foraging Bot");
		this.alsoPickGroundItems = Utils.getprefb("foragingBotPickGround", false);
		this.wanderWhenEmpty = Utils.getprefb("foragingBotWander", false);
		this.herbFilter = Utils.getpref("foragingBotFilter", "");

		int y = 10;
		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, y));
		y += 20;

		add(new Label("Filter (comma-sep):"), UI.scale(10, y));
		filterEntry = new TextEntry(UI.scale(120), herbFilter) {
			@Override
			public boolean keydown(KeyDownEvent ev) {
				boolean ret = super.keydown(ev);
				herbFilter = text().trim();
				Utils.setpref("foragingBotFilter", herbFilter);
				return ret;
			}
		};
		filterEntry.tooltip = RichText.render("Comma-separated herb names to pick.\nEmpty = pick all herbs.\nExample: nettle,tansy,clover", UI.scale(300));
		add(filterEntry, UI.scale(120, y - 2));
		y += 22;

		alsoPickGroundItemsCB = new CheckBox("Also pick ground items") {
			@Override
			public void changed(boolean val) {
				alsoPickGroundItems = val;
				Utils.setprefb("foragingBotPickGround", val);
			}
		};
		alsoPickGroundItemsCB.a = alsoPickGroundItems;
		add(alsoPickGroundItemsCB, UI.scale(10, y));
		y += 20;

		wanderCB = new CheckBox("Wander when empty") {
			@Override
			public void changed(boolean val) {
				wanderWhenEmpty = val;
				Utils.setprefb("foragingBotWander", val);
			}
		};
		wanderCB.a = wanderWhenEmpty;
		wanderCB.tooltip = RichText.render("Walk in a random direction when no forageables are nearby,\nthen re-scan. Avoids backtracking.", UI.scale(300));
		add(wanderCB, UI.scale(10, y));
		y += 25;

		activeButton = new Button(UI.scale(80), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					blacklisted.clear();
					lastWanderPos = null;
					this.change("Stop");
					statusLabel.settext("Running...");
				} else {
					idlePlayer();
					this.change("Start");
					statusLabel.settext("Stopped");
				}
			}
		};
		add(activeButton, UI.scale(80, y));
	}

	@Override
	protected void tick() throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (!checkVitals()) return;

		GameUI.Progress p = gui.prog;
		if (p != null) {
			setStatus("Working...");
			Actions.waitProgBar(gui);
			if (stop) return;
			return;
		}

		// Safety: check if player is near danger before doing anything
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		if (isNearDanger(playerPos)) {
			setStatus("Danger nearby! Pausing...");
			Thread.sleep(2000);
			return;
		}

		Gob herb = findNearestForageable();
		if (herb == null) {
			if (wanderWhenEmpty) {
				wander();
			} else {
				setStatus("No forageables found");
				deactivate();
			}
			return;
		}

		WItem vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
		if (stop) return;

		// Check if path to herb is safe (no nearby animals)
		Coord2d herbPos = new Coord2d(herb.rc.x, herb.rc.y);
		if (isNearDanger(herbPos)) {
			blacklisted.add(herb.id);
			setStatus("Skipping — dangerous animal nearby");
			return;
		}

		setStatus("Walking to forageable...");
		gui.map.pfLeftClick(herbPos.floor().add(PATHFIND_OFFSET, 0), null);
		if (!Actions.waitPf(gui)) {
			blacklisted.add(herb.id);
			Actions.unstuck(gui);
			return;
		}
		if (stop) return;

		player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (herbPos.dist(new Coord2d(player.rc.x, player.rc.y)) > 11 * 5) {
			blacklisted.add(herb.id);
			setStatus("Too far, skipping...");
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
			GameUI.Progress pg = gui.prog;
			if (pg != null && pg.prog >= 0) {
				Actions.waitProgBar(gui);
				break;
			}
			if (gui.map.glob.oc.getgob(herbId) == null) break;
			if (gui.vhand != null) break;
			Thread.sleep(50);
			waited += 50;
		}
		FlowerMenu.setNextSelection(null);

		// If herb still exists after timeout, blacklist it (couldn't pick — maybe collision)
		if (gui.map.glob.oc.getgob(herbId) != null && waited >= 10000) {
			blacklisted.add(herbId);
		}
		if (stop) return;

		// Record position for wander anti-backtracking
		lastWanderPos = new Coord2d(player.rc.x, player.rc.y);

		vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
	}

	/** Walk in a random direction to find new forageables. Avoids backtracking. */
	private void wander() throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) return;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);

		// Pick a random direction, biased away from where we came from
		double angle;
		if (lastWanderPos != null) {
			// Move away from last position
			double backAngle = Math.atan2(lastWanderPos.y - playerPos.y, lastWanderPos.x - playerPos.x);
			angle = backAngle + Math.PI + (random.nextDouble() - 0.5) * Math.PI; // ~opposite direction with spread
		} else {
			angle = random.nextDouble() * 2 * Math.PI;
		}

		double wanderDist = 11 * (8 + random.nextInt(7)); // 8-15 tiles
		Coord2d wanderTarget = new Coord2d(
			playerPos.x + Math.cos(angle) * wanderDist,
			playerPos.y + Math.sin(angle) * wanderDist
		);

		setStatus("Wandering...");
		lastWanderPos = playerPos;
		gui.map.pfLeftClick(wanderTarget.floor(), null);
		Actions.waitPf(gui);
		Thread.sleep(500);
	}

	private Gob findNearestForageable() {
		Gob closest = null;
		double closestDist = Double.MAX_VALUE;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		String[] filters = parseFilter();

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					if (blacklisted.contains(gob.id)) continue;
					Resource res = gob.getres();
					if (res == null) continue;
					boolean isHerb = res.name.startsWith("gfx/terobjs/herbs");
					boolean isPickable = alsoPickGroundItems && PICKABLE_ITEMS.contains(res.basename());
					if (!isHerb && !isPickable) continue;

					// Apply herb filter if set
					if (isHerb && filters != null) {
						String basename = res.basename();
						boolean matched = false;
						for (String f : filters) {
							if (basename.contains(f)) { matched = true; break; }
						}
						if (!matched) continue;
					}

					// Skip herbs near dangerous animals
					if (isNearDanger(gob.rc)) continue;

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

	/**
	 * Check if a position is dangerously close to an aggressive animal.
	 * Uses CombatDistanceTool.animalDistances for known aggro ranges,
	 * falls back to a default safe distance for unknown animals in potentialAggroTargets.
	 */
	private boolean isNearDanger(Coord2d pos) {
		final double DEFAULT_AGGRO_DIST = 30.0;
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					if (!potentialAggroTargets.contains(res.name)) continue;
					// Skip players — they're in potentialAggroTargets but aren't NPC threats
					if (res.name.equals("gfx/borka/body")) continue;

					double aggroDist = animalDistances.getOrDefault(res.name, DEFAULT_AGGRO_DIST);
					double safeDist = aggroDist * DANGER_MARGIN;
					if (pos.dist(gob.rc) < safeDist) return true;
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return false;
	}

	/** Parse comma-separated filter into trimmed lowercase terms. Returns null if empty/blank. */
	private String[] parseFilter() {
		String f = herbFilter;
		if (f == null || f.trim().isEmpty()) return null;
		String[] parts = f.split(",");
		List<String> result = new ArrayList<>();
		for (String part : parts) {
			String trimmed = part.trim().toLowerCase();
			if (!trimmed.isEmpty()) result.add(trimmed);
		}
		return result.isEmpty() ? null : result.toArray(new String[0]);
	}

	@Override
	protected String windowPrefKey() { return "wndc-foragingBotWindow"; }

	@Override
	protected void onCleanup() { gui.foragingBot = null; }
}
