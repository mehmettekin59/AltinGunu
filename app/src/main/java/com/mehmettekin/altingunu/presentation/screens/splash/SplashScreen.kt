package com.mehmettekin.altingunu.presentation.screens.splash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.ui.theme.Gold
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
    ) {
        // WelcomeText en üstte
        WelcomeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        )



        // En altta Checkbox ve Button
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var isChecked = remember { mutableStateOf(false) }
            DisclaimerBox(
                modifier = Modifier

            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.clickable{isChecked.value = !isChecked.value},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked.value,
                    onCheckedChange = null,
                    enabled = false,
                    colors = CheckboxDefaults.colors(
                        disabledCheckedColor = Gold,
                        checkedColor = Gold,
                        checkmarkColor = White,
                        disabledUncheckedColor = White
                    )
                )

                Text(
                    text = UiText.stringResource(R.string.confirm_understanding).asString(),
                    color = White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isChecked.value){
                        navController.navigate(Screen.Enter.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        //do nothing
                    }

                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    contentColor = White
                ),
                border = BorderStroke(width = 1.dp, color = Gold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    tint = Gold,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = UiText.stringResource(R.string.continue_button).asString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Gold
                )
            }
        }
    }
}

@Composable
private fun DisclaimerBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = Gold,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = UiText.stringResource(R.string.information_about_rules).asString(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify,
            fontSize = 16.sp,
            color = White,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WelcomeText(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_title_message),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            color = White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            painter = painterResource(R.drawable.image1),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
            tint = Color.Unspecified,
        )
    }
}