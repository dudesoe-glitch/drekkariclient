package haven;

import java.awt.Color;

/**
 * Extracted Quality Display Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.QualityDisplaySettingsPanel.
 */
class OptWndQualityPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		prev = panel.add(OptWnd.showQualityDisplayCheckBox = new CheckBox("Display Quality on Inventory Items"){
			{a = (Utils.getprefb("qtoggle", true));}
			public void set(boolean val) {
				Utils.setprefb("qtoggle", val);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				a = val;
			}
		}, 0, 6);
		prev = panel.add(OptWnd.showItemCategoryBadgesCheckBox = new CheckBox("Show Item Category Badges"){
			{a = Utils.getprefb("showItemCategoryBadges", false);}
			public void changed(boolean val) {
				Utils.setprefb("showItemCategoryBadges", val);
			}
		}, prev.pos("bl").adds(0, 2));

		prev = panel.add(new Label("Quality Display Mode:"), prev.pos("bl").adds(0, 10));
		final Widget p = panel;
		RadioGroup qualityAggGrp = new RadioGroup(panel) {
			public void changed(int btn, String lbl) {
				OptWnd.qualityAggMode = btn;
				Utils.setprefi("qualityAggMode", btn);
				if (p.ui != null && p.ui.gui != null) p.ui.gui.reloadAllItemOverlays();
			}
		};
		prev = qualityAggGrp.add("Default", prev.pos("bl").adds(0, 2));
		prev = qualityAggGrp.add("Mean (RMS)", prev.pos("bl").adds(0, 2));
		prev = qualityAggGrp.add("Average", prev.pos("bl").adds(0, 2));
		prev = qualityAggGrp.add("Min", prev.pos("bl").adds(0, 2));
		prev = qualityAggGrp.add("Max", prev.pos("bl").adds(0, 2));
		qualityAggGrp.check(Utils.getprefi("qualityAggMode", 0));

		prev = panel.add(OptWnd.roundedQualityCheckBox = new CheckBox("Rounded Quality Number"){
			{a = (Utils.getprefb("roundedQuality", true));}
			public void changed(boolean val) {
				Utils.setprefb("roundedQuality", val);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}
		}, prev.pos("bl").adds(0, 2));
		prev = panel.add(OptWnd.customQualityColorsCheckBox = new CheckBox("Enable Custom Quality Colors:"){
			{a = (Utils.getprefb("enableCustomQualityColors", false));}
			public void changed(boolean val) {
				Utils.setprefb("enableCustomQualityColors", val);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}
		}, prev.pos("bl").adds(0, 12));
		prev.tooltip = OptWndTooltips.customQualityColors;

		prev = panel.add(OptWnd.q7ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q7ColorTextEntry", "400")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q7ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10));
		prev = panel.add(OptWnd.q7ColorOptionWidget = new ColorOptionWidget(" Godlike Quality:", "q7ColorSetting", 120, Integer.parseInt(OptWnd.q7ColorSetting[0]), Integer.parseInt(OptWnd.q7ColorSetting[1]), Integer.parseInt(OptWnd.q7ColorSetting[2]), Integer.parseInt(OptWnd.q7ColorSetting[3]), (Color col) -> {
			OptWnd.q7ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q7ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q7ColorSetting_colorSetting", new String[]{"255","0","0","255"});
			OptWnd.q7ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q7ColorOptionWidget.currentColor = new Color(255, 0, 0, 255));
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(OptWnd.q6ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q6ColorTextEntry", "300")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q6ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
		prev = panel.add(OptWnd.q6ColorOptionWidget = new ColorOptionWidget("  Legendary Quality:", "q6ColorSetting", 120, Integer.parseInt(OptWnd.q6ColorSetting[0]), Integer.parseInt(OptWnd.q6ColorSetting[1]), Integer.parseInt(OptWnd.q6ColorSetting[2]), Integer.parseInt(OptWnd.q6ColorSetting[3]), (Color col) -> {
			OptWnd.q6ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q6ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q6ColorSetting_colorSetting", new String[]{"255","114","0","255"});
			OptWnd.q6ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q6ColorOptionWidget.currentColor = new Color(255, 114, 0, 255));
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(OptWnd.q5ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q5ColorTextEntry", "200")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q5ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
		prev = panel.add(OptWnd.q5ColorOptionWidget = new ColorOptionWidget("  Epic Quality:", "q5ColorSetting", 120, Integer.parseInt(OptWnd.q5ColorSetting[0]), Integer.parseInt(OptWnd.q5ColorSetting[1]), Integer.parseInt(OptWnd.q5ColorSetting[2]), Integer.parseInt(OptWnd.q5ColorSetting[3]), (Color col) -> {
			OptWnd.q5ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q5ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q5ColorSetting_colorSetting", new String[]{"165","0","255","255"});
			OptWnd.q5ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q5ColorOptionWidget.currentColor = new Color(165, 0, 255, 255));
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(OptWnd.q4ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q4ColorTextEntry", "100")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q4ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
		prev = panel.add(OptWnd.q4ColorOptionWidget = new ColorOptionWidget("  Rare Quality:", "q4ColorSetting", 120, Integer.parseInt(OptWnd.q4ColorSetting[0]), Integer.parseInt(OptWnd.q4ColorSetting[1]), Integer.parseInt(OptWnd.q4ColorSetting[2]), Integer.parseInt(OptWnd.q4ColorSetting[3]), (Color col) -> {
			OptWnd.q4ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q4ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q4ColorSetting_colorSetting", new String[]{"0","131","255","255"});
			OptWnd.q4ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q4ColorOptionWidget.currentColor = new Color(0, 131, 255, 255));
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(OptWnd.q3ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q3ColorTextEntry", "50")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q3ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
		prev = panel.add(OptWnd.q3ColorOptionWidget = new ColorOptionWidget("  Uncommon Quality:", "q3ColorSetting", 120, Integer.parseInt(OptWnd.q3ColorSetting[0]), Integer.parseInt(OptWnd.q3ColorSetting[1]), Integer.parseInt(OptWnd.q3ColorSetting[2]), Integer.parseInt(OptWnd.q3ColorSetting[3]), (Color col) -> {
			OptWnd.q3ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q3ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q3ColorSetting_colorSetting", new String[]{"0","214","10","255"});
			OptWnd.q3ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q3ColorOptionWidget.currentColor = new Color(0, 214, 10, 255));
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(OptWnd.q2ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q2ColorTextEntry", "10")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q2ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
		prev = panel.add(OptWnd.q2ColorOptionWidget = new ColorOptionWidget("  Common Quality:", "q2ColorSetting", 120, Integer.parseInt(OptWnd.q2ColorSetting[0]), Integer.parseInt(OptWnd.q2ColorSetting[1]), Integer.parseInt(OptWnd.q2ColorSetting[2]), Integer.parseInt(OptWnd.q2ColorSetting[3]), (Color col) -> {
			OptWnd.q2ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q2ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q2ColorSetting_colorSetting", new String[]{"255","255","255","255"});
			OptWnd.q2ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q2ColorOptionWidget.currentColor = new Color(255, 255, 255, 255));
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(OptWnd.q1ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q1ColorTextEntry", "1")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", ""));
				Utils.setpref("q1ColorTextEntry", this.buf.line());
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				super.changed();
			}
		}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
		prev = panel.add(OptWnd.q1ColorOptionWidget = new ColorOptionWidget("  Junk Quality:", "q1ColorSetting", 120, Integer.parseInt(OptWnd.q1ColorSetting[0]), Integer.parseInt(OptWnd.q1ColorSetting[1]), Integer.parseInt(OptWnd.q1ColorSetting[2]), Integer.parseInt(OptWnd.q1ColorSetting[3]), (Color col) -> {
			OptWnd.q1ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q1ColorOptionWidget.currentColor = col);
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}){}, prev.pos("ur").adds(5, -2));
		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("q1ColorSetting_colorSetting", new String[]{"180","180","180","255"});
			OptWnd.q1ColorOptionWidget.cb.colorChooser.setColor(OptWnd.q1ColorOptionWidget.currentColor = new Color(180, 180, 180, 255));
			if (panel.ui != null && panel.ui.gui != null) panel.ui.gui.reloadAllItemOverlays();
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
