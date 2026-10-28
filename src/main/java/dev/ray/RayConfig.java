package dev.ray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RayConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ray.json");
	private static RayConfig instance = new RayConfig();

	public static class Waypoint {
		public String name = "Waypoint";
		public double x, y, z;
		public String dim = "minecraft:overworld";
	}

	/** Per-module look and options. */
	public static class Style {
		public int textColor = 0;
		public int accentColor = 0;
		/** -1 = use the global HUD background, otherwise 0-100 opacity. */
		public int bg = -1;
		public int scale = 100;
		/** -1 = global, 1 = on, 0 = off. */
		public int shadow = -1;
		public boolean accentBar = true;
		public boolean border = false;
		/** GLFW key code that toggles this module, -1 = none. */
		public int key = -1;
		public Map<String, Integer> opts = new HashMap<>();
	}

	public static class Pos {
		public int x, y;
		public Pos() {}
		public Pos(int x, int y) { this.x = x; this.y = y; }
	}

	// Master switch + per-module state/positions
	public boolean enabled = true;
	public Map<String, Boolean> modules = new HashMap<>();
	public Map<String, Pos> positions = new HashMap<>();
	public Map<String, Style> styles = new HashMap<>();
	// Gameplay
	public boolean toggleSprint = false;
	public int zoomFov = 30;
	public boolean customCrosshair = false;
	public int crosshairSize = 5;
	public int crosshairGap = 2;
	public int crosshairThickness = 1;
	public boolean crosshairDot = false;
	public boolean toggleSneak = false;
	public boolean fullbright = false;
	public boolean blockOutline = false;
	public boolean outlineRainbow = false;
	public boolean outlineGlow = true;
	/** While the block outline is on: turns off view bobbing + FOV effects so the outline lines up exactly. */
	public boolean outlineStabilize = true;
	public int outlineColor = 0;
	public int outlineThickness = 2;
	public int outlineOpacity = 100;
	public boolean smoothZoom = true;
	public boolean hitMarker = false;
	public boolean hitSound = false;
	public boolean noHurtCam = false;
	public boolean hideScoreboard = false;
	public int scoreboardScale = 100;
	public boolean lowHealthWarning = false;
	public boolean durabilityAlert = false;
	public boolean autoRespawn = false;
	public long totalSeconds = 0;
	// Viewmodel / animation
	public boolean viewmodel = false;
	public int vmX = 0, vmY = 0, vmZ = 0, vmScale = 100;
	public boolean customSwing = false;
	public int swingDuration = 6;
	// Crosshair editor
	public boolean crosshairDrawn = false;
	public boolean enemyCrosshair = false;
	/** Draw the crosshair with vanilla's colour-inverting blend instead of flat colours. */
	public boolean crosshairVanillaBlend = false;
	public boolean crosshairOutline = false;
	public int crosshairColor = 0;
	public int enemyColor = 2;
	public int crosshairScale = 2;
	public int enemyTarget = 0;
	public String crosshairPixels = CrosshairArt.cross();
	public String enemyPixels = CrosshairArt.x();
	// Client screens
	public boolean customTitle = true;
	public boolean customLoading = true;
	public boolean zoomEnabled = true;
	/** Module ids the player starred in the menu. */
	public List<String> favorites = new ArrayList<>();
	// Theme
	public int accent = 0;
	public boolean rainbow = false;
	public int hudOpacity = 40;
	public boolean textShadow = true;
	// Data
	public List<Waypoint> waypoints = new ArrayList<>();

	public Style style(String id) {
		if (styles == null) styles = new HashMap<>();
		Style st = styles.get(id);
		if (st == null) {
			st = new Style();
			styles.put(id, st);
		}
		if (st.opts == null) st.opts = new HashMap<>();
		return st;
	}

	public static RayConfig get() {
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				RayConfig loaded = GSON.fromJson(Files.readString(FILE), RayConfig.class);
				if (loaded != null) {
					if (loaded.waypoints == null) loaded.waypoints = new ArrayList<>();
					if (loaded.favorites == null) loaded.favorites = new ArrayList<>();
					if (loaded.modules == null) loaded.modules = new HashMap<>();
					if (loaded.positions == null) loaded.positions = new HashMap<>();
					if (loaded.styles == null) loaded.styles = new HashMap<>();
					if (loaded.crosshairPixels == null) loaded.crosshairPixels = CrosshairArt.cross();
					if (loaded.enemyPixels == null) loaded.enemyPixels = CrosshairArt.x();
					instance = loaded;
				}
			}
		} catch (Exception e) {
			System.err.println("[Ray] Could not read config, using defaults: " + e);
		}
		save();
	}

	public static boolean saveTo(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(instance));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/** Replaces the active settings with a saved profile (keeps waypoints and playtime). */
	public static boolean loadFrom(Path file) {
		try {
			RayConfig loaded = GSON.fromJson(Files.readString(file), RayConfig.class);
			if (loaded == null) return false;
			RayConfig old = instance;
			loaded.waypoints = old.waypoints;
			loaded.totalSeconds = old.totalSeconds;
			loaded.favorites = old.favorites;
			if (loaded.modules == null) loaded.modules = new HashMap<>();
			if (loaded.positions == null) loaded.positions = new HashMap<>();
			if (loaded.styles == null) loaded.styles = new HashMap<>();
			if (loaded.crosshairPixels == null) loaded.crosshairPixels = CrosshairArt.cross();
			if (loaded.enemyPixels == null) loaded.enemyPixels = CrosshairArt.x();
			instance = loaded;
			save();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[Ray] Could not save config: " + e);
		}
	}
}
