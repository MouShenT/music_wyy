package com.example.music_wyy.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AutomationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleDailySignin() {
        val request = PeriodicWorkRequestBuilder<SigninWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_SIGNIN,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelDailySignin() {
        workManager.cancelUniqueWork(WORK_NAME_SIGNIN)
    }

    fun scheduleYunbei() {
        val request = PeriodicWorkRequestBuilder<YunbeiWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_YUNBEI,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelYunbei() {
        workManager.cancelUniqueWork(WORK_NAME_YUNBEI)
    }

    fun scheduleScrobble() {
        val request = PeriodicWorkRequestBuilder<ScrobbleWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_SCROBBLE,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelScrobble() {
        workManager.cancelUniqueWork(WORK_NAME_SCROBBLE)
    }

    companion object {
        private const val WORK_NAME_SIGNIN = "daily_signin"
        private const val WORK_NAME_YUNBEI = "daily_yunbei"
        private const val WORK_NAME_SCROBBLE = "daily_scrobble"
    }
}
