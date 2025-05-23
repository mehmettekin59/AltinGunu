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
import com.mehmettekin.altingunu.utils.LanguageDetectionUtil
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
        // Splash screen'i kur
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val isFirstLaunch = userPreferencesRepository.isFirstLaunch().first()
            var languageToApply = app.currentLanguage // Başlangıçta uygulama objesindeki dil

            if (isFirstLaunch) {
                val detectedDeviceLanguage = LanguageDetectionUtil.detectDeviceLanguage()
                Log.d("MainActivity", "First launch, detected device language: $detectedDeviceLanguage")

                // Cihaz dilini DataStore'a kaydet
                userPreferencesRepository.setLanguage(detectedDeviceLanguage)
                // Uygulama objesindeki dili de güncelle
                app.updateCurrentLanguage(detectedDeviceLanguage)
                languageToApply = detectedDeviceLanguage // Uygulanacak dil bu olacak

                userPreferencesRepository.setFirstLaunchCompleted()

                // Dil değiştiği için aktiviteyi yeniden başlatmak en temizi
                // (attachBaseContext'in doğru dille çalışması için)
                Log.d("MainActivity", "Recreating activity for first launch language setting.")
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                finish()
                return@launch // Coroutine'i burada bitir, çünkü aktivite yeniden başlatılıyor
            }

            // İlk açılış değilse, DataStore'dan gelen dili kontrol et
            val currentStoredLanguage = userPreferencesRepository.getLanguage().first()
            if (app.currentLanguage != currentStoredLanguage) {
                Log.d("MainActivity", "Language mismatch. App: ${app.currentLanguage}, Stored: $currentStoredLanguage. Recreating.")
                app.updateCurrentLanguage(currentStoredLanguage)
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                finish()
                return@launch
            }

            // Buraya kadar gelindiyse dil tutarlıdır veya ilk açılış değildir (ve dil zaten ayarlanmıştır)
            keepSplashScreen = false
            setContent {
                AltinGunuTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val navController = rememberNavController()
                        SetupNavGraph(modifier = Modifier.padding(innerPadding), navController = navController)
                    }
                }
            }
        }
    }



}

