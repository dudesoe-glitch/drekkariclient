package haven;

import haven.res.ui.pag.toggle.Toggle;

import java.util.Arrays;
import java.util.List;

/**
 * Extracted Gameplay Automation Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.GameplayAutomationSettingsPanel.
 */
class OptWndGameplayAutomationPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		Widget rightColumn;

		Widget toggleLabel = panel.add(new Label("Toggle on Login:"), 0, 0);
		prev = panel.add(OptWnd.toggleTrackingOnLoginCheckBox = new CheckBox("Tracking"){
			{a = Utils.getprefb("toggleTrackingOnLogin", true);}
			public void changed(boolean val) {
				Utils.setprefb("toggleTrackingOnLogin", val);
			}
		}, toggleLabel.pos("bl").adds(0, 6).x(UI.scale(0)));
		prev = panel.add(OptWnd.toggleSwimmingOnLoginCheckBox = new CheckBox("Swimming"){
			{a = Utils.getprefb("toggleSwimmingOnLogin", true);}
			public void changed(boolean val) {
				Utils.setprefb("toggleSwimmingOnLogin", val);
			}
		}, prev.pos("bl").adds(0, 2));
		prev = panel.add(OptWnd.toggleCriminalActsOnLoginCheckBox = new CheckBox("Criminal Acts"){
			{a = Utils.getprefb("toggleCriminalActsOnLogin", true);}
			public void changed(boolean val) {
				Utils.setprefb("toggleCriminalActsOnLogin", val);
			}
		}, prev.pos("bl").adds(0, 2));

		rightColumn = panel.add(OptWnd.toggleSiegeEnginesOnLoginCheckBox = new CheckBox("Check for Siege Engines"){
			{a = Utils.getprefb("toggleSiegeEnginesOnLogin", true);}
			public void changed(boolean val) {
				Utils.setprefb("toggleSiegeEnginesOnLogin", val);
			}
		}, toggleLabel.pos("bl").adds(110, 6));
		rightColumn = panel.add(OptWnd.togglePartyPermissionsOnLoginCheckBox = new CheckBox("Party Permissions"){
			{a = Utils.getprefb("togglePartyPermissionsOnLogin", false);}
			public void changed(boolean val) {
				Utils.setprefb("togglePartyPermissionsOnLogin", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.toggleItemStackingOnLoginCheckBox = new CheckBox("Automatic Item Stacking"){
			{a = Utils.getprefb("toggleItemStackingOnLogin", false);}
			public void changed(boolean val) {
				Utils.setprefb("toggleItemStackingOnLogin", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		prev = panel.add(new Label("Default Speed on Login:"), prev.pos("bl").adds(0, 16).x(0));
		List<String> runSpeeds = Arrays.asList("Crawl", "Walk", "Run", "Sprint");
		panel.add(new OldDropBox<String>(runSpeeds.size(), runSpeeds) {
			{
				super.change(runSpeeds.get(Utils.getprefi("defaultSetSpeed", 2)));
			}
			@Override
			protected String listitem(int i) {
				return runSpeeds.get(i);
			}
			@Override
			protected int listitems() {
				return runSpeeds.size();
			}
			@Override
			protected void drawitem(GOut g, String item, int i) {
				g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(String item) {
				super.change(item);
				for (int i = 0; i < runSpeeds.size(); i++){
					if (item.equals(runSpeeds.get(i))){
						Utils.setprefi("defaultSetSpeed", i);
					}
				}
			}
		}, prev.pos("bl").adds(130, -16));

		prev = panel.add(new Label("Other gameplay automations:"), prev.pos("bl").adds(0, 14).x(0));
		prev = panel.add(OptWnd.autoSelect1stFlowerMenuCheckBox = new CheckBox("Auto-Select 1st Flower-Menu Option (hold Ctrl)"){
			{a = Utils.getprefb("autoSelect1stFlowerMenu", true);}
			public void changed(boolean val) {
				Utils.setprefb("autoSelect1stFlowerMenu", val);
			}
		}, prev.pos("bl").adds(0, 6));
		OptWnd.autoSelect1stFlowerMenuCheckBox.tooltip = OptWndTooltips.autoSelect1stFlowerMenu;
		prev = panel.add(OptWnd.autoRepeatFlowerMenuCheckBox = new CheckBox("Auto-Repeat Flower-Menu (hold Ctrl+Shift)"){
			{a = Utils.getprefb("autoRepeatFlowerMenu", true);}
			public void changed(boolean val) {
				Utils.setprefb("autoRepeatFlowerMenu", val);
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.autoRepeatFlowerMenuCheckBox.tooltip = OptWndTooltips.autoRepeatFlowerMenu;
		prev = panel.add(OptWnd.alsoUseContainersWithRepeaterCheckBox = new CheckBox("Also use containers with Auto-Repeat"){
			{a = Utils.getprefb("alsoUseContainersWithRepeater", true);}
			public void changed(boolean val) {
				Utils.setprefb("alsoUseContainersWithRepeater", val);
			}
		}, prev.pos("bl").adds(16, 2));
		OptWnd.alsoUseContainersWithRepeaterCheckBox.tooltip = OptWndTooltips.alsoUseContainersWithRepeater;
		prev = panel.add(new Button(UI.scale(250), "Flower-Menu Auto-Select Manager", false, () -> {
			if(!OptWnd.flowerMenuAutoSelectManagerWindow.attached) {
				panel.parent.parent.add(OptWnd.flowerMenuAutoSelectManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
				OptWnd.flowerMenuAutoSelectManagerWindow.show();
			} else {
				OptWnd.flowerMenuAutoSelectManagerWindow.show(!OptWnd.flowerMenuAutoSelectManagerWindow.visible);
			}
		}),prev.pos("bl").adds(0, 4).x(0));
		prev.tooltip = OptWndTooltips.flowerMenuAutoSelectManager;
		prev = panel.add(OptWnd.autoReloadCuriositiesFromInventoryCheckBox = new CheckBox("Auto-Reload Curiosities from Inventory"){
			{a = Utils.getprefb("autoStudyFromInventory", false);}
			public void set(boolean val) {
				SAttrWnd.autoReloadCuriositiesFromInventoryCheckBox.a = val;
				Utils.setprefb("autoStudyFromInventory", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 12).x(0));
		OptWnd.autoReloadCuriositiesFromInventoryCheckBox.tooltip = OptWndTooltips.autoReloadCuriositiesFromInventory;
		prev = panel.add(OptWnd.preventTablewareFromBreakingCheckBox = new CheckBox("Prevent Tableware from Breaking"){
			{a = Utils.getprefb("preventTablewareFromBreaking", true);}
			public void set(boolean val) {
				Utils.setprefb("preventTablewareFromBreaking", val);
				a = val;
				TableInfo.preventTablewareFromBreakingCheckBox.a = val;
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.preventTablewareFromBreakingCheckBox.tooltip = OptWndTooltips.preventTablewareFromBreaking;
		prev = panel.add(OptWnd.autoDropLeechesCheckBox = new CheckBox("Auto-Drop Leeches"){
			{a = Utils.getprefb("autoDropLeeches", true);}
			public void set(boolean val) {
				Utils.setprefb("autoDropLeeches", val);
				a = val;
				Equipory.autoDropLeechesCheckBox.a = val;
				if (ui != null && ui.gui != null) {
					Equipory eq = ui.gui.getequipory();
					if (eq != null && eq.myOwnEquipory) {
						eq.checkForLeeches = true;
					}
				}
			}
		}, prev.pos("bl").adds(0, 12));
		prev = panel.add(OptWnd.autoDropTicksCheckBox = new CheckBox("Auto-Drop Ticks"){
			{a = Utils.getprefb("autoDropTicks", true);}
			public void set(boolean val) {
				Utils.setprefb("autoDropTicks", val);
				a = val;
				Equipory.autoDropTicksCheckBox.a = val;
				if (ui != null && ui.gui != null) {
					Equipory eq = ui.gui.getequipory();
					if (eq != null && eq.myOwnEquipory) {
						eq.checkForTicks = true;
					}
				}
			}
		}, prev.pos("bl").adds(0, 2));
		prev = panel.add(OptWnd.autoEquipBunnySlippersPlateBootsCheckBox = new CheckBox("Auto-Equip Bunny Slippers/Plate Boots"){
			{a = Utils.getprefb("autoEquipBunnySlippersPlateBoots", true);}
			public void set(boolean val) {
				Utils.setprefb("autoEquipBunnySlippersPlateBoots", val);
				if (Equipory.autoEquipBunnySlippersPlateBootsCheckBox != null)
					Equipory.autoEquipBunnySlippersPlateBootsCheckBox.a = val;
				a = val;
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.tooltip = OptWndTooltips.autoEquipBunnySlippersPlateBoots;
		prev = panel.add(new Button(UI.scale(250), "Auto-Drop Manager", false, () -> {
			if(!OptWnd.autoDropManagerWindow.attached) {
				panel.parent.parent.add(OptWnd.autoDropManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
				OptWnd.autoDropManagerWindow.show();
			} else {
				OptWnd.autoDropManagerWindow.show(!OptWnd.autoDropManagerWindow.visible);
			}
		}),prev.pos("bl").adds(0, 12).x(0));
		prev = panel.add(new Button(UI.scale(250), "Per-Item Auto-Drop Config", false, () -> {
			if(!OptWnd.itemAutoDropWindow.attached) {
				panel.parent.parent.add(OptWnd.itemAutoDropWindow);
				OptWnd.itemAutoDropWindow.show();
			} else {
				OptWnd.itemAutoDropWindow.show(!OptWnd.itemAutoDropWindow.visible);
			}
		}),prev.pos("bl").adds(0, 4).x(0));

		prev = panel.add(OptWnd.autoPeaceAnimalsWhenCombatStartsCheckBox = new CheckBox("Auto-Peace Animals when Combat Starts"){
			{a = Utils.getprefb("autoPeaceAnimalsWhenCombatStarts", false);}
			public void set(boolean val) {
				Utils.setprefb("autoPeaceAnimalsWhenCombatStarts", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Autopeace Animals when Combat Starts is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.autoPeaceAnimalsWhenCombatStartsCheckBox.tooltip = OptWndTooltips.autoPeaceAnimalsWhenCombatStarts;
		prev = panel.add(OptWnd.preventUsingRawHideWhenRidingCheckBox = new CheckBox("Prevent using Raw Hide when Riding a Horse"){
			{a = Utils.getprefb("preventUsingRawHideWhenRiding", false);}
			public void changed(boolean val) {
				Utils.setprefb("preventUsingRawHideWhenRiding", val);
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.preventUsingRawHideWhenRidingCheckBox.tooltip = OptWndTooltips.preventUsingRawHideWhenRiding;
		prev = panel.add(OptWnd.autoDrinkingCheckBox = new CheckBox("Auto-Drink Water below threshold:"){
			{a = Utils.getprefb("autoDrinkTeaOrWater", false);}
			public void set(boolean val) {
				Utils.setprefb("autoDrinkTeaOrWater", val);
				a = val;
				if (ui != null && ui.gui != null) {
					String threshold = "75";
					if (!OptWnd.autoDrinkingThresholdTextEntry.text().isEmpty()) threshold = OptWnd.autoDrinkingThresholdTextEntry.text();
					ui.gui.optionInfoMsg("Auto-Drinking Water is now " + (val ? "ENABLED, with a " + threshold + "% treshhold!" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.autoDrinkingCheckBox.tooltip = OptWndTooltips.autoDrinking;
		panel.add(OptWnd.autoDrinkingThresholdTextEntry = new TextEntry(UI.scale(40), Utils.getpref("autoDrinkingThreshold", "75")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
				this.settext(this.text().replaceAll("(?<=^.{2}).*", "")); // No more than 2 digits
				Utils.setpref("autoDrinkingThreshold", this.buf.line());
				super.changed();
			}
		}, prev.pos("ur").adds(10, 0));

		prev = panel.add(OptWnd.enableQueuedMovementCheckBox = new CheckBox("Enable Queued Movement Window (Alt+Click)"){
			{a = Utils.getprefb("enableQueuedMovement", true);}
			public void set(boolean val) {
				Utils.setprefb("enableQueuedMovement", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Queued Movement - Checkpoint Route Window is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					if (!val && ui.gui.map.checkpointManager != null)
						ui.gui.map.checkpointManager.wdgmsg("close");
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.enableQueuedMovementCheckBox.tooltip = OptWndTooltips.enableQueuedMovement;

		prev = panel.add(OptWnd.autoSortContainersCheckBox = new CheckBox("Auto-Sort Containers on Open"){
			{a = Utils.getprefb("autoSortContainers", false);}
			public void set(boolean val) {
				Utils.setprefb("autoSortContainers", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Auto-Sort Containers on Open is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.autoSortContainersCheckBox.settip("Automatically sort items when opening a container inventory.");

		prev = panel.add(OptWnd.walkWithPathFinderCheckBox = new CheckBox("Walk with Pathfinder (Ctrl+Shift+Click)"){
			{a = Utils.getprefb("walkWithPathfinder", false);}
			public void set(boolean val) {
				Utils.setprefb("walkWithPathfinder", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Walk with Pathfinder (Ctrl+Shift+Click) is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.walkWithPathFinderCheckBox.tooltip = OptWndTooltips.walkWithPathfinder;
		prev = panel.add(OptWnd.drawPathfinderRouteCheckBox = new CheckBox("Draw Pathfinder Route on the ground"){
			{a = Utils.getprefb("drawPathfinderRoute", false);}
			public void changed(boolean val) {
				Utils.setprefb("drawPathfinderRoute", val);
			}
		}, prev.pos("bl").adds(12, 2));
		prev = panel.add(OptWnd.pathfindOnMinimapCheckBox = new CheckBox("Pathfind on Minimap Click"){
			{a = Utils.getprefb("pathfindOnMinimap", false);}
			public void set(boolean val) {
				Utils.setprefb("pathfindOnMinimap", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Pathfind on Minimap Click is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.pathfindOnMinimapCheckBox.tooltip = RichText.render("When enabled, left-clicking on the minimap/world map will use the pathfinder\ninstead of walking in a straight line. Handles long distances automatically.", UI.scale(300));
		prev = panel.add(OptWnd.terrainWeightedPathfindingCheckBox = new CheckBox("Terrain-Weighted Pathfinding"){
			{a = Utils.getprefb("terrainWeightedPathfinding", false);}
			public void changed(boolean val) {
				Utils.setprefb("terrainWeightedPathfinding", val);
			}
		}, prev.pos("bl").adds(12, 2));
		OptWnd.terrainWeightedPathfindingCheckBox.tooltip = RichText.render("When enabled, the pathfinder prefers roads and avoids swamps/bogs.\nRoads cost less, dense terrain costs more.", UI.scale(300));
		prev = panel.add(OptWnd.pathfindOnRightClickCheckBox = new CheckBox("Pathfind on Right-Click Interaction"){
			{a = Utils.getprefb("pathfindOnRightClick", false);}
			public void set(boolean val) {
				Utils.setprefb("pathfindOnRightClick", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Pathfind on Right-Click is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, prev.pos("bl").adds(0, 2));
		OptWnd.pathfindOnRightClickCheckBox.tooltip = RichText.render("When enabled, right-clicking a distant object will pathfind to it\nbefore interacting, instead of walking in a straight line.", UI.scale(300));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
