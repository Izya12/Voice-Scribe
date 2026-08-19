package com.example.app

import android.app.Application
import com.example.core.domain.logging.AppLogger
import com.example.core.domain.repository.TranscriptionRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class VoiceScribeApp : Application() {

    @Inject
    lateinit var jobs: TranscriptionRepository

    @Inject
    lateinit var logger: AppLogger

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // A non-terminal job surviving a process restart means the pipeline died
        // mid-run (native crash); it can never reach COMPLETED. Fail it on boot.
        appScope.launch { runCatching { jobs.reconcileStaleJobs() } }
        // Native crashes (SIGSEGV in sherpa) kill the process without a Kotlin
        // stack trace; log whatever reaches the JVM so the failure is visible
        // in the log file even when the job card shows a bare FAILED.
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger.error("Uncaught", "Thread ${thread.name} crashed", throwable)
        }
    }
}