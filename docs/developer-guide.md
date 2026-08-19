# Руководство разработчика

## Модули и правило зависимостей

```
:core:model → :core:domain → :engine + :data → :app
```

Зависимости направлены только внутрь (ARCHITECTURE §1). `:app` — единственный модуль, который связывает конкретные реализации (Hilt).

| Модуль | Ключевые классы |
|---|---|
| `:core:model` | `JobState`, `TranscriptionJob`, `TranscriptionConfig`, `TranscriptionSegment`, `Word`, `Speaker`, `ModelDescriptor` (+ `ModelExtraFile`), `TranscriptionStatistics`, `LogLevel` |
| `:core:domain` | `SpeechEngine`, `VadEngine`, `DiarizationEngine`, `LanguageDetector`, `TranscriptionRepository`, `ModelRepository`, `TranscriptExporter`, `SettingsRepository`, `AppLogger`, `RunTranscriptionUseCase`, `GetModelsUseCase`, `ManageModelUseCase` |
| `:engine` | `SherpaWhisperEngine` (Whisper + GigaAM NeMo CTC), `SherpaVadEngine`, `SherpaDiarizationEngine`, `SherpaLanguageDetector`, `SherpaModelFiles` |
| `:data` | `VoiceScribeDatabase`, `VoiceScribeDao`, `AudioDecoder`, `AudioResampler`, `TranscriptionRepositoryImpl`, `ModelRepositoryImpl`, `ResumableDownloader`, `ModelCatalog`, `TranscriptExporterImpl`, `FileAppLogger`, `SettingsRepositoryImpl` |
| `:app` | `MainActivity`, `MainViewModel`, `ModelsViewModel`, `SettingsViewModel`, `TranscriptDetailViewModel`, `TranscriptionProgressStore`, `MediaProcessingService`, `AppModule` |

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
./gradlew test                  # unit-тесты всех модулей (42 шт.)
./gradlew :core:model:testDebugUnitTest   # машина состояний + LogLevel
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
5. **TRANSCRIBING** — распознавание по чанкам: Whisper **или GigaAM (NeMo CTC)**. Кэш `OfflineRecognizer` ключуется по `modelId|lang` (иначе язык первого запуска «запекается»); для GigaAM язык зашит в модель — ключ без языка.
6. **COMPLETED** — сегменты/слова/спикеры персистятся в Room (при `FAILED` — текст ошибки в `error_message`).

Задача исполняется в `MediaProcessingService` (foreground service, тип `mediaProcessing`), прогресс публикуется в `TranscriptionProgressStore` (app-singleton `StateFlow<Map<jobId, JobProgress>>`) и в уведомление.

**Отмена**: `cancelJob` передаёт `EXTRA_JOB_ID`, сервис обрабатывает `ACTION_CANCEL` (с обязательным `startForeground` — иначе `ForegroundServiceDidNotStartInTimeException` при отмене ещё не запущенной задачи). Состояние `CANCELLED` персистится напрямую.

## Модели

- Каталог: `ModelCatalog` (`:data`) — реальные URL и SHA-256 из `asr-models` checksum.txt и релизов `speaker-*` (k2-fsa); GigaAM v3 — официальная запись checksum.txt, GigaAM Multilingual — community-конвертация `istupakov/gigaam-multilingual-ctc-onnx` (HuggingFace), SHA вычислены локально.
- Установка: скачивание (ResumableDownloader) → проверка SHA-256 → атомарное перемещение → распаковка tar.bz2 (commons-compress) в `filesDir/models/<modelId>/` со срезанием ведущей папки архива. Для plain-моделей — sidecar-файлы (`ModelDescriptor.extraFiles`, напр. `multilingual_vocab.txt`) качаются и проверяются так же.
- Имена файлов Whisper в архивах: `tiny-encoder.onnx` / `tiny-encoder.int8.onnx` / `tiny-tokens.txt` (префикс = id модели, предпочтение int8).
- **NeMo CTC (GigaAM)**: `SherpaWhisperEngine` ветвится по префиксу `gigaam-` → `OfflineNemoEncDecCtcModelConfig` (`modelType="nemo_ctc"`, `greedy_search`, tokens). `RunTranscriptionUseCase.isAsrModel` принимает `whisper-*` и `gigaam-*`.
- **Критично**: все 4 адаптера `:engine` передают sherpa-onnx **null** как AssetManager — модели живут в `filesDir/models`, не в APK assets. Передача ненулевого AssetManager при загрузке по абсолютным путям вызывает `abort()` нативного кода (k2-fsa/sherpa-onnx#2562).
- Признак «установлено»: пустая распакованная директория = не установлено; архив без файлов → исключение.

## База данных

Room **v2**, таблицы: `jobs`, `segments`, `words`, `speakers`, `statistics`, `models`, `segment_fts` (FTS4 — ARCHITECTURE §13 говорит FTS5, но у Room нет `@Fts5`). Поиск: `JOIN segment_fts f ON f.rowid = s.id WHERE segment_fts MATCH :query AND s.job_id = :jobId`.

Миграция `MIGRATION_1_2`: `ALTER TABLE transcription_job ADD COLUMN error_message TEXT` (текст ошибки в карточке задания); `fallbackToDestructiveMigration` убран.

При старте приложения (`VoiceScribeApp.onCreate`) — `reconcileStaleJobs()`: задачи в нетерминальных состояниях (после убийства процесса) помечаются `FAILED`.

## Логирование

- `LogLevel` (core:model) — DEBUG(0)…ERROR(3), фильтр `canLog`.
- `AppLogger` (core:domain) — интерфейс; `FileAppLogger` (data) — `filesDir/logs/voicescribe.log`, синхронизированная запись, ротация при 5 МБ (хранится 3 файла), настройки применяются мгновенно через `@Volatile`.
- Включение/уровень — вкладка «Настройки» → `SettingsRepository` (SharedPreferences `settings.xml`, `callbackFlow`). `VoiceScribeApp` логирует необработанные исключения (`Thread.setDefaultUncaughtExceptionHandler`).

## Тестирование

- 42 unit-теста GREEN (подсчитано по JUnit XML, 2026-08-19): `:core:model` 10 (JobState 6 + LogLevel 4), `:core:domain` 17 (RunTranscriptionUseCase — пайплайн, фолбэки VAD/языка, выбор модели), `:data` 15 (TranscriptExporterImpl 7 + ResumableDownloader 4 + FileAppLogger 4).
- Интеграционные/JNI-тесты: pending (нужно устройство).
- Полный цикл проверки на устройстве: `adb install -r` → запуск → `logcat -d` → force-stop. Для UI-прогонов доступны MCP-инструменты `android_*` (scrcpy-сессия ускоряет тапы/скриншоты; при таймаутах `ui_dump` — обход через `uiautomator dump --compressed` в shell).

## Конвенции и gotchas

- Канонические документы лежат в **корне** репозитория (не в `docs/`): `ARCHITECTURE.md` (FROZEN), `RESEARCH.md`, `PROJECT_MANIFEST.md`. `AUDIT.md` (ARCHITECTURE §17) отсутствует.
- Ссылки `§N` указывают на внешний контракт (`promt.md`), которого нет в репозитории.
- Таймстампы/длительности — микросекундные `Long`.
- Пакеты — `com.example.*`.
- При изменении состояния/версий обновляйте `PROJECT_MANIFEST.md` (включая «Last Updated»), сверяясь с `gradle/libs.versions.toml`.
- `curl.exe` на этой машине падает с schannel SSL → для скачиваний использовать `Invoke-WebRequest -UseBasicParsing`.