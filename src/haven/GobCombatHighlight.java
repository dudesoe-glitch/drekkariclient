package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobCombatHighlight extends GAttrib implements Gob.SetupMod {
    public final Color COMBAT_FOE_COLOR = new Color(160, 0, 0, 164);

    public GobCombatHighlight(Gob g) {
	super(g);
    }
    
    public void start() {
    }
    
    public Pipe.Op gobstate() {
        return new MixColor(COMBAT_FOE_COLOR.getRed(), COMBAT_FOE_COLOR.getGreen(), COMBAT_FOE_COLOR.getBlue(), COMBAT_FOE_COLOR.getAlpha());
    }
}