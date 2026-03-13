package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays contents and drying status on drying frames (gfx/terobjs/dframe).
 * Shows item count and whether items are still drying or done.
 */
public class GobDFrameInfo extends GobInfo {
	private static final Map<String, Tex> texCache = new HashMap<>();
	private String lastKey = null;

	protected GobDFrameInfo(Gob owner) {
		super(owner);
	}

	@Override
	protected boolean enabled() {
		return OptWnd.showWorkstationProgressInProgressCheckBox.a && !gob.isHidden;
	}

	@Override
	protected Tex render() {
		if (gob == null || gob.getres() == null) return null;

		List<Gob.Overlay> olsSnapshot = new ArrayList<>(gob.ols);
		int total = 0;
		int drying = 0;
		String itemName = null;

		for (Gob.Overlay ol : olsSnapshot) {
			try {
				if (ol.spr == null || ol.spr.res == null) continue;
				String name = ol.spr.res.name;
				if (name == null) continue;
				// Skip collision box overlays
				if (name.contains("collision") || name.contains("hitbox")) continue;

				total++;
				if (name.endsWith("-blood") || name.endsWith("-windweed") || name.endsWith("-fishraw")) {
					drying++;
					// Extract base name (strip suffix and path)
					String base = name;
					if (base.endsWith("-blood")) base = base.substring(0, base.length() - 6);
					else if (base.endsWith("-windweed")) base = base.substring(0, base.length() - 9);
					else if (base.endsWith("-fishraw")) base = base.substring(0, base.length() - 8);
					if (itemName == null) itemName = extractItemName(base);
				} else {
					if (itemName == null) itemName = extractItemName(name);
				}
			} catch (Loading ignored) {}
		}

		if (total == 0) return null;

		String key;
		String label;
		Color color;
		if (drying > 0) {
			label = (itemName != null ? itemName + " " : "") + total + " - Drying";
			color = new Color(255, 200, 100);
			key = "drying_" + total + "_" + drying + "_" + itemName;
		} else {
			label = (itemName != null ? itemName + " " : "") + total + " - Done!";
			color = new Color(100, 255, 100);
			key = "done_" + total + "_" + itemName;
		}

		if (key.equals(lastKey) && texCache.containsKey(key)) {
			return texCache.get(key);
		}
		lastKey = key;

		Text.Line line = Text.std.renderstroked(label, color, Color.BLACK);
		Tex tex = new TexI(ItemInfo.catimgsh(3, 0, null, line.img));
		texCache.put(key, tex);
		return tex;
	}

	/**
	 * Extract a readable item name from an overlay resource path.
	 * e.g., "gfx/terobjs/dframe-meat" -> "Meat"
	 */
	private static String extractItemName(String resPath) {
		if (resPath == null) return null;
		// Get last path component
		int lastSlash = resPath.lastIndexOf('/');
		String basename = (lastSlash >= 0) ? resPath.substring(lastSlash + 1) : resPath;
		// Strip common prefixes
		if (basename.startsWith("dframe-")) basename = basename.substring(7);
		if (basename.isEmpty()) return null;
		// Capitalize first letter
		return Character.toUpperCase(basename.charAt(0)) + basename.substring(1);
	}

	/**
	 * Called when overlays change to force re-render.
	 */
	public void invalidate() {
		clear();
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}
