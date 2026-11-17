package com.example

import android.app.Application
import com.example.core.CheckinOrchestrator
import com.example.core.CloudGameApiClient
import com.example.data.EncryptedStorageRepository
import com.example.notification.NotificationHelper
import com.example.scheduler.ScheduleManager
import com.topjohnwu.superuser.Shell

class CloudCheckinApp : Application() {

  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this
    NotificationHelper.createNotificationChannels(this)
    container = AppContainer(this)
  }

  companion object {
    init {
      Shell.enableVerboseLogging = BuildConfig.DEBUG
      Shell.setDefaultBuilder(
        Shell.Builder.create()
          .setFlags(Shell.FLAG_MOUNT_MASTER)
          .setTimeout(10),
      )
    }

    @Volatile
    lateinit var instance: CloudCheckinApp
      private set
  }
}

class AppContainer(application: Application) {
  val storageRepository: EncryptedStorageRepository by lazy {
    EncryptedStorageRepository(application)
  }
  val apiClient: CloudGameApiClient by lazy {
    CloudGameApiClient()
  }
  val scheduleManager: ScheduleManager by lazy {
    ScheduleManager(application, storageRepository)
  }
  val orchestrator: CheckinOrchestrator by lazy {
    CheckinOrchestrator(application, storageRepository, apiClient, scheduleManager)
  }
}
