package haven.automated;

import haven.*;
import haven.automated.pathfinder.Pathfinder;

import java.util.ArrayList;
import java.util.Random;

import static haven.OCache.posres;

public class OceanScoutBot extends BotBase {
	private int checkClock;
	private MCache mcache;
	private int clockwiseDirection = 1;
	private double ang = 0;
	private double searchRadius = 5;
	private ArrayList<Gob> nearbyGobs = new ArrayList<>();
	private Random random = new Random();
	private int successLocs;

	public OceanScoutBot(GameUI gui) {
		super(gui, UI.scale(UI.scale(274, 96)), "Ocean Scouting Bot");
		checkHP = false; checkEnergy = false; checkStamina = false; checkInventory = false;

		checkClock = 0;
		mcache = gui.map.glob.map;
		add(new Label(""), UI.scale(263, 0));
		add(new Label("Remember: The direction of the Shoreline is always"), UI.scale(10, 4));
		add(new Label("the opposite of the Deeper Water Edge."), UI.scale(10, 18));
		add(new CheckBox("Clockwise (Deeper Water Edge)") {{ a = true; } public void set(boolean val) { clockwiseDirection = val ? 1 : -1; a = val; }}, UI.scale(16, 42));

		add(new Button(UI.scale(170), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) { this.change("Stop"); }
				else { idlePlayer(); this.change("Start"); }
			}
		}, UI.scale(52, 66));
		pack();
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				if (!active) { Thread.sleep(200); continue; }
				if (successLocs > 20) {
					Gob pl = gui.map.player(); if (pl == null) { Thread.sleep(200); continue; }
					Coord2d groundTile = findRandomGroundTile();
					Coord2d groundVector = groundTile.sub(pl.rc);
					groundVector = groundVector.div(groundVector.abs()).mul(44);
					gui.map.wdgmsg("click", Coord.z, pl.rc.add(groundVector).floor(posres), 1, 0);
					Thread.sleep(300);
				}
				nearbyGobs = getNearbyGobs();
				Coord loc = getNextLoc();
				if (loc != null) {
					ang -= clockwiseDirection * Math.PI / 2;
					gui.map.wdgmsg("click", Coord.z, new Coord2d(loc.x, loc.y).floor(posres), 1, 0);
				} else {
					Gob pl2 = gui.map.player(); if (pl2 == null) { Thread.sleep(200); continue; }
					Coord2d pcCoord = pl2.rc;
					Coord2d dangerMob = isVeryDangerZone(pcCoord.floor());
					if (dangerMob != null) {
						Coord2d addCoord = pcCoord.sub(dangerMob);
						gui.map.wdgmsg("click", Coord.z, pcCoord.add(addCoord.div(addCoord.abs()).mul(11 * 2)).floor(posres), 1, 0);
					} else {
						Coord2d gocoord = findRandomWaterTile();
						Coord2d groundVector = gocoord.sub(pl2.rc);
						groundVector = groundVector.div(groundVector.abs()).mul(44);
						gui.map.wdgmsg("click", Coord.z, pl2.rc.add(groundVector).floor(posres), 1, 0);
					}
					Thread.sleep(300);
				}
				Thread.sleep(200);
				checkClock++;
			}
		} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	private Coord2d findRandomGroundTile() { Coord2d bc = gui.map.player().rc; int r = 40 * 11; for (int i = 0; i < 1000; i++) { Coord2d rc = new Coord2d(random.nextInt(r * 2) - r, random.nextInt(r * 2) - r); if (!isWater(bc.add(rc).floor())) return bc.add(rc); } return bc; }
	private Coord2d findRandomWaterTile() { Coord2d bc = gui.map.player().rc; int r = 30 * 11; for (int i = 0; i < 1000; i++) { Coord2d rc = new Coord2d(random.nextInt(r * 2) - r, random.nextInt(r * 2) - r); if (isWater(bc.add(rc).floor())) return bc.add(rc); } return bc; }

	private ArrayList<Gob> getNearbyGobs() {
		ArrayList<Gob> gobs = new ArrayList<>();
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				if (gui.map.player().rc.dist(gob.rc) < 3) continue;
				if (gui.map.player().rc.dist(gob.rc) < 25 * 11 && gob.collisionBox != null && gob.collisionBox.fx != null) gobs.add(gob);
			}
		}
		return gobs;
	}

	private Coord getNextLoc() {
		Coord pc = gui.map.player().rc.floor();
		double curAng = ang;
		while (clockwiseDirection == 1 ? ang <= curAng + 2 * Math.PI : ang >= curAng - 2 * Math.PI) {
			boolean foundground = false;
			for (int i = 0; i < 20; i++) {
				Coord2d addcoord = new Coord2d(-Math.cos(-ang) * i * searchRadius, Math.sin(-ang) * i * searchRadius);
				if (checkTiles(pc.add(addcoord.floor()))) foundground = true;
			}
			if (!foundground) { Coord2d addcoord = new Coord2d(-Math.cos(-ang) * 20 * searchRadius, Math.sin(-ang) * 20 * searchRadius); successLocs++; return pc.add(addcoord.floor()); }
			else successLocs = 0;
			ang += clockwiseDirection * 2 * Math.PI / 20;
		}
		return null;
	}

	private boolean checkTiles(Coord t) {
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) { if (!isWater(t.add(i * 11, j * 11))) return true; if (isGobCollision(t.add(i * 11, j * 11))) return true; if (isDangerZone(t.add(i * 11, j * 11))) return true; }
		return false;
	}

	private boolean isGobCollision(Coord t) { for (Gob gob : nearbyGobs) { try { if (gob != null && gob.getres() != null && Pathfinder.isInsideBoundBox(gob.rc.floor(), gob.a, gob.getres().name, t)) return true; } catch (Loading ignored) {} } return false; }
	private boolean isDangerZone(Coord t) { for (Gob gob : nearbyGobs) { try { if (gob.getres() != null && (gob.getres().name.endsWith("/walrus") || gob.getres().name.endsWith("/orca")) && t.dist(gob.rc.floor()) < 11 * 14) return true; } catch (Loading ignored) {} } return false; }
	private Coord2d isVeryDangerZone(Coord t) { for (Gob gob : nearbyGobs) { try { if (gob.getres() != null && (gob.getres().name.endsWith("/walrus") || gob.getres().name.endsWith("/orca")) && t.dist(gob.rc.floor()) < 11 * 11) return gob.rc; } catch (Loading ignored) {} } return null; }

	private boolean isWater(Coord t) {
		try { int dt = mcache.gettile(new Coord(t.x / 11, t.y / 11)); Resource res = mcache.tilesetr(dt); return res != null && res.name.equals("gfx/tiles/odeep"); } catch (Loading e) { return false; }
	}

	@Override protected String windowPrefKey() { return "wndc-oceanScoutBotWindow"; }
	@Override protected void onCleanup() { gui.OceanScoutBot = null; }
}
