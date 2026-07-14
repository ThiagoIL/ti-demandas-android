package com.example.data.api

import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("demandas")
    suspend fun getDemandas(@Header("Authorization") token: String): Response<List<Demand>>

    @POST("demandas")
    suspend fun createDemanda(
        @Header("Authorization") token: String,
        @Body demanda: Demand
    ): Response<Demand>

    @PUT("demandas/{id}")
    suspend fun updateDemanda(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body demanda: Demand
    ): Response<Demand>

    @DELETE("demandas/{id}")
    suspend fun deleteDemanda(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>

    companion object {
        private const val BASE_URL = "https://system.tipmp.com.br/api/"

        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ApiService::class.java)
        }
    }
}
