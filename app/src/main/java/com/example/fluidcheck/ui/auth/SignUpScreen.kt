package com.example.fluidcheck.ui.auth

import kotlinx.coroutines.delay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.*

@Composable
fun SignUpScreen(
    onSignUpSuccessWithDetails: (String, String, String) -> Unit,
    onBackToLogin: () -> Unit,
    onGoogleSignInClick: () -> Unit = {},
    isLoading: Boolean = false,
    isGoogleAvailable: Boolean = false,
    firestoreRepository: com.example.fluidcheck.repository.FirestoreRepository? = null
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val emptyUsernameErr = stringResource(R.string.error_empty_username)
    val emptyEmailErr = stringResource(R.string.error_empty_email)
    val invalidEmailErr = stringResource(R.string.error_invalid_email)
    val shortPasswordErr = stringResource(R.string.error_short_password)
    val mismatchPasswordErr = stringResource(R.string.error_password_mismatch)
    
    // Task 9.3: Proactive Conflict Prevention
    var isCheckingUsername by remember { mutableStateOf(false) }
    LaunchedEffect(username) {
        if (username.length >= 4 && firestoreRepository != null) {
            delay(500) // Debounce
            isCheckingUsername = true
            val isAvailable = firestoreRepository.isUsernameAvailable(username)
            if (!isAvailable) {
                usernameError = "Username is already taken."
            } else {
                if (usernameError == "Username is already taken.") usernameError = null
            }
            isCheckingUsername = false
        }
    }

    val focusManager = LocalFocusManager.current
    
    fun validateInputs(): Boolean {
        var isValid = true

        usernameError = com.example.fluidcheck.util.ValidationUtils.validateUsername(username)
        if (usernameError != null) {
            isValid = false
        }

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        if (email.isBlank()) {
            emailError = emptyEmailErr
            isValid = false
        } else if (!email.matches(emailPattern.toRegex())) {
            emailError = invalidEmailErr
            isValid = false
        } else {
            emailError = null
        }

        if (password.length < 6) {
            passwordError = shortPasswordErr
            isValid = false
        } else {
            passwordError = null
        }

        if (password.isNotEmpty() && confirmPassword != password) {
            confirmPasswordError = mismatchPasswordErr
            isValid = false
        } else {
            confirmPasswordError = null
        }

        return isValid
    }

    AuthScreenBackground {
        // Logo and Title
        Image(
            painter = painterResource(id = AppIcons.AppLogo),
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.join_fluid_check),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Text(
            text = stringResource(R.string.signup_subtitle),
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthFormCard {
            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (usernameError != null) usernameError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.username_label), color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(AppIcons.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(text = usernameError!!, color = Color.White)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.email_label), color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(AppIcons.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(text = emailError!!, color = Color.White)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                singleLine = true,
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.password_label), color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(AppIcons.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(imageVector = image, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                isError = passwordError != null,
                supportingText = {
                    if (passwordError != null) {
                        @Suppress("DEPRECATION")
                        Text(text = passwordError!!, color = Color.White)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (password.isEmpty()) ImeAction.Done else ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { 
                        if (validateInputs() && password.isNotEmpty()) {
                            focusManager.moveFocus(FocusDirection.Down)
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                singleLine = true,
                colors = authTextFieldColors()
            )

            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (confirmPasswordError != null) confirmPasswordError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.confirm_password_label), color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(AppIcons.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                    isError = confirmPasswordError != null,
                    supportingText = {
                        if (confirmPasswordError != null) {
                            @Suppress("DEPRECATION")
                            Text(text = confirmPasswordError!!, color = Color.White)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (validateInputs()) {
                                onSignUpSuccessWithDetails(username.trim(), email.trim(), password.trim())
                            }
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    singleLine = true,
                    colors = authTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Create Account Button
                Button(
                    onClick = {
                        if (validateInputs()) {
                            onSignUpSuccessWithDetails(username.trim(), email.trim(), password.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        text = stringResource(R.string.create_account),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Back Button
                OutlinedButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = stringResource(R.string.go_back),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (isGoogleAvailable && !isLoading) {
                AuthGoogleSignIn(
                    text = " or sign up with ",
                    onGoogleSignInClick = onGoogleSignInClick
                )
            }
        }
    }
}
