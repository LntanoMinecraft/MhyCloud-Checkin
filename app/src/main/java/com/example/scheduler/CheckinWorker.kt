package com.example.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.CloudCheckinApp
import com.example.notification.NotificationHelper

class CheckinWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun getForegroundInfo(): ForegroundInfo {
    return NotificationHelper.createWorkerForegroundInfo(applicationContext)
  }

  override suspend fun doWork(): Result {
    val app = applicationContext as? CloudCheckinApp ?: return Result.failure()
    val isScheduled = inputData.getBoolean(KEY_IS_SCHEDULED, false)
    val isPeriodicFallback = inputData.getBoolean(KEY_IS_PERIODIC_FALLBACK, false)

    if (isPeriodicFallback) {
      val cfg = app.container.storageRepository.config.value
      if (!ScheduleManager.isPeriodicFallbackAllowedNow(cfg.scheduleHour, cfg.scheduleMinute)) {
        return Result.success()
      }
    }

    val batchResult = app.container.orchestrator.runBatchCheckin(isScheduledRun = isScheduled)
    return if (batchResult.hasTransientFailure && runAttemptCount < MAX_RETRY_ATTEMPTS) {
      Result.retry()
    } else {
      Result.success()
    }
  }

  companion object {
    const val KEY_IS_SCHEDULED = "key_is_scheduled"
    const val KEY_IS_PERIODIC_FALLBACK = "key_is_periodic_fallback"
    private const val MAX_RETRY_ATTEMPTS = 3
  }
}
