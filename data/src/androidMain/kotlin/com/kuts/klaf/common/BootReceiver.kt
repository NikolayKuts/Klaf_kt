package com.kuts.klaf.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.kuts.klaf.common.DeckReviewRescheduler.Companion.launchDeckReviewRescheduling
import com.lib.lokdroid.core.logD
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val workerManager: WorkManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        intent.doWhenBootCompleted {
            logD("onReceive() called")
            workerManager.launchDeckReviewRescheduling()
        }
    }

    private inline fun Intent.doWhenBootCompleted(block: () -> Unit) {
        if (action == Intent.ACTION_BOOT_COMPLETED) block()
    }
}
