package com.example.gulcinmobile.network

import com.example.gulcinmobile.model.TranslationRequest
import com.example.gulcinmobile.model.TranslationResponseItem
import retrofit2.http.*
import retrofit2.Call

interface MicrosoftTranslateService {
    @Headers("Content-Type: application/json")
    @POST("translate?api-version=3.0")
    fun translateDynamic(
        @Header("Ocp-Apim-Subscription-Key") key: String = "EtTISMBaiR6jFC2tM2YiozgsJCV6526WNxDRHxjhgcWmTAX8jqxJJQQJ99BGACqBBLyXJ3w3AAAbACOG56Zw",
        @Query("to") toLang: String,
        @Body body: List<TranslationRequest>
    ): Call<List<TranslationResponseItem>>
}
