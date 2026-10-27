package dev.lumina.mixin;

import dev.lumina.LuminaConfig;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Hurt Cam: skips the screen tilt when you take damage. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
	private void lumina$noHurtCam(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
		if (LuminaConfig.get().noHurtCam) ci.cancel();
	}
}
