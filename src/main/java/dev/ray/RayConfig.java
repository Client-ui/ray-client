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
	/** Zoom strength in tenths: 30 = 3.0x, range 15..100. */
	public int zoomLevel = 30;
	public boolean customCrosshair = false;
	public int crosshairSize = 5;
	public int crosshairGap = 2;
	public int crosshairThickness = 1;
	public boolean crosshairDot = false;
	public boolean toggleSneak = false;
	public boolean fullbright = false;
	/** Fullbright brightness (gamma). 100% in the vanilla slider is 1.0; Fullbright goes far past it. */
	public int fullbrightLevel = 15;
	/** 0 = gamma only, 1 = gamma + night vision effect. */
	public int fullbrightMode = 0;
	// Waypoints shown in the world
	public boolean waypointMarkers = true;
	public boolean waypointShowDist = true;
	/** 0 = unlimited. */
	public int waypointMaxDist = 0;
	/** Saves a "Death" waypoint where you die. */
	public boolean deathWaypoint = true;
	public Waypoint lastDeath = null;
	// Free look
	public boolean freelook = false;
	/** Keyboard key (GLFW code) or -1. */
	public int freelookKey = 342; // left alt
	/** Mouse button (GLFW code, 2 and up) or -1. */
	public int freelookBtn = -1;
	/** false = hold the key, true = press once to start and again to stop. */
	public boolean freelookToggle = false;
	/** Switch to third person while free look is on, and back to first person afterwards. */
	public boolean freelookThirdPerson = true;
	public boolean blockOutline = false;
	public boolean outlineRainbow = false;
	public boolean outlineGlow = true;
	/** 0 = only the face you are looking at (default), 1 = the whole block. */
	public int outlineMode = 0;
	/** While the block outline is on: turns off view bobbing + FOV effects so the outline lines up exactly. */
	public boolean outlineStabilize = true;
	public int outlineColor = 0;
	public int outlineThickness = 2;
	public int outlineOpacity = 100;
	public boolean smoothZoom = true;
	public boolean hitMarker = false;
	public boolean hitSound = false;
	// tunables that used to be hard-coded
	public int lowHealthHearts = 3;
	public int lowHealthColor = 0xFF0000;
	public int lowHealthThickness = 14;
	public int hitMarkerColor = 0xFFFFFF;
	public int hitMarkerSize = 8;
	public int hitMarkerTime = 220;
	/** Percent. */
	public int hitSoundVolume = 100;
	/** Percent (160 = 1.6, the old fixed value). */
	public int hitSoundPitch = 160;
	/** Play your own file from the ray-sounds folder instead of the Minecraft sound. */
	public boolean hitSoundCustom = false;
	public String hitSoundFile = "";
	/** Percent; 100 = the file at its original speed. */
	public int hitSoundFilePitch = 100;
	public int durabilityPercent = 10;
	public int durabilitySeconds = 15;
	/** Ticks to wait on the death screen before respawning. */
	public int respawnDelay = 0;
	// Hit color: recolours the red flash entities get when hurt
	public boolean hitColor = false;
	public int hitColorRgb = 0xFF0000;
	public int hitColorStrength = 70;
	// Custom hitbox
	public boolean hitbox = false;
	public int hitboxColor = 0xFFFFFF;
	public int hitboxHoverColor = 0xFF4040;
	public boolean hitboxHover = true;
	public int hitboxThickness = 1;
	public int hitboxOpacity = 100;
	/** vanilla's red eye-height line (off by default). */
	public boolean hitboxEye = false;
	/** vanilla's blue look-direction line (off by default). */
	public boolean hitboxLook = false;
	/** 0 = players, 1 = all living entities. */
	public int hitboxTarget = 0;
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
	/** Overall crosshair size multiplier you can type in (1.0 = normal, 1.33 = a third bigger). */
	public float crosshairZoom = 1.0f;
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
	/** Ray background on the singleplayer / multiplayer / options screens (when not in a world). */
	public boolean customBackground = true;
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
			loaded.lastDeath = old.lastDeath;
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
