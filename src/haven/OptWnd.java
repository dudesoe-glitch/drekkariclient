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
			OptWndQualityPanel.build(this, back, OptWnd.this);
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
