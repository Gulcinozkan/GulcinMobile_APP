package com.example.gulcinmobile.viewmodel

import NewsUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gulcinmobile.model.GNewsArticle
import com.example.gulcinmobile.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import com.example.gulcinmobile.model.TranslationRequest
import com.example.gulcinmobile.model.TranslationResponseItem
import retrofit2.Callback


class NewsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState

    private val repository = NewsRepository()
    private val apiKey = "0d45ddffecbd3f87a0ac7345ac4d7f5a"

    var selectedLanguageCode = "tr" // Varsayılan Türkçe
    private val translatedArticles = mutableListOf<GNewsArticle>()

    fun getTechNews() {
        viewModelScope.launch {
            try {
                val response = repository.searchTechNews(apiKey)
                translateArticleList(response.articles)
            } catch (e: Exception) {
                _uiState.value = NewsUiState(error = "Veri alınamadı: ${e.localizedMessage ?: e.toString()}")
            }
        }
    }

    private fun translateArticleList(articles: List<GNewsArticle>) {
        translatedArticles.clear()
        val total = articles.size
        var completed = 0

        for (article in articles.take(5)) {
            val title = article.title
            val desc = article.description ?: ""

            translateWithMicrosoft(title, selectedLanguageCode) { translatedTitle ->
                translateWithMicrosoft(desc, selectedLanguageCode) { translatedDesc ->
                    translatedArticles.add(
                        GNewsArticle(
                            title = translatedTitle,
                            description = translatedDesc,
                            url = article.url,
                            image = article.image
                        )
                    )
                    completed++
                    if (completed == minOf(5, total)) {
                        _uiState.value = NewsUiState(articles = translatedArticles)
                    }
                }
            }
        }
    }

    fun translateWithMicrosoft(text: String, targetLang: String, onResult: (String) -> Unit) {
        val request = listOf(TranslationRequest(text))
        val translateService = Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsofttranslator.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MicrosoftTranslateService::class.java)

        val call = translateService.translateDynamic("BearerKeyBurayaGelecek", targetLang, request)

        call.enqueue(object : Callback<List<TranslationResponseItem>> {
            override fun onResponse(
                call: Call<List<TranslationResponseItem>>,
                response: Response<List<TranslationResponseItem>>
            ) {
                if (response.isSuccessful) {
                    val translated = response.body()?.get(0)?.translations?.get(0)?.text
                    onResult(translated ?: text)
                } else {
                    onResult(text)
                }
            }

            override fun onFailure(call: Call<List<TranslationResponseItem>>, t: Throwable) {
                onResult(text)
            }
        })
    }
}



