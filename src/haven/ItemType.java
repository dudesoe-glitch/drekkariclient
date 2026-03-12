package haven;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Centralized item classification enum.
 * Classifies GItems by inspecting item info and resource name patterns,
 * using the same logic as WItem's badge detection AttrCache.
 */
public enum ItemType {
	FOOD("Food", new Color(50, 200, 50, 200)),
	ARMOR("Armor", new Color(80, 130, 255, 200)),
	CURIOSITY("Curiosity", new Color(200, 100, 255, 200)),
	TOOL("Tool", new Color(255, 200, 50, 200)),
	SEED("Seed", new Color(139, 90, 43, 200)),
	CONTAINER("Container", new Color(0, 180, 180, 200)),
	MATERIAL("Material", new Color(200, 120, 30, 200)),
	WEAPON("Weapon", new Color(220, 50, 50, 200)),
	UNKNOWN("Unknown", new Color(160, 160, 160, 200));

	private final String displayLabel;
	private final Color displayColor;

	ItemType(String label, Color color) {
		this.displayLabel = label;
		this.displayColor = color;
	}

	private static final Set<String> TOOL_BASENAMES = new HashSet<>(Arrays.asList(
		"axe", "stoneaxe", "boneaxe", "metalaxe",
		"pickaxe", "shovel", "woodenshovel", "tinkershovel", "metalshovel",
		"saw", "hammer", "sledgehammer", "smithshammer",
		"scythe", "sickle", "trowel",
		"smokren", "fryingpan", "crucible",
		"chisel", "needle", "sewing-needle", "spindle", "loom",
		"fishing-pole", "net", "hookline",
		"tinderbox", "firestarter",
		"rope", "leash", "bucket"
	));

	private static final Set<String> CONTAINER_BASENAMES = new HashSet<>(Arrays.asList(
		"flask", "waterskin", "waterflask", "kuksa", "tankard", "mug", "cup",
		"barrel", "trough", "cauldron", "pot"
	));

	private static final Set<String> WEAPON_BASENAMES = new HashSet<>(Arrays.asList(
		"sword", "b12sword", "bronzesword", "cutblade", "battlesword",
		"bow", "sling", "spear", "javelin",
		"mace", "club", "warhammer"
	));

	/**
	 * Classify a GItem by inspecting its item info and resource name.
	 * Returns the first matching type in priority order:
	 * Food > Armor > Curiosity > Tool > Seed > Container > Material > Weapon > Unknown.
	 */
	public static ItemType classify(GItem item) {
		if (item == null) return UNKNOWN;

		// Check info-based types first (most reliable)
		try {
			List<ItemInfo> info = item.info();
			for (ItemInfo inf : info) {
				if (inf instanceof haven.resutil.FoodInfo) return FOOD;
				if (inf instanceof haven.res.ui.tt.armor.Armor) return ARMOR;
				if (inf instanceof haven.resutil.Curiosity) return CURIOSITY;
			}
		} catch (Loading ignored) {
		} catch (Exception ignored) {
		}

		// Then resource name patterns (fallback)
		try {
			String resname = item.resname();
			if (resname == null || resname.isEmpty()) return UNKNOWN;
			String basename = resname.substring(resname.lastIndexOf('/') + 1);

			// TOOL check (includes /tools/ path and known basenames)
			if (resname.contains("/tools/") || TOOL_BASENAMES.contains(basename)) {
				return TOOL;
			}
			// CONTAINER check
			if (CONTAINER_BASENAMES.contains(basename)) {
				return CONTAINER;
			}
			// SEED check
			if (resname.contains("gfx/invobjs/seed-") || resname.contains("gfx/invobjs/seeds/")) {
				return SEED;
			}
			// MATERIAL check
			if (resname.contains("gfx/invobjs/bar-") || resname.contains("gfx/invobjs/nugget-") ||
				resname.contains("gfx/invobjs/ore-") || resname.contains("gfx/invobjs/fur-") ||
				basename.equals("brick") || basename.equals("board") || basename.equals("stone") ||
				basename.equals("coal") || basename.equals("branch") || basename.equals("log") ||
				basename.equals("leather") || basename.equals("string") || basename.equals("cloth") ||
				basename.equals("yarn") || basename.equals("wool") || basename.equals("hide") ||
				basename.equals("bone")) {
				return MATERIAL;
			}
			// WEAPON check
			if (resname.contains("/weapons/") || WEAPON_BASENAMES.contains(basename)) {
				return WEAPON;
			}
		} catch (Exception ignored) {
		}

		return UNKNOWN;
	}

	/**
	 * Get the display color for this type (for badges, UI elements).
	 */
	public Color color() {
		return displayColor;
	}

	/**
	 * Get display label for this type.
	 */
	public String label() {
		return displayLabel;
	}
}
