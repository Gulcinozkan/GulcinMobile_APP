package com.example.gulcinmobile.network

import com.example.gulcinmobile.model.GNewsResponse
import retrofit2.http.GET
import retrofit2.http.Query



interface GNewsApiService {
    @GET("search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("lang") language: String = "tr",
        @Query("token") apiKey: String,
        @Query("max") max: Int = 10
    ): GNewsResponse
}

    