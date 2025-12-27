package haven;

import haven.render.BaseColor;
import haven.render.Location;
import haven.render.Pipe;
import haven.render.RenderTree;

public class GobHidden extends GAttrib implements Gob.SetupMod {
    public boolean hidden = false;
    public static final Pipe.Op hiddenOp = Pipe.Op.compose(Location.scale(0.001f, 0.001f, 0.001f), Location.xlate(new Coord3f(0,0,200f)));

    public GobHidden(Gob gob) {
        super(gob);
    }

    public void update(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public Pipe.Op gobstate() {
        return hidden ? hiddenOp : null;
    }
}
