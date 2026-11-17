package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.example.MainActivity
import com.example.R
import com.example.data.AccountStatus
import com.example.data.GameType

data class BatchAccountOutcome(
  val accountId: String,
  val gameType: GameType,
  val alias: String,
  val status: AccountStatus,
  val deltaMinutes: Int,
  val freeMinutes: Int,
  val message: String,
)

object NotificationHelper {

  const val CHANNEL_SUMMARY = "checkin_summary"
  const val CHANNEL_ALERT = "checkin_alert"
  const val CHANNEL_WORKER = "checkin_worker"

  const val EXTRA_ALERT_ACCOUNT_ID = "extra_alert_account_id"
  const val EXTRA_ALERT_ACTION = "extra_alert_action"
  const val ACTION_OPEN_REPAIR = "open_repair"
  const val ACTION_OPEN_HOME = "open_home"

  private const val SUMMARY_NOTIFICATION_ID = 1001
  private const val WORKER_NOTIFICATION_ID = 1002
  private const val ALERT_NOTIFICATION_BASE_ID = 2000

  fun createNotificationChannels(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val summaryChannel = NotificationChannel(
      CHANNEL_SUMMARY,
      context.getString(R.string.channel_summary_name),
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = context.getString(R.string.channel_summary_desc)
    }

    val alertChannel = NotificationChannel(
      CHANNEL_ALERT,
      context.getString(R.string.channel_alert_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.channel_alert_desc)
      enableVibration(true)
    }

    val workerChannel = NotificationChannel(
      CHANNEL_WORKER,
      context.getString(R.string.channel_worker_name),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = context.getString(R.string.channel_worker_desc)
      setShowBadge(false)
    }

    manager.createNotificationChannels(listOf(summaryChannel, alertChannel, workerChannel))
  }

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun createWorkerForegroundInfo(context: Context): ForegroundInfo {
    createNotificationChannels(context)
    val notification: Notification = NotificationCompat.Builder(context, CHANNEL_WORKER)
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setContentTitle("云游戏签到巡检中")
      .setContentText("正在安全同步每日免费时长...")
      .setOngoing(true)
      .setSilent(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ForegroundInfo(
        WORKER_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
      )
    } else {
      ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
    }
  }

  fun showSummaryNotification(
    context: Context,
    outcomes: List<BatchAccountOutcome>,
  ) {
    if (outcomes.isEmpty() || !hasNotificationPermission(context)) return
    createNotificationChannels(context)

    val successCount = outcomes.count {
      it.status == AccountStatus.DONE_TODAY || it.status == AccountStatus.PAUSED_CAP_REACHED
    }
    val title = "今日云游戏签到完成 ($successCount/${outcomes.size})"

    val inboxStyle = NotificationCompat.InboxStyle()
      .setBigContentTitle(title)
    outcomes.forEach { item ->
      val deltaText = if (item.deltaMinutes > 0) "+${item.deltaMinutes}分钟" else "已同步"
      val line = "${item.gameType.displayName}·${item.alias}: ${item.status.label} ($deltaText, 剩余 ${item.freeMinutes} 分钟)"
      inboxStyle.addLine(line)
    }

    val summaryFallback = outcomes.joinToString("；") {
      "${it.gameType.displayName} ${it.freeMinutes}分钟"
    }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_ALERT_ACTION, ACTION_OPEN_HOME)
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      SUMMARY_NOTIFICATION_ID,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle(title)
      .setContentText(summaryFallback)
      .setStyle(inboxStyle)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) {
      try {
        NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
      } catch (_: SecurityException) {
        // Ignored if notification permission was revoked concurrently
      }
    }
  }

  fun showAlertNotification(
    context: Context,
    accountId: String,
    title: String,
    message: String,
    isReauthRequired: Boolean,
  ) {
    if (!hasNotificationPermission(context)) return
    createNotificationChannels(context)

    val notifId = ALERT_NOTIFICATION_BASE_ID + (accountId.hashCode() and 0x7FFF)
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_ALERT_ACCOUNT_ID, accountId)
      putExtra(
        EXTRA_ALERT_ACTION,
        if (isReauthRequired) ACTION_OPEN_REPAIR else ACTION_OPEN_HOME,
      )
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notifId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
      .setSmallIcon(android.R.drawable.stat_notify_error)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) {
      try {
        NotificationManagerCompat.from(context).notify(notifId, notification)
      } catch (_: SecurityException) {
        // Ignored if notification permission was revoked concurrently
      }
    }
  }
}
