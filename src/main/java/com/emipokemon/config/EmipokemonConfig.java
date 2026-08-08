package com.emipokemon.config;

public final class EmipokemonConfig {
    public int configVersion = 1;
    public boolean debugLogging = false;
    public boolean playerDataEnabled = true;

    public void normalize() {
        if (configVersion < 1) {
            configVersion = 1;
        }
    }
}
