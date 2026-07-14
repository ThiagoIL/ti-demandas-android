package com.example.data.api

import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 1. CONFIGURAÇÃO GLOBAL DA API
 * Mantém a URL base oficial conforme especificado.
 */
object ApiConfig {
    const val BASE_URL = "https://system.tipmp.com.br/"
}

/**
 * 2. MODELOS DE DADOS PARA REQUISIÇÕES E RESPOSTAS (Conforme documentação)
 */

// --- 1. Autenticação e Perfil ---
@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String? = null,
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val theme: String? = null,
    val message: String? = null
) {
    fun getJwtToken(): String? {
        return token
    }
}

@JsonClass(generateAdapter = true)
data class ApiUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val theme: String? = null,
    val active: Int? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UserProfileResponse(
    val success: Boolean,
    val user: ApiUser
)

// Modelo local exposto para a UI do app de forma compatível
data class UserProfile(
    val id: Int,
    val nome: String,
    val email: String,
    val nivel: String,
    val ativo: Boolean,
    val theme: String? = null
)

// --- 2. Estatísticas do Painel ---
@JsonClass(generateAdapter = true)
data class StatusStat(
    val status: String,
    val quantidade: Int
)

@JsonClass(generateAdapter = true)
data class DashboardStats(
    val total: Int,
    val concluidos: Int,
    val pendentes: Int,
    val alta_prioridade: Int? = null,
    val normal: Int? = null,
    val sem_prioridade: Int? = null,
    val por_status: List<StatusStat>? = null
)

// --- 3. Gerenciamento de Demandas ---
@JsonClass(generateAdapter = true)
data class ApiDemand(
    val id: Int,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val userId: Int? = null,
    val created_by_name: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateDemandRequest(
    val title: String,
    val description: String,
    val priority: String
)

@JsonClass(generateAdapter = true)
data class CreateDemandResponse(
    val success: Boolean,
    val id: Int?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class UpdateDemandRequest(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null
)

@JsonClass(generateAdapter = true)
data class SimpleResponse(
    val success: Boolean,
    val message: String?
)

// --- 4. Controle de Equipe ---
@JsonClass(generateAdapter = true)
data class ApiAdminUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val active: Int,
    val theme: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class CreateUserResponse(
    val success: Boolean,
    val id: Int?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val name: String? = null,
    val role: String? = null,
    val active: Int? = null
)

// --- 5. Histórico de Auditoria ---
@JsonClass(generateAdapter = true)
data class ApiAuditLog(
    val id: Int,
    val userName: String,
    val action: String,
    val targetType: String? = null,
    val details: String? = null,
    val created_at: String? = null
)

/**
 * 3. INTERCEPTADOR DE AUTENTICAÇÃO
 * Injeta o token salvo no cabeçalho Authorization, garante formato JSON e trata erros de autenticação (401).
 */
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

        if (response.code == 401) {
            secureSessionManager.clearToken()
            onUnauthorized()
        }

        return response
    }
}

/**
 * 4. INTERFACE DE ROTAS DA API COM RETROFIT
 */
interface TIDemandasApiService {

    // 1. Autenticação e Perfil
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/usuario/perfil")
    suspend fun getPerfil(): UserProfileResponse

    // 2. Estatísticas do Painel
    @GET("api/stats")
    suspend fun getStats(): DashboardStats

    // 3. Gerenciamento de Demandas
    @GET("api/demands")
    suspend fun getDemands(
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null
    ): List<ApiDemand>

    @POST("api/demands")
    suspend fun createDemand(@Body request: CreateDemandRequest): CreateDemandResponse

    @PUT("api/demands/{id}")
    suspend fun updateDemand(
        @Path("id") id: Int,
        @Body request: UpdateDemandRequest
    ): SimpleResponse

    @DELETE("api/demands/{id}")
    suspend fun deleteDemand(@Path("id") id: Int): SimpleResponse

    // 4. Controle de Equipe
    @GET("api/admin/users")
    suspend fun getAdminUsers(): List<ApiAdminUser>

    @POST("api/admin/users")
    suspend fun createAdminUser(@Body request: CreateUserRequest): CreateUserResponse

    @PUT("api/admin/users/{id}")
    suspend fun updateAdminUser(
        @Path("id") id: Int,
        @Body request: UpdateUserRequest
    ): SimpleResponse

    // 5. Histórico de Auditoria
    @GET("api/admin/logs")
    suspend fun getAdminLogs(): List<ApiAuditLog>
}

/**
 * 5. CONSTRUTOR GLOBAL DO CLIENTE RETROFIT
 */
object ApiClient {
    
    fun createService(
        secureSessionManager: SecureSessionManager,
        onUnauthorized: () -> Unit
    ): TIDemandasApiService {
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(secureSessionManager, onUnauthorized))
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        return retrofit.create(TIDemandasApiService::class.java)
    }
}
