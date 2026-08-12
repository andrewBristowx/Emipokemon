package com.emipokemon.rewards.data;

/** Server-owned gacha credits. They are persisted with the player and never live only on the client. */
public final class RewardWallet {
    public long standardRolls;
    public long emiRolls;

    public void normalize() {
        standardRolls = Math.max(0L, standardRolls);
        emiRolls = Math.max(0L, emiRolls);
    }
}
