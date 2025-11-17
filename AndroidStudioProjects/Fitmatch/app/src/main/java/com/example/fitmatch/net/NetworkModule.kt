package com.example.fitmatch.net

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL =
        "https://fitmatch-ml-api-1014929179927.africa-south1.run.app/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: FitmatchApi = retrofit.create(FitmatchApi::class.java)
}



//object NetworkModule {
//    private val logging = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//
//    private val client = OkHttpClient.Builder()
//        .addInterceptor(logging)
//        .readTimeout(30, TimeUnit.SECONDS)
//        .connectTimeout(20, TimeUnit.SECONDS)
//        .build()
//
//    private val moshi = Moshi.Builder()
//        .add(KotlinJsonAdapterFactory())
//        .build()
//
//    val api: FitmatchApi by lazy {
//        Retrofit.Builder()
//            .baseUrl(BuildConfig.FITMATCH_API_BASE) // must end with /
//            .addConverterFactory(MoshiConverterFactory.create(moshi))
//            .client(client)
//            .build()
//            .create(FitmatchApi::class.java)
//    }
//}