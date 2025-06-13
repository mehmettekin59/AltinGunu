package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen



import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.InviteStatus
import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantMethodScreen(
    navController: NavController,
    groupName: String,
    groupDescription: String,
    viewModel: ParticipantMethodViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Initialize group info
    LaunchedEffect(Unit) {
        viewModel.updateGroupName(groupName)
        viewModel.updateGroupDescription(groupDescription)
    }

    // Error handling
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.clearError()
        }
    }


    LaunchedEffect(state.isGroupCreated) {
        if (state.isGroupCreated && state.inviteCode != null) {
            while (true) {
                viewModel.refreshInvitedParticipants()
                kotlinx.coroutines.delay(15000) // Her 15 saniyede bir yenile
            }
        }
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = UiText.stringResource(R.string.add_participant).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() }
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
            // Group Info Section - ✅ Grup oluşturulduktan sonra disabled olacak
            GroupInfoSection(
                groupName = state.groupName,
                groupDescription = state.groupDescription,
                onGroupNameChange = {
                    if (!state.isGroupCreated) viewModel.updateGroupName(it)
                },
                onGroupDescriptionChange = {
                    if (!state.isGroupCreated) viewModel.updateGroupDescription(it)
                },
                isEnabled = !state.isGroupCreated  // ✅ YENİ: Grup oluşturulduysa disabled
            )

            // ✅ Grup oluşturma butonu (sadece grup oluşturulmadıysa göster)
            if (!state.isGroupCreated) {
                Button(
                    onClick = { viewModel.createGroup() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = NavyBlue
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !state.isLoading && state.groupName.isNotBlank()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NavyBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grup Oluştur",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // ✅ Grup oluşturulduysa başarı mesajı göster
            if (state.isGroupCreated) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grup başarıyla oluşturuldu!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Title
            Text(
                text = "Katılımcı Ekleme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NavyBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // ✅ Davet kartı - Sadece grup oluşturulduysa göster
            if (state.isGroupCreated) {
                InvitedParticipantsCard(
                    inviteCode = state.inviteCode,
                    invitedParticipants = state.invitedParticipants,
                    onGenerateInvite = { viewModel.generateInviteCode() },
                    onShareInvite = {
                        viewModel.shareInviteLink { url ->
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Altın Günü çekilişimize katılın!\n$url")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Davet Linkini Paylaş"))
                        }
                    },
                    onCopyInvite = {
                        viewModel.copyInviteLink { url ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Davet Linki", url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link kopyalandı", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onApproveParticipant = { viewModel.approveInvitedParticipant(it) },
                    onRejectParticipant = { viewModel.rejectInvitedParticipant(it) }
                )

                // Total Participants Summary
                TotalParticipantsSummary(
                    invitedCount = state.invitedParticipants.count { it.status == InviteStatus.ACCEPTED },
                    totalCount = viewModel.getTotalParticipantCount()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ✅ Continue Button - Sadece grup oluşturuldu ve yeterli katılımcı varsa
            if (state.isGroupCreated) {
                Button(
                    onClick = {
                        viewModel.proceedWithParticipants { groupId ->
                            navController.navigate(Screen.Participants.createRoute(groupId)) {
                                popUpTo(Screen.ParticipantMethod.route) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyBlue,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !state.isLoading && viewModel.getTotalParticipantCount() >= 2
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = UiText.stringResource(R.string.continue_button).asString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

// ✅ GroupInfoSection'a isEnabled parametresi ekle
@Composable
fun GroupInfoSection(
    groupName: String,
    groupDescription: String,
    onGroupNameChange: (String) -> Unit,
    onGroupDescriptionChange: (String) -> Unit,
    isEnabled: Boolean = true  // ✅ YENİ parametre
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
            Text(
                text = "Grup Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text("Grup Adı *") },
                placeholder = { Text("Örn: Aile Altın Günü") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    focusedLabelColor = Gold,
                    cursorColor = Gold
                ),
                singleLine = true,
                enabled = isEnabled,  // ✅ Kontrol ekle
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = NavyBlue
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = groupDescription,
                onValueChange = onGroupDescriptionChange,
                label = { Text("Açıklama (Opsiyonel)") },
                placeholder = { Text("Grup hakkında kısa bilgi...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    focusedLabelColor = Gold,
                    cursorColor = Gold
                ),
                minLines = 2,
                maxLines = 3,
                enabled = isEnabled,  // ✅ Kontrol ekle
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = NavyBlue
                    )
                }
            )
        }
    }
}

@Composable
fun InvitedParticipantsCard(
    inviteCode: String?,
    invitedParticipants: List<InvitedParticipant>,
    onGenerateInvite: () -> Unit,
    onShareInvite: () -> Unit,
    onCopyInvite: () -> Unit,
    onApproveParticipant: (String) -> Unit,
    onRejectParticipant: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (inviteCode != null) BorderStroke(2.dp, Color.Green) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = NavyBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Davetli Katılımcılar (${invitedParticipants.count { it.status == InviteStatus.ACCEPTED }})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Davet linkiyle eklenen katılımcılara bildirim gönderilebilir",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Green
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (inviteCode == null) {
                Button(
                    onClick = onGenerateInvite,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Davet Linki Oluştur")
                }
            } else {
                // Invite code display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Green.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Davet Kodu: $inviteCode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = onShareInvite) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paylaş")
                            }

                            TextButton(onClick = onCopyInvite) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kopyala")
                            }
                        }
                    }
                }

                // Invited participants list
                if (invitedParticipants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Katılım Talepleri",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        LazyColumn {
                            items(invitedParticipants) { invited ->
                                InvitedParticipantItem(
                                    participant = invited,
                                    onApprove = { onApproveParticipant(invited.id) },
                                    onReject = { onRejectParticipant(invited.id) }
                                )

                                if (invited != invitedParticipants.last()) {
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
        }
    }
}

@Composable
fun InvitedParticipantItem(
    participant: InvitedParticipant,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = participant.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (participant.status) {
                        InviteStatus.PENDING -> Icons.Default.HourglassEmpty
                        InviteStatus.ACCEPTED -> Icons.Default.CheckCircle
                        InviteStatus.REJECTED -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when (participant.status) {
                        InviteStatus.PENDING -> Color.Gray
                        InviteStatus.ACCEPTED -> Color.Green
                        InviteStatus.REJECTED -> Color.Red
                    },
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = when (participant.status) {
                        InviteStatus.PENDING -> "Bekliyor"
                        InviteStatus.ACCEPTED -> "Kabul Edildi"
                        InviteStatus.REJECTED -> "Reddedildi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (participant.status) {
                        InviteStatus.PENDING -> Color.Gray
                        InviteStatus.ACCEPTED -> Color.Green
                        InviteStatus.REJECTED -> Color.Red
                    }
                )
            }
        }

        if (participant.status == InviteStatus.PENDING) {
            Row {
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Onayla",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reddet",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TotalParticipantsSummary(
    invitedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    text = "Toplam Katılımcı",
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = White.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Davetli",
                        style = MaterialTheme.typography.bodySmall,
                        color = White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = invitedCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }

            if (totalCount < 2) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Red.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "En az 2 katılımcı gereklidir",
                        style = MaterialTheme.typography.bodySmall,
                        color = White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}