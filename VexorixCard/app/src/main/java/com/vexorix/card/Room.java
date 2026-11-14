package com.vexorix.card;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Multiplayer via Firebase Realtime Database (REST). */
public class Room {
    public interface Cb { void done(boolean ok, String body); }

    public static String base = "";
    static final String ALPHA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String newCode() {
        java.util.Random r = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(ALPHA.charAt(r.nextInt(ALPHA.length())));
        return sb.toString();
    }

    static void req(String method, String path, String body, final Cb cb) {
        Net.HttpRequest r = new Net.HttpRequest(method);
        r.setUrl(base + "/" + path + ".json");
        r.setTimeOut(8000);
        if (body != null) {
            r.setHeader("Content-Type", "application/json");
            r.setContent(body);
        }
        Gdx.net.sendHttpRequest(r, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse resp) {
                final int st = resp.getStatus().getStatusCode();
                final String t = resp.getResultAsString();
                Gdx.app.postRunnable(new Runnable() {
                    public void run() { cb.done(st >= 200 && st < 300, t); }
                });
            }
            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(new Runnable() {
                    public void run() { cb.done(false, ""); }
                });
            }
            @Override
            public void cancelled() { }
        });
    }

    public static void create(String code, long seed, Cb cb) {
        req("PUT", "rooms/" + code + "/meta", "{\"seed\":" + seed + ",\"joined\":false}", cb);
    }
    public static void getMeta(String code, Cb cb) { req("GET", "rooms/" + code + "/meta", null, cb); }
    public static void setJoined(String code, Cb cb) { req("PUT", "rooms/" + code + "/meta/joined", "true", cb); }
    public static void send(String code, int idx, String act, Cb cb) {
        req("PUT", "rooms/" + code + "/actions/" + idx, "\"" + act + "\"", cb);
    }
    public static void actions(String code, Cb cb) { req("GET", "rooms/" + code + "/actions", null, cb); }

    /** Ambil aksi ke-i dari JSON daftar aksi (array atau object), null jika belum ada. */
    public static String actionAt(String json, int i) {
        try {
            if (json == null || json.trim().equals("null")) return null;
            JsonValue root = new JsonReader().parse(json);
            if (root == null) return null;
            JsonValue v = root.isArray() ? root.get(i) : root.get(String.valueOf(i));
            if (v == null || v.isNull()) return null;
            return v.asString();
        } catch (Exception e) {
            return null;
        }
    }
}
