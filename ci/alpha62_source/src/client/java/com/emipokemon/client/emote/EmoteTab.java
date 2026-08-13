package com.emipokemon.client.emote;

import net.minecraft.text.Text;

enum EmoteTab {
    FAVORITES("Favoritos"),
    EMI("Emi"),
    GLOBAL("Globales"),
    RECENT("Recientes");

    private final String label;

    EmoteTab(String label) {
        this.label = label;
    }

    Text label() {
        return Text.literal(label);
    }
}
