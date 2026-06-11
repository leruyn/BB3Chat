# BB3Chat — Development Plan

Generated: 2026-06-11  
Status: S1–S18 scaffolded. Next: fix compile errors → runtime bindings → iOS → polish.

---

## Completed Sprints (S1–S18)

| Sprint | Description | Status |
|--------|-------------|--------|
| S1 | Project scaffold & build files (libs.versions.toml, settings, build.gradle.kts) | ✅ |
| S2 | SQLDelight schema + expect/actual DB driver (ChatRoom.sq, Message.sq, AppConfig.sq) | ✅ |
| S3 | Core: CryptoManager (expect/actual), SessionManager, KeyValueStorage | ✅ |
| S4 | Auth: Dual-PIN PBKDF2 key derivation, PinAuthRepositoryImpl, PinViewModel | ✅ |
| S5 | Messaging: FirebaseMessageRepository (callbackFlow), inline base64 image, Pull-Destruct | ✅ |
| S6 | FCM: data-only push, BB3MessagingService, SELF_DESTRUCT / ROOM_DESTRUCT handlers | ✅ |
| S7 | Panic: SensorManager expect/actual, accelerometer flip-to-hide, SessionManager.destroySessionKey() | ✅ |
| S8 | Koin DI wiring, AndroidModule, BB3ChatApplication, MainActivity, iOS AppDelegate | ✅ |
| S9 | Calculator Mask Screen (iOS-style UI, secret code detection, green flash) | ✅ |
| S10 | PIN Screen (4-dot indicator, shake animation, dual-mode setup/auth) | ✅ |
| S11 | Inbox Screen + Chatroom Screen (UDF, LazyColumn, bubble UI, attach preview) | ✅ |
| S12 | Navigation: BB3ChatApp root composable, Screen sealed class, ChatroomViewModelFactory | ✅ |
| S13 | Self-Destruct countdown overlay (ring animation, green→amber→red, onExpired callback) | ✅ |
| S14 | Safe Hours Gate screen (HH:MM:SS countdown, emergency bypass with PIN) | ✅ |
| S15 | QR Handshake screen (tab: my QR / scan, manual code entry, deterministic roomId) | ✅ |
| S16 | Intruder capture (Camera2 silent capture, AES encrypt, EncryptedSharedPrefs, alert banner) | ✅ |
| S17 | VIP Store screen (4-tier plans, gradient cards, Gold highlighted, VipStoreViewModel) | ✅ |
| S18 | Firebase Security Rules (firestore.rules) + unit tests (crypto, session, auth, image) | ✅ |

---

## 🔴 P0 — Compile Errors (fix these first)

### 1. `CryptoManager.generateRandomBytes` → rename to `randomBytes`
- **File**: `shared/src/commonMain/.../feature/pairing/presentation/QrHandshakeViewModel.kt`
- **Line**: `val bytes = CryptoManager.generateRandomBytes(6)`
- **Fix**: change to `CryptoManager.randomBytes(6)`

### 2. `CryptoManager.encryptWithDeviceKey` does not exist
- **File**: `shared/src/commonMain/.../feature/security/domain/usecase/CaptureIntruderUseCase.kt`
- **Line 39**: `val encryptResult = CryptoManager.encryptWithDeviceKey(rawJpegBytes)`
- **Fix**: generate ephemeral key + use `CryptoManager.encrypt()`:
  ```kotlin
  val ephemeralKey  = CryptoManager.randomBytes(32)
  val encryptResult = CryptoManager.encrypt(rawJpegBytes, ephemeralKey)
  // Store ephemeralKey encrypted with device key (AndroidKeyStore/Keychain) separately
  ```

### 3. `EncryptResult.encryptedData` → correct field name is `cipherBytes`
- **Files**:
  - `CaptureIntruderUseCase.kt` line 40: `encryptResult.encryptedData` → `encryptResult.cipherBytes`
  - `ChatroomViewModel.kt` line 150: `encryptedData = c.encryptedBody` (wrong param name — see fix #5)
- **EncryptResult definition**: `data class EncryptResult(val cipherBytes: ByteArray, val iv: ByteArray)`

### 4. `SessionManager.currentUserId` does not exist
- **File**: `shared/src/commonMain/.../core/crypto/SessionManager.kt`
- **Fix**: add field + set in `openRealSession()`:
  ```kotlin
  var currentUserId: String? = null
    private set

  fun openRealSession(key: ByteArray, driver: SqlDriver, userId: String) {
      _sessionKey   = key.copyOf()
      _database     = BB3Database(driver)
      currentUserId = userId
      _isAuthenticated.value = true
  }
  ```
  Also clear in `destroySessionKey()`: `currentUserId = null`

### 5. `ChatroomViewModel.toUiMessage()` uses wrong field names
- **File**: `shared/src/commonMain/.../feature/messaging/presentation/chatroom/ChatroomViewModel.kt`
- **Correct Message model fields**:
  ```
  Message.senderAlias       (not senderId)
  Message.sentAt            (not timestamp)
  Message.readBy            (Map<String,Long>, not isRead: Boolean)
  Message.destructConfig    (not selfDestruct: Boolean / selfDestructSeconds: Int)
  ```
- **Fix toUiMessage()**:
  ```kotlin
  senderId     = msg.senderAlias,
  isMine       = msg.senderAlias == myUid,
  timestampMs  = msg.sentAt,
  readByPeer   = msg.readBy.keys.any { it != myUid },
  selfDestruct = msg.destructConfig != null,
  destructSecs = msg.destructConfig?.countdownSeconds ?: 0
  ```

### 6. `MessageContent.Voice.durationSecs` → field is `durationMs`
- **File**: `ChatroomViewModel.kt` — `toUiMessage()` Image/Voice mapping
- **Fix**: `UiMessageContent.Voice((c.durationMs / 1000).toInt())`

### 7. `CryptoManager.decrypt()` type mismatch — `encryptedBody` is `String` not `ByteArray`
- **File**: `ChatroomViewModel.kt` lines 148–153
- **Message.Text** stores `encryptedBody: String` (base64) and `iv: String` (base64)
- **Fix**: decode base64 before passing to decrypt:
  ```kotlin
  is MessageContent.Text -> {
      val plain = runCatching {
          val cipherBytes = Base64.decode(c.encryptedBody)
          val ivBytes     = Base64.decode(c.iv)
          CryptoManager.decrypt(cipherBytes, ivBytes, sessionKey).decodeToString()
      }.getOrDefault("[Lỗi giải mã]")
      UiMessageContent.Text(plain)
  }
  ```
  Use `kotlin.io.encoding.Base64` (KMP stdlib ≥1.8) or include `ktor-io` base64 util.

### 8. `gradle/wrapper/gradle-wrapper.properties` is missing
- **Fix**: create file at `gradle/wrapper/gradle-wrapper.properties`:
  ```properties
  distributionBase=GRADLE_USER_HOME
  distributionPath=wrapper/dists
  distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
  zipStoreBase=GRADLE_USER_HOME
  zipStorePath=wrapper/dists
  ```
  Also need `gradlew` and `gradlew.bat` scripts at project root.

### 9. `KeyValueStorage.getInt()` signature mismatch
- **Interface** declares: `fun getInt(key: String, default: Int = 0): Int?`
- **AndroidImpl** returns: `Int` (not nullable)
- **Fix**: change interface to `fun getInt(key: String, default: Int = 0): Int` (non-nullable, consistent with `getLong`)

---

## 🟡 P1 — Runtime Bindings (Koin — will crash at startup)

### 10. `MessageRepository` not bound in Koin
- `FirebaseMessageRepository` lives in `androidMain` but is never registered as `MessageRepository`
- **Fix**: add to `androidApp/.../di/AndroidModule.kt`:
  ```kotlin
  single<MessageRepository> {
      FirebaseMessageRepository(
          firestore      = Firebase.firestore,
          sessionManager = get(),
          imageProcessor = get()
      )
  }
  ```

### 11. `TokenRepository` not bound in Koin
- `TokenRepositoryImpl` exists in androidMain but not registered
- **Fix**: add to `AndroidModule.kt`:
  ```kotlin
  single<TokenRepository> { TokenRepositoryImpl(get(), get()) }
  ```

### 12. `SendImageUseCase` constructor mismatch between test fake and real impl
- **Test** (`SendImageUseCaseTest.kt`) constructs as `SendImageUseCase(fakeRepo, sessionKey)`
- **Real impl** likely takes `(repo, imageProcessor, sessionManager)`
- **Fix**: align test to use the real constructor signature with fakes for all deps, or add a secondary constructor for testing

---

## 🟢 P2 — iOS Integration

### 13. `GoogleService-Info.plist`
- **Status**: user needs to download from Firebase Console and place at `iosApp/iosApp/GoogleService-Info.plist`
- **Do NOT commit** to git — add to `.gitignore`

### 14. iOS entry point — wrap KMP Compose in UIKit
- **File to create**: `iosApp/iosApp/ContentView.swift`
  ```swift
  import SwiftUI
  import shared

  struct ContentView: View {
      var body: some View {
          ComposeView()
              .ignoresSafeArea(.all)
      }
  }

  struct ComposeView: UIViewControllerRepresentable {
      func makeUIViewController(context: Context) -> UIViewController {
          BB3ChatAppKt.BB3ChatViewController()  // expect/actual entry point
      }
      func updateUIViewController(_ vc: UIViewController, context: Context) {}
  }
  ```
- **File to create**: `shared/src/iosMain/.../MainViewController.kt`
  ```kotlin
  fun BB3ChatViewController(): UIViewController =
      ComposeUIViewController { BB3ChatApp() }
  ```

### 15. iOS `KeyValueStorageImpl`
- **File**: `shared/src/iosMain/.../core/storage/KeyValueStorageImpl.kt`
- Use `NSUserDefaults.standardUserDefaults` for non-sensitive keys; Keychain for sensitive ones
- Must implement all interface methods including new `putInt`/`getInt`

---

## 🔵 P3 — Polish & Production Readiness

### 16. Real QR code bitmap generation
- **expect/actual**: `QrCodeGenerator.generateBitmap(content: String): ImageBitmap`
- Android: `com.google.zxing:core` (already available via Firebase deps transitively) or `zxing-android-embedded`
- iOS: `CoreImage.CIFilter` with `CIQRCodeGenerator`

### 17. `AsyncImage` + BlurHash decode in ChatroomScreen
- Image bubble currently shows 🖼 emoji placeholder
- Add `remember { decryptAndDecode(bytes) }` + BlurHash crossfade transition
- Use platform `ImageProcessor.decryptAndDecode()` expect/actual (already defined in S3)

### 18. Firebase deploy config
- Create `firebase.json`:
  ```json
  {
    "firestore": {
      "rules": "firestore.rules",
      "indexes": "firestore.indexes.json"
    }
  }
  ```
- Create `firestore.indexes.json` with composite index on `rooms/{roomId}/messages` ordered by `timestampMs ASC`

### 19. CI/CD — GitHub Actions
- **File**: `.github/workflows/build.yml`
- Steps: checkout → setup JDK 17 → `./gradlew :shared:testDebugUnitTest` → `./gradlew :androidApp:assembleDebug`
- Cache: Gradle + KMP konan cache

### 20. `.gitignore` additions
```
google-services.json
GoogleService-Info.plist
*.keystore
local.properties
**/.env
```

---

## Key Architecture Decisions (reference)

| Decision | Choice | Reason |
|----------|--------|--------|
| Backend | Firebase only (Firestore + FCM) | No separate server, zero-knowledge design |
| Image storage | Inline AES-256-GCM base64 in Firestore message doc | Server never stores raw images; < 900KB limit |
| DB encryption | SQLCipher with PBKDF2WithHmacSHA256 100k iterations | AndroidKeyStore/iOS Keychain salt |
| Dual PIN | Real PIN → real DB; Decoy PIN → empty decoy DB | Identical PBKDF2 timing — indistinguishable |
| FCM | Data-only payload; notification payload explicitly rejected | No OS-level notification trace |
| Session key | ByteArray in RAM only; `fill(0)` on panic/logout | Never persisted to disk |
| Panic triggers | Accelerometer z < -8 m/s² (Android) / z < -0.8g (iOS) + flip | Instant wipe without UI interaction |
| App disguise | "Calculator+" (app_name in Manifest) + CalculatorMaskScreen | Passes casual phone inspection |
| FCM token relay | Anonymous Ktor POST to VPS (no auth header) | Decouple token from identity |

---

## File Map (key files)

```
BB3Chat/
├── firestore.rules                          ← Firebase security rules (S18)
├── firebase.json                            ← TODO (P3-18)
├── PLAN.md                                  ← this file
├── gradle/
│   ├── libs.versions.toml                   ← KMP 2.0.20, AGP 8.5.2, Firebase BOM 33.4.0
│   └── wrapper/
│       └── gradle-wrapper.properties        ← TODO (P0-8) Gradle 8.9
├── shared/src/
│   ├── commonMain/kotlin/com/bb3/bb3chat/
│   │   ├── core/
│   │   │   ├── crypto/
│   │   │   │   ├── CryptoManager.kt         ← expect object (PBKDF2 + AES-GCM interface)
│   │   │   │   └── SessionManager.kt        ← RAM-only session key + DB handle
│   │   │   ├── db/DatabaseDriverFactory.kt  ← expect class (SQLCipher vs decoy)
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt             ← Koin modules
│   │   │   │   └── KoinInit.kt
│   │   │   ├── platform/
│   │   │   │   ├── SensorManager.kt         ← expect (accelerometer panic)
│   │   │   │   └── ImageProcessor.kt        ← expect (compress, BlurHash, decrypt+decode)
│   │   │   └── storage/KeyValueStorage.kt   ← interface + StorageKeys
│   │   ├── feature/
│   │   │   ├── auth/                        ← Dual-PIN, PinViewModel, PinScreen
│   │   │   ├── disguise/                    ← CalculatorMaskScreen
│   │   │   ├── messaging/
│   │   │   │   ├── domain/model/Message.kt  ← MessageContent sealed class (Text/Image/Voice/…)
│   │   │   │   └── presentation/
│   │   │   │       ├── inbox/               ← InboxContract, InboxViewModel, InboxScreen
│   │   │   │       ├── chatroom/            ← ChatroomContract, ChatroomViewModel, ChatroomScreen
│   │   │   │       └── selfdestruct/        ← SelfDestructOverlay
│   │   │   ├── pairing/                     ← QrHandshakeScreen (S15)
│   │   │   ├── security/
│   │   │   │   ├── domain/usecase/          ← TriggerLocalPanicUseCase, CaptureIntruderUseCase
│   │   │   │   └── presentation/            ← SafeHoursGateScreen, IntruderAlertBanner
│   │   │   ├── store/                       ← VipStoreScreen, VipStoreViewModel (S17)
│   │   │   └── token/                       ← HeartbeatUseCase, FCM token relay
│   │   ├── presentation/
│   │   │   ├── base/BaseViewModel.kt        ← generic UDF BaseViewModel<S,E,F>
│   │   │   └── navigation/
│   │   │       ├── BB3ChatApp.kt            ← root composable + Screen sealed class
│   │   │       └── ChatroomViewModelFactory.kt
│   │   └── ui/theme/Theme.kt                ← BB3ChatTheme dark + color tokens
│   ├── androidMain/kotlin/com/bb3/bb3chat/
│   │   ├── core/crypto/CryptoManager.kt     ← actual: AndroidKeyStore + PBKDF2 + AES-GCM
│   │   ├── core/db/DatabaseDriverFactory.kt ← actual: SQLCipher SupportFactory
│   │   ├── core/storage/KeyValueStorageImpl.kt ← EncryptedSharedPreferences AES256
│   │   ├── core/platform/SensorManager.kt   ← actual: TYPE_ACCELEROMETER
│   │   ├── core/platform/ImageProcessor.kt  ← actual: Bitmap JPEG compress + BlurHash
│   │   ├── core/di/AndroidModule.kt         ← platform Koin bindings
│   │   ├── feature/messaging/data/
│   │   │   ├── FirebaseMessageRepository.kt ← Firestore callbackFlow listener
│   │   │   └── BB3MessagingService.kt       ← FCM data-only handler
│   │   ├── feature/token/data/
│   │   │   └── TokenRepositoryImpl.kt       ← anonymous VPS relay via Ktor
│   │   └── security/
│   │       └── IntruderCameraCapture.kt     ← Camera2 silent front-camera capture
│   ├── iosMain/kotlin/com/bb3/bb3chat/
│   │   ├── core/crypto/CryptoManager.kt     ← actual: CCKeyDerivationPBKDF + CCCryptorGCM
│   │   ├── core/db/DatabaseDriverFactory.kt ← actual: NativeSqliteDriver + PRAGMA key
│   │   ├── core/platform/SensorManager.kt   ← actual: delegates to SensorBridgeHolder
│   │   └── core/storage/                    ← TODO (P2-15): NSUserDefaults / Keychain
│   └── commonTest/kotlin/com/bb3/bb3chat/
│       ├── crypto/CryptoManagerTest.kt      ← 8 tests: derivation, round-trip, tamper
│       ├── security/SessionManagerTest.kt   ← 5 tests: RAM wipe, throw after destroy
│       ├── messaging/SendImageUseCaseTest.kt ← size limit, content type
│       └── auth/PinAuthTest.kt              ← real/decoy/wrong PIN
├── androidApp/
│   ├── src/main/
│   │   ├── AndroidManifest.xml              ← app_name="Calculator+", no notification channel
│   │   └── kotlin/com/bb3/bb3chat/
│   │       ├── BB3ChatApplication.kt        ← initKoin(androidModule)
│   │       └── MainActivity.kt              ← BB3ChatTheme { BB3ChatApp() }
│   └── google-services.json                 ← ✅ added by user
└── iosApp/iosApp/
    ├── AppDelegate.swift                    ← FirebaseApp.configure(), silent push
    ├── PushHandler.swift                    ← SELF_DESTRUCT / ROOM_DESTRUCT bridge
    ├── SensorBridge.swift                   ← CMMotionManager → SensorBridgeHolder
    └── GoogleService-Info.plist             ← TODO (P2-13): add from Firebase Console
```
