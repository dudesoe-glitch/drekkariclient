package haven.automated;

import haven.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static haven.OCache.posres;

public class ButcherBot extends BotBase {
	private volatile boolean doLargeGame;
	private volatile boolean doLivestock;
	private volatile boolean doPredators;
	private volatile boolean doSmallGame;
	private final Set<Long> blacklisted = ConcurrentHashMap.newKeySet();

	private static final Set<String> LARGE_GAME = new HashSet<>(Arrays.asList(
		"gfx/kritter/boar/boar", "gfx/kritter/moose/moose", "gfx/kritter/reddeer/reddeer",
		"gfx/kritter/reindeer/reindeer", "gfx/kritter/roedeer/roedeer", "gfx/kritter/mammoth/mammoth",
		"gfx/kritter/walrus/walrus", "gfx/kritter/greyseal/greyseal", "gfx/kritter/troll/troll"
	));

	private static final Set<String> LIVESTOCK = new HashSet<>(Arrays.asList(
		"gfx/kritter/cattle/cattle", "gfx/kritter/cattle/calf", "gfx/kritter/sheep/sheep",
		"gfx/kritter/sheep/lamb", "gfx/kritter/goat/wildgoat", "gfx/kritter/goat/nanny",
		"gfx/kritter/goat/billy", "gfx/kritter/goat/kid", "gfx/kritter/pig/hog",
		"gfx/kritter/pig/sow", "gfx/kritter/pig/piglet", "gfx/kritter/horse/horse",
		"gfx/kritter/horse/foal", "gfx/kritter/chicken/chicken", "gfx/kritter/chicken/hen",
		"gfx/kritter/chicken/rooster", "gfx/kritter/chicken/chick"
	));

	private static final Set<String> PREDATORS = new HashSet<>(Arrays.asList(
		"gfx/kritter/bear/bear", "gfx/kritter/bear/polarbear", "gfx/kritter/wolf/wolf",
		"gfx/kritter/lynx/lynx", "gfx/kritter/wolverine/wolverine", "gfx/kritter/adder/adder",
		"gfx/kritter/caveangler/caveangler", "gfx/kritter/cavelouse/cavelouse",
		"gfx/kritter/orca/orca", "gfx/kritter/nidbane/nidbane",
		"gfx/kritter/bat/bat", "gfx/kritter/caverat/caverat"
	));

	private static final Set<String> SMALL_GAME = new HashSet<>(Arrays.asList(
		"gfx/kritter/fox/fox", "gfx/kritter/badger/badger", "gfx/kritter/beaver/beaver",
		"gfx/kritter/otter/otter", "gfx/kritter/stoat/stoat", "gfx/kritter/swan/swan",
		"gfx/kritter/pelican/pelican", "gfx/kritter/eagleowl/eagleowl",
		"gfx/kritter/goldeneagle/goldeneagle", "gfx/kritter/woodgrouse/woodgrouse-m",
		"gfx/kritter/garefowl/garefowl", "gfx/kritter/goshawk/goshawk",
		"gfx/kritter/crane/crane", "gfx/kritter/mallard/mallard",
		"gfx/kritter/chasmconch/chasmconch", "gfx/kritter/ooze/greenooze",
		"gfx/kritter/hedgehog/hedgehog", "gfx/kritter/rabbit/rabbit"
	));

	// Small animals use "Wring Its Neck" instead of "Butcher" (wiki: Hunting)
	private static final Set<String> WRING_NECK_ANIMALS = new HashSet<>(Arrays.asList(
		"gfx/kritter/chicken/chicken", "gfx/kritter/chicken/hen",
		"gfx/kritter/chicken/rooster", "gfx/kritter/rabbit/rabbit",
		"gfx/kritter/hedgehog/hedgehog"
	));

	public ButcherBot(GameUI gui) {
		super(gui, UI.scale(220, 145), "Butcher Bot");

		doLargeGame = Utils.getprefb("butcherBot_largeGame", true);
		doLivestock = Utils.getprefb("butcherBot_livestock", true);
		doPredators = Utils.getprefb("butcherBot_predators", true);
		doSmallGame = Utils.getprefb("butcherBot_smallGame", true);

		add(new CheckBox("Large Game") {
			{ a = doLargeGame; }
			public void set(boolean val) { doLargeGame = val; a = val; Utils.setprefb("butcherBot_largeGame", val); }
		}, UI.scale(10, 10));

		add(new CheckBox("Livestock") {
			{ a = doLivestock; }
			public void set(boolean val) { doLivestock = val; a = val; Utils.setprefb("butcherBot_livestock", val); }
		}, UI.scale(10, 30));

		add(new CheckBox("Predators") {
			{ a = doPredators; }
			public void set(boolean val) { doPredators = val; a = val; Utils.setprefb("butcherBot_predators", val); }
		}, UI.scale(110, 10));

		add(new CheckBox("Small Game") {
			{ a = doSmallGame; }
			public void set(boolean val) { doSmallGame = val; a = val; Utils.setprefb("butcherBot_smallGame", val); }
		}, UI.scale(110, 30));

		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, 60));

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
		add(activeButton, UI.scale(65, 105));
	}

	@Override
	protected void tick() throws InterruptedException {
		if (!(doLargeGame || doLivestock || doPredators || doSmallGame)) {
			Thread.sleep(500);
			return;
		}
		if (!checkVitals()) return;

		if (gui.prog != null) {
			setStatus("Working...");
			waitForProgressBar(30000);
			return;
		}

		Gob animal = findNearestKnockedAnimal();
		if (animal == null) {
			if (!blacklisted.isEmpty()) {
				blacklisted.clear();
				animal = findNearestKnockedAnimal();
			}
			if (animal == null) {
				setStatus("No knocked animals found");
				deactivate();
				return;
			}
		}

		// Capture vhand to local variable (TOCTOU fix)
		WItem vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}

		String animalName = "animal";
		String resName = null;
		try {
			Resource res = animal.getres();
			if (res != null) {
				resName = res.name;
				animalName = resName.substring(resName.lastIndexOf('/') + 1);
			}
		} catch (Loading ignored) {}
		setStatus("Walking to " + animalName);

		gui.map.pfLeftClick(animal.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			blacklisted.add(animal.id);
			Actions.unstuck(gui);
			return;
		}
		if (stop) return;

		Gob player = gui.map.player();
		if (player == null) return;
		if (animal.rc.dist(player.rc) > 11 * 5) {
			blacklisted.add(animal.id);
			setStatus("Too far, skipping " + animalName);
			return;
		}

		// Verify animal still exists after pathfinding
		if (gui.map.glob.oc.getgob(animal.id) == null) {
			setStatus(animalName + " already gone");
			return;
		}

		// Choose correct FlowerMenu option based on animal type
		String action = "Butcher";
		if (resName != null && WRING_NECK_ANIMALS.contains(resName)) {
			action = "Wring its neck";
		}

		setStatus("Butchering " + animalName);
		FlowerMenu.setNextSelection(action);
		gui.map.wdgmsg("click", Coord.z, animal.rc.floor(posres), 3, 0, 0,
			(int) animal.id, animal.rc.floor(posres), 0, -1);
		Thread.sleep(50);
		waitForProgressBar(30000);

		// Clear stale FlowerMenu selection on failure
		FlowerMenu.setNextSelection(null);

		vh = gui.vhand;
		if (vh != null) {
			vh.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
	}

	private Gob findNearestKnockedAnimal() {
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
					if (!GobHelper.isKnocked(gob)) continue;
					// Skip animals being carried by other players
					if (gob.getPoses().contains("carried")) continue;
					if (!isAnimalEnabled(res.name)) continue;
					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (dist < closestDist) { closestDist = dist; closest = gob; }
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return closest;
	}

	private boolean isAnimalEnabled(String resName) {
		if (doLargeGame && LARGE_GAME.contains(resName)) return true;
		if (doLivestock && LIVESTOCK.contains(resName)) return true;
		if (doPredators && PREDATORS.contains(resName)) return true;
		if (doSmallGame && SMALL_GAME.contains(resName)) return true;
		return false;
	}

	@Override
	protected String windowPrefKey() { return "wndc-butcherBotWindow"; }

	@Override
	protected void onCleanup() { gui.butcherBot = null; }
}
