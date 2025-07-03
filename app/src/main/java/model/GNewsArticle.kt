package com.example.gulcinmobile.model

data class GNewsArticle(
    val title: String,
    val description: String?,
    val url: String,
    val image: String?
)

data class GNewsResponse(
    val totalArticles: Int,
    val articles: List<GNewsArticle>
)