package com.mongle.android.data.remote

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val KEY_TOKEN = "auth_token"

@Singleton
class AuthInterceptor @Inject constructor(
    // MG-95 EncryptedSharedPreferences 인스턴스를 SecurityModule 에서 주입.
    @Named("auth") private val authPrefs: SharedPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authPrefs.getString(KEY_TOKEN, null)

        val lang = when (Locale.getDefault().language) {
            "ko" -> "ko"
            "ja" -> "ja"
            else -> "en"
        }

        val request = chain.request().newBuilder()
            .addHeader("Accept-Language", lang)
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()

        return chain.proceed(request)
    }
}
