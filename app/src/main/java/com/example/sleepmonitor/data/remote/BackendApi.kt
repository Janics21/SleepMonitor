package com.example.sleepmonitor.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BackendApi {
    @POST("users")
    suspend fun createUser(@Body payload: UserPayload)

    @PUT("users/{userId}")
    suspend fun updateUser(@Path("userId") userId: String, @Body payload: UserPayload)

    @DELETE("users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String)

    @POST("sessions")
    suspend fun createSession(@Body payload: SleepSessionPayload)

    @PUT("sessions/{sessionId}")
    suspend fun updateSession(@Path("sessionId") sessionId: String, @Body payload: SleepSessionPayload)

    @GET("users/{userId}/recommendations")
    suspend fun getRecommendations(@Path("userId") userId: String): List<RecommendationPayload>

    @POST("recommendations")
    suspend fun createRecommendation(@Body payload: RecommendationPayload)
}

object BackendApiFactory {
    private const val DEFAULT_BASE_URL = "https://example.sleepmonitor.api/"

    fun create(baseUrl: String = DEFAULT_BASE_URL): BackendApi {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
