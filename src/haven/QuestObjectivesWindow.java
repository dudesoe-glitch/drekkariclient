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
        if (ui.gui != null)
            ui.gui.qqview = null;
        super.cdestroy(w);
    }


    @Override
    public <T extends Widget> T add(T child) {
        show();
        return super.add(child);
    }
}
