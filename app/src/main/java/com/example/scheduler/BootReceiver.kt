package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CloudCheckinApp

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
      val app = context.applicationContext as? CloudCheckinApp ?: return
      app.container.storageRepository.performMidnightResetIfNeeded()
      app.container.scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    }
  }
}
