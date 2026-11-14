package com.vexorix.card;

import com.badlogic.gdx.utils.Array;
import java.util.Random;

/** Aturan game. Deterministik: hasilnya hanya bergantung pada seed + urutan aksi. */
public class Battle {
    public static final int MAX_HAND = 5, MAX_BOARD = 5, MAX_HP = 30;

    public static class Unit {
        public CardDef def;
        public int atk, hp;
        public boolean canAttack;
    }

    public static class Side {
        public int hp = 20, mana, maxMana;
        public Array<CardDef> deck = new Array<>();
        public Array<CardDef> hand = new Array<>();
        public Array<Unit> board = new Array<>();
    }

    public Side[] s = { new Side(), new Side() };
    public int turn = 0, turnNo = 0;
    public int winner = -1; // -1 belum, 0/1 pemenang, 2 seri

    public Battle(Array<CardDef> all, long seed) {
        for (int i = 0; i < 2; i++) {
            Random r = new Random(seed * 31 + i * 7919L);
            for (int k = 0; k < 30; k++) s[i].deck.add(all.get(r.nextInt(all.size)));
            int n = (i == 0) ? 3 : 4;
            for (int k = 0; k < n; k++) draw(i);
        }
        startTurn(0);
    }

    void draw(int i) {
        Side p = s[i];
        if (p.deck.size == 0) { p.hp -= 1; check(); return; }
        CardDef c = p.deck.pop();
        if (p.hand.size < MAX_HAND) p.hand.add(c);
    }

    void startTurn(int t) {
        turn = t;
        turnNo++;
        Side p = s[t];
        p.maxMana = Math.min(10, p.maxMana + 1);
        p.mana = p.maxMana;
        for (int i = 0; i < p.board.size; i++) p.board.get(i).canAttack = true;
        draw(t);
    }

    void check() {
        boolean a = s[0].hp <= 0, b = s[1].hp <= 0;
        if (a && b) winner = 2;
        else if (a) winner = 1;
        else if (b) winner = 0;
    }

    /** Aksi: "P:idxTangan", "A:idxPapan:target(-1=hero)", "E". Aksi selalu milik pemain yang sedang giliran. */
    public boolean apply(String act) {
        if (winner >= 0) return false;
        try {
            String[] p = act.split(":");
            Side me = s[turn], op = s[1 - turn];
            if (p[0].equals("E")) {
                startTurn(1 - turn);
                return true;
            }
            if (p[0].equals("P")) {
                int i = Integer.parseInt(p[1]);
                if (i < 0 || i >= me.hand.size) return false;
                CardDef d = me.hand.get(i);
                if (d.cost > me.mana || me.board.size >= MAX_BOARD) return false;
                me.mana -= d.cost;
                me.hand.removeIndex(i);
                Unit u = new Unit();
                u.def = d; u.atk = d.attack; u.hp = d.hp; u.canAttack = false;
                me.board.add(u);
                if (d.effect.equals("dmg")) op.hp -= d.value;
                else if (d.effect.equals("heal")) me.hp = Math.min(MAX_HP, me.hp + d.value);
                else if (d.effect.equals("draw")) for (int k = 0; k < d.value; k++) draw(turn);
                check();
                return true;
            }
            if (p[0].equals("A")) {
                int ai = Integer.parseInt(p[1]);
                int t = Integer.parseInt(p[2]);
                if (ai < 0 || ai >= me.board.size) return false;
                Unit u = me.board.get(ai);
                if (!u.canAttack) return false;
                if (t < 0) {
                    op.hp -= u.atk;
                } else {
                    if (t >= op.board.size) return false;
                    Unit e = op.board.get(t);
                    e.hp -= u.atk;
                    u.hp -= e.atk;
                    if (e.hp <= 0) op.board.removeIndex(t);
                    if (u.hp <= 0) me.board.removeIndex(ai);
                }
                u.canAttack = false;
                check();
                return true;
            }
        } catch (Exception ex) {
            // aksi rusak -> abaikan
        }
        return false;
    }
}
