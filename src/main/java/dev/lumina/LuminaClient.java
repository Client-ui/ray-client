package dev.lumina;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LuminaClient implements ClientModInitializer {
	public static final String MOD_ID = "lumina";

	private static KeyBinding menuKey;
	private static KeyBinding zoomKey;
	private static KeyBinding waypointKey;
	private static KeyBinding copyKey;
	private static KeyBinding swKey;
	private static KeyBinding swResetKey;
	private static long lastDurabilityAlert = 0;
	private static int alertTick = 0;
	private static boolean zooming = false;
	private static int savedFov = 70;
	private static boolean sprintForced = false;
	private static boolean sneakLock = false;
	private static boolean sneakForced = false;
	private static boolean nvForced = false;

	@Override
	public void onInitializeClient() {
		LuminaConfig.load();
		CombatTracker.init();

		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
		menuKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		zoomKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
		waypointKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, category));

		copyKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.copycoords", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
		swKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.stopwatch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
		swResetKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.stopwatchreset", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(MOD_ID, "hud"), Hud::render);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (ctx, tickCounter) -> {
			LuminaConfig cfg = LuminaConfig.get();
			MinecraftClient mc = MinecraftClient.getInstance();
			if (cfg.customCrosshair && mc.player != null && mc.options.getPerspective().isFirstPerson()) {
				Hud.drawCrosshair(ctx, mc);
			} else {
				original.render(ctx, tickCounter);
			}
		});
		try {
			HudElementRegistry.replaceElement(Identifier.of("minecraft", "scoreboard"), original -> (ctx, tickCounter) -> {
				LuminaConfig cfg = LuminaConfig.get();
				if (cfg.hideScoreboard) return;
				int sw = ctx.getScaledWindowWidth();
				int sh = ctx.getScaledWindowHeight();
				int[] off = Modules.scoreboard.offset(sw, sh);
				boolean custom = Modules.scoreboard.enabled() && (off[0] != 0 || off[1] != 0 || cfg.scoreboardScale != 100);
				if (!custom) {
					original.render(ctx, tickCounter);
					return;
				}
				float s = cfg.scoreboardScale / 100.0f;
				ctx.getMatrices().pushMatrix();
				ctx.getMatrices().translate(sw + off[0], sh / 2.0f + off[1]);
				ctx.getMatrices().scale(s, s);
				ctx.getMatrices().translate(-sw, -sh / 2.0f);
				original.render(ctx, tickCounter);
				ctx.getMatrices().popMatrix();
			});
		} catch (Throwable t) {
			System.err.println("[Ray Client] Could not hook the scoreboard: " + t);
		}
		ClientTickEvents.END_CLIENT_TICK.register(LuminaClient::onTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(LuminaClient::stopZoom);
	}

	public static void addWaypoint(MinecraftClient mc) {
		if (mc == null || mc.player == null || mc.world == null) return;
		LuminaConfig cfg = LuminaConfig.get();
		LuminaConfig.Waypoint w = new LuminaConfig.Waypoint();
		w.name = "Waypoint " + (cfg.waypoints.size() + 1);
		w.x = mc.player.getX();
		w.y = mc.player.getY();
		w.z = mc.player.getZ();
		w.dim = mc.world.getRegistryKey().getValue().toString();
		cfg.waypoints.add(w);
		LuminaConfig.save();
		mc.player.sendMessage(Text.literal("Saved " + w.name), true);
	}

	private static void onTick(MinecraftClient mc) {
		CpsTracker.poll(mc);
		Modules.tick(mc);
		CombatTracker.tick(mc);
		ModuleKeys.tick(mc);
		while (swKey.wasPressed()) Modules.stopwatchToggle();
		while (swResetKey.wasPressed()) Modules.stopwatchReset();

		while (menuKey.wasPressed()) {
			if (mc.player != null && mc.currentScreen == null) mc.setScreen(new HudEditorScreen());
		}
		while (waypointKey.wasPressed()) {
			if (mc.currentScreen == null) addWaypoint(mc);
		}

		while (copyKey.wasPressed()) {
			if (mc.player != null) {
				String coords = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
				mc.keyboard.setClipboard(coords);
				mc.player.sendMessage(Text.literal("Copied " + coords), true);
			}
		}

		// Auto respawn
		if (LuminaConfig.get().autoRespawn && mc.player != null && mc.currentScreen instanceof DeathScreen) {
			mc.player.requestRespawn();
		}

		// Durability alert (checked once a second)
		if (LuminaConfig.get().durabilityAlert && mc.player != null && ++alertTick >= 20) {
			alertTick = 0;
			EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
			for (EquipmentSlot es : slots) {
				ItemStack st = mc.player.getEquippedStack(es);
				if (st.isEmpty() || !st.isDamageable() || st.getMaxDamage() <= 0) continue;
				int left = st.getMaxDamage() - st.getDamage();
				if (left * 10 <= st.getMaxDamage() && System.currentTimeMillis() - lastDurabilityAlert > 15000L) {
					lastDurabilityAlert = System.currentTimeMillis();
					mc.player.sendMessage(Text.literal("Low durability: " + st.getName().getString()), true);
					break;
				}
			}
		}

		// Zoom (optionally smooth)
		boolean wantZoom = zoomKey.isPressed() && mc.currentScreen == null && mc.player != null;
		LuminaConfig zc = LuminaConfig.get();
		if (wantZoom && !zooming) {
			savedFov = mc.options.getFov().getValue();
			zooming = true;
		}
		if (zooming) {
			int target = wantZoom ? zc.zoomFov : savedFov;
			int cur = mc.options.getFov().getValue();
			int next = zc.smoothZoom ? cur + Math.max(-8, Math.min(8, target - cur)) : target;
			mc.options.getFov().setValue(next);
			if (!wantZoom && next == savedFov) zooming = false;
		}

		// Toggle sneak
		LuminaConfig c2 = LuminaConfig.get();
		while (mc.options.sneakKey.wasPressed()) {
			if (c2.toggleSneak && mc.currentScreen == null) sneakLock = !sneakLock;
		}
		if (c2.toggleSneak && mc.player != null && mc.currentScreen == null) {
			mc.options.sneakKey.setPressed(sneakLock);
			sneakForced = true;
		} else if (sneakForced) {
			mc.options.sneakKey.setPressed(false);
			sneakForced = false;
			sneakLock = false;
		}

		// Fullbright (client-side night vision)
		if (c2.fullbright && mc.player != null) {
			if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
				mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
				nvForced = true;
			}
		} else if (nvForced) {
			if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
			nvForced = false;
		}

		// Toggle sprint
		LuminaConfig cfg = LuminaConfig.get();
		boolean sprint = cfg.toggleSprint && mc.player != null && mc.currentScreen == null;
		if (sprint && mc.options.forwardKey.isPressed()) {
			mc.options.sprintKey.setPressed(true);
			sprintForced = true;
		} else if (sprintForced) {
			mc.options.sprintKey.setPressed(false);
			sprintForced = false;
		}
	}

	public static boolean fullbrightActive() {
		return nvForced;
	}

	private static void stopZoom(MinecraftClient mc) {
		if (zooming) {
			mc.options.getFov().setValue(savedFov);
			zooming = false;
		}
	}
}
