package com.mehmettekin.altingunu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = NavyBlue,
    tertiary = White,
)

private val LightColorScheme = lightColorScheme(
    primary = Gold,
    secondary = NavyBlue,
    tertiary = White,
    onTertiary = Black


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun responsiveTypography(): androidx.compose.material3.Typography {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    // Ekran genişliğine göre font boyutlarını ayarla
    val bodyLargeFontSize = when {
        screenWidth < 320 -> 14.sp
        screenWidth < 480 -> 16.sp
        else -> 18.sp
    }

    val bodyMediumFontSize = when {
        screenWidth < 320 -> 12.sp
        screenWidth < 480 -> 14.sp
        else -> 16.sp
    }

    val bodySmallFontSize = when {
        screenWidth < 320 -> 10.sp
        screenWidth < 480 -> 12.sp
        else -> 14.sp
    }
    val titleLargeFontSize = when {
        screenWidth < 320 -> 20.sp
        screenWidth < 480 -> 22.sp
        else -> 24.sp
    }

    val titleMediumFontSize = when {
        screenWidth < 320 -> 18.sp
        screenWidth < 480 -> 20.sp
        else -> 22.sp
    }

    val titleSmallFontSize = when {
        screenWidth < 320 -> 16.sp
        screenWidth < 480 -> 18.sp
        else -> 20.sp
    }

    // Label font sizes
    val labelLargeFontSize = when {
        screenWidth < 320 -> 14.sp
        screenWidth < 480 -> 16.sp
        else -> 18.sp
    }

    val labelMediumFontSize = when {
        screenWidth < 320 -> 12.sp
        screenWidth < 480 -> 14.sp
        else -> 16.sp
    }

    val labelSmallFontSize = when {
        screenWidth < 320 -> 10.sp
        screenWidth < 480 -> 12.sp
        else -> 14.sp
    }

    // Material 3 Typography oluştur
    // Not: Material 3'te body1 ve body2 yerine bodyLarge, bodyMedium, bodySmall kullanılır
    return androidx.compose.material3.Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = bodyLargeFontSize
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = bodyMediumFontSize
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = bodySmallFontSize
        ),

        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = titleLargeFontSize,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = titleMediumFontSize
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = titleSmallFontSize
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = labelLargeFontSize
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = labelMediumFontSize
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = labelSmallFontSize,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )

    )
}

@Composable
fun AltinGunuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = responsiveTypography(),
        content = content
    )
}