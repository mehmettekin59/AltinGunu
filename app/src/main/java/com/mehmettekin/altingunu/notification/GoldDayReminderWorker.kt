package com.mehmettekin.altingunu.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class GoldDayReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        try {
            val winnerName = inputData.getString("winner_name") ?: return Result.failure()
            val amount = inputData.getString("amount") ?: return Result.failure()
            val paymentDay = inputData.getInt("payment_day", 0)
            val paymentMonth = inputData.getInt("payment_month", 0)
            val paymentYear = inputData.getInt("payment_year", 0)

            val paymentDate = String.format("%02d/%02d/%d", paymentDay, paymentMonth, paymentYear)

            // Notification gönder
            val notificationManager = GoldDayNotificationManager(applicationContext)
            notificationManager.showTestNotification(winnerName, amount, paymentDate)

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}