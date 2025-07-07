package com.example.gulcinmobile.network

import com.example.gulcinmobile.model.GNewsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewsRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gnews.io/api/v4/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GNewsApiService::class.java)

    suspend fun searchTechNews(apiKey: String): GNewsResponse {
        val query = "artificial intelligence OR robotics OR technology invention"
        // Always fetch news in English
        return apiService.searchNews(query, "en", apiKey)
    }
}
