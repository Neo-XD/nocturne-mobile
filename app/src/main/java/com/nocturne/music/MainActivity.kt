package com.nocturne.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.media3.common.util.UnstableApi
import com.nocturne.music.ui.screens.MainScreen
import com.nocturne.music.ui.theme.NocturneTheme

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NocturneTheme {
                MainScreen()
            }
        }
    }
}
