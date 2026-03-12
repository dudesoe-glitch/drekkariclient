/* Preprocessed source code */
package haven.res.ui.tt.q.quality;

/* $use: ui/tt/q/qbuff */
import haven.*;
import haven.res.ui.tt.q.qbuff.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;

import haven.MenuGrid.Pagina;

/* >tt: Quality */
@haven.FromResource(name = "ui/tt/q/quality", version = 28)
public class Quality extends QBuff implements GItem.OverlayInfo<Tex> {
    public static boolean show = Utils.getprefb("qtoggle", false);
    public static final BufferedImage qualityWorkaround = Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).scaled();

    /** Pre-computed effectiveness multiplier: sqrt(q/10). Used for combat/crafting calculations. */
    public final double multiplier;

    public Quality(Owner owner, double q) {
    super(owner, qualityWorkaround, "Quality", q); // ND: workaround suggested by loftar
    this.multiplier = Math.sqrt(q / 10.0);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Quality(owner, ((Number)args[1]).doubleValue()));
    }

    /**
     * Resolve the effective quality value to display, taking into account
     * the user's chosen aggregation mode (Default / Mean / Average / Min / Max).
     * Falls back to raw quality when aggregation is not applicable.
     */
    private double resolveDisplayQuality(double rawQ) {
        int mode = OptWnd.qualityAggMode;
        if (mode == 0) return rawQ;
        if (owner instanceof GItem) {
            QualityList ql = ((GItem) owner).getQualityList();
            if (ql != null && !ql.isEmpty()) {
                QualityList.SingleType type = switch (mode) {
                    case 1 -> QualityList.SingleType.Mean;
                    case 2 -> QualityList.SingleType.Average;
                    case 3 -> QualityList.SingleType.Min;
                    case 4 -> QualityList.SingleType.Max;
                    default -> null;
                };
                if (type != null) return ql.get(type);
            }
        }
        return rawQ;
    }

    private TexI renderQualityTex(double displayQ, Color color) {
        return new TexI(PUtils.strokeImg(OptWnd.roundedQualityCheckBox.a
                ? GItem.NumberInfo.numrenderStroked((int) Math.round(displayQ), color, true)
                : GItem.NumberInfo.numrenderStrokedDecimal(displayQ, color, true)));
    }

    public Tex overlay() {
        boolean irrelevantQuality = false; // ND: Use this to show the quality of some containers as "Empty", rather than their actual quality.
        Color qualityColor;
        if(OptWnd.customQualityColorsCheckBox.a){
            qualityColor = findHighestTextEntryValueLessThanQ(q);
        } else {
            qualityColor = Color.WHITE;
        }
        try {
            for (ItemInfo info : owner.info()) {
                if (info instanceof Name) {
                    if (Arrays.stream(EMPTY_INDICATOR).anyMatch(((Name) info).str.text::contains)) {
                        irrelevantQuality = true;
                    }
                }
                if (info instanceof Contents) {
                    for (ItemInfo info2 : ((Contents) info).sub) {
                        if (info2 instanceof QBuff) {
                            double contentQ = resolveDisplayQuality(((QBuff) info2).q);
                            if ((((Contents) info).content != null) && (((Contents) info).content.name != null)) {
                                String liquidName = ((Contents) info).content.name;
                                if (liquidColorsMap.keySet().stream().anyMatch(liquidName::matches)){
                                    return renderQualityTex(contentQ, liquidColorsMap.get(liquidName));
                                }
                            }
                            return renderQualityTex(contentQ, Color.WHITE);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!irrelevantQuality) {
            double displayQ = resolveDisplayQuality(q);
            if(OptWnd.customQualityColorsCheckBox.a){
                qualityColor = findHighestTextEntryValueLessThanQ(displayQ);
            }
            return renderQualityTex(displayQ, qualityColor);
        } else
            return (new TexI(PUtils.strokeImg(GItem.NumberInfo.textrenderStroked("Empty", new Color(106, 106, 106, 255), true))));
    }

    public void drawoverlay(GOut g, Tex ol) {
        if(OptWnd.showQualityDisplayCheckBox.a)
            g.aimage(ol, new Coord(g.sz().x, 0), 0.95, 0.2);
    }

    public static final LinkedHashMap<String, Color> liquidColorsMap = new LinkedHashMap<String, Color>(){{
        put("Water", new Color(33, 149, 226, 255));
        put("Tea", new Color(83, 161, 0, 255));
    }};

    private static final String[] EMPTY_INDICATOR = { // ND: Only show the quality as "Empty" for these specific containers
            "Birchbark Kuksa", "Bucket", "Waterskin", "Waterflask", "Glass Jug",
    };

    public Color findHighestTextEntryValueLessThanQ(double q) {
        TextEntry[] textEntries = {OptWnd.q7ColorTextEntry, OptWnd.q6ColorTextEntry, OptWnd.q5ColorTextEntry, OptWnd.q4ColorTextEntry, OptWnd.q3ColorTextEntry, OptWnd.q2ColorTextEntry, OptWnd.q1ColorTextEntry};
        int highestValue = Integer.MIN_VALUE;
        int indexOfHighest = -1;

        for (int i = 0; i < textEntries.length; i++) {
            try {
                int value = Integer.parseInt(textEntries[i].text());
                if (value <= q && value > highestValue) {
                    highestValue = value;
                    indexOfHighest = i;
                }
            } catch (NumberFormatException ignored) {}
        }

        return switch (indexOfHighest) {
            case 0 -> OptWnd.q7ColorOptionWidget.currentColor;
            case 1 -> OptWnd.q6ColorOptionWidget.currentColor;
            case 2 -> OptWnd.q5ColorOptionWidget.currentColor;
            case 3 -> OptWnd.q4ColorOptionWidget.currentColor;
            case 4 -> OptWnd.q3ColorOptionWidget.currentColor;
            case 5 -> OptWnd.q2ColorOptionWidget.currentColor;
            case 6 -> OptWnd.q1ColorOptionWidget.currentColor;
            default -> new Color(255, 255, 255, 255);
        };
    }
}
