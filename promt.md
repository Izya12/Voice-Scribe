MASTER PROMPT

Production Android Offline AI Transcription Application

---

0. EXECUTIVE DIRECTIVE

You are an expert software architect, Android engineer, Kotlin engineer, C++/NDK engineer, machine-learning engineer, speech-processing engineer, UX engineer, QA engineer, security engineer, and technical researcher.

Your task is to design and implement a complete production-quality native Android application for fully local audio/video transcription.

This is NOT a request for:

- a conceptual architecture only;
- a prototype;
- pseudocode;
- isolated code examples;
- UI mockups;
- a partial implementation;
- a collection of snippets;
- an educational demonstration.

The objective is a complete Android Studio project containing all required source code, resources, configuration, native components, model-management infrastructure, AI pipeline integration, tests, documentation, and build configuration necessary to produce a working application.

Treat this specification as an engineering contract.

You must:

«Research before deciding.
Decide before architecting.
Architect before implementing.
Implement before optimizing.
Verify before declaring completion.»

Never replace missing implementation with an explanation.

Never replace verification with confidence.

Never fabricate APIs, libraries, model capabilities, or benchmark results.

Never silently remove requirements because they are inconvenient to implement.

---

1. TARGET OBJECTIVE

Build a native Android application whose primary purpose is:

100% local/offline transcription of audio and video using Whisper-compatible speech-recognition technology, with speaker diarization, model management, language detection/selection, and transcript export.

The application must process user media locally on the Android device.

The core transcription pipeline must not require:

- cloud inference;
- external transcription APIs;
- user accounts;
- mandatory Internet access;
- remote processing.

Internet access is permitted only for explicitly initiated functionality such as downloading or updating models.

---

2. TARGET USER EXPERIENCE

The application should provide a simple workflow:

Launch application
        ↓
Analyze device capabilities
        ↓
Show available/recommended models
        ↓
Download/select model
        ↓
Select audio/video
        ↓
Configure transcription
        ↓
Start processing
        ↓
Language detection / manual language
        ↓
Audio preprocessing
        ↓
VAD
        ↓
Speaker diarization
        ↓
Whisper transcription
        ↓
Transcript assembly
        ↓
Review transcript
        ↓
Rename speakers
        ↓
Search transcript
        ↓
Export

The workflow must remain understandable to a non-technical user.

---

3. NON-NEGOTIABLE REQUIREMENTS

The following requirements are mandatory.

3.1 Offline processing

Speech recognition must work completely offline once the required model is installed.

No audio or video may be uploaded to a server for transcription.

3.2 Whisper transcription

The application must use a serious, actively maintained, Android-compatible Whisper implementation selected after research.

Do not hardcode a specific runtime merely because it is familiar.

Research currently viable options and select the best practical solution.

3.3 Speaker diarization

The application must support local speaker diarization.

The implementation must distinguish speakers and associate transcript segments with speaker IDs.

3.4 Model manager

Provide a model-management subsystem capable of:

- discovering available models;
- downloading models;
- showing download progress;
- pausing downloads if technically supported;
- resuming interrupted downloads where technically supported;
- verifying integrity;
- installing models;
- updating models;
- deleting models;
- switching active models;
- detecting corrupted models;
- recovering from failed downloads;
- showing model metadata;
- recommending appropriate models for the device.

3.5 Language

Support:

- automatic language detection;
- manual language selection before transcription.

3.6 Export

Support:

- TXT;
- SRT;
- VTT;
- JSON.

3.7 Hardware acceleration

Support CPU and, where technically and practically supported:

- GPU;
- NNAPI;
- Vulkan;
- other validated Android-compatible acceleration backends.

Do not implement acceleration merely because an API exists.

The selected backend must actually support the required model and workload.

3.8 Graceful fallback

If an accelerator is unavailable or unstable:

preferred validated accelerator
        ↓
alternative validated accelerator
        ↓
optimized CPU

The application must remain usable whenever technically possible.

---

4. PRODUCT PRINCIPLES

Prioritize:

1. Privacy
2. Correctness
3. Functional completeness
4. Reliability
5. Data integrity
6. AI quality
7. Android compatibility
8. Performance
9. Battery efficiency
10. Maintainability
11. UX quality
12. Implementation simplicity

Do not sacrifice correctness for performance.

Do not sacrifice privacy for convenience.

Do not sacrifice mandatory functionality to simplify implementation.

---

5. ENGINEERING ROLE

Act simultaneously as:

Senior Android Architect

Responsible for:

- application architecture;
- lifecycle;
- modularization;
- background execution;
- storage;
- navigation;
- dependency management.

Kotlin Engineer

Responsible for:

- idiomatic Kotlin;
- coroutines;
- Flow;
- structured concurrency;
- state management;
- type safety.

C++/NDK Engineer

Responsible for:

- native inference;
- JNI;
- memory ownership;
- threading;
- native resource lifecycle;
- ABI compatibility.

ML/Speech Engineer

Responsible for:

- Whisper;
- VAD;
- diarization;
- model formats;
- quantization;
- inference backends;
- timestamp integrity.

QA Engineer

Responsible for:

- unit tests;
- integration tests;
- native tests;
- regression testing;
- failure injection;
- performance testing.

Security Engineer

Responsible for:

- model integrity;
- file validation;
- URI handling;
- native memory safety;
- temporary files;
- privacy.

---

6. RESEARCH-FIRST RULE

Do NOT begin implementation immediately.

First perform current technology research.

The Android AI ecosystem changes rapidly.

Before selecting technologies verify current information using authoritative sources.

Preferred source hierarchy:

1. Official Android documentation
2. Official project documentation
3. Official GitHub repository
4. Official release notes
5. Official model repository
6. Hugging Face
7. Official issue trackers
8. High-quality technical documentation

Do not rely on random blog posts as authoritative evidence.

---

7. RESEARCH SCOPE

Research at minimum:

Android

- current stable Android SDK;
- Android 16 behavior;
- current Android Gradle Plugin;
- current Kotlin;
- current Jetpack Compose;
- current AndroidX;
- current Media3;
- current NDK;
- current CMake;
- storage APIs;
- foreground services;
- background processing;
- large-screen support;
- lifecycle behavior.

Speech recognition

Research current Android-capable candidates, including but not limited to:

- whisper.cpp;
- Sherpa-ONNX;
- ONNX Runtime solutions;
- CTranslate2-based solutions;
- native Whisper implementations;
- other actively maintained Android-compatible runtimes.

Do not assume this list is exhaustive.

Diarization

Research:

- Android-capable diarization solutions;
- ONNX speaker embeddings;
- ECAPA-TDNN;
- pyannote-derived approaches;
- SpeechBrain-derived approaches;
- clustering approaches;
- native alternatives.

VAD

Research:

- Silero VAD;
- WebRTC VAD;
- Sherpa VAD;
- ONNX VAD;
- other current alternatives.

Acceleration

Research actual support for:

- ARM NEON;
- CPU;
- GPU;
- NNAPI;
- Vulkan;
- OpenCL where relevant;
- vendor-specific acceleration;
- other Android inference backends.

---

8. RESEARCH VALIDATION

For every critical dependency verify:

- repository exists;
- project is maintained;
- latest release;
- recent activity;
- Android support;
- ABI support;
- build requirements;
- native dependencies;
- model compatibility;
- license;
- known limitations;
- documentation quality.

If information cannot be verified:

UNVERIFIED

Do not fabricate certainty.

---

9. RESEARCH DATE

Record:

Research date:

Do not call something "latest" unless it was actually verified.

---

10. TECHNOLOGY DECISION MATRIX

For every major subsystem produce:

Subsystem:
Candidates:
Selected:
Reason:
Rejected alternatives:
Known risks:
Fallback:
License:
Verification status:
Confidence:

Score candidates from 1–5 for:

- Android compatibility;
- offline capability;
- performance;
- memory efficiency;
- accuracy;
- GPU support;
- ARM/CPU optimization;
- maintenance;
- documentation;
- integration complexity;
- license suitability;
- long-term viability.

Do not blindly average scores.

Explain important weighting.

---

11. HARD CONSTRAINTS

Reject a technology if it:

- requires cloud inference;
- cannot operate offline;
- cannot support the target Android architecture;
- has incompatible licensing;
- cannot run required models;
- is fundamentally incompatible with the required UX;
- creates unacceptable privacy risk;
- has no viable implementation path.

A high score cannot compensate for a hard-constraint violation.

---

12. MODEL RESEARCH

Research actual models, not merely inference runtimes.

For each candidate model determine:

- architecture;
- parameter count;
- language support;
- quantization;
- file size;
- RAM requirement;
- quality;
- CPU performance;
- GPU compatibility;
- license;
- redistribution restrictions;
- commercial-use restrictions;
- Android feasibility.

---

13. MODEL TIERS

Determine appropriate model tiers based on actual research.

Potential categories may include:

Very Small
Small
Medium
Large

Do not assume these exact categories are optimal.

Each model should have a purpose such as:

- minimum-memory devices;
- balanced devices;
- high-quality transcription;
- maximum-quality transcription.

---

14. QUANTIZATION

Research quantization options.

Evaluate:

- quality degradation;
- RAM reduction;
- speed;
- backend compatibility;
- model compatibility.

Do not assume a specific quantization level is universally optimal.

---

15. LICENSE AUDIT

For every dependency and model identify:

- software license;
- model license;
- redistribution restrictions;
- commercial-use restrictions;
- attribution requirements.

Do not bundle or automatically download incompatible models.

---

16. ARCHITECTURE PHASE

After research, design the final architecture.

Do not implement before architecture is reviewed.

The architecture must explicitly define:

- modules;
- responsibilities;
- dependencies;
- data flow;
- AI pipeline;
- native boundary;
- threading;
- memory ownership;
- model lifecycle;
- error handling;
- background processing;
- persistence;
- testing.

---

17. ARCHITECTURAL PRINCIPLES

Prefer:

- modular architecture;
- clear dependency direction;
- separation of concerns;
- testable business logic;
- replaceable AI backends;
- explicit native boundaries;
- lifecycle-safe components;
- structured concurrency.

Avoid:

- God Objects;
- global mutable state;
- hidden singletons;
- circular dependencies;
- unnecessary frameworks;
- speculative abstractions;
- overengineering.

---

18. SUGGESTED ARCHITECTURAL STRUCTURE

Use a clean architecture or equivalent architecture with clear separation between:

Presentation
    ↓
Domain
    ↓
Data
    ↓
AI / Inference
    ↓
Native Runtime

The exact module structure must be determined after research.

Do not blindly follow this diagram if research demonstrates a better architecture.

---

19. DEPENDENCY RULE

Dependencies must flow in a controlled direction.

High-level business logic must not become tightly coupled to:

- Android UI;
- Whisper implementation;
- a specific native runtime;
- a specific model;
- a specific acceleration backend.

Use interfaces at replacement boundaries.

---

20. STATE MANAGEMENT

The application must have explicit state.

Avoid scattered Boolean flags such as:

isLoading
isProcessing
isPaused
isError
isComplete

when a state machine is more appropriate.

---

21. TRANSCRIPTION JOB

Represent transcription as a persistent domain object.

Conceptually:

TranscriptionJob

id
source
sourceMetadata
model
configuration
language
languageDetectionResult
status
progress
segments
speakers
statistics
errors
createdAt
startedAt
completedAt

Refine the exact schema during architecture.

---

22. JOB STATES

Use explicit states:

CREATED
VALIDATING
PREPARING_AUDIO
LOADING_MODEL
DETECTING_LANGUAGE
RUNNING_VAD
RUNNING_DIARIZATION
RUNNING_TRANSCRIPTION
ASSEMBLING_RESULT
READY_FOR_REVIEW
EXPORTING
COMPLETED
CANCELLED
FAILED

Optional states may be added where justified.

Invalid transitions must be rejected.

---

23. TRANSCRIPTION CONFIGURATION

Conceptually:

TranscriptionConfig

modelId
languageMode
manualLanguage
enableDiarization
speakerCountMode
expectedSpeakerCount
enableVAD
backendPreference
threadCount
timestampMode
qualityMode

Do not expose low-level parameters to ordinary users without a UX justification.

---

24. LANGUAGE MODE

Support:

AUTO
MANUAL

AUTO:

- determine language locally;
- provide detected language;
- provide confidence when meaningful.

MANUAL:

- use explicitly selected language;
- avoid unnecessary language detection.

---

25. DIARIZATION MODE

Support:

DISABLED
AUTOMATIC
KNOWN_SPEAKER_COUNT

If speaker count is specified, handle mismatches gracefully.

---

26. SPEAKER MODEL

Conceptually:

Speaker

id
displayName
colorIndex
confidence

Internal IDs must remain stable.

Changing:

Speaker 1

to:

Alex

must not change transcript references.

---

27. TRANSCRIPT MODEL

Conceptually:

Transcript

id
sourceJobId
language
duration
speakers
segments
metadata

---

28. SEGMENT MODEL

Conceptually:

TranscriptSegment

id
startTime
endTime
speakerId
text
confidence
words

Fields may be optional if unsupported by the selected engine.

Never fabricate unsupported data.

---

29. WORD TIMESTAMPS

Architecturally support word timestamps.

If the backend cannot reliably provide them:

- do not fabricate them;
- retain segment timestamps.

---

30. TIMESTAMP INTEGRITY

Use a precise deterministic internal representation.

Prefer integer microseconds or equivalent.

Avoid floating-point seconds as the canonical representation.

Conversion to SRT/VTT/JSON occurs at serialization boundaries.

Required invariants:

startTime >= 0
endTime > startTime
segments ordered chronologically

---

31. TRANSCRIPT ASSEMBLY

Create a canonical transcript by reconciling:

Whisper output
+
VAD
+
diarization
+
timestamps

Do not maintain incompatible independent timelines.

---

32. OVERLAPPING SPEECH

Architecturally allow overlapping speech.

Do not assume one speaker per timestamp.

If the selected diarization system cannot represent overlap:

document the limitation.

Do not fabricate precision.

---

33. MODEL ENTITY

Conceptually:

ModelDescriptor

id
name
version
provider
type
format
quantization
sizeBytes
estimatedRamBytes
supportedLanguages
supportedBackends
license
downloadUrl
checksum
installedVersion
status

Refine after research.

---

34. MODEL REGISTRY

Separate:

Model Registry

from:

Installed Models

Registry presence does not imply successful installation.

---

35. MODEL VALIDATION

Before activation:

1. verify existence;
2. verify expected size where available;
3. verify checksum;
4. verify format;
5. verify metadata;
6. verify backend compatibility;
7. verify resources.

Only then mark the model usable.

---

36. ATOMIC MODEL INSTALLATION

Use:

temporary location
↓
download
↓
verification
↓
atomic move
↓
registration
↓
available

A partially downloaded model must never appear usable.

---

37. MODEL DELETION

Before deletion:

- check whether model is active;
- stop dependent processing;
- release native runtime;
- remove files;
- update registry;
- update UI.

Never delete a model still in native use.

---

38. MODEL SWITCHING

Use:

release old runtime
↓
load new runtime
↓
validate
↓
activate

If the new model fails:

retain the previous working model whenever possible.

---

39. MODEL DOWNLOAD

Support states:

QUEUED
DOWNLOADING
PAUSED
VERIFYING
COMPLETED
FAILED
CANCELLED

Where available show:

- bytes downloaded;
- total bytes;
- speed;
- estimated remaining time.

Do not fabricate estimates.

---

40. MEDIA INPUT

Do not assume media is a filesystem path.

Use Android-safe URI/content abstractions.

The domain layer must not directly depend on Android URI types.

---

41. MEDIA FORMATS

Research and support appropriate audio/video formats.

At minimum investigate:

- WAV;
- MP3;
- FLAC;
- M4A;
- OGG;
- OPUS;
- AAC;
- MP4;
- MKV;
- MOV;
- WEBM.

Do not claim support for a format that cannot be reliably decoded.

---

42. MEDIA VALIDATION

Before processing verify:

- readable;
- supported;
- audio stream exists;
- duration valid;
- audio decodable;
- sufficient temporary storage.

Handle malformed files gracefully.

---

43. MEDIA METADATA

Where available extract:

- filename;
- duration;
- MIME type;
- audio streams;
- sample rate;
- channel count;
- codec;
- file size.

---

44. AUDIO PREPROCESSING

The AI engine must receive the format required by the selected backend.

The preprocessing layer handles:

- decoding;
- channel conversion;
- resampling;
- normalization where required;
- PCM conversion.

Do not load arbitrarily large media entirely into RAM.

---

45. TEMPORARY STORAGE

Temporary files must:

- reside in application-controlled storage;
- never overwrite user data;
- be cleaned after success;
- be cleaned after failure;
- be safely handled after process interruption.

---

46. PROCESSING PROGRESS

Expose:

stage
stageProgress
overallProgress

when meaningful.

Do not represent unrelated processing stages with misleadingly precise percentages.

---

47. CANCELLATION

Cancellation is mandatory.

It must propagate through:

UI
→ domain
→ worker
→ inference
→ JNI
→ native runtime

After cancellation:

- stop inference;
- stop workers;
- release native resources;
- close files;
- clean temporary resources;
- persist correct state.

---

48. PAUSE

Do not expose pause/resume for transcription unless the architecture can guarantee safe resumability.

Cancellation is mandatory.

Pause is optional.

---

49. PARTIAL RESULTS

For long recordings preserve completed information where technically safe.

Example:

Hour 1 → completed
Hour 2 → completed
Hour 3 → processing
Hour 4 → pending

A failure must not unnecessarily destroy completed results.

---

50. RECOVERY

After interruption determine:

- completed chunks;
- incomplete chunk;
- model state;
- temporary files;
- job state.

Resume only when consistency can be guaranteed.

Otherwise restart safely.

Never silently create duplicated or missing transcript sections.

---

51. AI PIPELINE

The canonical pipeline should be conceptually:

Media
 ↓
Decoder
 ↓
Audio preprocessing
 ↓
VAD
 ↓
Speaker segmentation / embedding
 ↓
Whisper inference
 ↓
Timestamp alignment
 ↓
Speaker assignment
 ↓
Transcript assembly

The exact order may change if the selected technology requires another architecture.

The final order must be justified by research.

---

52. WHISPER ENGINE

The Whisper layer must be abstracted behind an application-level interface.

The rest of the application must not depend directly on a particular Whisper implementation.

Support:

- model loading;
- unloading;
- transcription;
- language configuration;
- timestamps;
- cancellation;
- progress;
- errors.

---

53. DIARIZATION ENGINE

Abstract the diarization subsystem.

It must support:

- segmentation;
- speaker embeddings;
- clustering;
- speaker timeline;
- confidence where available;
- cancellation.

---

54. VAD ENGINE

Abstract VAD.

It must provide speech regions suitable for downstream processing.

---

55. NATIVE/JNI BOUNDARY

Clearly define:

- Kotlin → native calls;
- native → Kotlin callbacks/events;
- object lifetime;
- ownership;
- thread ownership;
- cancellation;
- error propagation;
- buffers;
- strings;
- arrays.

---

56. JNI SAFETY

Explicitly inspect for:

- GlobalRef leaks;
- LocalRef misuse;
- stale native pointers;
- use-after-free;
- double-free;
- callbacks after destruction;
- invalid object lifetime;
- buffer overflow;
- incorrect thread attachment.

---

57. NATIVE MEMORY OWNERSHIP

Every native resource must have explicit ownership.

Document:

creator
owner
lifetime
release mechanism
thread

No implicit ownership assumptions.

---

58. THREADING

Use structured concurrency on Kotlin side.

Native threads must:

- have clear ownership;
- terminate deterministically;
- respond to cancellation;
- not outlive their owning runtime.

---

59. BACKEND SELECTION

Do not assume GPU/NNAPI/Vulkan is automatically faster.

Determine:

1. supported operation;
2. supported model;
3. supported device;
4. memory overhead;
5. transfer overhead;
6. initialization overhead;
7. sustained performance;
8. fallback behavior.

Backend selection must be evidence-based.

---

60. DEVICE CAPABILITIES

Internally determine:

CPU architecture
CPU cores
RAM
GPU availability
NNAPI capability
Vulkan capability
NEON
supported ABIs
Android version
available storage
thermal capability where available

Do not equate API existence with actual AI capability.

---

61. MODEL RECOMMENDATION

Recommendation should consider:

- RAM;
- CPU;
- GPU;
- accelerator support;
- available storage;
- thermal capability;
- battery;
- model size;
- quantization;
- quality;
- expected performance.

Do not simply recommend the largest model that fits storage.

---

62. FALLBACK HIERARCHY

Conceptually:

validated preferred accelerator
        ↓
validated alternative accelerator
        ↓
optimized CPU
        ↓
reduced-performance CPU
        ↓
user-visible failure

The exact hierarchy must be determined from research and benchmarks.

---

63. ANDROID ARCHITECTURE

Use modern Android development practices.

Research and select appropriate versions of:

- Kotlin;
- Android Gradle Plugin;
- Compose;
- AndroidX;
- Media3;
- NDK;
- CMake.

Do not blindly hardcode outdated versions.

---

64. UI

The UI must clearly represent:

- model availability;
- model downloads;
- processing state;
- progress;
- errors;
- cancellation;
- transcript;
- speakers;
- exports.

Provide:

- loading states;
- empty states;
- error states;
- success states.

---

65. NAVIGATION

Every screen must be reachable.

Back navigation must be predictable.

Long-running work must not depend on an Activity instance.

---

66. LIFECYCLE

Verify behavior during:

- rotation;
- backgrounding;
- foregrounding;
- configuration changes;
- Activity recreation;
- process recreation.

Long-running work must survive UI lifecycle changes where appropriate.

---

67. BACKGROUND PROCESSING

Long transcription must not block the UI.

Use an Android-appropriate mechanism for long-running work based on current platform restrictions.

Research current Android requirements before implementation.

---

68. ACCESSIBILITY

Support:

- TalkBack;
- semantic labels;
- meaningful descriptions;
- appropriate touch targets;
- text scaling;
- sufficient contrast;
- non-color-only status indicators.

---

69. ADAPTIVE UI

The application should work on:

- phones;
- large phones;
- tablets;
- different screen sizes;
- portrait;
- landscape where appropriate.

---

70. EXPORT ARCHITECTURE

All exporters operate on the canonical transcript:

Canonical Transcript
        ↓
Exporter
        ↓
TXT / SRT / VTT / JSON

Never export directly from raw Whisper output.

---

71. TXT EXPORT

Produce readable text containing:

- timestamps;
- speaker names;
- transcript.

Example:

[00:01:23] Speaker 1:
Text...

[00:01:31] Speaker 2:
Text...

Exact formatting may be configurable.

---

72. SRT EXPORT

Requirements:

- sequential numbering;
- valid timestamps;
- valid ordering;
- UTF-8;
- valid subtitle structure.

---

73. VTT EXPORT

Requirements:

WEBVTT

plus valid cues and timestamps.

---

74. JSON EXPORT

Preserve canonical transcript information.

Conceptually include:

schemaVersion
metadata
source
duration
language
model
speakers
segments
statistics

The final JSON schema must be formally defined before implementation.

---

75. JSON VERSIONING

Every JSON export must include a schema version.

Future schema changes must not silently invalidate old exports.

---

76. SEARCH

Search must operate on the canonical transcript.

Support:

- full-text search;
- case-insensitive search;
- Unicode-safe search;
- speaker filtering;
- timestamp navigation.

---

77. SPEAKER EDITING

Changing a speaker's display name must modify metadata, not transcript identity.

Internal speaker IDs remain stable.

---

78. STATISTICS

Where technically available record:

processingDuration
mediaDuration
realTimeFactor
model
backend
peakMemory
threadCount
detectedLanguage
speakerCount

Do not fabricate unavailable metrics.

---

79. PRIVACY

The canonical data flow is:

Media
 ↓
Android device
 ↓
Local AI
 ↓
Local transcript
 ↓
Local export

No cloud inference.

No mandatory account.

No mandatory telemetry.

No mandatory analytics.

No advertising SDK required for core functionality.

---

80. NETWORK BOUNDARY

Network access is restricted to explicitly initiated functions such as:

- model catalog;
- model download;
- model update.

Transcription itself must not require network access.

---

81. OFFLINE VERIFICATION

Disable network connectivity and verify:

- application launches;
- installed models remain usable;
- transcription works;
- diarization works;
- export works;
- search works;
- settings work;
- media processing works.

Only unavailable functionality should be operations requiring data that has not been downloaded yet.

---

82. SECURITY

Audit:

File input

- URI validation;
- MIME validation;
- malformed media;
- oversized input;
- path traversal where applicable.

Model downloads

- HTTPS;
- checksum;
- atomic installation;
- corrupted downloads;
- no execution of downloaded content.

Native

- bounds checking;
- integer overflow;
- pointer validity;
- ownership;
- synchronization.

---

83. LOGGING

Use structured logs.

Conceptually:

timestamp
level
component
event
jobId
modelId
message

Do not log complete transcripts or sensitive media content by default.

---

84. LOG LEVELS

Support:

ERROR
WARN
INFO
DEBUG
TRACE

Release builds should use conservative logging.

---

85. JOB CORRELATION

All logs belonging to a transcription should contain a job ID.

This allows tracing across:

Kotlin
JNI
C++
AI runtime

---

86. NATIVE DIAGNOSTICS

Propagate structured native errors where practical.

Do not hide native failures behind generic:

Something went wrong.

Internally preserve useful diagnostic information.

---

87. CRASH DIAGNOSTICS

Do not require cloud crash reporting.

If local diagnostics exist, allow explicit user export.

Never automatically transmit audio or transcript content.

---

88. PERFORMANCE LAB

Do not rely on theoretical hardware specifications.

Measure real application performance.

---

89. DEVICE CLASSES

Classify devices by capability rather than hardcoded CPU names.

Potential classes:

ENTRY
MID_RANGE
HIGH_END
FLAGSHIP

The classification algorithm must be validated.

---

90. BENCHMARK METRICS

Measure where possible:

Startup

- cold startup;
- warm startup.

Model

- download;
- verification;
- loading;
- unloading.

Inference

- first-result latency;
- processing duration;
- real-time factor;
- tokens/sec where meaningful.

Memory

- baseline;
- loaded model;
- peak RAM;
- native heap.

CPU/GPU

- utilization;
- memory;
- transfer overhead where measurable.

Thermal

- initial state;
- sustained state;
- throttling.

Battery

- battery impact where measurable.

---

91. REAL-TIME FACTOR

Use:

RTF = processing_time / media_duration

Record exact methodology.

---

92. BENCHMARK DATASET

Use a controlled local dataset containing representative:

- clean speech;
- noisy speech;
- multiple speakers;
- pauses;
- fast speech;
- slow speech;
- multiple languages;
- different formats/sample rates.

Do not use private user recordings for automated benchmarks.

---

93. ACCURACY BENCHMARKING

Where reference transcripts exist, measure:

- WER;
- CER where relevant;
- timestamp deviation;
- speaker assignment quality.

Speed alone does not constitute AI quality.

---

94. DIARIZATION BENCHMARKING

Where reference speaker labels exist, evaluate appropriate metrics such as:

- DER;
- missed speech;
- false alarm;
- speaker confusion;
- segmentation quality.

---

95. SUSTAINED PERFORMANCE

Benchmark long enough to expose:

- thermal throttling;
- memory pressure;
- performance degradation;
- battery restrictions.

Do not benchmark only the first minute.

---

96. LOW-RAM TESTING

Under memory pressure, consider:

- reducing parallelism;
- reducing buffers;
- releasing caches;
- switching backend;
- smaller chunks;
- user warning.

Never permit uncontrolled memory growth.

---

97. STORAGE PRESSURE

Before processing large media:

- estimate required temporary storage;
- warn when insufficient;
- do not start operations that cannot reasonably complete;
- clean temporary files after failure.

---

98. BENCHMARK REPORT

Generate:

Device:
Android:
Application version:
Model:
Model version:
Backend:
Threads:
Audio:
Duration:

Startup:
Model load:
Inference:
RTF:

Peak RAM:
Average RAM:

CPU:
GPU:

Thermal:
Battery:

WER:
CER:
DER:

Result:

Do not invent unavailable measurements.

---

99. PERFORMANCE REGRESSION

Maintain benchmark baselines.

When performance degrades significantly:

1. identify regression;
2. determine cause;
3. decide whether intentional;
4. fix or document.

---

100. TESTING STRATEGY

Testing must occur at multiple levels:

Unit
 ↓
Integration
 ↓
Native/JNI
 ↓
AI pipeline
 ↓
Android lifecycle
 ↓
Performance
 ↓
Security
 ↓
UX
 ↓
End-to-end

---

101. UNIT TESTS

Test:

Domain

- use cases;
- state transitions;
- validation;
- errors.

Model manager

- states;
- checksum;
- compatibility;
- installation;
- deletion;
- updates;
- corruption.

Media

- metadata;
- malformed media;
- unsupported media.

Transcription

- language;
- configuration;
- timestamp handling;
- segment assembly.

Diarization

- speaker assignment;
- clustering;
- merging.

Export

- TXT;
- SRT;
- VTT;
- JSON.

---

102. PROPERTY-BASED TESTING

Test invariants.

Examples:

startTime <= endTime

segments are chronologically ordered

speaker references are valid

verified model != corrupted model

export timestamps remain valid

---

103. NATIVE TESTING

Test:

- model loading;
- unloading;
- invalid models;
- corrupted models;
- memory allocation;
- cancellation;
- shutdown;
- audio conversion;
- resampling;
- VAD;
- embeddings;
- clustering;
- timestamps.

---

104. CONCURRENCY TESTING

Test:

- cancellation during model loading;
- cancellation during transcription;
- cancellation during diarization;
- cancellation during export;
- model switching during processing;
- rapid navigation;
- process recreation;
- repeated start/cancel.

---

105. MEDIA TEST MATRIX

Test appropriate supported formats including:

- WAV;
- MP3;
- FLAC;
- M4A;
- OGG;
- OPUS;
- AAC;
- MP4;
- MKV;
- MOV;
- WEBM.

Also test:

- mono;
- stereo;
- multichannel;
- different sample rates;
- silent files;
- long recordings;
- damaged files;
- missing audio streams.

---

106. TRANSCRIPTION TEST MATRIX

Test:

Short

10–30 seconds.

Medium

5–30 minutes.

Long

1–4 hours.

Very long

4+ hours where hardware permits.

Test:

- speech;
- silence;
- noise;
- multiple speakers;
- overlapping speech;
- accents;
- punctuation;
- language switching;
- poor recordings.

---

107. DIARIZATION TEST MATRIX

Test:

- one speaker;
- two speakers;
- many speakers;
- alternating speakers;
- monologues;
- interruptions;
- overlapping speech;
- silence;
- similar voices.

---

108. EXPORT VERIFICATION

Every exporter must be independently validated.

Verify:

- UTF-8;
- Unicode;
- ordering;
- timestamps;
- escaping;
- schema correctness.

---

109. FAILURE INJECTION

Intentionally simulate:

- corrupted model;
- insufficient RAM;
- insufficient storage;
- invalid media;
- interrupted download;
- cancellation;
- thermal throttling;
- low battery;
- process death;
- native exception;
- backend failure.

The application must recover whenever technically possible.

---

110. LIFECYCLE VERIFICATION

Verify:

- rotation;
- background;
- foreground;
- process recreation;
- configuration changes;
- Activity recreation;
- service termination;
- notification cancellation.

---

111. RESOURCE LEAK AUDIT

Inspect for:

- Activity leaks;
- Context leaks;
- coroutine leaks;
- thread leaks;
- native memory leaks;
- file descriptor leaks;
- media-player leaks;
- model-context leaks;
- temporary-file leaks;
- JNI reference leaks.

---

112. BUILD VERIFICATION

Verify:

- Gradle;
- plugins;
- dependencies;
- repositories;
- namespaces;
- resources;
- Manifest;
- CMake;
- NDK;
- ABI;
- JNI;
- R8/ProGuard;
- native packaging.

Never claim the project builds merely because source code looks correct.

---

113. STATIC ANALYSIS

Inspect for:

- unreachable code;
- unused dependencies;
- unsafe casts;
- nullable misuse;
- coroutine misuse;
- lifecycle leaks;
- race conditions;
- mutable global state;
- excessive allocation;
- resource leaks;
- bad exception handling.

---

114. REQUIREMENTS TRACEABILITY

Every requirement must map to:

Requirement
 ↓
Implementation
 ↓
Module
 ↓
Relevant file/class/function
 ↓
Test
 ↓
Verification status

No mandatory requirement may exist without an implementation path.

---

115. PROJECT STATE MACHINE

Maintain:

INIT
↓
RESEARCHING
↓
RESEARCH_COMPLETE
↓
ARCHITECTING
↓
ARCHITECTURE_REVIEW
↓
ARCHITECTURE_FROZEN
↓
IMPLEMENTING
↓
MODULE_REVIEW
↓
INTEGRATION
↓
QA
↓
BUG_FIXING
↓
FINAL_AUDIT
↓
COMPLETE

Do not skip states.

If a serious architectural issue is discovered, explicitly move back to architecture review.

---

116. SPECIFICATION FREEZE

After:

Research
Technology Selection
Architecture
Data Model
Implementation Blueprint

freeze the specification.

Do not continuously redesign the system during implementation.

---

117. POST-FREEZE CHANGES

Allow architectural changes only if:

- required API does not exist;
- build is impossible;
- critical performance problem exists;
- critical security problem exists;
- licensing problem exists;
- Android incompatibility exists;
- selected technology is unavailable;
- mandatory requirement cannot otherwise be satisfied.

For every major change record:

CHANGE:
WHY:
OLD:
NEW:
IMPACT:
AFFECTED MODULES:
REQUIRED REVALIDATION:

---

118. PROJECT MANIFEST

Maintain an internal project manifest containing:

Project name
Current state
Architecture version
Technology stack
Generated modules
Generated files
Pending files
Known issues
Known assumptions
Selected models
Selected backends
ABI targets
SDK targets
Test status
QA status

This is the canonical project state.

---

119. FILE STATE

Each file progresses through:

PLANNED
↓
IN_PROGRESS
↓
GENERATED
↓
REVIEWED
↓
VERIFIED

A generated file is not automatically a verified file.

---

120. MODULE STATE

Each module progresses through:

PLANNED
↓
IMPLEMENTING
↓
IMPLEMENTED
↓
UNIT_TESTED
↓
INTEGRATION_TESTED
↓
VERIFIED

---

121. CODE GENERATION RULES

When generating source code:

- generate complete files;
- use exact paths;
- include imports;
- include package declarations;
- include all required dependencies;
- include configuration;
- include resources;
- include native code;
- include tests;
- do not abbreviate repetitive code;
- do not use placeholders.

Forbidden examples:

// TODO: implement

// omitted for brevity

implementation omitted

...

inside a supposedly complete source file.

---

122. NO PLACEHOLDERS

Never use:

- fake implementations;
- mock implementations in production code;
- empty methods;
- dummy return values;
- hardcoded fake model responses;
- simulated AI results.

Tests may use mocks where appropriate.

Production functionality must be real.

---

123. NO FABRICATED APIs

Never invent:

- Android APIs;
- Compose APIs;
- Gradle plugins;
- Whisper APIs;
- C++ APIs;
- JNI functions;
- Vulkan extensions;
- model metadata.

If uncertain:

research first.

If still uncertain:

mark the assumption explicitly and design a replaceable abstraction.

---

124. RESPONSE PROTOCOL

The project must be developed in phases.

PHASE 1 — RESEARCH

Output:

Requirements interpretation
Technology candidates
Comparison
Selected technologies
Rejected technologies
Risks
Fallbacks
Licenses
Confidence
Final recommendation

Do not generate application source code yet.

---

PHASE 2 — ARCHITECTURE

Output:

System architecture
Module structure
Dependency graph
Data flow
AI pipeline
Native boundary
Threading
Memory ownership
Model lifecycle
Error model
Storage
Background execution
Testing strategy
Risks
Final architecture

Do not generate the full implementation yet.

---

PHASE 3 — PROJECT BLUEPRINT

Generate:

- complete project tree;
- module list;
- package structure;
- file list;
- dependencies;
- implementation order.

---

PHASE 4 — IMPLEMENTATION

Generate files in dependency order.

For every file state:

FILE:
PATH:
PURPOSE:
DEPENDENCIES:

Then provide the complete file.

---

PHASE 5 — MODULE REVIEW

For each logical module:

MODULE REVIEW

Module:
Files:
Interfaces:
Dependencies:
Tests:
Known issues:
Fixes:
Status:

---

PHASE 6 — INTEGRATION

Verify module interactions as a complete system.

Do not inspect files only in isolation.

---

PHASE 7 — QA

Execute the complete QA protocol.

---

PHASE 8 — FINAL AUDIT

Output:

FINAL AUDIT

Requirements:
Architecture:
Build:
Native:
AI:
Media:
Export:
Privacy:
Security:
Performance:
UX:
Testing:

PROJECT STATUS:

---

125. RESPONSE LENGTH LIMIT

If the response limit is reached:

- finish the current file;
- never truncate source code;
- record exact project state;
- list completed files;
- list remaining files;
- continue from the exact stopping point.

Never restart the project.

Never regenerate verified files unnecessarily.

---

126. CONTEXT CONTINUITY

When continuing after a previous response:

1. inspect the project manifest;
2. identify current state;
3. identify completed files;
4. identify pending files;
5. continue from the next required item.

Do not silently redesign completed architecture.

Do not overwrite verified components without reason.

---

127. ANTI-DEGRADATION PROTOCOL

As the project becomes larger, do not simplify it merely because maintaining consistency is difficult.

Never:

- remove features;
- replace real implementations with stubs;
- collapse modules without justification;
- delete tests;
- reduce error handling;
- remove native safety;
- remove model validation;
- remove export formats;
- replace offline processing with cloud APIs.

Complexity is not an excuse to reduce requirements.

---

128. UNCERTAINTY PROTOCOL

When certain:

implement.

When uncertain:

research.

When research is inconclusive:

choose the simplest viable architecture with replaceable boundaries.

When a feature is impossible:

do not fake it.

Report:

FEATURE:
WHY IMPOSSIBLE:
WHAT WAS INVESTIGATED:
CURRENT LIMITATION:
BEST PRACTICAL ALTERNATIVE:
ARCHITECTURAL CONSEQUENCE:

---

129. AVOID OVERENGINEERING

Do not introduce:

- microservices;
- unnecessary frameworks;
- unnecessary abstraction layers;
- speculative infrastructure;
- unused extension points.

Prefer:

simple + correct

over:

complex + theoretically flexible

But never remove required functionality merely to simplify architecture.

---

130. BENCHMARK REQUIREMENTS

Performance measurements must never be invented.

If a benchmark has not been executed:

state:

NOT MEASURED

If only theoretical expectations exist:

state:

ESTIMATED

Never present estimates as empirical measurements.

---

131. DEFINITION OF DONE

The project is NOT COMPLETE if:

- a mandatory feature is missing;
- a required module is missing;
- a required file is missing;
- source contains placeholders;
- source contains intentional production stubs;
- an interface lacks implementation;
- an implementation lacks its required caller;
- a screen is unreachable;
- dependency resolution fails;
- JNI contracts are incomplete;
- native resources leak;
- cancellation is broken;
- exports are incomplete;
- model verification is missing;
- offline processing is violated;
- critical error paths are unhandled.

---

132. FINAL ACCEPTANCE CHECKLIST

Architecture

- [ ] dependency direction correct
- [ ] module boundaries correct
- [ ] no circular dependencies
- [ ] no God Objects
- [ ] no hidden global state

Android

- [ ] modern APIs
- [ ] lifecycle-safe
- [ ] responsive UI
- [ ] adaptive UI
- [ ] accessibility
- [ ] background execution correct

AI

- [ ] Whisper backend operational
- [ ] diarization operational
- [ ] VAD operational
- [ ] language detection operational
- [ ] model management operational
- [ ] accelerator fallback operational

Native

- [ ] JNI complete
- [ ] native resources released
- [ ] cancellation propagated
- [ ] threads terminate correctly
- [ ] memory ownership explicit

Media

- [ ] supported formats tested
- [ ] invalid files handled
- [ ] large files handled
- [ ] timestamps preserved

Export

- [ ] TXT
- [ ] SRT
- [ ] VTT
- [ ] JSON

Privacy

- [ ] no cloud inference
- [ ] no mandatory telemetry
- [ ] no hidden uploads
- [ ] offline operation verified

Model Management

- [ ] download
- [ ] pause/resume where supported
- [ ] checksum
- [ ] install
- [ ] update
- [ ] switch
- [ ] delete
- [ ] corruption handling

Reliability

- [ ] cancellation
- [ ] process recreation
- [ ] error handling
- [ ] recovery
- [ ] temporary-file cleanup
- [ ] crash resilience

Performance

- [ ] benchmark methodology defined
- [ ] representative devices tested where available
- [ ] memory behavior evaluated
- [ ] sustained performance evaluated
- [ ] thermal behavior evaluated
- [ ] backend fallback evaluated

---

133. FINAL STATUS

At the end output exactly one of:

PROJECT STATUS: COMPLETE

or:

PROJECT STATUS: INCOMPLETE

If incomplete, output:

BLOCKERS
---------
1.
2.
3.

MISSING COMPONENTS
------------------
1.
2.
3.

REQUIRED ACTIONS
----------------
1.
2.
3.

Never output "PROJECT STATUS: COMPLETE" while a critical requirement remains unresolved.

---

134. FINAL EXECUTION DIRECTIVE

You are responsible for delivering the complete Android application described by this specification.

Do not begin by generating source code.

First:

1. Analyze requirements.
2. Resolve ambiguities using engineering judgment.
3. Research current technologies.
4. Compare alternatives.
5. Select the optimal technology stack.
6. Produce the final architecture.
7. Validate the architecture.
8. Produce the complete project tree.
9. Define module boundaries.
10. Define interfaces and contracts.
11. Define implementation order.
12. Freeze the specification.

Then implement:

13. Generate complete files.
14. Never use placeholders.
15. Never omit repetitive code.
16. Never invent APIs.
17. Preserve consistency between all files.
18. Track project state.
19. Review every module.
20. Fix defects immediately.
21. Continue until the complete project is generated.

Then verify:

22. Requirements.
23. Architecture.
24. Build.
25. Static analysis.
26. Unit tests.
27. Integration tests.
28. JNI/NDK.
29. AI pipeline.
30. Media processing.
31. Export.
32. Android lifecycle.
33. Security.
34. Privacy.
35. Performance.
36. UX.
37. Accessibility.
38. Offline operation.
39. Failure recovery.
40. Final repository integrity.

Fix discovered critical defects.

Repeat affected verification after fixes.

Never declare completion merely because source code has been generated.

The project is complete only when the implementation satisfies the specification and passes the Definition of Done.

If the response limit is reached:

- never truncate a file;
- finish the current file;
- record the exact project state;
- list completed files;
- list remaining files;
- continue from the exact stopping point.

Never restart the project.

Never regenerate completed files unnecessarily.

Never silently change architecture.

Never reduce requirements to make implementation easier.

Never substitute a demonstration for a real implementation.

Never substitute confidence for verification.

Your priority order is:

1. Privacy
2. Correctness
3. Functional completeness
4. Reliability
5. Data integrity
6. AI quality
7. Android compatibility
8. Performance
9. Battery efficiency
10. Maintainability
11. UX
12. Implementation speed

Begin with the RESEARCH PHASE.

Do not generate application source code until the research and technology-selection phase has been completed.Итог
