# 몽글 Android

> iOS 앱 Mongle의 Android 포팅 버전입니다.
> 가족이 매일 하나의 질문에 함께 답하며 서로를 더 깊이 알아가는 가족 소통 앱입니다.

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
| 네트워크 | Retrofit 2.11 + OkHttp 4.12 + Moshi 1.15 |
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
| Delegate Action (화면 간 통신) | `SharedFlow<Event>` | 일회성 이벤트 전달 |

---

## 프로젝트 구조

```
app/src/main/java/com/mongle/android/
│
├── MongleApplication.kt        # @HiltAndroidApp + Kakao SDK 초기화
├── MainActivity.kt             # 엣지-투-엣지, Compose 진입점
│
├── domain/                     # 비즈니스 로직 (외부 프레임워크 미의존)
│   ├── model/                  # 도메인 엔티티
│   │   ├── User.kt             # 사용자 + FamilyRole 열거형
│   │   ├── MongleGroup.kt      # 가족 그룹
│   │   ├── Question.kt         # 질문 + QuestionCategory
│   │   ├── Answer.kt           # 답변
│   │   ├── DailyQuestionHistory.kt
│   │   ├── MongleNotification.kt
│   │   └── SocialLoginCredential.kt
│   └── repository/             # Repository 인터페이스 (추상화 경계)
│       ├── AuthRepository.kt
│       ├── UserRepository.kt
│       ├── MongleRepository.kt
│       ├── QuestionRepository.kt
│       ├── AnswerRepository.kt
│       └── TreeRepository.kt
│
├── data/
│   ├── remote/                 # 실제 API 구현체
│   │   ├── MongleApiService.kt       # Retrofit 인터페이스 + DTO 정의
│   │   ├── AuthInterceptor.kt        # Bearer 토큰 자동 주입
│   │   ├── ApiAuthRepository.kt
│   │   ├── ApiFamilyRepository.kt
│   │   ├── ApiQuestionRepository.kt
│   │   ├── ApiAnswerRepository.kt
│   │   ├── ApiUserRepository.kt
│   │   ├── ApiTreeRepository.kt
│   │   └── ApiNotificationRepository.kt
│   └── mock/                   # Mock 구현체 (레거시, 미사용)
│
├── di/
│   ├── AppModule.kt            # Hilt: Repository 바인딩
│   └── NetworkModule.kt        # Retrofit / OkHttp 설정
│
└── ui/
    ├── theme/                  # 디자인 시스템 (iOS 수치 이식)
    │   ├── Color.kt            # MonglePrimary, 소셜/캐릭터 컬러 팔레트
    │   ├── Type.kt             # 타이포그래피
    │   ├── Spacing.kt          # MongleSpacing / MongleRadius
    │   └── Theme.kt            # Material3 Light / Dark 테마
    │
    ├── common/                 # 재사용 공통 컴포넌트
    │   ├── MongleButton.kt     # PRIMARY / SECONDARY 스타일 + 로딩 상태
    │   ├── MongleCard.kt       # 클릭 가능 / 정적 카드
    │   ├── MongleTextField.kt  # 에러 메시지 포함 입력 필드
    │   ├── MongleCharacter.kt  # 가족 구성원 캐릭터 아바타
    │   └── MongleLogo.kt       # 앱 로고
    │
    ├── navigation/
    │   └── MongleNavHost.kt    # 앱 상태(Loading/Unauth/Auth) 기반 라우팅
    │
    ├── root/       RootViewModel.kt     # 인증 확인 + 초기 데이터 로드
    ├── login/      LoginScreen/ViewModel  # 이메일·카카오·구글 로그인
    ├── main/       MainTabScreen          # 하단 탭 (홈·히스토리·설정)
    ├── home/       HomeScreen/ViewModel   # 오늘의 질문, 가족 현황, 스트릭
    ├── question/   QuestionDetailScreen/ViewModel  # 답변 입력·가족 답변
    ├── history/    HistoryScreen/ViewModel # 월별 달력 + 히스토리
    ├── settings/   SettingsScreen/ViewModel # 프로필·그룹·계정 관리
    └── notification/ NotificationScreen/ViewModel # 알림 목록
```

---

## MVI 패턴 구조

각 화면은 다음 세 가지 요소로 구성됩니다.

```kotlin
// 1. 불변 UI 상태
data class HomeUiState(
    val todayQuestion: Question? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// 2. ViewModel — 단방향 데이터 흐름
@HiltViewModel
class HomeViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 화면 전환 등 일회성 이벤트
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
}

// 3. Composable — 상태를 받아 UI 렌더링
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.events.collect { ... } }
}
```

---

## 구현 상태

| 항목 | 상태 | 비고 |
|---|---|---|
| 네트워크 레이어 (Retrofit + OkHttp + Moshi) | ✅ | Bearer 토큰 자동 주입 |
| 이메일 로그인 / 회원가입 | ✅ | |
| 카카오 / 구글 소셜 로그인 | ✅ | |
| 홈 화면 | ✅ | 오늘의 질문, 가족 현황, 스트릭, 그라디언트 |
| 질문 상세 화면 | ✅ | 답변 제출·수정, 가족 답변 목록 실제 API |
| 히스토리 화면 | ✅ | 월별 달력, 답변 표시 |
| 설정 화면 | ✅ | 프로필 편집, 그룹 관리, 계정 삭제 |
| 알림 화면 | ✅ | 읽음·전체읽음 처리 |
| 디자인 시스템 | ✅ | iOS 디자인 100% 이식 |

### 미구현

| 항목 | 비고 |
|---|---|
| 앱 아이콘 / 스플래시 화면 | `mipmap/ic_launcher` 리소스 교체 필요 |
| FCM 푸시 알림 | `google-services.json` + FCM SDK 추가 필요 |
| 단위 테스트 / UI 테스트 | ViewModel 테스트 등 |
| ProGuard 설정 | Release 빌드 난독화 |

---

## 시작하기

### 사전 요건

- Android Studio Ladybug (2024.2.1) 이상
- JDK 17
- Android SDK 35

### 소셜 로그인 설정

**카카오 로그인**

1. [Kakao Developers](https://developers.kakao.com)에서 앱 등록
2. `AndroidManifest.xml`의 `kakao{YOUR_KAKAO_NATIVE_APP_KEY}` 교체
3. `MongleApplication.kt`의 `"YOUR_KAKAO_NATIVE_APP_KEY"` 교체

**구글 로그인**

1. Firebase Console 또는 Google Cloud Console에서 OAuth 클라이언트 ID 발급
2. `google-services.json`을 `app/` 디렉토리에 추가
3. `LoginScreen.kt`의 `GOOGLE_WEB_CLIENT_ID` 상수 교체

### 빌드

```bash
./gradlew assembleDebug
```
