package com.example.fluidcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.ui.MainScreen
import com.example.fluidcheck.ui.auth.LoginScreen
import com.example.fluidcheck.ui.auth.SignUpScreen
import com.example.fluidcheck.ui.screens.InitialSetupScreen
import com.example.fluidcheck.ui.theme.AquaTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaTrackTheme {
                val context = LocalContext.current
                val repository = remember { UserPreferencesRepository(context) }
                val scope = rememberCoroutineScope()

                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var isAdmin by rememberSaveable { mutableStateOf(false) }
                var username by rememberSaveable { mutableStateOf("") }
                var currentAuthScreen by rememberSaveable { mutableStateOf("login") } // "login" or "signup"
                
                var isSetupComplete by remember { mutableStateOf(false) }
                var isCheckingSetup by remember { mutableStateOf(false) }
                
                // DEBUG FLAG: Set to true when 'ael' signs in to force setup visualization
                var debugForceSetup by rememberSaveable { mutableStateOf(false) }

                // Check for setup completion when logged in
                LaunchedEffect(isLoggedIn, username) {
                    if (isLoggedIn && !isAdmin && username.isNotEmpty()) {
                        isCheckingSetup = true
                        repository.isSetupComplete(username).collect { complete ->
                            isSetupComplete = complete
                            isCheckingSetup = false
                        }
                    } else {
                        isSetupComplete = false
                    }
                }

                if (!isLoggedIn) {
                    when (currentAuthScreen) {
                        "login" -> LoginScreen(
                            onSignInClick = { u, p ->
                                if (u == "ael" && p == "1234") {
                                    username = u
                                    isLoggedIn = true
                                    isAdmin = false
                                    debugForceSetup = true // Force setup for debugging
                                    true
                                } else if (u == "admin" && p == "admin") {
                                    username = u
                                    isLoggedIn = true
                                    isAdmin = true
                                    true
                                } else {
                                    false
                                }
                            },
                            onSignUpClick = {
                                currentAuthScreen = "signup"
                            }
                        )
                        "signup" -> SignUpScreen(
                            onSignUpSuccess = {
                                username = "new_user"
                                isLoggedIn = true
                                isAdmin = false
                            },
                            onBackToLogin = {
                                currentAuthScreen = "login"
                            }
                        )
                    }
                } else if (isCheckingSetup) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = com.example.fluidcheck.ui.theme.PrimaryBlue)
                    }
                } else if (!isAdmin && (!isSetupComplete || debugForceSetup)) {
                    InitialSetupScreen(
                        onComplete = { record, dailyGoal ->
                            scope.launch {
                                if (!record.isEmpty()) {
                                    repository.saveUserRecord(username, record)
                                }
                                if (dailyGoal != null) {
                                    repository.saveDailyGoal(username, dailyGoal)
                                }
                                repository.setSetupComplete(username, true)
                                debugForceSetup = false // Reset debug flag once complete
                            }
                        }
                    )
                } else {
                    MainScreen(
                        username = username,
                        isAdmin = isAdmin,
                        onLogout = { 
                            isLoggedIn = false
                            isAdmin = false
                            username = ""
                            currentAuthScreen = "login"
                            isSetupComplete = false
                            debugForceSetup = false
                        }
                    )
                }
            }
        }
    }
}
