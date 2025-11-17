package com.example.fitmatch.net

import com.example.fitmatch.data.ExerciseDTO
import com.example.fitmatch.data.PlanDTO
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class PredictBody(
    @Json(name = "user_id") val userId: String,
    // Retrofit/Moshi likes @JvmSuppressWildcards for Map<String, Any>
    val features: Map<String, @JvmSuppressWildcards Any>,
    @Json(name = "week_index") val weekIndex: Int? = 0,
    @Json(name = "variant_seed") val variantSeed: Int? = null
)



@JsonClass(generateAdapter = true)
data class PredictResponse(
    val prediction: Double,
    @Json(name = "plan_id") val planId: String,
    @Json(name = "microcycle_days") val microcycleDays: Int,
    val exercises: List<ExerciseDto>,
    val notes: String,
    @Json(name = "model_version") val modelVersion: String? = null // server may omit; keep optional
)

@JsonClass(generateAdapter = true)
data class ExerciseDto(
    val day: Int,
    val block: String,
    val name: String,
    val sets: Int,
    val reps: String,
    val tempo: String,
    @Json(name = "rest_sec") val restSec: Int
)


interface FitmatchApi {
    @POST("predict")
    suspend fun predict(@Body body: PredictBody): PlanDTO
}
