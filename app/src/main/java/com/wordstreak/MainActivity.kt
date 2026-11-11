package com.wordstreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wordstreak.navigation.WordStreakNavHost
import com.wordstreak.ui.theme.WordStreakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordStreakTheme {
                WordStreakNavHost()
            }
        }
    }
}
