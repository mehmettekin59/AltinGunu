package com.mehmettekin.altingunu.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ScrollBehavior için gerekli
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Uygulama versiyon bilgisini al
    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0" // Hata durumunda varsayılan değer
        }
    }


    // Handle error messages
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.onEvent(SettingsEvent.OnErrorDismiss)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CommonTopAppBar(
                title = "Ayarlar",
                navController = navController,
                isSettingsScreen = true,
                onBackPressed = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(SettingsEvent.OnDefaultsReset) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Varsayılan Ayarlar",
                            tint = White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language settings
            LanguageSettingsCard(
                selectedLanguage = state.selectedLanguage,
                onLanguageChange = { viewModel.onEvent(SettingsEvent.OnLanguageChange(it)) }
            )

            // API Update Interval Settings
            UpdateIntervalSettingsCard(
                currentInterval = state.apiUpdateInterval,
                onIntervalChange = { viewModel.onEvent(SettingsEvent.OnApiUpdateIntervalChange(it)) }
            )

            // App version info
            AppInfoCard(appVersion = appVersion.toString())

            // Reset to defaults button - Toolbar'a taşındığı için kaldırıldı
            Spacer(modifier = Modifier.height(16.dp))
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
        modifier = Modifier.fillMaxWidth().background(Gold),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth().background(Gold)
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
                    text = "Dil Seçimi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface
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
                            style = MaterialTheme.typography.bodyLarge
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
                            text = { Text(name) },
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
    // Önceden tanımlanmış yenileme aralığı seçenekleri
    val updateOptions = listOf(
        15 to "15 sn",
        30 to "30 sn",
        60 to "1 dk",
        120 to "2 dk",
        300 to "5 dk",
        600 to "10 dk"
    )

    Card(
        modifier = Modifier.fillMaxWidth().background(NavyBlue),
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
                    text = "Veri Güncelleme Sıklığı",
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Altın ve Döviz verilerinin güncellenme sıklığını seçin",
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    updateOptions.forEachIndexed { index, (seconds, label) ->
                        val isSelected = currentInterval == seconds
                        // Buton
                        Column(
                            modifier = Modifier
                                .weight(1f)
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

                            if (index == 2 && seconds == 60) { // Normal için etiket göster
                                Text(
                                    text = "(Normal)",
                                    fontSize = 10.sp,
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
        modifier = Modifier.fillMaxWidth().background(Gold),
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
                    text = "Uygulama Bilgileri",
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
                    text = "Uygulama Adı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surface
                )

                Text(
                    text = "Altın Günü",
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
                    text = "Versiyon",
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