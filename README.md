# BB3Chat

Ứng dụng nhắn tin bảo mật đa nền tảng (Android + iOS), xây dựng bằng **Kotlin Multiplatform** và **Compose Multiplatform**. Giao diện ngụy trang dạng máy tính, xác thực bằng PIN kép (thật / giả), mã hóa end-to-end ở tầng client, lưu trữ local bằng **SQLCipher**, đồng bộ tin nhắn qua **Firebase Firestore** và push **FCM**.

| | |
|---|---|
| **Bundle ID** | `com.bb3.bb3chat` |
| **Min Android** | API 26 |
| **Min iOS** | 15.0 |
| **Firebase project** | `bb3chat` |

---

## Tính năng chính

- **Ngụy trang** — Màn hình máy tính iOS-style; nhập secret code để mở khóa app
- **Dual PIN** — PIN thật → inbox chat; PIN giả → không gian decoy (ghi chú an toàn)
- **Mã hóa** — PBKDF2 (PIN → session key), AES-GCM cho tin nhắn và ảnh inline
- **SQLCipher** — Database local được mã hóa bằng session key
- **Firebase** — Anonymous Auth, Firestore sync, FCM data-only (self-destruct, room events)
- **Ghép phòng** — QR / mã 8 ký tự, room key dùng chung
- **Self-destruct** — Tin nhắn tự hủy với countdown overlay
- **Panic** — Lật máy / wipe session key, quay về màn máy tính
- **Intruder capture** — Chụp ảnh im lặng sau nhiều lần nhập PIN sai (Android)
- **Safe Hours** — Khóa app trong khung giờ cấu hình
- **VIP Store** — UI gói premium (placeholder thanh toán)

---

## Kiến trúc

```
CalculatorMask ──secret code──▶ PinScreen ──real PIN──▶ Inbox ──room──▶ Chatroom
                                      └──decoy PIN──▶ DecoyInbox
```

| Layer | Công nghệ |
|-------|-----------|
| UI | Compose Multiplatform, UDF (ViewModel + State/Effect) |
| DI | Koin |
| Local DB | SQLDelight + SQLCipher |
| Network | Ktor (WebSocket / HTTP) |
| Backend | Firebase Auth, Firestore, FCM |
| Crypto | PBKDF2, AES-GCM; iOS dùng CryptoKit qua Swift bridge |

**Clean Architecture** trong `shared`: `presentation` → `domain` (use case, repository interface) → `data` (implementation). Platform-specific code nằm ở `androidMain` / `iosMain`.

### iOS bridges (Swift ↔ Kotlin)

| Bridge | Chức năng |
|--------|-----------|
| `CryptoBridge` | AES-GCM (CryptoKit) |
| `AuthBridge` | Firebase Anonymous Auth |
| `FirestoreBridge` | Firestore read/write qua JSON |
| `ImagePickerBridge` | PHPicker chọn ảnh |
| `PushHandler` | FCM token & notification |

---

## Cấu trúc thư mục

```
BB3Chat/
├── androidApp/          # Android entry (MainActivity, Application, FCM service)
├── iosApp/              # Xcode project, Swift bridges, Podfile
├── shared/
│   ├── commonMain/      # UI, domain, crypto, SQLDelight schema
│   ├── androidMain/     # Firebase Android, SQLCipher driver
│   ├── iosMain/         # Bridges, iOS Firebase repo, SQLCipher native
│   └── commonTest/      # Unit tests (crypto, auth, messaging)
├── firestore.rules      # Firestore security rules
└── firebase.json
```

---

## Yêu cầu

- **JDK 17+**
- **Android Studio** Ladybug trở lên (AGP 8.5, compileSdk 35)
- **Xcode 15+** (iOS; đã test với Xcode 26)
- **CocoaPods** (`pod install` trong `iosApp/`)
- Tài khoản **Firebase** với project `bb3chat`

---

## Cài đặt

### 1. Clone repo

```bash
git clone https://github.com/leruyn/BB3Chat.git
cd BB3Chat
```

### 2. Firebase config (không commit lên git)

**Android** — tải `google-services.json` từ Firebase Console, đặt vào:

```
androidApp/google-services.json
```

**iOS** — copy template và điền giá trị thật:

```bash
cp iosApp/iosApp/GoogleService-Info.plist.example iosApp/iosApp/GoogleService-Info.plist
```

Trong Firebase Console cần bật:
- **Anonymous Authentication**
- **Cloud Firestore**
- **Cloud Messaging**

Deploy Firestore rules:

```bash
firebase deploy --only firestore:rules
```

### 3. Build Android

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug   # cài lên thiết bị/emulator
```

### 4. Build iOS

```bash
# Kotlin framework (simulator Apple Silicon)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# CocoaPods
cd iosApp && pod install && cd ..

# Mở workspace (không mở .xcodeproj)
open iosApp/iosApp.xcworkspace
```

Trong Xcode: chọn target **iosApp** → Run trên simulator hoặc thiết bị.

> **Lưu ý SQLCipher iOS:** Sau khi bật mã hóa DB, cần gỡ cài đặt app cũ trước khi cài lại (DB plain-text cũ không tương thích).

---

## Lần đầu sử dụng

1. Mở app → màn **máy tính**
2. Nhập secret code mặc định: `14022026`
3. **Thiết lập PIN** (3 bước):
   - **(1/3)** Tạo PIN thật (4 số)
   - **(2/3)** Xác nhận PIN thật
   - **(3/3)** Tạo PIN giả — **phải khác** PIN thật
4. Vào **Inbox** → ghép phòng qua QR hoặc mã 8 ký tự

Lần sau: secret code → nhập PIN thật hoặc PIN giả.

---

## Chạy tests

```bash
./gradlew :shared:testDebugUnitTest
```

Bao gồm test cho crypto, session, PIN auth flow, gửi tin nhắn/ảnh.

---

## Debug

**Android logcat (crash):**

```bash
adb logcat -c
adb shell am start -n com.bb3.bb3chat/.MainActivity
adb logcat -d | grep -E "FATAL|AndroidRuntime|bb3chat"
```

**Reset dữ liệu app (Android):**

```bash
adb shell pm clear com.bb3.bb3chat
```

---

## Bảo mật

- Không commit `google-services.json`, `GoogleService-Info.plist`, keystore
- Session key chỉ tồn tại trong RAM; panic / thoát app wipe key
- Firestore chỉ lưu payload đã mã hóa (base64); rules giới hạn kích thước document
- PIN hash lưu dạng derived key hex, không lưu plaintext PIN

---

## Trạng thái nền tảng

| Tính năng | Android | iOS |
|-----------|---------|-----|
| UI + navigation | ✅ | ✅ |
| Firebase Auth + Firestore | ✅ | ✅ (Swift bridge) |
| SQLCipher | ✅ | ✅ |
| FCM push | ✅ | ✅ |
| Image pick / hiển thị | ✅ | ✅ |
| Intruder camera | ✅ | — |

---

## License

Private project — all rights reserved.
