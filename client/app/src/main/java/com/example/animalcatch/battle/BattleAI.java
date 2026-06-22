package com.example.animalcatch.battle;

import java.util.Random;

/**
 * Simple rule-based AI for the enemy animal.
 *
 * Strategy:
 *  - If HP > 60%  → attack 80% of the time, defend 20%
 *  - If HP 30–60% → attack 60%, defend 40%
 *  - If HP < 30%  → attack 90% (desperate all-out) OR defend 10%
 */
public class BattleAI {

    public enum Action { ATTACK, DEFEND }

    private final Random rng = new Random();

    public Action decideAction(BattleAnimal enemy) {
        float hpPercent = enemy.getHpFraction();
        double roll = rng.nextDouble();

        if (hpPercent > 0.6f) {
            return roll < 0.80 ? Action.ATTACK : Action.DEFEND;
        } else if (hpPercent > 0.3f) {
            return roll < 0.60 ? Action.ATTACK : Action.DEFEND;
        } else {
            // Low HP — goes all-out
            return roll < 0.90 ? Action.ATTACK : Action.DEFEND;
        }
    }
}