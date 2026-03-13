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
import java.util.concurrent.ConcurrentHashMap;
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
	public final Set<String> collapsedGroups = ConcurrentHashMap.newKeySet();

	// Cached group key assignments — avoids calling getGroupKey() (which calls sortName()/info()) every frame
	private Map<WItem, String> cachedGroupKeys = new ConcurrentHashMap<>();
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
		sortInventory(ui.modshift);
	    }
	}, new Coord(UI.scale(1), btnY));
	sortBtn.settip("Sort items (Shift+click: full sort via player inventory)");

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
	// Auto-sort on open if enabled
	if (OptWnd.autoSortContainersCheckBox != null && OptWnd.autoSortContainersCheckBox.a) {
	    sortInventory(false);
	}
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
				try {
					String wdgname = ((WItem)wdg).item.getname();
					for (String name : names) {
						if (wdgname.equals(name)) {
							items.add((WItem) wdg);
							break;
						}
					}
				} catch (Loading ignored) {}
			}
		}
		return items;
	}

	public WItem getItemPrecise(String name) {
		if (name == null)
			return null;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				try {
					String wdgname = ((WItem)wdg).item.getname();
					if (wdgname.equals(name))
						return (WItem) wdg;
				} catch (Loading ignored) {}
			}
		}
		return null;
	}

	public WItem getItemPartial(String name) {
		if (name == null)
			return null;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				try {
					String wdgname = ((WItem)wdg).item.getname();
					if (wdgname.contains(name))
						return (WItem) wdg;
				} catch (Loading ignored) {}
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
				try {
					String wdgname = ((WItem)wdg).item.getname();
					for (String name : names) {
						if (name == null)
							continue;
						if (wdgname.contains(name)) {
							items.add((WItem) wdg);
							break;
						}
					}
				} catch (Loading ignored) {}
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

	public void sortInventory() { sortInventory(false); }

	public void sortInventory(boolean fullSort) {
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
				doSort(gui, fullSort);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				gui.error("Sort failed: " + e.getMessage());
			} finally {
				// Always clean up cursor if an item is stranded
				try {
					if (gui.vhand != null) {
						Coord itemGridSz = gui.vhand.sz.div(sqsz);
						int w = Math.max(1, itemGridSz.x);
						int h = Math.max(1, itemGridSz.y);
						Coord freeSlot = isRoom(w, h);
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

	private void doSort(GameUI gui, boolean fullSort) throws InterruptedException {
		if (isContainerInventory && fullSort) {
			doFullSort(gui);
		} else {
			doQuickSort(gui);
		}
	}

	/**
	 * Full sort: plan-then-execute approach.
	 * Phase 1: Compute target positions for ALL items (multi-slot first, then 1x1).
	 * Phase 2: Move multi-slot items to targets, clearing blockers intelligently.
	 * Phase 3: Swap-chain sort 1x1 items.
	 */
	private void doFullSort(GameUI gui) throws InterruptedException {
		Inventory playerInv = gui.maininv;
		Set<WItem> originalPlayerItems = (playerInv != null)
				? new HashSet<>(playerInv.wmap.values()) : Collections.emptySet();

		// === Phase 1: Classify items ===
		Set<WItem> immovable = new HashSet<>();
		List<WItem> multiSlot = new ArrayList<>();

		for (WItem wi : new ArrayList<>(wmap.values())) {
			try {
				if (hasContents(wi)) { immovable.add(wi); continue; }
				Coord gridSz = wi.sz.div(sqsz);
				if (gridSz.x > 1 || gridSz.y > 1)
					multiSlot.add(wi);
			} catch (Loading e) { immovable.add(wi); }
		}

		if (multiSlot.isEmpty()) {
			doQuickSort(gui);
			return;
		}

		multiSlot.sort(MULTI_SLOT_COMPARATOR);

		// === Phase 1b: Plan target positions for all multi-slot items ===
		// Build fixed occupancy: sqmask + immovable items only
		boolean[][] planned = new boolean[isz.x][isz.y];
		if (sqmask != null) {
			for (int i = 0; i < isz.x * isz.y; i++)
				if (sqmask[i]) planned[i % isz.x][i / isz.x] = true;
		}
		for (WItem wi : immovable) {
			if (!wmap.containsValue(wi)) continue;
			Coord slot = wi.c.sub(1, 1).div(sqsz);
			Coord gridSz = wi.sz.div(sqsz);
			markOccupied(planned, slot, gridSz);
		}

		// Assign target positions for each multi-slot item (largest first)
		Map<WItem, Coord> multiTargets = new LinkedHashMap<>();
		int noFitCount = 0;
		for (WItem wi : multiSlot) {
			Coord gridSz = wi.sz.div(sqsz);
			Coord target = findFreeSlot(planned, gridSz.x, gridSz.y);
			if (target != null) {
				multiTargets.put(wi, target);
				markOccupied(planned, target, gridSz);
			} else {
				// Can't fit — keep at current position
				noFitCount++;
				Coord current = wi.c.sub(1, 1).div(sqsz);
				multiTargets.put(wi, current);
				markOccupied(planned, current, gridSz);
			}
		}

		// === Phase 2: Execute multi-slot moves (with retry) ===
		int blockedCount = 0;
		int maxPasses = 2; // Retry once — first pass frees slots for previously blocked items
		for (int pass = 0; pass < maxPasses; pass++) {
			List<WItem> moveOrder = new ArrayList<>(multiSlot);
			moveOrder.sort((a, b) -> {
				Coord tA = multiTargets.get(a), tB = multiTargets.get(b);
				Coord curA = a.c.sub(1, 1).div(sqsz), curB = b.c.sub(1, 1).div(sqsz);
				boolean aAtTarget = curA.equals(tA);
				boolean bAtTarget = curB.equals(tB);
				if (aAtTarget != bAtTarget) return aAtTarget ? -1 : 1; // already-at-target first
				boolean aFree = isAreaClear(tA, a.sz.div(sqsz), a);
				boolean bFree = isAreaClear(tB, b.sz.div(sqsz), b);
				if (aFree != bFree) return aFree ? -1 : 1; // clear-target first
				return 0;
			});

			blockedCount = 0;
			for (WItem wi : moveOrder) {
				if (!wmap.containsValue(wi)) continue;
				Coord gridSz = wi.sz.div(sqsz);
				Coord currentSlot = wi.c.sub(1, 1).div(sqsz);
				Coord targetSlot = multiTargets.get(wi);
				if (targetSlot == null || currentSlot.equals(targetSlot)) continue;

				// Clear the target area — move blocking items intelligently
				clearTargetAreaPlanned(gui, targetSlot, gridSz, wi, immovable, multiTargets);

				if (!isAreaClear(targetSlot, gridSz, wi)) {
					// On last pass, try aggressive clearing: use player inv as staging
					if (pass == maxPasses - 1) {
						clearTargetAreaAggressive(gui, targetSlot, gridSz, wi, immovable);
					}
					if (!isAreaClear(targetSlot, gridSz, wi)) {
						blockedCount++;
						continue;
					}
				}

				wi.item.wdgmsg("take", Coord.z);
				if (!waitForCursor(gui, true)) continue;
				wdgmsg("drop", targetSlot);
				Thread.sleep(5);
				if (!waitForCursorEmpty(gui)) continue;
			}

			if (blockedCount == 0) break; // All items placed, no need for retry
		}

		// Transfer any overflow items back from player inventory
		if (playerInv != null) {
			Thread.sleep(50);
			for (WItem wi : new ArrayList<>(playerInv.wmap.values())) {
				if (originalPlayerItems.contains(wi)) continue;
				if (!playerInv.wmap.containsValue(wi)) continue;
				wi.item.wdgmsg("transfer", Coord.z);
				Thread.sleep(5);
			}
			Thread.sleep(50);
		}

		// === Phase 3: Swap-chain sort all 1x1 items ===
		doQuickSort(gui);

		// Report issues
		int totalIssues = noFitCount + blockedCount;
		if (totalIssues > 0)
			gui.errorsilent("Sort: " + totalIssues + " item(s) couldn't be rearranged (not enough space)");
	}

	/**
	 * Clear blocking items from target area using planned positions.
	 * If a blocker has a planned target that's currently free, move it there directly.
	 * Otherwise, move to any free slot or overflow to player inventory.
	 */
	private void clearTargetAreaPlanned(GameUI gui, Coord target, Coord targetSize,
										WItem self, Set<WItem> immovable,
										Map<WItem, Coord> multiTargets) throws InterruptedException {
		Set<WItem> moved = new HashSet<>();
		for (int dy = 0; dy < targetSize.y; dy++) {
			for (int dx = 0; dx < targetSize.x; dx++) {
				WItem blocking = getItemAtSlot(new Coord(target.x + dx, target.y + dy));
				if (blocking == null || blocking == self) continue;
				if (immovable.contains(blocking) || moved.contains(blocking)) continue;
				if (!wmap.containsValue(blocking)) continue;
				moved.add(blocking);

				Coord blockGridSz = blocking.sz.div(sqsz);

				// Strategy 1: If the blocker has a planned multi-slot target that's free, move it there
				Coord blockerTarget = multiTargets.get(blocking);
				if (blockerTarget != null && isAreaClear(blockerTarget, blockGridSz, blocking)) {
					blocking.item.wdgmsg("take", Coord.z);
					if (!waitForCursor(gui, true)) continue;
					wdgmsg("drop", blockerTarget);
					Thread.sleep(5);
					waitForCursorEmpty(gui);
					continue;
				}

				// Strategy 2: Move within container to any free slot outside the target zone
				Coord freeSlot = findFreeSlotOutside(target, targetSize, blockGridSz.x, blockGridSz.y);
				if (freeSlot != null) {
					blocking.item.wdgmsg("take", Coord.z);
					if (!waitForCursor(gui, true)) continue;
					wdgmsg("drop", freeSlot);
					Thread.sleep(5);
					waitForCursorEmpty(gui);
					continue;
				}

				// Strategy 3: Overflow to player inventory
				GameUI g = getparent(GameUI.class);
				if (g != null && g.maininv != null
						&& g.maininv.isRoom(blockGridSz.x, blockGridSz.y) != null) {
					blocking.item.wdgmsg("transfer", Coord.z);
					Thread.sleep(10);
				}
			}
		}
	}

	/**
	 * Aggressive clearing: forcefully move ALL blockers from target area to player inventory,
	 * regardless of their planned targets. Used as last resort in tight inventories.
	 */
	private void clearTargetAreaAggressive(GameUI gui, Coord target, Coord targetSize,
										   WItem self, Set<WItem> immovable) throws InterruptedException {
		for (int dy = 0; dy < targetSize.y; dy++) {
			for (int dx = 0; dx < targetSize.x; dx++) {
				WItem blocking = getItemAtSlot(new Coord(target.x + dx, target.y + dy));
				if (blocking == null || blocking == self) continue;
				if (immovable.contains(blocking)) continue;
				if (!wmap.containsValue(blocking)) continue;

				Coord blockGridSz = blocking.sz.div(sqsz);

				// Try any free slot in this container first
				Coord freeSlot = findFreeSlotOutside(target, targetSize, blockGridSz.x, blockGridSz.y);
				if (freeSlot != null) {
					blocking.item.wdgmsg("take", Coord.z);
					if (!waitForCursor(gui, true)) continue;
					wdgmsg("drop", freeSlot);
					Thread.sleep(5);
					waitForCursorEmpty(gui);
					continue;
				}

				// Force transfer to player inventory
				GameUI g = getparent(GameUI.class);
				if (g != null && g.maininv != null) {
					blocking.item.wdgmsg("transfer", Coord.z);
					Thread.sleep(15);
				}
			}
		}
	}

	/**
	 * Quick sort: swap-chain sort 1x1 items in-place.
	 * Multi-slot items are treated as fixed obstacles.
	 */
	private void doQuickSort(GameUI gui) throws InterruptedException {
		// Build occupancy: sqmask + all non-1x1 items (fixed in place)
		boolean[][] occupied = new boolean[isz.x][isz.y];
		if (sqmask != null) {
			for (int i = 0; i < isz.x * isz.y; i++)
				if (sqmask[i]) occupied[i % isz.x][i / isz.x] = true;
		}
		for (WItem wi : new ArrayList<>(wmap.values())) {
			try {
				Coord gridSz = wi.sz.div(sqsz);
				if (gridSz.x > 1 || gridSz.y > 1) {
					Coord slot = wi.c.sub(1, 1).div(sqsz);
					markOccupied(occupied, slot, gridSz);
				}
			} catch (Loading e) {}
		}

		// Compute target slots for 1x1 items
		List<Coord> targets = new ArrayList<>();
		for (int y = 0; y < isz.y; y++)
			for (int x = 0; x < isz.x; x++)
				if (!occupied[x][y]) targets.add(new Coord(x, y));

		// Collect and sort 1x1 items
		List<WItem> singleSlot = new ArrayList<>();
		for (WItem wi : new ArrayList<>(wmap.values())) {
			try {
				Coord gridSz = wi.sz.div(sqsz);
				if (gridSz.x == 1 && gridSz.y == 1) singleSlot.add(wi);
			} catch (Loading e) {
				singleSlot.add(wi);
			}
		}
		if (singleSlot.isEmpty()) return;
		singleSlot.sort(ITEM_COMPARATOR_DESC);

		// Build position maps
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

		// Swap-chain sort
		Set<WItem> placed = new HashSet<>();
		for (int i = 0; i < singleSlot.size() && i < targets.size(); i++) {
			WItem item = singleSlot.get(i);
			if (placed.contains(item)) continue;
			Coord target = targetPos.get(item);
			Coord current = currentPos.get(item);
			if (current.equals(target)) { placed.add(item); continue; }
			if (!wmap.containsValue(item)) { placed.add(item); continue; }

			item.item.wdgmsg("take", Coord.z);
			if (!waitForCursor(gui, true)) continue;

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
					if (!waitForCursor(gui, true)) break;
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

	/** Check if all cells in the target area are free (ignoring self). */
	private boolean isAreaClear(Coord target, Coord size, WItem self) {
		for (int dy = 0; dy < size.y; dy++)
			for (int dx = 0; dx < size.x; dx++) {
				WItem at = getItemAtSlot(new Coord(target.x + dx, target.y + dy));
				if (at != null && at != self) return false;
			}
		return true;
	}

	/** Find the WItem occupying the given grid slot. */
	private WItem getItemAtSlot(Coord gridSlot) {
		for (WItem wi : new ArrayList<>(wmap.values())) {
			Coord slot = wi.c.sub(1, 1).div(sqsz);
			Coord gridSz = wi.sz.div(sqsz);
			if (gridSlot.x >= slot.x && gridSlot.x < slot.x + gridSz.x
					&& gridSlot.y >= slot.y && gridSlot.y < slot.y + gridSz.y)
				return wi;
		}
		return null;
	}

	/** Find a free slot for w*h item, excluding the given rectangular area. */
	private Coord findFreeSlotOutside(Coord excludePos, Coord excludeSize, int w, int h) {
		boolean[][] occ = new boolean[isz.x][isz.y];
		if (sqmask != null) {
			for (int i = 0; i < isz.x * isz.y; i++)
				if (sqmask[i]) occ[i % isz.x][i / isz.x] = true;
		}
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				Coord slot = wdg.c.sub(1, 1).div(sqsz);
				Coord sz = wdg.sz.div(sqsz);
				for (int dx = 0; dx < sz.x; dx++)
					for (int dy = 0; dy < sz.y; dy++) {
						int gx = slot.x + dx, gy = slot.y + dy;
						if (gx >= 0 && gx < isz.x && gy >= 0 && gy < isz.y)
							occ[gx][gy] = true;
					}
			}
		}
		for (int dx = 0; dx < excludeSize.x; dx++)
			for (int dy = 0; dy < excludeSize.y; dy++) {
				int gx = excludePos.x + dx, gy = excludePos.y + dy;
				if (gx >= 0 && gx < isz.x && gy >= 0 && gy < isz.y)
					occ[gx][gy] = true;
			}
		return findFreeSlot(occ, w, h);
	}

	/** Check if an item has contents (liquid, stacked items, etc.) */
	private static boolean hasContents(WItem wi) {
		try {
			GItem item = wi.item;
			if (item.contents != null) return true;
			if (item.getcontents() != null) return true;
		} catch (Loading ignored) {}
		return false;
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

	private boolean waitForCursor(GameUI gui, boolean wantItem) throws InterruptedException {
		for (int i = 0; i < 200; i++) {
			if (wantItem && (gui.vhand != null || !gui.hand.isEmpty())) return true;
			if (!wantItem && gui.vhand == null && gui.hand.isEmpty()) return true;
			Thread.sleep(5);
		}
		return false;
	}

	private boolean waitForCursorEmpty(GameUI gui) throws InterruptedException {
		if (waitForCursor(gui, false)) return true;
		if (gui.vhand != null) {
			// Use actual item size — multi-slot items can't fit in a 1x1 slot
			Coord itemGridSz = gui.vhand.sz.div(sqsz);
			int w = Math.max(1, itemGridSz.x);
			int h = Math.max(1, itemGridSz.y);
			Coord freeSlot = isRoom(w, h);
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
