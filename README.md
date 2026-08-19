# VoiceScribe

**Офлайн-транскрибатор речи для Android.** Распознавание речи (Whisper ASR) выполняется полностью на устройстве через `sherpa-onnx` — без интернета, без облака, без телеметрии. Аудиофайлы декодируются локально (Media3/MediaCodec), модель Whisper запускается на CPU (ARM NEON).

## Возможности

- **Полностью офлайн**: все модели (Whisper, VAD, диаризация) скачиваются через встроенный `ModelDownloadManager` и хранятся в `filesDir/models` с проверкой SHA-256.
- **Транскрипция аудиофайлов** (MP3, AAC, M4A, OGG, WAV и другие форматы, поддерживаемые платформенными кодекaми) в фоновом сервисе с прогрессом в уведомлении.
- **VAD-фильтрация** (Silero VAD v5): тишина вырезается, речь режется на сегменты ~20 с; без VAD — фиксированные чанки по 30 с.
- **Диаризация спикеров** (pyannote-segmentation-3-0 + 3D-Speaker ERes2Net): определение говорящих, переименование спикеров.
- **Выбор языка** (ru, en, de, fr, es, it, pt, uk, pl, zh, ja, ko, tr) или автоопределение.
- **Просмотр транскрипта**: сегменты со спикерами, поиск по тексту (FTS), экспорт в TXT/SRT/VTT/JSON через SAF.
- **Управление моделями**: скачивание, активация, удаление (активная модель защищена от удаления).
- **Приватность**: никакой телеметрии, данные не покидают устройство.

## Архитектура

Gradle multi-module, зависимости направлены внутрь:

```
:core:model → :core:domain → :engine + :data → :app
```

| Модуль | Содержимое |
|---|---|
| `:core:model` | Доменные модели: `JobState` (машина состояний), `TranscriptionJob`, `TranscriptionSegment`, `Word`, `Speaker`, `ModelDescriptor` |
| `:core:domain` | Интерфейсы движков и репозиториев, use cases (`RunTranscriptionUseCase`, `GetModels`, `ManageModel`), ошибки |
| `:engine` | Адаптеры sherpa-onnx: `SherpaWhisperEngine`, `SherpaVadEngine`, `SherpaDiarizationEngine`, `SherpaLanguageDetector` |
| `:data` | Room DB (jobs/segments/words/speakers/statistics/FTS4/models), `AudioDecoder` + `AudioResampler` (→ 16 кГц mono float), `ModelRepositoryImpl` (SHA-256 атомарная установка, распаковка tar.bz2), `TranscriptExporterImpl` |
| `:app` | Compose UI (RU), MVVM + Hilt, `MediaProcessingService` (foreground service типа `mediaProcessing`), Navigation Compose |

Ключевые решения и обоснования: [ARCHITECTURE.md](ARCHITECTURE.md) (заморожен, Phase 2), [RESEARCH.md](RESEARCH.md) (исследования, Phase 1), [PROJECT_MANIFEST.md](PROJECT_MANIFEST.md) (состояние проекта и версии).

## Сборка

Требования: JDK 17, Android SDK (compileSdk 37, targetSdk 36, minSdk 24), Gradle 9.7.0 (wrapper в репозитории).

```bash
./gradlew projects          # все 5 модулей резолвятся
./gradlew test              # unit-тесты всех модулей
./gradlew :app:assembleDebug  # APK → app/build/outputs/apk/debug/app-debug.apk
```

> **Windows / не-ASCII путь:** если проект лежит в пути с кириллицей (например, `C:\Users\Администратор\...`), AGP требует `android.overridePathCheck=true` в `gradle.properties` (уже выставлено).

Установка на устройство:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.voicescribe/.ui.MainActivity
```

## Документация

- [docs/](docs/README.md) — индекс документации
  - [Руководство пользователя](docs/user-guide.md) — установка, первая настройка, транскрипция, экспорт
  - [Руководство разработчика](docs/developer-guide.md) — сборка, тесты, пайплайн, машина состояний
  - [Устранение неполадок](docs/troubleshooting.md) — типичные проблемы на устройстве

## Статус

- Милстоуны 0–7 выполнены: тулчейн, scaffold, `:core:model`/`:core:domain`/`:engine`/`:data`/`:app`, полная сборка + установка на устройство (Samsung SM-S928B).
- Phase C (UI): два таба, просмотр транскрипта, поиск, экспорт, спикеры, язык, управление моделями — реализованы.
- 25 unit-тестов GREEN; end-to-end прогон на устройстве (скачивание моделей → транскрипция → отмена) — в процессе проверки.

См. [PROJECT_MANIFEST.md](PROJECT_MANIFEST.md) для полного состояния и известных ограничений.