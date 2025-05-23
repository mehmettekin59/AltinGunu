package com.mehmettekin.altingunu

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.mehmettekin.altingunu.utils.LocaleHelper

abstract class BaseLanguageActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val app = newBase.applicationContext as AltinGunuApplication
            val languageCode = app.currentLanguage
            super.attachBaseContext(LocaleHelper.updateLocale(newBase, languageCode))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply language settings at activity creation
        val app = applicationContext as AltinGunuApplication
        LocaleHelper.updateLocale(this, app.currentLanguage)
    }

    override fun onResume() {
        super.onResume()

        // Re-apply language settings on resume to ensure consistency
        val app = applicationContext as AltinGunuApplication
        LocaleHelper.updateLocale(this, app.currentLanguage)
    }
}