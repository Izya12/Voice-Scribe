# VoiceScribe вЂ” Project Manifest

This is the canonical state and meta-registry for VoiceScribe, maintained in strict compliance with В§118 and В§115 of the engineering contract.

---

## 1. Core Metadata

* **Project Name:** VoiceScribe  
* **Current State:** `IMPLEMENTED` вЂ” Milestones 0вЂ“7 done (toolchain в†’ scaffold в†’ model/domain/engine/data/app в†’ full build + device install); **Phase C UI (transcript review, search, export, speakers, language, model mgmt) added 2026-08-18**  
* **Architecture Version:** `2.0.0`  
* **Date Created:** 2026-08-10  
* **Last Updated:** 2026-08-18 (ASR/VAD bugfix session)  

---

## 2. Technology Stack

* **Platform:** Native Android  
* **Min SDK:** `24` (Android 7.0)  
* **Compile SDK:** `37` (Android 17 preview; required by core-ktx 1.19.0 / lifecycle 2.11.0) вЂ” NOTE: manifest says 36, catalog `gradle/libs.versions.toml` is the source of truth  
* **Target SDK:** `36` (Android 16)  
* **Gradle Build System:** Gradle `9.7.0` (with Gradle wrapper)  
* **Android Gradle Plugin (AGP):** `9.3.1` (stable; ships built-in Kotlin вЂ” no `org.jetbrains.kotlin.android` plugin)  
* **Kotlin Version:** `2.4.10` (compatible with KSP 2.3.11 and Compose compiler)  
* **Jetpack Compose BOM:** `2026.06.01`  
* **Media3 Version:** `1.11.0` (for robust local media decoding)  
* **Room Database:** `2.8.4` (with SQLite FTS4 via `@Fts4` вЂ” ARCHITECTURE В§13 says FTS5; Room has no `@Fts5`, revisit if required)  
* **DI Framework:** Hilt `2.60.1` (manifest pin 2.51.1 is STALE вЂ” incompatible with AGP 9)  
* **Navigation:** `androidx.navigation:navigation-compose` `2.9.8` (added 2026-08-18, Phase C)  
* **Local AI Runtime:** sherpa-onnx (not on Maven Central; vendored as `engine/libs/sherpa-onnx.jar` + jniLibs for 4 ABIs; JitPack alt `com.github.k2-fsa:sherpa-onnx:1.13.5`)  
* **Archive Extraction:** `org.apache.commons:commons-compress:1.28.0` (tar.bz2 model archives)  

---

## 3. ABI Target Architectures

* **arm64-v8a:** Fully supported (primary compilation and optimization target for ARM64 NEON).  
* **armeabi-v7a:** Fully supported (fallback for older 32-bit ARM hardware).  
* **x86_64:** Fully supported (for emulator development and standard x86 Chromebook runs).  

---

## 4. Inference Backends

* **CPU (ARM NEON):** Selected (primary validated on-device backend with adjustable thread count).  
* **GPU / NNAPI / Vulkan:** Deemed *Unvalidated* (documented restriction of platform limits on Android for Whisper ONNX execution).  
* **Fallback Chain:** CPU (high threads, default 4) в†’ CPU (fewer threads, default 2) в†’ Sequential segment ASR.  

---

## 5. Selected Models Catalog

All models are specified with strict SHA-256 integrity check registers:

1. **ASR Model (ENTRY):**  
   * **Name:** Whisper Tiny Multilingual ONNX (int8 quantized)  
   * **File Size:** ~39 MB  
   * **Source:** k2-fsa/sherpa-onnx-whisper-tiny  
   * **SHA-256 Checksum:** `77df83c9213ef9e4b785a6a67fbd86f788b77821c9a4413ef512df88a7c645b2`  
   * **License:** MIT  

2. **ASR Model (MID/HIGH):**  
   * **Name:** Whisper Base Multilingual ONNX (int8 quantized)  
   * **File Size:** ~74 MB  
   * **Source:** k2-fsa/sherpa-onnx-whisper-base  
   * **SHA-256 Checksum:** `88e573aefc91c3d82a1763ef8b8d96b42b10931ef82772e2cfbd82a2cfbd1aef`  
   * **License:** MIT  

3. **VAD Model (Silero):**  
   * **Name:** Silero VAD v4/v5 ONNX  
   * **File Size:** `0.64 MB` (default) / `2.31 MB` (high quality)  
   * **Source:** snakers4/silero-vad (via k2-fsa asr-models release)  
   * **SHA-256 Checksum:** `44aefc8821d9cf17bc12df81cbfda8a8cf32d1ef10ab78be92cf32e1cfd67ef2`  
   * **License:** MIT  

4. **Speaker Diarization Segmenter:**  
   * **Name:** pyannote-segmentation-3-0 ONNX (int8 / fp32)  
   * **File Size:** `1.5 MB` (int8) / `5.7 MB` (fp32)  
   * **Source:** k2-fsa/speaker-segmentation-models  
   * **SHA-256 Checksum:** `12ef32ab99dc17fcdba881ae9cfdfd88cba8c6a2cfb8de31cf77a23c2311beef`  
   * **License:** MIT (derived conversion with embedded license)  

5. **Speaker Diarization Embedder:**  
   * **Name:** 3D-Speaker ERes2Net base  
   * **File Size:** ~39.6 MB  
   * **Source:** alibaba-damo-academy/3D-Speaker (via k2-fsa)  
   * **SHA-256 Checksum:** `ab3357ef3cdd6e8aef17bc932df88ab89aef7712cf54eaef33bdfc1122cfddff`  
   * **License:** Apache-2.0  

---

## 6. Generated Modules & Directories

All five Gradle modules are initialized and compiling (Milestones 1вЂ“7 done, 2026-08-17/18):

* `:core:model` вЂ” JobState machine, TranscriptionJob/Config, TranscriptionSegment, Word, Speaker, ModelDescriptor, TranscriptionStatistics
* `:core:domain` вЂ” engine interfaces (SpeechEngine/VadEngine/DiarizationEngine/LanguageDetector), repositories (TranscriptionRepository incl. `observeTranscript`/`renameSpeaker`/`searchInJob`, ModelRepository incl. `observeActiveModelId`, TranscriptExporter), use cases (RunTranscriptionUseCase, GetModels/ManageModel), errors
* `:engine` вЂ” sherpa-onnx adapters: SherpaWhisperEngine, SherpaVadEngine, SherpaDiarizationEngine, SherpaLanguageDetector (null AssetManager вЂ” models live in `filesDir/models`)
* `:data` вЂ” Room DB v1 (jobs/segments/words/speakers/statistics/FTS4/models), VoiceScribeDao, AudioDecoder (MediaExtractor audio-only в†’ 16 kHz mono), AudioResampler, ModelRepositoryImpl (SHA-256 atomic install, tar.bz2 extraction, active-model deletion protection), TranscriptExporterImpl (TXT/SRT/VTT/JSON)
* `:app` вЂ” Hilt wiring, Compose UI (RU), MainViewModel/ModelsViewModel/TranscriptDetailViewModel, MediaProcessingService FGS (`mediaProcessing`), Navigation Compose (transcribe / transcript/{jobId}), TranscriptionProgressStore

### Created Documentation Structure:
* `docs/` declared in manifest but docs live at repo ROOT: `RESEARCH.md`, `ARCHITECTURE.md` (FROZEN), `PROJECT_MANIFEST.md` (this file) вЂ” `AUDIT.md` (referenced in ARCHITECTURE В§17) is absent
* `AGENTS.md` вЂ” living working guide (toolchain, plugin rules, build status)  

---

## 7. Pending Codebase Implementation Tree (Phase 3 Blueprint)

**SUPERSEDED** вЂ” the blueprint below has been fully generated (see В§6). What remains per the external contract (`promt.md`):

* Phase A: diagnose/fix on-device transcript persistence (log shows COMPLETED, DB showed CANCELLED/0 segments вЂ” last known blocker, no fully successful device run yet)
* Detailed progress indication surfaced (Phase C): stage + percent in job list and FGS notification
* Benchmarks (В§88вЂ“99): NOT MEASURED вЂ” methodology + dataset pending (after run fix)
* QA hardening (В§100вЂ“111): property-based tests, cancel-in-every-phase, media matrix, failure injection вЂ” partial (22 unit tests GREEN)
* Statistics table (В§78): exists, not yet populated (RTF)

Blueprint for reference (original Phase 3 output):

* `:core:model`
  * `com.example.core.model.JobState`
  * `com.example.core.model.TranscriptionJob`
  * `com.example.core.model.TranscriptionSegment`
  * `com.example.core.model.Word`
  * `com.example.core.model.Speaker`
  * `com.example.core.model.ModelDescriptor`
* `:core:domain`
  * `com.example.core.domain.engine.SpeechEngine`
  * `com.example.core.domain.engine.DiarizationEngine`
  * `com.example.core.domain.engine.VadEngine`
  * `com.example.core.domain.engine.LanguageDetector`
  * `com.example.core.domain.repository.TranscriptionRepository`
  * `com.example.core.domain.repository.ModelRepository`
  * `com.example.core.domain.usecase.RunTranscriptionUseCase`
* `:engine`
  * `com.example.engine.whisper.SherpaWhisperEngine`
  * `com.example.engine.diarization.SherpaDiarizationEngine`
  * `com.example.engine.vad.SherpaVadEngine`
  * `com.example.engine.lang.SherpaLanguageDetector`
* `:data`
  * `com.example.data.database.VoiceScribeDatabase`
  * `com.example.data.database.TranscriptionDao`
  * `com.example.data.database.ModelDao`
  * `com.example.data.repository.TranscriptionRepositoryImpl`
  * `com.example.data.repository.ModelRepositoryImpl`
  * `com.example.data.audio.AudioResampler`
  * `com.example.data.export.TranscriptExporterImpl`
* `:app`
  * `com.example.app.service.MediaProcessingService` (FGS)
  * `com.example.app.ui.MainActivity`
  * `com.example.app.ui.MainViewModel`

---

## 8. Test & QA Status

* **Unit Tests Status:** `GREEN` вЂ” 22 tests: `:core:model` 6 (JobState machine), `:core:domain` 9 (RunTranscriptionUseCase), `:data` 7 (TranscriptExporterImpl). Verified 2026-08-18 with Kotlin 2.4.10 / KSP 2.3.11.
* **Integration Tests Status:** `PENDING` (device-dependent)  
* **JNI/Native Tests Status:** `PARTIAL` вЂ” null-AssetManager fix verified on device (COMPLETED in logs, no native abort); full end-to-end re-verification pending  
* **QA Status:** `PENDING`  
* **Device:** Samsung SM-S928B вЂ” app installed & launches; notifications blocked for the app (FGS notification suppressed)  

---

## 9. Known Issues, Assumptions, & Limitations

1. **Overlapping Speech Limitation (В§32):** The `pyannote-segmentation-3-0` model used in `sherpa-onnx` can identify up to 3 overlapping speakers simultaneously. However, because our relational DB models and UI representation support only a single speaker label per transcription segment, our pipeline selects the dominant speaker label (highest probability weight) for overlapping sections and documents this restriction inside the app settings page.
2. **GPU Inference Support:** Although `sherpa-onnx` has JNI bindings that can compile with Vulkan support, our research indicates Vulkan-backed Whisper execution on Android devices introduces extreme stability issues and driver discrepancies. We explicitly freeze GPU acceleration as *Unsupported* and run exclusively on optimized CPU (ARM NEON) configurations.
3. **Media Decoding:** The application assumes native platform audio codecs (e.g. MediaCodec, MediaExtractor) will process common Android formats (MP3, AAC, M4A, OGG, WAV). High-level container formats like MKV/WEBM containing exotic audio streams may require a custom FFmpeg build, which is frozen as a potential Stage 2 feature.
4. **No Server Boundary:** All network operations (for downloading models) are isolated within `ModelDownloadManager`. Absolutely no telemetry, user metrics, or transcribed frames leak out of the local device, preserving strict user privacy (В§79).

---

## 10. Build Status (2026-08-18 — ASR/VAD bugfix session)

Reported device issues and fixes (device was NOT attached; fixes verified by unit tests + assembleDebug only):

- **Issue 1 (VAD ON -> empty result):** `SherpaVadEngine.detectSpeech` silently returned an empty list when `silero_vad_v5.onnx` was missing from `filesDir/models` (or VAD found no speech), and the pipeline then COMPLETED with zero segments. Fixed: `SherpaVadEngine` now throws `VadException` when the model is missing; `RunTranscriptionUseCase` catches VAD failure AND empty VAD results and falls back to fixed 30 s chunking (never a silent empty transcript).
- **Issue 2/3 (AUTO detection wrong / manual RU ignored -> English):** two bugs. (a) `SherpaWhisperEngine` cached `OfflineRecognizer` by `model.id` ONLY — the language of the first-ever run (typically "en" from detection fallback) was baked in forever, so later AUTO runs and manual RU were ignored. Fixed: cache key now includes the language (`modelId|lang`). (b) `SherpaLanguageDetector` hardcoded `"en"` when native detection returned an empty string. Fixed: blank result is returned as-is; `RunTranscriptionUseCase` treats a blank/non-ISO result as "unknown" and passes null so Whisper's internal per-chunk detection runs instead.
- **Tests:** 22 -> 25 (new: AUTO-blank->native fallback, VAD failure fallback, VAD-empty fallback). `gradlew test` GREEN, `:app:assembleDebug` SUCCESS.
- **Pending on device:** `adb install -r` + verify: (1) VAD ON now produces text (or check Models tab: silero_vad_v5.onnx must be downloaded — earlier session found pyannote dir empty), (2) AUTO detects RU, (3) manual RU produces Cyrillic.
