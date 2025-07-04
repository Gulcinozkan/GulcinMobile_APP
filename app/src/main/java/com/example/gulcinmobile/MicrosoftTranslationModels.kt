package com.example.gulcinmobile.model

data class TranslatedText(
    val text: String,
    val to: String
)

data class TranslationResponseItem(
    val translations: List<TranslatedText>
)
