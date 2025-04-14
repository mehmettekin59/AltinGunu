package com.mehmettekin.altingunu.presentation.screens.weel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mehmettekin.altingunu.presentation.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Shadow
import kotlin.math.cos
import kotlin.math.sin

// Colors for the wheel
val colors = listOf(
    Color(0xFFFF6384), Color(0xFF36A2EB), Color(0xFFFFCE56), Color(0xFF4BC0C0),
    Color(0xFF9966FF), Color(0xFFFF9F40), Color(0xFF8AC926), Color(0xFF1982C4),
    Color(0xFF6A4C93), Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926),
    Color(0xFF1982C4), Color(0xFF6A4C93), Color(0xFFF15BB5), Color(0xFFFEE440),
    Color(0xFF00BBF9), Color(0xFF00F5D4), Color(0xFF9B5DE5), Color(0xFFF15BB5)
)

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(
    navController: NavController,
    viewModel: WheelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val textMeasurer = rememberTextMeasurer()
    val rotationAnimatable = remember { Animatable(viewModel.rotation) }

    // Determine layout based on screen width
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

    // Handle wheel spinning animation
    LaunchedEffect(viewModel.isSpinning) {
        if (viewModel.isSpinning) {
            val spinCount = 2 + (3 * Math.random()).toFloat()
            val targetRotation = rotationAnimatable.value + (spinCount * 360f)

            launch {
                rotationAnimatable.animateTo(
                    targetValue = targetRotation,
                    animationSpec = tween(
                        durationMillis = 4000,
                        easing = EaseOut
                    )
                ) {
                    viewModel.updateRotation(value)
                }

                delay(500L) // Let the wheel settle on the winner
                viewModel.finishSpin(rotationAnimatable.value)
            }
        }
    }

    // Handle last participant automatically
    LaunchedEffect(state.participants) {
        viewModel.handleLastParticipant()
    }

    // Show error message if there's an error
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
        }
    }

    // Navigate to results screen when results are saved
    LaunchedEffect(state.resultsSaved) {
        if (state.resultsSaved) {
            navController.navigate(Screen.Results.route) {
                popUpTo(Screen.Wheel.route) { inclusive = false }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Altın Günü Çekilişi",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Layout based on screen width
                if (isWideScreen) {
                    // Wide screen layout - Side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        WheelSection(
                            modifier = Modifier.weight(1f),
                            viewModel = viewModel,
                            textMeasurer = textMeasurer,
                            participants = state.participants
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        ParticipantsSection(
                            modifier = Modifier.weight(1f),
                            remainingParticipants = state.participants,
                            winners = viewModel.winners,
                            onSaveResults = { viewModel.saveResults() }
                        )
                    }
                } else {
                    // Narrow screen layout - Stacked
                    WheelSection(
                        modifier = Modifier.fillMaxWidth(),
                        viewModel = viewModel,
                        textMeasurer = textMeasurer,
                        participants = state.participants
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ParticipantsSection(
                        modifier = Modifier.fillMaxWidth(),
                        remainingParticipants = state.participants,
                        winners = viewModel.winners,
                        onSaveResults = { viewModel.saveResults() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun WheelSection(
    modifier: Modifier,
    viewModel: WheelViewModel,
    textMeasurer: TextMeasurer,
    participants: List<String>
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Çark",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(320.dp)
                    .padding(8.dp)
            ) {
                if (participants.isNotEmpty()) {
                    drawWheel(
                        participants = participants,
                        rotation = viewModel.rotation,
                        textMeasurer = textMeasurer
                    )
                    drawPointer()
                }
            }
        }

        ControlButtons(
            viewModel = viewModel,
            canSpin = participants.size > 1 && !viewModel.isSpinning
        )

        Spacer(modifier = Modifier.height(8.dp))

        WinnerAnnouncement(winner = viewModel.winner)
    }
}

@Composable
private fun ControlButtons(
    viewModel: WheelViewModel,
    canSpin: Boolean
) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { viewModel.spinWheel() },
            enabled = canSpin,
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Çarkı Çevir")
        }

        Button(
            onClick = { viewModel.reset() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray
            ),
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Yeniden Başlat")
        }
    }
}

@Composable
private fun WinnerAnnouncement(winner: String?) {
    winner?.let {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = " Kazanan:🎉✨ $it ✨🎉",
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParticipantsSection(
    modifier: Modifier,
    remainingParticipants: List<String>,
    winners: List<String>,
    onSaveResults: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ParticipantList(
                title = "Kalan Katılımcılar (${remainingParticipants.size})",
                participants = remainingParticipants,
                modifier = Modifier.weight(1f),
                showIndex = false
            )

            ParticipantList(
                title = "Kazananlar (${winners.size})",
                participants = winners,
                modifier = Modifier.weight(1f),
                showIndex = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Only show save button when all participants have been selected
        if (remainingParticipants.isEmpty() && winners.isNotEmpty()) {
            Button(
                onClick = onSaveResults,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    "Sonuçları Kaydet ve İlerle",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ParticipantList(
    title: String,
    participants: List<String>,
    modifier: Modifier,
    showIndex: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(200.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Divider(modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(participants) { index, participant ->
                    val displayText = if(showIndex){"${index + 1}. $participant"} else {participant}
                    Text(
                        text = displayText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        fontSize = 14.sp
                    )

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                }

                if (participants.isEmpty()) {
                    item {
                        Text(
                            text = if (title.startsWith("Kalan"))
                                "Tüm katılımcılar seçildi!"
                            else "Henüz kazanan yok",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawWheel(
    participants: List<String>,
    rotation: Float,
    textMeasurer: TextMeasurer
) {
    if (participants.isEmpty()) return

    val sliceAngle = 360f / participants.size
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 10.dp.toPx()

    // Draw wheel slices
    participants.forEachIndexed { index, participant ->
        val startAngle = index * sliceAngle - 90f + rotation
        val sweepAngle = sliceAngle

        // Draw the sector
        drawArc(
            color = colors[index % colors.size],
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2)
        )

        // Draw the text for this slice
        val middleAngle = startAngle + (sweepAngle / 2)
        val angleInRadians = Math.toRadians(middleAngle.toDouble())
        val textRadius = radius * 0.65f

        // Calculate position using trigonometry (polar to cartesian conversion)
        val textX = center.x + (textRadius * cos(angleInRadians)).toFloat()
        val textY = center.y + (textRadius * sin(angleInRadians)).toFloat()

        // Configure and measure text
        val maxTextWidthPx = (radius * 0.7f).toInt()
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(participant),
            style = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 3f
                ),
                textAlign = TextAlign.Center
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            softWrap = false,
            constraints = Constraints(maxWidth = maxTextWidthPx)
        )

        // Draw text centered at the calculated position
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = textX - textLayoutResult.size.width / 2,
                y = textY - textLayoutResult.size.height / 2
            )
        )
    }

    // Draw wheel border
    drawCircle(
        color = Color.DarkGray,
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawPointer() {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 10.dp.toPx()

    val path = Path().apply {
        moveTo(center.x + radius - 5.dp.toPx(), center.y)
        lineTo(center.x + radius + 15.dp.toPx(), center.y - 10.dp.toPx())
        lineTo(center.x + radius + 15.dp.toPx(), center.y + 10.dp.toPx())
        close()
    }

    drawPath(
        path = path,
        color = Color.Red,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(path = path, color = Color.Red)
}