package com.mongle.android.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * MG-95 토큰/PII 평문 SharedPreferences 저장 제거.
 *
 * iOS 가 Keychain 을 사용하는 것과 동등하게 EncryptedSharedPreferences(AES256) 적용.
 * 분실/루팅 단말에서 평문 추출이 불가능해지며, refresh_token 유출 위험을 차단한다.
 *
 * 마이그레이션:
 * - 첫 호출 시 기존 `mongle_auth` 평문 prefs 의 값을 `mongle_auth_secure` 로 복사 후 평문 prefs.clear().
 * - 키스토어 손상 등으로 EncryptedSharedPreferences 생성이 실패한 경우 평문 prefs 로 fallback (사용성 우선).
 *   이 경우 다음 가용 시점에 다시 시도하도록 마이그레이션 sentinel 도 둠.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    private const val LEGACY_AUTH_PREFS = "mongle_auth"
    private const val SECURE_AUTH_PREFS = "mongle_auth_secure"
    private const val MIGRATION_SENTINEL_KEY = "secure_migration_done"

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthPrefs(@ApplicationContext context: Context): SharedPreferences {
        val secure = createSecurePrefs(context) ?: run {
            Log.e("SecurityModule", "EncryptedSharedPreferences 생성 실패 — 평문 prefs 로 fallback")
            return context.getSharedPreferences(LEGACY_AUTH_PREFS, Context.MODE_PRIVATE)
        }
        migrateLegacyIfNeeded(context, secure)
        return secure
    }

    private fun createSecurePrefs(context: Context): SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_AUTH_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    private fun migrateLegacyIfNeeded(context: Context, secure: SharedPreferences) {
        if (secure.getBoolean(MIGRATION_SENTINEL_KEY, false)) return
        val legacy = context.getSharedPreferences(LEGACY_AUTH_PREFS, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) {
            secure.edit().putBoolean(MIGRATION_SENTINEL_KEY, true).apply()
            return
        }
        val editor = secure.edit()
        for ((k, v) in legacy.all) {
            when (v) {
                is String -> editor.putString(k, v)
                is Boolean -> editor.putBoolean(k, v)
                is Int -> editor.putInt(k, v)
                is Long -> editor.putLong(k, v)
                is Float -> editor.putFloat(k, v)
                else -> Log.w("SecurityModule", "마이그레이션 스킵 — 미지원 타입 key=$k")
            }
        }
        editor.putBoolean(MIGRATION_SENTINEL_KEY, true).apply()
        // 평문 prefs 의 토큰/PII 는 더 이상 신뢰 저장소가 아니므로 즉시 폐기.
        legacy.edit().clear().apply()
        Log.i("SecurityModule", "Legacy mongle_auth → mongle_auth_secure 마이그레이션 완료")
    }
}
