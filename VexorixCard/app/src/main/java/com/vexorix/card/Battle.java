package com.vexorix.card;

import com.badlogic.gdx.utils.Array;
import java.util.Random;

/** Aturan duel. Deterministik: hasilnya hanya bergantung pada seed + deck + urutan aksi. */
public class Battle {
    public static final int MAX_HAND = 5, MAX_BOARD = 5, MAX_HP = 30;

    public static class Unit {
        public CardDef def;
        public int atk, hp, skillCd;
        public boolean canAttack, ultUsed, frozen;
    }

    public static class Side {
        public int hp = 20, mana, maxMana, shield;
        public Array<CardDef> deck = new Array<>();
        public Array<CardDef> hand = new Array<>();
        public Array<Unit> board = new Array<>();
    }

    public Side[] s = { new Side(), new Side() };
    public int turn = 0, turnNo = 0;
    public int winner = -1; // -1 belum, 0/1 pemenang, 2 seri

    public Battle(Array<CardDef> all, long seed) { this(all, all, seed); }

    /** deckA = deck pemain 0, deckB = deck pemain 1. */
    public Battle(Array<CardDef> deckA, Array<CardDef> deckB, long seed) {
        for (int i = 0; i < 2; i++) {
            Array<CardDef> src = (i == 0) ? deckA : deckB;
            Random r = new Random(seed * 31 + i * 7919L);
            for (int k = 0; k < 30; k++) s[i].deck.add(src.get(r.nextInt(src.size)));
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
        for (int i = 0; i < p.board.size; i++) {
            Unit u = p.board.get(i);
            if (u.skillCd > 0) u.skillCd--;
            u.canAttack = !u.frozen; // kartu yang dibekukan melewatkan satu giliran
            u.frozen = false;
        }
        draw(t);
    }

    void check() {
        boolean a = s[0].hp <= 0, b = s[1].hp <= 0;
        if (a && b) winner = 2;
        else if (a) winner = 1;
        else if (b) winner = 0;
    }

    void damageHero(Side side, int dmg) {
        if (dmg <= 0) return;
        int absorbed = Math.min(side.shield, dmg);
        side.shield -= absorbed;
        side.hp -= dmg - absorbed;
    }

    Unit weakest(Side side) {
        Unit best = null;
        for (int i = 0; i < side.board.size; i++) {
            Unit u = side.board.get(i);
            if (best == null || u.hp < best.hp) best = u;
        }
        return best;
    }

    Unit strongest(Side side) {
        Unit best = null;
        for (int i = 0; i < side.board.size; i++) {
            Unit u = side.board.get(i);
            if (best == null || u.atk > best.atk) best = u;
        }
        return best;
    }

    void sweep() {
        for (int k = 0; k < 2; k++) {
            for (int i = s[k].board.size - 1; i >= 0; i--) {
                if (s[k].board.get(i).hp <= 0) s[k].board.removeIndex(i);
            }
        }
    }

    void runSteps(Unit u, Side me, Side op, CardDef.Step[] steps) {
        for (int i = 0; i < steps.length; i++) {
            String t = steps[i].t == null ? "" : steps[i].t;
            int v = steps[i].v;
            if (t.equals("hero")) damageHero(op, v);
            else if (t.equals("all")) { for (int k = 0; k < op.board.size; k++) op.board.get(k).hp -= v; }
            else if (t.equals("weak")) { Unit w = weakest(op); if (w != null) w.hp -= v; }
            else if (t.equals("strong")) { Unit g = strongest(op); if (g != null) g.hp -= v; }
            else if (t.equals("kill_strong")) { Unit g = strongest(op); if (g != null) g.hp = 0; }
            else if (t.equals("kill_weak")) { Unit w = weakest(op); if (w != null) w.hp = 0; }
            else if (t.equals("freeze")) { for (int k = 0; k < op.board.size; k++) op.board.get(k).frozen = true; }
            else if (t.equals("freeze_strong")) { Unit g = strongest(op); if (g != null) g.frozen = true; }
            else if (t.equals("heal")) me.hp = Math.min(MAX_HP, me.hp + v);
            else if (t.equals("heal_units")) { for (int k = 0; k < me.board.size; k++) me.board.get(k).hp += v; }
            else if (t.equals("buff_atk")) u.atk += v;
            else if (t.equals("buff_hp")) u.hp += v;
            else if (t.equals("team_atk")) { for (int k = 0; k < me.board.size; k++) me.board.get(k).atk += v; }
            else if (t.equals("team_hp")) { for (int k = 0; k < me.board.size; k++) me.board.get(k).hp += v; }
            else if (t.equals("shield")) me.shield += v;
            else if (t.equals("draw")) { for (int k = 0; k < v; k++) draw(turn); }
            else if (t.equals("mana")) me.mana += v;
            else if (t.equals("drain")) { damageHero(op, v); me.hp = Math.min(MAX_HP, me.hp + v); }
            else if (t.equals("rage")) u.atk *= 2;
            else if (t.equals("extra")) u.canAttack = true;
        }
    }

    /**
     * Aksi: "P:idxTangan", "A:idxPapan:target(-1=hero)", "S:idxPapan" (skill),
     * "U:idxPapan" (ultimate), "E" (akhiri giliran). Aksi selalu milik pemain yang sedang giliran.
     */
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
                if (d.effect.equals("dmg")) damageHero(op, d.value);
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
                    damageHero(op, u.atk);
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
            if (p[0].equals("S") || p[0].equals("U")) {
                boolean ult = p[0].equals("U");
                int ai = Integer.parseInt(p[1]);
                if (ai < 0 || ai >= me.board.size) return false;
                Unit u = me.board.get(ai);
                if (!u.canAttack) return false;
                CardDef d = u.def;
                CardDef.Step[] steps = ult ? d.ult : d.skill;
                if (steps == null || steps.length == 0) return false;
                int cost = ult ? d.ultCost : d.skillCost;
                if (cost > me.mana) return false;
                if (ult) { if (u.ultUsed) return false; }
                else if (u.skillCd > 0) return false;
                me.mana -= cost;
                if (ult) u.ultUsed = true; else u.skillCd = d.skillCd;
                u.canAttack = false;
                runSteps(u, me, op, steps);
                sweep();
                check();
                return true;
            }
        } catch (Exception ex) {
            // aksi rusak -> abaikan
        }
        return false;
    }
}
