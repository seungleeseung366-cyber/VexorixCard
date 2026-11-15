package com.vexorix.card;

import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Mode Party: 2-5 peserta (manusia + bot). Tiap ronde semua memilih 1 kartu dari 5 kartu miliknya,
 * kartu dinilai menurut tema ronde, lalu skor dijumlahkan. Pemenang ditentukan SKOR, bukan menyerang base.
 * Semua hasil deterministik dari seed + daftar pilihan, jadi tiap HP menghitung hasil yang sama.
 */
public class Party {
    public static final int HAND = 5;
    public static final String[] THEME = {
        "ATK tertinggi", "HP tertinggi", "Total kekuatan (ATK+HP)", "Paling efisien (ATK+HP-Biaya)", "Efek terkuat"
    };

    public final long seed;
    public final int n, rounds;
    public final String[] names;
    public final boolean[] bot;
    public final CardDef[][] hand;
    public final boolean[][] used;
    public final int[] themeOrder = { 0, 1, 2, 3, 4 };

    public int round = 0;
    public int[] score, tie;
    public int[] lastPick, lastMetric, lastPoints;
    public int lastTheme = 0;
    public boolean finished = false;

    public Party(Array<CardDef> all, long seed, String[] names, boolean[] bot, int rounds) {
        this.seed = seed;
        this.n = names.length;
        this.names = names;
        this.bot = bot;
        this.rounds = Math.max(1, Math.min(HAND, rounds));
        hand = new CardDef[n][HAND];
        used = new boolean[n][HAND];
        score = new int[n];
        tie = new int[n];
        lastPick = new int[n];
        lastMetric = new int[n];
        lastPoints = new int[n];
        for (int p = 0; p < n; p++) {
            Random r = new Random(seed * 31 + p * 7919L);
            for (int k = 0; k < HAND; k++) hand[p][k] = all.get(r.nextInt(all.size));
        }
        Random tr = new Random(seed * 17 + 5);
        for (int i = themeOrder.length - 1; i > 0; i--) {
            int j = tr.nextInt(i + 1);
            int t = themeOrder[i];
            themeOrder[i] = themeOrder[j];
            themeOrder[j] = t;
        }
    }

    public int theme() { return themeOrder[round % themeOrder.length]; }

    public String themeName() { return THEME[theme()]; }

    public static int metric(CardDef d, int theme) {
        switch (theme) {
            case 0: return d.attack;
            case 1: return d.hp;
            case 2: return d.attack + d.hp;
            case 3: return d.attack + d.hp - d.cost;
            default: return d.effect.equals("none") ? 0 : d.value;
        }
    }

    /** Pilihan bot (deterministik dari seed + ronde). */
    public int botPick(int p) {
        Random r = new Random(seed * 131 + round * 977L + p * 13L);
        int best = -1;
        float bestV = -1e9f;
        for (int k = 0; k < HAND; k++) {
            if (used[p][k]) continue;
            float v = metric(hand[p][k], theme()) + r.nextFloat() * 1.5f;
            if (v > bestV) { bestV = v; best = k; }
        }
        return best;
    }

    /** picks[p] = nomor kartu (0-4) atau -1 jika tidak memilih. */
    public void resolve(int[] picks) {
        if (finished) return;
        int th = theme();
        lastTheme = th;
        for (int p = 0; p < n; p++) {
            int k = p < picks.length ? picks[p] : -1;
            if (k < 0 || k >= HAND || used[p][k]) {
                lastPick[p] = -1;
                lastMetric[p] = 0;
            } else {
                used[p][k] = true;
                lastPick[p] = k;
                lastMetric[p] = metric(hand[p][k], th);
            }
        }
        for (int p = 0; p < n; p++) {
            if (lastPick[p] < 0) { lastPoints[p] = 0; continue; }
            int higher = 0;
            for (int q = 0; q < n; q++) {
                if (q != p && lastPick[q] >= 0 && lastMetric[q] > lastMetric[p]) higher++;
            }
            lastPoints[p] = n - higher;
            score[p] += lastPoints[p];
            CardDef d = hand[p][lastPick[p]];
            tie[p] += d.attack + d.hp;
        }
        round++;
        if (round >= rounds) finished = true;
    }

    /** Urutan peringkat: skor tertinggi dulu, lalu total kekuatan kartu yang dipakai. */
    public int[] ranking() {
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                if (score[a] != score[b]) return score[b] - score[a];
                if (tie[a] != tie[b]) return tie[b] - tie[a];
                return a - b;
            }
        });
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = idx[i];
        return out;
    }

    /** Semua peserta dengan skor (dan tie-break) paling rendah. */
    public boolean[] losers() {
        int ms = Integer.MAX_VALUE, mt = Integer.MAX_VALUE;
        for (int p = 0; p < n; p++) {
            if (score[p] < ms || (score[p] == ms && tie[p] < mt)) { ms = score[p]; mt = tie[p]; }
        }
        boolean[] l = new boolean[n];
        for (int p = 0; p < n; p++) l[p] = score[p] == ms && tie[p] == mt;
        return l;
    }

    /** Semua peserta dengan skor (dan tie-break) paling tinggi. */
    public boolean[] winners() {
        int ms = Integer.MIN_VALUE, mt = Integer.MIN_VALUE;
        for (int p = 0; p < n; p++) {
            if (score[p] > ms || (score[p] == ms && tie[p] > mt)) { ms = score[p]; mt = tie[p]; }
        }
        boolean[] w = new boolean[n];
        for (int p = 0; p < n; p++) w[p] = score[p] == ms && tie[p] == mt;
        return w;
    }
}
