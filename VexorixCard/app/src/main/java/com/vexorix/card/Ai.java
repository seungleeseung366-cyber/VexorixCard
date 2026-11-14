package com.vexorix.card;

public class Ai {
    /** Mengembalikan satu aksi berikutnya untuk AI. */
    public static String next(Battle b, int me) {
        Battle.Side a = b.s[me], o = b.s[1 - me];

        int best = -1;
        if (a.board.size < Battle.MAX_BOARD) {
            for (int i = 0; i < a.hand.size; i++) {
                CardDef c = a.hand.get(i);
                if (c.cost <= a.mana && (best < 0 || c.cost > a.hand.get(best).cost)) best = i;
            }
        }
        if (best >= 0) return "P:" + best;

        for (int i = 0; i < a.board.size; i++) {
            Battle.Unit u = a.board.get(i);
            if (!u.canAttack) continue;
            int target = -1;
            for (int j = 0; j < o.board.size; j++) { // bunuh tanpa mati
                Battle.Unit e = o.board.get(j);
                if (u.atk >= e.hp && u.hp > e.atk) { target = j; break; }
            }
            if (target < 0) {
                for (int j = 0; j < o.board.size; j++) { // tukar kartu
                    Battle.Unit e = o.board.get(j);
                    if (u.atk >= e.hp) { target = j; break; }
                }
            }
            return "A:" + i + ":" + target;
        }
        return "E";
    }
}
