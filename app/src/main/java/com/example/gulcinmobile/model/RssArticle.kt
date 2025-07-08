package com.example.gulcinmobile.model

data class RssArticle(
    val title: String,
    val description: String?,
    val url: String,
    val image: String?,
    val source: String, // Haberin kaynağı (TechCrunch, The Verge, Wired)
    val publishDate: String?
)

// GNewsArticle formatına dönüştürme fonksiyonu
fun RssArticle.toGNewsArticle(): GNewsArticle {
    return GNewsArticle(
        title = "$title - $source", // Başlığa kaynak bilgisini ekleyelim
        description = description,
        url = url,
        image = image
    )
}

// RSS makalelerini içeren yanıt sınıfı
data class RssResponse(
    val articles: List<RssArticle>
)
