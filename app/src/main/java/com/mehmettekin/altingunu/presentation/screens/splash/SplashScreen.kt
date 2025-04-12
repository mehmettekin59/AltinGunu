package com.mehmettekin.altingunu.presentation.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "SplashAlpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1500)
        navController.navigate(Screen.Enter.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Splash(alpha = alphaAnim.value)
}

@Composable
fun Splash(alpha: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .size(120.dp)
                .alpha(alpha),
            painter = painterResource(id = R.drawable.ic_gold_coin),
            contentDescription = "Logo"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Altın Günü",
            color = Gold,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(alpha)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Çekiliş Uygulaması",
            color = Gold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.alpha(alpha)
        )
    }
}