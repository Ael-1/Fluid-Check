package com.example.fluidcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fluidcheck.ui.MainScreen
import com.example.fluidcheck.ui.auth.LoginScreen
import com.example.fluidcheck.ui.theme.AquaTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaTrackTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    LoginScreen(
                        onSignInClick = { username, password ->
                            if (username == "ael" && password == "1234") {
                                isLoggedIn = true
                                true
                            } else {
                                false
                            }
                        },
                        onSignUpClick = {
                            // Navigate to Sign Up
                        }
                    )
                } else {
                    MainScreen(onLogout = { isLoggedIn = false })
                }
            }
        }
    }
}