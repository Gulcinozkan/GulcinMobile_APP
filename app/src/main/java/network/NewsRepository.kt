package com.example.gulcinmobile.network

import com.example.gulcinmobile.model.GNewsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import network.RssService

class NewsRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gnews.io/api/v4/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GNewsApiService::class.java)
    private val rssService = RssService()

    suspend fun searchTechNews(apiKey: String): GNewsResponse {
        // TechCrunch, The Verge ve Wired'dan teknoloji haberleri çekelim
        Log.d("NewsRepository", "Teknoloji haberleri çekiliyor")
        try {
            val response = rssService.fetchTechNews()
            Log.d("NewsRepository", "Teknoloji haberleri başarıyla çekildi: ${response.articles.size} makale")
            return response
        } catch (e: Exception) {
            Log.e("NewsRepository", "Teknoloji haberleri çekilirken hata oluştu: ${e.message}", e)
            return GNewsResponse(totalArticles = 0, articles = emptyList())
        }
    }

    suspend fun searchGeneralNews(apiKey: String): GNewsResponse {
        val query = "world news OR breaking news"
        return apiService.searchNews(query, "en", apiKey)
    }

    suspend fun searchPoliticalNews(apiKey: String): GNewsResponse {
        val query = "politics OR government OR election"
        return apiService.searchNews(query, "en", apiKey)
    }

    suspend fun searchSportsNews(apiKey: String): GNewsResponse {
        val query = "sports OR football OR basketball OR tennis OR olympics"
        return apiService.searchNews(query, "en", apiKey)
    }

    suspend fun searchBusinessNews(apiKey: String): GNewsResponse {
        val query = "business OR economy OR finance OR stock market"
        return apiService.searchNews(query, "en", apiKey)
    }

    suspend fun searchArtNews(apiKey: String): GNewsResponse {
        val query = "art OR exhibition OR museum OR painting OR sculpture"
        return apiService.searchNews(query, "en", apiKey)
    }

    suspend fun searchEntertainmentNews(apiKey: String): GNewsResponse {
        val query = "celebrity OR entertainment OR movie OR music OR fashion"
        return apiService.searchNews(query, "en", apiKey)
    }
}