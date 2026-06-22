package com.example.animalcatch.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WikipediaService {

    /**
     * Wikipedia's REST "page summary" endpoint. Returns a short extract plus
     * a thumbnail image URL when one is available — no API key needed.
     * e.g. GET https://en.wikipedia.org/api/rest_v1/page/summary/Lion
     */
    @GET("api/rest_v1/page/summary/{title}")
    Call<WikipediaSummaryResponse> getSummary(@Path("title") String title);
}
