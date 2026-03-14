package haven.automated;

import haven.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static haven.OCache.posres;

public class OreSmeltingBot extends BotBase {
	private Label smelterCountLabel;

	private volatile boolean doCopperOre;
	private volatile boolean doTinOre;
	private volatile boolean doIronOre;
	private volatile boolean doGoldOre;
	private volatile boolean doSilverOre;
	private volatile boolean doLeadOre;

	private volatile boolean doCollectOutput;
	private volatile boolean doGrabFromStockpiles;
	private volatile int fuelPerLoad;

	// Track smelters that are already lit/processing to skip them
	private final Set<Long> processedSmelters = ConcurrentHashMap.newKeySet();

	private static final String SMELTER_RES = "gfx/terobjs/smelter";
	private static final String STOCKPILE_PREFIX = "gfx/terobjs/stockpile-";

	// Ore basenames grouped by output metal — matched via resource().basename()
	private static final Set<String> COPPER_ORES = new HashSet<>(Arrays.asList(
		"chalcopyrite", "malachite", "peacockore", "cuprite"
	));
	private static final Set<String> TIN_ORES = new HashSet<>(Arrays.asList("cassiterite"));
	private static final Set<String> IRON_ORES = new HashSet<>(Arrays.asList(
		"ilmenite", "limonite", "hematite", "magnetite"
	));
	private static final Set<String> GOLD_ORES = new HashSet<>(Arrays.asList(
		"petzite", "sylvanite", "nagyagite"
	));
	private static final Set<String> SILVER_ORES = new HashSet<>(Arrays.asList(
		"argentite", "hornsilver", "galena"
	));
	private static final Set<String> LEAD_ORES = new HashSet<>(Arrays.asList(
		"leadglance", "galena"
	));

	// All ore basenames combined for stockpile matching
	private static final Set<String> ALL_ORE_BASENAMES = new HashSet<>();
	static {
		ALL_ORE_BASENAMES.addAll(COPPER_ORES);
		ALL_ORE_BASENAMES.addAll(TIN_ORES);
		ALL_ORE_BASENAMES.addAll(IRON_ORES);
		ALL_ORE_BASENAMES.addAll(GOLD_ORES);
		ALL_ORE_BASENAMES.addAll(SILVER_ORES);
		ALL_ORE_BASENAMES.addAll(LEAD_ORES);
	}

	private static final String[] FUEL_NAMES = {"Coal", "Black Coal", "Charcoal"};
	private static final String[] FUEL_STOCKPILE_NAMES = {"coal", "charcoal", "blackcoal"};
	private static final double MAX_INTERACT_DIST = 11 * 5;
	private static final int TRANSFER_DELAY = 200;

	public OreSmeltingBot(GameUI gui) {
		super(gui, UI.scale(250, 300), "Ore Smelting Bot");

		doCopperOre = Utils.getprefb("oreSmeltingBot_copper", true);
		doTinOre = Utils.getprefb("oreSmeltingBot_tin", true);
		doIronOre = Utils.getprefb("oreSmeltingBot_iron", true);
		doGoldOre = Utils.getprefb("oreSmeltingBot_gold", false);
		doSilverOre = Utils.getprefb("oreSmeltingBot_silver", false);
		doLeadOre = Utils.getprefb("oreSmeltingBot_lead", false);
		doCollectOutput = Utils.getprefb("oreSmeltingBot_collect", true);
		doGrabFromStockpiles = Utils.getprefb("oreSmeltingBot_stockpiles", false);
		fuelPerLoad = Utils.getprefi("oreSmeltingBot_fuelCount", 12);

		int y = 10;
		add(new CheckBox("Copper") {{ a = doCopperOre; } public void set(boolean val) { doCopperOre = val; a = val; Utils.setprefb("oreSmeltingBot_copper", val); }}, UI.scale(10, y));
		add(new CheckBox("Gold") {{ a = doGoldOre; } public void set(boolean val) { doGoldOre = val; a = val; Utils.setprefb("oreSmeltingBot_gold", val); }}, UI.scale(130, y));
		y += 20;
		add(new CheckBox("Tin") {{ a = doTinOre; } public void set(boolean val) { doTinOre = val; a = val; Utils.setprefb("oreSmeltingBot_tin", val); }}, UI.scale(10, y));
		add(new CheckBox("Silver") {{ a = doSilverOre; } public void set(boolean val) { doSilverOre = val; a = val; Utils.setprefb("oreSmeltingBot_silver", val); }}, UI.scale(130, y));
		y += 20;
		add(new CheckBox("Iron") {{ a = doIronOre; } public void set(boolean val) { doIronOre = val; a = val; Utils.setprefb("oreSmeltingBot_iron", val); }}, UI.scale(10, y));
		add(new CheckBox("Lead") {{ a = doLeadOre; } public void set(boolean val) { doLeadOre = val; a = val; Utils.setprefb("oreSmeltingBot_lead", val); }}, UI.scale(130, y));
		y += 25;
		add(new CheckBox("Collect output bars") {{ a = doCollectOutput; } public void set(boolean val) { doCollectOutput = val; a = val; Utils.setprefb("oreSmeltingBot_collect", val); }}, UI.scale(10, y));
		y += 20;
		add(new CheckBox("Grab from stockpiles") {{ a = doGrabFromStockpiles; } public void set(boolean val) { doGrabFromStockpiles = val; a = val; Utils.setprefb("oreSmeltingBot_stockpiles", val); }}, UI.scale(10, y));
		y += 25;

		Label fuelLabel = new Label("Fuel per smelter:");
		fuelLabel.tooltip = RichText.render("Minimum 12 coal/charcoal per load (~11.6 consumed).\nMiner Credo may reduce to 9 (-25% fuel).", UI.scale(300));
		add(fuelLabel, UI.scale(10, y));
		TextEntry fuelEntry = new TextEntry(UI.scale(30), String.valueOf(fuelPerLoad)) {
			@Override
			public boolean keydown(KeyDownEvent ev) {
				boolean ret = super.keydown(ev);
				try { int val = Integer.parseInt(text()); if (val > 0 && val <= 20) { fuelPerLoad = val; Utils.setprefi("oreSmeltingBot_fuelCount", val); } } catch (NumberFormatException ignored) {}
				return ret;
			}
		};
		add(fuelEntry, UI.scale(120, y - 2));
		y += 25;

		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, y));
		y += 15;
		smelterCountLabel = new Label("");
		add(smelterCountLabel, UI.scale(10, y));
		y += 20;

		activeButton = new Button(UI.scale(80), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					processedSmelters.clear();
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
		if (!hasAnyOreSelected()) { Thread.sleep(500); return; }
		if (!checkVitals()) return;
		GameUI.Progress p = gui.prog;
		if (p != null) { setStatus("Working..."); waitForProgressBar(10000); return; }
		if (gui.vhand != null) { gui.vhand.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); }

		// Step 1: Ensure we have ore — fetch from stockpiles if needed
		if (findOreInInventory() == null) {
			if (doGrabFromStockpiles) {
				setStatus("Fetching ore from stockpile...");
				if (!grabFromStockpile(true)) {
					setStatus("No ore found in inventory or stockpiles");
					deactivate();
					return;
				}
			} else {
				Gob smelter = findNearestUnprocessedSmelter();
				if (smelter != null && doCollectOutput) { setStatus("No ore. Collecting output..."); collectOutputFromSmelter(smelter); }
				else { setStatus("No ore in inventory"); deactivate(); }
				return;
			}
		}

		// Step 2: Ensure we have fuel — fetch from stockpiles if needed
		if (findFuelInInventory() == null) {
			if (doGrabFromStockpiles) {
				setStatus("Fetching fuel from stockpile...");
				if (!grabFromStockpile(false)) {
					setStatus("No fuel found in inventory or stockpiles");
					deactivate();
					return;
				}
			} else {
				setStatus("No fuel (Coal/Charcoal) in inventory");
				deactivate();
				return;
			}
		}

		// Step 3: Find and process next smelter
		Gob smelter = findNearestUnprocessedSmelter();
		if (smelter == null) {
			setStatus("No available smelters nearby (" + processedSmelters.size() + " done)");
			smelterCountLabel.settext("");
			deactivate();
			return;
		}
		smelterCountLabel.settext("Smelters: " + countSmelters() + " total, " + processedSmelters.size() + " done");

		processSmelter(smelter);
	}

	private void processSmelter(Gob smelter) throws InterruptedException {
		setStatus("Walking to smelter...");
		gui.map.pfLeftClick(smelter.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) { Actions.unstuck(gui); return; }
		Gob player = gui.map.player();
		if (player == null) return;
		if (new Coord2d(smelter.rc.x, smelter.rc.y).dist(new Coord2d(player.rc.x, player.rc.y)) > MAX_INTERACT_DIST) {
			setStatus("Too far from smelter, retrying...");
			return;
		}

		// Clear hand before interacting
		if (gui.vhand != null) { gui.vhand.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); }

		// Open smelter UI by right-clicking it
		setStatus("Opening smelter...");
		Coord2d smelterPos = new Coord2d(smelter.rc.x, smelter.rc.y);
		gui.map.wdgmsg("click", Coord.z, smelterPos.floor(posres), 3, 0, 0, (int) smelter.id, smelterPos.floor(posres), 0, -1);

		Inventory smelterInv = waitForContainerWindow("Smelter", 5000);
		if (smelterInv == null) { setStatus("Could not open smelter"); return; }

		// Load ore via shift-click (transfer) — wait for each item to actually move
		setStatus("Loading ore...");
		int oreLoaded = transferItemsToContainer(true);
		setStatus("Loaded " + oreLoaded + " ore");
		Thread.sleep(200);

		// Load fuel via shift-click (transfer)
		setStatus("Loading fuel...");
		int fuelLoaded = transferItemsToContainer(false);
		setStatus("Loaded " + fuelLoaded + " fuel");
		Thread.sleep(200);

		// Light smelter
		setStatus("Lighting smelter...");
		FlowerMenu.setNextSelection("Light");
		gui.map.wdgmsg("click", Coord.z, smelterPos.floor(posres), 3, 0, 0, (int) smelter.id, smelterPos.floor(posres), 0, -1);
		waitForProgressBar(5000);
		FlowerMenu.setNextSelection(null);

		// Mark this smelter as processed so we move to the next one
		processedSmelters.add(smelter.id);
		setStatus("Smelter lit! Moving to next...");
		Thread.sleep(500);
	}

	/** Wait for a container window with the given name to appear. */
	private Inventory waitForContainerWindow(String name, int timeoutMs) throws InterruptedException {
		int waited = 0;
		while (waited < timeoutMs) {
			for (Inventory inv : gui.getAllInventories()) {
				if (inv == gui.maininv) continue;
				if (inv.parent instanceof Window) {
					String cap = ((Window) inv.parent).cap;
					if (cap != null && cap.toLowerCase().contains(name.toLowerCase())) return inv;
				}
			}
			Thread.sleep(100);
			waited += 100;
		}
		return null;
	}

	/**
	 * Transfer items from player inventory to the currently open container.
	 * @param ore true = transfer matching ore, false = transfer fuel
	 * @return number of items transferred
	 */
	private int transferItemsToContainer(boolean ore) throws InterruptedException {
		int loaded = 0;
		int maxItems = ore ? 999 : fuelPerLoad;
		for (int i = 0; i < maxItems && active && !stop; i++) {
			WItem item = ore ? findOreInInventory() : findFuelInInventory();
			if (item == null) break;
			int beforeCount = countInventoryItems();
			try {
				item.item.wdgmsg("transfer", Coord.z);
			} catch (Exception ignored) { continue; }
			// Wait for the item to actually leave the inventory
			int waited = 0;
			while (waited < 2000) {
				Thread.sleep(50);
				waited += 50;
				if (countInventoryItems() < beforeCount) break;
			}
			if (countInventoryItems() < beforeCount) {
				loaded++;
			} else {
				// Item didn't transfer — smelter might be full
				break;
			}
		}
		return loaded;
	}

	/** Count total items in player main inventory. */
	private int countInventoryItems() {
		try { return gui.maininv.getAllItems().size(); } catch (Exception e) { return 0; }
	}

	/** Grab items from a nearby stockpile. @param ore true=ore stockpile, false=fuel stockpile */
	private boolean grabFromStockpile(boolean ore) throws InterruptedException {
		Gob stockpile = findNearestMatchingStockpile(ore);
		if (stockpile == null) return false;

		setStatus("Walking to " + (ore ? "ore" : "fuel") + " stockpile...");
		gui.map.pfLeftClick(stockpile.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) { Actions.unstuck(gui); return false; }

		Gob player = gui.map.player();
		if (player == null) return false;
		if (new Coord2d(stockpile.rc.x, stockpile.rc.y).dist(new Coord2d(player.rc.x, player.rc.y)) > MAX_INTERACT_DIST) return false;

		// Clear hand
		if (gui.vhand != null) { gui.vhand.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); }

		// Right-click stockpile to open it
		Coord2d spPos = new Coord2d(stockpile.rc.x, stockpile.rc.y);
		gui.map.wdgmsg("click", Coord.z, spPos.floor(posres), 3, 0, 0, (int) stockpile.id, spPos.floor(posres), 0, -1);
		Inventory spInv = waitForContainerWindow("Stockpile", 5000);
		if (spInv == null) { setStatus("Could not open stockpile"); return false; }

		// Transfer items from stockpile to player inventory
		setStatus("Grabbing from stockpile...");
		int grabbed = 0;
		for (WItem item : new ArrayList<>(spInv.getAllItems())) {
			if (stop || !active) break;
			if (gui.maininv.getFreeSpace() < 2) break;
			try {
				item.item.wdgmsg("transfer", Coord.z);
				grabbed++;
				Thread.sleep(TRANSFER_DELAY);
			} catch (Exception ignored) {}
		}
		setStatus("Grabbed " + grabbed + " items from stockpile");
		Thread.sleep(200);
		return grabbed > 0;
	}

	/** Find nearest ore or fuel stockpile. Matches by stockpile resource name suffix. */
	private Gob findNearestMatchingStockpile(boolean ore) {
		return GobHelper.findNearest(gui, MAX_SEARCH_DIST, g -> {
			try {
				Resource res = g.getres();
				if (res == null || !res.name.startsWith(STOCKPILE_PREFIX)) return false;
				String spType = res.name.substring(STOCKPILE_PREFIX.length());
				if (ore) {
					// Match exact ore basename or partial match (stockpile names may vary)
					if (ALL_ORE_BASENAMES.contains(spType) && isOreEnabled(spType)) return true;
					// Fallback: check if any enabled ore basename is contained in the stockpile name
					for (String oreName : ALL_ORE_BASENAMES) {
						if (isOreEnabled(oreName) && spType.contains(oreName)) return true;
					}
					return false;
				} else {
					for (String fuelName : FUEL_STOCKPILE_NAMES) {
						if (spType.equals(fuelName) || spType.contains(fuelName)) return true;
					}
					return false;
				}
			} catch (Loading | NullPointerException ignored) { return false; }
		});
	}

	private void collectOutputFromSmelter(Gob smelter) throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) return;
		if (new Coord2d(smelter.rc.x, smelter.rc.y).dist(new Coord2d(player.rc.x, player.rc.y)) > MAX_INTERACT_DIST) {
			gui.map.pfLeftClick(smelter.rc.floor().add(2, 0), null);
			if (!Actions.waitPf(gui)) return;
		}
		gui.map.wdgmsg("click", Coord.z, smelter.rc.floor(posres), 3, 0, 0, (int) smelter.id, smelter.rc.floor(posres), 0, -1);
		Inventory smelterInv = waitForContainerWindow("Smelter", 5000);
		if (smelterInv == null) return;
		for (WItem item : new ArrayList<>(smelterInv.getAllItems())) {
			if (stop || !active) break;
			try { item.item.wdgmsg("transfer", Coord.z); Thread.sleep(100); } catch (Exception ignored) {}
		}
		Thread.sleep(100);
	}

	/** Find nearest smelter that hasn't been processed yet. */
	private Gob findNearestUnprocessedSmelter() {
		Gob closest = null;
		Gob player = gui.map.player();
		if (player == null) return null;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		double closestDist = Double.MAX_VALUE;
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res == null || !res.name.equals(SMELTER_RES)) continue;
					if (processedSmelters.contains(gob.id)) continue;
					double dist = gob.rc.dist(playerPos);
					if (dist > MAX_SEARCH_DIST) continue;
					if (dist < closestDist) { closestDist = dist; closest = gob; }
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return closest;
	}

	private int countSmelters() {
		int count = 0;
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					Resource res = gob.getres();
					if (res != null && res.name.equals(SMELTER_RES)) count++;
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return count;
	}

	private WItem findOreInInventory() {
		for (WItem wi : InvHelper.getAllItemsExcludeBeltKeyring(gui)) {
			try { if (isOreEnabled(wi.item.resource().basename())) return wi; } catch (Loading | NullPointerException ignored) {}
		}
		return null;
	}

	private WItem findFuelInInventory() {
		for (WItem wi : InvHelper.getAllItemsExcludeBeltKeyring(gui)) {
			try {
				String name = wi.item.getname();
				if (name != null) { for (String fuelName : FUEL_NAMES) { if (name.contains(fuelName)) return wi; } }
			} catch (Loading ignored) {}
		}
		return null;
	}

	private boolean isOreEnabled(String basename) {
		if (basename == null) return false;
		if (doCopperOre && COPPER_ORES.contains(basename)) return true;
		if (doTinOre && TIN_ORES.contains(basename)) return true;
		if (doIronOre && IRON_ORES.contains(basename)) return true;
		if (doGoldOre && GOLD_ORES.contains(basename)) return true;
		if (doSilverOre && SILVER_ORES.contains(basename)) return true;
		if (doLeadOre && LEAD_ORES.contains(basename)) return true;
		return false;
	}

	private boolean hasAnyOreSelected() { return doCopperOre || doTinOre || doIronOre || doGoldOre || doSilverOre || doLeadOre; }

	@Override protected String windowPrefKey() { return "wndc-oreSmeltingBotWindow"; }
	@Override protected void onCleanup() { gui.oreSmeltingBot = null; }
}
