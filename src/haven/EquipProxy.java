package haven;

import static haven.Equipory.*;
import static haven.Inventory.invsq;
import static haven.Inventory.sqsz;

public class EquipProxy extends Widget implements DTarget {
    private static final int COLS = 2;
    private static final int PAD = UI.scale(1);
    private final int[] slotIndices;
    private final Coord[] slotCoords;
    private UI.Grab dragging;
    private Coord dc;

    public EquipProxy(int... slots) {
        super(Coord.z);
        this.slotIndices = slots;
        this.slotCoords = new Coord[slots.length];
        layoutSlots();
    }

    private void layoutSlots() {
        int cols = Math.min(slotIndices.length, COLS);
        int rows = (slotIndices.length + cols - 1) / cols;
        int sw = sqsz.x;
        int sh = sqsz.y;
        for (int i = 0; i < slotIndices.length; i++) {
            int col = i % cols;
            int row = i / cols;
            slotCoords[i] = new Coord(col * (sw + PAD), row * (sh + PAD));
        }
        resize(new Coord(cols * (sw + PAD) - PAD, rows * (sh + PAD) - PAD));
    }

    @Override
    public void draw(GOut g) {
        if (!GameUI.showUI) return;
        Equipory e = getEquipory();
        if (e == null) return;

        for (int i = 0; i < slotIndices.length; i++) {
            int si = slotIndices[i];
            Coord c = slotCoords[i];
            g.image(invsq, c);
            if (si < ebgs.length && ebgs[si] != null)
                g.image(ebgs[si], c);
            if (si < e.slots.length && e.slots[si] != null)
                e.slots[si].draw(g.reclipl(c, invsq.sz()));
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        Equipory e = getEquipory();
        if (e != null) {
            int si = slotAt(c);
            if (si >= 0 && si < e.slots.length && e.slots[si] != null)
                return e.slots[si].tooltip(c, (prev == this) ? e.slots[si] : prev);
            // Show slot name tooltip when empty
            int idx = slotIndexAt(c);
            if (idx >= 0 && idx < etts.length && etts[idx] != null)
                return etts[idx].tex();
        }
        return super.tooltip(c, prev);
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (ev.b == 2) {
            if (dragging != null) {
                dragging.remove();
                dragging = null;
            }
            dragging = ui.grabmouse(this);
            dc = ev.c;
            return true;
        }
        if (ev.b == 1 || ev.b == 3) {
            Equipory e = getEquipory();
            if (e != null) {
                int si = slotAt(ev.c);
                if (si >= 0 && si < e.slots.length && e.slots[si] != null) {
                    Coord lc = ev.c.sub(slotCoords[localIdx(si)]);
                    return e.slots[si].mousedown(new MouseDownEvent(lc, ev.b));
                }
            }
        }
        return super.mousedown(ev);
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
        if (dragging != null) {
            this.c = this.c.add(ev.c.x, ev.c.y).sub(dc);
            return;
        }
        super.mousemove(ev);
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        if (dragging != null) {
            dragging.remove();
            dragging = null;
            clampToScreen();
            Utils.setprefc("wndc-equipProxy", this.c);
            return true;
        }
        return super.mouseup(ev);
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        Equipory e = getEquipory();
        if (e != null) {
            int si = slotAt(cc);
            if (si >= 0) {
                e.wdgmsg("drop", si);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = getEquipory();
        if (e != null) {
            int si = slotAt(cc);
            if (si >= 0 && si < e.slots.length && e.slots[si] != null)
                return e.slots[si].iteminteract(cc, ul);
        }
        return false;
    }

    private int slotAt(Coord c) {
        for (int i = 0; i < slotIndices.length; i++) {
            if (c.isect(slotCoords[i], invsq.sz()))
                return slotIndices[i];
        }
        return -1;
    }

    private int slotIndexAt(Coord c) {
        return slotAt(c);
    }

    private int localIdx(int equipSlot) {
        for (int i = 0; i < slotIndices.length; i++) {
            if (slotIndices[i] == equipSlot)
                return i;
        }
        return 0;
    }

    private Equipory getEquipory() {
        if (ui == null || ui.gui == null) return null;
        return ui.gui.getequipory();
    }

    private void clampToScreen() {
        if (ui == null || ui.gui == null) return;
        if (this.c.x < 0) this.c.x = 0;
        if (this.c.y < 0) this.c.y = 0;
        if (this.c.x > ui.gui.sz.x - this.sz.x)
            this.c.x = ui.gui.sz.x - this.sz.x;
        if (this.c.y > ui.gui.sz.y - this.sz.y)
            this.c.y = ui.gui.sz.y - this.sz.y;
    }
}
