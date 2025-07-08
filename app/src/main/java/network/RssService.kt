package network

import android.util.Log
import com.example.gulcinmobile.model.GNewsArticle
import com.example.gulcinmobile.model.GNewsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.IOException
import java.util.concurrent.TimeUnit

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

            val articles = parseRssFeed(xmlContent, sourceName)
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

    private fun parseRssFeed(xmlContent: String, sourceName: String): List<GNewsArticle> {
        val articles = mutableListOf<GNewsArticle>()

        try {
            // XML içeriğini Jsoup ile parse edelim
            Log.d("RssService", "$sourceName XML içeriğini parse etmeye başlıyoruz")
            val document = Jsoup.parse(xmlContent, "", Parser.xmlParser())

            // RSS item'larını bulalım
            val items = document.select("item")
            Log.d("RssService", "$sourceName için ${items.size} item bulundu")

            for (item in items) {
                val title = item.select("title").text()
                val description = item.select("description").text()
                val link = item.select("link").text()
                val pubDate = item.select("pubDate").text()

                Log.d("RssService", "Makale işleniyor: $title")

                // Resim URL'sini bulalım
                var imageUrl: String? = null

                // Enclosure tag'inden resim URL'sini almayı deneyelim
                val enclosure = item.select("enclosure[type^=image]").firstOrNull()
                if (enclosure != null) {
                    imageUrl = enclosure.attr("url")
                    Log.d("RssService", "Enclosure'dan resim URL'si bulundu: $imageUrl")
                }

                // Eğer enclosure'dan resim bulamazsak, description içindeki ilk resmi almayı deneyelim
                if (imageUrl.isNullOrEmpty()) {
                    imageUrl = findImageInDescription(description)
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d("RssService", "Description'dan resim URL'si bulundu: $imageUrl")
                    }
                }

                // Eğer hala resim bulamazsak, media:content tag'ini kontrol edelim
                if (imageUrl.isNullOrEmpty()) {
                    val mediaContent = item.select("media\\:content, content").firstOrNull()
                    imageUrl = mediaContent?.attr("url")
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d("RssService", "Media content'ten resim URL'si bulundu: $imageUrl")
                    }
                }

                // Eğer hiçbir yerden resim bulamazsak, varsayılan bir resim URL'si kullanabiliriz
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
        } catch (e: Exception) {
            Log.e("RssService", "Error parsing RSS feed for $sourceName: ${e.message}", e)
        }

        return articles
    }

    private fun findImageInDescription(description: String): String? {
        if (description.isEmpty()) return null

        try {
            // HTML içeriğinden img tag'larını çıkaralım
            val doc = Jsoup.parse(description)
            val imgElements = doc.select("img")
            if (imgElements.isNotEmpty()) {
                return imgElements.first()?.attr("src")
            }
        } catch (e: Exception) {
            Log.e("RssService", "Error parsing HTML content: ${e.message}", e)
        }

        return null
    }
}