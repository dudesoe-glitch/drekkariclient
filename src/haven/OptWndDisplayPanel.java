package haven;

import haven.render.*;
import haven.res.ui.pag.toggle.Toggle;
import haven.resutil.Ridges;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Extracted Display Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.DisplaySettingsPanel.
 */
class OptWndDisplayPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget leftColumn;
		Widget middleColumn;
		Widget rightColumn;
		leftColumn = panel.add(new Label("Object fine-placement granularity"), 0, 0);
		{
			Label pos = panel.add(new Label("Position"), leftColumn.pos("bl").adds(5, 4));
			pos.tooltip = OptWndTooltips.granularityPosition;
			Label ang = panel.add(new Label("Angle"), pos.pos("bl").adds(0, 4));
			ang.tooltip = OptWndTooltips.granularityAngle;
			int x = Math.max(pos.pos("ur").x, ang.pos("ur").x);
			{
				Label dpy = new Label("");
				final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
				final int steps = (int)Math.round((smax - smin) / 0.25);
				int ival = (int)Math.round(MapView.plobpgran);
				panel.addhlp(Coord.of(x + UI.scale(5), pos.c.y), UI.scale(5),
						leftColumn = new HSlider(UI.scale(155) - x, 2, 65, (ival == 0) ? 65 : ival) {
							protected void added() {
								dpy();
							}
							void dpy() {
								dpy.settext((this.val == 65) ? "\u221e" : Integer.toString(this.val));
							}
							public void changed() {
								Utils.setprefd("plobpgran", MapView.plobpgran = ((this.val == 65) ? 0 : this.val));
								dpy();
							}
						},
						dpy);
				leftColumn.tooltip = OptWndTooltips.granularityPosition;
			}
			{
				Label dpy = new Label("");
				final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
				final int steps = (int)Math.round((smax - smin) / 0.25);
				int[] vals = {4, 5, 6, 8, 9, 10, 12, 15, 18, 20, 24, 30, 36, 40, 45, 60, 72, 90, 120, 180, 360};
				int ival = 0;
				for(int i = 0; i < vals.length; i++) {
					if(Math.abs((MapView.plobagran * 2) - vals[i]) < Math.abs((MapView.plobagran * 2) - vals[ival]))
						ival = i;
				}
				panel.addhlp(Coord.of(x + UI.scale(5), ang.c.y), UI.scale(5),
						leftColumn = new HSlider(UI.scale(155) - x, 0, vals.length - 1, ival) {
							protected void added() {
								dpy();
							}
							void dpy() {
								dpy.settext(String.format("%d\u00b0", 360 / vals[this.val]));
							}
							public void changed() {
								Utils.setprefd("plobagran", MapView.plobagran = (vals[this.val] / 2.0));
								dpy();
							}
						},
						dpy);
				leftColumn.tooltip = OptWndTooltips.granularityAngle;
			}
		}
		leftColumn = panel.add(OptWnd.highlightCliffsCheckBox = new CheckBox("Highlight Cliffs (Color Overlay)"){
			{a = (Utils.getprefb("highlightCliffs", false));}
			public void set(boolean val) {
				Utils.setprefb("highlightCliffs", val);
				a = val;
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Highlight Cliffs is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, leftColumn.pos("bl").adds(0, 18).x(0));
		OptWnd.highlightCliffsCheckBox.tooltip = OptWndTooltips.highlightCliffs;
		leftColumn = panel.add(OptWnd.highlightCliffsColorOptionWidget = new ColorOptionWidget("Highlight Cliffs Color:", "highlightCliffs", 115, Integer.parseInt(OptWnd.highlightCliffsColorSetting[0]), Integer.parseInt(OptWnd.highlightCliffsColorSetting[1]), Integer.parseInt(OptWnd.highlightCliffsColorSetting[2]), Integer.parseInt(OptWnd.highlightCliffsColorSetting[3]), (Color col) -> {
			Ridges.setCliffHighlightMat();
			if (panel.ui.sess != null)
				panel.ui.sess.glob.map.invalidateAll();
		}){}, leftColumn.pos("bl").adds(0, 1).x(0));

		leftColumn = panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("highlightCliffs" + "_colorSetting", new String[]{"255", "0", "0", "200"});
			OptWnd.highlightCliffsColorOptionWidget.cb.colorChooser.setColor(OptWnd.highlightCliffsColorOptionWidget.currentColor = new Color(255, 0, 0, 200));
			Ridges.setCliffHighlightMat();
			if (panel.ui.sess != null)
				panel.ui.sess.glob.map.invalidateAll();
		}), leftColumn.pos("ur").adds(10, 0));
		leftColumn.tooltip = OptWndTooltips.resetButton;

		leftColumn = panel.add(OptWnd.showContainerFullnessCheckBox = new CheckBox("Highlight Container Fullness:"){
			{a = (Utils.getprefb("showContainerFullness", true));}
			public void changed(boolean val) {
				Utils.setprefb("showContainerFullness", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 12).x(0));
		OptWnd.showContainerFullnessCheckBox.tooltip = OptWndTooltips.showContainerFullness;
		leftColumn = panel.add(OptWnd.showContainerFullnessFullCheckBox = new CheckBox("Full"){
			{a = (Utils.getprefb("showContainerFullnessFull", true));}
			public void changed(boolean val) {
				Utils.setprefb("showContainerFullnessFull", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(20, 4));
		panel.add(OptWnd.showContainerFullnessFullColorOptionWidget = new ColorOptionWidget("", "containerFullnessFull", 0, Integer.parseInt(OptWnd.containerFullnessFullColorSetting[0]), Integer.parseInt(OptWnd.containerFullnessFullColorSetting[1]), Integer.parseInt(OptWnd.containerFullnessFullColorSetting[2]), Integer.parseInt(OptWnd.containerFullnessFullColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
				panel.ui.gui.map.updatePlobContainerHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("containerFullnessFull" + "_colorSetting", new String[]{"170", "0", "0", "170"});
			OptWnd.showContainerFullnessFullColorOptionWidget.cb.colorChooser.setColor(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor = new Color(170, 0, 0, 170));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
				panel.ui.gui.map.updatePlobContainerHighlight();
			}
		}), OptWnd.showContainerFullnessFullColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.showContainerFullnessPartialCheckBox = new CheckBox("Partial"){
			{a = (Utils.getprefb("showContainerFullnessPartial", true));}
			public void changed(boolean val) {
				Utils.setprefb("showContainerFullnessPartial", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 8));
		panel.add(OptWnd.showContainerFullnessPartialColorOptionWidget = new ColorOptionWidget("", "containerFullnessPartial", 0, Integer.parseInt(OptWnd.containerFullnessPartialColorSetting[0]), Integer.parseInt(OptWnd.containerFullnessPartialColorSetting[1]), Integer.parseInt(OptWnd.containerFullnessPartialColorSetting[2]), Integer.parseInt(OptWnd.containerFullnessPartialColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
				panel.ui.gui.map.updatePlobContainerHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("containerFullnessPartial" + "_colorSetting", new String[]{"194", "155", "2", "140"});
			OptWnd.showContainerFullnessPartialColorOptionWidget.cb.colorChooser.setColor(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor = new Color(194, 155, 2, 140));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
				panel.ui.gui.map.updatePlobContainerHighlight();
			}
		}), OptWnd.showContainerFullnessPartialColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.showContainerFullnessEmptyCheckBox = new CheckBox("Empty"){
			{a = (Utils.getprefb("showContainerFullnessEmpty", true));}
			public void changed(boolean val) {
				Utils.setprefb("showContainerFullnessEmpty", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 8));
		panel.add(OptWnd.showContainerFullnessEmptyColorOptionWidget = new ColorOptionWidget("", "containerFullnessEmpty", 0, Integer.parseInt(OptWnd.containerFullnessEmptyColorSetting[0]), Integer.parseInt(OptWnd.containerFullnessEmptyColorSetting[1]), Integer.parseInt(OptWnd.containerFullnessEmptyColorSetting[2]), Integer.parseInt(OptWnd.containerFullnessEmptyColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
				panel.ui.gui.map.updatePlobContainerHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));

		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("containerFullnessEmpty" + "_colorSetting", new String[]{"0", "120", "0", "180"});
			OptWnd.showContainerFullnessEmptyColorOptionWidget.cb.colorChooser.setColor(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor = new Color(0, 120, 0, 180));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
				panel.ui.gui.map.updatePlobContainerHighlight();
			}
		}), OptWnd.showContainerFullnessEmptyColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.showWorkstationProgressCheckBox = new CheckBox("Highlight Workstation Progress:"){
			{a = (Utils.getprefb("showWorkstationProgress", true));}
			public void changed(boolean val) {
				Utils.setprefb("showWorkstationProgress", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 12).x(0));
		OptWnd.showWorkstationProgressCheckBox.tooltip = OptWndTooltips.showWorkstationProgress;
		leftColumn = panel.add(OptWnd.showWorkstationProgressFinishedCheckBox = new CheckBox("Finished"){
			{a = (Utils.getprefb("showWorkstationProgressFinished", true));}
			public void changed(boolean val) {
				Utils.setprefb("showWorkstationProgressFinished", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(20, 4));
		panel.add(OptWnd.showWorkstationProgressFinishedColorOptionWidget = new ColorOptionWidget("", "workstationProgressFinished", 0, Integer.parseInt(OptWnd.workstationProgressFinishedColorSetting[0]), Integer.parseInt(OptWnd.workstationProgressFinishedColorSetting[1]), Integer.parseInt(OptWnd.workstationProgressFinishedColorSetting[2]), Integer.parseInt(OptWnd.workstationProgressFinishedColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("workstationProgressFinished" + "_colorSetting", new String[]{"170", "0", "0", "170"});
			OptWnd.showWorkstationProgressFinishedColorOptionWidget.cb.colorChooser.setColor(OptWnd.showWorkstationProgressFinishedColorOptionWidget.currentColor = new Color(170, 0, 0, 170));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}), OptWnd.showWorkstationProgressFinishedColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.showWorkstationProgressInProgressCheckBox = new CheckBox("In progress"){
			{a = (Utils.getprefb("showWorkstationProgressInProgress", true));}
			public void changed(boolean val) {
				Utils.setprefb("showWorkstationProgressInProgress", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 8));
		panel.add(OptWnd.showWorkstationProgressInProgressColorOptionWidget = new ColorOptionWidget("", "workstationProgressInProgress", 0, Integer.parseInt(OptWnd.workstationProgressInProgressColorSetting[0]), Integer.parseInt(OptWnd.workstationProgressInProgressColorSetting[1]), Integer.parseInt(OptWnd.workstationProgressInProgressColorSetting[2]), Integer.parseInt(OptWnd.workstationProgressInProgressColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("workstationProgressInProgress" + "_colorSetting", new String[]{"194", "155", "2", "140"});
			OptWnd.showWorkstationProgressInProgressColorOptionWidget.cb.colorChooser.setColor(OptWnd.showWorkstationProgressInProgressColorOptionWidget.currentColor = new Color(194, 155, 2, 140));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}), OptWnd.showWorkstationProgressInProgressColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.showWorkstationProgressReadyForUseCheckBox = new CheckBox("Ready for use"){
			{a = (Utils.getprefb("showWorkstationProgressReadyForUse", true));}
			public void changed(boolean val) {
				Utils.setprefb("showWorkstationProgressReadyForUse", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 8));
		panel.add(OptWnd.showWorkstationProgressReadyForUseColorOptionWidget = new ColorOptionWidget("", "workstationProgressReadyForUse", 0, Integer.parseInt(OptWnd.workstationProgressReadyForUseColorSetting[0]), Integer.parseInt(OptWnd.workstationProgressReadyForUseColorSetting[1]), Integer.parseInt(OptWnd.workstationProgressReadyForUseColorSetting[2]), Integer.parseInt(OptWnd.workstationProgressReadyForUseColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("workstationProgressReadyForUse" + "_colorSetting", new String[]{"0", "120", "0", "180"});
			OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.cb.colorChooser.setColor(OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.currentColor = new Color(0, 120, 0, 180));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}), OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(OptWnd.showWorkstationProgressUnpreparedCheckBox = new CheckBox("Unprepared"){
			{a = (Utils.getprefb("showWorkstationProgressUnprepared", true));}
			public void changed(boolean val) {
				Utils.setprefb("showWorkstationProgressUnprepared", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}
		}, leftColumn.pos("bl").adds(0, 8));
		panel.add(OptWnd.showWorkstationProgressUnpreparedColorOptionWidget = new ColorOptionWidget("", "workstationProgressUnprepared", 0, Integer.parseInt(OptWnd.workstationProgressUnpreparedColorSetting[0]), Integer.parseInt(OptWnd.workstationProgressUnpreparedColorSetting[1]), Integer.parseInt(OptWnd.workstationProgressUnpreparedColorSetting[2]), Integer.parseInt(OptWnd.workstationProgressUnpreparedColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("workstationProgressUnprepared" + "_colorSetting", new String[]{"20", "20", "20", "180"});
			OptWnd.showWorkstationProgressUnpreparedColorOptionWidget.cb.colorChooser.setColor(OptWnd.showWorkstationProgressUnpreparedColorOptionWidget.currentColor = new Color(20, 20, 20, 180));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
				panel.ui.gui.map.updatePlobWorkstationProgressHighlight();
			}
		}), OptWnd.showWorkstationProgressUnpreparedColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;

		leftColumn = panel.add(OptWnd.showMineSupportCoverageCheckBox = new CheckBox("Show Mine Support Coverage"){
			{a = (Utils.getprefb("showMineSupportTiles", false));}
			public void set(boolean val) {
				Utils.setprefb("showMineSupportTiles", val);
				a = val;
				if (ui != null && ui.gui != null && ui.gui.map != null && ui.sess != null && ui.sess.glob != null){
					if (val) {
						GroundSupportOverlay.getInstance().setMap(ui.sess.glob.map);
						ui.gui.map.enol(GroundSupportOverlay.TAG);
						ui.sess.glob.oc.gobAction(gob -> {
							if (gob.msRadSize > 0) {
								GroundSupportOverlay.getInstance().addTilesInRadius(gob.rc, gob.msRadSize);
							}
						});
					} else {
						GroundSupportOverlay.getInstance().clear();
						ui.gui.map.disol(GroundSupportOverlay.TAG);
					}
					ui.gui.optionInfoMsg("Mine Support Coverage is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, leftColumn.pos("bl").adds(0, 22).x(0));
		OptWnd.showMineSupportCoverageCheckBox.tooltip = OptWndTooltips.showMineSupportCoverage;

		leftColumn = panel.add(OptWnd.safeTilesColorOptionWidget = new ColorOptionWidget("Safe Tiles Color:", "coveredTiles", 115, Integer.parseInt(OptWnd.coveredTilesColorSetting[0]), Integer.parseInt(OptWnd.coveredTilesColorSetting[1]), Integer.parseInt(OptWnd.coveredTilesColorSetting[2]), Integer.parseInt(OptWnd.coveredTilesColorSetting[3]), (Color col) -> {
			GroundSupportOverlay.material = new Material(new BaseColor(col),
					new States.Depthtest(States.Depthtest.Test.LE));
			GroundSupportOverlay.outlineMaterial = new Material(new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 255)),
					States.Depthtest.none, States.maskdepth);
			if (panel.ui != null && panel.ui.gui != null && panel.ui.gui.map != null && panel.ui.sess != null && panel.ui.sess.glob != null){
				GroundSupportOverlay.getInstance().clear();
				panel.ui.gui.map.disol(GroundSupportOverlay.TAG);
				ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
				scheduler.schedule(() -> {
					if (OptWnd.showMineSupportCoverageCheckBox.a) {
						GroundSupportOverlay.getInstance().setMap(panel.ui.sess.glob.map);
						panel.ui.gui.map.enol(GroundSupportOverlay.TAG);
						panel.ui.sess.glob.oc.gobAction(gob -> {
							if (gob.msRadSize > 0) {
								GroundSupportOverlay.getInstance().addTilesInRadius(gob.rc, gob.msRadSize);
							}
						});
					}
				}, 200, TimeUnit.MILLISECONDS);
			}
		}){}, leftColumn.pos("bl").adds(1, 0));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("coveredTiles" + "_colorSetting", new String[]{"0", "105", "210", "60"});
			OptWnd.safeTilesColorOptionWidget.cb.colorChooser.setColor(OptWnd.safeTilesColorOptionWidget.currentColor = new Color(0, 105, 210, 60));
			GroundSupportOverlay.material = new Material(new BaseColor(OptWnd.safeTilesColorOptionWidget.currentColor),
					new States.Depthtest(States.Depthtest.Test.LE));
			GroundSupportOverlay.outlineMaterial = new Material(new BaseColor(new Color(OptWnd.safeTilesColorOptionWidget.currentColor.getRed(), OptWnd.safeTilesColorOptionWidget.currentColor.getGreen(), OptWnd.safeTilesColorOptionWidget.currentColor.getBlue(), 255)),
					States.Depthtest.none, States.maskdepth);
			if (panel.ui != null && panel.ui.gui != null && panel.ui.gui.map != null && panel.ui.sess != null && panel.ui.sess.glob != null){
				GroundSupportOverlay.getInstance().clear();
				panel.ui.gui.map.disol(GroundSupportOverlay.TAG);
				ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
				scheduler.schedule(() -> {
					if (OptWnd.showMineSupportCoverageCheckBox.a) {
						GroundSupportOverlay.getInstance().setMap(panel.ui.sess.glob.map);
						panel.ui.gui.map.enol(GroundSupportOverlay.TAG);
						panel.ui.sess.glob.oc.gobAction(gob -> {
							if (gob.msRadSize > 0) {
								GroundSupportOverlay.getInstance().addTilesInRadius(gob.rc, gob.msRadSize);
							}
						});
					}
				}, 200, TimeUnit.MILLISECONDS);
			}
		}), OptWnd.safeTilesColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;

		leftColumn = panel.add(OptWnd.enableMineSweeperCheckBox = new CheckBox("Enable Mine Sweeper"){
			{a = (Utils.getprefb("enableMineSweeper", true));}
			public void set(boolean val) {
				Utils.setprefb("enableMineSweeper", val);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Mine Sweeper numbers are now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					if (ui != null && ui.gui != null && ui.gui.miningSafetyAssistantWindow != null)
						ui.gui.miningSafetyAssistantWindow.enableMineSweeperCheckBox.a = val;
				}
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12));
		OptWnd.enableMineSweeperCheckBox.tooltip = OptWndTooltips.enableMineSweeper;
		leftColumn = panel.add(new Label("Sweeper Display Duration (Min):"), leftColumn.pos("bl").adds(0, 2));
		leftColumn.tooltip = RichText.render("Use this to set how long you want the numbers to be displayed on the ground, in minutes. The numbers will be visible as long as the dust particle effect stays on the tile." +
				"\n" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{Changing this option will only affect the duration of newly spawned cave dust tiles. The duration is set once the wall tile is mined and the cave dust spawns in.}", UI.scale(300));
		panel.add(OptWnd.sweeperDurationDropbox = new OldDropBox<Integer>(UI.scale(40), OptWnd.sweeperDurations.size(), UI.scale(17)) {
			{
				super.change(OptWnd.sweeperDurations.get(OptWnd.sweeperSetDuration));
			}
			@Override
			protected Integer listitem(int i) {
				return OptWnd.sweeperDurations.get(i);
			}
			@Override
			protected int listitems() {
				return OptWnd.sweeperDurations.size();
			}
			@Override
			protected void drawitem(GOut g, Integer item, int i) {
				g.aimage(Text.renderstroked(item.toString()).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(Integer item) {
				super.change(item);
				OptWnd.sweeperSetDuration = OptWnd.sweeperDurations.indexOf(item);
				System.out.println(OptWnd.sweeperSetDuration);
				Utils.setprefi("sweeperSetDuration", OptWnd.sweeperDurations.indexOf(item));
				if (ui != null && ui.gui != null && ui.gui.miningSafetyAssistantWindow != null)
					ui.gui.miningSafetyAssistantWindow.sweeperDurationDropbox.change2(item);
			}
		}, leftColumn.pos("ul").adds(160, 2));

		middleColumn = panel.add(OptWnd.showObjectCollisionBoxesCheckBox = new CheckBox("Show Object Collision Boxes"){
			{a = (Utils.getprefb("showObjectCollisionBoxes", false));}
			public void set(boolean val) {
				Utils.setprefb("showObjectCollisionBoxes", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
					ui.gui.map.updatePlobCollisionBox();
				}
			}
		}, UI.scale(240, 0));
		OptWnd.showObjectCollisionBoxesCheckBox.tooltip = OptWndTooltips.showObjectCollisionBoxes;
		middleColumn = panel.add(OptWnd.collisionBoxColorOptionWidget = new ColorOptionWidget("Collision Box Color:", "collisionBox", 115, Integer.parseInt(OptWnd.collisionBoxColorSetting[0]), Integer.parseInt(OptWnd.collisionBoxColorSetting[1]), Integer.parseInt(OptWnd.collisionBoxColorSetting[2]), Integer.parseInt(OptWnd.collisionBoxColorSetting[3]), (Color col) -> {
			CollisionBox.SOLID_HOLLOW = Pipe.Op.compose(new ColorMask(col), new States.LineWidth(CollisionBox.WIDTH), Rendered.last, States.Depthtest.none);
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
				panel.ui.gui.map.updatePlobCollisionBox();
			}
		}){}, middleColumn.pos("bl").adds(1, 0));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("collisionBox" + "_colorSetting", new String[]{"255", "255", "255", "210"});
			OptWnd.collisionBoxColorOptionWidget.cb.colorChooser.setColor(OptWnd.collisionBoxColorOptionWidget.currentColor = new Color(255, 255, 255, 210));
			CollisionBox.SOLID_HOLLOW = Pipe.Op.compose(new ColorMask(OptWnd.collisionBoxColorOptionWidget.currentColor), new States.LineWidth(CollisionBox.WIDTH), Rendered.last, States.Depthtest.none);
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
				panel.ui.gui.map.updatePlobCollisionBox();
			}
		}), OptWnd.collisionBoxColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;

		Scrollport scroll = panel.add(new Scrollport(UI.scale(new Coord(230, 40))), middleColumn.pos("bl").adds(0, 8).xs(240));
		middleColumn = scroll;
		Widget cont = scroll.cont;
		addbtn(cont, "Show Collision Boxes Hotkey:", GameUI.kb_toggleCollisionBoxes, 0);

		middleColumn = panel.add(OptWnd.displayObjectDurabilityPercentageCheckBox = new CheckBox("Display Object Durability Percentage"){
			{a = (Utils.getprefb("displayObjectHealthPercentage", true));}
			public void changed(boolean val) {
				Utils.setprefb("displayObjectHealthPercentage", val);
			}
		}, middleColumn.pos("bl").adds(0, -9).x(UI.scale(240)));
		OptWnd.displayObjectDurabilityPercentageCheckBox.tooltip = OptWndTooltips.displayObjectDurabilityPercentage;
		middleColumn = panel.add(OptWnd.showDurabilityCrackTextureCheckBox = new CheckBox("Show Durability Crack Texture"){
			{a = (Utils.getprefb("showDurabilityCrackTexture", true));}
			public void changed(boolean val) {
				Utils.setprefb("showDurabilityCrackTexture", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::refreshGobHealthAttribute);
				}
			}
		}, middleColumn.pos("bl").adds(0, 2));
		OptWnd.showDurabilityCrackTextureCheckBox.tooltip = OptWndTooltips.showDurabilityCrackTexture;
		middleColumn = panel.add(OptWnd.displayObjectQualityOnInspectionCheckBox = new CheckBox("Display Object Quality on Inspection"){
			{a = (Utils.getprefb("displayObjectQualityOnInspection", true));}
			public void changed(boolean val) {
				Utils.setprefb("displayObjectQualityOnInspection", val);
			}
		}, middleColumn.pos("bl").adds(0, 2));
		OptWnd.displayObjectQualityOnInspectionCheckBox.tooltip = OptWndTooltips.displayObjectQualityOnInspection;

		middleColumn = panel.add(OptWnd.showCritterAurasCheckBox = new CheckBox("Show Critter Circle Auras (Clickable)"){
			{a = (Utils.getprefb("showCritterAuras", true));}
			public void changed(boolean val) {
				Utils.setprefb("showCritterAuras", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
				}
			}
		}, middleColumn.pos("bl").adds(0, 17));
		OptWnd.showCritterAurasCheckBox.tooltip = OptWndTooltips.showCritterAuras;
		middleColumn = panel.add(OptWnd.rabbitAuraColorOptionWidget = new ColorOptionWidget("Rabbit Aura:", "rabbitAura", 115, Integer.parseInt(OptWnd.rabbitAuraColorSetting[0]), Integer.parseInt(OptWnd.rabbitAuraColorSetting[1]), Integer.parseInt(OptWnd.rabbitAuraColorSetting[2]), Integer.parseInt(OptWnd.rabbitAuraColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
			}
		}){}, middleColumn.pos("bl").adds(1, 1));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("rabbitAura" + "_colorSetting", new String[]{"88", "255", "0", "140"});
			OptWnd.rabbitAuraColorOptionWidget.cb.colorChooser.setColor(OptWnd.rabbitAuraColorOptionWidget.currentColor = new Color(88, 255, 0, 140));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
			}
		}), OptWnd.rabbitAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		middleColumn = panel.add(OptWnd.genericCritterAuraColorOptionWidget = new ColorOptionWidget("Generic Critter Aura:", "genericCritterAura", 115, Integer.parseInt(OptWnd.genericCritterAuraColorSetting[0]), Integer.parseInt(OptWnd.genericCritterAuraColorSetting[1]), Integer.parseInt(OptWnd.genericCritterAuraColorSetting[2]), Integer.parseInt(OptWnd.genericCritterAuraColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
			}
		}){}, middleColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("genericCritterAura" + "_colorSetting", new String[]{"193", "0", "255", "140"});
			OptWnd.genericCritterAuraColorOptionWidget.cb.colorChooser.setColor(OptWnd.genericCritterAuraColorOptionWidget.currentColor = new Color(193, 0, 255, 140));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
			}
		}), OptWnd.genericCritterAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;


		middleColumn = panel.add(OptWnd.dangerousCritterAuraColorOptionWidget = new ColorOptionWidget("Dangerous Critter Aura:", "dangerousCritterAura", 115, Integer.parseInt(OptWnd.dangerousCritterAuraColorSetting[0]), Integer.parseInt(OptWnd.dangerousCritterAuraColorSetting[1]), Integer.parseInt(OptWnd.dangerousCritterAuraColorSetting[2]), Integer.parseInt(OptWnd.dangerousCritterAuraColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
			}
		}){}, middleColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("dangerousCritterAura" + "_colorSetting", new String[]{"193", "0", "0", "140"});
			OptWnd.dangerousCritterAuraColorOptionWidget.cb.colorChooser.setColor(OptWnd.dangerousCritterAuraColorOptionWidget.currentColor = new Color(193, 0, 0, 140));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
			}
		}), OptWnd.dangerousCritterAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;


		middleColumn = panel.add(OptWnd.showSpeedBuffAurasCheckBox = new CheckBox("Show Speed Buff Circle Auras"){
			{a = (Utils.getprefb("showSpeedBuffAuras", true));}
			public void set(boolean val) {
				Utils.setprefb("showSpeedBuffAuras", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateSpeedBuffAuras);
				}
			}
		}, middleColumn.pos("bl").adds(0, 18).x(UI.scale(240)));
		OptWnd.showSpeedBuffAurasCheckBox.tooltip = OptWndTooltips.showSpeedBuffAuras;
		middleColumn = panel.add(OptWnd.speedBuffAuraColorOptionWidget = new ColorOptionWidget("Speed Buff Aura:", "speedBuffAura", 115, Integer.parseInt(OptWnd.speedBuffAuraColorSetting[0]), Integer.parseInt(OptWnd.speedBuffAuraColorSetting[1]), Integer.parseInt(OptWnd.speedBuffAuraColorSetting[2]), Integer.parseInt(OptWnd.speedBuffAuraColorSetting[3]), (Color col) -> {
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateSpeedBuffAuras);
			}
		}){}, middleColumn.pos("bl").adds(1, 1));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("speedBuffAura" + "_colorSetting", new String[]{"255", "255", "255", "140"});
			OptWnd.speedBuffAuraColorOptionWidget.cb.colorChooser.setColor(OptWnd.speedBuffAuraColorOptionWidget.currentColor = new Color(255, 255, 255, 140));
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updateSpeedBuffAuras);
			}
		}), OptWnd.speedBuffAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;

		middleColumn = panel.add(OptWnd.showMidgesCircleAurasCheckBox = new CheckBox("Show Midges Circle Auras"){
			{a = (Utils.getprefb("showMidgesCircleAuras", true));}
			public void changed(boolean val) {
				Utils.setprefb("showMidgesCircleAuras", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateMidgesAuras);
				}
			}
		}, middleColumn.pos("bl").adds(0, 18).x(UI.scale(240)));
		OptWnd.showMidgesCircleAurasCheckBox.tooltip = OptWndTooltips.showMidgesCircleAuras;

		middleColumn = panel.add(OptWnd.showDangerousBeastRadiiCheckBox = new CheckBox("Show Dangerous Beast Radii"){
			{a = (Utils.getprefb("showDangerousBeastRadii", true));}
			public void changed(boolean val) {
				Utils.setprefb("showDangerousBeastRadii", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateDangerousBeastRadii);
				}
			}
		}, middleColumn.pos("bl").adds(0, 2));
		OptWnd.showDangerousBeastRadiiCheckBox.tooltip = OptWndTooltips.showDangerousBeastRadii;

		middleColumn = panel.add(OptWnd.showBeeSkepsRadiiCheckBox = new CheckBox("Show Bee Skep Radii"){
			{a = (Utils.getprefb("showBeeSkepsRadii", false));}
			public void set(boolean val) {
				Utils.setprefb("showBeeSkepsRadii", val);
				a = val;
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::updateBeeSkepRadius);
					ui.gui.optionInfoMsg("Bee Skep Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, middleColumn.pos("bl").adds(0, 2));
		OptWnd.showBeeSkepsRadiiCheckBox.tooltip = OptWndTooltips.showBeeSkepsRadii;
		middleColumn = panel.add(OptWnd.showFoodTroughsRadiiCheckBox = new CheckBox("Show Food Trough Radii"){
			{a = (Utils.getprefb("showFoodTroughsRadii", false));}
			public void set(boolean val) {
				Utils.setprefb("showFoodTroughsRadii", val);
				a = val;
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::updateTroughsRadius);
					ui.gui.optionInfoMsg("Food Trough Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, middleColumn.pos("bl").adds(0, 2));
		OptWnd.showFoodTroughsRadiiCheckBox.tooltip = OptWndTooltips.showFoodThroughsRadii;
		middleColumn = panel.add(OptWnd.showMoundBedsRadiiCheckBox = new CheckBox("Show Mound Beds Radii"){
			{a = (Utils.getprefb("showMoundBedsRadii", false));}
			public void set(boolean val) {
				Utils.setprefb("showMoundBedsRadii", val);
				a = val;
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::updateMoundBedsRadius);
					ui.gui.optionInfoMsg("Mound Beds Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, middleColumn.pos("bl").adds(0, 2));
		OptWnd.showMoundBedsRadiiCheckBox.tooltip = OptWndTooltips.showMoundBedsRadii;
		middleColumn = panel.add(OptWnd.objectPermanentHighlightingCheckBox = new CheckBox(""){
			{a = (Utils.getprefb("objectPermanentHighlighting", false));}
			public void changed(boolean val) {
				Utils.setprefb("objectPermanentHighlighting", val);
				if (!val) {
					if (ui != null && ui.gui != null)
						ui.sess.glob.oc.gobAction(Gob::removePermanentHighlightOverlay);
					Gob.permanentHighlightList.clear();
				}
			}
		}, middleColumn.pos("bl").adds(0, 20));
		OptWnd.objectPermanentHighlightingCheckBox.tooltip = OptWndTooltips.objectPermanentHighlighting;
		// ND: Doing funny workaround with 2 separate labels to split the checkbox on 2 rows haha
		panel.add(new Label("Permanently Highlight Objects with"){
			@Override
			public boolean mousedown(MouseDownEvent ev) {
				if(ev.b == 1) {
					OptWnd.objectPermanentHighlightingCheckBox.click();
					return(true);
				}
				return(super.mousedown(ev));
			}
		}, middleColumn.pos("ur").adds(6, -10)).tooltip = OptWndTooltips.objectPermanentHighlighting;;
		panel.add(new Label("Alt + Middle Click (Mouse Scroll Click)"){
			@Override
			public boolean mousedown(MouseDownEvent ev) {
				if(ev.b == 1) {
					OptWnd.objectPermanentHighlightingCheckBox.click();
					return(true);
				}
				return(super.mousedown(ev));
			}
		}, middleColumn.pos("bl").adds(21, -6)).tooltip = OptWndTooltips.objectPermanentHighlighting;;

		rightColumn = panel.add(new Label("Object Pinging Colors:"), UI.scale(480, 0));
		rightColumn = panel.add(OptWnd.areaChatPingColorOptionWidget = new ColorOptionWidget("Area Chat (Alt+LClick):", "areaChatPing", 115, Integer.parseInt(OptWnd.areaChatPingColorSetting[0]), Integer.parseInt(OptWnd.areaChatPingColorSetting[1]), Integer.parseInt(OptWnd.areaChatPingColorSetting[2]), Integer.parseInt(OptWnd.areaChatPingColorSetting[3]), (Color col) -> {
		}){}, rightColumn.pos("bl").adds(1, 1));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("areaChatPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
			OptWnd.areaChatPingColorOptionWidget.cb.colorChooser.setColor(OptWnd.areaChatPingColorOptionWidget.currentColor = new Color(255, 183, 0, 255));
		}), OptWnd.areaChatPingColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		rightColumn = panel.add(OptWnd.partyChatPingColorOptionWidget = new ColorOptionWidget("Party Chat (Alt+RClick):", "partyChatPing", 115, Integer.parseInt(OptWnd.partyChatPingColorSetting[0]), Integer.parseInt(OptWnd.partyChatPingColorSetting[1]), Integer.parseInt(OptWnd.partyChatPingColorSetting[2]), Integer.parseInt(OptWnd.partyChatPingColorSetting[3]), (Color col) -> {
		}){}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("partyChatPing" + "_colorSetting", new String[]{"243", "0", "0", "255"});
			OptWnd.partyChatPingColorOptionWidget.cb.colorChooser.setColor(OptWnd.partyChatPingColorOptionWidget.currentColor = new Color(243, 0, 0, 255));
		}), OptWnd.partyChatPingColorOptionWidget.pos("ur").adds(10, 0)).tooltip = OptWndTooltips.resetButton;
		OptWnd.partyChatPingColorOptionWidget.tooltip = OptWndTooltips.partyChatPingColorOption;
		rightColumn = panel.add(OptWnd.showObjectsSpeedCheckBox = new CheckBox("Show Objects Speed"){
			{a = Utils.getprefb("showObjectsSpeed", false);}
			public void changed(boolean val) {
				Utils.setprefb("showObjectsSpeed", val);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Objects Speed is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 12).x(UI.scale(480)));
		OptWnd.showObjectsSpeedCheckBox.tooltip = OptWndTooltips.showObjectsSpeed;
		rightColumn = panel.add(OptWnd.displayGrowthInfoCheckBox = new CheckBox("Display Growth Info on Plants and Trees"){
			{a = (Utils.getprefb("displayGrowthInfo", false));}
			public void changed(boolean val) {
				Utils.setprefb("displayGrowthInfo", val);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Growth Info on Plants and Trees is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 17));
		OptWnd.displayGrowthInfoCheckBox.tooltip = OptWndTooltips.displayGrowthInfo;
		rightColumn = panel.add(OptWnd.alsoShowOversizedTreesAbovePercentageCheckBox = new CheckBox("Also Show Trees Above %:"){
			{a = (Utils.getprefb("alsoShowOversizedTreesAbovePercentage", true));}
			public void changed(boolean val) {
				Utils.setprefb("alsoShowOversizedTreesAbovePercentage", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::refreshGrowthInfo);
				}
			}
		}, rightColumn.pos("bl").adds(12, 2));
		panel.add(OptWnd.oversizedTreesPercentageTextEntry = new TextEntry(UI.scale(36), Utils.getpref("oversizedTreesPercentage", "150")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
				this.settext(this.text().replaceAll("(?<=^.{3}).*", "")); // No more than 3 digits
				Utils.setpref("oversizedTreesPercentage", this.buf.line());
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::refreshGrowthInfo);
				}
				super.changed();
			}
		}, OptWnd.alsoShowOversizedTreesAbovePercentageCheckBox.pos("ur").adds(4, 0));
		rightColumn = panel.add(OptWnd.showTreesBushesHarvestIconsCheckBox = new CheckBox("Show Trees & Bushes Harvest Icons"){
			{a = Utils.getprefb("showTreesBushesHarvestIcons", false);}
			public void changed(boolean val) {
				Utils.setprefb("showTreesBushesHarvestIcons", val);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Trees & Bushes Harvest Icons are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 12).x(UI.scale(480)));
		OptWnd.showTreesBushesHarvestIconsCheckBox.tooltip = OptWndTooltips.showTreesBushesHarvestIcons;
		rightColumn = panel.add(OptWnd.showLowFoodWaterIconsCheckBox = new CheckBox("Show Low Food & Water Icons"){
			{a = Utils.getprefb("showLowFoodWaterIcons", false);}
			public void changed(boolean val) {
				Utils.setprefb("showLowFoodWaterIcons", val);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Low Food & Water Icons are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 2).x(UI.scale(480)));
		OptWnd.showLowFoodWaterIconsCheckBox.tooltip = OptWndTooltips.showLowFoodWaterIcons;
		rightColumn = panel.add(OptWnd.showBeeSkepsHarvestIconsCheckBox = new CheckBox("Show Bee Skep Harvest Icons"){
			{a = Utils.getprefb("showBeeSkepsHarvestIcons", false);}
			public void changed(boolean val) {
				Utils.setprefb("showBeeSkepsHarvestIcons", val);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Bee Skep Harvest Icons are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 2).x(UI.scale(480)));
		OptWnd.showBeeSkepsHarvestIconsCheckBox.tooltip = OptWndTooltips.showBeeSkepsHarvestIcons;

		rightColumn = panel.add(OptWnd.showBarrelContentsTextCheckBox = new CheckBox("Show Barrel Contents Text"){
			{a = (Utils.getprefb("showBarrelContentsText", true));}
			public void changed(boolean val) {
				Utils.setprefb("showBarrelContentsText", val);
				if (ui != null && ui.gui != null){
					ui.gui.optionInfoMsg("Barrel Contents Text is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 13));
		OptWnd.showBarrelContentsTextCheckBox.tooltip = OptWndTooltips.showBarrelContentsText;

		rightColumn = panel.add(OptWnd.showIconSignTextCheckBox = new CheckBox("Show Icon Sign Text"){
			{a = (Utils.getprefb("showIconSignText", true));}
			public void changed(boolean val) {
				Utils.setprefb("showIconSignText", val);
				if (ui != null && ui.gui != null){
					ui.gui.optionInfoMsg("Icon Sign Text is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.showIconSignTextCheckBox.tooltip = OptWndTooltips.showIconSignText;
		rightColumn = panel.add(OptWnd.showCheeseRacksTierTextCheckBox = new CheckBox("Show Cheese Racks Tier Text"){
			{a = (Utils.getprefb("showCheeseRacksTierText", false));}
			public void changed(boolean val) {
				Utils.setprefb("showCheeseRacksTierText", val);
				if (ui != null && ui.gui != null){
					ui.gui.optionInfoMsg("Cheese Racks Tier Text is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? OptWnd.msgGreen : OptWnd.msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.showCheeseRacksTierTextCheckBox.tooltip = OptWndTooltips.showCheeseRacksTierText;

		rightColumn = panel.add(OptWnd.removeMapTileBordersCheckBox = new CheckBox("Remove Map Tile Borders"){
			{a = Utils.getprefb("removeMapTileBorders", false);}
			public void changed(boolean val) {
				Utils.setprefb("removeMapTileBorders", val);
				refreshMapCache(panel);
			}
		}, rightColumn.pos("bl").adds(0, 15));
		OptWnd.removeMapTileBordersCheckBox.tooltip = OptWndTooltips.removeMapTileBorders;

		rightColumn = panel.add(OptWnd.simplifiedMapColorsCheckBox = new CheckBox("Simplified Map Colors"){
			{a = (Utils.getprefb("simplifiedMapColorsEnabled", false));}
			public void changed(boolean val) {
				Utils.setprefb("simplifiedMapColorsEnabled", val);
				SimplifiedMapColors.enabled = val;
				refreshMapCache(panel);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		String[] sprintLandsColor = Utils.getprefsa("simplifiedMapColors_sprintLands_colorSetting", new String[]{"0", "255", "0", "255"});
		rightColumn = panel.add(OptWnd.sprintLandsColorWidget = new ColorOptionWidget("4th speed:", "simplifiedMapColors_sprintLands", 160,
			Integer.parseInt(sprintLandsColor[0]), Integer.parseInt(sprintLandsColor[1]),
			Integer.parseInt(sprintLandsColor[2]), Integer.parseInt(sprintLandsColor[3]), (Color col) -> {
				SimplifiedMapColors.SprintLands = col;
				SimplifiedMapColors.updateSprintLandsMapping();
				refreshMapCache(panel);
			}){}, rightColumn.pos("bl").adds(1, 1));

		String[] thirdSpeedLandsColor = Utils.getprefsa("simplifiedMapColors_thirdSpeedLands_colorSetting", new String[]{"0", "128", "0", "255"});
		rightColumn = panel.add(OptWnd.thirdSpeedLandsColorWidget = new ColorOptionWidget("3rd speed:", "simplifiedMapColors_thirdSpeedLands", 160,
			Integer.parseInt(thirdSpeedLandsColor[0]), Integer.parseInt(thirdSpeedLandsColor[1]),
			Integer.parseInt(thirdSpeedLandsColor[2]), Integer.parseInt(thirdSpeedLandsColor[3]), (Color col) -> {
				SimplifiedMapColors.ThirdSpeedLands = col;
				SimplifiedMapColors.updateThirdSpeedLandsMapping();
				refreshMapCache(panel);
			}){}, rightColumn.pos("bl").adds(0, 4));

		String[] swampsColor = Utils.getprefsa("simplifiedMapColors_swamps_colorSetting", new String[]{"0", "128", "128", "255"});
		rightColumn = panel.add(OptWnd.swampsColorWidget = new ColorOptionWidget("Swamps:", "simplifiedMapColors_swamps", 160,
			Integer.parseInt(swampsColor[0]), Integer.parseInt(swampsColor[1]),
			Integer.parseInt(swampsColor[2]), Integer.parseInt(swampsColor[3]), (Color col) -> {
				SimplifiedMapColors.Swamps = col;
				SimplifiedMapColors.updateSwampsMapping();
				refreshMapCache(panel);
			}){}, rightColumn.pos("bl").adds(0, 4));

		String[] thicketColor = Utils.getprefsa("simplifiedMapColors_thicket_colorSetting", new String[]{"255", "255", "0", "255"});
		rightColumn = panel.add(OptWnd.thicketColorWidget = new ColorOptionWidget("Thicket:", "simplifiedMapColors_thicket", 160,
			Integer.parseInt(thicketColor[0]), Integer.parseInt(thicketColor[1]),
			Integer.parseInt(thicketColor[2]), Integer.parseInt(thicketColor[3]), (Color col) -> {
				SimplifiedMapColors.Thicket = col;
				SimplifiedMapColors.updateThicketMapping();
				refreshMapCache(panel);
			}){}, rightColumn.pos("bl").adds(0, 4));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}

	private static void refreshMapCache(Widget panel) {
		if (panel.ui != null && panel.ui.gui != null && panel.ui.gui.mapfile != null && panel.ui.gui.mapfile.view != null) {
			panel.ui.gui.mapfile.view.refreshMapCache();
		}
	}

	private static int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
		return (cont.addhl(new Coord(0, y), cont.sz.x,
				new Label(nm), new OptWnd.SetButton(UI.scale(70), cmd))
				+ UI.scale(2));
	}
}
