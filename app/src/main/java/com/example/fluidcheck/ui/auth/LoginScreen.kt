package com.example.fluidcheck.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.*

@Composable
fun LoginScreen(
    onSignInClick: (String, String) -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    isGoogleAvailable: Boolean = true,
    isLoading: Boolean = false
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var identifierError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current

    val emptyIdentifierErr = stringResource(R.string.error_empty_identifier)
    val emptyPasswordErr = stringResource(R.string.error_empty_password)

    fun validateInputs(): Boolean {
        var isValid = true

        if (identifier.isBlank()) {
            identifierError = emptyIdentifierErr
            isValid = false
        } else {
            identifierError = null
        }

        if (password.isBlank()) {
            passwordError = emptyPasswordErr
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = AppIcons.AppLogo),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Smart Fluid Intake Tracker", 
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Glass Form Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.login_welcome),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Username/Email Field
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { 
                            identifier = it 
                            if (identifierError != null) identifierError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.username_label), color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(AppIcons.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) },
                        isError = identifierError != null,
                        supportingText = {
                            if (identifierError != null) {
                                Text(text = identifierError!!, color = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            errorBorderColor = Color.White,
                            errorSupportingTextColor = Color.White,
                            disabledBorderColor = Color.White.copy(alpha = 0.1f),
                            disabledTextColor = Color.White.copy(alpha = 0.5f),
                            disabledLeadingIconColor = Color.White.copy(alpha = 0.3f),
                            disabledPlaceholderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            if (passwordError != null) passwordError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.password_label), color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(AppIcons.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                                Icon(imageVector = image, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                            }
                        },
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(text = passwordError!!, color = Color.White)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            errorBorderColor = Color.White,
                            errorSupportingTextColor = Color.White,
                            disabledBorderColor = Color.White.copy(alpha = 0.1f),
                            disabledTextColor = Color.White.copy(alpha = 0.5f),
                            disabledLeadingIconColor = Color.White.copy(alpha = 0.3f),
                            disabledPlaceholderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (validateInputs()) {
                                    focusManager.clearFocus()
                                    onSignInClick(identifier, password)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        // Sign In Button
                        Button(
                            onClick = {
                                if (validateInputs()) {
                                    focusManager.clearFocus()
                                    onSignInClick(identifier, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.sign_in),
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sign Up Button
                        OutlinedButton(
                            onClick = onSignUpClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.sign_up),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (isGoogleAvailable && !isLoading) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Separator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.2f)
                            )
                            Text(
                                text = " or sign in with ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Circular Google Icon - NO BORDER
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onGoogleSignInClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = AppIcons.GoogleLogo),
                                contentDescription = "Sign in with Google",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
