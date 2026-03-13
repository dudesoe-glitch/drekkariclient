package haven.automated;

import haven.*;

import java.util.ArrayList;
import java.util.List;

import static haven.OCache.posres;

/**
 * Stockpile Bot — deposits items from player inventory onto a target stockpile.
 *
 * Usage:
 * 1. Click "Set Target" then click a stockpile in the world
 * 2. Click "Start" — bot will pathfind to the stockpile and deposit matching items
 * 3. When stockpile is full, optionally finds next nearest stockpile of the same type
 */
public class StockpileBot extends BotBase {
	private Label targetLabel;
	private Coord2d targetPos;
	private long targetGobId = -1;
	private String targetResName;
	public boolean settingTarget;
	private boolean autoNextStockpile;
	private int deposited;

	private static final double MAX_INTERACT_DIST = 11 * 5;
	private static final int HAND_TIMEOUT = 2000;
	private static final int HAND_DELAY = 8;

	public StockpileBot(GameUI gui) {
		super(gui, UI.scale(250, 160), "Stockpile Bot");
		this.targetPos = null;
		this.settingTarget = false;
		this.autoNextStockpile = Utils.getprefb("stockpileBot_autoNext", true);
		this.deposited = 0;
		checkInventory = false; // We're emptying inventory, not filling it

		int y = 10;
		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, y));
		y += 18;

		targetLabel = new Label("Target: not set");
		add(targetLabel, UI.scale(10, y));
		y += 20;

		Button setTargetButton = new Button(UI.scale(100), "Set Target") {
			@Override
			public void click() {
				settingTarget = true;
				statusLabel.settext("Click a stockpile...");
			}
		};
		add(setTargetButton, UI.scale(10, y));
		y += 25;

		add(new CheckBox("Auto-find next when full") {{ a = autoNextStockpile; }
			public void set(boolean val) { autoNextStockpile = val; a = val; Utils.setprefb("stockpileBot_autoNext", val); }
		}, UI.scale(10, y));
		y += 25;

		activeButton = new Button(UI.scale(80), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					if (targetGobId < 0 || targetResName == null) {
						active = false;
						statusLabel.settext("Set target first!");
						return;
					}
					deposited = 0;
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

	/**
	 * Called from MapView when the user clicks a gob while settingTarget is true.
	 */
	public void setTarget(Gob gob) {
		settingTarget = false;
		if (gob == null) {
			statusLabel.settext("Invalid target");
			return;
		}
		try {
			Resource res = gob.getres();
			if (res == null || !res.name.startsWith("gfx/terobjs/stockpile")) {
				statusLabel.settext("Not a stockpile!");
				return;
			}
			targetGobId = gob.id;
			targetPos = gob.rc;
			targetResName = res.name;
			String displayName = res.name.substring("gfx/terobjs/stockpile-".length());
			targetLabel.settext("Target: " + displayName);
			statusLabel.settext("Target set");
		} catch (Loading e) {
			statusLabel.settext("Loading, try again...");
		}
	}

	@Override
	protected void tick() throws InterruptedException {
		if (targetGobId < 0 || targetResName == null) { Thread.sleep(200); return; }
		Gob player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (!checkVitals()) return;

		// Clear hand if holding something
		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}

		// Find items in inventory to deposit
		List<WItem> items = findItemsForStockpile();
		if (items.isEmpty()) {
			setStatus("No items to deposit (" + deposited + " deposited)");
			deactivate();
			return;
		}

		// Find the target stockpile gob (it may have been refreshed/reloaded)
		Gob stockpile = findTargetStockpile();
		if (stockpile == null) {
			if (autoNextStockpile) {
				stockpile = findNearestStockpileOfType(targetResName);
				if (stockpile != null) {
					targetGobId = stockpile.id;
					targetPos = stockpile.rc;
					setStatus("Switching to next stockpile...");
				} else {
					setStatus("No more stockpiles nearby (" + deposited + " deposited)");
					deactivate();
					return;
				}
			} else {
				setStatus("Target stockpile not found (" + deposited + " deposited)");
				deactivate();
				return;
			}
		}

		// Pathfind to stockpile
		setStatus("Walking to stockpile...");
		gui.map.pfLeftClick(stockpile.rc.floor().add(2, 0), null);
		if (!Actions.waitPf(gui)) {
			Actions.unstuck(gui);
			return;
		}

		player = gui.map.player();
		if (player == null) return;
		if (stockpile.rc.dist(player.rc) > MAX_INTERACT_DIST) {
			setStatus("Too far, retrying...");
			return;
		}

		// Deposit items
		setStatus("Depositing items...");
		int count = depositItems(stockpile, items);
		deposited += count;
		if (count > 0) {
			setStatus("Deposited " + count + " (" + deposited + " total)");
		}
		Thread.sleep(100);
	}

	private int depositItems(Gob stockpile, List<WItem> items) throws InterruptedException {
		int loaded = 0;
		for (WItem witem : items) {
			if (stop || !active) break;
			try {
				GItem item = witem.item;
				// Pick up item into cursor
				item.wdgmsg("take", new Coord(item.sz.x / 2, item.sz.y / 2));
				if (!waitForHand(true)) continue;
				if (gui.vhand == null) continue;

				GItem handItem = gui.vhand.item;

				// Use itemact on stockpile — modifier 1 keeps cycling items
				gui.map.wdgmsg("itemact", Coord.z, stockpile.rc.floor(posres), 1,
						0, (int) stockpile.id, stockpile.rc.floor(posres), 0, -1);
				Thread.sleep(300);

				// Wait for hand to change (item consumed or replaced)
				int timeout = 0;
				boolean consumed = false;
				while (timeout < HAND_TIMEOUT) {
					WItem handNow = gui.vhand;
					if (handNow == null) {
						consumed = true;
						break;
					} else if (handNow.item != handItem) {
						// Different item in hand — stockpile may be full or wrong type
						// Drop it back
						handNow.item.wdgmsg("drop", Coord.z);
						Actions.waitForEmptyHand(gui, 1000, "");
						consumed = true;
						break;
					}
					timeout += HAND_DELAY;
					Thread.sleep(HAND_DELAY);
				}

				if (!consumed) {
					// Item still in hand after timeout — stockpile full or wrong item
					if (gui.vhand != null) {
						gui.vhand.item.wdgmsg("drop", Coord.z);
						Actions.waitForEmptyHand(gui, 1000, "");
					}
					setStatus("Stockpile full or wrong item type");
					// Try next stockpile if auto-next is on
					if (autoNextStockpile) {
						Gob next = findNearestStockpileOfType(targetResName);
						if (next != null && next.id != stockpile.id) {
							targetGobId = next.id;
							targetPos = next.rc;
							setStatus("Switching to next stockpile...");
							return loaded; // Return to tick() to pathfind to new stockpile
						}
					}
					break;
				}
				loaded++;
				Thread.sleep(100);
			} catch (Loading ignored) {}
		}
		// Clean up hand
		if (gui.vhand != null) {
			gui.vhand.item.wdgmsg("drop", Coord.z);
			Actions.waitForEmptyHand(gui, 1000, "");
		}
		return loaded;
	}

	/**
	 * Find items in player inventory that could go on a stockpile.
	 * We deposit all items except equipped gear, belt, keyring.
	 */
	private List<WItem> findItemsForStockpile() {
		List<WItem> result = new ArrayList<>();
		for (WItem wi : InvHelper.getAllItemsExcludeBeltKeyring(gui)) {
			try {
				// Include all items — the stockpile will reject wrong types
				result.add(wi);
			} catch (Loading ignored) {}
		}
		return result;
	}

	private Gob findTargetStockpile() {
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				try {
					if (gob.id == targetGobId) return gob;
				} catch (Exception ignored) {}
			}
		}
		// Gob ID may have changed (reloaded), try finding by position + type
		if (targetPos != null) {
			synchronized (gui.map.glob.oc) {
				for (Gob gob : gui.map.glob.oc) {
					try {
						Resource res = gob.getres();
						if (res != null && res.name.equals(targetResName) && gob.rc.dist(targetPos) < 5) {
							targetGobId = gob.id;
							return gob;
						}
					} catch (Loading | NullPointerException ignored) {}
				}
			}
		}
		return null;
	}

	private Gob findNearestStockpileOfType(String resName) {
		return GobHelper.findNearest(gui, MAX_SEARCH_DIST, g -> {
			String name = GobHelper.getResName(g);
			return name != null && name.equals(resName) && g.id != targetGobId;
		});
	}

	private boolean waitForHand(boolean occupied) throws InterruptedException {
		int timeout = 0;
		while (timeout < HAND_TIMEOUT) {
			if (occupied && gui.vhand != null) return true;
			if (!occupied && gui.vhand == null) return true;
			timeout += HAND_DELAY;
			Thread.sleep(HAND_DELAY);
		}
		return false;
	}

	@Override protected String windowPrefKey() { return "wndc-stockpileBotWindow"; }
	@Override protected void onCleanup() { gui.stockpileBot = null; }
}
