package com.example.app.di

import android.content.Context
import androidx.room.Room
import com.example.core.domain.engine.DiarizationEngine
import com.example.core.domain.engine.LanguageDetector
import com.example.core.domain.engine.SpeechEngine
import com.example.core.domain.engine.VadEngine
import com.example.core.domain.logging.AppLogger
import com.example.core.domain.repository.ModelRepository
import com.example.core.domain.repository.SettingsRepository
import com.example.core.domain.repository.TranscriptExporter
import com.example.core.domain.repository.TranscriptionRepository
import com.example.core.domain.usecase.DefaultRunTranscriptionUseCase
import com.example.core.domain.usecase.RunTranscriptionUseCase
import com.example.data.audio.AudioDecoder
import com.example.data.audio.AudioResampler
import com.example.data.database.VoiceScribeDao
import com.example.data.database.VoiceScribeDatabase
import com.example.data.export.TranscriptExporterImpl
import com.example.data.logging.FileAppLogger
import com.example.data.repository.ModelRepositoryImpl
import com.example.data.repository.TranscriptionRepositoryImpl
import com.example.data.settings.SettingsRepositoryImpl
import com.example.engine.diarization.SherpaDiarizationEngine
import com.example.engine.lang.SherpaLanguageDetector
import com.example.engine.model.SherpaModelFiles
import com.example.engine.vad.SherpaVadEngine
import com.example.engine.whisper.SherpaWhisperEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Hilt wiring (§1.2): `:app` is the composition root binding concrete
 * implementations from `:engine` and `:data` to the domain contracts.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    // --- Repositories / persistence ---

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(impl: TranscriptionRepositoryImpl): TranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptExporter(impl: TranscriptExporterImpl): TranscriptExporter

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: FileAppLogger): AppLogger

    // --- Engines (native sherpa adapters) ---

    @Binds
    @Singleton
    abstract fun bindSpeechEngine(impl: SherpaWhisperEngine): SpeechEngine

    @Binds
    @Singleton
    abstract fun bindVadEngine(impl: SherpaVadEngine): VadEngine

    @Binds
    @Singleton
    abstract fun bindDiarizationEngine(impl: SherpaDiarizationEngine): DiarizationEngine

    @Binds
    @Singleton
    abstract fun bindLanguageDetector(impl: SherpaLanguageDetector): LanguageDetector
}

/**
 * Provider-style bindings that need `@ApplicationContext`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModuleProviders {

    @Provides
    @Singleton
    fun provideDao(db: VoiceScribeDatabase): VoiceScribeDao = db.dao()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VoiceScribeDatabase =
        Room.databaseBuilder(context, VoiceScribeDatabase::class.java, "voicescribe.db")
            .addMigrations(VoiceScribeDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideAudioResampler(): AudioResampler = AudioResampler()

    @Provides
    @Singleton
    fun provideAudioDecoder(
        @ApplicationContext context: Context,
        resampler: AudioResampler,
    ): AudioDecoder = AudioDecoder(context, resampler)

    @Provides
    @Singleton
    fun provideSherpaModelFiles(impl: ModelRepositoryImpl): SherpaModelFiles = impl

    @Provides
    @Singleton
    fun provideSherpaWhisperEngine(
        @ApplicationContext context: Context,
        modelFiles: SherpaModelFiles,
    ): SherpaWhisperEngine = SherpaWhisperEngine(context, modelFiles)

    @Provides
    @Singleton
    fun provideSherpaVadEngine(
        @ApplicationContext context: Context,
        modelFiles: SherpaModelFiles,
    ): SherpaVadEngine = SherpaVadEngine(context, modelFiles)

    @Provides
    @Singleton
    fun provideSherpaDiarizationEngine(
        @ApplicationContext context: Context,
        modelFiles: SherpaModelFiles,
    ): SherpaDiarizationEngine = SherpaDiarizationEngine(context, modelFiles)

    @Provides
    @Singleton
    fun provideSherpaLanguageDetector(
        @ApplicationContext context: Context,
        modelFiles: SherpaModelFiles,
    ): SherpaLanguageDetector = SherpaLanguageDetector(context, modelFiles)

    @Provides
    @Singleton
    fun provideFileAppLogger(@ApplicationContext context: Context): FileAppLogger =
        FileAppLogger(File(context.filesDir, "logs"))

    @Provides
    @Singleton
    fun provideRunTranscriptionUseCase(
        jobs: TranscriptionRepository,
        models: ModelRepository,
        vad: VadEngine,
        diarization: DiarizationEngine,
        language: LanguageDetector,
        speech: SpeechEngine,
        logger: AppLogger,
    ): RunTranscriptionUseCase = DefaultRunTranscriptionUseCase(
        jobs = jobs,
        models = models,
        vad = vad,
        diarization = diarization,
        language = language,
        speech = speech,
        logger = logger,
    )
}