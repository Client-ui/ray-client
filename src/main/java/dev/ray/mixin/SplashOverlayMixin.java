package dev.ray.mixin;

import dev.ray.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Subtle Ray branding during Minecraft's normal resource-loading screen. */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void ray$drawLoadingBrand(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int accent = Theme.accent();
        context.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("RAY CLIENT"), width / 2, height - 34, accent);
        context.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("v1.5"), width / 2, height - 22, 0xFFB8C9E8);
    }
}
