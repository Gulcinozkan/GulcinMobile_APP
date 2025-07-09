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
    private val theVergeFeedUrlAlternative = "https://www.theverge.com/rss/front-page/index.xml" // Alternatif The Verge URL'si
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
            try {
                Log.d("RssService", "RSS feed'lerini çekmeye başlıyoruz")

                // Her kaynağı ayrı ayrı çekelim ve hata durumunda diğerlerini etkilemesin
                val techCrunchArticles = try {
                    Log.d("RssService", "TechCrunch feed'ini çekmeye başlıyoruz")
                    fetchFromSource(techCrunchFeedUrl, "TechCrunch")
                } catch (e: Exception) {
                    Log.e("RssService", "TechCrunch feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                // The Verge için önce ana URL'yi deneyelim, başarısız olursa alternatif URL'yi kullanalım
                var vergeArticles = try {
                    Log.d("RssService", "The Verge feed'ini çekmeye başlıyoruz: $theVergeFeedUrl")
                    fetchFromSource(theVergeFeedUrl, "The Verge")
                } catch (e: Exception) {
                    Log.e("RssService", "The Verge ana feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                // Eğer The Verge ana URL'den hiç makale çekilemezse, alternatif URL'yi deneyelim
                if (vergeArticles.isEmpty()) {
                    Log.d("RssService", "The Verge alternatif feed'i deneniyor: $theVergeFeedUrlAlternative")
                    vergeArticles = try {
                        fetchFromSource(theVergeFeedUrlAlternative, "The Verge")
                    } catch (e: Exception) {
                        Log.e("RssService", "The Verge alternatif feed'i de çekilemedi: ${e.message}", e)
                        emptyList()
                    }
                }

                val wiredArticles = try {
                    Log.d("RssService", "Wired feed'ini çekmeye başlıyoruz")
                    fetchFromSource(wiredFeedUrl, "Wired")
                } catch (e: Exception) {
                    Log.e("RssService", "Wired feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                Log.d("RssService", "Tüm feed'ler çekildi, sonuçları birleştiriyoruz")
                Log.d("RssService", "TechCrunch: ${techCrunchArticles.size}, The Verge: ${vergeArticles.size}, Wired: ${wiredArticles.size}")

                // İstenen dağılım: 3 TechCrunch, 3 The Verge, 4 Wired
                val targetTechCrunch = 3
                val targetVerge = 3
                val targetWired = 4

                // Her kaynaktan kaç makale alacağımızı belirleyelim
                val techCrunchCount = minOf(targetTechCrunch, techCrunchArticles.size)
                val vergeCount = minOf(targetVerge, vergeArticles.size)
                val wiredCount = minOf(targetWired, wiredArticles.size)

                // Eksik makale sayısını hesaplayalım
                val missingArticles = (targetTechCrunch + targetVerge + targetWired) - (techCrunchCount + vergeCount + wiredCount)

                // Eğer eksik makale varsa, diğer kaynaklardan telafi edelim
                var extraTechCrunch = 0
                var extraVerge = 0
                var extraWired = 0

                if (missingArticles > 0) {
                    // Önce Wired'dan eksikleri tamamlamaya çalışalım
                    val availableExtraWired = wiredArticles.size - wiredCount
                    extraWired = minOf(missingArticles, availableExtraWired)

                    // Hala eksik varsa, TechCrunch'tan tamamlayalım
                    val remainingMissing = missingArticles - extraWired
                    if (remainingMissing > 0) {
                        val availableExtraTechCrunch = techCrunchArticles.size - techCrunchCount
                        extraTechCrunch = minOf(remainingMissing, availableExtraTechCrunch)

                        // Hala eksik varsa, The Verge'den tamamlayalım
                        val stillMissing = remainingMissing - extraTechCrunch
                        if (stillMissing > 0) {
                            val availableExtraVerge = vergeArticles.size - vergeCount
                            extraVerge = minOf(stillMissing, availableExtraVerge)
                        }
                    }
                }

                // Makaleleri birleştirelim
                val allArticles = mutableListOf<GNewsArticle>()

                // TechCrunch makalelerini ekleyelim
                allArticles.addAll(techCrunchArticles.take(techCrunchCount + extraTechCrunch))

                // The Verge makalelerini ekleyelim
                allArticles.addAll(vergeArticles.take(vergeCount + extraVerge))

                // Wired makalelerini ekleyelim
                allArticles.addAll(wiredArticles.take(wiredCount + extraWired))

                // Toplam makale sayısını loglayalım
                Log.d("RssService", "Dağılım - TechCrunch: ${techCrunchCount + extraTechCrunch}, " +
                        "The Verge: ${vergeCount + extraVerge}, Wired: ${wiredCount + extraWired}")
                Log.d("RssService", "Toplam ${allArticles.size} makale döndürülüyor")

                GNewsResponse(
                    totalArticles = allArticles.size,
                    articles = allArticles
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

            // XML içeriğini logcat'e yazdıralım (debug için)
            Log.d("RssService", "XML içeriği (ilk 500 karakter): ${xmlContent.take(500)}")

            val articles = when (sourceName) {
                "TechCrunch" -> parseTechCrunchFeed(xmlContent, sourceName)
                "The Verge" -> parseVergeFeed(xmlContent, sourceName)
                else -> parseRssFeedManually(xmlContent, sourceName)
            }

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

    /// TechCrunch için özel parser
    private fun parseTechCrunchFeed(xmlContent: String, sourceName: String): List<GNewsArticle> {
        val articles = mutableListOf<GNewsArticle>()

        try {
            Log.d("RssService", "TechCrunch XML içeriği analiz ediliyor")

            val itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL)
            val itemMatcher = itemPattern.matcher(xmlContent)

            while (itemMatcher.find()) {
                val itemContent = itemMatcher.group(1)
                if (itemContent != null) {
                    var title = extractTag(itemContent, "title")
                    val link = extractTag(itemContent, "link")

                    Log.d("RssService", "TechCrunch makalesi işleniyor: $title")

                    // TechCrunch için açıklama çıkarma stratejileri
                    var description = extractTag(itemContent, "content:encoded")
                    if (description.isEmpty()) description = extractTag(itemContent, "description")
                    if (description.isEmpty()) description = extractTag(itemContent, "excerpt:encoded")

                    // CDATA içindeki içeriği çıkarmayı deneyelim
                    if (description.contains("<![CDATA[")) {
                        val cdataPattern = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL)
                        val cdataMatcher = cdataPattern.matcher(description)
                        if (cdataMatcher.find()) {
                            description = cdataMatcher.group(1) ?: description
                            Log.d("RssService", "TechCrunch CDATA içeriği çıkarıldı")
                        }
                    }

                    // Açıklamayı temizleyelim ve kısaltalım
                    val cleanDescription = if (description.isNotEmpty()) {
                        cleanAndShortenDescription(description)
                    } else {
                        "Bu haber için açıklama bulunmamaktadır."
                    }

                    // Resim URL'sini bulalım
                    var imageUrl = extractImageUrlFromTechCrunch(itemContent)

                    // Eğer resim bulamazsak, varsayılan bir resim URL'si kullanabiliriz
                    if (imageUrl.isNullOrEmpty()) {
                        imageUrl = "https://via.placeholder.com/300x200?text=TechCrunch"
                        Log.d("RssService", "Resim bulunamadı, varsayılan resim kullanılıyor")
                    }

                    // Eğer başlık ve link varsa, makaleyi ekleyelim
                    if (title.isNotEmpty() && link.isNotEmpty()) {
                        val article = GNewsArticle(
                            title = title,
                            description = cleanDescription,
                            url = link,
                            image = imageUrl
                        )

                        articles.add(article)
                        Log.d("RssService", "TechCrunch makalesi listeye eklendi: $title")
                    }
                }
            }

            Log.d("RssService", "TechCrunch için toplam ${articles.size} makale bulundu")
        } catch (e: Exception) {
            Log.e("RssService", "Error parsing TechCrunch feed: ${e.message}", e)
        }

        return articles
    }


    // The Verge için özel parser
    private fun parseVergeFeed(xmlContent: String, sourceName: String): List<GNewsArticle> {
        val articles = mutableListOf<GNewsArticle>()

        try {
            Log.d("RssService", "The Verge XML içeriği analiz ediliyor")

            // The Verge için <entry> tag'lerini bulalım (Atom formatı kullanıyor olabilir)
            var itemPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL)
            var itemMatcher = itemPattern.matcher(xmlContent)
            var isAtomFormat = itemMatcher.find()

            // Eğer <entry> tag'i bulunamazsa, <item> tag'ini deneyelim
            if (!isAtomFormat) {
                Log.d("RssService", "The Verge için Atom formatı bulunamadı, RSS formatı deneniyor")
                itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL)
                itemMatcher = itemPattern.matcher(xmlContent)
            } else {
                Log.d("RssService", "The Verge için Atom formatı bulundu")
                // Matcher'ı sıfırlayalım
                itemMatcher.reset()
            }

            while (itemMatcher.find()) {
                val itemContent = itemMatcher.group(1)
                if (itemContent != null) {
                    var title = ""
                    var link = ""

                    if (isAtomFormat) {
                        // Atom formatı için başlık ve link çıkarma
                        title = extractTag(itemContent, "title")

                        // Atom formatında link bir attribute olabilir
                        val linkPattern = Pattern.compile("<link[^>]*href=\"([^\"]+)\"[^>]*/>")
                        val linkMatcher = linkPattern.matcher(itemContent)
                        if (linkMatcher.find()) {
                            link = linkMatcher.group(1) ?: ""
                        }
                    } else {
                        // RSS formatı için başlık ve link çıkarma
                        title = extractTag(itemContent, "title")
                        link = extractTag(itemContent, "link")
                    }

                    Log.d("RssService", "The Verge makalesi işleniyor: $title")

                    // The Verge için açıklama çıkarma stratejileri
                    var description = ""

                    if (isAtomFormat) {
                        // Atom formatı için açıklama çıkarma
                        description = extractTag(itemContent, "content")
                        if (description.isEmpty()) {
                            description = extractTag(itemContent, "summary")
                        }
                    } else {
                        // RSS formatı için açıklama çıkarma
                        description = extractTag(itemContent, "description")
                        if (description.isEmpty()) {
                            description = extractTag(itemContent, "content:encoded")
                        }
                    }

                    // CDATA içindeki içeriği çıkarmayı deneyelim
                    if (description.contains("<![CDATA[")) {
                        val cdataPattern = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL)
                        val cdataMatcher = cdataPattern.matcher(description)
                        if (cdataMatcher.find()) {
                            description = cdataMatcher.group(1) ?: description
                            Log.d("RssService", "The Verge CDATA içeriği çıkarıldı")
                        }
                    }

                    // Açıklamayı temizleyelim ve kısaltalım
                    val cleanDescription = if (description.isNotEmpty()) {
                        cleanAndShortenDescription(description)
                    } else {
                        "Bu haber için açıklama bulunmamaktadır."
                    }

                    // Resim URL'sini bulalım
                    var imageUrl = extractImageUrlFromVerge(itemContent)

                    // Eğer resim bulamazsak, varsayılan bir resim URL'si kullanabiliriz
                    if (imageUrl.isNullOrEmpty()) {
                        imageUrl = "https://via.placeholder.com/300x200?text=The+Verge"
                        Log.d("RssService", "Resim bulunamadı, varsayılan resim kullanılıyor")
                    }

                    // Eğer başlık ve link varsa, makaleyi ekleyelim
                    if (title.isNotEmpty() && link.isNotEmpty()) {
                        val article = GNewsArticle(
                            title = title,
                            description = cleanDescription,
                            url = link,
                            image = imageUrl
                        )

                        articles.add(article)
                        Log.d("RssService", "The Verge makalesi listeye eklendi: $title")
                    }
                }
            }

            Log.d("RssService", "The Verge için toplam ${articles.size} makale bulundu")
        } catch (e: Exception) {
            Log.e("RssService", "Error parsing The Verge feed: ${e.message}", e)
        }

        return articles
    }

    private fun parseRssFeedManually(xmlContent: String, sourceName: String): List<GNewsArticle> {
        val articles = mutableListOf<GNewsArticle>()

        try {
            Log.d("RssService", "XML içeriği analiz ediliyor")

            val itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL)
            val itemMatcher = itemPattern.matcher(xmlContent)

            while (itemMatcher.find()) {
                val itemContent = itemMatcher.group(1)
                if (itemContent != null) {
                    var title = extractTag(itemContent, "title")
                    var description = extractTag(itemContent, "description")
                    val link = extractTag(itemContent, "link")

                    // CDATA içindeki içeriği çıkarmayı deneyelim
                    if (description.contains("<![CDATA[")) {
                        val cdataPattern = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL)
                        val cdataMatcher = cdataPattern.matcher(description)
                        if (cdataMatcher.find()) {
                            description = cdataMatcher.group(1) ?: description
                            Log.d("RssService", "CDATA içeriği çıkarıldı")
                        }
                    }

                    // Açıklamayı temizleyelim ve kısaltalım
                    val cleanDescription = if (description.isNotEmpty()) {
                        cleanAndShortenDescription(description)
                    } else {
                        "Bu haber için açıklama bulunmamaktadır."
                    }

                    // Resim URL'sini bulalım
                    var imageUrl = extractImageUrl(itemContent)
                    if (imageUrl.isNullOrEmpty()) {
                        imageUrl = "https://via.placeholder.com/300x200?text=News"
                    }

                    val article = GNewsArticle(
                        title = title,
                        description = cleanDescription,
                        url = link,
                        image = imageUrl
                    )

                    articles.add(article)
                }
            }
        } catch (e: Exception) {
            Log.e("RssService", "Error parsing RSS feed: ${e.message}", e)
        }

        return articles
    }

    private fun cleanAndShortenDescription(description: String): String {
        if (description.isEmpty()) {
            return "Bu haber için açıklama bulunmamaktadır."
        }

        // HTML etiketlerini temizleyelim
        var cleanDesc = description.replace(Regex("<.*?>"), " ")

        // HTML entity'leri decode edelim
        cleanDesc = cleanDesc.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

        // Fazla boşlukları temizleyelim
        cleanDesc = cleanDesc.replace(Regex("\\s+"), " ").trim()

        // Açıklamayı kısaltalım (en fazla 150 karakter)
        if (cleanDesc.length > 150) {
            cleanDesc = cleanDesc.substring(0, 147) + "..."
        }

        return cleanDesc
    }

    private fun extractTag(content: String, tagName: String): String {
        val pattern = Pattern.compile("<$tagName>(.*?)</$tagName>", Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            val result = matcher.group(1)?.trim() ?: ""
            result
        } else {
            ""
        }
    }

    // TechCrunch için özel resim çıkarma
    private fun extractImageUrlFromTechCrunch(itemContent: String): String? {
        // 1. media:content tag'inden resim URL'sini almayı deneyelim
        val mediaPattern = Pattern.compile("<media:content[^>]*url=\"([^\"]+)\"[^>]*/>")
        val mediaMatcher = mediaPattern.matcher(itemContent)
        if (mediaMatcher.find()) {
            val imageUrl = mediaMatcher.group(1)
            Log.d("RssService", "TechCrunch: Media content'ten resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        // 2. content:encoded içindeki img tag'ini kontrol edelim
        val contentEncoded = extractTag(itemContent, "content:encoded")
        if (contentEncoded.isNotEmpty()) {
            val imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>")
            val imgMatcher = imgPattern.matcher(contentEncoded)
            if (imgMatcher.find()) {
                val imageUrl = imgMatcher.group(1)
                Log.d("RssService", "TechCrunch: Content:encoded'dan resim URL'si bulundu: $imageUrl")
                return imageUrl
            }
        }

        // 3. Description içindeki ilk img tag'ini kontrol edelim
        val description = extractTag(itemContent, "description")
        val imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>")
        val imgMatcher = imgPattern.matcher(description)
        if (imgMatcher.find()) {
            val imageUrl = imgMatcher.group(1)
            Log.d("RssService", "TechCrunch: Description'dan resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        return null
    }

    // The Verge için özel resim çıkarma
    private fun extractImageUrlFromVerge(itemContent: String): String? {
        // 1. media:content tag'inden resim URL'sini almayı deneyelim
        val mediaPattern = Pattern.compile("<media:content[^>]*url=\"([^\"]+)\"[^>]*/>")
        val mediaMatcher = mediaPattern.matcher(itemContent)
        if (mediaMatcher.find()) {
            val imageUrl = mediaMatcher.group(1)
            Log.d("RssService", "The Verge: Media content'ten resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        // 2. media:thumbnail tag'ini kontrol edelim
        val thumbnailPattern = Pattern.compile("<media:thumbnail[^>]*url=\"([^\"]+)\"[^>]*/>")
        val thumbnailMatcher = thumbnailPattern.matcher(itemContent)
        if (thumbnailMatcher.find()) {
            val imageUrl = thumbnailMatcher.group(1)
            Log.d("RssService", "The Verge: Media thumbnail'dan resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        // 3. Description içindeki ilk img tag'ini kontrol edelim
        val description = extractTag(itemContent, "description")
        val imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>")
        val imgMatcher = imgPattern.matcher(description)
        if (imgMatcher.find()) {
            val imageUrl = imgMatcher.group(1)
            Log.d("RssService", "The Verge: Description'dan resim URL'si bulundu: $imageUrl")
            return imageUrl
        }

        // 4. content tag'ini kontrol edelim
        val content = extractTag(itemContent, "content")
        if (content.isNotEmpty()) {
            val contentImgMatcher = imgPattern.matcher(content)
            if (contentImgMatcher.find()) {
                val imageUrl = contentImgMatcher.group(1)
                Log.d("RssService", "The Verge: Content'ten resim URL'si bulundu: $imageUrl")
                return imageUrl
            }
        }

        return null
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