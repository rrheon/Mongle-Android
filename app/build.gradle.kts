import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.ycompany.Monggle"
    compileSdk = 35

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        applicationId = "com.ycompany.Monggle"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://1cq1kfgvf1.execute-api.ap-northeast-2.amazonaws.com/\"")
        buildConfigField("String", "KAKAO_APP_KEY", "\"73b4d3e9a62701280ec877fe441949b3\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"43055125841-in9de5felh4f90rq8vee9lq60uice7uj.apps.googleusercontent.com\"")
        buildConfigField("String", "APPLE_CLIENT_ID", "\"com.mongle.app.signin\"")
        // AdMob 광고 ID — 기본값(디버그 빌드 포함)은 Google 공식 테스트 ID.
        // 실제 광고 ID 는 release buildType 에서만 주입한다 (계정 정지/정책 위반 방지).
        // 테스트 ID 출처: https://developers.google.com/admob/android/test-ads
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                propsFile.inputStream().use { stream -> props.load(stream) }
            }
            val sf = props.getProperty("storeFile")
            if (sf != null) storeFile = file(sf)
            storePassword = props.getProperty("storePassword") ?: ""
            keyAlias = props.getProperty("keyAlias") ?: ""
            keyPassword = props.getProperty("keyPassword") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // release 빌드에서만 실제 AdMob 광고 ID 사용
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-4718464707406824/9365243021\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-4718464707406824/2974225929\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Social Login
    implementation(libs.play.services.auth)
    implementation(libs.kakao.auth)

    // Browser (Custom Tabs for Apple Sign-In)
    implementation(libs.androidx.browser)

    // Google Mobile Ads (AdMob)
    implementation(libs.google.mobile.ads)
    // UMP SDK — GDPR/CCPA 동의 수집 CMP
    implementation(libs.google.user.messaging.platform)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // MG-95 EncryptedSharedPreferences — 토큰/PII 평문 저장 제거
    implementation(libs.androidx.security.crypto)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
