package com.mehmettekin.altingunu.presentation.drawgroupscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.utils.convertNumerals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawGroupsScreen(
    navController: NavController,
    viewModel: DrawGroupsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Error handling
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.onEvent(DrawGroupsEvent.OnErrorDismiss)
        }
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = UiText.stringResource(R.string.gold_day).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(DrawGroupsEvent.OnRefresh) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = UiText.stringResource(R.string.refresh).asString(),
                            tint = White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Gold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Tab Selector
                DrawGroupTabSelector(
                    selectedTab = state.selectedTab,
                    onTabSelected = { viewModel.onEvent(DrawGroupsEvent.OnTabChanged(it)) },
                    activeCount = state.activeGroups.size,
                    completedCount = state.completedGroups.size,
                    allCount = state.allGroups.size
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Groups List
                val filteredGroups = viewModel.getFilteredGroups()

                if (filteredGroups.isEmpty()) {
                    EmptyGroupsView(
                        selectedTab = state.selectedTab
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredGroups) { group ->
                            DrawGroupCard(
                                group = group,
                                onClick = {
                                    navController.navigate(Screen.DrawGroupDetail.createRoute(group.id))
                                },
                                onDelete = { viewModel.onEvent(DrawGroupsEvent.OnDeleteGroup(group)) }
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun DrawGroupTabSelector(
    selectedTab: DrawGroupTab,
    onTabSelected: (DrawGroupTab) -> Unit,
    activeCount: Int,
    completedCount: Int,
    allCount: Int,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        DrawGroupTab.ACTIVE to (UiText.stringResource(R.string.title_participants).asString() + " ($activeCount)"),
        DrawGroupTab.COMPLETED to (UiText.stringResource(R.string.raffle_completed).asString() + " ($completedCount)"),
        DrawGroupTab.ALL to ("Tümü ($allCount)")
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (tab, label) ->
                val isSelected = selectedTab == tab

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) },
                    color = if (isSelected) Gold else Color.Transparent,
                    border = if (isSelected) null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = label,
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        color = if (isSelected) NavyBlue else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawGroupCard(
    group: DrawGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else NavyBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (group.description.isNotEmpty()) {
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            group.isCompleted -> Color.Green.copy(alpha = 0.1f)
                            !group.isActive -> Color.Red.copy(alpha = 0.1f)
                            else -> Gold.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = group.getStatusText(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                group.isCompleted -> Color.Green
                                !group.isActive -> Color.Red
                                else -> Gold
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete Button
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = UiText.stringResource(R.string.delete).asString(),
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Group Summary
            Text(
                text = group.getSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress
            if (!group.isCompleted && group.results.isNotEmpty()) {
                val progress = group.getCompletionPercentage() / 100f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f),
                        color = Gold,
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${(progress * 100).toInt()}%".convertNumerals(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Next Payment Info
            group.getNextPaymentPerson()?.let { nextPerson ->
                group.getNextPaymentDate()?.let { (day, month, year) ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Gold.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Sonraki: $nextPerson - $day/$month/$year".convertNumerals(),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGroupsView(
    selectedTab: DrawGroupTab,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        val emptyMessage = when (selectedTab) {
            DrawGroupTab.ACTIVE -> "Henüz aktif çekiliş bulunmuyor"
            DrawGroupTab.COMPLETED -> "Henüz tamamlanmış çekiliş bulunmuyor"
            DrawGroupTab.ALL -> "Henüz hiç çekiliş oluşturulmamış"
        }

        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeleteGroupDialog(
    group: DrawGroup,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Çekilişi Sil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        },
        text = {
            Text(
                text = "\"${group.name}\" adlı çekilişi silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = UiText.stringResource(R.string.delete).asString(),
                    color = White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(UiText.stringResource(R.string.cancel).asString())
            }
        }
    )
}