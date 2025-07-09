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

    // AI ile ilgili teknoloji haberlerini çeken fonksiyon
    suspend fun fetchAINews(): GNewsResponse {
        Log.d("RssService", "fetchAINews başladı")
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RssService", "AI haberleri için RSS feed'lerini çekmeye başlıyoruz")

                // Her kaynaktan daha fazla makale çekelim ve sonra AI ile ilgili olanları filtreleyelim
                val techCrunchArticles = try {
                    Log.d("RssService", "TechCrunch feed'ini çekmeye başlıyoruz")
                    fetchFromSource(techCrunchFeedUrl, "TechCrunch")
                } catch (e: Exception) {
                    Log.e("RssService", "TechCrunch feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                var vergeArticles = try {
                    Log.d("RssService", "The Verge feed'ini çekmeye başlıyoruz")
                    fetchFromSource(theVergeFeedUrl, "The Verge")
                } catch (e: Exception) {
                    Log.e("RssService", "The Verge feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                // Eğer The Verge ana URL'den hiç makale çekilemezse, alternatif URL'yi deneyelim
                if (vergeArticles.isEmpty()) {
                    Log.d("RssService", "The Verge alternatif feed'i deneniyor")
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

                Log.d("RssService", "Tüm feed'ler çekildi, AI ile ilgili haberleri filtreliyoruz")

                Log.d("RssService", "Tüm feed'ler çekildi, AI ile ilgili haberleri filtreliyoruz")

                // AI ile ilgili anahtar kelimeleri tanımlayalım
                val aiKeywords = listOf(
                    // Genel AI terimleri - tam kelime eşleşmesi için
                    " ai ", " ai,", " ai.", " ai-", "-ai ", "artificial intelligence", "machine learning",
                    "deep learning", "neural network", "yapay zeka", "makine öğrenmesi", "derin öğrenme",

                    // Spesifik AI modelleri ve şirketler
                    "gpt", "chatgpt", "gpt-4", "gpt-5", "openai", "claude", "anthropic",
                    "gemini", "google ai", "bard", "llama", "meta ai", "mistral ai",
                    "llm", "large language model", "büyük dil modeli", "stable diffusion",

                    // AI teknolojileri ve konseptler - daha spesifik terimler
                    "generative ai", "üretken ai", "ai model", "ai assistant", "ai ethics",
                    "computer vision", "bilgisayarlı görü", "nlp", "natural language processing",
                    "doğal dil işleme", "reinforcement learning", "pekiştirmeli öğrenme",
                    "transformer", "diffusion model", "multimodal", "çok modlu", "ai-powered",

                    // AI uygulamaları - daha spesifik terimler
                    "ai application", "ai tool", "ai image", "ai video", "ai audio",
                    "ai code", "ai coding", "ai programming", "ai development",
                    "autonomous", "otonom", "self-driving", "sürücüsüz", "ai research"
                )

                // Daha güçlü bir filtreleme için, başlıkta veya açıklamada en az bir AI anahtar kelimesi bulunmalı
                // Her kaynaktan AI ile ilgili makaleleri filtreleyelim
                val aiTechCrunchArticles = techCrunchArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""

                    // Başlıkta veya açıklamada en az bir AI anahtar kelimesi bulunmalı
                    aiKeywords.any { keyword ->
                        titleLower.contains(keyword) || descLower.contains(keyword)
                    }
                }

                val aiVergeArticles = vergeArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""

                    // Başlıkta veya açıklamada en az bir AI anahtar kelimesi bulunmalı
                    aiKeywords.any { keyword ->
                        titleLower.contains(keyword) || descLower.contains(keyword)
                    }
                }

                val aiWiredArticles = wiredArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""

                    // Başlıkta veya açıklamada en az bir AI anahtar kelimesi bulunmalı
                    aiKeywords.any { keyword ->
                        titleLower.contains(keyword) || descLower.contains(keyword)
                    }
                }
                Log.d("RssService", "AI ile ilgili makaleler - TechCrunch: ${aiTechCrunchArticles.size}, " +
                        "The Verge: ${aiVergeArticles.size}, Wired: ${aiWiredArticles.size}")

                // İstenen dağılım: 3 TechCrunch, 3 The Verge, 4 Wired (mümkünse)
                val targetTechCrunch = 3
                val targetVerge = 3
                val targetWired = 4

                // Her kaynaktan kaç makale alacağımızı belirleyelim
                val techCrunchCount = minOf(targetTechCrunch, aiTechCrunchArticles.size)
                val vergeCount = minOf(targetVerge, aiVergeArticles.size)
                val wiredCount = minOf(targetWired, aiWiredArticles.size)

                // Makaleleri birleştirelim
                val allAIArticles = mutableListOf<GNewsArticle>()

                // TechCrunch makalelerini ekleyelim
                allAIArticles.addAll(aiTechCrunchArticles.take(techCrunchCount))

                // The Verge makalelerini ekleyelim
                allAIArticles.addAll(aiVergeArticles.take(vergeCount))

                // Wired makalelerini ekleyelim
                allAIArticles.addAll(aiWiredArticles.take(wiredCount))

                // Eğer toplam 10'dan az makale varsa, eksik olanları diğer kaynaklardan tamamlayalım
                val totalArticles = techCrunchCount + vergeCount + wiredCount
                if (totalArticles < 10) {
                    Log.d("RssService", "Toplam $totalArticles AI makalesi bulundu, 10'a tamamlamaya çalışılıyor")

                    // Önce Wired'dan eksikleri tamamlamaya çalışalım
                    if (aiWiredArticles.size > wiredCount) {
                        val extraWired = minOf(10 - totalArticles, aiWiredArticles.size - wiredCount)
                        allAIArticles.addAll(aiWiredArticles.subList(wiredCount, wiredCount + extraWired))
                        Log.d("RssService", "Ek olarak $extraWired Wired AI makalesi eklendi")
                    }

                    // Hala eksikse, TechCrunch'tan tamamlayalım
                    val currentTotal = allAIArticles.size
                    if (currentTotal < 10 && aiTechCrunchArticles.size > techCrunchCount) {
                        val extraTechCrunch = minOf(10 - currentTotal, aiTechCrunchArticles.size - techCrunchCount)
                        allAIArticles.addAll(aiTechCrunchArticles.subList(techCrunchCount, techCrunchCount + extraTechCrunch))
                        Log.d("RssService", "Ek olarak $extraTechCrunch TechCrunch AI makalesi eklendi")
                    }

                    // Hala eksikse, The Verge'den tamamlayalım
                    val updatedTotal = allAIArticles.size
                    if (updatedTotal < 10 && aiVergeArticles.size > vergeCount) {
                        val extraVerge = minOf(10 - updatedTotal, aiVergeArticles.size - vergeCount)
                        allAIArticles.addAll(aiVergeArticles.subList(vergeCount, vergeCount + extraVerge))
                        Log.d("RssService", "Ek olarak $extraVerge The Verge AI makalesi eklendi")
                    }
                }

                Log.d("RssService", "Toplam ${allAIArticles.size} AI makalesi döndürülüyor")

                GNewsResponse(
                    totalArticles = allAIArticles.size,
                    articles = allAIArticles
                )
            } catch (e: Exception) {
                Log.e("RssService", "Error fetching AI news: ${e.message}", e)
                // Hata durumunda boş liste döndür
                GNewsResponse(totalArticles = 0, articles = emptyList())
            }
        }
    }

    // Genel haberleri çeken fonksiyon - doğal afetler ve aktif savaşlarla ilgili haberler
    suspend fun fetchGeneralNews(): GNewsResponse {
        Log.d("RssService", "fetchGeneralNews başladı")
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RssService", "Genel haberler için RSS feed'lerini çekmeye başlıyoruz")

                // Her kaynaktan haberleri çekelim
                val techCrunchArticles = try {
                    Log.d("RssService", "TechCrunch feed'ini çekmeye başlıyoruz")
                    fetchFromSource(techCrunchFeedUrl, "TechCrunch")
                } catch (e: Exception) {
                    Log.e("RssService", "TechCrunch feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                var vergeArticles = try {
                    Log.d("RssService", "The Verge feed'ini çekmeye başlıyoruz")
                    fetchFromSource(theVergeFeedUrl, "The Verge")
                } catch (e: Exception) {
                    Log.e("RssService", "The Verge feed'i çekilemedi: ${e.message}", e)
                    emptyList()
                }

                // Eğer The Verge ana URL'den hiç makale çekilemezse, alternatif URL'yi deneyelim
                if (vergeArticles.isEmpty()) {
                    Log.d("RssService", "The Verge alternatif feed'i deneniyor")
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

                Log.d("RssService", "Tüm feed'ler çekildi, doğal afet ve savaş haberleri filtreleniyor")

                // Diğer kategorilerde kullanılan anahtar kelimeleri tanımlayalım (bunları hariç tutacağız)
                val techKeywords = listOf(
                    "technology", "tech", "software", "hardware", "app", "smartphone", "gadget",
                    "computer", "internet", "digital", "mobile", "device", "innovation"
                )

                val politicalKeywords = listOf(
                    "election", "vote", "campaign", "democrat", "republican", "parliament"
                )

                val sportsKeywords = listOf(
                    "sports", "football", "basketball", "tennis", "olympics", "game", "match",
                    "player", "team", "championship", "tournament", "league", "athlete"
                )

                val businessKeywords = listOf(
                    "business", "economy", "finance", "stock market", "investment", "company",
                    "startup", "entrepreneur", "market", "trade", "industry", "economic"
                )

                val artKeywords = listOf(
                    "art", "exhibition", "museum", "painting", "sculpture", "artist",
                    "gallery", "creative", "design", "culture", "artistic"
                )

                val entertainmentKeywords = listOf(
                    "celebrity", "entertainment", "movie", "music", "fashion", "film",
                    "actor", "actress", "singer", "star", "hollywood", "tv", "show"
                )

                val aiKeywords = listOf(
                    "ai", "artificial intelligence", "machine learning", "deep learning",
                    "neural network", "gpt", "chatgpt", "openai", "claude", "gemini"
                )

                // Doğal afet ve savaş haberleri ile ilgili anahtar kelimeleri tanımlayalım
                val disasterWarKeywords = listOf(
                    // Doğal afetler
                    "disaster", "natural disaster", "earthquake", "flood", "hurricane", "tornado",
                    "tsunami", "wildfire", "forest fire", "volcanic eruption", "volcano", "landslide",
                    "drought", "famine", "extreme weather", "climate disaster", "storm", "typhoon",
                    "cyclone", "blizzard", "avalanche", "mudslide",

                    // Türkçe doğal afet kelimeleri
                    "doğal afet", "deprem", "sel", "kasırga", "hortum", "tsunami", "yangın",
                    "orman yangını", "volkan", "volkanik", "heyelan", "kuraklık", "kıtlık",
                    "aşırı hava", "iklim felaketi", "fırtına", "tayfun", "çığ", "çamur",

                    // Savaşlar ve çatışmalar
                    "war", "conflict", "battle", "fighting", "combat", "military", "attack",
                    "invasion", "troops", "soldier", "army", "navy", "air force", "bombing",
                    "missile", "airstrike", "ceasefire", "peace talks", "hostage", "refugee",
                    "casualties", "killed", "wounded", "dead", "death toll", "violence",
                    "terrorism", "terrorist", "insurgent", "rebellion", "civil war", "genocide",
                    "ethnic cleansing", "humanitarian crisis", "displaced", "evacuation",

                    // Türkçe savaş kelimeleri
                    "savaş", "çatışma", "muharebe", "askeri", "saldırı", "işgal", "asker",
                    "ordu", "donanma", "hava kuvvetleri", "bombalama", "füze", "hava saldırısı",
                    "ateşkes", "barış görüşmeleri", "rehine", "mülteci", "kayıp", "ölü", "yaralı",
                    "can kaybı", "şiddet", "terörizm", "terörist", "isyancı", "iç savaş", "soykırım",
                    "insani kriz", "tahliye",

                    // Özel savaş bölgeleri ve güncel çatışmalar
                    "ukraine", "russia", "gaza", "israel", "palestine", "hamas", "syria", "yemen",
                    "sudan", "myanmar", "ethiopia", "somalia", "iraq", "afghanistan", "libya",
                    "ukrayna", "rusya", "gazze", "israil", "filistin", "suriye", "yemen",
                    "sudan", "myanmar", "etiyopya", "somali", "irak", "afganistan", "libya"
                )

                // Tüm kategori anahtar kelimelerini birleştirelim (hariç tutulacaklar)
                val excludeCategoryKeywords = techKeywords + sportsKeywords +
                        businessKeywords + artKeywords + entertainmentKeywords + aiKeywords

                // Doğal afet ve savaş haberleri ile ilgili makaleleri filtreleyelim
                val disasterWarTechCrunchArticles = techCrunchArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""
                    val contentToSearch = "$titleLower $descLower"

                    // Doğal afet veya savaş ile ilgili en az bir anahtar kelime içermeli
                    disasterWarKeywords.any { keyword -> contentToSearch.contains(keyword) }
                }

                val disasterWarVergeArticles = vergeArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""
                    val contentToSearch = "$titleLower $descLower"

                    disasterWarKeywords.any { keyword -> contentToSearch.contains(keyword) }
                }

                val disasterWarWiredArticles = wiredArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""
                    val contentToSearch = "$titleLower $descLower"

                    disasterWarKeywords.any { keyword -> contentToSearch.contains(keyword) }
                }

                // Eğer yeterli doğal afet ve savaş haberi bulamazsak, genel dünya haberleri de ekleyelim
                val worldNewsKeywords = listOf(
                    "world", "global", "international", "nation", "country", "countries",
                    "europe", "asia", "africa", "america", "australia", "middle east",
                    "united nations", "un", "nato", "eu", "european union", "g7", "g20",
                    "climate", "environment", "crisis", "agreement", "summit", "conference",
                    "pandemic", "epidemic", "emergency", "humanitarian", "migration", "protest",
                    "demonstration", "dünya", "küresel", "uluslararası", "ülke", "ülkeler",
                    "avrupa", "asya", "afrika"
                )

                val worldTechCrunchArticles = techCrunchArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""
                    val contentToSearch = "$titleLower $descLower"

                    // Dünya haberleri ile ilgili en az bir anahtar kelime içermeli
                    // VE doğal afet/savaş listesinde olmamalı
                    worldNewsKeywords.any { keyword -> contentToSearch.contains(keyword) } &&
                            !disasterWarTechCrunchArticles.contains(article) &&
                            // Diğer kategorilerin anahtar kelimelerini içermemeli (politik haberler hariç)
                            !excludeCategoryKeywords.any { keyword -> contentToSearch.contains(keyword) }
                }

                val worldVergeArticles = vergeArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""
                    val contentToSearch = "$titleLower $descLower"

                    worldNewsKeywords.any { keyword -> contentToSearch.contains(keyword) } &&
                            !disasterWarVergeArticles.contains(article) &&
                            !excludeCategoryKeywords.any { keyword -> contentToSearch.contains(keyword) }
                }

                val worldWiredArticles = wiredArticles.filter { article ->
                    val titleLower = article.title?.lowercase() ?: ""
                    val descLower = article.description?.lowercase() ?: ""
                    val contentToSearch = "$titleLower $descLower"

                    worldNewsKeywords.any { keyword -> contentToSearch.contains(keyword) } &&
                            !disasterWarWiredArticles.contains(article) &&
                            !excludeCategoryKeywords.any { keyword -> contentToSearch.contains(keyword) }
                }

                Log.d("RssService", "Doğal afet ve savaş haberleri - TechCrunch: ${disasterWarTechCrunchArticles.size}, " +
                        "The Verge: ${disasterWarVergeArticles.size}, Wired: ${disasterWarWiredArticles.size}")
                Log.d("RssService", "Dünya haberleri - TechCrunch: ${worldTechCrunchArticles.size}, " +
                        "The Verge: ${worldVergeArticles.size}, Wired: ${worldWiredArticles.size}")

                // İstenen dağılım: 3 TechCrunch, 3 The Verge, 4 Wired (mümkünse)
                val targetTechCrunch = 3
                val targetVerge = 3
                val targetWired = 4

                // Önce doğal afet ve savaş haberleri ile ilgili olanları ekleyelim
                val allGeneralArticles = mutableListOf<GNewsArticle>()

                // TechCrunch doğal afet ve savaş haberleri
                val disasterWarTechCrunchCount = minOf(targetTechCrunch, disasterWarTechCrunchArticles.size)
                allGeneralArticles.addAll(disasterWarTechCrunchArticles.take(disasterWarTechCrunchCount))

                // The Verge doğal afet ve savaş haberleri
                val disasterWarVergeCount = minOf(targetVerge, disasterWarVergeArticles.size)
                allGeneralArticles.addAll(disasterWarVergeArticles.take(disasterWarVergeCount))

                // Wired doğal afet ve savaş haberleri
                val disasterWarWiredCount = minOf(targetWired, disasterWarWiredArticles.size)
                allGeneralArticles.addAll(disasterWarWiredArticles.take(disasterWarWiredCount))

                // Eğer doğal afet ve savaş haberleri yeterli değilse, dünya haberleriyle tamamlayalım
                val remainingTechCrunch = targetTechCrunch - disasterWarTechCrunchCount
                if (remainingTechCrunch > 0) {
                    allGeneralArticles.addAll(worldTechCrunchArticles.take(remainingTechCrunch))
                }

                val remainingVerge = targetVerge - disasterWarVergeCount
                if (remainingVerge > 0) {
                    allGeneralArticles.addAll(worldVergeArticles.take(remainingVerge))
                }

                val remainingWired = targetWired - disasterWarWiredCount
                if (remainingWired > 0) {
                    allGeneralArticles.addAll(worldWiredArticles.take(remainingWired))
                }

                // Eğer toplam 10'dan az makale varsa, eksik olanları diğer kaynaklardan tamamlayalım
                val totalArticles = allGeneralArticles.size
                if (totalArticles < 10) {
                    Log.d("RssService", "Toplam $totalArticles genel makale bulundu, 10'a tamamlamaya çalışılıyor")

                    // Yeterli haber bulunamadıysa, filtrelemeyi biraz gevşetelim
                    // ve daha az kategori anahtar kelimesi içeren haberleri alalım
                    val remainingArticles = (techCrunchArticles + vergeArticles + wiredArticles).filter { article ->
                        !allGeneralArticles.any { it.title == article.title }
                        // Teknoloji, spor, sanat ve eğlence haberlerini hariç tutalım
                        val titleLower = article.title?.lowercase() ?: ""
                        val descLower = article.description?.lowercase() ?: ""
                        val contentToSearch = "$titleLower $descLower"

                        !excludeCategoryKeywords.any { keyword -> contentToSearch.contains(keyword) }
                    }.take(10 - totalArticles)

                    // Kalan makaleleri ekleyelim
                    allGeneralArticles.addAll(remainingArticles)
                }

                Log.d("RssService", "Toplam ${allGeneralArticles.size} genel makale döndürülüyor")

                GNewsResponse(
                    totalArticles = allGeneralArticles.size,
                    articles = allGeneralArticles
                )
            } catch (e: Exception) {
                Log.e("RssService", "Genel haberler çekilirken hata oluştu: ${e.message}", e)
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