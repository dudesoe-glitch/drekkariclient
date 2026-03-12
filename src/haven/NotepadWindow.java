package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class NotepadWindow extends Window {
	private final List<String> lines = new ArrayList<>();
	private TextEntry inputEntry;
	private int scrollOffset = 0;
	private static final int VISIBLE_LINES = 12;
	private static final int LINE_H = UI.scale(14);
	private static final int TEXT_AREA_W = UI.scale(260);
	private static final int TEXT_AREA_H = VISIBLE_LINES * LINE_H;
	private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 11).aa(true);
	private static final String PREF_KEY = "notepad_text";

	public NotepadWindow() {
		super(UI.scale(280, 260), "Notepad");

		// Load saved text
		String saved = Utils.getpref(PREF_KEY, "");
		if (!saved.isEmpty()) {
			for (String line : saved.split("\n", -1))
				lines.add(line);
		}

		int y = UI.scale(5);
		// Text display area is drawn manually
		y += TEXT_AREA_H + UI.scale(5);

		// Input entry
		inputEntry = add(new TextEntry(TEXT_AREA_W, "") {
			@Override
			public boolean keydown(KeyDownEvent ev) {
				if (ev.awt.getKeyCode() == KeyEvent.VK_ENTER) {
					addLine(text());
					settext("");
					return true;
				}
				return super.keydown(ev);
			}
		}, UI.scale(10, y));
		y += UI.scale(25);

		// Buttons
		add(new Button(UI.scale(50), "Clear") {
			public void click() {
				lines.clear();
				scrollOffset = 0;
				save();
			}
		}, UI.scale(10, y));
	}

	private void addLine(String text) {
		if (text.isEmpty()) return;
		lines.add(text);
		// Auto-scroll to bottom
		if (lines.size() > VISIBLE_LINES)
			scrollOffset = lines.size() - VISIBLE_LINES;
		save();
	}

	private void save() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) sb.append('\n');
			sb.append(lines.get(i));
		}
		Utils.setpref(PREF_KEY, sb.toString());
	}

	@Override
	public void draw(GOut g) {
		super.draw(g);

		// Draw text area background
		Coord areaPos = UI.scale(new Coord(10, 5));
		Coord areaSz = new Coord(TEXT_AREA_W, TEXT_AREA_H);
		g.chcolor(0, 0, 0, 180);
		g.frect(areaPos, areaSz);
		g.chcolor(80, 80, 80, 255);
		g.rect(areaPos, areaSz);
		g.chcolor();

		// Draw lines
		int startLine = Math.max(0, scrollOffset);
		int endLine = Math.min(lines.size(), startLine + VISIBLE_LINES);
		for (int i = startLine; i < endLine; i++) {
			int yOff = (i - startLine) * LINE_H;
			try {
				Tex t = fnd.render(lines.get(i), Color.WHITE).tex();
				g.image(t, areaPos.add(UI.scale(3), yOff + UI.scale(1)));
				t.dispose();
			} catch (Exception ignored) {}
		}

		// Scrollbar indicator
		if (lines.size() > VISIBLE_LINES) {
			int totalH = TEXT_AREA_H;
			int thumbH = Math.max(UI.scale(10), totalH * VISIBLE_LINES / lines.size());
			int thumbY = (int) ((double) scrollOffset / (lines.size() - VISIBLE_LINES) * (totalH - thumbH));
			g.chcolor(120, 120, 120, 180);
			g.frect(areaPos.add(TEXT_AREA_W - UI.scale(5), thumbY), new Coord(UI.scale(4), thumbH));
			g.chcolor();
		}
	}

	@Override
	public boolean mousewheel(MouseWheelEvent ev) {
		scrollOffset = Math.max(0, Math.min(lines.size() - VISIBLE_LINES, scrollOffset + ev.a));
		return true;
	}

	@Override
	public void reqdestroy() {
		Utils.setprefc("wndc-notepadWindow", this.c);
		super.reqdestroy();
	}
}
