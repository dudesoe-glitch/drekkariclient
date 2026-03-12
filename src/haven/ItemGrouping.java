package haven;

/**
 * Grouping strategies for the Extended Inventory window.
 * Determines how items are organized into collapsible groups.
 */
public enum ItemGrouping {
	NONE("None"),
	BY_NAME("By Name"),
	BY_QUALITY("By Quality"),
	BY_Q5("Q5 Brackets"),
	BY_Q10("Q10 Brackets"),
	BY_TYPE("By Type");

	public final String label;

	ItemGrouping(String label) {
		this.label = label;
	}

	public ItemGrouping next() {
		ItemGrouping[] vals = values();
		return vals[(ordinal() + 1) % vals.length];
	}

	/**
	 * Get the group key for an item under this grouping mode.
	 * Returns an empty string for NONE mode.
	 */
	public String groupKey(WItem witem) {
		if (witem == null) return "";
		try {
			switch (this) {
				case BY_NAME:
					return witem.sortName();
				case BY_QUALITY:
					double q = witem.quality();
					return "Q" + (int) Math.floor(q);
				case BY_Q5: {
					double qual = witem.quality();
					int bracket = ((int) Math.floor(qual)) / 5 * 5;
					return "Q" + bracket + "-" + (bracket + 5);
				}
				case BY_Q10: {
					double qual = witem.quality();
					int bracket = ((int) Math.floor(qual)) / 10 * 10;
					return "Q" + bracket + "-" + (bracket + 10);
				}
				case BY_TYPE: {
					ItemType type = ItemType.classify(witem.item);
					return type.label();
				}
				default:
					return "";
			}
		} catch (Exception e) {
			return "?";
		}
	}

	/**
	 * Get a sort key for ordering groups.
	 * Name groups sort alphabetically, quality groups sort numerically.
	 */
	public String groupSortKey(String groupKey) {
		if (this == BY_NAME) {
			return groupKey.toLowerCase();
		}
		if (this == BY_TYPE) {
			// Sort by type ordinal to keep consistent ordering
			for (ItemType t : ItemType.values()) {
				if (t.label().equals(groupKey)) return String.format("%02d", t.ordinal());
			}
			return "99";
		}
		// For quality-based groupings, extract numeric prefix for sorting
		try {
			String numStr = groupKey.replaceAll("[^0-9]", "");
			if (!numStr.isEmpty()) {
				return String.format("%010d", Integer.parseInt(numStr));
			}
		} catch (Exception ignored) {}
		return groupKey;
	}
}
