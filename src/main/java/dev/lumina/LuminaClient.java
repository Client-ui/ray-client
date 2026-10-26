package dev.lumina;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LuminaClient implements ClientModInitializer {
	public static final String MOD_ID = "lumina";
	private static final int ZOOM_FOV = 30;

	private static KeyBinding menuKey;
	private static KeyBinding zoomKey;
	private static boolean zooming = false;
	private static int savedFov = 70;
	private static boolean sprintForced = false;

	@Override
	public void onInitializeClient() {
		LuminaConfig.load();

		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
		menuKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		zoomKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.lumina.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(MOD_ID, "hud"), Hud::render);
		ClientTickEvents.END_CLIENT_TICK.register(LuminaClient::onTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> stopZoom(client));
	}

	private static void onTick(MinecraftClient mc) {
		CpsTracker.poll(mc);

		while (menuKey.wasPressed()) {
			mc.setScreen(new LuminaScreen(mc.currentScreen));
		}

		// Zoom
		boolean wantZoom = zoomKey.isPressed() && mc.currentScreen == null && mc.player != null;
		if (wantZoom && !zooming) {
			savedFov = mc.options.getFov().getValue();
			mc.options.getFov().setValue(ZOOM_FOV);
			zooming = true;
		} else if (!wantZoom && zooming) {
			stopZoom(mc);
		}

		// Toggle sprint (just holds the vanilla sprint key while you move forward)
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

	private static void stopZoom(MinecraftClient mc) {
		if (zooming) {
			mc.options.getFov().setValue(savedFov);
			zooming = false;
		}
	}
}
