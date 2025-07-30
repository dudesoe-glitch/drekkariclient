package haven;

import java.awt.*;

public class QuestObjectivesWindow extends Window {
    public QuestObjectivesWindow() {
        super(Coord.z,"Quest Objectives");
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (msg == "close")) {
            ui.gui.chrwdg.quest.cqst.unselect(1);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void cresize(Widget ch) {
        pack();
        if (parent != null)
            presize();
    }

    @Override
    public void cdestroy(Widget w) {
        hide();
        if (ui != null && ui.gui != null)
            ui.gui.qqview = null;
        super.cdestroy(w);
    }


    @Override
    public <T extends Widget> T add(T child) {
        show();
        return super.add(child);
    }


    public void resetDeco() {
        if (OptWnd.transparentQuestsObjectivesWindowCheckBox.a) {
            chdeco(new DefaultDeco() {
                @Override
                protected void drawbg(GOut g) {
                }

                @Override
                protected void drawframe(GOut g) {
                }

                @Override
                public void iresize(Coord isz) {
                    Coord mrgn = lg ? dlmrgn : dsmrgn;
                    Coord asz = isz;
                    Coord csz = asz.add(mrgn.mul(2));
                    wsz = csz.add(tlm).add(brm);
                    resize(wsz);
                    ca = Area.sized(tlm, csz);
                    aa = Area.sized(ca.ul.add(mrgn), asz);
                    cbtn.c = Coord.of(UI.scale(12), +UI.scale(28));
                    cpsz = Coord.of((int) (wsz.x * 0.95), cm.sz().y).sub(cptl);
                }

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if(ev.propagate(this))
                        return(true);
                    if(checkhit(ev.c)) {
                        Window wnd = (Window)parent;
                        wnd.parent.setfocus(wnd);
                        wnd.raise();
                        if(ev.b == 1)
                            wnd.drag(ev.c);
                        return(true);
                    }
                    return(super.mousedown(ev));
                }
            });
        } else {
            chdeco(new DefaultDeco() {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if(ev.propagate(this))
                        return(true);
                    if(checkhit(ev.c)) {
                        Window wnd = (Window)parent;
                        wnd.parent.setfocus(wnd);
                        wnd.raise();
                        if(ev.b == 1)
                            wnd.drag(ev.c);
                        return(true);
                    }
                    return(super.mousedown(ev));
                }
            });
        }
    }

}
