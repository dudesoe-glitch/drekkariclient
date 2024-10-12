package haven;

public class TableInfo extends Widget {

    public static CheckBox preventCutleryFromBreakingCheckBox = new CheckBox("Prevent Cutlery from Breaking"){
        {a = Utils.getprefb("antiCutleryBreakage", true);}
        public void set(boolean val) {
            OptWnd.preventCutleryFromBreakingCheckBox.set(val);
            a = val;
        }
    };

    public TableInfo(int x, int y) {
        this.sz = new Coord(x, y);
        add(preventCutleryFromBreakingCheckBox, 10, 0);
        preventCutleryFromBreakingCheckBox.tooltip = OptWnd.preventCutleryFromBreakingCheckBox.tooltip;
    }

}
