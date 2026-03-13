package haven.automated;

import haven.*;
import haven.automated.GobHelper;

import java.util.List;

import static haven.OCache.posres;

public class MiningBot extends BotBase {
	private Label targetLabel;
	private Coord2d targetPos;
	public boolean settingTarget;
	private boolean safeMining;

	public MiningBot(GameUI gui) {
		super(gui, UI.scale(250, 150), "Mining Bot");
		this.targetPos = null;
		this.settingTarget = false;
		this.safeMining = Utils.getprefb("miningBot_safeMining", false);

		statusLabel = new Label("Idle");
		add(statusLabel, UI.scale(10, 10));

		Button setTargetButton = new Button(UI.scale(100), "Set Target") {
			@Override
			public void click() {
				settingTarget = true;
				statusLabel.settext("Click a mine wall...");
			}
		};
		add(setTargetButton, UI.scale(10, 35));

		targetLabel = new Label("Target: not set");
		add(targetLabel, UI.scale(10, 58));

		add(new CheckBox("Safe Mining (support radius)") {{ a = safeMining; }
			public void set(boolean val) { safeMining = val; a = val; Utils.setprefb("miningBot_safeMining", val); }
		}, UI.scale(10, 78));

		activeButton = new Button(UI.scale(80), "Start") {
			@Override
			public void click() {
				active = !active;
				if (active) {
					if (targetPos == null) {
						active = false;
						statusLabel.settext("Set target first!");
						return;
					}
					this.change("Stop");
					statusLabel.settext("Running...");
				} else {
					idlePlayer();
					this.change("Start");
					statusLabel.settext("Stopped");
				}
			}
		};
		add(activeButton, UI.scale(80, 105));
	}

	@Override
	protected void tick() throws InterruptedException {
		if (targetPos == null) { Thread.sleep(200); return; }
		Gob player = gui.map.player();
		if (player == null) { Thread.sleep(200); return; }
		if (!checkVitals()) return;

		if (safeMining && !isInSupportRange(targetPos)) {
			setStatus("Target outside support range!");
			deactivate();
			return;
		}

		if (gui.prog != null) {
			setStatus("Mining...");
			waitForProgressBar(60000);
			if (gui.vhand != null) {
				gui.vhand.item.wdgmsg("drop", Coord.z);
				Actions.waitForEmptyHand(gui, 1000, "");
			}
			return;
		}

		// Pathfind if far from target
		double dist = player.rc.dist(targetPos);
		if (dist > 33) {
			setStatus("Walking to target...");
			gui.map.pfLeftClick(targetPos.floor(), null);
			if (!Actions.waitPf(gui)) {
				Actions.unstuck(gui);
				return;
			}
			player = gui.map.player();
			if (player == null) return;
		}

		setStatus("Mining...");
		gui.map.wdgmsg("click", Coord.z, targetPos.floor(posres), 1, 0);
		Thread.sleep(50);

		for (int i = 0; i < 30; i++) {
			if (gui.prog != null) break;
			if (stop || !active) break;
			Thread.sleep(100);
		}
	}

	private boolean isInSupportRange(Coord2d pos) {
		List<Gob> supports = GobHelper.findAllSupports(gui);
		for (Gob support : supports) {
			try {
				Resource r = support.getres();
				if (r == null) continue;
				String res = r.name;
				double dist = support.rc.dist(pos);
				if ((res.equals("gfx/terobjs/ladder") || res.equals("gfx/terobjs/minesupport")) && dist <= 100)
					return true;
				if (res.equals("gfx/terobjs/column") && dist <= 125)
					return true;
				if (res.equals("gfx/terobjs/minebeam") && dist <= 150)
					return true;
			} catch (Loading ignored) {}
		}
		return false;
	}

	public void setTarget(Coord2d mc) {
		targetPos = mc;
		settingTarget = false;
		targetLabel.settext("Target: " + mc.floor().x + ", " + mc.floor().y);
		statusLabel.settext("Target set");
	}

	@Override
	protected String windowPrefKey() { return "wndc-miningBotWindow"; }

	@Override
	protected void onCleanup() { gui.miningBot = null; }
}
