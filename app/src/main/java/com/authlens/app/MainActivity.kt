package com.authlens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.authlens.app.presentation.navigation.AuthLensApp
import com.authlens.app.core.theme.AuthLensTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity hosting the Compose UI. All screens are rendered inside [AuthLensApp].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            AuthLensTheme {
                AuthLensApp()
            }
        }
    }
}
