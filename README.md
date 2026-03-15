# QLFS — QR Local File Share

Share files from your Android device to any device — no cables, no cloud, no extra apps. Your phone creates its own Wi-Fi hotspot, others connect by scanning a QR code, then download files directly from your phone via a second QR code.

---

## Features

- **Self-contained hotspot sharing** — app starts a local Wi-Fi AP automatically; no router required
- **Two-step QR flow** — one QR to join the hotspot, one QR to download files
- **No receiver app needed** — works with any browser on any device
- **Multi-file support** — share multiple files in one session
- **Token-secured sessions** — 16-byte random token + 15-minute TTL per session
- **Foreground service** — server stays alive with a persistent notification
- **Range request support** — browsers can resume interrupted downloads
- **Max 5 concurrent connections** — bounded thread pool prevents abuse
- **Offline HTML download page** — no external CDN, fully air-gapped
- **File type detection** — badges and icons for audio, video, PDF, archive, image, text
- **Download counter** — see how many times files were downloaded per session
- **Permission gate** — guided onboarding screen explains each permission before requesting

---

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1.1+ (or Meerkat) |
| JDK | 17+ |
| Android SDK | Compile SDK 35, Min SDK 26 (Android 8.0+) |
| Gradle | 8.7 (wrapper included) |
| Kotlin | 2.0+ |

---

## Clone & Run

### 1. Clone the repository

```bash
git clone https://github.com/LongKelvin/qr-local-file-share.git
cd qr-local-file-share
```

### 2. Open in Android Studio

- **File → Open** and select the cloned folder
- Wait for Gradle sync to complete (downloads dependencies automatically)

### 3. Build & run

Connect a physical device, then:

```bash
./gradlew installDebug
```

Or press **Run ▶** in Android Studio.

> **Note:** A physical device is required. The app starts a local Wi-Fi hotspot — this does not work on emulators.

---

## Build from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Install debug directly to connected device
./gradlew installDebug

# Run unit tests
./gradlew test
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## How It Works

1. App launches → permission gate requests **Nearby Wi-Fi Devices** (API 33+) or **Location** (API ≤32) + **Notifications**
2. User selects one or more files
3. User taps **Start Sharing**
4. App calls `WifiManager.startLocalOnlyHotspot()` — Android creates a dedicated Wi-Fi AP and returns SSID + password + gateway IP
5. A NanoHTTPD file server starts on the gateway IP with a session token
6. The sharing screen shows **two QR codes**:
   - **Step 1 QR** — `WIFI:T:WPA2;S:<ssid>;P:<pass>;;` → camera app prompts "Join this network?"
   - **Step 2 QR** — `http://192.168.49.1:8080/?token=<token>` → browser opens the download page
7. Each file streams directly from the Android content resolver (never fully loaded into memory)
8. Session expires after 15 minutes or when the user taps **Stop Sharing** (hotspot is also torn down)

---

## Project Structure

```
app/src/main/kotlin/com/example/qlfs/
├── MainActivity.kt               # Permission gate, Compose host, screen routing
├── QLFSApplication.kt            # Hilt application class
├── di/
│   └── AppModule.kt              # Hilt DI bindings
├── model/
│   └── SharedFile.kt             # Parcelable file metadata model
├── network/
│   ├── NetworkManager.kt         # Local IPv4 detection (Wi-Fi + hotspot mode)
│   └── HotspotManager.kt         # Local-only hotspot lifecycle (start/stop)
├── qr/
│   └── QrGenerator.kt            # ZXing QR bitmap generator
├── server/
│   ├── FileServer.kt             # NanoHTTPD server (/, /download, /thumbnail)
│   └── DownloadPageRenderer.kt   # Offline HTML page generator
├── service/
│   └── ShareForegroundService.kt # Foreground service, notification, port retry
├── share/
│   ├── SessionManager.kt         # Token generation, TTL enforcement
│   ├── ShareController.kt        # Global state: download count, server flags
│   └── TransferTracker.kt        # Transfer lifecycle events (sealed class)
├── ui/
│   ├── ShareScreen.kt            # File picker + idle screen
│   ├── SharingActiveScreen.kt    # Two-step QR screen (hotspot + download)
│   ├── AboutScreen.kt            # App info + GitHub link
│   └── ShareViewModel.kt         # State machine + hotspot orchestration
└── util/
    ├── FileSizeFormatter.kt      # Human-readable file sizes
    └── TokenStore.kt             # Secure random token generator
```

---

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material 3 |
| HTTP server | NanoHTTPD 2.3.1 |
| QR code | ZXing 3.5.3 |
| Dependency injection | Hilt (Dagger) 2.51 |
| Image loading | Coil 2.6.0 |
| Async / state | Kotlin Coroutines + StateFlow |
| ViewModel | AndroidX Lifecycle 2.8.0 |
| Testing | JUnit 4 + MockK 1.13.11 |

---

## HTTP API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/?token=<t>` | Returns HTML download page |
| `GET` | `/download?token=<t>&index=<n>` | Streams file `n` with range-request support |
| `HEAD` | `/download?token=<t>&index=<n>` | Returns file headers without body |
| `GET` | `/thumbnail?token=<t>&index=<n>` | Returns thumbnail image for file `n` |

All requests require a valid session token. Invalid or expired tokens receive `403 Forbidden`.

---

## Permissions

| Permission | API level | Reason |
|-----------|-----------|--------|
| `INTERNET` | all | Run embedded HTTP server |
| `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` / `CHANGE_WIFI_STATE` | all | Start and manage Wi-Fi hotspot |
| `NEARBY_WIFI_DEVICES` | 33+ | Start local-only hotspot without location access |
| `ACCESS_FINE_LOCATION` | ≤32 | Required by Android to start a Wi-Fi hotspot |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | all | Keep server alive in background |
| `POST_NOTIFICATIONS` | 33+ | Show persistent sharing notification |
| `READ_EXTERNAL_STORAGE` | ≤32 | File access on Android 12 and below |

All runtime permissions are explained to the user at launch with a rationale card before the system dialog appears.

---

## Running Tests

```bash
./gradlew test
```

Test coverage includes:
- `NetworkManagerTest` — IP detection edge cases
- `SessionManagerTest` — token validation and TTL expiry
- `FileSizeFormatterTest` — formatting for B, KB, MB, GB, TB

---

## Contributing

1. Fork the repository
2. Create a branch: `feat/your-feature` or `fix/your-fix`
3. Commit using [Conventional Commits](https://www.conventionalcommits.org/): `feat(scope): message`
4. Open a pull request with the same format as the commit title

---

## License

This project is open source and freely available for personal and commercial use.

---

## Author

**Long Kelvin** · [github.com/LongKelvin](https://github.com/LongKelvin/)
