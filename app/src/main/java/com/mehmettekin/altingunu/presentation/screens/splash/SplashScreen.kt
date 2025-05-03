package com.mehmettekin.altingunu.presentation.screens.splash

import android.graphics.drawable.shapes.RoundRectShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var animationStarted by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    // Automatically navigate to the Enter screen after the splash animation completes
    LaunchedEffect(key1 = true) {
        animationStarted = true
        delay(900) // Wait a bit before showing content
        showContent = true
        delay(3000) // Total time to show splash screen
        navController.navigate(Screen.Enter.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavyBlue,
                        NavyBlue.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        // Create shimmering gold particles in the background
        GoldParticlesBackground()

        // Content centered in the screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with pulsating effect
            AnimatedLogo(showContent)

            Spacer(modifier = Modifier.height(24.dp))

            // App name with fade-in animation
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Text(
                    text = stringResource(R.string.welcome_title_message),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }

        // Optional disclaimer text at the bottom
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 800,
                    delayMillis = 500,
                    easing = FastOutSlowInEasing
                )
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.what_is_the_app_name),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GoldParticlesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val particlesAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particlesAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw several gold particles
                repeat(21) {
                    val x = (Math.random() * canvasWidth).toFloat()
                    val y = (Math.random() * canvasHeight).toFloat()
                    val particleSize = (5 + Math.random() * 8).toFloat()

                    // Create a slightly different alpha for each particle
                    val thisParticleAlpha = (0.1f + Math.random() * 0.3f) * particlesAlpha

                    drawCircle(
                        color = Gold.copy(alpha = thisParticleAlpha.toFloat()),
                        radius = particleSize,
                        center = Offset(x, y)
                    )
                }
            }
    )
}

@Composable
private fun AnimatedLogo(visible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val ringAnimationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val appearScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = tween(durationMillis = 800),
        label = "appear"
    )

    Box(
        modifier = Modifier
            .size(180.dp)
            .drawBehind {
                // Draw an animated ring around the logo
                val radius = size.width / 2
                val stroke = 4.dp.toPx()
                drawCircle(
                    color = Gold.copy(alpha = 0.6f * (1 - ringAnimationValue)),
                    radius = radius * (0.8f + ringAnimationValue * 0.3f),
                    style = Stroke(width = stroke * (1 - ringAnimationValue * 0.5f))
                )
            }
            .scale(appearScale * scale),
        contentAlignment = Alignment.Center
    ) {
        // Circular background for the logo
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Gold.copy(alpha = 0.3f),
                            Gold.copy(alpha = 0.1f)
                        )
                    )
                )
        )

        // The image logo
        Image(
            painter = painterResource(R.drawable.appiconcircle),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}