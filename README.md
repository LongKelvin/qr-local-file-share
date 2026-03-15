# QLFS — QR Local File Share

Share files from your Android device to **any device nearby** — no cables, no cloud, no extra apps.

QLFS creates its **own Wi-Fi hotspot**, generates **two QR codes**, and lets receivers download files directly through a browser.

No installation required on the receiving device.

---

## Features

- **Built-in Wi-Fi hotspot** — the app automatically creates a local hotspot using Android's `LocalOnlyHotspot`
- **Two-step QR workflow** — scan QR to join hotspot, then scan QR to open download page
- **No receiver app required** — works with any browser (Android, iOS, Windows, Linux, Mac)
- **Offline sharing** — no internet required, works completely air-gapped
- **Multi-file sessions** — share multiple files at once
- **Token-secured sessions** — 16-byte random token, 15-minute TTL, prevents unauthorized downloads
- **Foreground service** — keeps the file server alive while sharing
- **Range request support** — browsers can resume interrupted downloads
- **Max 5 concurrent connections** — protects device from overload
- **Offline HTML download page** — no CDN or internet dependency
- **File type detection** — icons and badges for audio, video, images, PDF, archives, text files
- **Download counter** — shows how many times files were downloaded during the session
- **Guided permission onboarding** — explains why permissions are needed before requesting them

---

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1.1+ |
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

Open the folder and wait for **Gradle sync** to complete.

### 3. Run on device

```bash
./gradlew installDebug
```

Or press **Run ▶** in Android Studio.

> **A physical device is required.** Android emulators cannot create a Wi-Fi hotspot.

---

## Build from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Install debug build directly to connected device
./gradlew installDebug

# Run unit tests
./gradlew test
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## How It Works

1. User selects files to share
2. User taps **Start Sharing**
3. App calls `WifiManager.startLocalOnlyHotspot()` — Android creates a temporary hotspot and returns the SSID, password, and gateway IP
4. The app starts a **NanoHTTPD file server** on the gateway IP with a session token
5. The UI displays **two QR codes**:

**QR #1 — Join hotspot**
```
WIFI:T:WPA2;S:<ssid>;P:<password>;;
```
Scanning this QR connects the device to the hotspot.

**QR #2 — Open download page**
```
http://192.168.49.1:8080/?token=<session-token>
```
The browser opens the styled download page.

6. Receiver downloads files directly from the phone. Files stream from the Android **ContentResolver** — never fully loaded into memory.
7. Session ends when the user presses **Stop Sharing** or 15 minutes expire. Hotspot and server shut down automatically.

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
| Architecture | MVVM + ViewModel (AndroidX Lifecycle 2.8.0) |
| Testing | JUnit 4 + MockK 1.13.11 |

---

## HTTP API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/?token=<t>` | Returns HTML download page |
| `GET` | `/download?token=<t>&index=<n>` | Streams file `n` with range-request support |
| `HEAD` | `/download?token=<t>&index=<n>` | Returns file headers without body |
| `GET` | `/thumbnail?token=<t>&index=<n>` | Returns thumbnail image for file `n` |

Invalid tokens return `403 Forbidden`.

---

## Permissions

| Permission | API level | Reason |
|-----------|-----------|--------|
| `INTERNET` | all | Run embedded HTTP server |
| `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` / `CHANGE_WIFI_STATE` | all | Start and manage Wi-Fi hotspot |
| `NEARBY_WIFI_DEVICES` | 33+ | Start local-only hotspot without location access |
| `ACCESS_FINE_LOCATION` | ≤32 | Required by Android hotspot API |
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
2. Create a branch: `feat/your-feature` or `fix/your-bug`
3. Commit using [Conventional Commits](https://www.conventionalcommits.org/): `feat(scope): message`
4. Open a pull request with the same format as the commit title

---

## License

This project is open source and free for personal or commercial use.

---

## Author

**Long Kelvin** · [github.com/LongKelvin](https://github.com/LongKelvin/)
