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
            out.add(d);
        }
        return out;
    }
}
