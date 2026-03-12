package haven;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended Inventory window with grouping, filtering, and enhanced display.
 * Shows inventory items organized by configurable groups with collapsible headers,
 * item counts, average quality, and click-to-highlight support.
 */
public class ExtInventoryWindow extends Window {
	private final GameUI gui;
	private final Scrollport scroll;
	private final Widget listArea;
	private final Label itemCountLabel;
	private final TextEntry filterEntry;
	private final Button groupBtn;

	private ItemGrouping grouping = ItemGrouping.BY_NAME;
	private ItemFilter activeFilter = null;
	private String lastFilterText = "";
	private String highlightedGroupKey = null;
	private String highlightedItemName = null;

	// Dirty-flag refresh
	private boolean dirty = true;
	private int lastFingerprint = 0;
	private double lastCheckTime = 0;
	private static final double CHECK_INTERVAL = 0.5;

	// Collapsed groups tracking
	private final Set<String> collapsedGroups = new HashSet<>();

	// Tex cache
	private final Map<String, Tex> texCache = new ConcurrentHashMap<>();

	// Sort mode for items within groups
	private enum SortMode {
		BY_NAME("Name"), BY_QUALITY("Quality"), BY_COUNT("Count");
		public final String label;
		SortMode(String label) { this.label = label; }
		public SortMode next() {
			SortMode[] vals = values();
			return vals[(ordinal() + 1) % vals.length];
		}
	}
	private SortMode sortMode = SortMode.BY_NAME;

	// Stored inventory reference (null means use gui.maininv)
	private Inventory targetInv;

	private static final int WIN_W = 300;
	private static final int WIN_H = 420;
	private static final int ROW_H = 18;
	private static final int HEADER_H = 20;
	private static final int ICON_SZ = 14;

	public ExtInventoryWindow(GameUI gui) {
		this(gui, null);
	}

	public ExtInventoryWindow(GameUI gui, Inventory inv) {
		super(UI.scale(new Coord(WIN_W, WIN_H)), "Extended Inventory");
		this.gui = gui;
		this.targetInv = inv;

		int y = 0;

		// --- Control bar ---
		groupBtn = add(new Button(UI.scale(80), grouping.label) {
			public void click() {
				grouping = grouping.next();
				this.change(grouping.label);
				collapsedGroups.clear();
				dirty = true;
			}
		}, UI.scale(0, y));
		groupBtn.settip("Cycle grouping mode");

		Button sortBtn = add(new Button(UI.scale(55), "By " + sortMode.label) {
			public void click() {
				sortMode = sortMode.next();
				this.change("By " + sortMode.label);
				dirty = true;
			}
		}, groupBtn.pos("ur").adds(4, 0));
		sortBtn.settip("Change sort order within groups");

		Button invSortBtn = add(new Button(UI.scale(45), "Sort") {
			public void click() {
				Inventory inv = getTargetInv();
				if (inv != null) inv.sortInventory();
			}
		}, sortBtn.pos("ur").adds(4, 0));
		invSortBtn.settip("Sort the actual inventory grid");

		itemCountLabel = add(new Label(""), invSortBtn.pos("ur").adds(6, 3));
		y += UI.scale(22);

		// --- Filter bar ---
		add(new Label("Filter:"), UI.scale(0, y + 2));
		filterEntry = add(new TextEntry(UI.scale(WIN_W - 75), "") {
			@Override
			protected void changed() {
				String text = this.buf.line().trim();
				if (!text.equals(lastFilterText)) {
					lastFilterText = text;
					activeFilter = ItemFilter.parse(text);
					dirty = true;
				}
			}
		}, UI.scale(40, y));

		Button clearBtn = add(new Button(UI.scale(25), "X") {
			public void click() {
				filterEntry.settext("");
				lastFilterText = "";
				activeFilter = null;
				dirty = true;
			}
		}, filterEntry.pos("ur").adds(4, 0));
		clearBtn.settip("Clear filter");
		y += UI.scale(24);

		// --- Scrollable group list ---
		int scrollH = UI.scale(WIN_H) - y - UI.scale(4);
		scroll = new Scrollport(new Coord(UI.scale(WIN_W - 4), scrollH));
		add(scroll, UI.scale(0, y));
		listArea = scroll.cont;

		dirty = true;
	}

	private Inventory getTargetInv() {
		if (targetInv != null) return targetInv;
		return gui.maininv;
	}

	@Override
	public void tick(double dt) {
		super.tick(dt);
		lastCheckTime += dt;
		if (lastCheckTime >= CHECK_INTERVAL) {
			lastCheckTime = 0;
			int fp = computeFingerprint();
			if (fp != lastFingerprint) {
				lastFingerprint = fp;
				dirty = true;
			}
			if (dirty) {
				dirty = false;
				refreshList();
			}
		}
	}

	private int computeFingerprint() {
		Inventory inv = getTargetInv();
		if (inv == null) return 0;
		int hash = 0;
		int count = 0;
		for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				WItem wi = (WItem) wdg;
				count++;
				try {
					hash += wi.sortName().hashCode();
					hash += Double.hashCode(wi.quality());
				} catch (Exception ignored) {}
			}
		}
		return hash * 31 + count;
	}

	/**
	 * Represents aggregated data for a group of items.
	 */
	private static class GroupData {
		String key;
		int count;
		double totalQ;
		double minQ;
		double maxQ;
		Resource iconRes;
		// Item name -> ItemRow (for sub-items within the group)
		Map<String, ItemRow> items = new LinkedHashMap<>();
	}

	private static class ItemRow {
		String name;
		int count;
		double totalQ;
		double minQ;
		double maxQ;
		Resource iconRes;
	}

	private void refreshList() {
		// Dispose cached Tex objects
		for (Tex t : texCache.values()) {
			try { t.dispose(); } catch (Exception ignored) {}
		}
		texCache.clear();

		// Clear existing rows
		Widget nxt;
		for (Widget w = listArea.child; w != null; w = nxt) {
			nxt = w.next;
			w.destroy();
		}

		Inventory inv = getTargetInv();
		if (inv == null) return;

		// Collect all items from inventory
		List<WItem> allItems = new ArrayList<>();
		for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				WItem wi = (WItem) wdg;
				// Apply filter
				if (activeFilter != null) {
					try {
						if (!activeFilter.matches(wi)) continue;
					} catch (Exception ignored) { continue; }
				}
				allItems.add(wi);
			}
		}

		// Update item count label
		int totalAll = 0;
		for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) totalAll++;
		}
		int filteredCount = allItems.size();
		if (activeFilter != null) {
			itemCountLabel.settext(filteredCount + "/" + totalAll);
		} else {
			int capacity = inv.isz.x * inv.isz.y;
			if (inv.sqmask != null) {
				for (boolean b : inv.sqmask) if (b) capacity--;
			}
			itemCountLabel.settext(totalAll + "/" + capacity);
		}

		if (allItems.isEmpty()) return;

		// Build group data
		Map<String, GroupData> groups = new LinkedHashMap<>();

		for (WItem wi : allItems) {
			String groupKey;
			if (grouping == ItemGrouping.NONE) {
				groupKey = "All Items";
			} else {
				groupKey = grouping.groupKey(wi);
			}

			GroupData gd = groups.get(groupKey);
			if (gd == null) {
				gd = new GroupData();
				gd.key = groupKey;
				gd.minQ = Double.MAX_VALUE;
				groups.put(groupKey, gd);
			}

			double q = 0;
			String itemName = "?";
			try {
				q = wi.quality();
				itemName = wi.sortName();
			} catch (Exception ignored) {}

			gd.count++;
			gd.totalQ += q;
			if (q < gd.minQ) gd.minQ = q;
			if (q > gd.maxQ) gd.maxQ = q;
			if (gd.iconRes == null) {
				try { gd.iconRes = wi.item.resource(); } catch (Exception ignored) {}
			}

			// Build item sub-row data
			ItemRow row = gd.items.get(itemName);
			if (row == null) {
				row = new ItemRow();
				row.name = itemName;
				row.minQ = Double.MAX_VALUE;
				gd.items.put(itemName, row);
				try { row.iconRes = wi.item.resource(); } catch (Exception ignored) {}
			}
			row.count++;
			row.totalQ += q;
			if (q < row.minQ) row.minQ = q;
			if (q > row.maxQ) row.maxQ = q;
		}

		// Sort groups
		List<GroupData> sortedGroups = new ArrayList<>(groups.values());
		if (grouping != ItemGrouping.NONE) {
			sortedGroups.sort(Comparator.comparing(g -> grouping.groupSortKey(g.key)));
		}

		// Render groups
		int y = 0;
		int rowW = UI.scale(WIN_W - 20); // account for scrollbar

		for (GroupData gd : sortedGroups) {
			boolean collapsed = collapsedGroups.contains(gd.key);
			double avgQ = gd.count > 0 ? gd.totalQ / gd.count : 0;

			// --- Group header ---
			if (grouping != ItemGrouping.NONE || sortedGroups.size() > 1) {
				final String gKey = gd.key;
				final int headerY = y;
				String arrow = collapsed ? "\u25B6" : "\u25BC";
				String headerText = arrow + " " + gd.key + " (" + gd.count + ")  avg q" + (int) avgQ;
				final GroupData fgd = gd;

				Widget header = new Widget(new Coord(rowW, UI.scale(HEADER_H))) {
					public void draw(GOut g) {
						// Background
						g.chcolor(40, 50, 65, 220);
						g.frect(Coord.z, sz);
						g.chcolor();

						int x = UI.scale(2);
						// Group icon
						if (fgd.iconRes != null) {
							try {
								Resource.Image img = fgd.iconRes.layer(Resource.imgc);
								if (img != null) {
									g.image(img.tex(), new Coord(x, UI.scale(2)), new Coord(UI.scale(ICON_SZ + 2), UI.scale(ICON_SZ + 2)));
									x += UI.scale(ICON_SZ + 4);
								}
							} catch (Exception ignored) {}
						}

						// Header text
						String ck = "hdr|" + headerText;
						Tex tex = texCache.get(ck);
						if (tex == null) {
							tex = Text.renderstroked(headerText, Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
							texCache.put(ck, tex);
						}
						g.image(tex, new Coord(x, UI.scale(2)));

						// Bottom border
						g.chcolor(80, 100, 130, 180);
						g.line(new Coord(0, sz.y - 1), new Coord(sz.x, sz.y - 1), 1);
						g.chcolor();
					}

					public boolean mousedown(MouseDownEvent ev) {
						if (ev.b == 1) {
							if (collapsedGroups.contains(gKey)) {
								collapsedGroups.remove(gKey);
							} else {
								collapsedGroups.add(gKey);
							}
							dirty = true;
							return true;
						}
						return super.mousedown(ev);
					}
				};
				header.settip("Click to collapse/expand this group");
				listArea.add(header, new Coord(0, y));
				y += UI.scale(HEADER_H);
			}

			// --- Item rows (if not collapsed) ---
			if (!collapsed) {
				// Sort item rows within group
				List<ItemRow> sortedRows = new ArrayList<>(gd.items.values());
				switch (sortMode) {
					case BY_NAME:
						sortedRows.sort(Comparator.comparing(r -> r.name.toLowerCase()));
						break;
					case BY_QUALITY:
						sortedRows.sort((a, b) -> {
							double aq = a.count > 0 ? a.totalQ / a.count : 0;
							double bq = b.count > 0 ? b.totalQ / b.count : 0;
							return Double.compare(bq, aq);
						});
						break;
					case BY_COUNT:
						sortedRows.sort((a, b) -> Integer.compare(b.count, a.count));
						break;
				}

				for (ItemRow row : sortedRows) {
					final ItemRow frow = row;
					final String rowName = row.name;
					double rowAvgQ = row.count > 0 ? row.totalQ / row.count : 0;
					Color qColor = getQualityColor(rowAvgQ);

					String qStr;
					if (row.count == 1) {
						qStr = "q" + (int) row.minQ;
					} else if (row.minQ == row.maxQ) {
						qStr = "q" + (int) row.minQ;
					} else {
						qStr = "q" + (int) row.minQ + "-" + (int) row.maxQ;
					}
					String rowText = row.count + "x " + row.name + "  " + qStr;
					boolean isHighlighted = rowName.equals(highlightedItemName);

					Widget rowWidget = new Widget(new Coord(rowW, UI.scale(ROW_H))) {
						public void draw(GOut g) {
							// Hover / highlight background
							if (isHighlighted) {
								g.chcolor(60, 60, 100, 180);
								g.frect(Coord.z, sz);
							}

							int x = UI.scale(4);
							// Item icon
							if (frow.iconRes != null) {
								try {
									Resource.Image img = frow.iconRes.layer(Resource.imgc);
									if (img != null) {
										g.image(img.tex(), new Coord(x, UI.scale(1)), new Coord(UI.scale(ICON_SZ), UI.scale(ICON_SZ)));
									}
								} catch (Exception ignored) {}
							}
							x += UI.scale(ICON_SZ + 3);

							// Row text
							String ck = "row|" + rowText;
							Tex tex = texCache.get(ck);
							if (tex == null) {
								tex = Text.renderstroked(rowText, qColor, Color.BLACK).tex();
								texCache.put(ck, tex);
							}
							g.image(tex, new Coord(x, UI.scale(1)));
							g.chcolor();
						}

						public boolean mousedown(MouseDownEvent ev) {
							if (ev.b == 1) {
								toggleHighlight(rowName);
								return true;
							}
							return super.mousedown(ev);
						}
					};
					listArea.add(rowWidget, new Coord(0, y));
					y += UI.scale(ROW_H);
				}
			}
		}

		// Summary row
		y += UI.scale(4);
		int totalTypes = 0;
		for (GroupData gd : sortedGroups) totalTypes += gd.items.size();
		String summary = filteredCount + " items, " + totalTypes + " types, " + sortedGroups.size() + " groups";
		Tex summaryTex = Text.renderstroked(summary, new Color(150, 150, 150), Color.BLACK).tex();
		texCache.put("summary", summaryTex);
		Widget summaryRow = new Widget(new Coord(rowW, UI.scale(ROW_H))) {
			public void draw(GOut g) {
				Tex t = texCache.get("summary");
				if (t != null) {
					g.chcolor(150, 150, 150, 255);
					g.image(t, new Coord(UI.scale(4), UI.scale(1)));
					g.chcolor();
				}
			}
		};
		listArea.add(summaryRow, new Coord(0, y));
	}

	private void toggleHighlight(String itemName) {
		Inventory inv = getTargetInv();
		if (itemName.equals(highlightedItemName)) {
			highlightedItemName = null;
			if (inv != null) inv.highlightItemName = null;
		} else {
			highlightedItemName = itemName;
			if (inv != null) inv.highlightItemName = itemName;
		}
		dirty = true;
	}

	private static Color getQualityColor(double q) {
		if (q < 10) return new Color(170, 170, 170);
		if (q < 25) return new Color(255, 255, 255);
		if (q < 50) return new Color(100, 200, 100);
		if (q < 100) return new Color(100, 150, 255);
		return new Color(200, 100, 255);
	}

	private void clearHighlight() {
		highlightedItemName = null;
		Inventory inv = getTargetInv();
		if (inv != null) inv.highlightItemName = null;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && msg.equals("close")) {
			clearHighlight();
			Utils.setprefc("wndc-extInventoryWindow", this.c);
			gui.extInventoryWindow = null;
			reqdestroy();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public void reqdestroy() {
		Utils.setprefc("wndc-extInventoryWindow", this.c);
		for (Tex t : texCache.values()) {
			try { t.dispose(); } catch (Exception ignored) {}
		}
		texCache.clear();
		super.reqdestroy();
	}
}
