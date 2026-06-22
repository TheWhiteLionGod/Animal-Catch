package com.example.animalcatch.api;

import com.google.gson.annotations.SerializedName;

public class IdentifyResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("animalName")
    private String name;

    public boolean isSuccess() { return success; }
    public String getName() { return name; }
}