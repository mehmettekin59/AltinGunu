package com.mehmettekin.altingunu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.presentation.navigation.SetupNavGraph
import com.mehmettekin.altingunu.ui.theme.AltinGunuTheme
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.LocaleHelper
import com.mehmettekin.altingunu.utils.NumeralHelper
import com.mehmettekin.altingunu.utils.RTLHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val altinGunuApp: AltinGunuApplication by lazy {
        application as AltinGunuApplication
    }

    private var keepSplashScreen = true

    // Permission launcher
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission granted: $isGranted")
        lifecycleScope.launch {
            userPreferencesRepository.setFirstLaunchCompleted()
            keepSplashScreen = false
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedLanguage = prefs.getString("language_code", null)

            val targetLang = if (savedLanguage != null) {
                savedLanguage
            } else {
                val deviceLang = Locale.getDefault().language
                if (deviceLang in Constraints.SUPPORTED_LANGUAGES) deviceLang else "tr"
            }

            super.attachBaseContext(LocaleHelper.updateLocale(newBase, targetLang))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Uygulama içeriğini hemen set et
        setContent {
            AltinGunuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    SetupNavGraph(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController
                    )
                }
            }
        }

        // Arka planda initialization yap
        lifecycleScope.launch {
            initializeApp()
        }
    }

    private suspend fun initializeApp() {
        try {
            val isFirstLaunch = userPreferencesRepository.isFirstLaunch().first()
            val storedLanguage = userPreferencesRepository.getLanguage().first()

            // Dil ayarlarını güncelle
            updateLanguageSettings(storedLanguage)

            if (isFirstLaunch) {
                // İlk açılışta izin kontrolü yap
                checkNotificationPermission()
            } else {
                // Normal başlatma
                keepSplashScreen = false

                // Dil değişikliği kontrolü
                if (altinGunuApp.currentLanguage != storedLanguage) {
                    altinGunuApp.setCurrentLanguage(storedLanguage)
                    recreateActivity()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization error", e)
            keepSplashScreen = false
        }
    }

    private fun updateLanguageSettings(language: String) {
        NumeralHelper.setLanguage(language)
        RTLHelper.setLanguage(language)
        Constraints.setLanguage(language)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                lifecycleScope.launch {
                    userPreferencesRepository.setFirstLaunchCompleted()
                    keepSplashScreen = false
                }
            }
        } else {
            lifecycleScope.launch {
                userPreferencesRepository.setFirstLaunchCompleted()
                keepSplashScreen = false
            }
        }
    }

    private fun recreateActivity() {
        val intent = intent
        finish()
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}





