# BB3Chat — Cursor Fix Plan (P0 Compile Errors)

> **Mục tiêu:** Fix toàn bộ compile errors theo thứ tự dependency. Không thêm feature, không refactor ngoài scope.

---

## Step 1 — Gradle wrapper (không có thì build không chạy được)

### 1a. Tạo `gradle/wrapper/gradle-wrapper.properties`
```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 1b. Tạo `gradlew` ở root
Copy nội dung standard Gradle 8.9 wrapper script (Unix shell). Nếu đã có Android Studio cài sẵn, chạy lệnh này để generate:
```bash
gradle wrapper --gradle-version=8.9
```

---

## Step 2 — `KeyValueStorage` interface: fix `getInt` nullable mismatch

**File:** `shared/src/commonMain/.../core/storage/KeyValueStorage.kt` — dòng 9

**Vấn đề:** Interface khai báo `getInt(...): Int?` nhưng Android impl trả về `Int` (non-nullable) → compile error.

**Sửa:**
```kotlin
// Đổi từ:
fun getInt(key: String, default: Int = 0): Int?
// Thành:
fun getInt(key: String, default: Int = 0): Int
```

**File:** `shared/src/iosMain/.../core/storage/KeyValueStorageImpl.kt`

**Vấn đề:** iOS impl **thiếu hoàn toàn** `putInt` và `getInt`.

**Thêm vào class:**
```kotlin
override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)
override fun getInt(key: String, default: Int): Int =
    if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default
```

---

## Step 3 — `SessionManager`: thêm `currentUserId`

**File:** `shared/src/commonMain/.../core/crypto/SessionManager.kt`

**Vấn đề:** `ChatroomViewModel` gọi `sessionManager.currentUserId` nhưng field này không tồn tại.

**Sửa — thêm field và cập nhật 2 method:**
```kotlin
var currentUserId: String? = null
    private set

fun openRealSession(key: ByteArray, driver: SqlDriver, userId: String) {
    _sessionKey   = key.copyOf()
    _database     = BB3Database(driver)
    currentUserId = userId
    _isAuthenticated.value = true
}

fun destroySessionKey() {
    _sessionKey?.fill(0)
    _sessionKey   = null
    _database     = null
    currentUserId = null
    _isAuthenticated.value = false
}
```

> **Chú ý:** `openRealSession` thêm param `userId: String` → update tất cả call sites
> (tìm bằng `grep -r "openRealSession" shared/`).

---

## Step 4 — `QrHandshakeViewModel`: rename `generateRandomBytes` → `randomBytes`

**File:** `shared/src/commonMain/.../feature/pairing/presentation/QrHandshakeViewModel.kt` — dòng 32

```kotlin
// Đổi từ:
val bytes = CryptoManager.generateRandomBytes(6)
// Thành:
val bytes = CryptoManager.randomBytes(6)
```

---

## Step 5 — `CaptureIntruderUseCase`: fix 3 lỗi liên tiếp

**File:** `shared/src/commonMain/.../feature/security/domain/usecase/CaptureIntruderUseCase.kt`

### 5a. Dòng 39 — `encryptWithDeviceKey` không tồn tại
```kotlin
// Đổi từ:
val encryptResult = CryptoManager.encryptWithDeviceKey(rawJpegBytes)
// Thành:
val ephemeralKey  = CryptoManager.randomBytes(32)
val encryptResult = CryptoManager.encrypt(rawJpegBytes, ephemeralKey)
// Lưu ephemeralKey vào storage (slot riêng) để decrypt sau:
storage.putString(StorageKeys.INTRUDER_PHOTO_KEY_PREFIX + slot,
    android.util.Base64.encodeToString(ephemeralKey, android.util.Base64.NO_WRAP))
```
> Nếu cần KMP (không dùng android.util.Base64), dùng helper ở 5c bên dưới.

### 5b. Dòng 40 — `encryptResult.encryptedData` → `encryptResult.cipherBytes`
```kotlin
// Đổi từ:
val base64Payload = encryptResult.encryptedData.encodeBase64()
// Thành:
val base64Payload = encryptResult.cipherBytes.encodeBase64()
```

### 5c. Dòng 80–83 — Extension `encodeBase64()` dùng sai API
```kotlin
// Đổi từ:
private fun ByteArray.encodeBase64(): String {
    return kotlinx.io.bytestring.encodeToByteString(this).toString()
}
// Thành:
private fun ByteArray.encodeBase64(): String =
    kotlin.io.encoding.Base64.Default.encode(this)
```
> `kotlin.io.encoding.Base64` có sẵn trong KMP stdlib ≥ 1.8. Không cần thêm dependency.

### 5d. Thêm `INTRUDER_PHOTO_KEY_PREFIX` vào `StorageKeys`
**File:** `KeyValueStorage.kt` — object `StorageKeys`
```kotlin
const val INTRUDER_PHOTO_KEY_PREFIX = "intruder_photo_key_"
```

---

## Step 6 — `ChatroomViewModel.toUiMessage()`: fix tất cả field name sai

**File:** `shared/src/commonMain/.../feature/messaging/presentation/chatroom/ChatroomViewModel.kt`

### 6a. Dòng 148–154 — Text: named arg sai + kiểu String thay vì ByteArray
`decrypt()` nhận `ByteArray` nhưng `c.encryptedBody` là `String` (base64).
Dùng `decryptString` thay vì `decrypt` vì nó đã handle base64:
```kotlin
is MessageContent.Text -> {
    val plain = runCatching {
        CryptoManager.decryptString(c.encryptedBody, c.iv, sessionKey)
    }.getOrDefault("[Lỗi giải mã]")
    UiMessageContent.Text(plain)
}
```

### 6b. Dòng 158–163 — Image: `c.encryptedBase64` và `c.iv` là String, không phải ByteArray
```kotlin
is MessageContent.Image -> {
    val bytes = runCatching {
        val cipher = kotlin.io.encoding.Base64.Default.decode(c.encryptedBase64)
        val iv     = kotlin.io.encoding.Base64.Default.decode(c.iv)
        CryptoManager.decrypt(cipher, iv, sessionKey)
    }.getOrNull()
    UiMessageContent.Image(decryptedBytes = bytes, blurHash = c.blurHash)
}
```

### 6c. Dòng 166 — `c.durationSecs` không tồn tại → `c.durationMs`
```kotlin
// Đổi từ:
is MessageContent.Voice -> UiMessageContent.Voice(c.durationSecs)
// Thành:
is MessageContent.Voice -> UiMessageContent.Voice((c.durationMs / 1000).toInt())
```

### 6d. Dòng 167 — `c.eventText` không tồn tại → `c.eventType.name`
```kotlin
// Đổi từ:
is MessageContent.SystemEvent -> UiMessageContent.SystemEvent(c.eventText)
// Thành:
is MessageContent.SystemEvent -> UiMessageContent.SystemEvent(c.eventType.name)
```

### 6e. Dòng 170–179 — `UiMessage(...)` dùng field không tồn tại trên `Message`
```kotlin
return UiMessage(
    id           = id,
    senderId     = senderAlias,                           // Message.senderAlias (không phải senderId)
    isMine       = senderAlias == myUid,                  // senderAlias
    content      = uiContent,
    timestampMs  = sentAt,                                // Message.sentAt (không phải timestamp)
    readByPeer   = readBy.keys.any { it != myUid },       // Map<String,Long> (không phải isRead)
    selfDestruct = destructConfig != null,                // DestructConfig? (không phải Boolean)
    destructSecs = destructConfig?.countdownSeconds ?: 0  // countdownSeconds (không phải selfDestructSeconds)
)
```

---

## Step 7 — `FirebaseMessageRepository`: fix `RoomStatus` enum access

**File:** `shared/src/androidMain/.../feature/messaging/data/FirebaseMessageRepository.kt` — dòng 79–83

Enum values là SCREAMING_SNAKE_CASE nhưng code dùng PascalCase:
```kotlin
// Đổi từ:
"PENDING_DESTRUCT" -> RoomStatus.PendingDestruct
"DESTROYED"        -> RoomStatus.Destroyed
else               -> RoomStatus.Active
// Thành:
"PENDING_DESTRUCT" -> RoomStatus.PENDING_DESTRUCT
"DESTROYED"        -> RoomStatus.DESTROYED
else               -> RoomStatus.ACTIVE
```

---

## Step 8 — `SendImageUseCase`: fix `java.util.Base64` (Android-only, không dùng được trong KMP test)

**File:** `shared/src/commonMain/.../feature/messaging/domain/usecase/SendImageUseCase.kt` — dòng 43–45

```kotlin
// Đổi từ:
private fun ByteArray.toBase64(): String =
    java.util.Base64.getEncoder().encodeToString(this)
// Thành:
private fun ByteArray.toBase64(): String =
    kotlin.io.encoding.Base64.Default.encode(this)
```

---

## Step 9 — `SendImageUseCaseTest`: fix constructor + interface mismatch

**File:** `shared/src/commonTest/.../messaging/SendImageUseCaseTest.kt`

### 9a. Fake `MessageRepository` — sai signature
```kotlin
// Interface thật:
// sendMessage(roomId: String, content: MessageContent, destructConfig: DestructConfig?): String
// checkRoomStatus(roomId: String): RoomStatus
// Thêm: markMessageRead + triggerSelfDestruct

private val fakeRepo = object : MessageRepository {
    override fun observeMessages(roomId: String): Flow<List<Message>> = emptyFlow()
    override suspend fun sendMessage(
        roomId: String,
        content: MessageContent,
        destructConfig: DestructConfig?
    ): String { return "fake-msg-id" }
    override suspend fun checkRoomStatus(roomId: String): RoomStatus = RoomStatus.ACTIVE
    override suspend fun destroyLocalRoom(roomId: String) {}
    override suspend fun markMessageRead(messageId: String, alias: String) {}
    override suspend fun triggerSelfDestruct(messageId: String) {}
}
```

### 9b. Constructor — `SendImageUseCase` nhận 3 deps, không phải 2
Cần tạo fake `SessionManager` và fake `ImageProcessor`:
```kotlin
// Tạo fake SessionManager với session key
private val fakeSession = SessionManager().also {
    // openRealSession cần SqlDriver — dùng fake driver hoặc skip
    // Chỉ cần requireSessionKey() trả về key:
    // → Dùng reflection HOẶC thêm constructor internal cho test
}
```
> **Gợi ý đơn giản nhất:** Thêm constructor thứ hai trong `SendImageUseCase` cho test:
> ```kotlin
> internal constructor(
>     repository: MessageRepository,
>     sessionKey: ByteArray,
>     imageProcessor: ImageProcessor
> ) : this(repository, SessionManagerStub(sessionKey), imageProcessor)
> ```
> Hoặc extract `sessionKey` ra param của `invoke()` nếu muốn testable hơn.

### 9c. Invoke call — param name `rawBytes` không khớp
```kotlin
// Đổi từ:
useCase(roomId = "room1", rawBytes = rawJpeg, blurHash = "LEHV6n...")
// Thành (khớp real signature):
useCase(roomId = "room1", rawImageBytes = rawJpeg, mimeType = "image/jpeg")
```

---

## Thứ tự thực hiện (dependency order)

```
Step 1  → gradle wrapper           (không phụ thuộc gì)
Step 2  → KeyValueStorage          (không phụ thuộc gì)
Step 3  → SessionManager           (không phụ thuộc gì)
Step 4  → QrHandshakeViewModel     (phụ thuộc CryptoManager — đã đúng)
Step 5  → CaptureIntruderUseCase   (phụ thuộc CryptoManager + KeyValueStorage)
Step 6  → ChatroomViewModel        (phụ thuộc SessionManager step 3)
Step 7  → FirebaseMessageRepository(không phụ thuộc gì)
Step 8  → SendImageUseCase         (không phụ thuộc gì)
Step 9  → SendImageUseCaseTest     (phụ thuộc step 8)
```

---

## Kiểm tra sau khi fix

```bash
./gradlew :shared:compileDebugKotlinAndroid     # Phải clean
./gradlew :shared:testDebugUnitTest             # Chạy unit tests
./gradlew :androidApp:assembleDebug             # Build APK
```

---

## Những gì KHÔNG cần fix (đã đúng)

- `AndroidModule.kt` — `MessageRepository` và `TokenRepository` đã được bind đúng ✓
- `FirebaseMessageRepository` constructor — đúng với AndroidModule ✓
- Tất cả screen composables (PinScreen, InboxScreen, etc.) — chưa thấy lỗi ✓
