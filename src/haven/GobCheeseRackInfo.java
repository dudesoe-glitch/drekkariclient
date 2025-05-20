package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class GobCheeseRackInfo extends GobInfo {

	private static final Color TEXT_COL = new Color(255, 255, 255, 255);
	private static final Color BG = new Color(0, 0, 0, 84);
	public static final int MARGIN = UI.scale(3);
	public static final int PAD = 0;
	private Tex infoTex;
	private static final Map<Pair<Color, String>, Text.Line> TEXT_CACHE = new HashMap<>();


    protected GobCheeseRackInfo(Gob owner) {
	super(owner);
    }

    @Override
	protected boolean enabled() {
		return OptWnd.showCheeseRacksTierTextCheckBox.a && !gob.isHidden;
	}

	@Override
	protected Tex render() {
		if(gob == null || gob.getres() == null) {return null;}
		BufferedImage content = content();
		if(content == null) {
			infoTex = null;
			return null;
		}
		if (infoTex != null)
			return infoTex;
		return infoTex = new TexI(ItemInfo.catimgsh(3, 0, BG, content));
	}

	@Override
    public void dispose() {
	super.dispose();
    }

	private BufferedImage content() {
		for (Gob.Overlay ol : gob.ols) {
			if (!ol.getSprResName().startsWith("gfx/fx/eq")) {continue;}
			Sprite spr = Reflect.getFieldValue(ol.spr, "espr", Sprite.class);
			if (spr == null) {continue;}
			String name = spr.res.name;
			String text = null;
			if (name.startsWith("gfx/terobjs/items/cheesetray-")) {
				text = "T" + name.substring(name.lastIndexOf("-") + 2);
			}
			if(text != null) {
				spr.setTex2d(combine(PUtils.strokeImg(text(text, TEXT_COL).img)));
			} else {
				spr.setTex2d(null);
			}
		}
		return null;
	}

	private static Text.Line text(String text, Color col) {
		Pair<Color, String> key = new Pair<>(col, text);
		if(TEXT_CACHE.containsKey(key)) {
			return TEXT_CACHE.get(key);
		} else {
			Text.Line line = Text.std.renderstroked(text, col, Color.black);
			TEXT_CACHE.put(key, line);
			return line;
		}
	}

	private static Tex combine(BufferedImage... parts) {
		return new TexI(ItemInfo.catimgsh(MARGIN, PAD, BG, parts));
	}
}