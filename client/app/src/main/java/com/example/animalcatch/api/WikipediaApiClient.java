package com.example.animalcatch.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Separate Retrofit client for Wikipedia's public REST API.
 * Used only to fetch a representative photo for the wild enemy animal —
 * no API key required.
 */
public class WikipediaApiClient {

    private static final String BASE_URL = "https://en.wikipedia.org/";
    private static final String USER_AGENT = "AnimalCatch/1.0 (https://github.com/example/animalcatch)";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request request = chain.request()
                                .newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static WikipediaService getService() {
        return getClient().create(WikipediaService.class);
    }
}