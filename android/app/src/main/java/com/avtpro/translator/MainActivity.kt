package com.avtpro.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.avtpro.translator.navigation.AppNavHost
import com.avtpro.translator.ui.theme.AvtProTheme
import com.avtpro.translator.viewmodel.HistoryViewModel
import com.avtpro.translator.viewmodel.SettingsViewModel
import com.avtpro.translator.viewmodel.TranslationViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val translationViewModel: TranslationViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvtProTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        settingsViewModel = settingsViewModel,
                        translationViewModel = translationViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }
        }
    }
}
