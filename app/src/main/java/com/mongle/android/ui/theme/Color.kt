package com.mongle.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

// 디자인(MongleUI)이 Display P3 색공간 기준이라 Compose 기본 sRGB로 해석하면
// iPhone 11 등 구형 디스플레이 및 일반 Android 단말에서 채도가 과장되어 진하게 보인다.
// ARGB Long 값을 P3 색공간 좌표로 해석하도록 래핑해 iOS와 동일한 색역을 사용한다.
fun pastelColor(argb: Long): Color {
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(red = r, green = g, blue = b, alpha = a, colorSpace = ColorSpaces.DisplayP3)
}

// MARK: - Primary (iOS 기준: #4CAF50 계열)
val MonglePrimary = pastelColor(0xFF4CAF50)
val MonglePrimaryDark = pastelColor(0xFF388E3C)
val MonglePrimaryLight = pastelColor(0xFFA5D6A7)
val MonglePrimaryLightDark = pastelColor(0xFFC2E8D4)
val MonglePrimaryDarker = pastelColor(0xFF43A047)
val MonglePrimaryDarkerDark = pastelColor(0xFF6BBF93)
val MonglePrimarySoft = pastelColor(0xFF43A047)
val MonglePrimarySoftDark = pastelColor(0xFF6BBF93)
val MonglePrimaryGradientStart = pastelColor(0xFF6BBF93)
val MonglePrimaryGradientEnd = pastelColor(0xFF7BC8A0)
val MonglePrimaryXLight = pastelColor(0xFFC2E8D4)
val MonglePrimaryMuted = pastelColor(0xFF5BAF85)
val MonglePrimaryDeep = pastelColor(0xFF2E7D32)

// MARK: - Secondary / warm accent
val MongleSecondary = pastelColor(0xFFFF7043)

// MARK: - Social Login
val MongleKakao = pastelColor(0xFFFEE500)
val MongleKakaoText = pastelColor(0xFF191919)
val MongleNaver = pastelColor(0xFF03C75A)
val MongleNaverText = pastelColor(0xFFFFFFFF)
val MongleAppleLight = pastelColor(0xFF000000)
val MongleAppleDark = pastelColor(0xFFFFFFFF)
val MongleAppleTextLight = pastelColor(0xFFFFFFFF)
val MongleAppleTextDark = pastelColor(0xFF000000)

// MARK: - Background (iOS 기준: #F8FAF8)
val MongleBackgroundLight = pastelColor(0xFFF8FAF8)
val MongleBackgroundDark = pastelColor(0xFF1A120D)
val MongleSurfaceLight = pastelColor(0xFFFDF8F5)
val MongleSurfaceDark = pastelColor(0xFF1E1A16)
val MongleBgNeutral = pastelColor(0xFFF5F4F1)
val MongleBgCreamy = pastelColor(0xFFFFFCF8)
val MongleBgWarm = pastelColor(0xFFFFF0E6)
val MongleBgNeutralWarm = pastelColor(0xFFF3EFEA)
val MongleBgInfoLight = pastelColor(0xFFE8F2FD)
val MongleBgSuccessLight = pastelColor(0xFFE8F6EA)
val MongleBgWarmLight = pastelColor(0xFFFFF1E2)
val MongleBgMintLight = pastelColor(0xFFEAF7EE)
val MongleBgErrorLight = pastelColor(0xFFFCEEEF)
val MongleBgDanger = pastelColor(0xFFFDE8E8)
val MongleBgErrorSoft = pastelColor(0xFFFDEBEC)
val MongleBgYellowSoft = pastelColor(0xFFFFF1DE)
val MongleBgPeach = pastelColor(0xFFFFE5D9)

// App background gradient (iOS 기준)
val MongleGradientBgStart = pastelColor(0xFFFFF8F0)
val MongleGradientBgMid = pastelColor(0xFFFFF2EB)
val MongleGradientBgEnd = pastelColor(0xFFEFF8F1)

// MARK: - Card
val MongleCardBackgroundLight = pastelColor(0xCCFFFFFF) // white 0.8 opacity (iOS 기준)
val MongleCardBackgroundDark = pastelColor(0xFF1E1E1E)
val MongleCardBackgroundSolid = pastelColor(0xFFFFFFFF)
val MongleCardGlass = pastelColor(0x99FFFFFF) // white 0.6 opacity
val MongleCardHighlightLight = pastelColor(0xFFEDF7F0)
val MongleCardHighlightDark = pastelColor(0xFF1E3A2A)

// MARK: - Border
val MongleBorder = pastelColor(0xFFE0E0E0)
val MongleBorderWarm = pastelColor(0xFFEEE3D8)
val MongleDividerLight = pastelColor(0xFFE0E0E0)
val MongleDividerDark = pastelColor(0xFF2E2E2E)

// MARK: - Text
val MongleTextPrimary = pastelColor(0xFF1A1A1A)
val MongleTextSecondary = pastelColor(0xFF6D6D6D)
val MongleTextHint = pastelColor(0xFF9E9E9E)
val MongleTextOnPrimary = pastelColor(0xFFFFFFFF)

// MARK: - Status (iOS 기준: success = #4CAF50)
val MongleError = pastelColor(0xFFF44336)
val MongleWarning = pastelColor(0xFFFF9800)
val MongleSuccessLight = pastelColor(0xFF4CAF50)
val MongleSuccessDark = pastelColor(0xFF66BB6A)
val MongleInfo = pastelColor(0xFF42A5F5)
val MongleNotificationDot = pastelColor(0xFFF44336)

// MARK: - Monggle characters (파스텔톤 — iOS 디자인 기준)
val MongleMonggleGreenLight = pastelColor(0xFF8FD5A6)
val MongleMonggleGreenDark = pastelColor(0xFFA5E0C0)
val MongleMonggleYellow = pastelColor(0xFFFFD54F)
val MongleMonggleBlue = pastelColor(0xFF42A5F5)
val MongleMongglePink = pastelColor(0xFFF06292)
val MongleMonggleOrange = pastelColor(0xFFFF9800)

// MARK: - Mood
val MongleMoodHappy = pastelColor(0xFFFFD54F)
val MongleMoodHappyLight = pastelColor(0xFFFFF3C4)
val MongleMoodLoved = pastelColor(0xFFF5978E)
val MongleMoodLovedLight = pastelColor(0xFFFDDDD8)
val MongleMoodCalm = pastelColor(0xFFA8DFBC)
val MongleMoodCalmLight = pastelColor(0xFFD4F0E0)
val MongleMoodSad = pastelColor(0xFF90CAF9)
val MongleMoodSadLight = pastelColor(0xFFD4EAFC)
val MongleMoodAngry = pastelColor(0xFFEF9A9A)
val MongleMoodAngryLight = pastelColor(0xFFFDDEDE)
val MongleMoodAnxious = pastelColor(0xFFA5D6A7)
val MongleMoodAnxiousLight = pastelColor(0xFFD4EDDA)
val MongleMoodExcited = pastelColor(0xFFFF9800)
val MongleMoodExcitedLight = pastelColor(0xFFFFE0B2)
val MongleMoodTired = pastelColor(0xFFB39DDB)
val MongleMoodTiredLight = pastelColor(0xFFE0D6F0)

// MARK: - Accent
val MongleAccentBlue = pastelColor(0xFF42A5F5)
val MongleAccentCoralLight = pastelColor(0xFFFF8A80) // iOS 기준: coralLight = #FF8A80
val MongleAccentCoralDark = pastelColor(0xFFF5978E)
val MongleAccentYellow = pastelColor(0xFFFFD54F)
val MongleAccentYellowLight = pastelColor(0xFFFFE082)
val MongleAccentPurple = pastelColor(0xFFAB47BC)
val MongleAccentPink = pastelColor(0xFFF06292)
val MongleAccentOrange = pastelColor(0xFFFF9800)
val MongleAccentPeach = pastelColor(0xFFF7B4A0)

// MARK: - Heart (iOS 기준: 추가 색상 포함)
val MongleHeartRed = pastelColor(0xFFFF6B6B)
val MongleHeartRedLight = pastelColor(0xFFFFE5E5)
val MongleHeartPink = pastelColor(0xFFFF7C85)
val MongleHeartPinkLight = pastelColor(0xFFFF9393)
val MongleHeartPastel = pastelColor(0xFFFFB3B8)
val MongleHeartPastelLight = pastelColor(0xFFFFD8D8)

// MARK: - UI Extras
val MongleBrown = pastelColor(0xFF5D4037)
val MonglePageIndicatorInactive = pastelColor(0xFFE7DED5)
val MongleCalendarSunday = pastelColor(0xFF1565C0)

// MARK: - Google Brand Colors
val MongleGoogleRed = pastelColor(0xFFEA4335)
val MongleGoogleBlue = pastelColor(0xFF4285F4)
val MongleGoogleYellow = pastelColor(0xFFFBBC05)
val MongleGoogleGreen = pastelColor(0xFF34A853)
val MongleGoogleBorder = pastelColor(0xFF747775) // iOS 기준: #747775
