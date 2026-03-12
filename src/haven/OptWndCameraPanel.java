package haven;

import java.awt.*;
import java.util.function.Consumer;

/**
 * Extracted Camera Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.CameraSettingsPanel.
 */
class OptWndCameraPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		// ND: Capture panel reference for use in RadioGroup.changed() where 'ui' is needed
		final Widget p = panel;

		// Local widget references for visibility toggling
		// Free camera widgets
		final Label[] freeCamZoomSpeedLabel = new Label[1];
		final HSlider[] freeCamZoomSpeedSlider = new HSlider[1];
		final Button[] freeCamZoomSpeedResetButton = new Button[1];
		final Label[] freeCamRotationSensitivityLabel = new Label[1];
		final HSlider[] freeCamRotationSensitivitySlider = new HSlider[1];
		final Button[] freeCamRotationSensitivityResetButton = new Button[1];
		final Label[] freeCamHeightLabel = new Label[1];
		final HSlider[] freeCamHeightSlider = new HSlider[1];
		final Button[] freeCamHeightResetButton = new Button[1];
		final CheckBox[] lockVerticalAngleAt45DegreesCheckBox = new CheckBox[1];
		final CheckBox[] allowLowerFreeCamTiltCheckBox = new CheckBox[1];
		final CheckBox[] reverseFreeCamXAxisCheckBox = new CheckBox[1];
		final CheckBox[] reverseFreeCamYAxisCheckBox = new CheckBox[1];

		// Ortho camera widgets
		final CheckBox[] unlockedOrthoCamCheckBox = new CheckBox[1];
		final Label[] orthoCamZoomSpeedLabel = new Label[1];
		final HSlider[] orthoCamZoomSpeedSlider = new HSlider[1];
		final Button[] orthoCamZoomSpeedResetButton = new Button[1];
		final Label[] orthoCamRotationSensitivityLabel = new Label[1];
		final HSlider[] orthoCamRotationSensitivitySlider = new HSlider[1];
		final Button[] orthoCamRotationSensitivityResetButton = new Button[1];
		final CheckBox[] reverseOrthoCameraAxesCheckBox = new CheckBox[1];

		// Visibility toggle lambdas
		Consumer<Boolean> setFreeCameraSettingsVisibility = (bool) -> {
			freeCamZoomSpeedLabel[0].visible = bool;
			freeCamZoomSpeedSlider[0].visible = bool;
			freeCamZoomSpeedResetButton[0].visible = bool;
			freeCamRotationSensitivityLabel[0].visible = bool;
			freeCamRotationSensitivitySlider[0].visible = bool;
			freeCamRotationSensitivityResetButton[0].visible = bool;
			freeCamHeightLabel[0].visible = bool;
			freeCamHeightSlider[0].visible = bool;
			freeCamHeightResetButton[0].visible = bool;
			lockVerticalAngleAt45DegreesCheckBox[0].visible = bool;
			allowLowerFreeCamTiltCheckBox[0].visible = bool;
			reverseFreeCamXAxisCheckBox[0].visible = bool;
			reverseFreeCamYAxisCheckBox[0].visible = bool;
		};

		Consumer<Boolean> setOrthoCameraSettingsVisibility = (bool) -> {
			unlockedOrthoCamCheckBox[0].visible = bool;
			orthoCamZoomSpeedLabel[0].visible = bool;
			orthoCamZoomSpeedSlider[0].visible = bool;
			orthoCamZoomSpeedResetButton[0].visible = bool;
			orthoCamRotationSensitivityLabel[0].visible = bool;
			orthoCamRotationSensitivitySlider[0].visible = bool;
			orthoCamRotationSensitivityResetButton[0].visible = bool;
			reverseOrthoCameraAxesCheckBox[0].visible = bool;
		};

		panel.add(new Label(""), 278, 0); // ND: added this so the window's width does not change when switching camera type and closing/reopening the panel
		Widget TopPrev; // ND: these are always visible at the top, with either camera settings
		Widget FreePrev; // ND: used to calculate the positions for the Free camera settings
		Widget OrthoPrev; // ND: used to calculate the positions for the Ortho camera settings

		TopPrev = panel.add(new Label("Selected Camera Type:"), 0, 0);{
			RadioGroup camGrp = new RadioGroup(panel) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setpref("defcam", "Free");
							setFreeCameraSettingsVisibility.accept(true);
							setOrthoCameraSettingsVisibility.accept(false);
							MapView.currentCamera = 1;
							if (p.ui != null && p.ui.gui != null && p.ui.gui.map != null) {
								p.ui.gui.map.setcam("Free");
							}
						}
						if(btn==1) {
							Utils.setpref("defcam", "Ortho");
							setFreeCameraSettingsVisibility.accept(false);
							setOrthoCameraSettingsVisibility.accept(true);
							MapView.currentCamera = 2;
							if (p.ui != null && p.ui.gui != null && p.ui.gui.map != null) {
								p.ui.gui.map.setcam("Ortho");
							}
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
		TopPrev = camGrp.add("Free Camera", TopPrev.pos("bl").adds(16, 2));
		TopPrev = camGrp.add("Ortho Camera", TopPrev.pos("bl").adds(0, 1));
		TopPrev = panel.add(new Label("Camera Dragging:"), TopPrev.pos("bl").adds(0, 6).x(0));
			TopPrev = panel.add(OptWnd.allowMouse4CamDragCheckBox = new CheckBox("Also allow Mouse 4 Button to drag the Camera"){
				{a = (Utils.getprefb("allowMouse4CamDrag", false));}
				public void changed(boolean val) {
					Utils.setprefb("allowMouse4CamDrag", val);
				};
			}, TopPrev.pos("bl").adds(12, 2));
			TopPrev = panel.add(OptWnd.allowMouse5CamDragCheckBox = new CheckBox("Also allow Mouse 5 Button to drag the Camera"){
				{a = Utils.getprefb("allowMouse5CamDrag", false);}
				public void changed(boolean val) {
					Utils.setprefb("allowMouse5CamDrag", val);
				}
			}, TopPrev.pos("bl").adds(0, 2));
		TopPrev = panel.add(new Label("Selected Camera Settings:"), TopPrev.pos("bl").adds(0, 6).x(0));
		// ND: The Ortho Camera Settings
		OrthoPrev = panel.add(reverseOrthoCameraAxesCheckBox[0] = OptWnd.reverseOrthoCameraAxesCheckBox = new CheckBox("Reverse Ortho Look Axis"){
			{a = (Utils.getprefb("reverseOrthoCamAxis", true));}
			public void changed(boolean val) {
				Utils.setprefb("reverseOrthoCamAxis", val);
			};
		}, TopPrev.pos("bl").adds(12, 2));
		reverseOrthoCameraAxesCheckBox[0].tooltip = OptWndTooltips.reverseOrthoCameraAxes;
		OrthoPrev = panel.add(unlockedOrthoCamCheckBox[0] = OptWnd.unlockedOrthoCamCheckBox = new CheckBox("Unlocked Ortho Camera"){
			{a = Utils.getprefb("unlockedOrthoCam", true);}
			public void changed(boolean val) {
				Utils.setprefb("unlockedOrthoCam", val);
			}
		}, OrthoPrev.pos("bl").adds(0, 2));
		unlockedOrthoCamCheckBox[0].tooltip = OptWndTooltips.unlockedOrthoCam;
		OrthoPrev = panel.add(orthoCamZoomSpeedLabel[0] = new Label("Ortho Camera Zoom Speed:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
		OrthoPrev = panel.add(orthoCamZoomSpeedSlider[0] = OptWnd.orthoCamZoomSpeedSlider = new HSlider(UI.scale(200), 2, 40, Utils.getprefi("orthoCamZoomSpeed", 10)) {
			public void changed() {
				Utils.setprefi("orthoCamZoomSpeed", val);
			}
		}, OrthoPrev.pos("bl").adds(0, 4));
		panel.add(orthoCamZoomSpeedResetButton[0] = new Button(UI.scale(70), "Reset", false).action(() -> {
			orthoCamZoomSpeedSlider[0].val = 10;
			Utils.setprefi("orthoCamZoomSpeed", 10);
		}), OrthoPrev.pos("bl").adds(210, -20));
		orthoCamZoomSpeedResetButton[0].tooltip = OptWndTooltips.resetButton;
			OrthoPrev = panel.add(orthoCamRotationSensitivityLabel[0] = new Label("Ortho Camera Rotation Sensitivity:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
			OrthoPrev = panel.add(orthoCamRotationSensitivitySlider[0] = OptWnd.orthoCamRotationSensitivitySlider = new HSlider(UI.scale(200), 100, 1000, Utils.getprefi("orthoCamRotationSensitivity", 1000)) {
				public void changed() {
					Utils.setprefi("orthoCamRotationSensitivity", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 4));
			panel.add(orthoCamRotationSensitivityResetButton[0] = new Button(UI.scale(70), "Reset", false).action(() -> {
				orthoCamRotationSensitivitySlider[0].val = 1000;
				Utils.setprefi("orthoCamRotationSensitivity", 1000);
			}), OrthoPrev.pos("bl").adds(210, -20));
			orthoCamRotationSensitivityResetButton[0].tooltip = OptWndTooltips.resetButton;

		// ND: The Free Camera Settings
		FreePrev = panel.add(reverseFreeCamXAxisCheckBox[0] = OptWnd.reverseFreeCamXAxisCheckBox = new CheckBox("Reverse X Axis"){
			{a = (Utils.getprefb("reverseFreeCamXAxis", true));}
			public void changed(boolean val) {
				Utils.setprefb("reverseFreeCamXAxis", val);
			}
		}, TopPrev.pos("bl").adds(12, 2));
		panel.add(reverseFreeCamYAxisCheckBox[0] = OptWnd.reverseFreeCamYAxisCheckBox = new CheckBox("Reverse Y Axis"){
			{a = (Utils.getprefb("reverseFreeCamYAxis", true));}
			public void changed(boolean val) {
				Utils.setprefb("reverseFreeCamYAxis", val);
			}
		}, FreePrev.pos("ul").adds(110, 0));
		FreePrev = panel.add(lockVerticalAngleAt45DegreesCheckBox[0] = OptWnd.lockVerticalAngleAt45DegreesCheckBox = new CheckBox("Lock Vertical Angle at 45\u00B0"){
			{a = (Utils.getprefb("lockVerticalAngleAt45Degrees", false));}
			public void changed(boolean val) {
				Utils.setprefb("lockVerticalAngleAt45Degrees", val);
				if (p.ui.gui.map != null)
					if (p.ui.gui.map.camera instanceof MapView.FreeCam)
						((MapView.FreeCam)p.ui.gui.map.camera).telev = (float)Math.PI / 4.0f;
			}
		}, FreePrev.pos("bl").adds(0, 2));
		FreePrev = panel.add(allowLowerFreeCamTiltCheckBox[0] = OptWnd.allowLowerFreeCamTiltCheckBox = new CheckBox("Enable Lower Tilting Angle", Color.RED){
			{a = (Utils.getprefb("allowLowerTiltBool", false));}
			public void changed(boolean val) {
				Utils.setprefb("allowLowerTiltBool", val);
			}
		}, FreePrev.pos("bl").adds(0, 2));
		allowLowerFreeCamTiltCheckBox[0].tooltip = OptWndTooltips.allowLowerFreeCamTilt;
		allowLowerFreeCamTiltCheckBox[0].lbl = Text.create("Enable Lower Tilting Angle", PUtils.strokeImg(Text.std.render("Enable Lower Tilting Angle", new Color(185,0,0,255))));
		FreePrev = panel.add(freeCamZoomSpeedLabel[0] = new Label("Free Camera Zoom Speed:"), FreePrev.pos("bl").adds(0, 10).x(0));
		FreePrev = panel.add(freeCamZoomSpeedSlider[0] = OptWnd.freeCamZoomSpeedSlider = new HSlider(UI.scale(200), 4, 40, Utils.getprefi("freeCamZoomSpeed", 25)) {
			public void changed() {
				Utils.setprefi("freeCamZoomSpeed", val);
			}
		}, FreePrev.pos("bl").adds(0, 4));
		panel.add(freeCamZoomSpeedResetButton[0] = new Button(UI.scale(70), "Reset", false).action(() -> {
			freeCamZoomSpeedSlider[0].val = 25;
			Utils.setprefi("freeCamZoomSpeed", 25);
		}), FreePrev.pos("bl").adds(210, -20));
		freeCamZoomSpeedResetButton[0].tooltip = OptWndTooltips.resetButton;
		FreePrev = panel.add(freeCamRotationSensitivityLabel[0] = new Label("Free Camera Rotation Sensitivity:"), FreePrev.pos("bl").adds(0, 10).x(0));
		FreePrev = panel.add(freeCamRotationSensitivitySlider[0] = OptWnd.freeCamRotationSensitivitySlider = new HSlider(UI.scale(200), 100, 1000, Utils.getprefi("freeCamRotationSensitivity", 1000)) {
			public void changed() {
				Utils.setprefi("freeCamRotationSensitivity", val);
			}
		}, FreePrev.pos("bl").adds(0, 4));
		panel.add(freeCamRotationSensitivityResetButton[0] = new Button(UI.scale(70), "Reset", false).action(() -> {
			freeCamRotationSensitivitySlider[0].val = 1000;
			Utils.setprefi("freeCamRotationSensitivity", 1000);
		}), FreePrev.pos("bl").adds(210, -20));
		freeCamRotationSensitivityResetButton[0].tooltip = OptWndTooltips.resetButton;
		FreePrev = panel.add(freeCamHeightLabel[0] = new Label("Free Camera Height:"), FreePrev.pos("bl").adds(0, 10));
		freeCamHeightLabel[0].tooltip = OptWndTooltips.freeCamHeight;
		FreePrev = panel.add(freeCamHeightSlider[0] = OptWnd.freeCamHeightSlider = new HSlider(UI.scale(200), 10, 300, (Math.round((float) Utils.getprefd("cameraHeightDistance", 15f)))*10) {
			public void changed() {
				Utils.setprefd("cameraHeightDistance", (float) (val/10));
			}
		}, FreePrev.pos("bl").adds(0, 4));
		freeCamHeightSlider[0].tooltip = OptWndTooltips.freeCamHeight;
		panel.add(freeCamHeightResetButton[0] = new Button(UI.scale(70), "Reset", false).action(() -> {
			freeCamHeightSlider[0].val = 150;
			Utils.setprefd("cameraHeightDistance", 15f);
		}), FreePrev.pos("bl").adds(210, -20));
		freeCamHeightResetButton[0].tooltip = OptWndTooltips.resetButton;

		// ND: Finally, check which camera is selected and set the right options to be visible
		String startupSelectedCamera = Utils.getpref("defcam", "Free");
		if (startupSelectedCamera.equals("Free") || startupSelectedCamera.equals("worse") || startupSelectedCamera.equals("follow")){
			camGrp.check(0);
			Utils.setpref("defcam", "Free");
			setFreeCameraSettingsVisibility.accept(true);
			setOrthoCameraSettingsVisibility.accept(false);
			MapView.currentCamera = 1;
		}
		else {
			camGrp.check(1);
			Utils.setpref("defcam", "Ortho");
			setFreeCameraSettingsVisibility.accept(false);
			setOrthoCameraSettingsVisibility.accept(true);
			MapView.currentCamera = 2;
		}
		}

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), FreePrev.pos("bl").adds(0, 18));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
