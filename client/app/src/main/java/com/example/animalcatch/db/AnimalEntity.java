package com.example.animalcatch.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "animals")
public class AnimalEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "hp")
    private int hp;

    @ColumnInfo(name = "atk")
    private int atk;

    @ColumnInfo(name = "def")
    private int def;

    @ColumnInfo(name = "spd")
    private int spd;

    @ColumnInfo(name = "count")
    private int count;

    @ColumnInfo(name = "photo_path")
    private String photoPath;

    @ColumnInfo(name = "xp")
    private int xp;

    @ColumnInfo(name = "level")
    private int level;

    public AnimalEntity(@NonNull String name, int hp, int atk, int def, int spd,
                        int count, String photoPath, int xp, int level) {
        this.name     = name;
        this.hp       = hp;
        this.atk      = atk;
        this.def      = def;
        this.spd      = spd;
        this.count    = count;
        this.photoPath = photoPath;
        this.xp       = xp;
        this.level    = level;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    @NonNull public String getName()     { return name; }
    public int getHp()                   { return hp; }
    public int getAtk()                  { return atk; }
    public int getDef()                  { return def; }
    public int getSpd()                  { return spd; }
    public int getCount()                { return count; }
    public String getPhotoPath()         { return photoPath; }
    public int getXp()                   { return xp; }
    public int getLevel()                { return level; }

    // ── Setters (needed by Room update) ──────────────────────────────────────
    public void setCount(int count)          { this.count = count; }
    public void setPhotoPath(String p)       { this.photoPath = p; }
    public void setXp(int xp)               { this.xp = xp; }
    public void setLevel(int level)          { this.level = level; }
    public void setHp(int hp)               { this.hp = hp; }
    public void setAtk(int atk)             { this.atk = atk; }
    public void setDef(int def)             { this.def = def; }
    public void setSpd(int spd)             { this.spd = spd; }

    // ── XP helpers ───────────────────────────────────────────────────────────
    /** XP required to reach the next level (simple linear curve). */
    public static int xpForNextLevel(int level) {
        return level * 100;
    }
}