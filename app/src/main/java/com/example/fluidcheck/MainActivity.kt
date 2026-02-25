package com.example.fluidcheck

import android.os.Bundle
import android.widget.Toast
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
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.ui.MainScreen
import com.example.fluidcheck.ui.auth.LoginScreen
import com.example.fluidcheck.ui.auth.SignUpScreen
import com.example.fluidcheck.ui.screens.InitialSetupScreen
import com.example.fluidcheck.ui.theme.FluidCheckTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluidCheckTheme {
                val context = LocalContext.current
                val authRepository = remember { AuthRepository() }
                val firestoreRepository = remember { FirestoreRepository() }
                val scope = rememberCoroutineScope()

                var isLoggedIn by rememberSaveable { mutableStateOf(authRepository.isUserLoggedIn()) }
                var isAdmin by rememberSaveable { mutableStateOf(false) }
                var userEmail by rememberSaveable { mutableStateOf(authRepository.currentUser?.email ?: "") }
                var currentAuthScreen by rememberSaveable { mutableStateOf("login") }
                
                var isSetupComplete by remember { mutableStateOf(false) }
                var isCheckingSetup by remember { mutableStateOf(false) }

                LaunchedEffect(isLoggedIn, authRepository.currentUser?.uid) {
                    if (isLoggedIn) {
                        isCheckingSetup = true
                        val record = firestoreRepository.getUserRecord(authRepository.currentUser?.uid ?: "")
                        // Use the setupCompleted flag to determine if we should show the setup screen
                        isSetupComplete = record?.setupCompleted ?: false
                        isCheckingSetup = false
                    }
                }

                if (!isLoggedIn) {
                    when (currentAuthScreen) {
                        "login" -> LoginScreen(
                            onSignInClick = { identifier, password ->
                                scope.launch {
                                    val email = if (identifier.contains("@")) {
                                        identifier
                                    } else {
                                        firestoreRepository.getEmailFromUsername(identifier)
                                    }

                                    if (email == null) {
                                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val result = authRepository.signIn(email, password)
                                        if (result.isSuccess) {
                                            userEmail = email
                                            isLoggedIn = true
                                        } else {
                                            Toast.makeText(context, "Login Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                true
                            },
                            onSignUpClick = { currentAuthScreen = "signup" }
                        )
                        "signup" -> SignUpScreen(
                            onSignUpSuccessWithDetails = { username, email, password ->
                                scope.launch {
                                    val result = authRepository.signUp(email, password)
                                    if (result.isSuccess) {
                                        val userId = authRepository.currentUser?.uid ?: return@launch
                                        // Store email in user document for username login lookup
                                        val userRecord = UserRecord() // Empty record for now, filled in setup
                                        firestoreRepository.saveUserRecord(userId, userRecord, username)
                                        // Also update the document with email
                                        try {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users").document(userId)
                                                .update("email", email).await()
                                        } catch (e: Exception) {
                                            // Handle case where document might not exist yet if saveUserRecord failed
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users").document(userId)
                                                .set(mapOf("email" to email, "username" to username), com.google.firebase.firestore.SetOptions.merge())
                                        }
                                        
                                        userEmail = email
                                        isLoggedIn = true
                                    } else {
                                        Toast.makeText(context, "Sign Up Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackToLogin = { currentAuthScreen = "login" }
                        )
                    }
                } else if (isCheckingSetup) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = com.example.fluidcheck.ui.theme.PrimaryBlue)
                    }
                } else if (!isSetupComplete) {
                    InitialSetupScreen(
                        onComplete = { record, dailyGoal ->
                            scope.launch {
                                try {
                                    val userId = authRepository.currentUser?.uid ?: return@launch
                                    // Get the current username to preserve it
                                    val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("users").document(userId).get().await()
                                    val username = doc.getString("username") ?: ""
                                    
                                    firestoreRepository.saveUserRecord(userId, record, username)
                                    if (dailyGoal != null) {
                                        firestoreRepository.saveDailyGoal(userId, dailyGoal)
                                    }
                                    isSetupComplete = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error saving setup: ${e.message}", Toast.LENGTH_SHORT).show()
                                    // If saving failed, we still want the user to be able to use the app if possible, 
                                    // or at least stay on the setup screen to try again.
                                    // For "Skip for now", we might want to force completion anyway if we want to bypass errors.
                                }
                            }
                        }
                    )
                } else {
                    MainScreen(
                        username = userEmail.split("@")[0], // Fallback display name
                        isAdmin = isAdmin,
                        onLogout = { 
                            authRepository.signOut()
                            isLoggedIn = false
                            userEmail = ""
                            currentAuthScreen = "login"
                            isSetupComplete = false
                        }
                    )
                }
            }
        }
    }
}
