package dev.lumina;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

/** Tracks combo and reach for the PvP HUD modules (display only). */
public final class CombatTracker {
	public static int combo = 0;
	public static double reach = -1;
	private static long lastHit = 0;
	private static boolean wasHurt = false;

	private CombatTracker() {}

	public static void init() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (player == mc.player && entity instanceof LivingEntity) {
				combo++;
				lastHit = System.currentTimeMillis();
				reach = hit != null ? hit.getPos().distanceTo(player.getEyePos()) : player.distanceTo(entity);
			}
			return ActionResult.PASS;
		});
	}

	public static void tick(MinecraftClient mc) {
		if (mc.player == null) return;
		boolean hurt = mc.player.hurtTime > 0;
		if (hurt && !wasHurt) combo = 0;
		wasHurt = hurt;
		if (combo > 0 && System.currentTimeMillis() - lastHit > 4000L) combo = 0;
	}
}
