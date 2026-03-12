package haven;

import haven.res.ui.pag.toggle.Toggle;
import haven.resutil.Ridges;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Extracted World Graphics Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.WorldGraphicsSettingsPanel.
 */
class OptWndWorldGraphicsPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget leftColumn;
		Widget rightColumn;
		panel.add(new Label(""), 278, 0); // To fix window width

		leftColumn = panel.add(OptWnd.nightVisionLabel = new Label("Night Vision / Brighter World:"), 0, 0);
		OptWnd.nightVisionLabel.tooltip = OptWndTooltips.nightVision;
		Glob.nightVisionBrightness = Utils.getprefd("nightVisionSetting", 0.0);
		leftColumn = panel.add(OptWnd.nightVisionSlider = new HSlider(UI.scale(200), 0, 650, (int)(Glob.nightVisionBrightness*1000)) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(Glob.nightVisionBrightness*1000);
			}
			public void changed() {
				Glob.nightVisionBrightness = val/1000.0;
				Utils.setprefd("nightVisionSetting", val/1000.0);
				if(ui.sess != null && ui.sess.glob != null) {
					ui.sess.glob.brighten();
				}
			}
		}, leftColumn.pos("bl").adds(0, 6));
		OptWnd.nightVisionSlider.tooltip = OptWndTooltips.nightVision;
		panel.add(OptWnd.nightVisionResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
			Glob.nightVisionBrightness = 0.0;
			OptWnd.nightVisionSlider.val = 0;
			Utils.setprefd("nightVisionSetting", 0.0);
			if(panel.ui.sess != null && panel.ui.sess.glob != null) {
				panel.ui.sess.glob.brighten();
			}
		}), leftColumn.pos("bl").adds(210, -20));
		OptWnd.nightVisionResetButton.tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.flatWorldCheckBox = new CheckBox("Flat World"){
			{a = Utils.getprefb("flatWorld", false);}
			public void changed(boolean val) {
				Utils.setprefb("flatWorld", val);
				if (ui.sess != null)
					ui.sess.glob.map.resetMap();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Flat World is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, leftColumn.pos("bl").adds(12, 8));
		OptWnd.flatWorldCheckBox.tooltip = OptWndTooltips.flatWorld;
		leftColumn = panel.add(OptWnd.disableTileSmoothingCheckBox = new CheckBox("Disable Tile Smoothing"){
			{a = Utils.getprefb("disableTileSmoothing", false);}
			public void changed(boolean val) {
				Utils.setprefb("disableTileSmoothing", val);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Tile Smoothing is now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? OptWnd.msgRed : OptWnd.msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.disableTileSmoothingCheckBox.tooltip = OptWndTooltips.disableTileSmoothing;
		leftColumn = panel.add(OptWnd.disableTileBlendingCheckBox = new CheckBox("Disable Tile Blending"){
			{a = Utils.getprefb("disableTileBlending", false);}
			public void changed(boolean val) {
				Utils.setprefb("disableTileBlending", val);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Tile Blending is now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? OptWnd.msgRed : OptWnd.msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.disableTileBlendingCheckBox.tooltip = OptWndTooltips.disableTileBlending;
		leftColumn = panel.add(OptWnd.disableTileTransitionsCheckBox = new CheckBox("Disable Tile Transitions"){
			{a = Utils.getprefb("disableTileTransitions", false);}
			public void changed(boolean val) {
				Utils.setprefb("disableTileTransitions", val);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Tile Transitions are now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? OptWnd.msgRed : OptWnd.msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.disableTileTransitionsCheckBox.tooltip = OptWndTooltips.disableTileTransitions;
		leftColumn = panel.add(OptWnd.hideFlavorObjectsCheckBox = new CheckBox("Hide Flavor Objects"){
			{a = Utils.getprefb("hideFlavorObjects", false);}
			public void changed(boolean val) {
				Utils.setprefb("hideFlavorObjects", val);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Flavor Objects are now " + (val ? "HIDDEN" : "SHOWN") + "!", (val ? OptWnd.msgGray : OptWnd.msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
				}
			}
		}, leftColumn.pos("bl").adds(0, 12));
		OptWnd.hideFlavorObjectsCheckBox.tooltip = OptWndTooltips.hideFlavorObjects;
		leftColumn = panel.add(OptWnd.simplifiedCropsCheckBox = new CheckBox("Simplified Crops"){
			{a = Utils.getprefb("simplifiedCrops", false);}
			public void changed(boolean val) {
				Utils.setprefb("simplifiedCrops", val);
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::refreshCrops);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.simplifiedCropsCheckBox.tooltip = OptWndTooltips.simplifiedCrops;
		leftColumn = panel.add(OptWnd.simplifiedForageablesCheckBox = new CheckBox("Simplified Forageables"){
			{a = Utils.getprefb("simplifiedForageables", false);}
			public void changed(boolean val) {
				Utils.setprefb("simplifiedForageables", val);
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::refreshForageables);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.simplifiedForageablesCheckBox.tooltip = OptWndTooltips.simplifiedForageables;
		leftColumn = panel.add(OptWnd.flatCaveWallsCheckBox = new CheckBox("Flat Cave Walls"){
			{a = Utils.getprefb("flatCaveWalls", false);}
			public void changed(boolean val) {
				Utils.setprefb("flatCaveWalls", val);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null)
					ui.gui.optionInfoMsg("Flat Cave Walls are now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
			}
		}, leftColumn.pos("bl").adds(0, 12));
		OptWnd.flatCaveWallsCheckBox.tooltip = OptWndTooltips.flatCaveWalls;
		leftColumn = panel.add(OptWnd.straightCliffEdgesCheckBox = new CheckBox("Straight Cliff Edges"){
			{a = Utils.getprefb("straightCliffEdges", false);}
			public void changed(boolean val) {
				Utils.setprefb("straightCliffEdges", val);
				if (ui.sess != null)
					ui.sess.glob.map.resetMap();
				if (ui != null && ui.gui != null)
					ui.gui.optionInfoMsg("Straight Cliff Edges are now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.straightCliffEdgesCheckBox.tooltip = OptWndTooltips.straightCliffEdges;
		leftColumn = panel.add(new Label("Other Altered Objects:"), leftColumn.pos("bl").adds(0, 10).x(UI.scale(0)));
		leftColumn = panel.add(OptWnd.flatCupboardsCheckBox = new CheckBox("Flat Cupboards"){
			{a = (Utils.getprefb("flatCupboards", true));}
			public void set(boolean val) {
				Utils.setprefb("flatCupboards", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCustomSizeAndRotation);
					ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
					ui.gui.map.updatePlobCustomSizeAndRotation();
					ui.gui.map.updatePlobCollisionBox();
				}
			}
		}, leftColumn.pos("bl").adds(12, 8));
		OptWnd.flatCupboardsCheckBox.tooltip = OptWndTooltips.flatCupboards;

		// TODO: ND: Would be nice if this was a scrollable list with selectable items, rather than individual checkboxes
		leftColumn = panel.add(new Label("Disable Variable Materials for Objects:"), leftColumn.pos("bl").adds(0, 10).x(UI.scale(0)));
		leftColumn = panel.add(OptWnd.disableHerbalistTablesVarMatsCheckBox = new CheckBox("Herbalist Tables Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableHerbalistTablesVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableHerbalistTablesVarMats", val);
			}
		}, leftColumn.pos("bl").adds(12, 8));
		leftColumn = panel.add(OptWnd.disableCupboardsVarMatsCheckBox = new CheckBox("Cupboards Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableCupboardsVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableCupboardsVarMats", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.disableChestsVarMatsCheckBox = new CheckBox("Chests Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableChestsVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableChestsVarMats", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.disableMetalCabinetsVarMatsCheckBox = new CheckBox("Metal Cabinets Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableMetalCabinetsVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableMetalCabinetsVarMats", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.disableTrellisesVarMatsCheckBox = new CheckBox("Trellises Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableTrellisesVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableTrellisesVarMats", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.disableSmokeShedsVarMatsCheckBox = new CheckBox("Smoke Sheds Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableSmokeShedsVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableSmokeShedsVarMats", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(OptWnd.disableAllObjectsVarMatsCheckBox = new CheckBox("ALL OBJECTS Variable Materials (Requires Reload)"){
			{a = (Utils.getprefb("disableAllObjectsVarMats", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableAllObjectsVarMats", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Palisades & Brick Walls Scale:"), leftColumn.pos("bl").adds(0, 10).x(0));
		leftColumn.tooltip = OptWndTooltips.palisadesAndBrickWallsScale;
		leftColumn = panel.add(OptWnd.palisadesAndBrickWallsScaleSlider = new HSlider(UI.scale(200), 40, 100, Utils.getprefi("palisadesAndBrickWallsScale", 100)) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = Utils.getprefi("palisadesAndBrickWallsScale", 100);
			}
			public void changed() {
				Utils.setprefi("palisadesAndBrickWallsScale", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::reloadPalisadeScale);
				}
			}
		}, leftColumn.pos("bl").adds(0, 6));
		OptWnd.palisadesAndBrickWallsScaleSlider.tooltip = OptWndTooltips.palisadesAndBrickWallsScale;
		panel.add(OptWnd.palisadesAndBrickWallsScaleResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
			OptWnd.palisadesAndBrickWallsScaleSlider.val = 100;
			if (panel.ui != null && panel.ui.gui != null)
				panel.ui.sess.glob.oc.gobAction(Gob::reloadPalisadeScale);
			Utils.setprefi("palisadesAndBrickWallsScale", 100);
		}), leftColumn.pos("bl").adds(210, -20));
		OptWnd.palisadesAndBrickWallsScaleResetButton.tooltip = OptWndTooltips.resetButton;

		rightColumn = panel.add(OptWnd.enableSkyboxCheckBox = new CheckBox("Enable Skybox"){
			{a = (Utils.getprefb("enableSkybox", false));}
			public void changed(boolean val) {
				Utils.setprefb("enableSkybox", val);
			}
		}, UI.scale(302, 0));
		OptWnd.enableSkyboxCheckBox.tooltip = OptWndTooltips.enableSkybox;

		rightColumn = panel.add(new Label("Skybox Style:"), rightColumn.pos("bl").adds(0, 4));
		List<String> skyboxStyles = Arrays.asList("Clouds", "Galaxy");
		panel.add(new OldDropBox<String>(skyboxStyles.size(), skyboxStyles) {
			{
				super.change(skyboxStyles.get(Utils.getprefi("skyboxStyle", 0)));
			}
			@Override
			protected String listitem(int i) {
				return skyboxStyles.get(i);
			}
			@Override
			protected int listitems() {
				return skyboxStyles.size();
			}
			@Override
			protected void drawitem(GOut g, String item, int i) {
				g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(String item) {
				super.change(item);
				for (int i = 0; i < skyboxStyles.size(); i++){
					if (item.equals(skyboxStyles.get(i))){
						Utils.setprefi("skyboxStyle", i);
						if (OptWnd.enableSkyboxCheckBox.a) { // ND: It's easier to just reset the checkbox to load the new skybox, haha...
							OptWnd.enableSkyboxCheckBox.set(false);
							if (OptWnd.skyboxFuture != null)
								OptWnd.skyboxFuture.cancel(true);
							OptWnd.skyboxFuture = OptWnd.skyboxExecutor.scheduleWithFixedDelay(optWnd::resetSkyboxCheckbox, 200, 3000, TimeUnit.MILLISECONDS);
						}
					}
				}
			}
		}, rightColumn.pos("ur").adds(4, 0));

		rightColumn = panel.add(new Label("Trees & Bushes Scale:"), rightColumn.pos("bl").adds(0, 14).xs(290));
		rightColumn = panel.add(OptWnd.treeAndBushScaleSlider = new HSlider(UI.scale(200), 30, 100, Utils.getprefi("treeAndBushScale", 100)) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = Utils.getprefi("treeAndBushScale", 100);
			}
			public void changed() {
				Utils.setprefi("treeAndBushScale", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::reloadTreeScale);
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
				}
			}
		}, rightColumn.pos("bl").adds(0, 6));
		panel.add(OptWnd.treeAndBushScaleResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
			OptWnd.treeAndBushScaleSlider.val = 100;
			if (panel.ui != null && panel.ui.gui != null)
				panel.ui.sess.glob.oc.gobAction(Gob::reloadTreeScale);
			Utils.setprefi("treeAndBushScale", 100);
		}), rightColumn.pos("bl").adds(210, -20));
		OptWnd.treeAndBushScaleResetButton.tooltip = OptWndTooltips.resetButton;
		rightColumn = panel.add(OptWnd.disableTreeAndBushSwayingCheckBox = new CheckBox("Disable Tree & Bush Swaying"){
			{a = Utils.getprefb("disableTreeAndBushSwaying", false);}
			public void changed(boolean val) {
				Utils.setprefb("disableTreeAndBushSwaying", val);
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::reloadTreeSwaying);
			}
		}, rightColumn.pos("bl").adds(12, 14));
		OptWnd.disableTreeAndBushSwayingCheckBox.tooltip = OptWndTooltips.disableTreeAndBushSwaying;
		rightColumn = panel.add(OptWnd.disableIndustrialSmokeCheckBox = new CheckBox("Disable Industrial Smoke (Requires Reload)"){
			{a = (Utils.getprefb("disableIndustrialSmoke", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableIndustrialSmoke", val);
				if (val) synchronized (ui.sess.glob.oc){
					for(Gob gob : ui.sess.glob.oc){
						if(gob.getres() != null && !gob.getres().name.equals("gfx/terobjs/clue")){
							synchronized (gob.ols){
								for(Gob.Overlay ol : gob.ols){
									if(ol.spr!= null && ol.spr.res != null && ol.spr.res.name.contains("ismoke")){
										gob.removeOl(ol);
									}
								}
							}
							gob.ols.clear();
						}
					}
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.disableIndustrialSmokeCheckBox.tooltip = OptWndTooltips.disableIndustrialSmoke;
		rightColumn = panel.add(OptWnd.disableScentSmokeCheckBox = new CheckBox("Disable Scent Smoke (Requires Reload)"){
			{a = (Utils.getprefb("disableScentSmoke", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableScentSmoke", val);
				if (val) synchronized (ui.sess.glob.oc){
					synchronized (ui.sess.glob.oc){
						for(Gob gob : ui.sess.glob.oc){
							if(gob.getres() != null && gob.getres().name.equals("gfx/terobjs/clue")){
								synchronized (gob.ols){
									for(Gob.Overlay ol : gob.ols){
										gob.removeOl(ol);
									}
								}
								gob.ols.clear();
							}
						}
					}
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.disableScentSmokeCheckBox.tooltip = OptWndTooltips.disableScentSmoke;

		rightColumn = panel.add(new Label("World Effects:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(290)));
		rightColumn = panel.add(OptWnd.disableSeasonalGroundColorsCheckBox = new CheckBox("Disable Seasonal Ground Colors"){
			{a = (Utils.getprefb("disableSeasonalGroundColors", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableSeasonalGroundColors", val);
			}
		}, rightColumn.pos("bl").adds(12, 8));
		OptWnd.disableSeasonalGroundColorsCheckBox.tooltip = OptWndTooltips.disableSeasonalGroundColors;

		rightColumn = panel.add(OptWnd.disableGroundCloudShadowsCheckBox = new CheckBox("Disable Ground Cloud Shadows"){
			{a = (Utils.getprefb("disableGroundCloudShadows", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableGroundCloudShadows", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.disableGroundCloudShadowsCheckBox.tooltip = OptWndTooltips.disableGroundCloudShadows;

		rightColumn = panel.add(OptWnd.disableRainCheckBox = new CheckBox("Disable Raining Particles"){
			{a = (Utils.getprefb("disableRain", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableRain", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(OptWnd.disableWetGroundOverlayCheckBox = new CheckBox("Disable Wet Ground Overlay"){
			{a = (Utils.getprefb("disableWetGroundOverlay", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableWetGroundOverlay", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.disableWetGroundOverlayCheckBox.tooltip = OptWndTooltips.disableWetGroundOverlay;

		rightColumn = panel.add(OptWnd.disableSnowingCheckBox = new CheckBox("Disable Snowing Particles"){
			{a = (Utils.getprefb("disableSnowing", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableSnowing", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Screen Effects:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(290)));
		rightColumn = panel.add(OptWnd.disableValhallaFilterCheckBox = new CheckBox("Disable Valhalla Desaturation Filter"){
			{a = (Utils.getprefb("disableValhallaFilter", true));}
			public void changed(boolean val) {
				Utils.setprefb("disableValhallaFilter", val);
			}
		}, rightColumn.pos("bl").adds(12, 8));
		OptWnd.disableValhallaFilterCheckBox.tooltip = OptWndTooltips.disableValhallaFilter;

		rightColumn = panel.add(OptWnd.disableScreenShakingCheckBox = new CheckBox("Disable Screen Shaking"){
			{a = (Utils.getprefb("disableScreenShaking", true));}
			public void changed(boolean val) {
				Utils.setprefb("disableScreenShaking", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.disableScreenShakingCheckBox.tooltip = OptWndTooltips.disableScreenShaking;

		rightColumn = panel.add(OptWnd.disableHempHighCheckBox = new CheckBox("Disable Hemp High"){
			{a = (Utils.getprefb("disableHempHigh", true));}
			public void changed(boolean val) {
				Utils.setprefb("disableHempHigh", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.disableOpiumHighCheckBox = new CheckBox("Disable Opium High"){
			{a = (Utils.getprefb("disableOpiumHigh", true));}
			public void changed(boolean val) {
				Utils.setprefb("disableOpiumHigh", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.disableLibertyCapsHighCheckBox = new CheckBox("Disable Liberty Caps High"){
			{a = (Utils.getprefb("disableLibertyCapsHigh", true));}
			public void changed(boolean val) {
				Utils.setprefb("disableLibertyCapsHigh", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.disableLibertyCapsHighCheckBox.setTextColor(Color.red);
		OptWnd.disableLibertyCapsHighCheckBox.tooltip = OptWndTooltips.disableLibertyCapsHigh;
		rightColumn = panel.add(OptWnd.disableDrunkennessDistortionCheckBox = new CheckBox("Disable Drunkenness Distortion"){
			{a = (Utils.getprefb("disableDrunkennessDistortion", true));}
			public void changed(boolean val) {
				Utils.setprefb("disableDrunkennessDistortion", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(OptWnd.onlyRenderCameraVisibleObjectsCheckBox = new CheckBox("Only Render Camera-Visible Objects (Experimental)"){
			{a = (Utils.getprefb("onlyRenderCameraVisibleObjects", false));}
			public void changed(boolean val) {
				Utils.setprefb("onlyRenderCameraVisibleObjects", val);
			}
		}, rightColumn.pos("bl").adds(0, 34));
		OptWnd.onlyRenderCameraVisibleObjectsCheckBox.tooltip = OptWndTooltips.onlyRenderCameraVisibleObjects;


		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 38));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
