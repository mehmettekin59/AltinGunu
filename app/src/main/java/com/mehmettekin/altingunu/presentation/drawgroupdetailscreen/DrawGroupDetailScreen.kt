package com.mehmettekin.altingunu.presentation.drawgroupdetailscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.utils.ValueFormatter
import com.mehmettekin.altingunu.utils.convertNumerals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawGroupDetailScreen(
    navController: NavController,
    groupId: String,
    viewModel: DrawGroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(groupId) {
        viewModel.loadDrawGroup(groupId)
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = state.drawGroup?.name ?: UiText.stringResource(R.string.gold_day).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() },
                actions = {
                    state.drawGroup?.let { group ->
                        if (!group.isCompleted) {
                            IconButton(onClick = { viewModel.onInviteClick() }) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Davet et",
                                    tint = White
                                )
                            }
                        }

                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            }

            state.drawGroup != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Grup bilgileri
                    item {
                        GroupInfoCard(drawGroup = state.drawGroup)
                    }

                    // İlerleme durumu
                    item {
                        ProgressCard(drawGroup = state.drawGroup)
                    }

                    // Katılımcılar
                    item {
                        ParticipantsCard(
                            participants = state.drawGroup.participants,
                            activeTokenCount = state.drawGroup.getActiveParticipantCount()
                        )
                    }

                    // Bekleyen katılım talepleri
                    if (state.pendingRequests.isNotEmpty()) {
                        item {
                            PendingRequestsCard(
                                requests = state.pendingRequests,
                                onApprove = { requestId ->
                                    viewModel.approveRequest(requestId, true)
                                },
                                onReject = { requestId ->
                                    viewModel.approveRequest(requestId, false)
                                }
                            )
                        }
                    }

                    // Sonuçlar
                    if (state.drawGroup.results.isNotEmpty()) {
                        item {
                            ResultsCard(
                                results = state.drawGroup.results,
                                onShowFullResults = {
                                    navController.navigate(Screen.Results.createRoute(groupId))
                                }
                            )
                        }
                    }

                    // Aksiyonlar
                    item {
                        ActionsSection(
                            drawGroup = state.drawGroup,
                            onContinueDrawClick = {
                                navController.navigate(Screen.Wheel.createRoute(groupId))
                            },
                            onMarkCompleteClick = {
                                viewModel.markAsCompleted()
                            }
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Çekiliş bulunamadı",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupInfoCard(drawGroup: DrawGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Grup Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                label = "Durum",
                value = drawGroup.getStatusText(),
                valueColor = when {
                    drawGroup.isCompleted -> Color.Green
                    !drawGroup.isActive -> Color.Red
                    else -> Gold
                }
            )

            InfoRow(
                label = "Oluşturulma",
                value = formatDate(drawGroup.createdDate)
            )

            InfoRow(
                label = "Tür",
                value = when (drawGroup.settings.itemType) {
                    ItemType.TL -> "TL"
                    ItemType.CURRENCY -> {
                        val name = Constraints.currencyCodeToName[drawGroup.settings.specificItem]
                            ?: drawGroup.settings.specificItem
                        "Döviz ($name)"
                    }
                    ItemType.GOLD -> {
                        val name = Constraints.goldCodeToName[drawGroup.settings.specificItem]
                            ?: drawGroup.settings.specificItem
                        "Altın ($name)"
                    }
                }
            )

            InfoRow(
                label = "Aylık Tutar",
                value = ValueFormatter.formatWithSymbol(
                    drawGroup.settings.monthlyAmount.toString(),
                    drawGroup.settings.itemType,
                    drawGroup.settings.specificItem
                ).convertNumerals()
            )

            InfoRow(
                label = "Süre",
                value = "${drawGroup.settings.durationMonths}".convertNumerals()
            )
        }
    }
}

@Composable
private fun ProgressCard(drawGroup: DrawGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İlerleme Durumu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )

                Text(
                    text = "${drawGroup.getCompletionPercentage().toInt()}%".convertNumerals(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { drawGroup.getCompletionPercentage() / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Gold,
                trackColor = Color.Gray.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${drawGroup.currentPaymentIndex} / ${drawGroup.settings.durationMonths} ay tamamlandı".convertNumerals(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            drawGroup.getNextPaymentPerson()?.let { nextPerson ->
                drawGroup.getNextPaymentDate()?.let { (day, month, year) ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = NavyBlue
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sonraki Ödeme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = White
                            )

                            Text(
                                text = "$nextPerson - $day/$month/$year".convertNumerals(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantsCard(
    participants: List<Participant>,
    activeTokenCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Katılımcılar (${participants.size})".convertNumerals(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )

                if (activeTokenCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Green.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Green
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$activeTokenCount aktif".convertNumerals(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Green
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            participants.forEach { participant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (participant != participants.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRequestsCard(
    requests: List<InvitedParticipant>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, Gold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bekleyen Talepler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Gold
                ) {
                    Text(
                        text = requests.size.toString().convertNumerals(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            requests.forEach { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Gray.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = request.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatDate(request.joinedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { onApprove(request.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Onayla",
                                    tint = Color.Green
                                )
                            }

                            IconButton(
                                onClick = { onReject(request.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reddet",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsCard(
    results: List<DrawResult>,
    onShowFullResults: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sonuçlar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )

                TextButton(onClick = onShowFullResults) {
                    Text("Tümünü Gör")
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // İlk 3 sonucu göster
            results.take(3).forEachIndexed { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${result.participantName}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = result.amount,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
            }

            if (results.size > 3) {
                Text(
                    text = "... ve ${results.size - 3} kişi daha".convertNumerals(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionsSection(
    drawGroup: DrawGroup,
    onContinueDrawClick: () -> Unit,
    onMarkCompleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!drawGroup.isCompleted && drawGroup.results.isEmpty()) {
            Button(
                onClick = onContinueDrawClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çekilişe Başla")
            }
        } else if (!drawGroup.isCompleted && drawGroup.currentPaymentIndex < drawGroup.settings.durationMonths) {
            Button(
                onClick = onContinueDrawClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çekilişe Devam Et")
            }
        }

        if (!drawGroup.isCompleted && drawGroup.currentPaymentIndex >= drawGroup.settings.durationMonths - 1) {
            OutlinedButton(
                onClick = onMarkCompleteClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Green),
                border = BorderStroke(1.dp, Color.Green)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çekilişi Tamamla")
            }
        }

    }
}


@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}