package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    username: String,
    repository: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSaveDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Load initial data
    val currentRecord by repository.getUserRecord(username).collectAsState(initial = UserRecord())
    
    // States for personal records
    var weight by remember(currentRecord) { mutableStateOf(currentRecord.weight) }
    var height by remember(currentRecord) { mutableStateOf(currentRecord.height) }
    var age by remember(currentRecord) { mutableStateOf(currentRecord.age) }
    
    val placeholder = "Please select..."
    var sex by remember(currentRecord) { mutableStateOf(if (currentRecord.sex.isEmpty()) placeholder else currentRecord.sex) }
    var activity by remember(currentRecord) { mutableStateOf(if (currentRecord.activity.isEmpty()) placeholder else currentRecord.activity) }
    var environment by remember(currentRecord) { mutableStateOf(if (currentRecord.environment.isEmpty()) placeholder else currentRecord.environment) }

    var showError by remember { mutableStateOf(false) }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(text = stringResource(R.string.save_changes), fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to save the changes you've made?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.saveUserRecord(
                                username,
                                UserRecord(weight, height, age, sex, activity, environment)
                            )
                            showSaveDialog = false
                            onBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm).uppercase(), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            ProfileSettingsContainer(username)

            Spacer(modifier = Modifier.height(24.dp))

            if (showError) {
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
                placeholder = placeholder
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (weight.isBlank() || height.isBlank() || age.isBlank() || 
                        sex == placeholder || activity == placeholder || environment == placeholder) {
                        showError = true
                    } else {
                        showError = false
                        showSaveDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ProfileSettingsContainer(currentUsername: String) {
    var displayName by remember { mutableStateOf(currentUsername.replaceFirstChar { it.uppercase() }) }
    var username by remember { mutableStateOf(currentUsername) }
    var email by remember { mutableStateOf("$currentUsername@example.com") }
    var password by remember { mutableStateOf("********") }

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
                        onClick = { /* Change Photo */ },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    ) {
                        Icon(AppIcons.Camera, contentDescription = "Change photo", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            EditField(label = stringResource(R.string.display_name_label), value = displayName, onValueChange = { displayName = it }, icon = AppIcons.Badge)
            Spacer(modifier = Modifier.height(16.dp))
            EditField(label = stringResource(R.string.username_label), value = username, onValueChange = { username = it }, icon = AppIcons.PersonOutline)
            Spacer(modifier = Modifier.height(16.dp))
            EditField(label = stringResource(R.string.email_label), value = email, onValueChange = { email = it }, icon = AppIcons.Email)
            Spacer(modifier = Modifier.height(16.dp))
            EditField(label = stringResource(R.string.password_label), value = password, onValueChange = { password = it }, icon = AppIcons.Lock, isPassword = true)
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
    placeholder: String
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
            Text(
                text = stringResource(R.string.personal_records_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.weight_kg_label), value = weight, onValueChange = onWeightChange, icon = AppIcons.Scale)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.height_cm_label), value = height, onValueChange = onHeightChange, icon = AppIcons.Height)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.age_label), value = age, onValueChange = onAgeChange, icon = AppIcons.Age)
                }
                Spacer(modifier = Modifier.width(16.dp))
                
                // Sex Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = stringResource(R.string.sex_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = sexExpanded,
                            onExpandedChange = { sexExpanded = !sexExpanded }
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
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            ExposedDropdownMenu(
                                expanded = sexExpanded,
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
            Text(
                text = stringResource(R.string.activity_level_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = actExpanded,
                onExpandedChange = { actExpanded = !actExpanded }
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
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                ExposedDropdownMenu(
                    expanded = actExpanded,
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
            Text(
                text = stringResource(R.string.environment_dropdown_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = envExpanded,
                onExpandedChange = { envExpanded = !envExpanded }
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
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                ExposedDropdownMenu(
                    expanded = envExpanded,
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
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
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
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (isPassword) {
                    val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
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
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}
