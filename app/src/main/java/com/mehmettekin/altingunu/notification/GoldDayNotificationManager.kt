package com.mehmettekin.altingunu.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mehmettekin.altingunu.MainActivity
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.ValueFormatter
import com.mehmettekin.altingunu.utils.convertNumerals
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoldDayNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fcmRepository: FcmRepository,
    private val kapaliCarsiRepository: KapaliCarsiRepository
) {
    companion object {
        const val CHANNEL_ID = "gold_day_reminder_channel"
        const val NOTIFICATION_ID_BASE = 1000
        const val ACTION_PAYMENT_REMINDER = "com.mehmettekin.altingunu.PAYMENT_REMINDER"
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_PAYER_NAME = "payer_name"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_PAYMENT_DATE = "payment_date"
        const val EXTRA_ITEM_TYPE = "item_type"
        const val EXTRA_SPECIFIC_ITEM = "specific_item"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "GoldDayNotificationManager"
    }

    private val notificationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.gold_day_reminders)
            val descriptionText = context.getString(R.string.gold_day_reminder_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminders(
        drawSettings: ParticipantsScreenWholeInformation,
        results: List<DrawResult>,
        reminderDaysBefore: Int,
        groupId: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        results.forEachIndexed { index, result ->
            val paymentDate = parsePaymentDate(result.month)
            paymentDate?.let { date ->
                val reminderDate = Calendar.getInstance().apply {
                    time = date
                    add(Calendar.DAY_OF_MONTH, -reminderDaysBefore)
                    set(Calendar.HOUR_OF_DAY, 10)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (reminderDate.timeInMillis > System.currentTimeMillis()) {
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = ACTION_PAYMENT_REMINDER
                        putExtra(EXTRA_GROUP_ID, groupId)
                        putExtra(EXTRA_PAYER_NAME, result.participantName)
                        putExtra(EXTRA_AMOUNT, result.amount)
                        putExtra(EXTRA_PAYMENT_DATE, result.month)
                        putExtra(EXTRA_ITEM_TYPE, drawSettings.itemType.name)
                        putExtra(EXTRA_SPECIFIC_ITEM, drawSettings.specificItem)
                        putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_BASE + index)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        NOTIFICATION_ID_BASE + index,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    reminderDate.timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    reminderDate.timeInMillis,
                                    pendingIntent
                                )
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderDate.timeInMillis,
                                pendingIntent
                            )
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Failed to schedule alarm", e)
                    }
                }
            }
        }
    }

    fun showNotification(
        payerName: String,
        amount: String,
        paymentDate: String,
        groupId: String,
        itemType: ItemType,
        specificItem: String
    ) {
        notificationScope.launch {
            var notificationText = context.getString(
                R.string.gold_day_reminder_text,
                payerName,
                amount,
                paymentDate
            )

            // Altın veya döviz ise güncel fiyat bilgisini ekle
            if ((itemType == ItemType.GOLD || itemType == ItemType.CURRENCY) && specificItem.isNotBlank()) {
                try {
                    val exchangeRates = kapaliCarsiRepository.getExchangeRates().first()
                    if (exchangeRates is ResultState.Success) {
                        val rate = exchangeRates.data.find { it.code == specificItem }
                        rate?.let {
                            val currentPrice = ValueFormatter.formatWithSymbol(
                                it.satis,
                                itemType,
                                specificItem
                            )
                            val itemName = when (itemType) {
                                ItemType.GOLD -> Constraints.goldCodeToName[specificItem] ?: specificItem
                                ItemType.CURRENCY -> Constraints.currencyCodeToName[specificItem] ?: specificItem
                                else -> ""
                            }
                            notificationText += "\n\n$itemName güncel fiyatı: $currentPrice"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get current price", e)
                }
            }

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .bigText(notificationText)
                .setBigContentTitle(context.getString(R.string.gold_day_reminder_title))

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
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.gold_day_reminder_title))
                .setContentText(notificationText)
                .setStyle(bigTextStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID_BASE, notification)

            // FCM ile grup üyelerine bildirim gönder
            sendFcmNotification(groupId, payerName, amount, paymentDate, itemType, specificItem)
        }
    }

    private suspend fun sendFcmNotification(
        groupId: String,
        payerName: String,
        amount: String,
        paymentDate: String,
        itemType: ItemType,
        specificItem: String
    ) {
        val title = context.getString(R.string.gold_day_reminder_title)
        var message = context.getString(
            R.string.gold_day_reminder_text,
            payerName,
            amount,
            paymentDate
        )

        // Güncel fiyat bilgisini ekle
        if ((itemType == ItemType.GOLD || itemType == ItemType.CURRENCY) && specificItem.isNotBlank()) {
            try {
                val exchangeRates = kapaliCarsiRepository.getExchangeRates().first()
                if (exchangeRates is ResultState.Success) {
                    val rate = exchangeRates.data.find { it.code == specificItem }
                    rate?.let {
                        val currentPrice = ValueFormatter.formatWithSymbol(
                            it.satis,
                            itemType,
                            specificItem
                        )
                        val itemName = when (itemType) {
                            ItemType.GOLD -> Constraints.goldCodeToName[specificItem] ?: specificItem
                            ItemType.CURRENCY -> Constraints.currencyCodeToName[specificItem] ?: specificItem
                            else -> ""
                        }
                        message += "\n$itemName güncel fiyatı: $currentPrice"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current price for FCM", e)
            }
        }

        val data = mapOf(
            "type" to "payment_reminder",
            "group_id" to groupId,
            "payer_name" to payerName,
            "amount" to amount,
            "payment_date" to paymentDate,
            "item_type" to itemType.name,
            "specific_item" to specificItem
        )

        fcmRepository.sendGroupNotification(
            groupId = groupId,
            title = title,
            message = message,
            data = data
        )
    }

    private fun parsePaymentDate(dateString: String): Date? {
        return try {
            val parts = dateString.split(" ")
            if (parts.size >= 3) {
                val day = parts[0].toInt()
                val monthName = parts[1]
                val year = parts[2].toInt()

                val calendar = Calendar.getInstance()
                val month = getMonthFromName(monthName)

                calendar.set(year, month, day)
                calendar.time
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: $dateString", e)
            null
        }
    }

    private fun getMonthFromName(monthName: String): Int {
        val months = mapOf(
            "Ocak" to Calendar.JANUARY, "January" to Calendar.JANUARY, "يناير" to Calendar.JANUARY,
            "Şubat" to Calendar.FEBRUARY, "February" to Calendar.FEBRUARY, "فبراير" to Calendar.FEBRUARY,
            "Mart" to Calendar.MARCH, "March" to Calendar.MARCH, "مارس" to Calendar.MARCH,
            "Nisan" to Calendar.APRIL, "April" to Calendar.APRIL, "أبريل" to Calendar.APRIL,
            "Mayıs" to Calendar.MAY, "May" to Calendar.MAY, "مايو" to Calendar.MAY,
            "Haziran" to Calendar.JUNE, "June" to Calendar.JUNE, "يونيو" to Calendar.JUNE,
            "Temmuz" to Calendar.JULY, "July" to Calendar.JULY, "يوليو" to Calendar.JULY,
            "Ağustos" to Calendar.AUGUST, "August" to Calendar.AUGUST, "أغسطس" to Calendar.AUGUST,
            "Eylül" to Calendar.SEPTEMBER, "September" to Calendar.SEPTEMBER, "سبتمبر" to Calendar.SEPTEMBER,
            "Ekim" to Calendar.OCTOBER, "October" to Calendar.OCTOBER, "أكتوبر" to Calendar.OCTOBER,
            "Kasım" to Calendar.NOVEMBER, "November" to Calendar.NOVEMBER, "نوفمبر" to Calendar.NOVEMBER,
            "Aralık" to Calendar.DECEMBER, "December" to Calendar.DECEMBER, "ديسمبر" to Calendar.DECEMBER
        )
        return months[monthName] ?: Calendar.JANUARY
    }
}