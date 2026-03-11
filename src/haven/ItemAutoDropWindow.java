package haven;

import haven.res.ui.pag.toggle.Toggle;

import java.awt.Color;
import java.util.*;

public class ItemAutoDropWindow extends Window {
    private Scrollport scroll;
    private CheckBox enableCheckBox;
    private TextEntry addNameEntry;
    private TextEntry addThresholdEntry;
    private static final int WIN_W = 320;

    public ItemAutoDropWindow() {
        super(UI.scale(WIN_W, 400), "Per-Item Auto-Drop", true);
        buildUI();
        this.c = Utils.getprefc("wndc-itemAutoDropWindow", UI.unscale(new Coord(250, 100)));
    }

    private void buildUI() {
        Widget prev;
        prev = add(enableCheckBox = new CheckBox("Enable Per-Item Auto-Drop") {
            {a = ItemAutoDrop.isEnabled();}
            public void set(boolean val) {
                ItemAutoDrop.setEnabled(val);
                a = val;
                if (ui != null && ui.gui != null) {
                    ui.gui.optionInfoMsg("Per-Item Auto-Drop is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
                }
            }
        }, 0, 6);

        prev = add(new Label("Drop items with quality BELOW threshold."), prev.pos("bl").adds(0, 8));

        // Add new item row
        prev = add(new Label("Item name:"), prev.pos("bl").adds(0, 10));
        addNameEntry = new TextEntry(UI.scale(160), "") {
            public boolean keydown(KeyDownEvent ev) {
                if (ev.awt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    addCurrentItem();
                    return true;
                }
                return super.keydown(ev);
            }
        };
        add(addNameEntry, prev.pos("ur").adds(4, -2));

        prev = add(new Label("Q <"), prev.pos("bl").adds(0, 6));
        addThresholdEntry = new TextEntry(UI.scale(40), "30") {
            protected void changed() {
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        };
        add(addThresholdEntry, prev.pos("ur").adds(4, -2));
        add(new Button(UI.scale(60), "Add") {
            public void click() {
                addCurrentItem();
            }
        }, prev.pos("ur").adds(UI.scale(52), -4));
        add(new Button(UI.scale(100), "Add from Cursor") {
            public void click() {
                addFromCursor();
            }
        }.settip("Pick up an item, then click this to add it."), prev.pos("ur").adds(UI.scale(120), -4));

        // Scroll list of configured items
        prev = add(new Label("Configured items:"), prev.pos("bl").adds(0, 12));
        scroll = add(new Scrollport(UI.scale(new Coord(WIN_W - 10, 230))), prev.pos("bl").adds(0, 4));
        rebuildList();

        pack();
    }

    private void addCurrentItem() {
        String name = addNameEntry.text().trim();
        if (name.isEmpty())
            return;
        int threshold;
        try {
            threshold = Integer.parseInt(addThresholdEntry.text().trim());
        } catch (NumberFormatException e) {
            threshold = 30;
        }
        ItemAutoDrop.addItem(name, threshold);
        addNameEntry.settext("");
        rebuildList();
    }

    private void addFromCursor() {
        if (ui == null || ui.gui == null)
            return;
        GItem cursorItem = null;
        if (ui.gui.vhand != null) {
            cursorItem = ui.gui.vhand.item;
        }
        if (cursorItem == null) {
            ui.gui.ui.error("Pick up an item first, then click 'Add from Cursor'.");
            return;
        }
        try {
            String baseName = cursorItem.resource().basename();
            int threshold;
            try {
                threshold = Integer.parseInt(addThresholdEntry.text().trim());
            } catch (NumberFormatException e) {
                threshold = 30;
            }
            ItemAutoDrop.addItem(baseName, threshold);
            rebuildList();
            ui.gui.ui.msg("Added '" + baseName + "' to per-item auto-drop (Q < " + threshold + ").");
        } catch (Loading l) {
            ui.gui.ui.error("Item is still loading, try again.");
        }
    }

    private void rebuildList() {
        Widget cont = scroll.cont;
        // Remove all children
        for (Widget w = cont.child; w != null; ) {
            Widget next = w.next;
            w.reqdestroy();
            w = next;
        }
        int y = 0;
        Map<String, Integer> items = ItemAutoDrop.getItems();
        List<String> sortedKeys = new ArrayList<>(items.keySet());
        Collections.sort(sortedKeys);
        for (String baseName : sortedKeys) {
            int threshold = items.get(baseName);
            final String itemName = baseName;
            Label nameLabel = new Label(baseName);
            nameLabel.setcolor(new Color(200, 220, 255));
            cont.add(nameLabel, new Coord(0, y));

            cont.add(new Label("Q <"), new Coord(UI.scale(160), y));
            TextEntry te = new TextEntry(UI.scale(40), String.valueOf(threshold)) {
                protected void changed() {
                    this.settext(this.text().replaceAll("[^\\d]", ""));
                    try {
                        int val = Integer.parseInt(this.text());
                        ItemAutoDrop.updateThreshold(itemName, val);
                    } catch (NumberFormatException ignored) {}
                    super.changed();
                }
            };
            cont.add(te, new Coord(UI.scale(180), y - 2));

            cont.add(new Button(UI.scale(40), "X") {
                public void click() {
                    ItemAutoDrop.removeItem(itemName);
                    rebuildList();
                }
            }, new Coord(UI.scale(226), y - 4));
            y += UI.scale(22);
        }
        if (sortedKeys.isEmpty()) {
            Label emptyLabel = new Label("No items configured. Add items above.");
            emptyLabel.setcolor(Color.GRAY);
            cont.add(emptyLabel, new Coord(0, 0));
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            hide();
            Utils.setprefc("wndc-itemAutoDropWindow", this.c);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean show(boolean show) {
        if (show)
            rebuildList();
        Utils.setprefc("wndc-itemAutoDropWindow", this.c);
        return super.show(show);
    }
}
