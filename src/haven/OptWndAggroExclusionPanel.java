package haven;

import java.awt.Color;

/**
 * Extracted Aggro Exclusion Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.AggroExclusionSettingsPanel.
 */
class OptWndAggroExclusionPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		prev = panel.add(new Label("Manually attacking will still work, regardless of these settings!"), 0, 0);
		prev = panel.add(new Label("Select which Players should be excluded from Aggro Keybinds:"), prev.pos("bl").adds(0, 4));

		prev = panel.add(OptWnd.excludeGreenBuddyFromAggroCheckBox = new CheckBox("Green Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludeGreenBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeGreenBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.excludeGreenBuddyFromAggroCheckBox.lbl = Text.create("Green Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Green Memorised / Kinned Players", BuddyWnd.gc[1])));
		prev = panel.add(OptWnd.excludeRedBuddyFromAggroCheckBox = new CheckBox("Red Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludeRedBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeRedBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 4));
		OptWnd.excludeRedBuddyFromAggroCheckBox.lbl = Text.create("Red Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Red Memorised / Kinned Players", BuddyWnd.gc[2])));
		prev = panel.add(OptWnd.excludeBlueBuddyFromAggroCheckBox = new CheckBox("Blue Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludeBlueBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeBlueBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 4));
		OptWnd.excludeBlueBuddyFromAggroCheckBox.lbl = Text.create("Blue Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Blue Memorised / Kinned Players", BuddyWnd.gc[3])));
		prev = panel.add(OptWnd.excludeTealBuddyFromAggroCheckBox = new CheckBox("Teal Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludeTealBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeTealBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 4));
		OptWnd.excludeTealBuddyFromAggroCheckBox.lbl = Text.create("Teal Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Teal Memorised / Kinned Players", BuddyWnd.gc[4])));
		prev = panel.add(OptWnd.excludeYellowBuddyFromAggroCheckBox = new CheckBox("Yellow Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludeYellowBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeYellowBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 4));
		OptWnd.excludeYellowBuddyFromAggroCheckBox.lbl = Text.create("Yellow Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Yellow Memorised / Kinned Players", BuddyWnd.gc[5])));
		prev = panel.add(OptWnd.excludePurpleBuddyFromAggroCheckBox = new CheckBox("Purple Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludePurpleBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludePurpleBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 4));
		OptWnd.excludePurpleBuddyFromAggroCheckBox.lbl = Text.create("Purple Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Purple Memorised / Kinned Players", BuddyWnd.gc[6])));
		prev = panel.add(OptWnd.excludeOrangeBuddyFromAggroCheckBox = new CheckBox("Orange Memorised / Kinned Players"){
			{a = (Utils.getprefb("excludeOrangeBuddyFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeOrangeBuddyFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 4));
		OptWnd.excludeOrangeBuddyFromAggroCheckBox.lbl = Text.create("Orange Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Orange Memorised / Kinned Players", BuddyWnd.gc[7])));

		prev = panel.add(OptWnd.excludeAllVillageOrRealmMembersFromAggroCheckBox = new CheckBox("ALL Village & Realm Members (Regardless of Memo/Kin)"){
			{a = (Utils.getprefb("excludeAllVillageOrRealmMembersFromAggro", false));}
			public void changed(boolean val) {
				Utils.setprefb("excludeAllVillageOrRealmMembersFromAggro", val);
			}
		}, prev.pos("bl").adds(0, 20));
		OptWnd.excludeAllVillageOrRealmMembersFromAggroCheckBox.lbl = Text.create("ALL Village & Realm Members (Regardless of Memo/Kin)", PUtils.strokeImg(Text.std.render("ALL Village & Realm Members (Regardless of Memo/Kin)", new Color(151, 17, 17, 255))));

		prev = panel.add(new Label("PARTY MEMBERS ARE ALWAYS EXCLUDED!"), prev.pos("bl").adds(0, 20));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
