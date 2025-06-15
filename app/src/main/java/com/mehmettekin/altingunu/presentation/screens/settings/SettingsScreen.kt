package com.mehmettekin.altingunu.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.MainActivity
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import android.provider.Settings
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Check notification permission status
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Pre-Android 13 doesn't need runtime permission
            }
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            // Show snackbar to guide user to settings
            viewModel.setError(UiText.stringResource(R.string.notification_permission_denied))
        }
    }

    LaunchedEffect(state.languageChanged) {
        if (state.languageChanged) {
            (context as? MainActivity)?.recreate()
            viewModel.resetLanguageChanged()
        }
    }

    // Error handling
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.asString(context),
                duration = SnackbarDuration.Short
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CommonTopAppBar(
                title = UiText.stringResource(R.string.title_settings).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() }

            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification Settings Card
            NotificationSettingsCard(
                hasPermission = hasNotificationPermission,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            // Language settings
            LanguageSettingsCard(
                selectedLanguage = state.selectedLanguage,
                onLanguageChange = { viewModel.onEvent(SettingsEvent.OnLanguageChange(it)) }
            )

            // Update interval settings
            UpdateIntervalSettingsCard(
                currentInterval = state.apiUpdateInterval,
                onIntervalChange = { viewModel.onEvent(SettingsEvent.OnApiUpdateIntervalChange(it)) }
            )

            // Reset to defaults button
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Button(
                    onClick = { viewModel.onEvent(SettingsEvent.OnDefaultsReset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = UiText.stringResource(R.string.reset_to_defaults).asString())
                }
            }

            // Loading indicator
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Gold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
                .padding(16.dp)
        ) {
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
                    text = "Bildirim Ayarları",
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Altın günü hatırlatmaları için bildirim izni gereklidir. Sistem otomatik olarak 1 gün önceden hatırlatma gönderecektir.",
                style = MaterialTheme.typography.bodyMedium,
                color = White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (hasPermission) {
                // Permission granted
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Green.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bildirimler aktif",
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Permission not granted
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = NavyBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bildirimleri Aç",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
fun LanguageSettingsCard(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "tr" to "Türkçe",
        "en" to "English",
        "ar" to "العربية"
    )

    val currentLanguage = languages.find { it.first == selectedLanguage }?.second ?: "Türkçe"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gold),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gold)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = UiText.stringResource(R.string.language_selection).asString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DropDown Menu
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = White,
                        contentColor = NavyBlue
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentLanguage,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(White)
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name,style = MaterialTheme.typography.titleMedium,color = NavyBlue) },
                            onClick = {
                                onLanguageChange(code)
                                expanded = false
                            },
                            leadingIcon = {
                                if (code == selectedLanguage) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Gold
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateIntervalSettingsCard(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {

    val updateOptions = listOf(
        15 to UiText.stringResource(R.string.time_15_seconds).asString(),
        30 to UiText.stringResource(R.string.time_30_seconds).asString(),
        60 to UiText.stringResource(R.string.time_1_minutes).asString(),
        120 to UiText.stringResource(R.string.time_2_minutes).asString(),
        300 to UiText.stringResource(R.string.time_5_minutes).asString(),
        600 to UiText.stringResource(R.string.time_10_minutes).asString(),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = White
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = UiText.stringResource(R.string.data_update_frequency).asString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = UiText.stringResource(R.string.choose_data_update_fruquency).asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.background
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Button Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    updateOptions.forEachIndexed { index, (seconds, label) ->
                        val isSelected = currentInterval == seconds
                        // Buton
                        Column(
                            modifier = Modifier
                                .wrapContentSize()
                                .clickable { onIntervalChange(seconds) }
                                .background(
                                    if (isSelected) Gold else Color.Transparent
                                )
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) White else Color.DarkGray,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )

                            if (index == 3 && seconds == 120) { // Normal için etiket göster
                                Text(
                                    text = UiText.stringResource(R.string.normal).asString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) White.copy(alpha = 0.8f) else Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Son buton hariç dikey ayraç çiz
                        if (index < updateOptions.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.LightGray.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoCard(appVersion: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gold),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gold)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = UiText.stringResource(R.string.app_information).asString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = UiText.stringResource(R.string.what_is_the_app_name).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surface
                )

                Text(
                    text = UiText.stringResource(R.string.app_name).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = UiText.stringResource(R.string.app_version).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surface
                )

                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}