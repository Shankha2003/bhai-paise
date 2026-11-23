package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BhaiPaiseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }
            .getOrNull()
            ?.takeIf { it.isEmailVerified } ?: return
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("userDevices").document(user.uid)
                .collection("tokens").document(token)
                .set(mapOf("updatedAt" to System.currentTimeMillis()))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Bhai Paise"
        val body = message.notification?.body ?: message.data["body"] ?: return
        GroupNotificationHelper(this).showNotification(
            title = title,
            message = body,
            notificationId = (message.messageId ?: "${message.data["groupId"]}_${System.currentTimeMillis()}").hashCode(),
        )
    }
}
