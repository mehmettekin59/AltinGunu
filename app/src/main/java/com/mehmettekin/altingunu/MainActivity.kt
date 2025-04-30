package com.mehmettekin.altingunu

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.mehmettekin.altingunu.presentation.navigation.SetupNavGraph
import com.mehmettekin.altingunu.ui.theme.AltinGunuTheme
import com.mehmettekin.altingunu.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val app = newBase.applicationContext as AltinGunuApplication
            val languageCode = app.currentLanguage
            super.attachBaseContext(LocaleHelper.updateLocale(newBase, languageCode))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private var keepSplashScreen = true
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen'i kur - SplashScreen API
        val splashScreen = installSplashScreen()
        // Splash screen'in görünürlüğünü kontrol et
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            keepSplashScreen = false
            AltinGunuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    SetupNavGraph(modifier = Modifier.padding(innerPadding), navController = navController)
                }
            }
        }
    }
}

