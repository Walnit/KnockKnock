package com.example.knockknock.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.view.View
import android.app.NotificationManager
import android.content.Intent

object KnockNotificationManager {

    lateinit var notificationManager: NotificationManager

    fun createSystemNotificationChannel(context: Context) : NotificationChannel {
        val channel = NotificationChannel("com.example.knockknock.system", "KnockKnock - System", NotificationManager.IMPORTANCE_DEFAULT)
        channel.enableVibration(true)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channel
    }

    fun createChatNotificationChannel(id: String, name: String, description: String, importance: Int, context: Context) {
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.parseColor("#E55934")
        channel.enableVibration(true)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun sendSystemNotification(channel: NotificationChannel, title: String, content: String, context: Context){
        val notificationID = 101
        val channelID = channel.id
        val notification = Notification.Builder(context, channelID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setChannelId(channelID).build()
        notificationManager.notify(notificationID, notification)
    }

//    fun sendNotification(view: View){
//        val notificationID = 101
//        val channelID = "com.example.notifydemo.news"
//        val intentResult = Intent(this, ResultActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this@MainActivity, 0,
//            intentResult, PendingIntent.FLAG_UPDATE_CURRENT)
//        val notification = Notification.Builder(this@MainActivity, channelID)
//            .setContentTitle(getString(R.string.egNote))
//            .setContentText(getString(R.string.egText))
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentIntent(pendingIntent)
//            .setChannelId(channelID).build()
//        notificationManager.notify(notificationID, notification)
//    }
//    fun sendBundledNotification(view: View){
//        val channelID = "com.example.notifydemo.news"
//        val GROUP_KEY_NOTIFY = "group_key_notify"
//        val builderSummary = Notification.Builder(this@MainActivity, channelID)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle(getString(R.string.bundleTitle))
//            .setContentText(getString(R.string.bundleText))
//            .setGroup(GROUP_KEY_NOTIFY).setGroupSummary(true)
//        val builder1 = Notification.Builder(this@MainActivity, channelID)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle(getString(R.string.bundleTitle1))
//            .setContentText(getString(R.string.bundleMsg1))
//            .setGroup(GROUP_KEY_NOTIFY).setGroupSummary(true)
//        val builder2 = Notification.Builder(this@MainActivity, channelID)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle(getString(R.string.bundleTitle2))
//            .setContentText(getString(R.string.bundleMsg2))
//            .setGroup(GROUP_KEY_NOTIFY).setGroupSummary(true)
//        val builder3 = Notification.Builder(this@MainActivity, channelID)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle(getString(R.string.bundleTitle3))
//            .setContentText(getString(R.string.bundleMsg3))
//            .setGroup(GROUP_KEY_NOTIFY).setGroupSummary(true)
//        notificationManager.notify(81, builder1.build())
//        notificationManager.notify(82, builder2.build())
//        notificationManager.notify(83, builder3.build())
//        notificationManager.notify(80, builderSummary.build())
//    }
//    fun directReplyNotification(view : View){
//        val channelID = "com.example.notifydemo.news"
//        val KEY_TEXT_REPLY = "key_text_reply"
//        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
//            .setLabel(getString(R.string.replyPrompt)).build()
//        val intentResult = Intent(this, ResultActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this@MainActivity, 0,
//            intentResult, PendingIntent.FLAG_UPDATE_CURRENT)
//        val icon = Icon.createWithResource(this@MainActivity,
//            android.R.drawable.ic_dialog_info)
//        val replyAction = Notification.Action.Builder(icon, "Reply", pendingIntent)
//            .addRemoteInput(remoteInput).build()
//        val notification = Notification.Builder(this@MainActivity, channelID)
//            .setColor(ContextCompat.getColor(this@MainActivity,
//                R.color.design_default_color_primary))
//            .setContentTitle(getString(R.string.egNote))
//            .setContentText(getString(R.string.egText))
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setChannelId(channelID)
//            .setActions(replyAction).build()
//        notificationManager.notify(101, notification)
//    }

}