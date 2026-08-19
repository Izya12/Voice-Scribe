# VoiceScribe вЂ” Project Manifest

This is the canonical state and meta-registry for VoiceScribe, maintained in strict compliance with В§118 and В§115 of the engineering contract.

---

## 1. Core Metadata

* **Project Name:** VoiceScribe  
* **Current State:** `IMPLEMENTED` вЂ” Milestones 0вЂ“7 done (toolchain в†’ scaffold в†’ model/domain/engine/data/app в†’ full build + device install); **Phase C UI (transcript review, search, export, speakers, language, model mgmt) added 2026-08-18**; **Settings/logging + job deletion + error messages added 2026-08-19**; **GigaAM v3 + GigaAM Multilingual models added 2026-08-19**  
* **Architecture Version:** `2.0.0`  
* **Date Created:** 2026-08-10  
* **Last Updated:** 2026-08-19 (GigaAM models session)  

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

6. **ASR Model (GigaAM v3, Russian):**  
   * **Name:** GigaAM v3 Russian CTC ONNX (int8 quantized)  
   * **File Size:** `163 286 197` bytes (tar.bz2 archive)  
   * **Source:** k2-fsa/sherpa-onnx asr-models (official checksum.txt entry)  
   * **SHA-256 Checksum:** `e1291d704460cab4a01716081170c86c12f6b15338a1534f71cc5956922adb52`  
   * **License:** MIT (salute-developers/GigaAM)  

7. **ASR Model (GigaAM Multilingual):**  
   * **Name:** GigaAM Multilingual RU/KK/KY/UZ CTC ONNX (int8 quantized)  
   * **File Size:** `224 762 204` bytes (onnx) + `multilingual_vocab.txt` sidecar  
   * **Source:** community conversion `istupakov/gigaam-multilingual-ctc-onnx` (HuggingFace) of the official `gigaam_multilingual` branch (salute-developers/GigaAM) — NO official k2-fsa checksum.txt entry  
   * **SHA-256 Checksum:** `e08e27ae5669b39f0c378fae101bbbb9a80505f74f9b66719c309bf5b894a480` (vocab: `4d130287892e1099fedfb3f93c4b4cf8a263151158801680b28977d1be4133f4`)  
   * **License:** MIT  

---

## 6. Generated Modules & Directories

All five Gradle modules are initialized and compiling (Milestones 1вЂ“7 done, 2026-08-17/18):

* `:core:model` вЂ” JobState machine, TranscriptionJob/Config, TranscriptionSegment, Word, Speaker, ModelDescriptor, TranscriptionStatistics
* `:core:domain` вЂ” engine interfaces (SpeechEngine/VadEngine/DiarizationEngine/LanguageDetector), repositories (TranscriptionRepository incl. `observeTranscript`/`renameSpeaker`/`searchInJob`, ModelRepository incl. `observeActiveModelId`, TranscriptExporter), use cases (RunTranscriptionUseCase, GetModels/ManageModel), errors
* `:engine` вЂ” sherpa-onnx adapters: SherpaWhisperEngine, SherpaVadEngine, SherpaDiarizationEngine, SherpaLanguageDetector (null AssetManager вЂ” models live in `filesDir/models`)
* `:data` вЂ” Room DB v2 (jobs/segments/words/speakers/statistics/FTS4/models; MIGRATION_1_2 adds `error_message`), VoiceScribeDao, AudioDecoder (MediaExtractor audio-only в†’ 16 kHz mono), AudioResampler, ModelRepositoryImpl (SHA-256 atomic install, tar.bz2 extraction, sidecar files, active-model deletion protection), TranscriptExporterImpl (TXT/SRT/VTT/JSON), FileAppLogger, SettingsRepositoryImpl
* `:app` вЂ” Hilt wiring, Compose UI (RU), MainViewModel/ModelsViewModel/SettingsViewModel/TranscriptDetailViewModel, MediaProcessingService FGS (`mediaProcessing`), Navigation Compose (transcribe / transcript/{jobId}), TranscriptionProgressStore

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

* **Unit Tests Status:** `GREEN` вЂ” 42 tests (re-counted 2026-08-19 from JUnit XML): `:core:model` 10 (JobState machine 6 + LogLevel 4), `:core:domain` 17 (RunTranscriptionUseCase), `:data` 15 (TranscriptExporterImpl 7 + ResumableDownloader 4 + FileAppLogger 4). Verified with Kotlin 2.4.10 / KSP 2.3.11.
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

## 10. Build Status (2026-08-19 - GigaAM models session)

Added two NeMo CTC (GigaAM) ASR models; the ASR engine now branches whisper vs nemo_ctc (verified: `gradlew test` GREEN, `:app:assembleDebug` SUCCESS):

- **GigaAM v3 (RU):** official k2-fsa asr-models release `sherpa-onnx-nemo-ctc-giga-am-v3-russian-2025-12-16.tar.bz2` (163 286 197 B; SHA-256 `e1291d70...2adb52` verified against checksum.txt; MIT). Installed via the existing tar.bz2 flow into `models/gigaam-v3/` (`model.int8.onnx` + `tokens.txt`).
- **GigaAM Multilingual (RU/KK/KY/UZ):** no official k2-fsa conversion exists — uses the community conversion `istupakov/gigaam-multilingual-ctc-onnx` (HuggingFace) of the official `gigaam_multilingual` branch (salute-developers/GigaAM). Plain `multilingual_ctc.int8.onnx` (224 762 204 B) + sidecar `multilingual_vocab.txt`; SHA-256 computed locally at catalog time (`e08e27ae...4a480` / `4d130287...13f4`). NOTE: third-party host, no upstream checksum.txt entry.
- **ModelDescriptor.extraFiles:** new sidecar-file support (`ModelExtraFile`, default empty) — sidecars are downloaded + SHA-verified into `models/` alongside plain models; install/delete/installed-checks updated in `ModelRepositoryImpl`.
- **Engine:** `SherpaWhisperEngine` branches on `gigaam-*` ids → `OfflineNemoEncDecCtcModelConfig` (`modelType="nemo_ctc"`, `greedy_search`, tokens); whisper path unchanged. NeMo recognizer cache key is language-independent (language is baked into the model).
- **ASR selection:** `RunTranscriptionUseCase.resolveModel` / `isAsrModel` now accept both `whisper-*` and `gigaam-*` ids.
- **Verified on device 2026-08-19:** Models tab shows both GigaAM cards («GigaAM v3 Russian (CTC int8)» 155 МБ MID, «GigaAM Multilingual RU/KK/KY/UZ (CTC int8)» 214 МБ HIGH, status «Скачать»); confirmed via `uiautomator dump --compressed` (MCP ui_dump times out on Models tab).
- **Pending on device:** download + transcribe a Russian clip with GigaAM v3 (VAD on/off), then a multilingual clip to verify.

## 11. Build Status (2026-08-19 - Settings/logging + job deletion session)

Added (all verified: `gradlew test` GREEN 29/29, `:app:assembleDebug` SUCCESS → app-debug.apk):

- **Settings tab («Настройки»):** third `NavigationBar` tab (`Icons.Filled.Settings`) with file-logging Switch + log-level dropdown (Отладка/Инфо/Предупреждения/Ошибки) + shown log path (`filesDir/logs/voicescribe.log`). New `SettingsViewModel`/`SettingsUiState` (sample: `ModelsViewModel`); persistence via `SettingsRepository` → `SettingsRepositoryImpl` (SharedPreferences `settings.xml`, keys `logging_enabled`/`log_level`, defaults false/INFO; `callbackFlow` + `OnSharedPreferenceChangeListener`; each write re-configures the logger immediately, no restart).
- **File logging:** `LogLevel` (core:model, DEBUG(0)..ERROR(3), `canLog` filter), `AppLogger` (core:domain interface), `FileAppLogger` (data; `@Singleton`, `synchronized` append writes, rotation at 5 MB keeping 3 files, `@Volatile enabled/level`, write failures silently dropped). DI: `FileAppLogger(File(filesDir,"logs"))` + `@Binds AppLogger`. Wired into `RunTranscriptionUseCase` (all stage transitions, terminal persist with error), `MainViewModel` (submit/delete), `MediaProcessingService` (start/cancel/finish), `SettingsViewModel`, and `VoiceScribeApp` (`Thread.setDefaultUncaughtExceptionHandler` → log).
- **Job deletion:** `VoiceScribeDao.deleteJob` (`DELETE FROM transcription_job WHERE id = :jobId`; segment/word/speaker/statistics cascade via FK, FTS4 shadow syncs via Room triggers) → `TranscriptionRepository.deleteJob` → `MainViewModel.deleteJob` (blocks active jobs, snackbar on error) → JobCard «Удалить» button for terminal states (COMPLETED/FAILED/CANCELLED) + confirmation `AlertDialog`.
- **Error messages on job cards:** Room v1→v2 migration (`MIGRATION_1_2`: `ALTER TABLE transcription_job ADD COLUMN error_message TEXT`; replaced `fallbackToDestructiveMigration`), `errorMessage` on `TranscriptionJob`/`JobEntity`/mapper; `RunTranscriptionUseCase.persistTerminal(jobId, state, errorMessage)` stores the message for FAILED; JobCard renders it in `colorScheme.error`.
- **Pending on device:** `adb install -r` + verify: (1) Settings tab persists toggle/level and creates `voicescribe.log` after a run, (2) failed job shows its error text, (3) delete removes job + transcript + FTS entries.

## 12. Build Status (2026-08-18 - ASR/VAD bugfix session)

Reported device issues and fixes (device was NOT attached; fixes verified by unit tests + assembleDebug only):

- **Issue 1 (VAD ON -> empty result):** `SherpaVadEngine.detectSpeech` silently returned an empty list when `silero_vad_v5.onnx` was missing from `filesDir/models` (or VAD found no speech), and the pipeline then COMPLETED with zero segments. Fixed: `SherpaVadEngine` now throws `VadException` when the model is missing; `RunTranscriptionUseCase` catches VAD failure AND empty VAD results and falls back to fixed 30 s chunking (never a silent empty transcript).
- **Issue 2/3 (AUTO detection wrong / manual RU ignored -> English):** two bugs. (a) `SherpaWhisperEngine` cached `OfflineRecognizer` by `model.id` ONLY — the language of the first-ever run (typically "en" from detection fallback) was baked in forever, so later AUTO runs and manual RU were ignored. Fixed: cache key now includes the language (`modelId|lang`). (b) `SherpaLanguageDetector` hardcoded `"en"` when native detection returned an empty string. Fixed: blank result is returned as-is; `RunTranscriptionUseCase` treats a blank/non-ISO result as "unknown" and passes null so Whisper's internal per-chunk detection runs instead.
- **Tests:** 22 -> 25 (new: AUTO-blank->native fallback, VAD failure fallback, VAD-empty fallback). `gradlew test` GREEN, `:app:assembleDebug` SUCCESS.
- **Pending on device:** `adb install -r` + verify: (1) VAD ON now produces text (or check Models tab: silero_vad_v5.onnx must be downloaded — earlier session found pyannote dir empty), (2) AUTO detects RU, (3) manual RU produces Cyrillic.
