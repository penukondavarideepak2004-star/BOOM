package com.boom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.boom.app.navigation.BoomNavHost
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BoomDarkBackground
                ) {
                    BoomNavHost()
                }
            }
        }
    }
}
