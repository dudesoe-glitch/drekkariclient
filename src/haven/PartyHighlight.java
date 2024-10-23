package haven;


import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PartyHighlight {
    public static Color MEMBER_OL_COLOR = new Color(0, 160, 0, 164);
    public static Color LEADER_OL_COLOR = new Color(0, 74, 208, 164);
    public static Color MYSELF_OL_COLOR = new Color(255, 255, 255, 128);

    private final Party party;
    private final long playerId;
    private final HashMap<Gob, GobPartyHighlight> overlays;

    public PartyHighlight(Party party, long playerId) {
        this.party = party;
        this.playerId = playerId;
        this.overlays = new HashMap<Gob, GobPartyHighlight>();
    }

    public void update() {
        if (party.memb.size() > 1) {
            for (Party.Member m : party.memb.values()) {
                Gob gob = m.getgob();
                if (gob == null)
                    continue;
                if (OptWnd.highlightPartyMembersCheckBox.a && m == party.leader)
                    highlight(gob, LEADER_OL_COLOR);
                else if (OptWnd.highlightPartyMembersCheckBox.a && m.gobid == playerId && m != party.leader)
                    highlight(gob, MYSELF_OL_COLOR);
                else if (OptWnd.highlightPartyMembersCheckBox.a && m != party.leader)
                    highlight(gob, MEMBER_OL_COLOR);
                else
                    unhighlight(gob);
            }
        } else {
            Iterator<Map.Entry<Gob, GobPartyHighlight>> iter = overlays.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Gob, GobPartyHighlight> new_Map = (Map.Entry<Gob, GobPartyHighlight>) iter.next();
                new_Map.getKey().delattr(GobPartyHighlight.class);
                iter.remove();
            }
        }
    }

    private void highlight(Gob gob, Color color) {
        if (overlays.containsKey(gob) && overlays.get(gob).c == color)
            return;
        GobPartyHighlight overlay = new GobPartyHighlight(gob, color);
        gob.setattr(overlay);
        overlays.put(gob, overlay);
    }

    private void unhighlight(Gob gob) {
        GobPartyHighlight overlay = overlays.remove(gob);
        if (overlay != null)
            gob.delattr(GobPartyHighlight.class);
    }
}