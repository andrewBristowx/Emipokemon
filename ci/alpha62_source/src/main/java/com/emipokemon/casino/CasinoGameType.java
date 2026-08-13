package com.emipokemon.casino;

public enum CasinoGameType {
    SLOT("slot", "Máquina tragamonedas"),
    CHIP_EXCHANGE("chip_exchange", "Fichas Michicoins"),
    TICKET_EXCHANGE("ticket_exchange", "Máquina de tickets"),
    ROULETTE("roulette", "Ruleta"),
    POKER("poker", "Mesa de póker"),
    BLACKJACK("blackjack", "Mesa de blackjack"),
    DICE("dice", "Mesa de dados"),
    CLAW("claw", "Máquina de garra"),
    POKEMON_FLIP("pokemon_flip", "Cara o sello Pokémon");

    private final String id;
    private final String displayName;

    CasinoGameType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
