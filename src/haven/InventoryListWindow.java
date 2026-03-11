package haven;

import java.awt.Color;
import java.util.*;

/**
 * Item list view showing inventory contents in a summarized text format.
 * Groups items by name, showing count and average quality per type.
 * Click a row to highlight items of that type in the inventory.
 */
public class InventoryListWindow extends Window {
	private final GameUI gui;
	private final Scrollport scroll;
	private final Widget listArea;
	private double lastRefresh = 0;
	private static final double REFRESH_INTERVAL = 0.5; // seconds
	private String highlightedName = null;

	// Sort modes for the list
	public enum ListSort {
		BY_NAME("Name"), BY_COUNT("Count"), BY_QUALITY("Quality");
		public final String label;
		ListSort(String label) { this.label = label; }
		public ListSort next() {
			ListSort[] vals = values();
			return vals[(ordinal() + 1) % vals.length];
		}
	}
	private ListSort sortMode = ListSort.BY_NAME;

	public InventoryListWindow(GameUI gui) {
		super(UI.scale(new Coord(220, 300)), "Inventory List");
		this.gui = gui;

		// Header row with sort button
		Button sortBtn = add(new Button(UI.scale(60), "By " + sortMode.label) {
			public void click() {
				sortMode = sortMode.next();
				this.change("By " + sortMode.label);
				refreshList();
			}
		}, UI.scale(0, 0));
		sortBtn.settip("Change list sort order");

		// Scrollable area for item rows
		scroll = new Scrollport(UI.scale(new Coord(215, 270)));
		add(scroll, UI.scale(0, 22));
		listArea = scroll.cont;

		refreshList();
	}

	public void tick(double dt) {
		super.tick(dt);
		lastRefresh += dt;
		if (lastRefresh >= REFRESH_INTERVAL) {
			lastRefresh = 0;
			refreshList();
		}
	}

	private void refreshList() {
		// Clear existing rows
		Widget nxt;
		for (Widget w = listArea.child; w != null; w = nxt) {
			nxt = w.next;
			w.destroy();
		}

		if (gui.maininv == null) return;

		// Aggregate items: name -> (count, total quality, min quality, max quality)
		Map<String, int[]> itemStats = new LinkedHashMap<>(); // [count, totalQ*100, minQ*100, maxQ*100]
		Map<String, String> itemResNames = new LinkedHashMap<>();
		final Map<String, Resource> itemResources = new LinkedHashMap<>();

		List<WItem> allItems = gui.maininv.getAllItems();
		for (WItem wi : allItems) {
			try {
				String name = wi.sortName();
				double q = wi.quality();
				int qi = (int)(q * 100);
				if (!itemStats.containsKey(name)) {
					itemStats.put(name, new int[]{1, qi, qi, qi});
					itemResNames.put(name, wi.item.resname());
					try { itemResources.put(name, wi.item.resource()); } catch (Exception ignored) {}
				} else {
					int[] stats = itemStats.get(name);
					stats[0]++;
					stats[1] += qi;
					if (qi < stats[2]) stats[2] = qi;
					if (qi > stats[3]) stats[3] = qi;
				}
			} catch (Exception ignored) {}
		}

		// Sort entries
		List<Map.Entry<String, int[]>> sorted = new ArrayList<>(itemStats.entrySet());
		switch (sortMode) {
			case BY_NAME:
				sorted.sort(Comparator.comparing(Map.Entry::getKey));
				break;
			case BY_COUNT:
				sorted.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
				break;
			case BY_QUALITY:
				sorted.sort((a, b) -> {
					double avgA = (double)a.getValue()[1] / a.getValue()[0] / 100.0;
					double avgB = (double)b.getValue()[1] / b.getValue()[0] / 100.0;
					return Double.compare(avgB, avgA);
				});
				break;
		}

		// Render rows
		int y = 0;
		int rowH = UI.scale(18);
		for (Map.Entry<String, int[]> entry : sorted) {
			String name = entry.getKey();
			int[] stats = entry.getValue();
			int count = stats[0];
			double avgQ = (double)stats[1] / count / 100.0;
			double minQ = stats[2] / 100.0;
			double maxQ = stats[3] / 100.0;

			String qStr;
			if (count == 1) {
				qStr = String.format("q%.0f", avgQ);
			} else {
				qStr = String.format("q%.0f-%.0f", minQ, maxQ);
			}
			String text = count + "x " + name + "  " + qStr;

			Color qColor = getQualityColor(avgQ);
			boolean isHighlighted = name.equals(highlightedName);

			final String itemName = name;
			final int rowY = y;
			Widget row = new Widget(new Coord(UI.scale(210), rowH)) {
				public void draw(GOut g) {
					if (isHighlighted) {
						g.chcolor(60, 60, 100, 180);
						g.frect(Coord.z, sz);
					}
					g.chcolor(qColor);
					Tex tex = Text.renderstroked(text, qColor, Color.BLACK).tex();
					int iconSz = UI.scale(14);
					int textX = iconSz + UI.scale(3);
					Resource itemRes = itemResources.get(itemName);
					if (itemRes != null) {
						try {
							Resource.Image img = itemRes.layer(Resource.imgc);
							if (img != null) {
								g.image(img.tex(), new Coord(UI.scale(1), 0), new Coord(iconSz, iconSz));
							}
						} catch (Exception ignored) {}
					}
					g.image(tex, new Coord(textX, 1));
					tex.dispose();
					g.chcolor();
				}

				public boolean mousedown(MouseDownEvent ev) {
					if (ev.b == 1) {
						if (itemName.equals(highlightedName)) {
							highlightedName = null;
						} else {
							highlightedName = itemName;
						}
						if (gui.maininv != null) {
							gui.maininv.highlightItemName = highlightedName;
						}
						refreshList();
						return true;
					}
					return super.mousedown(ev);
				}
			};
			listArea.add(row, new Coord(0, y));
			y += rowH;
		}

		// Add summary at bottom
		if (!sorted.isEmpty()) {
			y += UI.scale(4);
			int totalCount = allItems.size();
			int typeCount = sorted.size();
			String summary = totalCount + " items, " + typeCount + " types";
			Widget summaryRow = new Widget(new Coord(UI.scale(210), rowH)) {
				public void draw(GOut g) {
					g.chcolor(150, 150, 150, 255);
					Tex tex = Text.renderstroked(summary, new Color(150, 150, 150), Color.BLACK).tex();
					g.image(tex, new Coord(UI.scale(2), 1));
					tex.dispose();
					g.chcolor();
				}
			};
			listArea.add(summaryRow, new Coord(0, y));
		}
	}

	private static Color getQualityColor(double q) {
		if (q < 10) return new Color(170, 170, 170);      // Gray - trash
		if (q < 25) return new Color(255, 255, 255);       // White - common
		if (q < 50) return new Color(100, 200, 100);       // Green - decent
		if (q < 100) return new Color(100, 150, 255);      // Blue - good
		return new Color(200, 100, 255);                    // Purple - elite
	}

	private void clearHighlight() {
		highlightedName = null;
		if (gui.maininv != null)
			gui.maininv.highlightItemName = null;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && msg.equals("close")) {
			clearHighlight();
			Utils.setprefc("wndc-inventoryListWindow", this.c);
			gui.inventoryListWindow = null;
			reqdestroy();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public void reqdestroy() {
		Utils.setprefc("wndc-inventoryListWindow", this.c);
		super.reqdestroy();
	}
}
