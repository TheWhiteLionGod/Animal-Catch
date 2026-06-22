package com.example.animalcatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.animalcatch.api.ApiClient;
import com.example.animalcatch.api.StatsResponse;
import com.example.animalcatch.api.WikipediaApiClient;
import com.example.animalcatch.api.WikipediaSummaryResponse;
import com.example.animalcatch.battle.BattleAI;
import com.example.animalcatch.battle.BattleAI.Action;
import com.example.animalcatch.battle.BattleAnimal;
import com.example.animalcatch.battle.EnemyScaler;
import com.example.animalcatch.db.AnimalDao;
import com.example.animalcatch.db.AnimalEntity;
import com.example.animalcatch.db.AppDatabase;
import com.google.android.material.button.MaterialButton;

import java.io.File;
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
    private ImageView imgPlayer, imgEnemy;
    private TextView tvPlayerName, tvPlayerHp, tvPlayerLevel;
    private ProgressBar pbPlayerHp;
    private TextView tvEnemyName, tvEnemyHp;
    private ProgressBar pbEnemyHp;
    private TextView tvVsBadge;
    private TextView tvBattleLog;
    private ScrollView scrollLog;
    private MaterialButton btnAttack, btnDefend, btnFlee;
    private View loadingOverlay;
    private View spritePlayerGroup, spriteEnemyGroup;

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
        applySystemBarInsets();

        animalDao = AppDatabase.getInstance(this).animalDao();
        ai = new BattleAI();

        pickPlayerAndEnemy();
    }

    /**
     * On API 35+ (targetSdk 35+), the app draws edge-to-edge by default and
     * content can be drawn underneath the system navigation bar / gesture
     * area. That bar's height varies by device (3-button nav vs gesture nav),
     * which is why this only showed up "sometimes" depending on the phone.
     * We push the action buttons up by the system bar inset so they're never
     * obscured, while letting the background art still extend full-bleed.
     */
    private void applySystemBarInsets() {
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int extraBottom = systemBars.bottom;
            addBottomMargin(btnAttack, extraBottom);
            addBottomMargin(btnDefend, extraBottom);
            addBottomMargin(btnFlee, extraBottom);
            return insets;
        });
    }

    private void addBottomMargin(View view, int extraBottomPx) {
        ConstraintLayout.LayoutParams lp =
                (ConstraintLayout.LayoutParams) view.getLayoutParams();
        lp.bottomMargin = lp.bottomMargin + extraBottomPx;
        view.setLayoutParams(lp);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        imgPlayer     = findViewById(R.id.img_player);
        imgEnemy      = findViewById(R.id.img_enemy);
        tvPlayerName  = findViewById(R.id.tv_player_name);
        tvPlayerHp    = findViewById(R.id.tv_player_hp);
        tvPlayerLevel = findViewById(R.id.tv_player_level);
        pbPlayerHp    = findViewById(R.id.pb_player_hp);
        tvEnemyName   = findViewById(R.id.tv_enemy_name);
        tvEnemyHp     = findViewById(R.id.tv_enemy_hp);
        pbEnemyHp     = findViewById(R.id.pb_enemy_hp);
        tvVsBadge     = findViewById(R.id.tv_vs_badge);
        tvBattleLog   = findViewById(R.id.tv_battle_log);
        scrollLog     = findViewById(R.id.scroll_log);
        btnAttack     = findViewById(R.id.btn_attack);
        btnDefend     = findViewById(R.id.btn_defend);
        btnFlee       = findViewById(R.id.btn_flee);
        loadingOverlay = findViewById(R.id.loading_overlay);
        spritePlayerGroup = findViewById(R.id.sprite_player_group);
        spriteEnemyGroup  = findViewById(R.id.sprite_enemy_group);

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
                loadPlayerSprite();
                fetchEnemyFromApi();
            });
        });
    }

    /** Load the player's saved catch photo into the arena sprite. */
    private void loadPlayerSprite() {
        String path = playerEntity.getPhotoPath();
        Glide.with(this)
                .load(path != null ? new File(path) : null)
                .placeholder(R.drawable.ic_paw_placeholder)
                .error(R.drawable.ic_paw_placeholder)
                .circleCrop()
                .into(imgPlayer);
    }

    /**
     * Pick a random wild animal, load its base stats from the game API, scale
     * them to the player's level, then fetch a real photo from Wikipedia.
     */
    private void fetchEnemyFromApi() {
        String enemyName = WILD_ANIMALS[new Random().nextInt(WILD_ANIMALS.length)];
        int playerLevel = playerEntity.getLevel();

        ApiClient.getApiService().getStats(enemyName).enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                loadingOverlay.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    StatsResponse s = response.body();

                    // Scale the wild animal's stats to match the player's level
                    // so battles stay challenging as the player grows stronger.
                    int scaledHp  = EnemyScaler.scaleHp(s.getHp(), playerLevel);
                    int scaledAtk = EnemyScaler.scaleAtk(s.getAtk(), playerLevel);
                    int scaledDef = EnemyScaler.scaleDef(s.getDef(), playerLevel);
                    int scaledSpd = EnemyScaler.scaleSpd(s.getSpd(), playerLevel);

                    enemy = new BattleAnimal(s.getName(), scaledHp, scaledAtk, scaledDef, scaledSpd);
                    updateEnemyHud();
                    loadEnemySprite(s.getName());
                    setButtonsEnabled(true);
                    playIntroAnimation();

                    appendLog("⚔️ A wild " + enemy.getName() + " appeared!");
                    appendLog("📋 " + enemy.getName()
                            + " — HP:" + enemy.getMaxHp()
                            + " ATK:" + enemy.getAtk()
                            + " DEF:" + enemy.getDef()
                            + " SPD:" + enemy.getSpd());
                    if (playerLevel > 1) {
                        appendLog("📈 Scaled to your Lv." + playerLevel + " animal!");
                    }
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

    /** Fetch a real photo of the wild animal's species from Wikipedia. */
    private void loadEnemySprite(String animalName) {
        Glide.with(this)
                .load(R.drawable.ic_paw_placeholder)
                .into(imgEnemy);

        // Wikipedia's REST summary API uses the canonical article title.
        // Some of our wild animal names need disambiguation suffixes.
        String title = toWikipediaTitle(animalName);

        WikipediaApiClient.getService().getSummary(title)
                .enqueue(new Callback<WikipediaSummaryResponse>() {
                    @Override
                    public void onResponse(Call<WikipediaSummaryResponse> call,
                                           Response<WikipediaSummaryResponse> response) {
                        if (isFinishing() || isDestroyed()) return;

                        if (!response.isSuccessful() || response.body() == null) {
                            appendLog("🖼️ No enemy photo (HTTP " + response.code() + ")");
                            return;
                        }

                        String url = response.body().getThumbnailUrl();

                        // Enforce HTTPS — Glide blocks plain HTTP on API 28+
                        if (url != null && url.startsWith("http://")) {
                            url = url.replaceFirst("http://", "https://");
                        }

                        if (url != null && !url.isEmpty()) {
                            final String finalUrl = url;
                            Glide.with(BattleActivity.this)
                                    .load(finalUrl)
                                    .placeholder(R.drawable.ic_paw_placeholder)
                                    .error(R.drawable.ic_paw_placeholder)
                                    .circleCrop()
                                    .into(imgEnemy);
                        } else {
                            appendLog("🖼️ Wikipedia returned no thumbnail for " + title);
                        }
                    }

                    @Override
                    public void onFailure(Call<WikipediaSummaryResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        appendLog("🖼️ Photo load failed: " + t.getMessage());
                    }
                });
    }

    /**
     * Maps game animal names to their exact Wikipedia article titles.
     * "Panther" and "Cheetah" in particular don't resolve cleanly from
     * the summary endpoint without the right title.
     */
    private String toWikipediaTitle(String animalName) {
        switch (animalName.toLowerCase()) {
            case "panther":   return "Panthera";          // "Panther" is a redirect
            case "bear":      return "Bear";
            case "wolf":      return "Wolf";
            case "eagle":     return "Eagle";
            case "shark":     return "Shark";
            case "lion":      return "Lion";
            case "tiger":     return "Tiger";
            case "crocodile": return "Crocodile";
            case "gorilla":   return "Gorilla";
            case "cheetah":   return "Cheetah";
            case "rhino":     return "Rhinoceros";
            case "hyena":     return "Hyena";
            default:
                return animalName.substring(0, 1).toUpperCase()
                        + animalName.substring(1).toLowerCase();
        }
    }


    /** Plays the sprite entrance + "VS" badge moment once both fighters are loaded. */
    private void playIntroAnimation() {
        spriteEnemyGroup.startAnimation(AnimationUtils.loadAnimation(this, R.anim.enter_from_right));
        spritePlayerGroup.startAnimation(AnimationUtils.loadAnimation(this, R.anim.enter_from_left));

        tvVsBadge.setVisibility(View.VISIBLE);
        tvVsBadge.startAnimation(AnimationUtils.loadAnimation(this, R.anim.vs_badge_pop));
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> tvVsBadge.setVisibility(View.INVISIBLE), 1150);
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
            spritePlayerGroup.startAnimation(AnimationUtils.loadAnimation(this, R.anim.lunge_left));
            updateEnemyHud();
            shakeView(spriteEnemyGroup);
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
            spriteEnemyGroup.startAnimation(AnimationUtils.loadAnimation(this, R.anim.lunge_right));
            updatePlayerHud();
            shakeView(spritePlayerGroup);
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
        tintHpBar(pbPlayerHp, player.getHpFraction());
    }

    private void updateEnemyHud() {
        tvEnemyName.setText(enemy.getName());
        tvEnemyHp.setText("HP: " + enemy.getCurrentHp() + "/" + enemy.getMaxHp());
        pbEnemyHp.setMax(enemy.getMaxHp());
        pbEnemyHp.setProgress(enemy.getCurrentHp());
        tintHpBar(pbEnemyHp, enemy.getHpFraction());
    }

    /** Shifts an HP bar from green → amber → red as the animal's health drops. */
    private void tintHpBar(ProgressBar bar, float hpFraction) {
        int colorRes;
        if (hpFraction > 0.5f) {
            colorRes = R.color.hp_high;
        } else if (hpFraction > 0.2f) {
            colorRes = R.color.hp_mid;
        } else {
            colorRes = R.color.hp_low;
        }

        // Mutate the clip layer's color directly — more reliable across
        // devices than tint lists when the progress drawable is a custom
        // layer-list (as ours is here). mutate() ensures this bar's drawable
        // instance doesn't share state with the other HP bar.
        android.graphics.drawable.Drawable progressLayer =
                bar.getProgressDrawable();
        if (progressLayer != null) {
            progressLayer = progressLayer.mutate();
            bar.setProgressDrawable(progressLayer);
        }
        if (progressLayer instanceof android.graphics.drawable.LayerDrawable) {
            android.graphics.drawable.Drawable clip =
                    ((android.graphics.drawable.LayerDrawable) progressLayer)
                            .findDrawableByLayerId(android.R.id.progress);
            if (clip != null) {
                clip.setTint(getColor(colorRes));
            }
        }
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