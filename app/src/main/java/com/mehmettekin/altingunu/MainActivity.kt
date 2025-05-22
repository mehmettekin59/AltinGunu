package com.mehmettekin.altingunu

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.presentation.navigation.SetupNavGraph
import com.mehmettekin.altingunu.ui.theme.AltinGunuTheme
import com.mehmettekin.altingunu.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

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
        // Splash screen'i kur
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // İlk kurulum kontrolü ve gerekirse dil tespiti yap
        lifecycleScope.launch {
            // İlk kurulum mu kontrol et
            val isFirstLaunch = userPreferencesRepository.isFirstLaunch().first()

            if (isFirstLaunch) {
                // İlk kurulum işaretini kapat
                userPreferencesRepository.setFirstLaunchCompleted()
            }

            // Güncel dil ayarını uygula
            val app = applicationContext as AltinGunuApplication
            LocaleHelper.updateLocale(this@MainActivity, app.currentLanguage)

            // Splash screen'i kapat ve içeriği göster
            keepSplashScreen = false
        }

        // Ana içeriği ayarla
        setContent {
            AltinGunuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    SetupNavGraph(modifier = Modifier.padding(innerPadding), navController = navController)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Dil ayarlarının korunmasını sağla
        val app = applicationContext as AltinGunuApplication
        LocaleHelper.updateLocale(this, app.currentLanguage)
    }

    override fun onResume() {
        super.onResume()
        // Aktivite önplana geldiğinde dil ayarlarını tekrar uygula
        val app = applicationContext as AltinGunuApplication
        LocaleHelper.updateLocale(this, app.currentLanguage)
    }

}



//BURASI SİL
/*
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

 */