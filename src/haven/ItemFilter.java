package haven;

import haven.res.ui.tt.q.qbuff.QBuff;
import haven.res.ui.tt.armor.Armor;
import haven.res.ui.tt.wear.Wear;
import haven.resutil.Curiosity;
import haven.resutil.FoodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item filter supporting extended query syntax:
 * - "name"            — fuzzy name match
 * - "q>10"            — quality greater than 10
 * - "q<50"            — quality less than 50
 * - "q>=10"           — quality >= 10
 * - "q<=50"           — quality <= 50
 * - "q=10"            — exact quality (rounded)
 * - "q!=10"           — quality not equal to 10
 * - "q:10-50"         — quality range (inclusive)
 * - "fep>10"          — total FEP greater than 10
 * - "fep:str>1"       — specific FEP type (str/agi/int/con/per/cha/dex/wil/psy) above value
 * - "armor>10"        — total armor (hard+soft) greater than 10
 * - "armor:hard>5"    — hard armor greater than 5
 * - "armor:soft>5"    — soft armor greater than 5
 * - "lp>100"          — curiosity LP greater than 100
 * - "lph>50"          — LP per hour greater than 50
 * - "has:water"       — container contains specified substance
 * - "wear>50"         — durability remaining % greater than 50
 * - Combine with space: "turnip q>10 fep>5" — all conditions must match (AND)
 */
public class ItemFilter {
	private static final Pattern QUALITY_CMP = Pattern.compile("q\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern QUALITY_RANGE = Pattern.compile("q\\s*:\\s*([\\d.]+)\\s*-\\s*([\\d.]+)");
	private static final Pattern FEP_TOTAL = Pattern.compile("fep\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern FEP_TYPE = Pattern.compile("fep:(\\w+)\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern ARMOR_TOTAL = Pattern.compile("armor\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern ARMOR_TYPE = Pattern.compile("armor:(hard|soft)\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern LP_FILTER = Pattern.compile("lp\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern LPH_FILTER = Pattern.compile("lph\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern HAS_FILTER = Pattern.compile("has:(\\w+)");
	private static final Pattern WEAR_FILTER = Pattern.compile("wear\\s*([><=!]+)\\s*([\\d.]+)");

	// All patterns for stripping from remaining text
	private static final Pattern[] ALL_PATTERNS = {
		QUALITY_RANGE, QUALITY_CMP, FEP_TYPE, FEP_TOTAL,
		ARMOR_TYPE, ARMOR_TOTAL, LP_FILTER, LPH_FILTER, HAS_FILTER, WEAR_FILTER
	};

	private final List<Predicate> predicates;
	private final String raw;

	@FunctionalInterface
	private interface Predicate {
		boolean test(GItem item, String itemName);
	}

	private ItemFilter(String query, List<Predicate> predicates) {
		this.raw = query;
		this.predicates = predicates;
	}

	public static ItemFilter parse(String query) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}

		List<Predicate> predicates = new ArrayList<>();
		String remaining = query.trim();

		// Quality range: q:10-50
		Matcher m = QUALITY_RANGE.matcher(remaining);
		while (m.find()) {
			double lo = Double.parseDouble(m.group(1));
			double hi = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> {
				double q = getQuality(item);
				return q >= lo && q <= hi;
			});
		}
		remaining = QUALITY_RANGE.matcher(remaining).replaceAll("").trim();

		// Quality comparison: q>10, q>=10, q<50, q<=50, q=10, q!=10
		m = QUALITY_CMP.matcher(remaining);
		while (m.find()) {
			String op = m.group(1);
			double val = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> compare(getQuality(item), op, val));
		}
		remaining = QUALITY_CMP.matcher(remaining).replaceAll("").trim();

		// FEP type: fep:str>1 (must parse before fep total)
		m = FEP_TYPE.matcher(remaining);
		while (m.find()) {
			String type = m.group(1).toLowerCase();
			String op = m.group(2);
			double val = Double.parseDouble(m.group(3));
			predicates.add((item, name) -> {
				double fep = getFepByType(item, type);
				return compare(fep, op, val);
			});
		}
		remaining = FEP_TYPE.matcher(remaining).replaceAll("").trim();

		// FEP total: fep>10
		m = FEP_TOTAL.matcher(remaining);
		while (m.find()) {
			String op = m.group(1);
			double val = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> compare(getTotalFep(item), op, val));
		}
		remaining = FEP_TOTAL.matcher(remaining).replaceAll("").trim();

		// Armor type: armor:hard>5, armor:soft>5
		m = ARMOR_TYPE.matcher(remaining);
		while (m.find()) {
			String type = m.group(1);
			String op = m.group(2);
			double val = Double.parseDouble(m.group(3));
			predicates.add((item, name) -> {
				double armor = getArmorByType(item, type);
				return compare(armor, op, val);
			});
		}
		remaining = ARMOR_TYPE.matcher(remaining).replaceAll("").trim();

		// Armor total: armor>10
		m = ARMOR_TOTAL.matcher(remaining);
		while (m.find()) {
			String op = m.group(1);
			double val = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> compare(getTotalArmor(item), op, val));
		}
		remaining = ARMOR_TOTAL.matcher(remaining).replaceAll("").trim();

		// LP: lp>100
		m = LP_FILTER.matcher(remaining);
		while (m.find()) {
			String op = m.group(1);
			double val = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> compare(getLP(item), op, val));
		}
		remaining = LP_FILTER.matcher(remaining).replaceAll("").trim();

		// LPH: lph>50
		m = LPH_FILTER.matcher(remaining);
		while (m.find()) {
			String op = m.group(1);
			double val = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> compare(getLPH(item), op, val));
		}
		remaining = LPH_FILTER.matcher(remaining).replaceAll("").trim();

		// Container contents: has:water
		m = HAS_FILTER.matcher(remaining);
		while (m.find()) {
			String substance = m.group(1).toLowerCase();
			predicates.add((item, name) -> hasContent(item, substance));
		}
		remaining = HAS_FILTER.matcher(remaining).replaceAll("").trim();

		// Wear/durability: wear>50
		m = WEAR_FILTER.matcher(remaining);
		while (m.find()) {
			String op = m.group(1);
			double val = Double.parseDouble(m.group(2));
			predicates.add((item, name) -> compare(getWearPercent(item), op, val));
		}
		remaining = WEAR_FILTER.matcher(remaining).replaceAll("").trim();

		// Remaining text is a name filter (fuzzy match)
		if (!remaining.isEmpty()) {
			String nameFilter = remaining.toLowerCase();
			predicates.add((item, name) -> Fuzzy.fuzzyContains(name.toLowerCase(), nameFilter));
		}

		if (predicates.isEmpty()) {
			return null;
		}

		return new ItemFilter(query, predicates);
	}

	public boolean matches(GItem item) {
		if (predicates.isEmpty()) return true;
		String name;
		try {
			name = item.getname().toLowerCase();
		} catch (Exception e) {
			name = item.resname().toLowerCase();
		}
		for (Predicate p : predicates) {
			if (!p.test(item, name)) return false;
		}
		return true;
	}

	public boolean matches(WItem witem) {
		return matches(witem.item);
	}

	public boolean hasNameFilter() {
		String remaining = raw.trim();
		for (Pattern p : ALL_PATTERNS)
			remaining = p.matcher(remaining).replaceAll("").trim();
		return !remaining.isEmpty();
	}

	public String getNamePart() {
		String remaining = raw.trim();
		for (Pattern p : ALL_PATTERNS)
			remaining = p.matcher(remaining).replaceAll("").trim();
		return remaining;
	}

	private static boolean compare(double actual, String op, double val) {
		switch (op) {
			case ">": return actual > val;
			case ">=": return actual >= val;
			case "<": return actual < val;
			case "<=": return actual <= val;
			case "=": case "==": return Math.round(actual) == Math.round(val);
			case "!=": return Math.round(actual) != Math.round(val);
			default: return false;
		}
	}

	private static double getQuality(GItem item) {
		QBuff q = item.getQBuff();
		return q != null ? q.q : 0;
	}

	private static double getTotalFep(GItem item) {
		try {
			FoodInfo food = ItemInfo.find(FoodInfo.class, item.info());
			return food != null ? food.sev : 0;
		} catch (Loading e) {
			return 0;
		}
	}

	private static double getFepByType(GItem item, String type) {
		try {
			FoodInfo food = ItemInfo.find(FoodInfo.class, item.info());
			if (food == null || food.evs == null) return 0;
			for (FoodInfo.Event ev : food.evs) {
				if (ev.ev != null && ev.ev.nm != null && ev.ev.nm.toLowerCase().startsWith(type)) {
					return ev.a;
				}
			}
			return 0;
		} catch (Loading e) {
			return 0;
		}
	}

	private static double getTotalArmor(GItem item) {
		try {
			Armor armor = ItemInfo.find(Armor.class, item.info());
			return armor != null ? armor.hard + armor.soft : 0;
		} catch (Loading e) {
			return 0;
		}
	}

	private static double getArmorByType(GItem item, String type) {
		try {
			Armor armor = ItemInfo.find(Armor.class, item.info());
			if (armor == null) return 0;
			return "hard".equals(type) ? armor.hard : armor.soft;
		} catch (Loading e) {
			return 0;
		}
	}

	private static double getLP(GItem item) {
		try {
			Curiosity cur = ItemInfo.find(Curiosity.class, item.info());
			return cur != null ? cur.exp : 0;
		} catch (Loading e) {
			return 0;
		}
	}

	private static double getLPH(GItem item) {
		try {
			Curiosity cur = ItemInfo.find(Curiosity.class, item.info());
			return cur != null ? cur.lph : 0;
		} catch (Loading e) {
			return 0;
		}
	}

	private static boolean hasContent(GItem item, String substance) {
		try {
			ItemInfo.Contents contents = ItemInfo.find(ItemInfo.Contents.class, item.info());
			if (contents == null || contents.content == null) return false;
			return contents.content.is(substance) || (contents.content.name != null && contents.content.name.toLowerCase().contains(substance));
		} catch (Loading e) {
			return false;
		}
	}

	private static double getWearPercent(GItem item) {
		try {
			Wear wear = ItemInfo.find(Wear.class, item.info());
			return wear != null ? wear.percentage : 100;
		} catch (Loading e) {
			return 100;
		}
	}
}
