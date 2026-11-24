# RedX — AI Assistant Mobile Client (Kotlin & Jetpack Compose)

**RedX** is a next-generation Android client for local and remote AI coding agents (compatible with OpenCode server protocol). Built 100% natively from scratch with Jetpack Compose, Material 3, Hilt, Room, and OkHttp/WebSocket streaming.

---

## 🚀 Key Features

- **Real-Time Streaming**: Live token-by-token streaming via persistent WebSocket (`/api/event`) with automatic exponential backoff reconnection.
- **Model Selector**: Switch between local and cloud LLMs (Claude, GPT-4o, Gemini, DeepSeek) with model search and provider categorization.
- **Voice Calls & Dictation**: Full-duplex voice interaction screen with real-time audio waveform visualization and background foreground service.
- **Multi-Session Management**: Sessions drawer with pinned chats, folder categorization, rename, and swipe-to-delete.
- **Code & Tool Execution Display**: Syntax-highlighted code blocks, interactive tool invocation cards (`session.tool.called`), and collapsible argument/result views.
- **QR & Manual Pairing**: Pair with your OpenCode server seamlessly by scanning a desktop QR code or entering IP and port manually.
- **Offline Outbox & Local Storage**: Room SQLite database with full offline cache and automated sync queue.
- **Homescreen Widget**: Android Glance Material 3 homescreen widget for 1-tap quick chat launching.
- **Obsidian Dark Theme**: Pixel-perfect OLED dark palette (`#161616`), custom typography (Inter + OpenMono), and smooth spring physics animations.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **UI** | 100% Jetpack Compose, Material 3, Custom Spring Animations |
| **Typography** | Inter & OpenMono bundled fonts |
| **Architecture** | MVVM + Clean Architecture (Data / Domain / Presentation) |
| **Dependency Injection** | Dagger Hilt 2.54 |
| **Database** | Room 2.7.0 (SQLite with Kotlin Coroutines Flow) |
| **Preferences** | Jetpack DataStore Preferences |
| **Networking** | OkHttp 4.12.0 + WebSocket + Kotlinx Serialization JSON |
| **Image Loading** | Coil 3 (with SVG decoder) |
| **Widget** | Jetpack Glance 1.1.1 |
| **CI / CD** | GitHub Actions Automated Release APK Build |

---

## 📦 Building via GitHub Actions

This project is configured with automated CI/CD via GitHub Actions (`.github/workflows/build.yml`).

### How to trigger a build:
1. Push your code to `main` or create a tag (e.g. `v1.0.0`).
2. Alternatively, navigate to **Actions** → **Build RedX Android APK** → **Run workflow**.
3. Once the workflow finishes, download the signed APK artifact directly from the GitHub Actions summary page!

### Custom Keystore (Optional):
To sign with your own private release key, add the following GitHub Secrets to your repository:
- `KEYSTORE_BASE64`: Base64 encoded `.jks` file
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password

If no secrets are provided, the workflow automatically generates a standalone release keystore during the build.
