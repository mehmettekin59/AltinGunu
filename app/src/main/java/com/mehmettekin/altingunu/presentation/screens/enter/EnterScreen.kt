package com.mehmettekin.altingunu.presentation.screens.enter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
import com.mehmettekin.altingunu.utils.convertNumerals
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
    val showDataSourceDialog by viewModel.showDataSourceDialogState.collectAsStateWithLifecycle()

    // Ekran yapılandırmasını al
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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

    if (showDataSourceDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDataSourceDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = UiText.stringResource(R.string.data_source_info).asString(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Text(
                    text = UiText.stringResource(R.string.api_info).asString(),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissDataSourceDialog() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(UiText.stringResource(R.string.ok).asString())
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        )
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
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Item type selector
            ItemTypeSelector(
                selectedItemType = selectedItemType,
                onItemTypeSelect = { selectedItemType = it }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Display content based on loading state
            when (exchangeRatesState) {
                is ResultState.Loading -> {
                    LoadingIndicator()
                }

                is ResultState.Error -> {
                    val errorState = exchangeRatesState as ResultState.Error
                    ErrorState(
                        message = errorState.message.asString(),
                        onRetry = { viewModel.refreshExchangeRates() }
                    )
                }

                is ResultState.Success, is ResultState.Idle -> {
                    // Rate section
                    if (filteredRates.isNotEmpty()) {
                        RatesSectionTitle(
                            title = if (selectedItemType == ItemType.GOLD) UiText.stringResource(R.string.gold_price)
                                .asString()
                            else UiText.stringResource(R.string.currencies).asString(),
                            titleColor = if (selectedItemType == ItemType.GOLD) MaterialTheme.colorScheme.primary else {if (isSystemInDarkTheme()) White else MaterialTheme.colorScheme.secondary} ,
                            icon = if (selectedItemType == ItemType.GOLD)
                                painterResource(id = R.drawable.gold_bar)
                            else
                                painterResource(id = R.drawable.dollar),
                            iconTint = if (selectedItemType == ItemType.GOLD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 210.dp else 200.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            // Aynı karüseli kullanıyoruz ancak landscape için farklı açı değeri
                            CoverFlowCarousel(
                                items = filteredRates,
                                initialPageIndex = minOf(3, filteredRates.size - 1),
                                itemWidth = if (isLandscape) 210.dp else 175.dp,
                                itemHeight = if (isLandscape) 220.dp else 190.dp,
                                minScale = 0.7f,
                                centerScale = 1.05f,
                                maxRotationY = if (isLandscape) 45f else 45f, // Landscape için daha az açı
                                minAlpha = 0.7f,
                                maxElevation = 0.dp,
                                minElevation = 0.dp,
                                pageSpacing = if (isLandscape) (-10).dp else (-20).dp // Landscape için daha az boşluk
                            ) { rate, modifier, elevation ->
                                AnimatedRateCard(
                                    rate = rate,
                                    codeToNameMap = codeToNameMap,
                                    backgroundColor =  if (selectedItemType == ItemType.GOLD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    textColor =  MaterialTheme.colorScheme.tertiary,
                                    modifier = modifier,
                                    elevation = elevation,
                                    isLandscape = isLandscape
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
                            text = UiText.stringResource(
                                R.string.data_not_found,
                                dataType.asString()
                            ).asString(),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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
            ItemType.CURRENCY to ItemType.CURRENCY.displayName,
            ItemType.GOLD to ItemType.GOLD.displayName
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
                    text = label.asString(),
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
        selected && itemType == ItemType.GOLD -> MaterialTheme.colorScheme.primary
        selected && itemType == ItemType.CURRENCY ->MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    val textColor = if (selected) White else Color.Gray
    val borderColor = when {
        selected && itemType == ItemType.GOLD -> MaterialTheme.colorScheme.primary
        selected && itemType == ItemType.CURRENCY -> MaterialTheme.colorScheme.secondary
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
                style = MaterialTheme.typography.bodyLarge
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
    titleColor: Color = MaterialTheme.colorScheme.secondary,
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
                color = titleColor
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
    textColor: Color = MaterialTheme.colorScheme.tertiary,
    modifier: Modifier = Modifier,
    elevation: Dp = 12.dp,
    isLandscape: Boolean = false
) {
    val context = LocalContext.current
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
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 6.dp)
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = textColor,
                fontSize = if (isLandscape) 18.sp else 16.sp
            )

            // Last updated timestamp (if available)
            rate.tarih.let { tarih ->
                val localizedDate = tarih.convertNumerals(context)
                Text(
                    text = UiText.stringResource(R.string.last_update).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = if (isLandscape) 14.sp else 12.sp
                )
                Text(
                    text = localizedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = if (isLandscape) 14.sp else 12.sp
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = if (isLandscape) 4.dp else 2.dp),
                thickness = 1.dp,
                color = textColor.copy(alpha = 0.2f)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumn(
                    label = UiText.stringResource(R.string.buy).asString(),
                    value = rate.alis,
                    textColor = textColor,
                    itemType = itemType,
                    specificItem = rate.code,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoColumn(
                    label = UiText.stringResource(R.string.sell).asString(),
                    value = rate.satis,
                    textColor = textColor,
                    itemType = itemType,
                    specificItem = rate.code,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
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
    textColor: Color = MaterialTheme.colorScheme.tertiary,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
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
            fontSize = fontSize
        )
        Text(
            text = UiText.stringResource(R.string.currency_value, formattedValue).asString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = fontSize,
            maxLines = 1
        )
    }
}

@Composable
fun GoldDayLotteryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(width = 0.5.dp, color = Color.Gray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color.Transparent else MaterialTheme.colorScheme.tertiary)
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
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = UiText.stringResource(R.string.set_up_raffle_for_thegold_day).asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    contentColor = White
                ),
                border = BorderStroke(width = 0.5.dp, color = Gold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    tint = White,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = UiText.stringResource(R.string.enter_the_participants).asString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


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
    cameraDistance: Dp = 4.dp,
    pageSpacing: Dp = 1.dp, // Değiştirildi: Pozitif değer ile kartlar arası mesafeyi belirgin hale getirdik
    itemContent: @Composable (item: T, modifier: Modifier, elevation: Dp) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

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

    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        pageSpacing = pageSpacing, // Artık pozitif değer kullanıyoruz
        beyondViewportPageCount = 3, // Show more items for a smooth effect
        flingBehavior = flingBehavior
    ) { page ->
        // Calculate transformation values based on page offset
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

        // Düzeltildi: Daha dengeli mesafeler için offset ölçeklendirmesi
        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 2f)

        // offsetSign determines if we're to the left (-1) or right (1) of the center
        val offsetSign = if (pageOffset > 0) 1f else -1f

        // Scale değeri - uzaktaki kartlar için daha fazla küçültme
        val scale = when {
            absOffset < 1f -> lerp(
                start = minScale * 0.95f,
                stop = centerScale,
                fraction = (1f - absOffset)
            )

            else -> lerp(
                start = minScale * 0.55f,  // Uzaktaki kartlar daha küçük
                stop = minScale * 0.95f,
                fraction = (2f - absOffset)
            )
        }

        // Rotasyon değerini mesafeye göre ayarladık
        val rotationDirection = if (isRtl) -1f else 1f
        val rotationY = when {
            absOffset < 1f -> lerp(
                start = maxRotationY,
                stop = 0f,
                fraction = (1f - absOffset)
            ) * rotationDirection * -offsetSign

            else -> {
                // Uzaktaki kartların açısını daha az artır
                val angle = maxRotationY + (absOffset - 1f) * 5f  // Uzak kartlar için açı artışı daha düşük
                angle.coerceAtMost(maxRotationY + 10f) * rotationDirection * -offsetSign
            }
        }

        // Alfa değeri - uzak kartlarda daha düşük alpha
        val alpha = when {
            absOffset < 1f -> lerp(
                start = minAlpha,
                stop = 1f,
                fraction = 1f - absOffset
            )

            else -> lerp(
                start = minAlpha * 0.8f,  // Uzak kartlar daha saydamlaşıyor
                stop = minAlpha,
                fraction = 2f - absOffset
            )
        }

        // Elevation effect - center items popping out more
        val elevation = lerp(
            start = minElevation,
            stop = maxElevation,
            fraction = 1f - absOffset.coerceIn(0f, 1f)
        )

        // Perspektif mesafesini ayarla
        val adjustedCameraDistance =
            (if (isLandscape) cameraDistance.value * 2f else cameraDistance.value * 1.8f) * density.density

        // Uzak kartların ekstra yana kayması için offset değeri - daha güçlü etki
        val horizontalOffset = when {
            absOffset < 1f -> 0f  // Merkeze yakın kartlar normal pozisyonda
            else -> (absOffset - 1f) * 10f * offsetSign  // Uzak kartlar için daha fazla yatay uzaklaşma
        }

        // Item content with transformations
        Box(
            modifier = Modifier.graphicsLayer {
                // Bu bileşenin çizim sırası için Z sıralaması uyguluyoruz (z-ordering)
                // Uzak kartlar daha önce çizilir (önce çizilen arkada kalır)
                // Bu API hatası olmadan çalışmalı
                translationY = absOffset * 0.01f  // Çok küçük bir değer, sadece çizim sırası için
            }
        ) {
            itemContent(
                items[page],
                Modifier
                    .width(itemWidth)
                    .height(itemHeight)
                    .graphicsLayer {
                        this.cameraDistance = adjustedCameraDistance
                        transformOrigin = TransformOrigin(0.5f, 0.5f)

                        // Scale kartlara uygula
                        this.scaleX = scale
                        this.scaleY = scale

                        // Rotasyon uygula
                        this.rotationY = rotationY

                        // Alfa uygula
                        this.alpha = alpha

                        // Uzak kartlar için ekstra yatay offseti uygula
                        translationX = horizontalOffset.dp.toPx()
                    },
                elevation
            )
        }
    }
}

// Linear interpolation helper
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(
        start.value + (stop.value - start.value) * fraction
    )
}














