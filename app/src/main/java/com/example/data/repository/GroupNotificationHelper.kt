package com.example.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class GroupNotificationHelper(context: Context) {
    private val appContext = context.applicationContext
    private val notifications = NotificationManagerCompat.from(appContext)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Group activity",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerts when someone joins one of your groups"
            }
            appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showMemberJoined(groupName: String, event: CloudGroupJoinEvent) {
        showNotification("New group member", "${event.memberName} joined $groupName", event.id.hashCode())
    }

    fun showNotification(title: String, message: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!notifications.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        @Suppress("MissingPermission")
        notifications.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "group_activity"
    }
}
