package dev.lumina;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class Hud {
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

	private Hud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		LuminaConfig cfg = LuminaConfig.get();
		if (mc.player == null || mc.options.hudHidden || !cfg.enabled) return;

		CpsTracker.poll(mc);
		TextRenderer tr = mc.textRenderer;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();

		drawInfoPanel(ctx, mc, tr, cfg, sw, sh);
		if (cfg.keystrokes) drawKeystrokes(ctx, mc, tr, sh);
		if (cfg.armorHud) drawArmor(ctx, mc, tr, sw, sh);
	}

	private static void drawInfoPanel(DrawContext ctx, MinecraftClient mc, TextRenderer tr, LuminaConfig cfg, int sw, int sh) {
		List<String> lines = new ArrayList<>();
		if (cfg.showFps) lines.add("FPS: " + mc.getCurrentFps());
		if (cfg.showCps) lines.add("CPS: " + CpsTracker.left() + " | " + CpsTracker.right());
		if (cfg.showPing) {
			int ping = 0;
			if (mc.getNetworkHandler() != null) {
				PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
				if (e != null) ping = e.getLatency();
			}
			lines.add("Ping: " + ping + " ms");
		}
		if (cfg.showCoords) {
			lines.add(String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
		}
		if (cfg.showFacing) {
			String f = mc.player.getHorizontalFacing().asString();
			lines.add("Facing: " + Character.toUpperCase(f.charAt(0)) + f.substring(1));
		}
		if (cfg.showClock) lines.add("Time: " + LocalTime.now().format(CLOCK));
		if (cfg.showMemory) {
			Runtime rt = Runtime.getRuntime();
			long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
			lines.add("RAM: " + used + "/" + (rt.maxMemory() / 1048576L) + " MB");
		}
		if (lines.isEmpty()) return;

		int lineH = 12;
		int total = lines.size() * lineH;
		boolean right = cfg.corner == 1 || cfg.corner == 3;
		boolean bottom = cfg.corner == 2 || cfg.corner == 3;
		int y = bottom ? sh - 6 - total : 4;

		for (String line : lines) {
			int w = tr.getWidth(line) + 6;
			int x = right ? sw - 4 - w : 4;
			ctx.fill(x, y, x + w, y + lineH - 1, 0x66000000);
			ctx.drawText(tr, line, x + 3, y + 2, 0xFFFFFFFF, true);
			y += lineH;
		}
	}

	private static void drawKeystrokes(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sh) {
		int size = 20, gap = 2;
		int x0 = 4;
		int y0 = sh / 2 - 36;
		int col1 = x0 + size + gap;
		int col2 = col1 + size + gap;

		key(ctx, tr, "W", col1, y0, size, size, mc.options.forwardKey.isPressed());
		int row2 = y0 + size + gap;
		key(ctx, tr, "A", x0, row2, size, size, mc.options.leftKey.isPressed());
		key(ctx, tr, "S", col1, row2, size, size, mc.options.backKey.isPressed());
		key(ctx, tr, "D", col2, row2, size, size, mc.options.rightKey.isPressed());

		int row3 = row2 + size + gap;
		int fullW = size * 3 + gap * 2;
		int halfW = (fullW - gap) / 2;
		key(ctx, tr, "LMB " + CpsTracker.left(), x0, row3, halfW, size, CpsTracker.leftHeld());
		key(ctx, tr, "RMB " + CpsTracker.right(), x0 + halfW + gap, row3, halfW, size, CpsTracker.rightHeld());

		int row4 = row3 + size + gap;
		key(ctx, tr, "SPACE", x0, row4, fullW, 14, mc.options.jumpKey.isPressed());
	}

	private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h, boolean down) {
		ctx.fill(x, y, x + w, y + h, down ? 0xAAFFFFFF : 0x66000000);
		int color = down ? 0xFF000000 : 0xFFFFFFFF;
		ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, color, false);
	}

	private static void drawArmor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sw, int sh) {
		ItemStack[] stacks = {
			mc.player.getEquippedStack(EquipmentSlot.HEAD),
			mc.player.getEquippedStack(EquipmentSlot.CHEST),
			mc.player.getEquippedStack(EquipmentSlot.LEGS),
			mc.player.getEquippedStack(EquipmentSlot.FEET),
			mc.player.getEquippedStack(EquipmentSlot.MAINHAND)
		};
		int x = sw - 22;
		int y = sh / 2 - 45;
		for (ItemStack s : stacks) {
			if (s.isEmpty()) continue;
			ctx.drawItem(s, x, y);
			ctx.drawStackOverlay(tr, s, x, y);
			if (s.isDamageable()) {
				String d = String.valueOf(s.getMaxDamage() - s.getDamage());
				ctx.drawText(tr, d, x - tr.getWidth(d) - 3, y + 4, 0xFFFFFFFF, true);
			}
			y += 20;
		}
	}
}
