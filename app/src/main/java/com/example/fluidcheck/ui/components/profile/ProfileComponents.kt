package com.example.fluidcheck.ui.components.profile

import com.example.fluidcheck.model.UserRecord
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@Composable
fun ProfileSettingsContainer(
    userId: String,
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    profilePhotoModel: Any? = null,
    onEditPhoto: () -> Unit,
    confirmPasswordError: String? = null,
    enabled: Boolean,
    isGoogleUser: Boolean = false,
    emailVerified: Boolean = false,
    onVerifyEmail: () -> Unit = {}
) {
    val isGuest = userId == "GUEST"
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue.copy(alpha = 0.2f))
                    ) {
                        if (profilePhotoModel != null) {
                            coil.compose.SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profilePhotoModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.profile_photo_label),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                    }
                                },
                                error = {
                                    Icon(
                                        imageVector = AppIcons.PersonOutline,
                                        contentDescription = null,
                                        modifier = Modifier.padding(24.dp),
                                        tint = PrimaryBlue
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = AppIcons.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.padding(24.dp),
                                tint = PrimaryBlue
                            )
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = { if (enabled) onEditPhoto() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        containerColor = if (enabled) PrimaryBlue else Color.Gray,
                        contentColor = Color.White
                    ) {
                        Icon(AppIcons.Camera, contentDescription = stringResource(R.string.change_photo_label), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isGuest) {
                EditField(
                    label = stringResource(R.string.username_label),
                    value = username,
                    onValueChange = onUsernameChange,
                    icon = AppIcons.PersonOutline,
                    enabled = enabled && !isGoogleUser,
                    helperText = if (isGoogleUser) stringResource(R.string.google_username_msg) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                 EditField(
                    label = stringResource(R.string.email_label),
                    value = email,
                    onValueChange = onEmailChange,
                    icon = AppIcons.Email,
                    enabled = enabled && !isGoogleUser,
                    helperText = if (isGoogleUser) stringResource(R.string.google_email_msg) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (isGoogleUser) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                        onDone = { focusManager.clearFocus() }
                    )
                )

                if (!isGoogleUser && email.isNotEmpty()) {
                    if (emailVerified) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.verified_label), tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.verified_label), color = SuccessGreen, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
                        }
                    } else {
                        TextButton(
                            onClick = onVerifyEmail,
                            modifier = Modifier.padding(top = 4.dp, start = 0.dp),
                            enabled = enabled,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(stringResource(R.string.verify_email_label), color = PrimaryBlue, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
                
                if (!isGoogleUser) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EditField(
                        label = stringResource(R.string.new_password_label),
                        value = password,
                        onValueChange = onPasswordChange,
                        icon = AppIcons.Lock,
                        isPassword = true,
                        enabled = enabled,
                        placeholder = stringResource(R.string.password_keep_current_placeholder),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (password.isEmpty()) ImeAction.Done else ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                    
                    if (password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        EditField(
                            label = stringResource(R.string.confirm_password_label),
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            icon = AppIcons.Lock,
                            isPassword = true,
                            enabled = enabled,
                            error = confirmPasswordError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.google_password_msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsContainer(
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    sex: String, onSexChange: (String) -> Unit,
    activity: String, onActivityChange: (String) -> Unit,
    environment: String, onEnvironmentChange: (String) -> Unit,
    enabled: Boolean
) {
    var sexExpanded by remember { mutableStateOf(false) }
    var actExpanded by remember { mutableStateOf(false) }
    var envExpanded by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val weightFocus = remember { FocusRequester() }
    val heightFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }

    val sexOptions = listOf(stringResource(R.string.sex_male), stringResource(R.string.sex_female))
    val activityLevels = listOf(
        stringResource(R.string.activity_sedentary),
        stringResource(R.string.activity_light),
        stringResource(R.string.activity_moderate),
        stringResource(R.string.activity_very),
        stringResource(R.string.activity_extra)
    )
    val weatherOptions = listOf(
        stringResource(R.string.env_sunny),
        stringResource(R.string.env_cloudy),
        stringResource(R.string.env_rainy),
        stringResource(R.string.env_humid),
        stringResource(R.string.env_hot),
        stringResource(R.string.env_cold),
        stringResource(R.string.env_dry)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.personal_records_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(
                        label = stringResource(R.string.weight_kg_label), 
                        value = weight, 
                        onValueChange = onWeightChange, 
                        icon = AppIcons.Scale, 
                        enabled = enabled,
                        modifier = Modifier.focusRequester(weightFocus),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    EditField(
                        label = stringResource(R.string.height_cm_label), 
                        value = height, 
                        onValueChange = { onHeightChange(it) }, 
                        icon = AppIcons.Height, 
                        enabled = enabled,
                        modifier = Modifier.focusRequester(heightFocus),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { ageFocus.requestFocus() })
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(
                        label = stringResource(R.string.age_label), 
                        value = age, 
                        onValueChange = onAgeChange, 
                        icon = AppIcons.Age, 
                        enabled = enabled,
                        modifier = Modifier.focusRequester(ageFocus),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = stringResource(R.string.sex_label),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = TextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = sexExpanded && enabled,
                            onExpandedChange = { if (enabled) sexExpanded = !sexExpanded }
                        ) {
                            OutlinedTextField(
                                value = sex,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                leadingIcon = { Icon(AppIcons.Gender, contentDescription = null, tint = PrimaryBlue) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                enabled = enabled
                            )
                            ExposedDropdownMenu(
                                expanded = sexExpanded && enabled,
                                onDismissRequest = { sexExpanded = false }
                            ) {
                                sexOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onSexChange(option)
                                            sexExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.activity_level_label),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = actExpanded && enabled,
                onExpandedChange = { if (enabled) actExpanded = !actExpanded }
            ) {
                OutlinedTextField(
                    value = activity,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    leadingIcon = { Icon(AppIcons.Activity, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    enabled = enabled
                )
                ExposedDropdownMenu(
                    expanded = actExpanded && enabled,
                    onDismissRequest = { actExpanded = false }
                ) {
                    activityLevels.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onActivityChange(option)
                                actExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.environment_dropdown_label),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = envExpanded && enabled,
                onExpandedChange = { if (enabled) envExpanded = !envExpanded }
            ) {
                OutlinedTextField(
                    value = environment,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = envExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    leadingIcon = { Icon(AppIcons.Weather, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    enabled = enabled
                )
                ExposedDropdownMenu(
                    expanded = envExpanded && enabled,
                    onDismissRequest = { envExpanded = false }
                ) {
                    weatherOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onEnvironmentChange(option)
                                envExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    placeholder: String = "",
    error: String? = null,
    helperText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = TextDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (isPassword) {
                    val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            readOnly = readOnly,
            enabled = enabled,
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(text = error, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                } else if (helperText != null) {
                    Text(text = helperText, color = MutedForeground, style = MaterialTheme.typography.bodySmall)
                }
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}
