package haven.automated;

import haven.*;

public class GrubGrubBot extends BotBase {
	public static boolean transferTicks = false;

	public GrubGrubBot(GameUI gui) {
		super(gui, UI.scale(UI.scale(254, 96)), "Grub-Grub Bot");
		checkHP = false;
		checkStamina = false;
		checkInventory = false;

		add(new Label(""), UI.scale(243, 0));
		add(new Button(UI.scale(160), "Start"){
			@Override
			public void click() {
				active = !active;
				if (active){
					GrubGrubBot.transferTicks = true;
					this.change("Stop");
				} else {
					GrubGrubBot.transferTicks = false;
					this.change("Start");
				}
			}
		}, UI.scale(32, 10));
		pack();
	}

	@Override
	public void stop() {
		GrubGrubBot.transferTicks = false;
		super.stop();
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				if (!active) {
					Thread.sleep(200);
					continue;
				}
				// Energy check
				try {
					if (gui.getmeter("nrj", 0).a < ENERGY_THRESHOLD) {
						gui.errorsilent("Grub Grub Bot: Low on energy, stopping.");
						GrubGrubBot.transferTicks = false;
						active = false;
						Thread.sleep(2000);
						continue;
					}
				} catch (Exception e) {
					if (e instanceof InterruptedException) { Thread.currentThread().interrupt(); return; }
				}
				int totalTicks = gui.maininv.getItemsPartial("Tick").size();
				if (totalTicks >= 2 ) {
					if(gui.makewnd != null && gui.makewnd.makeWidget != null){
						if (gui.makewnd.cap.equals("Grub-Grub"))
							gui.makewnd.makeWidget.wdgmsg("make",0);
						else
							gui.ui.error("Grub Grub Bot: Crafting Window is not set to craft Grub-Grub!");
					} else {
						gui.ui.error("Grub Grub Bot: Couldn't find Grub-Grub Crafting Window!");
					}
				}
				Thread.sleep(200);

				if(gui.prog != null) {
					Thread.sleep(2000);
				}

				for (haven.WItem witem : gui.maininv.getItemsPartial("Grub-Grub")) {
					witem.item.wdgmsg("transfer", Coord.z);
				}
				Thread.sleep(200);

				GrubGrubBot.transferTicks = gui.maininv.getFreeSpace() > 1;

				Thread.sleep(1000);
			}
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected String windowPrefKey() { return "wndc-grubGrubBotWindow"; }

	@Override
	protected void onCleanup() { gui.grubGrubBot = null; }
}
