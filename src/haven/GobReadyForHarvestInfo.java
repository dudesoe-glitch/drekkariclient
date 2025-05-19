package haven;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GobReadyForHarvestInfo extends GobInfo {

	public static final Map<String, String> SeedsMap = new HashMap<String, String>() {{
		// ND: There's no goddamn consistency. Some work fine with "seed-basename", but others have different names:
		// Pine trees are called "pine", and their seed is "seed-pine" (so this works by default)
		// Quince trees are called "quincetree", and their seed is "quince"
		// Mulberry trees are called "mulberry", and their seed is "mulberry"

		// Trees:
		put("almondtree", "gfx/invobjs/almond");
		put("appletree", "gfx/invobjs/apple");
		put("appletreegreen", "gfx/invobjs/applegreen");
		put("birdcherrytree", "gfx/invobjs/birdcherry");
		put("carobtree", "gfx/invobjs/carobfruit");
		put("cherry", "gfx/invobjs/cherry");
		put("chestnuttree", "gfx/invobjs/chestnut");
		put("corkoak", "gfx/invobjs/cork");
		put("figtree", "gfx/invobjs/fig");
		put("hazel", "gfx/invobjs/hazelnut");
		put("lemontree", "gfx/invobjs/lemon");
		put("medlartree", "gfx/invobjs/medlar");
		put("mulberry", "gfx/invobjs/mulberry");
		put("olivetree", "gfx/invobjs/olive");
		put("orangetree", "gfx/invobjs/orange");
		put("peartree", "gfx/invobjs/pear");
		put("persimmontree", "gfx/invobjs/persimmon");
		put("plumtree", "gfx/invobjs/plum");
		put("quincetree", "gfx/invobjs/quince");
		put("rowan", "gfx/invobjs/rowanberry");
		put("sorbtree", "gfx/invobjs/sorbapple");
		put("stonepine", "gfx/invobjs/stonepinecone");
		put("strawberrytree", "gfx/invobjs/woodstrawberry");
		put("walnuttree", "gfx/invobjs/walnut");
		put("whitebeam", "gfx/invobjs/whitebeamfruit");

		// Bushes:
		put("ghostpipe", "gfx/invobjs/ghostpipes");
		put("mastic", "gfx/invobjs/masticfruit");
		put("poppycaps", "gfx/invobjs/poppycapss"); // ND: Yes, "poppycapss" with 2 "s". This is not a typo.

	}};

	public static final Map<String, String> LeavesMap = new HashMap<String, String>() {{
		// ND: These have to be manually done, cause if the tree/bush doesn't actually produce leaves, the indicator for leaves is always true
		put("conkertree", "gfx/invobjs/leaf-conkertree");
		put("figtree", "gfx/invobjs/leaf-fig");
		put("laurel", "gfx/invobjs/leaf-laurel");
		put("maple", "gfx/invobjs/leaf-maple");
		put("mulberry", "gfx/invobjs/leaf-mulberrytree");

		put("teabush", "gfx/invobjs/tea-fresh");
	}};

    protected GobReadyForHarvestInfo(Gob owner) {
	super(owner);
    }

    @Override
	protected boolean enabled() {
		return OptWnd.showTreesBushesHarvestIconsCheckBox.a && !gob.isHidden;
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
		Message data = getDrawableData(gob);
		Resource res = gob.getres();
		if(data != null && !data.eom()) {
			data.skip(1);
			int growth = data.eom() ? -1 : data.uint8();
			if (growth != -1) {
				if(res.name.contains("gfx/terobjs/trees") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk")) {
					growth = (int) (GobGrowthInfo.TREE_MULT * (growth - GobGrowthInfo.TREE_START));
				} else if(res.name.startsWith("gfx/terobjs/bushes")) {
					growth = (int) (GobGrowthInfo.BUSH_MULT * (growth - GobGrowthInfo.BUSH_START));
				}
			}
			if (growth == -1 || growth >= 100) {
				if (isSpriteKind(gob, "Tree")) {
					int sdt = gob.sdt();
					String resBaseName = gob.getres().basename();
					boolean seed = (sdt & 1) != 1;
					boolean leaf = (sdt & 2) != 2; // ND: If the tree/bush doesn't actually produce seeds, this is always true (smh)
					parts = new BufferedImage[]{
							seed ? getIcon(resBaseName, "seed") : null,
							leaf ? getIcon(resBaseName, "leaf") : null,
					};
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

	private static final Map<String, BufferedImage> iconCache = new HashMap<>();

	private static BufferedImage getIcon(String basename, String type) {
		if(basename == null) {return null;}
		String resourceName = null;
		if (type.equals("seed")) {
			if (SeedsMap.containsKey(basename)) {
				resourceName = SeedsMap.get(basename);
			} else {
				resourceName = "gfx/invobjs/seed-" + basename;
			}
		} else if (type.equals("leaf")) {
			if (LeavesMap.containsKey(basename)) {
				resourceName = LeavesMap.get(basename);
			}
		}
		if(resourceName == null) {return null;}
		if(iconCache.containsKey(resourceName)) {
			return iconCache.get(resourceName);
		}
		BufferedImage img;
		try {
			img = Resource.remote().loadwait(resourceName).layer(Resource.imgc).img;
			img = PUtils.convolvedown(img, UI.scale(26, 26), CharWnd.iconfilter);
		} catch (Exception e) {
			System.out.println("Couldn't find content icon for \"" + resourceName + "\"! Tell Nightdawg to add it!");
			img = null;
		}
		iconCache.put(basename, img);
		return img;
	}

	private static Message getDrawableData(Gob gob) {
		Drawable dr = gob.getattr(Drawable.class);
		ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
		if(d != null)
			return d.sdt.clone();
		else
			return null;
	}

	private static boolean isSpriteKind(Gob gob, String... kind) {
		List<String> kinds = Arrays.asList(kind);
		boolean result = false;
		Class spc;
		Drawable d = gob.getattr(Drawable.class);
		Resource.CodeEntry ce = gob.getres().layer(Resource.CodeEntry.class);
		if(ce != null) {
			spc = ce.get("spr");
			result = spc != null && (kinds.contains(spc.getSimpleName()) || kinds.contains(spc.getSuperclass().getSimpleName()));
		}
		if(!result) {
			if(d instanceof ResDrawable) {
				Sprite spr = ((ResDrawable) d).spr;
				if(spr == null) {throw new Loading();}
				spc = spr.getClass();
				result = kinds.contains(spc.getSimpleName()) || kinds.contains(spc.getSuperclass().getSimpleName());
			}
		}
		return result;
	}

}