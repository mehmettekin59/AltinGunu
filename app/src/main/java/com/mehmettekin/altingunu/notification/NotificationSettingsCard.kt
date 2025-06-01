package com.mehmettekin.altingunu.notification

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText

@Composable
fun NotificationSettingsCard(
    isReminderEnabled: Boolean,
    reminderDaysBefore: Int,
    onReminderToggle: (Boolean) -> Unit,
    onReminderDaysChange: (Int) -> Unit,
    onTestNotification: () -> Unit,
    onRequestPermissions: () -> Unit // ✅ YENİ: İzin isteme callback'i
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = White
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = UiText.stringResource(R.string.notification_settings).asString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ YENİ: İzin kontrolü ve isteme butonu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {

                val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                } else true

                val hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else true

                if (!hasNotificationPermission || !hasExactAlarmPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!hasNotificationPermission) {
                                        UiText.stringResource(R.string.notification_permission_required).asString()
                                    } else {
                                        UiText.stringResource(R.string.exact_alarm_permission_required).asString()
                                    },
                                    color = Color.Red,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onRequestPermissions,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.Red
                                ),
                                border = BorderStroke(1.dp, Color.Red)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = UiText.stringResource(R.string.request_permissions).asString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Ana toggle switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = UiText.stringResource(R.string.reminder_enabled).asString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = NavyBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isReminderEnabled)
                                UiText.stringResource(R.string.gold_day_reminder_description).asString()
                            else
                                UiText.stringResource(R.string.reminder_disabled).asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = onReminderToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold,
                            checkedTrackColor = Gold.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Gün seçimi (sadece etkinse göster)
            if (isReminderEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = UiText.stringResource(R.string.reminder_days_before).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gün seçenekleri
                val reminderOptions = listOf(
                    1 to UiText.stringResource(R.string.reminder_1_day).asString(),
                    2 to UiText.stringResource(R.string.reminder_2_days).asString(),
                    3 to UiText.stringResource(R.string.reminder_3_days).asString(),
                    7 to UiText.stringResource(R.string.reminder_1_week).asString()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column {
                        reminderOptions.forEachIndexed { index, (days, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onReminderDaysChange(days) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (reminderDaysBefore == days) Gold else Color.DarkGray,
                                    fontWeight = if (reminderDaysBefore == days) FontWeight.Bold else FontWeight.Normal
                                )

                                if (reminderDaysBefore == days) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Gold
                                    )
                                }
                            }

                            if (index < reminderOptions.size - 1) {
                                HorizontalDivider(
                                    color = Color.Gray.copy(alpha = 0.2f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                // Test butonu
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = White
                    ),
                    border = BorderStroke(1.dp, White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(8.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = UiText.stringResource(R.string.test_notification).asString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}