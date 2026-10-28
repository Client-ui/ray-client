package dev.ray;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Block outline for the block you are looking at.
 *
 * The edges of the block's real outline shape (stairs, slabs, fences... not just a cube) are projected into
 * screen space from the interpolated camera and drawn as crisp lines. Edges on the far side of the block are
 * hidden like vanilla's depth-tested outline. Lines are clipped at the camera near plane so nothing explodes
 * when you stand right next to a block.
 *
 * It lines up exactly when view bobbing and FOV effects are off; the "Stabilize View" option does that for you
 * while the outline is on.
 */
public final class BlockOutline {
	private static final double NEAR = 0.05;
	private static final double EPS = 1.0e-6;

	private BlockOutline() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		RayConfig cfg = RayConfig.get();
		if (!cfg.blockOutline) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
		if (!mc.options.getPerspective().isFirstPerson()) return;
		if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

		BlockPos pos = bhr.getBlockPos();
		BlockState state = mc.world.getBlockState(pos);
		if (state.isAir()) return;
		VoxelShape shape = state.getOutlineShape(mc.world, pos);
		if (shape.isEmpty()) return;

		// ---- camera ----
		float tp = tickCounter.getTickProgress(false);
		Vec3d cam = mc.player.getCameraPosVec(tp);
		double yaw = Math.toRadians(mc.player.getYaw(tp));
		double pitch = Math.toRadians(mc.player.getPitch(tp));
		double cyaw = Math.cos(yaw), syaw = Math.sin(yaw), cpit = Math.cos(pitch), spit = Math.sin(pitch);
		final double fx = -syaw * cpit, fy = -spit, fz = cyaw * cpit;   // forward
		final double rx = -cyaw, ry = 0.0, rz = -syaw;                  // right
		final double ux = -syaw * spit, uy = cpit, uz = cyaw * spit;    // up

		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		double baseFov = mc.options.getFov().getValue();
		double fov = baseFov * (ZoomState.hooked && cfg.zoomEnabled ? ZoomState.fovFactor(baseFov, ZoomState.update()) : 1.0);
		double tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
		final double xs = sw / 2.0 / (tanHalf * ((double) sw / sh));
		final double ys = sh / 2.0 / tanHalf;
		final double hx = sw / 2.0, hy = sh / 2.0;

		// camera relative to the block, to know which faces of its bounding box face us
		Box bb = shape.getBoundingBox();
		final double lcx = cam.x - pos.getX(), lcy = cam.y - pos.getY(), lcz = cam.z - pos.getZ();
		final double[] bmin = {bb.minX, bb.minY, bb.minZ}, bmax = {bb.maxX, bb.maxY, bb.maxZ};
		final double[] lc = {lcx, lcy, lcz};

		final List<float[]> lines = new ArrayList<>();
		final BlockPos bp = pos;
		shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
			double[] a = {x1, y1, z1}, b = {x2, y2, z2};

			// hide edges that only touch faces turned away from the camera
			int planes = 0;
			boolean visible = false;
			for (int i = 0; i < 3; i++) {
				if (Math.abs(a[i] - bmin[i]) < EPS && Math.abs(b[i] - bmin[i]) < EPS) {
					planes++;
					if (lc[i] < bmin[i]) visible = true;
				}
				if (Math.abs(a[i] - bmax[i]) < EPS && Math.abs(b[i] - bmax[i]) < EPS) {
					planes++;
					if (lc[i] > bmax[i]) visible = true;
				}
			}
			if (planes > 0 && !visible) return;

			// world -> camera space
			double[] pa = toCam(x1 + bp.getX() - cam.x, y1 + bp.getY() - cam.y, z1 + bp.getZ() - cam.z, rx, ry, rz, ux, uy, uz, fx, fy, fz);
			double[] pb = toCam(x2 + bp.getX() - cam.x, y2 + bp.getY() - cam.y, z2 + bp.getZ() - cam.z, rx, ry, rz, ux, uy, uz, fx, fy, fz);

			// clip against the near plane
			if (pa[2] < NEAR && pb[2] < NEAR) return;
			if (pa[2] < NEAR) pa = clip(pa, pb);
			else if (pb[2] < NEAR) pb = clip(pb, pa);

			float sx1 = (float) (hx + pa[0] / pa[2] * xs), sy1 = (float) (hy - pa[1] / pa[2] * ys);
			float sx2 = (float) (hx + pb[0] / pb[2] * xs), sy2 = (float) (hy - pb[1] / pb[2] * ys);
			lines.add(new float[]{sx1, sy1, sx2, sy2});
		});
		if (lines.isEmpty()) return;

		// ---- style ----
		int base = cfg.outlineRainbow
			? (Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.65f, 1f) & 0xFFFFFF)
			: (Theme.pick(cfg.outlineColor, Theme.accent()) & 0xFFFFFF);
		int alpha = Math.max(20, Math.min(100, cfg.outlineOpacity)) * 255 / 100;
		float gs = (float) mc.getWindow().getScaleFactor();
		int thick = Math.max(1, Math.round(cfg.outlineThickness * gs / 2.0f));

		if (cfg.outlineGlow) {
			int glow = ((alpha * 40 / 100) << 24) | base;
			for (float[] l : lines) line(ctx, l, thick * 3, gs, glow);
		}
		int main = (alpha << 24) | base;
		for (float[] l : lines) line(ctx, l, thick, gs, main);
	}

	private static double[] toCam(double dx, double dy, double dz,
								  double rx, double ry, double rz, double ux, double uy, double uz,
								  double fx, double fy, double fz) {
		return new double[]{dx * rx + dy * ry + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
	}

	/** Moves the point that is behind the camera onto the near plane along the segment. */
	private static double[] clip(double[] behind, double[] front) {
		double t = (NEAR - behind[2]) / (front[2] - behind[2]);
		return new double[]{
			behind[0] + (front[0] - behind[0]) * t,
			behind[1] + (front[1] - behind[1]) * t,
			NEAR
		};
	}

	/** A line of the given thickness in real screen pixels (a rotated rectangle, so any angle stays crisp). */
	private static void line(DrawContext ctx, float[] l, int thickPx, float guiScale, int color) {
		float dx = l[2] - l[0], dy = l[3] - l[1];
		float len = (float) Math.hypot(dx, dy);
		if (len < 0.01f || len > 20000f) return;
		ctx.getMatrices().pushMatrix();
		ctx.getMatrices().translate(l[0], l[1]);
		ctx.getMatrices().rotate((float) Math.atan2(dy, dx));
		ctx.getMatrices().scale(1.0f / guiScale, 1.0f / guiScale);
		int length = Math.round(len * guiScale);
		int half = thickPx / 2;
		ctx.fill(-half, -half, length + (thickPx - half), thickPx - half, color);
		ctx.getMatrices().popMatrix();
	}
}
