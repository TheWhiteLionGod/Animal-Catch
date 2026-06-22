package com.example.animalcatch.battle;

/**
 * Scales a wild animal's base stats up to match the player's level, so
 * battles stay challenging as the player's animal grows stronger.
 *
 * Formula: statʼ = base * (1 + STEP * (playerLevel - 1))
 * Level 1  -> ×1.00 (no change)
 * Level 5  -> ×1.32
 * Level 10 -> ×1.72
 * HP scales a little faster than the other stats so fights don't become
 * pure damage races as levels climb.
 */
public final class EnemyScaler {

    private static final float STAT_STEP = 0.08f; // +8% per level above 1
    private static final float HP_STEP    = 0.12f; // +12% per level above 1

    private EnemyScaler() { }

    public static int scaleHp(int baseHp, int playerLevel) {
        return scale(baseHp, playerLevel, HP_STEP);
    }

    public static int scaleAtk(int baseAtk, int playerLevel) {
        return scale(baseAtk, playerLevel, STAT_STEP);
    }

    public static int scaleDef(int baseDef, int playerLevel) {
        return scale(baseDef, playerLevel, STAT_STEP);
    }

    public static int scaleSpd(int baseSpd, int playerLevel) {
        return scale(baseSpd, playerLevel, STAT_STEP);
    }

    private static int scale(int base, int playerLevel, float step) {
        int level = Math.max(1, playerLevel);
        float multiplier = 1f + step * (level - 1);
        return Math.max(1, Math.round(base * multiplier));
    }
}
