package com.example.animalcatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.animalcatch.api.ApiClient;
import com.example.animalcatch.api.StatsResponse;
import com.example.animalcatch.battle.BattleAI;
import com.example.animalcatch.battle.BattleAI.Action;
import com.example.animalcatch.battle.BattleAnimal;
import com.example.animalcatch.db.AnimalDao;
import com.example.animalcatch.db.AnimalEntity;
import com.example.animalcatch.db.AppDatabase;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BattleActivity extends AppCompatActivity {

    // ── XP awarded per battle outcome ─────────────────────────────────────────
    private static final int XP_WIN  = 50;
    private static final int XP_LOSE = 10;

    // ── A pool of wild animals to fight. The API will fetch their real stats. ──
    private static final String[] WILD_ANIMALS = {
            "wolf", "bear", "eagle", "shark", "lion", "tiger",
            "crocodile", "gorilla", "cheetah", "panther", "rhino", "hyena"
    };

    // ── UI ────────────────────────────────────────────────────────────────────
    private TextView tvPlayerName, tvPlayerHp, tvPlayerLevel;
    private ProgressBar pbPlayerHp;
    private TextView tvEnemyName, tvEnemyHp;
    private ProgressBar pbEnemyHp;
    private TextView tvBattleLog;
    private ScrollView scrollLog;
    private MaterialButton btnAttack, btnDefend, btnFlee;
    private View loadingOverlay;

    // ── State ─────────────────────────────────────────────────────────────────
    private BattleAnimal player;
    private BattleAnimal enemy;
    private BattleAI ai;
    private AnimalEntity playerEntity;
    private AnimalDao animalDao;
    private boolean battleOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        bindViews();

        animalDao = AppDatabase.getInstance(this).animalDao();
        ai = new BattleAI();

        pickPlayerAndEnemy();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvPlayerName  = findViewById(R.id.tv_player_name);
        tvPlayerHp    = findViewById(R.id.tv_player_hp);
        tvPlayerLevel = findViewById(R.id.tv_player_level);
        pbPlayerHp    = findViewById(R.id.pb_player_hp);
        tvEnemyName   = findViewById(R.id.tv_enemy_name);
        tvEnemyHp     = findViewById(R.id.tv_enemy_hp);
        pbEnemyHp     = findViewById(R.id.pb_enemy_hp);
        tvBattleLog   = findViewById(R.id.tv_battle_log);
        scrollLog     = findViewById(R.id.scroll_log);
        btnAttack     = findViewById(R.id.btn_attack);
        btnDefend     = findViewById(R.id.btn_defend);
        btnFlee       = findViewById(R.id.btn_flee);
        loadingOverlay = findViewById(R.id.loading_overlay);

        btnAttack.setOnClickListener(v -> playerTurn(false));
        btnDefend.setOnClickListener(v -> playerTurn(true));
        btnFlee.setOnClickListener(v -> finish());
    }

    /** Load all player animals from Room, pick one at random, then fetch the enemy. */
    private void pickPlayerAndEnemy() {
        setButtonsEnabled(false);
        loadingOverlay.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<AnimalEntity> all = animalDao.getAll();
            if (all.isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Catch some animals first before battling!",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            }

            playerEntity = all.get(new Random().nextInt(all.size()));
            player = new BattleAnimal(playerEntity);

            runOnUiThread(() -> {
                updatePlayerHud();
                fetchEnemyFromApi();
            });
        });
    }

    /** Pick a random wild animal and load its stats from the API. */
    private void fetchEnemyFromApi() {
        String enemyName = WILD_ANIMALS[new Random().nextInt(WILD_ANIMALS.length)];

        ApiClient.getApiService().getStats(enemyName).enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                loadingOverlay.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    StatsResponse s = response.body();
                    enemy = new BattleAnimal(s.getName(), s.getHp(), s.getAtk(), s.getDef(), s.getSpd());
                    updateEnemyHud();
                    setButtonsEnabled(true);
                    appendLog("⚔️ A wild " + enemy.getName() + " appeared!");
                    appendLog("📋 " + enemy.getName()
                            + " — HP:" + enemy.getMaxHp()
                            + " ATK:" + enemy.getAtk()
                            + " DEF:" + enemy.getDef()
                            + " SPD:" + enemy.getSpd());
                    appendLog("──────────────────");
                    appendLog("Your turn! Choose an action.");
                } else {
                    appendLog("⚠️ Couldn't load enemy stats. Try again.");
                    btnFlee.setText("Back");
                }
            }

            @Override
            public void onFailure(Call<StatsResponse> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                appendLog("⚠️ Network error: " + t.getMessage());
                btnFlee.setText("Back");
            }
        });
    }

    // ── Turn logic ────────────────────────────────────────────────────────────

    /**
     * @param defending true = player chose Defend, false = Attack
     */
    private void playerTurn(boolean defending) {
        if (battleOver || enemy == null) return;
        setButtonsEnabled(false);

        if (defending) {
            player.defend();
            appendLog("🛡️ " + player.getName() + " takes a defensive stance!");
        } else {
            int dmg = enemy.receiveDamage(player);
            appendLog("⚔️ " + player.getName() + " attacks for " + dmg + " damage!");
            updateEnemyHud();
            shakeView(tvEnemyHp);
        }

        if (enemy.isDefeated()) {
            endBattle(true);
            return;
        }

        // Short delay so the player can read their action before the AI responds
        new Handler(Looper.getMainLooper()).postDelayed(this::enemyTurn, 700);
    }

    private void enemyTurn() {
        Action aiAction = ai.decideAction(enemy);

        if (aiAction == Action.DEFEND) {
            enemy.defend();
            appendLog("🛡️ " + enemy.getName() + " braces for impact!");
        } else {
            int dmg = player.receiveDamage(enemy);
            appendLog("💥 " + enemy.getName() + " attacks for " + dmg + " damage!");
            updatePlayerHud();
            shakeView(tvPlayerHp);
        }

        if (player.isDefeated()) {
            endBattle(false);
        } else {
            appendLog("──────────────────");
            setButtonsEnabled(true);
        }
    }

    // ── Battle end ────────────────────────────────────────────────────────────

    private void endBattle(boolean playerWon) {
        battleOver = true;
        setButtonsEnabled(false);
        btnAttack.setVisibility(View.GONE);
        btnDefend.setVisibility(View.GONE);
        btnFlee.setText("Continue");

        if (playerWon) {
            appendLog("══════════════════");
            appendLog("🏆 " + player.getName() + " won the battle!");
            awardXp(XP_WIN);
        } else {
            appendLog("══════════════════");
            appendLog("💀 " + player.getName() + " was defeated...");
            awardXp(XP_LOSE);
        }
    }

    /**
     * Award XP to the player's animal. Handles level-ups (multiple if needed)
     * and persists the result to Room.
     */
    private void awardXp(int xpGained) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Re-fetch from DB to get the freshest values
            AnimalEntity fresh = animalDao.getByName(playerEntity.getName());
            if (fresh == null) return;

            int xp    = fresh.getXp() + xpGained;
            int level = fresh.getLevel();
            int hp    = fresh.getHp();
            int atk   = fresh.getAtk();
            int def   = fresh.getDef();
            int spd   = fresh.getSpd();

            // Handle one or more level-ups
            StringBuilder levelUpMsg = new StringBuilder();
            while (xp >= AnimalEntity.xpForNextLevel(level)) {
                xp    -= AnimalEntity.xpForNextLevel(level);
                level += 1;
                // Stat boosts on level-up
                hp  += 10;
                atk += 3;
                def += 2;
                spd += 2;
                levelUpMsg.append("\n✨ Level up! ").append(fresh.getName()).append(" is now Lv.")
                        .append(level).append("!");
            }

            final int finalXp    = xp;
            final int finalLevel = level;
            final int finalHp    = hp;
            final int finalAtk   = atk;
            final int finalDef   = def;
            final int finalSpd   = spd;
            final String lvlMsg  = levelUpMsg.toString();

            animalDao.updateXpAndStats(fresh.getName(),
                    finalXp, finalLevel, finalHp, finalAtk, finalDef, finalSpd);

            runOnUiThread(() -> {
                appendLog("⭐ +" + xpGained + " XP"
                        + " (" + finalXp + "/" + AnimalEntity.xpForNextLevel(finalLevel) + ")");
                if (!lvlMsg.isEmpty()) appendLog(lvlMsg);
                tvPlayerLevel.setText("Lv." + finalLevel);
            });
        });
    }

    // ── HUD updates ───────────────────────────────────────────────────────────

    private void updatePlayerHud() {
        tvPlayerName.setText(player.getName());
        tvPlayerLevel.setText("Lv." + playerEntity.getLevel());
        tvPlayerHp.setText("HP: " + player.getCurrentHp() + "/" + player.getMaxHp());
        pbPlayerHp.setMax(player.getMaxHp());
        pbPlayerHp.setProgress(player.getCurrentHp());
    }

    private void updateEnemyHud() {
        tvEnemyName.setText(enemy.getName());
        tvEnemyHp.setText("HP: " + enemy.getCurrentHp() + "/" + enemy.getMaxHp());
        pbEnemyHp.setMax(enemy.getMaxHp());
        pbEnemyHp.setProgress(enemy.getCurrentHp());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendLog(String line) {
        tvBattleLog.append(line + "\n");
        // Auto-scroll to bottom
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private void setButtonsEnabled(boolean enabled) {
        btnAttack.setEnabled(enabled);
        btnDefend.setEnabled(enabled);
        btnAttack.setAlpha(enabled ? 1f : 0.5f);
        btnDefend.setAlpha(enabled ? 1f : 0.5f);
    }

    private void shakeView(View v) {
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }
}