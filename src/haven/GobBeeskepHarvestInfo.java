package haven;

import java.awt.image.BufferedImage;

public class GobBeeskepHarvestInfo extends GobInfo {

	public static BufferedImage waxImage = PUtils.convolvedown(Resource.local().loadwait("customclient/wax").layer(Resource.imgc).img, UI.scale(26, 26), CharWnd.iconfilter);
	public static BufferedImage honeyImage = PUtils.convolvedown(Resource.local().loadwait("customclient/honey").layer(Resource.imgc).img, UI.scale(26, 26), CharWnd.iconfilter);

    protected GobBeeskepHarvestInfo(Gob owner) {
	super(owner);
    }

    @Override
	protected boolean enabled() {
		return OptWnd.showBeeSkepsHarvestIconsCheckBox.a && !gob.isHidden;
	}

	@Override
	protected Tex render() {
	up(2);
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
            if(resName.equals("gfx/terobjs/beehive")) {
				if (rbuf == 7 || rbuf == 15) {
					parts = new BufferedImage[]{waxImage, honeyImage};
				} else if (rbuf == 6 || rbuf == 14) {
					parts = new BufferedImage[]{waxImage};
				} else if (rbuf == 3 || rbuf == 11) {
					parts = new BufferedImage[]{honeyImage};
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