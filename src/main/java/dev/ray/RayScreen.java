package dev.ray;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Module list / settings menu. Mouse input is polled via GLFW so it does not depend on Screen click signatures. */
public class RayScreen extends Screen {
	private static final String[] TABS = {"Stats", "World", "Player", "PvP", "Extras", "Gameplay", "Visuals", "Crosshair", "Waypoints", "Theme", "Profiles", "Outline"};
	private static final int SIDEBAR = 96;
	private static final int ROW = 28;
	private static final String[] TARGETS = {"Players", "Living", "Any"};
	private static int tab = 0;

	public static void selectTab(String name) {
		for (int i = 0; i < TABS.length; i++) if (TABS[i].equals(name)) tab = i;
	}

	private record Hit(String id, int x, int y, int w, int h, Runnable click, IntConsumer drag) {
		boolean contains(int mx, int my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final Screen parent;
	private final List<Hit> hits = new ArrayList<>();
	private boolean mouseWasDown = true;
	private String dragId = null;
	private int mx, my, px, py, pw, ph, cx, cy, cw, slot;
	private List<String> profileNames = new ArrayList<>();
	private long profilesTime = 0;
	private boolean profilesDirty = true;

	public RayScreen(Screen parent) {
		super(Text.literal("Ray Client"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		mouseWasDown = true;
		dragId = null;
	}

	// ---------- layout helpers ----------

	private int[] half() {
		int colW = (cw - 6) / 2;
		int x = cx + (slot % 2) * (colW + 6);
		int y = cy + (slot / 2) * ROW;
		slot++;
		return new int[]{x, y, colW};
	}

	private int[] full() {
		if (slot % 2 == 1) slot++;
		int y = cy + (slot / 2) * ROW;
		slot += 2;
		return new int[]{cx, y, cw};
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
		hits.add(new Hit("t" + name, r[0], r[1], r[2], 24, () -> { flip.run(); RayConfig.save(); }, null));
	}

	private void choice(DrawContext ctx, int[] r, String name, String value, Runnable next) {
		card(ctx, r);
		ctx.drawText(textRenderer, name, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
		ctx.drawText(textRenderer, value, r[0] + r[2] - 8 - textRenderer.getWidth(value), r[1] + 8, Theme.accent(), false);
		hits.add(new Hit("c" + name, r[0], r[1], r[2], 24, () -> { next.run(); RayConfig.save(); }, null));
	}

	/** Module card: name, a settings button and an on/off switch. */
	private void moduleToggle(DrawContext ctx, int[] r, HudModule mod) {
		card(ctx, r);
		boolean on = mod.enabled();
		ctx.drawText(textRenderer, mod.name, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
		int tx = r[0] + r[2] - 32, ty = r[1] + 7;
		ctx.fill(tx, ty, tx + 24, ty + 10, on ? Theme.accent() : 0xFF2A3347);
		int kx = on ? tx + 14 : tx + 2;
		ctx.fill(kx, ty + 1, kx + 8, ty + 9, 0xFFFFFFFF);
		int gx = r[0] + r[2] - 54, gy = r[1] + 4;
		boolean hov = mx >= gx && mx < gx + 16 && my >= gy && my < gy + 16;
		ctx.fill(gx, gy, gx + 16, gy + 16, hov ? 0xFF2A3A5C : 0xFF1A2540);
		ctx.drawText(textRenderer, "...", gx + 3, gy + 4, 0xFFB0BCD6, false);
		hits.add(new Hit("g" + mod.id, gx, gy, 16, 16, () -> client.setScreen(new ModuleSettingsScreen(this, mod)), null));
		hits.add(new Hit("t" + mod.id, r[0], r[1], r[2], 24, () -> { mod.setEnabled(!mod.enabled()); RayConfig.save(); }, null));
	}

	private void smallButton(DrawContext ctx, int x, int y, int w, int h, String label, int color, String id, Runnable action) {
		boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
		ctx.fill(x, y, x + w, y + h, Theme.withAlpha(color, hov ? 0xFF : 0xCC));
		ctx.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 4, 0xFFFFFFFF, false);
		hits.add(new Hit(id, x, y, w, h, action, null));
	}

	private void slider(DrawContext ctx, int[] r, String name, int min, int max, int value, String suffix, IntConsumer set) {
		card(ctx, r);
		ctx.drawText(textRenderer, name, r[0] + 8, r[1] + 4, 0xFFE5E9F2, false);
		String v = value + suffix;
		ctx.drawText(textRenderer, v, r[0] + r[2] - 8 - textRenderer.getWidth(v), r[1] + 4, 0xFF9FB0D0, false);
		final int bx = r[0] + 8, bw = r[2] - 16, by = r[1] + 16;
		ctx.fill(bx, by, bx + bw, by + 4, 0xFF2A3347);
		int fill = (int) ((long) (value - min) * bw / (max - min));
		ctx.fill(bx, by, bx + fill, by + 4, Theme.accent());
		ctx.fill(bx + fill - 2, by - 2, bx + fill + 2, by + 6, 0xFFFFFFFF);
		hits.add(new Hit("s" + name, r[0], r[1] + 10, r[2], 14, () -> {}, m -> {
			int nv = min + Math.round((m - bx) * (max - min) / (float) bw);
			set.accept(Math.max(min, Math.min(max, nv)));
		}));
	}

	private void button(DrawContext ctx, int[] r, String label, Runnable action) {
		boolean hov = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + 24;
		ctx.fill(r[0], r[1], r[0] + r[2], r[1] + 24, Theme.withAlpha(Theme.accent(), hov ? 0xAA : 0x77));
		ctx.drawText(textRenderer, label, r[0] + (r[2] - textRenderer.getWidth(label)) / 2, r[1] + 8, 0xFFFFFFFF, false);
		hits.add(new Hit("b" + label, r[0], r[1], r[2], 24, action, null));
	}

	private static void border(DrawContext c, int x, int y, int w, int h, int col) {
		c.fill(x, y, x + w, y + 1, col);
		c.fill(x, y + h - 1, x + w, y + h, col);
		c.fill(x, y, x + 1, y + h, col);
		c.fill(x + w - 1, y, x + w, y + h, col);
	}

	// ---------- render ----------

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		RayConfig cfg = RayConfig.get();
		mx = mouseX;
		my = mouseY;
		hits.clear();
		slot = 0;
		int accent = Theme.accent();

		pw = Math.min(440, width - 16);
		ph = Math.min(270, height - 16);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		cx = px + SIDEBAR + 10;
		cy = py + 34;
		cw = pw - SIDEBAR - 20;

		ctx.fill(px, py, px + pw, py + ph, 0xF00A101C);
		border(ctx, px, py, pw, ph, Theme.withAlpha(accent, 0x99));
		ctx.fill(px + 1, py + 1, px + SIDEBAR, py + ph - 1, 0xFF0D1524);

		// logo
		ctx.fill(px + 10, py + 10, px + 24, py + 24, accent);
		ctx.drawText(textRenderer, "R", px + 15, py + 14, 0xFFFFFFFF, false);
		ctx.drawText(textRenderer, "RAY", px + 30, py + 11, 0xFFFFFFFF, true);
		ctx.drawText(textRenderer, "CLIENT", px + 30, py + 20, accent, false);

		// sidebar tabs
		for (int i = 0; i < TABS.length; i++) {
			final int idx = i;
			int ty = py + 36 + i * 15;
			boolean sel = tab == i;
			boolean hov = mx >= px + 6 && mx < px + SIDEBAR - 6 && my >= ty && my < ty + 13;
			if (sel) {
				ctx.fill(px + 6, ty, px + SIDEBAR - 6, ty + 13, Theme.withAlpha(accent, 0x33));
				ctx.fill(px + 6, ty, px + 8, ty + 13, accent);
			} else if (hov) {
				ctx.fill(px + 6, ty, px + SIDEBAR - 6, ty + 13, 0xFF131C2E);
			}
			ctx.drawText(textRenderer, TABS[i], px + 16, ty + 3, sel ? 0xFFFFFFFF : 0xFF9FB0D0, false);
			hits.add(new Hit("tab" + i, px + 6, ty, SIDEBAR - 12, 13, () -> tab = idx, null));
		}
		if (ph >= 236) {
			ctx.drawText(textRenderer, "v1.0.0", px + 10, py + ph - 22, 0xFF6B7A99, false);
			ctx.drawText(textRenderer, "MC 1.21.11", px + 10, py + ph - 12, 0xFF6B7A99, false);
		}

		// heading
		ctx.drawText(textRenderer, TABS[tab], cx, py + 12, 0xFFFFFFFF, true);
		ctx.fill(cx, py + 24, cx + 24, py + 26, accent);

		switch (tab) {
			case 0, 1, 2, 3 -> {
				for (HudModule m : Modules.ALL) {
					if (!m.category.equals(TABS[tab])) continue;
					moduleToggle(ctx, half(), m);
				}
				if (tab == 3) {
					toggle(ctx, half(), "Hit Marker", cfg.hitMarker, () -> cfg.hitMarker = !cfg.hitMarker);
					toggle(ctx, half(), "Hit Sound", cfg.hitSound, () -> cfg.hitSound = !cfg.hitSound);
				}
			}
			case 4 -> {
				for (HudModule m : Modules.ALL) {
					if (!m.category.equals("Extras")) continue;
					moduleToggle(ctx, half(), m);
				}
			}
			case 5 -> {
				toggle(ctx, half(), "Client HUD", cfg.enabled, () -> cfg.enabled = !cfg.enabled);
				toggle(ctx, half(), "Toggle Sprint", cfg.toggleSprint, () -> cfg.toggleSprint = !cfg.toggleSprint);
				toggle(ctx, half(), "Toggle Sneak", cfg.toggleSneak, () -> cfg.toggleSneak = !cfg.toggleSneak);
				slider(ctx, half(), "Zoom FOV (hold C)", 10, 60, cfg.zoomFov, "", v -> cfg.zoomFov = v);
				toggle(ctx, half(), "Smooth Zoom", cfg.smoothZoom, () -> cfg.smoothZoom = !cfg.smoothZoom);
				slider(ctx, half(), "FOV Effects", 0, 100, (int) Math.round(client.options.getFovEffectScale().getValue() * 100.0), "%",
					v -> client.options.getFovEffectScale().setValue(v / 100.0));
				toggle(ctx, half(), "Low Health Warning", cfg.lowHealthWarning, () -> cfg.lowHealthWarning = !cfg.lowHealthWarning);
				toggle(ctx, half(), "Durability Alert", cfg.durabilityAlert, () -> cfg.durabilityAlert = !cfg.durabilityAlert);
				toggle(ctx, half(), "Auto Respawn", cfg.autoRespawn, () -> cfg.autoRespawn = !cfg.autoRespawn);
			}
			case 6 -> {
				toggle(ctx, half(), "Fullbright", cfg.fullbright, () -> cfg.fullbright = !cfg.fullbright);
				toggle(ctx, half(), "No Hurt Cam", cfg.noHurtCam, () -> cfg.noHurtCam = !cfg.noHurtCam);
				toggle(ctx, half(), "Viewmodel", cfg.viewmodel, () -> cfg.viewmodel = !cfg.viewmodel);
				slider(ctx, half(), "Viewmodel Scale", 20, 150, cfg.vmScale, "%", v -> cfg.vmScale = v);
				slider(ctx, half(), "Viewmodel X", -50, 50, cfg.vmX, "", v -> cfg.vmX = v);
				slider(ctx, half(), "Viewmodel Y", -50, 50, cfg.vmY, "", v -> cfg.vmY = v);
				slider(ctx, half(), "Viewmodel Z", -50, 50, cfg.vmZ, "", v -> cfg.vmZ = v);
				toggle(ctx, half(), "Custom Swing", cfg.customSwing, () -> cfg.customSwing = !cfg.customSwing);
				slider(ctx, half(), "Swing Duration", 1, 30, cfg.swingDuration, " ticks", v -> cfg.swingDuration = v);
				toggle(ctx, half(), "Hide Scoreboard", cfg.hideScoreboard, () -> cfg.hideScoreboard = !cfg.hideScoreboard);
				slider(ctx, half(), "Scoreboard Scale", 50, 150, cfg.scoreboardScale, "%", v -> cfg.scoreboardScale = v);
				toggle(ctx, half(), "Scoreboard Position", Modules.scoreboard.enabled(), () -> Modules.scoreboard.setEnabled(!Modules.scoreboard.enabled()));
			}
			case 7 -> {
				toggle(ctx, half(), "Custom Crosshair", cfg.customCrosshair, () -> cfg.customCrosshair = !cfg.customCrosshair);
				toggle(ctx, half(), "Use Drawn Crosshair", cfg.crosshairDrawn, () -> cfg.crosshairDrawn = !cfg.crosshairDrawn);
				toggle(ctx, full(), "Custom Enemy Crosshair", cfg.enemyCrosshair, () -> cfg.enemyCrosshair = !cfg.enemyCrosshair);
				toggle(ctx, half(), "Crosshair Outline", cfg.crosshairOutline, () -> cfg.crosshairOutline = !cfg.crosshairOutline);
				choice(ctx, half(), "Enemy Target", TARGETS[cfg.enemyTarget], () -> cfg.enemyTarget = (cfg.enemyTarget + 1) % TARGETS.length);
				slider(ctx, half(), "Size", 2, 20, cfg.crosshairSize, "", v -> cfg.crosshairSize = v);
				slider(ctx, half(), "Gap", 0, 10, cfg.crosshairGap, "", v -> cfg.crosshairGap = v);
				slider(ctx, half(), "Thickness", 1, 3, cfg.crosshairThickness, "", v -> cfg.crosshairThickness = v);
				slider(ctx, half(), "Pixel Scale", 1, 4, cfg.crosshairScale, "x", v -> cfg.crosshairScale = v);
				button(ctx, full(), "Open Crosshair Editor", () -> client.setScreen(new CrosshairEditorScreen(this)));
			}
			case 8 -> {
				button(ctx, full(), "+ Add waypoint at my position", () -> RayClient.addWaypoint(client));
				int maxRows = Math.max(1, (py + ph - 8 - (cy + ROW)) / ROW);
				if (cfg.waypoints.isEmpty()) {
					ctx.drawText(textRenderer, "No waypoints yet. Press B in-game to add one.", cx, cy + ROW + 8, 0xFF6B7A99, false);
				}
				for (int i = 0; i < Math.min(maxRows, cfg.waypoints.size()); i++) {
					final int idx = i;
					RayConfig.Waypoint w = cfg.waypoints.get(i);
					int[] r = full();
					card(ctx, r);
					String label = w.name + "  " + (int) w.x + " " + (int) w.y + " " + (int) w.z;
					ctx.drawText(textRenderer, label, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
					int bx = r[0] + r[2] - 22;
					ctx.fill(bx, r[1] + 4, bx + 16, r[1] + 20, 0xFF7F1D1D);
					ctx.drawText(textRenderer, "X", bx + 5, r[1] + 8, 0xFFFFFFFF, false);
					hits.add(new Hit("del" + i, bx, r[1] + 4, 16, 16, () -> {
						if (idx < cfg.waypoints.size()) cfg.waypoints.remove(idx);
						RayConfig.save();
					}, null));
				}
			}
			case 9 -> {
				ctx.drawText(textRenderer, "Accent color", cx, cy, 0xFF9FB0D0, false);
				for (int i = 0; i < Theme.COLORS.length; i++) {
					final int idx = i;
					int sx = cx + i * 32, sy = cy + 12;
					ctx.fill(sx, sy, sx + 24, sy + 24, Theme.COLORS[i]);
					if (cfg.accent == i && !cfg.rainbow) border(ctx, sx - 2, sy - 2, 28, 28, 0xFFFFFFFF);
					hits.add(new Hit("sw" + i, sx, sy, 24, 24, () -> {
						cfg.accent = idx;
						cfg.rainbow = false;
						RayConfig.save();
					}, null));
				}
				slot = 4;
				toggle(ctx, half(), "Rainbow Accent", cfg.rainbow, () -> cfg.rainbow = !cfg.rainbow);
				slider(ctx, half(), "HUD Background", 0, 100, cfg.hudOpacity, "%", v -> cfg.hudOpacity = v);
				toggle(ctx, half(), "Text Shadow", cfg.textShadow, () -> cfg.textShadow = !cfg.textShadow);
			}
			case 10 -> {
				button(ctx, full(), "+ Save current setup as new profile", () -> {
					Profiles.save(Profiles.nextName());
					profilesDirty = true;
				});
				if (profilesDirty || System.currentTimeMillis() - profilesTime > 1500L) {
					profileNames = Profiles.list();
					profilesTime = System.currentTimeMillis();
					profilesDirty = false;
				}
				int maxRows = Math.max(1, (py + ph - 8 - (cy + ROW)) / ROW);
				if (profileNames.isEmpty()) {
					ctx.drawText(textRenderer, "No profiles yet. Save one to switch layouts instantly.", cx, cy + ROW + 8, 0xFF6B7A99, false);
				}
				for (int i = 0; i < Math.min(maxRows, profileNames.size()); i++) {
					final String pn = profileNames.get(i);
					int[] r = full();
					card(ctx, r);
					ctx.drawText(textRenderer, pn, r[0] + 8, r[1] + 8, 0xFFE5E9F2, false);
					int bx = r[0] + r[2] - 112;
					smallButton(ctx, bx, r[1] + 4, 38, 16, "Load", 0xFF1E3A8A, "pl" + pn, () -> Profiles.load(pn));
					smallButton(ctx, bx + 42, r[1] + 4, 38, 16, "Save", 0xFF14532D, "ps" + pn, () -> Profiles.save(pn));
					smallButton(ctx, bx + 84, r[1] + 4, 28, 16, "Del", 0xFF7F1D1D, "pd" + pn, () -> {
						Profiles.delete(pn);
						profilesDirty = true;
					});
				}
			}
			case 11 -> {
				toggle(ctx, half(), "Block Outline", cfg.blockOutline, () -> cfg.blockOutline = !cfg.blockOutline);
				choice(ctx, half(), "Color", cfg.outlineColor == 0 ? "Accent" : Theme.PALETTE_NAMES[cfg.outlineColor],
					() -> cfg.outlineColor = (cfg.outlineColor + 1) % Theme.PALETTE_NAMES.length);
				toggle(ctx, half(), "Rainbow Outline", cfg.outlineRainbow, () -> cfg.outlineRainbow = !cfg.outlineRainbow);
				toggle(ctx, half(), "Outline Glow", cfg.outlineGlow, () -> cfg.outlineGlow = !cfg.outlineGlow);
				slider(ctx, half(), "Outline Thickness", 1, 4, cfg.outlineThickness, "", v -> cfg.outlineThickness = v);
				slider(ctx, half(), "Outline Opacity", 20, 100, cfg.outlineOpacity, "%", v -> cfg.outlineOpacity = v);
				button(ctx, full(), "Fix alignment: no view bobbing, no FOV effects", () -> {
					client.options.getBobView().setValue(false);
					client.options.getFovEffectScale().setValue(0.0);
				});
				ctx.drawText(textRenderer, "Outlines the block you look at. Drawn on screen, so it lines up best", cx, cy + 4 * ROW + 4, 0xFF6B7A99, false);
				ctx.drawText(textRenderer, "with view bobbing off and FOV effects at 0 (the button above).", cx, cy + 4 * ROW + 16, 0xFF6B7A99, false);
			}
			default -> {}
		}

		ctx.drawText(textRenderer, "ESC to go back", px + pw - 8 - textRenderer.getWidth("ESC to go back"), py + ph - 12, 0xFF6B7A99, false);

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
