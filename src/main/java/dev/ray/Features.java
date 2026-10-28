package dev.ray;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * One unified list of everything you can switch on in Ray: every HUD module plus the standalone features
 * (toggle sprint, zoom, crosshair, block outline...). The menu shows them all as cards.
 */
public final class Features {
	public static final String[] CATEGORIES = {"Stats", "World", "Player", "PvP", "Extras", "Visual", "Gameplay", "Client"};

	/** Builds the rows of a feature's settings page. */
	public interface Rows {
		void build(SettingsScreen screen, Ui ui, Ui.Form f);
	}

	/** One tile in the module grid. */
	public static final class Card {
		public final String id, name, category, keywords;
		public final BooleanSupplier on;
		public final Runnable flip;
		public final Function<Screen, Screen> settings;
		private final String iconKey;
		private ItemStack icon;

		Card(String id, String name, String category, String keywords, String iconKey, BooleanSupplier on, Runnable flip,
			 Function<Screen, Screen> settings) {
			this.id = id;
			this.name = name;
			this.category = category;
			this.keywords = (name + " " + category + " " + keywords).toLowerCase();
			this.iconKey = iconKey;
			this.on = on;
			this.flip = flip;
			this.settings = settings;
		}

		public ItemStack icon() {
			if (icon == null) icon = new ItemStack(iconItem(iconKey, category));
			return icon;
		}

		public boolean favorite() {
			return RayConfig.get().favorites.contains(id);
		}

		public void toggleFavorite() {
			List<String> fav = RayConfig.get().favorites;
			if (!fav.remove(id)) fav.add(id);
			RayConfig.save();
		}
	}

	private static List<Card> cache;

	private Features() {}

	public static String normalize(String category) {
		return "Visuals".equals(category) ? "Visual" : category;
	}

	public static List<Card> cards() {
		if (cache == null) cache = build();
		return cache;
	}

	// ---------- icons ----------

	private static final Map<String, Item> ICONS = new HashMap<>();

	static {
		ICONS.put("fps", Items.REPEATER);
		ICONS.put("cps", Items.LEVER);
		ICONS.put("ping", Items.LEAD);
		ICONS.put("coords", Items.COMPASS);
		ICONS.put("direction", Items.RECOVERY_COMPASS);
		ICONS.put("biome", Items.GRASS_BLOCK);
		ICONS.put("keystrokes", Items.LEVER);
		ICONS.put("armor", Items.IRON_CHESTPLATE);
		ICONS.put("effects", Items.POTION);
		ICONS.put("waypoints", Items.MAP);
		ICONS.put("speed", Items.SUGAR);
		ICONS.put("memory", Items.COMPARATOR);
		ICONS.put("clock", Items.CLOCK);
		ICONS.put("date", Items.BOOK);
		ICONS.put("session", Items.CLOCK);
		ICONS.put("stopwatch", Items.CLOCK);
		ICONS.put("serverip", Items.NAME_TAG);
		ICONS.put("day", Items.CLOCK);
		ICONS.put("worldtime", Items.CLOCK);
		ICONS.put("light", Items.TORCH);
		ICONS.put("compass", Items.COMPASS);
		ICONS.put("weather", Items.WATER_BUCKET);
		ICONS.put("health", Items.GOLDEN_APPLE);
		ICONS.put("food", Items.COOKED_BEEF);
		ICONS.put("arrows", Items.ARROW);
		ICONS.put("bowcharge", Items.BOW);
		ICONS.put("shield", Items.SHIELD);
		ICONS.put("pearl", Items.ENDER_PEARL);
		ICONS.put("targetinfo", Items.DIAMOND_SWORD);
		ICONS.put("inventory", Items.CHEST);
		ICONS.put("fpsgraph", Items.PAPER);
		ICONS.put("pinggraph", Items.PAPER);
		ICONS.put("xp", Items.EXPERIENCE_BOTTLE);
		ICONS.put("air", Items.GLASS_BOTTLE);
		ICONS.put("blocks", Items.CRAFTING_TABLE);
		ICONS.put("distance", Items.LEATHER_BOOTS);
		ICONS.put("online", Items.NAME_TAG);
		ICONS.put("toolcondition", Items.DIAMOND_PICKAXE);
		ICONS.put("scoreboard", Items.OAK_SIGN);
		// standalone features
		ICONS.put("f_sprint", Items.IRON_BOOTS);
		ICONS.put("f_sneak", Items.LEATHER_BOOTS);
		ICONS.put("f_zoom", Items.SPYGLASS);
		ICONS.put("f_fullbright", Items.LANTERN);
		ICONS.put("f_hurtcam", Items.TOTEM_OF_UNDYING);
		ICONS.put("f_viewmodel", Items.DIAMOND_SWORD);
		ICONS.put("f_swing", Items.IRON_SWORD);
		ICONS.put("f_hitmarker", Items.TARGET);
		ICONS.put("f_hitsound", Items.NOTE_BLOCK);
		ICONS.put("f_lowhealth", Items.GOLDEN_APPLE);
		ICONS.put("f_durability", Items.ANVIL);
		ICONS.put("f_respawn", Items.TOTEM_OF_UNDYING);
		ICONS.put("f_scoreboard", Items.OAK_SIGN);
		ICONS.put("f_crosshair", Items.TARGET);
		ICONS.put("f_outline", Items.GLASS);
		ICONS.put("f_hud", Items.BEACON);
		ICONS.put("f_title", Items.NETHER_STAR);
		ICONS.put("f_loading", Items.NETHER_STAR);
	}

	private static Item iconItem(String key, String category) {
		Item it = ICONS.get(key);
		if (it != null) return it;
		return switch (category) {
			case "Stats" -> Items.REPEATER;
			case "World" -> Items.COMPASS;
			case "Player" -> Items.LEATHER_CHESTPLATE;
			case "PvP" -> Items.DIAMOND_SWORD;
			case "Visual" -> Items.ENDER_EYE;
			case "Gameplay" -> Items.IRON_BOOTS;
			case "Client" -> Items.BEACON;
			default -> Items.NETHER_STAR;
		};
	}

	// ---------- registry ----------

	private static Card feature(String id, String name, String category, String keywords, BooleanSupplier on, Runnable flip, Rows rows) {
		return new Card(id, name, category, keywords, id, on, flip, parent -> new FeatureSettingsScreen(parent, name, rows));
	}

	private static List<Card> build() {
		List<Card> out = new ArrayList<>();

		for (HudModule m : Modules.ALL) {
			out.add(new Card(m.id, m.name, normalize(m.category), "hud module", m.id,
				m::enabled,
				() -> m.setEnabled(!m.enabled()),
				parent -> new ModuleSettingsScreen(parent, m)));
		}

		out.add(feature("f_hud", "Client HUD", "Client", "master switch all hud overlay",
			() -> RayConfig.get().enabled, () -> { RayConfig c = RayConfig.get(); c.enabled = !c.enabled; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Client HUD");
				ui.toggle(f.full(), "Enabled", c.enabled, () -> c.enabled = !c.enabled);
				f.note("Master switch for every HUD module. Your individual module settings are kept when it is off.");
			}));

		out.add(feature("f_title", "Custom Title Screen", "Client", "main menu start menu background buttons",
			() -> RayConfig.get().customTitle, () -> { RayConfig c = RayConfig.get(); c.customTitle = !c.customTitle; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Title screen");
				ui.toggle(f.full(), "Ray Client Title Screen", c.customTitle, () -> c.customTitle = !c.customTitle);
				f.note("Replaces the Minecraft main menu with the Ray Client one. Takes effect the next time you go back to the title screen.");
			}));

		out.add(feature("f_loading", "Custom Loading Screen", "Client", "splash mojang startup boot",
			() -> RayConfig.get().customLoading, () -> { RayConfig c = RayConfig.get(); c.customLoading = !c.customLoading; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Loading screen");
				ui.toggle(f.full(), "Ray Client Loading Screen", c.customLoading, () -> c.customLoading = !c.customLoading);
				f.note("Replaces the red Mojang Studios loading screen with the dark Ray Client one. Takes effect on the next launch.");
			}));

		out.add(feature("f_sprint", "Toggle Sprint", "Gameplay", "sprint run hold",
			() -> RayConfig.get().toggleSprint, () -> { RayConfig c = RayConfig.get(); c.toggleSprint = !c.toggleSprint; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Toggle Sprint");
				ui.toggle(f.full(), "Enabled", c.toggleSprint, () -> c.toggleSprint = !c.toggleSprint);
				f.note("Hold forward and you keep sprinting, without holding the sprint key.");
			}));

		out.add(feature("f_sneak", "Toggle Sneak", "Gameplay", "sneak crouch hold",
			() -> RayConfig.get().toggleSneak, () -> { RayConfig c = RayConfig.get(); c.toggleSneak = !c.toggleSneak; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Toggle Sneak");
				ui.toggle(f.full(), "Enabled", c.toggleSneak, () -> c.toggleSneak = !c.toggleSneak);
				f.note("Press sneak once to start sneaking, again to stop.");
			}));

		out.add(feature("f_zoom", "Zoom", "Gameplay", "fov spyglass optifine",
			() -> RayConfig.get().zoomEnabled, () -> { RayConfig c = RayConfig.get(); c.zoomEnabled = !c.zoomEnabled; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Zoom");
				ui.toggle(f.half(), "Enabled", c.zoomEnabled, () -> c.zoomEnabled = !c.zoomEnabled);
				ui.toggle(f.half(), "Smooth Zoom", c.smoothZoom, () -> c.smoothZoom = !c.smoothZoom);
				ui.slider(f.half(), "Zoom FOV", 10, 60, c.zoomFov, v -> c.zoomFov = v);
				ui.slider(f.half(), "FOV Effects", 0, 100, (int) Math.round(ui.mc.options.getFovEffectScale().getValue() * 100.0), "%",
					v -> ui.mc.options.getFovEffectScale().setValue(v / 100.0));
				f.note("Hold the Zoom key to zoom in (C by default, change it in Controls).");
			}));

		out.add(feature("f_fullbright", "Fullbright", "Visual", "night vision gamma bright dark",
			() -> RayConfig.get().fullbright, () -> { RayConfig c = RayConfig.get(); c.fullbright = !c.fullbright; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Fullbright");
				ui.toggle(f.full(), "Enabled", c.fullbright, () -> c.fullbright = !c.fullbright);
				f.note("Client-side night vision so caves and nights are fully lit.");
			}));

		out.add(feature("f_hurtcam", "No Hurt Cam", "Visual", "damage tilt shake screen",
			() -> RayConfig.get().noHurtCam, () -> { RayConfig c = RayConfig.get(); c.noHurtCam = !c.noHurtCam; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("No Hurt Cam");
				ui.toggle(f.full(), "Enabled", c.noHurtCam, () -> c.noHurtCam = !c.noHurtCam);
				f.note("Stops the screen tilting when you take damage.");
			}));

		out.add(feature("f_viewmodel", "Viewmodel", "Visual", "hand item position first person offset scale",
			() -> RayConfig.get().viewmodel, () -> { RayConfig c = RayConfig.get(); c.viewmodel = !c.viewmodel; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Viewmodel");
				ui.toggle(f.full(), "Enabled", c.viewmodel, () -> c.viewmodel = !c.viewmodel);
				ui.slider(f.half(), "Scale", 20, 150, c.vmScale, "%", v -> c.vmScale = v);
				ui.slider(f.half(), "X", -50, 50, c.vmX, v -> c.vmX = v);
				ui.slider(f.half(), "Y", -50, 50, c.vmY, v -> c.vmY = v);
				ui.slider(f.half(), "Z", -50, 50, c.vmZ, v -> c.vmZ = v);
				f.note("Moves and resizes the item in your hand. Visual only.");
			}));

		out.add(feature("f_swing", "Custom Swing", "Visual", "animation hand swing speed",
			() -> RayConfig.get().customSwing, () -> { RayConfig c = RayConfig.get(); c.customSwing = !c.customSwing; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Custom Swing");
				ui.toggle(f.half(), "Enabled", c.customSwing, () -> c.customSwing = !c.customSwing);
				ui.slider(f.half(), "Swing Duration", 1, 30, c.swingDuration, " ticks", v -> c.swingDuration = v);
				f.note("Changes how long your swing animation lasts. Visual only; attack timing is unchanged.");
			}));

		out.add(feature("f_hitmarker", "Hit Marker", "PvP", "hit indicator crosshair damage",
			() -> RayConfig.get().hitMarker, () -> { RayConfig c = RayConfig.get(); c.hitMarker = !c.hitMarker; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Hit Marker");
				ui.toggle(f.full(), "Enabled", c.hitMarker, () -> c.hitMarker = !c.hitMarker);
				f.note("Flashes a small marker around the crosshair when you hit something.");
			}));

		out.add(feature("f_hitsound", "Hit Sound", "PvP", "sound audio hit",
			() -> RayConfig.get().hitSound, () -> { RayConfig c = RayConfig.get(); c.hitSound = !c.hitSound; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Hit Sound");
				ui.toggle(f.full(), "Enabled", c.hitSound, () -> c.hitSound = !c.hitSound);
				f.note("Plays a short sound when you land a hit.");
			}));

		out.add(feature("f_lowhealth", "Low Health Warning", "Gameplay", "red pulse screen hearts",
			() -> RayConfig.get().lowHealthWarning, () -> { RayConfig c = RayConfig.get(); c.lowHealthWarning = !c.lowHealthWarning; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Low Health Warning");
				ui.toggle(f.full(), "Enabled", c.lowHealthWarning, () -> c.lowHealthWarning = !c.lowHealthWarning);
				f.note("Pulses a red border around the screen when you are at 3 hearts or less.");
			}));

		out.add(feature("f_durability", "Durability Alert", "Gameplay", "tool armor break low warning",
			() -> RayConfig.get().durabilityAlert, () -> { RayConfig c = RayConfig.get(); c.durabilityAlert = !c.durabilityAlert; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Durability Alert");
				ui.toggle(f.full(), "Enabled", c.durabilityAlert, () -> c.durabilityAlert = !c.durabilityAlert);
				f.note("Shows a message when your armor or held tool is below 10% durability.");
			}));

		out.add(feature("f_respawn", "Auto Respawn", "Gameplay", "death screen respawn",
			() -> RayConfig.get().autoRespawn, () -> { RayConfig c = RayConfig.get(); c.autoRespawn = !c.autoRespawn; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Auto Respawn");
				ui.toggle(f.full(), "Enabled", c.autoRespawn, () -> c.autoRespawn = !c.autoRespawn);
				f.note("Skips the death screen and respawns immediately.");
			}));

		out.add(feature("f_scoreboard", "Hide Scoreboard", "Visual", "sidebar server scoreboard",
			() -> RayConfig.get().hideScoreboard, () -> { RayConfig c = RayConfig.get(); c.hideScoreboard = !c.hideScoreboard; },
			(s, ui, f) -> {
				RayConfig c = RayConfig.get();
				f.section("Scoreboard");
				ui.toggle(f.half(), "Hide Scoreboard", c.hideScoreboard, () -> c.hideScoreboard = !c.hideScoreboard);
				ui.slider(f.half(), "Scale", 50, 150, c.scoreboardScale, "%", v -> c.scoreboardScale = v);
				f.note("To move the scoreboard, enable the Scoreboard module and drag it in the HUD editor.");
			}));

		out.add(feature("f_crosshair", "Custom Crosshair", "Visual", "aim reticle dot draw editor blending",
			() -> RayConfig.get().customCrosshair, () -> { RayConfig c = RayConfig.get(); c.customCrosshair = !c.customCrosshair; },
			Features::crosshairRows));

		out.add(feature("f_outline", "Block Outline", "Visual", "highlight selection target block box",
			() -> RayConfig.get().blockOutline, () -> { RayConfig c = RayConfig.get(); c.blockOutline = !c.blockOutline; },
			Features::outlineRows));

		out.sort(Comparator.<Card>comparingInt(c -> categoryIndex(c.category)).thenComparing(c -> c.name.toLowerCase()));
		return out;
	}

	public static int categoryIndex(String category) {
		for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equals(category)) return i;
		return CATEGORIES.length;
	}

	// ---------- bigger settings pages ----------

	private static final String[] TARGETS = {"Players", "Living", "Any"};

	private static void crosshairRows(SettingsScreen s, Ui ui, Ui.Form f) {
		RayConfig c = RayConfig.get();
		f.section("Crosshair");
		ui.toggle(f.half(), "Custom Crosshair", c.customCrosshair, () -> c.customCrosshair = !c.customCrosshair);
		ui.toggle(f.half(), "Use Drawn Crosshair", c.crosshairDrawn, () -> c.crosshairDrawn = !c.crosshairDrawn);
		ui.toggle(f.half(), "Vanilla Blending", c.crosshairVanillaBlend, () -> c.crosshairVanillaBlend = !c.crosshairVanillaBlend);
		ui.toggle(f.half(), "Outline", c.crosshairOutline, () -> c.crosshairOutline = !c.crosshairOutline);
		ui.colorChoice(f.half(), "Color", CrosshairArt.COLOR_NAMES[c.crosshairColor], CrosshairArt.color(c.crosshairColor),
			() -> c.crosshairColor = (c.crosshairColor + 1) % CrosshairArt.colorCount());
		ui.slider(f.half(), "Pixel Scale", 1, 4, c.crosshairScale, "x", v -> c.crosshairScale = v);
		f.section("Classic crosshair");
		ui.slider(f.half(), "Size", 2, 20, c.crosshairSize, v -> c.crosshairSize = v);
		ui.slider(f.half(), "Gap", 0, 10, c.crosshairGap, v -> c.crosshairGap = v);
		ui.slider(f.half(), "Thickness", 1, 3, c.crosshairThickness, v -> c.crosshairThickness = v);
		ui.toggle(f.half(), "Center Dot", c.crosshairDot, () -> c.crosshairDot = !c.crosshairDot);
		f.section("Enemy crosshair");
		ui.toggle(f.half(), "Enemy Crosshair", c.enemyCrosshair, () -> c.enemyCrosshair = !c.enemyCrosshair);
		ui.choice(f.half(), "Enemy Target", TARGETS[c.enemyTarget], () -> c.enemyTarget = (c.enemyTarget + 1) % TARGETS.length);
		f.section("Draw your own");
		ui.button(f.full(), "Open Crosshair Editor", () -> s.open(new CrosshairEditorScreen(s)));
		f.note("Vanilla Blending draws with the same colour-inverting blend as the normal crosshair, so it stays visible on any background (your colour on dark areas, its opposite on bright ones). The outline is skipped while it is on.");
	}

	private static void outlineRows(SettingsScreen s, Ui ui, Ui.Form f) {
		RayConfig c = RayConfig.get();
		f.section("Block Outline");
		ui.toggle(f.half(), "Block Outline", c.blockOutline, () -> c.blockOutline = !c.blockOutline);
		ui.toggle(f.half(), "Stabilize View", c.outlineStabilize, () -> c.outlineStabilize = !c.outlineStabilize);
		ui.colorChoice(f.half(), "Color", c.outlineColor == 0 ? "Accent" : Theme.PALETTE_NAMES[c.outlineColor],
			Theme.pick(c.outlineColor, Theme.accent()), () -> c.outlineColor = (c.outlineColor + 1) % Theme.PALETTE_NAMES.length);
		ui.toggle(f.half(), "Rainbow", c.outlineRainbow, () -> c.outlineRainbow = !c.outlineRainbow);
		ui.toggle(f.half(), "Glow", c.outlineGlow, () -> c.outlineGlow = !c.outlineGlow);
		ui.slider(f.half(), "Thickness", 1, 4, c.outlineThickness, v -> c.outlineThickness = v);
		ui.slider(f.half(), "Opacity", 20, 100, c.outlineOpacity, "%", v -> c.outlineOpacity = v);
		f.note("Outlines the exact shape of the block you are looking at (stairs, slabs and fences included).");
		f.note("Stabilize View turns off view bobbing and FOV effects while the outline is on, so the lines sit perfectly on the block. Your own settings are restored when you turn the outline off.");
	}
}
