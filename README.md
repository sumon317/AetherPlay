# AetherPlay

**AetherPlay** is a modern Android video and audio media player built with **Jetpack Compose**, **Material 3**, and **Jetpack Media3 (ExoPlayer)**. It combines local folder-first media discovery, gesture-driven cinema playback, a customizable music player, playlists, network streaming, and a 5-band hardware audio equalizer in a single privacy-focused app.

> **Current Release:** `v0.0.0-beta` (Pre-release) · Min SDK 24 (Android 7.0) · Target SDK 36

---

## Key Features

### Media Library & Discovery
- **All-Files Auto Scan**: Prompts for device storage access on launch and scans internal/external storage folders for supported video and audio files.
- **Folder-First Browsing**: Both **Videos** and **Audio** libraries default to Folder View, with instant toggling between Folder and Flat List/Grid views.
- **Format Support**:
  - **Video**: `MP4`, `MKV`, `WEBM`, `MOV`, `AVI`, `FLV`, `TS`, `M2TS`, `3GP`, `WMV`, `OGV`, plus `HLS (.m3u8)` and `DASH (.mpd)` streams.
  - **Audio**: `MP3`, `FLAC`, `AAC`, `M4A`, `OGG`, `OPUS`, `WAV`, `ALAC`, `AC3`, `EAC3`, `DTS`, and more.
- **Playlists & Favorites**: Create, rename, and manage mixed video/audio playlists, plus an auto-synced **Liked Media & Songs** collection.
- **Network Streams**: Open `http://`, `https://`, `rtsp://`, `rtmp://`, `rtp://`, and `udp://` streams with input scheme/host validation.

### Cinema Video Player
- **Gesture Controls** (with configurable sensitivities in Settings):
  - Left-side vertical drag for **Brightness**
  - Right-side vertical drag for **Volume**
  - Horizontal drag for **Timeline Scrubbing**
  - **Double-tap** left/right edges to seek (`10s` default, configurable) or center to play/pause
  - **Long-press** for `2.0x` speed boost
  - **Pinch-to-zoom** & screen lock
- **Subtitle Engine**: Supports embedded tracks and external `.srt`, `.vtt`, `.ass`/`.ssa`, and `.ttml` files with live sync delay adjustment, font family selection (`Plus Jakarta Sans`, `JetBrains Mono`, `Space Grotesk`), font sizing, and edge styling.
- **Playback Extras**: Picture-in-Picture (PiP), floating draggable mini-video overlay, docked mini-player bar, A–B loop points, playback speed control, sleep timer, aspect ratio modes, and background video-as-audio mode.

### Music & Audio Player
- **Customizable Artwork**: Choose between **Still (Rounded)** album art or **Rotate (Round Vinyl)** spinning artwork during playback.
- **Configurable Gestures**:
  - **Swipe Down / Swipe Up**: Map either gesture to *Minimise*, *Open Queue*, *Open Equalizer*, or *Open Liked Songs*.
  - **Double-Tap Artwork**: Map to *Like / Dislike* or *Open Subtitles / Lyrics*.
- **Transport & Utility**: Clear pill indicators for **Shuffle** and **Repeat** (`Off` / `All` / `One`), live audio spectrum visualizer, Up Next queue sheet, sleep timer, playback speed, and subtitle/lyric banner.
- **Task-Clear Cleanup**: Active playback stops and clears cleanly when the app is swiped away from Recents while saving your playback position.

### 5-Band Studio Equalizer
- Hardware-bound `Equalizer`, `BassBoost`, `Virtualizer`, and `LoudnessEnhancer` DSP pipeline attached to the Media3 audio session.
- **10 Built-in Presets**: Flat, Studio Monitor, Cinema, Bass Boost, Vocal Clarity, Electronic, Acoustic, Jazz, Rock, Night Mode.
- **Custom Profiles**: Save, apply, overwrite, and delete your own custom EQ profiles locally.

### Appearance, Privacy & Safety
- **Themes**: Obsidian Dark, Daylight Light, System Default, and Material You Dynamic Color (Android 12+) with edge-to-edge system bar icon contrast handling.
- **On-Device Privacy**: Local media index and history stay on-device (Room `database` and `file` domains are excluded from cloud backups).
- **Security Hardening**: Strict URL/URI validation for incoming `ACTION_VIEW` intents and stream dialogs, app-private directory isolation against path traversal, memory-bounded thumbnail downsampling, and size-capped subtitle parsing.

---

## Tech Stack

| Layer | Technologies |
| --- | --- |
| **UI & Navigation** | Jetpack Compose, Material 3, Navigation Compose (type-safe routes), Edge-to-Edge |
| **Media Engine** | AndroidX Media3 (`ExoPlayer`, `MediaSessionService`, `OkHttpDataSource`, `PlayerSurface`) |
| **Persistence** | Room (`SQLite` + KSP), Jetpack DataStore Preferences, Kotlinx Serialization |
| **Concurrency** | Kotlin Coroutines & `StateFlow` |
| **Build & Testing** | Gradle (AGP `9.1.1`, Kotlin `2.2.10`), JUnit 4, Robolectric |

---

## Building & Testing

### Prerequisites
- Android Studio / JDK 25 (configured via Gradle Daemon toolchain)
- Android SDK Platform 36

### Run Unit & Safety Tests
```bash
./gradlew :app:testDebugUnitTest
```

### Run Release Lint & Build Signed APK
```bash
./gradlew :app:lintVitalRelease :app:assembleRelease
```
Generated APKs automatically follow the naming rule `AetherPlay-v<versionName>-<variant>.apk` (for example, `AetherPlay-v0.0.0-beta-release.apk` in `app/build/outputs/apk/release/`).

> **Signing Note**: Release builds read `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` from environment variables (falling back to local development defaults if unset). Keystore files (`*.jks`, `*.keystore`) and build binaries (`*.apk`, `*.aab`) are excluded via `.gitignore`.

---

## Project Structure

```text
app/src/main/java/com/example/
├── AetherPlayApp.kt              # Application & manual DI AppContainer
├── MainActivity.kt               # Edge-to-edge host, permissions, PiP & Navigation
├── data/
│   ├── local/                    # Room database, DAOs, entities & DataStore settings
│   └── repository/               # Device media scanner, SAF tree & URI imports
├── player/                       # PlaybackService, MediaController holder, EQ DSP,
│                                 # SubtitleParser & MediaSafety URI/URL validators
└── ui/
    ├── components/               # MiniPlayerBar, FloatingMiniVideoOverlay, MediaThumbnail, dialogs
    ├── navigation/               # Type-safe Compose Navigation destinations
    ├── screens/                  # Videos, Audio, Playlists, Video/Audio Players, EQ, Settings, Gestures
    ├── theme/                    # Color schemes, typography, shapes & system bar styling
    └── viewmodel/                # Library, Player, Equalizer & Settings ViewModels
```
