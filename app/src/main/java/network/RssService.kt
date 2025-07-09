package com.example.gulcinmobile.network

import android.util.Log
import com.example.gulcinmobile.model.GNewsArticle
import com.example.gulcinmobile.model.GNewsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class RssService {
    // RSS feed URL'leri
    private val techCrunchFeedUrl = "https://techcrunch.com/feed/"
    private val theVergeFeedUrl = "https://www.theverge.com/rss/index.xml"
    private val wiredFeedUrl = "https://www.wired.com/feed/rss"

    // Daha uzun timeout süresi ile OkHttpClient oluşturalım
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Teknoloji haberlerini çeken ana fonksiyon
    suspend fun fetchTechNews(): GNewsResponse {
        Log.d("RssService", "fetchTechNews başladı")
        return withContext(Dispatchers.IO) {
            val articles = mutableListOf<GNewsArticle>()

            try {
                Log.d("RssService", "RSS feed'lerini çekmeye başlıyoruz")
                // Paralel olarak tüm RSS feed'lerini çekelim
                val deferredResults = listOf(
                    async {
                        Log.d("RssService", "TechCrunch feed'ini çekmeye başlıyoruz")
                        fetchFromSource(techCrunchFeedUrl, "TechCrunch")
                    },
                    async {
                        Log.d("RssService", "The Verge feed'ini çekmeye başlıyoruz")
                        fetchFromSource(theVergeFeedUrl, "The Verge")
                    },
                    async {
                        Log.d("RssService", "Wired feed'ini çekmeye başlıyoruz")
                        fetchFromSource(wiredFeedUrl, "Wired")
                    }
                )

                // Tüm sonuçları bekleyelim ve birleştirelim
                val results = deferredResults.awaitAll()
                Log.d("RssService", "Tüm feed'ler çekildi, sonuçları birleştiriyoruz")

                results.forEachIndexed { index, list ->
                    Log.d("RssService", "Kaynak ${index + 1}: ${list.size} makale bulundu")
                    articles.addAll(list)
                }

                // En fazla 10 haber gösterelim
                val limitedArticles = articles.take(10)
                Log.d("RssService", "Toplam ${limitedArticles.size} makale döndürülüyor")

                GNewsResponse(
                    totalArticles = limitedArticles.size,
                    articles = limitedArticles
                )
            } catch (e: Exception) {
                Log.e("RssService", "Error fetching tech news: ${e.message}", e)
                // Hata durumunda boş liste döndür
                GNewsResponse(totalArticles = 0, articles = emptyList())
            }
        }
    }

    private suspend fun fetchFromSource(feedUrl: String, sourceName: String): List<GNewsArticle> {
        return try {
            Log.d("RssService", "$sourceName XML içeriğini çekmeye başlıyoruz: $feedUrl")
            val xmlContent = fetchXmlContent(feedUrl)
            Log.d("RssService", "$sourceName XML içeriği çekildi (${xmlContent.length} karakter)")

            val articles = parseRssFeedManually(xmlContent, sourceName)
            Log.d("RssService", "$sourceName için ${articles.size} makale parse edildi")
            articles
        } catch (e: Exception) {
            Log.e("RssService", "Error fetching from $sourceName: ${e.message}", e)
            emptyList()
        }
    }

    private fun fetchXmlContent(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) GulcinMobileApp/1.0")
            .build()

        Log.d("RssService", "HTTP isteği gönderiliyor: $url")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("RssService", "HTTP hatası: ${response.code}")
                throw IOException("Unexpected code $response")
            }
            val content = response.body?.string() ?: ""
            Log.d("RssService", "HTTP yanıtı alındı: ${content.length} karakter")
            return content
        }
    }

    private fun parseRssFeedManually(xmlContent: String, sourceName: String): List<GNewsArticle> {
        val articles = mutableListOf<GNewsArticle>()

        try {
            // <item> tag'lerini bulalım
            val itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL)
            val itemMatcher = itemPattern.matcher(xmlContent)

            while (itemMatcher.find()) {
                val itemContent = itemMatcher.group(1)
                if (itemContent != null) {
                    // Başlık, açıklama, link ve resim URL'sini çıkaralım
                    val title = extractTag(itemContent, "title")
                    val description = extractTag(itemContent, "description")
                    val link = extractTag(itemContent, "link")

                    Log.d("RssService", "Makale işleniyor: $title")

                    // Resim URL'sini bulalım
                    var imageUrl = extractImageUrl(itemContent)

                    // Eğer resim bulamazsak, varsayılan bir resim URL'si kullanabiliriz
                    if (imageUrl.isNullOrEmpty()) {
                        imageUrl = "https://via.placeholder.com/300x200?text=$sourceName"
                        Log.d("RssService", "Resim bulunamadı, varsayılan resim kullanılıyor")
                    }

                    val article = GNewsArticle(
                        title = "$title - $sourceName",
                        description = description,
                        url = link,
                        image = imageUrl
                    )

                    articles.add(article)
                    Log.d("RssService", "Makale listeye eklendi: $title")
                }
            }
        } catch (e: Exception) {
            Log.e("RssService", "Error parsing RSS feed for $sourceName: ${e.message}", e)
        }

        return articles
    }

    private fun extractTag(content: String, tagName: String): String {
        val pattern = Pattern.compile("<$tagName>(.*?)</$tagName>", Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            val result = matcher.group(1)?.trim() ?: ""
            // HTML tag'lerini temizleyelim
            result.replace(Regex("<.*?>"), "")
        } else {
            ""
        }
    }

    private fun extractImageUrl(itemContent: String): String? {
        // 1. Enclosure tag'inden resim URL'sini almayı deneyelim
        val enclosurePattern = Pattern.compile("<enclosure[^>]*url=\"([^\"]+)\"[^>]*type=\"image/[^\"]+\"[^>]*/>")
        val enclosureMatcher = enclosurePattern.matcher(itemContent)
        if (enclosureMatcher.find()) {
            val imageUrl = enclosureMatcher.group(1)
            Log.d("RssService", "Enclosure'dan resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        // 2. Media:content tag'inden resim URL'sini almayı deneyelim
        val mediaPattern = Pattern.compile("<media:content[^>]*url=\"([^\"]+)\"[^>]*/>")
        val mediaMatcher = mediaPattern.matcher(itemContent)
        if (mediaMatcher.find()) {
            val imageUrl = mediaMatcher.group(1)
            Log.d("RssService", "Media content'ten resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        // 3. Description içindeki ilk img tag'inden resim URL'sini almayı deneyelim
        val description = extractTag(itemContent, "description")
        val imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>")
        val imgMatcher = imgPattern.matcher(description)
        if (imgMatcher.find()) {
            val imageUrl = imgMatcher.group(1)
            Log.d("RssService", "Description'dan resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        return null
    }
}