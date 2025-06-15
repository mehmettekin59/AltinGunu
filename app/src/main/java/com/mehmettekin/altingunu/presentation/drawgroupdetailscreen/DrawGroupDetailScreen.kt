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
import androidx.compose.ui.text.style.TextAlign
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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mehmettekin.altingunu.utils.RTLHelper
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawGroupDetailScreen(
    navController: NavController,
    groupId: String,
    viewModel: DrawGroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val layoutDirection = if (RTLHelper.isRTL()) LayoutDirection.Rtl else LayoutDirection.Ltr

    LaunchedEffect(groupId) {
        viewModel.loadDrawGroup(groupId)
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = state.drawGroup?.name ?: UiText.stringResource(R.string.gold_day).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() },
                actions = {
                    state.drawGroup?.let { group ->
                        if (!group.isCompleted) {

                            IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Grubu Sil",
                                    tint = White
                                )
                            }

                            // ✅ Davet kodu varsa göster
                            state.inviteCode?.let { code ->
                                IconButton(onClick = {
                                    viewModel.generateInviteCode()
                                }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Davet Et",
                                        tint = White
                                    )
                                }
                            }
                        }

                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
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
                val drawGroup = state.drawGroup!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Grup bilgileri
                    item {
                        GroupInfoCard(drawGroup = drawGroup)
                    }

                    // İlerleme durumu
                    item {
                        ProgressCard(drawGroup = drawGroup)
                    }

                    // Katılımcılar
                    item {
                        ParticipantsCard(
                            participants = drawGroup.participants,
                            activeTokenCount = drawGroup.getActiveParticipantCount()
                        )
                    }
                    // Davet kodu kartı

                    if (!drawGroup.isCompleted) {
                        item {
                            InviteCodeSection(
                                inviteCode = state.inviteCode,
                                isLoading = state.isLoading,
                                onGenerateCode = { viewModel.generateInviteCode() },
                                onShareCode = { code ->
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Altın Günü çekilişimize katılın!\nhttps://altingunu.app/invite/$code")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Davet Linkini Paylaş"))
                                },
                                onCopyCode = { code ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Davet Kodu", code)
                                    clipboard.setPrimaryClip(clip)

                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Davet kodu kopyalandı")
                                    }
                                }
                            )
                        }
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
                    if (drawGroup.results.isNotEmpty()) {
                        item {
                            ResultsCard(
                                results = drawGroup.results,
                                onShowFullResults = {
                                    navController.navigate(Screen.Results.createRoute(groupId))
                                }
                            )
                        }
                    }


                    // Aksiyonlar
                    item {
                        ActionsSection(
                            drawGroup = drawGroup,
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


        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = {
                    Text(
                        text = "Grubu Sil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                },
                text = {
                    Text(
                        text = "\"${state.drawGroup?.name}\" grubunu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteGroup()
                            navController.navigateUp()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Sil", color = White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                        Text("İptal")
                    }
                }
            )
        }


    }
}
}


@Composable
private fun InviteCodeSection(
    inviteCode: String?,
    isLoading: Boolean,
    onGenerateCode: () -> Unit,
    onShareCode: (String) -> Unit,
    onCopyCode: (String) -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = NavyBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Davet Kodu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (inviteCode != null) {
                // Kod var, göster
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = White
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = inviteCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Bu kodu paylaşarak yeni katılımcılar davet edebilirsiniz",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { onShareCode(inviteCode) }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paylaş")
                    }

                    TextButton(
                        onClick = { onCopyCode(inviteCode) }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopyala")
                    }
                }
            } else {
                // Kod yok, oluşturma butonu göster
                Button(
                    onClick = onGenerateCode,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NavyBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Davet Kodu Oluştur",
                            color = NavyBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupInfoCard(drawGroup: DrawGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
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
                color = MaterialTheme.colorScheme.primary
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
                                text = request.joinedAt?.let { formatDate(it) } ?: "Tarih bilinmiyor",
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
            style = MaterialTheme.typography.bodyMedium.copy(
                textDirection = RTLHelper.getTextDirection()
            ),
            color = Color.Gray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDirection = RTLHelper.getTextDirection()
            ),
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = RTLHelper.getEndTextAlign()
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}