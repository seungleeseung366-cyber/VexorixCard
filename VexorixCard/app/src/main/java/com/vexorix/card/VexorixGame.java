package com.vexorix.card;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

    enum Mode { MENU, LOBBY, GAME }
    Mode mode = Mode.MENU;

    SpriteBatch batch;
    ShapeRenderer sr;
    BitmapFont font;
    GlyphLayout gl = new GlyphLayout();
    OrthographicCamera cam;
    FitViewport vp;

    Array<CardDef> cards = new Array<>();
    HashMap<String, Texture> art = new HashMap<>();
    String error = null, msg = "", roomCode = "";
    float msgT = 0;

    Battle b;
    int me = 0;
    boolean online = false;
    int applied = 0;
    float pollT = 0, aiT = 0;
    boolean polling = false, lobbyBusy = false;
    long lobbySeed;
    int selected = -1;
    Vector2 tap = new Vector2();

    static class T { String s; float x, y, w, sc; Color c; }
    static class Im { Texture t; float x, y, w, h; }
    Array<T> texts = new Array<>();
    Array<Im> imgs = new Array<>();

    Rectangle btnAi = new Rectangle(160, 760, 400, 90);
    Rectangle btnCreate = new Rectangle(160, 640, 400, 90);
    Rectangle btnJoin = new Rectangle(160, 520, 400, 90);
    Rectangle btnEnd = new Rectangle(250, 640, 220, 70);
    Rectangle btnMenu = new Rectangle(20, 20, 160, 60);
    Rectangle enemyHero = new Rectangle(110, 1185, 500, 80);
    Rectangle myHero = new Rectangle(110, 250, 500, 80);

    @Override
    public void create() {
        batch = new SpriteBatch();
        sr = new ShapeRenderer();
        font = new BitmapFont();
        cam = new OrthographicCamera();
        vp = new FitViewport(W, H, cam);
        try {
            cards = Cards.load();
            if (cards.size == 0) error = "cards.json kosong";
            JsonValue c = new JsonReader().parse(Gdx.files.internal("config.json"));
            Room.base = c.getString("firebaseUrl", "").trim().replaceAll("/+$", "");
        } catch (Exception e) {
            error = "Gagal membaca file di assets:\n" + e.getMessage();
        }
    }

    @Override
    public void resize(int w, int h) { vp.update(w, h, true); }

    // ---------- helpers ----------
    float slotX(int i) { return 7 + i * 144; }

    void toast(String s) { msg = s; msgT = 2.5f; }

    void label(String s, float x, float y, float w, float sc, Color c) {
        T t = new T();
        t.s = s; t.x = x; t.y = y; t.w = w; t.sc = sc; t.c = c;
        texts.add(t);
    }

    void button(Rectangle r, String s, Color c) { button(r, s, c, false, 1.2f); }

    void button(Rectangle r, String s, Color c, boolean border, float sc) {
        if (border) {
            sr.setColor(Color.YELLOW);
            sr.rect(r.x - 4, r.y - 4, r.width + 8, r.height + 8);
        }
        sr.setColor(c);
        sr.rect(r.x, r.y, r.width, r.height);
        label(s, r.x, r.y + r.height / 2 + 8 * sc, r.width, sc, Color.WHITE);
    }

    Texture art(CardDef d) {
        if (d.image == null || d.image.isEmpty()) return null;
        if (art.containsKey(d.image)) return art.get(d.image);
        Texture t = null;
        try {
            FileHandle f = Gdx.files.internal(d.image);
            if (f.exists()) t = new Texture(f);
        } catch (Exception e) { }
        art.put(d.image, t);
        return t;
    }

    String effText(CardDef d) {
        if (d.effect.equals("dmg")) return "Saat dimainkan: " + d.value + " dmg ke hero lawan";
        if (d.effect.equals("heal")) return "Saat dimainkan: pulihkan " + d.value + " HP";
        if (d.effect.equals("draw")) return "Saat dimainkan: ambil " + d.value + " kartu";
        return "";
    }

    Color effColor(CardDef d) {
        if (d.effect.equals("dmg")) return Color.valueOf("7f1d1d");
        if (d.effect.equals("heal")) return Color.valueOf("14532d");
        if (d.effect.equals("draw")) return Color.valueOf("1e3a8a");
        return Color.valueOf("475569");
    }

    // ---------- loop ----------
    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        if (msgT > 0) msgT -= dt;
        update(dt);

        Gdx.gl.glClearColor(0.07f, 0.08f, 0.13f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        vp.apply();
        cam.update();
        sr.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        texts.clear();
        imgs.clear();
        sr.begin(ShapeRenderer.ShapeType.Filled);
        if (error != null) label(error, 40, 700, 640, 1.4f, Color.SALMON);
        else if (mode == Mode.MENU) drawMenu();
        else if (mode == Mode.LOBBY) drawLobby();
        else drawGame();
        if (msgT > 0) label(msg, 20, 200, 680, 1.3f, Color.YELLOW);
        sr.end();

        batch.begin();
        for (int i = 0; i < imgs.size; i++) {
            Im im = imgs.get(i);
            batch.draw(im.t, im.x, im.y, im.w, im.h);
        }
        for (int i = 0; i < texts.size; i++) {
            T t = texts.get(i);
            font.getData().setScale(t.sc);
            gl.setText(font, t.s, t.c, t.w, Align.center, true);
            font.draw(batch, gl, t.x, t.y);
        }
        batch.end();
    }

    void update(float dt) {
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

    // ---------- drawing ----------
    void drawMenu() {
        label("VEXORIX CARD", 0, 1050, W, 3f, Color.GOLD);
        label("Battle kartu buatanmu sendiri", 0, 980, W, 1.4f, Color.LIGHT_GRAY);
        button(btnAi, "LAWAN AI", Color.valueOf("2d6a4f"), false, 1.8f);
        button(btnCreate, "BUAT ROOM", Color.valueOf("1d4ed8"), false, 1.8f);
        button(btnJoin, "GABUNG ROOM", Color.valueOf("7c3aed"), false, 1.8f);
        label(cards.size + " kartu dimuat dari cards.json", 0, 400, W, 1.2f, Color.GRAY);
    }

    void drawLobby() {
        label("KODE ROOM", 0, 900, W, 1.8f, Color.LIGHT_GRAY);
        label(roomCode, 0, 820, W, 4f, Color.GOLD);
        label("Bagikan kode ini ke temanmu.\nMenunggu lawan bergabung...", 40, 640, 640, 1.4f, Color.WHITE);
        button(btnMenu, "BATAL", Color.valueOf("475569"));
    }

    void drawGame() {
        Battle.Side en = b.s[1 - me], my = b.s[me];

        button(enemyHero, "LAWAN   HP " + en.hp + "   Kartu " + en.hand.size + "   Deck " + en.deck.size,
                Color.valueOf("7f1d1d"), selected >= 0, 1.2f);
        button(myHero, "KAMU   HP " + my.hp + "   Mana " + my.mana + "/" + my.maxMana,
                Color.valueOf("14532d"), false, 1.2f);

        for (int i = 0; i < en.board.size; i++) {
            Battle.Unit u = en.board.get(i);
            card(u.def, u.atk, u.hp, slotX(i), ROW_EN, selected >= 0, false);
        }
        boolean myTurn = b.turn == me && b.winner < 0;
        for (int i = 0; i < my.board.size; i++) {
            Battle.Unit u = my.board.get(i);
            card(u.def, u.atk, u.hp, slotX(i), ROW_MY, i == selected, myTurn && u.canAttack);
        }
        for (int i = 0; i < my.hand.size; i++) {
            CardDef d = my.hand.get(i);
            card(d, d.attack, d.hp, slotX(i), ROW_HAND, false,
                    myTurn && d.cost <= my.mana && my.board.size < Battle.MAX_BOARD);
        }

        label(myTurn ? "Giliran kamu (ronde " + b.turnNo + ")" : "Giliran lawan...", 0, 945, W, 1.2f, Color.LIGHT_GRAY);
        button(btnEnd, b.turn == me ? "AKHIRI GILIRAN" : "Menunggu...",
                b.turn == me ? Color.valueOf("b45309") : Color.valueOf("475569"));
        button(btnMenu, "MENU", Color.valueOf("475569"));

        if (b.winner >= 0) {
            String t = b.winner == 2 ? "SERI!" : (b.winner == me ? "MENANG!" : "KALAH");
            sr.setColor(0, 0, 0, 1);
            sr.rect(60, 560, 600, 160);
            label(t, 60, 690, 600, 3f, Color.GOLD);
            label("Ketuk untuk kembali ke menu", 60, 610, 600, 1.2f, Color.WHITE);
        }
    }

    void card(CardDef d, int atk, int hp, float x, float y, boolean sel, boolean ready) {
        if (sel) { sr.setColor(Color.YELLOW); sr.rect(x - 4, y - 4, CW + 8, CH + 8); }
        else if (ready) { sr.setColor(Color.GREEN); sr.rect(x - 3, y - 3, CW + 6, CH + 6); }
        sr.setColor(Color.valueOf("1e293b"));
        sr.rect(x, y, CW, CH);

        Texture t = art(d);
        if (t != null) {
            Im im = new Im();
            im.t = t; im.x = x + 8; im.y = y + 68; im.w = 114; im.h = 70;
            imgs.add(im);
        } else {
            sr.setColor(effColor(d));
            sr.rect(x + 8, y + 68, 114, 70);
        }
        sr.setColor(Color.valueOf("2563eb"));
        sr.circle(x + 17, y + CH - 17, 14);

        label("" + d.cost, x + 2, y + CH - 10, 30, 1.1f, Color.WHITE);
        label(d.name, x + 32, y + CH - 6, 94, 0.8f, Color.WHITE);
        label(effText(d), x + 6, y + 64, CW - 12, 0.7f, Color.LIGHT_GRAY);
        label("A" + atk, x + 4, y + 22, 50, 1.2f, Color.ORANGE);
        label("H" + hp, x + CW - 54, y + 22, 50, 1.2f, Color.SALMON);
    }

    // ---------- input ----------
    void backToMenu() {
        mode = Mode.MENU;
        online = false;
        roomCode = "";
        selected = -1;
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
        if (b.turn != me) return;

        Battle.Side my = b.s[me], en = b.s[1 - me];
        if (btnEnd.contains(tap)) { selected = -1; act("E"); return; }

        for (int i = 0; i < my.hand.size; i++) {
            if (new Rectangle(slotX(i), ROW_HAND, CW, CH).contains(tap)) {
                CardDef d = my.hand.get(i);
                if (d.cost > my.mana) toast("Mana tidak cukup");
                else if (my.board.size >= Battle.MAX_BOARD) toast("Papan penuh");
                else act("P:" + i);
                return;
            }
        }
        for (int i = 0; i < my.board.size; i++) {
            if (new Rectangle(slotX(i), ROW_MY, CW, CH).contains(tap)) {
                if (my.board.get(i).canAttack) selected = (selected == i) ? -1 : i;
                else toast("Kartu ini belum bisa menyerang");
                return;
            }
        }
        if (selected >= 0) {
            for (int j = 0; j < en.board.size; j++) {
                if (new Rectangle(slotX(j), ROW_EN, CW, CH).contains(tap)) {
                    int a = selected;
                    selected = -1;
                    act("A:" + a + ":" + j);
                    return;
                }
            }
            if (enemyHero.contains(tap)) {
                int a = selected;
                selected = -1;
                act("A:" + a + ":-1");
            }
        }
    }

    // ---------- game / room flow ----------
    void startGame(boolean on, int who, long seed) {
        online = on; me = who;
        b = new Battle(cards, seed);
        applied = 0; selected = -1; pollT = 0; aiT = 0;
        polling = false; lobbyBusy = false;
        mode = Mode.GAME;
    }

    void act(String a) {
        if (!b.apply(a)) { toast("Aksi tidak valid"); return; }
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
        font.dispose();
        for (Texture t : art.values()) if (t != null) t.dispose();
    }
}
