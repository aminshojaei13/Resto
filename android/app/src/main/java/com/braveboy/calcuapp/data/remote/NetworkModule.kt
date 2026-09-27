package com.braveboy.calcuapp.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL = "http://10.0.2.2:8000/api/v1/" // 10.0.2.2 for Android emulator host loopback

    @Volatile
    private var activeOrgId: String = ""

    @Volatile
    private var activeStoreId: String = ""

    fun setTenantContext(orgId: String, storeId: String) {
        activeOrgId = orgId
        activeStoreId = storeId
    }

    private val tenantInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("X-Tenant-ID", activeOrgId)
            .header("X-Store-ID", activeStoreId)
            .header("Accept", "application/json")
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(tenantInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: CalcuappApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CalcuappApiService::class.java)
    }
}
