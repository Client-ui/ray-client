package dev.lumina;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Per-module settings: scale, colours, background, shadow, border and module-specific options. */
public class ModuleSettingsScreen extends Screen {
	private static final String[] SHADOW = {"Default", "On", "Off"};
	private static final int ROW = 28;

	private record Hit(String id, int x, int y, int w, int h, Runnable click, IntConsumer drag) {
		boolean contains(int mx, int my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final Screen parent;
	private final HudModule m;
	private final List<Hit> hits = new ArrayList<>();
	private boolean mouseWasDown = true;
	private String dragId = null;
	private int mx, my, px, py, pw, ph, cx, cy, cw, slot;
	private boolean listening = false;
	private long listenStart = 0;

	public ModuleSettingsScreen(Screen parent, HudModule m) {
		super(Text.literal(m.name + " Settings"));
		this.parent = parent;
		this.m = m;
	}

	@Override
	protected void init() {
		mouseWasDown = true;
		dragId = null;
	}

	private int[] half() {
		int colW = (cw - 6) / 2;
		int x = cx + (slot % 2) * (colW + 6);
		int y = cy + (slot / 2) * ROW;
		slot++;
		return new int[]{x, y, colW};
	}

	private void card(DrawContext ctx, int[] r) {
		boolean hov = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + 24;
		ctx.fill(r[0], r[1], r[0] + r[2], r[1] + 24, hov ? 0xFF1A2540 : 0xFF131C2E);
	}

	private void toggle(DrawContext ctx, int[] r, String name, boolean on, Runnable flip) {
		card(ctx, r);
		ctx.drawText(textRenderer, name, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
		int tx = r[0] + r[2] - 32, ty = r[1] + 7;
		ctx.fill(tx, ty, tx + 24, ty + 10, on ? Theme.accent() : 0xFF2A3347);
		int kx = on ? tx + 14 : tx + 2;
		ctx.fill(kx, ty + 1, kx + 8, ty + 9, 0xFFFFFFFF);
		hits.add(new Hit("t" + name, r[0], r[1], r[2], 24, () -> { flip.run(); LuminaConfig.save(); }, null));
	}

	private void choice(DrawContext ctx, int[] r, String name, String value, Runnable next) {
		card(ctx, r);
		ctx.drawText(textRenderer, name, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
		ctx.drawText(textRenderer, value, r[0] + r[2] - 8 - textRenderer.getWidth(value), r[1] + 8, Theme.accent(), false);
		hits.add(new Hit("c" + name, r[0], r[1], r[2], 24, () -> { next.run(); LuminaConfig.save(); }, null));
	}

	private void colorChoice(DrawContext ctx, int[] r, String name, int idx, int def, Runnable next) {
		card(ctx, r);
		ctx.drawText(textRenderer, name, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
		ctx.fill(r[0] + r[2] - 22, r[1] + 5, r[0] + r[2] - 8, r[1] + 19, Theme.pick(idx, def));
		String v = Theme.PALETTE_NAMES[idx];
		ctx.drawText(textRenderer, v, r[0] + r[2] - 28 - textRenderer.getWidth(v), r[1] + 8, 0xFF9FB0D0, false);
		hits.add(new Hit("cc" + name, r[0], r[1], r[2], 24, () -> { next.run(); LuminaConfig.save(); }, null));
	}

	private void slider(DrawContext ctx, int[] r, String name, int min, int max, int value, String suffix, IntConsumer set) {
		sliderShown(ctx, r, name, min, max, value, value + suffix, set);
	}

	private void sliderShown(DrawContext ctx, int[] r, String name, int min, int max, int value, String shown, IntConsumer set) {
		card(ctx, r);
		ctx.drawText(textRenderer, name, r[0] + 8, r[1] + 4, 0xFFE5E9F2, false);
		String v = shown;
		ctx.drawText(textRenderer, v, r[0] + r[2] - 8 - textRenderer.getWidth(v), r[1] + 4, 0xFF9FB0D0, false);
		final int bx = r[0] + 8, bw = r[2] - 16, by = r[1] + 16;
		ctx.fill(bx, by, bx + bw, by + 4, 0xFF2A3347);
		int fill = (int) ((long) (value - min) * bw / Math.max(1, max - min));
		ctx.fill(bx, by, bx + fill, by + 4, Theme.accent());
		ctx.fill(bx + fill - 2, by - 2, bx + fill + 2, by + 6, 0xFFFFFFFF);
		hits.add(new Hit("s" + name, r[0], r[1] + 10, r[2], 14, () -> {}, mxv -> {
			int nv = min + Math.round((mxv - bx) * (max - min) / (float) bw);
			set.accept(Math.max(min, Math.min(max, nv)));
		}));
	}

	private static void border(DrawContext c, int x, int y, int w, int h, int col) {
		c.fill(x, y, x + w, y + 1, col);
		c.fill(x, y + h - 1, x + w, y + h, col);
		c.fill(x, y, x + 1, y + h, col);
		c.fill(x + w - 1, y, x + w, y + h, col);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		LuminaConfig cfg = LuminaConfig.get();
		LuminaConfig.Style st = m.style();
		int accent = Theme.accent();
		mx = mouseX;
		my = mouseY;
		hits.clear();
		slot = 0;

		pw = Math.min(440, width - 16);
		ph = Math.min(270, height - 16);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		cx = px + 14;
		cy = py + 34;
		cw = pw - 28;

		ctx.fill(px, py, px + pw, py + ph, 0xF00A101C);
		border(ctx, px, py, pw, ph, Theme.withAlpha(accent, 0x99));
		ctx.drawText(textRenderer, m.name + " Settings", px + 14, py + 12, 0xFFFFFFFF, true);
		ctx.fill(px + 14, py + 24, px + 38, py + 26, accent);

		// reset button
		int rx = px + pw - 92, ry = py + 8;
		boolean hovR = mx >= rx && mx < rx + 80 && my >= ry && my < ry + 16;
		ctx.fill(rx, ry, rx + 80, ry + 16, hovR ? 0xFF7F1D1D : 0xFF3B1212);
		ctx.drawText(textRenderer, "Reset Style", rx + (80 - textRenderer.getWidth("Reset Style")) / 2, ry + 4, 0xFFFFFFFF, false);
		hits.add(new Hit("reset", rx, ry, 80, 16, () -> { cfg.styles.remove(m.id); LuminaConfig.save(); }, null));

		toggle(ctx, half(), "Enabled", m.enabled(), () -> m.setEnabled(!m.enabled()));
		slider(ctx, half(), "Scale", 50, 200, st.scale, "%", v -> st.scale = v);
		colorChoice(ctx, half(), "Text Color", st.textColor, 0xFFFFFFFF, () -> st.textColor = (st.textColor + 1) % Theme.PALETTE_NAMES.length);
		colorChoice(ctx, half(), "Accent Color", st.accentColor, Theme.accent(), () -> st.accentColor = (st.accentColor + 1) % Theme.PALETTE_NAMES.length);
		sliderShown(ctx, half(), "Background", -1, 100, st.bg, st.bg < 0 ? "Global" : st.bg + "%", v -> st.bg = v);
		toggle(ctx, half(), "Accent Bar", st.accentBar, () -> st.accentBar = !st.accentBar);
		int shIdx = st.shadow < 0 ? 0 : (st.shadow == 1 ? 1 : 2);
		choice(ctx, half(), "Text Shadow", SHADOW[shIdx], () -> {
			int n = (shIdx + 1) % 3;
			st.shadow = n == 0 ? -1 : (n == 1 ? 1 : 0);
		});
		toggle(ctx, half(), "Border", st.border, () -> st.border = !st.border);
		String keyText = listening ? "Press a key..." : Keys.name(st.key);
		choice(ctx, half(), "Toggle Key", keyText, () -> {
			listening = true;
			listenStart = System.currentTimeMillis();
		});

		for (Modules.Opt o : Modules.optionsFor(m.id)) {
			int v = Modules.opt(m.id, o.key);
			if (o.type == Modules.Opt.TOGGLE) {
				toggle(ctx, half(), o.label, v != 0, () -> st.opts.put(o.key, v == 0 ? 1 : 0));
			} else if (o.type == Modules.Opt.SLIDER) {
				slider(ctx, half(), o.label, o.min, o.max, v, "", val -> st.opts.put(o.key, val));
			} else {
				choice(ctx, half(), o.label, o.choices[Math.max(0, Math.min(o.choices.length - 1, v))],
					() -> st.opts.put(o.key, (v + 1) % o.choices.length));
			}
		}

		ctx.drawText(textRenderer, "ESC to go back", px + pw - 8 - textRenderer.getWidth("ESC to go back"), py + ph - 12, 0xFF6B7A99, false);

		if (listening && System.currentTimeMillis() - listenStart > 250L) {
			long kw = client.getWindow().getHandle();
			for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
				if (GLFW.glfwGetKey(kw, k) == GLFW.GLFW_PRESS) {
					listening = false;
					if (k != GLFW.GLFW_KEY_ESCAPE) {
						st.key = (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) ? -1 : k;
						LuminaConfig.save();
					}
					break;
				}
			}
		}

		// ---- mouse input (polled) ----
		long win = client.getWindow().getHandle();
		boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		if (down && !mouseWasDown) {
			for (Hit h : hits) {
				if (h.contains(mouseX, mouseY)) {
					h.click().run();
					if (h.drag() != null) dragId = h.id();
					break;
				}
			}
		}
		if (down && dragId != null) {
			for (Hit h : hits) {
				if (h.id().equals(dragId) && h.drag() != null) {
					h.drag().accept(mouseX);
					break;
				}
			}
		}
		if (!down && dragId != null) {
			dragId = null;
			LuminaConfig.save();
		}
		mouseWasDown = down;
	}

	@Override
	public void close() {
		LuminaConfig.save();
		client.setScreen(parent);
	}
}
