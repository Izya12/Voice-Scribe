# AGENTS.md

## What this repo is

**VoiceScribe**, an offline Android speech-to-text app (Whisper ASR via `sherpa-onnx`). Originally a docs-only spec repo; **Phase 3/4 code scaffolding is now in progress** — Gradle multi-module build is set up and `:core:model` has real Kotlin code + tests. More modules (`:core:domain`, `:engine`, `:data`, `:app`) are being scaffolded incrementally.

Three canonical docs at the repo root:
- `RESEARCH.md` — Phase 1 research/decisions. Largely in **Russian** (EN/RU mixed); preserve that convention when editing.
- `ARCHITECTURE.md` — Phase 2 system architecture spec, **FROZEN/ratified**. Treat as authoritative; don't make incidental architecture edits.
- `PROJECT_MANIFEST.md` — canonical state tracker / meta-registry (project state, version pinning, model catalog with SHA-256 checksums, pending codebase blueprint). **NOTE:** it lags the code (still lists Hilt 2.51.1, says modules aren't initialized); the version catalog `gradle/libs.versions.toml` is the source of truth for what actually builds.

## Working here

- `PROJECT_MANIFEST.md` is the source of truth for project state and version pins. When state or versions change, update it (including "Last Updated") — but do NOT blindly trust its pins; verify against `gradle/libs.versions.toml` first.
- `§N` references (e.g. `§11`, `§51`, `§94`) point to an external engineering contract (`promt.md`) that is **not in this repo** — they cannot be resolved locally.
- Gotchas: the manifest says docs live under `docs/`, but they are actually at the repo root. `AUDIT.md`, referenced in ARCHITECTURE §17, is also absent.

## Module layout & dependency rule (ARCHITECTURE §1 / MANIFEST §7)

- Modules: `:core:model` → `:core:domain` → `:engine` + `:data` → `:app`; dependencies point inward only.
- Packages `com.example.*`; `:app` = Compose UI + MVVM + Hilt + Foreground Service; `:app` is the only module wiring concrete implementations.

Hard constraints that must not be violated (all verified in the docs):
- CPU-only inference (ARM NEON); GPU/NNAPI/Vulkan frozen as unsupported.
- 100% offline privacy: no telemetry, cloud, or Firebase; models download via `ModelDownloadManager` only.
- Timestamps/durations stored as microsecond `Long`.
- Models live in `filesDir/models`, installed atomically with SHA-256 verification against the catalog in PROJECT_MANIFEST.
- Transcription runs in a Foreground Service of type `mediaProcessing`; WorkManager is for passive tasks only.
- Job state machine (SUBMITTED→DECODING→PREPROCESSING→DIARIZING→TRANSCRIBING→COMPLETED) forbids skipping steps and exiting terminal states.

## Toolchain & build (Windows, verified working)

- Project path is **non-ASCII** (`C:\Users\Администратор\...`) — AGP refuses to build there unless `android.overridePathCheck=true` is set in `gradle.properties` (already done; see b.android.com/95744).
- **JDK 17:** `C:\Users\836D~1\AppData\Local\Temp\opencode\tools\jdk-17.0.20+8` (no system-wide Java).
- **Gradle:** `C:\Users\836D~1\AppData\Local\Temp\opencode\tools\gradle-9.7.0\bin\gradle.bat` (`gradlew` wrapper also generated in repo).
- **Android SDK:** `C:\AndroidSdk` (ASCII path — moved off the non-ASCII user profile on purpose). `local.properties` already points `sdk.dir` there.
- PowerShell execution policy blocks dot-sourcing `env.ps1` → set env inline per command:
  `JAVA_HOME=...jdk-17.0.20+8`, `ANDROID_HOME=C:\AndroidSdk`, `ANDROID_SDK_ROOT=C:\AndroidSdk`, `GRADLE_USER_HOME=C:\Users\836D~1\AppData\Local\Temp\opencode\gradle-home`.
- `curl.exe` fails with schannel SSL errors → use `Invoke-WebRequest -UseBasicParsing` for downloads.

### Plugin rules (AGP 9, hard-won)

- **AGP `9.3.1` ships built-in Kotlin** — do NOT apply `org.jetbrains.kotlin.android` or `org.jetbrains.kotlin.jvm` to any module (errors: "no longer required", "plugin already on the classpath"). Use `com.android.application`/`com.android.library` only. Compose still needs `org.jetbrains.kotlin.plugin.compose`.
- **Hilt `2.51.1` is incompatible with AGP 9** ("Android BaseExtension not found") — use `2.59.2+`. Manifest pin `2.51.1` is stale. Current pin: `2.60.1` (Hilt <2.60 bundles `kotlin-metadata-jvm` that chokes on Kotlin 2.4.10 metadata: "maximum supported version is 2.3.0").
- **KSP `2.3.11` is tied to Kotlin 2.3.x.** Catalog pins Kotlin `2.4.10`; compatibility is under verification — if `:core:model:testDebugUnitTest` fails on KSP/Kotlin, downgrade Kotlin to `2.3.21` and update the catalog.
- **sherpa-onnx is NOT on Maven Central** (404). Vendored: AGP 9 **forbids direct local `.aar` deps** when the consuming module is itself an AAR (`:engine`), so the release AAR was unpacked into `engine/libs/sherpa-onnx.jar` (classes) + `engine/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}` (native `.so`), wired via `implementation(files("libs/sherpa-onnx.jar"))`. JitPack alternative: `com.github.k2-fsa:sherpa-onnx:1.13.5`.
- **compileSdk is 37** (androidx core-ktx 1.19.0 / lifecycle 2.11.0 require API 37; installed as `platforms;android-37.0`). targetSdk stays 36. `android-36` also installed.
- Verified stable versions in `gradle/libs.versions.toml`: AGP 9.3.1, Gradle 9.7.0, Kotlin 2.4.10, KSP 2.3.11, Hilt 2.60.1, Compose BOM 2026.06.01, Room 2.8.4, Media3 1.11.0, core-ktx 1.19.0, activity-compose 1.13.0, lifecycle 2.11.0, coroutines 1.11.0, JDK 17.

### Verify builds/tests

Run from repo root with inline env (see above):
- `gradlew projects` (wrapper) or the `gradle.bat` equivalent — confirms all 5 modules resolve.
- `gradlew :core:model:testDebugUnitTest` — unit tests for the job state machine.
- `adb devices` — physical-device installs require the phone connected via USB; raw `adb` may be missing from PATH (`spawn adb ENOENT`) — use the `android_*` MCP tools instead (see «MCP capabilities» below).

## MCP capabilities (opencode session)

MCP servers available in this environment and how to use them. Raw `adb` may be absent from PATH (`spawn adb ENOENT`) — the `android_*` MCP tools bundle their own adb/scrcpy and are the preferred way to touch the device.

### Android device automation (`android_*`) — all on-device work

- Discovery: `android_device_list` / `android_device_info` (replaces manual `adb devices`; physical device = Samsung SM-S928B).
- Install/launch: `android_app_install` (equiv. of `adb install -r`), `android_app_start` (prefix `+` = force-stop first), `android_app_stop`, `android_app_uninstall`.
- UI verification: `android_screenshot`, `android_ui_dump`, `android_ui_find_element`, `android_tap`/`android_swipe`/`android_scroll`/`android_input_text`/`android_key_event` — hands-free UI walkthroughs.
- Logs & files: `android_shell_exec` (`logcat -d`, `dumpsys`), `android_file_list`/`push`/`pull` (push test audio in, pull exported transcripts out).
- Speed: `android_start_session` (scrcpy; taps/screenshots 10–50× faster) before long UI walks; also `android_expand_notifications` (FGS notification check), `android_screen_on/off`, `android_screen_record_start/stop`, `android_clipboard_get/set`.
- Canonical device cycle: build → `android_app_install` → `android_app_start` → screenshots/UI-dump → `android_shell_exec("logcat -d ...")` → force-stop. Use it to close the pending Phase A/B/C on-device verification items.

### context7 — current library docs

Room, Hilt, Compose, Media3, navigation-compose, sherpa-onnx Java API: `context7_resolve-library-id` first, then `context7_query-docs` (≤3 calls per question). Use when unsure about an API or version behavior.

### firecrawl — web/GitHub research

- `firecrawl_developer_search` — GitHub issues/PRs/READMEs (the k2-fsa/sherpa-onnx#2562 AssetManager diagnosis is the reference pattern).
- `firecrawl_scrape`/`firecrawl_search`/`firecrawl_map` — verify model URLs/SHA-256 from k2-fsa releases (asr-models checksum.txt, speaker-* tags); `firecrawl_parse` for PDFs.

### Rarely used (brief)

- `mobile_*` — remote cloud device fleet; only on explicit user request (requires `mobile_login_to_cloud_provider`); local physical device is preferred.
- `playwright_*` + `websearch`/`webfetch` — browser automation / web search; rarely needed in this Android repo.

## Relevant skills (Codex/Claude skill store)

Real store: `C:\AI\claude-skills`. **Use `~/.agents/skills` symlinks — all 360 resolve.** Do NOT use `~/.claude/skills`: all 344 are broken/dangling symlinks. Routing table: `C:\AI\claude-skills\engineering\.codex\instructions.md` (load only 1–2 skills per request, not bulk).

Skills that fit this project:
- `spec-driven-workflow` — spec-first development; matches the frozen ARCHITECTURE → codegen flow.
- `codebase-onboarding` — generate onboarding/architecture docs for the module tree.
- `database-designer` / `database-schema-designer` — Room schema for jobs/segments/words/speakers (FTS5).
- `migration-architect` — Room schema migrations when models change.
- `dependency-auditor` — version-pin/CVE/license audits (MANIFEST pins, AAR vendoring).
- `tdd-guide` — unit tests (JUnit) for domain/model code.
- `code-reviewer` — PR review incl. Kotlin.
- `api-design-reviewer` — review module interface boundaries (`SpeechEngine`, `TranscriptionRepository`, …).
- `self-eval`, `ship-gate`, `tech-debt-tracker` — honest post-task eval, pre-release gate, debt tracking.
- Others in store: `monorepo-navigator` (JS/TS monorepos — partial fit), `performance-profiler`, `observability-designer`, `ci-cd-pipeline-builder`, `focused-fix`, `changelog-generator`, `senior-*`/`security-*` tiers.

## Build status (as of last session)

- Milestone 0 (toolchain) ✅ · Milestone 1 (Gradle scaffold) ✅ · Milestone 2 (`:core:model` code + tests) ✅ — `gradlew :core:model:testDebugUnitTest` GREEN (6/6 JobState tests pass on Kotlin 2.4.10 / KSP 2.3.11).
- Milestone 3 (`:core:domain`) ✅ — interfaces + `DefaultRunTranscriptionUseCase`; `:core:domain:testDebugUnitTest` GREEN (9/9).
- Milestone 4 (`:engine`) ✅ — sherpa adapters (`SherpaWhisperEngine`, `SherpaVadEngine`, `SherpaDiarizationEngine`, `SherpaLanguageDetector`) over vendored jar + jniLibs; compiles.
- Milestone 5 (`:data`) ✅ — Room DB (jobs/segments/words/speakers/statistics/FTS4/models), DAOs, `TranscriptionRepositoryImpl`, `ModelRepositoryImpl` (SHA-256 atomic install), `AudioDecoder` (MediaExtractor→16k mono float), `AudioResampler`, `TranscriptExporterImpl`; `:data:testDebugUnitTest` GREEN (7/7). NOTE: ARCHITECTURE §13 says FTS5 but Room has no `@Fts5` — used `@Fts4` (compatible `MATCH`); revisit if FTS5 required.
- Milestone 6 (`:app`) ✅ — Hilt wiring, Compose UI (RU), MVVM, `MediaProcessingService` FGS (type `mediaProcessing`) with progress notification + cancel.
- Milestone 7 (full build) ✅ compile — `gradlew assembleDebug` BUILD SUCCESSFUL → `app/build/outputs/apk/debug/app-debug.apk` (≈141 MB, 4 ABIs). Device install ✅ (`adb install -r` SUCCESS, Samsung SM-S928B), app launches without crashes.
- Model download pipeline ✅ fixed 2026-08-17: `ModelCatalog` now has real URLs/SHA-256 from the `asr-models` checksum.txt and `speaker-*` release tags (whisper-tiny/base/medium, silero_vad_v5, pyannote-segmentation-3-0, 3d-speaker-campplus). Whisper + pyannote ship as `.tar.bz2` and are extracted into `filesDir/models/<modelId>/` via `org.apache.commons:commons-compress:1.28.0` (added to `:data`); `ModelRepositoryImpl` strips the leading package folder (e.g. `sherpa-onnx-whisper-tiny/`). `MainViewModel.download` wrapped in try/catch → errors surface as snackbar, no process crash. NOTE: whisper-medium is ~1.9 GB and `silero_vad_v5.onnx` (NOT `silero_vad.onnx`) is the VAD file name.
- On-device model download + JNI inference not yet exercised (device attached; needs a real download run — screen is locked behind a cover).

## Build status (2026-08-17 evening session)

- **Diagnosis of stuck job:** `:core:domain` `RunTranscriptionUseCase` set `DECODING` **after** `streamPcm().toList()` → job stayed `SUBMITTED` the whole MediaCodec pass. Fixed: transition `DECODING` + emit progress **before** decoding.
- **Cancel fix (`0.3/0.4`):** `MainViewModel.cancelJob` now passes `EXTRA_JOB_ID`, hard-persists `CANCELLED` via `jobs.saveJob` (even if FGS isn't running), and uses `startForegroundService`. `MediaProcessingService` handles `ACTION_CANCEL` with a mandatory `startForeground` call (avoids `ForegroundServiceDidNotStartInTimeException` when cancelling a not-yet-started job).
- **Whisper file-name fix (`0.1`):** real archives name files `tiny-encoder.onnx`/`tiny-encoder.int8.onnx`/`tiny-tokens.txt` (prefix = model id), not `encoder.onnx`/`decoder-onnx`/`tokens.txt`. `ModelRepositoryImpl.whisper()` now matches by prefix (`whisperFilePrefix`: tiny/base/medium/large-v1..v3), prefers int8 variants.
- **Installed-check fix (`0.5`):** `modelFilesOnDisk()` treats an empty extracted dir as "not installed"; `extractTarBz2()` throws if archive yields 0 files. NOTE: on-device pyannote-segmentation dir was **empty** (DB row existed but no `model.int8.onnx`) → if diarization fails, re-download the model.
- **Task 3 (VAD-off chunking):** without VAD the whole file was one `SpeechSegment` (too long for Whisper's 30 s context). `RunTranscriptionUseCase` now splits via `fixedDurationSegments(totalSamples, WHISPER_CHUNK_SAMPLES=480_000)` (30 s @16 kHz) when `useVad=false`; VAD path unchanged (20 s segments).
- **Task 1 (tabs):** UI split into two tabs with Material3 `NavigationBar` — «Транскрипция» (`TranscriptionScreen` + `MainViewModel`/`TranscriptionUiState`) and «Модели» (`ModelsScreen` + new `ModelsViewModel`/`ModelsUiState`, downloads moved out of MainViewModel). Added `androidx.compose.material:material-icons-core` (BOM-managed) for `Icons.Filled.PlayArrow/List`.
- **Task 2 (VAD/diarization switches):** `TranscriptionScreen` has two `Switch` rows (VAD-фильтрация default ON, Диаризация default OFF); `createJobAndTranscribe(uri, useVad, diarize)` maps them into `TranscriptionConfig`.
- **Verify:** `gradlew test` GREEN (all modules); `:app:assembleDebug` BUILD SUCCESSFUL → `app-debug.apk` ≈147 MB (17:54). **Not yet installed/verified on device** (Samsung SM-S928B was not attached at build time) — needs `adb install -r` + a real download→transcribe→cancel run.

## Build status (2026-08-17 night session — device crash diagnosis)

- **Crash root cause (all 3 runs):** `F sherpa-onnx: Read binary file: Load '<abs path>' failed` → `exited cleanly (255)`. NOT missing files (all models verified on disk) — the 4 `:engine` adapters passed a **non-null `AssetManager`** to `Vad`/`OfflineRecognizer`/`OfflineSpeakerDiarization`/`SpokenLanguageIdentification` while loading from **absolute filesystem paths**, so sherpa tried `AAssetManager` and called `abort()` (k2-fsa/sherpa-onnx#2562). **FIXED: pass `null` as the AssetManager in all 4 engines** (models live in `filesDir/models`, never in APK assets). Also removed the now-unused `context.assets` fields and added `checkModelFile()` guards (missing/empty file → `VadException`/`RecognitionException`/`DiarizationException` instead of native exit).
- **ASR model selection bug (fixed):** `resolveModel` used `installed.minByOrNull { it.tier.ordinal }` → silero-vad (ENTRY, ordinal 0) was chosen as the ASR model (`DECODING ... model=silero-vad`). Now filters to `whisper-*` (`ASR_MODEL_PREFIX`) before choosing; verified on device: `model=whisper-tiny`.
- **Stale-job reconciliation (added):** process death left jobs stuck in non-terminal states (e.g. PREPROCESSING). `VoiceScribeApp.onCreate` now calls `TranscriptionRepository.reconcileStaleJobs()` → `VoiceScribeDao.failAllNonTerminal(FAILED, now)` marks SUBMITTED/DECODING/PREPROCESSING/DIARIZING/TRANSCRIBING as FAILED on boot. Verified: job `1f8f6186` (was PREPROCESSING) → FAILED after restart.
- **AudioDecoder EOS loop (fixed earlier, verified):** output EOS buffer (`BUFFER_FLAG_END_OF_STREAM`, size=0) was filtered by `size > 0` → `dequeueOutputBuffer` loop never terminated. New loop: `while (!outputEos)`, `when(outputIndex)` on `INFO_TRY_AGAIN_LATER`/`INFO_OUTPUT_FORMAT_CHANGED`, `DRAIN_IDLE_TICKS=100` timeout; `getOutputBuffer ?: continue` replaced with `?.let`. Verified: 48.4 s AAC decoded in ~2.5 s (`done fed=322423 emitted=1547714`, `decoded chunks=4`).
- **NOTE (device):** user has notifications blocked for the app (`NotificationService: isPkgBlocked = true`) — FGS notification is suppressed; if mediaProcessing FGS misbehaves, ask the user to unblock notifications (Settings → Apps → VoiceScribe → Notifications). Crash is NOT caused by this.
- **Status:** `gradlew test` GREEN, `:app:assembleDebug` SUCCESS. **Needs `adb install -r` + one real run (useVad + diarization) on device** to confirm the null-AssetManager fix end-to-end.
## Build status (2026-08-18 — Phase C: UI screens)

Executed per user choice (navigation-compose, in-job search, job deletion deferred). gradlew test GREEN (22/22), :app:assembleDebug SUCCESS → app-debug.apk ≈136 MB. **Device NOT attached at build time — `adb install -r` + manual UI verification pending; now drivable via the `android_*` MCP tools (see «MCP capabilities»).**

- **C0:** ndroidx.navigation:navigation-compose 2.9.8 added (stable for compileSdk 37; 2.10 is rc-only).
- **C4 (domain/data):** TranscriptionRepository += observeTranscript(jobId): Flow<Transcript>, enameSpeaker(jobId, speakerId, name), searchInJob(jobId, query, limit); Transcript now carries speakers. **FIX: saveTranscript now persists SpeakerEntity rows** (id = stable cluster id, displayName "Говорящий N", colorIndex = id; previously speakers were never written). DAO += observeSegments/observeWords/observeSpeakers, enameSpeaker, searchInJob (FTS4 join: JOIN segment_fts f ON f.rowid = s.id WHERE segment_fts MATCH :query AND s.job_id = :jobId), observeActiveModelId(). AppModule: TranscriptExporter now @Binds by interface.
- **C1 (progress):** new TranscriptionProgressStore (app singleton StateFlow<Map<jobId, JobProgress>>); MediaProcessingService publishes per-collectLatest, cleans on cancel/terminal; notification text += "%" (
otification_progress string). MainViewModel.uiState combines jobs + progress; JobCard shows LinearProgressIndicator + stage % for active jobs.
- **C2:** NavHost: 	ranscribe + 	ranscript/{jobId}; JobCard (COMPLETED) clickable → detail; back = popBackStack.
- **C3:** new TranscriptScreen + TranscriptDetailViewModel (SavedStateHandle jobId): job meta (date, VAD/diarization/language chips), segments with clickable colored speaker chips, rename dialog (stable ids, §26), debounced FTS search with results view, export menu TXT/SRT/VTT/JSON via SAF CreateDocument("*/*").
- **C5:** LanguageMenu dropdown (ru/en/de/fr/es/it/pt/uk/pl/zh/ja/ko/tr + Авто) → LanguageMode.MANUAL+language in config; createJobAndTranscribe(uri, useVad, diarize, language).
- **C6 (models tab):** ModelsUiState += ctiveModelId; card shows «Активна» badge, size/license, «Активировать»/«Удалить» (confirm dialog; active model delete blocked by repo §37).
- **PROJECT_MANIFEST.md** updated (state IMPLEMENTED, real versions, Phase C + remaining-work list). Remaining: Phase A (device persistence diagnosis), Phase B (successful end-to-end run), D (model mgmt depth), E (stats/logging/benchmarks/QA).
