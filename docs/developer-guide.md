# Руководство разработчика

## Модули и правило зависимостей

```
:core:model → :core:domain → :engine + :data → :app
```

Зависимости направлены только внутрь (ARCHITECTURE §1). `:app` — единственный модуль, который связывает конкретные реализации (Hilt).

| Модуль | Ключевые классы |
|---|---|
| `:core:model` | `JobState`, `TranscriptionJob`, `TranscriptionConfig`, `TranscriptionSegment`, `Word`, `Speaker`, `ModelDescriptor`, `TranscriptionStatistics` |
| `:core:domain` | `SpeechEngine`, `VadEngine`, `DiarizationEngine`, `LanguageDetector`, `TranscriptionRepository`, `ModelRepository`, `TranscriptExporter`, `RunTranscriptionUseCase`, `GetModelsUseCase`, `ManageModelUseCase` |
| `:engine` | `SherpaWhisperEngine`, `SherpaVadEngine`, `SherpaDiarizationEngine`, `SherpaLanguageDetector`, `SherpaModelFiles` |
| `:data` | `VoiceScribeDatabase`, `VoiceScribeDao`, `AudioDecoder`, `AudioResampler`, `TranscriptionRepositoryImpl`, `ModelRepositoryImpl`, `ResumableDownloader`, `ModelCatalog`, `TranscriptExporterImpl` |
| `:app` | `MainActivity`, `MainViewModel`, `ModelsViewModel`, `TranscriptDetailViewModel`, `TranscriptionProgressStore`, `MediaProcessingService`, `AppModule` |

## Сборка и тесты

### Тулчейн (Windows, проверено)

- **JDK 17** — нет системной Java; задаётся через `JAVA_HOME` (см. AGENTS.md).
- **Gradle 9.7.0** — wrapper в репозитории (`gradlew.bat`).
- **Android SDK** — `C:\AndroidSdk` (ASCII-путь; `local.properties` указывает `sdk.dir`).
- **Не-ASCII путь проекта** — обязателен `android.overridePathCheck=true` в `gradle.properties` (уже выставлено).
- PowerShell блокирует dot-sourcing `env.ps1` → переменные окружения задаются инлайн в каждой команде.

### Правила плагинов (AGP 9)

- AGP 9.3.1 поставляет встроенный Kotlin → **не** применять `org.jetbrains.kotlin.android`/`jvm` к модулям. Compose требует `org.jetbrains.kotlin.plugin.compose`.
- Hilt ≥ 2.60.1 (2.51.1 несовместим с AGP 9).
- KSP 2.3.11 завязан на Kotlin 2.3.x; каталог пиннит Kotlin 2.4.10 — совместимость подтверждена (тесты зелёные).
- sherpa-onnx не на Maven Central: вендоринг через `engine/libs/sherpa-onnx.jar` + `engine/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}` (AGP 9 запрещает прямые локальные `.aar`-зависимости для модулей, которые сами являются AAR). Альтернатива: JitPack `com.github.k2-fsa:sherpa-onnx:1.13.5`.

### Команды

```bash
./gradlew test                  # unit-тесты всех модулей (25 шт.)
./gradlew :core:model:testDebugUnitTest   # машина состояний
./gradlew :app:assembleDebug    # APK (≈136–147 МБ, 4 ABI)
```

Источник истины по версиям — `gradle/libs.versions.toml` (не PROJECT_MANIFEST.md, он отстаёт).

## Пайплайн транскрипции

`RunTranscriptionUseCase` (в `:core:domain`) исполняет машину состояний:

```
SUBMITTED → DECODING → PREPROCESSING → DIARIZING → TRANSCRIBING → COMPLETED
```

1. **SUBMITTED** — задача создана в Room.
2. **DECODING** — переход происходит **до** декодирования (иначе задача «застревает» в SUBMITTED на время MediaCodec-прохода). `AudioDecoder` (MediaExtractor → PCM 16 кГц mono) + `AudioResampler`.
3. **PREPROCESSING** — сегментация: при `useVad=true` — Silero VAD, сегменты ~20 с; при `useVad=false` — фиксированные чанки по 30 с (`WHISPER_CHUNK_SAMPLES = 480_000`). Сбой/пустой результат VAD → фолбэк на фиксированные чанки (никогда не «тихая» пустая транскрипция).
4. **DIARIZING** — pyannote-сегментация + эмбеддер 3D-Speaker (только при `diarize=true`).
5. **TRANSCRIBING** — Whisper по чанкам; кэш `OfflineRecognizer` ключуется по `modelId|lang` (иначе язык первого запуска «запекается»).
6. **COMPLETED** — сегменты/слова/спикеры персистятся в Room.

Задача исполняется в `MediaProcessingService` (foreground service, тип `mediaProcessing`), прогресс публикуется в `TranscriptionProgressStore` (app-singleton `StateFlow<Map<jobId, JobProgress>>`) и в уведомление.

**Отмена**: `cancelJob` передаёт `EXTRA_JOB_ID`, сервис обрабатывает `ACTION_CANCEL` (с обязательным `startForeground` — иначе `ForegroundServiceDidNotStartInTimeException` при отмене ещё не запущенной задачи). Состояние `CANCELLED` персистится напрямую.

## Модели

- Каталог: `ModelCatalog` (`:data`) — реальные URL и SHA-256 из `asr-models` checksum.txt и релизов `speaker-*` (k2-fsa).
- Установка: скачивание (ResumableDownloader) → проверка SHA-256 → атомарное перемещение → распаковка tar.bz2 (commons-compress) в `filesDir/models/<modelId>/` со срезанием ведущей папки архива.
- Имена файлов Whisper в архивах: `tiny-encoder.onnx` / `tiny-encoder.int8.onnx` / `tiny-tokens.txt` (префикс = id модели, предпочтение int8).
- **Критично**: все 4 адаптера `:engine` передают sherpa-onnx **null** как AssetManager — модели живут в `filesDir/models`, не в APK assets. Передача ненулевого AssetManager при загрузке по абсолютным путям вызывает `abort()` нативного кода (k2-fsa/sherpa-onnx#2562).
- Признак «установлено»: пустая распакованная директория = не установлено; архив без файлов → исключение.

## База данных

Room v1, таблицы: `jobs`, `segments`, `words`, `speakers`, `statistics`, `models`, `segment_fts` (FTS4 — ARCHITECTURE §13 говорит FTS5, но у Room нет `@Fts5`). Поиск: `JOIN segment_fts f ON f.rowid = s.id WHERE segment_fts MATCH :query AND s.job_id = :jobId`.

При старте приложения (`VoiceScribeApp.onCreate`) — `reconcileStaleJobs()`: задачи в нетерминальных состояниях (после убийства процесса) помечаются `FAILED`.

## Тестирование

- 25 unit-тестов GREEN: `:core:model` 6 (JobState), `:core:domain` 9 (RunTranscriptionUseCase), `:data` 7 (TranscriptExporterImpl) + доп. кейсы фолбэков (AUTO-язык, сбой/пустой VAD).
- Интеграционные/JNI-тесты: pending (нужно устройство).
- Полный цикл проверки на устройстве: `adb install -r` → запуск → `logcat -d` → force-stop. Для UI-прогонов доступны MCP-инструменты `android_*` (scrcpy-сессия ускоряет тапы/скриншоты).

## Конвенции и gotchas

- Канонические документы лежат в **корне** репозитория (не в `docs/`): `ARCHITECTURE.md` (FROZEN), `RESEARCH.md`, `PROJECT_MANIFEST.md`. `AUDIT.md` (ARCHITECTURE §17) отсутствует.
- Ссылки `§N` указывают на внешний контракт (`promt.md`), которого нет в репозитории.
- Таймстампы/длительности — микросекундные `Long`.
- Пакеты — `com.example.*`.
- При изменении состояния/версий обновляйте `PROJECT_MANIFEST.md` (включая «Last Updated»), сверяясь с `gradle/libs.versions.toml`.
- `curl.exe` на этой машине падает с schannel SSL → для скачиваний использовать `Invoke-WebRequest -UseBasicParsing`.