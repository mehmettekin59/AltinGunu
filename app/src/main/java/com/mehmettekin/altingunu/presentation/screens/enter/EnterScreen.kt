package com.mehmettekin.altingunu.presentation.screens.enter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.ExchangeRate
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.utils.ValueFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterScreen(
    navController: NavController,
    viewModel: KapaliCarsiViewModel = hiltViewModel()
) {
    val exchangeRatesState by viewModel.exchangeRates.collectAsStateWithLifecycle()

    var selectedItemType by remember { mutableStateOf(ItemType.GOLD) }

    val goldCodeToName = remember { Constraints.goldCodeToName }
    val currencyCodeToName = remember { Constraints.currencyCodeToName }
    val goldCodeList = remember { Constraints.goldCodeList }
    val currencyCodeList = remember { Constraints.currencyCodeList }

    val codeToNameMap = remember { goldCodeToName + currencyCodeToName }

    // Filtrelenmiş verileri bir derivedStateOf ile daha verimli hale getiriyoruz
    val filteredRates by remember(exchangeRatesState, selectedItemType) {
        derivedStateOf {
            when (val state = exchangeRatesState) {
                is ResultState.Success -> {
                    val data = state.data
                    when (selectedItemType) {
                        ItemType.GOLD -> data.filter { it.code in goldCodeList.toSet() }
                        ItemType.CURRENCY -> data.filter { it.code in currencyCodeList.toSet() }
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CommonTopAppBar(
                title = UiText.stringResource(R.string.gold_and_currency).asString(),
                navController = navController,
                actions = {
                    IconButton(onClick = { viewModel.refreshExchangeRates() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = UiText.stringResource(R.string.refresh).asString(),
                            tint = White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item type selector
            ItemTypeSelector(
                selectedItemType = selectedItemType,
                onItemTypeSelect = { selectedItemType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    // Rate section
                    if (filteredRates.isNotEmpty()) {
                        RatesSectionTitle(
                            title = if (selectedItemType == ItemType.GOLD) UiText.stringResource(R.string.gold_price).asString()
                            else UiText.stringResource(R.string.currencies).asString(),
                            icon = if (selectedItemType == ItemType.GOLD)
                                painterResource(id = R.drawable.gold_bar)
                            else
                                painterResource(id = R.drawable.dollar),
                            iconTint = if (selectedItemType == ItemType.GOLD) Gold else NavyBlue
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            CoverFlowCarousel(
                                items = filteredRates,
                                initialPageIndex = minOf(3, filteredRates.size - 1),
                                itemWidth = 200.dp,
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
                                    backgroundColor = if (selectedItemType == ItemType.GOLD) Gold else NavyBlue,
                                    textColor = White,
                                    modifier = modifier,
                                    elevation = elevation
                                )
                            }
                        }
                    } else {
                        val dataType = if (selectedItemType == ItemType.GOLD) {
                            UiText.stringResource(R.string.gold)
                        } else {
                            UiText.stringResource(R.string.currency)
                        }
                        Text(
                            text = UiText.stringResource(R.string.data_not_found, dataType.asString()).asString(),
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
            Spacer(modifier = Modifier.height(16.dp))
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
            ItemType.GOLD to "Altın (🥇, 🪙, 💰)"
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
            contentDescription = UiText.stringResource(R.string.error).asString(),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (!message.isNullOrBlank()) {
                UiText.stringResource(R.string.data_load_failed, message)
            } else {
                UiText.stringResource(R.string.data_load_failed_retry)
            }.asString(),
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
            Text(UiText.stringResource(R.string.retry).asString())
        }
    }
}

@Composable
fun RatesSectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
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
    }
}
@OptIn(ExperimentalFoundationApi::class)
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
    val itemType = if (Constraints.goldCodeList.contains(rate.code)) {
        ItemType.GOLD
    } else {
        ItemType.CURRENCY
    }

    Card(
        modifier = modifier,
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

            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = textColor,
                fontSize = 18.sp
            )

            // Last updated timestamp (if available)
            rate.tarih.let {
                Text(
                    text = UiText.stringResource(R.string.last_update).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = textColor.copy(alpha = 0.2f)
            )


            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumn(label = UiText.stringResource(R.string.buy).asString(), value = rate.alis, textColor = textColor, itemType = itemType, specificItem = rate.code)
                InfoColumn(label = UiText.stringResource(R.string.sell).asString(), value = rate.satis, textColor = textColor, itemType = itemType, specificItem = rate.code)
            }
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    itemType: ItemType = ItemType.CURRENCY,
    specificItem: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val formattedValue = remember(value, itemType, specificItem) {
        ValueFormatter.formatWithSymbol(value, itemType, specificItem)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            text = "$formattedValue TL",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

// Orijinal CoverFlowCarousel implementasyonu
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
        val rotationY =
            lerp(start = maxRotationY, stop = 0f, fraction = 1f - absOffset) * -pageOffset.coerceIn(
                -1f,
                 1f
            )
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
    return Dp(start.value + (stop.value - start.value) * fraction
    )
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
                text = UiText.stringResource(R.string.gold_day).asString(),
                style = MaterialTheme.typography.titleLarge,
                color = White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = UiText.stringResource(R.string.set_up_raffle_for_thegold_day).asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    contentColor = White
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
                    text = UiText.stringResource(R.string.enter_the_participants).asString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
