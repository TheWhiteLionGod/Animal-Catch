package com.example.animalcatch;

public class Animal {
    private String name;
    private int hp;
    private int atk;
    private int def;
    private int spd;
    private int imageResId;

    public Animal(String name, int hp, int atk, int def, int spd, int imageResId) {
        this.name = name;
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getSpd() { return spd; }
    public int getImageResId() { return imageResId; }
}
