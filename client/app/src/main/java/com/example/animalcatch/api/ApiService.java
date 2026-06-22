package com.example.animalcatch.api;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @Multipart
    @POST("identify")
    Call<IdentifyResponse> identifyAnimal(
            @Part MultipartBody.Part image
    );

    @GET("getstats/{animalName}")
    Call<StatsResponse> getStats(
            @Path("animalName") String animalName
    );
}