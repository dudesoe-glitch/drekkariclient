package haven.automated;

import haven.*;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;

public class CellarDiggingBot extends BotBase {

	public CellarDiggingBot(GameUI gui) {
		super(gui, UI.scale(150, 70), "Cellar Digging Bot");

		activeButton = new Button(UI.scale(150), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) { this.change("Stop"); }
				else { idlePlayer(); this.change("Start"); }
			}
		};
		add(activeButton, UI.scale(0, 10));
		pack();
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				if (!checkVitals()) { sleep(200); continue; }
				if (active) {
					IMeter.Meter stamMeter = gui.getmeter("stam", 0);
					if (stamMeter != null && stamMeter.a < STAMINA_THRESHOLD) {
						try { Actions.drinkTillFull(gui, 0.99, 0.99); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
						sleep(200); continue;
					}
					if (gui.maininv.getFreeSpace() < MIN_FREE_SLOTS) {
						gui.errorsilent("Cellar Digging Bot: Inventory full, stopping.");
						deactivate(); sleep(2000); continue;
					}

					Gob cellar = findCellarGob();
					if (cellar == null) {
						gui.errorsilent("Cellar Digging Bot: No cellar door present! Stopping.");
						deactivate(); continue;
					}

					chipAllBoulders();
					if (!active || stop) continue;

					tryEnterCellar(cellar);
					while (isMiningOrRunning()) { if (!checkVitals()) break; sleep(100); }
				}
				sleep(200);
			}
		} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	private Gob findCellarGob() {
		Gob found = null; Gob player = gui.map.player(); if (player == null) return null;
		Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
		synchronized (gui.map.glob.oc) {
			for (Gob g : gui.map.glob.oc) {
				try { Resource r = g.getres(); if (r == null) continue;
					if ("gfx/terobjs/arch/cellardoor".equals(r.name)) { double dist = g.rc.dist(playerPos); if (dist > MAX_SEARCH_DIST) continue; if (found == null || dist < found.rc.dist(playerPos)) found = g; }
				} catch (Loading | NullPointerException ignored) {}
			}
		}
		return found;
	}

	private void tryEnterCellar(Gob cell) throws InterruptedException {
		gui.map.pfLeftClick(cell.rc.floor().add(12, 0), null);
		if (!Actions.waitPf(gui)) Actions.unstuck(gui);
		Actions.clearhand(gui);
		FlowerMenu.setNextSelection("Enter");
		gui.map.wdgmsg("click", Coord.z, cell.rc.floor(posres), 3, 0, 0, (int) cell.id, cell.rc.floor(posres), 0, -1);
	}

	private void chipAllBoulders() throws InterruptedException {
		while (active && !stop) { if (!checkVitals()) return; Gob g = closestBumling(); if (g == null) return; chipBoulderOnce(g); if (!active || stop) return; sleep(150); }
	}

	private void chipBoulderOnce(Gob bumling) throws InterruptedException {
		if (!bumlingExists(bumling) || !active || stop) return;
		Gob player = gui.map.player();
		if (player == null) return;
		if (bumling.rc.dist(player.rc) > 11 * 5) { gui.map.pfLeftClick(bumling.rc.floor().add(10, 0), null); if (!Actions.waitPf(gui)) Actions.unstuck(gui); }
		if (!bumlingExists(bumling) || !active || stop) return;
		Actions.clearhand(gui);
		FlowerMenu.setNextSelection("Chip");
		gui.map.wdgmsg("click", Coord.z, bumling.rc.floor(posres), 3, 0, 0, (int) bumling.id, bumling.rc.floor(posres), 0, -1);
		int idleTicks = 0;
		while (active && !stop && bumlingExists(bumling)) {
			if (!checkVitals()) break;
			if (isMiningOrRunning()) idleTicks = 0; else { idleTicks++; if (idleTicks >= 3) break; }
			sleep(100);
		}
	}

	private Gob closestBumling() {
		Gob best = null; Gob player = gui.map.player(); if (player == null) return null; Coord2d me = new Coord2d(player.rc.x, player.rc.y);
		synchronized (gui.map.glob.oc) {
			for (Gob g : gui.map.glob.oc) { try { Resource r = g.getres(); if (r == null || !r.name.contains("/bumlings/")) continue; double dist = g.rc.dist(me); if (dist > MAX_SEARCH_DIST) continue; if (best == null || dist < best.rc.dist(me)) best = g; } catch (Loading | NullPointerException ignored) {} }
		}
		return best;
	}

	private boolean isMiningOrRunning() {
		try { Gob pl = gui.map.player(); Thread pf = gui.map.pfthread; GameUI.Progress p = gui.prog; return (pl != null && pl.getPoses().contains("pickan")) || (pf != null && pf.isAlive()) || (p != null && p.prog != -1); } catch (Exception ignored) {} return false;
	}

	private boolean bumlingExists(Gob b) {
		if (b == null) return false;
		synchronized (gui.map.glob.oc) { for (Gob g : gui.map.glob.oc) if (g.id == b.id) return true; }
		return false;
	}

	@Override protected String windowPrefKey() { return "wndc-cellarDiggingBotWindow"; }
	@Override protected void onCleanup() { gui.cellarDiggingBot = null; }
}
