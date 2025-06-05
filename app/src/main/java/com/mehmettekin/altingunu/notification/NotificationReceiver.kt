package com.mehmettekin.altingunu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mehmettekin.altingunu.domain.model.ItemType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManager: GoldDayNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            GoldDayNotificationManager.ACTION_PAYMENT_REMINDER -> {
                val groupId = intent.getStringExtra(GoldDayNotificationManager.EXTRA_GROUP_ID) ?: return
                val payerName = intent.getStringExtra(GoldDayNotificationManager.EXTRA_PAYER_NAME) ?: return
                val amount = intent.getStringExtra(GoldDayNotificationManager.EXTRA_AMOUNT) ?: return
                val paymentDate = intent.getStringExtra(GoldDayNotificationManager.EXTRA_PAYMENT_DATE) ?: return
                val itemTypeName = intent.getStringExtra(GoldDayNotificationManager.EXTRA_ITEM_TYPE) ?: return
                val specificItem = intent.getStringExtra(GoldDayNotificationManager.EXTRA_SPECIFIC_ITEM) ?: ""

                val itemType = try {
                    ItemType.valueOf(itemTypeName)
                } catch (e: Exception) {
                    ItemType.TL
                }

                notificationManager.showNotification(
                    payerName = payerName,
                    amount = amount,
                    paymentDate = paymentDate,
                    groupId = groupId,
                    itemType = itemType,
                    specificItem = specificItem
                )
            }
        }
    }
}