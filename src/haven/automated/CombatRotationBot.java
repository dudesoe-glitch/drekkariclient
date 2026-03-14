package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static haven.OCache.posres;

/**
 * Combat Rotation Bot — executes a configurable sequence of combat moves.
 * Each step specifies an action bar slot (1-10) and a repeat count.
 * The rotation loops while in combat and the bot is active.
 *
 * Moves are executed by sending "use"/"rel" messages through the Fightsess widget,
 * respecting cooldowns tracked via Fightsess.Action.ct.
 */
public class CombatRotationBot extends BotBase {
	// Rotation steps: each is {slotIndex (0-9), repeatCount}. Synchronized for thread safety.
	private final List<int[]> steps = Collections.synchronizedList(new ArrayList<>());
	private volatile int currentStep = 0;
	private volatile int currentRepeat = 0;
	private volatile int selectedStep = -1;
	private volatile boolean loopRotation = true;
	private volatile boolean waitForCooldowns = true;

	// UI components
	private final Label[] stepLabels;
	private final TextEntry slotEntry;
	private final TextEntry countEntry;

	private static final int MAX_DISPLAY_STEPS = 8;
	private static final int STEP_HEIGHT = 16;
	private static final Color ACTIVE_COLOR = new Color(60, 180, 60);
	private static final Color SELECTED_COLOR = new Color(180, 180, 60);
	private static final Color NORMAL_COLOR = Color.WHITE;

	public CombatRotationBot(GameUI gui) {
		super(gui, UI.scale(new Coord(250, 310)), "Combat Rotation", true);
		checkHP = false;
		checkEnergy = false;
		checkStamina = false;
		checkInventory = false;

		loadRotation();

		int y = 0;

		// Slot key reference
		add(new Label("Slots: 1-3=1/2/3  4=R  5=F  6-8=S+1/2/3  9=F2  10=F1") {
			{ setcolor(new Color(160, 160, 160)); }
		}, UI.scale(0, y));
		y += UI.scale(14);

		// Step display labels (clickable)
		stepLabels = new Label[MAX_DISPLAY_STEPS];
		for (int i = 0; i < MAX_DISPLAY_STEPS; i++) {
			final int idx = i;
			stepLabels[i] = add(new Label("") {
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					selectedStep = idx;
					return true;
				}
			}, UI.scale(4, y));
			y += UI.scale(STEP_HEIGHT);
		}
		refreshStepLabels();
		y += UI.scale(4);

		// Add/Remove/Move buttons
		Widget prev;
		prev = add(new Button(UI.scale(40), "Add") {
			public void click() { addStep(); }
		}, UI.scale(0, y));
		prev = add(new Button(UI.scale(55), "Remove") {
			public void click() { removeStep(); }
		}, prev.pos("ur").adds(2, 0));
		prev = add(new Button(UI.scale(30), "\u2191") {
			public void click() { moveStep(-1); }
		}, prev.pos("ur").adds(2, 0));
		prev = add(new Button(UI.scale(30), "\u2193") {
			public void click() { moveStep(1); }
		}, prev.pos("ur").adds(2, 0));
		y = prev.pos("bl").y + UI.scale(6);

		// Slot and count entry
		prev = add(new Label("Slot(1-10):"), UI.scale(0, y + 2));
		slotEntry = new TextEntry(UI.scale(30), "1");
		prev = add(slotEntry, prev.pos("ur").adds(2, -2));
		prev = add(new Label("Count:"), prev.pos("ur").adds(6, 2));
		countEntry = new TextEntry(UI.scale(30), "1");
		add(countEntry, prev.pos("ur").adds(2, -2));
		y += UI.scale(24);

		// Checkboxes
		add(new CheckBox("Loop rotation") {{
			a = loopRotation;
		}
			public void set(boolean val) {
				loopRotation = val;
				a = val;
				Utils.setprefb("combatRot_loop", val);
			}
		}, UI.scale(0, y));
		y += UI.scale(18);
		add(new CheckBox("Wait for cooldowns") {{
			a = waitForCooldowns;
		}
			public void set(boolean val) {
				waitForCooldowns = val;
				a = val;
				Utils.setprefb("combatRot_waitCD", val);
			}
		}, UI.scale(0, y));
		y += UI.scale(24);

		// Start/Stop
		activeButton = add(new Button(UI.scale(60), "Start") {
			public void click() {
				if (active) {
					active = false;
					change("Start");
				} else {
					active = true;
					currentStep = 0;
					currentRepeat = 0;
					change("Stop");
				}
			}
		}, UI.scale(0, y));

		statusLabel = add(new Label("Idle"), UI.scale(70, y + 3));
		pack();
	}

	@Override
	protected String windowPrefKey() {
		return "wndc-combatRotationBotWindow";
	}

	@Override
	protected void onCleanup() {
		gui.combatRotationBot = null;
	}

	/** Skip idlePlayer — we're in combat. */
	@Override
	public void stop() {
		stop = true;
		active = false;
		if (botThread != null) {
			botThread.interrupt();
			botThread = null;
		}
	}

	private void refreshStepLabels() {
		synchronized (steps) {
		for (int i = 0; i < MAX_DISPLAY_STEPS; i++) {
			if (i < steps.size()) {
				int[] step = steps.get(i);
				String name = getActionName(step[0]);
				String prefix = (i == selectedStep) ? "> " : "  ";
				stepLabels[i].settext(prefix + (i + 1) + ". Slot " + (step[0] + 1) + " (" + name + ") x" + step[1]);
				if (i == currentStep && active) {
					stepLabels[i].setcolor(ACTIVE_COLOR);
				} else if (i == selectedStep) {
					stepLabels[i].setcolor(SELECTED_COLOR);
				} else {
					stepLabels[i].setcolor(NORMAL_COLOR);
				}
			} else {
				stepLabels[i].settext("");
			}
		}
		} // synchronized
	}

	private void addStep() {
		try {
			int slot = Integer.parseInt(slotEntry.text().trim()) - 1;
			int count = Integer.parseInt(countEntry.text().trim());
			if (slot < 0 || slot > 9) {
				gui.errorsilent("Slot must be 1-10");
				return;
			}
			if (count < 1 || count > 99) {
				gui.errorsilent("Count must be 1-99");
				return;
			}
			if (steps.size() >= MAX_DISPLAY_STEPS) {
				gui.errorsilent("Max " + MAX_DISPLAY_STEPS + " steps");
				return;
			}
			steps.add(new int[]{slot, count});
			saveRotation();
			refreshStepLabels();
		} catch (NumberFormatException e) {
			gui.errorsilent("Invalid number");
		}
	}

	private void removeStep() {
		if (selectedStep >= 0 && selectedStep < steps.size()) {
			steps.remove(selectedStep);
			selectedStep = -1;
			saveRotation();
			refreshStepLabels();
		}
	}

	private void moveStep(int dir) {
		int target = selectedStep + dir;
		if (selectedStep >= 0 && selectedStep < steps.size() && target >= 0 && target < steps.size()) {
			int[] temp = steps.get(selectedStep);
			steps.set(selectedStep, steps.get(target));
			steps.set(target, temp);
			selectedStep = target;
			saveRotation();
			refreshStepLabels();
		}
	}

	private String getActionName(int slot) {
		try {
			Fightsess fs = gui.fs;
			if (fs != null && slot < fs.actions.length && fs.actions[slot] != null) {
				Resource res = fs.actions[slot].res.get();
				if (res != null) {
					return res.flayer(Resource.tooltip).t;
				}
			}
		} catch (Exception ignored) {}
		return "?";
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				refreshStepLabels();

				if (!active || steps.isEmpty()) {
					setStatus(active ? "No steps" : "Idle");
					Thread.sleep(200);
					continue;
				}

				// Check combat state
				if (gui.fv == null || gui.fv.current == null) {
					setStatus("Waiting for combat...");
					Thread.sleep(300);
					continue;
				}

				if (gui.fs == null) {
					setStatus("No combat session");
					Thread.sleep(300);
					continue;
				}

				if (currentStep >= steps.size()) {
					if (loopRotation) {
						currentStep = 0;
						currentRepeat = 0;
					} else {
						setStatus("Rotation complete");
						deactivate();
						continue;
					}
				}

				if (currentStep >= steps.size()) continue;
				int[] step = steps.get(currentStep);
				int slot = step[0];
				int repeats = step[1];
				String name = getActionName(slot);

				setStatus(name + " (" + (currentRepeat + 1) + "/" + repeats + ")");

				// Wait for cooldown — re-fetch fs in case combat state changed
				Fightsess fsCD = gui.fs;
				if (waitForCooldowns && fsCD != null && slot < fsCD.actions.length && fsCD.actions[slot] != null) {
					double ct = fsCD.actions[slot].ct;
					double now = Utils.rtime();
					if (ct > now) {
						long waitMs = (long) ((ct - now) * 1000) + 50;
						Thread.sleep(Math.min(waitMs, 3000));
						continue;
					}
				}

				// Execute the move
				executeMove(slot);

				currentRepeat++;
				if (currentRepeat >= repeats) {
					currentRepeat = 0;
					currentStep++;
				}

				// Brief delay between moves
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			// Unexpected error — stop gracefully
		}
	}

	private void executeMove(int slot) {
		// Fetch fresh combat session — it can change or end at any time
		Fightsess fs = gui.fs;
		if (fs == null || slot >= fs.actions.length || fs.actions[slot] == null) return;

		// Get opponent position for targeted moves
		Coord2d targetPos = null;
		try {
			if (gui.fv != null && gui.fv.current != null) {
				Gob enemy = gui.map.glob.oc.getgob(gui.fv.current.gobid);
				if (enemy != null) {
					targetPos = new Coord2d(enemy.rc.x, enemy.rc.y);
				}
			}
		} catch (Exception ignored) {}

		// Send "use" message (press)
		if (targetPos != null) {
			fs.wdgmsg("use", slot, 1, 0, targetPos.floor(posres));
		} else {
			fs.wdgmsg("use", slot, 1, 0);
		}

		// Brief hold, then release — re-fetch fs in case combat ended during sleep
		try { Thread.sleep(60); } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		Fightsess fs2 = gui.fs;
		if (fs2 != null) {
			fs2.wdgmsg("rel", slot);
		}
	}

	// --- Persistence ---

	private void saveRotation() {
		StringBuilder sb = new StringBuilder();
		for (int[] step : steps) {
			if (sb.length() > 0) sb.append(";");
			sb.append(step[0]).append(",").append(step[1]);
		}
		Utils.setpref("combatRot_steps", sb.toString());
	}

	private void loadRotation() {
		loopRotation = Utils.getprefb("combatRot_loop", true);
		waitForCooldowns = Utils.getprefb("combatRot_waitCD", true);
		String data = Utils.getpref("combatRot_steps", "");
		if (data.isEmpty()) return;
		for (String part : data.split(";")) {
			String[] kv = part.split(",");
			if (kv.length == 2) {
				try {
					int slot = Integer.parseInt(kv[0]);
					int count = Integer.parseInt(kv[1]);
					if (slot >= 0 && slot <= 9 && count >= 1 && count <= 99) {
						steps.add(new int[]{slot, count});
					}
				} catch (NumberFormatException ignored) {}
			}
		}
	}
}
