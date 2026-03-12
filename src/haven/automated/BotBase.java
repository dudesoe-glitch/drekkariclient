package haven.automated;

import haven.*;

import java.util.Objects;

import static haven.OCache.posres;

/**
 * Abstract base class for all Hurricane bot windows.
 * Provides shared lifecycle management, vitals checking, thread management,
 * and UI helpers to eliminate boilerplate across bot implementations.
 *
 * Standard bots override {@link #tick()} for per-iteration logic.
 * Complex bots (e.g., FishingBot) can override {@link #run()} entirely.
 */
public abstract class BotBase extends Window implements Runnable {
	protected final GameUI gui;
	public volatile boolean stop;
	protected volatile boolean active;
	protected Button activeButton;
	protected Label statusLabel;
	protected Thread botThread;

	// Safety check flags — subclasses can disable in constructor
	protected boolean checkHP = true;
	protected boolean checkEnergy = true;
	protected boolean checkStamina = true;
	protected boolean checkInventory = true;

	// Standard thresholds
	public static final double HP_THRESHOLD = 0.02;
	public static final double ENERGY_THRESHOLD = 0.25;
	public static final double STAMINA_THRESHOLD = 0.40;
	public static final int MIN_FREE_SLOTS = 2;
	public static final double MAX_SEARCH_DIST = 550.0; // ~50 tiles

	protected BotBase(GameUI gui, Coord sz, String title) {
		super(sz, title);
		this.gui = gui;
		this.stop = false;
		this.active = false;
	}

	protected BotBase(GameUI gui, Coord sz, String title, boolean lg) {
		super(sz, title, lg);
		this.gui = gui;
		this.stop = false;
		this.active = false;
	}

	// --- Thread Management ---

	public void startThread(String threadName) {
		botThread = new Thread(this, threadName);
		botThread.start();
	}

	// --- Default Run Loop ---

	/**
	 * Default run loop: waits while inactive, then calls {@link #tick()}.
	 * Override for bots with complex control flow (e.g., state machines).
	 */
	@Override
	public void run() {
		try {
			while (!stop) {
				if (!active) {
					Thread.sleep(200);
					continue;
				}
				tick();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Called each active iteration by the default {@link #run()} loop.
	 * Override to implement bot-specific logic.
	 */
	protected void tick() throws InterruptedException {
		Thread.sleep(1000);
	}

	// --- Abstract Methods ---

	/** Preference key for saving/restoring window position (e.g., "wndc-farmingBotWindow"). */
	protected abstract String windowPrefKey();

	/** Null this bot's reference(s) in GameUI (e.g., {@code gui.farmingBot = null}). */
	protected abstract void onCleanup();

	// --- Vitals Checking ---

	/**
	 * Check player vitals (HP, energy, stamina, inventory space).
	 * Returns false if the bot should skip this iteration (hearthed, or deactivated).
	 * Controlled by {@link #checkHP}, {@link #checkEnergy}, {@link #checkStamina}, {@link #checkInventory} flags.
	 */
	protected boolean checkVitals() throws InterruptedException {
		if (checkHP) {
			try {
				if (gui.getmeters("hp").get(1).a < HP_THRESHOLD) {
					setStatus("Low HP! Hearthing...");
					gui.act("travel", "hearth");
					Thread.sleep(8000);
					return false;
				}
			} catch (InterruptedException e) { throw e; }
			catch (Exception ignored) {}
		}
		if (checkEnergy) {
			try {
				if (gui.getmeter("nrj", 0).a < ENERGY_THRESHOLD) {
					gui.error(cap + ": Low on energy, stopping.");
					deactivate();
					Thread.sleep(2000);
					return false;
				}
			} catch (InterruptedException e) { throw e; }
			catch (Exception ignored) {}
		}
		if (checkStamina) {
			try {
				if (gui.getmeter("stam", 0).a < STAMINA_THRESHOLD) {
					setStatus("Drinking...");
					AUtils.drinkTillFull(gui, 0.99, 0.99);
				}
			} catch (InterruptedException e) { throw e; }
		}
		if (checkInventory) {
			try {
				if (gui.maininv.getFreeSpace() < MIN_FREE_SLOTS) {
					gui.error(cap + ": Inventory full, stopping.");
					deactivate();
					Thread.sleep(2000);
					return false;
				}
			} catch (InterruptedException e) { throw e; }
			catch (Exception ignored) {}
		}
		return true;
	}

	// --- UI Helpers ---

	/** Set status label text (null-safe). */
	protected void setStatus(String text) {
		if (statusLabel != null) {
			statusLabel.settext(text);
		}
	}

	/** Deactivate the bot: set active=false, update button text, set status. */
	protected void deactivate() {
		active = false;
		if (activeButton != null) {
			activeButton.change("Start");
		}
		setStatus("Stopped");
	}

	/** Click the player to idle (stop walking). Null-safe. */
	protected void idlePlayer() {
		try {
			Gob player = ui != null && ui.gui != null && ui.gui.map != null ? ui.gui.map.player() : null;
			if (player != null)
				ui.gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 1, 0);
		} catch (Exception ignored) {}
	}

	// --- Lifecycle ---

	/**
	 * Stop the bot: idle player, interrupt pathfinding, interrupt bot thread, destroy widget.
	 * Called from wdgmsg("close") and MenuGrid toggle-off.
	 */
	public void stop() {
		stop = true;
		idlePlayer();
		try {
			if (ui != null && ui.gui != null && ui.gui.map != null && ui.gui.map.pfthread != null) {
				ui.gui.map.pfthread.interrupt();
			}
		} catch (Exception ignored) {}
		if (botThread != null) {
			botThread.interrupt();
			botThread = null;
		}
		this.destroy();
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && (Objects.equals(msg, "close"))) {
			stop();
			reqdestroy();
			onCleanup();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public void reqdestroy() {
		Utils.setprefc(windowPrefKey(), this.c);
		super.reqdestroy();
	}

	// --- Progress Bar ---

	/** Wait for progress bar to complete, with timeout (ms). Breaks on stop/inactive. */
	protected void waitForProgressBar(int timeout) throws InterruptedException {
		int elapsed = 0;
		int hz = 100;
		while (gui.prog != null && gui.prog.prog != -1 && elapsed < timeout) {
			if (stop || !active) break;
			elapsed += hz;
			Thread.sleep(hz);
		}
	}
}
