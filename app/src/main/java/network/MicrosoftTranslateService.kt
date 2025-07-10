package com.example.gulcinmobile.network

import com.example.gulcinmobile.model.TranslationRequest
import com.example.gulcinmobile.model.TranslationResponseItem
import retrofit2.http.*
import retrofit2.Call

interface MicrosoftTranslateService {
    @Headers(
        "Content-Type: application/json",
        "Ocp-Apim-Subscription-Region: southeastasia"
    )
    @POST("translate")
    fun translateDynamic(
        @Header("Ocp-Apim-Subscription-Key") key: String,
        @Query("api-version") apiVersion: String = "3.0",
        @Query("to") toLang: String,
        @Query("from") fromLang: String = "en",
        @Body body: List<TranslationRequest>
    ): Call<List<TranslationResponseItem>>

}