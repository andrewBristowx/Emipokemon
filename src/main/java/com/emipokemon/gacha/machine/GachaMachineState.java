package com.emipokemon.gacha.machine;

public enum GachaMachineState {
    IDLE,
    ACTIVATING,
    ROLLING,
    REVEAL_COMMON,
    REVEAL_EPIC,
    REVEAL_LEGENDARY,
    ERROR;

    public boolean isBusy() {
        return this != IDLE;
    }
}
