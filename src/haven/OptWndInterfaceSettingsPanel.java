package haven;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Extracted Interface Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.InterfaceSettingsPanel.
 */
class OptWndInterfaceSettingsPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget leftColumn = panel.add(new Label("Interface scale (requires restart)"), 0, 0);
		leftColumn.tooltip = OptWndTooltips.interfaceScale;
		{
			Label dpy = new Label("");
			final double gran = 0.05;
			final double smin = 1, smax = Math.floor(UI.maxscale() / gran) * gran;
			final int steps = (int)Math.round((smax - smin) / gran);
			panel.addhlp(leftColumn.pos("bl").adds(0, 4), UI.scale(5),
					leftColumn = new HSlider(UI.scale(160), 0, steps, (int)Math.round(steps * (Utils.getprefd("uiscale", 1.0) - smin) / (smax - smin))) {
						protected void added() {
							dpy();
						}
						void dpy() {
							dpy.settext(String.format("%.2f\u00d7", smin + (((double)this.val / steps) * (smax - smin))));
						}
						public void changed() {
							double val = smin + (((double)this.val / steps) * (smax - smin));
							Utils.setprefd("uiscale", val);
							dpy();
						}
					},
					dpy);
			leftColumn.tooltip = OptWndTooltips.interfaceScale;
		}
		leftColumn = panel.add(OptWnd.showFramerateCheckBox = new CheckBox("Show Framerate"){
			{a = (Utils.getprefb("showFramerate", true));}
			public void changed(boolean val) {
				GLPanel.Loop.showFramerate = val;
				Utils.setprefb("showFramerate", val);
			}
		}, leftColumn.pos("bl").adds(0, 18));
		OptWnd.showFramerateCheckBox.tooltip = OptWndTooltips.showFramerate;
		leftColumn = panel.add(OptWnd.snapWindowsBackInsideCheckBox = new CheckBox("Snap windows back when dragged out"){
			{a = (Utils.getprefb("snapWindowsBackInside", true));}
			public void changed(boolean val) {
				Utils.setprefb("snapWindowsBackInside", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.snapWindowsBackInsideCheckBox.tooltip = OptWndTooltips.snapWindowsBackInside;
		leftColumn = panel.add(OptWnd.dragWindowsInWhenResizingCheckBox = new CheckBox("Drag windows in when resizing game"){
			{a = (Utils.getprefb("dragWindowsInWhenResizing", false));}
			public void changed(boolean val) {
				Utils.setprefb("dragWindowsInWhenResizing", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.dragWindowsInWhenResizingCheckBox.tooltip = OptWndTooltips.dragWindowsInWhenResizing;
		leftColumn = panel.add(OptWnd.showHoverInventoriesWhenHoldingShiftCheckBox = new CheckBox("Show Hover-Inventories (Stacks, Belt, etc.) only when holding Shift"){
			{a = (Utils.getprefb("showHoverInventoriesWhenHoldingShift", true));}
			public void changed(boolean val) {
				Utils.setprefb("showHoverInventoriesWhenHoldingShift", val);
			}
		}, leftColumn.pos("bl").adds(0, 12));
		leftColumn = panel.add(OptWnd.showQuickSlotsCheckBox = new CheckBox("Enable Quick Slots Widget:"){
			{a = (Utils.getprefb("showQuickSlotsBar", true));}
			public void changed(boolean val) {
				Utils.setprefb("showQuickSlotsBar", val);
				if (ui != null && ui.gui != null && ui.gui.quickslots != null){
					ui.gui.quickslots.show(val);
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.showQuickSlotsCheckBox.tooltip = OptWndTooltips.showQuickSlots;
		leftColumn = panel.add(new Label("> Show:"), leftColumn.pos("bl").adds(0, 1).xs(0));
		leftColumn = panel.add(OptWnd.leftHandQuickSlotCheckBox = new CheckBox("Left Hand"){
			{a = Utils.getprefb("leftHandQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("leftHandQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(4, 0));
		panel.add(OptWnd.rightHandQuickSlotCheckBox = new CheckBox("Right Hand"){
			{a = Utils.getprefb("rightHandQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("rightHandQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(10, 0));

		leftColumn = panel.add(OptWnd.leftPouchQuickSlotCheckBox = new CheckBox("Left Pouch"){
			{a = Utils.getprefb("leftPouchQuickSlot", false);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("leftPouchQuickSlot", val);
			}
		}, leftColumn.pos("bl").adds(0, 1));
		panel.add(OptWnd.rightPouchQuickSlotCheckBox = new CheckBox("Right Pouch"){
			{a = Utils.getprefb("rightPouchQuickSlot", false);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("rightPouchQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(4, 0));

		leftColumn = panel.add(OptWnd.beltQuickSlotCheckBox = new CheckBox("Belt"){
			{a = Utils.getprefb("beltQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("beltQuickSlot", val);
			}
		}, leftColumn.pos("bl").adds(0, 1));
		panel.add(OptWnd.backpackQuickSlotCheckBox = new CheckBox("Backpack"){
			{a = Utils.getprefb("backpackQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("backpackQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(37, 0));
		leftColumn = panel.add(OptWnd.shouldersQuickSlotCheckBox = new CheckBox("Shoulders"){
			{a = Utils.getprefb("shouldersQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("shouldersQuickSlot", val);
			}
		}, leftColumn.pos("bl").adds(0, 1));
		leftColumn = panel.add(OptWnd.capeQuickSlotCheckBox = new CheckBox("Cape"){
			{a = Utils.getprefb("capeQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("capeQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(9, 0));


		leftColumn = panel.add(OptWnd.showStudyReportHistoryCheckBox = new CheckBox("Show Study Report History"){
			{a = (Utils.getprefb("showStudyReportHistory", true));}
			public void set(boolean val) {
				SAttrWnd.showStudyReportHistoryCheckBox.a = val;
				Utils.setprefb("showStudyReportHistory", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12).xs(0));
		OptWnd.showStudyReportHistoryCheckBox.tooltip = OptWndTooltips.showStudyReportHistory;
		leftColumn = panel.add(OptWnd.lockStudyReportCheckBox = new CheckBox("Lock Study Report"){
			{a = (Utils.getprefb("lockStudyReport", false));}
			public void set(boolean val) {
				SAttrWnd.lockStudyReportCheckBox.a = val;
				Utils.setprefb("lockStudyReport", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.lockStudyReportCheckBox.tooltip = OptWndTooltips.lockStudyReport;
		leftColumn = panel.add(OptWnd.soundAlertForFinishedCuriositiesCheckBox = new CheckBox("Sound Alert for Finished Curiosities"){
			{a = (Utils.getprefb("soundAlertForFinishedCuriosities", false));}
			public void set(boolean val) {
				SAttrWnd.soundAlertForFinishedCuriositiesCheckBox.a = val;
				Utils.setprefb("soundAlertForFinishedCuriosities", val);
				a = val;
				if (val) {
					try {
						File file = new File(haven.MainFrame.gameDir + "res/customclient/sfx/CurioFinished.wav");
						if (file.exists()) {
							AudioInputStream in = AudioSystem.getAudioInputStream(file);
							AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
							AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
							Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
							((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 0.8));
						}
					} catch (Exception e) {
					}
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.soundAlertForFinishedCuriositiesCheckBox.tooltip = OptWndTooltips.soundAlertForFinishedCuriosities;

		leftColumn = panel.add(OptWnd.alwaysOpenMiniStudyOnLoginCheckBox = new CheckBox("Always Open Mini-Study on Login"){
			{a = (Utils.getprefb("alwaysOpenMiniStudyOnLogin", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysOpenMiniStudyOnLogin", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(OptWnd.alwaysShowCombatUIStaminaBarCheckBox = new CheckBox("Always Show Combat UI Stamina Bar"){
			{a = (Utils.getprefb("alwaysShowCombatUIStaminaBar", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysShowCombatUIStaminaBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 12));
		OptWnd.alwaysShowCombatUIStaminaBarCheckBox.tooltip = OptWndTooltips.alwaysShowCombatUiBar;
		leftColumn = panel.add(OptWnd.alwaysShowCombatUIHealthBarCheckBox = new CheckBox("Always Show Combat UI Health Bar"){
			{a = (Utils.getprefb("alwaysShowCombatUIHealthBar", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysShowCombatUIHealthBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.alwaysShowCombatUIHealthBarCheckBox.tooltip = OptWndTooltips.alwaysShowCombatUiBar;

		leftColumn = panel.add(OptWnd.transparentQuestsObjectivesWindowCheckBox = new CheckBox("Transparent Quests Objectives Window"){
			{a = (Utils.getprefb("transparentQuestsObjectivesWindow", false));}
			public void changed(boolean val) {
				Utils.setprefb("transparentQuestsObjectivesWindow", val);
				if (ui != null && ui.gui != null && ui.gui.questObjectivesWindow != null && ui.gui.questObjectivesWindow.visible()) {
					ui.gui.questObjectivesWindow.resetDeco();
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.transparentQuestsObjectivesWindowCheckBox.tooltip = OptWndTooltips.transparentQuestsObjectivesWindow;

		Widget rightColumn;
		rightColumn = panel.add(new Label("UI Theme (Req. Restart):"), UI.scale(230, 2));
		List<String> uiThemes = Arrays.asList("Nightdawg Dark", "Trollex Red", "Trollex Blue", "Custom Theme");
		Widget uiThemesWdg = panel.add(new OldDropBox<String>(uiThemes.size(), uiThemes) {
			{
				super.change(uiThemes.get(Utils.getprefi("uiThemeDropBox", 0)));
			}
			@Override
			protected String listitem(int i) {
				return uiThemes.get(i);
			}
			@Override
			protected int listitems() {
				return uiThemes.size();
			}
			@Override
			protected void drawitem(GOut g, String item, int i) {
				g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(String item) {
				super.change(item);
				for (int i = 0; i < uiThemes.size(); i++){
					if (item.equals(uiThemes.get(i))){
						Utils.setprefi("uiThemeDropBox", i);
						Utils.setpref("uiThemeName", uiThemes.get(i));
					}
				}
			}
		}, rightColumn.pos("ur").adds(2, 0));
		uiThemesWdg.tooltip = OptWndTooltips.uiTheme;
		rightColumn.tooltip = OptWndTooltips.uiTheme;

		rightColumn = panel.add(OptWnd.extendedMouseoverInfoCheckBox = new CheckBox("Extended Mouseover Info (Dev)"){
			{a = (Utils.getprefb("extendedMouseoverInfo", false));}
			public void changed(boolean val) {
				Utils.setprefb("extendedMouseoverInfo", val);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		OptWnd.extendedMouseoverInfoCheckBox.tooltip = OptWndTooltips.extendedMouseoverInfo;
		rightColumn = panel.add(OptWnd.disableMenuGridHotkeysCheckBox = new CheckBox("Disable All Menu Grid Hotkeys"){
			{a = (Utils.getprefb("disableMenuGridHotkeys", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableMenuGridHotkeys", val);
			}
		}, rightColumn.pos("bl").adds(0, 15));
		OptWnd.disableMenuGridHotkeysCheckBox.tooltip = OptWndTooltips.disableMenuGridHotkeys;
		rightColumn = panel.add(OptWnd.alwaysOpenBeltOnLoginCheckBox = new CheckBox("Always Open Belt on Login"){
			{a = (Utils.getprefb("alwaysOpenBeltOnLogin", true));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysOpenBeltOnLogin", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.alwaysOpenBeltOnLoginCheckBox.tooltip = OptWndTooltips.alwaysOpenBeltOnLogin;
		rightColumn = panel.add(OptWnd.showMapMarkerNamesCheckBox = new CheckBox("Show Map Marker Names"){
			{a = (Utils.getprefb("showMapMarkerNames", true));}
			public void changed(boolean val) {
				Utils.setprefb("showMapMarkerNames", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.showMapMarkerNamesCheckBox.tooltip = OptWndTooltips.showMapMarkerNames;
		rightColumn = panel.add(OptWnd.verticalContainerIndicatorsCheckBox = new CheckBox("Vertical Container Indicators"){
			{a = (Utils.getprefb("verticalContainerIndicators", true));}
			public void changed(boolean val) {
				Utils.setprefb("verticalContainerIndicators", val);
			}
		}, rightColumn.pos("bl").adds(0, 32));
		OptWnd.verticalContainerIndicatorsCheckBox.tooltip = OptWndTooltips.verticalContainerIndicators;
		Label expWindowLocationLabel;
		rightColumn = panel.add(expWindowLocationLabel = new Label("Experience Event Window Location:"), rightColumn.pos("bl").adds(0, 11));{
			RadioGroup expWindowGrp = new RadioGroup(panel) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setprefb("expWindowLocationIsTop", true);
							OptWnd.expWindowLocationIsTop = true;
						}
						if(btn==1) {
							Utils.setprefb("expWindowLocationIsTop", false);
							OptWnd.expWindowLocationIsTop = false;
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			rightColumn = expWindowGrp.add("Top", rightColumn.pos("bl").adds(26, 3));
			rightColumn = expWindowGrp.add("Bottom", rightColumn.pos("ur").adds(30, 0));
			if (Utils.getprefb("expWindowLocationIsTop", true)){
				expWindowGrp.check(0);
			} else {
				expWindowGrp.check(1);
			}
		}
		expWindowLocationLabel.tooltip = OptWndTooltips.experienceWindowLocation;

		rightColumn = panel.add(new Label("Map Window Zoom Speed:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(230)));
		rightColumn = panel.add(OptWnd.mapZoomSpeedSlider = new HSlider(UI.scale(110), 10, 50, Utils.getprefi("mapZoomSpeed", 15)) {
			public void changed() {
				Utils.setprefi("mapZoomSpeed", val);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(60), "Reset", false).action(() -> {
			OptWnd.mapZoomSpeedSlider.val = 15;
			Utils.setprefi("mapZoomSpeed", 15);
		}), rightColumn.pos("ur").adds(6, -4)).tooltip = OptWndTooltips.resetButton;

		rightColumn = panel.add(new Label("Map Icons Size:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(230)));
		rightColumn = panel.add(OptWnd.mapIconsSizeSlider = new HSlider(UI.scale(110), 16, 40, Utils.getprefi("mapIconsSize", 20)) {
			public void changed() {
				Utils.setprefi("mapIconsSize", val);
				GobIcon.size = UI.scale(val);
				synchronized(GobIcon.Image.cache) {
					GobIcon.Image.cache.clear();
				}
				BufferedImage buf = MiniMap.plpImg.img;
				buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
				Coord tsz;
				if(buf.getWidth() > buf.getHeight())
					tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
				else
					tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
				buf = PUtils.convolve(buf, tsz, GobIcon.filter);
				MiniMap.plp = new TexI(buf);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(60), "Reset", false).action(() -> {
			OptWnd.mapIconsSizeSlider.val = 20;
			GobIcon.size = UI.scale(20);
			synchronized(GobIcon.Image.cache) {
				GobIcon.Image.cache.clear();
			}
			Utils.setprefi("mapIconsSize", 20);
			BufferedImage buf = MiniMap.plpImg.img;
			buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
			Coord tsz;
			if(buf.getWidth() > buf.getHeight())
				tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
			else
				tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
			buf = PUtils.convolve(buf, tsz, GobIcon.filter);
			MiniMap.plp = new TexI(buf);
		}), rightColumn.pos("ur").adds(6, -4)).tooltip = OptWndTooltips.resetButton;
		rightColumn = panel.add(OptWnd.improvedInstrumentMusicWindowCheckBox = new CheckBox("Improved Instrument Music Window"){
			{a = (Utils.getprefb("improvedInstrumentMusicWindow", true));}
			public void changed(boolean val) {
				Utils.setprefb("improvedInstrumentMusicWindow", val);
			}
		}, rightColumn.pos("bl").adds(0, 15));
		OptWnd.improvedInstrumentMusicWindowCheckBox.tooltip = OptWndTooltips.improvedInstrumentMusicWindow;
		rightColumn = panel.add(OptWnd.preventEscKeyFromClosingWindowsCheckBox = new CheckBox("Prevent ESC from closing Windows"){
			{a = (Utils.getprefb("preventEscKeyFromClosingWindows", false));}
			public void changed(boolean val) {
				Utils.setprefb("preventEscKeyFromClosingWindows", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.stackWindowsWhenOpenedCheckBox = new CheckBox("Stack Windows when Opened"){
			{a = (Utils.getprefb("stackWindowsWhenOpened", false));}
			public void changed(boolean val) {
				Utils.setprefb("stackWindowsWhenOpened", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.stackWindowsWhenOpenedCheckBox.tooltip = OptWndTooltips.stackWindowsWhenOpened;

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 30).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
