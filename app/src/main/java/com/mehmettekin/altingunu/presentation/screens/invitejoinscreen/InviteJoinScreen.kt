package com.mehmettekin.altingunu.presentation.screens.invitejoinscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteJoinScreen(
    navController: NavController,
    inviteCode: String,
    viewModel: InviteJoinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(inviteCode) {
        viewModel.loadInvitation(inviteCode)
    }

    LaunchedEffect(state.joinSuccess) {
        if (state.joinSuccess) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Katılım talebiniz gönderildi!")
                navController.navigate(Screen.Enter.route) {
                    popUpTo(Screen.InviteJoin.route) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = UiText.stringResource(R.string.gold_day).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(color = Gold)
                }
                state.invitation != null -> {
                    InvitationContent(
                        invitation = state.invitation,
                        participantName = state.participantName,
                        onParticipantNameChange = viewModel::onParticipantNameChange,
                        onJoinClick = viewModel::onJoinClick,
                        isJoining = state.isJoining
                    )
                }
                else -> {
                    ErrorContent(
                        message = "Davet bulunamadı veya süresi dolmuş.",
                        onRetryClick = { viewModel.loadInvitation(inviteCode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationContent(
    invitation: DrawInvitation,
    participantName: String,
    onParticipantNameChange: (String) -> Unit,
    onJoinClick: () -> Unit,
    isJoining: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Gold
            )

            Text(
                text = "Altın Günü Davetiyesi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NavyBlue
            )

            Text(
                text = "${invitation.inviterName} sizi '${invitation.drawGroupName}' çekilişine davet ediyor",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            if (invitation.getRemainingDays() > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Gold.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Bu davet ${invitation.getRemainingDays()} gün içinde geçerli",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = participantName,
                onValueChange = onParticipantNameChange,
                label = { Text("Adınız") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                }
            )

            Button(
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = participantName.isNotBlank() && !isJoining,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue
                )
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Katılım Talebini Gönder")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tekrar Dene")
        }
    }
}