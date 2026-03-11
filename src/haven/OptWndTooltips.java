package haven;

/**
 * Extracted tooltip definitions for OptWnd settings panels.
 * Previously inlined as ~380 lines at the bottom of OptWnd.java.
 */
class OptWndTooltips {

	// Interface Settings Tooltips
	static final Object interfaceScale = RichText.render("$col[218,163,0]{Warning:} This setting is by no means perfect, and it can mess up many UI related things." +
			"\nSome windows might just break when this is set above 1.00x." +
			"\n" +
			"\n$col[185,185,185]{I really try my best to support this setting, but I can't guarantee everything will work." +
			"\nUnless you're on a 4K or 8K display, I'd keep this at 1.00x.}", UI.scale(300));
	static final Object uiTheme = RichText.render("This sets the overall theme for the User Interface." +
			"\n" +
			"\nAdditionally, you can add files to the \"Custom Theme\" folder, to create your own theme (which won't get erased when you update the client). " +
			"\nThe Custom Theme folder can be found in your $col[218,163,0]{client folder}, under $col[218,163,0]{\\ res \\ customclient \\ uiThemes \\ Custom Theme}" +
			"\n" +
			"\n$col[185,185,185]{You don't need to change *everything* for the Custom Theme to work. If it's missing something, it just defaults to whatever the \"Nightdawg Dark\" theme uses.}", UI.scale(300));
	static final Object extendedMouseoverInfo = RichText.render("Holding Ctrl+Shift shows the Resource Path when mousing over Objects or Tiles. " +
			"\nThis setting will add a lot of additional information on top of that." +
			"\n" +
			"\n$col[185,185,185]{Unless you're a client dev, you don't really need to enable this setting, like ever.}", UI.scale(300));
	static final Object disableMenuGridHotkeys = RichText.render("This completely disables the hotkeys for the action buttons & categories in the bottom right corner menu (aka the menu grid)." +
			"\n" +
			"\n$col[185,185,185]{Your action bar keybinds are NOT affected by this setting.}", UI.scale(300));
	static final Object alwaysOpenBeltOnLogin = RichText.render("This will cause your belt window to always open when you log in." +
			"\n" +
			"\n$col[185,185,185]{By default, Loftar saves the status of the belt at logout. So if you don't enable this setting, but leave the belt window open when you log out/exit the game, it will still open on login.}", UI.scale(300));
	static final Object showMapMarkerNames = RichText.render("$col[185,185,185]{The marker names are NOT visible in compact mode.}", UI.scale(320));
	static final Object verticalContainerIndicators = RichText.render("Orientation for inventory container indicators." +
			"\n" +
			"\n$col[185,185,185]{For example, the amount of water in waterskins, seeds in a bucket, etc.}", UI.scale(230));
	static final Object experienceWindowLocation = RichText.render("Select where you want the experience event pop-up window to show up." +
			"\n" +
			"\n$col[185,185,185]{The default client pops it up in the middle of your screen, which can be annoying.}", UI.scale(300));
	static final Object showFramerate = RichText.render("Shows the current FPS in the top-right corner of the game window.", UI.scale(300));
	static final Object snapWindowsBackInside = RichText.render("This will cause most windows, that are not too large, to be fully snapped back into your game's window." +
			"\nBy default, when you try to drag a window outside of your game window, it will only pop 25% of it back in." +
			"\n" +
			"\n$col[185,185,185]{Very large windows are not affected by this setting. Only the 25% rule applies to them." +
			"\nThe map window is always fully snapped back.}", UI.scale(300));
	static final Object dragWindowsInWhenResizing = RichText.render("This will force ALL windows to be dragged back inside the game window, whenever you resize it." +
			"\n" +
			"\n$col[185,185,185]{Without this setting, windows remain in the same spot when you resize your game window, even if they end up outside of it. They will only come back if closed and reopened (for example, via keybinds)", UI.scale(300));
	static final Object showQuickSlots = RichText.render("Just a small interactable widget that can show your hands, pouches, belt, backpack and cape slots, depending on what you select." +
			"\nYou can use this so you don't have to open your equipment window all the time." +
			"\n" +
			"\nThis window can be dragged using the middle mouse button (Scroll Click)." +
			"\n" +
			"\n$col[185,185,185]{Your quick-switch keybinds ('Right Hand' and 'Left Hand') are NOT affected by this setting.}", UI.scale(300));
	static final Object showStudyReportHistory = RichText.render("Shows what curiosity was formerly placed in each slot. " +
			"\nThe history is saved separately for every character, on every account." +
			"\n" +
			"\n$col[185,185,185]{It doesn't work with Gems. Don't ask me why.}", UI.scale(300));
	static final Object lockStudyReport = RichText.render("This will prevent grabbing or dropping items from the Study Report", UI.scale(300));
	static final Object soundAlertForFinishedCuriosities = RichText.render("A violin sound will be played every time a curiosity is finished." +
			"\n" +
			"\n$col[218,163,0]{Preview:}$col[185,185,185]{Enable this to hear the sound!", UI.scale(300));
	static final Object alwaysShowCombatUiBar = RichText.render("For more options for this bar, check the Combat Settings.", UI.scale(320));
	static final Object transparentQuestsObjectivesWindow = RichText.render("This makes the Quest Objectives window background transparent, like in the default client." +
			"\n" +
			"\n$col[185,185,185]{You can still drag the window around, regardless.", UI.scale(300));
	static final Object stackWindowsWhenOpened = RichText.render("This makes windows that have the same title open on top of each other." +
			"\n" +
			"\n$col[185,185,185]{This also saves their positions to wherever you dragged/closed them.}", UI.scale(320));

	// Combat Settings Tooltips
	static final Object singleRowCombatMoves = RichText.render("This makes the Bottom Panel show the combat moves in one row, rather than two.", UI.scale(300));
	static final Object showDamagePredictUI = RichText.render("This makes the Combat Moves that can deal damage show how much damage they can potentially do, when used." +
			"\n" +
			"\nThis is calculated depending on the following:" +
			"\n$col[185,185,185]{- How high your current target's $col[218,163,0]{Openings} are (depending on the openings the combat move applies to)" +
			"\n- How much total $col[218,163,0]{Strength} your character has" +
			"\n- How much $col[218,163,0]{Damage} your currently equipped $col[218,163,0]{Weapon} has (if the move uses the weapon)}", UI.scale(320));
	static final Object damageInfoClear = RichText.render("Clears all damage info." +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object onlyShowOpeningsAbovePercentageCombatInfo = RichText.render("Only show the combat info openings if at least one of them is above the set number. If one of them is above that, show all of them." +
			"\n" +
			"\nThis does NOT apply to your current target, only other combat foes.}", UI.scale(320));
	static final Object showCombatOpeningsAsLetters = RichText.render("This will change the openings from full squares into colored letters. For example, the red square will become a colored R, blue will become B, etc." +
			"\n" +
			"\n$col[185,185,185]{The color settings from below still apply to the letters.}" +
			"\n$col[185,185,185]{I only added this as an extra aid for colorblind people, but I doubt anybody will use it...}", UI.scale(300));
	static final Object highlightPartyMembers = RichText.render("This will put a color highlight over all party members." +
			"\n" +
			"\n$col[185,185,185]{If you are the party leader, your color highlight will always be the $col[218,163,0]{Leader's Color}, regardless of what you set $col[218,163,0]{Your Color} to.}", UI.scale(310));
	static final Object showCirclesUnderPartyMembers = RichText.render("This will put a colored circle under all party members." +
			"\n" +
			"\n$col[185,185,185]{If you are the party leader, your circle's color will always be the $col[218,163,0]{Leader's Color}, regardless of what you set $col[218,163,0]{Your Color} to.}", UI.scale(300));
	static final Object highlightCombatFoes = RichText.render("This will put a color highlight over all enemies that you are currently in combat with.", UI.scale(310));
	static final Object showCirclesUnderCombatFoes = RichText.render("This will put a colored circle under all enemies that you are currently in combat with.", UI.scale(300));
	static final Object targetSprite = RichText.render("The target sprite uses the same color you set for Combat Foes.", UI.scale(300));
	static final Object drawChaseVectors = RichText.render("If this setting is enabled, colored lines will be drawn between chasers and chased targets." +
			"\n" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{Chase vectors include queuing attacks, clicking a critter to pick up, or simply following someone.}" +
			"\n$col[218,163,0]{Disclaimer:} $col[185,185,185]{Chase vectors sometimes don't show when chasing a critter that is standing still. The client treats this as something else for some reason and I can't fix it.}", UI.scale(430));
	static final Object drawYourCurrentPath = RichText.render("When this is enabled, a straight line will be drawn between your character and wherever you clicked" +
			"\n" +
			"\n$col[185,185,185]{You can use this to make sure you won't run into a tree or something, I guess.}", UI.scale(300));
	static final Object showYourCombatRangeCircles = RichText.render("This will display two circles under your character, that show your unarmed range, and currently equipped weapon range (if you have a weapon equipped)." +
			"\n" +
			"\n$col[185,185,185]{The circles only show up when you're on foot.}", UI.scale(300));
	static final Object improvedInstrumentMusicWindow = RichText.render("The improved window changes the layout of the keys, and adds an automatic player you can use to play notes from midi files." +
			"\nYou have to re-open the instrument music window after changing this setting." +
			"\n" +
			"\n$col[185,185,185]{If you want to use Midi2Haven to play notes with a midi controller, you must disable this improvement and use the default window layout.}", UI.scale(300));

	// Display Settings Tooltips
	static final Object granularityPosition = RichText.render("This works like the :placegrid console command. " +
			"\nIt allows you to have more freedom when placing constructions/objects.", UI.scale(300));
	static final Object granularityAngle = RichText.render("This works like the :placeangle console command. " +
			"\nIt allows you to have more freedom when rotating constructions/objects before placement.", UI.scale(300));
	static final Object displayGrowthInfo = RichText.render("This will show the following growth information:" +
			"\n" +
			"\n> Trees and Bushes will display their growth percentage (below 100%) and extra size percentage, if you enable the \"Also Show Trees Above %\" setting." +
			"\n$col[185,185,185]{If a Tree or Bush is not showing a percentage below 100%, that means it reached full growth.}" +
			"\n" +
			"\n> Crops will generally display their growth stage as \"Current\", and a red dot when they reached the final stage." +
			"\n$col[185,185,185]{Crops with a seeds stage (carrots, turnips, leeks, etc.) will also display a blue dot during the seeds stage.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(330));
	static final Object highlightCliffs = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object showContainerFullness = RichText.render("Colors containers (Cupboards, Chests, Crates, etc.), depending on how much stuff is in them." +
			"\n" +
			"\n$col[185,185,185]{Select from below what states you want to be highlighted, and what colors you want each of them to show.}", UI.scale(330));
	static final Object showWorkstationProgress = RichText.render("Colors workstations (Drying Racks, Tanning Tubs, Cheese Racks, Flower Pots), depending on their current progress." +
			"\n" +
			"\n$col[185,185,185]{Select from below what states you want to be highlighted, and what colors you want each of them to show.}", UI.scale(330));
	static final Object showObjectCollisionBoxes = RichText.render("This shows the collision boundaries of objects in the world by outlining each edge with a line." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object displayObjectDurabilityPercentage = RichText.render("This makes objects that took decay hits show a percentage number.", UI.scale(300));
	static final Object showDurabilityCrackTexture = RichText.render("This makes objects that took decay hits have a cracked texture.", UI.scale(300));
	static final Object displayObjectQualityOnInspection = RichText.render("This makes objects that have been inspected show their quality number on top, until unload them.", UI.scale(300));
	static final Object showCritterAuras = RichText.render("This will draw clickable circles under all critters, which makes it easier to spot them, and right-click to chase them." +
			"\n" +
			"\n$col[185,185,185]{This can be very nice during combat, due to the $col[218,163,0]{Speed Boost provided by the Forager Credo} when chasing critters. " +
			"\nIt can also just make your life easier when foraging in general.}", UI.scale(300));
	static final Object showSpeedBuffAuras = RichText.render("This will draw a circle under speed buffs that spawn in the world, to make it easier to spot them." +
			"\n" +
			"\n$col[185,185,185]{This circle is not clickable, but it shows you where exactly the speed buff is on the ground plane.}" , UI.scale(300));
	static final Object showMidgesCircleAuras = RichText.render("This will draw a red circle under midges, to make it easier to spot and avoid them.", UI.scale(300));
	static final Object showDangerousBeastRadii = RichText.render("This will draw a large red radius around dangerous animals to make it easier to spot them." +
			"\n" +
			"\n$col[200,0,0]{WARNING: This doesn't show you how close you can get to it!}" +
			"\n" +
			"\n$col[185,185,185]{If you don't know how dangerous an animal is and how close you can get to it, just stay as far as possible.}", UI.scale(300));
	static final Object showBeeSkepsRadii = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object showFoodThroughsRadii = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object showMoundBedsRadii = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object objectPermanentHighlighting = RichText.render("This allows you to set a purple highlight on objects that persists even if you reload the objects, switch character, log out, etc." +
			"\n" +
			"\n$col[185,185,185]{For example, you can use this to keep track of which cows you milked already, or whatever.}" +
			"\n" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{Disabling this setting will also reset the current list of highlighted objects.}", UI.scale(300));
	static final Object showBarrelContentsText = RichText.render("This adds text on top of barrels, to make it easier to determine what's inside of them. Empty barrels won't show any text." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object showIconSignText = RichText.render("This adds text on top of icon signs, that shows the name of the currently displayed icon. Empty signs won't show any text." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object showCheeseRacksTierText = RichText.render("This adds text on top of each cheese tray inside cheese racks, that shows the current tier of the cheese present in the tray." +
			"\n" +
			"\n$col[185,185,185]{Unfortunately, the server only sends the tier info, so the client can't tell which exact cheese is in the trays.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object showMineSupportCoverage = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object enableMineSweeper = RichText.render("This will cause cave dust tiles to show the number of potential cave-ins surrounding them, just like in Minesweeper." +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{If a cave-in has been mined out, the tiles surrounding it will still drop cave dust, and they will still show a number on the ground. The cave dust tiles are pre-generated with the world. That's just how Loftar coded it.}" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{You can still pick up the cave dust item off the ground. The numbers are affected only by the duration of the falling dust particles effect (aka dust rain), which can be set below}" +
			"\n" +
			"\n$col[200,0,0]{NOTE:} $col[185,185,185]{There's a bug with the falling dust particles, that we can't really \"fix\". If you mine them out on a level, the same particles can also show up on different levels or the overworld. If you want them to vanish, you can just relog, but they will despawn from their original location too.}" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object showObjectsSpeed = RichText.render("This will show the speed of moving objects (Players, Mobs, Vehicles, etc.) below them." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object showTreesBushesHarvestIcons = RichText.render("This will show the icons of seeds and leaves that can be collected on fully grown trees and bushes." +
			"\n" +
			"\n$col[185,185,185]{It won't show the icons on trees/bushes that are not 100% grown, even if they can already be harvested.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object showLowFoodWaterIcons = RichText.render("This will show warning icons for low food and low water over Chicken Coops and Rabbit Hutches" +
			"\n" +
			"\n$col[185,185,185]{For Chicken Coops, the icons show up when they go below 50% Food/Water. " +
			"\nFor Rabbit Hutches, the icons show up when they go below 33% for Food and below 50% for Water." +
			"\nI can't change this because the game does not differentiate between any other values for them.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object showBeeSkepsHarvestIcons = RichText.render("This will show icons for Wax and Honey when they can be harvested from Bee Skeps." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	static final Object partyChatPingColorOption = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{If you ping players for your party, you will instead set a Party Mark on them.}", UI.scale(300));
	static final Object removeMapTileBorders = RichText.render("$col[200,0,0]{WARNING: This setting might not work with your Web Map Integration!}", UI.scale(300));

	// Quality Display Settings Tooltips
	static final Object customQualityColors = RichText.render("These numbers and colors are completely arbitrary, and you can change them to whatever you like." +
			"\n" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{The quality color for container liquids is not affected by this setting.}", UI.scale(300));

	// Audio Settings Tooltips
	static final Object audioLatency = RichText.render("Sets the size of the audio buffer." +
			"\n" +
			"\n$col[185,185,185]{Loftar claims that smaller sizes are better, but anything below 50ms always seems to stutter, so I limited it to that." +
			"\nIncrease this if your audio is still stuttering.}", UI.scale(300));

	// Gameplay Automation Settings Tooltips
	static final Object autoReloadCuriositiesFromInventory = RichText.render("If enabled, curiosities will be automatically reloaded into the Study Report once they finish being studied." +
			"\nThis picks items only from your Inventory and currently open Cupboards. No other containers." +
			"\n" +
			"\n$col[185,185,185]{It only reloads curiosities that are currently being studied. It can't add new curiosities.}", UI.scale(300));
	static final Object preventTablewareFromBreaking = RichText.render("Prevents eating when the table contains Tableware with only 1 durability left." +
			"\n" +
			"\n$col[185,185,185]{A system warning message will be shown, to let you know which Tableware is at 1 durability.}", UI.scale(300));
	static final Object autoSelect1stFlowerMenu = RichText.render("Holding Ctrl before right clicking an item or object will auto-select the first available option from the flower menu." +
			"\n" +
			"\n$col[185,185,185]{Except for the Head of Lettuce. It will select the 2nd option there, so you split it rather than eat it.}", UI.scale(300));
	static final Object autoRepeatFlowerMenu = RichText.render("This will trigger the Auto-Repeat Flower-Menu Script to run when you Right Click an item while holding Ctrl + Shift." +
			"\n\n$col[185,185,185]{You have} $col[218,163,0]{2 seconds} $col[185,185,185]{to select a Flower Menu option, after which the script will automatically click the selected option for ALL items that have the same name in your inventory.}" +
			"\n$col[200,0,0]{If you don't select an option within} $col[218,163,0]{2 seconds}$col[200,0,0]{, the script won't run.}" +
			"\n\nYou can stop the script before it finishes by pressing ESC." +
			"\n\n$col[218,163,0]{Example:} You have 10 Oak Blocks in your inventory. You hold Ctrl + Shift and right click one of the Oak Blocks and select \"Split\" in the flower menu. The script starts running and it splits all 10 Oak Blocks." +
			"\n\n$col[218,163,0]{Note:} $col[185,185,185]{This script only runs on items that have the same name inside your inventory. It does not take into consideration items of the same \"type\" (for example, if you run the script on Oak Blocks, it won't also run on Spruce Blocks).} ", UI.scale(310));
	static final Object alsoUseContainersWithRepeater = RichText.render("Allow the Auto-Repeat Flower-Menu Script to run through all inventories, such as open cupboards, chests, crates, or any other containers.", UI.scale(300));
	static final Object flowerMenuAutoSelectManager = RichText.render("An advanced menu to automatically select specific flower menu options all the time. New options are added to the list as you discover them." +
			"\n" +
			"\n$col[185,185,185]{I don't recommend using this, but nevertheless it exists due to popular demand.}", UI.scale(300));
	static final Object autoEquipBunnySlippersPlateBoots = RichText.render("Switches your currently equipped shoes to Bunny Slippers when you right click to chase a rabbit, or Plate Boots if you click on anything else." +
			"\n" +
			"\n$col[185,185,185]{I suggest always using this setting in PVP.}", UI.scale(300));
	static final Object autoPeaceAnimalsWhenCombatStarts = RichText.render("This will automatically set your status to 'Peace' when combat is initiated with a new target (animals only). " +
			"\nToggling this on, while in combat, will also autopeace all animals you are currently fighting." +
			"\n\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object preventUsingRawHideWhenRiding = RichText.render("This will prevent you from using Raw Hide while riding a Horse, and only allow using it when you're not mounted.", UI.scale(300));
	static final Object autoDrinking = RichText.render("When your Stamina Bar goes below the set threshold, try to drink Water. If the threshold box is empty, it defaults to 75%." +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object enableQueuedMovement = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(300));
	static final Object walkWithPathfinder = RichText.render("You can use this to walk and avoid possible obstacles, for example, in your base. It's not perfect, and doesn't work with cliffs though." +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(300));

	// Altered Gameplay Settings Tooltips
	static final Object overrideCursorItemWhenHoldingAlt = RichText.render("Holding Alt while having an item on your cursor will allow you to left click to walk, or right click to interact with objects, rather than drop it on the ground." +
			"\n" +
			"\n$col[185,185,185]{Left click ignores the UI when you do this, so don't try to click on the map to walk while holding an item.}" +
			"\n" +
			"\n$col[200,0,0]{SETTING OVERRIDE:} This doesn't work with the \"No Cursor Dropping\" settings, and it will toggle them off when this is enabled.", UI.scale(320));
	static final Object noCursorItemDroppingAnywhere = RichText.render("This will allow you to have an item on your cursor and still be able to left click to walk." +
			"\n" +
			"\n$col[185,185,185]{You can drop the item from your cursor if you hold Alt.}" +
			"\n" +
			"\n$col[200,0,0]{WARNING: If you're holding something on your cursor, you're NOT ABLE to enter Deep Water to Swim. The game prevents you from doing it.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}" +
			"\n" +
			"\n$col[200,0,0]{SETTING OVERRIDE:} This doesn't work with the \"Override Cursor Item\" setting, and it will toggle it off when this is enabled.", UI.scale(320));
	static final Object noCursorItemDroppingInWater = RichText.render("This will allow you to have an item on your cursor and still be able to left click to walk, while you are in water. " +
			"\nIf the previous setting is Enabled, this one will be ignored." +
			"\n" +
			"\n$col[185,185,185]{You can drop the item from your cursor if you hold Alt.}" +
			"\n" +
			"\n$col[200,0,0]{WARNING: If you're holding something on your cursor, you're NOT ABLE to enter Deep Water to Swim. The game prevents you from doing it.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}" +
			"\n" +
			"\n$col[200,0,0]{SETTING OVERRIDE:} This doesn't work with the \"Override Cursor Item\" setting, and it will toggle it off when this is enabled.", UI.scale(320));
	static final Object useOGControlsForBuildingAndPlacing = RichText.render("Hold Ctrl to smoothly place, and Ctrl+Shift to also smoothly rotate. To walk to the place you click (rather than build/place the object) hold Alt." +
			"\n" +
			"\n$col[185,185,185]{Idk why Loftar changed them when he did, but some of you might be used to the new controls rather than the OG ones, so you have the option to disable this.}", UI.scale(320));
	static final Object useImprovedInventoryTransferControls = RichText.render("Alt+Left Click for descending order, and Alt+Right click for ascending order.", UI.scale(320));
	static final Object tileCentering = RichText.render("This forces your left and right clicks in the world to go to the center of the tile you clicked. So you will always walk to the center of the tile, or place items down on the center." +
			"\n" +
			"\n$col[185,185,185]{It doesn't affect the manual precise placement of objects, just the quick right-click one.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));

	// Camera Settings Tooltips
	static final Object reverseOrthoCameraAxes = RichText.render("This will reverse the Horizontal axis when dragging the camera to look around." +
			"\n" +
			"\n$col[185,185,185]{I don't know why Loftar inverts it in the first place...}", UI.scale(280));
	static final Object unlockedOrthoCam = RichText.render("This allows you to rotate the Ortho camera freely, without locking it to only 4 view angles.", UI.scale(280));
	static final Object allowLowerFreeCamTilt = RichText.render("This will allow you to tilt the camera below the character (and under the ground), to look upwards." +
			"\n" +
			"\n$col[200,0,0]{WARNING: Be careful when using this setting, especially in combat! You're NOT able to click on the ground when looking at the world from below.}" +
			"\n" +
			"\n$col[185,185,185]{Honestly just enable this when you need to take a screenshot or something, and keep it disabled the rest of the time. I added this setting for fun.}", UI.scale(300));
	static final Object freeCamHeight = RichText.render("This affects the height of the point at which the free camera is pointed. By default, it is pointed right above the player's head." +
			"\n" +
			"\n$col[185,185,185]{This doesn't really affect gameplay that much, if at all. With this setting, you can make the camera point at the feet, torso, head, slightly above you, or whatever's in between.}", UI.scale(300));

	// World Graphics Settings Tooltips
	static final Object nightVision = RichText.render("Increasing this will simulate daytime lighting during the night." +
			"\n" +
			"\n$col[185,185,185]{It can slightly affect the light levels during the day too, but it is barely noticeable.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This slider can also be switched between minimum and maximum by using the 'Night Vision' keybind.}", UI.scale(300));
	static final Object hideFlavorObjects = RichText.render("This hides the random objects that appear in the world, like random weeds on the ground and whatnot, which you cannot interact with." +
			"\n" +
			"\n$col[185,185,185]{Players usually disable flavor objects to improve visibility, especially in combat.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object flatWorld = RichText.render("This will make the entire game world terrain flat, except for cliffs." +
			"\n" +
			"\n$col[185,185,185]{Cliffs will still be visible, but with their relative height scaled down.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object disableTileSmoothing = RichText.render("This will cause all biome tiles to look the same, with no variations." +
			"\n" +
			"\n$col[185,185,185]{I guess some people think this makes it easier to differentiate between terrain types, or maybe it's just preference.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object disableTileBlending = RichText.render("This will remove the blending of the random texture patches that are being applied to biome tiles." +
			"\n" +
			"\n$col[185,185,185]{It might make it a bit more confusing, because some patches of land might look like completely different biomes, but they're not...}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object disableTileTransitions = RichText.render("This will turn all tiles into squares, so you can determine where one biome ends and another one starts, or where the shoreline meets the water." +
			"\n" +
			"\n$col[185,185,185]{It can be useful in some niche cases I won't bother listing, but it's preference.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	static final Object simplifiedCrops = RichText.render("This reduces each crop tile to a single plant for a cleaner, more minimal look." +
			"\n" +
			"\n$col[185,185,185]{No more random clumps. Just a single sprout in the center of the tile, for each crop type.}", UI.scale(300));
	static final Object simplifiedForageables = RichText.render("This makes forageable spawns appear as a single item per tile instead of a cluster, for simplified visuals." +
			"\n" +
			"\n$col[185,185,185]{I never liked how they look like 10 things in a cluster, but you only pick one up... lol.}", UI.scale(300));
	static final Object flatCaveWalls = RichText.render("This lowers the height of cave walls significantly, and also displays their texture on the ground." +
			"\n" +
			"\n$col[185,185,185]{It makes it easier to see what you're looking at, from all angles.}", UI.scale(300));
	static final Object straightCliffEdges = RichText.render("This disables the variations for cliffs, making it a bit easier to distinguish their angles." +
			"\n" +
			"\n$col[185,185,185]{The difference is very small, honestly.}", UI.scale(300));
	static final Object flatCupboards = RichText.render("This turns cupboards into short boxes that open towards the ceiling." +
			"\n" +
			"\n$col[185,185,185]{Parchements that are placed on the cupboards are also moved to the top.}", UI.scale(300));
	static final Object palisadesAndBrickWallsScale = RichText.render("This changes how tall Palisades and Brick Walls are." +
			"\n" +
			"\n$col[185,185,185]{Only wall sections, NOT gates!}", UI.scale(300));
	static final Object enableSkybox = RichText.render("Adds a skybox to the game world." +
			"\n" +
			"\n$col[185,185,185]{Summon the sky above the hearthlands, and banish the endless void!}", UI.scale(190));
	static final Object disableTreeAndBushSwaying = RichText.render("Trees and bushes will no longer move as if the wind is blowing them around." +
			"\n" +
			"\n$col[185,185,185]{Disabling swaying can improve your framerate.}", UI.scale(300));
	static final Object disableIndustrialSmoke = RichText.render("Completely removes the smoke particles from things like smelters and kilns." +
			"\n" +
			"\n$col[185,185,185]{It *might* improve your framerate around smelters and stuff, but I'm not sure though.}", UI.scale(300));
	static final Object disableScentSmoke = RichText.render("Completely removes the smoke particles from crime scents." +
			"\n" +
			"\n$col[185,185,185]{It can significantly improve your framerate during big battles, but I still recommend disabling scents completely when you're participating in big fights.}", UI.scale(300));
	static final Object disableSeasonalGroundColors = RichText.render("This makes all biomes keep their summer colors, during any season." +
			"\n" +
			"\n$col[185,185,185]{Have you noticed how all biome colors shift their hue when the season changes?}", UI.scale(300));
	static final Object disableGroundCloudShadows = RichText.render("$col[185,185,185]{They look kinda nice to be honest.}", UI.scale(300));
	static final Object disableWetGroundOverlay = RichText.render("$col[185,185,185]{Honestly just keep it disabled. It looks VERY UGLY, lmao.}", UI.scale(300));
	static final Object disableValhallaFilter = RichText.render("This makes Valhalla look the same as the normal world." +
			"\n" +
			"\n$col[185,185,185]{I hate how it makes Valhalla look more gray, as if it's some weird purgatory.}", UI.scale(300));
	static final Object disableScreenShaking = RichText.render("$col[185,185,185]{This usually happens when a dungeon is about to collapse, after you've defeated the boss.}", UI.scale(300));
	static final Object disableLibertyCapsHigh = RichText.render("$col[200,0,0]{WARNING:} This is the only screen effect in the game that displays intense flashing lights and has sharp sounds." +
			"\n" +
			"\nIf you have epilepsy or are sensitive to these kinds of effects, I BEG YOU to keep it DISABLED if you are at risk." +
			"\n" +
			"\n$col[185,185,185]{I have no idea why this disgusting effect exists at all. " +
			"\nThe vanilla client does not warn you about it in any way, shape or form.}", UI.scale(280));
	static final Object onlyRenderCameraVisibleObjects = RichText.render("Render only objects within the camera's view frustum. Objects behind the camera are not rendered, reducing GPU load and potentially improving performance." +
			"\n" +
			"\n$col[218,163,0]{This is an experimental feature. It should work fine, but I wouldn't trust it with my life.}", UI.scale(300));

	// Server Integration Settings Tooltips
	static final Object uploadMapTiles = RichText.render("Enable this to upload your map tiles to your web map server.", UI.scale(300));
	static final Object sendLiveLocation = RichText.render("Enable this to show your current location on your web map server.", UI.scale(320));
	static final Object liveLocationName = RichText.render("If you send your location to the server, your name will appear as whatever you set in this text entry + your current character name." +
			"\n" +
			"\n$col[218,163,0]{For example:} Nightdawg (VillageCrafter)$col[185,185,185]{, where }\"Nightdawg\" $col[185,185,185]{is the name I set in this text entry, and} \"VillageCrafter\" $col[185,185,185]{is the character's original name." +
			"\nThe character's original name is the one you see in the character selection screen, NOT the presentation name.}", UI.scale(320));

	// Misc/Other
	static final Object resetButton = RichText.render("Reset to default value.", UI.scale(300));
	static final Object genericHasKeybind = RichText.render("$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
}
