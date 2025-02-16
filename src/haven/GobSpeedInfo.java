package haven;

import haven.render.Homo3D;
import haven.render.Pipe;

import java.awt.*;
import java.awt.image.BufferedImage;


public class GobSpeedInfo extends GobInfo {
    private double savedSpeed = 0;

    public GobSpeedInfo(Gob owner) {
        super(owner);
        up(-2); // ND: Default was 12.0 // ND: For each 3.4 added here, add 1.0 at "b:" in the pair below. It's probably not 100% correct, but it's super close.
        center = new Pair<>(0.5, 0.0); // Default was 0.5, 1.0
    }

    @Override
    protected boolean enabled() {
        return ((OptWnd.showObjectsSpeedCheckBox.a) && !(gob.getattr(Moving.class) instanceof Following) && (gob.gobSpeed > 0));
    }


    @Override
    public void draw(GOut g, Pipe state) {
        if (!GameUI.showUI)
            return;
        synchronized (texLock) {
            if(enabled() && tex != null) {
                Coord3f c3d = Homo3D.obj2view2(pos, state, Area.sized(g.sz()));
                if(c3d == null) {return;}
                Coord sc = c3d.round2();
                if(sc.isect(Coord.z, g.sz())) {
                    g.aimage(tex, sc, center.a, center.b);
                }
            }
        }
    }

    @Override
    protected Tex render() {
        return PUtils.strokeTex(Text.renderstroked(String.format("%.2f u/s", gob.gobSpeed)));
    }

    @Override
    public void ctick(double dt) {
        synchronized (texLock) {
            if (savedSpeed != gob.gobSpeed) {
                savedSpeed = gob.gobSpeed;
                clear();
            }
            if(enabled() && dirty && tex == null) {
                tex = render();
                dirty = false;
            }
        }
    }

}
