package com.example.data.model

import com.example.core.model.ModelDescriptor
import com.example.core.model.ModelTier

/**
 * Hardcoded verified model catalog (§8.1). Mirrors PROJECT_MANIFEST §5.
 *
 * SHA-256 values are the authoritative install checksums (§35–36). Verified
 * against `https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/checksum.txt`
 * and the `speaker-*` release tags on 2026-08-17.
 *
 * NOTE: Whisper ASR models and pyannote segmentation ship as `.tar.bz2`
 * archives and must be extracted into `filesDir/models/<modelId>/` after the
 * SHA-256 check (see ModelRepositoryImpl).
 */
object ModelCatalog {

    val catalog: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            id = "whisper-tiny",
            displayName = "Whisper Tiny Multilingual (int8)",
            fileName = "sherpa-onnx-whisper-tiny.tar.bz2",
            fileSizeBytes = 116_204_861,
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.tar.bz2",
            sha256 = "c46116994e539aa165266d96b325252728429c12535eb9d8b6a2b10f129e66b1",
            license = "MIT",
            tier = ModelTier.ENTRY,
        ),
        ModelDescriptor(
            id = "whisper-base",
            displayName = "Whisper Base Multilingual (int8)",
            fileName = "sherpa-onnx-whisper-base.tar.bz2",
            fileSizeBytes = 207_557_382,
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-base.tar.bz2",
            sha256 = "911b2083efd7c0dca2ac3b358b75222660dc09fb716d64fbfc417ba6c99ff3de",
            license = "MIT",
            tier = ModelTier.MID,
        ),
        ModelDescriptor(
            id = "whisper-medium",
            displayName = "Whisper Medium Multilingual (int8)",
            fileName = "sherpa-onnx-whisper-medium.tar.bz2",
            fileSizeBytes = 1_931_372_882,
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-medium.tar.bz2",
            sha256 = "614b1172557049069d846c29d9399640bce83a4dd6c580decebd9ce2a4f32c33",
            license = "MIT",
            tier = ModelTier.HIGH,
        ),
        ModelDescriptor(
            id = "silero-vad",
            displayName = "Silero VAD v5",
            fileName = "silero_vad_v5.onnx",
            fileSizeBytes = 2_313_101,
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad_v5.onnx",
            sha256 = "6b99cbfd39246b6706f98ec13c7c50c6b299181f2474fa05cbc8046acc274396",
            license = "MIT",
            tier = ModelTier.ENTRY,
        ),
        ModelDescriptor(
            id = "pyannote-segmentation",
            displayName = "pyannote-segmentation-3-0 (int8)",
            fileName = "sherpa-onnx-pyannote-segmentation-3-0.tar.bz2",
            fileSizeBytes = 6_958_444,
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-segmentation-models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2",
            sha256 = "24615ee884c897d9d2ba09bb4d30da6bb1b15e685065962db5b02e76e4996488",
            license = "MIT",
            tier = ModelTier.MID,
        ),
        ModelDescriptor(
            id = "3d-speaker-campplus",
            displayName = "3D-Speaker CAM++ (voxceleb)",
            fileName = "3dspeaker_speech_campplus_sv_en_voxceleb_16k.onnx",
            fileSizeBytes = 29_596_978,
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recongition-models/3dspeaker_speech_campplus_sv_en_voxceleb_16k.onnx",
            sha256 = "357a834f702b80161e5b981182c038e18553c1f2ca752ed6cec2052365d4129b",
            license = "Apache-2.0",
            tier = ModelTier.MID,
        ),
    )
}
