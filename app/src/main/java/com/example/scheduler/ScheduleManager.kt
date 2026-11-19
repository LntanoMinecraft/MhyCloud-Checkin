package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.EncryptedStorageRepository
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ScheduleManager(
  private val context: Context,
  private val storage: EncryptedStorageRepository,
) {

  private val alarmManager: AlarmManager?
    get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  fun canScheduleExactAlarms(): Boolean {
    val am = alarmManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      am.canScheduleExactAlarms()
    } else {
      true
    }
  }

  fun syncScheduleWithAccounts(forceTomorrow: Boolean = false) {
    if (!storage.hasAnyEnabledCheckinAccount()) {
      cancelAllSchedules()
      return
    }
    scheduleNextAlarm(forceTomorrow = forceTomorrow)
    ensurePeriodicFallbackRegistered()
  }

  fun scheduleNextAlarm(forceTomorrow: Boolean = false) {
    if (!storage.hasAnyEnabledCheckinAccount()) {
      cancelAllSchedules()
      return
    }
    val am = alarmManager ?: return
    val cfg = storage.config.value

    val now = ZonedDateTime.now(ZoneId.systemDefault())
    var baseTarget = now
      .withHour(cfg.scheduleHour.coerceIn(0, 23))
      .withMinute(cfg.scheduleMinute.coerceIn(0, 59))
      .withSecond(0)
      .withNano(0)

    if (forceTomorrow || !baseTarget.isAfter(now.plusMinutes(1))) {
      baseTarget = baseTarget.plusDays(1)
    }

    val jitterOffsetMinutes = if (cfg.jitterMinutes > 0) {
      Random.nextInt(0, cfg.jitterMinutes + 1).toLong()
    } else {
      0L
    }
    val triggerAt = baseTarget.plusMinutes(jitterOffsetMinutes)
    val triggerMillis = triggerAt.toInstant().toEpochMilli()

    val pendingIntent = buildAlarmPendingIntent()
    if (canScheduleExactAlarms()) {
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    } else {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    storage.updateConfig { it.copy(nextScheduledEpochMillis = triggerMillis) }
  }

  fun ensurePeriodicFallbackRegistered() {
    if (!storage.hasAnyEnabledCheckinAccount()) return
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val periodicRequest = PeriodicWorkRequestBuilder<CheckinWorker>(12, TimeUnit.HOURS)
      .setConstraints(constraints)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
      .setInputData(
        workDataOf(
          CheckinWorker.KEY_IS_SCHEDULED to true,
          CheckinWorker.KEY_IS_PERIODIC_FALLBACK to true,
        ),
      )
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      periodicRequest,
    )
  }

  fun enqueueExpeditedCheckin(isScheduledRun: Boolean) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = OneTimeWorkRequestBuilder<CheckinWorker>()
      .setConstraints(constraints)
      .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .setInputData(
        workDataOf(
          CheckinWorker.KEY_IS_SCHEDULED to isScheduledRun,
          CheckinWorker.KEY_IS_PERIODIC_FALLBACK to false,
        ),
      )
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      ONE_TIME_WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      request,
    )
  }

  fun cancelAllSchedules() {
    alarmManager?.cancel(buildAlarmPendingIntent())
    WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    storage.updateConfig { it.copy(nextScheduledEpochMillis = 0L) }
  }

  private fun buildAlarmPendingIntent(): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = ACTION_DAILY_CHECKIN_ALARM
    }
    return PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  companion object {
    private const val ACTION_DAILY_CHECKIN_ALARM = "com.example.action.DAILY_CHECKIN_ALARM"
    private const val ALARM_REQUEST_CODE = 3101
    private const val ONE_TIME_WORK_NAME = "cloud_checkin_expedited_work"
    private const val PERIODIC_WORK_NAME = "cloud_checkin_periodic_fallback"

    fun isPeriodicFallbackAllowedNow(
      scheduleHour: Int,
      scheduleMinute: Int,
      now: ZonedDateTime = ZonedDateTime.now(EncryptedStorageRepository.BEIJING_ZONE),
    ): Boolean {
      val beijingTime = now.withZoneSameInstant(EncryptedStorageRepository.BEIJING_ZONE).toLocalTime()
      val resetBoundary = LocalTime.of(EncryptedStorageRepository.GAME_DAY_RESET_HOUR, 0)
      if (beijingTime.isBefore(resetBoundary)) {
        return false
      }
      val userScheduleTime = LocalTime.of(scheduleHour.coerceIn(0, 23), scheduleMinute.coerceIn(0, 59))
      return !beijingTime.isBefore(userScheduleTime)
    }

    fun formatNextScheduleDisplay(epochMillis: Long): String {
      if (epochMillis <= 0L) return "未预约"
      val dt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
      return dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }
  }
}
