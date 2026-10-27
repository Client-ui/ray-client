package dev.ray;

import java.awt.Color;

public final class Theme {
	public static final int[] COLORS = {0xFF3B82F6, 0xFF8B5CF6, 0xFFEF4444, 0xFF22C55E, 0xFFF97316, 0xFFE5E7EB};
	public static final String[] NAMES = {"Blue", "Purple", "Red", "Green", "Orange", "White"};

	public static final String[] PALETTE_NAMES = {"Default", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Pink", "Orange", "Purple"};
	private static final int[] PALETTE = {0, 0xFFFFFFFF, 0xFFEF4444, 0xFF22C55E, 0xFF3B82F6, 0xFFFACC15, 0xFF22D3EE, 0xFFF472B6, 0xFFF97316, 0xFF8B5CF6};

	private Theme() {}

	/** Palette colour for an index; index 0 means "use the default colour". */
	public static int pick(int idx, int def) {
		if (idx <= 0 || idx >= PALETTE.length) return def;
		return PALETTE[idx];
	}

	public static int accent() {
		RayConfig cfg = RayConfig.get();
		if (cfg.rainbow) {
			float h = (System.currentTimeMillis() % 4000L) / 4000f;
			return 0xFF000000 | (Color.HSBtoRGB(h, 0.6f, 1f) & 0xFFFFFF);
		}
		int i = Math.max(0, Math.min(COLORS.length - 1, cfg.accent));
		return COLORS[i];
	}

	public static int withAlpha(int argb, int alpha) {
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	/** HUD background colour (black with the configured opacity). */
	public static int bg() {
		int a = RayConfig.get().hudOpacity * 255 / 100;
		return a << 24;
	}
}
