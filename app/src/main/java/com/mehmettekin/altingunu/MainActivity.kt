package com.mehmettekin.altingunu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import org.bouncycastle.oer.its.EndEntityType.app
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {



    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val altinGunuApp: AltinGunuApplication by lazy {
        application as AltinGunuApplication
    }

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
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val isFirstLaunch = userPreferencesRepository.isFirstLaunch().first()

            if (isFirstLaunch) {
                // İlk açılışta cihaz dilini tespit et ve kaydet
                val detectedLanguage = detectDeviceLanguage()
                userPreferencesRepository.setLanguage(detectedLanguage)
                altinGunuApp.setCurrentLanguage(detectedLanguage)
                userPreferencesRepository.setFirstLaunchCompleted()

                // Aktiviteyi yeniden başlat
                recreateActivity()
                return@launch
            }

            // İlk açılış değilse, DataStore'dan dili kontrol et
            val storedLanguage = userPreferencesRepository.getLanguage().first()
            if (altinGunuApp.currentLanguage != storedLanguage) {
                altinGunuApp.setCurrentLanguage(storedLanguage)
                recreateActivity()
                return@launch
            }

            // Her şey tutarlıysa UI'ı göster
            keepSplashScreen = false
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
        }
    }

    private fun detectDeviceLanguage(): String {
        val deviceLanguage = java.util.Locale.getDefault().language
        return when {
            deviceLanguage in com.mehmettekin.altingunu.utils.Constraints.SUPPORTED_LANGUAGES -> deviceLanguage
            "en" in com.mehmettekin.altingunu.utils.Constraints.SUPPORTED_LANGUAGES -> "en"
            else -> com.mehmettekin.altingunu.utils.Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }
    }

    private fun recreateActivity() {
        val intent = Intent(this@MainActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
    }



}

