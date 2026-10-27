package dev.ray;
import net.minecraft.sound.SoundCategory;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/** Tracks combo, reach, hit markers and pearl cooldown for the PvP HUD modules (display only). */
public final class CombatTracker {
	public static int combo = 0;
	public static double reach = -1;
	private static long lastHit = 0;
	private static long pearlUntil = 0;
	private static boolean wasHurt = false;
	private static float lastHealth = -1;

	private CombatTracker() {}

	public static void init() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (player == mc.player && entity instanceof LivingEntity) {
				combo++;
				lastHit = System.currentTimeMillis();
				reach = hit != null ? hit.getPos().distanceTo(player.getEyePos()) : player.distanceTo(entity);
				if (RayConfig.get().hitSound) {
					mc.getSoundManager().play(new PositionedSoundInstance(
    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 
    SoundCategory.PLAYERS, 
    1.0f, // volume
    1.6f, // pitch
    mc.player.getBlockPos()
));
				}
			}
			return ActionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, world, hand) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (player == mc.player && player.getStackInHand(hand).isOf(Items.ENDER_PEARL)
				&& System.currentTimeMillis() >= pearlUntil) {
				pearlUntil = System.currentTimeMillis() + 1000L;
			}
			return ActionResult.PASS;
		});
	}

	public static long lastHitMs() { return lastHit; }

	public static long pearlRemainingMs() { return Math.max(0L, pearlUntil - System.currentTimeMillis()); }

	public static void tick(MinecraftClient mc) {
		if (mc.player == null) return;
		boolean hurt = mc.player.hurtTime > 0;
		if (hurt && !wasHurt) combo = 0;
		wasHurt = hurt;
		float hp = mc.player.getHealth();
		if (lastHealth >= 0 && hp < lastHealth) combo = 0;
		lastHealth = hp;
		if (combo > 0 && System.currentTimeMillis() - lastHit > 4000L) combo = 0;
	}
}
