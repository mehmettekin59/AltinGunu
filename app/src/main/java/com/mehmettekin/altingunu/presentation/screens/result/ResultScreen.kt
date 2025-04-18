package com.mehmettekin.altingunu.presentation.screens.result

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.dismissError()
        }
    }

    // Handle PDF sharing
    LaunchedEffect(state.pdfUri) {
        state.pdfUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Çekiliş Sonuçlarını Paylaş"))
            viewModel.clearPdfUri()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Çekiliş Sonuçları",
                        color = White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyBlue,
                    titleContentColor = White
                ),
                actions = {
                    // PDF export button
                    IconButton(onClick = {
                        viewModel.createPdf(context)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "PDF İndir",
                            tint = White
                        )
                    }

                    // Share results button
                    IconButton(onClick = {
                        viewModel.createPdf(context)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Sonuçları Paylaş",
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
        } else if (state.results.isEmpty()) {
            // No results found
            EmptyResultsView(
                onRestartClick = {
                    navController.navigate(Screen.Participants.route) {
                        popUpTo(Screen.Enter.route) { inclusive = false }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Display results
            ResultsContent(
                results = state.results,
                drawSettings = state.drawSettings,
                onRestartClick = {
                    viewModel.restart()
                    navController.navigate(Screen.Participants.route) {
                        popUpTo(Screen.Enter.route) { inclusive = false }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyResultsView(
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Henüz çekiliş sonucu bulunmuyor",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRestartClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Yeni Çekiliş Başlat")
        }
    }
}

@Composable
private fun ResultsContent(
    results: List<DrawResult>,
    drawSettings: ParticipantsScreenWholeInformation?,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Settings summary
        drawSettings?.let { settings ->
            ResultsSettingsSummary(settings = settings)

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Results header
        Text(
            text = "Çekiliş Sonuçları",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = NavyBlue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results table
        ResultsTable(results = results)

        Spacer(modifier = Modifier.height(24.dp))

        // Restart button
        Button(
            onClick = onRestartClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Yeni Çekiliş Başlat")
        }
    }
}

@Composable
private fun ResultsSettingsSummary(
    settings: ParticipantsScreenWholeInformation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Çekiliş Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Item Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Değer Türü:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                val itemTypeText = when (settings.itemType) {
                    ItemType.TL -> "TL"
                    ItemType.CURRENCY -> "Döviz (${settings.specificItem})"
                    ItemType.GOLD -> "Altın (${settings.specificItem})"
                }

                Text(
                    text = itemTypeText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyBlue
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Monthly Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Aylık Miktar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    text = "${settings.monthlyAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyBlue
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Toplam Süre:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    text = "${settings.durationMonths} ay",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyBlue
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Participant Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Katılımcı Sayısı:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    text = "${settings.participantCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyBlue
                )
            }
        }
    }
}

@Composable
private fun ResultsTable(
    results: List<DrawResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gold.copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Sıra",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.weight(0.1f)
                )

                Text(
                    text = "İsim",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.weight(0.4f)
                )

                Text(
                    text = "Ay",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.weight(0.3f)
                )

                Text(
                    text = "Miktar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.weight(0.2f)
                )
            }

            // Table Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                itemsIndexed(results) { index, result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            .background(
                                if (index % 2 == 0) Color.Transparent
                                else Color.LightGray.copy(alpha = 0.1f)
                            )
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(0.1f)
                        )

                        Text(
                            text = result.participantName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NavyBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(0.4f)
                        )

                        Text(
                            text = result.month,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(0.3f)
                        )

                        Text(
                            text = result.amount,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(0.2f)
                        )
                    }
                }
            }
        }
    }
}