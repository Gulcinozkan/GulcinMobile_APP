package com.example.gulcinmobile.viewmodel

import NewsUiState
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gulcinmobile.datastore.DataStoreManager
import com.example.gulcinmobile.model.GNewsArticle
import com.example.gulcinmobile.model.TranslatedText
import com.example.gulcinmobile.model.TranslationRequest
import com.example.gulcinmobile.model.TranslationResponseItem
import com.example.gulcinmobile.network.MicrosoftTranslateService
import network.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NewsViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState

    private val repository = NewsRepository()
    private val apiKey = "0d45ddffecbd3f87a0ac7345ac4d7f5a"
    private val dataStoreManager = DataStoreManager(context)

    // Microsoft Translate API anahtarı
    private val translateApiKey = "EtTISMBaiR6jFC2tM2YiozgsJCV6526WNxDRHxjhgcWmTAX8jqxJJQQJ99BGACqBBLyXJ3w3AAAbACOG56Zw"

    // Orijinal İngilizce haberler
    private var originalArticles = listOf<GNewsArticle>()

    var selectedLanguageCode = "en" // Default to English

    // Aktif kategori
    private var currentCategory = "tech" // Default kategori

    // Retrofit client for translation with detailed logging
    private val translateService: MicrosoftTranslateService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsofttranslator.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MicrosoftTranslateService::class.java)
    }

    init {
        // Sadece dil tercihini yükle, haberleri otomatik çekme
        viewModelScope.launch {
            selectedLanguageCode = dataStoreManager.getLanguage()
            Log.d("NewsViewModel", "Loaded language preference: $selectedLanguageCode")
        }
    }

    // Teknoloji haberleri
    fun getTechNews() {
        currentCategory = "tech"
        fetchNews { repository.searchTechNews(apiKey) }
    }

    // Genel haberler
    fun getGeneralNews() {
        currentCategory = "general"
        fetchNews { repository.searchGeneralNews(apiKey) }
    }

    // Siyasi haberler
    fun getPoliticalNews() {
        currentCategory = "political"
        fetchNews { repository.searchPoliticalNews(apiKey) }
    }

    // Spor haberleri
    fun getSportsNews() {
        currentCategory = "sports"
        fetchNews { repository.searchSportsNews(apiKey) }
    }

    // İş dünyası haberleri
    fun getBusinessNews() {
        currentCategory = "business"
        fetchNews { repository.searchBusinessNews(apiKey) }
    }

    // Sanat haberleri
    fun getArtNews() {
        currentCategory = "art"
        fetchNews { repository.searchArtNews(apiKey) }
    }

    // Magazin haberleri
    fun getEntertainmentNews() {
        currentCategory = "entertainment"
        fetchNews { repository.searchEntertainmentNews(apiKey) }
    }
    // AI haberleri
    fun getAINews() {
        currentCategory = "ai"
        fetchNews { repository.searchAINews(apiKey) }
    }


    // Ortak haber çekme fonksiyonu
    private fun fetchNews(newsSource: suspend () -> com.example.gulcinmobile.model.GNewsResponse) {
        viewModelScope.launch {
            try {
                Log.d("NewsViewModel", "Loading news for category: $currentCategory")
                _uiState.value = NewsUiState(error = "", isLoading = true) // Clear error message and show loading

                val response = newsSource()
                Log.d("NewsViewModel", "API response received: ${response.totalArticles} news found")

                if (response.articles.isEmpty()) {
                    _uiState.value = NewsUiState(error = "No news found", isLoading = false)
                    return@launch
                }

                // Orijinal İngilizce haberleri sakla
                originalArticles = response.articles

                // Eğer dil İngilizce ise doğrudan göster
                if (selectedLanguageCode == "en") {
                    _uiState.value = NewsUiState(articles = originalArticles, isLoading = false)
                    Log.d("NewsViewModel", "Showing original English news")
                } else {
                    // Değilse çevir
                    Log.d("NewsViewModel", "Translating news to: $selectedLanguageCode")
                    translateArticles(originalArticles)
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error loading news", e)
                // Set error message based on selected language
                val errorPrefix = when (selectedLanguageCode) {
                    "tr" -> "Veri alınamadı"
                    "fr" -> "Impossible de récupérer les données"
                    "es" -> "No se pudieron obtener datos"
                    "de" -> "Daten konnten nicht abgerufen werden"
                    else -> "Could not retrieve data"
                }
                _uiState.value = NewsUiState(error = "$errorPrefix: ${e.localizedMessage ?: e.toString()}", isLoading = false)
            }
        }
    }

    private fun translateArticles(articles: List<GNewsArticle>) {
        Log.d("NewsViewModel", "Starting translation of ${articles.size} articles to $selectedLanguageCode")

        // Eğer dil İngilizceyse, çeviriye gerek yok
        if (selectedLanguageCode == "en") {
            _uiState.value = NewsUiState(articles = articles, isLoading = false)
            Log.d("NewsViewModel", "Language is English, no translation needed")
            return
        }

        // En fazla 5 makaleyi çevirelim (API limitleri için)
        val articlesToTranslate = articles.take(5)
        val translatedArticles = mutableListOf<GNewsArticle>()
        val pendingTranslations = AtomicInteger(articlesToTranslate.size * 2) // Her makale için başlık ve açıklama

        Log.d("NewsViewModel", "Will translate ${articlesToTranslate.size} articles (${pendingTranslations.get()} texts total)")

        // Her makale için başlık ve açıklamayı çevir
        for (article in articlesToTranslate) {
            val title = article.title
            val description = article.description ?: ""

            // Başlık ve açıklama için geçici değişkenler
            var translatedTitle = ""
            var translatedDescription = ""

            // Başlığı çevir
            Log.d("NewsViewModel", "Translating title: ${title.take(30)}...")
            translateText(title) { result ->
                translatedTitle = result
                Log.d("NewsViewModel", "Title translated: ${result.take(30)}...")

                // Çeviri tamamlandığında sayacı azalt
                if (pendingTranslations.decrementAndGet() == 0) {
                    // Tüm çeviriler tamamlandı, UI'ı güncelle
                    Log.d("NewsViewModel", "All translations complete, updating UI")
                    _uiState.value = NewsUiState(articles = translatedArticles, isLoading = false)
                }
            }

            // Açıklamayı çevir
            Log.d("NewsViewModel", "Translating description: ${description.take(30)}...")
            translateText(description) { result ->
                translatedDescription = result
                Log.d("NewsViewModel", "Description translated: ${result.take(30)}...")

                // Yeni çevrilmiş makale oluştur
                val translatedArticle = GNewsArticle(
                    title = if (translatedTitle.isNotBlank()) translatedTitle else title,
                    description = result,
                    url = article.url,
                    image = article.image
                )

                translatedArticles.add(translatedArticle)
                Log.d("NewsViewModel", "Added translated article to list")

                // Çeviri tamamlandığında sayacı azalt
                if (pendingTranslations.decrementAndGet() == 0) {
                    // Tüm çeviriler tamamlandı, UI'ı güncelle
                    Log.d("NewsViewModel", "All translations complete, updating UI")
                    _uiState.value = NewsUiState(articles = translatedArticles, isLoading = false)
                }
            }
        }
    }

    private fun translateText(text: String, onComplete: (String) -> Unit) {
        // Boş metin kontrolü
        if (text.isBlank()) {
            onComplete(text)
            return
        }

        Log.d("NewsViewModel", "Translating text to $selectedLanguageCode: ${text.take(30)}...")

        // Çeviri isteği oluştur
        val request = listOf(TranslationRequest(text))

        // API çağrısı yap
        try {
            translateService.translateDynamic(
                key = translateApiKey,
                toLang = selectedLanguageCode,
                fromLang = "en", // Her zaman İngilizce'den çeviri yapıyoruz
                body = request
            ).enqueue(object : Callback<List<TranslationResponseItem>> {
                override fun onResponse(
                    call: Call<List<TranslationResponseItem>>,
                    response: Response<List<TranslationResponseItem>>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("NewsViewModel", "Translation API response successful: $responseBody")

                        try {
                            if (responseBody != null && responseBody.isNotEmpty()) {
                                val translations = responseBody[0].translations
                                if (translations.isNotEmpty()) {
                                    val translatedText = translations[0].text
                                    Log.d("NewsViewModel", "Translation successful: ${translatedText.take(30)}...")
                                    onComplete(translatedText)
                                    return
                                }
                            }

                            Log.e("NewsViewModel", "Translation response was empty or invalid")
                            onComplete(text) // Çeviri başarısız olursa orijinal metni döndür
                        } catch (e: Exception) {
                            Log.e("NewsViewModel", "Error parsing translation response", e)
                            onComplete(text)
                        }
                    } else {
                        Log.e("NewsViewModel", "Translation API error: ${response.code()} - ${response.message()}")
                        try {
                            val errorBody = response.errorBody()?.string()
                            Log.e("NewsViewModel", "Error body: $errorBody")
                        } catch (e: Exception) {
                            Log.e("NewsViewModel", "Could not read error body", e)
                        }
                        onComplete(text)
                    }
                }

                override fun onFailure(call: Call<List<TranslationResponseItem>>, t: Throwable) {
                    Log.e("NewsViewModel", "Translation API call failed", t)
                    onComplete(text)
                }
            })
        } catch (e: Exception) {
            Log.e("NewsViewModel", "Exception while making translation API call", e)
            onComplete(text)
        }
    }

    // Dil tercihini güncelle ve mevcut haberleri çevir
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            Log.d("NewsViewModel", "Updating language from $selectedLanguageCode to $languageCode")
            dataStoreManager.saveLanguage(languageCode)
            selectedLanguageCode = languageCode

            // Eğer haber yoksa bir şey yapma
            if (originalArticles.isEmpty()) {
                Log.d("NewsViewModel", "No articles to translate")
                return@launch
            }

            // Eğer dil İngilizceyse, orijinal haberleri göster
            if (languageCode == "en") {
                _uiState.value = NewsUiState(articles = originalArticles, isLoading = false)
                Log.d("NewsViewModel", "Showing original English news after language change")
            } else {
                // Değilse, mevcut haberleri yeni dile çevir
                Log.d("NewsViewModel", "Translating news to new language: $languageCode")
                translateArticles(originalArticles)
            }
        }
    }
}