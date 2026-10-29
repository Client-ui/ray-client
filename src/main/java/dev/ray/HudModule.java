package dev.ray;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** A draggable, customisable HUD element. */
public abstract class HudModule {
	public static final int TL = 0, TR = 1, BL = 2, BR = 3, ML = 4, MR = 5;

	public final String id, name, category;
	private final boolean defaultOn;
	private final int anchor, ox, oy;
	/** Unscaled size, valid after prepare(). */
	public int w, h;
	/** Position, valid after place(). */
	public int x, y;

	protected HudModule(String id, String name, String category, boolean defaultOn, int anchor, int ox, int oy) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.defaultOn = defaultOn;
		this.anchor = anchor;
		this.ox = ox;
		this.oy = oy;
	}

	public boolean enabled() {
		Boolean b = RayConfig.get().modules.get(id);
		return b == null ? defaultOn : b;
	}

	public void setEnabled(boolean v) {
		RayConfig.get().modules.put(id, v);
	}

	/** Work out size for this frame. Return false if there is nothing to show (unless editing). */
	public abstract boolean prepare(MinecraftClient mc, TextRenderer tr, boolean editing);

	/** Draw at (x, y) in unscaled coordinates. */
	public abstract void draw(DrawContext ctx, MinecraftClient mc, TextRenderer tr);

	// ---------- per-module style ----------

	public RayConfig.Style style() { return RayConfig.get().style(id); }
	public float scale() { return Math.max(0.5f, Math.min(2.0f, style().scale / 100.0f)); }
	/** On-screen size after scaling. */
	public int ew() { return Math.round(w * scale()); }
	public int eh() { return Math.round(h * scale()); }
	public int textColor() { return Theme.pick(style().textColor, 0xFFFFFFFF); }
	public int accentColor() { return Theme.pick(style().accentColor, Theme.accent()); }
	public int bgAlpha() {
		int o = style().bg >= 0 ? style().bg : RayConfig.get().hudOpacity;
		return o * 255 / 100;
	}
	public boolean hasBg() { return bgAlpha() > 0; }
	public int bgColor() { return bgAlpha() << 24; }
	public boolean shadow() {
		int s = style().shadow;
		return s < 0 ? RayConfig.get().textShadow : s == 1;
	}
	public boolean accentBar() { return style().accentBar; }
	public int opt(String key) { return Modules.opt(id, key); }

	/** Draws the module with its scale and border applied. */
	public final void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
		float s = scale();
		boolean scaled = Math.abs(s - 1.0f) > 0.001f;
		if (scaled) {
			ctx.getMatrices().pushMatrix();
			ctx.getMatrices().translate((float) x, (float) y);
			ctx.getMatrices().scale(s, s);
			ctx.getMatrices().translate((float) -x, (float) -y);
		}
		draw(ctx, mc, tr);
		if (style().border) {
			int c = accentColor();
			ctx.fill(x - 1, y - 1, x + w + 1, y, c);
			ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, c);
			ctx.fill(x - 1, y, x, y + h, c);
			ctx.fill(x + w, y, x + w + 1, y + h, c);
		}
		if (scaled) ctx.getMatrices().popMatrix();
	}

	// ---------- position ----------

	/** Where this module sits when the player has not moved it. */
	public int[] defaultPos(int sw, int sh) {
		int ew = ew(), eh = eh();
		switch (anchor) {
			case TR: return new int[]{sw - ew - ox, oy};
			case BL: return new int[]{ox, sh - eh - oy};
			case BR: return new int[]{sw - ew - ox, sh - eh - oy};
			case ML: return new int[]{ox, sh / 2 + oy};
			case MR: return new int[]{sw - ew - ox, sh / 2 + oy};
			default: return new int[]{ox, oy};
		}
	}

	public void place(int sw, int sh) {
		RayConfig.Pos p = RayConfig.get().positions.get(id);
		int nx, ny;
		if (p != null) {
			nx = p.x;
			ny = p.y;
		} else {
			int[] d = defaultPos(sw, sh);
			nx = d[0];
			ny = d[1];
		}
		x = Math.max(0, Math.min(sw - ew(), nx));
		y = Math.max(0, Math.min(sh - eh(), ny));
	}

	public void moveTo(int nx, int ny, int sw, int sh) {
		x = Math.max(0, Math.min(sw - ew(), nx));
		y = Math.max(0, Math.min(sh - eh(), ny));
		RayConfig.get().positions.put(id, new RayConfig.Pos(x, y));
	}
}
