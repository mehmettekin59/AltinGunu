package com.mehmettekin.altingunu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val altinGunuApp: AltinGunuApplication by lazy {
        application as AltinGunuApplication
    }

    // ✅ DÜZELTME: İzin sonucuna göre uygulama başlatma
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
        // İzin verildikten sonra (veya reddedildikten sonra) uygulamayı başlat
        startAppAfterPermissionCheck()
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
                // İlk açılışta önce dil ayarlarını yap
                setupInitialLanguage()

                // Sonra izin kontrolü yap
                checkNotificationPermission()

                // NOT: recreateActivity() artık izin kontrolünden sonra çağrılacak
                return@launch
            }

            // İlk açılış değilse, normal startup süreci
            handleNormalStartup()
        }
    }

    //  İlk açılış dil ayarları
    private suspend fun setupInitialLanguage() {
        val detectedLanguage = detectDeviceLanguage()
        userPreferencesRepository.setLanguage(detectedLanguage)
        altinGunuApp.setCurrentLanguage(detectedLanguage)
        userPreferencesRepository.setFirstLaunchCompleted()
        NumeralHelper.setLanguage(detectedLanguage)
        RTLHelper.setLanguage(detectedLanguage)
        Constraints.setLanguage(detectedLanguage)
    }

    //  İzin kontrolü (dil ayarlarından bağımsız)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                // İzin yoksa iste - sonuç launcher'da işlenecek
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // İzin zaten var, uygulamayı başlat
                startAppAfterPermissionCheck()
            }
        } else {
            // Android 13 öncesi, izin gerekmiyor
            startAppAfterPermissionCheck()
        }
    }

    // ✅ YENİ: İzin kontrolünden sonra uygulama başlatma
    private fun startAppAfterPermissionCheck() {
        lifecycleScope.launch {
            val isFirstLaunch = userPreferencesRepository.isFirstLaunch().first()

            if (isFirstLaunch) {
                // İlk açılış tamamlandı, aktiviteyi yeniden başlat
                recreateActivity()
            } else {
                // Normal startup devam et
                handleNormalStartup()
            }
        }
    }

    // ✅ YENİ: Normal startup süreci
    private suspend fun handleNormalStartup() {
        // İlk açılış değilse, DataStore'dan dili kontrol et
        val storedLanguage = userPreferencesRepository.getLanguage().first()
        if (altinGunuApp.currentLanguage != storedLanguage) {
            altinGunuApp.setCurrentLanguage(storedLanguage)
            NumeralHelper.setLanguage(storedLanguage)
            RTLHelper.setLanguage(storedLanguage)
            Constraints.setLanguage(storedLanguage)
            recreateActivity()
            return
        }

        NumeralHelper.setLanguage(altinGunuApp.currentLanguage)
        RTLHelper.setLanguage(altinGunuApp.currentLanguage)
        Constraints.setLanguage(altinGunuApp.currentLanguage)

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

