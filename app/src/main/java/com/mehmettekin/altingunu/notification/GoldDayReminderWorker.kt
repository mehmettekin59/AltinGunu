package com.mehmettekin.altingunu.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mehmettekin.altingunu.MainActivity
import com.mehmettekin.altingunu.R

class GoldDayReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val winnerName = inputData.getString("winner_name") ?: return Result.failure()
            val amount = inputData.getString("amount") ?: return Result.failure()
            val paymentDay = inputData.getInt("payment_day", 0)
            val paymentMonth = inputData.getInt("payment_month", 0)
            val paymentYear = inputData.getInt("payment_year", 0)

            val paymentDate = String.format("%02d/%02d/%d", paymentDay, paymentMonth, paymentYear)

            // NotificationManager'ı manuel oluştur (Hilt Worker'da düzgün çalışmıyor)
            createAndShowNotification(winnerName, amount, paymentDate)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createAndShowNotification(winnerName: String, amount: String, paymentDate: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel oluştur (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "gold_day_reminders",
                applicationContext.getString(R.string.gold_day_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, "gold_day_reminders")
            .setSmallIcon(R.drawable.gold_bar)
            .setContentTitle(applicationContext.getString(R.string.gold_day_reminder_title))
            .setContentText(
                applicationContext.getString(R.string.gold_day_reminder_text, winnerName, amount, paymentDate)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        applicationContext.getString(R.string.gold_day_reminder_big_text, winnerName, amount, paymentDate)
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}