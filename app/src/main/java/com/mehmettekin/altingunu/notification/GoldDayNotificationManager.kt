package com.mehmettekin.altingunu.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mehmettekin.altingunu.MainActivity
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoldDayNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "gold_day_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE_BASE = 2000
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
        try {
            // Önceki alarm'ları iptal et
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
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Error scheduling reminders", e)
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

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("winner_name", winnerName)
                putExtra("amount", amount)
                putExtra("payment_day", day)
                putExtra("payment_month", month)
                putExtra("payment_year", year)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE_BASE + resultIndex,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Alarm'ı ayarla
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderDate.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderDate.timeInMillis,
                    pendingIntent
                )
            }

            android.util.Log.d("NotificationManager", "Alarm scheduled for index: $resultIndex")
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to schedule alarm for index: $resultIndex", e)
        }
    }

    fun cancelAllReminders() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Tüm alarm'ları iptal et
            for (i in 0..100) {
                val intent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE_BASE + i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
            android.util.Log.d("NotificationManager", "All alarms cancelled")
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to cancel alarms", e)
        }
    }

    // ✅ Test notification
    fun showTestNotification(winnerName: String, amount: String, paymentDate: String) {
        createAndShowNotification(winnerName, amount, paymentDate)
    }

    fun createAndShowNotification(winnerName: String, amount: String, paymentDate: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                context.getString(R.string.gold_day_reminder_text, winnerName, amount, paymentDate)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        context.getString(R.string.gold_day_reminder_big_text, winnerName, amount, paymentDate)
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

// ✅ BroadcastReceiver for AlarmManager
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val winnerName = intent.getStringExtra("winner_name") ?: return
            val amount = intent.getStringExtra("amount") ?: return
            val paymentDay = intent.getIntExtra("payment_day", 0)
            val paymentMonth = intent.getIntExtra("payment_month", 0)
            val paymentYear = intent.getIntExtra("payment_year", 0)

            if (paymentDay == 0 || paymentMonth == 0 || paymentYear == 0) return

            val paymentDate = String.format("%02d/%02d/%d", paymentDay, paymentMonth, paymentYear)

            // Direkt notification oluştur (Hilt kullanmıyoruz çünkü BroadcastReceiver)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, "gold_day_reminders")
                .setSmallIcon(R.drawable.gold_bar)
                .setContentTitle(context.getString(R.string.gold_day_reminder_title))
                .setContentText(
                    context.getString(R.string.gold_day_reminder_text, winnerName, amount, paymentDate)
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            context.getString(R.string.gold_day_reminder_big_text, winnerName, amount, paymentDate)
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)

        } catch (e: Exception) {
            android.util.Log.e("NotificationReceiver", "Error showing notification", e)
        }
    }
}