package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class FarmingBot extends BotBase {
	private CheckBox harvestCheckBox;
	private CheckBox replantCheckBox;
	private CheckBox useBestSeedCheckBox;

	private static final String CROP_PREFIX = "gfx/terobjs/plants/";
	private static final String SEED_PREFIX = "gfx/invobjs/seed-";

	private static final Map<String, String> CROP_TO_SEED = new HashMap<String, String>() {{
		put("carrot", "gfx/invobjs/seed-carrot");
		put("flax", "gfx/invobjs/seed-flax");
		put("hemp", "gfx/invobjs/seed-hemp");
		put("leek", "gfx/invobjs/seed-leek");
		put("lettuce", "gfx/invobjs/seed-lettuce");
		put("onion", "gfx/invobjs/seed-onion");
		put("peas", "gfx/invobjs/seed-peas");
		put("pipeweed", "gfx/invobjs/seed-pipeweed");
		put("poppy", "gfx/invobjs/seed-poppy");
		put("pumpkin", "gfx/invobjs/seed-pumpkin");
		put("turnip", "gfx/invobjs/seed-turnip");
		put("wheat", "gfx/invobjs/seed-wheat");
		put("beetroot", "gfx/invobjs/seed-beetroot");
		put("yellowonion", "gfx/invobjs/seed-yellowonion");
		put("barley", "gfx/invobjs/seed-barley");
		put("millet", "gfx/invobjs/seed-millet");
		put("wine", "gfx/invobjs/seed-grape");
		put("grape", "gfx/invobjs/seed-grape");
		put("hops", "gfx/invobjs/seed-hops");
		put("pepper", "gfx/invobjs/seed-pepper");
		put("tea", "gfx/invobjs/seed-tea");
		put("cucumber", "gfx/invobjs/seed-cucumber");
	}};

	public FarmingBot(GameUI gui) {
		super(gui, UI.scale(220, 130), "Farming Bot");

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
					this.change("Stop");
				} else {
					idlePlayer();
					this.change("Start");
				}
			}
		};
		add(activeButton, UI.scale(120, y - 25));
	}

	@Override
	protected void tick() throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) {
			Thread.sleep(500);
			return;
		}
		if (!checkVitals()) return;

		Gob crop = findMatureCrop();
		if (crop != null) {
			processCrop(crop);
		} else {
			setStatus("No mature crops found.");
			deactivate();
		}
		Thread.sleep(1000);
	}

	private void processCrop(Gob crop) throws InterruptedException {
		if (crop == null) return;

		String cropRes = null;
		try {
			cropRes = crop.getres().name;
		} catch (Loading l) {
			return;
		}
		String cropBaseName = cropRes.substring(cropRes.lastIndexOf('/') + 1);
		Coord2d cropPos = crop.rc;

		setStatus("Walking to " + cropBaseName + "...");
		gui.map.pfLeftClick(crop.rc.floor().add(2, 0), null);
		if (!AUtils.waitPf(gui)) {
			AUtils.unstuck(gui);
			return;
		}

		if (crop.rc.dist(gui.map.player().rc) > 11 * 5) {
			setStatus("Too far from crop, skipping.");
			return;
		}

		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			AUtils.waitForEmptyHand(gui, 1000, "Farming Bot: Couldn't clear hand");
		}

		if (harvestCheckBox.a) {
			setStatus("Harvesting " + cropBaseName + "...");
			FlowerMenu.setNextSelection("Harvest");
			gui.map.wdgmsg("click", Coord.z, crop.rc.floor(posres), 3, 0, 0, (int) crop.id, crop.rc.floor(posres), 0, -1);
			Thread.sleep(300);
			AUtils.waitProgBar(gui);
			Thread.sleep(500);
		}

		if (replantCheckBox.a) {
			setStatus("Replanting " + cropBaseName + "...");
			WItem seed = findBestSeed(cropBaseName);
			if (seed != null) {
				seed.item.wdgmsg("take", Coord.z);
				if (!AUtils.waitForOccupiedHand(gui, 2000, "Farming Bot: Couldn't pick up seed")) {
					return;
				}
				Thread.sleep(100);
				gui.map.wdgmsg("itemact", Coord.z, cropPos.floor(posres), 0);
				Thread.sleep(500);
				AUtils.waitProgBar(gui);
				Thread.sleep(300);
				if (gui.vhand != null) {
					gui.vhand.item.wdgmsg("drop", Coord.z);
					AUtils.waitForEmptyHand(gui, 1000, "");
				}
			} else {
				setStatus("No seeds for " + cropBaseName);
				Thread.sleep(1000);
			}
		}
	}

	private Gob findMatureCrop() {
		Gob closest = null;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = player.rc;

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.startsWith(CROP_PREFIX)) continue;
					if (!Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")) continue;
					if (!GobHelper.isMature(gob)) continue;

					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (closest == null || dist < closest.rc.dist(playerPos)) {
						closest = gob;
					}
				} catch (Loading | NullPointerException ignored) {
				}
			}
		}
		return closest;
	}

	private WItem findBestSeed(String cropBaseName) {
		String seedResName = CROP_TO_SEED.get(cropBaseName);
		if (seedResName == null) {
			seedResName = SEED_PREFIX + cropBaseName;
		}

		List<WItem> seeds = new ArrayList<>();
		Inventory inv = gui.maininv;

		for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				WItem wi = (WItem) wdg;
				try {
					if (wi.item.getres().name.equals(seedResName)) {
						seeds.add(wi);
					}
				} catch (Loading ignored) {
				}
			}
		}

		if (seeds.isEmpty()) return null;

		if (useBestSeedCheckBox.a) {
			seeds.sort((a, b) -> {
				double qa = a.quality();
				double qb = b.quality();
				return Double.compare(qb, qa);
			});
		}

		return seeds.get(0);
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
