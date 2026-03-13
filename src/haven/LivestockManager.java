package haven;

import haven.automated.GobHelper;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class LivestockManager extends Window {
	private final GameUI gui;
	private int scrollOffset = 0;
	private List<AnimalEntry> animals = new ArrayList<>();
	private long lastRefresh = 0;
	private static final long REFRESH_INTERVAL = 2000;
	private static final int VISIBLE_ROWS = 14;
	private static final int ROW_H = UI.scale(16);
	private static final int WIN_W = 520;
	private static final int TABLE_W = UI.scale(WIN_W - 20);
	private static final int TABLE_H = VISIBLE_ROWS * ROW_H;
	private static final Text.Foundry fndr = new Text.Foundry(Text.sans, 11).aa(true);
	private static final Text.Foundry fndb = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 11).aa(true);
	private int sortCol = 3; // default sort by distance
	private boolean sortAsc = true;

	// Column x offsets
	private static final int COL_NAME_X = 0;
	private static final int COL_TYPE_X = UI.scale(130);
	private static final int COL_QUALITY_X = UI.scale(210);
	private static final int COL_DIST_X = UI.scale(260);
	private static final int COL_STATUS_X = UI.scale(320);
	private static final int COL_INFO_X = UI.scale(400);

	// Animal categories
	private static final Set<String> LIVESTOCK = new HashSet<>(Arrays.asList(
		"cattle", "calf", "sheep", "lamb", "nanny", "billy", "kid",
		"hog", "sow", "piglet", "horse", "foal", "mare", "stallion",
		"chicken", "hen", "rooster", "chick"
	));
	private static final Set<String> PREDATORS = new HashSet<>(Arrays.asList(
		"bear", "polarbear", "wolf", "lynx", "wolverine", "adder",
		"caveangler", "cavelouse", "orca", "nidbane"
	));

	private CheckBox livestockCb, wildCb, predatorCb;

	public LivestockManager(GameUI gui) {
		super(UI.scale(WIN_W, 330), "Livestock Manager", true);
		this.gui = gui;

		int y = 0;
		livestockCb = add(new CheckBox("Livestock") {
			{a = true;}
			public void changed(boolean val) { refreshNow(); }
		}, UI.scale(5, y));
		wildCb = add(new CheckBox("Wild") {
			{a = true;}
			public void changed(boolean val) { refreshNow(); }
		}, UI.scale(100, y));
		predatorCb = add(new CheckBox("Predators") {
			{a = false;}
			public void changed(boolean val) { refreshNow(); }
		}, UI.scale(170, y));

		add(new Button(UI.scale(70), "Refresh") {
			public void click() { refreshNow(); }
		}, UI.scale(380, y - 4));

		this.c = Utils.getprefc("wndc-livestockManager", UI.unscale(new Coord(200, 100)));
		pack();
	}

	private void refreshNow() {
		lastRefresh = 0;
	}

	private void refreshAnimals() {
		if (gui == null || gui.map == null) return;
		Gob player = gui.map.player();
		if (player == null) return;
		Coord2d playerPos = player.rc;

		List<Gob> gobs = GobHelper.findAll(gui, 2000, GobHelper::isAnimal);
		List<AnimalEntry> entries = new ArrayList<>();

		for (Gob gob : gobs) {
			try {
				String resName = GobHelper.getResName(gob);
				if (resName == null) continue;

				String species = extractSpecies(resName);
				String variant = extractVariant(resName);
				String category = classifyAnimal(variant);

				if ("Livestock".equals(category) && !livestockCb.a) continue;
				if ("Wild".equals(category) && !wildCb.a) continue;
				if ("Predator".equals(category) && !predatorCb.a) continue;

				double dist = gob.rc.dist(playerPos);
				boolean knocked = GobHelper.isKnocked(gob);
				GobHealth health = gob.getattr(GobHealth.class);
				float hp = (health != null) ? health.hp : 1.0f;

				// Try to get roster data (quality, custom name, breeding info)
				double quality = -1;
				String customName = null;
				String info = "";
				CattleId cattleId = gob.getattr(CattleId.class);
				if (cattleId != null) {
					try {
						Entry entry = cattleId.entry();
						if (entry != null) {
							quality = entry.q;
							if (entry.name != null && !entry.name.isEmpty())
								customName = entry.name;
							info = extractBreedingInfo(entry);
						}
					} catch (Exception ignored) {}
				}

				String displayName = customName != null ? customName :
					prettifyName(variant.isEmpty() ? species : variant);
				String status = knocked ? "Knocked" : (hp < 1.0f ? String.format("%d%%", Math.round(hp * 100)) : "Alive");

				entries.add(new AnimalEntry(displayName, category, dist, status, knocked, hp, quality, info, gob.id));
			} catch (Loading ignored) {}
		}

		animals = entries;
		sortAnimals();
		lastRefresh = System.currentTimeMillis();
	}

	private void sortAnimals() {
		Comparator<AnimalEntry> cmp;
		switch (sortCol) {
			case 0: cmp = Comparator.comparing(e -> e.name.toLowerCase()); break;
			case 1: cmp = Comparator.comparing(e -> e.category); break;
			case 2: cmp = Comparator.comparingDouble(e -> e.quality); break;
			case 3: cmp = Comparator.comparingDouble(e -> e.distance); break;
			case 4: cmp = Comparator.comparing(e -> e.status); break;
			case 5: cmp = Comparator.comparing(e -> e.info); break;
			default: cmp = Comparator.comparingDouble(e -> e.distance); break;
		}
		if (!sortAsc) cmp = cmp.reversed();
		animals.sort(cmp);
	}

	private static String extractSpecies(String resName) {
		String[] parts = resName.split("/");
		return parts.length >= 3 ? parts[2] : resName;
	}

	private static String extractVariant(String resName) {
		String[] parts = resName.split("/");
		return parts.length >= 4 ? parts[3] : "";
	}

	private static String classifyAnimal(String variant) {
		if (LIVESTOCK.contains(variant)) return "Livestock";
		if (PREDATORS.contains(variant)) return "Predator";
		return "Wild";
	}

	private static String prettifyName(String raw) {
		if (raw.isEmpty()) return "Unknown";
		return raw.substring(0, 1).toUpperCase() + raw.substring(1);
	}

	/**
	 * Extract breeding info from Entry subclass via reflection.
	 * Entry subclasses (loaded from .res) typically have boolean fields:
	 * sex (true=male), dead, preg (pregnant), lact (lactating), child (young).
	 */
	private static String extractBreedingInfo(Entry entry) {
		StringBuilder sb = new StringBuilder();
		try {
			Class<?> cls = entry.getClass();
			// Sex
			Boolean sex = getBoolField(cls, entry, "sex");
			if (sex != null)
				sb.append(sex ? "\u2642" : "\u2640"); // male/female symbols
			// Growth
			Boolean isChild = getBoolField(cls, entry, "child");
			if (isChild != null && isChild)
				sb.append(sb.length() > 0 ? " " : "").append("Young");
			// Dead
			Boolean dead = getBoolField(cls, entry, "dead");
			if (dead != null && dead)
				sb.append(sb.length() > 0 ? " " : "").append("Dead");
			// Pregnant
			Boolean preg = getBoolField(cls, entry, "preg");
			if (preg != null && preg)
				sb.append(sb.length() > 0 ? " " : "").append("Preg");
			// Lactating
			Boolean lact = getBoolField(cls, entry, "lact");
			if (lact != null && lact)
				sb.append(sb.length() > 0 ? " " : "").append("Lact");
			// Ownership
			Integer owned = getIntField(cls, entry, "owned");
			if (owned != null && owned > 0)
				sb.append(sb.length() > 0 ? " " : "").append(owned == 3 ? "Mine" : "Other");
		} catch (Exception ignored) {}
		return sb.toString();
	}

	private static Boolean getBoolField(Class<?> cls, Object obj, String name) {
		try {
			Field f = cls.getField(name);
			return f.getBoolean(obj);
		} catch (Exception e) {
			return null;
		}
	}

	private static Integer getIntField(Class<?> cls, Object obj, String name) {
		try {
			Field f = cls.getField(name);
			return f.getInt(obj);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void tick(double dt) {
		super.tick(dt);
		if (System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL)
			refreshAnimals();
	}

	@Override
	public void draw(GOut g) {
		super.draw(g);

		Coord tablePos = UI.scale(new Coord(10, 26));

		// Header background
		g.chcolor(40, 40, 60, 200);
		g.frect(tablePos, new Coord(TABLE_W, ROW_H));
		g.chcolor();

		// Column headers
		drawHeader(g, tablePos, "Name", COL_NAME_X, 0);
		drawHeader(g, tablePos, "Type", COL_TYPE_X, 1);
		drawHeader(g, tablePos, "Q", COL_QUALITY_X, 2);
		drawHeader(g, tablePos, "Dist", COL_DIST_X, 3);
		drawHeader(g, tablePos, "Status", COL_STATUS_X, 4);
		drawHeader(g, tablePos, "Info", COL_INFO_X, 5);

		// Table background
		Coord bodyPos = tablePos.add(0, ROW_H);
		g.chcolor(0, 0, 0, 160);
		g.frect(bodyPos, new Coord(TABLE_W, TABLE_H));
		g.chcolor();

		// Rows
		int startRow = Math.max(0, scrollOffset);
		int endRow = Math.min(animals.size(), startRow + VISIBLE_ROWS);
		for (int i = startRow; i < endRow; i++) {
			int yOff = (i - startRow) * ROW_H;
			AnimalEntry e = animals.get(i);

			if ((i - startRow) % 2 == 1) {
				g.chcolor(255, 255, 255, 15);
				g.frect(bodyPos.add(0, yOff), new Coord(TABLE_W, ROW_H));
				g.chcolor();
			}

			Color nameColor = getTypeColor(e.category);
			drawCell(g, bodyPos, e.name, COL_NAME_X, yOff, nameColor);
			drawCell(g, bodyPos, e.category, COL_TYPE_X, yOff, nameColor);
			String qStr = e.quality >= 0 ? String.format("%.0f", e.quality) : "-";
			Color qColor = e.quality >= 0 ? getQualityColor(e.quality) : Color.GRAY;
			drawCell(g, bodyPos, qStr, COL_QUALITY_X, yOff, qColor);
			drawCell(g, bodyPos, String.format("%.0f", e.distance / 11.0), COL_DIST_X, yOff, Color.LIGHT_GRAY);
			Color statusColor = e.knocked ? Color.RED : (e.hp < 1.0f ? Color.YELLOW : new Color(100, 220, 100));
			drawCell(g, bodyPos, e.status, COL_STATUS_X, yOff, statusColor);
			if (!e.info.isEmpty()) {
				Color infoColor = e.info.contains("Preg") ? new Color(255, 180, 220) :
						e.info.contains("Lact") ? new Color(180, 220, 255) :
						new Color(200, 200, 200);
				drawCell(g, bodyPos, e.info, COL_INFO_X, yOff, infoColor);
			}
		}

		// Scrollbar
		if (animals.size() > VISIBLE_ROWS) {
			int thumbH = Math.max(UI.scale(10), TABLE_H * VISIBLE_ROWS / animals.size());
			int maxScroll = animals.size() - VISIBLE_ROWS;
			int thumbY = maxScroll > 0 ? (int) ((double) scrollOffset / maxScroll * (TABLE_H - thumbH)) : 0;
			g.chcolor(120, 120, 120, 180);
			g.frect(bodyPos.add(TABLE_W - UI.scale(5), thumbY), new Coord(UI.scale(4), thumbH));
			g.chcolor();
		}

		// Summary bar
		int summaryY = tablePos.y + ROW_H + TABLE_H + UI.scale(4);
		long livestockCount = animals.stream().filter(e -> "Livestock".equals(e.category)).count();
		long wildCount = animals.stream().filter(e -> "Wild".equals(e.category)).count();
		long predCount = animals.stream().filter(e -> "Predator".equals(e.category)).count();
		String summary = String.format("Total: %d  |  Livestock: %d  |  Wild: %d  |  Predators: %d",
			animals.size(), livestockCount, wildCount, predCount);
		try {
			Tex t = fndr.render(summary, Color.LIGHT_GRAY).tex();
			g.image(t, new Coord(tablePos.x, summaryY));
			t.dispose();
		} catch (Exception ignored) {}
	}

	private void drawHeader(GOut g, Coord tablePos, String text, int xOff, int col) {
		Color c = (sortCol == col) ? new Color(180, 200, 255) : new Color(200, 200, 200);
		String display = text + (sortCol == col ? (sortAsc ? " \u25B2" : " \u25BC") : "");
		try {
			Tex t = fndb.render(display, c).tex();
			g.image(t, tablePos.add(xOff + UI.scale(3), UI.scale(1)));
			t.dispose();
		} catch (Exception ignored) {}
	}

	private void drawCell(GOut g, Coord bodyPos, String text, int xOff, int yOff, Color color) {
		try {
			Tex t = fndr.render(text, color).tex();
			g.image(t, bodyPos.add(xOff + UI.scale(3), yOff + UI.scale(1)));
			t.dispose();
		} catch (Exception ignored) {}
	}

	private static Color getTypeColor(String category) {
		switch (category) {
			case "Livestock": return new Color(100, 220, 100);
			case "Predator": return new Color(255, 100, 100);
			case "Wild": return new Color(200, 200, 120);
			default: return Color.WHITE;
		}
	}

	private static Color getQualityColor(double q) {
		if (q >= 50) return new Color(100, 255, 100);
		if (q >= 30) return new Color(200, 220, 100);
		if (q >= 10) return new Color(220, 180, 80);
		return new Color(200, 130, 130);
	}

	@Override
	public boolean mousewheel(MouseWheelEvent ev) {
		scrollOffset = Math.max(0, Math.min(animals.size() - VISIBLE_ROWS, scrollOffset + ev.a));
		return true;
	}

	@Override
	public boolean mousedown(MouseDownEvent ev) {
		Coord tablePos = UI.scale(new Coord(10, 26));
		// Header click — sort
		if (ev.c.y >= tablePos.y && ev.c.y < tablePos.y + ROW_H) {
			int x = ev.c.x - tablePos.x;
			int clickedCol = -1;
			if (x >= COL_INFO_X) clickedCol = 5;
			else if (x >= COL_STATUS_X) clickedCol = 4;
			else if (x >= COL_DIST_X) clickedCol = 3;
			else if (x >= COL_QUALITY_X) clickedCol = 2;
			else if (x >= COL_TYPE_X) clickedCol = 1;
			else if (x >= COL_NAME_X) clickedCol = 0;

			if (clickedCol >= 0) {
				if (sortCol == clickedCol)
					sortAsc = !sortAsc;
				else {
					sortCol = clickedCol;
					sortAsc = (clickedCol != 2); // quality defaults to descending
				}
				sortAnimals();
				return true;
			}
		}
		// Body click — left=navigate, right=interact
		Coord bodyPos = tablePos.add(0, ROW_H);
		if ((ev.b == 1 || ev.b == 3) && ev.c.y >= bodyPos.y && ev.c.y < bodyPos.y + TABLE_H) {
			int row = scrollOffset + (ev.c.y - bodyPos.y) / ROW_H;
			if (row >= 0 && row < animals.size()) {
				if (ev.b == 1)
					navigateToAnimal(animals.get(row));
				else
					interactWithAnimal(animals.get(row));
				return true;
			}
		}
		return super.mousedown(ev);
	}

	private void navigateToAnimal(AnimalEntry entry) {
		if (gui == null || gui.map == null) return;
		Gob gob = gui.map.glob.oc.getgob(entry.gobId);
		if (gob == null) {
			gui.errorsilent("Animal no longer visible");
			return;
		}
		gui.map.pfLeftClick(gob.rc.floor().add(2, 0), null);
	}

	private void interactWithAnimal(AnimalEntry entry) {
		if (gui == null || gui.map == null) return;
		Gob gob = gui.map.glob.oc.getgob(entry.gobId);
		if (gob == null) {
			gui.errorsilent("Animal no longer visible");
			return;
		}
		// Right-click interaction: pathfind to animal and interact
		gui.map.pfRightClick(gob, 0, 3, 0, null);
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && Objects.equals(msg, "close")) {
			hide();
			Utils.setprefc("wndc-livestockManager", this.c);
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	private static class AnimalEntry {
		final String name;
		final String category;
		final double distance;
		final String status;
		final boolean knocked;
		final float hp;
		final double quality;
		final String info;
		final long gobId;

		AnimalEntry(String name, String category, double distance, String status,
				   boolean knocked, float hp, double quality, String info, long gobId) {
			this.name = name;
			this.category = category;
			this.distance = distance;
			this.status = status;
			this.knocked = knocked;
			this.hp = hp;
			this.quality = quality;
			this.info = info;
			this.gobId = gobId;
		}
	}
}
