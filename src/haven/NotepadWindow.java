package haven;

import org.json.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class NotepadWindow extends Window {
	private static class Note {
		String title;
		final List<String> lines = new ArrayList<>();
		Note(String title) { this.title = title; }
	}

	private final List<Note> notes = new ArrayList<>();
	private int selectedNote = 0;
	private int contentScroll = 0;
	private int listScroll = 0;
	private int contentVisLines;

	private static final int LIST_W = UI.scale(100);
	private static final int CONTENT_W = UI.scale(260);
	private static final int GAP = UI.scale(8);
	private static final int LINE_H = UI.scale(14);
	private static final int PANEL_H = UI.scale(14) * 14; // 14 lines tall
	private static final int LIST_ITEM_H = UI.scale(20);
	private static final int PAD = UI.scale(5);
	private static final int MAX_NOTES = 50;

	private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 11).aa(true);
	private static final String PREF_KEY = "notepad_notes_v2";
	private static final String PREF_KEY_OLD = "notepad_text";

	private TextEntry titleEntry;
	private TextEntry inputEntry;

	public NotepadWindow() {
		super(Coord.z, "Notepad");
		loadNotes();
		if (notes.isEmpty())
			notes.add(new Note("Note 1"));
		buildUI();
	}

	private void buildUI() {
		int contentX = PAD + LIST_W + GAP;
		int y = 0;

		// === Note list (left panel) ===
		add(new Widget(new Coord(LIST_W, PANEL_H)) {
			@Override
			public void draw(GOut g) {
				g.chcolor(0, 0, 0, 160);
				g.frect(Coord.z, sz);
				g.chcolor(80, 80, 80, 255);
				g.rect(Coord.z, sz);
				g.chcolor();

				int maxVis = sz.y / LIST_ITEM_H;
				int start = Math.max(0, listScroll);
				int end = Math.min(notes.size(), start + maxVis);
				for (int i = start; i < end; i++) {
					int yOff = (i - start) * LIST_ITEM_H;
					if (i == selectedNote) {
						g.chcolor(60, 90, 140, 200);
						g.frect(new Coord(1, yOff + 1), new Coord(sz.x - 2, LIST_ITEM_H - 1));
						g.chcolor();
					}
					try {
						g.image(fnd.render(notes.get(i).title, Color.WHITE).tex(),
								new Coord(UI.scale(4), yOff + UI.scale(3)));
					} catch (Exception ignored) {}
				}

				if (notes.size() > maxVis) {
					int thumbH = Math.max(UI.scale(10), sz.y * maxVis / notes.size());
					int maxScr = Math.max(1, notes.size() - maxVis);
					int thumbY = (int) ((double) listScroll / maxScr * (sz.y - thumbH));
					g.chcolor(120, 120, 120, 180);
					g.frect(new Coord(sz.x - UI.scale(5), thumbY), new Coord(UI.scale(3), thumbH));
					g.chcolor();
				}
			}

			@Override
			public boolean mousedown(MouseDownEvent ev) {
				if (ev.b == 1) {
					int idx = listScroll + ev.c.y / LIST_ITEM_H;
					if (idx >= 0 && idx < notes.size() && idx != selectedNote)
						selectNote(idx);
					return true;
				}
				return super.mousedown(ev);
			}

			@Override
			public boolean mousewheel(MouseWheelEvent ev) {
				int maxVis = sz.y / LIST_ITEM_H;
				listScroll = Math.max(0, Math.min(notes.size() - maxVis, listScroll + ev.a));
				return true;
			}
		}, new Coord(PAD, y));

		// === Title entry (top of right panel) ===
		titleEntry = add(new TextEntry(CONTENT_W, notes.get(selectedNote).title) {
			@Override
			public boolean keydown(KeyDownEvent ev) {
				if (ev.awt.getKeyCode() == KeyEvent.VK_ENTER) {
					commitTitle();
					saveNotes();
					parent.setfocus(inputEntry);
					return true;
				}
				return super.keydown(ev);
			}
		}, new Coord(contentX, y));
		titleEntry.settip("Note title (Enter to save)");

		int titleH = titleEntry.sz.y;
		int contentY = y + titleH + UI.scale(2);
		int contentH = PANEL_H - titleH - UI.scale(2);
		contentVisLines = contentH / LINE_H;

		// === Content display (right panel) ===
		add(new Widget(new Coord(CONTENT_W, contentH)) {
			@Override
			public void draw(GOut g) {
				g.chcolor(0, 0, 0, 180);
				g.frect(Coord.z, sz);
				g.chcolor(80, 80, 80, 255);
				g.rect(Coord.z, sz);
				g.chcolor();

				Note note = getCurrentNote();
				if (note == null) return;

				int vl = sz.y / LINE_H;
				int startLine = Math.max(0, contentScroll);
				int endLine = Math.min(note.lines.size(), startLine + vl);
				for (int i = startLine; i < endLine; i++) {
					int yOff = (i - startLine) * LINE_H;
					try {
						String line = note.lines.get(i);
						Color col = Color.WHITE;
						// Highlight calc results in a different color
						if (line.contains(" = ") && !line.startsWith(" "))
							col = new Color(180, 220, 255);
						g.image(fnd.render(line, col).tex(),
								new Coord(UI.scale(3), yOff + UI.scale(1)));
					} catch (Exception ignored) {}
				}

				if (note.lines.size() > vl) {
					int thumbH = Math.max(UI.scale(10), sz.y * vl / note.lines.size());
					int maxScr = Math.max(1, note.lines.size() - vl);
					int thumbY = (int) ((double) contentScroll / maxScr * (sz.y - thumbH));
					g.chcolor(120, 120, 120, 180);
					g.frect(new Coord(sz.x - UI.scale(6), thumbY), new Coord(UI.scale(4), thumbH));
					g.chcolor();
				}
			}

			@Override
			public boolean mousedown(MouseDownEvent ev) {
				if (ev.b == 3) {
					Note note = getCurrentNote();
					if (note == null) return true;
					int idx = contentScroll + ev.c.y / LINE_H;
					if (idx >= 0 && idx < note.lines.size()) {
						note.lines.remove(idx);
						int vl = sz.y / LINE_H;
						if (contentScroll > 0 && contentScroll >= note.lines.size() - vl)
							contentScroll = Math.max(0, note.lines.size() - vl);
						saveNotes();
					}
					return true;
				}
				return super.mousedown(ev);
			}

			@Override
			public boolean mousewheel(MouseWheelEvent ev) {
				Note note = getCurrentNote();
				if (note != null) {
					int vl = sz.y / LINE_H;
					contentScroll = Math.max(0, Math.min(note.lines.size() - vl, contentScroll + ev.a));
				}
				return true;
			}
		}, new Coord(contentX, contentY));

		y = contentY + contentH + PAD;

		// === Input entry ===
		inputEntry = add(new TextEntry(CONTENT_W, "") {
			@Override
			public boolean keydown(KeyDownEvent ev) {
				if (ev.awt.getKeyCode() == KeyEvent.VK_ENTER) {
					String text = text();
					if (!text.isEmpty())
						addLine(text);
					settext("");
					return true;
				}
				return super.keydown(ev);
			}
		}, new Coord(contentX, y));
		inputEntry.settip("Type text + Enter to add. '=expr' to calculate (e.g. =sqrt(100), =qeff(50))");
		y += inputEntry.sz.y + PAD;

		// === Buttons ===
		int btnW = UI.scale(42);
		int btnGap = UI.scale(3);
		int bx = PAD;

		add(new Button(btnW, "New") {
			public void click() {
				if (notes.size() >= MAX_NOTES) return;
				notes.add(new Note("Note " + (notes.size() + 1)));
				selectNote(notes.size() - 1);
				saveNotes();
			}
		}, new Coord(bx, y));
		bx += btnW + btnGap;

		add(new Button(btnW, "Del") {
			public void click() {
				if (notes.size() <= 1) return;
				notes.remove(selectedNote);
				if (selectedNote >= notes.size())
					selectedNote = notes.size() - 1;
				contentScroll = 0;
				updateTitleEntry();
				saveNotes();
			}
		}, new Coord(bx, y));
		bx += btnW + btnGap;

		add(new Button(btnW + UI.scale(6), "Clear") {
			public void click() {
				Note note = getCurrentNote();
				if (note != null) {
					note.lines.clear();
					contentScroll = 0;
					saveNotes();
				}
			}
		}, new Coord(bx, y));

		pack();
		setfocus(inputEntry);
	}

	private Note getCurrentNote() {
		if (selectedNote >= 0 && selectedNote < notes.size())
			return notes.get(selectedNote);
		return null;
	}

	private void selectNote(int idx) {
		commitTitle();
		selectedNote = idx;
		contentScroll = 0;
		updateTitleEntry();
	}

	private void commitTitle() {
		Note note = getCurrentNote();
		if (note != null && titleEntry != null) {
			String t = titleEntry.text().trim();
			if (!t.isEmpty())
				note.title = t;
		}
	}

	private void updateTitleEntry() {
		Note note = getCurrentNote();
		if (note != null && titleEntry != null)
			titleEntry.settext(note.title);
	}

	private void addLine(String text) {
		Note note = getCurrentNote();
		if (note == null) return;

		// Calculator: lines starting with '=' are evaluated
		if (text.startsWith("=")) {
			String expr = text.substring(1).trim();
			if (!expr.isEmpty()) {
				try {
					double result = evalExpression(expr);
					String resultStr;
					if (result == Math.floor(result) && !Double.isInfinite(result)
							&& Math.abs(result) < 1e15)
						resultStr = String.valueOf((long) result);
					else
						resultStr = String.format("%.4f", result);
					text = expr + " = " + resultStr;
				} catch (Exception e) {
					text = expr + " = Error";
				}
			}
		}

		note.lines.add(text);
		if (note.lines.size() > contentVisLines)
			contentScroll = note.lines.size() - contentVisLines;
		saveNotes();
	}

	// === Persistence ===

	private void saveNotes() {
		commitTitle();
		JSONArray arr = new JSONArray();
		for (Note note : notes) {
			JSONObject obj = new JSONObject();
			obj.put("title", note.title);
			JSONArray lines = new JSONArray();
			for (String line : note.lines)
				lines.put(line);
			obj.put("lines", lines);
			arr.put(obj);
		}
		Utils.setpref(PREF_KEY, arr.toString());
	}

	private void loadNotes() {
		String saved = Utils.getpref(PREF_KEY, "");
		if (!saved.isEmpty()) {
			try {
				JSONArray arr = new JSONArray(saved);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					Note note = new Note(obj.getString("title"));
					JSONArray lines = obj.getJSONArray("lines");
					for (int j = 0; j < lines.length(); j++)
						note.lines.add(lines.getString(j));
					notes.add(note);
				}
				return;
			} catch (Exception ignored) {}
		}
		// Migrate from old single-note format
		String old = Utils.getpref(PREF_KEY_OLD, "");
		if (!old.isEmpty()) {
			Note note = new Note("Note 1");
			for (String line : old.split("\n", -1))
				note.lines.add(line);
			notes.add(note);
		}
	}

	// === Expression evaluator ===
	// Supports: +, -, *, /, %, ^, parentheses
	// Functions: sqrt, abs, ceil, floor, round, log
	// H&H specific: qeff(q) = sqrt(q/10) — quality effectiveness

	private static double evalExpression(String expr) {
		return new Object() {
			int pos = -1;
			int ch;

			void nextChar() {
				ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
			}

			boolean eat(int c) {
				while (ch == ' ') nextChar();
				if (ch == c) { nextChar(); return true; }
				return false;
			}

			double parse() {
				nextChar();
				double x = parseExpr();
				if (pos < expr.length())
					throw new RuntimeException("Unexpected");
				return x;
			}

			double parseExpr() {
				double x = parseTerm();
				for (;;) {
					if (eat('+')) x += parseTerm();
					else if (eat('-')) x -= parseTerm();
					else return x;
				}
			}

			double parseTerm() {
				double x = parsePower();
				for (;;) {
					if (eat('*')) x *= parsePower();
					else if (eat('/')) x /= parsePower();
					else if (eat('%')) x %= parsePower();
					else return x;
				}
			}

			double parsePower() {
				double x = parseUnary();
				if (eat('^')) x = Math.pow(x, parsePower()); // right-associative
				return x;
			}

			double parseUnary() {
				if (eat('-')) return -parseUnary();
				return parseAtom();
			}

			double parseAtom() {
				while (ch == ' ') nextChar();
				if (eat('(')) {
					double x = parseExpr();
					eat(')');
					return x;
				}
				if ((ch >= '0' && ch <= '9') || ch == '.') {
					int start = this.pos;
					while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
					return Double.parseDouble(expr.substring(start, this.pos));
				}
				if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
					int start = this.pos;
					while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) nextChar();
					String func = expr.substring(start, this.pos).toLowerCase();
					// Constants
					if ("pi".equals(func)) return Math.PI;
					if ("e".equals(func)) return Math.E;
					// Functions
					eat('(');
					double arg = parseExpr();
					eat(')');
					switch (func) {
						case "sqrt": return Math.sqrt(arg);
						case "qeff": return Math.sqrt(arg / 10.0);
						case "abs": return Math.abs(arg);
						case "ceil": return Math.ceil(arg);
						case "floor": return Math.floor(arg);
						case "round": return Math.round(arg);
						case "log": return Math.log(arg);
						case "log10": return Math.log10(arg);
						default: throw new RuntimeException("Unknown: " + func);
					}
				}
				throw new RuntimeException("Unexpected");
			}
		}.parse();
	}

	@Override
	public void reqclose() {
		commitTitle();
		saveNotes();
		Utils.setprefc("wndc-notepadWindow", this.c);
		ui.gui.notepadWindow = null;
		reqdestroy();
	}
}
