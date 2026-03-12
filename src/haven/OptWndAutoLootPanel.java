package haven;

/**
 * Extracted Auto-Loot Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.AutoLootSettingsPanel.
 */
class OptWndAutoLootPanel {

	private static int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
		return (cont.addhl(new Coord(0, y), cont.sz.x,
				new Label(nm), new OptWnd.SetButton(UI.scale(140), cmd))
				+ UI.scale(2));
	}

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		Widget leftColumn, rightColumn;

		Scrollport scroll = panel.add(new Scrollport(UI.scale(new Coord(350, 40))), 0, 0);
		prev = scroll.cont;
		addbtn(prev, "Loot Nearest Knocked Player hotkey:", GameUI.kb_lootNearestKnockedPlayer, 0);

		prev = panel.add(new Label("Auto-Loot the following gear from players (when stealing):"), prev.pos("bl").adds(0, 6));

		leftColumn = panel.add(OptWnd.autoLootHeadgearCheckBox = new CheckBox("Headgear"){
			{a = Utils.getprefb("autoLootHeadgear", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootHeadgear", val);
			}
		}, prev.pos("bl").adds(0, 4).x(12));
		leftColumn = panel.add(OptWnd.autoLootNecklaceCheckBox = new CheckBox("Necklace"){
			{a = Utils.getprefb("autoLootNecklace", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootNecklace", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.autoLootShouldersCheckBox = new CheckBox("Shoulders"){
			{a = Utils.getprefb("autoLootShoulders", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootShoulders", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.autoLootShirtCheckBox = new CheckBox("Shirt"){
			{a = Utils.getprefb("autoLootShirt", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootShirt", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.autoLootGlovesCheckBox = new CheckBox("Gloves"){
			{a = Utils.getprefb("autoLootGloves", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootGloves", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.autoLootWeaponCheckBox = new CheckBox("Weapon"){
			{a = Utils.getprefb("autoLootWeapon", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootWeapon", val);
				OptWnd.autoLootWeaponCheckBox2.set(val);
			}
		}, leftColumn.pos("bl").adds(0, 2)).settip("Try to move it to Belt first, then Inventory if Belt is full.");
		leftColumn = panel.add(OptWnd.autoLootRingsCheckBox = new CheckBox("Rings"){
			{a = Utils.getprefb("autoLootRings", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootRings", val);
				OptWnd.autoLootRingsCheckBox2.set(val);
			}
		}, leftColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
		leftColumn = panel.add(OptWnd.autoLootCloakRobeCheckBox = new CheckBox("Cloak/Robe"){
			{a = Utils.getprefb("autoLootCloakRobe", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootCloakRobe", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.autoLootPouchesCheckBox = new CheckBox("Pouches"){
			{a = Utils.getprefb("autoLootCloakRobe", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootCloakRobe", val);
				OptWnd.autoLootPouchesCheckBox2.set(val);
			}
		}, leftColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
		leftColumn = panel.add(OptWnd.autoLootPantsCheckBox = new CheckBox("Pants"){
			{a = Utils.getprefb("autoLootPants", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootPants", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = panel.add(OptWnd.autoLootCapeCheckBox = new CheckBox("Cape"){
			{a = Utils.getprefb("autoLootCape", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootCape", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		rightColumn = panel.add(OptWnd.autoLootMaskCheckBox = new CheckBox("Mask"){
			{a = Utils.getprefb("autoLootMask", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootMask", val);
			}
		}, prev.pos("bl").adds(0, 4).x(200));
		rightColumn = panel.add(OptWnd.autoLootEyewearCheckBox = new CheckBox("Eyewear"){
			{a = Utils.getprefb("autoLootEyewear", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootEyewear", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.autoLootMouthwearCheckBox = new CheckBox("Mouthwear"){
			{a = Utils.getprefb("autoLootMouthwear", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootMouthwear", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.autoLootChestArmorCheckBox = new CheckBox("Chest Armor"){
			{a = Utils.getprefb("autoLootChestArmor", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootChestArmor", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(new Label("Belt (nope)"), rightColumn.pos("bl").adds(19, 2)).settip("Belts can't be placed in your inventory if they got stuff in them.");
		rightColumn = panel.add(OptWnd.autoLootWeaponCheckBox2 = new CheckBox("Weapon"){
			{a = Utils.getprefb("autoLootWeapon", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootWeapon", val);
				OptWnd.autoLootWeaponCheckBox.set(val);
			}
		}, rightColumn.pos("bl").adds(-19, 2)).settip("Try to move it to Belt first, then Inventory if Belt is full.");
		rightColumn = panel.add(OptWnd.autoLootRingsCheckBox2 = new CheckBox("Rings"){
			{a = Utils.getprefb("autoLootRings", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootRings", val);
				OptWnd.autoLootRingsCheckBox.set(val);
			}
		}, rightColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
		rightColumn = panel.add(OptWnd.autoLootBackpackCheckBox = new CheckBox("Backpack"){
			{a = Utils.getprefb("autoLootBackpack", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootBackpack", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.autoLootPouchesCheckBox2 = new CheckBox("Pouches"){
			{a = Utils.getprefb("autoLootCloakRobe", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootCloakRobe", val);
				OptWnd.autoLootPouchesCheckBox.set(val);
			}
		}, rightColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
		rightColumn = panel.add(OptWnd.autoLootLegArmorCheckBox = new CheckBox("Leg Armor"){
			{a = Utils.getprefb("autoLootLegArmor", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootLegArmor", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		rightColumn = panel.add(OptWnd.autoLootShoesCheckBox = new CheckBox("Shoes"){
			{a = Utils.getprefb("autoLootShoes", false);}
			public void changed(boolean val) {
				Utils.setprefb("autoLootShoes", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));


		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), rightColumn.pos("bl").adds(0, 18).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
