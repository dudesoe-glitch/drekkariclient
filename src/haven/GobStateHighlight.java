package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobStateHighlight extends GAttrib implements Gob.SetupMod {
    public Color color;

    public GobStateHighlight(Gob g, Color color) {
	super(g);
    this.color = color;
    }
    
    public Pipe.Op gobstate() {
        return new MixColor(color);
    }
}