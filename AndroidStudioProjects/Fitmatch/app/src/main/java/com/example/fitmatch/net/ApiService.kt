package com.example.fitmatch.net

import com.example.fitmatch.data.PlanDTO
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class PredictBody(
    val user_id: String,
    val features: Map<String, Any>
)

interface FitmatchApi {
    @POST("predict")
    suspend fun predict(@Body body: PredictBody): PlanDTO
}
