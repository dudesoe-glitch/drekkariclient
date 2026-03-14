package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class FarmingBot extends BotBase {
	private CheckBox harvestCheckBox;
	private CheckBox replantCheckBox;
	private CheckBox useBestSeedCheckBox;
	private boolean frozenWarningShown = false;
	private int plantFailCount = 0;
	private final Set<Long> blacklistedCrops = new HashSet<>();

	private static final String CROP_PREFIX = "gfx/terobjs/plants/";
	private static final String SEED_PREFIX = "gfx/invobjs/seed-";
	private static final int WINTER = 3;
	private static final double SCYTHE_RADIUS = 40.0; // 2x3 tiles diagonal (~39.6 units) — wiki: "2x3 tiles in front of character"
	private static final String SCYTHE_RES = "scythe";
	private static final int HARVEST_VERIFY_TIMEOUT = 3000; // ms to wait for crop gob removal
	private static final int HARVEST_VERIFY_INTERVAL = 50; // ms between checks

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
		if (!frozenWarningShown && isWinter()) {
			frozenWarningShown = true;
			gui.errorsilent("Warning: Ground is frozen in winter! Replanting disabled.");
			replantCheckBox.a = false;
		} else if (frozenWarningShown && !isWinter()) {
			frozenWarningShown = false;
			plantFailCount = 0;
		}
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

		Gob crop = findMatureCrop();
		if (crop != null) {
			processHarvest(crop);
		} else {
			if (!blacklistedCrops.isEmpty()) {
				// Clear blacklist and try once more before giving up
				blacklistedCrops.clear();
				crop = findMatureCrop();
				if (crop != null) {
					processHarvest(crop);
					return;
				}
			}
			setStatus("No mature crops found.");
			deactivate();
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

		int total = positions.size();
		for (int i = 0; i < total; i++) {
			if (stop) return;
			Coord2d pos = positions.get(i);

			WItem plantItem = findPlantItem(cropBaseName);
			if (plantItem == null) {
				setStatus("Out of " + cropBaseName + " seeds (" + i + "/" + total + " replanted)");
				return;
			}

			setStatus("Replanting " + cropBaseName + " " + (i + 1) + "/" + total + "...");
			plantItem.item.wdgmsg("take", Coord.z);
			if (!Actions.waitForOccupiedHand(gui, 2000, "")) {
				return;
			}
			if (stop) return;

			gui.map.wdgmsg("itemact", Coord.z, pos.floor(posres), 0);
			Thread.sleep(50);
			Actions.waitProgBar(gui);
			if (stop) return;

			if (gui.vhand != null) {
				plantFailCount++;
				// Transfer seed back to inventory instead of dropping on ground
				gui.vhand.item.wdgmsg("transfer", Coord.z);
				Actions.waitForEmptyHand(gui, 1000, "");
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

	private void processHarvest(Gob crop) throws InterruptedException {
		if (crop == null) return;

		String cropRes;
		try {
			Resource res = crop.getres();
			if (res == null) return;
			cropRes = res.name;
		} catch (Loading l) {
			return;
		}
		String cropBaseName = cropRes.substring(cropRes.lastIndexOf('/') + 1);
		// Defensive copy — Coord2d is mutable
		Coord2d cropPos = new Coord2d(crop.rc.x, crop.rc.y);

		setStatus("Walking to " + cropBaseName + "...");
		gui.map.pfLeftClick(cropPos.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			// Blacklist this crop to avoid retry loop on unreachable crops
			blacklistedCrops.add(crop.id);
			Actions.unstuck(gui);
			return;
		}

		Gob player = gui.map.player();
		if (player == null) return;
		if (cropPos.dist(player.rc) > 11 * 5) {
			blacklistedCrops.add(crop.id);
			setStatus("Too far from " + cropBaseName + ", skipping.");
			return;
		}

		// Verify crop still exists (may have been harvested by another player)
		if (gui.map.glob.oc.getgob(crop.id) == null) {
			setStatus(cropBaseName + " already gone, moving on.");
			return;
		}

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}

		// Snapshot before harvest
		boolean scythe = isScytheEquipped();
		LinkedHashMap<Long, Coord2d> snapshot;
		if (scythe) {
			snapshot = snapshotNearbyCrops(cropRes, cropPos, SCYTHE_RADIUS);
		} else {
			snapshot = new LinkedHashMap<>();
			snapshot.put(crop.id, new Coord2d(cropPos.x, cropPos.y));
		}

		// Harvest
		setStatus("Harvesting " + cropBaseName + (scythe ? " (scythe)..." : "..."));
		FlowerMenu.setNextSelection("Harvest");
		gui.map.wdgmsg("click", Coord.z, crop.rc.floor(posres), 3, 0, 0, (int) crop.id, crop.rc.floor(posres), 0, -1);
		Thread.sleep(50);
		Actions.waitProgBar(gui);

		// Verify harvest succeeded by checking gob removal (with timeout)
		List<Coord2d> harvested = findHarvestedPositions(snapshot);
		if (harvested.isEmpty()) {
			// Harvest failed — crop still there (another player got it, or FlowerMenu didn't appear)
			// Clear any unconsumed FlowerMenu selection
			FlowerMenu.setNextSelection(null);
			blacklistedCrops.add(crop.id);
			setStatus("Harvest failed for " + cropBaseName + ", skipping.");
			return;
		}

		// Replant harvested positions
		if (replantCheckBox.a && !NO_REPLANT_CROPS.contains(cropBaseName)) {
			replantPositions(harvested, cropBaseName);
		}
	}

	private Gob findMatureCrop() {
		Gob closest = null;
		double closestDist = Double.MAX_VALUE;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = player.rc;

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					if (blacklistedCrops.contains(gob.id)) continue;
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.startsWith(CROP_PREFIX)) continue;
					if (!Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")) continue;
					if (!GobHelper.isMature(gob)) continue;

					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (dist < closestDist) {
						closestDist = dist;
						closest = gob;
					}
				} catch (Loading | NullPointerException ignored) {
				}
			}
		}
		return closest;
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
