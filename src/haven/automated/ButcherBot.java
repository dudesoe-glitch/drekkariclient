package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class ButcherBot extends BotBase {
	private boolean doLargeGame;
	private boolean doLivestock;
	private boolean doPredators;
	private boolean doSmallGame;

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
		"gfx/kritter/orca/orca", "gfx/kritter/nidbane/nidbane"
	));

	private static final Set<String> SMALL_GAME = new HashSet<>(Arrays.asList(
		"gfx/kritter/fox/fox", "gfx/kritter/badger/badger", "gfx/kritter/beaver/beaver",
		"gfx/kritter/otter/otter", "gfx/kritter/stoat/stoat", "gfx/kritter/swan/swan",
		"gfx/kritter/pelican/pelican", "gfx/kritter/eagleowl/eagleowl",
		"gfx/kritter/goldeneagle/goldeneagle", "gfx/kritter/woodgrouse/woodgrouse-m",
		"gfx/kritter/garefowl/garefowl", "gfx/kritter/goshawk/goshawk",
		"gfx/kritter/crane/crane", "gfx/kritter/mallard/mallard",
		"gfx/kritter/chasmconch/chasmconch", "gfx/kritter/ooze/greenooze"
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
			Thread.sleep(1000);
			return;
		}
		if (!checkVitals()) return;

		if (gui.prog != null) {
			setStatus("Working...");
			waitForProgressBar(10000);
			Thread.sleep(500);
			return;
		}

		Gob animal = findNearestKnockedAnimal();
		if (animal == null) {
			setStatus("No knocked animals found");
			Thread.sleep(3000);
			return;
		}

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Thread.sleep(500);
		}

		String animalName = "animal";
		try {
			Resource res = animal.getres();
			if (res != null) animalName = res.name.substring(res.name.lastIndexOf('/') + 1);
		} catch (Loading ignored) {}
		setStatus("Walking to " + animalName);

		gui.map.pfLeftClick(animal.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			Actions.unstuck(gui);
			Thread.sleep(1000);
			return;
		}

		if (animal.rc.dist(gui.map.player().rc) > 11 * 5) {
			setStatus("Too far, retrying...");
			Thread.sleep(1000);
			return;
		}

		setStatus("Butchering " + animalName);
		FlowerMenu.setNextSelection("Butcher");
		gui.map.wdgmsg("click", Coord.z, animal.rc.floor(posres), 3, 0, 0,
			(int) animal.id, animal.rc.floor(posres), 0, -1);
		Thread.sleep(1000);
		waitForProgressBar(30000);
		Thread.sleep(500);
		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Thread.sleep(500);
		}
		Thread.sleep(2000);
	}

	private Gob findNearestKnockedAnimal() {
		Gob closest = null;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = player.rc;

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					if (!GobHelper.isKnocked(gob)) continue;
					if (!isAnimalEnabled(res.name)) continue;
					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (closest == null || dist < closest.rc.dist(playerPos)) closest = gob;
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
