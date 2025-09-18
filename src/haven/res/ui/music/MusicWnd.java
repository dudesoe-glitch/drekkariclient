/* Preprocessed source code */
package haven.res.ui.music;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.awt.event.KeyEvent;
import haven.Audio.CS;

/* >wdg: MusicWnd */
@haven.FromResource(name = "ui/music", version = 34)
public class MusicWnd extends Window {
    public static final Tex[] tips;
    public static final Map<Integer, Integer> keys;
    public static final int[] nti = {0, 2, 4, 5, 7, 9, 11}, shi = {1, 3, 6, 8, 10};
    public static final int[] ntp = {0, 1, 2, 3, 4, 5,  6}, shp = {0, 1, 3, 4,  5};
    public static final Tex[] ikeys;
    public final boolean[] cur = new boolean[12 * 3];
    public final int[] act;
    public final double start;
    public double latcomp = 0.15;
    public int actn;

    static {
	Map<Integer, Integer> km = new HashMap<Integer, Integer>();
	km.put(KeyEvent.VK_Z,  0);
	km.put(KeyEvent.VK_S,  1);
	km.put(KeyEvent.VK_X,  2);
	km.put(KeyEvent.VK_D,  3);
	km.put(KeyEvent.VK_C,  4);
	km.put(KeyEvent.VK_V,  5);
	km.put(KeyEvent.VK_G,  6);
	km.put(KeyEvent.VK_B,  7);
	km.put(KeyEvent.VK_H,  8);
	km.put(KeyEvent.VK_N,  9);
	km.put(KeyEvent.VK_J, 10);
	km.put(KeyEvent.VK_M, 11);
	Tex[] il = new Tex[4];
	for(int i = 0; i < 4; i++) {
	    il[i] = Resource.classres(MusicWnd.class).layer(Resource.imgc, i).tex();
	}
	String tc = "ZSXDCVGBHNJM";
	Text.Foundry fnd = new Text.Foundry(Text.fraktur.deriveFont(java.awt.Font.BOLD, 16)).aa(true);
	Tex[] tl = new Tex[tc.length()];
	for(int i = 0; i < nti.length; i++) {
	    int ki = nti[i];
	    tl[ki] = fnd.render(tc.substring(ki, ki + 1), new Color(0, 0, 0)).tex();
	}
	for(int i = 0; i < shi.length; i++) {
	    int ki = shi[i];
	    tl[ki] = fnd.render(tc.substring(ki, ki + 1), new Color(255, 255, 255)).tex();
	}
	keys = km;
	ikeys = il;
	tips = tl;
    };

    public MusicWnd(String name, int maxpoly) {
	super(ikeys[0].sz().mul(nti.length, 1), name, true);
	this.act = new int[maxpoly];
	this.start = System.currentTimeMillis() / 1000.0;
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	String nm = (String)args[0];
	int maxpoly = (Integer)args[1];
	return(new MusicWnd(nm, maxpoly));
    }

    protected void added() {
	super.added();
	ui.grabkeys(this);
    }

    public void cdraw(GOut g) {
	boolean[] cact = new boolean[cur.length];
	for(int i = 0; i < actn; i++)
	    cact[act[i]] = true;
	int base = 12;
	if(ui.modshift) base += 12;
	if(ui.modctrl)  base -= 12;
	for(int i = 0; i < nti.length; i++) {
	    Coord c = new Coord(ikeys[0].sz().x * ntp[i], 0);
	    boolean a = cact[nti[i] + base];
	    g.image(ikeys[a?1:0], c);
	    g.image(tips[nti[i]], c.add((ikeys[0].sz().x - tips[nti[i]].sz().x) / 2, ikeys[0].sz().y - tips[nti[i]].sz().y - (a?9:12)));
	}
	int sho = ikeys[0].sz().x - (ikeys[2].sz().x / 2);
	for(int i = 0; i < shi.length; i++) {
	    Coord c = new Coord(ikeys[0].sz().x * shp[i] + sho, 0);
	    boolean a = cact[shi[i] + base];
	    g.image(ikeys[a?3:2], c);
	    g.image(tips[shi[i]], c.add((ikeys[2].sz().x - tips[shi[i]].sz().x) / 2, ikeys[2].sz().y - tips[shi[i]].sz().y - (a?9:12)));
	}
    }

    public boolean keydown(KeyEvent ev) {
	double now = (ev.getWhen() / 1000.0) + latcomp;
	Integer keyp = keys.get(ev.getKeyCode());
	if(keyp != null) {
	    int key = keyp + 12;
	    if((ev.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0) key += 12;
	    if((ev.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)  key -= 12;
	    if(!cur[key]) {
		if(actn >= act.length) {
		    wdgmsg("stop", act[0], (float)(now - start));
		    for(int i = 1; i < actn; i++)
			act[i - 1] = act[i];
		    actn--;
		}
		wdgmsg("play", key, (float)(now - start));
		cur[key] = true;
		act[actn++] = key;
	    }
	    return(true);
	}
	super.keydown(ev);
	return(true);
    }

    private void stopnote(double now, int key) {
	if(cur[key]) {
	    outer: for(int i = 0; i < actn; i++) {
		if(act[i] == key) {
		    wdgmsg("stop", key, (float)(now - start));
		    for(actn--; i < actn; i++)
			act[i] = act[i + 1];
		    break outer;
		}
	    }
	    cur[key] = false;
	}
    }

    public boolean keyup(KeyEvent ev) {
	double now = (ev.getWhen() / 1000.0) + latcomp;
	Integer keyp = keys.get(ev.getKeyCode());
	if(keyp != null) {
	    int key = keyp;
	    stopnote(now, key);
	    stopnote(now, key + 12);
	    stopnote(now, key + 24);
	    return(true);
	}
	return(super.keydown(ev));
    }
}
