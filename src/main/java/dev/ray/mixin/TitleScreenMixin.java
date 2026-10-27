package dev.ray.mixin;

import dev.ray.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a lightweight Ray identity badge to the vanilla title screen without replacing vanilla buttons. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void ray$drawBrand(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int accent = Theme.accent();
        String title = "RAY CLIENT";
        String version = "v1.5  \u2022  Multiplayer-friendly";
        int tw = net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(title);
        int x = (width - tw) / 2;
        context.fill(x - 10, 8, x + tw + 10, 31, 0xB80A1424);
        context.fill(x - 10, 8, x - 7, 31, accent);
        context.drawCenteredTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, Text.literal(title), width / 2, 11, accent);
        context.drawCenteredTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, Text.literal(version), width / 2, 22, 0xFFB8C9E8);
    }
}
