package haven.automated;

import haven.*;

import static haven.OCache.posres;

public class MiningBot extends BotBase {
	private Label targetLabel;
	private Coord2d targetPos;
	public boolean settingTarget;

	public MiningBot(GameUI gui) {
		super(gui, UI.scale(250, 120), "Mining Bot");
		this.targetPos = null;
		this.settingTarget = false;

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
		add(activeButton, UI.scale(80, 75));
	}

	@Override
	protected void tick() throws InterruptedException {
		if (targetPos == null) { Thread.sleep(500); return; }
		Gob player = gui.map.player();
		if (player == null) { Thread.sleep(500); return; }
		if (!checkVitals()) return;

		if (gui.prog != null) {
			setStatus("Mining...");
			waitForProgressBar(60000);
			Thread.sleep(500);
			if (gui.vhand != null) {
				gui.vhand.item.wdgmsg("drop", Coord.z);
				Thread.sleep(500);
			}
			return;
		}

		setStatus("Mining...");
		gui.map.wdgmsg("click", Coord.z, targetPos.floor(posres), 1, 0);
		Thread.sleep(1000);

		for (int i = 0; i < 30; i++) {
			if (gui.prog != null) break;
			if (stop || !active) break;
			Thread.sleep(200);
		}
		Thread.sleep(500);
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
