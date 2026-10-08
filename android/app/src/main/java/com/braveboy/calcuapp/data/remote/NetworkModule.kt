package com.braveboy.calcuapp.data.remote

import com.braveboy.calcuapp.data.local.datastore.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/v1/"

    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var activeOrgId: String = ""

    @Volatile
    private var activeStoreId: String = ""

    @Volatile
    private var sessionStore: SessionStore? = null

    @Volatile
    private var onUnauthorizedCallback: (() -> Unit)? = null

    fun initialize(sessionStore: SessionStore, onUnauthorized: (() -> Unit)? = null) {
        this.sessionStore = sessionStore
        this.onUnauthorizedCallback = onUnauthorized
    }

    fun setBaseUrl(url: String) {
        val trimmed = url.trim()
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        if (baseUrl != normalized) {
            baseUrl = normalized
            // Invalidate lazy instance when base URL changes
            retrofitInstance = null
            apiServiceInstance = null
        }
    }

    fun getBaseUrl(): String = baseUrl

    fun setTenantContext(orgId: String, storeId: String) {
        activeOrgId = orgId
        activeStoreId = storeId
    }

    fun clearTenantContext() {
        activeOrgId = ""
        activeStoreId = ""
    }

    private val authAndTenantInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        builder.header("Accept", "application/json")

        // Attach Authorization header if authenticated
        val token = sessionStore?.tokenSnapshot() ?: ""
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        // Attach tenant headers if present
        if (activeOrgId.isNotBlank()) {
            builder.header("X-Tenant-ID", activeOrgId)
        }
        if (activeStoreId.isNotBlank()) {
            builder.header("X-Store-ID", activeStoreId)
        }

        val response: Response = chain.proceed(builder.build())

        // Handle 401 Unauthorized: session expired or revoked
        if (response.code == 401) {
            onUnauthorizedCallback?.invoke()
        }

        response
    }

    // Safe logging: Never log Bearer tokens or sensitive headers
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authAndTenantInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var apiServiceInstance: CalcuappApiService? = null

    val apiService: CalcuappApiService
        get() {
            return apiServiceInstance ?: synchronized(this) {
                apiServiceInstance ?: Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(CalcuappApiService::class.java)
                    .also {
                        apiServiceInstance = it
                    }
            }
        }
}
