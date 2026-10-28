package dev.ray;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Draw your own crosshair (normal + enemy) on an 11x11 grid.
 * Left-click / drag draws pixels, right-click / drag erases them.
 */
public class CrosshairEditorScreen extends Screen {
	private static final String[] TARGETS = {"Players", "Living", "Any"};
	private static final int CELL = 14;

	private final Screen parent;
	private final Ui ui = new Ui();
	private int editTab = 0; // 0 = normal, 1 = enemy
	private int paintMode = 0; // 0 = idle, 1 = drawing, 2 = erasing
	private int gx, gy;

	public CrosshairEditorScreen(Screen parent) {
		super(Text.literal("Crosshair Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ui.reset();
		paintMode = 0;
	}

	private String art() {
		RayConfig c = RayConfig.get();
		return editTab == 0 ? c.crosshairPixels : c.enemyPixels;
	}

	private void setArt(String a) {
		RayConfig c = RayConfig.get();
		if (editTab == 0) c.crosshairPixels = a;
		else c.enemyPixels = a;
	}

	private int colorIdx() {
		RayConfig c = RayConfig.get();
		return editTab == 0 ? c.crosshairColor : c.enemyColor;
	}

	private void setColor(int i) {
		RayConfig c = RayConfig.get();
		if (editTab == 0) c.crosshairColor = i;
		else c.enemyColor = i;
	}

	private int cellAt(int x, int y) {
		if (x < gx || y < gy) return -1;
		int c = (x - gx) / CELL, r = (y - gy) / CELL;
		if (c < 0 || c >= CrosshairArt.N || r < 0 || r >= CrosshairArt.N) return -1;
		return r * CrosshairArt.N + c;
	}

	/** Compact on/off row. */
	private void smallToggle(int x, int y, int w, String label, boolean on, Runnable flip) {
		boolean hov = ui.over(x, y, w, 17);
		ui.panel(x, y, w, 17, 3, hov ? Theme.CARD_HOVER : Theme.CARD, Theme.STROKE);
		ui.text(ui.fit(label, w - 40), x + 7, y + 5, Theme.TEXT);
		int tx = x + w - 28, ty = y + 4;
		ui.rrect(tx, ty, 21, 9, 3, on ? Theme.accent() : 0xFF2B3248);
		ui.rrect(on ? tx + 12 : tx + 1, ty + 1, 8, 7, 2, 0xFFFFFFFF);
		ui.click("tg" + label, x, y, w, 17, flip);
	}

	private void smallButton(int x, int y, int w, String label, Runnable action) {
		boolean hov = ui.over(x, y, w, 16);
		ui.panel(x, y, w, 16, 3, hov ? Theme.CARD_HOVER : Theme.CARD, Theme.STROKE);
		ui.center(label, x + w / 2, y + 4, Theme.TEXT);
		ui.click("sb" + label, x, y, w, 16, action);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		if (client.world == null) Brand.backdrop(ctx, width, height, System.currentTimeMillis(), 1.0f);
		ui.begin(ctx, textRenderer, mouseX, mouseY);
		RayConfig cfg = RayConfig.get();
		int accent = Theme.accent();

		int pw = Math.min(372, width - 12), ph = 268;
		int px = (width - pw) / 2, py = Math.max(4, (height - ph) / 2);
		ui.panel(px, py, pw, ph, 5, Theme.PANEL, Theme.fade(accent, 0.55f));
		ui.rect(px + 1, py + 1, pw - 2, 2, accent);

		// header
		boolean backHov = ui.over(px + 8, py + 9, 46, 17);
		ui.rrect(px + 8, py + 9, 46, 17, 3, backHov ? Theme.CARD_HOVER : Theme.CARD);
		ui.backIcon(px + 14, py + 14, Theme.DIM);
		ui.text("Back", px + 25, py + 14, Theme.TEXT);
		ui.click("back", px + 8, py + 9, 46, 17, this::close);
		ui.textShadow("Crosshair Editor", px + 62, py + 14, 0xFFFFFFFF);
		ui.rect(px + 8, py + 31, pw - 16, 1, Theme.STROKE);

		// tabs
		int tx = px + 14;
		tx += ui.chip(tx, py + 40, "NORMAL", editTab == 0, () -> editTab = 0) + 4;
		ui.chip(tx, py + 40, "ENEMY", editTab == 1, () -> editTab = 1);

		// ---- grid ----
		gx = px + 14;
		gy = py + 62;
		String art = art();
		int col = CrosshairArt.color(colorIdx());
		int gridPx = CrosshairArt.N * CELL;
		ui.rrect(gx - 3, gy - 3, gridPx + 6, gridPx + 6, 4, 0xFF090B12);
		int hoverCell = cellAt(mouseX, mouseY);
		for (int r = 0; r < CrosshairArt.N; r++) {
			for (int c = 0; c < CrosshairArt.N; c++) {
				int x = gx + c * CELL, y = gy + r * CELL;
				boolean lit = CrosshairArt.get(art, r, c);
				int base = ((r + c) & 1) == 0 ? 0xFF161B2A : 0xFF131827;
				ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, lit ? col : base);
				if (hoverCell == r * CrosshairArt.N + c) ui.border(x, y, CELL, CELL, 0x99FFFFFF);
			}
		}
		ui.border(gx + 5 * CELL, gy + 5 * CELL, CELL, CELL, 0x44FFFFFF); // centre marker
		ui.text("Left-click: draw", gx, gy + gridPx + 8, Theme.DIM);
		ui.text("Right-click: erase", gx, gy + gridPx + 19, Theme.DIM);

		// ---- right column ----
		int rx = px + 190, rw = pw - 190 - 14;
		ui.text("COLOR", rx, py + 41, Theme.MUTE);
		for (int i = 0; i < CrosshairArt.colorCount(); i++) {
			final int idx = i;
			int sx = rx + i * 20, sy = py + 53;
			ui.rrect(sx, sy, 16, 16, 3, CrosshairArt.color(i));
			if (colorIdx() == i) ui.border(sx - 2, sy - 2, 20, 20, 0xFFFFFFFF);
			ui.click("col" + i, sx, sy, 16, 16, () -> setColor(idx));
		}

		// preview (dark | light background) - the blend is real here, so you can see what it does
		int pvy = py + 78, pvh = 62;
		ctx.fill(rx, pvy, rx + rw / 2, pvy + pvh, 0xFF1B2130);
		ctx.fill(rx + rw / 2, pvy, rx + rw, pvy + pvh, 0xFFC4CCDB);
		ui.border(rx - 1, pvy - 1, rw + 2, pvh + 2, Theme.STROKE);
		CrosshairArt.draw(ctx, art, rx + rw / 2, pvy + pvh / 2, cfg.crosshairScale, col, cfg.crosshairOutline, cfg.crosshairVanillaBlend);

		// presets
		int bw = (rw - 8) / 3;
		int by = pvy + pvh + 8;
		smallButton(rx, by, bw, "Clear", () -> setArt(CrosshairArt.blank()));
		smallButton(rx + bw + 4, by, bw, "Cross", () -> setArt(CrosshairArt.cross()));
		smallButton(rx + (bw + 4) * 2, by, bw, "Dot", () -> setArt(CrosshairArt.dot()));
		smallButton(rx, by + 19, bw, "Ring", () -> setArt(CrosshairArt.ring()));
		smallButton(rx + bw + 4, by + 19, bw, "X", () -> setArt(CrosshairArt.x()));
		smallButton(rx + (bw + 4) * 2, by + 19, bw, "Size " + cfg.crosshairScale, () -> cfg.crosshairScale = cfg.crosshairScale % 4 + 1);

		// toggles
		int ty = by + 42;
		smallToggle(rx, ty, rw, "Vanilla Blending", cfg.crosshairVanillaBlend, () -> cfg.crosshairVanillaBlend = !cfg.crosshairVanillaBlend);
		smallToggle(rx, ty + 20, rw, "Outline", cfg.crosshairOutline, () -> cfg.crosshairOutline = !cfg.crosshairOutline);
		if (editTab == 0) {
			smallToggle(rx, ty + 40, rw, "Use Drawn Crosshair", cfg.crosshairDrawn, () -> cfg.crosshairDrawn = !cfg.crosshairDrawn);
		} else {
			smallToggle(rx, ty + 40, rw, "Enemy Crosshair", cfg.enemyCrosshair, () -> cfg.enemyCrosshair = !cfg.enemyCrosshair);
			smallButton(rx, ty + 60, rw, "Target: " + TARGETS[cfg.enemyTarget], () -> cfg.enemyTarget = (cfg.enemyTarget + 1) % TARGETS.length);
		}

		ui.poll();

		// ---- painting (after the widgets had their chance to take the click) ----
		if (paintMode == 0 && (ui.leftPressed || ui.rightPressed) && cellAt(mouseX, mouseY) >= 0) {
			paintMode = ui.leftPressed ? 1 : 2;
		}
		if (paintMode != 0) {
			boolean held = paintMode == 1 ? ui.left : ui.right;
			if (!held) {
				paintMode = 0;
				RayConfig.save();
			} else {
				int cell = cellAt(mouseX, mouseY);
				if (cell >= 0) setArt(CrosshairArt.set(art(), cell / CrosshairArt.N, cell % CrosshairArt.N, paintMode == 1));
			}
		}
	}

	@Override
	public void close() {
		RayConfig.save();
		client.setScreen(parent);
	}
}
