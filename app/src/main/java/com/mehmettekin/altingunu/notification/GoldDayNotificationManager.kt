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
        private const val WORK_TAG_PREFIX = "gold_day_reminder"
    }

    fun scheduleReminders(
        drawSettings: ParticipantsScreenWholeInformation,
        results: List<DrawResult>,
        reminderDaysBefore: Int = 1
    ) {
        // Önceki reminder'ları iptal et
        cancelAllReminders()

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
            set(year, month - 1, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reminderDate = paymentDate.clone() as Calendar
        reminderDate.add(Calendar.DAY_OF_MONTH, -reminderDaysBefore)

        // Geçmiş tarihler için skip
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

        val workRequest = OneTimeWorkRequestBuilder<GoldDayReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("$WORK_TAG_PREFIX$resultIndex") // Tutarlı etiketleme
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun cancelAllReminders() {
        // Tüm gold day reminder work'lerini iptal et
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_PREFIX)
    }

    // Test için geliştirme fonksiyonu
    fun scheduleTestReminder(delaySeconds: Long = 10) {
        val inputData = workDataOf(
            "winner_name" to "Test Kullanıcı",
            "amount" to "1.000 TL",
            "payment_day" to Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            "payment_month" to Calendar.getInstance().get(Calendar.MONTH) + 1,
            "payment_year" to Calendar.getInstance().get(Calendar.YEAR),
            "reminder_days_before" to 0
        )

        val testWork = OneTimeWorkRequestBuilder<GoldDayReminderWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(inputData)
            .addTag("test_reminder")
            .build()

        WorkManager.getInstance(context).enqueue(testWork)
    }
}