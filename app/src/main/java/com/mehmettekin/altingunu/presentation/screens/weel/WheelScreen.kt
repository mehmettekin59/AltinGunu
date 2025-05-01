package com.mehmettekin.altingunu.presentation.screens.weel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.drawscope.rotate
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.presentation.screens.common.CommonTopAppBar
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.utils.UiText
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
enum class ParticipantListType {
    REMAINING,
    WINNERS
}

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(
    navController: NavController,
    viewModel: WheelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val textMeasurer = rememberTextMeasurer()
    val rotationAnimatable = remember { Animatable(viewModel.rotation) }

    // Determine layout based on screen width
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

    // Handle wheel spinning animation
    LaunchedEffect(viewModel.isSpinning) {
        if (viewModel.isSpinning) {
            val spinCount = 5 + (3 * Math.random()).toFloat()
            val targetRotation = rotationAnimatable.value + (spinCount * 360f)

            launch {
                rotationAnimatable.animateTo(
                    targetValue = targetRotation,
                    animationSpec = tween(
                        durationMillis = 4000,
                        easing = FastOutSlowInEasing
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
            CommonTopAppBar(
                title = UiText.stringResource(R.string.raffle_for_thegold_day).asString(),
                navController = navController,
                onBackPressed = { navController.navigateUp() }
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
                    .padding(vertical = 8.dp, horizontal = 16.dp),
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
                            winners = state.winners,// burdaki viewmodel.winners yerine state yaptık
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
                    Spacer(modifier = Modifier.height(12.dp))
                    // Only show save button when all participants have been selected
                    if (state.participants.isEmpty() && viewModel.winners.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.saveResults() },
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
                                tint = White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = UiText.stringResource(R.string.continue_button).asString(),
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
            UiText.stringResource(R.string.wheel).asString(),
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

        WinnerAnnouncement(winner = if (participants.isEmpty() && viewModel.winners.size > 1)
            viewModel.winners[viewModel.winners.size - 2]
        else
            viewModel.winner)
        Spacer(modifier = Modifier.height(8.dp))
        ControlButtons(
            viewModel = viewModel,
            canSpin = participants.size > 1 && !viewModel.isSpinning
        )







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
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(UiText.stringResource(R.string.spin_wheel).asString())
        }

        Button(
            onClick = { viewModel.reset() },
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .padding(4.dp)
        ) {
            Text(UiText.stringResource(R.string.restart).asString())
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
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = NavyBlue)
        ) {
            Text(
                text = UiText.stringResource(R.string.winner_celebration, it).asString(),
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.surface,
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
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ParticipantList(
                title = UiText.stringResource(R.string.remaining_participants_special, remainingParticipants.size).asString(),
                participants = remainingParticipants,
                modifier = Modifier.weight(1f),
                listType = ParticipantListType.REMAINING,
                showIndex = false
            )

            ParticipantList(
                title = UiText.stringResource(R.string.winners_special, winners.size).asString(),
                participants = winners,
                modifier = Modifier.weight(1f),
                listType = ParticipantListType.WINNERS,
                showIndex = true
            )
        }

    }
}

@Composable
private fun ParticipantList(
    title: String,
    participants: List<String>,
    listType: ParticipantListType,
    modifier: Modifier,
    showIndex: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Gold)
                .fillMaxWidth()
                .padding(12.dp)
                .height(160.dp)
        ) { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
                color = White
            )

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                thickness = 0.5.dp,
                color = NavyBlue
            )

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
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.surface
                    )

                    HorizontalDivider(
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
                            text = when(listType) {
                                ParticipantListType.REMAINING ->
                                    UiText.stringResource(R.string.all_participants_have_been_selected).asString()
                                ParticipantListType.WINNERS ->
                                    UiText.stringResource(R.string.there_is_no_winner_yet).asString()
                            },
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

        // Draw divider lines between slices for better visual separation
        val lineAngle = startAngle * (Math.PI / 180f)
        val lineStartX = center.x + (radius * 0.3f * cos(lineAngle)).toFloat()
        val lineStartY = center.y + (radius * 0.3f * sin(lineAngle)).toFloat()
        val lineEndX = center.x + (radius * cos(lineAngle)).toFloat()
        val lineEndY = center.y + (radius * sin(lineAngle)).toFloat()

        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(lineStartX, lineStartY),
            end = Offset(lineEndX, lineEndY),
            strokeWidth = 1.dp.toPx()
        )

        // Rotate for this slice, key insight: use the block version of rotate!
        rotate(startAngle + sliceAngle / 2, pivot = center) {
            // Calculate appropriate text size based on number of participants
            val fontSize = when {
                participants.size > 15 -> 10.sp
                participants.size > 10 -> 12.sp
                else -> 14.sp
            }

            // Calculate appropriate text radius based on participant count
            val textRadius = when {
                participants.size > 15 -> radius * 0.55f
                participants.size > 10 -> radius * 0.6f
                else -> radius * 0.65f
            }

            // Set maximum text width
            val maxTextWidthPx = (radius * 0.7f).toInt()

            // Measure text
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(participant),
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
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

            // Position text correctly
            val textPosition = Offset(
                x = center.x + textRadius - textLayoutResult.size.width / 2,
                y = center.y - textLayoutResult.size.height / 2
            )

            // Draw text
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = textPosition
            )
        }
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