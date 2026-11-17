package com.vexorix.card;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class Cards {
    public static Array<CardDef> load() {
        Array<CardDef> out = new Array<>();
        JsonValue root = new JsonReader().parse(Gdx.files.internal("cards.json"));
        for (JsonValue c = root.child; c != null; c = c.next) {
            CardDef d = new CardDef();
            d.id = c.getString("id", "card" + out.size);
            d.name = c.getString("name", "???");
            d.cost = c.getInt("cost", 1);
            d.attack = c.getInt("attack", 1);
            d.hp = Math.max(1, c.getInt("hp", 1));
            d.effect = c.getString("effect", "none");
            d.value = c.getInt("value", 0);
            d.image = c.getString("image", "");

            JsonValue sk = c.get("skill");
            if (sk != null) {
                d.skillName = sk.getString("name", "Skill");
                d.skillCost = sk.getInt("cost", 1);
                d.skillCd = Math.max(1, sk.getInt("cd", 2));
                d.skill = steps(sk.get("steps"));
            }
            JsonValue ul = c.get("ult");
            if (ul != null) {
                d.ultName = ul.getString("name", "Ultimate");
                d.ultCost = ul.getInt("cost", 5);
                d.ult = steps(ul.get("steps"));
            }
            out.add(d);
        }
        return out;
    }

    static CardDef.Step[] steps(JsonValue arr) {
        if (arr == null) return new CardDef.Step[0];
        CardDef.Step[] out = new CardDef.Step[arr.size];
        int i = 0;
        for (JsonValue s = arr.child; s != null; s = s.next) {
            CardDef.Step st = new CardDef.Step();
            st.t = s.getString("t", "");
            st.v = s.getInt("v", 0);
            out[i++] = st;
        }
        return out;
    }
}
