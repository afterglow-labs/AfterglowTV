package com.afterglowtv.app.store.amazon.adm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afterglowtv.app.MainActivity
import com.afterglowtv.app.R

internal object AmazonDeviceMessagePayload {
    private const val TAG = "AfterglowADM"
    private const val CHANNEL_ID = "afterglow_adm_messages"
    private const val CHANNEL_NAME = "Afterglow messages"

    fun handle(context: Context, intent: Intent) {
        val extras = intent.extras
        if (extras == null || extras.isEmpty) {
            Log.i(TAG, "ADM message received with no extras")
            return
        }

        val payload = extras.keySet()
            .associateWith { key -> extras.getString(key).orEmpty() }
            .filterValues { it.isNotBlank() }

        Log.i(TAG, "ADM message keys: ${payload.keys.joinToString()}")
        showNotificationIfPresent(context, payload)
    }

    private fun showNotificationIfPresent(context: Context, payload: Map<String, String>) {
        val title = payload.firstValue("title", "notificationTitle", "heading")
            ?: "Afterglow TV"
        val message = payload.firstValue("message", "body", "text", "alert")
            ?: return

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "ADM message received, notification permission not granted")
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        createNotificationChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            payload["url"]?.let { putExtra("adm_url", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            message.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(message.hashCode(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun Map<String, String>.firstValue(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> get(key)?.takeIf { it.isNotBlank() } }
}
