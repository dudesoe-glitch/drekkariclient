package haven.automated;

import haven.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static haven.OCache.posres;

public class FarmingBot extends BotBase {
	private CheckBox harvestCheckBox;
	private CheckBox replantCheckBox;
	private CheckBox useBestSeedCheckBox;
	private volatile boolean frozenWarningShown = false;
	private volatile int plantFailCount = 0;
	private final Set<Long> blacklistedCrops = ConcurrentHashMap.newKeySet();

	private static final String CROP_PREFIX = "gfx/terobjs/plants/";
	private static final String SEED_PREFIX = "gfx/invobjs/seed-";
	private static final int WINTER = 3;
	private static final double SCYTHE_RADIUS = 40.0; // 2x3 tiles diagonal (~39.6 units) — wiki: "2x3 tiles in front of character"
	private static final String SCYTHE_RES = "scythe";
	private static final int HARVEST_VERIFY_TIMEOUT = 1500; // ms to wait for crop gob removal
	private static final int HARVEST_VERIFY_INTERVAL = 50; // ms between checks
	private static final double DIRECT_WALK_DIST = 55.0; // ~5 tiles — skip pathfinder, walk directly
	private static final int DIRECT_WALK_TIMEOUT = 3000; // ms timeout for direct walk
	private static final String MOUND_BED_RES = "gfx/terobjs/moundbed";
	private static final double MOUND_BED_RADIUS = 225.0; // game units — from Gob.java radius overlay

	// Crops that use seeds for replanting
	private static final Map<String, String> CROP_TO_SEED = new HashMap<String, String>() {{
		put("carrot", "gfx/invobjs/seed-carrot");
		put("flax", "gfx/invobjs/seed-flax");
		put("hemp", "gfx/invobjs/seed-hemp");
		put("lettuce", "gfx/invobjs/seed-lettuce");
		put("pipeweed", "gfx/invobjs/seed-pipeweed");
		put("poppy", "gfx/invobjs/seed-poppy");
		put("pumpkin", "gfx/invobjs/seed-pumpkin");
		put("wheat", "gfx/invobjs/seed-wheat");
		put("barley", "gfx/invobjs/seed-barley");
		put("millet", "gfx/invobjs/seed-millet");
		put("wine", "gfx/invobjs/seed-grape");
		put("grape", "gfx/invobjs/seed-grape");
		put("pepper", "gfx/invobjs/seed-pepper");
		put("tea", "gfx/invobjs/seed-tea");
		put("cucumber", "gfx/invobjs/seed-cucumber");
	}};

	// Crops replanted using the harvested item itself, not seeds
	// Wiki: beetroot, leek, onions, garlic, kale, peas, peppercorn are planted as the item
	private static final Map<String, String> CROP_TO_ITEM = new HashMap<String, String>() {{
		put("beetroot", "gfx/invobjs/beet");
		put("leek", "gfx/invobjs/leek");
		put("onion", "gfx/invobjs/yellowonion");
		put("yellowonion", "gfx/invobjs/yellowonion");
		put("redonion", "gfx/invobjs/redonion");
		put("peas", "gfx/invobjs/peapod");
		put("pepper", "gfx/invobjs/peppercorn");
		put("garlic", "gfx/invobjs/garlic");
		put("greenkale", "gfx/invobjs/greenkale");
		put("wildkale", "gfx/invobjs/wildkale");
		put("wildonion", "gfx/invobjs/wildonion");
		put("wildtuber", "gfx/invobjs/wildtuber");
	}};

	// Crops that regrow or cannot be replanted — do NOT replant
	// Wiki: hops regrow from stage 4 after harvest on same trellis
	// Dev update "Radish Butchery" (2026-03-12): radish cannot be replanted
	private static final Set<String> NO_REPLANT_CROPS = new HashSet<>(Arrays.asList(
		"hops",
		"radish"
	));

	public FarmingBot(GameUI gui) {
		super(gui, UI.scale(220, 130), "Farming Bot");
		checkInventory = false; // Farming works with full inventory (harvest+replant cycle)

		int y = 10;
		harvestCheckBox = new CheckBox("Harvest mature crops") {{a = true;}};
		add(harvestCheckBox, UI.scale(10, y));
		y += 20;

		replantCheckBox = new CheckBox("Replant after harvest") {{a = true;}};
		add(replantCheckBox, UI.scale(10, y));
		y += 20;

		useBestSeedCheckBox = new CheckBox("Use highest quality seeds") {{a = true;}};
		add(useBestSeedCheckBox, UI.scale(10, y));
		y += 25;

		statusLabel = new Label("Status: Idle");
		add(statusLabel, UI.scale(10, y));
		y += 20;

		activeButton = new Button(UI.scale(50), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					frozenWarningShown = false;
					plantFailCount = 0;
					blacklistedCrops.clear();
					this.change("Stop");
				} else {
					idlePlayer();
					this.change("Start");
				}
			}
		};
		add(activeButton, UI.scale(120, y - 25));
	}

	private boolean isWinter() {
		try {
			Astronomy ast = gui.map.glob.ast;
			return ast != null && ast.is == WINTER;
		} catch (NullPointerException e) {
			return false;
		}
	}

	private void checkFrozenGround() {
		if (!isWinter()) {
			frozenWarningShown = false;
			plantFailCount = 0;
			return;
		}
		// Winter — check if player is within mound bed range (warm zone)
		if (isNearMoundBed()) {
			if (frozenWarningShown) {
				frozenWarningShown = false;
				plantFailCount = 0;
				replantCheckBox.a = true;
				gui.ui.msg("Mound Bed nearby — replanting re-enabled.");
			}
			return;
		}
		if (!frozenWarningShown) {
			frozenWarningShown = true;
			gui.errorsilent("Ground is frozen! Replanting disabled (no Mound Bed nearby).");
			replantCheckBox.a = false;
		}
	}

	private List<Coord2d> findMoundBedPositions() {
		List<Coord2d> beds = new ArrayList<>();
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res != null && res.name.equals(MOUND_BED_RES)) {
						beds.add(new Coord2d(gob.rc.x, gob.rc.y));
					}
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return beds;
	}

	private boolean isNearMoundBed() {
		Gob player = gui.map.player();
		if (player == null) return false;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		for (Coord2d bed : findMoundBedPositions()) {
			if (playerPos.dist(bed) <= MOUND_BED_RADIUS) {
				return true;
			}
		}
		return false;
	}

	private boolean isPositionInMoundBedRange(Coord2d pos, List<Coord2d> moundBeds) {
		for (Coord2d bed : moundBeds) {
			if (pos.dist(bed) <= MOUND_BED_RADIUS) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void tick() throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) {
			Thread.sleep(200);
			return;
		}
		if (!checkVitals()) return;
		checkFrozenGround();

		if (!harvestCheckBox.a) {
			setStatus("Enable harvest to begin.");
			Thread.sleep(500);
			return;
		}

		List<Gob> crops = findAllMatureCrops();
		if (crops.isEmpty()) {
			if (!blacklistedCrops.isEmpty()) {
				blacklistedCrops.clear();
				crops = findAllMatureCrops();
			}
			if (crops.isEmpty()) {
				setStatus("No mature crops found.");
				deactivate();
				return;
			}
		}

		// Phase 1: Harvest all crops, collecting positions by crop type
		// cropBaseName is captured BEFORE harvest (gob may despawn after)
		LinkedHashMap<String, List<Coord2d>> harvestedByType = new LinkedHashMap<>();
		for (Gob crop : crops) {
			if (stop) return;
			if (!checkVitals()) return;
			String cropBaseName = getCropBaseName(crop);
			if (cropBaseName == null) continue;
			List<Coord2d> positions = harvestSingleCrop(crop);
			if (positions != null && !positions.isEmpty()) {
				harvestedByType.computeIfAbsent(cropBaseName, k -> new ArrayList<>()).addAll(positions);
			}
		}

		// Phase 2: Replant all positions (all seeds now in inventory for best quality selection)
		if (replantCheckBox.a && !harvestedByType.isEmpty()) {
			for (Map.Entry<String, List<Coord2d>> entry : harvestedByType.entrySet()) {
				if (stop) return;
				if (!checkVitals()) return;
				replantPositions(entry.getValue(), entry.getKey());
			}
		}
	}

	private boolean isScytheEquipped() {
		try {
			Equipory equip = gui.getequipory();
			if (equip == null) return false;
			WItem leftHand = equip.slots[6];
			if (leftHand == null) return false;
			String resName = leftHand.item.res.get().name;
			return resName.contains(SCYTHE_RES);
		} catch (Loading | NullPointerException e) {
			return false;
		}
	}

	private LinkedHashMap<Long, Coord2d> snapshotNearbyCrops(String cropResName, Coord2d center, double radius) {
		LinkedHashMap<Long, Coord2d> snapshot = new LinkedHashMap<>();
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.equals(cropResName)) continue;
					if (!Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")) continue;
					if (!GobHelper.isMature(gob)) continue;
					if (gob.rc.dist(center) > radius) continue;
					snapshot.put(gob.id, new Coord2d(gob.rc.x, gob.rc.y));
				} catch (Loading | NullPointerException ignored) {
				}
			}
		}
		return snapshot;
	}

	/**
	 * Wait for harvested gobs to be removed from OCache, with timeout.
	 * Returns positions of gobs that disappeared from the snapshot.
	 */
	private List<Coord2d> findHarvestedPositions(LinkedHashMap<Long, Coord2d> snapshot) throws InterruptedException {
		// Poll for gob removals up to the timeout
		int elapsed = 0;
		List<Coord2d> harvested = new ArrayList<>();
		while (elapsed < HARVEST_VERIFY_TIMEOUT) {
			harvested.clear();
			for (Map.Entry<Long, Coord2d> entry : snapshot.entrySet()) {
				if (gui.map.glob.oc.getgob(entry.getKey()) == null) {
					harvested.add(entry.getValue());
				}
			}
			if (!harvested.isEmpty()) {
				return harvested;
			}
			Thread.sleep(HARVEST_VERIFY_INTERVAL);
			elapsed += HARVEST_VERIFY_INTERVAL;
		}
		return harvested; // empty if nothing was harvested
	}

	private void replantPositions(List<Coord2d> positions, String cropBaseName) throws InterruptedException {
		// Hops regrow from stage 4 — no replanting needed
		if (NO_REPLANT_CROPS.contains(cropBaseName)) {
			return;
		}

		boolean winter = isWinter();
		List<Coord2d> moundBeds = winter ? findMoundBedPositions() : null;
		int total = positions.size();
		for (int i = 0; i < total; i++) {
			if (stop) return;
			Coord2d pos = positions.get(i);

			// In winter, skip positions outside mound bed range
			if (winter && !isPositionInMoundBedRange(pos, moundBeds)) {
				continue;
			}

			WItem plantItem = findPlantItem(cropBaseName);
			if (plantItem == null) {
				setStatus("Out of " + cropBaseName + " seeds (" + i + "/" + total + " replanted)");
				return;
			}

			setStatus("Replanting " + cropBaseName + " " + (i + 1) + "/" + total + "...");
			plantItem.item.wdgmsg("take", Coord.z);
			if (!Actions.waitForOccupiedHand(gui, 500, "")) {
				return;
			}
			if (stop) return;

			gui.map.wdgmsg("itemact", Coord.z, pos.floor(posres), 0);
			Thread.sleep(50);
			waitForProgressBar(30000);
			if (stop) return;

			if (gui.vhand != null) {
				plantFailCount++;
				// Transfer seed back to inventory instead of dropping on ground
				gui.vhand.item.wdgmsg("transfer", Coord.z);
				Actions.waitForEmptyHand(gui, 500, "");
				if (plantFailCount >= 3) {
					gui.errorsilent("Planting failed repeatedly. Ground may be frozen or tiles occupied.");
					replantCheckBox.a = false;
					plantFailCount = 0;
					return;
				}
			} else {
				plantFailCount = 0;
			}
		}
	}

	private boolean waitForMovement(int timeout) throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) return false;
		// Wait for movement to start
		int waited = 0;
		while (player.getv() <= 0 && waited < 500) {
			Thread.sleep(30);
			waited += 30;
			player = gui.map.player();
			if (player == null) return false;
		}
		// Wait for movement to finish
		int elapsed = 0;
		while (elapsed < timeout) {
			player = gui.map.player();
			if (player == null) return false;
			if (player.getv() <= 0) return true;
			Thread.sleep(30);
			elapsed += 30;
			if (stop) return false;
		}
		return true;
	}

	private String getCropBaseName(Gob crop) {
		try {
			Resource res = crop.getres();
			if (res == null) return null;
			return res.name.substring(res.name.lastIndexOf('/') + 1);
		} catch (Loading | NullPointerException e) {
			return null;
		}
	}

	private List<Coord2d> harvestSingleCrop(Gob crop) throws InterruptedException {
		if (crop == null) return null;

		String cropRes;
		try {
			Resource res = crop.getres();
			if (res == null) return null;
			cropRes = res.name;
		} catch (Loading l) {
			return null;
		}
		String cropBaseName = cropRes.substring(cropRes.lastIndexOf('/') + 1);
		Coord2d cropPos = new Coord2d(crop.rc.x, crop.rc.y);

		setStatus("Walking to " + cropBaseName + "...");
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		if (cropPos.dist(playerPos) < DIRECT_WALK_DIST) {
			gui.map.wdgmsg("click", Coord.z, cropPos.floor(posres), 1, 0);
			if (!waitForMovement(DIRECT_WALK_TIMEOUT)) {
				blacklistedCrops.add(crop.id);
				return null;
			}
		} else {
			gui.map.pfLeftClick(cropPos.floor().add(2, 0), null);
			if (!Actions.waitPf(gui)) {
				blacklistedCrops.add(crop.id);
				Actions.unstuck(gui);
				return null;
			}
		}

		player = gui.map.player();
		if (player == null) return null;
		if (cropPos.dist(new Coord2d(player.rc.x, player.rc.y)) > 11 * 5) {
			blacklistedCrops.add(crop.id);
			setStatus("Too far from " + cropBaseName + ", skipping.");
			return null;
		}

		if (gui.map.glob.oc.getgob(crop.id) == null) {
			setStatus(cropBaseName + " already gone, moving on.");
			return null;
		}

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}

		boolean scythe = isScytheEquipped();
		LinkedHashMap<Long, Coord2d> snapshot;
		if (scythe) {
			snapshot = snapshotNearbyCrops(cropRes, cropPos, SCYTHE_RADIUS);
		} else {
			snapshot = new LinkedHashMap<>();
			snapshot.put(crop.id, new Coord2d(cropPos.x, cropPos.y));
		}

		setStatus("Harvesting " + cropBaseName + (scythe ? " (scythe)..." : "..."));
		FlowerMenu.setNextSelection("Harvest");
		gui.map.wdgmsg("click", Coord.z, cropPos.floor(posres), 3, 0, 0, (int) crop.id, cropPos.floor(posres), 0, -1);
		waitForProgressBar(30000);

		List<Coord2d> harvested = findHarvestedPositions(snapshot);
		if (harvested.isEmpty()) {
			FlowerMenu.setNextSelection(null);
			blacklistedCrops.add(crop.id);
			setStatus("Harvest failed for " + cropBaseName + ", skipping.");
			return null;
		}

		return harvested;
	}

	private List<Gob> findAllMatureCrops() {
		List<Gob> crops = new ArrayList<>();
		Gob player = gui.map.player();
		if (player == null) return crops;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		// Snapshot positions for safe sorting outside the sync block
		Map<Long, Double> distMap = new HashMap<>();

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					if (blacklistedCrops.contains(gob.id)) continue;
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.startsWith(CROP_PREFIX)) continue;
					if (!Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")) continue;
					if (!GobHelper.isMature(gob)) continue;

					double dist = new Coord2d(gob.rc.x, gob.rc.y).dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					crops.add(gob);
					distMap.put(gob.id, dist);
				} catch (Loading | NullPointerException ignored) {
				}
			}
		}
		// Sort by snapshotted distance — safe outside synchronized block
		crops.sort((a, b) -> Double.compare(
				distMap.getOrDefault(a.id, Double.MAX_VALUE),
				distMap.getOrDefault(b.id, Double.MAX_VALUE)));
		return crops;
	}

	/**
	 * Find the best item to plant for this crop type.
	 * Checks seeds first (CROP_TO_SEED), then items (CROP_TO_ITEM for crops
	 * planted as themselves like beetroot/leek/onion), then falls back to
	 * seed-<name> pattern.
	 */
	private WItem findPlantItem(String cropBaseName) {
		// Try seed first
		String seedResName = CROP_TO_SEED.get(cropBaseName);
		if (seedResName != null) {
			WItem seed = findBestItemByRes(seedResName);
			if (seed != null) return seed;
		}

		// Try item-as-seed (beetroot, leek, onion, etc.)
		String itemResName = CROP_TO_ITEM.get(cropBaseName);
		if (itemResName != null) {
			WItem item = findBestItemByRes(itemResName);
			if (item != null) return item;
		}

		// Fallback: try generic seed-<name> pattern
		if (seedResName == null) {
			String fallback = SEED_PREFIX + cropBaseName;
			WItem seed = findBestItemByRes(fallback);
			if (seed != null) return seed;
		}

		return null;
	}

	/**
	 * Find the best quality item matching a resource name.
	 * Searches main inventory and item stacks, deduplicating results.
	 */
	private WItem findBestItemByRes(String resName) {
		Set<GItem> seen = new HashSet<>();
		List<WItem> found = new ArrayList<>();

		if (gui.maininv == null) return null;
		// Search main inventory via wmap (ConcurrentHashMap — thread-safe)
		for (WItem wi : gui.maininv.wmap.values()) {
			try {
				Resource res = wi.item.getres();
				if (res != null && res.name.equals(resName) && seen.add(wi.item)) {
					found.add(wi);
				}
			} catch (Loading | NullPointerException ignored) {}
		}

		// Also search inside stacks (seeds may be stacked), deduplicating
		try {
			for (WItem wi : gui.getAllContentsWindows()) {
				try {
					Resource res = wi.item.getres();
					if (res != null && res.name.equals(resName) && seen.add(wi.item)) {
						found.add(wi);
					}
				} catch (Loading | NullPointerException ignored) {}
			}
		} catch (Exception ignored) {}

		if (found.isEmpty()) return null;

		if (useBestSeedCheckBox.a) {
			found.sort((a, b) -> Double.compare(b.quality(), a.quality()));
		}

		return found.get(0);
	}

	@Override
	protected void setStatus(String status) {
		if (statusLabel != null)
			statusLabel.settext("Status: " + status);
	}

	@Override
	protected String windowPrefKey() {
		return "wndc-farmingBotWindow";
	}

	@Override
	protected void onCleanup() {
		gui.farmingBot = null;
	}
}
