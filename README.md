# 몽글 Android

> iOS 앱 [Mongle](../README.md)의 Android 포팅 버전입니다.
> 가족이 매일 하나의 질문에 함께 답하며 가상의 나무를 키우는 가족 소통 앱입니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| 언어 | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.03) |
| 아키텍처 | Clean Architecture + MVI |
| 상태 관리 | ViewModel + StateFlow + SharedFlow |
| 의존성 주입 | Hilt 2.52 |
| 비동기 | Kotlin Coroutines 1.8.1 |
| 네비게이션 | Compose Navigation 2.8.2 |
| 소셜 로그인 | Kakao SDK 2.20.3, Google Sign-In 21.2.0 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## iOS → Android 아키텍처 매핑

iOS 프로젝트는 **Clean Architecture + TCA(The Composable Architecture)** 패턴으로 구성되어 있습니다.
Android 포팅 시 동일한 계층 구조를 유지하되, 각 플랫폼의 관용적인 패턴으로 변환했습니다.

| iOS | Android | 비고 |
|---|---|---|
| Swift Package (Domain) | `domain/` 패키지 | 도메인 모델·인터페이스 동일 구조 |
| TCA `@Reducer` + `State` + `Action` | `ViewModel` + `UiState` + `Event` | StateFlow / SharedFlow 활용 |
| SwiftUI `View` | Jetpack Compose `@Composable` | |
| TCA `@Dependency` | Hilt `@Inject` / `@HiltViewModel` | |
| Swift `async throws` | `suspend` + Coroutines | |
| TCA `@Presents` (Sheet 전환) | `remember { mutableStateOf<T?>(null) }` | |
| `MongleData` (Swift Package, 미구현) | `data/mock/` | Mock Repository 구현 |

---

## 프로젝트 구조

```
android/
├── gradle/
│   └── libs.versions.toml          # 버전 카탈로그 (의존성 버전 중앙 관리)
├── build.gradle.kts                # 루트 빌드 파일 (플러그인 선언)
├── settings.gradle.kts             # 프로젝트 설정 (모듈·리포지토리 등록)
└── app/
    ├── build.gradle.kts            # 앱 모듈 의존성
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/mongle/android/
            │
            ├── MongleApplication.kt        # @HiltAndroidApp + Kakao SDK 초기화
            ├── MainActivity.kt             # 엣지-투-엣지, Compose 진입점
            │
            ├── domain/                     # 비즈니스 로직 (외부 프레임워크 미의존)
            │   ├── model/                  # 도메인 엔티티
            │   │   ├── User.kt             # 사용자 + FamilyRole 열거형
            │   │   ├── MongleGroup.kt      # 가족 그룹
            │   │   ├── Member.kt           # 가족 구성원 정보
            │   │   ├── Question.kt         # 질문 템플릿 + QuestionCategory
            │   │   ├── DailyQuestion.kt    # 오늘의 질문 인스턴스
            │   │   ├── Answer.kt           # 사용자 답변
            │   │   ├── TreeProgress.kt     # 나무 성장 (7단계: 씨앗→열매)
            │   │   ├── MongleNotification.kt
            │   │   └── SocialLoginCredential.kt   # 소셜 로그인 인터페이스 + 구현체
            │   └── repository/             # Repository 인터페이스 (추상화 경계)
            │       ├── AuthRepository.kt
            │       ├── UserRepository.kt
            │       ├── MongleRepository.kt  # 가족 그룹 CRUD
            │       ├── QuestionRepository.kt
            │       ├── DailyQuestionRepository.kt
            │       ├── AnswerRepository.kt
            │       └── TreeRepository.kt
            │
            ├── data/
            │   └── mock/                   # Mock 구현체 (실제 API 연동 전 사용)
            │       ├── MockAuthRepository.kt
            │       ├── MockMongleRepository.kt
            │       ├── MockQuestionRepository.kt
            │       ├── MockAnswerRepository.kt
            │       └── MockTreeRepository.kt
            │
            ├── di/
            │   └── AppModule.kt            # Hilt: Mock → Interface 바인딩
            │
            └── ui/
                ├── theme/                  # 디자인 시스템 (iOS 수치 그대로 이식)
                │   ├── Color.kt            # MonglePrimary, 소셜/무드/캐릭터 컬러 팔레트
                │   ├── Type.kt             # Outfit 폰트 기반 타이포그래피
                │   ├── Spacing.kt          # MongleSpacing / MongleRadius
                │   └── Theme.kt            # Material3 Light / Dark 테마
                │
                ├── common/                 # 재사용 가능한 공통 컴포넌트
                │   ├── MongleButton.kt     # PRIMARY / SECONDARY 스타일 + 로딩 상태
                │   ├── MongleCard.kt       # 클릭 가능 / 정적 카드
                │   ├── MongleTextField.kt  # 에러 메시지 포함 입력 필드
                │   └── MongleLogo.kt       # 앱 로고 (SMALL / MEDIUM / LARGE)
                │
                ├── navigation/
                │   └── MongleNavHost.kt    # 앱 상태(Loading/Unauth/Auth) 기반 라우팅
                │
                ├── root/
                │   └── RootViewModel.kt    # 인증 상태 확인 + 홈 데이터 로드
                │
                ├── login/
                │   ├── LoginViewModel.kt   # 이메일·소셜 로그인 로직
                │   └── LoginScreen.kt      # 이메일/비밀번호 + 카카오/구글 버튼
                │
                ├── main/
                │   └── MainTabScreen.kt    # 하단 탭 네비게이션 (홈·히스토리·설정)
                │
                ├── home/
                │   ├── HomeViewModel.kt    # 오늘의 질문 + 나무 + 가족 상태
                │   └── HomeScreen.kt       # 나무 카드, 오늘의 질문 카드, 가족 현황
                │
                ├── question/
                │   ├── QuestionDetailViewModel.kt
                │   └── QuestionDetailScreen.kt   # 질문 상세 + 내 답변 입력 + 가족 답변 목록
                │
                ├── history/
                │   ├── HistoryViewModel.kt  # 달력 상태, Mock 히스토리 데이터
                │   └── HistoryScreen.kt     # 월별 달력 + 선택 날짜 질문 카드
                │
                └── settings/
                    ├── SettingsViewModel.kt # 로그아웃·계정 삭제 처리
                    └── SettingsScreen.kt    # 프로필·알림·계정·앱 정보 섹션
```

---

## MVI 패턴 구조

각 화면은 다음 세 가지 요소로 구성됩니다.

```kotlin
// 1. 불변 UI 상태
data class HomeUiState(
    val todayQuestion: Question? = null,
    val isLoading: Boolean = false,
    ...
)

// 2. ViewModel - 단방향 데이터 흐름
@HiltViewModel
class HomeViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 화면 전환 등 일회성 이벤트
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
}

// 3. Composable - 상태를 받아 UI 렌더링
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.events.collect { ... } }
    ...
}
```

---

## 도메인 모델

### 나무 성장 단계 (TreeStage)

| 단계 | 값 | 이름 | 달성 조건 (예정) |
|---|---|---|---|
| SEED | 0 | 씨앗 | 시작 |
| SPROUT | 1 | 새싹 | 총 5개 답변 |
| SAPLING | 2 | 묘목 | 총 15개 답변 |
| YOUNG_TREE | 3 | 어린 나무 | 총 30개 답변 |
| MATURE_TREE | 4 | 큰 나무 | 총 60개 답변 |
| FLOWERING | 5 | 꽃 피는 나무 | 총 100개 답변 |
| BOUND | 6 | 열매 맺는 나무 | 총 150개 답변 |

### 질문 카테고리 (QuestionCategory)

| 값 | 표시명 |
|---|---|
| DAILY | 일상 & 취미 |
| MEMORY | 추억 & 과거 |
| VALUES | 가치관 & 생각 |
| FUTURE | 미래 & 계획 |
| GRATITUDE | 감사 & 애정 |

---

## 현재 구현 상태

### 완료

| 항목 | 상태 | 비고 |
|---|---|---|
| 프로젝트 기본 설정 (Gradle, Hilt, Compose) | ✅ 완료 | |
| Domain 레이어 (모델 9개, Repository 인터페이스 7개) | ✅ 완료 | iOS와 동일 구조 |
| Mock Repository 구현체 5개 | ✅ 완료 | 네트워크 지연 시뮬레이션 포함 |
| Hilt 의존성 주입 모듈 | ✅ 완료 | |
| Design System (Color / Typography / Spacing / Theme) | ✅ 완료 | iOS 수치 100% 이식 |
| 공통 컴포넌트 (Button / Card / TextField / Logo) | ✅ 완료 | |
| 로그인 화면 (이메일·카카오·구글) | ✅ 완료 | 실제 SDK 연동 필요 |
| 홈 화면 (나무·오늘의 질문·가족 현황) | ✅ 완료 | Pull-to-Refresh 포함 |
| 질문 상세 화면 (답변 입력·가족 답변 목록) | ✅ 완료 | |
| 히스토리 화면 (월별 달력) | ✅ 완료 | |
| 설정 화면 (프로필·알림·로그아웃·계정 삭제) | ✅ 완료 | |
| 앱 상태 기반 네비게이션 | ✅ 완료 | |

### 미구현 (TODO)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 실제 백엔드 API 연동 | P0 | Mock → 실제 Retrofit/Ktor 구현체 교체 |
| 카카오 로그인 SDK 실제 연동 | P0 | `KakaoLoginCredential`에 실제 토큰 전달 필요 |
| 구글 로그인 SDK 실제 연동 | P0 | `GoogleLoginCredential`에 실제 idToken 전달 필요 |
| 앱 아이콘 / 스플래시 화면 | P1 | `mipmap/ic_launcher` 리소스 추가 필요 |
| Outfit 폰트 적용 | P1 | `res/font/` 에 폰트 파일 추가 후 `Type.kt` 수정 |
| FCM 푸시 알림 | P1 | `google-services.json` + FCM SDK 추가 |
| 가족 초대 / 그룹 생성 화면 | P1 | 미구현 화면 |
| 프로필 편집 화면 | P1 | `ProfileEditScreen` 미구현 |
| 알림 목록 화면 | P2 | `NotificationScreen` 미구현 |
| 단위 테스트 / UI 테스트 | P2 | ViewModel 테스트 등 |
| ProGuard 설정 | P2 | Release 빌드 난독화 |

---

## 시작하기

### 사전 요건

- Android Studio Ladybug (2024.2.1) 이상
- JDK 17
- Android SDK 35

### 소셜 로그인 설정 (실제 연동 시)

**카카오 로그인**

1. [Kakao Developers](https://developers.kakao.com)에서 앱 등록
2. `AndroidManifest.xml`의 `kakao{YOUR_KAKAO_NATIVE_APP_KEY}` 교체
3. `MongleApplication.kt`의 `"YOUR_KAKAO_NATIVE_APP_KEY"` 교체

**구글 로그인**

1. [Firebase Console](https://console.firebase.google.com) 또는 Google Cloud Console에서 OAuth 클라이언트 ID 발급
2. `google-services.json`을 `app/` 디렉토리에 추가
3. `LoginScreen.kt`의 구글 로그인 버튼 onClick에 `GoogleSignInClient` 연동

### 빌드

```bash
cd android
./gradlew assembleDebug
```

---

## 실제 API 연동 방법

현재 모든 Repository는 Mock 구현체를 사용합니다.
실제 API 서버 연동 시 `data/mock/` 대신 `data/remote/` 구현체를 만들고 `di/AppModule.kt`의 바인딩만 교체하면 됩니다.

```kotlin
// di/AppModule.kt - 바인딩 교체 예시
@Binds
@Singleton
// abstract fun bindAuthRepository(impl: MockAuthRepository): AuthRepository  // 기존 Mock
abstract fun bindAuthRepository(impl: RemoteAuthRepository): AuthRepository   // 실제 API
```

Domain 레이어와 UI 레이어는 수정 없이 그대로 사용합니다.
