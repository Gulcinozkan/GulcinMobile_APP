package com.example.gulcinmobile.network

import retrofit2.http.*
import retrofit2.Call

data class TranslationRequest(
    val Text: String
)

data class TranslationResponseItem(
    val translations: List<TranslatedText>
)

data class TranslatedText(
    val text: String,
    val to: String
)

interface MicrosoftTranslateService {
    @Headers(
        "Content-Type: application/json",
        "Ocp-Apim-Subscription-Key: EtTISMBaiR6jFC2tM2YiozgsJCV6526WNxDRHxjhgcWmTAX8jqxJJQQJ99BGACqBBLyXJ3w3AAAbACOG56Zw",
        "Ocp-Apim-Subscription-Region: southeastasia"
    )
    @POST("translate?api-version=3.0&to=tr")
    fun translate(@Body body: List<TranslationRequest>): Call<List<TranslationResponseItem>>
}
