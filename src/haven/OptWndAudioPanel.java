package haven;

import haven.res.sfx.ambient.weather.wsound.WeatherSound;

import java.util.Arrays;
import java.util.List;

/**
 * Extracted Audio Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.AudioPanel.
 */
class OptWndAudioPanel {

	private static final int SLIDER_WIDTH = 220;

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget leftColumn, rightColumn;
		leftColumn = panel.add(new Label("Master audio volume"), 179, 0);
		leftColumn = panel.add(new HSlider(UI.scale(460), 0, 1000, (int)(Audio.volume * 1000)) {
			public void changed() {
				Audio.setvolume(val / 1000.0);
			}
		}, leftColumn.pos("bl").adds(0, 2).x(0));

		leftColumn = panel.add(new Label("In-game event volume (Sound FX)"), leftColumn.pos("bl").adds(0, 15));
		leftColumn = panel.add(new HSlider(UI.scale(SLIDER_WIDTH), 0, 1000, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(ui.audio.pos.volume * 1000);
			}
			public void changed() {
				ui.audio.pos.setvolume(val / 1000.0);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Background Music Volume (Custom Client)"), leftColumn.pos("bl").adds(0, 5));
		leftColumn = panel.add(OptWnd.themeSongVolumeSlider = new HSlider(UI.scale(220), 0, 100, Utils.getprefi("themeSongVolume", 40)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				if (LoginScreen.mainThemeClip != null) ((Audio.VolAdjust) LoginScreen.mainThemeClip).vol = val/100d;
				if (LoginScreen.charSelectThemeClip != null) ((Audio.VolAdjust) LoginScreen.charSelectThemeClip).vol = val/100d;
				if (GameUI.cabinThemeClip != null) ((Audio.VolAdjust) GameUI.cabinThemeClip).vol = val/100d;
				if (GameUI.caveThemeClip != null) ((Audio.VolAdjust) GameUI.caveThemeClip).vol = val/100d;
				if (GameUI.fishingThemeClip != null) ((Audio.VolAdjust) GameUI.fishingThemeClip).vol = val/100d;
				if (GameUI.hookahThemeClip != null) ((Audio.VolAdjust) GameUI.hookahThemeClip).vol = val/100d;
				if (GameUI.feastingThemeClip != null) ((Audio.VolAdjust) GameUI.feastingThemeClip).vol = val/100d;

				if (LoginScreen.themeSongVolumeSlider != null) LoginScreen.themeSongVolumeSlider.val = val;
				if (Charlist.themeSongVolumeSlider != null) Charlist.themeSongVolumeSlider.val = val;
				Utils.setprefi("themeSongVolume", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(new Label("Background Music Theme:"), leftColumn.pos("bl").adds(0, 6).x(0));
		List<String> musicThemes = Arrays.asList("Hurricane  ", "Legacy");
		panel.add(new OldDropBox<String>(musicThemes.size(), musicThemes) {
			{
				super.change(musicThemes.get(Utils.getprefi("backgroundMusicTheme", 0)));
			}
			@Override
			protected String listitem(int i) {
				return musicThemes.get(i);
			}
			@Override
			protected int listitems() {
				return musicThemes.size();
			}
			@Override
			protected void drawitem(GOut g, String item, int i) {
				g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(String item) {
				super.change(item);
				for (int i = 0; i < musicThemes.size(); i++){
					if (item.equals(musicThemes.get(i))){
						Utils.setprefi("backgroundMusicTheme", i);
					}
				}
				GameUI.settingStopAllThemes();
			}
		}, leftColumn.pos("ur").adds(0, 1));

		rightColumn = panel.add(new Label("Ambient volume"), UI.scale(240, 51));
		rightColumn = panel.add(new HSlider(UI.scale(SLIDER_WIDTH), 0, 1000, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(ui.audio.amb.volume * 1000);
			}
			public void changed() {
				ui.audio.amb.setvolume(val / 1000.0);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Interface sound volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(new HSlider(UI.scale(SLIDER_WIDTH), 0, 1000, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(ui.audio.aui.volume * 1000);
			}
			public void changed() {
				ui.audio.aui.setvolume(val / 1000.0);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Weather Sound Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.weatherSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("weatherSoundVolume", 30)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("weatherSoundVolume", val);
				WeatherSound.volumeUpdated = true;
			}
		}, rightColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Audio latency"), leftColumn.pos("bl").adds(195, 20));
		leftColumn.tooltip = OptWndTooltips.audioLatency;
		{
			Label dpy = new Label("");
			panel.addhlp(leftColumn.pos("bl").adds(0, 2).x(0), UI.scale(5),
				leftColumn = new HSlider(UI.scale(460-40), Math.round(Audio.fmt.getSampleRate() * 0.05f), Math.round(Audio.fmt.getSampleRate() / 4), Audio.bufsize()) {
					protected void added() {
						dpy();
					}
					void dpy() {
						dpy.settext(Math.round((this.val * 1000) / Audio.fmt.getSampleRate()) + " ms");
					}
					public void changed() {
						Audio.bufsize(val, true);
						dpy();
					}
				}, dpy);
			leftColumn.tooltip = OptWndTooltips.audioLatency;
		}

		leftColumn = panel.add(new Label("Other Sound Settings"), leftColumn.pos("bl").adds(177, 20));

		leftColumn = panel.add(new Label("Boiling Cauldron Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.cauldronSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("cauldronSoundVolume", 25)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("cauldronSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Squeak Sound Volume (Roasting Spit, etc.)"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.squeakSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("squeakSoundVolume", 25)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("squeakSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Butchering Sound Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.butcherSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("butcherSoundVolume", 75)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("butcherSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Quern Sound Effect Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.quernSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("quernSoundVolume", 10)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("quernSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Swoosh Sound Effect Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.swooshSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("swooshSoundVolume", 75)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("swooshSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Grammophone Hat Sound Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.grammophoneHatSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("grammophoneHatSoundVolume", 100)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("grammophoneHatSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Creak Sound Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.creakSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("creakSoundVolume", 40)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("creakSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Water Splash Sound Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = panel.add(OptWnd.waterSplashSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("waterSplashSoundVolume", 30)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("waterSplashSoundVolume", val); }
		}, leftColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Music Instruments Volume"), rightColumn.pos("bl").adds(0, 83));
		rightColumn = panel.add(OptWnd.instrumentsSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("instrumentsSoundVolume", 70)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("instrumentsSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Clap Sound Effect Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.clapSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("clapSoundVolume", 10)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("clapSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("White Duck Cap Sound Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.whiteDuckCapSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("whiteDuckCapSoundVolume", 75)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("whiteDuckCapSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Chipping Sound Effect Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.chippingSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("chippingSoundVolume", 75)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("chippingSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Mining Sound Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.miningSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("miningSoundVolume", 75)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("miningSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Doom Bell Cap Sound Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.doomBellCapSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("doomBellCapSoundVolume", 75)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("doomBellCapSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(new Label("Knarr Sound Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = panel.add(OptWnd.knarrSoundVolumeSlider = new HSlider(UI.scale(SLIDER_WIDTH), 0, 100, Utils.getprefi("knarrSoundVolume", 30)) {
			protected void attach(UI ui) { super.attach(ui); }
			public void changed() { Utils.setprefi("knarrSoundVolume", val); }
		}, rightColumn.pos("bl").adds(0, 2));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Options            "), leftColumn.pos("bl").adds(0, 30).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
