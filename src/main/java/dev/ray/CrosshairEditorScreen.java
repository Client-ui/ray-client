package dev.ray;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Draw your own crosshair (normal + enemy) on an 11x11 grid. */
public class CrosshairEditorScreen extends Screen {
	private static final String[] TARGETS = {"Players", "Living", "Any"};
	private static final int PW = 340, PH = 244, CELL = 14;

	private record Hit(int x, int y, int w, int h, Runnable click) {
		boolean contains(int mx, int my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final Screen parent;
	private final List<Hit> hits = new ArrayList<>();
	private int editTab = 0; // 0 = normal, 1 = enemy
	private boolean mouseWasDown = true, gridDrag = false, paintValue = true;
	private int mx, my, px, py, gx, gy;

	public CrosshairEditorScreen(Screen parent) {
		super(Text.literal("Crosshair Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		mouseWasDown = true;
		gridDrag = false;
	}

	private String art() {
		RayConfig c = RayConfig.get();
		return editTab == 0 ? c.crosshairPixels : c.enemyPixels;
	}

	private void setArt(String a) {
		RayConfig c = RayConfig.get();
		if (editTab == 0) c.crosshairPixels = a; else c.enemyPixels = a;
	}

	private int colorIdx() {
		RayConfig c = RayConfig.get();
		return editTab == 0 ? c.crosshairColor : c.enemyColor;
	}

	private void setColor(int i) {
		RayConfig c = RayConfig.get();
		if (editTab == 0) c.crosshairColor = i; else c.enemyColor = i;
	}

	private static void border(DrawContext c, int x, int y, int w, int h, int col) {
		c.fill(x, y, x + w, y + 1, col);
		c.fill(x, y + h - 1, x + w, y + h, col);
		c.fill(x, y, x + 1, y + h, col);
		c.fill(x + w - 1, y, x + w, y + h, col);
	}

	private void btn(DrawContext ctx, int x, int y, int w, int h, String label, boolean active, Runnable action) {
		boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
		int bg = active ? Theme.withAlpha(Theme.accent(), 0x88) : (hov ? 0xFF1A2540 : 0xFF131C2E);
		ctx.fill(x, y, x + w, y + h, bg);
		ctx.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
		hits.add(new Hit(x, y, w, h, () -> { action.run(); RayConfig.save(); }));
	}

	private int cellAt(int x, int y) {
		int c = (x - gx) / CELL, r = (y - gy) / CELL;
		if (x < gx || y < gy || c < 0 || c >= CrosshairArt.N || r < 0 || r >= CrosshairArt.N) return -1;
		return r * CrosshairArt.N + c;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		RayConfig cfg = RayConfig.get();
		int accent = Theme.accent();
		mx = mouseX;
		my = mouseY;
		hits.clear();
		px = (width - PW) / 2;
		py = Math.max(0, (height - PH) / 2);
		gx = px + 12;
		gy = py + 48;

		ctx.fill(px, py, px + PW, py + PH, 0xF00A101C);
		border(ctx, px, py, PW, PH, Theme.withAlpha(accent, 0x99));
		ctx.drawText(textRenderer, "Crosshair Editor", px + 12, py + 10, 0xFFFFFFFF, true);

		// tabs
		btn(ctx, px + 12, py + 26, 60, 16, "Normal", editTab == 0, () -> editTab = 0);
		btn(ctx, px + 76, py + 26, 60, 16, "Enemy", editTab == 1, () -> editTab = 1);

		// grid
		String art = art();
		int col = CrosshairArt.color(colorIdx());
		ctx.fill(gx - 2, gy - 2, gx + CrosshairArt.N * CELL + 2, gy + CrosshairArt.N * CELL + 2, 0xFF0D1524);
		for (int r = 0; r < CrosshairArt.N; r++) {
			for (int c = 0; c < CrosshairArt.N; c++) {
				int x = gx + c * CELL, y = gy + r * CELL;
				boolean lit = CrosshairArt.get(art, r, c);
				ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, lit ? col : 0xFF1A2540);
			}
		}
		border(ctx, gx + 5 * CELL, gy + 5 * CELL, CELL, CELL, 0x66FFFFFF);

		// right column
		int rx = px + 182;
		ctx.drawText(textRenderer, "Color", rx, py + 28, 0xFF9FB0D0, false);
		for (int i = 0; i < CrosshairArt.colorCount(); i++) {
			final int idx = i;
			int sx = rx + i * 17, sy = py + 40;
			ctx.fill(sx, sy, sx + 14, sy + 14, CrosshairArt.color(i));
			if (colorIdx() == i) border(ctx, sx - 1, sy - 1, 16, 16, 0xFFFFFFFF);
			hits.add(new Hit(sx, sy, 14, 14, () -> { setColor(idx); RayConfig.save(); }));
		}

		// preview (dark | light background)
		ctx.fill(rx, py + 62, rx + 73, py + 126, 0xFF1E2430);
		ctx.fill(rx + 73, py + 62, rx + 146, py + 126, 0xFFB8C2D4);
		CrosshairArt.draw(ctx, art, rx + 73, py + 94, cfg.crosshairScale, col, cfg.crosshairOutline);

		// presets
		btn(ctx, rx, py + 134, 46, 14, "Clear", false, () -> setArt(CrosshairArt.blank()));
		btn(ctx, rx + 50, py + 134, 46, 14, "Cross", false, () -> setArt(CrosshairArt.cross()));
		btn(ctx, rx + 100, py + 134, 46, 14, "Dot", false, () -> setArt(CrosshairArt.dot()));
		btn(ctx, rx, py + 152, 46, 14, "Ring", false, () -> setArt(CrosshairArt.ring()));
		btn(ctx, rx + 50, py + 152, 46, 14, "X", false, () -> setArt(CrosshairArt.x()));
		btn(ctx, rx + 100, py + 152, 46, 14, "Scale " + cfg.crosshairScale, false,
			() -> cfg.crosshairScale = cfg.crosshairScale % 4 + 1);

		// toggles
		btn(ctx, rx, py + 172, 146, 14, "Outline: " + (cfg.crosshairOutline ? "ON" : "OFF"), cfg.crosshairOutline,
			() -> cfg.crosshairOutline = !cfg.crosshairOutline);
		if (editTab == 0) {
			btn(ctx, rx, py + 190, 146, 14, "Use Drawn: " + (cfg.crosshairDrawn ? "ON" : "OFF"), cfg.crosshairDrawn,
				() -> cfg.crosshairDrawn = !cfg.crosshairDrawn);
		} else {
			btn(ctx, rx, py + 190, 146, 14, "Enemy Crosshair: " + (cfg.enemyCrosshair ? "ON" : "OFF"), cfg.enemyCrosshair,
				() -> cfg.enemyCrosshair = !cfg.enemyCrosshair);
			btn(ctx, rx, py + 208, 146, 14, "Target: " + TARGETS[cfg.enemyTarget], false,
				() -> cfg.enemyTarget = (cfg.enemyTarget + 1) % TARGETS.length);
		}
		btn(ctx, gx, py + 210, 154, 20, "Done", false, this::close);

		String hint = "Click / drag on the grid to draw";
		ctx.drawText(textRenderer, hint, gx, py + 236 - 4, 0xFF6B7A99, false);

		// ---- mouse input (polled) ----
		long win = client.getWindow().getHandle();
		boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		if (down && !mouseWasDown) {
			boolean used = false;
			for (Hit h : hits) {
				if (h.contains(mouseX, mouseY)) {
					used = true;
					h.click().run();
					break;
				}
			}
			if (!used) {
				int cell = cellAt(mouseX, mouseY);
				if (cell >= 0) {
					gridDrag = true;
					paintValue = !CrosshairArt.get(art(), cell / CrosshairArt.N, cell % CrosshairArt.N);
				}
			}
		}
		if (down && gridDrag) {
			int cell = cellAt(mouseX, mouseY);
			if (cell >= 0) setArt(CrosshairArt.set(art(), cell / CrosshairArt.N, cell % CrosshairArt.N, paintValue));
		}
		if (!down && gridDrag) {
			gridDrag = false;
			RayConfig.save();
		}
		mouseWasDown = down;
	}

	@Override
	public void close() {
		RayConfig.save();
		client.setScreen(parent);
	}
}
