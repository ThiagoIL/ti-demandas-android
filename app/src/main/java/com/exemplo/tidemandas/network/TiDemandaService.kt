package com.exemplo.tidemandas.network

import com.example.data.api.SecureSessionManager
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

// ==============================================================================
// 3. DATA CLASSES / MODELOS DE DADOS COMPATÍVEIS
// ==============================================================================

@JsonClass(generateAdapter = true)
data class BaseResponse(
    val success: Boolean,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val theme: String?
)

@JsonClass(generateAdapter = true)
data class PerfilResponse(
    val success: Boolean,
    val user: User
)

@JsonClass(generateAdapter = true)
data class ThemeRequest(
    val theme: String
)

@JsonClass(generateAdapter = true)
data class ThemeResponse(
    val success: Boolean,
    val theme: String?
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordResponse(
    val message: String?,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class Demand(
    val id: Int,
    val name: String,
    val description: String?,
    val priority: Int,
    val done: Int,
    val sort_order: Int?,
    val file_url: String?,
    val file_name: String?,
    val files: String?,
    val created_at: String,
    val updated_at: String?
)

@JsonClass(generateAdapter = true)
data class DemandFileRequest(
    val name: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class CreateDemandRequest(
    val name: String,
    val description: String,
    val priority: Int,
    val files: List<DemandFileRequest>? = null,
    val status: String? = null,
    val done: Int? = null
)

@JsonClass(generateAdapter = true)
data class StatusRequest(
    val status: String, // "pendente" | "concluido"
    val done: Int? = null,
    @com.squareup.moshi.Json(name = "is_done") val isDone: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PriorityRequest(
    val priority: String
)

@JsonClass(generateAdapter = true)
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String, // "master" | "colaborador"
    val active: Int, // 1 para ativo, 0 para desabilitado
    val theme: String?,
    val created_at: String?
)

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class EditUserRequest(
    val name: String,
    val email: String,
    val role: String,
    val active: Int,
    val password: String? = null
)

@JsonClass(generateAdapter = true)
data class ActiveRequest(
    val active: Int
)

@JsonClass(generateAdapter = true)
data class ReorderItem(
    val id: Int,
    val sort_order: Int
)

@JsonClass(generateAdapter = true)
data class ReorderDemandsRequest(
    val orders: List<ReorderItem>
)

@JsonClass(generateAdapter = true)
data class ReorderDemandsResponse(
    val success: Boolean
)

@JsonClass(generateAdapter = true)
data class AuditLog(
    val id: Int,
    val user_id: Int?,
    val user_name: String?,
    val action: String, // "LOGIN_SUCCESS", "CREATE_DEMAND", etc.
    val module: String? = null,
    val target_type: String? = null,
    val target_id: Int?,
    val details: String?,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class StatsResponse(
    val summary: StatsSummary,
    val history: List<HistoryItem>?
)

@JsonClass(generateAdapter = true)
data class StatsSummary(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val high: Int,
    val normal: Int,
    val none: Int,
    val total_prio: Int?,
    val completed_prio: Int?,
    val pending_prio: Int?,
    val high_pending: Int?,
    val normal_pending: Int?,
    val none_pending: Int?
)

@JsonClass(generateAdapter = true)
data class HistoryItem(
    val date: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class UploadResponse(
    val fileUrl: String,
    val fileName: String
)

@JsonClass(generateAdapter = true)
data class DeleteUploadRequest(
    val fileUrl: String
)

@JsonClass(generateAdapter = true)
data class DeleteUploadResponse(
    val success: Boolean,
    val message: String
)

// ==============================================================================
// 1. ARQUITETURA DE REDE E AUTENTICAÇÃO SEGURA (INTERCEPTOR)
// ==============================================================================

class AuthInterceptor(
    private val secureSessionManager: SecureSessionManager,
    private val onUnauthorized: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        requestBuilder.header("Accept", "application/json")
        requestBuilder.header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:100.0) Gecko/100.0 Firefox/100.0")

        val token = secureSessionManager.getToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Captura retornos HTTP 401 (Unauthorized) globalmente
        if (response.code == 401) {
            secureSessionManager.clearToken()
            onUnauthorized()
        }

        return response
    }
}

// ==============================================================================
// 2. INTERFACE RETROFIT
// ==============================================================================

interface TiDemandaService {
    // A) AUTENTICAÇÃO
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/logout")
    suspend fun logout(): BaseResponse

    @GET("api/usuario/perfil")
    suspend fun getPerfil(): PerfilResponse

    @PUT("api/auth/theme")
    suspend fun updateTheme(@Body request: ThemeRequest): ThemeResponse

    @POST("api/users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordResponse

    // B) DASHBOARD & METRICAS
    @GET("api/stats")
    suspend fun getStats(): StatsResponse

    // C) DEMANDAS (CRUD COMPLETO)
    @GET("api/demands")
    suspend fun getDemands(@Query("search") search: String? = null): List<Demand>

    @GET("api/demands/{id}")
    suspend fun getDemandById(@Path("id") id: Int): Demand

    @POST("api/demands")
    suspend fun createDemand(@Body demand: CreateDemandRequest): Demand

    @PUT("api/demands/{id}")
    suspend fun updateDemand(@Path("id") id: Int, @Body demand: CreateDemandRequest): Demand

    @DELETE("api/demands/{id}")
    suspend fun deleteDemand(@Path("id") id: Int): BaseResponse

    @PUT("api/demands/{id}/status")
    suspend fun updateDemandStatus(@Path("id") id: Int, @Body request: StatusRequest): BaseResponse

    @PUT("api/demands/{id}/priority")
    suspend fun updateDemandPriority(@Path("id") id: Int, @Body request: PriorityRequest): BaseResponse

    @PUT("api/demands/reorder")
    suspend fun reorderDemands(@Body request: ReorderDemandsRequest): ReorderDemandsResponse

    // D) CONTROLE DE USUÁRIOS / EQUIPE (Apenas Admin/Master)
    @GET("api/users")
    suspend fun getUsers(): List<User>

    @POST("api/users")
    suspend fun createUser(@Body request: CreateUserRequest): User

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body request: EditUserRequest): User

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): BaseResponse

    // E) AUDITORIA & HISTÓRICO DE ATIVIDADES
    @GET("api/audit")
    suspend fun getAuditLogs(): List<AuditLog>

    // F) UPLOAD DE ARQUIVOS / ANEXOS
    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploadResponse

    @POST("api/upload/delete")
    suspend fun deleteFile(@Body request: DeleteUploadRequest): DeleteUploadResponse
}

// ==============================================================================
// CONSTRUTOR DO CLIENTE RETROFIT
// ==============================================================================

object ApiClient {
    private const val BASE_URL = "https://system.tipmp.com.br/"

    fun createService(
        secureSessionManager: SecureSessionManager,
        onUnauthorized: () -> Unit
    ): TiDemandaService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(secureSessionManager, onUnauthorized))
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        return retrofit.create(TiDemandaService::class.java)
    }
}
