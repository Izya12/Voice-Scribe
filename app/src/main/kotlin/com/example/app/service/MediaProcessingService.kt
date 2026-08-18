package com.example.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.app.R
import com.example.app.ui.MainActivity
import com.example.app.ui.TranscriptionProgressStore
import com.example.core.domain.usecase.RunTranscriptionUseCase
import com.example.core.model.JobState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service of type `mediaProcessing` (§11). Owns the transcription
 * pipeline; keeps the process alive and renders a persistent progress
 * notification with a Cancel action.
 */
@AndroidEntryPoint
class MediaProcessingService : Service() {

    @Inject
    lateinit var runTranscription: RunTranscriptionUseCase

    @Inject
    lateinit var progressStore: TranscriptionProgressStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJobId: String? = null
    private var pipelineJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(EXTRA_ACTION)
        if (action == ACTION_CANCEL) {
            val jobId = intent?.getStringExtra(EXTRA_JOB_ID)
            // A startForegroundService() with ACTION_CANCEL (cancel of a
            // not-yet-started job) still requires startForeground() within 5s.
            startForegroundWithType(jobId ?: "cancelled")
            jobId?.let(progressStore::remove)
            pipelineJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val jobId = intent?.getStringExtra(EXTRA_JOB_ID)
        if (jobId == null || jobId == activeJobId) {
            return START_NOT_STICKY
        }
        activeJobId = jobId
        startForegroundWithType(jobId)

        pipelineJob = scope.launch {
            runTranscription.invoke(jobId).collectLatest { progress ->
                progressStore.update(progress)
                updateNotification(jobId, progress.state, progress.fraction)
                if (progress.state.isTerminal) {
                    progressStore.remove(jobId)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        activeJobId = null
        super.onDestroy()
    }

    private fun startForegroundWithType(jobId: String) {
        val notification = buildNotification(jobId, JobState.SUBMITTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(jobId: String, state: JobState, fraction: Float) {
        val notification = buildNotification(jobId, state, fraction)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(jobId: String, state: JobState, fraction: Float = 0f): Notification {
        val cancelIntent = PendingIntent.getService(
            this,
            REQUEST_CANCEL,
            Intent(this, MediaProcessingService::class.java).putExtra(EXTRA_ACTION, ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentText = if (state.isTerminal || fraction <= 0f) {
            stateLabel(state)
        } else {
            getString(R.string.notification_progress, stateLabel(state), (fraction * 100).toInt())
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(!state.isTerminal)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.notification_cancel), cancelIntent)
            .build()
    }

    private fun stateLabel(state: JobState): String = when (state) {
        JobState.SUBMITTED -> getString(R.string.state_submitted)
        JobState.DECODING -> getString(R.string.state_decoding)
        JobState.PREPROCESSING -> getString(R.string.state_preprocessing)
        JobState.DIARIZING -> getString(R.string.state_diarizing)
        JobState.TRANSCRIBING -> getString(R.string.state_transcribing)
        JobState.COMPLETED -> getString(R.string.state_completed)
        JobState.FAILED -> getString(R.string.state_failed)
        JobState.CANCELLED -> getString(R.string.state_cancelled)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_JOB_ID = "com.example.app.extra.JOB_ID"
        const val EXTRA_ACTION = "com.example.app.extra.ACTION"
        const val ACTION_CANCEL = "cancel"
        private const val CHANNEL_ID = "transcription"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CANCEL = 1
        private const val REQUEST_OPEN = 2
    }
}