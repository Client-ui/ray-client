package dev.lumina.mixin;

import dev.lumina.LuminaConfig;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Viewmodel mod: move and scale the first-person held item. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
	@Inject(method = "applyEquipOffset", at = @At("RETURN"), require = 0)
	private void lumina$viewmodel(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
		LuminaConfig cfg = LuminaConfig.get();
		if (!cfg.viewmodel) return;
		float side = arm == Arm.RIGHT ? 1.0f : -1.0f;
		matrices.translate(side * cfg.vmX / 100.0f, cfg.vmY / 100.0f, cfg.vmZ / 100.0f);
		float s = cfg.vmScale / 100.0f;
		matrices.scale(s, s, s);
	}
}
