package com.mongle.android.domain.model

enum class SocialProviderType(val value: String) {
    APPLE("apple"),
    KAKAO("kakao"),
    GOOGLE("google")
}

/**
 * 소셜 로그인 자격증명 인터페이스.
 * 새로운 소셜 로그인 제공자 추가 시 이 인터페이스를 구현하는 클래스만 추가하면 됩니다.
 */
interface SocialLoginCredential {
    val providerType: SocialProviderType

    /**
     * 서버에 전송할 제공자별 페이로드 (key-value)
     * - Apple: identity_token, authorization_code, name?, email?
     * - Kakao: access_token, name?, email?
     * - Google: id_token, name?, email?
     */
    val fields: Map<String, String>
}

data class KakaoLoginCredential(
    val accessToken: String,
    val name: String? = null,
    val email: String? = null
) : SocialLoginCredential {
    override val providerType = SocialProviderType.KAKAO
    override val fields: Map<String, String> = buildMap {
        put("access_token", accessToken)
        name?.let { put("name", it) }
        email?.let { put("email", it) }
    }
}

data class GoogleLoginCredential(
    val idToken: String,
    val name: String? = null,
    val email: String? = null
) : SocialLoginCredential {
    override val providerType = SocialProviderType.GOOGLE
    override val fields: Map<String, String> = buildMap {
        put("id_token", idToken)
        name?.let { put("name", it) }
        email?.let { put("email", it) }
    }
}

data class AppleLoginCredential(
    val identityToken: String,
    val authorizationCode: String,
    val name: String? = null,
    val email: String? = null
) : SocialLoginCredential {
    override val providerType = SocialProviderType.APPLE
    override val fields: Map<String, String> = buildMap {
        put("identity_token", identityToken)
        put("authorization_code", authorizationCode)
        name?.let { put("name", it) }
        email?.let { put("email", it) }
    }
}
