# Vosk to Moonshine Migration

This document describes the migration of Dicta's ASR backend from Vosk to Moonshine Voice, and the accompanying UI modernization.

## Why Moonshine

[Moonshine Voice](https://github.com/moonshine-ai/moonshine) by Useful Sensors is an open-source, on-device speech-to-text engine that significantly outperforms Vosk on every metric that matters for a dictation app:

| Metric | Vosk (best model) | Moonshine Small Streaming |
|--------|-------------------|---------------------------|
| Word Error Rate | ~15% | 7.84% |
| Model Size | 1.8 GB (XLarge) | 100 MB |
| Streaming | Yes | Yes |
| Latency | Good | Better (ergodic encoder) |

Moonshine's smallest streaming model (Tiny, 26 MB) is already more usable than Vosk's largest. The Small Streaming model (100 MB, 7.84% WER) is the recommended default -- more accurate than Vosk XLarge at 1/18th the size.

## What Changed

### Build System (Phase 1)

**Dependency swap:**
- Removed `com.alphacephei:vosk-android:0.3.47`
- Added `ai.moonshine:moonshine-voice:0.0.48`
- Removed JitPack repository (only needed for Vosk)

**SDK targets:**
- `minSdk` raised to 35 (Moonshine requirement for ARM64 ONNX Runtime)
- `targetSdk` kept at 34 (Moonshine's native libs are not yet 16KB page-aligned, same issue Vosk had)

### ASR Engine (Phase 2)

Replaced `VoskEngine` with `MoonshineEngine`, both implementing the `AsrEngine` interface.

Key architectural difference: Vosk used a `Recognizer` that accepted raw byte arrays and returned JSON strings that needed parsing. Moonshine uses a `Transcriber` with a proper event-based API:

```
Vosk flow:
  ShortArray -> ByteArray -> recognizer.acceptWaveForm(bytes) -> JSON -> parse text

Moonshine flow:
  ShortArray -> FloatArray -> transcriber.addAudio(floats, sampleRate) -> event callbacks
```

We use the bare `Transcriber` class (not `MicTranscriber`) to keep control of audio capture via our own `AudioRecorder`. This lets us save WAV files while transcribing.

The `AsrEngine` interface gained a `modelArch` parameter on `initialize()` since Moonshine needs to know which model architecture to load (tiny, base, small streaming, medium streaming, etc.).

**Event bridging:**
- `TranscriptEvent.LineTextChanged` -> `TranscriptionResult.Partial`
- `TranscriptEvent.LineCompleted` -> `TranscriptionResult.Final`
- `TranscriptEvent.Error` -> `TranscriptionResult.Error`

### Model Management (Phase 3)

**Old Vosk models:**
| Model | Size | Notes |
|-------|------|-------|
| Small | 50 MB | Basic accuracy |
| Medium | 128 MB | Default |
| Large | 1.6 GB | Better |
| XLarge | 1.8 GB | Best |

**New Moonshine models:**
| Model | Arch Constant | Size | WER | Use Case |
|-------|--------------|------|-----|----------|
| Tiny Streaming | 2 | ~26 MB | 12% | Quick notes, constrained devices |
| Small Streaming | 4 | ~100 MB | 7.84% | Daily use (default) |
| Medium Streaming | 5 | ~200 MB | 6.65% | Best real-time accuracy |
| Base (non-streaming) | 1 | ~50 MB | ~10% | File transcription |

**Model file format change:**
- Vosk: complex directory structure with `am/`, `conf/`, `graph/`, `ivector/` subdirectories
- Moonshine: flat, 3 files -- `encoder_model.ort`, `decoder_model_merged.ort`, `tokenizer.bin`

**Migration safety:** `UserPreferences` wraps the stored model enum name in a try/catch so users upgrading from Vosk (with `VOSK_MEDIUM_EN_US` stored) gracefully fall back to `MOONSHINE_SMALL_STREAMING`.

### ViewModel Wiring (Phase 4)

Minimal changes -- the Flow-based transcription observation pattern is identical between Vosk and Moonshine. Only the `initialize()` call changed to pass `archConstant`, and default model references were updated.

### UI Modernization (Phase 5)

**Bottom navigation:** Replaced History/Settings icons in the TopAppBar with a proper `NavigationBar` at the bottom. Three tabs: Record, History, Settings. Hidden during onboarding.

**Theme:** With `minSdk = 35`, dynamic color (Material You) is guaranteed on all devices. Removed the static fallback color schemes and the SDK version check.

**HomeScreen:**
- Clean TopAppBar with just "Dicta" title
- `SelectionContainer` wrapping transcription text for copy/paste
- Partial text rendered in a lighter color during recording
- Labeled action buttons (`FilledTonalButton`) -- "Copy", "Save", "Clear" -- instead of icon-only `IconButton`s
- Buttons only visible when there's text (animated)

**OnboardingScreen:**
- `ElevatedCard` for the recommended model, `OutlinedCard` for others
- `SuggestionChip` "Recommended" badge
- WER displayed on each card alongside size

**SettingsScreen:**
- `ListItem` composable for the data management section
- WER displayed on model cards

**HistoryScreen / SettingsScreen:**
- Removed back navigation icons (handled by bottom nav)

## Model Hosting

Models need to be hosted as zip files containing the 3 model files. The download URLs in `AsrModel.kt` currently point to a placeholder GitHub Release (`models-v1` tag).

To prepare model files:
1. `pip install moonshine-voice`
2. `python -m moonshine_voice.download --language en` for each architecture
3. Zip each model's 3 files
4. Upload to a GitHub Release or other hosting

## Known Limitations

- **ARM64 only**: Moonshine's ONNX Runtime native libs are ARM64. No x86/emulator support.
- **16KB page alignment**: `libmoonshine-jni.so` and `libmoonshine.so` have LOAD segments not aligned to 16KB. This blocks `targetSdk = 35` and Play Store submission for Android 15+ until upstream fixes it.
- **Model download URLs**: Need actual hosted model files before the app is functional end-to-end.

## Files Changed

| Action | File |
|--------|------|
| Modify | `gradle/libs.versions.toml` |
| Modify | `app/build.gradle.kts` |
| Modify | `settings.gradle.kts` |
| Modify | `asr/AsrEngine.kt` |
| Create | `asr/moonshine/MoonshineEngine.kt` |
| Delete | `asr/vosk/VoskEngine.kt` |
| Modify | `di/AsrModule.kt` |
| Rewrite | `domain/model/AsrModel.kt` |
| Modify | `data/repository/ModelRepositoryImpl.kt` |
| Modify | `data/preferences/UserPreferences.kt` |
| Modify | `presentation/home/HomeViewModel.kt` |
| Modify | `presentation/onboarding/OnboardingViewModel.kt` |
| Modify | `presentation/settings/SettingsViewModel.kt` |
| Simplify | `presentation/theme/Color.kt` |
| Simplify | `presentation/theme/Theme.kt` |
| Refine | `presentation/theme/Type.kt` |
| Rewrite | `presentation/MainActivity.kt` |
| Rewrite | `presentation/navigation/DictaNavHost.kt` |
| Rewrite | `presentation/home/HomeScreen.kt` |
| Rewrite | `presentation/onboarding/OnboardingScreen.kt` |
| Rewrite | `presentation/settings/SettingsScreen.kt` |
| Modify | `presentation/history/HistoryScreen.kt` |
