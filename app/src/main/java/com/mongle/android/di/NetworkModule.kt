package com.mongle.android.di

import com.mongle.android.data.remote.AuthInterceptor
import com.mongle.android.data.remote.MongleApiService
import com.mongle.android.data.remote.TokenAuthenticator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ycompany.Monggle.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = OkHttpClient.Builder()
        // MG-94 약전계/핸드오프/서버 SIGTERM 직전 body stream hang 시 코루틴 무한 대기 방지.
        // OkHttp 기본 callTimeout 은 0(무제한) — 명시 상한 필요.
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(
            // MG-105 BODY 레벨은 EmailLoginBody/AuthResponse 평문 logcat 노출.
            // HEADERS 레벨로 낮추고 Authorization/Cookie redact.
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
                redactHeader("Cookie")
            }
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideMongleApiService(retrofit: Retrofit): MongleApiService =
        retrofit.create(MongleApiService::class.java)
}
