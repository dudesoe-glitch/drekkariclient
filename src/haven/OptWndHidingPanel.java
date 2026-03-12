package haven;

import haven.render.*;

import java.awt.Color;

/**
 * Extracted Hiding Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.HidingSettingsPanel.
 */
class OptWndHidingPanel {

	private static int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
		return (cont.addhl(new Coord(0, y), cont.sz.x,
				new Label(nm), new OptWnd.SetButton(UI.scale(140), cmd))
				+ UI.scale(2));
	}

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		final Widget p = panel;
		Widget prev;
		Widget prev2;
		prev = panel.add(OptWnd.toggleGobHidingCheckBox = new CheckBox("Hide Objects"){
			{a = (Utils.getprefb("hideObjects", false));}
			public void set(boolean val) {
				Utils.setprefb("hideObjects", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, 0, 10);
		OptWnd.toggleGobHidingCheckBox.tooltip = OptWndTooltips.genericHasKeybind;

		panel.add(OptWnd.alsoFillTheHidingBoxesCheckBox = new CheckBox("Also fill the Hiding Boxes"){
			{a = (Utils.getprefb("alsoFillTheHidingBoxes", true));}
			public void changed(boolean val) {
				Utils.setprefb("alsoFillTheHidingBoxes", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("ur").adds(50, 0));
		OptWnd.alsoFillTheHidingBoxesCheckBox.tooltip = RichText.render("Fills in the boxes. Only the outer lines will remain visible through other objects (like cliffs).");

		prev = panel.add(OptWnd.dontHideObjectsThatHaveTheirMapIconEnabledCheckBox = new CheckBox("Don't hide Objects that have their Map Icon Enabled"){
			{a = (Utils.getprefb("dontHideObjectsThatHaveTheirMapIconEnabled", true));}
			public void changed(boolean val) {
				Utils.setprefb("dontHideObjectsThatHaveTheirMapIconEnabled", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));

		Scrollport scroll = panel.add(new Scrollport(UI.scale(new Coord(300, 40))), prev.pos("bl").adds(14, 16));
		Widget cont = scroll.cont;
		addbtn(cont, "Toggle object hiding hotkey:", GameUI.kb_toggleHidingBoxes, 0);

		prev = panel.add(OptWnd.hiddenObjectsColorOptionWidget = new ColorOptionWidget("Hidden Objects Box Color:", "hidingBox", 170, Integer.parseInt(OptWnd.hiddenObjectsColorSetting[0]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[1]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[2]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[3]), (Color col) -> {
			HidingBox.SOLID_FILLED = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
			HidingBox.SOLID_HOLLOW = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), Rendered.last, States.Depthtest.none);
			if (p.ui != null && p.ui.gui != null) {
				p.ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
				p.ui.gui.map.updatePlobHidingBox();
			}
		}){}, scroll.pos("bl").adds(1, -2));

		prev = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("hidingBox" + "_colorSetting", new String[]{"0", "225", "255", "170"});
			OptWnd.hiddenObjectsColorOptionWidget.cb.colorChooser.setColor(OptWnd.hiddenObjectsColorOptionWidget.currentColor = new Color(0, 225, 255, 170));
			HidingBox.SOLID_FILLED = Pipe.Op.compose(new BaseColor(OptWnd.hiddenObjectsColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
			HidingBox.SOLID_HOLLOW = Pipe.Op.compose(new BaseColor(OptWnd.hiddenObjectsColorOptionWidget.currentColor), new States.LineWidth(HidingBox.WIDTH), Rendered.last, States.Depthtest.none);
			if (p.ui != null && p.ui.gui != null) {
				p.ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
				p.ui.gui.map.updatePlobHidingBox();
			}
		}), prev.pos("ur").adds(30, 0));
		prev.tooltip = OptWndTooltips.resetButton;

		prev = panel.add(new Label("Objects that will be hidden:"), prev.pos("bl").adds(0, 20).x(0));

		prev2 = panel.add(OptWnd.hideTreesCheckbox = new CheckBox("Trees"){
			{a = Utils.getprefb("hideTrees", true);}
			public void changed(boolean val) {
				Utils.setprefb("hideTrees", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(16, 10));

		prev = panel.add(OptWnd.hideBushesCheckbox = new CheckBox("Bushes"){
			{a = Utils.getprefb("hideBushes", true);}
			public void changed(boolean val) {
				Utils.setprefb("hideBushes", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev2.pos("bl").adds(0, 2));

		prev = panel.add(OptWnd.hideBouldersCheckbox = new CheckBox("Boulders"){
			{a = Utils.getprefb("hideBoulders", true);}
			public void changed(boolean val) {
				Utils.setprefb("hideBoulders", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));

		prev = panel.add(OptWnd.hideTreeLogsCheckbox = new CheckBox("Tree Logs"){
			{a = Utils.getprefb("hideTreeLogs", true);}
			public void changed(boolean val) {
				Utils.setprefb("hideTreeLogs", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));

		prev = panel.add(OptWnd.hideWallsCheckbox = new CheckBox("Palisades and Brick Walls"){
			{a = Utils.getprefb("hideWalls", false);}
			public void changed(boolean val) {
				Utils.setprefb("hideWalls", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev2.pos("ur").adds(90, 0));
		prev.tooltip = RichText.render("Only wall sections, NOT gates!");

		// TODO ND: Gotta figure out a way to not hide the doors... somehow
		prev = panel.add(OptWnd.hideHousesCheckbox = new CheckBox("Houses"){
			{a = Utils.getprefb("hideHouses", false);}
			public void changed(boolean val) {
				Utils.setprefb("hideHouses", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));
		prev = panel.add(OptWnd.hideStockpilesCheckbox = new CheckBox("Stockpiles"){
			{a = Utils.getprefb("hideStockpiles", false);}
			public void changed(boolean val) {
				Utils.setprefb("hideStockpiles", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));
		prev = panel.add(OptWnd.hideCropsCheckbox = new CheckBox("Crops"){
			{a = Utils.getprefb("hideCrops", false);}
			public void changed(boolean val) {
				Utils.setprefb("hideCrops", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));
		prev.tooltip = RichText.render("These won't show a hiding box, cause there's no collision.", UI.scale(300));

		prev = panel.add(OptWnd.hideTrellisCheckbox = new CheckBox("Trellises"){
			{a = Utils.getprefb("hideTrellis", false);}
			public void changed(boolean val) {
				Utils.setprefb("hideTrellis", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}
		}, prev.pos("bl").adds(0, 2));
		prev.tooltip = RichText.render("This only hides the trellises, and not the crops growing on them.", UI.scale(300));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
