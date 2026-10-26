package dev.lumina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LuminaConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("lumina.json");
	private static LuminaConfig instance = new LuminaConfig();

	public boolean enabled = true;
	public boolean showFps = true;
	public boolean showCps = true;
	public boolean showPing = true;
	public boolean showCoords = true;
	public boolean showFacing = true;
	public boolean showClock = false;
	public boolean showMemory = false;
	public boolean armorHud = true;
	public boolean keystrokes = true;
	public boolean toggleSprint = false;
	/** 0 = top-left, 1 = top-right, 2 = bottom-left, 3 = bottom-right */
	public int corner = 0;

	public static LuminaConfig get() {
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				LuminaConfig loaded = GSON.fromJson(Files.readString(FILE), LuminaConfig.class);
				if (loaded != null) instance = loaded;
			}
		} catch (Exception e) {
			System.err.println("[Lumina] Could not read config, using defaults: " + e);
		}
		save();
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[Lumina] Could not save config: " + e);
		}
	}
}
