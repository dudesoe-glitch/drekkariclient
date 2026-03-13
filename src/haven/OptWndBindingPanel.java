package haven;

import java.awt.*;

/**
 * Extracted Keybindings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.BindingPanel.
 */
class OptWndBindingPanel {

	private static int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
		return(cont.addhl(new Coord(0, y), cont.sz.x,
				new Label(nm), new OptWnd.SetButton(UI.scale(140), cmd))
				+ UI.scale(2));
	}

	private static int addbtnImproved(Widget cont, String nm, String tooltip, Color color, KeyBinding cmd, int y) {
		Label theLabel = new Label(nm);
		if (tooltip != null && !tooltip.equals(""))
			theLabel.tooltip = RichText.render(tooltip, UI.scale(300));
		theLabel.setcolor(color);
		return (cont.addhl(new Coord(0, y), cont.sz.x,
				theLabel, new OptWnd.SetButton(UI.scale(140), cmd))
				+ UI.scale(2));
	}

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		int y = 5;
		Label topNote = new Label("Don't use the same keys on multiple Keybinds!");
		topNote.setcolor(Color.RED);
		y = panel.adda(topNote, UI.scale(155), y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = panel.adda(new Label("If you do that, only one of them will work. God knows which."), 310 / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		Scrollport scroll = panel.add(new Scrollport(UI.scale(new Coord(310, 360))), 0, 60);
		Widget cont = scroll.cont;
		Widget prev;
		y = 0;
		y = cont.adda(new Label("Main menu"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Inventory", GameUI.kb_inv, y);
		y = addbtn(cont, "Equipment", GameUI.kb_equ, y);
		y = addbtn(cont, "Belt", GameUI.kb_blt, y);
		y = addbtn(cont, "Character sheet", GameUI.kb_chr, y);
		y = addbtn(cont, "Map window", GameUI.kb_map, y);
		y = addbtn(cont, "Kith & Kin", GameUI.kb_bud, y);
		y = addbtn(cont, "Options", GameUI.kb_opt, y);
		y = addbtn(cont, "Search actions", GameUI.kb_srch, y);
		y = addbtn(cont, "Focus chat window", GameUI.kb_chat, y);
//		y = addbtn(cont, "Quick chat", ChatUI.kb_quick, y);
//		y = addbtn(cont, "Take screenshot", GameUI.kb_shoot, y);
		y = addbtn(cont, "Minimap icons", GameUI.kb_ico, y);
		y = addbtn(cont, "Toggle UI", GameUI.kb_hide, y);
		y = addbtn(cont, "Toggle Mini-Study Window", GameUI.kb_miniStudy, y);
		y = addbtn(cont, "Log out", GameUI.kb_logout, y);
		y = addbtn(cont, "Switch character", GameUI.kb_switchchr, y);

		y = cont.adda(new Label("Map buttons"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Reset view", MapWnd.kb_home, y);
		y = addbtn(cont, "Compact mode", MapWnd.kb_compact, y);
		y = addbtn(cont, "Hide markers", MapWnd.kb_hmark, y);
		y = addbtn(cont, "Add marker", MapWnd.kb_mark, y);

		y = cont.adda(new Label("Game World Toggles"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Display Personal Claims", GameUI.kb_claim, y);
		y = addbtn(cont, "Display Village Claims", GameUI.kb_vil, y);
		y = addbtn(cont, "Display Realm Provinces", GameUI.kb_rlm, y);
		y = addbtn(cont, "Display Tile Grid", MapView.kb_grid, y);

		y = cont.adda(new Label("Camera control"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Snap North", MapView.kb_camSnapNorth, y);
		y = addbtn(cont, "Snap South", MapView.kb_camSnapSouth, y);
		y = addbtn(cont, "Snap East", MapView.kb_camSnapEast, y);
		y = addbtn(cont, "Snap West", MapView.kb_camSnapWest, y);


		y = cont.adda(new Label("Walking speed"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Increase speed", Speedget.kb_speedup, y);
		y = addbtn(cont, "Decrease speed", Speedget.kb_speeddn, y);
		for(int i = 0; i < 4; i++)
			y = addbtn(cont, String.format("Set speed %d", i + 1), Speedget.kb_speeds[i], y);

		y = cont.adda(new Label("Combat actions"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		for(int i = 0; i < Fightsess.kb_acts.length; i++)
			y = addbtn(cont, String.format("Combat action %d", i + 1), Fightsess.kb_acts[i], y);
		y = addbtnImproved(cont, "Cycle through targets", "This only cycles through the targets you are currently engaged in combat with.", Color.WHITE, Fightsess.kb_relcycle, y);
		y = addbtnImproved(cont, "Switch to nearest target", "This only switches to the nearest target you are currently engaged in combat with.", Color.WHITE, GameUI.kb_nearestTarget, y);
		y = addbtnImproved(cont, "Switch to leader marked target", "Switches to the target marked by your party leader (the red crosshair)." +
				"\n\n$col[185,185,185]{Only works if your party leader has marked a target before.}", Color.WHITE, GameUI.kb_leaderTarget, y);
		y = addbtnImproved(cont, "Aggro Nearest Player/Animal", "Selects the nearest non-friendly Player or Animal to attack, based on your situation:" +
				"\n\n$col[218,163,0]{Case 1:} $col[185,185,185]{If you are in combat with Players, it will only attack other not-already-aggroed non-friendly players.}" +
				"\n$col[218,163,0]{Case 2:} $col[185,185,185]{If you are in combat with Animals, it will try to attack the closest not-already-aggroed player. If none is found, try to attack the closest animal. Once this happens, you're back to Case 1.}" +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroNearestTargetButton, y);
		y = addbtnImproved(cont, "Aggro/Target Nearest Cursor", "Tries to attack/target the closest player/animal it can find near the cursor." +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroOrTargetNearestCursor, y);
		y = addbtnImproved(cont, "Aggro Nearest Player", "Selects the nearest non-aggroed Player to attack." +
				"\n\n$col[185,185,185]{This only attacks players.}" +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroNearestPlayerButton, y);
		y = addbtnImproved(cont, "Aggro all Non-Friendly Players", "Tries to attack everyone in range. " +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroAllNonFriendlyPlayers, y);
		y = addbtnImproved(cont, "Push Nearest Player", "Pushes the nearest non-friendly player within range." +
				"\n\n$col[185,185,185]{Party members will never be pushed by this button. Range: 20 tiles}", new Color(255, 165, 0,255), GameUI.kb_pushPlayerButton, y);
		y = addbtnImproved(cont, "Peace Current Target", "", new Color(0, 255, 34,255), GameUI.kb_peaceCurrentTarget, y);

		y = cont.adda(new Label("Other Custom features"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtnImproved(cont, "Drink Button", "", new Color(0, 140, 255, 255), GameUI.kb_drinkButton, y+6);
		y = addbtn(cont, "Left Hand (Quick-Switch)", GameUI.kb_leftQuickSlotButton, y);
		y = addbtn(cont, "Right Hand (Quick-Switch)", GameUI.kb_rightQuickSlotButton, y);
		y = addbtnImproved(cont, "Night Vision / Brighter World", "This will simulate daytime lighting during the night. \n$col[185,185,185]{It slightly affects the light levels during the day too.}" +
				"\n\n$col[218,163,0]{Note:} $col[185,185,185]{This keybind just switches the value of Night Vision / Brighter World between minimum and maximum. This can also be set more precisely using the slider in the World Graphics Settings.}", Color.WHITE, GameUI.kb_nightVision, y);

		y+=UI.scale(12);
		y = addbtn(cont, "Inventory search", GameUI.kb_searchInventoriesButton, y);
		y = addbtn(cont, "Object search", GameUI.kb_searchObjectsButton, y);

		y+=UI.scale(20);
		y = addbtnImproved(cont, "Click Nearest Object (Cursor)","When this button is pressed, you will instantly click the nearest object to your cursor, selected from below." +
				"\n$col[218,163,0]{Range:} $col[185,185,185]{12 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_clickNearestCursorObject, y);
		y = addbtnImproved(cont, "Click Nearest Object (You)","When this button is pressed, you will instantly click the nearest object to you, selected from below." +
				"\n$col[218,163,0]{Range:} $col[185,185,185]{12 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_clickNearestObject, y);
		Widget objectsLeft, objectsRight;
		y = cont.adda(objectsLeft = new Label("Objects to Click:"), UI.scale(20), y + UI.scale(2), 0, 0.0).pos("bl").adds(0, 5).y;
		objectsLeft = cont.add(new CheckBox("Forageables"){{a = Utils.getprefb("clickNearestObject_Forageables", true);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Forageables", val);}}, objectsLeft.pos("ur").adds(4, 0)).settip("Pick the nearest Forageable.");
		objectsRight = cont.add(new CheckBox("Critters"){{a = Utils.getprefb("clickNearestObject_Critters", true);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Critters", val);}}, objectsLeft.pos("ur").adds(50, 0)).settip("Chase the nearest Critter.");
		objectsLeft = cont.add(new CheckBox("Non-Visitor Gates"){{a = Utils.getprefb("clickNearestObject_NonVisitorGates", true);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_NonVisitorGates", val);}}, objectsLeft.pos("bl").adds(0, 4)).settip("Open/Close the nearest Non-Visitor Gate.");
		objectsRight = cont.add(new CheckBox("Caves"){{a = Utils.getprefb("clickNearestObject_Caves", false);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Caves", val);}}, objectsRight.pos("bl").adds(0, 4)).settip("Go through the nearest Cave Entrance/Exit.");
		objectsLeft = cont.add(new CheckBox("Mineholes & Ladders"){{a = Utils.getprefb("clickNearestObject_MineholesAndLadders", false);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_MineholesAndLadders", val);}}, objectsLeft.pos("bl").adds(0, 4)).settip("Hop down the nearest Minehole, or Climb up the nearest Ladder.");
		objectsRight = cont.add(new CheckBox("Doors"){{a = Utils.getprefb("clickNearestObject_Doors", false);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Doors", val);}}, objectsRight.pos("bl").adds(0, 4)).settip("Go through the nearest Door.");
		y+=UI.scale(60);
		y = addbtnImproved(cont, "Hop on Nearest Vehicle", "When this button is pressed, your character will run towards the nearest mountable Vehicle/Animal, and try to mount it." +
				"\n\n$col[185,185,185]{If the closest vehicle to you is full, or unmountable (like a rowboat on land), it will keep looking for the next closest mountable vehicle.}" +
				"\n\n$col[218,163,0]{Works with:} Knarr, Snekkja, Rowboat, Dugout, Kicksled, Coracle, Wagon, Wilderness Skis, Tamed Horse" +
				"\n\n$col[218,163,0]{Range:} $col[185,185,185]{36 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_enterNearestVehicle, y);
		y+=UI.scale(20);
		y = addbtnImproved(cont, "Lift Nearest into Wagon/Cart", "When pressed the nearest supported liftable object will be stored in the nearest Wagon/Cart" +
				"\n\n$col[185,185,185]{If you are riding a Wagon it will try to exit the wagon, store the object and enter the wagon again.}", new Color(255, 191, 0,255), GameUI.kb_wagonNearestLiftable, y);
		Widget objectsLiftActionLeft, objectsLiftActionRight;
		y = cont.adda(objectsLiftActionLeft = new Label("Objects to Lift:"), UI.scale(20), y + UI.scale(2), 0, 0.0).pos("bl").adds(0, 5).y;
		objectsLiftActionLeft = cont.add(new CheckBox("Dead Animals"){{a = Utils.getprefb("wagonNearestLiftable_animalcarcass", true);}
			public void changed(boolean val) {Utils.setprefb("wagonNearestLiftable_animalcarcass", val);}}, objectsLiftActionLeft.pos("ur").adds(39, 0)).settip("Lift the nearest animal carcass into Wagon/Cart.");
		objectsLiftActionRight = cont.add(new CheckBox("Containers"){{a = Utils.getprefb("wagonNearestLiftable_container", true);}
			public void changed(boolean val) {Utils.setprefb("wagonNearestLiftable_container", val);}}, objectsLiftActionLeft.pos("ur").adds(4, 0)).settip("Lift the nearest storage container into Wagon/Cart.");
		objectsLiftActionLeft = cont.add(new CheckBox("Tree Logs"){{a = Utils.getprefb("wagonNearestLiftable_log", true);}
			public void changed(boolean val) {Utils.setprefb("wagonNearestLiftable_log", val);}}, objectsLiftActionLeft.pos("bl").adds(0, 4)).settip("Lift nearest log into Wagon/Cart.");

		y+=UI.scale(40);
		y = addbtnImproved(cont, "Combat Cheese Auto-Distance", "", new Color(0, 255, 34,255), GameUI.kb_autoCombatDistance, y);
		y = addbtnImproved(cont, "Toggle Auto-Reaggro Target", "Use this to cheese animals and instantly re-aggro them when they flee.", new Color(0, 255, 34,255), GameUI.kb_autoReaggroTarget, y);
		y+=UI.scale(20);
		y = addbtn(cont, "Toggle Collision Boxes", GameUI.kb_toggleCollisionBoxes, y);
		y = addbtn(cont, "Toggle Object Hiding", GameUI.kb_toggleHidingBoxes, y);
		y = addbtn(cont, "Display Growth Info on Plants", GameUI.kb_toggleGrowthInfo, y);
		y = addbtn(cont, "Show Tree/Bush Harvest Icons", GameUI.kb_toggleHarvestIcons, y);
		y = addbtn(cont, "Show Low Food/Water Icons", GameUI.kb_toggleLowFoodWaterIcons, y);
		y = addbtn(cont, "Show Bee Skep Harvest Icons", GameUI.kb_toggleBeeSkepIcons, y);
		y = addbtn(cont, "Show Barrel Contents Text", GameUI.kb_toggleBarrelContentsText, y);
		y = addbtn(cont, "Show Icon Sign Text", GameUI.kb_toggleIconSignText, y);
		y = addbtn(cont, "Show Cheese Racks Tier Text", GameUI.kb_toggleCheeseRacksTierText, y);
		y = addbtn(cont, "Show Objects Speed", GameUI.kb_toggleSpeedInfo, y);
		y = addbtn(cont, "Hide/Show Cursor Item", GameUI.kb_toggleCursorItem, y);
		y+=UI.scale(20);
		y = addbtn(cont, "Loot Nearest Knocked Player", GameUI.kb_lootNearestKnockedPlayer, y);
		y+=UI.scale(20);
		y = addbtn(cont, "Instant Log Out", GameUI.kb_instantLogout, y);

		y = cont.adda(new Label("Equipment Quick-Swap (from Belt)"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtnImproved(cont, "B12 Axe", "Equip B12 Axe from belt (two-hander)", new Color(255, 100, 100, 255), GameUI.kb_equipB12, y);
		y = addbtnImproved(cont, "Cutblade", "Equip Cutblade from belt (two-hander)", new Color(255, 100, 100, 255), GameUI.kb_equipCutblade, y);
		y = addbtnImproved(cont, "Boar Spear", "Equip Boar Spear from belt (two-hander)", new Color(255, 100, 100, 255), GameUI.kb_equipBoarSpear, y);
		y = addbtnImproved(cont, "Giant Needle", "Equip Giant Needle from belt (two-hander)", new Color(255, 100, 100, 255), GameUI.kb_equipGiantNeedle, y);
		y = addbtnImproved(cont, "Hirdsman's Sword + Shield", "Equip Hirdsman's Sword and Wooden Shield from belt", new Color(255, 100, 100, 255), GameUI.kb_equipHirdsmanSword, y);
		y = addbtnImproved(cont, "Bronze Sword + Shield", "Equip Bronze Sword and Wooden Shield from belt", new Color(255, 100, 100, 255), GameUI.kb_equipBronzeSword, y);
		y = addbtnImproved(cont, "Fyrdsman's Sword + Shield", "Equip Fyrdsman's Sword and Wooden Shield from belt", new Color(255, 100, 100, 255), GameUI.kb_equipFyrdsmanSword, y);
		y = addbtnImproved(cont, "Hunter's Bow", "Equip Hunter's Bow from belt (two-hander)", new Color(255, 100, 100, 255), GameUI.kb_equipHuntersBow, y);
		y = addbtnImproved(cont, "Ranger's Bow", "Equip Ranger's Bow from belt (two-hander)", new Color(255, 100, 100, 255), GameUI.kb_equipRangersBow, y);
		y += UI.scale(5);
		y = addbtnImproved(cont, "Pickaxe", "Equip Pickaxe from belt (two-hander)", new Color(100, 200, 255, 255), GameUI.kb_equipPickaxe, y);
		y = addbtnImproved(cont, "Sledgehammer", "Equip Sledgehammer from belt (two-hander)", new Color(100, 200, 255, 255), GameUI.kb_equipSledgehammer, y);
		y = addbtnImproved(cont, "Scythe", "Equip Scythe from belt (two-hander)", new Color(100, 200, 255, 255), GameUI.kb_equipScythe, y);
		y = addbtnImproved(cont, "Metal Shovel", "Equip Metal Shovel from belt (two-hander)", new Color(100, 200, 255, 255), GameUI.kb_equipMetalShovel, y);
		y = addbtnImproved(cont, "Tinker's Shovel", "Equip Tinker's Shovel from belt (two-hander)", new Color(100, 200, 255, 255), GameUI.kb_equipTinkersShovel, y);
		y = addbtnImproved(cont, "Wooden Shovel", "Equip Wooden Shovel from belt (two-hander)", new Color(100, 200, 255, 255), GameUI.kb_equipWoodenShovel, y);
		y += UI.scale(5);
		y = addbtnImproved(cont, "Traveller's Sacks", "Equip Traveller's Sacks from belt (both hands)", new Color(100, 255, 100, 255), GameUI.kb_equipTravelersSacks, y);
		y = addbtnImproved(cont, "Wanderer's Bindles", "Equip Wanderer's Bindles from belt (both hands)", new Color(100, 255, 100, 255), GameUI.kb_equipWanderersBindles, y);

		y = cont.adda(new Label("Quick Actions"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtnImproved(cont, "Toggle Nearest Gate", "Open/close the nearest gate within 3 tiles", new Color(255, 200, 100, 255), GameUI.kb_toggleGate, y);
		y = addbtnImproved(cont, "Pickup Nearest Item", "Pick up the nearest ground item within 2 tiles", new Color(255, 200, 100, 255), GameUI.kb_pickupNearest, y);
		y = addbtnImproved(cont, "Close All Containers", "Close all open container windows at once", new Color(255, 200, 100, 255), GameUI.kb_closeAllContainers, y);

		prev = panel.adda(new OptWnd.PointBind(UI.scale(200)), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
		prev = panel.adda(optWnd.new PButton(UI.scale(200), "Back", 27, back, "Options            "), prev.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
		panel.pack();
	}
}
