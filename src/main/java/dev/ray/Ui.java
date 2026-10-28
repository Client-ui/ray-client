package dev.ray;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Small immediate-mode UI toolkit shared by every Ray screen.
 *
 * Mouse input is polled through GLFW (left + right button) so nothing depends on the Screen click signatures,
 * which changed between Minecraft versions. Widgets are drawn and registered as "hits" every frame; call
 * {@link #poll()} at the end of the frame to dispatch clicks and slider drags.
 */
public final class Ui {
	public static final int H = 24;
	public static final int ROW = 28;

	private record Hit(String id, int x, int y, int w, int h, Runnable click, Runnable rclick, IntConsumer drag) {
		boolean contains(int px, int py) {
			return px >= x && px < x + w && py >= y && py < y + h;
		}
	}

	public DrawContext ctx;
	public TextRenderer tr;
	public MinecraftClient mc = MinecraftClient.getInstance();
	public int mx, my;
	/** Button state as of the last {@link #poll()}. */
	public boolean left, right, leftPressed, rightPressed;

	private boolean leftWas = true, rightWas = true;
	private String dragId = null;
	private final List<Hit> hits = new ArrayList<>();
	private boolean clipped = false;
	private int clipX1, clipY1, clipX2, clipY2;

	// ---------- frame lifecycle ----------

	/** Call from Screen#init so the click that opened the screen is not re-used. */
	public void reset() {
		leftWas = true;
		rightWas = true;
		dragId = null;
		left = right = leftPressed = rightPressed = false;
	}

	public void begin(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
		this.ctx = ctx;
		this.tr = tr;
		this.mx = mouseX;
		this.my = mouseY;
		hits.clear();
		clipped = false;
	}

	/** Reads the mouse and dispatches clicks / drags to the widgets drawn this frame. */
	public void poll() {
		long win = mc.getWindow().getHandle();
		left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		leftPressed = left && !leftWas;
		rightPressed = right && !rightWas;
		leftWas = left;
		rightWas = right;

		if (leftPressed) {
			for (Hit h : hits) {
				if (!h.contains(mx, my)) continue;
				if (h.click() == null && h.drag() == null) continue;
				if (h.click() != null) {
					h.click().run();
					return; // the click may have opened another screen
				}
				dragId = h.id(); // drag-only widget (slider): applied just below
				break;
			}
		}
		if (rightPressed) {
			for (Hit h : hits) {
				if (h.contains(mx, my) && h.rclick() != null) {
					h.rclick().run();
					return;
				}
			}
		}
		if (left && dragId != null) {
			for (Hit h : hits) {
				if (h.id().equals(dragId) && h.drag() != null) {
					h.drag().accept(mx);
					break;
				}
			}
		}
		if (!left && dragId != null) {
			dragId = null;
			RayConfig.save();
		}
	}

	// ---------- clipping (scrolling areas) ----------

	public void pushClip(int x, int y, int w, int h) {
		ctx.enableScissor(x, y, x + w, y + h);
		clipped = true;
		clipX1 = x;
		clipY1 = y;
		clipX2 = x + w;
		clipY2 = y + h;
	}

	public void popClip() {
		ctx.disableScissor();
		clipped = false;
	}

	// ---------- hit registration ----------

	public void hit(String id, int x, int y, int w, int h, Runnable click, Runnable rclick, IntConsumer drag) {
		int x1 = x, y1 = y, x2 = x + w, y2 = y + h;
		if (clipped) {
			x1 = Math.max(x1, clipX1);
			y1 = Math.max(y1, clipY1);
			x2 = Math.min(x2, clipX2);
			y2 = Math.min(y2, clipY2);
		}
		if (x2 <= x1 || y2 <= y1) return;
		hits.add(new Hit(id, x1, y1, x2 - x1, y2 - y1, click, rclick, drag));
	}

	/** Left-click hit that also saves the config afterwards. */
	public void click(String id, int x, int y, int w, int h, Runnable action) {
		hit(id, x, y, w, h, () -> {
			action.run();
			RayConfig.save();
		}, null, null);
	}

	public boolean over(int x, int y, int w, int h) {
		if (clipped && (mx < clipX1 || mx >= clipX2 || my < clipY1 || my >= clipY2)) return false;
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---------- drawing primitives ----------

	public void rect(int x, int y, int w, int h, int c) {
		ctx.fill(x, y, x + w, y + h, c);
	}

	/** Filled rectangle with softly cut corners. */
	public void rrect(int x, int y, int w, int h, int r, int c) {
		if (w <= 0 || h <= 0) return;
		r = Math.min(r, Math.min(w, h) / 2);
		if (r <= 0) {
			ctx.fill(x, y, x + w, y + h, c);
			return;
		}
		ctx.fill(x, y + r, x + w, y + h - r, c);
		for (int i = 0; i < r; i++) {
			double d = r - i - 0.5;
			int inset = r - (int) Math.round(Math.sqrt(r * r - d * d));
			ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
			ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
		}
	}

	/** Rounded panel with a 1px stroke. The FILL must be opaque (the stroke is drawn as a full layer underneath it). */
	public void panel(int x, int y, int w, int h, int r, int fill, int stroke) {
		rrect(x, y, w, h, r, stroke);
		rrect(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
	}

	public void border(int x, int y, int w, int h, int c) {
		ctx.fill(x, y, x + w, y + 1, c);
		ctx.fill(x, y + h - 1, x + w, y + h, c);
		ctx.fill(x, y, x + 1, y + h, c);
		ctx.fill(x + w - 1, y, x + w, y + h, c);
	}

	/** Draws a 1-bit pixel picture ('#' = lit) with the given pixel size. */
	public void pix(String[] rows, int x, int y, int size, int c) {
		for (int r = 0; r < rows.length; r++) {
			String row = rows[r];
			for (int col = 0; col < row.length(); col++) {
				if (row.charAt(col) == '#') ctx.fill(x + col * size, y + r * size, x + (col + 1) * size, y + (r + 1) * size, c);
			}
		}
	}

	private static final String[] HEART = {".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};
	private static final String[] CLOSE = {"#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"};
	private static final String[] BACK = {"...#", "..##", ".###", "####", ".###", "..##", "...#"};

	public void heart(int x, int y, int c) {
		pix(HEART, x, y, 1, c);
	}

	public void closeIcon(int x, int y, int c) {
		pix(CLOSE, x, y, 1, c);
	}

	public void backIcon(int x, int y, int c) {
		pix(BACK, x, y, 1, c);
	}

	/** Small "sliders" glyph used for the settings button. */
	public void slidersIcon(int x, int y, int c) {
		ctx.fill(x, y + 1, x + 10, y + 2, c);
		ctx.fill(x, y + 4, x + 10, y + 5, c);
		ctx.fill(x, y + 7, x + 10, y + 8, c);
		ctx.fill(x + 6, y, x + 8, y + 3, c);
		ctx.fill(x + 2, y + 3, x + 4, y + 6, c);
		ctx.fill(x + 5, y + 6, x + 7, y + 9, c);
	}

	// ---------- text ----------

	public int w(String s) {
		return tr.getWidth(s);
	}

	public void text(String s, int x, int y, int c) {
		ctx.drawText(tr, s, x, y, c, false);
	}

	public void textShadow(String s, int x, int y, int c) {
		ctx.drawText(tr, s, x, y, c, true);
	}

	public void center(String s, int cx, int y, int c) {
		ctx.drawText(tr, s, cx - tr.getWidth(s) / 2, y, c, false);
	}

	public void right(String s, int rx, int y, int c) {
		ctx.drawText(tr, s, rx - tr.getWidth(s), y, c, false);
	}

	public String fit(String s, int maxW) {
		if (tr.getWidth(s) <= maxW) return s;
		String e = "...";
		while (s.length() > 1 && tr.getWidth(s + e) > maxW) s = s.substring(0, s.length() - 1);
		return s + e;
	}

	public void scaledText(String s, int x, int y, float scale, int c, boolean shadow) {
		ctx.getMatrices().pushMatrix();
		ctx.getMatrices().translate((float) x, (float) y);
		ctx.getMatrices().scale(scale, scale);
		ctx.drawText(tr, s, 0, 0, c, shadow);
		ctx.getMatrices().popMatrix();
	}

	// ---------- small widgets ----------

	/** Filter chip. Returns its width so callers can lay chips out in a row. */
	public int chip(int x, int y, String label, boolean active, Runnable click) {
		int w = tr.getWidth(label) + 12;
		boolean hov = over(x, y, w, 14);
		int accent = Theme.accent();
		if (active) {
			rrect(x, y, w, 14, 3, Theme.fade(accent, 0.30f));
			border(x + 1, y + 13, w - 2, 1, accent);
		} else {
			rrect(x, y, w, 14, 3, hov ? Theme.CARD_HOVER : Theme.CARD);
		}
		text(label, x + 6, y + 3, active ? 0xFFFFFFFF : Theme.DIM);
		click("chip" + label, x, y, w, 14, click);
		return w;
	}

	/** Square icon button; draw the glyph yourself via the returned hover flag. */
	public boolean iconButton(int x, int y, int w, int h, int base, int hover, Runnable click) {
		boolean hov = over(x, y, w, h);
		rrect(x, y, w, h, 3, hov ? hover : base);
		click("ib" + x + "," + y, x, y, w, h, click);
		return hov;
	}

	public void labelButton(int x, int y, int w, int h, String label, int color, Runnable action) {
		boolean hov = over(x, y, w, h);
		rrect(x, y, w, h, 3, Theme.fade(color, hov ? 1.0f : 0.78f));
		center(label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
		click("lb" + label + x + "," + y, x, y, w, h, action);
	}

	public void scrollbar(int x, int y, int h, int viewH, int contentH, int scroll) {
		if (contentH <= viewH) return;
		rrect(x, y, 3, h, 1, 0x33FFFFFF);
		int th = Math.max(14, h * viewH / contentH);
		int ty = y + (int) ((long) (h - th) * scroll / Math.max(1, contentH - viewH));
		rrect(x, ty, 3, th, 1, Theme.accent());
	}

	// ---------- settings rows (r = {x, y, width}) ----------

	private void card(int[] r) {
		boolean hov = over(r[0], r[1], r[2], H);
		panel(r[0], r[1], r[2], H, 3, hov ? Theme.CARD_HOVER : Theme.CARD, Theme.STROKE);
	}

	public void toggle(int[] r, String name, boolean on, Runnable flip) {
		card(r);
		text(fit(name, r[2] - 50), r[0] + 8, r[1] + 8, Theme.TEXT);
		int tx = r[0] + r[2] - 32, ty = r[1] + 7;
		rrect(tx, ty, 24, 10, 3, on ? Theme.accent() : 0xFF2B3248);
		rrect(on ? tx + 14 : tx + 2, ty + 1, 8, 8, 2, 0xFFFFFFFF);
		click("t" + name, r[0], r[1], r[2], H, flip);
	}

	public void choice(int[] r, String name, String value, Runnable next) {
		card(r);
		String v = fit(value, r[2] / 2);
		text(fit(name, r[2] - w(v) - 24), r[0] + 8, r[1] + 8, Theme.TEXT);
		right(v, r[0] + r[2] - 8, r[1] + 8, Theme.accent());
		click("c" + name, r[0], r[1], r[2], H, next);
	}

	public void colorChoice(int[] r, String name, String valueName, int swatch, Runnable next) {
		card(r);
		text(fit(name, r[2] - 70), r[0] + 8, r[1] + 8, Theme.TEXT);
		rrect(r[0] + r[2] - 22, r[1] + 5, 14, 14, 3, swatch);
		right(valueName, r[0] + r[2] - 28, r[1] + 8, Theme.DIM);
		click("cc" + name, r[0], r[1], r[2], H, next);
	}

	/** Slider with a fully custom value label. */
	public void sliderShown(int[] r, String name, int min, int max, int value, String shown, IntConsumer set) {
		card(r);
		text(fit(name, r[2] - w(shown) - 20), r[0] + 8, r[1] + 4, Theme.TEXT);
		right(shown, r[0] + r[2] - 8, r[1] + 4, Theme.DIM);
		final int bx = r[0] + 8, bw = r[2] - 16, by = r[1] + 16;
		rrect(bx, by, bw, 4, 2, 0xFF2B3248);
		int fill = (int) ((long) (value - min) * bw / Math.max(1, max - min));
		fill = Math.max(0, Math.min(bw, fill));
		rrect(bx, by, Math.max(2, fill), 4, 2, Theme.accent());
		rrect(bx + fill - 2, by - 2, 5, 8, 2, 0xFFFFFFFF);
		hit("s" + name, r[0], r[1] + 10, r[2], 14, null, null, m -> {
			int nv = min + Math.round((m - bx) * (max - min) / (float) bw);
			set.accept(Math.max(min, Math.min(max, nv)));
		});
	}

	/** Slider whose label is the value followed by a suffix (e.g. "%"). */
	public void slider(int[] r, String name, int min, int max, int value, String suffix, IntConsumer set) {
		sliderShown(r, name, min, max, value, value + suffix, set);
	}

	public void slider(int[] r, String name, int min, int max, int value, IntConsumer set) {
		sliderShown(r, name, min, max, value, String.valueOf(value), set);
	}

	public void button(int[] r, String label, Runnable action) {
		boolean hov = over(r[0], r[1], r[2], H);
		int accent = Theme.accent();
		rrect(r[0], r[1], r[2], H, 3, Theme.fade(accent, hov ? 0.85f : 0.55f));
		center(label, r[0] + r[2] / 2, r[1] + 8, 0xFFFFFFFF);
		click("b" + label, r[0], r[1], r[2], H, action);
	}

	/** Two-line wrapped hint text; returns the pixel height used. */
	public int note(String s, int x, int y, int w) {
		int yy = y;
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String next = line.length() == 0 ? word : line + " " + word;
			if (tr.getWidth(next) > w && line.length() > 0) {
				text(line.toString(), x, yy, Theme.MUTE);
				yy += 11;
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(next);
			}
		}
		if (line.length() > 0) {
			text(line.toString(), x, yy, Theme.MUTE);
			yy += 11;
		}
		return yy - y;
	}

	/** Row layout helper: two columns of cards, full-width rows, section headings, wrapped notes. */
	public static final class Form {
		private final Ui ui;
		private final int x, w;
		private int y, col;

		public Form(Ui ui, int x, int y, int w) {
			this.ui = ui;
			this.x = x;
			this.y = y;
			this.w = w;
		}

		private void flush() {
			if (col != 0) {
				col = 0;
				y += ROW;
			}
		}

		public int[] half() {
			int colW = (w - 6) / 2;
			int[] r = {x + col * (colW + 6), y, colW};
			if (++col == 2) {
				col = 0;
				y += ROW;
			}
			return r;
		}

		public int[] full() {
			flush();
			int[] r = {x, y, w};
			y += ROW;
			return r;
		}

		public void section(String title) {
			flush();
			y += 3;
			String t = title.toUpperCase();
			ui.text(t, x, y, Theme.MUTE);
			int lx = x + ui.w(t) + 6;
			ui.rect(lx, y + 4, Math.max(0, x + w - lx), 1, Theme.STROKE);
			y += 14;
		}

		public void note(String s) {
			flush();
			y += ui.note(s, x, y, w) + 5;
		}

		public int x() {
			return x;
		}

		/** Reserves a custom-height row and returns its top y. */
		public int reserve(int h) {
			flush();
			int top = y;
			y += h;
			return top;
		}

		public int bottom() {
			flush();
			return y;
		}
	}
}
