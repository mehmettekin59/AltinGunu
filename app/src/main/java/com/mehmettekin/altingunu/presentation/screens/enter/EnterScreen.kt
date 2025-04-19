package com.mehmettekin.altingunu.presentation.screens.enter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.absoluteValue
import com.mehmettekin.altingunu.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterScreen(
    navController: NavController,
    viewModel: KapaliCarsiViewModel = hiltViewModel()
) {
    // State collection
    val exchangeRatesState by viewModel.exchangeRates.collectAsStateWithLifecycle()

    // UI state
    var selectedItemType by remember { mutableStateOf(ItemType.GOLD) }

    // Memorized constant data
    val goldCodeToName = remember { Constraints.goldCodeToName }
    val currencyCodeToName = remember { Constraints.currencyCodeToName }
    val goldCodeList = remember { Constraints.goldCodeList }
    val currencyCodeList = remember { Constraints.currencyCodeList }

    // Combined code-to-name map for lookup
    val codeToNameMap = remember { goldCodeToName + currencyCodeToName }

    // Filter exchange rates by type
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

    // Main layout scaffold
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Altın ve Döviz Kurları",fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Gold,
                    titleContentColor = White,
                    actionIconContentColor = Gold
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item type selector (Gold/Currency)
                ItemTypeSelector(
                    selectedItemType = selectedItemType,
                    onItemTypeSelect = { selectedItemType = it }
                )

                // Display content based on loading state
                when (exchangeRatesState) {
                    is ResultState.Loading -> {
                        LoadingIndicator()
                    }
                    is ResultState.Error -> {
                        val errorState = exchangeRatesState as ResultState.Error
                        ErrorState(
                            message = errorState.message.toString(),
                            onRetry = { viewModel.refreshExchangeRates() }
                        )
                    }
                    is ResultState.Success, is ResultState.Idle -> {
                        // Display content based on selected type
                        when (selectedItemType) {
                            ItemType.GOLD -> RatesSection(
                                title = "Altın Fiyatları",
                                icon = painterResource(id = R.drawable.gold_bar),
                                rates = goldRates,
                                codeToNameMap = codeToNameMap,
                                backgroundColor = Gold,
                                textColor = White,
                                iconTint = Gold
                            )
                            ItemType.CURRENCY -> RatesSection(
                                title = "Döviz Kurları",
                                icon = painterResource(id = R.drawable.dollar),
                                rates = currencyRates,
                                codeToNameMap = codeToNameMap,
                                backgroundColor = NavyBlue,
                                textColor = White,
                                iconTint = NavyBlue
                            )
                            ItemType.TL -> { /* TL case not implemented in original */ }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Gold Day Lottery Card
                GoldDayLotteryCard(
                    onClick = { navController.navigate(Screen.Participants.route) }
                )
            }

    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

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

@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemType: ItemType = ItemType.GOLD
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

@Composable
private fun ErrorState(
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
fun RatesSection(
    title: String,
    icon: Painter,
    rates: List<ExchangeRate>,
    codeToNameMap: Map<String, String>,
    backgroundColor: Color,
    textColor: Color,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = iconTint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = NavyBlue
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // CoverFlowCarousel of Rate Cards
        if (rates.isNotEmpty()) {
            // Box to contain the carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)  // Height appropriate for the cards
                    .padding(vertical = 8.dp)
            ) {
                CoverFlowCarousel(
                    items = rates,
                    initialPageIndex = 3,
                    itemWidth = 190.dp,
                    itemHeight = 210.dp,
                    minScale = 0.7f,
                    centerScale = 1.05f,
                    maxRotationY = 40f,
                    minAlpha = 0.7f,
                    maxElevation = 0.dp,
                    minElevation = 0.dp,
                    pageSpacing = (-20).dp
                ) { rate, modifier, elevation ->
                    AnimatedRateCard(
                        rate = rate,
                        codeToNameMap = codeToNameMap,
                        backgroundColor = backgroundColor,
                        textColor = textColor,
                        modifier = modifier,
                        elevation = elevation
                    )
                }
            }
        } else {
            // Show message when no rates are available
            Text(
                text = "$title için veri bulunamadı.",
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnimatedRateCard(
    rate: ExchangeRate,
    codeToNameMap: Map<String, String>,
    backgroundColor: Color = Gold,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    elevation: Dp = 12.dp
) {
    // Get item name from the map, or use code if not found
    val itemName = codeToNameMap[rate.code] ?: rate.code

    Card(
        modifier = modifier,  // Use modifier from CoverFlowCarousel
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textColor
        )
    ) {
        Column(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Currency/Gold name
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = textColor,
                fontSize = 18.sp
            )

            // Last updated timestamp (if available)
            rate.tarih.let {
                Text(
                    text = "Son Güncelleme",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = textColor.copy(alpha = 0.2f)
            )

            // Buy/Sell info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(label = "Alış", value = rate.alis, textColor = textColor)
                Spacer(Modifier.width(8.dp))
                InfoColumn(label = "Satış", value = rate.satis, textColor = textColor)
            }
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    // Create formatter with current locale
    val formatter = remember {
        val currentLocale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(currentLocale)
        DecimalFormat("#,##0.00", symbols)
    }

    // Format the value
    val formattedValue = remember(value, formatter) {
        formatDecimalValue(value, formatter)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.8f)
        )
        Text(
            text = "$formattedValue TL",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1
        )
    }
}

// CoverFlowCarousel implementation
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> CoverFlowCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    initialPageIndex: Int = 0,
    itemWidth: Dp = 220.dp,
    itemHeight: Dp = 220.dp,
    minScale: Float = 0.7f,
    centerScale: Float = 1.05f,
    maxRotationY: Float = 45f,
    minAlpha: Float = 0.6f,
    maxElevation: Dp = 12.dp,
    minElevation: Dp = 4.dp,
    cameraDistance: Dp = 12.dp,
    pageSpacing: Dp = (-50).dp,
    itemContent: @Composable (item: T, modifier: Modifier, elevation: Dp) -> Unit
) {
    // Make sure initial page is within bounds
    val safeInitialPage = remember(items.size, initialPageIndex) {
        if (items.isNotEmpty()) initialPageIndex.coerceIn(0, items.size - 1) else 0
    }

    val pagerState = rememberPagerState(initialPage = safeInitialPage) { items.size }
    val flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
    val density = LocalDensity.current

    // Calculate padding based on screen width
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalPadding = remember(screenWidth, itemWidth) {
        ((screenWidth - itemWidth) / 2).coerceAtLeast(0.dp)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        pageSpacing = pageSpacing,
        beyondViewportPageCount = 2,
        flingBehavior = flingBehavior
    ) { page ->
        // Calculate transformation values based on page offset
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

        // Interpolate values for animations
        val scale = lerp(start = minScale, stop = centerScale, fraction = 1f - absOffset)
        val rotationY = lerp(start = maxRotationY, stop = 0f, fraction = 1f - absOffset) * -pageOffset.coerceIn(-1f, 1f)
        val alpha = lerp(start = minAlpha, stop = 1f, fraction = 1f - absOffset)
        val elevation = lerp(
            start = minElevation,
            stop = maxElevation,
            fraction = 1f - absOffset
        )

        // Item content with transformations
        itemContent(
            items[page],
            Modifier
                .width(itemWidth)
                .height(itemHeight)
                .graphicsLayer {
                    this.cameraDistance = cameraDistance.value * density.density
                    this.scaleX = scale
                    this.scaleY = scale
                    this.rotationY = rotationY
                    this.alpha = alpha
                },
            elevation
        )
    }
}

// Linear interpolation helper
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(start.value + (stop.value - start.value) * fraction)
}

@Composable
fun GoldDayLotteryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                text = "Altın Günü",
                style = MaterialTheme.typography.titleLarge,
                color = White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Altın günü için çekiliş düzenleyin",
                style = MaterialTheme.typography.bodyMedium,
                color = White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Gold
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
