/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.automated.mapper.MappingClient;
import haven.render.*;
import haven.res.sfx.ambient.weather.wsound.WeatherSound;
import haven.res.ui.pag.toggle.Toggle;
import haven.resutil.Ridges;
import haven.sprites.AggroCircleSprite;
import haven.sprites.ChaseVectorSprite;
import haven.sprites.PartyCircleSprite;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.Future;

public class OptWnd extends Window {
    public final Panel main;
	public final Panel advancedSettings;
    public Panel current;
	static final ScheduledExecutorService skyboxExecutor = Executors.newSingleThreadScheduledExecutor();
	static Future<?> skyboxFuture;
	public static final Color msgGreen = new Color(8, 211, 0);
	public static final Color msgGray = new Color(145, 145, 145);
	public static final Color msgRed = new Color(197, 0, 0);
	public static final Color msgYellow = new Color(218, 163, 0);
	public static FlowerMenuAutoSelectManagerWindow flowerMenuAutoSelectManagerWindow;
	public static AutoDropManagerWindow autoDropManagerWindow;
	public static ItemAutoDropWindow itemAutoDropWindow;
	AlarmWindow alarmWindow;
	public static GSettings currentgprefs;
	public static final Map<String, Color> improvedOpeningsImageColor =	new ConcurrentHashMap<>(4);

    public void chpanel(Panel p) {
	if(current != null)
	    current.hide();
	(current = p).show();
	cresize(p);
    }

    public void cresize(Widget ch) {
	if(ch == current) {
	    Coord cc = this.c.add(this.sz.div(2));
	    pack();
	    move(cc.sub(this.sz.div(2)));
	}
    }

    public class PButton extends Button {
	public final Panel tgt;
	public final int key;
	public String newCap; // ND: Used to change the title of the options window

//	public PButton(int w, String title, int key, Panel tgt) {
//	    super(w, title, false);
//	    this.tgt = tgt;
//	    this.key = key;
//	}

	public PButton(int w, String title, int key, Panel tgt, String newCap) {
		super(w, title, false);
		this.tgt = tgt;
		this.key = key;
		this.newCap = newCap;
	}

	public void click() {
	    chpanel(tgt);
		OptWnd.this.cap = newCap;
	}

	public boolean keydown(KeyDownEvent ev) {
	    if((this.key != -1) && (ev.c == this.key)) {
		click();
		return(true);
	    }
	    return(super.keydown(ev));
	}
    }

    public class Panel extends Widget {
	public Panel() {
	    visible = false;
	    c = Coord.z;
	}
    }

    private void error(String msg) {
	GameUI gui = getparent(GameUI.class);
	if(gui != null)
	    gui.error(msg);
    }

    public class VideoPanel extends Panel {
	private final Widget back;
	private CPanel curcf;

	public VideoPanel(Panel prev) {
	    super();
		back = add(new PButton(UI.scale(200), "Back", 27, prev, "Options            "));
		pack(); // ND: Fixes top bar not being fully draggable the first time I open the video panel. Idfk.
	}

	public class CPanel extends Widget {
	    public GSettings prefs;

	    public CPanel(GSettings gprefs) {
		this.prefs = gprefs;
		OptWndVideoPanel.buildCPanel(this,
		    () -> prefs,
		    (p) -> { ui.setgprefs(prefs = p); },
		    (msg) -> error(msg),
		    () -> resetcf(),
		    () -> { ui.setgprefs(GSettings.defaults()); curcf.destroy(); curcf = null; }
		);
	    }
	}

	public void draw(GOut g) {
	    if((curcf == null) || (ui.gprefs != curcf.prefs))
		resetcf();
	    super.draw(g);
	}

	private void resetcf() {
	    if(curcf != null)
		curcf.destroy();
	    curcf = add(new CPanel(ui.gprefs), 0, 0);
	    back.move(curcf.pos("bl").adds(0, 15));
	    pack();
	}
    }

	public static HSlider instrumentsSoundVolumeSlider;
	public static HSlider clapSoundVolumeSlider;
	public static HSlider quernSoundVolumeSlider;
    public static HSlider swooshSoundVolumeSlider;
    public static HSlider grammophoneHatSoundVolumeSlider;
    public static HSlider creakSoundVolumeSlider;
    public static HSlider waterSplashSoundVolumeSlider;
	public static HSlider cauldronSoundVolumeSlider;
	public static HSlider squeakSoundVolumeSlider;
	public static HSlider butcherSoundVolumeSlider;
	public static HSlider whiteDuckCapSoundVolumeSlider;
    public static HSlider chippingSoundVolumeSlider;
    public static HSlider miningSoundVolumeSlider;
    public static HSlider doomBellCapSoundVolumeSlider;
	public static HSlider themeSongVolumeSlider;
    public static HSlider weatherSoundVolumeSlider;
    public static HSlider knarrSoundVolumeSlider;

    public class AudioPanel extends Panel {
	public AudioPanel(Panel back) {
		OptWndAudioPanel.build(this, back, OptWnd.this);
	}
    }

	public static OldDropBox uiThemeDropBox;
	public static CheckBox extendedMouseoverInfoCheckBox;
	public static CheckBox disableMenuGridHotkeysCheckBox;
	public static CheckBox alwaysOpenBeltOnLoginCheckBox;
	public static CheckBox showMapMarkerNamesCheckBox;
	public static CheckBox verticalContainerIndicatorsCheckBox;
	public static boolean expWindowLocationIsTop = Utils.getprefb("expWindowLocationIsTop", true);
	static CheckBox showFramerateCheckBox;
	public static CheckBox snapWindowsBackInsideCheckBox;
	public static CheckBox dragWindowsInWhenResizingCheckBox;
	public static CheckBox showHoverInventoriesWhenHoldingShiftCheckBox;
	public static CheckBox showQuickSlotsCheckBox;
	public static CheckBox showEquipProxyCheckBox;
	public static CheckBox leftHandQuickSlotCheckBox, rightHandQuickSlotCheckBox, leftPouchQuickSlotCheckBox, rightPouchQuickSlotCheckBox,
			beltQuickSlotCheckBox, backpackQuickSlotCheckBox, shouldersQuickSlotCheckBox, capeQuickSlotCheckBox;
	public static CheckBox showStudyReportHistoryCheckBox;
	public static CheckBox lockStudyReportCheckBox;
	public static CheckBox soundAlertForFinishedCuriositiesCheckBox;
	public static CheckBox alwaysShowCombatUIStaminaBarCheckBox;
	public static CheckBox alwaysShowCombatUIHealthBarCheckBox;
	public static CheckBox transparentQuestsObjectivesWindowCheckBox;
	public static HSlider mapZoomSpeedSlider;
	public static CheckBox alwaysOpenMiniStudyOnLoginCheckBox;
	public static HSlider mapIconsSizeSlider;
	public static CheckBox simplifiedMapColorsCheckBox;
	public static ColorOptionWidget sprintLandsColorWidget;
	public static ColorOptionWidget thirdSpeedLandsColorWidget;
	public static ColorOptionWidget swampsColorWidget;
	public static ColorOptionWidget thicketColorWidget;
	public static CheckBox removeMapTileBordersCheckBox;
	public static CheckBox improvedInstrumentMusicWindowCheckBox;
    public static CheckBox preventEscKeyFromClosingWindowsCheckBox;
    public static CheckBox stackWindowsWhenOpenedCheckBox;

    public class InterfaceSettingsPanel extends Panel {
	public InterfaceSettingsPanel(Panel back) {
	    OptWndInterfaceSettingsPanel.build(this, back, OptWnd.this);
	}
    }

	public static CheckBox holdCTRLtoRemoveActionButtonsCheckBox;

	public class ActionBarsSettingsPanel extends Panel {
		public ActionBarsSettingsPanel(Panel back) {
			OptWndActionBarsPanel.build(this, back, OptWnd.this);
		}
	}

	public static CheckBox showCombatHotkeysUICheckBox;
	public static CheckBox showDamagePredictUICheckBox;
	public static CheckBox singleRowCombatMovesCheckBox;
	public static CheckBox includeHHPTextHealthBarCheckBox;
	public static ColorOptionWidget greenCombatColorOptionWidget;
	public static String[] greenCombatColorSetting = Utils.getprefsa("greenCombat" + "_colorSetting", new String[]{"0", "128", "3", "255"});
	public static ColorOptionWidget blueCombatColorOptionWidget;
	public static String[] blueCombatColorSetting = Utils.getprefsa("blueCombat" + "_colorSetting", new String[]{"39", "82", "191", "255"});
	public static ColorOptionWidget yellowCombatColorOptionWidget;
	public static String[] yellowCombatColorSetting = Utils.getprefsa("yellowCombat" + "_colorSetting", new String[]{"217", "177", "20", "255"});
	public static ColorOptionWidget redCombatColorOptionWidget;
	public static String[] redCombatColorSetting = Utils.getprefsa("redCombat" + "_colorSetting", new String[]{"192", "28", "28", "255"});
	public static CheckBox showCombatOpeningsAsLettersCheckBox;
	public static ColorOptionWidget myIPCombatColorOptionWidget;
	public static String[] myIPCombatColorSetting = Utils.getprefsa("myIPCombat" + "_colorSetting", new String[]{"0", "201", "4", "255"});
	public static ColorOptionWidget enemyIPCombatColorOptionWidget;
	public static String[] enemyIPCombatColorSetting = Utils.getprefsa("enemyIPCombat" + "_colorSetting", new String[]{"245", "0", "0", "255"});
	public static CheckBox showEstimatedAgilityTextCheckBox;
	public static CheckBox drawFloatingCombatDataCheckBox;
	public static CheckBox drawFloatingCombatDataOnCurrentTargetCheckBox;
	public static CheckBox drawFloatingCombatDataOnOthersCheckBox;
	public static CheckBox showCombatManeuverCombatInfoCheckBox;
	public static CheckBox onlyShowOpeningsAbovePercentageCombatInfoCheckBox;
    public static CheckBox includeCurrentTargetShowOpeningsAbovePercentageCombatInfoCheckBox;
	public static CheckBox onlyShowCoinsAbove4CombatInfoCheckBox;
	public static CheckBox drawFloatingCombatOpeningsAboveYourselfCheckBox;
	public static TextEntry minimumOpeningTextEntry;
	public static HSlider combatUITopPanelHeightSlider;
	public static HSlider combatUIBottomPanelHeightSlider;
	public static CheckBox toggleGobDamageInfoCheckBox;
	public static CheckBox toggleGobDamageWoundInfoCheckBox;
	public static CheckBox toggleGobDamageArmorInfoCheckBox;
	public static Button damageInfoClearButton;
	public static CheckBox yourselfDamageInfoCheckBox;
	public static CheckBox partyMembersDamageInfoCheckBox;
	public static boolean stamBarLocationIsTop = Utils.getprefb("stamBarLocationIsTop", true);
	public static CheckBox highlightPartyMembersCheckBox;
	public static CheckBox showCirclesUnderPartyMembersCheckBox;
	public static ColorOptionWidget yourselfPartyColorOptionWidget;
	public static String[] yourselfPartyColorSetting = Utils.getprefsa("yourselfParty" + "_colorSetting", new String[]{"255", "255", "255", "128"});
	public static ColorOptionWidget leaderPartyColorOptionWidget;
	public static String[] leaderPartyColorSetting = Utils.getprefsa("leaderParty" + "_colorSetting", new String[]{"0", "74", "208", "164"});
	public static ColorOptionWidget memberPartyColorOptionWidget;
	public static String[] memberPartyColorSetting = Utils.getprefsa("memberParty" + "_colorSetting", new String[]{"0", "160", "0", "164"});
	public static CheckBox highlightCombatFoesCheckBox;
	public static CheckBox showCirclesUnderCombatFoesCheckBox;
	public static ColorOptionWidget combatFoeColorOptionWidget;
	public static String[] combatFoeColorSetting = Utils.getprefsa("combatFoe" + "_colorSetting", new String[]{"180", "0", "0", "196"});
	public static boolean refreshCurrentTargetSpriteColor = false;
	public static HSlider targetSpriteSizeSlider;
	public static CheckBox drawChaseVectorsCheckBox;
	public static CheckBox drawYourCurrentPathCheckBox;

    public static ColorOptionWidget yourselfVectorColorOptionWidget;
    public static String[] yourselfVectorColorSetting = Utils.getprefsa("yourselfVector" + "_colorSetting", new String[]{"255", "255", "255", "255"});
    public static ColorOptionWidget friendVectorColorOptionWidget;
    public static String[] friendVectorColorSetting = Utils.getprefsa("friendVector" + "_colorSetting", new String[]{"47", "191", "7", "255"});
    public static ColorOptionWidget enemyVectorColorOptionWidget;
    public static String[] enemyVectorColorSetting = Utils.getprefsa("enemyVector" + "_colorSetting", new String[]{"255", "0", "0", "255"});

    public static CheckBox showYourCombatRangeCirclesCheckBox;
	public static boolean refreshMyUnarmedRange = false;
	public static boolean refreshMyWeaponRange = false;
	public static ColorOptionWidget unarmedCombatRangeColorOptionWidget;
	public static String[] unarmedCombatRangeColorSetting = Utils.getprefsa("unarmedCombatRange" + "_colorSetting", new String[]{"0", "160", "0", "255"});
	public static ColorOptionWidget weaponCombatRangeColorOptionWidget;
	public static String[] weaponCombatRangeColorSetting = Utils.getprefsa("weaponCombatRange" + "_colorSetting", new String[]{"130", "0", "172", "255"});

	public class CombatSettingsPanel extends Panel {
		public CombatSettingsPanel(Panel back) {
			OptWndCombatPanel.build(this, back, OptWnd.this);
		}
	}


	public static CheckBox excludeGreenBuddyFromAggroCheckBox;
	public static CheckBox excludeRedBuddyFromAggroCheckBox;
	public static CheckBox excludeBlueBuddyFromAggroCheckBox;
	public static CheckBox excludeTealBuddyFromAggroCheckBox;
	public static CheckBox excludeYellowBuddyFromAggroCheckBox;
	public static CheckBox excludePurpleBuddyFromAggroCheckBox;
	public static CheckBox excludeOrangeBuddyFromAggroCheckBox;
	public static CheckBox excludeAllVillageOrRealmMembersFromAggroCheckBox;

	public class AggroExclusionSettingsPanel extends Panel {
		public AggroExclusionSettingsPanel(Panel back) {
			OptWndAggroExclusionPanel.build(this, back, OptWnd.this);
		}
	}

	public static CheckBox chatAlertSoundsCheckBox;
	public static CheckBox areaChatAlertSoundsCheckBox;
	public static CheckBox partyChatAlertSoundsCheckBox;
	public static CheckBox privateChatAlertSoundsCheckBox;

	public static TextEntry villageNameTextEntry;
	public static CheckBox villageChatAlertSoundsCheckBox;
	public static CheckBox autoSelectNewChatCheckBox;
	public static CheckBox removeRealmChatCheckBox;
	public static CheckBox showKinStatusChangeMessages;

	public static HSlider systemMessagesListSizeSlider;
	public static HSlider systemMessagesDurationSlider;

	public class ChatSettingsPanel extends Panel {
		public ChatSettingsPanel(Panel back) {
			OptWndChatPanel.build(this, back, OptWnd.this);
		}
	}


	public static CheckBox showObjectCollisionBoxesCheckBox;
	public static ColorOptionWidget collisionBoxColorOptionWidget;
	public static String[] collisionBoxColorSetting = Utils.getprefsa("collisionBox" + "_colorSetting", new String[]{"255", "255", "255", "210"});
	public static CheckBox displayObjectDurabilityPercentageCheckBox;
    public static CheckBox showDurabilityCrackTextureCheckBox;
	public static CheckBox displayObjectQualityOnInspectionCheckBox;
	public static CheckBox displayGrowthInfoCheckBox;
	public static CheckBox alsoShowOversizedTreesAbovePercentageCheckBox;
	public static TextEntry oversizedTreesPercentageTextEntry;
	public static CheckBox showCritterAurasCheckBox;
	public static ColorOptionWidget rabbitAuraColorOptionWidget;
	public static String[] rabbitAuraColorSetting = Utils.getprefsa("rabbitAura" + "_colorSetting", new String[]{"88", "255", "0", "140"});
	public static ColorOptionWidget genericCritterAuraColorOptionWidget;
	public static String[] genericCritterAuraColorSetting = Utils.getprefsa("genericCritterAura" + "_colorSetting", new String[]{"193", "0", "255", "140"});
    public static ColorOptionWidget dangerousCritterAuraColorOptionWidget;
    public static String[] dangerousCritterAuraColorSetting = Utils.getprefsa("dangerousCritterAura" + "_colorSetting", new String[]{"193", "0", "0", "140"});
	public static CheckBox showSpeedBuffAurasCheckBox;
	public static ColorOptionWidget speedBuffAuraColorOptionWidget;
	public static String[] speedBuffAuraColorSetting = Utils.getprefsa("speedBuffAura" + "_colorSetting", new String[]{"255", "255", "255", "140"});
	public static CheckBox showMidgesCircleAurasCheckBox;
	public static CheckBox showDangerousBeastRadiiCheckBox;
	public static CheckBox showBeeSkepsRadiiCheckBox;
	public static CheckBox showFoodTroughsRadiiCheckBox;
	public static CheckBox showMoundBedsRadiiCheckBox;
	public static CheckBox showBarrelContentsTextCheckBox;
	public static CheckBox showIconSignTextCheckBox;
	public static CheckBox showCheeseRacksTierTextCheckBox;
	public static CheckBox highlightCliffsCheckBox;
	public static ColorOptionWidget highlightCliffsColorOptionWidget;
	public static String[] highlightCliffsColorSetting = Utils.getprefsa("highlightCliffs" + "_colorSetting", new String[]{"255", "0", "0", "200"});
	public static CheckBox showContainerFullnessCheckBox;
	public static CheckBox showContainerFullnessFullCheckBox;
	public static ColorOptionWidget showContainerFullnessFullColorOptionWidget;
	public static String[] containerFullnessFullColorSetting = Utils.getprefsa("containerFullnessFull" + "_colorSetting", new String[]{"170", "0", "0", "170"});
	public static CheckBox showContainerFullnessPartialCheckBox;
	public static ColorOptionWidget showContainerFullnessPartialColorOptionWidget;
	public static String[] containerFullnessPartialColorSetting = Utils.getprefsa("containerFullnessPartial" + "_colorSetting", new String[]{"194", "155", "2", "140"});
	public static CheckBox showContainerFullnessEmptyCheckBox;
	public static ColorOptionWidget showContainerFullnessEmptyColorOptionWidget;
	public static String[] containerFullnessEmptyColorSetting = Utils.getprefsa("containerFullnessEmpty" + "_colorSetting", new String[]{"0", "120", "0", "180"});
	public static CheckBox showWorkstationProgressCheckBox;
	public static CheckBox showWorkstationProgressFinishedCheckBox;
	public static ColorOptionWidget showWorkstationProgressFinishedColorOptionWidget;
	public static String[] workstationProgressFinishedColorSetting = Utils.getprefsa("workstationProgressFinished" + "_colorSetting", new String[]{"170", "0", "0", "170"});
	public static CheckBox showWorkstationProgressInProgressCheckBox;
	public static ColorOptionWidget showWorkstationProgressInProgressColorOptionWidget;
	public static String[] workstationProgressInProgressColorSetting = Utils.getprefsa("workstationProgressInProgress" + "_colorSetting", new String[]{"194", "155", "2", "140"});
	public static CheckBox showWorkstationProgressReadyForUseCheckBox;
	public static ColorOptionWidget showWorkstationProgressReadyForUseColorOptionWidget;
	public static String[] workstationProgressReadyForUseColorSetting = Utils.getprefsa("workstationProgressReadyForUse" + "_colorSetting", new String[]{"0", "120", "0", "180"});
	public static CheckBox showWorkstationProgressUnpreparedCheckBox;
	public static ColorOptionWidget showWorkstationProgressUnpreparedColorOptionWidget;
	public static String[] workstationProgressUnpreparedColorSetting = Utils.getprefsa("workstationProgressUnprepared" + "_colorSetting", new String[]{"20", "20", "20", "180"});
    public static CheckBox showMineSupportCoverageCheckBox;
    public static ColorOptionWidget safeTilesColorOptionWidget;
    public static String[] coveredTilesColorSetting = Utils.getprefsa("coveredTiles" + "_colorSetting", new String[]{"0", "105", "210", "60"});
	public static CheckBox enableMineSweeperCheckBox;
	public static OldDropBox<Integer> sweeperDurationDropbox;
	public static final List<Integer> sweeperDurations = Arrays.asList(5, 10, 15, 30, 45, 60, 120);
	public static int sweeperSetDuration = Utils.getprefi("sweeperSetDuration", 1);
	public static ColorOptionWidget areaChatPingColorOptionWidget;
	public static String[] areaChatPingColorSetting = Utils.getprefsa("areaChatPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
	public static ColorOptionWidget partyChatPingColorOptionWidget;
	public static String[] partyChatPingColorSetting = Utils.getprefsa("partyChatPing" + "_colorSetting", new String[]{"243", "0", "0", "255"});
	public static CheckBox showObjectsSpeedCheckBox;
	public static CheckBox showTreesBushesHarvestIconsCheckBox;
	public static CheckBox showLowFoodWaterIconsCheckBox;
	public static CheckBox showBeeSkepsHarvestIconsCheckBox;

	public static CheckBox objectPermanentHighlightingCheckBox;

	public class DisplaySettingsPanel extends Panel {
		public DisplaySettingsPanel(Panel back) {
			OptWndDisplayPanel.build(this, back, OptWnd.this);
		}
	}




	public static CheckBox showQualityDisplayCheckBox;
	public static CheckBox showItemCategoryBadgesCheckBox;
	public static CheckBox showArmorValuesCheckBox;
	public static CheckBox showDurabilityNumberCheckBox;
	public static CheckBox roundedQualityCheckBox;
	public static CheckBox customQualityColorsCheckBox;
	public static int qualityAggMode = Utils.getprefi("qualityAggMode", 0);

	public static TextEntry q7ColorTextEntry, q6ColorTextEntry, q5ColorTextEntry, q4ColorTextEntry, q3ColorTextEntry, q2ColorTextEntry, q1ColorTextEntry;
	public static ColorOptionWidget q7ColorOptionWidget, q6ColorOptionWidget, q5ColorOptionWidget, q4ColorOptionWidget, q3ColorOptionWidget, q2ColorOptionWidget, q1ColorOptionWidget;
	public static String[] q7ColorSetting = Utils.getprefsa("q7ColorSetting_colorSetting", new String[]{"255","0","0","255"});
	public static String[] q6ColorSetting = Utils.getprefsa("q6ColorSetting_colorSetting", new String[]{"255","114","0","255"});
	public static String[] q5ColorSetting = Utils.getprefsa("q5ColorSetting_colorSetting", new String[]{"165","0","255","255"});
	public static String[] q4ColorSetting = Utils.getprefsa("q4ColorSetting_colorSetting", new String[]{"0","131","255","255"});
	public static String[] q3ColorSetting = Utils.getprefsa("q3ColorSetting_colorSetting", new String[]{"0","214","10","255"});
	public static String[] q2ColorSetting = Utils.getprefsa("q2ColorSetting_colorSetting", new String[]{"255","255","255","255"});
	public static String[] q1ColorSetting = Utils.getprefsa("q1ColorSetting_colorSetting", new String[]{"180","180","180","255"});

	public class QualityDisplaySettingsPanel extends Panel {
		public QualityDisplaySettingsPanel(Panel back) {
			OptWndQualityPanel.build(this, back, OptWnd.this);
		}
	}


    private static final Text kbtt = RichText.render("$col[255,200,0]{Escape}: Cancel input\n" +
						     "$col[255,200,0]{Backspace}: Revert to default\n" +
						     "$col[255,200,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
	public BindingPanel(Panel back) {
	    super();
	    OptWndBindingPanel.build(this, back, OptWnd.this);
	}
	}

	public static class SetButton extends KeyMatch.Capture {
	    public final KeyBinding cmd;

	    public SetButton(int w, KeyBinding cmd) {
		super(w, cmd.key());
		this.cmd = cmd;
	    }

	    public void set(KeyMatch key) {
		super.set(key);
		cmd.set(key);
	    }

	    public void draw(GOut g) {
		if(cmd.key() != key)
		    super.set(cmd.key());
		super.draw(g);
	    }

	    protected KeyMatch mkmatch(KeyEvent ev) {
		return(KeyMatch.forevent(ev, ~cmd.modign));
	    }

	    protected boolean handle(KeyEvent ev) {
		if(ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
		    cmd.set(null);
		    super.set(cmd.key());
		    return(true);
		}
		return(super.handle(ev));
	    }

	    public Object tooltip(Coord c, Widget prev) {
		return(kbtt.tex());
	    }
	}

	public static CheckBox toggleTrackingOnLoginCheckBox;
	public static CheckBox toggleSwimmingOnLoginCheckBox;
	public static CheckBox toggleCriminalActsOnLoginCheckBox;
	public static CheckBox toggleSiegeEnginesOnLoginCheckBox;
	public static CheckBox togglePartyPermissionsOnLoginCheckBox;
	public static CheckBox toggleItemStackingOnLoginCheckBox;
	public static CheckBox autoSelect1stFlowerMenuCheckBox;
	public static CheckBox alsoUseContainersWithRepeaterCheckBox;
	public static CheckBox autoRepeatFlowerMenuCheckBox;
	public static CheckBox autoReloadCuriositiesFromInventoryCheckBox;
	public static CheckBox preventTablewareFromBreakingCheckBox = null;
	public static CheckBox autoDropLeechesCheckBox;
	public static CheckBox autoEquipBunnySlippersPlateBootsCheckBox;
	public static CheckBox autoDropTicksCheckBox;
	public static CheckBox autoPeaceAnimalsWhenCombatStartsCheckBox;
	public static CheckBox preventUsingRawHideWhenRidingCheckBox;
	public static CheckBox autoDrinkingCheckBox;
	public static TextEntry autoDrinkingThresholdTextEntry;
	public static CheckBox autoSortContainersCheckBox;
	public static CheckBox enableQueuedMovementCheckBox;
    public static CheckBox walkWithPathFinderCheckBox;
    public static CheckBox drawPathfinderRouteCheckBox;
    public static CheckBox pathfindOnMinimapCheckBox;
    public static CheckBox continuousPathfindingCheckBox;
    public static CheckBox terrainWeightedPathfindingCheckBox;
    public static CheckBox pathfindOnRightClickCheckBox;

	public class GameplayAutomationSettingsPanel extends Panel {
		public GameplayAutomationSettingsPanel(Panel back) {
			OptWndGameplayAutomationPanel.build(this, back, OptWnd.this);
		}
	}


	public static CheckBox overrideCursorItemWhenHoldingAltCheckBox;
	public static CheckBox noCursorItemDroppingAnywhereCheckBox;
	public static CheckBox noCursorItemDroppingInWaterCheckBox;
	public static CheckBox useOGControlsForBuildingAndPlacingCheckBox;
	public static CheckBox useImprovedInventoryTransferControlsCheckBox;
	public static CheckBox tileCenteringCheckBox;
	public static CheckBox clickThroughContainerDecalCheckBox;
	public static CheckBox continuousWalkingCheckBox;

	public class AlteredGameplaySettingsPanel extends Panel {
		public AlteredGameplaySettingsPanel(Panel back) {
			OptWndAlteredGameplayPanel.build(this, back, OptWnd.this);
		}
	}


	public static CheckBox allowMouse4CamDragCheckBox;
	public static CheckBox allowMouse5CamDragCheckBox;
	public static HSlider freeCamZoomSpeedSlider;
	public static HSlider freeCamRotationSensitivitySlider;
	public static HSlider freeCamHeightSlider;
	public static CheckBox unlockedOrthoCamCheckBox;
	public static HSlider orthoCamZoomSpeedSlider;
	public static HSlider orthoCamRotationSensitivitySlider;
	public static CheckBox reverseOrthoCameraAxesCheckBox;
	public static CheckBox reverseFreeCamXAxisCheckBox;
	public static CheckBox reverseFreeCamYAxisCheckBox;
	public static CheckBox lockVerticalAngleAt45DegreesCheckBox;
	public static CheckBox allowLowerFreeCamTiltCheckBox;

	public class CameraSettingsPanel extends Panel {
		public CameraSettingsPanel(Panel back) {
			OptWndCameraPanel.build(this, back, OptWnd.this);
		}
	}

	static Label nightVisionLabel;
	public static HSlider nightVisionSlider;
	static Button nightVisionResetButton;
	public static CheckBox simplifiedCropsCheckBox;
	public static CheckBox simplifiedForageablesCheckBox;
	public static CheckBox hideFlavorObjectsCheckBox;
	public static CheckBox flatWorldCheckBox;
	public static CheckBox disableTileSmoothingCheckBox;
    public static CheckBox disableTileBlendingCheckBox;
	public static CheckBox disableTileTransitionsCheckBox;
	public static CheckBox flatCaveWallsCheckBox;
	public static CheckBox straightCliffEdgesCheckBox;
	public static CheckBox disableSeasonalGroundColorsCheckBox;
	public static CheckBox disableGroundCloudShadowsCheckBox;
	public static CheckBox disableRainCheckBox;
	public static CheckBox disableWetGroundOverlayCheckBox;
	public static CheckBox disableSnowingCheckBox;
	public static HSlider treeAndBushScaleSlider;
	static Button treeAndBushScaleResetButton;
	public static CheckBox disableTreeAndBushSwayingCheckBox;
	public static CheckBox disableIndustrialSmokeCheckBox;
	public static CheckBox disableScentSmokeCheckBox;
	public static CheckBox flatCupboardsCheckBox;
	public static CheckBox disableHerbalistTablesVarMatsCheckBox;
	public static CheckBox disableCupboardsVarMatsCheckBox;
	public static CheckBox disableChestsVarMatsCheckBox;
	public static CheckBox disableMetalCabinetsVarMatsCheckBox;
	public static CheckBox disableTrellisesVarMatsCheckBox;
	public static CheckBox disableSmokeShedsVarMatsCheckBox;
	public static CheckBox disableAllObjectsVarMatsCheckBox;
	public static CheckBox disableValhallaFilterCheckBox;
	public static CheckBox disableScreenShakingCheckBox;
	public static CheckBox disableHempHighCheckBox;
	public static CheckBox disableOpiumHighCheckBox;
	public static CheckBox disableLibertyCapsHighCheckBox;
	public static CheckBox disableDrunkennessDistortionCheckBox;
    public static CheckBox onlyRenderCameraVisibleObjectsCheckBox;
	public static HSlider palisadesAndBrickWallsScaleSlider;
	static Button palisadesAndBrickWallsScaleResetButton;
	public static CheckBox enableSkyboxCheckBox;

	public class WorldGraphicsSettingsPanel extends Panel {
		public WorldGraphicsSettingsPanel(Panel back) {
			OptWndWorldGraphicsPanel.build(this, back, OptWnd.this);
		}
	}

	public static CheckBox toggleGobHidingCheckBox;
	public static CheckBox alsoFillTheHidingBoxesCheckBox;
	public static CheckBox dontHideObjectsThatHaveTheirMapIconEnabledCheckBox;
	public static CheckBox hideTreesCheckbox;
	public static CheckBox hideBushesCheckbox;
	public static CheckBox hideBouldersCheckbox;
	public static CheckBox hideTreeLogsCheckbox;
	public static CheckBox hideWallsCheckbox;
	public static CheckBox hideHousesCheckbox;
	public static CheckBox hideCropsCheckbox;
	public static CheckBox hideTrellisCheckbox;
	public static CheckBox hideStockpilesCheckbox;
	public static ColorOptionWidget hiddenObjectsColorOptionWidget;
	public static String[] hiddenObjectsColorSetting = Utils.getprefsa("hidingBox" + "_colorSetting", new String[]{"0", "225", "255", "170"});

	public class HidingSettingsPanel extends Panel {
		public HidingSettingsPanel(Panel back) {
			OptWndHidingPanel.build(this, back, OptWnd.this);
		}
	}

	public static CheckBox whitePlayerAlarmEnabledCheckbox, whiteVillageOrRealmPlayerAlarmEnabledCheckbox, greenPlayerAlarmEnabledCheckbox,
			redPlayerAlarmEnabledCheckbox, bluePlayerAlarmEnabledCheckbox, tealPlayerAlarmEnabledCheckbox, yellowPlayerAlarmEnabledCheckbox,
			purplePlayerAlarmEnabledCheckbox, orangePlayerAlarmEnabledCheckbox;
	public static TextEntry whitePlayerAlarmFilename, whiteVillageOrRealmPlayerAlarmFilename, greenPlayerAlarmFilename,
			redPlayerAlarmFilename, bluePlayerAlarmFilename, tealPlayerAlarmFilename, yellowPlayerAlarmFilename,
			purplePlayerAlarmFilename, orangePlayerAlarmFilename;
	public static HSlider whitePlayerAlarmVolumeSlider, whiteVillageOrRealmPlayerAlarmVolumeSlider, greenPlayerAlarmVolumeSlider,
			redPlayerAlarmVolumeSlider, bluePlayerAlarmVolumeSlider, tealPlayerAlarmVolumeSlider, yellowPlayerAlarmVolumeSlider,
			purplePlayerAlarmVolumeSlider, orangePlayerAlarmVolumeSlider;

	public static CheckBox combatStartSoundEnabledCheckbox, cleaveSoundEnabledCheckbox, opkSoundEnabledCheckbox,
			ponyPowerSoundEnabledCheckbox, lowEnergySoundEnabledCheckbox;
	public static TextEntry combatStartSoundFilename, cleaveSoundFilename, opkSoundFilename,
			ponyPowerSoundFilename, lowEnergySoundFilename;
	public static HSlider combatStartSoundVolumeSlider, cleaveSoundVolumeSlider, opkSoundVolumeSlider,
			ponyPowerSoundVolumeSlider, lowEnergySoundVolumeSlider;

	Button CustomAlarmManagerButton;

	public class AlarmsAndSoundsSettingsPanel extends Panel {
		public AlarmsAndSoundsSettingsPanel(Panel back) {
			OptWndAlarmsPanel.build(this, back, OptWnd.this);
		}
	}

	public static TextEntry webmapEndpointTextEntry;
	public static CheckBox uploadMapTilesCheckBox;
	public static CheckBox sendLiveLocationCheckBox;
	public static TextEntry liveLocationNameTextEntry;
//	public static TextEntry webmapTokenTextEntry;

	public static TextEntry cookBookEndpointTextEntry;
	public static TextEntry cookBookTokenTextEntry;



	public class ServerIntegrationSettingsPanel extends Panel {
		public ServerIntegrationSettingsPanel(Panel back) {
			OptWndServerIntegrationPanel.build(this, back, OptWnd.this);
		}
	}

    public static CheckBox autoLootHeadgearCheckBox;
    public static CheckBox autoLootNecklaceCheckBox;
    public static CheckBox autoLootShouldersCheckBox;
    public static CheckBox autoLootShirtCheckBox;
    public static CheckBox autoLootGlovesCheckBox;
    public static CheckBox autoLootCloakRobeCheckBox;
    public static CheckBox autoLootPantsCheckBox;
    public static CheckBox autoLootCapeCheckBox;

    public static CheckBox autoLootMaskCheckBox;
    public static CheckBox autoLootEyewearCheckBox;
    public static CheckBox autoLootMouthwearCheckBox;
    public static CheckBox autoLootChestArmorCheckBox;
    // ND: Belt shouldn't be attempted, since you can't put it in your inventory if filled anyway
    public static CheckBox autoLootBackpackCheckBox;
	public static CheckBox autoLootLegArmorCheckBox;
	public static CheckBox autoLootShoesCheckBox;

    // ND: it's stupid that I have to add them like this, but the game freezes if I only use one and add it twice lol
    public static CheckBox autoLootWeaponCheckBox, autoLootWeaponCheckBox2; // ND: Checks both hands just for weapons
    public static CheckBox autoLootRingsCheckBox, autoLootRingsCheckBox2; // ND: Tries both rings
    public static CheckBox autoLootPouchesCheckBox, autoLootPouchesCheckBox2; // ND: Tries both pouches

	public class AutoLootSettingsPanel extends Panel {
		public AutoLootSettingsPanel(Panel back) {
			OptWndAutoLootPanel.build(this, back, OptWnd.this);
		}
	}


    public static class PointBind extends Button implements CursorQuery.Handler {
	public static final String msg = "Bind other elements...";
	public static final Resource curs = Resource.local().loadwait("gfx/hud/curs/wrench");
	private UI.Grab mg, kg;
	private KeyBinding cmd;

	public PointBind(int w) {
	    super(w, msg, false);
	    tooltip = RichText.render("Bind a key to an element not listed above, such as an action-menu " +
				      "button. Click the element to bind, and then press the key to bind to it. " +
				      "Right-click to stop rebinding.",
				      300);
	}

	public void click() {
	    if(mg == null) {
		change("Click element...");
		mg = ui.grabmouse(this);
	    } else if(kg != null) {
		kg.remove();
		kg = null;
		change(msg);
	    }
	}

	private boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    int code = ev.getKeyCode();
	    if(code == KeyEvent.VK_ESCAPE) {
		return(true);
	    }
	    if(code == KeyEvent.VK_BACK_SPACE) {
		cmd.set(null);
		return(true);
	    }
	    if(code == KeyEvent.VK_DELETE) {
		cmd.set(KeyMatch.nil);
		return(true);
	    }
	    KeyMatch key = KeyMatch.forevent(ev, ~cmd.modign);
	    if(key != null)
		cmd.set(key);
	    return(true);
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(!ev.grabbed)
		return(super.mousedown(ev));
	    Coord gc = ui.mc;
	    if(ev.b == 1) {
		this.cmd = KeyBinding.Bindable.getbinding(ui.root, gc);
		return(true);
	    }
	    if(ev.b == 3) {
		mg.remove();
		mg = null;
		change(msg);
		return(true);
	    }
	    return(false);
	}

	public boolean mouseup(MouseUpEvent ev) {
	    if(mg == null)
		return(super.mouseup(ev));
	    Coord gc = ui.mc;
	    if(ev.b == 1) {
		if((this.cmd != null) && (KeyBinding.Bindable.getbinding(ui.root, gc) == this.cmd)) {
		    mg.remove();
		    mg = null;
		    kg = ui.grabkeys(this);
		    change("Press key...");
		} else {
		    this.cmd = null;
		}
		return(true);
	    }
	    if(ev.b == 3)
		return(true);
	    return(false);
	}

	public boolean getcurs(CursorQuery ev) {
	    return(ev.grabbed ? ev.set(curs) : false);
	}

	public boolean keydown(KeyDownEvent ev) {
	    if(!ev.grabbed)
		return(super.keydown(ev));
	    if(handle(ev.awt)) {
		kg.remove();
		kg = null;
		cmd = null;
		change("Click another element...");
		mg = ui.grabmouse(this);
	    }
	    return(true);
	}
    }

    public OptWnd(boolean gopts) {
	super(Coord.z, "Options            ", true); // ND: Added a bunch of spaces to the caption(title) in order avoid text cutoff when changing it
	autoDropManagerWindow = new AutoDropManagerWindow();
	itemAutoDropWindow = new ItemAutoDropWindow();
	flowerMenuAutoSelectManagerWindow = new FlowerMenuAutoSelectManagerWindow();
	main = add(new Panel());
	Panel video = add(new VideoPanel(main));
	Panel audio = add(new AudioPanel(main));
	Panel keybind = add(new BindingPanel(main));

	int y = UI.scale(6);
	Widget prev;
	y = main.add(new PButton(UI.scale(200), "Video Settings", -1, video, "Video Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Audio Settings", -1, audio, "Audio Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Keybindings (Hotkeys)", -1, keybind, "Keybindings (Hotkeys)"), 0, y).pos("bl").adds(0, 5).y;
	y += UI.scale(20);

	advancedSettings = add(new Panel());
	// ND: Add the sub-panel buttons for the advanced settings here
		Panel interfacesettings = add(new InterfaceSettingsPanel(advancedSettings));
		Panel actionbarssettings =  add(new ActionBarsSettingsPanel(advancedSettings));
		Panel chatsettings =  add(new ChatSettingsPanel(advancedSettings));
		Panel displaysettings = add(new DisplaySettingsPanel(advancedSettings));
		Panel qualitydisplaysettings = add(new QualityDisplaySettingsPanel(advancedSettings));
		Panel gameplayautomationsettings = add(new GameplayAutomationSettingsPanel(advancedSettings));
		Panel alteredgameplaysettings =  add(new AlteredGameplaySettingsPanel(advancedSettings));
		Panel camsettings = add(new CameraSettingsPanel(advancedSettings));
		Panel worldgraphicssettings = add(new WorldGraphicsSettingsPanel(advancedSettings));
		Panel hidingsettings = add(new HidingSettingsPanel(advancedSettings));
		Panel alarmsettings = add(new AlarmsAndSoundsSettingsPanel(advancedSettings));
		Panel combatsettings = add(new CombatSettingsPanel(advancedSettings));
		Panel combataggrosettings = add(new AggroExclusionSettingsPanel(advancedSettings));
		Panel serverintegrationsettings = add(new ServerIntegrationSettingsPanel(advancedSettings));
		Panel autolootsettings = add(new AutoLootSettingsPanel(advancedSettings));

		int leftY = UI.scale(6);
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Interface Settings", -1, interfacesettings, "Interface Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Action Bars Settings", -1, actionbarssettings, "Action Bars Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Combat Settings", -1, combatsettings, "Combat Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Quality Display Settings", -1, qualitydisplaysettings, "Quality Display Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Chat Settings", -1, chatsettings, "Chat Settings"), 0, leftY).pos("bl").adds(0, 5).y;

		leftY += UI.scale(20);
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Altered Gameplay Settings", -1, alteredgameplaysettings, "Altered Gameplay Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Aggro Exclusion Settings", -1, combataggrosettings, "Aggro Exclusion Settings"), 0, leftY).pos("bl").adds(0, 5).y;

		int rightX = UI.scale(220);
		int rightY = UI.scale(6);
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Display Settings", -1, displaysettings, "Display Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Camera Settings", -1, camsettings, "Camera Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "World Graphics Settings", -1, worldgraphicssettings, "World Graphics Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Hiding Settings", -1, hidingsettings, "Hiding Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Alarms & Sounds Settings", -1, alarmsettings, "Alarms & Sounds Settings"), rightX, rightY).pos("bl").adds(0, 5).y;

		rightY += UI.scale(20);
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Gameplay Automation Settings", -1, gameplayautomationsettings, "Gameplay Automation Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Auto-Loot Settings", -1, autolootsettings, "Auto-Loot Settings"), rightX, rightY).pos("bl").adds(0, 5).y;


		int middleX = UI.scale(110);
		int middleY = leftY + UI.scale(20);
		middleY = advancedSettings.add(new PButton(UI.scale(200), "Server Integration Settings", -1, serverintegrationsettings, "Server Integration Settings"), middleX, middleY).pos("bl").adds(0, 5).y;
		middleY += UI.scale(20);
		middleY = advancedSettings.add(new PButton(UI.scale(200), "Back", 27, main, "Options            "), middleX, middleY).pos("bl").adds(0, 5).y;
	this.advancedSettings.pack();

	// Now back to the main panel, we add the advanced settings button and continue with everything else
	y = main.add(new PButton(UI.scale(200), "Advanced Settings", -1, advancedSettings, "Advanced Settings"), 0, y).pos("bl").adds(0, 5).y;
	y += UI.scale(20);
	if(gopts) {
	    if((SteamStore.steamsvc.get() != null) && (Steam.get() != null)) {
		y = main.add(new Button(UI.scale(200), "Visit store", false).action(() -> {
			    SteamStore.launch(ui.sess);
		}), 0, y).pos("bl").adds(0, 5).y;
	    }
	    y = main.add(new Button(UI.scale(200), "Switch character", false).action(() -> {
			getparent(GameUI.class).act("lo", "cs");
	    }), 0, y).pos("bl").adds(0, 5).y;
	    y = main.add(new Button(UI.scale(200), "Log out", false).action(() -> {
			getparent(GameUI.class).act("lo");
	    }), 0, y).pos("bl").adds(0, 5).y;
	}
	y = main.add(new Button(UI.scale(200), "Close", false).action(() -> {
		    OptWnd.this.hide();
	}), 0, y).pos("bl").adds(0, 5).y;
	this.main.pack();

	chpanel(this.main);
    }

    public OptWnd() {
	this(true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
	    hide();
		cap = "Options            ";
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void show() {
	chpanel(main);
	super.show();
    }

	void centerBackButton(Widget backButton, Widget parent){ // ND: Should only be used at the very end after the panel was already packed once.
		backButton.move(new Coord(parent.sz.x/2-backButton.sz.x/2, backButton.c.y));
		pack();
	}

	void resetSkyboxCheckbox(){
		enableSkyboxCheckBox.set(true);
		skyboxFuture.cancel(true);
	}


	@Override
	protected void attached() {
		super.attached();
		if (ui != null)
			currentgprefs = ui.gprefs;
		if (ui.gui != null) {
			ui.gui.add(autoDropManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
			autoDropManagerWindow.hide();
			ui.gui.add(flowerMenuAutoSelectManagerWindow);
			flowerMenuAutoSelectManagerWindow.hide();
		}
	}
}
