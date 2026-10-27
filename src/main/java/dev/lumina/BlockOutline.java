package dev.lumina;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.awt.Color;

/**
 * Block outline for the block you are looking at (shape-accurate, hidden edges removed).
 * It is projected and drawn on screen, so it does not depend on the 3D render pipeline.
 */
public final class BlockOutline {
	private static final double NEAR = 0.05;

	private BlockOutline() {}

	private record View(double cx, double cy, double cz, double fx, double fy, double fz,
						double rx, double rz, double ux, double uy, double uz,
						double tanHalf, double aspect, int sw, int sh) {
		double[] toCam(double wx, double wy, double wz) {
			double dx = wx - cx, dy = wy - cy, dz = wz - cz;
			return new double[]{dx * rx + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
		}
	}

	public static void render(DrawContext ctx, MinecraftClient mc) {
		LuminaConfig cfg = LuminaConfig.get();
		if (!cfg.blockOutline || mc.world == null || mc.player == null || mc.currentScreen != null) return;
		if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

		BlockPos pos = bhr.getBlockPos();
		VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
		if (shape.isEmpty()) return;

		Camera cam = mc.gameRenderer.getCamera();
		Vec3d cp = cam.getPos();
		double yaw = Math.toRadians(cam.getYaw());
		double pitch = Math.toRadians(cam.getPitch());
		double cyaw = Math.cos(yaw), syaw = Math.sin(yaw), cpit = Math.cos(pitch), spit = Math.sin(pitch);
		double fx = -syaw * cpit, fy = -spit, fz = cyaw * cpit;
		double rx = -cyaw, rz = -syaw;
		double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
		int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
		double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
		final View v = new View(cp.x, cp.y, cp.z, fx, fy, fz, rx, rz, ux, uy, uz, tanHalf, (double) sw / sh, sw, sh);

		int rgb = cfg.outlineRainbow
			? (Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.7f, 1f) & 0xFFFFFF)
			: (Theme.pick(cfg.outlineColor, Theme.accent()) & 0xFFFFFF);
		final int alpha = cfg.outlineOpacity * 255 / 100;
		final int core = (alpha << 24) | rgb;
		final int glow = ((alpha / 4) << 24) | rgb;
		final int thick = cfg.outlineThickness;
		final boolean useGlow = cfg.outlineGlow;

		Box bb = shape.getBoundingBox();
		final double[] mn = {bb.minX, bb.minY, bb.minZ};
		final double[] mx = {bb.maxX, bb.maxY, bb.maxZ};
		final double[] cam0 = {cp.x - pos.getX(), cp.y - pos.getY(), cp.z - pos.getZ()};
		final int bx = pos.getX(), by = pos.getY(), bz = pos.getZ();

		shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
			double[] m = {(x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2};
			boolean onFace = false, front = false;
			for (int a = 0; a < 3; a++) {
				if (Math.abs(m[a] - mn[a]) < 1e-4) {
					onFace = true;
					if (cam0[a] < mn[a]) front = true;
				}
				if (Math.abs(m[a] - mx[a]) < 1e-4) {
					onFace = true;
					if (cam0[a] > mx[a]) front = true;
				}
			}
			if (onFace && !front) return; // edge is on a face turned away from you
			if (useGlow) edge(ctx, v, bx + x1, by + y1, bz + z1, bx + x2, by + y2, bz + z2, thick + 2, glow);
			edge(ctx, v, bx + x1, by + y1, bz + z1, bx + x2, by + y2, bz + z2, thick, core);
		});
	}

	private static double[] clip(double[] a, double[] b) {
		double t = (NEAR - a[2]) / (b[2] - a[2]);
		return new double[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1]), NEAR};
	}

	private static void edge(DrawContext ctx, View v, double ax, double ay, double az, double bx, double by, double bz, int t, int color) {
		double[] p = v.toCam(ax, ay, az);
		double[] q = v.toCam(bx, by, bz);
		if (p[2] < NEAR && q[2] < NEAR) return;
		if (p[2] < NEAR) p = clip(p, q);
		else if (q[2] < NEAR) q = clip(q, p);
		double x1 = (0.5 + 0.5 * p[0] / (p[2] * v.tanHalf * v.aspect)) * v.sw;
		double y1 = (0.5 - 0.5 * p[1] / (p[2] * v.tanHalf)) * v.sh;
		double x2 = (0.5 + 0.5 * q[0] / (q[2] * v.tanHalf * v.aspect)) * v.sw;
		double y2 = (0.5 - 0.5 * q[1] / (q[2] * v.tanHalf)) * v.sh;
		double dx = x2 - x1, dy = y2 - y1;
		int steps = (int) Math.min(2500.0, Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
		if (steps < 1) steps = 1;
		for (int i = 0; i <= steps; i++) {
			double f = i / (double) steps;
			int px = (int) Math.round(x1 + dx * f) - t / 2;
			int py = (int) Math.round(y1 + dy * f) - t / 2;
			if (px < -t || py < -t || px > v.sw || py > v.sh) continue;
			ctx.fill(px, py, px + t, py + t, color);
		}
	}
}
