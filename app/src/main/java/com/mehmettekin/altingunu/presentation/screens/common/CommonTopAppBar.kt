package com.mehmettekin.altingunu.presentation.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mehmettekin.altingunu.presentation.navigation.Screen
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.ui.theme.White
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.ui.theme.Gold

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
    Box(
        modifier = Modifier
            .height(50.dp) // Daha kompakt yükseklik
            .fillMaxWidth()
            .background(backgroundColor)
            .border(width = 1.dp, color = Gold)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically // Dikey olarak ortalama
        ) {
            // Geri butonu
            if (onBackPressed != null) {
                IconButton(
                    modifier = Modifier.size(36.dp), // Daha küçük buton alanı
                    onClick = onBackPressed
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = UiText.stringResource(R.string.go_back).asString(),
                        tint = iconColor,
                        modifier = Modifier.size(18.dp) // Daha küçük ikon
                    )
                }
            } else {
                // Geri butonu yoksa dengelemek için boşluk bırak
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Başlık (genişliğin çoğunu kaplayacak şekilde ağırlık verdim)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center // İçeriği merkeze hizala
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    textAlign = TextAlign.Center, // Metni ortala
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sağ taraftaki ikonlar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ekranın kendi özel action'ları
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    actions()
                }

                // Eğer ayarlar ekranında değilsek, ayarlar ikonunu göster
                if (!isSettingsScreen) {
                    IconButton(
                        modifier = Modifier.size(36.dp), // Daha küçük buton alanı
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = UiText.stringResource(R.string.title_settings).asString(),
                            tint = iconColor,
                            modifier = Modifier.size(18.dp) // Daha küçük ikon
                        )
                    }
                }
            }
        }
    }
}