package dev.lumina;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Lunar-style HUD editor: drag modules, right-click one for its settings, "Mods" opens the module list. */
public class HudEditorScreen extends Screen {
	private HudModule dragging = null;
	private int offX, offY;
	private boolean mouseWasDown = true, rightWasDown = true;

	public HudEditorScreen() {
		super(Text.literal("HUD Editor"));
	}

	@Override
	protected void init() {
		mouseWasDown = true;
		rightWasDown = true;
		dragging = null;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static void border(DrawContext c, int x, int y, int w, int h, int col) {
		c.fill(x, y, x + w, y + 1, col);
		c.fill(x, y + h - 1, x + w, y + h, col);
		c.fill(x, y, x + 1, y + h, col);
		c.fill(x + w - 1, y, x + w, y + h, col);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		LuminaConfig cfg = LuminaConfig.get();
		int accent = Theme.accent();
		ctx.fill(0, 0, width, height, 0x66000000);

		List<HudModule> active = new ArrayList<>();
		for (HudModule m : Modules.ALL) {
			if (!m.enabled()) continue;
			if (!m.prepare(client, textRenderer, true)) continue;
			if (m == dragging) {
				m.x = Math.max(0, Math.min(width - m.ew(), m.x));
				m.y = Math.max(0, Math.min(height - m.eh(), m.y));
			} else {
				m.place(width, height);
			}
			active.add(m);
		}
		for (HudModule m : active) {
			m.render(ctx, client, textRenderer);
			boolean hot = m == dragging || (dragging == null && inside(mouseX, mouseY, m.x, m.y, m.ew(), m.eh()));
			border(ctx, m.x - 1, m.y - 1, m.ew() + 2, m.eh() + 2, hot ? 0xFFFFFFFF : Theme.withAlpha(accent, 0xAA));
		}

		String title = "HUD Editor";
		String hint = "Drag to move  -  Right-click a module for its settings  -  ESC to close";
		ctx.drawText(textRenderer, title, (width - textRenderer.getWidth(title)) / 2, 10, 0xFFFFFFFF, true);
		ctx.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 22, 0xFFB0BCD6, false);

		int bx = width / 2 - 60, by = height / 2 - 12;
		boolean hovMods = inside(mouseX, mouseY, bx, by, 120, 24);
		ctx.fill(bx, by, bx + 120, by + 24, Theme.withAlpha(accent, hovMods ? 0xEE : 0xBB));
		border(ctx, bx, by, 120, 24, 0xFFFFFFFF);
		String mods = "Mods";
		ctx.drawText(textRenderer, mods, bx + (120 - textRenderer.getWidth(mods)) / 2, by + 8, 0xFFFFFFFF, true);

		int ry = by + 30;
		boolean hovReset = inside(mouseX, mouseY, bx, ry, 120, 18);
		ctx.fill(bx, ry, bx + 120, ry + 18, hovReset ? 0xFF2A3347 : 0xFF131C2E);
		String reset = "Reset Positions";
		ctx.drawText(textRenderer, reset, bx + (120 - textRenderer.getWidth(reset)) / 2, ry + 5, 0xFFE5E9F2, false);

		// ---- mouse input (polled) ----
		long win = client.getWindow().getHandle();
		boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean rdown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		if (rdown && !rightWasDown && dragging == null) {
			for (int i = active.size() - 1; i >= 0; i--) {
				HudModule m = active.get(i);
				if (inside(mouseX, mouseY, m.x, m.y, m.ew(), m.eh())) {
					rightWasDown = rdown;
					mouseWasDown = down;
					client.setScreen(new ModuleSettingsScreen(this, m));
					return;
				}
			}
		}
		rightWasDown = rdown;

		if (down && !mouseWasDown) {
			if (hovMods) {
				mouseWasDown = down;
				client.setScreen(new LuminaScreen(this));
				return;
			} else if (hovReset) {
				cfg.positions.clear();
				LuminaConfig.save();
			} else {
				for (int i = active.size() - 1; i >= 0; i--) {
					HudModule m = active.get(i);
					if (inside(mouseX, mouseY, m.x, m.y, m.ew(), m.eh())) {
						dragging = m;
						offX = mouseX - m.x;
						offY = mouseY - m.y;
						break;
					}
				}
			}
		}
		if (down && dragging != null) {
			int nx = mouseX - offX;
			int ny = mouseY - offY;
			int ew = dragging.ew(), eh = dragging.eh();
			if (Math.abs(nx - 4) < 6) nx = 4;
			if (Math.abs(nx + ew - (width - 4)) < 6) nx = width - 4 - ew;
			if (Math.abs(ny - 4) < 6) ny = 4;
			if (Math.abs(ny + eh - (height - 4)) < 6) ny = height - 4 - eh;
			if (Math.abs(nx + ew / 2 - width / 2) < 5) nx = width / 2 - ew / 2;
			dragging.moveTo(nx, ny, width, height);
		}
		if (!down && dragging != null) {
			dragging = null;
			LuminaConfig.save();
		}
		mouseWasDown = down;
	}
}
