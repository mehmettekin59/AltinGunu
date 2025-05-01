package com.mehmettekin.altingunu.presentation.screens.participants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    navController: NavController,
    viewModel: ParticipantsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Navigation and error handling
    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest {
            navController.navigate(Screen.Wheel.route)
        }
    }

    LaunchedEffect(key1 = state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.onEvent(ParticipantsEvent.OnErrorDismiss)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CommonTopAppBar(
                title = UiText.stringResource(R.string.add_participant).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() }
            )
        }

    ) { paddingValues ->
        ParticipantsContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        // Show confirmation dialog
        if (state.isShowingConfirmDialog) {
            ConfirmationDialog(
                state = state,
                onConfirm = { viewModel.onEvent(ParticipantsEvent.OnConfirmDialogConfirm) },
                onDismiss = { viewModel.onEvent(ParticipantsEvent.OnConfirmDialogDismiss) }
            )
        }
    }
}

@Composable
fun ParticipantsContent(
    state: ParticipantsState,
    onEvent: (ParticipantsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Item type selection (TL, Currency, Gold)
        ItemTypeSelector(
            selectedItemType = state.selectedItemType,
            onItemTypeSelect = { onEvent(ParticipantsEvent.OnItemTypeSelect(it)) }
        )

       Spacer(modifier = Modifier.height(8.dp))

        // Specific currency or gold type selection when applicable
        if (state.selectedItemType == ItemType.CURRENCY || state.selectedItemType == ItemType.GOLD) {
            SpecificItemSelector(
                selectedItemType = state.selectedItemType,
                selectedSpecificItem = state.selectedSpecificItem,
                currencyOptions = state.currencyOptions,
                goldOptions = state.goldOptions,
                onSpecificItemSelect = { onEvent(ParticipantsEvent.OnSpecificItemSelect(it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Monthly amount
        ModernTextField(
            value = state.monthlyAmount,
            onValueChange = { onEvent(ParticipantsEvent.OnMonthlyAmountChange(it)) },
            label = when (state.selectedItemType) {
                ItemType.GOLD -> UiText.stringResource(R.string.number_of_gold).asString()
                else -> UiText.stringResource(R.string.monthly_amount).asString()
            },
            keyboardType = KeyboardType.Decimal,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Gold
                )
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Duration in months
        ModernTextField(
            value = state.durationMonths,
            onValueChange = { onEvent(ParticipantsEvent.OnDurationChange(it)) },
            label = stringResource(R.string.how_many_months_will_it_last),
            keyboardType = KeyboardType.Number,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Gold
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Starting month and year
        Text(
            text = UiText.stringResource(R.string.starting_date).asString(),
            style = MaterialTheme.typography.titleMedium,
            color = NavyBlue,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ModernDateSelector(
            selectedMonth = state.startMonth,
            selectedYear = state.startYear,
            onMonthSelected = { onEvent(ParticipantsEvent.OnStartMonthSelect(it)) },
            onYearSelected = { onEvent(ParticipantsEvent.OnStartYearSelect(it)) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Participants section
        ParticipantsSection(
            participants = state.participants,
            onAddParticipant = { onEvent(ParticipantsEvent.OnAddParticipant(it)) },
            onRemoveParticipant = { onEvent(ParticipantsEvent.OnRemoveParticipant(it)) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Continue button
        Button(
            onClick = { onEvent(ParticipantsEvent.OnContinueClick) },
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue,
                contentColor = White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                UiText.stringResource(R.string.continue_button).asString(),
                fontWeight = FontWeight.Bold,
                color = White
            )
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            focusedLabelColor = Gold,
            cursorColor = Gold
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun ItemTypeSelector(
    selectedItemType: ItemType,
    onItemTypeSelect: (ItemType) -> Unit
) {
    Column {
        Text(
            text = UiText.stringResource(R.string.type_of_value_to_be_collected).asString(),
            style = MaterialTheme.typography.titleMedium,
            color = NavyBlue,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ItemType.entries.forEach { itemType ->
                ItemSelectableChip(
                    text = itemType.displayName.asString(),
                    selected = itemType == selectedItemType,
                    onClick = { onItemTypeSelect(itemType) },
                    modifier = Modifier.weight(1f),
                    itemType = itemType  // ItemType parametresini geçtik
                )
            }
        }
    }
}

@Composable
fun ItemSelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemType: ItemType  // Yeni parametre ekledik
) {
    val shape = RoundedCornerShape(8.dp)

    // İtemType'a göre arka plan rengini belirleme
    val backgroundColor = when {
        selected && itemType == ItemType.GOLD -> Gold
        selected && itemType == ItemType.CURRENCY -> NavyBlue
        selected && itemType == ItemType.TL -> NavyBlue
        else -> Color.Transparent
    }

    // İtemType'a göre kenarlık rengini belirleme
    val borderColor = when {
        selected && itemType == ItemType.GOLD -> Gold
        selected && itemType == ItemType.CURRENCY -> NavyBlue
        selected && itemType == ItemType.TL -> NavyBlue
        else -> Color.Gray.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        color = backgroundColor,  // Yeni değişken kullanıldı
        border = BorderStroke(1.dp, borderColor),  // Yeni değişken kullanıldı
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) White else Color.Gray,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SpecificItemSelector(
    selectedItemType: ItemType,
    selectedSpecificItem: String,
    currencyOptions: List<String>,
    goldOptions: List<String>,
    onSpecificItemSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val title = if (selectedItemType == ItemType.CURRENCY) UiText.stringResource(R.string.type_of_currency).asString()
    else UiText.stringResource(R.string.type_of_gold).asString()

    val options = if (selectedItemType == ItemType.CURRENCY) {
        currencyOptions.map { Constraints.currencyCodeToName[it] ?: it }
    } else {
        goldOptions.map { Constraints.goldCodeToName[it] ?: it }
    }

    val selectedValue = if (selectedSpecificItem.isNotEmpty()) {
        if (selectedItemType == ItemType.CURRENCY) {
           Constraints.currencyCodeToName[selectedSpecificItem] ?: selectedSpecificItem
        } else {
            Constraints.goldCodeToName[selectedSpecificItem] ?: selectedSpecificItem
        }
    } else ""

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = NavyBlue,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true },
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue.ifEmpty { UiText.stringResource(R.string.select).asString() },
                    color = if (selectedValue.isEmpty()) Color.Gray.copy(alpha = 0.5f) else Color.DarkGray
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Gold
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
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        // Find the code that corresponds to this display name
                        val code = if (selectedItemType == ItemType.CURRENCY) {
                            currencyOptions[index]
                        } else {
                            goldOptions[index]
                        }
                        onSpecificItemSelect(code)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = NavyBlue
                    )
                )
            }
        }
    }
}

@Composable
fun ModernDateSelector(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit
) {
    var showMonthDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }

    val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, selectedMonth - 1)

    // Mevcut ay ve yılı al
    val currentCalendar = Calendar.getInstance()
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1  // 0-based to 1-based
    val currentYear = currentCalendar.get(Calendar.YEAR)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month selector
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showMonthDialog = true },
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthFormat.format(calendar.time),
                    color = Color.DarkGray
                )

                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Gold
                )
            }
        }

        // Year selector
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showYearDialog = true },
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedYear.toString(),
                    color = Color.DarkGray
                )

                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Gold
                )
            }
        }
    }

    // Month selection dialog
    if (showMonthDialog) {
        // Seçilebilecek ayları belirle
        val availableMonths = if (selectedYear == currentYear) {
            // Eğer seçili yıl mevcut yılsa, sadece mevcut ay ve sonraki aylar seçilebilir
            (currentMonth..12).toList()
        } else {
            // Eğer gelecek bir yılsa, tüm aylar seçilebilir
            (1..12).toList()
        }

        DateSelectorDialog(
            title = UiText.stringResource(R.string.select_month).asString(),
            options = availableMonths.map {
                calendar.set(Calendar.MONTH, it - 1)
                monthFormat.format(calendar.time)
            },
            selectedIndex = availableMonths.indexOf(selectedMonth),
            onOptionSelected = { index ->
                onMonthSelected(availableMonths[index])
                showMonthDialog = false
            },
            onDismiss = { showMonthDialog = false }
        )
    }

    // Year selection dialog
    if (showYearDialog) {
        val yearOptions = (currentYear..currentYear + 9).toList()  // Güncel yıl ve sonraki 9 yıl

        DateSelectorDialog(
            title = UiText.stringResource(R.string.select_year).asString(),
            options = yearOptions.map { it.toString() },
            selectedIndex = yearOptions.indexOf(selectedYear),
            onOptionSelected = { index ->
                val newYear = yearOptions[index]
                onYearSelected(newYear)

                // Eğer yıl değişip mevcut yıla eşitlenirse ve seçili ay geçmiş bir aysa, ayı güncel aya güncelle
                if (newYear == currentYear && selectedMonth < currentMonth) {
                    onMonthSelected(currentMonth)
                }

                showYearDialog = false
            },
            onDismiss = { showYearDialog = false }
        )
    }
}

@Composable
fun DateSelectorDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NavyBlue,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = White,
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(options.size) { index ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(index) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = options[index],
                                color = if (index == selectedIndex) Gold else Color.DarkGray,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )

                            if (index == selectedIndex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Gold
                                )
                            }
                        }

                        if (index < options.size - 1) {
                            HorizontalDivider(
                                color = Color.Gray.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Gold
                )
            ) {
                Text(UiText.stringResource(R.string.close).asString())
            }
        }
    )
}

@Composable
fun ParticipantsSection(
    participants: List<Participant>,
    onAddParticipant: (String) -> Unit,
    onRemoveParticipant: (Participant) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Column {
        Text(
            text = UiText.stringResource(R.string.participants_count_special, participants.size).asString(),
            style = MaterialTheme.typography.titleMedium,
            color = NavyBlue,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Participant entry field and add button
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(UiText.stringResource(R.string.participant_name).asString()) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                focusedLabelColor = Gold,
                cursorColor = Gold
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddParticipant(name)
                            name = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = UiText.stringResource(R.string.add_button_description).asString(),
                        tint = Gold
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Participants list
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.2f))
        ) {
            if (participants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        UiText.stringResource(R.string.no_participants_have_been_added_yet).asString(),
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn {
                    items(participants) { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = participant.name,
                                color = NavyBlue
                            )

                            IconButton(
                                onClick = { onRemoveParticipant(participant) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = UiText.stringResource(R.string.delete).asString(),
                                    tint = Gold
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color.Gray.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    state: ParticipantsState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =  UiText.stringResource(R.string.confirm_information).asString(),
                color = NavyBlue,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = White,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConfirmationItem(
                    label = UiText.stringResource(R.string.participants_count).asString(),
                    value = UiText.stringResource(R.string.participant_count_special,state.participants.size).asString()
                )
                val valueTypeAndItem = when(state.selectedItemType) {
                    ItemType.TL -> state.selectedItemType.displayName.asString()
                    ItemType.CURRENCY -> UiText.stringResource(R.string.currency_with_type, Constraints.currencyCodeToName[state.selectedSpecificItem] ?: state.selectedSpecificItem).asString()
                    ItemType.GOLD -> UiText.stringResource(R.string.gold_with_type, Constraints.goldCodeToName[state.selectedSpecificItem] ?: state.selectedSpecificItem).asString()
                }


                ConfirmationItem(
                    label = UiText.stringResource(R.string.type_of_value_to_be_collected).asString(),
                    value = valueTypeAndItem
                )

                ConfirmationItem(
                    label = UiText.stringResource(R.string.monthly_amount).asString(),
                    value = state.monthlyAmount
                )

                ConfirmationItem(
                    label = UiText.stringResource(R.string.duration).asString(),
                    value = UiText.stringResource(R.string.duration_months,state.durationMonths).asString()
                )

                // Başlangıç ayı ve yılı
                val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MONTH, state.startMonth - 1)
                val monthName = monthFormat.format(calendar.time)

                ConfirmationItem(
                    label = UiText.stringResource(R.string.starting_date).asString(),
                    value = "$monthName ${state.startYear}"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(UiText.stringResource(R.string.confirm).asString())
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NavyBlue
                ),
                border = BorderStroke(1.dp, NavyBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(UiText.stringResource(R.string.cancel).asString())
            }
        }
    )
}

@Composable
fun ConfirmationItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp)
        )

        Text(
            text = value,
            color = NavyBlue,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
