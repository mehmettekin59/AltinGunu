package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.flow.collectLatest


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

    // Initialize with group info on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeGroup(groupName, groupDescription)
    }

    // Navigation handling
    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest { navigation ->
            when (navigation) {
                is ParticipantMethodNavigation.ToParticipantsScreen -> {
                    navController.navigate(
                        Screen.Participants.createRoute(navigation.groupId)
                    ) {
                        popUpTo(Screen.DrawGroups.route) { inclusive = false }
                    }
                }
                is ParticipantMethodNavigation.ShareInviteLink -> {
                    // Share the invite link
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, navigation.inviteUrl)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Davet Linkini Paylaş"))
                }
            }
        }
    }

    // Error handling
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.clearError()
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Info Card
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
                        text = groupName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue
                    )

                    if (groupDescription.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = groupDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Title
            Text(
                text = "Katılımcıları nasıl eklemek istiyorsunuz?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NavyBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Method Selection Cards
            MethodCard(
                method = ParticipantMethod.MANUAL,
                title = "Manuel Ekle",
                description = "İsimleri tek tek girin. Bildirim gönderilemez.",
                icon = Icons.Default.PersonAdd,
                isSelected = state.selectedMethod == ParticipantMethod.MANUAL,
                onClick = { viewModel.selectMethod(ParticipantMethod.MANUAL) }
            )

            MethodCard(
                method = ParticipantMethod.INVITE,
                title = "Davet Kodu Gönder",
                description = "Link paylaşarak davet edin. Bildirim gönderilir.",
                icon = Icons.Default.Share,
                isSelected = state.selectedMethod == ParticipantMethod.INVITE,
                onClick = { viewModel.selectMethod(ParticipantMethod.INVITE) }
            )

            // Show manual participant entry if MANUAL method is selected
            if (state.selectedMethod == ParticipantMethod.MANUAL) {
                Spacer(modifier = Modifier.height(16.dp))

                // Participants Section for Manual Entry
                ManualParticipantsSection(
                    participants = state.manualParticipants,
                    onAddParticipant = { viewModel.addManualParticipant(it) },
                    onRemoveParticipant = { viewModel.removeManualParticipant(it) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Continue Button
            Button(
                onClick = {
                    if (state.selectedMethod == ParticipantMethod.MANUAL) {
                        viewModel.continueWithManualParticipants()
                    } else {
                        viewModel.createInviteLink()
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
                enabled = !state.isLoading && (
                        state.selectedMethod == ParticipantMethod.INVITE ||
                                (state.selectedMethod == ParticipantMethod.MANUAL && state.manualParticipants.size >= 2)
                        )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = White
                    )
                } else {
                    Icon(
                        imageVector = if (state.selectedMethod == ParticipantMethod.INVITE)
                            Icons.Default.Share else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.selectedMethod == ParticipantMethod.INVITE)
                            "Davet Linki Oluştur" else UiText.stringResource(R.string.continue_button).asString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun MethodCard(
    method: ParticipantMethod,
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NavyBlue else White
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Gold)
        } else {
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = if (isSelected) Gold else NavyBlue.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) NavyBlue else NavyBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) White else NavyBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) White.copy(alpha = 0.9f) else Color.Gray
                )
            }

            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ManualParticipantsSection(
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
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Participant entry field and add button
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(text = UiText.stringResource(R.string.participant_name).asString()) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                focusedLabelColor = Gold,
                cursorColor = Gold,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
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
                    },
                    enabled = name.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = UiText.stringResource(R.string.add_button_description).asString(),
                        tint = if (name.isNotBlank()) Gold else Color.Gray
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Participants list
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(width = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = participant.name,
                                fontWeight = FontWeight.Medium,
                                color = NavyBlue,
                            )

                            IconButton(
                                onClick = { onRemoveParticipant(participant) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = UiText.stringResource(R.string.delete).asString(),
                                    tint = Color.Red
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

