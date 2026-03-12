package haven;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Extracted Alarms & Sounds Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.AlarmsAndSoundsSettingsPanel.
 */
class OptWndAlarmsPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		AlarmWidgetComponents comps;

		panel.add(new Label("You can add your own alarm sound files in the \"AlarmSounds\" folder.", new Text.Foundry(Text.sans, 12)), 0, 0);
		panel.add(new Label("(The file extension must be .wav)", new Text.Foundry(Text.sans, 12)), UI.scale(0, 16));
		prev = panel.add(new Label("Enabled Player Alarms:"), UI.scale(0, 50));
		prev = panel.add(new Label("Sound File"), prev.pos("ur").add(70, 0));
		prev = panel.add(new Label("Volume"), prev.pos("ur").add(78, 0));

		comps = addAlarmWidget(panel, "whitePlayerAlarm", "White OR Unknown:", "ND_YoHeadsUp", true, null, prev);
		{OptWnd.whitePlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.whitePlayerAlarmFilename = comps.filename; OptWnd.whitePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "whiteVillageOrRealmPlayerAlarm", "Village/Realm Member:", "ND_HelloFriend", true, null, prev);
		{OptWnd.whiteVillageOrRealmPlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.whiteVillageOrRealmPlayerAlarmFilename = comps.filename; OptWnd.whiteVillageOrRealmPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "greenPlayerAlarm", "Green:", "ND_FlyingTheFriendlySkies", false, BuddyWnd.gc[1], prev);
		{OptWnd.greenPlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.greenPlayerAlarmFilename = comps.filename; OptWnd.greenPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "redPlayerAlarm", "Red:", "ND_EnemySighted", true, BuddyWnd.gc[2], prev);
		{OptWnd.redPlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.redPlayerAlarmFilename = comps.filename; OptWnd.redPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "bluePlayerAlarm", "Blue:", "", false, BuddyWnd.gc[3], prev);
		{OptWnd.bluePlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.bluePlayerAlarmFilename = comps.filename; OptWnd.bluePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "tealPlayerAlarm", "Teal:", "", false, BuddyWnd.gc[4], prev);
		{OptWnd.tealPlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.tealPlayerAlarmFilename = comps.filename; OptWnd.tealPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "yellowPlayerAlarm", "Yellow:", "", false, BuddyWnd.gc[5], prev);
		{OptWnd.yellowPlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.yellowPlayerAlarmFilename = comps.filename; OptWnd.yellowPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "purplePlayerAlarm", "Purple:", "", false, BuddyWnd.gc[6], prev);
		{OptWnd.purplePlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.purplePlayerAlarmFilename = comps.filename; OptWnd.purplePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "orangePlayerAlarm", "Orange:", "", false, BuddyWnd.gc[7], prev);
		{OptWnd.orangePlayerAlarmEnabledCheckbox = comps.checkbox; OptWnd.orangePlayerAlarmFilename = comps.filename; OptWnd.orangePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

		prev = panel.add(new Label("Enabled Sounds & Alerts:"), prev.pos("bl").add(0, 20).x(0));
		prev = panel.add(new Label("Sound File"), prev.pos("ur").add(69, 0));
		prev = panel.add(new Label("Volume"), prev.pos("ur").add(78, 0));

		comps = addAlarmWidget(panel, "combatStartSound", "Combat Started Alert:", "ND_HitAndRun", false, null, prev);
		{OptWnd.combatStartSoundEnabledCheckbox = comps.checkbox; OptWnd.combatStartSoundFilename = comps.filename; OptWnd.combatStartSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "cleaveSound", "Cleave Sound Effect:", "ND_Cleave", true, null, prev);
		{OptWnd.cleaveSoundEnabledCheckbox = comps.checkbox; OptWnd.cleaveSoundFilename = comps.filename; OptWnd.cleaveSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "opkSound", "Oppknock Sound Effect:", "ND_Opk", true, null, prev);
		{OptWnd.opkSoundEnabledCheckbox = comps.checkbox; OptWnd.opkSoundFilename = comps.filename; OptWnd.opkSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "ponyPowerSound", "Pony Power <10% Alert:", "ND_HorseEnergy", true, null, prev);
		{OptWnd.ponyPowerSoundEnabledCheckbox = comps.checkbox; OptWnd.ponyPowerSoundFilename = comps.filename; OptWnd.ponyPowerSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

		comps = addAlarmWidget(panel, "lowEnergySound", "Energy <2500% Alert:", "ND_NotEnoughEnergy", true, null, prev);
		{OptWnd.lowEnergySoundEnabledCheckbox = comps.checkbox; OptWnd.lowEnergySoundFilename = comps.filename; OptWnd.lowEnergySoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

		prev = panel.add(optWnd.CustomAlarmManagerButton = new Button(UI.scale(360), ">>> Other Alarms (Custom Alarm Manager) <<<", () -> {
			if(optWnd.alarmWindow == null) {
				optWnd.alarmWindow = panel.parent.parent.add(new AlarmWindow());
				optWnd.alarmWindow.show();
			} else {
				optWnd.alarmWindow.show(!optWnd.alarmWindow.visible);
				optWnd.alarmWindow.bottomNote.settext("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)");
				optWnd.alarmWindow.bottomNote.setcolor(Color.WHITE);
				optWnd.alarmWindow.bottomNote.c.x = UI.scale(140);
			}
		}), prev.pos("bl").adds(0, 18).x(UI.scale(51)));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}

	private static AlarmWidgetComponents addAlarmWidget(Widget panel, String prefPrefix, String label, String defaultFilename, boolean defaultEnabled, Color labelColor, Widget prev) {
		AlarmWidgetComponents out = new AlarmWidgetComponents();

		out.checkbox = new CheckBox(label) {
			{ a = Utils.getprefb(prefPrefix + "Enabled", defaultEnabled); }
			public void set(boolean val) {
				Utils.setprefb(prefPrefix + "Enabled", val);
				a = val;
			}
		};
		prev = panel.add(out.checkbox, prev.pos("bl").adds(0, 7).x(0));

		if (labelColor != null) {
			out.checkbox.lbl = Text.create(label, PUtils.strokeImg(Text.std.render(label, labelColor)));
		}

		out.filename = new TextEntry(UI.scale(140), Utils.getpref(prefPrefix + "Filename", defaultFilename)) {
			protected void changed() {
				this.settext(this.text().replaceAll(" ", ""));
				Utils.setpref(prefPrefix + "Filename", this.buf.line());
				super.changed();
			}
		};
		prev = panel.add(out.filename, prev.pos("ur").adds(0, -2).x(UI.scale(143)));

		out.volume = new HSlider(UI.scale(100), 0, 100, Utils.getprefi(prefPrefix + "Volume", 50)) {
			@Override
			public void changed() {
				Utils.setprefi(prefPrefix + "Volume", val);
				super.changed();
			}
		};
		prev = panel.add(out.volume, prev.pos("ur").adds(6, 3));

		TextEntry finalFilename = out.filename;
		HSlider finalVolume = out.volume;
		prev = panel.add(new Button(UI.scale(70), "Preview") {
			@Override
			public boolean mousedown(MouseDownEvent ev) {
				if (ev.b != 1)
					return true;
				File file = new File(haven.MainFrame.gameDir + "AlarmSounds/" + finalFilename.buf.line() + ".wav");
				if (!file.exists() || file.isDirectory()) {
					if (ui != null && ui.gui != null)
						ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!", Color.WHITE);
					return super.mousedown(ev);
				}
				try {
					AudioInputStream in = AudioSystem.getAudioInputStream(file);
					AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
					AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
					Audio.CS clip = new Audio.PCMClip(pcmStream, 2, 2);
					((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(clip, finalVolume.val / 50.0));
				} catch (UnsupportedAudioFileException | IOException e) {
					e.printStackTrace();
				}
				return super.mousedown(ev);
			}
		}, prev.pos("ur").adds(6, -5));

		out.lastWidget = prev;
		return out;
	}

	private static class AlarmWidgetComponents {
		CheckBox checkbox;
		TextEntry filename;
		HSlider volume;
		Widget lastWidget;
	}
}
