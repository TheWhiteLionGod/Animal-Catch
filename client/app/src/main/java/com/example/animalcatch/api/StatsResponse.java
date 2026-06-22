package com.example.animalcatch.api;

import com.google.gson.annotations.SerializedName;

public class StatsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("name")
    private String name;

    @SerializedName("hp")
    private int hp;

    @SerializedName("atk")
    private int atk;

    @SerializedName("def")
    private int def;

    @SerializedName("spd")
    private int spd;

    public boolean isSuccess() { return success; }
    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getSpd() { return spd; }
}