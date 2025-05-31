package com.mehmettekin.altingunu.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mehmettekin.altingunu.MainActivity
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoldDayNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "gold_day_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "gold_day_reminder_work"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.gold_day_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.gold_day_reminder_description)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminders(
        drawSettings: ParticipantsScreenWholeInformation,
        results: List<DrawResult>,
        reminderDaysBefore: Int = 1
    ) {
        // Önceki reminder'ları iptal et
        cancelAllReminders()

        // Her ödeme tarihi için reminder schedule et
        results.forEachIndexed { index, result ->
            val (day, month, year) = drawSettings.getNextPaymentDate(index)
            scheduleReminderForDate(
                day = day,
                month = month,
                year = year,
                winnerName = result.participantName,
                amount = result.amount,
                reminderDaysBefore = reminderDaysBefore,
                resultIndex = index
            )
        }
    }

    private fun scheduleReminderForDate(
        day: Int,
        month: Int,
        year: Int,
        winnerName: String,
        amount: String,
        reminderDaysBefore: Int,
        resultIndex: Int
    ) {
        val paymentDate = Calendar.getInstance().apply {
            set(year, month - 1, day, 9, 0, 0) // Sabah 9'da hatırlat
            set(Calendar.MILLISECOND, 0)
        }

        // Hatırlatma tarihini hesapla
        val reminderDate = paymentDate.clone() as Calendar
        reminderDate.add(Calendar.DAY_OF_MONTH, -reminderDaysBefore)

        // Eğer hatırlatma tarihi geçmişse skip et
        if (reminderDate.timeInMillis <= System.currentTimeMillis()) {
            return
        }

        val delay = reminderDate.timeInMillis - System.currentTimeMillis()

        val inputData = workDataOf(
            "winner_name" to winnerName,
            "amount" to amount,
            "payment_day" to day,
            "payment_month" to month,
            "payment_year" to year,
            "reminder_days_before" to reminderDaysBefore
        )

        val reminderWork = OneTimeWorkRequestBuilder<GoldDayReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_$resultIndex")
            .build()

        WorkManager.getInstance(context).enqueue(reminderWork)
    }

    fun cancelAllReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_NAME)
    }

    fun showTestNotification(winnerName: String, amount: String, paymentDate: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.gold_bar)
            .setContentTitle(context.getString(R.string.gold_day_reminder_title))
            .setContentText(
                context.getString(
                    R.string.gold_day_reminder_text,
                    winnerName,
                    amount,
                    paymentDate
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        context.getString(
                            R.string.gold_day_reminder_big_text,
                            winnerName,
                            amount,
                            paymentDate
                        )
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}