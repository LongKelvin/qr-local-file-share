package com.example.qlfs.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.qlfs.MainActivity
import com.example.qlfs.model.SharedFile
import com.example.qlfs.server.FileServer
import com.example.qlfs.share.SessionManager
import com.example.qlfs.share.ShareController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.qlfs.BuildConfig
import com.example.qlfs.network.HotspotManager

@AndroidEntryPoint
class ShareForegroundService : Service() {

    @Inject lateinit var shareController: ShareController
    @Inject lateinit var hotspotManager: HotspotManager

    private var fileServer: FileServer? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "qlfs_sharing_channel"
        const val ACTION_STOP = "com.example.qlfs.ACTION_STOP"
        
        const val EXTRA_FILES = "extra_files"
        const val EXTRA_PORT = "extra_port"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val files = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableArrayListExtra(EXTRA_FILES, SharedFile::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableArrayListExtra<SharedFile>(EXTRA_FILES)
        } ?: emptyList()

        if (files.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        var port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080

        val title = if (files.size == 1) files.first().name else "${files.size} files"
        startForeground(NOTIFICATION_ID, buildNotification(title))

        try {
            val session = SessionManager()
            shareController.activeSession = session

            // Port retry logic up to 3 times
            var bound = false
            var attempts = 0
            while (!bound && attempts < 3) {
                try {
                    fileServer = FileServer(
                        port = port,
                        files = files,
                        contentResolver = contentResolver,
                        session = session,
                        onTransferEvent = { event -> shareController.onTransferEvent(event) }
                    )
                    fileServer?.start()
                    bound = true
                    if (BuildConfig.DEBUG) Log.d("QLFS", "Server started on port $port")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w("QLFS", "Port $port in use, retrying on ${port + 1}")
                    port++
                    attempts++
                }
            }

            if (!bound) {
                throw Exception("Failed to bind to any port")
            }
            
            shareController.setServiceRunning(true)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("QLFS", "Unhandled error", e)
            shareController.reportServerError(e.message ?: "Failed to start server")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fileServer?.stop()
        fileServer = null
        hotspotManager.stopHotspot()
        shareController.setServiceRunning(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(fileName: String): Notification {
        val stopIntent = Intent(this, ShareForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // The intent target points to MainActivity, which we will create shortly.
        // Needs a valid class reference, will import here and make sure MainActivity exists later.
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sharing: $fileName")
            .setContentText("QLFS server is running")
            .setSmallIcon(android.R.drawable.stat_sys_upload) 
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sharing", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "File Sharing"
            val descriptionText = "Shows active local file sharing server status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
