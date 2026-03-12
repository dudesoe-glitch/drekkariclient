package haven;

/**
 * Extracted Action Bars Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.ActionBarsSettingsPanel.
 */
class OptWndActionBarsPanel {

	private static int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
		return (cont.addhl(new Coord(0, y), cont.sz.x,
				new Label(nm), new OptWnd.SetButton(UI.scale(140), cmd))
				+ UI.scale(2));
	}

	private static void addOrientationRadio(Widget panel, Widget prev, String prefName, int actionBarNumber) {
		final Widget p = panel;
		RadioGroup radioGroup = new RadioGroup(panel) {
			public void changed(int btn, String lbl) {
				try {
					if(btn==0) {
						Utils.setprefb(prefName, true);
						if (p.ui != null && p.ui.gui != null){
							GameUI.ActionBar actionBar = p.ui.gui.getActionBar(actionBarNumber);
							actionBar.setActionBarHorizontal(true);
						}
					}
					if(btn==1) {
						Utils.setprefb(prefName, false);
						if (p.ui != null && p.ui.gui != null){
							GameUI.ActionBar actionBar = p.ui.gui.getActionBar(actionBarNumber);
							actionBar.setActionBarHorizontal(false);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		Widget prevOption = radioGroup.add("Horizontal", prev.pos("ur").adds(40, 0));
		radioGroup.add("Vertical", prevOption.pos("ur").adds(10, 0));
		if (Utils.getprefb(prefName, true)){
			radioGroup.check(0);
		} else {
			radioGroup.check(1);
		}
	}

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		prev = panel.add(new Label("You can move the bars with Middle Mouse Button (scroll click)."), 0, 4);
		prev = panel.add(new Label("Enabled Action Bars:"), prev.pos("bl").adds(0, 12));
		panel.add(new Label("Action Bar Orientation:"), prev.pos("ur").adds(42, 0));
		prev = panel.add(new CheckBox("Action Bar 1"){
			{a = Utils.getprefb("showActionBar1", true);}
			public void changed(boolean val) {
				Utils.setprefb("showActionBar1", val);
				if (ui != null && ui.gui != null && ui.gui.actionBar1 != null){
					ui.gui.actionBar1.show(val);
				}
			}
		}, prev.pos("bl").adds(12, 6));
		addOrientationRadio(panel, prev, "actionBar1Horizontal", 1);

		prev = panel.add(new CheckBox("Action Bar 2"){
			{a = Utils.getprefb("showActionBar2", false);}
			public void changed(boolean val) {
				Utils.setprefb("showActionBar2", val);
				if (ui != null && ui.gui != null && ui.gui.actionBar2 != null){
					ui.gui.actionBar2.show(val);
				}
			}
		}, prev.pos("bl").adds(0, 2));
		addOrientationRadio(panel, prev, "actionBar2Horizontal", 2);

		prev = panel.add(new CheckBox("Action Bar 3"){
			{a = Utils.getprefb("showActionBar3", false);}
			public void changed(boolean val) {
				Utils.setprefb("showActionBar3", val);
				if (ui != null && ui.gui != null && ui.gui.actionBar3 != null){
					ui.gui.actionBar3.show(val);
				}
			}
		}, prev.pos("bl").adds(0, 2));
		addOrientationRadio(panel, prev, "actionBar3Horizontal", 3);

		prev = panel.add(new CheckBox("Action Bar 4"){
			{a = Utils.getprefb("showActionBar4", false);}
			public void changed(boolean val) {
				Utils.setprefb("showActionBar4", val);
				if (ui != null && ui.gui != null && ui.gui.actionBar4 != null){
					ui.gui.actionBar4.show(val);
				}
			}
		}, prev.pos("bl").adds(0, 2));
		addOrientationRadio(panel, prev, "actionBar4Horizontal", 4);

		prev = panel.add(new CheckBox("Action Bar 5"){
			{a = Utils.getprefb("showActionBar5", false);}
			public void changed(boolean val) {
				Utils.setprefb("showActionBar5", val);
				if (ui != null && ui.gui != null && ui.gui.actionBar5 != null){
					ui.gui.actionBar5.show(val);
				}
			}
		}, prev.pos("bl").adds(0, 2));
		addOrientationRadio(panel, prev, "actionBar5Horizontal", 5);

		prev = panel.add(new CheckBox("Action Bar 6"){
			{a = Utils.getprefb("showActionBar6", false);}
			public void changed(boolean val) {
				Utils.setprefb("showActionBar6", val);
				if (ui != null && ui.gui != null && ui.gui.actionBar6 != null){
					ui.gui.actionBar6.show(val);
				}
			}
		}, prev.pos("bl").adds(0, 2));
		addOrientationRadio(panel, prev, "actionBar6Horizontal", 6);

		prev = panel.add(OptWnd.holdCTRLtoRemoveActionButtonsCheckBox = new CheckBox("Hold CTRL when right-clicking to remove action buttons"){
			{a = (Utils.getprefb("holdCTRLtoRemoveActionButtons", false));}
			public void changed(boolean val) {
				Utils.setprefb("holdCTRLtoRemoveActionButtons", val);
			}
		}, prev.pos("bl").adds(0, 12).x(0));

		Scrollport scroll = panel.add(new Scrollport(UI.scale(new Coord(290, 380))), prev.pos("bl").adds(0,10).x(0));
		Widget cont = scroll.cont;
		int y = 0;
		y = cont.adda(new Label("Action Bar 1 Keybinds"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		for (int i = 0; i < GameUI.kb_actbar1.length; i++)
			y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar1[i], y);
		y = cont.adda(new Label("Action Bar 2 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		for (int i = 0; i < GameUI.kb_actbar2.length; i++)
			y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar2[i], y);
		y = cont.adda(new Label("Action Bar 3 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		for (int i = 0; i < GameUI.kb_actbar3.length; i++)
			y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar3[i], y);
		y = cont.adda(new Label("Action Bar 4 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		for (int i = 0; i < GameUI.kb_actbar4.length; i++)
			y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar4[i], y);
		y = cont.adda(new Label("Action Bar 5 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		for (int i = 0; i < GameUI.kb_actbar5.length; i++)
			y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar5[i], y);
		y = cont.adda(new Label("Action Bar 6 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		for (int i = 0; i < GameUI.kb_actbar6.length; i++)
			y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar6[i], y);
		panel.adda(optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
		panel.pack();
	}
}
