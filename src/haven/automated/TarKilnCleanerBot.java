package haven.automated;

import haven.*;

import java.util.ArrayList;
import java.util.List;

import static haven.OCache.posres;

public class TarKilnCleanerBot extends BotBase {
	private final CheckBox activeBox;
	private int phase = 1;

	public TarKilnCleanerBot(GameUI gui) {
		super(gui, UI.scale(150, 50), "Tar Kiln Emptier");
		checkHP = false; checkEnergy = false; checkStamina = false; checkInventory = false;

		activeBox = new CheckBox("Active") {
			{ a = active; }
			public void set(boolean val) {
				if (val) {
					active = true;
					a = true;
					phase = 1;
				} else {
					deactivate();
				}
			}
		};
		add(activeBox, UI.scale(40, 15));
	}

	@Override
	protected void deactivate() {
		active = false;
		activeBox.a = false;
		phase = 1;
	}

	@Override
	protected void tick() throws InterruptedException {
		if (phase == 1) {
			if (gui.vhand != null && gui.vhand.item != null) gui.vhand.item.wdgmsg("drop", Coord.z);
			dropCoal();

			List<Gob> tarKilns = GobHelper.findByName(gui, "gfx/terobjs/tarkiln", 550.0);
			Gob closest = null;
			Gob player = gui.map.player(); if (player == null) return;
			Coord2d playerPos = new Coord2d(player.rc.x, player.rc.y);
			for (Gob tarKiln : tarKilns) {
				if (closest == null || tarKiln.rc.dist(playerPos) < closest.rc.dist(playerPos)) {
					ResDrawable resDrawable = tarKiln.getattr(ResDrawable.class);
					if (resDrawable != null && (resDrawable.sdt.checkrbuf(0) == 10 || resDrawable.sdt.checkrbuf(0) == 42)) closest = tarKiln;
				}
			}

			if (closest == null) { gui.errorsilent("No full tar kilns nearby."); deactivate(); return; }

			if (gui.prog == null) {
				int[][] options = {{33, 0}, {-33, 0}, {0, 33}, {0, -33}};
				for (int[] option : options) {
					Coord newCoord = closest.rc.floor().add(option[0], option[1]);
					gui.map.pfLeftClick(newCoord, null);
					Thread.sleep(500);
					Actions.waitPf(gui);
					Gob p2 = gui.map.player(); if (p2 == null) return;
					if (p2.rc.dist(new Coord2d(newCoord)) < 40) break;
				}
				Actions.rightClickShiftCtrl(gui, closest);
				Thread.sleep(1000);
				Actions.waitProgBar(gui);
			}
		}
		Thread.sleep(200);
	}

	private void dropCoal() {
		for (WItem wItem : gui.maininv.getAllItems()) {
			try { GItem gitem = wItem.item; if (gitem.getname().contains("Coal")) gitem.wdgmsg("drop", new Coord(wItem.item.sz.x / 2, wItem.item.sz.y / 2)); } catch (Loading ignored) {}
		}
	}

	@Override protected String windowPrefKey() { return "wndc-tarKilnCleanerBotWindow"; }
	@Override protected void onCleanup() { gui.tarKilnCleanerBot = null; }
}
