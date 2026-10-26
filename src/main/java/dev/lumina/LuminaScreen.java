package dev.lumina;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LuminaScreen extends Screen {
	private static final String[] CORNERS = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
	private final Screen parent;
	private int row = 0;

	public LuminaScreen(Screen parent) {
		super(Text.literal("Lumina Client"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		LuminaConfig c = LuminaConfig.get();
		row = 0;
		int left = width / 2 - 155;
		int right = width / 2 + 5;
		int top = 36;

		toggle(left, top, "Client HUD", () -> c.enabled, v -> c.enabled = v);
		toggle(right, top, "FPS", () -> c.showFps, v -> c.showFps = v);
		toggle(left, top + 24, "CPS", () -> c.showCps, v -> c.showCps = v);
		toggle(right, top + 24, "Ping", () -> c.showPing, v -> c.showPing = v);
		toggle(left, top + 48, "Coordinates", () -> c.showCoords, v -> c.showCoords = v);
		toggle(right, top + 48, "Direction", () -> c.showFacing, v -> c.showFacing = v);
		toggle(left, top + 72, "Clock", () -> c.showClock, v -> c.showClock = v);
		toggle(right, top + 72, "Memory", () -> c.showMemory, v -> c.showMemory = v);
		toggle(left, top + 96, "Keystrokes", () -> c.keystrokes, v -> c.keystrokes = v);
		toggle(right, top + 96, "Armor HUD", () -> c.armorHud, v -> c.armorHud = v);
		toggle(left, top + 120, "Toggle Sprint", () -> c.toggleSprint, v -> c.toggleSprint = v);

		addDrawableChild(ButtonWidget.builder(Text.literal("Panel: " + CORNERS[c.corner]), btn -> {
			c.corner = (c.corner + 1) % CORNERS.length;
			btn.setMessage(Text.literal("Panel: " + CORNERS[c.corner]));
			LuminaConfig.save();
		}).dimensions(right, top + 120, 150, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
			.dimensions(width / 2 - 75, top + 156, 150, 20).build());
	}

	private void toggle(int x, int y, String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		addDrawableChild(ButtonWidget.builder(label(name, getter.get()), btn -> {
			setter.accept(!getter.get());
			btn.setMessage(label(name, getter.get()));
			LuminaConfig.save();
		}).dimensions(x, y, 150, 20).build());
	}

	private static Text label(String name, boolean on) {
		return Text.literal(name + ": " + (on ? "ON" : "OFF"));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
	}

	@Override
	public void close() {
		LuminaConfig.save();
		client.setScreen(parent);
	}
}
