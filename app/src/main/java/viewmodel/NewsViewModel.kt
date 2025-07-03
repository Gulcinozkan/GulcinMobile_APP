package com.example.gulcinmobile.viewmodel

import NewsUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gulcinmobile.network.MicrosoftTranslateService
import com.example.gulcinmobile.network.NewsRepository
import com.example.gulcinmobile.network.TranslationRequest
import com.example.gulcinmobile.network.TranslationResponseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class NewsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState

    private val repository = NewsRepository()
    private val apiKey = "0d45ddffecbd3f87a0ac7345ac4d7f5a"

    fun getTechNews() {
        viewModelScope.launch {
            try {
                val response = repository.searchTechNews(apiKey)
                _uiState.value = NewsUiState(articles = response.articles)
            } catch (e: Exception) {
                _uiState.value = NewsUiState(error = "Veri alınamadı: ${e.localizedMessage ?: e.toString()}")
            }
        }
    }


    fun translateWithMicrosoft(text: String, onResult: (String) -> Unit) {
        val request = listOf(TranslationRequest(text))
        val translateService = Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsofttranslator.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MicrosoftTranslateService::class.java)

        val call = translateService.translate(request)

        call.enqueue(object : Callback<List<TranslationResponseItem>> {
            override fun onResponse(
                call: Call<List<TranslationResponseItem>>,
                response: Response<List<TranslationResponseItem>>
            ) {
                if (response.isSuccessful) {
                    val translated = response.body()?.get(0)?.translations?.get(0)?.text
                    onResult(translated ?: "Çeviri alınamadı")
                } else {
                    onResult("Çeviri başarısız: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<TranslationResponseItem>>, t: Throwable) {
                onResult("Hata: ${t.localizedMessage}")
            }
        })
    }



}
