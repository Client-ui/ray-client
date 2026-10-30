package dev.ray;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ray Client menu: every module and feature in ONE searchable grid (filter chips on top, favourites,
 * an enable button and a settings button on each card), plus Profiles, Waypoints and Theme pages.
 */
public class RayScreen extends Screen {
	private static final String[] PAGES = {"MODS", "SETTINGS", "PROFILES", "WAYPOINTS"};
	private static final String[] CHIPS = {"ALL", "FAV", "STATS", "WORLD", "PLAYER", "PVP", "EXTRAS", "VISUAL", "GAMEPLAY"};
	private static final int CARD_H = 60, GAP = 6, HEADER_H = 34;

	private static int page = 0;
	private static String filter = "ALL";

	public static void selectPage(String name) {
		for (int i = 0; i < PAGES.length; i++) if (PAGES[i].equalsIgnoreCase(name)) page = i;
	}

	public static void selectFilter(String name) {
		filter = name.toUpperCase();
	}

	private final Screen parent;
	private final Ui ui = new Ui();
	private TextFieldWidget search;
	private String query = "";
	private float scrollCur = 0f;
	private int scrollTarget = 0, scrollMax = 0;
	private List<String> profileNames = new ArrayList<>();
	private long profilesTime = 0;
	private boolean profilesDirty = true;
	// search pill bounds from the last frame (for focus handling)
	private int sx, sy, sw, sh;

	public RayScreen(Screen parent) {
		super(Text.literal("Ray Client"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ui.reset();
		search = new TextFieldWidget(textRenderer, 0, 0, 90, 10, Text.literal("Search"));
		search.setDrawsBackground(false);
		search.setMaxLength(32);
		search.setPlaceholder(Text.literal("Search modules..."));
		search.setText(query);
		search.setChangedListener(s -> {
			query = s;
			scrollTarget = 0;
			scrollCur = 0f;
		});
		addSelectableChild(search);
		if (page == 0) setInitialFocus(search);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scrollTarget = Math.max(0, Math.min(scrollMax, scrollTarget - (int) (verticalAmount * 28)));
		return true;
	}

	private void setPage(int p) {
		page = p;
		scrollTarget = 0;
		scrollCur = 0f;
	}

	private void setFilter(String f) {
		filter = f;
		scrollTarget = 0;
		scrollCur = 0f;
	}

	// ---------- render ----------

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		if (client.world == null) Brand.backdrop(ctx, width, height, System.currentTimeMillis(), 1.0f);
		ui.begin(ctx, textRenderer, mouseX, mouseY);
		int accent = Theme.accent();

		int pw = Math.min(540, width - 12);
		int ph = Math.min(312, height - 12);
		int px = (width - pw) / 2, py = (height - ph) / 2;
		ui.panel(px, py, pw, ph, 6, Theme.PANEL, Theme.fade(accent, 0.55f));
		ui.rect(px + 1, py + 1, pw - 2, 2, accent);

		drawHeader(px, py, pw);

		// ease the scroll position
		scrollCur += (scrollTarget - scrollCur) * 0.4f;
		if (Math.abs(scrollTarget - scrollCur) < 0.5f) scrollCur = scrollTarget;
		int off = Math.round(scrollCur);

		if (page == 0) {
			drawMods(ctx, px, py, pw, ph, off, delta);
		} else {
			if (getFocused() != null) setFocused(null);
			drawPage(px, py, pw, ph, off);
		}

		ui.poll();

		// focus the search box when it is clicked, release it when something else is
		if (page == 0 && ui.leftPressed) {
			boolean inside = mouseX >= sx && mouseX < sx + sw && mouseY >= sy && mouseY < sy + sh;
			if (inside) setFocused(search);
			else setFocused(null);
		}
	}

	private void drawHeader(int px, int py, int pw) {
		// logo lock-up
		Brand.bolt(ui.ctx, px + 17, py + 7, 1, 1.0f, System.currentTimeMillis());
		Brand.word(ui.ctx, "RAY", px + 32, py + 7, 2, 0xFFFFFFFF);
		Brand.word(ui.ctx, "CLIENT", px + 32, py + 24, 1, Theme.accent());

		// page tabs (padding shrinks and the editor shortcut shortens / hides when the window is narrow)
		int x = px + 88, y = py + 9;
		boolean inGame = client.world != null;
		int avail = pw - 88 - 36;
		int pad = 18, gap = 4;
		String editorLabel = "HUD EDITOR";
		for (int attempt = 0; attempt < 4; attempt++) {
			int tabs = 0;
			for (String p : PAGES) tabs += ui.w(p) + pad + gap;
			if (tabs + ui.w(editorLabel) + pad <= avail) break;
			if (attempt == 0) editorLabel = "EDITOR";
			else if (attempt == 1) { pad = 10; gap = 2; editorLabel = "HUD EDITOR"; }
			else if (attempt == 2) editorLabel = "EDITOR";
			else editorLabel = null;
		}
		for (int i = 0; i < PAGES.length; i++) {
			final int idx = i;
			String p = PAGES[i];
			int w = ui.w(p) + pad;
			boolean sel = page == i;
			boolean hov = ui.over(x, y, w, 18);
			ui.rrect(x, y, w, 18, 3, sel ? Theme.fade(Theme.accent(), 0.28f) : (hov ? Theme.CARD_HOVER : Theme.CARD));
			if (sel) ui.rect(x + 3, y + 16, w - 6, 2, Theme.accent());
			ui.text(p, x + pad / 2, y + 5, sel ? 0xFFFFFFFF : Theme.DIM);
			ui.click("page" + i, x, y, w, 18, () -> setPage(idx));
			x += w + gap;
		}
		// HUD editor shortcut (only inside a world)
		if (editorLabel != null) {
			int ew = ui.w(editorLabel) + pad;
			boolean ehov = inGame && ui.over(x, y, ew, 18);
			ui.rrect(x, y, ew, 18, 3, inGame ? Theme.mix(Theme.CARD, Theme.accent(), ehov ? 0.45f : 0.2f) : 0xFF11141C);
			ui.text(editorLabel, x + pad / 2, y + 5, inGame ? 0xFFFFFFFF : Theme.MUTE);
			if (inGame) ui.click("editor", x, y, ew, 18, () -> client.setScreen(new HudEditorScreen()));
		}

		// close
		int cx = px + pw - 26;
		boolean chov = ui.iconButton(cx, y, 18, 18, 0xFF3A1519, 0xFFB42B35, this::close);
		ui.closeIcon(cx + 6, y + 6, chov ? 0xFFFFFFFF : 0xFFFF9AA2);

		ui.rect(px + 8, py + HEADER_H, pw - 16, 1, Theme.STROKE);
	}

	// ---------- MODS page ----------

	private void drawMods(DrawContext ctx, int px, int py, int pw, int ph, int off, float delta) {
		int left = px + 12;
		int innerW = pw - 24;

		// search pill (right) and chips (left, wrapping)
		sw = Math.min(118, innerW / 3);
		sh = 16;
		sx = px + pw - 12 - sw;
		sy = py + HEADER_H + 7;
		ui.panel(sx, sy, sw, sh, 4, Theme.BG, ui.over(sx, sy, sw, sh) || search.isFocused() ? Theme.accent() : Theme.STROKE);
		search.setX(sx + 7);
		search.setY(sy + 4);
		search.setWidth(sw - 14);
		search.render(ctx, ui.mx, ui.my, 0f);

		int chipAreaW = innerW - sw - 8;
		int cx = left, cy = py + HEADER_H + 8, rows = 1;
		for (String chip : CHIPS) {
			int cw = ui.w(chip) + 12;
			if (cx + cw > left + chipAreaW && cx > left) {
				cx = left;
				cy += 16;
				rows++;
			}
			final String label = chip;
			cx += ui.chip(cx, cy, chip, filter.equals(chip), () -> setFilter(label)) + 3;
		}

		// filtered cards
		String q = query.trim().toLowerCase();
		List<Features.Card> shown = new ArrayList<>();
		int total = 0;
		for (Features.Card c : Features.cards()) {
			total++;
			if (filter.equals("FAV") && !c.favorite()) continue;
			if (!filter.equals("ALL") && !filter.equals("FAV") && !c.category.equalsIgnoreCase(filter)) continue;
			if (!q.isEmpty() && !c.keywords.contains(q)) continue;
			shown.add(c);
		}

		int gy = py + HEADER_H + 8 + rows * 16 + 4;
		int gh = py + ph - 22 - gy;
		int gw = innerW - 7;
		int cols = Math.max(2, Math.min(4, (gw + GAP) / (112 + GAP)));
		int cardW = (gw - (cols - 1) * GAP) / cols;
		int gridRows = (shown.size() + cols - 1) / cols;
		int contentH = gridRows * (CARD_H + GAP);
		scrollMax = Math.max(0, contentH - gh);
		scrollTarget = Math.max(0, Math.min(scrollMax, scrollTarget));
		off = Math.max(0, Math.min(scrollMax, off));

		ui.pushClip(left - 1, gy, gw + 2, gh);
		for (int i = 0; i < shown.size(); i++) {
			int x = left + (i % cols) * (cardW + GAP);
			int y = gy + (i / cols) * (CARD_H + GAP) - off;
			if (y + CARD_H < gy || y > gy + gh) continue;
			drawCard(shown.get(i), x, y, cardW, CARD_H);
		}
		if (shown.isEmpty()) {
			ui.center("Nothing matches.", left + gw / 2, gy + 24, Theme.DIM);
			ui.center(filter.equals("FAV") ? "Click the heart on a module to favourite it." : "Try a different search or filter.",
				left + gw / 2, gy + 38, Theme.MUTE);
		}
		ui.popClip();
		ui.scrollbar(px + pw - 11, gy, gh, gh, contentH, off);

		// footer
		int fy = py + ph - 15;
		String count = shown.size() + " of " + total + " modules";
		ui.text(count, left, fy, Theme.MUTE);
		String hint = "Toggle on the left   Gear = settings   Scroll for more";
		if (ui.w(count) + ui.w(hint) + 24 > innerW) hint = "Gear = settings";
		if (ui.w(count) + ui.w(hint) + 24 <= innerW) ui.right(hint, px + pw - 12, fy, Theme.MUTE);
	}

	private void drawCard(Features.Card c, int x, int y, int w, int h) {
		boolean hov = ui.over(x, y, w, h);
		boolean on = c.on.getAsBoolean();
		int accent = Theme.accent();
		ui.panel(x, y, w, h, 4, hov ? Theme.CARD_HOVER : Theme.CARD, on ? Theme.fade(accent, 0.6f) : Theme.STROKE);

		// icon tile
		ui.rrect(x + 6, y + 6, 22, 22, 3, 0xFF0D1018);
		ui.ctx.drawItem(c.icon(), x + 9, y + 9);

		// title + category
		ui.text(ui.fit(c.name, w - 33 - 18), x + 33, y + 8, Theme.TEXT);
		ui.text(c.category.toUpperCase(), x + 33, y + 19, on ? Theme.fade(accent, 0.9f) : Theme.MUTE);

		// favourite heart
		boolean fav = c.favorite();
		boolean hh = ui.over(x + w - 20, y + 4, 16, 14);
		ui.heart(x + w - 15, y + 8, fav ? 0xFFFF5C7A : (hh ? Theme.DIM : 0xFF3A425A));
		ui.hit("fav" + c.id, x + w - 20, y + 4, 16, 14, c::toggleFavorite, null, null);

		// enable toggle (wide) + settings gear (separate button on the right)
		int by = y + h - 22;
		int bx = x + 6, bw = w - 12 - 24;
		boolean eh = ui.over(bx, by, bw, 16);
		if (on) {
			ui.rrect(bx, by, bw, 16, 3, Theme.GOOD);
			ui.rrect(bx + 1, by + 1, bw - 2, 14, 2, eh ? 0xFF164B2A : 0xFF123D22);
			ui.center("ENABLED", bx + bw / 2, by + 4, Theme.GOOD);
		} else {
			ui.rrect(bx, by, bw, 16, 3, eh ? 0xFF3A4260 : Theme.STROKE);
			ui.rrect(bx + 1, by + 1, bw - 2, 14, 2, eh ? 0xFF1B2132 : 0xFF0F121B);
			ui.center("ENABLE", bx + bw / 2, by + 4, eh ? 0xFFFFFFFF : Theme.DIM);
		}
		ui.hit("en" + c.id, bx, by, bw, 16, () -> {
			c.flip.run();
			RayConfig.save();
		}, null, null);

		int gx = x + w - 26;
		boolean gh = ui.over(gx, by, 20, 16);
		ui.rrect(gx, by, 20, 16, 3, gh ? Theme.accent() : 0xFF232A40);
		ui.slidersIcon(gx + 5, by + 4, gh ? 0xFFFFFFFF : Theme.DIM);
		ui.hit("gear" + c.id, gx, by, 20, 16, () -> client.setScreen(c.settings.apply(this)), null, null);
	}

	// ---------- other pages ----------

	private void drawPage(int px, int py, int pw, int ph, int off) {
		RayConfig cfg = RayConfig.get();
		int vx = px + 12, vy = py + HEADER_H + 8, vw = pw - 28, vh = ph - HEADER_H - 8 - 22;
		ui.pushClip(vx - 2, vy, vw + 4, vh);
		Ui.Form f = new Ui.Form(ui, vx, vy - off, vw);

		switch (page) {
			case 1 -> settingsPage(f, cfg);
			case 2 -> profilesPage(f);
			case 3 -> waypointsPage(f, cfg);
			default -> {}
		}

		int contentH = f.bottom() - (vy - off);
		ui.popClip();
		scrollMax = Math.max(0, contentH - vh);
		scrollTarget = Math.max(0, Math.min(scrollMax, scrollTarget));
		ui.scrollbar(px + pw - 11, vy, vh, vh, contentH, off);
		ui.right("ESC to close", px + pw - 12, py + ph - 15, Theme.MUTE);
	}

	private void profilesPage(Ui.Form f) {
		f.section("Profiles");
		ui.button(f.full(), "+ Save current setup as new profile", () -> {
			Profiles.save(Profiles.nextName());
			profilesDirty = true;
		});
		if (profilesDirty || System.currentTimeMillis() - profilesTime > 1500L) {
			profileNames = Profiles.list();
			profilesTime = System.currentTimeMillis();
			profilesDirty = false;
		}
		if (profileNames.isEmpty()) {
			f.note("No profiles yet. Save one to switch between layouts and settings instantly.");
		}
		for (String pn : new ArrayList<>(profileNames)) {
			int[] r = f.full();
			ui.panel(r[0], r[1], r[2], Ui.H, 3, Theme.CARD, Theme.STROKE);
			ui.text(ui.fit(pn, r[2] - 130), r[0] + 8, r[1] + 8, Theme.TEXT);
			int bx = r[0] + r[2] - 118;
			ui.labelButton(bx, r[1] + 4, 38, 16, "Load", 0xFF1E3A8A, () -> Profiles.load(pn));
			ui.labelButton(bx + 42, r[1] + 4, 38, 16, "Save", 0xFF14532D, () -> Profiles.save(pn));
			ui.labelButton(bx + 84, r[1] + 4, 32, 16, "Del", 0xFF7F1D1D, () -> {
				Profiles.delete(pn);
				profilesDirty = true;
			});
		}
	}

	private void waypointsPage(Ui.Form f, RayConfig cfg) {
		f.section("Waypoints");
		ui.button(f.full(), "+ Add waypoint at my position", () -> RayClient.addWaypoint(client));
		ui.toggle(f.half(), "Show in World", cfg.waypointMarkers, () -> cfg.waypointMarkers = !cfg.waypointMarkers);
		ui.toggle(f.half(), "Show Distance", cfg.waypointShowDist, () -> cfg.waypointShowDist = !cfg.waypointShowDist);
		f.note("Waypoints show in the world as markers. Enable the Minimap module (World) to see them on a map too.");
		if (cfg.waypoints.isEmpty()) {
			f.note("No waypoints yet. Press B in-game to drop one where you stand.");
		}
		for (int i = 0; i < cfg.waypoints.size(); i++) {
			final int idx = i;
			RayConfig.Waypoint w = cfg.waypoints.get(i);
			int[] r = f.full();
			ui.panel(r[0], r[1], r[2], Ui.H, 3, Theme.CARD, Theme.STROKE);
			String dimName = w.dim.contains("nether") ? "Nether" : (w.dim.contains("end") ? "End" : "Overworld");
			String label = w.name + "   " + (int) w.x + "  " + (int) w.y + "  " + (int) w.z + "   " + dimName;
			ui.text(ui.fit(label, r[2] - 40), r[0] + 8, r[1] + 8, Theme.TEXT);
			int bx = r[0] + r[2] - 22;
			boolean hov = ui.iconButton(bx, r[1] + 4, 16, 16, 0xFF3A1519, 0xFFB42B35, () -> {
				if (idx < RayConfig.get().waypoints.size()) RayConfig.get().waypoints.remove(idx);
				RayConfig.save();
			});
			ui.closeIcon(bx + 4, r[1] + 8, hov ? 0xFFFFFFFF : 0xFFFF9AA2);
		}
	}

	/** General client settings (kept out of the module list on purpose) + appearance. */
	private void settingsPage(Ui.Form f, RayConfig cfg) {
		f.section("General");
		ui.toggle(f.half(), "Client HUD", cfg.enabled, () -> cfg.enabled = !cfg.enabled);
		ui.toggle(f.half(), "Title Screen", cfg.customTitle, () -> cfg.customTitle = !cfg.customTitle);
		ui.toggle(f.half(), "Loading Screen", cfg.customLoading, () -> cfg.customLoading = !cfg.customLoading);
		ui.toggle(f.half(), "Menu Background", cfg.customBackground, () -> cfg.customBackground = !cfg.customBackground);
		f.note("Client HUD is the master switch for every HUD module. The title screen, loading screen and menu background (singleplayer, multiplayer, options) apply the next time they show.");

		f.section("Zoom");
		ui.sliderShown(f.half(), "Zoom Strength", 15, 100, cfg.zoomLevel, Features.zoomText(cfg.zoomLevel), v -> cfg.zoomLevel = v);
		ui.toggle(f.half(), "Smooth Zoom", cfg.smoothZoom, () -> cfg.smoothZoom = !cfg.smoothZoom);

		f.section("Appearance");
		int top = f.reserve(30);
		for (int i = 0; i < Theme.COLORS.length; i++) {
			final int idx = i;
			int sx2 = f.x() + 2 + i * 34;
			int sy2 = top + 2;
			boolean sel = cfg.accent == i && !cfg.rainbow;
			boolean hov = ui.over(sx2, sy2, 26, 26);
			ui.rrect(sx2, sy2, 26, 26, 4, Theme.COLORS[i]);
			if (sel) ui.border(sx2 - 2, sy2 - 2, 30, 30, 0xFFFFFFFF);
			else if (hov) ui.border(sx2 - 1, sy2 - 1, 28, 28, 0x88FFFFFF);
			ui.click("sw" + i, sx2, sy2, 26, 26, () -> {
				cfg.accent = idx;
				cfg.rainbow = false;
			});
		}
		ui.toggle(f.half(), "Rainbow Accent", cfg.rainbow, () -> cfg.rainbow = !cfg.rainbow);
		ui.toggle(f.half(), "Text Shadow", cfg.textShadow, () -> cfg.textShadow = !cfg.textShadow);
		ui.slider(f.half(), "HUD Background", 0, 100, cfg.hudOpacity, "%", v -> cfg.hudOpacity = v);
		f.note("The accent colour is used across this menu, the title screen and your HUD.");
	}

	@Override
	public void close() {
		RayConfig.save();
		client.setScreen(parent);
	}
}
