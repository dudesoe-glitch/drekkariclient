package haven.automated;

import haven.*;

import java.util.*;
import java.util.List;

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
	private int fuelPerLoad;

	private static final String SMELTER_RES = "gfx/terobjs/smelter";

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

	private static final String[] FUEL_NAMES = {"Coal", "Black Coal", "Charcoal"};
	private static final double MAX_INTERACT_DIST = 11 * 5;
	private static final int HAND_TIMEOUT = 2000;
	private static final int HAND_DELAY = 8;

	public OreSmeltingBot(GameUI gui) {
		super(gui, UI.scale(250, 260), "Ore Smelting Bot");

		doCopperOre = Utils.getprefb("oreSmeltingBot_copper", true);
		doTinOre = Utils.getprefb("oreSmeltingBot_tin", true);
		doIronOre = Utils.getprefb("oreSmeltingBot_iron", true);
		doGoldOre = Utils.getprefb("oreSmeltingBot_gold", false);
		doSilverOre = Utils.getprefb("oreSmeltingBot_silver", false);
		doLeadOre = Utils.getprefb("oreSmeltingBot_lead", false);
		doCollectOutput = Utils.getprefb("oreSmeltingBot_collect", true);
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
				if (active) { this.change("Stop"); statusLabel.settext("Running..."); }
				else { idlePlayer(); this.change("Start"); statusLabel.settext("Stopped"); }
			}
		};
		add(activeButton, UI.scale(80, y));
	}

	@Override
	protected void tick() throws InterruptedException {
		if (!hasAnyOreSelected()) { Thread.sleep(500); return; }
		if (!checkVitals()) return;
		if (gui.prog != null) { setStatus("Working..."); waitForProgressBar(10000); return; }

		Gob smelter = findNearestSmelter();
		if (smelter == null) { setStatus("No smelters found nearby"); smelterCountLabel.settext(""); deactivate(); return; }
		smelterCountLabel.settext("Smelters nearby: " + countSmelters());
		if (gui.vhand != null) { gui.vhand.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); }

		WItem oreItem = findOreInInventory();
		if (oreItem == null) {
			if (doCollectOutput) { setStatus("No ore found. Collecting output..."); collectOutputFromSmelter(smelter); }
			else { setStatus("No ore in inventory"); deactivate(); }
			return;
		}
		WItem fuelItem = findFuelInInventory();
		if (fuelItem == null) { setStatus("No fuel (Coal/Charcoal) in inventory"); deactivate(); return; }

		processSmelter(smelter);
	}

	private void processSmelter(Gob smelter) throws InterruptedException {
		setStatus("Walking to smelter...");
		gui.map.pfLeftClick(smelter.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) { Actions.unstuck(gui); return; }
		Gob player = gui.map.player();
		if (player == null) return;
		if (smelter.rc.dist(player.rc) > MAX_INTERACT_DIST) { setStatus("Too far from smelter, retrying..."); return; }

		setStatus("Loading ore...");
		int oreLoaded = loadItemsIntoSmelter(smelter);
		if (oreLoaded == 0) { setStatus("Failed to load ore"); return; }
		setStatus("Loaded " + oreLoaded + " ore"); Thread.sleep(50);

		setStatus("Loading fuel...");
		int fuelLoaded = loadFuelIntoSmelter(smelter);
		if (fuelLoaded == 0) { setStatus("Failed to load fuel"); return; }
		setStatus("Loaded " + fuelLoaded + " fuel"); Thread.sleep(50);

		setStatus("Lighting smelter...");
		FlowerMenu.setNextSelection("Light");
		gui.map.wdgmsg("click", Coord.z, smelter.rc.floor(posres), 3, 0, 0, (int) smelter.id, smelter.rc.floor(posres), 0, -1);
		Thread.sleep(500);

		setStatus("Smelting in progress...");
		int waitTime = 0;
		int maxWait = 3600000; // 60 minutes — smelting takes ~55 min per load
		while (waitTime < maxWait && active && !stop) {
			if (gui.prog != null) { waitForProgressBar(60000); }
			if (gui.getmeter("stam", 0).a < STAMINA_THRESHOLD) { Actions.drinkTillFull(gui, 0.99, 0.99); }
			try {
				ResDrawable rd = smelter.getattr(ResDrawable.class);
				if (rd != null && waitTime > 5000 && rd.sdt.checkrbuf(0) == 0) { setStatus("Smelting complete!"); break; }
			} catch (Exception ignored) {}
			Thread.sleep(2000); waitTime += 2000;
			setStatus("Smelting... " + (waitTime / 60000) + "m " + ((waitTime % 60000) / 1000) + "s");
		}
		Thread.sleep(200);
		if (doCollectOutput && active && !stop) { setStatus("Collecting output..."); collectOutputFromSmelter(smelter); }
	}

	private int loadItemsIntoSmelter(Gob smelter) throws InterruptedException {
		int loaded = 0;
		List<WItem> items = findAllOreInInventory();
		for (WItem witem : items) {
			if (stop || !active) break;
			try {
				GItem item = witem.item;
				item.wdgmsg("take", new Coord(item.sz.x / 2, item.sz.y / 2));
				if (!waitForHand(true)) continue;
				if (gui.vhand == null) continue;
				GItem handItem = gui.vhand.item;
				gui.map.wdgmsg("itemact", Coord.z, smelter.rc.floor(posres), 1, 0, (int) smelter.id, smelter.rc.floor(posres), 0, -1);
				Thread.sleep(300);
				int timeout = 0;
				while (timeout < HAND_TIMEOUT) {
					WItem handNow = gui.vhand;
					if (handNow == null) break;
					else if (handNow.item != handItem) { handNow.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); break; }
					timeout += HAND_DELAY; Thread.sleep(HAND_DELAY);
				}
				loaded++; Thread.sleep(100);
			} catch (Loading ignored) {}
		}
		if (gui.vhand != null) { gui.vhand.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); }
		return loaded;
	}

	private int loadFuelIntoSmelter(Gob smelter) throws InterruptedException {
		WItem fuelWItem = findFuelInInventory();
		if (fuelWItem == null) return 0;
		GItem fuel = fuelWItem.item;
		fuel.wdgmsg("take", new Coord(fuel.sz.x / 2, fuel.sz.y / 2));
		if (!waitForHand(true)) return 0;
		fuel = gui.vhand.item;
		int loaded = 0;
		for (int i = 0; i < fuelPerLoad && active && !stop; i++) {
			int modifier = (i < fuelPerLoad - 1) ? 1 : 0;
			gui.map.wdgmsg("itemact", Coord.z, smelter.rc.floor(posres), modifier, 0, (int) smelter.id, smelter.rc.floor(posres), 0, -1);
			int timeout = 0; boolean done = false;
			while (timeout < HAND_TIMEOUT) {
				WItem newFuel = gui.vhand;
				if (newFuel != null && newFuel.item != fuel) { fuel = newFuel.item; loaded++; break; }
				else if (newFuel == null) { loaded++; done = true; break; }
				timeout += HAND_DELAY; Thread.sleep(HAND_DELAY);
			}
			if (timeout >= HAND_TIMEOUT || done) break;
		}
		if (gui.vhand != null) { gui.vhand.item.wdgmsg("drop", Coord.z); Actions.waitForEmptyHand(gui, 1000, ""); }
		return loaded;
	}

	private void collectOutputFromSmelter(Gob smelter) throws InterruptedException {
		Gob player = gui.map.player();
		if (player == null) return;
		if (smelter.rc.dist(player.rc) > MAX_INTERACT_DIST) {
			gui.map.pfLeftClick(smelter.rc.floor().add(2, 0), null);
			if (!Actions.waitPf(gui)) return;
		}
		gui.map.wdgmsg("click", Coord.z, smelter.rc.floor(posres), 3, 0, 0, (int) smelter.id, smelter.rc.floor(posres), 0, -1);
		Thread.sleep(500);
		List<Inventory> allInvs = gui.getAllInventories();
		for (Inventory inv : allInvs) {
			if (inv == gui.maininv) continue;
			if (inv.parent instanceof Window) {
				String cap = ((Window) inv.parent).cap;
				if (cap != null && (cap.contains("Smelter") || cap.contains("smelter"))) {
					for (WItem item : inv.getAllItems()) {
						if (stop || !active) break;
						try { item.item.wdgmsg("transfer", Coord.z); Thread.sleep(100); } catch (Exception ignored) {}
					}
				}
			}
		}
		Thread.sleep(100);
	}

	private Gob findNearestSmelter() {
		Gob closest = null; Gob player = gui.map.player(); if (player == null) return null;
		Coord2d playerPos = player.rc;
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try { Resource res = gob.getres(); if (res == null) continue;
					if (res.name.equals(SMELTER_RES)) { double dist = gob.rc.dist(playerPos); if (dist > MAX_SEARCH_DIST) continue; if (closest == null || dist < closest.rc.dist(playerPos)) closest = gob; }
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return closest;
	}

	private int countSmelters() {
		int count = 0;
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) { try { Resource res = gob.getres(); if (res != null && res.name.equals(SMELTER_RES)) count++; } catch (Loading | NullPointerException ignored) {} }
		}
		return count;
	}

	private WItem findOreInInventory() {
		for (WItem wi : InvHelper.getAllItemsExcludeBeltKeyring(gui)) {
			try { if (isOreEnabled(wi.item.resource().basename())) return wi; } catch (Loading | NullPointerException ignored) {}
		}
		return null;
	}

	private List<WItem> findAllOreInInventory() {
		List<WItem> ores = new ArrayList<>();
		for (WItem wi : InvHelper.getAllItemsExcludeBeltKeyring(gui)) {
			try { if (isOreEnabled(wi.item.resource().basename())) ores.add(wi); } catch (Loading | NullPointerException ignored) {}
		}
		return ores;
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

	private boolean waitForHand(boolean occupied) throws InterruptedException {
		int timeout = 0;
		while (timeout < HAND_TIMEOUT) { if (occupied && gui.vhand != null) return true; if (!occupied && gui.vhand == null) return true; timeout += HAND_DELAY; Thread.sleep(HAND_DELAY); }
		return false;
	}

	@Override protected String windowPrefKey() { return "wndc-oreSmeltingBotWindow"; }
	@Override protected void onCleanup() { gui.oreSmeltingBot = null; }
}
