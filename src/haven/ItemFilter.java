package haven;

import haven.res.ui.tt.q.qbuff.QBuff;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item filter supporting extended query syntax:
 * - "name"      — fuzzy name match
 * - "q>10"      — quality greater than 10
 * - "q<50"      — quality less than 50
 * - "q>=10"     — quality >= 10
 * - "q<=50"     — quality <= 50
 * - "q=10"      — exact quality (rounded)
 * - "q:10-50"   — quality range (inclusive)
 * - Combine with space: "turnip q>10" — name AND quality filter
 */
public class ItemFilter {
	private static final Pattern QUALITY_CMP = Pattern.compile("q\\s*([><=!]+)\\s*([\\d.]+)");
	private static final Pattern QUALITY_RANGE = Pattern.compile("q\\s*:\\s*([\\d.]+)\\s*-\\s*([\\d.]+)");

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

		// Extract quality range patterns: q:10-50
		Matcher rangeMatcher = QUALITY_RANGE.matcher(remaining);
		while (rangeMatcher.find()) {
			double lo = Double.parseDouble(rangeMatcher.group(1));
			double hi = Double.parseDouble(rangeMatcher.group(2));
			predicates.add((item, name) -> {
				double q = getQuality(item);
				return q >= lo && q <= hi;
			});
		}
		remaining = QUALITY_RANGE.matcher(remaining).replaceAll("").trim();

		// Extract quality comparison patterns: q>10, q>=10, q<50, q<=50, q=10
		Matcher cmpMatcher = QUALITY_CMP.matcher(remaining);
		while (cmpMatcher.find()) {
			String op = cmpMatcher.group(1);
			double val = Double.parseDouble(cmpMatcher.group(2));
			predicates.add((item, name) -> {
				double q = getQuality(item);
				switch (op) {
					case ">": return q > val;
					case ">=": return q >= val;
					case "<": return q < val;
					case "<=": return q <= val;
					case "=": case "==": return Math.round(q) == Math.round(val);
					case "!=": return Math.round(q) != Math.round(val);
					default: return false;
				}
			});
		}
		remaining = QUALITY_CMP.matcher(remaining).replaceAll("").trim();

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
		// Check if there's a text component beyond just quality filters
		String remaining = raw.trim();
		remaining = QUALITY_RANGE.matcher(remaining).replaceAll("").trim();
		remaining = QUALITY_CMP.matcher(remaining).replaceAll("").trim();
		return !remaining.isEmpty();
	}

	public String getNamePart() {
		String remaining = raw.trim();
		remaining = QUALITY_RANGE.matcher(remaining).replaceAll("").trim();
		remaining = QUALITY_CMP.matcher(remaining).replaceAll("").trim();
		return remaining;
	}

	private static double getQuality(GItem item) {
		QBuff q = item.getQBuff();
		return q != null ? q.q : 0;
	}
}
