package ro.banking40.team01.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface OnboardingApi {
    @POST("applications")
    suspend fun create(@Body body: CreateApplicationRequest): CreateApplicationResponse

    @POST("applications/{id}/claim")
    suspend fun claim(@Path("id") id: String, @Body body: ClaimRequest): StatusResponse

    @GET("applications/{id}/status")
    suspend fun status(@Path("id") id: String): StatusResponse

    @GET("applications/{id}/review")
    suspend fun review(@Path("id") id: String): ReviewResponse

    @POST("applications/{id}/confirm")
    suspend fun confirm(@Path("id") id: String, @Body body: ConfirmRequest): ConfirmResponse

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        /** No HTTP logging on purpose: responses carry personal data. */
        fun create(baseUrl: String): OnboardingApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OnboardingApi::class.java)
        }
    }
}

/** The backend's `{"error": "..."}` code, or null for network failures and unparseable bodies. */
fun Throwable.backendErrorCode(): String? {
    val body = (this as? HttpException)?.response()?.errorBody()?.string() ?: return null
    return runCatching { OnboardingApi.json.decodeFromString<ErrorResponse>(body).error }.getOrNull()
}
