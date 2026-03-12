package haven;

import haven.render.*;

/**
 * Extracted Video Settings panel builder for OptWnd.
 * Contains the CPanel build logic that was previously inlined
 * in OptWnd.VideoPanel.CPanel constructor.
 *
 * The CPanel inner class is kept in OptWnd.VideoPanel since it has
 * a mutable 'prefs' field and references VideoPanel.resetcf().
 * Only the constructor body (widget creation) is extracted here.
 */
class OptWndVideoPanel {

	/**
	 * Build the CPanel contents.
	 * @param cp the CPanel widget to add children to
	 * @param getPrefs supplier that returns current prefs (cp.prefs)
	 * @param setPrefs consumer that sets cp.prefs and calls ui.setgprefs
	 * @param errorMsg consumer to display error messages
	 * @param resetcf callback to reset the CPanel (VideoPanel.resetcf())
	 * @param resetDefaults callback that resets to GSettings.defaults()
	 */
	interface PrefsGetter { GSettings get(); }
	interface PrefsSetter { void set(GSettings p); }
	interface ErrorHandler { void handle(String msg); }

	static void buildCPanel(Widget cp, PrefsGetter getPrefs, PrefsSetter setPrefs, ErrorHandler errorMsg, Runnable resetcf, Runnable resetDefaults) {
		Widget prev;
		int marg = UI.scale(5);
		prev = cp.add(new CheckBox("Render shadows") {
			{a = getPrefs.get().lshadow.val;}

			public void set(boolean val) {
				try {
					GSettings np = getPrefs.get().update(null, getPrefs.get().lshadow, val);
					setPrefs.set(np);
				} catch(GSettings.SettingException e) {
					errorMsg.handle(e.getMessage());
					return;
				}
				a = val;
			}
		}, Coord.z);
		prev = cp.add(new Label("Render scale"), prev.pos("bl").adds(0, 5));
		{
			Label dpy = new Label("");
			final int steps = 4;
			cp.addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
					prev = new HSlider(UI.scale(160), -2 * steps, 1 * steps, (int)Math.round(steps * Math.log(getPrefs.get().rscale.val) / Math.log(2.0f))) {
						protected void added() {
							dpy();
						}
						void dpy() {
							dpy.settext(String.format("%.2f\u00d7", Math.pow(2, this.val / (double)steps)));
						}
						public void changed() {
							try {
								float val = (float)Math.pow(2, this.val / (double)steps);
								GSettings np = getPrefs.get().update(null, getPrefs.get().rscale, val);
								setPrefs.set(np);
								if(ui.gui != null && ui.gui.map != null) {ui.gui.map.updateGridMat();}
							} catch(GSettings.SettingException e) {
								errorMsg.handle(e.getMessage());
								return;
							}
							dpy();
						}
					},
					dpy);
		}
		prev = cp.add(new CheckBox("Vertical sync") {
			{a = getPrefs.get().vsync.val;}

			public void set(boolean val) {
				try {
					GSettings np = getPrefs.get().update(null, getPrefs.get().vsync, val);
					setPrefs.set(np);
				} catch(GSettings.SettingException e) {
					errorMsg.handle(e.getMessage());
					return;
				}
				a = val;
			}
		}, prev.pos("bl").adds(0, 5));
		prev = cp.add(new Label("Framerate limit (active window)"), prev.pos("bl").adds(0, 5));
		{
			Label dpy = new Label("");
			final int max = 250;
			cp.addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
					prev = new HSlider(UI.scale(160), 20, max, (getPrefs.get().hz.val == Float.POSITIVE_INFINITY) ? max : getPrefs.get().hz.val.intValue()) {
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
								GSettings np = getPrefs.get().update(null, getPrefs.get().hz, val);
								setPrefs.set(np);
							} catch(GSettings.SettingException e) {
								errorMsg.handle(e.getMessage());
								return;
							}
							dpy();
						}
					},
					dpy);
		}
		prev = cp.add(new Label("Framerate limit (background window)"), prev.pos("bl").adds(0, 5));
		{
			Label dpy = new Label("");
			final int max = 250;
			cp.addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
					prev = new HSlider(UI.scale(160), 20, max, (getPrefs.get().bghz.val == Float.POSITIVE_INFINITY) ? max : getPrefs.get().bghz.val.intValue()) {
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
								GSettings np = getPrefs.get().update(null, getPrefs.get().bghz, val);
								setPrefs.set(np);
							} catch(GSettings.SettingException e) {
								errorMsg.handle(e.getMessage());
								return;
							}
							dpy();
						}
					},
					dpy);
		}
		prev = cp.add(new Label("Lighting mode"), prev.pos("bl").adds(0, 5));
		{
			boolean[] done = {false};
			RadioGroup grp = new RadioGroup(cp) {
				public void changed(int btn, String lbl) {
					if(!done[0])
						return;
					try {
						GSettings np = getPrefs.get()
								.update(null, getPrefs.get().lightmode, GSettings.LightMode.values()[btn])
								.update(null, getPrefs.get().maxlights, 0);
						setPrefs.set(np);
					} catch(GSettings.SettingException e) {
						errorMsg.handle(e.getMessage());
						return;
					}
					resetcf.run();
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
			grp.check(getPrefs.get().lightmode.val.ordinal());
			done[0] = true;
		}
		prev = cp.add(new Label("Light-source limit"), prev.pos("bl").adds(0, 5).x(0));
		{
			Label dpy = new Label("");
			int val = getPrefs.get().maxlights.val, max = 32;
			if(val == 0) {    /* XXX: This is just ugly. */
				if(getPrefs.get().lightmode.val == GSettings.LightMode.ZONED)
					val = Lighting.LightGrid.defmax;
				else
					val = Lighting.SimpleLights.defmax;
			}
			if(getPrefs.get().lightmode.val == GSettings.LightMode.SIMPLE)
				max = 4;
			cp.addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
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
								GSettings np = getPrefs.get().update(null, getPrefs.get().maxlights, this.val * 4);
								setPrefs.set(np);
							} catch(GSettings.SettingException e) {
								errorMsg.handle(e.getMessage());
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
		prev = cp.add(new Label("Frame sync mode"), prev.pos("bl").adds(0, 5).x(0));
		{
			boolean[] done = {false};
			RadioGroup grp = new RadioGroup(cp) {
				public void changed(int btn, String lbl) {
					if(!done[0])
						return;
					try {
						GSettings np = getPrefs.get().update(null, getPrefs.get().syncmode, JOGLPanel.SyncMode.values()[btn]);
						setPrefs.set(np);
					} catch(GSettings.SettingException e) {
						errorMsg.handle(e.getMessage());
						return;
					}
				}
			};
			prev = cp.add(new Label("\u2191 Better performance, worse latency"), prev.pos("bl").adds(5, 2));
			prev = grp.add("One-frame overlap", prev.pos("bl").adds(0, 2));
			prev = grp.add("Tick overlap", prev.pos("bl").adds(0, 2));
			prev = grp.add("CPU-sequential", prev.pos("bl").adds(0, 2));
			prev = grp.add("GPU-sequential", prev.pos("bl").adds(0, 2));
			prev = cp.add(new Label("\u2193 Worse performance, better latency"), prev.pos("bl").adds(0, 2));
			grp.check(getPrefs.get().syncmode.val.ordinal());
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
		cp.add(new Button(UI.scale(200), "Reset to defaults", false).action(() -> {
			resetDefaults.run();
		}), prev.pos("bl").adds(-5, 5));
		cp.pack();
	}
}
