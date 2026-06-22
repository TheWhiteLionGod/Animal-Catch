package com.example.animalcatch.battle;

import com.example.animalcatch.db.AnimalEntity;

/**
 * Lightweight in-memory model used during a battle.
 * Tracks current HP separately from the persisted base stats so we never
 * accidentally write mid-battle HP back to the database.
 */
public class BattleAnimal {

    private final String name;
    private final int maxHp;
    private int currentHp;
    private final int atk;
    private final int def;
    private final int spd;
    private boolean defending; // true for one round after Defend is chosen

    /** Construct from a Room entity (the player's animal). */
    public BattleAnimal(AnimalEntity entity) {
        this.name      = capitalize(entity.getName());
        this.maxHp     = entity.getHp();
        this.currentHp = entity.getHp();
        this.atk       = entity.getAtk();
        this.def       = entity.getDef();
        this.spd       = entity.getSpd();
        this.defending = false;
    }

    /** Construct from raw stats (the AI enemy fetched from the API). */
    public BattleAnimal(String name, int hp, int atk, int def, int spd) {
        this.name      = capitalize(name);
        this.maxHp     = hp;
        this.currentHp = hp;
        this.atk       = atk;
        this.def       = def;
        this.spd       = spd;
        this.defending = false;
    }

    // ── Combat ────────────────────────────────────────────────────────────────

    /**
     * Deal damage from an attacker to this animal.
     * Formula: damage = max(1, attacker.atk - this.def/2)
     * If this animal is defending this round, incoming damage is halved.
     */
    public int receiveDamage(BattleAnimal attacker) {
        int raw = Math.max(1, attacker.getAtk() - (this.def / 2));
        if (defending) raw = Math.max(1, raw / 2);
        currentHp = Math.max(0, currentHp - raw);
        defending = false; // defence only lasts one hit
        return raw;
    }

    /** Mark this animal as defending this round. */
    public void defend() {
        defending = true;
    }

    public boolean isDefeated() {
        return currentHp <= 0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getName()      { return name; }
    public int getMaxHp()        { return maxHp; }
    public int getCurrentHp()    { return currentHp; }
    public int getAtk()          { return atk; }
    public int getDef()          { return def; }
    public int getSpd()          { return spd; }
    public boolean isDefending() { return defending; }

    /** HP as a 0–1 fraction for progress bars. */
    public float getHpFraction() {
        return (float) currentHp / maxHp;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}