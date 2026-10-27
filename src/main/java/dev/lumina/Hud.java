package dev.lumina;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public final class Hud {
	private Hud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		LuminaConfig cfg = LuminaConfig.get();
		if (mc.player == null || mc.world == null || mc.options.hudHidden || !cfg.enabled) return;
		// the HUD editor draws the modules itself
		if (mc.currentScreen instanceof HudEditorScreen) return;

		CpsTracker.poll(mc);
		TextRenderer tr = mc.textRenderer;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		if (cfg.lowHealthWarning && mc.player.getHealth() <= 6.0f && mc.player.isAlive()) {
			float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 200.0);
			int a = (int) (40 + 70 * pulse);
			int c = (a << 24) | 0xFF0000;
			int t = 14;
			ctx.fill(0, 0, sw, t, c);
			ctx.fill(0, sh - t, sw, sh, c);
			ctx.fill(0, t, t, sh - t, c);
			ctx.fill(sw - t, t, sw, sh - t, c);
		}
		for (HudModule m : Modules.ALL) {
			if (!m.enabled()) continue;
			if (!m.prepare(mc, tr, false)) continue;
			m.place(sw, sh);
			m.render(ctx, mc, tr);
		}
	}

	private static boolean targetsEnemy(MinecraftClient mc, LuminaConfig cfg) {
		if (mc.crosshairTarget instanceof EntityHitResult ehr) {
			Entity e = ehr.getEntity();
			return switch (cfg.enemyTarget) {
				case 0 -> e instanceof PlayerEntity;
				case 1 -> e instanceof LivingEntity;
				default -> true;
			};
		}
		return false;
	}

	/** Custom crosshair: solid colours (no vanilla invert-blending), optional outline, enemy variant. */
	public static void drawCrosshair(DrawContext ctx, MinecraftClient mc) {
		LuminaConfig cfg = LuminaConfig.get();
		int cx = ctx.getScaledWindowWidth() / 2;
		int cy = ctx.getScaledWindowHeight() / 2;

		if (cfg.enemyCrosshair && targetsEnemy(mc, cfg)) {
			CrosshairArt.draw(ctx, cfg.enemyPixels, cx, cy, cfg.crosshairScale, CrosshairArt.color(cfg.enemyColor), cfg.crosshairOutline);
			return;
		}
		int c = CrosshairArt.color(cfg.crosshairColor);
		if (cfg.crosshairDrawn) {
			CrosshairArt.draw(ctx, cfg.crosshairPixels, cx, cy, cfg.crosshairScale, c, cfg.crosshairOutline);
			return;
		}
		int s = cfg.crosshairSize, g = cfg.crosshairGap, t = cfg.crosshairThickness;
		int x0 = cx - t / 2, y0 = cy - t / 2;
		if (cfg.crosshairOutline) {
			int o = 0xFF000000;
			ctx.fill(x0 - g - s - 1, y0 - 1, x0 - g + 1, y0 + t + 1, o);
			ctx.fill(x0 + t + g - 1, y0 - 1, x0 + t + g + s + 1, y0 + t + 1, o);
			ctx.fill(x0 - 1, y0 - g - s - 1, x0 + t + 1, y0 - g + 1, o);
			ctx.fill(x0 - 1, y0 + t + g - 1, x0 + t + 1, y0 + t + g + s + 1, o);
		}
		ctx.fill(x0 - g - s, y0, x0 - g, y0 + t, c);
		ctx.fill(x0 + t + g, y0, x0 + t + g + s, y0 + t, c);
		ctx.fill(x0, y0 - g - s, x0 + t, y0 - g, c);
		ctx.fill(x0, y0 + t + g, x0 + t, y0 + t + g + s, c);
		if (cfg.crosshairDot) ctx.fill(x0, y0, x0 + t, y0 + t, c);
	}
}
