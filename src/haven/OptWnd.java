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
		Widget prev;
		int marg = UI.scale(5);
		prev = add(new CheckBox("Render shadows") {
			{a = prefs.lshadow.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.lshadow, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, Coord.z);
		prev = add(new Label("Render scale"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int steps = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), -2 * steps, 1 * steps, (int)Math.round(steps * Math.log(prefs.rscale.val) / Math.log(2.0f))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", Math.pow(2, this.val / (double)steps)));
			       }
			       public void changed() {
				   try {
				       float val = (float)Math.pow(2, this.val / (double)steps);
				       ui.setgprefs(prefs = prefs.update(null, prefs.rscale, val));
					   if(ui.gui != null && ui.gui.map != null) {ui.gui.map.updateGridMat();}
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new CheckBox("Vertical sync") {
			{a = prefs.vsync.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.vsync, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, prev.pos("bl").adds(0, 5));
		prev = add(new Label("Framerate limit (active window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 20, max, (prefs.hz.val == Float.POSITIVE_INFINITY) ? max : prefs.hz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.hz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Framerate limit (background window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 20, max, (prefs.bghz.val == Float.POSITIVE_INFINITY) ? max : prefs.bghz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.bghz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Lighting mode"), prev.pos("bl").adds(0, 5));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs
						 .update(null, prefs.lightmode, GSettings.LightMode.values()[btn])
						 .update(null, prefs.maxlights, 0));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				resetcf();
			    }
			};
		    prev = grp.add("Global", prev.pos("bl").adds(5, 2));
		    prev.settip("Global lighting supports fewer light sources, and scales worse in " +
				"performance per additional light source, than zoned lighting, but " +
				"has lower baseline performance requirements.", true);
		    prev = grp.add("Zoned", prev.pos("bl").adds(0, 2));
		    prev.settip("Zoned lighting supports far more light sources than global " +
				"lighting with better performance, but may have higher performance " +
				"requirements in cases with few light sources, and may also have " +
				"issues on old graphics hardware.", true);
		    grp.check(prefs.lightmode.val.ordinal());
		    done[0] = true;
		}
		prev = add(new Label("Light-source limit"), prev.pos("bl").adds(0, 5).x(0));
		{
		    Label dpy = new Label("");
		    int val = prefs.maxlights.val, max = 32;
		    if(val == 0) {    /* XXX: This is just ugly. */
			if(prefs.lightmode.val == GSettings.LightMode.ZONED)
			    val = Lighting.LightGrid.defmax;
			else
			    val = Lighting.SimpleLights.defmax;
		    }
		    if(prefs.lightmode.val == GSettings.LightMode.SIMPLE)
			max = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, val / 4) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(Integer.toString(this.val * 4));
			       }
			       public void changed() {dpy();}
			       public void fchanged() {
				   try {
				       ui.setgprefs(prefs = prefs.update(null, prefs.maxlights, this.val * 4));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			       {
				   settip("The light-source limit means different things depending on the " +
					  "selected lighting mode. For Global lighting, it limits the total "+
					  "number of light-sources globally. For Zoned lighting, it limits the " +
					  "total number of overlapping light-sources at any point in space.",
					  true);
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Frame sync mode"), prev.pos("bl").adds(0, 5).x(0));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs.update(null, prefs.syncmode, JOGLPanel.SyncMode.values()[btn]));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
			    }
			};
		    prev = add(new Label("\u2191 Better performance, worse latency"), prev.pos("bl").adds(5, 2));
		    prev = grp.add("One-frame overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("Tick overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("CPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = grp.add("GPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = add(new Label("\u2193 Worse performance, better latency"), prev.pos("bl").adds(0, 2));
		    grp.check(prefs.syncmode.val.ordinal());
		    done[0] = true;
		}
		/* XXXRENDER
		composer.add(new CheckBox("Antialiasing") {
			{a = cf.fsaa.val;}

			public void set(boolean val) {
			    try {
				cf.fsaa.set(val);
			    } catch(GLSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			    cf.dirty = true;
			}
		    });
		composer.add(new Label("Anisotropic filtering"));
		if(cf.anisotex.max() <= 1) {
		    composer.add(new Label("(Not supported)"));
		} else {
		    final Label dpy = new Label("");
		    composer.addRow(
			    new HSlider(UI.scale(160), (int)(cf.anisotex.min() * 2), (int)(cf.anisotex.max() * 2), (int)(cf.anisotex.val * 2)) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(val < 2)
				    dpy.settext("Off");
				else
				    dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
			    }
			    public void changed() {
				try {
				    cf.anisotex.set(val / 2.0f);
				} catch(GLSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
				cf.dirty = true;
			    }
			},
			dpy
		    );
		}
		*/
		add(new Button(UI.scale(200), "Reset to defaults", false).action(() -> {
			    ui.setgprefs(GSettings.defaults());
			    curcf.destroy();
			    curcf = null;
		}), prev.pos("bl").adds(-5, 5));
		pack();
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
	private static CheckBox showFramerateCheckBox;
	public static CheckBox snapWindowsBackInsideCheckBox;
	public static CheckBox dragWindowsInWhenResizingCheckBox;
	public static CheckBox showHoverInventoriesWhenHoldingShiftCheckBox;
	public static CheckBox showQuickSlotsCheckBox;
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
	    Widget leftColumn = add(new Label("Interface scale (requires restart)"), 0, 0);
		leftColumn.tooltip = OptWndTooltips.interfaceScale;
	    {
		Label dpy = new Label("");
		final double gran = 0.05;
		final double smin = 1, smax = Math.floor(UI.maxscale() / gran) * gran;
		final int steps = (int)Math.round((smax - smin) / gran);
		addhlp(leftColumn.pos("bl").adds(0, 4), UI.scale(5),
		       leftColumn = new HSlider(UI.scale(160), 0, steps, (int)Math.round(steps * (Utils.getprefd("uiscale", 1.0) - smin) / (smax - smin))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", smin + (((double)this.val / steps) * (smax - smin))));
			       }
			       public void changed() {
				   double val = smin + (((double)this.val / steps) * (smax - smin));
				   Utils.setprefd("uiscale", val);
				   dpy();
			       }
			   },
		       dpy);
		leftColumn.tooltip = OptWndTooltips.interfaceScale;
	    }
		leftColumn = add(showFramerateCheckBox = new CheckBox("Show Framerate"){
			{a = (Utils.getprefb("showFramerate", true));}
			public void changed(boolean val) {
				GLPanel.Loop.showFramerate = val;
				Utils.setprefb("showFramerate", val);
			}
		}, leftColumn.pos("bl").adds(0, 18));
		showFramerateCheckBox.tooltip = OptWndTooltips.showFramerate;
		leftColumn = add(snapWindowsBackInsideCheckBox = new CheckBox("Snap windows back when dragged out"){
			{a = (Utils.getprefb("snapWindowsBackInside", true));}
			public void changed(boolean val) {
				Utils.setprefb("snapWindowsBackInside", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		snapWindowsBackInsideCheckBox.tooltip = OptWndTooltips.snapWindowsBackInside;
		leftColumn = add(dragWindowsInWhenResizingCheckBox = new CheckBox("Drag windows in when resizing game"){
			{a = (Utils.getprefb("dragWindowsInWhenResizing", false));}
			public void changed(boolean val) {
				Utils.setprefb("dragWindowsInWhenResizing", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		dragWindowsInWhenResizingCheckBox.tooltip = OptWndTooltips.dragWindowsInWhenResizing;
		leftColumn = add(showHoverInventoriesWhenHoldingShiftCheckBox = new CheckBox("Show Hover-Inventories (Stacks, Belt, etc.) only when holding Shift"){
			{a = (Utils.getprefb("showHoverInventoriesWhenHoldingShift", true));}
			public void changed(boolean val) {
				Utils.setprefb("showHoverInventoriesWhenHoldingShift", val);
			}
		}, leftColumn.pos("bl").adds(0, 12));
		leftColumn = add(showQuickSlotsCheckBox = new CheckBox("Enable Quick Slots Widget:"){
			{a = (Utils.getprefb("showQuickSlotsBar", true));}
			public void changed(boolean val) {
				Utils.setprefb("showQuickSlotsBar", val);
				if (ui != null && ui.gui != null && ui.gui.quickslots != null){
					ui.gui.quickslots.show(val);
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		showQuickSlotsCheckBox.tooltip = OptWndTooltips.showQuickSlots;
		leftColumn = add(new Label("> Show:"), leftColumn.pos("bl").adds(0, 1).xs(0));
		leftColumn = add(leftHandQuickSlotCheckBox = new CheckBox("Left Hand"){
			{a = Utils.getprefb("leftHandQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("leftHandQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(4, 0));
		add(rightHandQuickSlotCheckBox = new CheckBox("Right Hand"){
			{a = Utils.getprefb("rightHandQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("rightHandQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(10, 0));

		leftColumn = add(leftPouchQuickSlotCheckBox = new CheckBox("Left Pouch"){
			{a = Utils.getprefb("leftPouchQuickSlot", false);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("leftPouchQuickSlot", val);
			}
		}, leftColumn.pos("bl").adds(0, 1));
		add(rightPouchQuickSlotCheckBox = new CheckBox("Right Pouch"){
			{a = Utils.getprefb("rightPouchQuickSlot", false);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("rightPouchQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(4, 0));

		leftColumn = add(beltQuickSlotCheckBox = new CheckBox("Belt"){
			{a = Utils.getprefb("beltQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("beltQuickSlot", val);
			}
		}, leftColumn.pos("bl").adds(0, 1));
		add(backpackQuickSlotCheckBox = new CheckBox("Backpack"){
			{a = Utils.getprefb("backpackQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("backpackQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(37, 0));
        leftColumn = add(shouldersQuickSlotCheckBox = new CheckBox("Shoulders"){
            {a = Utils.getprefb("shouldersQuickSlot", true);}
            public void changed(boolean val) {
                ui.gui.quickslots.reloadSlots();
                Utils.setprefb("shouldersQuickSlot", val);
            }
        }, leftColumn.pos("bl").adds(0, 1));
        leftColumn = add(capeQuickSlotCheckBox = new CheckBox("Cape"){
			{a = Utils.getprefb("capeQuickSlot", true);}
			public void changed(boolean val) {
				ui.gui.quickslots.reloadSlots();
				Utils.setprefb("capeQuickSlot", val);
			}
		}, leftColumn.pos("ur").adds(9, 0));


		leftColumn = add(showStudyReportHistoryCheckBox = new CheckBox("Show Study Report History"){
			{a = (Utils.getprefb("showStudyReportHistory", true));}
			public void set(boolean val) {
				SAttrWnd.showStudyReportHistoryCheckBox.a = val;
				Utils.setprefb("showStudyReportHistory", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12).xs(0));
		showStudyReportHistoryCheckBox.tooltip = OptWndTooltips.showStudyReportHistory;
		leftColumn = add(lockStudyReportCheckBox = new CheckBox("Lock Study Report"){
			{a = (Utils.getprefb("lockStudyReport", false));}
			public void set(boolean val) {
				SAttrWnd.lockStudyReportCheckBox.a = val;
				Utils.setprefb("lockStudyReport", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));
		lockStudyReportCheckBox.tooltip = OptWndTooltips.lockStudyReport;
		leftColumn = add(soundAlertForFinishedCuriositiesCheckBox = new CheckBox("Sound Alert for Finished Curiosities"){
			{a = (Utils.getprefb("soundAlertForFinishedCuriosities", false));}
			public void set(boolean val) {
				SAttrWnd.soundAlertForFinishedCuriositiesCheckBox.a = val;
				Utils.setprefb("soundAlertForFinishedCuriosities", val);
				a = val;
				if (val) {
					try {
						File file = new File(haven.MainFrame.gameDir + "res/customclient/sfx/CurioFinished.wav");
						if (file.exists()) {
							AudioInputStream in = AudioSystem.getAudioInputStream(file);
							AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
							AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
							Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
							((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 0.8));
						}
					} catch (Exception e) {
					}
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		soundAlertForFinishedCuriositiesCheckBox.tooltip = OptWndTooltips.soundAlertForFinishedCuriosities;

		leftColumn = add(alwaysOpenMiniStudyOnLoginCheckBox = new CheckBox("Always Open Mini-Study on Login"){
			{a = (Utils.getprefb("alwaysOpenMiniStudyOnLogin", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysOpenMiniStudyOnLogin", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(alwaysShowCombatUIStaminaBarCheckBox = new CheckBox("Always Show Combat UI Stamina Bar"){
			{a = (Utils.getprefb("alwaysShowCombatUIStaminaBar", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysShowCombatUIStaminaBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 12));
		alwaysShowCombatUIStaminaBarCheckBox.tooltip = OptWndTooltips.alwaysShowCombatUiBar;
		leftColumn = add(alwaysShowCombatUIHealthBarCheckBox = new CheckBox("Always Show Combat UI Health Bar"){
			{a = (Utils.getprefb("alwaysShowCombatUIHealthBar", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysShowCombatUIHealthBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		alwaysShowCombatUIHealthBarCheckBox.tooltip = OptWndTooltips.alwaysShowCombatUiBar;

		leftColumn = add(transparentQuestsObjectivesWindowCheckBox = new CheckBox("Transparent Quests Objectives Window"){
			{a = (Utils.getprefb("transparentQuestsObjectivesWindow", false));}
			public void changed(boolean val) {
				Utils.setprefb("transparentQuestsObjectivesWindow", val);
				if (ui != null && ui.gui != null && ui.gui.questObjectivesWindow != null && ui.gui.questObjectivesWindow.visible()) {
					ui.gui.questObjectivesWindow.resetDeco();
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		transparentQuestsObjectivesWindowCheckBox.tooltip = OptWndTooltips.transparentQuestsObjectivesWindow;

		Widget rightColumn;
        rightColumn = add(new Label("UI Theme (Req. Restart):"), UI.scale(230, 2));
        List<String> uiThemes = Arrays.asList("Nightdawg Dark", "Trollex Red", "Trollex Blue", "Custom Theme");
        Widget uiThemesWdg = add(new OldDropBox<String>(uiThemes.size(), uiThemes) {
            {
                super.change(uiThemes.get(Utils.getprefi("uiThemeDropBox", 0)));
            }
            @Override
            protected String listitem(int i) {
                return uiThemes.get(i);
            }
            @Override
            protected int listitems() {
                return uiThemes.size();
            }
            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
            }
            @Override
            public void change(String item) {
                super.change(item);
                for (int i = 0; i < uiThemes.size(); i++){
                    if (item.equals(uiThemes.get(i))){
                        Utils.setprefi("uiThemeDropBox", i);
                        Utils.setpref("uiThemeName", uiThemes.get(i));
                    }
                }
            }
        }, rightColumn.pos("ur").adds(2, 0));
        uiThemesWdg.tooltip = OptWndTooltips.uiTheme;
        rightColumn.tooltip = OptWndTooltips.uiTheme;

		rightColumn = add(extendedMouseoverInfoCheckBox = new CheckBox("Extended Mouseover Info (Dev)"){
			{a = (Utils.getprefb("extendedMouseoverInfo", false));}
			public void changed(boolean val) {
				Utils.setprefb("extendedMouseoverInfo", val);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		extendedMouseoverInfoCheckBox.tooltip = OptWndTooltips.extendedMouseoverInfo;
		rightColumn = add(disableMenuGridHotkeysCheckBox = new CheckBox("Disable All Menu Grid Hotkeys"){
			{a = (Utils.getprefb("disableMenuGridHotkeys", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableMenuGridHotkeys", val);
			}
		}, rightColumn.pos("bl").adds(0, 15));
		disableMenuGridHotkeysCheckBox.tooltip = OptWndTooltips.disableMenuGridHotkeys;
		rightColumn = add(alwaysOpenBeltOnLoginCheckBox = new CheckBox("Always Open Belt on Login"){
			{a = (Utils.getprefb("alwaysOpenBeltOnLogin", true));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysOpenBeltOnLogin", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		alwaysOpenBeltOnLoginCheckBox.tooltip = OptWndTooltips.alwaysOpenBeltOnLogin;
		rightColumn = add(showMapMarkerNamesCheckBox = new CheckBox("Show Map Marker Names"){
			{a = (Utils.getprefb("showMapMarkerNames", true));}
			public void changed(boolean val) {
				Utils.setprefb("showMapMarkerNames", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		showMapMarkerNamesCheckBox.tooltip = OptWndTooltips.showMapMarkerNames;
		rightColumn = add(verticalContainerIndicatorsCheckBox = new CheckBox("Vertical Container Indicators"){
			{a = (Utils.getprefb("verticalContainerIndicators", true));}
			public void changed(boolean val) {
				Utils.setprefb("verticalContainerIndicators", val);
			}
		}, rightColumn.pos("bl").adds(0, 32));
		verticalContainerIndicatorsCheckBox.tooltip = OptWndTooltips.verticalContainerIndicators;
		Label expWindowLocationLabel;
		rightColumn = add(expWindowLocationLabel = new Label("Experience Event Window Location:"), rightColumn.pos("bl").adds(0, 11));{
			RadioGroup expWindowGrp = new RadioGroup(this) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setprefb("expWindowLocationIsTop", true);
							expWindowLocationIsTop = true;
						}
						if(btn==1) {
							Utils.setprefb("expWindowLocationIsTop", false);
							expWindowLocationIsTop = false;
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			rightColumn = expWindowGrp.add("Top", rightColumn.pos("bl").adds(26, 3));
			rightColumn = expWindowGrp.add("Bottom", rightColumn.pos("ur").adds(30, 0));
			if (Utils.getprefb("expWindowLocationIsTop", true)){
				expWindowGrp.check(0);
			} else {
				expWindowGrp.check(1);
			}
		}
		expWindowLocationLabel.tooltip = OptWndTooltips.experienceWindowLocation;

		rightColumn = add(new Label("Map Window Zoom Speed:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(230)));
		rightColumn = add(mapZoomSpeedSlider = new HSlider(UI.scale(110), 10, 50, Utils.getprefi("mapZoomSpeed", 15)) {
			public void changed() {
				Utils.setprefi("mapZoomSpeed", val);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		add(new Button(UI.scale(60), "Reset", false).action(() -> {
			mapZoomSpeedSlider.val = 15;
			Utils.setprefi("mapZoomSpeed", 15);
		}), rightColumn.pos("ur").adds(6, -4)).tooltip = OptWndTooltips.resetButton;

		rightColumn = add(new Label("Map Icons Size:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(230)));
		rightColumn = add(mapIconsSizeSlider = new HSlider(UI.scale(110), 16, 40, Utils.getprefi("mapIconsSize", 20)) {
			public void changed() {
				Utils.setprefi("mapIconsSize", val);
				GobIcon.size = UI.scale(val);
				synchronized(GobIcon.Image.cache) {
					GobIcon.Image.cache.clear();
				}
				BufferedImage buf = MiniMap.plpImg.img;
				buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
				Coord tsz;
				if(buf.getWidth() > buf.getHeight())
					tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
				else
					tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
				buf = PUtils.convolve(buf, tsz, GobIcon.filter);
				MiniMap.plp = new TexI(buf);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		add(new Button(UI.scale(60), "Reset", false).action(() -> {
			mapIconsSizeSlider.val = 20;
			GobIcon.size = UI.scale(20);
			synchronized(GobIcon.Image.cache) {
				GobIcon.Image.cache.clear();
			}
			Utils.setprefi("mapIconsSize", 20);
			BufferedImage buf = MiniMap.plpImg.img;
			buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
			Coord tsz;
			if(buf.getWidth() > buf.getHeight())
				tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
			else
				tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
			buf = PUtils.convolve(buf, tsz, GobIcon.filter);
			MiniMap.plp = new TexI(buf);
		}), rightColumn.pos("ur").adds(6, -4)).tooltip = OptWndTooltips.resetButton;
		rightColumn = add(improvedInstrumentMusicWindowCheckBox = new CheckBox("Improved Instrument Music Window"){
			{a = (Utils.getprefb("improvedInstrumentMusicWindow", true));}
			public void changed(boolean val) {
				Utils.setprefb("improvedInstrumentMusicWindow", val);
			}
		}, rightColumn.pos("bl").adds(0, 15));
		improvedInstrumentMusicWindowCheckBox.tooltip = OptWndTooltips.improvedInstrumentMusicWindow;
        rightColumn = add(preventEscKeyFromClosingWindowsCheckBox = new CheckBox("Prevent ESC from closing Windows"){
            {a = (Utils.getprefb("preventEscKeyFromClosingWindows", false));}
            public void changed(boolean val) {
                Utils.setprefb("preventEscKeyFromClosingWindows", val);
            }
        }, rightColumn.pos("bl").adds(0, 2));
        rightColumn = add(stackWindowsWhenOpenedCheckBox = new CheckBox("Stack Windows when Opened"){
            {a = (Utils.getprefb("stackWindowsWhenOpened", false));}
            public void changed(boolean val) {
                Utils.setprefb("stackWindowsWhenOpened", val);
            }
        }, rightColumn.pos("bl").adds(0, 2));
        stackWindowsWhenOpenedCheckBox.tooltip = OptWndTooltips.stackWindowsWhenOpened;

		Widget backButton;
		add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 30).x(0));
	    pack();
		centerBackButton(backButton, this);
	}
    }

	public static CheckBox holdCTRLtoRemoveActionButtonsCheckBox;

	public class ActionBarsSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public ActionBarsSettingsPanel(Panel back) {
			Widget prev;
            prev = add(new Label("You can move the bars with Middle Mouse Button (scroll click)."), 0, 4);
			prev = add(new Label("Enabled Action Bars:"), prev.pos("bl").adds(0, 12));
			add(new Label("Action Bar Orientation:"), prev.pos("ur").adds(42, 0));
			prev = add(new CheckBox("Action Bar 1"){
				{a = Utils.getprefb("showActionBar1", true);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar1", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar1 != null){
						ui.gui.actionBar1.show(val);
					}
				}
			}, prev.pos("bl").adds(12, 6));
			addOrientationRadio(prev, "actionBar1Horizontal", 1);

			prev = add(new CheckBox("Action Bar 2"){
				{a = Utils.getprefb("showActionBar2", false);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar2", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar2 != null){
						ui.gui.actionBar2.show(val);
					}
				}
			}, prev.pos("bl").adds(0, 2));
			addOrientationRadio(prev, "actionBar2Horizontal", 2);

			prev = add(new CheckBox("Action Bar 3"){
				{a = Utils.getprefb("showActionBar3", false);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar3", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar3 != null){
						ui.gui.actionBar3.show(val);
					}
				}
			}, prev.pos("bl").adds(0, 2));
			addOrientationRadio(prev, "actionBar3Horizontal", 3);

			prev = add(new CheckBox("Action Bar 4"){
				{a = Utils.getprefb("showActionBar4", false);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar4", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar4 != null){
						ui.gui.actionBar4.show(val);
					}
				}
			}, prev.pos("bl").adds(0, 2));
			addOrientationRadio(prev, "actionBar4Horizontal", 4);

            prev = add(new CheckBox("Action Bar 5"){
                {a = Utils.getprefb("showActionBar5", false);}
                public void changed(boolean val) {
                    Utils.setprefb("showActionBar5", val);
                    if (ui != null && ui.gui != null && ui.gui.actionBar5 != null){
                        ui.gui.actionBar5.show(val);
                    }
                }
            }, prev.pos("bl").adds(0, 2));
            addOrientationRadio(prev, "actionBar5Horizontal", 5);

            prev = add(new CheckBox("Action Bar 6"){
                {a = Utils.getprefb("showActionBar6", false);}
                public void changed(boolean val) {
                    Utils.setprefb("showActionBar6", val);
                    if (ui != null && ui.gui != null && ui.gui.actionBar6 != null){
                        ui.gui.actionBar6.show(val);
                    }
                }
            }, prev.pos("bl").adds(0, 2));
            addOrientationRadio(prev, "actionBar6Horizontal", 6);

			prev = add(holdCTRLtoRemoveActionButtonsCheckBox = new CheckBox("Hold CTRL when right-clicking to remove action buttons"){
				{a = (Utils.getprefb("holdCTRLtoRemoveActionButtons", false));}
				public void changed(boolean val) {
					Utils.setprefb("holdCTRLtoRemoveActionButtons", val);
				}
			}, prev.pos("bl").adds(0, 12).x(0));

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(290, 380))), prev.pos("bl").adds(0,10).x(0));
			Widget cont = scroll.cont;
			int y = 0;
			y = cont.adda(new Label("Action Bar 1 Keybinds"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar1.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar1[i], y);
			y = cont.adda(new Label("Action Bar 2 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar2.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar2[i], y);
			y = cont.adda(new Label("Action Bar 3 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar3.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar3[i], y);
			y = cont.adda(new Label("Action Bar 4 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar4.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar4[i], y);
            y = cont.adda(new Label("Action Bar 5 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
            for (int i = 0; i < GameUI.kb_actbar5.length; i++)
                y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar5[i], y);
            y = cont.adda(new Label("Action Bar 6 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
            for (int i = 0; i < GameUI.kb_actbar6.length; i++)
                y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar6[i], y);
			adda(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
			pack();
		}

		private void addOrientationRadio(Widget prev, String prefName, int actionBarNumber){
			RadioGroup radioGroup = new RadioGroup(this) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setprefb(prefName, true);
							if (ui != null && ui.gui != null){
								GameUI.ActionBar actionBar = ui.gui.getActionBar(actionBarNumber);
								actionBar.setActionBarHorizontal(true);
							}
						}
						if(btn==1) {
							Utils.setprefb(prefName, false);
							if (ui != null && ui.gui != null){
								GameUI.ActionBar actionBar = ui.gui.getActionBar(actionBarNumber);
								actionBar.setActionBarHorizontal(false);
							}
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			Widget prevOption = radioGroup.add("Horizontal", prev.pos("ur").adds(40, 0));
			radioGroup.add("Vertical", prevOption.pos("ur").adds(10, 0));
			if (Utils.getprefb(prefName, true)){
				radioGroup.check(0);
			} else {
				radioGroup.check(1);
			}
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
			Widget prev;
			prev = add(new Label("Manually attacking will still work, regardless of these settings!"), 0, 0);
			prev = add(new Label("Select which Players should be excluded from Aggro Keybinds:"), prev.pos("bl").adds(0, 4));

			prev = add(excludeGreenBuddyFromAggroCheckBox = new CheckBox("Green Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeGreenBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeGreenBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 12));
			excludeGreenBuddyFromAggroCheckBox.lbl = Text.create("Green Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Green Memorised / Kinned Players", BuddyWnd.gc[1])));
			prev = add(excludeRedBuddyFromAggroCheckBox = new CheckBox("Red Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeRedBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeRedBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeRedBuddyFromAggroCheckBox.lbl = Text.create("Red Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Red Memorised / Kinned Players", BuddyWnd.gc[2])));
			prev = add(excludeBlueBuddyFromAggroCheckBox = new CheckBox("Blue Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeBlueBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeBlueBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeBlueBuddyFromAggroCheckBox.lbl = Text.create("Blue Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Blue Memorised / Kinned Players", BuddyWnd.gc[3])));
			prev = add(excludeTealBuddyFromAggroCheckBox = new CheckBox("Teal Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeTealBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeTealBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeTealBuddyFromAggroCheckBox.lbl = Text.create("Teal Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Teal Memorised / Kinned Players", BuddyWnd.gc[4])));
			prev = add(excludeYellowBuddyFromAggroCheckBox = new CheckBox("Yellow Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeYellowBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeYellowBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeYellowBuddyFromAggroCheckBox.lbl = Text.create("Yellow Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Yellow Memorised / Kinned Players", BuddyWnd.gc[5])));
			prev = add(excludePurpleBuddyFromAggroCheckBox = new CheckBox("Purple Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludePurpleBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludePurpleBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludePurpleBuddyFromAggroCheckBox.lbl = Text.create("Purple Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Purple Memorised / Kinned Players", BuddyWnd.gc[6])));
			prev = add(excludeOrangeBuddyFromAggroCheckBox = new CheckBox("Orange Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeOrangeBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeOrangeBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeOrangeBuddyFromAggroCheckBox.lbl = Text.create("Orange Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Orange Memorised / Kinned Players", BuddyWnd.gc[7])));

			prev = add(excludeAllVillageOrRealmMembersFromAggroCheckBox = new CheckBox("ALL Village & Realm Members (Regardless of Memo/Kin)"){
				{a = (Utils.getprefb("excludeAllVillageOrRealmMembersFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeAllVillageOrRealmMembersFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 20));
			excludeAllVillageOrRealmMembersFromAggroCheckBox.lbl = Text.create("ALL Village & Realm Members (Regardless of Memo/Kin)", PUtils.strokeImg(Text.std.render("ALL Village & Realm Members (Regardless of Memo/Kin)", new Color(151, 17, 17, 255))));

			prev = add(new Label("PARTY MEMBERS ARE ALWAYS EXCLUDED!"), prev.pos("bl").adds(0, 20));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
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
			Widget prev;

			prev = add(chatAlertSoundsCheckBox = new CheckBox("Enable chat message notification sounds"){
				{a = (Utils.getprefb("chatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("chatAlertSounds", val);
				}
			}, 0, 0);

			prev = add(areaChatAlertSoundsCheckBox = new CheckBox("Area Chat Sound"){
				{a = (Utils.getprefb("areaChatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("areaChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(20, 4));

			prev = add(privateChatAlertSoundsCheckBox = new CheckBox("Private Messages Sound"){
				{a = (Utils.getprefb("privateChatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("privateChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(partyChatAlertSoundsCheckBox = new CheckBox("Party Chat Sound"){
				{a = (Utils.getprefb("partyChatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("partyChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(villageChatAlertSoundsCheckBox = new CheckBox("Village Chat Sound"){
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

			prev = add(new Label("Village Name:"), prev.pos("bl").adds(20, 4));
			add(villageNameTextEntry = new TextEntry(UI.scale(100), Utils.getpref("villageNameForChatAlerts", "")){
				protected void changed() {
					Utils.setpref("villageNameForChatAlerts", this.buf.line());
					super.changed();
				}}, prev.pos("ur").adds(6, 0));

			prev = add(autoSelectNewChatCheckBox = new CheckBox("Auto-select new chats"){
				{a = (Utils.getprefb("autoSelectNewChat", true));}
				public void changed(boolean val) {
					Utils.setprefb("autoSelectNewChat", val);
				}
			}, prev.pos("bl").adds(0, 4).x(0));

			prev = add(removeRealmChatCheckBox = new CheckBox("Remove public realm chat (requires relog)"){
				{a = (Utils.getprefb("removeRealmChat", false));}
				public void changed(boolean val) {
					Utils.setprefb("removeRealmChat", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(showKinStatusChangeMessages = new CheckBox("Show kin status system messages"){
				{a = (Utils.getprefb("showKinStatusChangeMessages", true));}
				public void changed(boolean val) {
					Utils.setprefb("showKinStatusChangeMessages", val);
				}
			}, prev.pos("bl").adds(0, 4));
			prev = add(new Label("System Messages List Size: "), prev.pos("bl").adds(0, 5));
			Label systemMessagesListSizeLabel = new Label(Utils.getprefi("systemMessagesListSize", 5) + " rows");
			add(systemMessagesListSizeLabel, prev.pos("ur").adds(0, 0));
			prev = add(systemMessagesListSizeSlider = new HSlider(UI.scale(230), 1, 10, Utils.getprefi("systemMessagesListSize", 5)) {
				public void changed() {
					Utils.setprefi("systemMessagesListSize", val);
					systemMessagesListSizeLabel.settext(val + " rows");
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(new Label("System Messages Duration: "), prev.pos("bl").adds(0, 5));
			Label systemMessagesDurationLabel = new Label(Utils.getprefi("systemMessagesDuration", 4) + (Utils.getprefi("systemMessagesDuration", 5) > 1 ? " seconds" : " second"));
			add(systemMessagesDurationLabel, prev.pos("ur").adds(0, 0));
			prev = add(systemMessagesDurationSlider = new HSlider(UI.scale(230), 3, 10, Utils.getprefi("systemMessagesDuration", 4)) {
				public void changed() {
					Utils.setprefi("systemMessagesDuration", val);
					systemMessagesDurationLabel.settext(val + (val > 1 ? " seconds" : " second"));
				}
			}, prev.pos("bl").adds(0, 2));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
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
	public static CheckBox roundedQualityCheckBox;
	public static CheckBox customQualityColorsCheckBox;

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
			Widget prev;
			prev = add(showQualityDisplayCheckBox = new CheckBox("Display Quality on Inventory Items"){
				{a = (Utils.getprefb("qtoggle", true));}
				public void set(boolean val) {
					Utils.setprefb("qtoggle", val);
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					a = val;
				}
			}, 0, 6);
			prev = add(showItemCategoryBadgesCheckBox = new CheckBox("Show Item Category Badges"){
				{a = Utils.getprefb("showItemCategoryBadges", false);}
				public void changed(boolean val) {
					Utils.setprefb("showItemCategoryBadges", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(roundedQualityCheckBox = new CheckBox("Rounded Quality Number"){
				{a = (Utils.getprefb("roundedQuality", true));}
				public void changed(boolean val) {
					Utils.setprefb("roundedQuality", val);
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(customQualityColorsCheckBox = new CheckBox("Enable Custom Quality Colors:"){
				{a = (Utils.getprefb("enableCustomQualityColors", false));}
				public void changed(boolean val) {
					Utils.setprefb("enableCustomQualityColors", val);
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				}
			}, prev.pos("bl").adds(0, 12));
			prev.tooltip = OptWndTooltips.customQualityColors;

			prev = add(q7ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q7ColorTextEntry", "400")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q7ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10));
			prev = add(q7ColorOptionWidget = new ColorOptionWidget(" Godlike Quality:", "q7ColorSetting", 120, Integer.parseInt(q7ColorSetting[0]), Integer.parseInt(q7ColorSetting[1]), Integer.parseInt(q7ColorSetting[2]), Integer.parseInt(q7ColorSetting[3]), (Color col) -> {
				q7ColorOptionWidget.cb.colorChooser.setColor(q7ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q7ColorSetting_colorSetting", new String[]{"255","0","0","255"});
				q7ColorOptionWidget.cb.colorChooser.setColor(q7ColorOptionWidget.currentColor = new Color(255, 0, 0, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(q6ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q6ColorTextEntry", "300")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q6ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q6ColorOptionWidget = new ColorOptionWidget("  Legendary Quality:", "q6ColorSetting", 120, Integer.parseInt(q6ColorSetting[0]), Integer.parseInt(q6ColorSetting[1]), Integer.parseInt(q6ColorSetting[2]), Integer.parseInt(q6ColorSetting[3]), (Color col) -> {
				q6ColorOptionWidget.cb.colorChooser.setColor(q6ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q6ColorSetting_colorSetting", new String[]{"255","114","0","255"});
				q6ColorOptionWidget.cb.colorChooser.setColor(q6ColorOptionWidget.currentColor = new Color(255, 114, 0, 255));
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(q5ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q5ColorTextEntry", "200")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q5ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q5ColorOptionWidget = new ColorOptionWidget("  Epic Quality:", "q5ColorSetting", 120, Integer.parseInt(q5ColorSetting[0]), Integer.parseInt(q5ColorSetting[1]), Integer.parseInt(q5ColorSetting[2]), Integer.parseInt(q5ColorSetting[3]), (Color col) -> {
				q5ColorOptionWidget.cb.colorChooser.setColor(q5ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q5ColorSetting_colorSetting", new String[]{"165","0","255","255"});
				q5ColorOptionWidget.cb.colorChooser.setColor(q5ColorOptionWidget.currentColor = new Color(165, 0, 255, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(q4ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q4ColorTextEntry", "100")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q4ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q4ColorOptionWidget = new ColorOptionWidget("  Rare Quality:", "q4ColorSetting", 120, Integer.parseInt(q4ColorSetting[0]), Integer.parseInt(q4ColorSetting[1]), Integer.parseInt(q4ColorSetting[2]), Integer.parseInt(q4ColorSetting[3]), (Color col) -> {
				q4ColorOptionWidget.cb.colorChooser.setColor(q4ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q4ColorSetting_colorSetting", new String[]{"0","131","255","255"});
				q4ColorOptionWidget.cb.colorChooser.setColor(q4ColorOptionWidget.currentColor = new Color(0, 131, 255, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(q3ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q3ColorTextEntry", "50")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q3ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q3ColorOptionWidget = new ColorOptionWidget("  Uncommon Quality:", "q3ColorSetting", 120, Integer.parseInt(q3ColorSetting[0]), Integer.parseInt(q3ColorSetting[1]), Integer.parseInt(q3ColorSetting[2]), Integer.parseInt(q3ColorSetting[3]), (Color col) -> {
				q3ColorOptionWidget.cb.colorChooser.setColor(q3ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q3ColorSetting_colorSetting", new String[]{"0","214","10","255"});
				q3ColorOptionWidget.cb.colorChooser.setColor(q3ColorOptionWidget.currentColor = new Color(0, 214, 10, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(q2ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q2ColorTextEntry", "10")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q2ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q2ColorOptionWidget = new ColorOptionWidget("  Common Quality:", "q2ColorSetting", 120, Integer.parseInt(q2ColorSetting[0]), Integer.parseInt(q2ColorSetting[1]), Integer.parseInt(q2ColorSetting[2]), Integer.parseInt(q2ColorSetting[3]), (Color col) -> {
				q2ColorOptionWidget.cb.colorChooser.setColor(q2ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q2ColorSetting_colorSetting", new String[]{"255","255","255","255"});
				q2ColorOptionWidget.cb.colorChooser.setColor(q2ColorOptionWidget.currentColor = new Color(255, 255, 255, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(q1ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q1ColorTextEntry", "1")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q1ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q1ColorOptionWidget = new ColorOptionWidget("  Junk Quality:", "q1ColorSetting", 120, Integer.parseInt(q1ColorSetting[0]), Integer.parseInt(q1ColorSetting[1]), Integer.parseInt(q1ColorSetting[2]), Integer.parseInt(q1ColorSetting[3]), (Color col) -> {
				q1ColorOptionWidget.cb.colorChooser.setColor(q1ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q1ColorSetting_colorSetting", new String[]{"180","180","180","255"});
				q1ColorOptionWidget.cb.colorChooser.setColor(q1ColorOptionWidget.currentColor = new Color(180, 180, 180, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}
	}


    private static final Text kbtt = RichText.render("$col[255,200,0]{Escape}: Cancel input\n" +
						     "$col[255,200,0]{Backspace}: Revert to default\n" +
						     "$col[255,200,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
	private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
	    return(cont.addhl(new Coord(0, y), cont.sz.x,
			      new Label(nm), new SetButton(UI.scale(140), cmd))
		   + UI.scale(2));
	}

		private int addbtnImproved(Widget cont, String nm, String tooltip, Color color, KeyBinding cmd, int y) {
			Label theLabel = new Label(nm);
			if (tooltip != null && !tooltip.equals(""))
				theLabel.tooltip = RichText.render(tooltip, UI.scale(300));
			theLabel.setcolor(color);
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					theLabel, new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

	public BindingPanel(Panel back) {
	    super();
		int y = 5;
		Label topNote = new Label("Don't use the same keys on multiple Keybinds!");
		topNote.setcolor(Color.RED);
		y = adda(topNote, UI.scale(155), y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = adda(new Label("If you do that, only one of them will work. God knows which."), 310 / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		Scrollport scroll = add(new Scrollport(UI.scale(new Coord(310, 360))), 0, 60);
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
//	    y = addbtn(cont, "Quick chat", ChatUI.kb_quick, y);
//	    y = addbtn(cont, "Take screenshot", GameUI.kb_shoot, y);
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

		prev = adda(new PointBind(UI.scale(200)), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
	    prev = adda(new PButton(UI.scale(200), "Back", 27, back, "Options            "), prev.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
	    pack();
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
	public static CheckBox enableQueuedMovementCheckBox;
    public static CheckBox walkWithPathFinderCheckBox;
    public static CheckBox drawPathfinderRouteCheckBox;
    public static CheckBox pathfindOnMinimapCheckBox;
    public static CheckBox continuousPathfindingCheckBox;

	public class GameplayAutomationSettingsPanel extends Panel {

		public GameplayAutomationSettingsPanel(Panel back) {
			Widget prev;
			Widget rightColumn;

			Widget toggleLabel = add(new Label("Toggle on Login:"), 0, 0);
			prev = add(toggleTrackingOnLoginCheckBox = new CheckBox("Tracking"){
				{a = Utils.getprefb("toggleTrackingOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleTrackingOnLogin", val);
				}
			}, toggleLabel.pos("bl").adds(0, 6).x(UI.scale(0)));
			prev = add(toggleSwimmingOnLoginCheckBox = new CheckBox("Swimming"){
				{a = Utils.getprefb("toggleSwimmingOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleSwimmingOnLogin", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(toggleCriminalActsOnLoginCheckBox = new CheckBox("Criminal Acts"){
				{a = Utils.getprefb("toggleCriminalActsOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleCriminalActsOnLogin", val);
				}
			}, prev.pos("bl").adds(0, 2));

			rightColumn = add(toggleSiegeEnginesOnLoginCheckBox = new CheckBox("Check for Siege Engines"){
				{a = Utils.getprefb("toggleSiegeEnginesOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleSiegeEnginesOnLogin", val);
				}
			}, toggleLabel.pos("bl").adds(110, 6));
			rightColumn = add(togglePartyPermissionsOnLoginCheckBox = new CheckBox("Party Permissions"){
				{a = Utils.getprefb("togglePartyPermissionsOnLogin", false);}
				public void changed(boolean val) {
					Utils.setprefb("togglePartyPermissionsOnLogin", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			rightColumn = add(toggleItemStackingOnLoginCheckBox = new CheckBox("Automatic Item Stacking"){
				{a = Utils.getprefb("toggleItemStackingOnLogin", false);}
				public void changed(boolean val) {
					Utils.setprefb("toggleItemStackingOnLogin", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));

			prev = add(new Label("Default Speed on Login:"), prev.pos("bl").adds(0, 16).x(0));
			List<String> runSpeeds = Arrays.asList("Crawl", "Walk", "Run", "Sprint");
			add(new OldDropBox<String>(runSpeeds.size(), runSpeeds) {
				{
					super.change(runSpeeds.get(Utils.getprefi("defaultSetSpeed", 2)));
				}
				@Override
				protected String listitem(int i) {
					return runSpeeds.get(i);
				}
				@Override
				protected int listitems() {
					return runSpeeds.size();
				}
				@Override
				protected void drawitem(GOut g, String item, int i) {
					g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
				}
				@Override
				public void change(String item) {
					super.change(item);
					for (int i = 0; i < runSpeeds.size(); i++){
						if (item.equals(runSpeeds.get(i))){
							Utils.setprefi("defaultSetSpeed", i);
						}
					}
				}
			}, prev.pos("bl").adds(130, -16));

			prev = add(new Label("Other gameplay automations:"), prev.pos("bl").adds(0, 14).x(0));
			prev = add(autoSelect1stFlowerMenuCheckBox = new CheckBox("Auto-Select 1st Flower-Menu Option (hold Ctrl)"){
				{a = Utils.getprefb("autoSelect1stFlowerMenu", true);}
				public void changed(boolean val) {
					Utils.setprefb("autoSelect1stFlowerMenu", val);
				}
			}, prev.pos("bl").adds(0, 6));
			autoSelect1stFlowerMenuCheckBox.tooltip = OptWndTooltips.autoSelect1stFlowerMenu;
			prev = add(autoRepeatFlowerMenuCheckBox = new CheckBox("Auto-Repeat Flower-Menu (hold Ctrl+Shift)"){
				{a = Utils.getprefb("autoRepeatFlowerMenu", true);}
				public void changed(boolean val) {
					Utils.setprefb("autoRepeatFlowerMenu", val);
				}
			}, prev.pos("bl").adds(0, 2));
			autoRepeatFlowerMenuCheckBox.tooltip = OptWndTooltips.autoRepeatFlowerMenu;
			prev = add(alsoUseContainersWithRepeaterCheckBox = new CheckBox("Also use containers with Auto-Repeat"){
				{a = Utils.getprefb("alsoUseContainersWithRepeater", true);}
				public void changed(boolean val) {
					Utils.setprefb("alsoUseContainersWithRepeater", val);
				}
			}, prev.pos("bl").adds(16, 2));
			alsoUseContainersWithRepeaterCheckBox.tooltip = OptWndTooltips.alsoUseContainersWithRepeater;
			prev = add(new Button(UI.scale(250), "Flower-Menu Auto-Select Manager", false, () -> {
				if(!flowerMenuAutoSelectManagerWindow.attached) {
					this.parent.parent.add(flowerMenuAutoSelectManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
					flowerMenuAutoSelectManagerWindow.show();
				} else {
					flowerMenuAutoSelectManagerWindow.show(!flowerMenuAutoSelectManagerWindow.visible);
				}
			}),prev.pos("bl").adds(0, 4).x(0));
			prev.tooltip = OptWndTooltips.flowerMenuAutoSelectManager;
			prev = add(autoReloadCuriositiesFromInventoryCheckBox = new CheckBox("Auto-Reload Curiosities from Inventory"){
				{a = Utils.getprefb("autoStudyFromInventory", false);}
				public void set(boolean val) {
					SAttrWnd.autoReloadCuriositiesFromInventoryCheckBox.a = val;
					Utils.setprefb("autoStudyFromInventory", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 12).x(0));
			autoReloadCuriositiesFromInventoryCheckBox.tooltip = OptWndTooltips.autoReloadCuriositiesFromInventory;
			prev = add(preventTablewareFromBreakingCheckBox = new CheckBox("Prevent Tableware from Breaking"){
				{a = Utils.getprefb("preventTablewareFromBreaking", true);}
				public void set(boolean val) {
					Utils.setprefb("preventTablewareFromBreaking", val);
					a = val;
					TableInfo.preventTablewareFromBreakingCheckBox.a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			preventTablewareFromBreakingCheckBox.tooltip = OptWndTooltips.preventTablewareFromBreaking;
			prev = add(autoDropLeechesCheckBox = new CheckBox("Auto-Drop Leeches"){
				{a = Utils.getprefb("autoDropLeeches", true);}
				public void set(boolean val) {
					Utils.setprefb("autoDropLeeches", val);
					a = val;
					Equipory.autoDropLeechesCheckBox.a = val;
					if (ui != null && ui.gui != null) {
						Equipory eq = ui.gui.getequipory();
						if (eq != null && eq.myOwnEquipory) {
							eq.checkForLeeches = true;
						}
					}
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(autoDropTicksCheckBox = new CheckBox("Auto-Drop Ticks"){
				{a = Utils.getprefb("autoDropTicks", true);}
				public void set(boolean val) {
					Utils.setprefb("autoDropTicks", val);
					a = val;
					Equipory.autoDropTicksCheckBox.a = val;
					if (ui != null && ui.gui != null) {
						Equipory eq = ui.gui.getequipory();
						if (eq != null && eq.myOwnEquipory) {
							eq.checkForTicks = true;
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoEquipBunnySlippersPlateBootsCheckBox = new CheckBox("Auto-Equip Bunny Slippers/Plate Boots"){
				{a = Utils.getprefb("autoEquipBunnySlippersPlateBoots", true);}
				public void set(boolean val) {
					Utils.setprefb("autoEquipBunnySlippersPlateBoots", val);
					if (Equipory.autoEquipBunnySlippersPlateBootsCheckBox != null)
						Equipory.autoEquipBunnySlippersPlateBootsCheckBox.a = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			autoEquipBunnySlippersPlateBootsCheckBox.tooltip = OptWndTooltips.autoEquipBunnySlippersPlateBoots;
			prev = add(new Button(UI.scale(250), "Auto-Drop Manager", false, () -> {
				if(!autoDropManagerWindow.attached) {
					this.parent.parent.add(autoDropManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
					autoDropManagerWindow.show();
				} else {
					autoDropManagerWindow.show(!autoDropManagerWindow.visible);
				}
			}),prev.pos("bl").adds(0, 12).x(0));
			prev = add(new Button(UI.scale(250), "Per-Item Auto-Drop Config", false, () -> {
				if(!itemAutoDropWindow.attached) {
					this.parent.parent.add(itemAutoDropWindow);
					itemAutoDropWindow.show();
				} else {
					itemAutoDropWindow.show(!itemAutoDropWindow.visible);
				}
			}),prev.pos("bl").adds(0, 4).x(0));

			prev = add(autoPeaceAnimalsWhenCombatStartsCheckBox = new CheckBox("Auto-Peace Animals when Combat Starts"){
				{a = Utils.getprefb("autoPeaceAnimalsWhenCombatStarts", false);}
				public void set(boolean val) {
					Utils.setprefb("autoPeaceAnimalsWhenCombatStarts", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Autopeace Animals when Combat Starts is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, prev.pos("bl").adds(0, 12));
			autoPeaceAnimalsWhenCombatStartsCheckBox.tooltip = OptWndTooltips.autoPeaceAnimalsWhenCombatStarts;
			prev = add(preventUsingRawHideWhenRidingCheckBox = new CheckBox("Prevent using Raw Hide when Riding a Horse"){
				{a = Utils.getprefb("preventUsingRawHideWhenRiding", false);}
				public void changed(boolean val) {
					Utils.setprefb("preventUsingRawHideWhenRiding", val);
				}
			}, prev.pos("bl").adds(0, 12));
			preventUsingRawHideWhenRidingCheckBox.tooltip = OptWndTooltips.preventUsingRawHideWhenRiding;
			prev = add(autoDrinkingCheckBox = new CheckBox("Auto-Drink Water below threshold:"){
				{a = Utils.getprefb("autoDrinkTeaOrWater", false);}
				public void set(boolean val) {
					Utils.setprefb("autoDrinkTeaOrWater", val);
					a = val;
					if (ui != null && ui.gui != null) {
						String threshold = "75";
						if (!autoDrinkingThresholdTextEntry.text().isEmpty()) threshold = autoDrinkingThresholdTextEntry.text();
						ui.gui.optionInfoMsg("Auto-Drinking Water is now " + (val ? "ENABLED, with a " + threshold + "% treshhold!" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, prev.pos("bl").adds(0, 12));
			autoDrinkingCheckBox.tooltip = OptWndTooltips.autoDrinking;
			add(autoDrinkingThresholdTextEntry = new TextEntry(UI.scale(40), Utils.getpref("autoDrinkingThreshold", "75")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
					this.settext(this.text().replaceAll("(?<=^.{2}).*", "")); // No more than 2 digits
					Utils.setpref("autoDrinkingThreshold", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(10, 0));

			prev = add(enableQueuedMovementCheckBox = new CheckBox("Enable Queued Movement Window (Alt+Click)"){
				{a = Utils.getprefb("enableQueuedMovement", true);}
				public void set(boolean val) {
					Utils.setprefb("enableQueuedMovement", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Queued Movement - Checkpoint Route Window is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
						if (!val && ui.gui.map.checkpointManager != null)
							ui.gui.map.checkpointManager.wdgmsg("close");
					}
				}
			}, prev.pos("bl").adds(0, 12));
			enableQueuedMovementCheckBox.tooltip = OptWndTooltips.enableQueuedMovement;

            prev = add(walkWithPathFinderCheckBox = new CheckBox("Walk with Pathfinder (Ctrl+Shift+Click)"){
                {a = Utils.getprefb("walkWithPathfinder", false);}
                public void set(boolean val) {
                    Utils.setprefb("walkWithPathfinder", val);
                    a = val;
                    if (ui != null && ui.gui != null) {
                        ui.gui.optionInfoMsg("Walk with Pathfinder (Ctrl+Shift+Click) is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
                    }
                }
            }, prev.pos("bl").adds(0, 12));
            walkWithPathFinderCheckBox.tooltip = OptWndTooltips.walkWithPathfinder;
            prev = add(drawPathfinderRouteCheckBox = new CheckBox("Draw Pathfinder Route on the ground"){
                {a = Utils.getprefb("drawPathfinderRoute", false);}
                public void changed(boolean val) {
                    Utils.setprefb("drawPathfinderRoute", val);
                }
            }, prev.pos("bl").adds(12, 2));
            prev = add(pathfindOnMinimapCheckBox = new CheckBox("Pathfind on Minimap Click"){
                {a = Utils.getprefb("pathfindOnMinimap", false);}
                public void set(boolean val) {
                    Utils.setprefb("pathfindOnMinimap", val);
                    a = val;
                    if (ui != null && ui.gui != null) {
                        ui.gui.optionInfoMsg("Pathfind on Minimap Click is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
                    }
                }
            }, prev.pos("bl").adds(0, 2));
            pathfindOnMinimapCheckBox.tooltip = RichText.render("When enabled, left-clicking on the minimap/world map will use the pathfinder\ninstead of walking in a straight line. Handles long distances automatically.", UI.scale(300));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18));
			pack();
			centerBackButton(backButton, this);
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
	private Label freeCamZoomSpeedLabel;
	public static HSlider freeCamZoomSpeedSlider;
	private Button freeCamZoomSpeedResetButton;
	private Label freeCamRotationSensitivityLabel;
	public static HSlider freeCamRotationSensitivitySlider;
	private Button freeCamRotationSensitivityResetButton;
	private Label freeCamHeightLabel;
	public static HSlider freeCamHeightSlider;
	private Button freeCamHeightResetButton;
	public static CheckBox unlockedOrthoCamCheckBox;
	private Label orthoCamZoomSpeedLabel;
	public static HSlider orthoCamZoomSpeedSlider;
	private Button orthoCamZoomSpeedResetButton;
	private Label orthoCamRotationSensitivityLabel;
	public static HSlider orthoCamRotationSensitivitySlider;
	private Button orthoCamRotationSensitivityResetButton;
	public static CheckBox reverseOrthoCameraAxesCheckBox;
	public static CheckBox reverseFreeCamXAxisCheckBox;
	public static CheckBox reverseFreeCamYAxisCheckBox;
	public static CheckBox lockVerticalAngleAt45DegreesCheckBox;
	public static CheckBox allowLowerFreeCamTiltCheckBox;

	public class CameraSettingsPanel extends Panel {

		public CameraSettingsPanel(Panel back) {
			add(new Label(""), 278, 0); // ND: added this so the window's width does not change when switching camera type and closing/reopening the panel
			Widget TopPrev; // ND: these are always visible at the top, with either camera settings
			Widget FreePrev; // ND: used to calculate the positions for the Free camera settings
			Widget OrthoPrev; // ND: used to calculate the positions for the Ortho camera settings

			TopPrev = add(new Label("Selected Camera Type:"), 0, 0);{
				RadioGroup camGrp = new RadioGroup(this) {
					public void changed(int btn, String lbl) {
						try {
							if(btn==0) {
								Utils.setpref("defcam", "Free");
								setFreeCameraSettingsVisibility(true);
								setOrthoCameraSettingsVisibility(false);
								MapView.currentCamera = 1;
								if (ui != null && ui.gui != null && ui.gui.map != null) {
									ui.gui.map.setcam("Free");
								}
							}
							if(btn==1) {
								Utils.setpref("defcam", "Ortho");
								setFreeCameraSettingsVisibility(false);
								setOrthoCameraSettingsVisibility(true);
								MapView.currentCamera = 2;
								if (ui != null && ui.gui != null && ui.gui.map != null) {
									ui.gui.map.setcam("Ortho");
								}
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			TopPrev = camGrp.add("Free Camera", TopPrev.pos("bl").adds(16, 2));
			TopPrev = camGrp.add("Ortho Camera", TopPrev.pos("bl").adds(0, 1));
			TopPrev = add(new Label("Camera Dragging:"), TopPrev.pos("bl").adds(0, 6).x(0));
				TopPrev = add(allowMouse4CamDragCheckBox = new CheckBox("Also allow Mouse 4 Button to drag the Camera"){
					{a = (Utils.getprefb("allowMouse4CamDrag", false));}
					public void changed(boolean val) {
						Utils.setprefb("allowMouse4CamDrag", val);
					};
				}, TopPrev.pos("bl").adds(12, 2));
				TopPrev = add(allowMouse5CamDragCheckBox = new CheckBox("Also allow Mouse 5 Button to drag the Camera"){
					{a = Utils.getprefb("allowMouse5CamDrag", false);}
					public void changed(boolean val) {
						Utils.setprefb("allowMouse5CamDrag", val);
					}
				}, TopPrev.pos("bl").adds(0, 2));
			TopPrev = add(new Label("Selected Camera Settings:"), TopPrev.pos("bl").adds(0, 6).x(0));
			// ND: The Ortho Camera Settings
			OrthoPrev = add(reverseOrthoCameraAxesCheckBox = new CheckBox("Reverse Ortho Look Axis"){
				{a = (Utils.getprefb("reverseOrthoCamAxis", true));}
				public void changed(boolean val) {
					Utils.setprefb("reverseOrthoCamAxis", val);
				};
			}, TopPrev.pos("bl").adds(12, 2));
			reverseOrthoCameraAxesCheckBox.tooltip = OptWndTooltips.reverseOrthoCameraAxes;
			OrthoPrev = add(unlockedOrthoCamCheckBox = new CheckBox("Unlocked Ortho Camera"){
				{a = Utils.getprefb("unlockedOrthoCam", true);}
				public void changed(boolean val) {
					Utils.setprefb("unlockedOrthoCam", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 2));
			unlockedOrthoCamCheckBox.tooltip = OptWndTooltips.unlockedOrthoCam;
			OrthoPrev = add(orthoCamZoomSpeedLabel = new Label("Ortho Camera Zoom Speed:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
			OrthoPrev = add(orthoCamZoomSpeedSlider = new HSlider(UI.scale(200), 2, 40, Utils.getprefi("orthoCamZoomSpeed", 10)) {
				public void changed() {
					Utils.setprefi("orthoCamZoomSpeed", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 4));
			add(orthoCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				orthoCamZoomSpeedSlider.val = 10;
				Utils.setprefi("orthoCamZoomSpeed", 10);
			}), OrthoPrev.pos("bl").adds(210, -20));
			orthoCamZoomSpeedResetButton.tooltip = OptWndTooltips.resetButton;
				OrthoPrev = add(orthoCamRotationSensitivityLabel = new Label("Ortho Camera Rotation Sensitivity:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
				OrthoPrev = add(orthoCamRotationSensitivitySlider = new HSlider(UI.scale(200), 100, 1000, Utils.getprefi("orthoCamRotationSensitivity", 1000)) {
					public void changed() {
						Utils.setprefi("orthoCamRotationSensitivity", val);
					}
				}, OrthoPrev.pos("bl").adds(0, 4));
				add(orthoCamRotationSensitivityResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
					orthoCamRotationSensitivitySlider.val = 1000;
					Utils.setprefi("orthoCamRotationSensitivity", 1000);
				}), OrthoPrev.pos("bl").adds(210, -20));
				orthoCamRotationSensitivityResetButton.tooltip = OptWndTooltips.resetButton;

			// ND: The Free Camera Settings
			FreePrev = add(reverseFreeCamXAxisCheckBox = new CheckBox("Reverse X Axis"){
				{a = (Utils.getprefb("reverseFreeCamXAxis", true));}
				public void changed(boolean val) {
					Utils.setprefb("reverseFreeCamXAxis", val);
				}
			}, TopPrev.pos("bl").adds(12, 2));
			add(reverseFreeCamYAxisCheckBox = new CheckBox("Reverse Y Axis"){
				{a = (Utils.getprefb("reverseFreeCamYAxis", true));}
				public void changed(boolean val) {
					Utils.setprefb("reverseFreeCamYAxis", val);
				}
			}, FreePrev.pos("ul").adds(110, 0));
			FreePrev = add(lockVerticalAngleAt45DegreesCheckBox = new CheckBox("Lock Vertical Angle at 45°"){
				{a = (Utils.getprefb("lockVerticalAngleAt45Degrees", false));}
				public void changed(boolean val) {
					Utils.setprefb("lockVerticalAngleAt45Degrees", val);
					if (ui.gui.map != null)
						if (ui.gui.map.camera instanceof MapView.FreeCam)
							((MapView.FreeCam)ui.gui.map.camera).telev = (float)Math.PI / 4.0f;
				}
			}, FreePrev.pos("bl").adds(0, 2));
			FreePrev = add(allowLowerFreeCamTiltCheckBox = new CheckBox("Enable Lower Tilting Angle", Color.RED){
				{a = (Utils.getprefb("allowLowerTiltBool", false));}
				public void changed(boolean val) {
					Utils.setprefb("allowLowerTiltBool", val);
				}
			}, FreePrev.pos("bl").adds(0, 2));
			allowLowerFreeCamTiltCheckBox.tooltip = OptWndTooltips.allowLowerFreeCamTilt;
			allowLowerFreeCamTiltCheckBox.lbl = Text.create("Enable Lower Tilting Angle", PUtils.strokeImg(Text.std.render("Enable Lower Tilting Angle", new Color(185,0,0,255))));
			FreePrev = add(freeCamZoomSpeedLabel = new Label("Free Camera Zoom Speed:"), FreePrev.pos("bl").adds(0, 10).x(0));
			FreePrev = add(freeCamZoomSpeedSlider = new HSlider(UI.scale(200), 4, 40, Utils.getprefi("freeCamZoomSpeed", 25)) {
				public void changed() {
					Utils.setprefi("freeCamZoomSpeed", val);
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamZoomSpeedSlider.val = 25;
				Utils.setprefi("freeCamZoomSpeed", 25);
			}), FreePrev.pos("bl").adds(210, -20));
			freeCamZoomSpeedResetButton.tooltip = OptWndTooltips.resetButton;
			FreePrev = add(freeCamRotationSensitivityLabel = new Label("Free Camera Rotation Sensitivity:"), FreePrev.pos("bl").adds(0, 10).x(0));
			FreePrev = add(freeCamRotationSensitivitySlider = new HSlider(UI.scale(200), 100, 1000, Utils.getprefi("freeCamRotationSensitivity", 1000)) {
				public void changed() {
					Utils.setprefi("freeCamRotationSensitivity", val);
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamRotationSensitivityResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamRotationSensitivitySlider.val = 1000;
				Utils.setprefi("freeCamRotationSensitivity", 1000);
			}), FreePrev.pos("bl").adds(210, -20));
			freeCamRotationSensitivityResetButton.tooltip = OptWndTooltips.resetButton;
			FreePrev = add(freeCamHeightLabel = new Label("Free Camera Height:"), FreePrev.pos("bl").adds(0, 10));
			freeCamHeightLabel.tooltip = OptWndTooltips.freeCamHeight;
			FreePrev = add(freeCamHeightSlider = new HSlider(UI.scale(200), 10, 300, (Math.round((float) Utils.getprefd("cameraHeightDistance", 15f)))*10) {
				public void changed() {
					Utils.setprefd("cameraHeightDistance", (float) (val/10));
				}
			}, FreePrev.pos("bl").adds(0, 4));
			freeCamHeightSlider.tooltip = OptWndTooltips.freeCamHeight;
			add(freeCamHeightResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamHeightSlider.val = 150;
				Utils.setprefd("cameraHeightDistance", 15f);
			}), FreePrev.pos("bl").adds(210, -20));
			freeCamHeightResetButton.tooltip = OptWndTooltips.resetButton;

			// ND: Finally, check which camera is selected and set the right options to be visible
			String startupSelectedCamera = Utils.getpref("defcam", "Free");
			if (startupSelectedCamera.equals("Free") || startupSelectedCamera.equals("worse") || startupSelectedCamera.equals("follow")){
				camGrp.check(0);
				Utils.setpref("defcam", "Free");
				setFreeCameraSettingsVisibility(true);
				setOrthoCameraSettingsVisibility(false);
				MapView.currentCamera = 1;
			}
			else {
				camGrp.check(1);
				Utils.setpref("defcam", "Ortho");
				setFreeCameraSettingsVisibility(false);
				setOrthoCameraSettingsVisibility(true);
				MapView.currentCamera = 2;
			}
			}

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), FreePrev.pos("bl").adds(0, 18));
			pack();
			centerBackButton(backButton, this);
		}
		private void setFreeCameraSettingsVisibility(boolean bool){
			freeCamZoomSpeedLabel.visible = bool;
			freeCamZoomSpeedSlider.visible = bool;
			freeCamZoomSpeedResetButton.visible = bool;
			freeCamRotationSensitivityLabel.visible = bool;
			freeCamRotationSensitivitySlider.visible = bool;
			freeCamRotationSensitivityResetButton.visible = bool;
			freeCamHeightLabel.visible = bool;
			freeCamHeightSlider.visible = bool;
			freeCamHeightResetButton.visible = bool;
			lockVerticalAngleAt45DegreesCheckBox.visible = bool;
			allowLowerFreeCamTiltCheckBox.visible = bool;
			reverseFreeCamXAxisCheckBox.visible = bool;
			reverseFreeCamYAxisCheckBox.visible = bool;
		}
		private void setOrthoCameraSettingsVisibility(boolean bool){
			unlockedOrthoCamCheckBox.visible = bool;
			orthoCamZoomSpeedLabel.visible = bool;
			orthoCamZoomSpeedSlider.visible = bool;
			orthoCamZoomSpeedResetButton.visible = bool;
			orthoCamRotationSensitivityLabel.visible = bool;
			orthoCamRotationSensitivitySlider.visible = bool;
			orthoCamRotationSensitivityResetButton.visible = bool;
			reverseOrthoCameraAxesCheckBox.visible = bool;
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
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public HidingSettingsPanel(Panel back) {
			Widget prev;
			Widget prev2;
			prev = add(toggleGobHidingCheckBox = new CheckBox("Hide Objects"){
				{a = (Utils.getprefb("hideObjects", false));}
				public void set(boolean val) {
					Utils.setprefb("hideObjects", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, 0, 10);
			toggleGobHidingCheckBox.tooltip = OptWndTooltips.genericHasKeybind;

			add(alsoFillTheHidingBoxesCheckBox = new CheckBox("Also fill the Hiding Boxes"){
				{a = (Utils.getprefb("alsoFillTheHidingBoxes", true));}
				public void changed(boolean val) {
					Utils.setprefb("alsoFillTheHidingBoxes", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("ur").adds(50, 0));
			alsoFillTheHidingBoxesCheckBox.tooltip = RichText.render("Fills in the boxes. Only the outer lines will remain visible through other objects (like cliffs).");

			prev = add(dontHideObjectsThatHaveTheirMapIconEnabledCheckBox = new CheckBox("Don't hide Objects that have their Map Icon Enabled"){
				{a = (Utils.getprefb("dontHideObjectsThatHaveTheirMapIconEnabled", true));}
				public void changed(boolean val) {
					Utils.setprefb("dontHideObjectsThatHaveTheirMapIconEnabled", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 40))), prev.pos("bl").adds(14, 16));
			Widget cont = scroll.cont;
			addbtn(cont, "Toggle object hiding hotkey:", GameUI.kb_toggleHidingBoxes, 0);

			prev = add(hiddenObjectsColorOptionWidget = new ColorOptionWidget("Hidden Objects Box Color:", "hidingBox", 170, Integer.parseInt(hiddenObjectsColorSetting[0]), Integer.parseInt(hiddenObjectsColorSetting[1]), Integer.parseInt(hiddenObjectsColorSetting[2]), Integer.parseInt(hiddenObjectsColorSetting[3]), (Color col) -> {
				HidingBox.SOLID_FILLED = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_HOLLOW = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), Rendered.last, States.Depthtest.none);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}){}, scroll.pos("bl").adds(1, -2));

			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("hidingBox" + "_colorSetting", new String[]{"0", "225", "255", "170"});
				hiddenObjectsColorOptionWidget.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget.currentColor = new Color(0, 225, 255, 170));
				HidingBox.SOLID_FILLED = Pipe.Op.compose(new BaseColor(hiddenObjectsColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_HOLLOW = Pipe.Op.compose(new BaseColor(hiddenObjectsColorOptionWidget.currentColor), new States.LineWidth(HidingBox.WIDTH), Rendered.last, States.Depthtest.none);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = OptWndTooltips.resetButton;

			prev = add(new Label("Objects that will be hidden:"), prev.pos("bl").adds(0, 20).x(0));

			prev2 = add(hideTreesCheckbox = new CheckBox("Trees"){
				{a = Utils.getprefb("hideTrees", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideTrees", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(hideBushesCheckbox = new CheckBox("Bushes"){
				{a = Utils.getprefb("hideBushes", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideBushes", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev2.pos("bl").adds(0, 2));

			prev = add(hideBouldersCheckbox = new CheckBox("Boulders"){
				{a = Utils.getprefb("hideBoulders", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideBoulders", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(hideTreeLogsCheckbox = new CheckBox("Tree Logs"){
				{a = Utils.getprefb("hideTreeLogs", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideTreeLogs", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(hideWallsCheckbox = new CheckBox("Palisades and Brick Walls"){
				{a = Utils.getprefb("hideWalls", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideWalls", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev2.pos("ur").adds(90, 0));
			prev.tooltip = RichText.render("Only wall sections, NOT gates!");

			// TODO ND: Gotta figure out a way to not hide the doors... somehow
			prev = add(hideHousesCheckbox = new CheckBox("Houses"){
				{a = Utils.getprefb("hideHouses", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideHouses", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(hideStockpilesCheckbox = new CheckBox("Stockpiles"){
				{a = Utils.getprefb("hideStockpiles", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideStockpiles", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(hideCropsCheckbox = new CheckBox("Crops"){
				{a = Utils.getprefb("hideCrops", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideCrops", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev.tooltip = RichText.render("These won't show a hiding box, cause there's no collision.", UI.scale(300));

			prev = add(hideTrellisCheckbox = new CheckBox("Trellises"){
				{a = Utils.getprefb("hideTrellis", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideTrellis", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev.tooltip = RichText.render("This only hides the trellises, and not the crops growing on them.", UI.scale(300));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
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
			Widget prev;
			AlarmWidgetComponents comps;

			add(new Label("You can add your own alarm sound files in the \"AlarmSounds\" folder.", new Text.Foundry(Text.sans, 12)), 0, 0);
			add(new Label("(The file extension must be .wav)", new Text.Foundry(Text.sans, 12)), UI.scale(0, 16));
			prev = add(new Label("Enabled Player Alarms:"), UI.scale(0, 50));
			prev = add(new Label("Sound File"), prev.pos("ur").add(70, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(78, 0));

			comps = addAlarmWidget("whitePlayerAlarm", "White OR Unknown:", "ND_YoHeadsUp", true,null, prev);
			{whitePlayerAlarmEnabledCheckbox = comps.checkbox; whitePlayerAlarmFilename = comps.filename; whitePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("whiteVillageOrRealmPlayerAlarm", "Village/Realm Member:", "ND_HelloFriend",true,null, prev);
			{whiteVillageOrRealmPlayerAlarmEnabledCheckbox = comps.checkbox; whiteVillageOrRealmPlayerAlarmFilename = comps.filename; whiteVillageOrRealmPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("greenPlayerAlarm", "Green:", "ND_FlyingTheFriendlySkies", false, BuddyWnd.gc[1], prev);
			{greenPlayerAlarmEnabledCheckbox = comps.checkbox; greenPlayerAlarmFilename = comps.filename; greenPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("redPlayerAlarm", "Red:", "ND_EnemySighted", true, BuddyWnd.gc[2], prev);
			{redPlayerAlarmEnabledCheckbox = comps.checkbox; redPlayerAlarmFilename = comps.filename; redPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("bluePlayerAlarm", "Blue:", "", false, BuddyWnd.gc[3], prev);
			{bluePlayerAlarmEnabledCheckbox = comps.checkbox; bluePlayerAlarmFilename = comps.filename; bluePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("tealPlayerAlarm", "Teal:", "", false, BuddyWnd.gc[4], prev);
			{tealPlayerAlarmEnabledCheckbox = comps.checkbox; tealPlayerAlarmFilename = comps.filename; tealPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("yellowPlayerAlarm", "Yellow:", "", false, BuddyWnd.gc[5], prev);
			{yellowPlayerAlarmEnabledCheckbox = comps.checkbox; yellowPlayerAlarmFilename = comps.filename; yellowPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("purplePlayerAlarm", "Purple:", "", false, BuddyWnd.gc[6], prev);
			{purplePlayerAlarmEnabledCheckbox = comps.checkbox; purplePlayerAlarmFilename = comps.filename; purplePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("orangePlayerAlarm", "Orange:", "", false, BuddyWnd.gc[7], prev);
			{orangePlayerAlarmEnabledCheckbox = comps.checkbox; orangePlayerAlarmFilename = comps.filename; orangePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			prev = add(new Label("Enabled Sounds & Alerts:"), prev.pos("bl").add(0, 20).x(0));
			prev = add(new Label("Sound File"), prev.pos("ur").add(69, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(78, 0));

			comps = addAlarmWidget("combatStartSound", "Combat Started Alert:", "ND_HitAndRun", false, null, prev);
			{combatStartSoundEnabledCheckbox = comps.checkbox; combatStartSoundFilename = comps.filename; combatStartSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("cleaveSound", "Cleave Sound Effect:", "ND_Cleave", true, null, prev);
			{cleaveSoundEnabledCheckbox = comps.checkbox; cleaveSoundFilename = comps.filename; cleaveSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("opkSound", "Oppknock Sound Effect:", "ND_Opk", true, null, prev);
			{opkSoundEnabledCheckbox = comps.checkbox; opkSoundFilename = comps.filename; opkSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("ponyPowerSound", "Pony Power <10% Alert:", "ND_HorseEnergy", true, null, prev);
			{ponyPowerSoundEnabledCheckbox = comps.checkbox; ponyPowerSoundFilename = comps.filename; ponyPowerSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("lowEnergySound", "Energy <2500% Alert:", "ND_NotEnoughEnergy", true, null, prev);
			{lowEnergySoundEnabledCheckbox = comps.checkbox; lowEnergySoundFilename = comps.filename; lowEnergySoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			prev = add(CustomAlarmManagerButton = new Button(UI.scale(360), ">>> Other Alarms (Custom Alarm Manager) <<<", () -> {
				if(alarmWindow == null) {
					alarmWindow = this.parent.parent.add(new AlarmWindow());
					alarmWindow.show();
				} else {
					alarmWindow.show(!alarmWindow.visible);
					alarmWindow.bottomNote.settext("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)");
					alarmWindow.bottomNote.setcolor(Color.WHITE);
					alarmWindow.bottomNote.c.x = UI.scale(140);
				}
			}),prev.pos("bl").adds(0, 18).x(UI.scale(51)));


			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}

		private AlarmWidgetComponents addAlarmWidget(String prefPrefix, String label, String defaultFilename, boolean defaultEnabled, Color labelColor, Widget prev) {
			AlarmWidgetComponents out = new AlarmWidgetComponents();

			out.checkbox = new CheckBox(label) {
				{ a = Utils.getprefb(prefPrefix + "Enabled", defaultEnabled); }
				public void set(boolean val) {
					Utils.setprefb(prefPrefix + "Enabled", val);
					a = val;
				}
			};
			prev = add(out.checkbox, prev.pos("bl").adds(0, 7).x(0));

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
			prev = add(out.filename, prev.pos("ur").adds(0, -2).x(UI.scale(143)));

			out.volume = new HSlider(UI.scale(100), 0, 100, Utils.getprefi(prefPrefix + "Volume", 50)) {
				@Override
				public void changed() {
					Utils.setprefi(prefPrefix + "Volume", val);
					super.changed();
				}
			};
			prev = add(out.volume, prev.pos("ur").adds(6, 3));

			TextEntry finalFilename = out.filename;
			HSlider finalVolume = out.volume;
			prev = add(new Button(UI.scale(70), "Preview") {
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

		public class AlarmWidgetComponents { // ND: I can't set the static components from within the addAlarmWidget method without using this. It's some Java limitation, idk.
			public CheckBox checkbox;
			public TextEntry filename;
			public HSlider volume;
			public Widget lastWidget;
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

		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public AutoLootSettingsPanel(Panel back) {
			Widget prev;
            Widget leftColumn, rightColumn;

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(350, 40))), 0, 0);
			prev = scroll.cont;
			addbtn(prev, "Loot Nearest Knocked Player hotkey:", GameUI.kb_lootNearestKnockedPlayer, 0);

			prev = add(new Label("Auto-Loot the following gear from players (when stealing):"), prev.pos("bl").adds(0, 6));

            leftColumn = add(autoLootHeadgearCheckBox = new CheckBox("Headgear"){
                {a = Utils.getprefb("autoLootHeadgear", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootHeadgear", val);
                }
            }, prev.pos("bl").adds(0, 4).x(12));
            leftColumn = add(autoLootNecklaceCheckBox = new CheckBox("Necklace"){
                {a = Utils.getprefb("autoLootNecklace", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootNecklace", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));
            leftColumn = add(autoLootShouldersCheckBox = new CheckBox("Shoulders"){
                {a = Utils.getprefb("autoLootShoulders", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootShoulders", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));
            leftColumn = add(autoLootShirtCheckBox = new CheckBox("Shirt"){
                {a = Utils.getprefb("autoLootShirt", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootShirt", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));
            leftColumn = add(autoLootGlovesCheckBox = new CheckBox("Gloves"){
                {a = Utils.getprefb("autoLootGloves", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootGloves", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));
            leftColumn = add(autoLootWeaponCheckBox = new CheckBox("Weapon"){
                {a = Utils.getprefb("autoLootWeapon", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootWeapon", val);
                    autoLootWeaponCheckBox2.set(val);
                }
            }, leftColumn.pos("bl").adds(0, 2)).settip("Try to move it to Belt first, then Inventory if Belt is full.");
            leftColumn = add(autoLootRingsCheckBox = new CheckBox("Rings"){
                {a = Utils.getprefb("autoLootRings", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootRings", val);
                    autoLootRingsCheckBox2.set(val);
                }
            }, leftColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
            leftColumn = add(autoLootCloakRobeCheckBox = new CheckBox("Cloak/Robe"){
                {a = Utils.getprefb("autoLootCloakRobe", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootCloakRobe", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));
            leftColumn = add(autoLootPouchesCheckBox = new CheckBox("Pouches"){
                {a = Utils.getprefb("autoLootCloakRobe", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootCloakRobe", val);
                    autoLootPouchesCheckBox2.set(val);
                }
            }, leftColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
            leftColumn = add(autoLootPantsCheckBox = new CheckBox("Pants"){
                {a = Utils.getprefb("autoLootPants", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootPants", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));
            leftColumn = add(autoLootCapeCheckBox = new CheckBox("Cape"){
                {a = Utils.getprefb("autoLootCape", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootCape", val);
                }
            }, leftColumn.pos("bl").adds(0, 2));

            rightColumn = add(autoLootMaskCheckBox = new CheckBox("Mask"){
                {a = Utils.getprefb("autoLootMask", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootMask", val);
                }
            }, prev.pos("bl").adds(0, 4).x(200));
            rightColumn = add(autoLootEyewearCheckBox = new CheckBox("Eyewear"){
                {a = Utils.getprefb("autoLootEyewear", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootEyewear", val);
                }
            }, rightColumn.pos("bl").adds(0, 2));
            rightColumn = add(autoLootMouthwearCheckBox = new CheckBox("Mouthwear"){
                {a = Utils.getprefb("autoLootMouthwear", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootMouthwear", val);
                }
            }, rightColumn.pos("bl").adds(0, 2));
            rightColumn = add(autoLootChestArmorCheckBox = new CheckBox("Chest Armor"){
                {a = Utils.getprefb("autoLootChestArmor", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootChestArmor", val);
                }
            }, rightColumn.pos("bl").adds(0, 2));
            rightColumn = add(new Label("Belt (nope)"), rightColumn.pos("bl").adds(19, 2)).settip("Belts can't be placed in your inventory if they got stuff in them.");
            rightColumn = add(autoLootWeaponCheckBox2 = new CheckBox("Weapon"){
                {a = Utils.getprefb("autoLootWeapon", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootWeapon", val);
                    autoLootWeaponCheckBox.set(val);
                }
            }, rightColumn.pos("bl").adds(-19, 2)).settip("Try to move it to Belt first, then Inventory if Belt is full.");
            rightColumn = add(autoLootRingsCheckBox2 = new CheckBox("Rings"){
                {a = Utils.getprefb("autoLootRings", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootRings", val);
                    autoLootRingsCheckBox.set(val);
                }
            }, rightColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
            rightColumn = add(autoLootBackpackCheckBox = new CheckBox("Backpack"){
                {a = Utils.getprefb("autoLootBackpack", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootBackpack", val);
                }
            }, rightColumn.pos("bl").adds(0, 2));
            rightColumn = add(autoLootPouchesCheckBox2 = new CheckBox("Pouches"){
                {a = Utils.getprefb("autoLootCloakRobe", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootCloakRobe", val);
                    autoLootPouchesCheckBox.set(val);
                }
            }, rightColumn.pos("bl").adds(0, 2)).settip("Works for both slots");
            rightColumn = add(autoLootLegArmorCheckBox = new CheckBox("Leg Armor"){
                {a = Utils.getprefb("autoLootLegArmor", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootLegArmor", val);
                }
            }, rightColumn.pos("bl").adds(0, 2));
            rightColumn = add(autoLootShoesCheckBox = new CheckBox("Shoes"){
                {a = Utils.getprefb("autoLootShoes", false);}
                public void changed(boolean val) {
                    Utils.setprefb("autoLootShoes", val);
                }
            }, rightColumn.pos("bl").adds(0, 2));


			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), rightColumn.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
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
