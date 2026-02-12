<p align="center">
  <img src="banner.svg" alt="Dicta Banner" width="100%"/>
</p>

# Dicta

A modern, minimalistic Android dictation app that uses local/edge ASR (Automatic Speech Recognition) models for fully offline transcription. English-only.

## Features

- **Fully Offline** - All speech recognition happens on-device. No internet required after model download.
- **Real-time Streaming** - See your words transcribed as you speak.
- **Multiple Model Options** - Choose from 4 Vosk models based on your accuracy/storage needs.
- **Recording History** - Save and review past transcriptions with audio playback.
- **Export Data** - Export all recordings as a ZIP file with JSON metadata and audio files.
- **Modern UI** - Built with Jetpack Compose and Material3 design.
- **Privacy First** - Your voice data never leaves your device.

## Screenshots

| Home | Recording | History | Settings |
|------|-----------|---------|----------|
| Record and transcribe | Real-time transcription | Browse past recordings | Manage models |

## Models

| Model | Size | Accuracy | Best For |
|-------|------|----------|----------|
| **Small** | 50 MB | Basic | Quick notes, low storage devices |
| **Medium** | 128 MB | Good | Daily use (Recommended) |
| **Large** | 1.6 GB | Better | Important transcriptions |
| **XLarge** | 1.8 GB | Best | Maximum accuracy |

Models are downloaded on first launch. You can switch between models in Settings.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **ASR Engine**: [Vosk](https://alphacephei.com/vosk/)
- **Audio**: Android AudioRecord API (16kHz mono)

## Project Structure

```
app/src/main/java/com/example/dicta/
├── di/                 # Dependency injection modules
├── data/
│   ├── local/          # Room database
│   ├── repository/     # Repository implementations
│   └── preferences/    # DataStore preferences
├── domain/
│   ├── model/          # Domain models
│   └── repository/     # Repository interfaces
├── asr/
│   └── vosk/           # Vosk ASR engine implementation
├── audio/              # Audio recording
├── presentation/
│   ├── home/           # Main recording screen
│   ├── history/        # Recording history
│   ├── settings/       # Model management & export
│   └── onboarding/     # First-launch model selection
└── util/               # Utilities
```

## Building

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34+

### Build Debug APK

```bash
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

```bash
./gradlew assembleRelease
```

## Installation

1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from unknown sources" if prompted
3. Install and open the app
4. Select a model to download (Medium recommended)
5. Grant microphone permission
6. Start dictating!

## Permissions

- **RECORD_AUDIO** - Required for speech recognition
- **INTERNET** - Required for initial model download only
- **POST_NOTIFICATIONS** - For download progress notifications

## Export Format

When you export recordings, you get a ZIP file containing:

```
dicta_export_[timestamp].zip
├── recordings.json      # Metadata for all recordings
├── audio_1_[name].wav   # Audio file for recording 1
├── audio_2_[name].wav   # Audio file for recording 2
└── ...
```

The `recordings.json` contains:
```json
{
  "exportedAt": "2024-01-15T10:30:00Z",
  "appVersion": "1.0",
  "recordingCount": 5,
  "recordings": [
    {
      "id": 1,
      "title": "Recording - Jan 15, 10:30 AM",
      "transcription": "Your transcribed text here...",
      "durationMs": 15000,
      "createdAt": "2024-01-15T10:30:00Z",
      "modelUsed": "VOSK_MEDIUM_EN_US",
      "audioFile": "audio_1_recording.wav"
    }
  ]
}
```

## License

This project is open source. The Vosk library and models are licensed under Apache 2.0.

## Credits

- [Vosk](https://alphacephei.com/vosk/) - Offline speech recognition toolkit
- [Alpha Cephei](https://alphacephei.com/) - Vosk model providers

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
