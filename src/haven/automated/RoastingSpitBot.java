package haven.automated;

import haven.*;
import haven.Button;
import haven.render.RenderTree;

import java.awt.Color;
import java.util.*;
import java.util.List;

import static haven.OCache.posres;

public class RoastingSpitBot extends BotBase {
	private Gob fireplace = null;
	private final String[] spitroastableItems = {
			"gfx/invobjs/rabbit-clean",
			"gfx/invobjs/fish-",
			"gfx/invobjs/chicken-cleaned",
			"gfx/invobjs/bat-clean",
			"gfx/invobjs/mole-clean",
			"gfx/invobjs/rockdove-cleaned",
			"gfx/invobjs/mallard-cleaned",
			"gfx/invobjs/squirrel-clean",
			"gfx/invobjs/kebabraw",
	};
	double maxDistance = 2 * 11;
	public String roastingSpitOverlayName = "gfx/terobjs/roastspit";
	private boolean passiveMode = false;
	CheckBox passiveModeBox = null;

	public RoastingSpitBot(GameUI gui) {
		super(gui, UI.scale(160, 50), "Roasting Spit Bot", true);
		checkHP = false; checkEnergy = false; checkStamina = false; checkInventory = false;

		activeButton = add(new Button(UI.scale(150), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					Gob theObject = null;
					Gob player = gui.map.player();
					if (player == null) { active = false; return; }
					Coord2d plc = player.rc;
					for (Gob gob : Utils.getAllGobs(gui)) {
						double distFromPlayer = gob.rc.dist(plc);
						if (gob.id == gui.map.plgob || distFromPlayer >= maxDistance)
							continue;
						Resource res = null;
						try { res = gob.getres(); } catch (Loading l) {}
						if (res != null) {
							if (res.name.equals("gfx/terobjs/pow")) {
								if (AUtils.gobHasOverlay(gob, roastingSpitOverlayName)){
									if (distFromPlayer < maxDistance && (theObject == null || distFromPlayer < theObject.rc.dist(plc))) {
										theObject = gob;
									}
								}
							}
						}
					}
					if (theObject == null) {
						gui.errorsilent("Roasting Spit Bot: No Roasting Spit found nearby.");
						active = false;
					} else {
						fireplace = theObject;
						gui.msg("Roasting Spit Bot: Started", Color.WHITE);
						this.change("Stop");
					}
				} else {
					fireplace = null;
					gui.msg("Roasting Spit Bot: Stopped", Color.WHITE);
					this.change("Start");
				}
			}
		}, 0, 0);

		passiveModeBox = add(new CheckBox("Passive mode") {
			{a = passiveMode;}
			public void changed(boolean val) { passiveMode = val; }
		}, activeButton.pos("bl").adds(0, 6));

		pack();
		passiveModeBox.tooltip = RichText.render("If enabled, the bot will only roast once there is something on the spit and once done roasting it will wait for another item to be put on the roast", UI.scale(300));
	}

	private void sleep(int duration) {
		try { Thread.sleep(duration); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
	}

	public boolean isSpitroastableItem(String input) {
		for (String item : spitroastableItems) {
			if (input.toLowerCase().contains(item.toLowerCase())) return true;
		}
		return false;
	}

	public List<GItem> findSpitroastableItems(List<WItem> invItems) {
		List<GItem> foundSpitroastableItems = new ArrayList<>();
		for (Object item : invItems.toArray()) {
			if (item instanceof WItem) {
				WItem wi = (WItem) item;
				try {
					if (isSpitroastableItem(wi.item.resource().name)) foundSpitroastableItems.add(wi.item);
				} catch (Loading ignored) {}
			}
		}
		return foundSpitroastableItems;
	}

	public boolean readyToRoast() {
		try {
			if (fireplace != null && !fireplace.ols.isEmpty()) {
				Optional<Gob.Overlay> foundOverlay = fireplace.ols.stream()
						.filter(ol -> ol != null && ol.spr != null && ol.spr.res != null && roastingSpitOverlayName.equals(ol.spr.res.name))
						.findFirst();
				if (foundOverlay.isPresent()) {
					Gob.Overlay gobOverlay = foundOverlay.get();
					if(((RenderTree.TreeSlot)((ArrayList<?>) gobOverlay.slots).get(0)).children[0].children.length > 2 && ((RenderTree.TreeSlot)((ArrayList<?>) gobOverlay.slots).get(0)).children[0].children[2] != null)
						return ((RenderTree.TreeSlot)((ArrayList<?>) gobOverlay.slots).get(0)).children[0].children[2].toString().contains("raw");
				}
			}
		} catch (Exception ignored) {}
		return false;
	}

	public boolean isCooked() {
		try {
			if (fireplace != null && !fireplace.ols.isEmpty()) {
				Optional<Gob.Overlay> foundOverlay = fireplace.ols.stream()
						.filter(ol -> ol != null && ol.spr != null && ol.spr.res != null && roastingSpitOverlayName.equals(ol.spr.res.name))
						.findFirst();
				if (foundOverlay.isPresent()) {
					Gob.Overlay gobOverlay = foundOverlay.get();
					if(((RenderTree.TreeSlot)((ArrayList<?>) gobOverlay.slots).get(0)).children[0].children.length > 2 && ((RenderTree.TreeSlot)((ArrayList<?>) gobOverlay.slots).get(0)).children[0].children[2] != null)
						return ((RenderTree.TreeSlot)((ArrayList<?>) gobOverlay.slots).get(0)).children[0].children[2].toString().contains("roast");
				}
			}
		} catch (Exception ignored) {}
		return false;
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				sleep(10);
				if (active && fireplace != null) {
					try {
						if (!passiveMode) {
							List<WItem> invItems = InvHelper.getAllItemsExcludeBeltKeyring(this.gui);
							List<GItem> foundSpitroastableItems = findSpitroastableItems(invItems);
							if (!foundSpitroastableItems.isEmpty() && !readyToRoast() && !isCooked()) {
								GItem spitroastableItem = foundSpitroastableItems.get(0);
								sleep(1000);
								putItemOnRoast(spitroastableItem);
								startRoasting();
								carve();
							} else if (readyToRoast()) {
								sleep(1000);
								startRoasting();
								carve();
							} else if (isCooked()) {
								sleep(1000);
								carve();
							} else {
								gui.msg("Roasting Spit Bot: Done cooking!", Color.WHITE);
								active = false;
								activeButton.change("Start");
							}
						} else {
							if (readyToRoast() && !isCooked()) {
								startRoasting();
							} else if (isCooked()) {
								sleep(1000);
								idlePlayer();
							}
						}
					} catch (Exception e) {
						if (e instanceof InterruptedException) { Thread.currentThread().interrupt(); return; }
						gui.errorsilent("Roasting Spit Bot: Something went wrong, resetting...");
						active = false;
						activeButton.change("Start");
						fireplace = null;
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof InterruptedException) Thread.currentThread().interrupt();
		}
	}

	public void putItemOnRoast(GItem item) {
		item.wdgmsg("take", Coord.z);
		sleep(500);
		if (Actions.rightClickGobOverlayWithItem(this.gui, fireplace, roastingSpitOverlayName)) {
			sleep(2000);
		} else {
			gui.errorsilent("Roasting Spit Bot: The Roasting Spit is gone!");
			active = false;
			fireplace = null;
			activeButton.change("Start");
		}
	}

	public void startRoasting() {
		Actions.rightClickGobOverlayAndSelectOption(this.gui, fireplace, 0, roastingSpitOverlayName);
		sleep(2000);
		try { waitProgBarRoastingSpit(gui); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
	}

	public void carve() {
		if (isCooked()) {
			Actions.rightClickGobOverlayAndSelectOption(this.gui, fireplace, 1, roastingSpitOverlayName);
			while (gui.prog != null) sleep(1000);
		}
	}

	public static void waitProgBarRoastingSpit(GameUI gui) throws InterruptedException {
		double maxProg = 0;
		while (gui.prog != null && gui.prog.prog >= 0) {
			if (maxProg > gui.prog.prog) break;
			maxProg = gui.prog.prog;
			Thread.sleep(40);
		}
	}

	@Override
	protected String windowPrefKey() { return "wndc-roastingSpitBotWindow"; }

	@Override
	protected void onCleanup() { gui.roastingSpitBot = null; }
}
