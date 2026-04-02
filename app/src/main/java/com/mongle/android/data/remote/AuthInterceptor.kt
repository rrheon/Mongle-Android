package com.mongle.android.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_NAME = "mongle_auth"
private const val KEY_TOKEN = "auth_token"

@Singleton
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

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
