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

    fun scheduleDailySignin() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

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

    companion object {
        private const val WORK_NAME_SIGNIN = "daily_signin"
    }
}
