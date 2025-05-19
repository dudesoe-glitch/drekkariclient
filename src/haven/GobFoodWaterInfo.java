package haven;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GobFoodWaterInfo extends GobInfo {

	public static BufferedImage lowFoodImage = PUtils.convolvedown(Resource.local().loadwait("customclient/lowFood").layer(Resource.imgc).img, UI.scale(34, 34), CharWnd.iconfilter);
	public static BufferedImage lowWaterImage = PUtils.convolvedown(Resource.local().loadwait("customclient/lowWater").layer(Resource.imgc).img, UI.scale(34, 34), CharWnd.iconfilter);

    protected GobFoodWaterInfo(Gob owner) {
	super(owner);
    }

    @Override
	protected boolean enabled() {
		return OptWnd.showLowFoodWaterIconsCheckBox.a && !gob.isHidden;
	}

	@Override
	protected Tex render() {
	up(6);
	if(gob == null || gob.getres() == null) { return null;}
		if (icons() != null)
			return new TexI(icons());
		return null;
	}

	@Override
    public void dispose() {
	super.dispose();
    }

	private BufferedImage icons() {
		BufferedImage[] parts = null;
		Drawable dr = gob.getattr(Drawable.class);
		ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
		String resName = gob.getres().name;
		if(d != null) {
			int rbuf = d.sdt.checkrbuf(0);
            if(resName.equals("gfx/terobjs/chickencoop")) {
                if (rbuf == 0) {
                    parts = new BufferedImage[]{lowFoodImage, lowWaterImage};
                } else if (rbuf == 1) {
                    parts = new BufferedImage[]{lowFoodImage};
                } else if (rbuf == 2) {
                    parts = new BufferedImage[]{lowWaterImage};
                } else {
                    return null;
                }
            } else if (resName.equals("gfx/terobjs/rabbithutch")) {
				// ND: For rabbit hutches, the water only has 2 indicators (>50%, 0%) and the food has 3 indicators (>66%, >33%, 0%)
				// They also have open/closed states, so that's twice as many rbufs
				if (rbuf == 65 || rbuf == 66 || rbuf == 73 || rbuf == 74) {
					parts = new BufferedImage[]{lowFoodImage, lowWaterImage};
				} else if (rbuf == 69 || rbuf == 70 || rbuf == 77 || rbuf == 78) {
					parts = new BufferedImage[]{lowFoodImage};
				} else if (rbuf == 121 || rbuf == 122 || rbuf == 89 || rbuf == 90) {
					parts = new BufferedImage[]{lowWaterImage};
				} else {
					return null;
				}
			}
		}
		if(parts == null) {return null;}
		for (BufferedImage part : parts) {
			if(part == null) {continue;}
			return ItemInfo.catimgs(1, parts);
		}
		return null;
	}

}