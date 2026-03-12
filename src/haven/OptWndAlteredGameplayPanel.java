package haven;

import haven.res.ui.pag.toggle.Toggle;

/**
 * Extracted Altered Gameplay Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.AlteredGameplaySettingsPanel.
 */
class OptWndAlteredGameplayPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;

		prev = panel.add(OptWnd.overrideCursorItemWhenHoldingAltCheckBox = new CheckBox("Override Cursor Item and prevent dropping it (hold Alt)"){
			{a = Utils.getprefb("overrideCursorItemWhenHoldingAlt", true);}
			public void set(boolean val) {
				Utils.setprefb("overrideCursorItemWhenHoldingAlt", val);
				a = val;
				if (val) {
					if (OptWnd.noCursorItemDroppingAnywhereCheckBox.a) {// ND: Set it like this so it doesn't do the optionInfoMsg
						OptWnd.noCursorItemDroppingAnywhereCheckBox.a = false;
						Utils.setprefb("noCursorItemDroppingAnywhere", false);
					}
					if (OptWnd.noCursorItemDroppingInWaterCheckBox.a) { // ND: Set it like this so it doesn't do the optionInfoMsg
						OptWnd.noCursorItemDroppingInWaterCheckBox.a = false;
						Utils.setprefb("noCursorItemDroppingInWater", false);
					}
				}
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Override Cursor Item and prevent dropping it (hold Alt) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, UI.scale(0, 2));
		OptWnd.overrideCursorItemWhenHoldingAltCheckBox.tooltip = OptWndTooltips.overrideCursorItemWhenHoldingAlt;
		prev = panel.add(OptWnd.noCursorItemDroppingAnywhereCheckBox = new CheckBox("No Cursor Item Dropping (Anywhere)"){
			{a = Utils.getprefb("noCursorItemDroppingAnywhere", false);}
			public void set(boolean val) {
				Utils.setprefb("noCursorItemDroppingAnywhere", val);
				a = val;
				if (val) {
					if (OptWnd.overrideCursorItemWhenHoldingAltCheckBox.a) { // ND: Set it like this so it doesn't do the optionInfoMsg
						OptWnd.overrideCursorItemWhenHoldingAltCheckBox.a = false;
						Utils.setprefb("overrideCursorItemWhenHoldingAlt", false);
					}
				}
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("No Item Dropping (Anywhere) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.noCursorItemDroppingAnywhereCheckBox.tooltip = OptWndTooltips.noCursorItemDroppingAnywhere;
		prev = panel.add(OptWnd.noCursorItemDroppingInWaterCheckBox = new CheckBox("No Cursor Item Dropping (Water Only)"){
			{a = Utils.getprefb("noCursorItemDroppingInWater", false);}
			public void set(boolean val) {
				Utils.setprefb("noCursorItemDroppingInWater", val);
				a = val;
				if (val) {
					if (OptWnd.overrideCursorItemWhenHoldingAltCheckBox.a) OptWnd.overrideCursorItemWhenHoldingAltCheckBox.set(false);
				}
				if (ui != null && ui.gui != null) {
					if (!OptWnd.noCursorItemDroppingAnywhereCheckBox.a) {
						ui.gui.optionInfoMsg("No Item Dropping (in Water) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					} else {
						ui.gui.optionInfoMsg("No Item Dropping (in Water) is now " + (val ? "ENABLED" : "DISABLED") + "!" + (val ? "" : " (WARNING!!!: No Item Dropping (Anywhere) IS STILL ENABLED, and it overwrites this option!)"), (val ? OptWnd.msgGreen : OptWnd.msgYellow), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.noCursorItemDroppingInWaterCheckBox.tooltip = OptWndTooltips.noCursorItemDroppingInWater;

		prev = panel.add(OptWnd.useOGControlsForBuildingAndPlacingCheckBox = new CheckBox("Use OG controls for Building and Placing"){
			{a = Utils.getprefb("useOGControlsForBuildingAndPlacing", true);}
			public void changed(boolean val) {
				Utils.setprefb("useOGControlsForBuildingAndPlacing", val);
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.useOGControlsForBuildingAndPlacingCheckBox.tooltip = OptWndTooltips.useOGControlsForBuildingAndPlacing;
		prev = panel.add(OptWnd.useImprovedInventoryTransferControlsCheckBox = new CheckBox("Use improved Inventory Transfer controls (hold Alt)"){
			{a = Utils.getprefb("useImprovedInventoryTransferControls", true);}
			public void changed(boolean val) {
				Utils.setprefb("useImprovedInventoryTransferControls", val);
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.useImprovedInventoryTransferControlsCheckBox.tooltip = OptWndTooltips.useImprovedInventoryTransferControls;

		prev = panel.add(OptWnd.tileCenteringCheckBox = new CheckBox("Tile Centering"){
			{a = Utils.getprefb("tileCentering", false);}
			public void set(boolean val) {
				Utils.setprefb("tileCentering", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Tile Centering is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.tileCenteringCheckBox.tooltip = OptWndTooltips.tileCentering;

		prev = panel.add(OptWnd.clickThroughContainerDecalCheckBox = new CheckBox("Click through Container Decal (Hold Ctrl to pick)"){
			{a = Utils.getprefb("clickThroughContainerDecal", true);}
			public void changed(boolean val) {
				Utils.setprefb("clickThroughContainerDecal", val);
			}
		}, prev.pos("bl").adds(0, 12));

		prev = panel.add(OptWnd.continuousWalkingCheckBox = new CheckBox("Continuous Walking when holding down Left Click"){
			{a = Utils.getprefb("continuousWalking", false);}
			public void changed(boolean val) {
				Utils.setprefb("continuousWalking", val);
			}
		}, prev.pos("bl").adds(0, 12));
		prev = panel.add(OptWnd.continuousPathfindingCheckBox = new CheckBox("Use Pathfinder while Continuous Walking"){
			{a = Utils.getprefb("continuousPathfinding", false);}
			public void changed(boolean val) {
				Utils.setprefb("continuousPathfinding", val);
			}
		}, prev.pos("bl").adds(12, 2));
		OptWnd.continuousPathfindingCheckBox.tooltip = RichText.render("When enabled alongside Continuous Walking and Walk with Pathfinder,\nholding left click will use A* pathfinding to avoid obstacles.", UI.scale(300));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
