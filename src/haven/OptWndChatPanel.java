package haven;

/**
 * Extracted Chat Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.ChatSettingsPanel.
 */
class OptWndChatPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;

		prev = panel.add(OptWnd.chatAlertSoundsCheckBox = new CheckBox("Enable chat message notification sounds"){
			{a = (Utils.getprefb("chatAlertSounds", true));}
			public void changed(boolean val) {
				Utils.setprefb("chatAlertSounds", val);
			}
		}, 0, 0);

		prev = panel.add(OptWnd.areaChatAlertSoundsCheckBox = new CheckBox("Area Chat Sound"){
			{a = (Utils.getprefb("areaChatAlertSounds", true));}
			public void changed(boolean val) {
				Utils.setprefb("areaChatAlertSounds", val);
			}
		}, prev.pos("bl").adds(20, 4));

		prev = panel.add(OptWnd.privateChatAlertSoundsCheckBox = new CheckBox("Private Messages Sound"){
			{a = (Utils.getprefb("privateChatAlertSounds", true));}
			public void changed(boolean val) {
				Utils.setprefb("privateChatAlertSounds", val);
			}
		}, prev.pos("bl").adds(0, 4));

		prev = panel.add(OptWnd.partyChatAlertSoundsCheckBox = new CheckBox("Party Chat Sound"){
			{a = (Utils.getprefb("partyChatAlertSounds", true));}
			public void changed(boolean val) {
				Utils.setprefb("partyChatAlertSounds", val);
			}
		}, prev.pos("bl").adds(0, 4));

		prev = panel.add(OptWnd.villageChatAlertSoundsCheckBox = new CheckBox("Village Chat Sound"){
			{
				a = (Utils.getprefb("villageChatAlertSounds", true));
				tooltip = RichText.render("You must set a Village Name below, for this setting to properly work." +
						"\n" +
						"\nIf you don't set a village name, the sound alert will always trigger if chat message notification sounds are enabled.", UI.scale(300));
			}
			public void changed(boolean val) {
				Utils.setprefb("villageChatAlertSounds", val);
			}
		}, prev.pos("bl").adds(0, 4));

		prev = panel.add(new Label("Village Name:"), prev.pos("bl").adds(20, 4));
		panel.add(OptWnd.villageNameTextEntry = new TextEntry(UI.scale(100), Utils.getpref("villageNameForChatAlerts", "")){
			protected void changed() {
				Utils.setpref("villageNameForChatAlerts", this.buf.line());
				super.changed();
			}}, prev.pos("ur").adds(6, 0));

		prev = panel.add(OptWnd.autoSelectNewChatCheckBox = new CheckBox("Auto-select new chats"){
			{a = (Utils.getprefb("autoSelectNewChat", true));}
			public void changed(boolean val) {
				Utils.setprefb("autoSelectNewChat", val);
			}
		}, prev.pos("bl").adds(0, 4).x(0));

		prev = panel.add(OptWnd.removeRealmChatCheckBox = new CheckBox("Remove public realm chat (requires relog)"){
			{a = (Utils.getprefb("removeRealmChat", false));}
			public void changed(boolean val) {
				Utils.setprefb("removeRealmChat", val);
			}
		}, prev.pos("bl").adds(0, 4));

		prev = panel.add(OptWnd.showKinStatusChangeMessages = new CheckBox("Show kin status system messages"){
			{a = (Utils.getprefb("showKinStatusChangeMessages", true));}
			public void changed(boolean val) {
				Utils.setprefb("showKinStatusChangeMessages", val);
			}
		}, prev.pos("bl").adds(0, 4));
		prev = panel.add(new Label("System Messages List Size: "), prev.pos("bl").adds(0, 5));
		Label systemMessagesListSizeLabel = new Label(Utils.getprefi("systemMessagesListSize", 5) + " rows");
		panel.add(systemMessagesListSizeLabel, prev.pos("ur").adds(0, 0));
		prev = panel.add(OptWnd.systemMessagesListSizeSlider = new HSlider(UI.scale(230), 1, 10, Utils.getprefi("systemMessagesListSize", 5)) {
			public void changed() {
				Utils.setprefi("systemMessagesListSize", val);
				systemMessagesListSizeLabel.settext(val + " rows");
			}
		}, prev.pos("bl").adds(0, 2));
		prev = panel.add(new Label("System Messages Duration: "), prev.pos("bl").adds(0, 5));
		Label systemMessagesDurationLabel = new Label(Utils.getprefi("systemMessagesDuration", 4) + (Utils.getprefi("systemMessagesDuration", 5) > 1 ? " seconds" : " second"));
		panel.add(systemMessagesDurationLabel, prev.pos("ur").adds(0, 0));
		prev = panel.add(OptWnd.systemMessagesDurationSlider = new HSlider(UI.scale(230), 3, 10, Utils.getprefi("systemMessagesDuration", 4)) {
			public void changed() {
				Utils.setprefi("systemMessagesDuration", val);
				systemMessagesDurationLabel.settext(val + (val > 1 ? " seconds" : " second"));
			}
		}, prev.pos("bl").adds(0, 2));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
