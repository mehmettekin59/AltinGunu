package com.mehmettekin.altingunu.presentation.screens.enter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.domain.model.ExchangeRate
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.formatDecimalValue
import java.text.DecimalFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterScreen(
    navController: NavController,
    viewModel: KapaliCarsiViewModel = hiltViewModel()
) {
    val exchangeRatesState by viewModel.exchangeRates.collectAsStateWithLifecycle()

    // Gösterilecek veri türü için state (Altın / Döviz)
    var selectedItemType by remember { mutableStateOf(ItemType.GOLD) }

    val goldCodeToName = remember { Constraints.goldCodeToName }
    val currencyCodeToName = remember { Constraints.currencyCodeToName }
    val goldCodeList = remember { Constraints.goldCodeList }
    val currencyCodeList = remember { Constraints.currencyCodeList }

    // RateCard içinde isim bulmak için birleşik harita
    val codeToNameMap = remember { goldCodeToName + currencyCodeToName }

    // ViewModel'den gelen veriyi filtreleyerek altın ve döviz listelerini oluştur
    val (goldRates, currencyRates) = remember(exchangeRatesState) {
        when (val state = exchangeRatesState) {
            is ResultState.Success -> {
                val data = state.data
                val gold = data.filter { it.code in goldCodeList.toSet() }
                val currency = data.filter { it.code in currencyCodeList.toSet() }
                Pair(gold, currency)
            }
            else -> Pair(emptyList(), emptyList())
        }
    }

    // Scaffold: Ekranın temel yapısını sağlar
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Altın ve Döviz Kurları") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshExchangeRates() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Zaten ana sayfadayız */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
                    label = { Text("Ana Sayfa") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Participants.route) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Katılımcılar") },
                    label = { Text("Katılımcılar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Wheel.route) },
                    icon = { Icon(Icons.Default.Casino, contentDescription = "Çark") },
                    label = { Text("Çark") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Results.route) },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Sonuçlar") },
                    label = { Text("Sonuçlar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Settings.route) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
                    label = { Text("Ayarlar") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Döviz / Altın seçici
            ItemTypeSelector(
                selectedItemType = selectedItemType,
                onItemTypeSelect = { selectedItemType = it }
            )

            // Veri durumuna göre içeriği göster
            val currentState = exchangeRatesState
            when (currentState) {
                is ResultState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ResultState.Error -> {
                    TheErrorState(
                        message = currentState.message.toString(),
                        onRetry = { viewModel.refreshExchangeRates() }
                    )
                }
                is ResultState.Success, is ResultState.Idle -> {
                    when (selectedItemType) {
                        ItemType.GOLD -> RateRowSection(
                            title = "Altın Fiyatları",
                            icon = Icons.Filled.Diamond,
                            rates = goldRates,
                            codeToNameMap = codeToNameMap,
                            backgroundColor = Gold,
                            textColor = White
                        )
                        ItemType.CURRENCY -> RateRowSection(
                            title = "Döviz Kurları",
                            icon = Icons.Filled.MonetizationOn,
                            rates = currencyRates,
                            codeToNameMap = codeToNameMap,
                            backgroundColor = NavyBlue,
                            textColor = White
                        )
                        ItemType.TL -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Altın Günü Çekilişi Buton
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Gold
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Altın Günü Çekilişi",
                        style = MaterialTheme.typography.titleLarge,
                        color = White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Altın günü için çekiliş düzenleyin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { navController.navigate(Screen.Participants.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = White,
                            contentColor = Gold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Katılımcıları Gir",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

// İtem seçici bileşeni
@Composable
private fun ItemTypeSelector(
    selectedItemType: ItemType,
    onItemTypeSelect: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemTypes = remember {
        listOf(
            ItemType.CURRENCY to "Döviz ($, €, £)",
            ItemType.GOLD to "Altın (\uD83E\uDD47, \uD83E\uDE99, \uD83D\uDCB0)"
        )
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemTypes.forEach { (type, label) ->
                SelectableChip(
                    text = label,
                    selected = type == selectedItemType,
                    onClick = { onItemTypeSelect(type) },
                    modifier = Modifier.weight(1f),
                    itemType = type
                )
            }
        }
    }
}

// Hata durumu bileşeni
@Composable
private fun TheErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Hata",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Veri yüklenemedi${if (!message.isNullOrBlank()) ": $message" else ". Tekrar deneyin."}",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Tekrar Dene")
        }
    }
}

@Composable
fun RateRowSection(
    title: String,
    icon: ImageVector,
    rates: List<ExchangeRate>,
    codeToNameMap: Map<String, String>,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section header with title and icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (rates.isEmpty()) {
                Text(
                    text = "Veri bulunamadı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rates.forEach { rate ->
                        RateCard(
                            rate = rate,
                            name = codeToNameMap[rate.code] ?: rate.code,
                            textColor = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RateCard(
    rate: ExchangeRate,
    name: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val decimalFormat = remember { DecimalFormat("#,##0.00") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = textColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency/Gold name
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Price columns
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Buy price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Alış",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDecimalValue(rate.alis, decimalFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }

                // Sell price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Satış",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDecimalValue(rate.satis, decimalFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemType: ItemType
) {
    val backgroundColor = when {
        selected && itemType == ItemType.GOLD -> Gold
        selected && itemType == ItemType.CURRENCY -> NavyBlue
        else -> Color.Transparent
    }

    val textColor = if (selected) White else Color.Gray
    val borderColor = when {
        selected && itemType == ItemType.GOLD -> Gold
        selected && itemType == ItemType.CURRENCY -> NavyBlue
        else -> Color.Gray.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}