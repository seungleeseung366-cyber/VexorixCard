package com.vexorix.card;

public class CardDef {
    /** Satu langkah efek skill/ultimate, mis. {t:"hero", v:3}. */
    public static class Step {
        public String t = "";
        public int v = 0;
    }

    public String id, name, effect, image;
    public int cost, attack, hp, value;

    // skill biasa (bisa dipakai berulang, ada cooldown) dan ultimate (sekali per kartu per duel)
    public String skillName = "", ultName = "";
    public int skillCost = 1, skillCd = 2, ultCost = 5;
    public Step[] skill = new Step[0];
    public Step[] ult = new Step[0];
}
