package haven;

import haven.render.*;
import haven.res.ui.pag.toggle.Toggle;
import haven.sprites.AggroCircleSprite;
import haven.sprites.ChaseVectorSprite;
import haven.sprites.PartyCircleSprite;

import java.awt.*;

/**
 * Extracted Combat Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.CombatSettingsPanel.
 */
class OptWndCombatPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget leftColumn, rightColumn;

		leftColumn = panel.add(new Label("Top panel height:"), 0, 0);
		leftColumn = panel.add(OptWnd.combatUITopPanelHeightSlider = new HSlider(UI.scale(200), 36, 440, Utils.getprefi("combatTopPanelHeight", 400)) {
			public void changed() {
				Utils.setprefi("combatTopPanelHeight", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			OptWnd.combatUITopPanelHeightSlider.val = 400;
			Utils.setprefi("combatTopPanelHeight", 400);
		}), leftColumn.pos("bl").adds(210, -20)).tooltip = OptWndTooltips.resetButton;
		leftColumn = panel.add(new Label("Bottom panel height:"), leftColumn.pos("bl").adds(0, 10));
		leftColumn = panel.add(OptWnd.combatUIBottomPanelHeightSlider = new HSlider(UI.scale(200), 10, 440, Utils.getprefi("combatBottomPanelHeight", 100)) {
			public void changed() {
				Utils.setprefi("combatBottomPanelHeight", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			OptWnd.combatUIBottomPanelHeightSlider.val = 100;
			Utils.setprefi("combatBottomPanelHeight", 100);
		}), leftColumn.pos("bl").adds(210, -20)).tooltip = OptWndTooltips.resetButton;

		leftColumn = panel.add(OptWnd.showEstimatedAgilityTextCheckBox = new CheckBox("Show Target Estimated Agility (Top Panel)"){
			{a = Utils.getprefb("showEstimatedAgility", true);}
			public void changed(boolean val) {
				Utils.setprefb("showEstimatedAgility", val);
			}
		}, leftColumn.pos("bl").adds(0, 12).x(0));

		leftColumn = panel.add(OptWnd.includeHHPTextHealthBarCheckBox = new CheckBox("Include HHP% text in Health Bar (Top Panel)"){
			{a = Utils.getprefb("includeHHPTextHealthBar", false);}
			public void changed(boolean val) {
				Utils.setprefb("includeHHPTextHealthBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = panel.add(new Label("Stamina Bar Location:"), leftColumn.pos("bl").adds(0, 10));{
			RadioGroup expWindowGrp = new RadioGroup(panel) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setprefb("stamBarLocationIsTop", true);
							OptWnd.stamBarLocationIsTop = true;
						}
						if(btn==1) {
							Utils.setprefb("stamBarLocationIsTop", false);
							OptWnd.stamBarLocationIsTop = false;
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			leftColumn = expWindowGrp.add("Top Panel", leftColumn.pos("bl").adds(20, 3));
			leftColumn = expWindowGrp.add("Bottom Panel", leftColumn.pos("ur").adds(30, 0));
			if (Utils.getprefb("stamBarLocationIsTop", true)){
				expWindowGrp.check(0);
			} else {
				expWindowGrp.check(1);
			}
		}

		leftColumn = panel.add(OptWnd.showCombatHotkeysUICheckBox = new CheckBox("Show Combat Move Hotkeys (Bottom Panel)"){
			{a = Utils.getprefb("showCombatHotkeysUI", true);}
			public void changed(boolean val) {
				Utils.setprefb("showCombatHotkeysUI", val);
			}
		}, leftColumn.pos("bl").adds(0, 12).xs(0));
		leftColumn = panel.add(OptWnd.singleRowCombatMovesCheckBox = new CheckBox("Single Row for Combat Moves (Bottom Panel)"){
			{a = Utils.getprefb("singleRowCombatMoves", false);}
			public void set(boolean val) {
				Utils.setprefb("singleRowCombatMoves", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.singleRowCombatMovesCheckBox.tooltip = OptWndTooltips.singleRowCombatMoves;
		leftColumn = panel.add(OptWnd.showDamagePredictUICheckBox = new CheckBox("Show Combat Damage Prediction (Bottom Panel)"){
			{a = Utils.getprefb("showDamagePredictUI", true);}
			public void changed(boolean val) {
				Utils.setprefb("showDamagePredictUI", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		OptWnd.showDamagePredictUICheckBox.tooltip = OptWndTooltips.showDamagePredictUI;

		leftColumn = panel.add(OptWnd.drawFloatingCombatOpeningsAboveYourselfCheckBox = new CheckBox("Display Combat Openings above Yourself"){
			{a = Utils.getprefb("drawFloatingCombatDataAboveYourself", true);}
			public void changed(boolean val) {
				Utils.setprefb("drawFloatingCombatDataAboveYourself", val);
			}
		}, leftColumn.pos("bl").adds(0, 18).x(0));

		leftColumn = panel.add(OptWnd.drawFloatingCombatDataCheckBox = new CheckBox("Display Combat Data above Combat Foes:"){
			{a = Utils.getprefb("drawFloatingCombatData", true);}
			public void changed(boolean val) {
				Utils.setprefb("drawFloatingCombatData", val);
			}
		}, leftColumn.pos("bl").adds(0, 4));
		panel.add(new Label(" >"), leftColumn.pos("bl").adds(0, 4).xs(0));
		leftColumn = panel.add(OptWnd.drawFloatingCombatDataOnCurrentTargetCheckBox = new CheckBox("Show on Current Target"){
			{a = Utils.getprefb("drawFloatingCombatDataOnCurrentTarget", true);}
			public void changed(boolean val) {
				Utils.setprefb("drawFloatingCombatDataOnCurrentTarget", val);
			}
		}, leftColumn.pos("bl").adds(20, 4));
		panel.add(new Label(" >"), leftColumn.pos("bl").adds(0, 3).xs(0));
		leftColumn = panel.add(OptWnd.drawFloatingCombatDataOnOthersCheckBox = new CheckBox("Show on other Combat Foes"){
			{a = Utils.getprefb("drawFloatingCombatDataOnOthers", true);}
			public void changed(boolean val) {
				Utils.setprefb("drawFloatingCombatDataOnOthers", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		panel.add(new Label(" >"), leftColumn.pos("bl").adds(0, 3).xs(0));
		leftColumn = panel.add(OptWnd.showCombatManeuverCombatInfoCheckBox = new CheckBox("Show Combat Stance/Maneuver"){
			{a = Utils.getprefb("showCombatManeuverCombatInfo", true);}
			public void changed(boolean val) {
				Utils.setprefb("showCombatManeuverCombatInfo", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		panel.add(new Label(" >"), leftColumn.pos("bl").adds(0, 3).xs(0));
		leftColumn = panel.add(OptWnd.onlyShowOpeningsAbovePercentageCombatInfoCheckBox = new CheckBox("Only show openings when higher than:"){
			{a = Utils.getprefb("onlyShowOpeningsAbovePercentage", false);}
			public void changed(boolean val) {
				Utils.setprefb("onlyShowOpeningsAbovePercentage", val);
			}
		}, leftColumn.pos("bl").adds(0, 4));
		OptWnd.onlyShowOpeningsAbovePercentageCombatInfoCheckBox.tooltip = OptWndTooltips.onlyShowOpeningsAbovePercentageCombatInfo;
		panel.add(OptWnd.minimumOpeningTextEntry = new TextEntry(UI.scale(40), Utils.getpref("minimumOpening", "30")){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
				this.settext(this.text().replaceAll("(?<=^.{2}).*", "")); // No more than 2 digits
				Utils.setpref("minimumOpening", this.buf.line());
				super.changed();
			}
		}, leftColumn.pos("ur").adds(10, 0));
		leftColumn = panel.add(OptWnd.includeCurrentTargetShowOpeningsAbovePercentageCombatInfoCheckBox = new CheckBox("Include Current Target"){
			{a = Utils.getprefb("includeCurrentTargetShowOpeningsAbovePercentage", false);}
			public void changed(boolean val) {
				Utils.setprefb("includeCurrentTargetShowOpeningsAbovePercentage", val);
			}
		}, leftColumn.pos("bl").adds(20, 4));
		panel.add(new Label(" >"), leftColumn.pos("bl").adds(0, 2).xs(0));

		leftColumn = panel.add(OptWnd.onlyShowCoinsAbove4CombatInfoCheckBox = new CheckBox("Only show coins when higher than 4"){
			{a = Utils.getprefb("onlyShowCoinsAbove4", false);}
			public void changed(boolean val) {
				Utils.setprefb("onlyShowCoinsAbove4", val);
			}
		}, leftColumn.pos("bl").adds(0, 2).xs(20));

		leftColumn = panel.add(OptWnd.toggleGobDamageInfoCheckBox = new CheckBox("Display Damage Info:"){
			{a = Utils.getprefb("GobDamageInfoToggled", true);}
			public void changed(boolean val) {
				Utils.setprefb("GobDamageInfoToggled", val);
			}
		}, leftColumn.pos("bl").adds(0, 10).xs(0));
		leftColumn = panel.add(new Label("> Include:"), leftColumn.pos("bl").adds(0, 1).xs(0));
		leftColumn = panel.add(OptWnd.toggleGobDamageWoundInfoCheckBox = new CheckBox("Wounds"){
			{a = Utils.getprefb("GobDamageInfoWoundsToggled", true);}
			public void changed(boolean val) {
				Utils.setprefb("GobDamageInfoWoundsToggled", val);
			}
		}, leftColumn.pos("bl").adds(56, -17));
		OptWnd.toggleGobDamageWoundInfoCheckBox.lbl = Text.create("Wounds", PUtils.strokeImg(Text.std.render("Wounds", new Color(255, 232, 0, 255))));
		leftColumn = panel.add(OptWnd.toggleGobDamageArmorInfoCheckBox = new CheckBox("Armor"){
			{a = Utils.getprefb("GobDamageInfoArmorToggled", true);}
			public void changed(boolean val) {
				Utils.setprefb("GobDamageInfoArmorToggled", val);
			}
		}, leftColumn.pos("bl").adds(66, -18));
		OptWnd.toggleGobDamageArmorInfoCheckBox.lbl = Text.create("Armor", PUtils.strokeImg(Text.std.render("Armor", new Color(50, 255, 92, 255))));
		panel.add(OptWnd.damageInfoClearButton = new Button(UI.scale(70), "Clear", false).action(() -> {
			GobDamageInfo.clearAllDamage(panel.ui.gui);
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.gui.optionInfoMsg("All Combat Damage Info has been CLEARED!", OptWnd.msgYellow, Audio.resclip(Toggle.sfxoff));
			}
		}), leftColumn.pos("ur").adds(37, -16));
		OptWnd.damageInfoClearButton.tooltip = OptWndTooltips.damageInfoClear;
		leftColumn = panel.add(new Label("> Also show on:"), leftColumn.pos("bl").adds(0, 2).xs(0));
		leftColumn = panel.add(OptWnd.yourselfDamageInfoCheckBox = new CheckBox("Yourself"){
			{a = Utils.getprefb("yourselfDamageInfo", true);}
			public void changed(boolean val) {
				Utils.setprefb("yourselfDamageInfo", val);
			}
		}, leftColumn.pos("bl").adds(80, -17));
		leftColumn = panel.add(OptWnd.partyMembersDamageInfoCheckBox = new CheckBox("Party Members"){
			{a = Utils.getprefb("(partyMembersDamageInfo", true);}
			public void changed(boolean val) {
				Utils.setprefb("(partyMembersDamageInfo", val);
			}
		}, leftColumn.pos("ur").adds(6, 0));

		leftColumn = panel.add(OptWnd.showYourCombatRangeCirclesCheckBox = new CheckBox("Show Your Combat Range Circles"){
			{a = Utils.getprefb("showYourCombatRangeCircles", false);}
			public void changed(boolean val) {
				Utils.setprefb("showYourCombatRangeCircles", val);
			}
		}, leftColumn.pos("bl").adds(0, 12).x(0));
		OptWnd.showYourCombatRangeCirclesCheckBox.tooltip = OptWndTooltips.showYourCombatRangeCircles;
		leftColumn = panel.add(new Label("Unarmed"), leftColumn.pos("bl").adds(16, 1));
		panel.add(OptWnd.unarmedCombatRangeColorOptionWidget = new ColorOptionWidget("", "unarmedCombatRange", 0, Integer.parseInt(OptWnd.unarmedCombatRangeColorSetting[0]), Integer.parseInt(OptWnd.unarmedCombatRangeColorSetting[1]), Integer.parseInt(OptWnd.unarmedCombatRangeColorSetting[2]), Integer.parseInt(OptWnd.unarmedCombatRangeColorSetting[3]), (Color col) -> {
			OptWnd.refreshMyUnarmedRange = true;
		}){}, leftColumn.pos("bl").adds(12, 0));
		leftColumn = panel.add(new Label("Weapon"), leftColumn.pos("ur").adds(20, 0));
		leftColumn = panel.add(OptWnd.weaponCombatRangeColorOptionWidget = new ColorOptionWidget("", "weaponCombatRange", 0, Integer.parseInt(OptWnd.weaponCombatRangeColorSetting[0]), Integer.parseInt(OptWnd.weaponCombatRangeColorSetting[1]), Integer.parseInt(OptWnd.weaponCombatRangeColorSetting[2]), Integer.parseInt(OptWnd.weaponCombatRangeColorSetting[3]), (Color col) -> {
			OptWnd.refreshMyWeaponRange = true;
		}){}, leftColumn.pos("bl").adds(10, 0));
		leftColumn = panel.add(new Button(UI.scale(70), "Reset All", false).action(() -> {
			Utils.setprefsa("unarmedCombatRange" + "_colorSetting", new String[]{"0", "160", "0", "255"});
			Utils.setprefsa("weaponCombatRange" + "_colorSetting", new String[]{"130", "0", "172", "255"});
			OptWnd.unarmedCombatRangeColorOptionWidget.cb.colorChooser.setColor(OptWnd.unarmedCombatRangeColorOptionWidget.currentColor = new Color(0, 160, 0, 255));
			OptWnd.weaponCombatRangeColorOptionWidget.cb.colorChooser.setColor(OptWnd.weaponCombatRangeColorOptionWidget.currentColor = new Color(130, 0, 172, 255));
			OptWnd.refreshMyUnarmedRange = true;
			OptWnd.refreshMyWeaponRange = true;
		}), leftColumn.pos("ur").adds(46, -2));

		rightColumn = panel.add(OptWnd.showCombatOpeningsAsLettersCheckBox = new CheckBox("Show Combat Openings as Colored Letters"){
			{a = Utils.getprefb("showCombatOpeningsAsLetters", false);}
			public void changed(boolean val) {
				Utils.setprefb("showCombatOpeningsAsLetters", val);
			}
		}, UI.scale(320, 0));
		OptWnd.showCombatOpeningsAsLettersCheckBox.tooltip = OptWndTooltips.showCombatOpeningsAsLetters;

		rightColumn = panel.add(new Label("Combat Openings Colors:"), rightColumn.pos("bl").adds(0, 4));
		rightColumn = panel.add(new Label("Green"), rightColumn.pos("bl").adds(2, 1));
		panel.add(OptWnd.greenCombatColorOptionWidget = new ColorOptionWidget("", "greenCombat", 0,
				Integer.parseInt(OptWnd.greenCombatColorSetting[0]), Integer.parseInt(OptWnd.greenCombatColorSetting[1]), Integer.parseInt(OptWnd.greenCombatColorSetting[2]), Integer.parseInt(OptWnd.greenCombatColorSetting[3]), (Color col) -> {
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/offbalance", OptWnd.greenCombatColorOptionWidget.currentColor);
		}){}, rightColumn.pos("bl").adds(6, 0));
		rightColumn = panel.add(new Label("Blue"), rightColumn.pos("ur").adds(8, 0));
		panel.add(OptWnd.blueCombatColorOptionWidget = new ColorOptionWidget("", "blueCombat", 0,
				Integer.parseInt(OptWnd.blueCombatColorSetting[0]), Integer.parseInt(OptWnd.blueCombatColorSetting[1]), Integer.parseInt(OptWnd.blueCombatColorSetting[2]), Integer.parseInt(OptWnd.blueCombatColorSetting[3]), (Color col) -> {
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/dizzy", OptWnd.blueCombatColorOptionWidget.currentColor);
		}){}, rightColumn.pos("bl").adds(2, 0));
		rightColumn = panel.add(new Label("Yellow"), rightColumn.pos("ur").adds(6, 0));
		panel.add(OptWnd.yellowCombatColorOptionWidget = new ColorOptionWidget("", "yellowCombat", 0,
				Integer.parseInt(OptWnd.yellowCombatColorSetting[0]), Integer.parseInt(OptWnd.yellowCombatColorSetting[1]), Integer.parseInt(OptWnd.yellowCombatColorSetting[2]), Integer.parseInt(OptWnd.yellowCombatColorSetting[3]), (Color col) -> {
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/reeling", OptWnd.yellowCombatColorOptionWidget.currentColor);
		}){}, rightColumn.pos("bl").adds(8, 0));
		rightColumn = panel.add(new Label("Red"), rightColumn.pos("ur").adds(8, 0));
		rightColumn = panel.add(OptWnd.redCombatColorOptionWidget = new ColorOptionWidget("", "redCombat", 0,
				Integer.parseInt(OptWnd.redCombatColorSetting[0]), Integer.parseInt(OptWnd.redCombatColorSetting[1]), Integer.parseInt(OptWnd.redCombatColorSetting[2]), Integer.parseInt(OptWnd.redCombatColorSetting[3]), (Color col) -> {
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/cornered", OptWnd.redCombatColorOptionWidget.currentColor);
		}){}, rightColumn.pos("bl").adds(1, 0));
		rightColumn = panel.add(new Button(UI.scale(70), "Reset All", false).action(() -> {
			Utils.setprefsa("greenCombat" + "_colorSetting", new String[]{"0", "128", "3", "255"});
			Utils.setprefsa("blueCombat" + "_colorSetting", new String[]{"39", "82", "191", "255"});
			Utils.setprefsa("yellowCombat" + "_colorSetting", new String[]{"217", "177", "20", "255"});
			Utils.setprefsa("redCombat" + "_colorSetting", new String[]{"192", "28", "28", "255"});
			OptWnd.greenCombatColorOptionWidget.cb.colorChooser.setColor(OptWnd.greenCombatColorOptionWidget.currentColor = new Color(0, 128, 3, 255));
			OptWnd.blueCombatColorOptionWidget.cb.colorChooser.setColor(OptWnd.blueCombatColorOptionWidget.currentColor = new Color(39, 82, 191, 255));
			OptWnd.yellowCombatColorOptionWidget.cb.colorChooser.setColor(OptWnd.yellowCombatColorOptionWidget.currentColor = new Color(217, 177, 20, 255));
			OptWnd.redCombatColorOptionWidget.cb.colorChooser.setColor(OptWnd.redCombatColorOptionWidget.currentColor = new Color(192, 28, 28, 255));
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/offbalance", OptWnd.greenCombatColorOptionWidget.currentColor);
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/dizzy", OptWnd.blueCombatColorOptionWidget.currentColor);
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/reeling", OptWnd.yellowCombatColorOptionWidget.currentColor);
			OptWnd.improvedOpeningsImageColor.put("paginae/atk/cornered", OptWnd.redCombatColorOptionWidget.currentColor);
		}), rightColumn.pos("ur").adds(21, -2));
		OptWnd.improvedOpeningsImageColor.put("paginae/atk/offbalance", OptWnd.greenCombatColorOptionWidget.currentColor);
		OptWnd.improvedOpeningsImageColor.put("paginae/atk/dizzy", OptWnd.blueCombatColorOptionWidget.currentColor);
		OptWnd.improvedOpeningsImageColor.put("paginae/atk/reeling", OptWnd.yellowCombatColorOptionWidget.currentColor);
		OptWnd.improvedOpeningsImageColor.put("paginae/atk/cornered", OptWnd.redCombatColorOptionWidget.currentColor);

		rightColumn = panel.add(new Label("Combat IP (Coins) Colors:"), rightColumn.pos("bl").adds(0, 10).xs(320));
		rightColumn = panel.add(new Label("Your IP"), rightColumn.pos("bl").adds(20, 1));
		panel.add(OptWnd.myIPCombatColorOptionWidget = new ColorOptionWidget("", "myIPCombat", 0,
				Integer.parseInt(OptWnd.myIPCombatColorSetting[0]), Integer.parseInt(OptWnd.myIPCombatColorSetting[1]), Integer.parseInt(OptWnd.myIPCombatColorSetting[2]), Integer.parseInt(OptWnd.myIPCombatColorSetting[3]), (Color col) -> {
		}){}, rightColumn.pos("bl").adds(8, 0));
		rightColumn = panel.add(new Label("Enemy IP"), rightColumn.pos("ur").adds(24, 0));
		rightColumn = panel.add(OptWnd.enemyIPCombatColorOptionWidget = new ColorOptionWidget("", "enemyIPCombat", 0,
				Integer.parseInt(OptWnd.enemyIPCombatColorSetting[0]), Integer.parseInt(OptWnd.enemyIPCombatColorSetting[1]), Integer.parseInt(OptWnd.enemyIPCombatColorSetting[2]), Integer.parseInt(OptWnd.enemyIPCombatColorSetting[3]), (Color col) -> {
		}){}, rightColumn.pos("bl").adds(12, 0));

		rightColumn = panel.add(new Button(UI.scale(70), "Reset All", false).action(() -> {
			Utils.setprefsa("myIPCombat" + "_colorSetting", new String[]{"0", "201", "4", "255"});
			Utils.setprefsa("enemyIPCombat" + "_colorSetting", new String[]{"245", "0", "0", "255"});
			OptWnd.myIPCombatColorOptionWidget.cb.colorChooser.setColor(OptWnd.myIPCombatColorOptionWidget.currentColor = new Color(0, 201, 4, 255));
			OptWnd.enemyIPCombatColorOptionWidget.cb.colorChooser.setColor(OptWnd.enemyIPCombatColorOptionWidget.currentColor = new Color(245, 0, 0, 255));
		}), rightColumn.pos("ur").adds(46, -2));

		rightColumn = panel.add(OptWnd.highlightPartyMembersCheckBox = new CheckBox("Highlight Party Members"){
			{a = Utils.getprefb("highlightPartyMembers", false);}
			public void changed(boolean val) {
				Utils.setprefb("highlightPartyMembers", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
				}
			}
		}, rightColumn.pos("bl").adds(0, 16).xs(320));
		OptWnd.highlightPartyMembersCheckBox.tooltip = OptWndTooltips.highlightPartyMembers;
		rightColumn = panel.add(OptWnd.showCirclesUnderPartyMembersCheckBox = new CheckBox("Show Circles under Party Members"){
			{a = Utils.getprefb("showCirclesUnderPartyMembers", true);}
			public void changed(boolean val) {
				Utils.setprefb("showCirclesUnderPartyMembers", val);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.showCirclesUnderPartyMembersCheckBox.tooltip = OptWndTooltips.showCirclesUnderPartyMembers;

		rightColumn = panel.add(OptWnd.yourselfPartyColorOptionWidget = new ColorOptionWidget("Yourself (Party Color):", "yourselfParty", 120, Integer.parseInt(OptWnd.yourselfPartyColorSetting[0]), Integer.parseInt(OptWnd.yourselfPartyColorSetting[1]), Integer.parseInt(OptWnd.yourselfPartyColorSetting[2]), Integer.parseInt(OptWnd.yourselfPartyColorSetting[3]), (Color col) -> {
			GobPartyHighlight.YOURSELF_OL_COLOR = new MixColor(col);
			PartyCircleSprite.YOURSELF_OL_COLOR = col;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
			}
		}){}, rightColumn.pos("bl").adds(6, 2));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("yourselfParty" + "_colorSetting", new String[]{"255", "255", "255", "128"});
			OptWnd.yourselfPartyColorOptionWidget.cb.colorChooser.setColor(OptWnd.yourselfPartyColorOptionWidget.currentColor = new Color(255, 255, 255, 128));
			GobPartyHighlight.YOURSELF_OL_COLOR = new MixColor(OptWnd.yourselfPartyColorOptionWidget.currentColor);
			PartyCircleSprite.YOURSELF_OL_COLOR = OptWnd.yourselfPartyColorOptionWidget.currentColor;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
			}
		}), OptWnd.yourselfPartyColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;
		rightColumn = panel.add(OptWnd.leaderPartyColorOptionWidget = new ColorOptionWidget("Leader (Party Color):", "leaderParty", 120, Integer.parseInt(OptWnd.leaderPartyColorSetting[0]), Integer.parseInt(OptWnd.leaderPartyColorSetting[1]), Integer.parseInt(OptWnd.leaderPartyColorSetting[2]), Integer.parseInt(OptWnd.leaderPartyColorSetting[3]), (Color col) -> {
			GobPartyHighlight.LEADER_OL_COLOR = new MixColor(col);
			PartyCircleSprite.LEADER_OL_COLOR = col;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
			}
		}){}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("leaderParty" + "_colorSetting", new String[]{"0", "74", "208", "164"});
			OptWnd.leaderPartyColorOptionWidget.cb.colorChooser.setColor(OptWnd.leaderPartyColorOptionWidget.currentColor = new Color(0, 74, 208, 164));
			GobPartyHighlight.LEADER_OL_COLOR = new MixColor(OptWnd.leaderPartyColorOptionWidget.currentColor);
			PartyCircleSprite.LEADER_OL_COLOR = OptWnd.leaderPartyColorOptionWidget.currentColor;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
			}
		}), OptWnd.leaderPartyColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;
		rightColumn = panel.add(OptWnd.memberPartyColorOptionWidget = new ColorOptionWidget("Member (Party Color):", "memberParty", 120, Integer.parseInt(OptWnd.memberPartyColorSetting[0]), Integer.parseInt(OptWnd.memberPartyColorSetting[1]), Integer.parseInt(OptWnd.memberPartyColorSetting[2]), Integer.parseInt(OptWnd.memberPartyColorSetting[3]), (Color col) -> {
			GobPartyHighlight.MEMBER_OL_COLOR = new MixColor(col);
			PartyCircleSprite.MEMBER_OL_COLOR = col;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
			}
		}){}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("memberParty" + "_colorSetting", new String[]{"0", "160", "0", "164"});
			OptWnd.memberPartyColorOptionWidget.cb.colorChooser.setColor(OptWnd.memberPartyColorOptionWidget.currentColor = new Color(0, 160, 0, 164));
			GobPartyHighlight.MEMBER_OL_COLOR = new MixColor(OptWnd.memberPartyColorOptionWidget.currentColor);
			PartyCircleSprite.MEMBER_OL_COLOR = OptWnd.memberPartyColorOptionWidget.currentColor;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
			}
		}), OptWnd.memberPartyColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;

		rightColumn = panel.add(OptWnd.highlightCombatFoesCheckBox = new CheckBox("Highlight Combat Foes"){
			{a = Utils.getprefb("highlightCombatFoes", false);}
			public void changed(boolean val) {
				Utils.setprefb("highlightCombatFoes", val);
			}
		}, rightColumn.pos("bl").adds(0, 12).xs(320));
		OptWnd.highlightCombatFoesCheckBox.tooltip = OptWndTooltips.highlightCombatFoes;
		rightColumn = panel.add(OptWnd.showCirclesUnderCombatFoesCheckBox = new CheckBox("Show Circles under Combat Foes"){
			{a = Utils.getprefb("showCirclesUnderCombatFoes", true);}
			public void changed(boolean val) {
				Utils.setprefb("showCirclesUnderCombatFoes", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.showCirclesUnderCombatFoesCheckBox.tooltip = OptWndTooltips.showCirclesUnderCombatFoes;

		rightColumn = panel.add(OptWnd.combatFoeColorOptionWidget = new ColorOptionWidget("Combat Foes:", "combatFoes", 120, Integer.parseInt(OptWnd.combatFoeColorSetting[0]), Integer.parseInt(OptWnd.combatFoeColorSetting[1]), Integer.parseInt(OptWnd.combatFoeColorSetting[2]), Integer.parseInt(OptWnd.combatFoeColorSetting[3]), (Color col) -> {
			GobCombatHighlight.COMBAT_FOE_MIXCOLOR = new MixColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
			AggroCircleSprite.COMBAT_FOE_COLOR = col;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::removeCombatFoeCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::removeCombatFoeHighlight);
			}
			haven.sprites.CurrentAggroSprite.col = new BaseColor(col);
			OptWnd.refreshCurrentTargetSpriteColor = true;
		}){}, rightColumn.pos("bl").adds(6, 2));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("combatFoes" + "_colorSetting", new String[]{"180", "0", "0", "196"});
			OptWnd.combatFoeColorOptionWidget.cb.colorChooser.setColor(OptWnd.combatFoeColorOptionWidget.currentColor = new Color(180, 0, 0, 196));
			GobCombatHighlight.COMBAT_FOE_MIXCOLOR = new MixColor(OptWnd.combatFoeColorOptionWidget.currentColor.getRed(), OptWnd.combatFoeColorOptionWidget.currentColor.getGreen(), OptWnd.combatFoeColorOptionWidget.currentColor.getBlue(), OptWnd.combatFoeColorOptionWidget.currentColor.getAlpha());
			AggroCircleSprite.COMBAT_FOE_COLOR = OptWnd.combatFoeColorOptionWidget.currentColor;
			if (panel.ui != null && panel.ui.gui != null) {
				panel.ui.sess.glob.oc.gobAction(Gob::removeCombatFoeCircleOverlay);
				panel.ui.sess.glob.oc.gobAction(Gob::removeCombatFoeHighlight);
			}
			haven.sprites.CurrentAggroSprite.col = new BaseColor(OptWnd.combatFoeColorOptionWidget.currentColor);
			OptWnd.refreshCurrentTargetSpriteColor = true;
		}), OptWnd.combatFoeColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;

		rightColumn = panel.add(new Label("Target Sprite Size:"), rightColumn.pos("bl").adds(0, 4).xs(325));
		rightColumn.tooltip = OptWndTooltips.targetSprite;
		rightColumn = panel.add(OptWnd.targetSpriteSizeSlider = new HSlider(UI.scale(110), 3, 7, Utils.getprefi("targetSpriteSize", 5)) {
			public void changed() {
				Utils.setprefi("targetSpriteSize", val);
				haven.sprites.CurrentAggroSprite.size = val;
				OptWnd.refreshCurrentTargetSpriteColor = true;
			}
		}, rightColumn.pos("ur").adds(26, 4));
		OptWnd.targetSpriteSizeSlider.tooltip = OptWndTooltips.targetSprite;

		rightColumn = panel.add(OptWnd.drawChaseVectorsCheckBox = new CheckBox("Draw Chase Vectors"){
			{a = Utils.getprefb("drawChaseVectors", true);}
			public void changed(boolean val) {
				Utils.setprefb("drawChaseVectors", val);
			}
		}, rightColumn.pos("bl").adds(0, 12).xs(320));
		OptWnd.drawChaseVectorsCheckBox.tooltip = OptWndTooltips.drawChaseVectors;
		rightColumn = panel.add(OptWnd.drawYourCurrentPathCheckBox = new CheckBox("Draw Your Current Path"){
			{a = Utils.getprefb("drawYourCurrentPath", false);}
			public void changed(boolean val) {
				Utils.setprefb("drawYourCurrentPath", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		OptWnd.drawYourCurrentPathCheckBox.tooltip = OptWndTooltips.drawYourCurrentPath;
		rightColumn = panel.add(OptWnd.yourselfVectorColorOptionWidget = new ColorOptionWidget("Yourself (Vector Color):", "yourselfVector", 120, Integer.parseInt(OptWnd.yourselfVectorColorSetting[0]), Integer.parseInt(OptWnd.yourselfVectorColorSetting[1]), Integer.parseInt(OptWnd.yourselfVectorColorSetting[2]), Integer.parseInt(OptWnd.yourselfVectorColorSetting[3]), (Color col) -> {
			ChaseVectorSprite.YOURCOLOR = col;
		}){}, rightColumn.pos("bl").adds(6, 2));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("yourselfVector" + "_colorSetting", new String[]{"255", "255", "255", "255"});
			OptWnd.yourselfVectorColorOptionWidget.cb.colorChooser.setColor(OptWnd.yourselfVectorColorOptionWidget.currentColor = new Color(255, 255, 255, 255));
			ChaseVectorSprite.YOURCOLOR = OptWnd.yourselfVectorColorOptionWidget.currentColor;
		}), OptWnd.yourselfVectorColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;
		rightColumn = panel.add(OptWnd.friendVectorColorOptionWidget = new ColorOptionWidget("Friend (Vector Color):", "friendVector", 120, Integer.parseInt(OptWnd.friendVectorColorSetting[0]), Integer.parseInt(OptWnd.friendVectorColorSetting[1]), Integer.parseInt(OptWnd.friendVectorColorSetting[2]), Integer.parseInt(OptWnd.friendVectorColorSetting[3]), (Color col) -> {
			ChaseVectorSprite.FRIENDCOLOR = col;
		}){}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("friendVector" + "_colorSetting", new String[]{"47", "191", "7", "255"});
			OptWnd.friendVectorColorOptionWidget.cb.colorChooser.setColor(OptWnd.friendVectorColorOptionWidget.currentColor = new Color(47, 191, 7, 255));
			ChaseVectorSprite.FRIENDCOLOR = OptWnd.friendVectorColorOptionWidget.currentColor;
		}), OptWnd.friendVectorColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;

		rightColumn = panel.add(OptWnd.enemyVectorColorOptionWidget = new ColorOptionWidget("Enemy (Vector Color):", "enemyVector", 120, Integer.parseInt(OptWnd.enemyVectorColorSetting[0]), Integer.parseInt(OptWnd.enemyVectorColorSetting[1]), Integer.parseInt(OptWnd.enemyVectorColorSetting[2]), Integer.parseInt(OptWnd.enemyVectorColorSetting[3]), (Color col) -> {
			ChaseVectorSprite.ENEMYCOLOR = col;
		}){}, rightColumn.pos("bl").adds(0, 4));
		panel.add(new Button(UI.scale(70), "Reset", false).action(() -> {
			Utils.setprefsa("enemyVector" + "_colorSetting", new String[]{"255", "0", "0", "255"});
			OptWnd.enemyVectorColorOptionWidget.cb.colorChooser.setColor(OptWnd.enemyVectorColorOptionWidget.currentColor = new Color(255, 0, 0, 255));
			ChaseVectorSprite.ENEMYCOLOR = OptWnd.enemyVectorColorOptionWidget.currentColor;
		}), OptWnd.enemyVectorColorOptionWidget.pos("ur").adds(16, 0)).tooltip = OptWndTooltips.resetButton;



		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 33).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
