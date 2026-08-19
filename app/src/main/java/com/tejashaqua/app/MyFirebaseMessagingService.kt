package com.tejashaqua.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tejashaqua.app.utils.AppStateTracker

class
MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"]
        val chatId = data["chatId"]

        android.util.Log.d("FCM", "Message received. Data: $data")

        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val posterId = data["posterId"]

        // If this is a listing notification and I am the one who posted it, 
        // skip showing the notification on this specific device.
        if (type == "listing" && posterId != null && posterId == currentUserId) {
            android.util.Log.d("FCM", "Skipping own listing notification on posting device")
            return
        }

        // If it's a chat message, check if we should show a notification
        if (type == "chat" && chatId != null) {
            // Don't show if app is in foreground AND user is already on the same chat screen
            if (AppStateTracker.isAppInForeground && AppStateTracker.activeChatId == chatId) {
                android.util.Log.d("FCM", "User is active in this chat, skipping notification")
                return
            }
        }

        val title = remoteMessage.notification?.title ?: data["title"] ?: "Tejash Aqua"
        val body = remoteMessage.notification?.body ?: data["body"] ?: ""
        
        // Clean up "(no change)", "No Change", and Telugu equivalent from notification body and title
        val noChangeRegex = Regex("\\(?no change\\)?|\\(?మార్పు లేదు\\)?", RegexOption.IGNORE_CASE)
        
        val cleanTitle = title.replace(noChangeRegex, "").trim()
        val cleanBody = body.replace(noChangeRegex, "").trim()
        
        sendNotification(cleanTitle, cleanBody, data)
    }

    override fun onNewToken(token: String) {
        android.util.Log.d("FCM", "New token: $token")
        // Update token in Firestore if user is logged in
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
        }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String> = emptyMap()) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            // Explicitly set action if present in data
            data["click_action"]?.let { action = it }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "general_notifications_v2"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for listings and rates"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
