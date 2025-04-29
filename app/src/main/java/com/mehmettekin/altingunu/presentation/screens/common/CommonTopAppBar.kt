package com.mehmettekin.altingunu.presentation.screens.common


import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(
    title: String,
    navController: NavController,
    backgroundColor: Color = NavyBlue,
    titleColor: Color = White,
    iconColor: Color = White,
    onBackPressed: (() -> Unit)? = null,
    isSettingsScreen: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = titleColor
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = titleColor,
            actionIconContentColor = iconColor
        ),
        navigationIcon = {
            if (onBackPressed != null) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Geri Dön",
                        tint = iconColor
                    )
                }
            }
        },
        actions = {
            // Eğer ayarlar ekranında değilsek, ayarlar ikonunu göster
            if (!isSettingsScreen) {
                IconButton(onClick = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = iconColor
                    )
                }
            }

            // Ekranın kendi özel action'ları
            actions()
        }
    )
}