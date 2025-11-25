package com.example.bloom.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.bloom.R
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var elapsedTime = 0L // en secondes

    // Pour communiquer avec l'UI
    private var updateCallback: ((Long) -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "timer_service_channel"
        const val NOTIFICATION_ID = 1
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startTimer()
            "STOP" -> stopTimer()
        }
        return START_STICKY
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000) // 1 seconde
                elapsedTime++
                updateCallback?.invoke(elapsedTime)

                // Mettre à jour la notification
                updateNotification()
            }
        }

        // Démarrer en foreground
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun stopTimer() {
        timerJob?.cancel()
        elapsedTime = 0L
        updateCallback?.invoke(0L)
        stopForeground(true)
        stopSelf()
    }

    fun getElapsedTime(): Long = elapsedTime

    fun setUpdateCallback(callback: (Long) -> Unit) {
        updateCallback = callback
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🌿 Bloom - Plant Tracker")
            .setContentText("Timer en cours: ${formatTime(elapsedTime)}")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Montre le timer du suivi des plantes"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }
}