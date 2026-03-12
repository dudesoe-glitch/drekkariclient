package haven.automated;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;

import java.util.*;

import static haven.OCache.posres;

public class FarmingBot extends Window implements Runnable {
	private final GameUI gui;
	public volatile boolean stop;
	private volatile boolean active;
	private Button activeButton;
	private Label statusLabel;
	private CheckBox harvestCheckBox;
	private CheckBox replantCheckBox;
	private CheckBox useBestSeedCheckBox;

	private static final double MAX_SEARCH_DIST = 550.0; // ~50 tiles

	// Crop resource name prefix
	private static final String CROP_PREFIX = "gfx/terobjs/plants/";
	private static final String TRELLIS_PREFIX = "gfx/terobjs/plants/"; // trellis plants are also under plants/
	private static final String SEED_PREFIX = "gfx/invobjs/seed-";

	// Map crop resource base name to seed resource name
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
		super(UI.scale(220, 130), "Farming Bot");
		this.gui = gui;
		this.stop = false;
		this.active = false;

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
					Gob player = ui.gui.map.player();
					if (player != null)
						ui.gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 1, 0);
					this.change("Start");
				}
			}
		};
		add(activeButton, UI.scale(120, y - 25));
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				if (active) {
					Gob player = gui.map.player();
					if (player == null) {
						Thread.sleep(500);
						continue;
					}

					// HP check
					if (gui.getmeters("hp").get(1).a < 0.02) {
						setStatus("Low HP! Hearthing...");
						gui.act("travel", "hearth");
						Thread.sleep(8000);
						continue;
					}
					// Energy check
					if (ui.gui.getmeter("nrj", 0).a < 0.25) {
						gui.error("Farming Bot: Low on energy, stopping.");
						stopBot();
						Thread.sleep(2000);
						continue;
					}
					// Stamina check
					if (gui.getmeter("stam", 0).a < 0.40) {
						setStatus("Drinking...");
						try {
							AUtils.drinkTillFull(gui, 0.99, 0.99);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
					// Inventory full check
					if (gui.maininv.getFreeSpace() < 2) {
						gui.error("Farming Bot: Inventory full, stopping.");
						stopBot();
						Thread.sleep(2000);
						continue;
					}

					Gob crop = findMatureCrop();
					if (crop != null) {
						processCrop(crop);
					} else {
						setStatus("No mature crops found.");
						active = false;
						activeButton.change("Start");
					}
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void processCrop(Gob crop) throws InterruptedException {
		if (crop == null) return;

		// Remember crop type for replanting
		String cropRes = null;
		try {
			cropRes = crop.getres().name;
		} catch (Loading l) {
			return;
		}
		String cropBaseName = cropRes.substring(cropRes.lastIndexOf('/') + 1);
		Coord2d cropPos = crop.rc;

		// Walk to the crop
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

		// Clear hand
		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			AUtils.waitForEmptyHand(gui, 1000, "Farming Bot: Couldn't clear hand");
		}

		// Harvest
		if (harvestCheckBox.a) {
			setStatus("Harvesting " + cropBaseName + "...");
			FlowerMenu.setNextSelection("Harvest");
			gui.map.wdgmsg("click", Coord.z, crop.rc.floor(posres), 3, 0, 0, (int) crop.id, crop.rc.floor(posres), 0, -1);
			Thread.sleep(300);
			AUtils.waitProgBar(gui);
			Thread.sleep(500);
		}

		// Replant
		if (replantCheckBox.a) {
			setStatus("Replanting " + cropBaseName + "...");
			WItem seed = findBestSeed(cropBaseName);
			if (seed != null) {
				// Pick up the seed
				seed.item.wdgmsg("take", Coord.z);
				if (!AUtils.waitForOccupiedHand(gui, 2000, "Farming Bot: Couldn't pick up seed")) {
					return;
				}
				Thread.sleep(100);

				// Plant at the crop position (use item on ground)
				gui.map.wdgmsg("itemact", Coord.z, cropPos.floor(posres), 0);
				Thread.sleep(500);
				AUtils.waitProgBar(gui);
				Thread.sleep(300);

				// If hand still has item, put it back
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
		Coord2d playerPos = gui.map.player().rc;

		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null) continue;
					if (!res.name.startsWith(CROP_PREFIX)) continue;

					// Check if it's a growing plant at max stage
					if (!Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant")) continue;

					if (!isCropMature(gob)) continue;

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

	private boolean isCropMature(Gob gob) {
		try {
			// Get max growth stage from mesh layers
			int maxStage = 0;
			for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
				if (layer.id / 10 > maxStage) {
					maxStage = layer.id / 10;
				}
			}

			// Get current growth stage from drawable data
			Drawable dr = gob.getattr(Drawable.class);
			ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
			if (d == null) return false;

			Message data = d.sdt.clone();
			if (data == null) return false;

			int stage = data.uint8();
			if (stage > maxStage) stage = maxStage;

			return stage == maxStage;
		} catch (Exception e) {
			if (e instanceof InterruptedException) Thread.currentThread().interrupt();
			return false;
		}
	}

	private WItem findBestSeed(String cropBaseName) {
		String seedResName = CROP_TO_SEED.get(cropBaseName);
		if (seedResName == null) {
			// Try default pattern
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
			// Sort by quality descending, pick highest
			seeds.sort((a, b) -> {
				double qa = a.quality();
				double qb = b.quality();
				return Double.compare(qb, qa);
			});
		}

		return seeds.get(0);
	}

	private void setStatus(String status) {
		statusLabel.settext("Status: " + status);
	}

	private void stopBot() {
		active = false;
		activeButton.change("Start");
		setStatus("Stopped");
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && (Objects.equals(msg, "close"))) {
			stop = true;
			stopBot();
			reqdestroy();
			gui.farmingBot = null;
		} else
			super.wdgmsg(sender, msg, args);
	}

	public void stop() {
		Gob player = ui.gui.map.player();
		if (player != null)
			ui.gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 1, 0);
		if (ui.gui.map.pfthread != null) {
			ui.gui.map.pfthread.interrupt();
		}
		if (gui.farmingBotThread != null) {
			gui.farmingBotThread.interrupt();
			gui.farmingBotThread = null;
		}
		this.destroy();
	}

	@Override
	public void reqdestroy() {
		Utils.setprefc("wndc-farmingBotWindow", this.c);
		super.reqdestroy();
	}
}
