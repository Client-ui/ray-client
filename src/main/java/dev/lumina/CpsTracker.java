package dev.lumina;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Counts mouse clicks per second by polling GLFW (no mixins needed). */
public final class CpsTracker {
	private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftDown, rightDown;

	private CpsTracker() {}

	public static void poll(MinecraftClient mc) {
		long window = mc.getWindow().getHandle();
		boolean l = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		long now = System.currentTimeMillis();
		if (mc.currentScreen == null) {
			if (l && !leftDown) LEFT.addLast(now);
			if (r && !rightDown) RIGHT.addLast(now);
		}
		leftDown = l;
		rightDown = r;
	}

	public static int left() { return count(LEFT); }
	public static int right() { return count(RIGHT); }
	public static boolean leftHeld() { return leftDown; }
	public static boolean rightHeld() { return rightDown; }

	private static int count(ArrayDeque<Long> q) {
		long cutoff = System.currentTimeMillis() - 1000;
		while (!q.isEmpty() && q.peekFirst() < cutoff) q.removeFirst();
		return q.size();
	}
}
