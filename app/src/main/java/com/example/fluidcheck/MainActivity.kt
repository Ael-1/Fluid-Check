package com.example.fluidcheck

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.ui.MainScreen
import com.example.fluidcheck.ui.auth.LoginScreen
import com.example.fluidcheck.ui.auth.SignUpScreen
import com.example.fluidcheck.ui.screens.InitialSetupScreen
import com.example.fluidcheck.ui.theme.AppIcons
import com.example.fluidcheck.ui.theme.FluidCheckTheme
import com.example.fluidcheck.ui.theme.PrimaryBlue
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

private const val ADMIN_EMAIL = "admin@fluidcheck.ai"
private const val ADMIN_USERNAME = "admin"

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
                var currentUsername by rememberSaveable { mutableStateOf("") }
                var currentAuthScreen by rememberSaveable { mutableStateOf("login") }
                
                var isSetupComplete by rememberSaveable { mutableStateOf(false) }
                // Use remember(isLoggedIn) to ensure isCheckingSetup is reset to true whenever isLoggedIn changes to true
                var isCheckingSetup by remember(isLoggedIn) { mutableStateOf(isLoggedIn) }
                var isAuthInProgress by remember { mutableStateOf(false) }

                // Auth Status Dialog State
                var showAuthStatusDialog by remember { mutableStateOf(false) }
                var authStatusTitle by remember { mutableStateOf("") }
                var authStatusMessage by remember { mutableStateOf("") }
                var isAuthSuccess by remember { mutableStateOf(false) }
                var pendingActionAfterDialog by remember { mutableStateOf<(() -> Unit)?>(null) }

                // Check for Google Play Services availability
                val isGoogleAvailable = remember {
                    com.google.android.gms.common.GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
                }

                // Google Sign In Launcher
                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            scope.launch {
                                isAuthInProgress = true
                                val authResult = authRepository.signInWithGoogle(idToken)
                                if (authResult.isSuccess) {
                                    val userId = authRepository.currentUser?.uid ?: return@launch
                                    val email = authRepository.currentUser?.email ?: ""
                                    
                                    var username = firestoreRepository.getUsernameFromUid(userId)
                                    if (username == null) {
                                        username = email.split("@")[0]
                                        val userRecord = UserRecord(
                                            uid = userId,
                                            username = username,
                                            email = email,
                                            createdAt = Timestamp.now()
                                        )
                                        firestoreRepository.saveUserRecord(userId, userRecord)
                                    }
                                    currentUsername = username
                                    isLoggedIn = true
                                } else {
                                    authStatusTitle = "Sign In Failed"
                                    authStatusMessage = "Firebase Google Auth Failed"
                                    isAuthSuccess = false
                                    showAuthStatusDialog = true
                                }
                                isAuthInProgress = false
                            }
                        }
                    } catch (e: ApiException) {
                        val statusCode = e.statusCode
                        authStatusTitle = "Google Sign In Error"
                        authStatusMessage = "Error code: $statusCode"
                        isAuthSuccess = false
                        showAuthStatusDialog = true
                        Log.e("MainActivity", "Google Sign In Error: $statusCode", e)
                        isAuthInProgress = false
                    }
                }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        isCheckingSetup = true
                        val currentUser = authRepository.currentUser
                        val userId = currentUser?.uid ?: ""
                        val email = currentUser?.email ?: ""
                        
                        // Fetch the full record to check setup status
                        var record = firestoreRepository.getUserRecord(userId)
                        
                        // Special handling for Admin - check email case-insensitively
                        if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                            if (record == null || record.role != "ADMIN" || !record.setupCompleted || record.dailyGoal != null) {
                                val adminRecord = (record ?: UserRecord()).copy(
                                    uid = userId,
                                    username = ADMIN_USERNAME,
                                    email = ADMIN_EMAIL,
                                    role = "ADMIN",
                                    setupCompleted = true,
                                    createdAt = record?.createdAt ?: Timestamp.now(),
                                    dailyGoal = null // Admin has no daily goal
                                )
                                firestoreRepository.saveUserRecord(userId, adminRecord)
                                record = adminRecord
                            }
                        }

                        if (record != null) {
                            currentUsername = record.username
                            isSetupComplete = record.setupCompleted
                        } else {
                            isSetupComplete = false
                        }
                        
                        isCheckingSetup = false
                    }
                }

                // Auth Status Dialog
                if (showAuthStatusDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showAuthStatusDialog = false 
                            pendingActionAfterDialog?.invoke()
                            pendingActionAfterDialog = null
                            isAuthInProgress = false
                        },
                        confirmButton = {
                            Button(
                                onClick = { 
                                    showAuthStatusDialog = false 
                                    pendingActionAfterDialog?.invoke()
                                    pendingActionAfterDialog = null
                                    isAuthInProgress = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isAuthSuccess) "Awesome" else "Try Again")
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isAuthSuccess) AppIcons.Goal else AppIcons.Info,
                                contentDescription = null,
                                tint = if (isAuthSuccess) Color(0xFFFFD700) else Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        title = {
                            @Suppress("DEPRECATION")
                            Text(
                                text = authStatusTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            @Suppress("DEPRECATION")
                            Text(
                                text = authStatusMessage,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                if (!isLoggedIn) {
                    when (currentAuthScreen) {
                        "login" -> LoginScreen(
                            onSignInClick = { identifier, password ->
                                scope.launch {
                                    isAuthInProgress = true
                                    val email = if (identifier.equals(ADMIN_USERNAME, ignoreCase = true) || identifier.equals(ADMIN_EMAIL, ignoreCase = true)) {
                                        ADMIN_EMAIL
                                    } else if (identifier.contains("@")) {
                                        identifier
                                    } else {
                                        firestoreRepository.getEmailFromUsername(identifier)
                                    }

                                    if (email == null) {
                                        authStatusTitle = getString(R.string.error_user_not_found_title)
                                        authStatusMessage = getString(R.string.error_user_not_found_msg)
                                        isAuthSuccess = false
                                        showAuthStatusDialog = true
                                    } else {
                                        val result = authRepository.signIn(email, password)
                                        if (result.isSuccess) {
                                            isLoggedIn = true
                                            isAuthInProgress = false 
                                        } else {
                                            authStatusTitle = getString(R.string.error_invalid_credentials_title)
                                            authStatusMessage = getString(R.string.error_invalid_credentials_msg)
                                            isAuthSuccess = false
                                            showAuthStatusDialog = true
                                        }
                                    }
                                }
                            },
                            onSignUpClick = { currentAuthScreen = "signup" },
                            onGoogleSignInClick = {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build()
                                val googleSignInClient = GoogleSignIn.getClient(this@MainActivity, gso)
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            isGoogleAvailable = isGoogleAvailable,
                            isLoading = isAuthInProgress
                        )
                        "signup" -> SignUpScreen(
                            onSignUpSuccessWithDetails = { username, email, password ->
                                scope.launch {
                                    isAuthInProgress = true
                                    try {
                                        if (email != ADMIN_EMAIL && !firestoreRepository.isUsernameAvailable(username)) {
                                            authStatusTitle = getString(R.string.error_username_taken_title)
                                            authStatusMessage = getString(R.string.error_username_taken_msg)
                                            isAuthSuccess = false
                                            showAuthStatusDialog = true
                                            return@launch
                                        }

                                        val result = authRepository.signUp(email, password)
                                        if (result.isSuccess) {
                                            val userId = authRepository.currentUser?.uid ?: return@launch
                                            val userRecord = UserRecord(
                                                uid = userId,
                                                username = if (email.equals(ADMIN_EMAIL, ignoreCase = true)) ADMIN_USERNAME else username,
                                                email = email,
                                                role = if (email.equals(ADMIN_EMAIL, ignoreCase = true)) "ADMIN" else "USER",
                                                setupCompleted = email.equals(ADMIN_EMAIL, ignoreCase = true),
                                                createdAt = Timestamp.now(),
                                                dailyGoal = if (email.equals(ADMIN_EMAIL, ignoreCase = true)) null else 3000
                                            )
                                            val saveResult = firestoreRepository.saveUserRecord(userId, userRecord)
                                            
                                            if (saveResult.isSuccess) {
                                                authRepository.signOut()
                                                authStatusTitle = getString(R.string.signup_success_title)
                                                authStatusMessage = getString(R.string.signup_success_msg)
                                                isAuthSuccess = true
                                                pendingActionAfterDialog = { currentAuthScreen = "login" }
                                                showAuthStatusDialog = true
                                            } else {
                                                authStatusTitle = "Profile Error"
                                                authStatusMessage = "Account created but failed to save profile details."
                                                isAuthSuccess = false
                                                showAuthStatusDialog = true
                                            }
                                        } else {
                                            authStatusTitle = getString(R.string.signup_failed_title)
                                            authStatusMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                            isAuthSuccess = false
                                            showAuthStatusDialog = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "SignUp Error", e)
                                        authStatusTitle = "Connection Error"
                                        authStatusMessage = "An unexpected error occurred."
                                        isAuthSuccess = false
                                        showAuthStatusDialog = true
                                    }
                                }
                            },
                            onBackToLogin = { currentAuthScreen = "login" },
                            isLoading = isAuthInProgress
                        )
                    }
                } else if (isCheckingSetup) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else if (!isSetupComplete) {
                    InitialSetupScreen(
                        onComplete = { record, dailyGoal ->
                            scope.launch {
                                try {
                                    val userId = authRepository.currentUser?.uid ?: ""
                                    if (userId.isNotEmpty()) {
                                        val currentRecord = firestoreRepository.getUserRecord(userId)
                                        val finalRecord = record.copy(
                                            uid = userId,
                                            username = currentRecord?.username ?: currentUsername,
                                            email = authRepository.currentUser?.email ?: "",
                                            createdAt = currentRecord?.createdAt ?: Timestamp.now(),
                                            setupCompleted = true,
                                            dailyGoal = dailyGoal ?: currentRecord?.dailyGoal ?: 3000
                                        )
                                        
                                        firestoreRepository.saveUserRecord(userId, finalRecord)
                                        isSetupComplete = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Setup completion error", e)
                                }
                            }
                        }
                    )
                } else {
                    MainScreen(
                        userId = authRepository.currentUser?.uid ?: "",
                        username = currentUsername,
                        onLogout = { 
                            authRepository.signOut()
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(this@MainActivity, gso).signOut()
                            isLoggedIn = false
                            currentUsername = ""
                            currentAuthScreen = "login"
                            isSetupComplete = false
                        }
                    )
                }
            }
        }
    }
}
