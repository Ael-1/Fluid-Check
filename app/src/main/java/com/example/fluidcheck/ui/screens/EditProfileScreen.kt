package com.example.fluidcheck.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.ui.theme.*
import kotlinx.coroutines.launch

private const val ADMIN_EMAIL = "admin@fluidcheck.ai"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userId: String,
    username: String,
    isAdminMode: Boolean = false,
    repository: UserPreferencesRepository,
    firestoreRepository: FirestoreRepository = remember { FirestoreRepository() },
    authRepository: AuthRepository = remember { AuthRepository() },
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load initial data using userId - Wrap in remember so the flow isn't recreated on every recomposition
    val userRecordFlow = remember(userId) { firestoreRepository.getUserRecordFlow(userId) }
    val currentRecord by userRecordFlow.collectAsState(initial = null)
    
    // Core administrative check
    val isPrimaryAdmin = currentRecord?.email == ADMIN_EMAIL
    
    // Show personal records ONLY if not primary admin AND not currently in admin mode
    val showPersonalRecords = !isPrimaryAdmin && !isAdminMode
    
    // States for profile settings
    var editableUsername by remember(currentRecord) { mutableStateOf(currentRecord?.username ?: username) }
    var editableEmail by remember(currentRecord) { mutableStateOf(currentRecord?.email ?: "") }
    var editablePassword by remember { mutableStateOf("") }
    var editableConfirmPassword by remember { mutableStateOf("") }
    var reauthPassword by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // States for personal records
    var weight by remember(currentRecord) { mutableStateOf(currentRecord?.weight ?: "") }
    var height by remember(currentRecord) { mutableStateOf(currentRecord?.height ?: "") }
    var age by remember(currentRecord) { mutableStateOf(currentRecord?.age ?: "") }
    
    val placeholder = "Please select..."
    var sex by remember(currentRecord) { mutableStateOf(if (currentRecord?.sex?.isEmpty() == true) placeholder else currentRecord?.sex ?: placeholder) }
    var activity by remember(currentRecord) { mutableStateOf(if (currentRecord?.activity?.isEmpty() == true) placeholder else currentRecord?.activity ?: placeholder) }
    var environment by remember(currentRecord) { mutableStateOf(if (currentRecord?.environment?.isEmpty() == true) placeholder else currentRecord?.environment ?: placeholder) }

    val mismatchPasswordErr = stringResource(R.string.error_password_mismatch)
    var showError by remember { mutableStateOf(false) }

    fun hasChanges(): Boolean {
        val record = currentRecord ?: UserRecord()
        return editableUsername != (record.username.ifEmpty { username }) ||
               editableEmail != record.email ||
               editablePassword.isNotEmpty() ||
               weight != record.weight ||
               height != record.height ||
               age != record.age ||
               sex != (if (record.sex.isEmpty()) placeholder else record.sex) ||
               activity != (if (record.activity.isEmpty()) placeholder else record.activity) ||
               environment != (if (record.environment.isEmpty()) placeholder else record.environment)
    }

    val onAttemptBack = {
        if (hasChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = !isLoading) {
        onAttemptBack()
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Are you sure you want to discard them and go back?") },
            confirmButton = {
                TextButton(onClick = { 
                    showUnsavedChangesDialog = false
                    onBack()
                }) {
                    Text("DISCARD", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("KEEP EDITING", color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = { showReauthDialog = false },
            title = { Text("Re-authentication Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please enter your current password to confirm changes to your email or password.")
                    Spacer(modifier = Modifier.height(16.dp))
                    EditField(
                        label = "Current Password",
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        icon = AppIcons.Lock,
                        isPassword = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = authRepository.reauthenticate(reauthPassword)
                            if (result.isSuccess) {
                                showReauthDialog = false
                                showSaveDialog = true
                            } else {
                                snackbarHostState.showSnackbar("Invalid password. Please try again.")
                            }
                            isLoading = false
                        }
                    }
                ) {
                    Text("CONFIRM", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReauthDialog = false }) {
                    Text("CANCEL", color = TextDark)
                }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showSaveDialog = false },
            title = { Text(text = stringResource(R.string.save_changes), fontWeight = FontWeight.Bold) },
            text = { 
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Text(text = "Are you sure you want to save the changes you've made?")
                }
            },
            confirmButton = {
                if (!isLoading) {
                    @Suppress("DEPRECATION")
                    TextButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                
                                // 1. Update Username if changed
                                if (editableUsername != (currentRecord?.username ?: "")) {
                                    // Check if the new username is already taken by someone else
                                    if (!firestoreRepository.isUsernameAvailable(editableUsername)) {
                                        snackbarHostState.showSnackbar("This username is already taken. Please try another one.")
                                        isLoading = false
                                        return@launch
                                    }
                                    
                                    val userResult = firestoreRepository.updateUsername(userId, currentRecord?.username ?: "", editableUsername)
                                    if (userResult.isFailure) {
                                        snackbarHostState.showSnackbar(userResult.exceptionOrNull()?.message ?: "Error updating username")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // 2. Update Email if changed
                                if (editableEmail != (currentRecord?.email ?: "")) {
                                    val emailResult = authRepository.updateEmail(editableEmail)
                                    if (emailResult.isFailure) {
                                        snackbarHostState.showSnackbar("Error updating email. You may need to sign in again.")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // 3. Update Password if provided
                                if (editablePassword.isNotEmpty()) {
                                    val passResult = authRepository.updatePassword(editablePassword)
                                    if (passResult.isFailure) {
                                        snackbarHostState.showSnackbar("Error updating password.")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // 4. Save User Record
                                val newRecord = (currentRecord ?: UserRecord()).copy(
                                    uid = userId,
                                    username = editableUsername,
                                    email = editableEmail,
                                    weight = weight,
                                    height = height,
                                    age = age,
                                    sex = sex,
                                    activity = activity,
                                    environment = environment,
                                    setupCompleted = true
                                )
                                val saveResult = firestoreRepository.saveUserRecord(userId, newRecord)
                                
                                if (saveResult.isSuccess) {
                                    repository.saveUserRecord(userId, newRecord)
                                    isLoading = false
                                    showSaveDialog = false
                                    onBack()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to sync profile to cloud. Please check your internet and try again.")
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm).uppercase(), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isLoading) {
                    @Suppress("DEPRECATION")
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.cancel), color = TextDark)
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onAttemptBack, enabled = !isLoading) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Container
            ProfileSettingsContainer(
                userId = userId,
                username = editableUsername,
                onUsernameChange = { editableUsername = it },
                email = editableEmail,
                onEmailChange = { editableEmail = it },
                password = editablePassword,
                onPasswordChange = { 
                    editablePassword = it 
                    if (it.isEmpty()) {
                        editableConfirmPassword = ""
                        confirmPasswordError = null
                    }
                },
                confirmPassword = editableConfirmPassword,
                onConfirmPasswordChange = { 
                    editableConfirmPassword = it 
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                confirmPasswordError = confirmPasswordError,
                enabled = !isLoading
            )

            if (showPersonalRecords) {
                Spacer(modifier = Modifier.height(24.dp))

                if (showError) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "Please fill in all fields and selections.",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Personal Records Container
                PersonalRecordsContainer(
                    weight = weight, onWeightChange = { weight = it },
                    height = height, onHeightChange = { height = it },
                    age = age, onAgeChange = { age = it },
                    sex = sex, onSexChange = { sex = it },
                    activity = activity, onActivityChange = { activity = it },
                    environment = environment, onEnvironmentChange = { environment = it },
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Button(
                    onClick = {
                        if (showPersonalRecords && (weight.isBlank() || height.isBlank() || age.isBlank() ||
                            sex == placeholder || activity == placeholder || environment == placeholder)) {
                            showError = true
                        } else {
                            showError = false
                            
                            // Check password match
                            if (editablePassword.isNotEmpty() && editablePassword != editableConfirmPassword) {
                                confirmPasswordError = mismatchPasswordErr
                                scope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                                return@Button
                            } else {
                                confirmPasswordError = null
                            }

                            // Check if sensitive changes require reauth
                            if (editableEmail != (currentRecord?.email ?: "") || editablePassword.isNotEmpty()) {
                                showReauthDialog = true
                            } else {
                                showSaveDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

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
    confirmPasswordError: String? = null,
    enabled: Boolean
) {
    val isGuest = userId == "GUEST"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Photo Edit
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = AppIcons.PersonOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = PrimaryBlue
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = { if (enabled) { /* TODO: Implement Photo Selection */ } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        containerColor = if (enabled) PrimaryBlue else Color.Gray,
                        contentColor = Color.White
                    ) {
                        Icon(AppIcons.Camera, contentDescription = "Change photo", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isGuest) {
                EditField(label = stringResource(R.string.username_label), value = username, onValueChange = onUsernameChange, icon = AppIcons.PersonOutline, enabled = enabled)
                Spacer(modifier = Modifier.height(16.dp))
                EditField(label = stringResource(R.string.email_label), value = email, onValueChange = onEmailChange, icon = AppIcons.Email, enabled = enabled)
                Spacer(modifier = Modifier.height(16.dp))
                EditField(label = "New Password", value = password, onValueChange = onPasswordChange, icon = AppIcons.Lock, isPassword = true, enabled = enabled, placeholder = "Leave blank to keep current")
                
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EditField(
                        label = stringResource(R.string.confirm_password_label),
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        icon = AppIcons.Lock,
                        isPassword = true,
                        enabled = enabled,
                        error = confirmPasswordError
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

    val sexOptions = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy", "Humid", "Hot", "Cold", "Dry")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.personal_records_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.weight_kg_label), value = weight, onValueChange = onWeightChange, icon = AppIcons.Scale, enabled = enabled)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.height_cm_label), value = height, onValueChange = { onHeightChange(it) }, icon = AppIcons.Height, enabled = enabled)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.age_label), value = age, onValueChange = onAgeChange, icon = AppIcons.Age, enabled = enabled)
                }
                Spacer(modifier = Modifier.width(16.dp))
                
                // Sex Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.sex_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
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
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
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
            
            // Activity Level Dropdown
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.activity_level_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
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
            
            // Environment Dropdown
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.environment_dropdown_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
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
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    placeholder: String = "",
    error: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        @Suppress("DEPRECATION")
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontSize = 14.sp) },
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (isPassword) {
                    val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            readOnly = readOnly,
            enabled = enabled,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(text = error, color = Color.Red, fontSize = 12.sp) }
            } else null
        )
    }
}
