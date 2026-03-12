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

	// Grouping modes for visual inventory organization
	public enum GroupingMode {
		NONE("No Groups"), BY_NAME("By Name"), BY_QUALITY("By Quality");
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

	// Cached group header hit areas for click detection
	private final Map<String, Coord[]> groupHeaderAreas = new LinkedHashMap<>();

	// Cached group header Tex objects — invalidated when groups change
	private final Map<String, Tex> groupHeaderTexCache = new HashMap<>();
	// Cached icon Tex objects for group header icons — keyed by resource name
	private final Map<String, Tex> groupHeaderIconCache = new HashMap<>();
	private GroupingMode lastCachedGroupingMode = null;
	private int lastCachedChildCount = -1;

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

	// Quality bracket comparator: groups by quality range, then name, then quality
	public static final Comparator<WItem> ITEM_COMPARATOR_QUALITY_BRACKET = Comparator
			.comparing((WItem w) -> getQualityBracket(w.quality()))
			.thenComparing(WItem::sortName)
			.thenComparing(w -> w.item.resname())
			.thenComparing(WItem::quality, Comparator.reverseOrder());

	private static int getQualityBracket(double q) {
		if (q < 10) return 0;
		if (q < 25) return 1;
		if (q < 50) return 2;
		if (q < 100) return 3;
		return 4;
	}

	private static String getQualityBracketLabel(double q) {
		if (q < 10) return "Q 0-10";
		if (q < 25) return "Q 10-25";
		if (q < 50) return "Q 25-50";
		if (q < 100) return "Q 50-100";
		return "Q 100+";
	}

	private String getGroupKey(WItem wi) {
		try {
			switch (groupingMode) {
				case BY_NAME:
					return wi.sortName();
				case BY_QUALITY:
					return getQualityBracketLabel(wi.quality());
				default:
					return "";
			}
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
	// Hide collapsed items before drawing children
	if (groupingMode != GroupingMode.NONE && !collapsedGroups.isEmpty()) {
	    for (Widget wdg = child; wdg != null; wdg = wdg.next) {
		if (wdg instanceof WItem) {
		    WItem wi = (WItem) wdg;
		    String key = getCachedGroupKey(wi);
		    if (collapsedGroups.contains(key)) {
			wi.visible = false;
		    }
		}
	    }
	}
	super.draw(g);
	// Restore visibility after draw
	if (groupingMode != GroupingMode.NONE && !collapsedGroups.isEmpty()) {
	    for (Widget wdg = child; wdg != null; wdg = wdg.next) {
		if (wdg instanceof WItem) {
		    ((WItem) wdg).visible = true;
		}
	    }
	}
	// Draw group headers on top of everything
	if (groupingMode != GroupingMode.NONE) {
	    drawGroupHeaders(g);
	}
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
		boolean collapsed = collapsedGroups.contains(key);
		if (collapsed) {
		    // Dimmed overlay for collapsed groups
		    g.chcolor(40, 40, 40, 140);
		} else {
		    g.chcolor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
		}
		g.frect(wi.c.sub(1, 1), wi.sz.add(2, 2));
		g.chcolor();
	    }
	}
    }

    private void invalidateGroupHeaderTexCache() {
	for (Tex t : groupHeaderTexCache.values()) {
	    try { t.dispose(); } catch (Exception ignored) {}
	}
	groupHeaderTexCache.clear();
	// Clear icon cache references (don't dispose — they are owned by Resource.Image)
	groupHeaderIconCache.clear();
    }

    private void drawGroupHeaders(GOut g) {
	// Build group info: first item position + item count + representative icon
	Map<String, Coord> groupFirstPos = new LinkedHashMap<>();
	Map<String, Integer> groupCounts = new LinkedHashMap<>();
	Map<String, WItem> groupRepItem = new LinkedHashMap<>();

	int childCount = 0;
	for (Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if (wdg instanceof WItem) {
		childCount++;
		WItem wi = (WItem) wdg;
		String key = getCachedGroupKey(wi);
		groupCounts.merge(key, 1, Integer::sum);
		if (!groupFirstPos.containsKey(key)) {
		    groupFirstPos.put(key, wi.c.sub(1, 1));
		    groupRepItem.put(key, wi);
		} else {
		    // Track topmost-leftmost position
		    Coord cur = groupFirstPos.get(key);
		    if (wi.c.y < cur.y || (wi.c.y == cur.y && wi.c.x < cur.x)) {
			groupFirstPos.put(key, wi.c.sub(1, 1));
			groupRepItem.put(key, wi);
		    }
		}
	    }
	}

	// Invalidate header Tex cache if grouping mode or item count changed
	if (lastCachedGroupingMode != groupingMode || lastCachedChildCount != childCount) {
	    invalidateGroupHeaderTexCache();
	    lastCachedGroupingMode = groupingMode;
	    lastCachedChildCount = childCount;
	}

	groupHeaderAreas.clear();
	int headerH = UI.scale(12);

	for (Map.Entry<String, Coord> entry : groupFirstPos.entrySet()) {
	    String key = entry.getKey();
	    Coord pos = entry.getValue();
	    int count = groupCounts.getOrDefault(key, 0);
	    boolean collapsed = collapsedGroups.contains(key);

	    // Build cache key that includes collapse state and count
	    String cacheKey = key + "|" + collapsed + "|" + count;

	    // Draw header background
	    String label = collapsed ? "\u25B6 " + key + " (" + count + ")" : "\u25BC " + key + " (" + count + ")";
	    Tex labelTex = groupHeaderTexCache.get(cacheKey);
	    if (labelTex == null) {
		try {
		    labelTex = Text.renderstroked(label, java.awt.Color.WHITE, java.awt.Color.BLACK, Text.num12boldFnd).tex();
		    groupHeaderTexCache.put(cacheKey, labelTex);
		} catch (Exception e) {
		    continue;
		}
	    }

	    // Draw small item icon next to label
	    int iconSz = UI.scale(11);
	    Coord headerPos = new Coord(pos.x, pos.y - headerH);
	    Coord headerSz = new Coord(labelTex.sz().x + iconSz + UI.scale(4), headerH);

	    // Semi-transparent header background
	    g.chcolor(0, 0, 0, 160);
	    g.frect(headerPos, headerSz);
	    g.chcolor();

	    // Draw representative item icon (cached)
	    WItem rep = groupRepItem.get(key);
	    if (rep != null) {
		try {
		    Resource res = rep.item.resource();
		    if (res != null) {
			String resName = res.name;
			Tex iconTex = groupHeaderIconCache.get(resName);
			if (iconTex == null) {
			    Resource.Image img = res.layer(Resource.imgc);
			    if (img != null) {
				iconTex = img.tex();
				groupHeaderIconCache.put(resName, iconTex);
			    }
			}
			if (iconTex != null) {
			    g.image(iconTex, headerPos.add(1, 0), new Coord(iconSz, iconSz));
			}
		    }
		} catch (Exception ignored) {}
	    }

	    // Draw label text
	    g.image(labelTex, headerPos.add(iconSz + UI.scale(3), 0));

	    // Store header area for click detection
	    groupHeaderAreas.put(key, new Coord[]{headerPos, headerSz});
	}
    }

    public boolean mousedown(MouseDownEvent ev) {
	if (groupingMode != GroupingMode.NONE && ev.b == 1) {
	    Coord mc = ev.c;
	    for (Map.Entry<String, Coord[]> entry : groupHeaderAreas.entrySet()) {
		Coord pos = entry.getValue()[0];
		Coord sz = entry.getValue()[1];
		if (mc.x >= pos.x && mc.x <= pos.x + sz.x &&
		    mc.y >= pos.y && mc.y <= pos.y + sz.y) {
		    String key = entry.getKey();
		    if (collapsedGroups.contains(key)) {
			collapsedGroups.remove(key);
		    } else {
			collapsedGroups.add(key);
		    }
		    return true;
		}
	    }
	}
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

	isContainerInventory = true;
	int toolbarH = UI.scale(22);
	int btnY = sz.y + UI.scale(2);

	Button sortBtn = add(new Button(UI.scale(40), "Sort") {
	    public void click() {
		sortInventory();
	    }
	}, new Coord(UI.scale(1), btnY));
	sortBtn.settip("Sort items by type, then quality (Ctrl+Shift+S)");

	containerGroupBtn = add(new Button(UI.scale(65), groupingMode.label) {
	    public void click() {
		groupingMode = groupingMode.next();
		collapsedGroups.clear();
		this.change(groupingMode.label);
	    }
	}, sortBtn.pos("ur").adds(4, 0));
	containerGroupBtn.settip("Cycle grouping mode");

	Button extBtn = add(new Button(UI.scale(35), "Ext") {
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
	}, containerGroupBtn.pos("ur").adds(4, 0));
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
	private Thread sortThread;

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
				sorting = false;
				sortThread = null;
			}
		}, "InventorySort");
		sortThread.start();
	}

	private void doSort(GameUI gui) throws InterruptedException {
		// Collect all items, separate 1x1 from multi-slot
		List<WItem> sortable = new ArrayList<>();
		boolean[][] blocked = new boolean[isz.x][isz.y];

		// Mark sqmask-blocked cells
		if (sqmask != null) {
			for (int i = 0; i < isz.x * isz.y; i++) {
				if (sqmask[i])
					blocked[i % isz.x][i / isz.x] = true;
			}
		}

		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				WItem wi = (WItem) wdg;
				Coord gridSz = wi.sz.div(sqsz);
				if (gridSz.x == 1 && gridSz.y == 1) {
					sortable.add(wi);
				} else {
					// Mark multi-slot items as blocked
					Coord gridPos = wi.c.sub(1, 1).div(sqsz);
					for (int dx = 0; dx < gridSz.x; dx++) {
						for (int dy = 0; dy < gridSz.y; dy++) {
							int gx = gridPos.x + dx, gy = gridPos.y + dy;
							if (gx >= 0 && gx < isz.x && gy >= 0 && gy < isz.y)
								blocked[gx][gy] = true;
						}
					}
				}
			}
		}

		if (sortable.isEmpty()) return;

		// Sort items using grouping-mode-aware comparator
		Comparator<WItem> comparator = (groupingMode == GroupingMode.BY_QUALITY)
			? ITEM_COMPARATOR_QUALITY_BRACKET
			: ITEM_COMPARATOR_DESC;
		sortable.sort(comparator);

		// Build target positions: scan left-to-right, top-to-bottom, skip blocked
		List<Coord> targets = new ArrayList<>();
		for (int y = 0; y < isz.y; y++) {
			for (int x = 0; x < isz.x; x++) {
				if (!blocked[x][y]) {
					targets.add(new Coord(x, y));
				}
			}
		}

		// Build current position map and target assignment
		Map<WItem, Coord> currentPos = new HashMap<>();
		Map<Coord, WItem> posToItem = new HashMap<>();
		for (WItem wi : sortable) {
			Coord gp = wi.c.sub(1, 1).div(sqsz);
			currentPos.put(wi, gp);
			posToItem.put(gp, wi);
		}

		// Assign targets: sortable[i] should go to targets[i]
		Map<WItem, Coord> targetPos = new HashMap<>();
		for (int i = 0; i < sortable.size() && i < targets.size(); i++) {
			targetPos.put(sortable.get(i), targets.get(i));
		}

		// Swap-chain sort: move items to their target positions
		Set<WItem> placed = new HashSet<>();
		for (int i = 0; i < sortable.size() && i < targets.size(); i++) {
			WItem item = sortable.get(i);
			if (placed.contains(item)) continue;
			Coord target = targetPos.get(item);
			Coord current = currentPos.get(item);
			if (current.equals(target)) {
				placed.add(item);
				continue;
			}

			// Start chain: pick up this item
			item.item.wdgmsg("take", Coord.z);
			if (!waitForCursor(gui, true)) return;
			Thread.sleep(30);

			// Follow the chain
			WItem carrying = item;
			while (carrying != null && !placed.contains(carrying)) {
				Coord dropTarget = targetPos.get(carrying);
				placed.add(carrying);

				// Check what's at the drop target
				WItem displaced = posToItem.get(dropTarget);

				// Drop at target
				wdgmsg("drop", dropTarget);
				Thread.sleep(30);

				// Update tracking
				posToItem.remove(currentPos.get(carrying));
				currentPos.put(carrying, dropTarget);
				posToItem.put(dropTarget, carrying);

				if (displaced != null && !placed.contains(displaced)) {
					// The displaced item is now on cursor
					if (!waitForCursor(gui, true)) return;
					carrying = displaced;
				} else {
					// Chain ended, cursor should be empty
					waitForCursor(gui, false);
					carrying = null;
				}
			}

			// Safety: if cursor still has item, drop it back into inventory
			if (gui.vhand != null) {
				Coord freeSlot = isRoom(1, 1);
				if (freeSlot != null) {
					wdgmsg("drop", freeSlot);
				} else {
					gui.vhand.item.wdgmsg("drop", Coord.z);
				}
				waitForCursor(gui, false);
			}
		}
	}

	private boolean waitForCursor(GameUI gui, boolean wantItem) throws InterruptedException {
		for (int i = 0; i < 200; i++) {
			if (wantItem && (gui.vhand != null || !gui.hand.isEmpty())) return true;
			if (!wantItem && gui.vhand == null && gui.hand.isEmpty()) return true;
			Thread.sleep(5);
		}
		return false;
	}
}
