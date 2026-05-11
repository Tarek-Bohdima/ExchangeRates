package com.terraconnect.exchangerates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.terraconnect.exchangerates.ui.conversion.ConversionScreen
import com.terraconnect.exchangerates.ui.theme.ExchangeRatesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host — every screen is a Composable. The activity exists
 * purely as the platform entry point.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExchangeRatesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConversionScreen()
                }
            }
        }
    }
}
