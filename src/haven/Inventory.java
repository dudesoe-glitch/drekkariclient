/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.res.ui.stackinv.ItemStack;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

public class Inventory extends Widget implements DTarget {
    public static final Coord sqsz = UI.scale(new Coord(33, 33));
    public static final Tex invsq = Resource.loadtex("gfx/hud/invsq");
    public boolean dropul = true;
    public Coord isz;
    public boolean[] sqmask = null;
    public String highlightItemName = null;
    public Map<GItem, WItem> wmap = new java.util.concurrent.ConcurrentHashMap<GItem, WItem>();
	public static Set<String> PLAYER_INVENTORY_NAMES = new HashSet<>(Arrays.asList("Inventory", "Belt", "Equipment", "Character Sheet", "Study"));
	public static Set<String> PRODUCTION_DEVICE_NAMES = new HashSet<>(Arrays.asList(
		"Cauldron", "Oven", "Kiln", "Ore Smelter", "Smith's Smelter", "Finery Forge",
		"Fireplace", "Stack Furnace", "Smoke Shed", "Herbalist Table", "Extraction Press",
		"Tanning Tub", "Drying Frame", "Cheese Rack", "Pane Mold", "Steelbox",
		"Smelter", "Garden Pot", "Clay Pot"
	));

	// Grouping modes for visual inventory organization
	public enum GroupingMode {
		NONE("No Groups"), BY_NAME("By Name");
		public final String label;
		GroupingMode(String label) { this.label = label; }
		public GroupingMode next() {
			GroupingMode[] vals = values();
			return vals[(ordinal() + 1) % vals.length];
		}
	}
	public GroupingMode groupingMode = GroupingMode.NONE;

	// Collapsed groups tracking (by group key)
	public final Set<String> collapsedGroups = new HashSet<>();

	// Cached group key assignments — avoids calling getGroupKey() (which calls sortName()/info()) every frame
	private Map<WItem, String> cachedGroupKeys = new HashMap<>();
	private int lastGroupKeyCacheChildCount = -1;
	private GroupingMode lastGroupKeyCacheMode = null;

	// Subtle tint colors for distinguishing groups
	private static final java.awt.Color[] GROUP_COLORS = {
		new java.awt.Color(50, 120, 50, 55),
		new java.awt.Color(120, 50, 50, 55),
		new java.awt.Color(50, 50, 120, 55),
		new java.awt.Color(120, 120, 50, 55),
		new java.awt.Color(120, 50, 120, 55),
		new java.awt.Color(50, 120, 120, 55),
		new java.awt.Color(100, 80, 50, 55),
		new java.awt.Color(80, 100, 80, 55),
		new java.awt.Color(100, 50, 80, 55),
		new java.awt.Color(50, 80, 100, 55),
	};


	private String getGroupKey(WItem wi) {
		try {
			if (groupingMode == GroupingMode.BY_NAME)
				return wi.sortName();
			return "";
		} catch (Exception e) {
			return "?";
		}
	}

	/**
	 * Returns cached group key for the given WItem.
	 * The cache is rebuilt when child count or grouping mode changes.
	 */
	private String getCachedGroupKey(WItem wi) {
		String key = cachedGroupKeys.get(wi);
		return (key != null) ? key : getGroupKey(wi);
	}

	/**
	 * Refreshes the group key cache if the child count or grouping mode changed.
	 * Called once per frame in draw() before any group rendering.
	 */
	private void refreshGroupKeyCache() {
		int childCount = 0;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) childCount++;
		}
		if (childCount != lastGroupKeyCacheChildCount || groupingMode != lastGroupKeyCacheMode) {
			cachedGroupKeys = new HashMap<>();
			for (Widget wdg = child; wdg != null; wdg = wdg.next) {
				if (wdg instanceof WItem) {
					WItem wi = (WItem) wdg;
					cachedGroupKeys.put(wi, getGroupKey(wi));
				}
			}
			lastGroupKeyCacheChildCount = childCount;
			lastGroupKeyCacheMode = groupingMode;
		}
	}

	// Container toolbar support
	private boolean containerStatusChecked = false;
	public boolean isContainerInventory = false;
	private Button containerGroupBtn;

	public static final Comparator<WItem> ITEM_COMPARATOR_ASC = Comparator
			.comparing(WItem::sortName)
			.thenComparing(w -> w.item.resname())
			.thenComparing(WItem::quality);

	public static final Comparator<WItem> ITEM_COMPARATOR_DESC = Comparator
			.comparing(WItem::sortName)
			.thenComparing(w -> w.item.resname())
			.thenComparing(WItem::quality, Comparator.reverseOrder());

	// ND: WHY is this happening when there's literally a texture resource for this?
	// ND: This affects the menugrid slots color, I'm basically replacing it with the inventory square texture
//    static {
//	Coord sz = sqsz.add(1, 1);
//	WritableRaster buf = PUtils.imgraster(sz);
//	for(int i = 1, y = sz.y - 1; i < sz.x - 1; i++) {
//	    buf.setSample(i, 0, 0, 20); buf.setSample(i, 0, 1, 28); buf.setSample(i, 0, 2, 21); buf.setSample(i, 0, 3, 167);
//	    buf.setSample(i, y, 0, 20); buf.setSample(i, y, 1, 28); buf.setSample(i, y, 2, 21); buf.setSample(i, y, 3, 167);
//	}
//	for(int i = 1, x = sz.x - 1; i < sz.y - 1; i++) {
//	    buf.setSample(0, i, 0, 20); buf.setSample(0, i, 1, 28); buf.setSample(0, i, 2, 21); buf.setSample(0, i, 3, 167);
//	    buf.setSample(x, i, 0, 20); buf.setSample(x, i, 1, 28); buf.setSample(x, i, 2, 21); buf.setSample(x, i, 3, 167);
//	}
//	for(int y = 1; y < sz.y - 1; y++) {
//	    for(int x = 1; x < sz.x - 1; x++) {
//		buf.setSample(x, y, 0, 36); buf.setSample(x, y, 1, 52); buf.setSample(x, y, 2, 38); buf.setSample(x, y, 3, 125);
//	    }
//	}
//	invsq = new TexI(PUtils.rasterimg(buf));
//    }

    @RName("inv")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Inventory((Coord)args[0]));
	}
    }

    public void draw(GOut g) {
	Coord c = new Coord();
	int mo = 0;
	for(c.y = 0; c.y < isz.y; c.y++) {
	    for(c.x = 0; c.x < isz.x; c.x++) {
		if((sqmask != null) && sqmask[mo++]) {
		    g.chcolor(64, 64, 64, 255);
		    g.image(invsq, c.mul(sqsz));
		    g.chcolor();
		} else {
		    g.image(invsq, c.mul(sqsz));
		}
	    }
	}
	if (groupingMode != GroupingMode.NONE) {
	    refreshGroupKeyCache();
	    drawGroupOverlays(g);
	}
	super.draw(g);
    }

    private void drawGroupOverlays(GOut g) {
	Map<String, Integer> groupColorIndex = new LinkedHashMap<>();
	int nextColor = 0;

	for (Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if (wdg instanceof WItem) {
		WItem wi = (WItem) wdg;
		String key = getCachedGroupKey(wi);
		if (!groupColorIndex.containsKey(key)) {
		    groupColorIndex.put(key, nextColor++);
		}
		int ci = groupColorIndex.get(key) % GROUP_COLORS.length;
		java.awt.Color col = GROUP_COLORS[ci];
		g.chcolor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
		g.frect(wi.c.sub(1, 1), wi.sz.add(2, 2));
		g.chcolor();
	    }
	}
    }

    public boolean mousedown(MouseDownEvent ev) {
	return super.mousedown(ev);
    }
	
    public Inventory(Coord sz) {
	super(sqsz.mul(sz).add(1, 1));
	isz = sz;
    }

    @Override
    protected void added() {
	super.added();
	checkContainerToolbar();
    }

    private void checkContainerToolbar() {
	if (containerStatusChecked) return;
	containerStatusChecked = true;
	Window w = getparent(Window.class);
	if (w == null || w.cap == null) return;
	if (PLAYER_INVENTORY_NAMES.contains(w.cap)) return;
	if (PRODUCTION_DEVICE_NAMES.contains(w.cap)) return;

	// Skip toolbar for very small inventories (e.g., symbels on tables)
	if (isz.x * isz.y <= 4) return;

	isContainerInventory = true;
	int toolbarH = UI.scale(22);
	int btnY = sz.y + UI.scale(2);
	int invWidth = isz.x * sqsz.x;

	// Use compact button sizes that fit small containers
	int sortW = UI.scale(36);
	int grpW = UI.scale(38);
	int extW = UI.scale(30);
	int gap = UI.scale(2);
	int totalBtnW = sortW + grpW + extW + gap * 2;
	// If buttons don't fit, shrink group button
	if (totalBtnW > invWidth) {
	    grpW = Math.max(UI.scale(24), invWidth - sortW - extW - gap * 2);
	}

	Button sortBtn = add(new Button(sortW, "Sort") {
	    public void click() {
		sortInventory();
	    }
	}, new Coord(UI.scale(1), btnY));
	sortBtn.settip("Sort items by type, then quality");

	containerGroupBtn = add(new Button(grpW, "Grp") {
	    public void click() {
		groupingMode = groupingMode.next();
		collapsedGroups.clear();
		this.settip(groupingMode.label);
	    }
	}, sortBtn.pos("ur").adds(gap, 0));
	containerGroupBtn.settip(groupingMode.label);

	Button extBtn = add(new Button(extW, "Ext") {
	    public void click() {
		GameUI gui = getparent(GameUI.class);
		if (gui == null) return;
		if (gui.extInventoryWindow != null) {
		    Utils.setprefc("wndc-extInventoryWindow", gui.extInventoryWindow.c);
		    gui.extInventoryWindow.reqdestroy();
		    gui.extInventoryWindow = null;
		}
		gui.extInventoryWindow = new ExtInventoryWindow(gui, Inventory.this);
		gui.add(gui.extInventoryWindow, Utils.getprefc("wndc-extInventoryWindow",
		    new Coord(gui.sz.x / 2 + 150, gui.sz.y / 2 - 250)));
	    }
	}, containerGroupBtn.pos("ur").adds(gap, 0));
	extBtn.settip("Open Extended Inventory for this container");

	// Resize inventory to include toolbar
	resize(sz.add(0, toolbarH));
	// Resize parent window to fit new content
	w.resize(w.contentsz());
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	if(ui.modshift) {
	    Inventory minv = getparent(GameUI.class).maininv;
	    if(minv != this) {
		if(ev.a < 0)
		    wdgmsg("invxf", minv.wdgid(), 1);
		else if(ev.a > 0)
		    minv.wdgmsg("invxf", this.wdgid(), 1);
	    }
	}
	return(true);
    }
    
    public void addchild(Widget child, Object... args) {
	add(child);
	Coord c = (Coord)args[0];
	if(child instanceof GItem) {
	    GItem i = (GItem)child;
	    wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
	}
    }
    
    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    ui.destroy(wmap.remove(i));
	}
    }

    @Override
    public void destroy() {
	if (sortThread != null) {
	    sortThread.interrupt();
	    sortThread = null;
	}
	super.destroy();
    }
    
    public boolean drop(Coord cc, Coord ul) {
	Coord dc;
	if(dropul)
	    dc = ul.add(sqsz.div(2)).div(sqsz);
	else
	    dc = cc.div(sqsz);
	wdgmsg("drop", dc);
	return(true);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "sz") {
	    isz = (Coord)args[0];
	    resize(invsq.sz().add(UI.scale(new Coord(-1, -1))).mul(isz).add(UI.scale(new Coord(1, 1))));
	    sqmask = null;
	} else if(msg == "mask") {
	    boolean[] nmask;
	    if(args[0] == null) {
		nmask = null;
	    } else {
		nmask = new boolean[isz.x * isz.y];
		byte[] raw = (byte[])args[0];
		for(int i = 0; i < isz.x * isz.y; i++)
		    nmask[i] = (raw[i >> 3] & (1 << (i & 7))) != 0;
	    }
	    this.sqmask = nmask;
	} else if(msg == "mode") {
	    dropul = !Utils.bv(args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }

	public List<WItem> getAllItems() {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				items.add((WItem) wdg);
			}
		}
		return items;
	}

	public List<WItem> getItemsExact(String... names) {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				for (String name : names) {
					if (wdgname.equals(name)) {
						items.add((WItem) wdg);
						break;
					}
				}
			}
		}
		return items;
	}

	public WItem getItemPrecise(String name) {
		if (name == null)
			return null;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				if (wdgname.equals(name))
					return (WItem) wdg;
			}
		}
		return null;
	}

	public WItem getItemPartial(String name) {
		if (name == null)
			return null;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				if (wdgname.contains(name))
					return (WItem) wdg;
			}
		}
		return null;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if(msg.equals("transfer-ordered")){
            try {
                processTransfer(getSame((GItem) args[0], (Boolean) args[1]));
            } catch (RuntimeException ignored) {
            }
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	private static boolean isInPlayerInventory(WItem item) {
		Window window = item.getparent(Window.class);
		return window != null && Objects.equals("Inventory", window.cap);
	}

	private static List<Integer> getExternalInventoryIds(UI ui) {
		List<Inventory> inventories = ui.gui.getAllWindows()
				.stream()
				.flatMap(w -> w.children().stream())
				.filter(child -> child instanceof Inventory)
				.map(i -> (Inventory) i)
				.collect(Collectors.toList());

		List<Integer> externalInventoryIds = inventories
				.stream()
				.filter(i -> {
					Window window = i.getparent(Window.class);
					return window != null && !PLAYER_INVENTORY_NAMES.contains(window.cap);
				}).map(i -> i.wdgid())
				.collect(Collectors.toList());

		List<Integer> stockpileIds = ui.gui.getAllWindows()
				.stream()
				.map(i -> i.getchild(ISBox.class))
				.filter(Objects::nonNull)
				.map(Widget::wdgid)
				.collect(Collectors.toList());

		externalInventoryIds.addAll(stockpileIds);
		return externalInventoryIds;
	}

	private static void attemptTransferSplittingStack(List<Integer> externalInventoryIds, ItemStack stack) {
		for (Integer externalInventoryId : externalInventoryIds) {
			Object[] invxf2Args = new Object[3];
			invxf2Args[0] = 0;
			invxf2Args[1] = stack.order.size();
			invxf2Args[2] = externalInventoryId;

			stack.order.get(0).wdgmsg("invxf2", invxf2Args);
		}
	}

	private void processTransfer(List<WItem> items) {
		List<Integer> externalInventoryIds = getExternalInventoryIds(ui);
		for (WItem item : items){
			item.item.wdgmsg("transfer", Coord.z);

			Widget contents = item.item.contents;
			if (contents instanceof ItemStack && isInPlayerInventory(item)) {
				attemptTransferSplittingStack(externalInventoryIds, (ItemStack) contents);
			}
		}
	}

	private List<WItem> getSame(GItem item, Boolean ascending) {
		List<WItem> items = new ArrayList<>();
		try {
			String name = item.res.get().name;
			GSprite spr = item.spr();
			for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if(wdg.visible && wdg instanceof WItem) {
					WItem wItem = (WItem) wdg;
					GItem child = wItem.item;
					try {
						if(child.res.get().name.equals(name) && ((spr == child.spr()) || (spr != null && spr.same(child.spr())))) {
							items.add(wItem);
						}
					} catch (Loading e) {}
				}
			}
			Collections.sort(items, ascending ? ITEM_COMPARATOR_ASC : ITEM_COMPARATOR_DESC);
		} catch (Loading e) { }
		return items;
	}

	public Coord isRoom(int x, int y) {
		//check if there is a space for an x times y item, return coordinate where.
		Coord freespot = null;
		boolean[][] occumap = new boolean[isz.x][isz.y];
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				for (int i = 0; i < wdg.sz.x; i++) {
					for (int j = 0; j < wdg.sz.y; j++) {
						occumap[(wdg.c.x/sqsz.x+i/sqsz.x)][(wdg.c.y/sqsz.y+j/sqsz.y)] = true;
					}
				}
			}
		}
		//(NICE LOOPS)
		//Iterate through all spots in inventory
		superloop:
		for (int i = 0; i < isz.x; i++) {
			for (int j = 0; j < isz.y; j++) {
				boolean itsclear = true;
				//Check if there is X times Y free slots
				try {
					for (int k = 0; k < x; k++) {
						for (int l = 0; l < y; l++) {
							if (occumap[i+k][j+l] == true) {
								itsclear = false;
							}
						}
					}
				} catch (IndexOutOfBoundsException e) {
					itsclear = false;
				}

				if (itsclear) {
					freespot = new Coord(i,j);
					break superloop;
				}
			}
		}

		return freespot;
	}

	public int getFreeSpace() {
		int feespace = isz.x * isz.y;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem)
				feespace -= (wdg.sz.x * wdg.sz.y) / (sqsz.x * sqsz.y);
		}
		return feespace;
	}

	public List<WItem> getItemsPartial(String... names) {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				for (String name : names) {
					if (name == null)
						continue;
					if (wdgname.contains(name)) {
						items.add((WItem) wdg);
						break;
					}
				}
			}
		}
		return items;
	}

	private volatile boolean sorting = false;
	private volatile Thread sortThread;

	// Multi-slot comparator: largest area first, then name, then quality
	private static final Comparator<WItem> MULTI_SLOT_COMPARATOR = Comparator
			.comparing((WItem w) -> w.sz.div(sqsz).x * w.sz.div(sqsz).y, Comparator.reverseOrder())
			.thenComparing(WItem::sortName)
			.thenComparing(w -> w.item.resname())
			.thenComparing(WItem::quality, Comparator.reverseOrder());

	public boolean keydown(KeyDownEvent ev) {
		if (ev.awt.getKeyCode() == KeyEvent.VK_S && ui.modshift && ui.modctrl) {
			sortInventory();
			return true;
		}
		return super.keydown(ev);
	}

	public void sortInventory() {
		if (sorting) return;
		GameUI gui = getparent(GameUI.class);
		if (gui == null) return;
		if (gui.vhand != null) {
			gui.error("Cannot sort while holding an item.");
			return;
		}
		sorting = true;
		sortThread = new Thread(() -> {
			try {
				doSort(gui);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				gui.error("Sort failed: " + e.getMessage());
			} finally {
				// Always clean up cursor if an item is stranded
				try {
					if (gui.vhand != null) {
						Coord freeSlot = isRoom(1, 1);
						if (freeSlot != null) {
							wdgmsg("drop", freeSlot);
							waitForCursor(gui, false);
						} else {
							gui.error("Sort: no room, dropping item to ground.");
							gui.vhand.item.wdgmsg("drop", Coord.z);
						}
					}
				} catch (Exception ignored) {}
				sorting = false;
				sortThread = null;
			}
		}, "InventorySort");
		sortThread.start();
	}

	private void doSort(GameUI gui) throws InterruptedException {
		if (isContainerInventory) {
			doSortContainer(gui);
		} else {
			doSortPlayerInventory(gui);
		}
	}

	/**
	 * Sort a container by transferring all items to player inventory,
	 * then placing them back in sorted order. This avoids all the
	 * swap-chain complexity and handles mixed-size items cleanly.
	 */
	private void doSortContainer(GameUI gui) throws InterruptedException {
		Inventory playerInv = gui.maininv;
		if (playerInv == null) return;

		// Snapshot all items in this container
		List<WItem> allItems = new ArrayList<>(wmap.values());
		if (allItems.isEmpty()) return;

		// Record what items existed (by resource name + quality) for placing back
		List<ItemRecord> records = new ArrayList<>();
		for (WItem wi : allItems) {
			try {
				records.add(new ItemRecord(wi));
			} catch (Loading e) {
				// Item not loaded, skip
			}
		}

		// Phase 1: Transfer all items from container to player inventory
		for (WItem wi : allItems) {
			if (!wmap.containsValue(wi)) continue;
			wi.item.wdgmsg("transfer", Coord.z);
			Thread.sleep(5);
		}
		// Wait for transfers to complete
		Thread.sleep(100);

		// Sort records: multi-slot (largest first) then single-slot, all by name then quality
		records.sort(Comparator
			.comparing((ItemRecord r) -> r.area, Comparator.reverseOrder())
			.thenComparing(r -> r.name)
			.thenComparing(r -> r.resname)
			.thenComparing(r -> r.quality, Comparator.reverseOrder()));

		// Phase 2: Place items back from player inventory in sorted order
		// Build occupancy grid
		boolean[][] occupied = new boolean[isz.x][isz.y];
		if (sqmask != null) {
			for (int i = 0; i < isz.x * isz.y; i++) {
				if (sqmask[i])
					occupied[i % isz.x][i / isz.x] = true;
			}
		}

		for (ItemRecord rec : records) {
			// Find this item in player inventory
			WItem found = findItemInInventory(playerInv, rec);
			if (found == null) continue;

			Coord gridSz = found.sz.div(sqsz);
			Coord targetSlot = findFreeSlot(occupied, gridSz.x, gridSz.y);
			if (targetSlot == null) continue; // No room left

			// Pick up from player inventory and drop into container
			found.item.wdgmsg("take", Coord.z);
			if (!waitForCursor(gui, true)) return;
			wdgmsg("drop", targetSlot);
			Thread.sleep(5);
			if (!waitForCursorEmpty(gui)) return;

			markOccupied(occupied, targetSlot, gridSz);
		}
	}

	/**
	 * Sort player inventory using swap-chain approach (no external staging area).
	 */
	private void doSortPlayerInventory(GameUI gui) throws InterruptedException {
		// Snapshot items from ConcurrentHashMap for thread safety
		List<WItem> multiSlot = new ArrayList<>();
		List<WItem> singleSlot = new ArrayList<>();
		boolean[][] masked = new boolean[isz.x][isz.y];

		// Mark sqmask-blocked cells
		if (sqmask != null) {
			for (int i = 0; i < isz.x * isz.y; i++) {
				if (sqmask[i])
					masked[i % isz.x][i / isz.x] = true;
			}
		}

		for (WItem wi : new ArrayList<>(wmap.values())) {
			try {
				Coord gridSz = wi.sz.div(sqsz);
				if (gridSz.x == 1 && gridSz.y == 1)
					singleSlot.add(wi);
				else
					multiSlot.add(wi);
			} catch (Loading e) {
				singleSlot.add(wi);
			}
		}

		if (singleSlot.isEmpty() && multiSlot.isEmpty()) return;

		// Sort: multi-slot by area (largest first) then name/quality
		multiSlot.sort(MULTI_SLOT_COMPARATOR);
		singleSlot.sort(ITEM_COMPARATOR_DESC);

		// Phase 1: Place multi-slot items first (they need contiguous space)
		boolean[][] occupied = new boolean[isz.x][isz.y];
		for (int x = 0; x < isz.x; x++)
			for (int y = 0; y < isz.y; y++)
				occupied[x][y] = masked[x][y];

		for (WItem wi : multiSlot) {
			Coord gridSz = wi.sz.div(sqsz);
			Coord currentSlot = wi.c.sub(1, 1).div(sqsz);
			Coord targetSlot = findFreeSlot(occupied, gridSz.x, gridSz.y);

			if (targetSlot == null) {
				markOccupied(occupied, currentSlot, gridSz);
				continue;
			}

			if (!currentSlot.equals(targetSlot)) {
				if (!wmap.containsValue(wi)) continue;
				wi.item.wdgmsg("take", Coord.z);
				if (!waitForCursor(gui, true)) return;
				wdgmsg("drop", targetSlot);
				Thread.sleep(5);
				if (!waitForCursorEmpty(gui)) return;
			}
			markOccupied(occupied, targetSlot, gridSz);
		}

		// Phase 2: Swap-chain sort 1x1 items
		List<Coord> targets = new ArrayList<>();
		for (int y = 0; y < isz.y; y++)
			for (int x = 0; x < isz.x; x++)
				if (!occupied[x][y]) targets.add(new Coord(x, y));

		// Refresh single-slot list (positions may have shifted)
		singleSlot.clear();
		for (WItem wi : new ArrayList<>(wmap.values())) {
			try {
				Coord gridSz = wi.sz.div(sqsz);
				if (gridSz.x == 1 && gridSz.y == 1) singleSlot.add(wi);
			} catch (Loading e) {
				singleSlot.add(wi);
			}
		}
		singleSlot.sort(ITEM_COMPARATOR_DESC);

		Map<WItem, Coord> currentPos = new HashMap<>();
		Map<Coord, WItem> posToItem = new HashMap<>();
		for (WItem wi : singleSlot) {
			Coord gp = wi.c.sub(1, 1).div(sqsz);
			currentPos.put(wi, gp);
			posToItem.put(gp, wi);
		}

		Map<WItem, Coord> targetPos = new HashMap<>();
		for (int i = 0; i < singleSlot.size() && i < targets.size(); i++)
			targetPos.put(singleSlot.get(i), targets.get(i));

		Set<WItem> placed = new HashSet<>();
		for (int i = 0; i < singleSlot.size() && i < targets.size(); i++) {
			WItem item = singleSlot.get(i);
			if (placed.contains(item)) continue;
			Coord target = targetPos.get(item);
			Coord current = currentPos.get(item);
			if (current.equals(target)) { placed.add(item); continue; }
			if (!wmap.containsValue(item)) { placed.add(item); continue; }

			item.item.wdgmsg("take", Coord.z);
			if (!waitForCursor(gui, true)) return;

			WItem carrying = item;
			while (carrying != null && !placed.contains(carrying)) {
				Coord dropTarget = targetPos.get(carrying);
				if (dropTarget == null) break;
				placed.add(carrying);
				WItem displaced = posToItem.get(dropTarget);
				wdgmsg("drop", dropTarget);
				Thread.sleep(5);
				posToItem.remove(currentPos.get(carrying));
				currentPos.put(carrying, dropTarget);
				posToItem.put(dropTarget, carrying);

				if (displaced != null && !placed.contains(displaced)) {
					if (!waitForCursor(gui, true)) return;
					carrying = displaced;
				} else {
					waitForCursorEmpty(gui);
					carrying = null;
				}
			}

			if (gui.vhand != null) {
				Coord freeSlot = isRoom(1, 1);
				if (freeSlot != null) {
					wdgmsg("drop", freeSlot);
					waitForCursorEmpty(gui);
				} else {
					gui.error("Sort: no room, dropping item to ground.");
					gui.vhand.item.wdgmsg("drop", Coord.z);
					waitForCursorEmpty(gui);
				}
			}
		}
	}

	/** Record of an item's identity for matching after transfer. */
	private static class ItemRecord {
		final String name;
		final String resname;
		final double quality;
		final int area; // grid slots occupied

		ItemRecord(WItem wi) {
			this.name = wi.sortName();
			this.resname = wi.item.resname();
			this.quality = wi.quality();
			Coord gridSz = wi.sz.div(sqsz);
			this.area = gridSz.x * gridSz.y;
		}
	}

	/** Find an item in the given inventory matching the record. */
	private WItem findItemInInventory(Inventory inv, ItemRecord rec) {
		WItem best = null;
		double bestDiff = Double.MAX_VALUE;
		for (WItem wi : new ArrayList<>(inv.wmap.values())) {
			try {
				if (wi.sortName().equals(rec.name) && wi.item.resname().equals(rec.resname)) {
					double diff = Math.abs(wi.quality() - rec.quality);
					if (diff < bestDiff) {
						bestDiff = diff;
						best = wi;
					}
				}
			} catch (Loading e) { /* skip */ }
		}
		return best;
	}

	private void markOccupied(boolean[][] occupied, Coord slot, Coord gridSz) {
		for (int dx = 0; dx < gridSz.x; dx++)
			for (int dy = 0; dy < gridSz.y; dy++) {
				int gx = slot.x + dx, gy = slot.y + dy;
				if (gx < isz.x && gy < isz.y) occupied[gx][gy] = true;
			}
	}

	private Coord findFreeSlot(boolean[][] occupied, int w, int h) {
		for (int y = 0; y <= isz.y - h; y++)
			for (int x = 0; x <= isz.x - w; x++) {
				boolean fits = true;
				for (int dx = 0; dx < w && fits; dx++)
					for (int dy = 0; dy < h && fits; dy++)
						if (occupied[x + dx][y + dy]) fits = false;
				if (fits) return new Coord(x, y);
			}
		return null;
	}

	/**
	 * Waits for an item to appear on cursor, or for cursor to become empty.
	 * Timeout: ~1000ms (200 x 5ms).
	 */
	private boolean waitForCursor(GameUI gui, boolean wantItem) throws InterruptedException {
		for (int i = 0; i < 200; i++) {
			if (wantItem && (gui.vhand != null || !gui.hand.isEmpty())) return true;
			if (!wantItem && gui.vhand == null && gui.hand.isEmpty()) return true;
			Thread.sleep(5);
		}
		return false;
	}

	/**
	 * Wait for cursor to become empty. If a displaced item is on cursor,
	 * drop it in a free slot first.
	 */
	private boolean waitForCursorEmpty(GameUI gui) throws InterruptedException {
		if (waitForCursor(gui, false)) return true;
		// Cursor still has item — try to drop displaced item somewhere
		if (gui.vhand != null) {
			Coord freeSlot = isRoom(1, 1);
			if (freeSlot != null) {
				wdgmsg("drop", freeSlot);
				return waitForCursor(gui, false);
			} else {
				gui.error("Sort: no room, dropping item to ground.");
				gui.vhand.item.wdgmsg("drop", Coord.z);
				return waitForCursor(gui, false);
			}
		}
		return false;
	}
}
