package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CloudCheckinApp

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val app = context.applicationContext as? CloudCheckinApp ?: return
    val scheduleManager = app.container.scheduleManager
    scheduleManager.enqueueExpeditedCheckin(isScheduledRun = true)
    // Chain tomorrow's alarm immediately
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = true)
  }
}
