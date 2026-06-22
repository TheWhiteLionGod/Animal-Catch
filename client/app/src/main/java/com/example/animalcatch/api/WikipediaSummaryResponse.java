package com.example.animalcatch.api;

import com.google.gson.annotations.SerializedName;

/**
 * Minimal parse of Wikipedia's page-summary response — we only care about
 * the thumbnail image, if one exists.
 */
public class WikipediaSummaryResponse {

    @SerializedName("thumbnail")
    private Thumbnail thumbnail;

    public Thumbnail getThumbnail() { return thumbnail; }

    /** @return the thumbnail image URL, or null if Wikipedia has none for this page. */
    public String getThumbnailUrl() {
        return thumbnail != null ? thumbnail.source : null;
    }

    public static class Thumbnail {
        @SerializedName("source")
        private String source;

        @SerializedName("width")
        private int width;

        @SerializedName("height")
        private int height;
    }
}
