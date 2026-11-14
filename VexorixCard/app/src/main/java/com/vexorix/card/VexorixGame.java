package com.vexorix.card;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Random;

public class VexorixGame extends ApplicationAdapter {
    static final float W = 720, H = 1280, CW = 130, CH = 170;
    static final float ROW_EN = 960, ROW_MY = 730, ROW_HAND = 400;

    // ---- tema warna ----
    static final Color GOLD = Color.valueOf("e0b95c");
    static final Color GOLD_DK = Color.valueOf("7a5f22");
    static final Color INK = Color.valueOf("060913");
    static final Color NIGHT = Color.valueOf("1b1235");
    static final Color CARD_TOP = Color.valueOf("28325a");
    static final Color CARD_BOT = Color.valueOf("0b1024");
    static final Color TEXT = Color.valueOf("f4f0e6");
    static final Color MUTED = Color.valueOf("aaa6bd");
    static final Color TARGET = Color.valueOf("ff4d4d");
    static final Color READY = Color.valueOf("4ade80");
    static final Color GEM = Color.valueOf("2563eb");
    static final Color ATK_C = Color.valueOf("e8590c");
    static final Color HP_C = Color.valueOf("c2255c");
    static final Color SILVER = Color.valueOf("9aa6c0");
    static final Color PURPLE = Color.valueOf("c77dff");
    static final Color PANEL = new Color(0.02f, 0.03f, 0.08f, 0.55f);
    static final Color WATERMARK = new Color(1f, 1f, 1f, 0.035f);
    static final Color EMBER = new Color(0.9f, 0.72f, 0.35f, 0.3f);
    static final Color DIM = new Color(0f, 0f, 0f, 0.72f);

    enum Mode { MENU, LOBBY, GAME }
    Mode mode = Mode.MENU;

    SpriteBatch batch;
    ShapeRenderer sr;
    BitmapFont fallback;
    FreeTypeFontGenerator genBody, genTitle;
    HashMap<String, BitmapFont> fonts = new HashMap<>();
    GlyphLayout gl = new GlyphLayout();
    OrthographicCamera cam;
    FitViewport vp;

    Array<CardDef> cards = new Array<>();
    HashMap<String, Texture> art = new HashMap<>();
    String error = null, msg = "", roomCode = "";
    float msgT = 0, time = 0;

    Battle b;
    int me = 0;
    boolean online = false;
    int applied = 0;
    float pollT = 0, aiT = 0;
    boolean polling = false, lobbyBusy = false;
    long lobbySeed;
    int selected = -1, selHand = -1;
    CardDef insp = null;
    int inspAtk, inspHp;
    String inspNote = "";
    Vector2 tap = new Vector2();

    float[] ex = new float[30], ey = new float[30], es = new float[30], ez = new float[30];

    static class T { String s; float x, y, w; int px, align; boolean title; Color c; }
    static class Im { Texture t; float x, y, w, h; }
    Array<T> texts = new Array<>();
    Array<Im> imgs = new Array<>();
    Array<Runnable> over = new Array<>();

    Rectangle btnAi = new Rectangle(160, 760, 400, 90);
    Rectangle btnCreate = new Rectangle(160, 640, 400, 90);
    Rectangle btnJoin = new Rectangle(160, 520, 400, 90);
    Rectangle btnEnd = new Rectangle(230, 618, 260, 76);
    Rectangle btnMenu = new Rectangle(20, 20, 150, 56);
    Rectangle enemyHero = new Rectangle(80, 1180, 560, 84);
    Rectangle myHero = new Rectangle(80, 240, 560, 96);

    @Override
    public void create() {
        batch = new SpriteBatch();
        sr = new ShapeRenderer();
        fallback = new BitmapFont();
        cam = new OrthographicCamera();
        vp = new FitViewport(W, H, cam);
        try {
            genBody = new FreeTypeFontGenerator(Gdx.files.internal("body.ttf"));
            genTitle = new FreeTypeFontGenerator(Gdx.files.internal("title.ttf"));
        } catch (Throwable t) {
            genBody = null;
            genTitle = null;
        }
        try {
            cards = Cards.load();
            if (cards.size == 0) error = "cards.json kosong";
            JsonValue c = new JsonReader().parse(Gdx.files.internal("config.json"));
            Room.base = c.getString("firebaseUrl", "").trim().replaceAll("/+$", "");
        } catch (Exception e) {
            error = "Gagal membaca file di assets:\n" + e.getMessage();
        }
        Random r = new Random(7);
        for (int i = 0; i < ex.length; i++) {
            ex[i] = r.nextFloat() * W;
            ey[i] = r.nextFloat() * H;
            es[i] = 12 + r.nextFloat() * 28;
            ez[i] = 1.2f + r.nextFloat() * 2.6f;
        }
    }

    @Override
    public void resize(int w, int h) { vp.update(w, h, true); }

    // ---------- font ----------
    BitmapFont font(boolean title, int px) {
        FreeTypeFontGenerator g = title ? genTitle : genBody;
        if (g == null) {
            fallback.getData().setScale(px / 15f);
            return fallback;
        }
        String key = (title ? "t" : "b") + px;
        BitmapFont f = fonts.get(key);
        if (f == null) {
            try {
                FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                p.size = px;
                p.minFilter = Texture.TextureFilter.Linear;
                p.magFilter = Texture.TextureFilter.Linear;
                p.borderWidth = px >= 40 ? 3f : (px >= 20 ? 1.5f : 1f);
                p.borderColor = new Color(0f, 0f, 0f, 0.9f);
                p.characters = px >= 56 ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!? -" : FreeTypeFontGenerator.DEFAULT_CHARS;
                f = g.generateFont(p);
                fonts.put(key, f);
            } catch (Throwable e) {
                genBody = null;
                genTitle = null;
                fallback.getData().setScale(px / 15f);
                return fallback;
            }
        }
        return f;
    }

    // ---------- helper gambar ----------
    void toast(String s) { msg = s; msgT = 2.5f; }

    void label(String s, float x, float y, float w, int px, Color c, boolean title, int align) {
        T t = new T();
        t.s = s; t.x = x; t.y = y; t.w = w; t.px = px; t.c = c; t.title = title; t.align = align;
        texts.add(t);
    }

    void label(String s, float x, float y, float w, int px, Color c, boolean title) {
        label(s, x, y, w, px, c, title, Align.center);
    }

    /** Teks rata tengah di sekitar titik (cx, cy). */
    void labelC(String s, float cx, float cy, int px, Color c, boolean title) {
        label(s, cx - 250, cy + px * 0.36f, 500, px, c, title, Align.center);
    }

    void overlay(Runnable r) { over.add(r); }

    void addImg(Texture t, float x, float y, float w, float h) {
        Im im = new Im();
        im.t = t; im.x = x; im.y = y; im.w = w; im.h = h;
        imgs.add(im);
    }

    void grad(float x, float y, float w, float h, Color top, Color bot) {
        sr.rect(x, y, w, h, bot, bot, top, top);
    }

    void box(float x, float y, float w, float h, float t, Color border, Color top, Color bot) {
        sr.setColor(border);
        sr.rect(x - t, y - t, w + 2 * t, h + 2 * t);
        grad(x, y, w, h, top, bot);
    }

    void frame(float x, float y, float w, float h, float t, Color c) {
        sr.setColor(c);
        sr.rect(x, y, w, t);
        sr.rect(x, y + h - t, w, t);
        sr.rect(x, y, t, h);
        sr.rect(x + w - t, y, t, h);
    }

    void diamond(float cx, float cy, float r, Color c) {
        sr.setColor(c);
        sr.triangle(cx - r, cy, cx, cy + r, cx + r, cy);
        sr.triangle(cx - r, cy, cx, cy - r, cx + r, cy);
    }

    void button(Rectangle r, String s, Color base) { button(r, s, base, 30); }

    void button(Rectangle r, String s, Color base, int px) {
        sr.setColor(GOLD_DK);
        sr.rect(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
        grad(r.x, r.y, r.width, r.height, base.cpy().lerp(Color.WHITE, 0.18f), base.cpy().lerp(Color.BLACK, 0.4f));
        sr.setColor(1f, 1f, 1f, 0.12f);
        sr.rect(r.x, r.y + r.height - 3, r.width, 3);
        labelC(s, r.x + r.width / 2, r.y + r.height / 2, px, TEXT, true);
    }

    float slotX(int i) { return 7 + i * 144; }

    Texture art(CardDef d) {
        if (d.image == null || d.image.isEmpty()) return null;
        if (art.containsKey(d.image)) return art.get(d.image);
        Texture t = null;
        try {
            FileHandle f = Gdx.files.internal(d.image);
            if (f.exists()) {
                Pixmap src = new Pixmap(f);
                if (src.getWidth() >= 400) { // perkecil 2x supaya tidak bergerigi
                    Pixmap half = new Pixmap(src.getWidth() / 2, src.getHeight() / 2, src.getFormat());
                    half.setBlending(Pixmap.Blending.None);
                    half.setFilter(Pixmap.Filter.BiLinear);
                    half.drawPixmap(src, 0, 0, src.getWidth(), src.getHeight(), 0, 0, half.getWidth(), half.getHeight());
                    t = new Texture(half);
                    half.dispose();
                } else {
                    t = new Texture(src);
                }
                src.dispose();
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        } catch (Exception e) { }
        art.put(d.image, t);
        return t;
    }

    String effText(CardDef d) {
        if (d.effect.equals("dmg")) return "Saat dimainkan: beri " + d.value + " damage ke hero lawan.";
        if (d.effect.equals("heal")) return "Saat dimainkan: pulihkan " + d.value + " HP hero kamu.";
        if (d.effect.equals("draw")) return "Saat dimainkan: ambil " + d.value + " kartu.";
        return "Tidak ada efek khusus.";
    }

    String effTag(CardDef d) {
        if (d.effect.equals("dmg")) return "DMG " + d.value;
        if (d.effect.equals("heal")) return "HEAL " + d.value;
        if (d.effect.equals("draw")) return "DRAW " + d.value;
        return "";
    }

    Color effTagColor(CardDef d) {
        if (d.effect.equals("dmg")) return Color.valueOf("ff8a8a");
        if (d.effect.equals("heal")) return Color.valueOf("8bf0a8");
        return Color.valueOf("8cc8ff");
    }

    Color effColor(CardDef d) {
        if (d.effect.equals("dmg")) return Color.valueOf("7f1d1d");
        if (d.effect.equals("heal")) return Color.valueOf("14532d");
        if (d.effect.equals("draw")) return Color.valueOf("1e3a8a");
        return Color.valueOf("3b4256");
    }

    Color frameColor(CardDef d) {
        if (d.cost <= 2) return SILVER;
        if (d.cost <= 4) return GOLD;
        return PURPLE;
    }

    // ---------- loop ----------
    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        if (msgT > 0) msgT -= dt;
        update(dt);

        Gdx.gl.glClearColor(0.02f, 0.03f, 0.07f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        vp.apply();
        cam.update();
        sr.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        texts.clear();
        imgs.clear();
        over.clear();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        drawBackground();
        if (error != null) label(error, 40, 700, 640, 26, Color.SALMON, false);
        else if (mode == Mode.MENU) drawMenu();
        else if (mode == Mode.LOBBY) drawLobby();
        else drawGame();
        if (msgT > 0 && error == null) {
            box(60, 1138, 600, 40, 2, GOLD_DK, Color.valueOf("2a2140"), Color.valueOf("120d22"));
            labelC(msg, 360, 1158, 22, Color.YELLOW, false);
        }
        sr.end();

        batch.begin();
        for (int i = 0; i < imgs.size; i++) {
            Im im = imgs.get(i);
            batch.draw(im.t, im.x, im.y, im.w, im.h);
        }
        batch.end();

        if (over.size > 0) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < over.size; i++) over.get(i).run();
            sr.end();
        }

        batch.begin();
        for (int i = 0; i < texts.size; i++) {
            T t = texts.get(i);
            BitmapFont f = font(t.title, t.px);
            gl.setText(f, t.s, t.c, t.w, t.align, true);
            f.draw(batch, gl, t.x, t.y);
        }
        batch.end();
    }

    void update(float dt) {
        time += dt;
        for (int i = 0; i < ex.length; i++) {
            ey[i] += es[i] * dt;
            ex[i] += MathUtils.sin(time * 0.6f + i) * 6f * dt;
            if (ey[i] > H + 10) { ey[i] = -10; ex[i] = MathUtils.random(0f, W); }
        }
        if (error != null) return;
        if (Gdx.input.justTouched()) {
            tap.set(Gdx.input.getX(), Gdx.input.getY());
            vp.unproject(tap);
            onTap();
        }
        if (mode == Mode.LOBBY) {
            lobbyPoll(dt);
        } else if (mode == Mode.GAME && b != null && b.winner < 0) {
            if (!online) {
                if (b.turn == 1) {
                    aiT += dt;
                    if (aiT > 0.8f) { aiT = 0; b.apply(Ai.next(b, 1)); }
                }
            } else if (b.turn != me) {
                pollT += dt;
                if (pollT >= 1f && !polling) { pollT = 0; pollActions(); }
            }
        }
    }

    // ---------- tampilan ----------
    void drawBackground() {
        grad(0, 0, W, H, NIGHT, INK);
        diamond(360, 640, 340, WATERMARK);
        diamond(360, 640, 250, WATERMARK);
        for (int i = 0; i < ex.length; i++) {
            sr.setColor(EMBER);
            sr.circle(ex[i], ey[i], ez[i]);
        }
    }

    void drawMenu() {
        label("VEXORIX", 0, 1110, W, 96, GOLD, true);
        label("CARD", 0, 1010, W, 66, TEXT, true);
        sr.setColor(GOLD_DK);
        sr.rect(120, 928, 200, 2);
        sr.rect(400, 928, 200, 2);
        diamond(360, 929, 10, GOLD);
        label("Susun strategi, mainkan kartumu", 0, 900, W, 24, MUTED, false);
        button(btnAi, "LAWAN AI", Color.valueOf("2f6f4f"), 32);
        button(btnCreate, "BUAT ROOM", Color.valueOf("2b4fa3"), 32);
        button(btnJoin, "GABUNG ROOM", Color.valueOf("6a3fa8"), 32);
        label(cards.size + " kartu dimuat", 0, 440, W, 20, MUTED, false);
    }

    void drawLobby() {
        label("KODE ROOM", 0, 940, W, 30, MUTED, false);
        label(roomCode, 0, 880, W, 110, GOLD, true);
        label("Bagikan kode ini ke temanmu.\nMenunggu lawan bergabung...", 40, 690, 640, 28, TEXT, false);
        button(btnMenu, "BATAL", Color.valueOf("475569"), 22);
    }

    void zone(float y, float h) {
        sr.setColor(PANEL);
        sr.rect(0, y, W, h);
        sr.setColor(GOLD_DK);
        sr.rect(0, y, W, 2);
        sr.rect(0, y + h - 2, W, 2);
    }

    void hero(Rectangle r, String name, Battle.Side s, boolean mine, boolean target) {
        if (target) { sr.setColor(TARGET); sr.rect(r.x - 6, r.y - 6, r.width + 12, r.height + 12); }
        Color base = mine ? Color.valueOf("14532d") : Color.valueOf("7f1d1d");
        box(r.x, r.y, r.width, r.height, 3, GOLD_DK, base.cpy().lerp(Color.WHITE, 0.12f), base.cpy().lerp(Color.BLACK, 0.5f));
        labelC(name + "    HP " + s.hp, r.x + r.width / 2, r.y + r.height - 24, 30, TEXT, false);
        // bar HP
        float frac = MathUtils.clamp(s.hp / (float) Battle.MAX_HP, 0f, 1f);
        sr.setColor(0, 0, 0, 0.6f);
        sr.rect(r.x + 14, r.y + 8, r.width - 28, 10);
        grad(r.x + 14, r.y + 8, (r.width - 28) * frac, 10, Color.valueOf("ff6b6b"), Color.valueOf("c92a2a"));
        if (mine) {
            labelC("Mana " + s.mana + "/" + s.maxMana, r.x + 72, r.y + 42, 22, Color.valueOf("8cd8ff"), false);
            for (int i = 0; i < s.maxMana; i++) {
                diamond(r.x + 150 + i * 26, r.y + 42, 10, i < s.mana ? Color.valueOf("5ec8ff") : Color.valueOf("1f3a52"));
            }
        } else {
            labelC("Kartu " + s.hand.size + "     Deck " + s.deck.size, r.x + r.width / 2, r.y + 38, 22, MUTED, false);
        }
    }

    void drawGame() {
        Battle.Side en = b.s[1 - me], my = b.s[me];
        boolean myTurn = b.turn == me && b.winner < 0;

        zone(950, 190);
        zone(722, 186);
        zone(388, 194);

        hero(enemyHero, "LAWAN", en, false, selected >= 0 && myTurn);
        hero(myHero, "KAMU", my, true, false);

        for (int i = 0; i < en.board.size; i++) {
            Battle.Unit u = en.board.get(i);
            card(u.def, u.atk, u.hp, slotX(i), ROW_EN, false, false, selected >= 0 && myTurn);
        }
        for (int i = 0; i < my.board.size; i++) {
            Battle.Unit u = my.board.get(i);
            card(u.def, u.atk, u.hp, slotX(i), ROW_MY, i == selected, myTurn && u.canAttack, false);
        }
        for (int i = 0; i < my.hand.size; i++) {
            CardDef d = my.hand.get(i);
            card(d, d.attack, d.hp, slotX(i), ROW_HAND, i == selHand, myTurn && d.cost <= my.mana && my.board.size < Battle.MAX_BOARD, false);
        }

        if (b.winner < 0) {
            labelC(myTurn ? "GILIRAN KAMU  -  Ronde " + b.turnNo : "Giliran lawan...", 360, 929, 22, myTurn ? GOLD : MUTED, false);
        }
        if (b.turn == me) button(btnEnd, "AKHIRI GILIRAN", Color.valueOf("b45309"), 24);
        else button(btnEnd, "Menunggu...", Color.valueOf("475569"), 24);
        button(btnMenu, "MENU", Color.valueOf("475569"), 22);

        drawInspect();

        if (b.winner >= 0) {
            final String t = b.winner == 2 ? "SERI!" : (b.winner == me ? "MENANG!" : "KALAH");
            overlay(new Runnable() {
                public void run() {
                    sr.setColor(DIM);
                    sr.rect(0, 0, W, H);
                    box(60, 540, 600, 200, 3, GOLD, Color.valueOf("2a2140"), Color.valueOf("0c0818"));
                }
            });
            label(t, 60, 700, 600, 84, GOLD, true);
            label("Ketuk untuk kembali ke menu", 60, 610, 600, 24, TEXT, false);
        }
    }

    void drawInspect() {
        final float px0 = 20, py0 = 96, pw = 680, ph = 140;
        box(px0, py0, pw, ph, 3, GOLD_DK, Color.valueOf("231a3d"), Color.valueOf("0a0716"));
        if (insp == null) {
            labelC("Ketuk kartu untuk melihat detailnya", px0 + pw / 2, py0 + ph / 2, 24, MUTED, false);
            return;
        }
        final CardDef d = insp;
        final float ax = px0 + 14, ay = py0 + 24, aw = 150, ah = 92;
        Texture t = art(d);
        if (t != null) addImg(t, ax, ay, aw, ah);
        else {
            grad(ax, ay, aw, ah, effColor(d), CARD_BOT);
            labelC((d.name.isEmpty() ? "?" : d.name.substring(0, 1)), ax + aw / 2, ay + ah / 2, 48, new Color(1f, 1f, 1f, 0.4f), true);
        }
        final Color fr = frameColor(d);
        overlay(new Runnable() {
            public void run() { frame(ax - 2, ay - 2, aw + 4, ah + 4, 2, fr); }
        });
        label(d.name, 200, py0 + ph - 8, 490, 30, GOLD, true, Align.left);
        label("Biaya " + d.cost + "    ATK " + inspAtk + "    HP " + inspHp, 200, py0 + ph - 50, 490, 22, TEXT, false, Align.left);
        label(effText(d), 200, py0 + ph - 78, 490, 20, MUTED, false, Align.left);
        if (inspNote != null && !inspNote.isEmpty()) {
            label(inspNote, 200, py0 + 28, 490, 20, Color.YELLOW, false, Align.left);
        }
    }

    void card(CardDef d, int atk, int hp, float x, float y, boolean sel, boolean ready, boolean target) {
        final Color fr = frameColor(d);
        if (sel) { sr.setColor(Color.YELLOW); sr.rect(x - 7, y - 7, CW + 14, CH + 14); }
        else if (target) { sr.setColor(TARGET); sr.rect(x - 6, y - 6, CW + 12, CH + 12); }
        else if (ready) { sr.setColor(READY); sr.rect(x - 5, y - 5, CW + 10, CH + 10); }
        sr.setColor(fr);
        sr.rect(x - 2, y - 2, CW + 4, CH + 4);
        grad(x, y, CW, CH, CARD_TOP, CARD_BOT);
        sr.setColor(0, 0, 0, 0.35f);
        sr.rect(x, y + CH - 36, CW, 36);

        final float ax = x + 8, ay = y + 64;
        Texture t = art(d);
        if (t != null) addImg(t, ax, ay, 114, 70);
        else {
            grad(ax, ay, 114, 70, effColor(d), CARD_BOT);
            labelC((d.name.isEmpty() ? "?" : d.name.substring(0, 1)), ax + 57, ay + 35, 44, new Color(1f, 1f, 1f, 0.4f), true);
        }
        overlay(new Runnable() {
            public void run() { frame(ax - 1, ay - 1, 116, 72, 2, fr); }
        });

        diamond(x + 16, y + CH - 18, 17, fr);
        diamond(x + 16, y + CH - 18, 14, GEM);
        labelC("" + d.cost, x + 16, y + CH - 18, 20, Color.WHITE, false);
        label(d.name, x + 32, y + CH - 8, CW - 36, d.name.length() > 11 ? 13 : 15, TEXT, true);

        sr.setColor(0, 0, 0, 0.6f);
        sr.circle(x + 22, y + 28, 22);
        sr.circle(x + CW - 22, y + 28, 22);
        sr.setColor(ATK_C);
        sr.circle(x + 22, y + 28, 19);
        sr.setColor(HP_C);
        sr.circle(x + CW - 22, y + 28, 19);
        labelC("" + atk, x + 22, y + 28, 22, Color.WHITE, false);
        labelC("" + hp, x + CW - 22, y + 28, 22, hp < d.hp ? Color.valueOf("ffd43b") : Color.WHITE, false);
        String tag = effTag(d);
        if (!tag.isEmpty()) labelC(tag, x + CW / 2, y + 28, 13, effTagColor(d), false);
    }

    // ---------- input ----------
    void backToMenu() {
        mode = Mode.MENU;
        online = false;
        roomCode = "";
        selected = -1;
        selHand = -1;
        insp = null;
    }

    void inspect(CardDef d, int atk, int hp, String note) {
        insp = d; inspAtk = atk; inspHp = hp; inspNote = note;
    }

    void onTap() {
        if (mode == Mode.MENU) {
            if (btnAi.contains(tap)) startGame(false, 0, new Random().nextInt(1000000000));
            else if (btnCreate.contains(tap)) createRoom();
            else if (btnJoin.contains(tap)) askCode();
            return;
        }
        if (btnMenu.contains(tap)) { backToMenu(); return; }
        if (mode != Mode.GAME) return;
        if (b.winner >= 0) { backToMenu(); return; }

        boolean myTurn = b.turn == me;
        Battle.Side my = b.s[me], en = b.s[1 - me];
        if (myTurn && btnEnd.contains(tap)) { selected = -1; selHand = -1; act("E"); return; }

        for (int i = 0; i < my.hand.size; i++) {
            if (new Rectangle(slotX(i), ROW_HAND, CW, CH).contains(tap)) {
                CardDef d = my.hand.get(i);
                if (myTurn && selHand == i) { // ketuk kedua = mainkan
                    if (d.cost > my.mana) toast("Mana tidak cukup");
                    else if (my.board.size >= Battle.MAX_BOARD) toast("Papan penuh");
                    else { selHand = -1; act("P:" + i); }
                    return;
                }
                selHand = i;
                selected = -1;
                inspect(d, d.attack, d.hp, myTurn ? "Ketuk lagi untuk memainkan kartu ini" : "");
                return;
            }
        }
        for (int i = 0; i < my.board.size; i++) {
            if (new Rectangle(slotX(i), ROW_MY, CW, CH).contains(tap)) {
                Battle.Unit u = my.board.get(i);
                String note = "";
                if (myTurn) note = u.canAttack ? "Siap menyerang: pilih target di atas" : "Belum bisa menyerang";
                inspect(u.def, u.atk, u.hp, note);
                if (myTurn && u.canAttack) {
                    selected = (selected == i) ? -1 : i;
                    selHand = -1;
                }
                return;
            }
        }
        for (int j = 0; j < en.board.size; j++) {
            if (new Rectangle(slotX(j), ROW_EN, CW, CH).contains(tap)) {
                if (myTurn && selected >= 0) {
                    int a = selected;
                    selected = -1;
                    act("A:" + a + ":" + j);
                } else {
                    Battle.Unit u = en.board.get(j);
                    inspect(u.def, u.atk, u.hp, "");
                }
                return;
            }
        }
        if (myTurn && selected >= 0 && enemyHero.contains(tap)) {
            int a = selected;
            selected = -1;
            act("A:" + a + ":-1");
        }
    }

    // ---------- alur game / room ----------
    void startGame(boolean on, int who, long seed) {
        online = on; me = who;
        b = new Battle(cards, seed);
        applied = 0; selected = -1; selHand = -1; insp = null; pollT = 0; aiT = 0;
        polling = false; lobbyBusy = false;
        mode = Mode.GAME;
    }

    void act(String a) {
        if (!b.apply(a)) { toast("Aksi tidak valid"); return; }
        selHand = -1;
        if (online) send(applied, a, 0);
        applied++;
    }

    void send(final int idx, final String a, final int tries) {
        final String code = roomCode;
        Room.send(code, idx, a, new Room.Cb() {
            public void done(boolean ok, String body) {
                if (!ok && tries < 5 && mode == Mode.GAME && code.equals(roomCode)) send(idx, a, tries + 1);
            }
        });
    }

    void pollActions() {
        polling = true;
        final String code = roomCode;
        Room.actions(code, new Room.Cb() {
            public void done(boolean ok, String body) {
                polling = false;
                if (!ok || mode != Mode.GAME || !online || !code.equals(roomCode)) return;
                String a;
                while (b.winner < 0 && b.turn != me && (a = Room.actionAt(body, applied)) != null) {
                    b.apply(a);
                    applied++;
                }
            }
        });
    }

    boolean firebaseOk() {
        if (!Room.base.startsWith("https://")) {
            toast("Isi firebaseUrl di assets/config.json dulu");
            return false;
        }
        return true;
    }

    void createRoom() {
        if (!firebaseOk()) return;
        final String code = Room.newCode();
        final long seed = new Random().nextInt(1000000000);
        toast("Membuat room...");
        Room.create(code, seed, new Room.Cb() {
            public void done(boolean ok, String body) {
                if (!ok) { toast("Gagal membuat room. Cek URL & rules Firebase"); return; }
                roomCode = code; lobbySeed = seed;
                pollT = 0; lobbyBusy = false;
                mode = Mode.LOBBY;
            }
        });
    }

    void lobbyPoll(float dt) {
        pollT += dt;
        if (pollT < 1f || lobbyBusy) return;
        pollT = 0;
        lobbyBusy = true;
        final String code = roomCode;
        Room.getMeta(code, new Room.Cb() {
            public void done(boolean ok, String body) {
                lobbyBusy = false;
                if (mode != Mode.LOBBY || !code.equals(roomCode)) return;
                try {
                    if (ok && !body.trim().equals("null")) {
                        JsonValue j = new JsonReader().parse(body);
                        if (j.getBoolean("joined", false)) startGame(true, 0, lobbySeed);
                    }
                } catch (Exception e) { }
            }
        });
    }

    void askCode() {
        if (!firebaseOk()) return;
        Gdx.input.getTextInput(new Input.TextInputListener() {
            public void input(final String text) {
                Gdx.app.postRunnable(new Runnable() {
                    public void run() { joinRoom(text.trim().toUpperCase()); }
                });
            }
            public void canceled() { }
        }, "Gabung Room", "", "Kode room");
    }

    void joinRoom(final String code) {
        if (code.isEmpty()) return;
        toast("Mencari room...");
        Room.getMeta(code, new Room.Cb() {
            public void done(boolean ok, String body) {
                if (!ok) { toast("Gagal terhubung ke server"); return; }
                try {
                    if (body.trim().equals("null")) { toast("Room tidak ditemukan"); return; }
                    JsonValue j = new JsonReader().parse(body);
                    if (j.getBoolean("joined", false)) { toast("Room sudah penuh"); return; }
                    final long seed = j.getLong("seed");
                    Room.setJoined(code, new Room.Cb() {
                        public void done(boolean ok2, String body2) {
                            if (!ok2) { toast("Gagal bergabung"); return; }
                            roomCode = code;
                            startGame(true, 1, seed);
                        }
                    });
                } catch (Exception e) {
                    toast("Data room rusak");
                }
            }
        });
    }

    @Override
    public void dispose() {
        batch.dispose();
        sr.dispose();
        fallback.dispose();
        for (BitmapFont f : fonts.values()) f.dispose();
        if (genBody != null) genBody.dispose();
        if (genTitle != null) genTitle.dispose();
        for (Texture t : art.values()) if (t != null) t.dispose();
    }
}
