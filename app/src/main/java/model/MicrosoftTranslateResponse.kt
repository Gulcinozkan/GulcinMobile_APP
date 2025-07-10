package com.example.gulcinmobile.model

data class MicrosoftTranslation(
    val text: String,
    val to: String
)

data class MicrosoftTranslateResponse(
    val translations: List<MicrosoftTranslation>
)


