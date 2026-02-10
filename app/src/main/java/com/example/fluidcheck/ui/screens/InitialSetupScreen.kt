package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.BuildConfig
import com.example.fluidcheck.R
import com.example.fluidcheck.ai.GeminiCoach
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupScreen(
    onComplete: (UserRecord, Int?) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    
    val selectionPlaceholder = "Select..."
    val inputPlaceholder = "Input..."
    
    var sex by remember { mutableStateOf(selectionPlaceholder) }
    var activity by remember { mutableStateOf(selectionPlaceholder) }
    var environment by remember { mutableStateOf(selectionPlaceholder) }
    
    var sexExpanded by remember { mutableStateOf(false) }
    var actExpanded by remember { mutableStateOf(false) }
    var envExpanded by remember { mutableStateOf(false) }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val sexOptions = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy", "Humid", "Hot", "Cold", "Dry")
    val scrollState = rememberScrollState()

    val coach = remember { GeminiCoach(BuildConfig.GEMINI_API_KEY) }
    val scope = rememberCoroutineScope()
    var isLoadingGoal by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf<String?>(null) }

    if (showGoalDialog != null) {
        AlertDialog(
            onDismissRequest = { /* Force choice */ },
            title = { Text("Personalized Goal", fontWeight = FontWeight.Bold) },
            text = { Text("Based on your profile, the AI suggests a daily goal of ${showGoalDialog}ml. Would you like to set this as your daily goal?") },
            confirmButton = {
                Button(
                    onClick = {
                        val goal = showGoalDialog?.filter { it.isDigit() }?.toIntOrNull()
                        onComplete(
                            UserRecord(weight, height, age, sex, activity, environment),
                            goal
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Set as Daily Goal")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onComplete(
                            UserRecord(weight, height, age, sex, activity, environment),
                            3000 // Explicitly use 3000ml as requested
                        )
                    }
                ) {
                    Text("Use Default (3000ml)", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = stringResource(R.string.welcome_setup_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.95f)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.personal_records_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "You can skip this setup and change these settings anytime later in the app.",
                        fontSize = 13.sp,
                        color = MutedForeground,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (showError) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ResponsiveEditField(label = stringResource(R.string.weight_kg_label), value = weight, onValueChange = { weight = it }, icon = AppIcons.Scale, placeholder = inputPlaceholder)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ResponsiveEditField(label = stringResource(R.string.height_cm_label), value = height, onValueChange = { height = it }, icon = AppIcons.Height, placeholder = inputPlaceholder)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ResponsiveEditField(label = stringResource(R.string.age_label), value = age, onValueChange = { age = it }, icon = AppIcons.Age, placeholder = inputPlaceholder)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Sex Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ResponsiveDropdownField(
                                label = stringResource(R.string.sex_label),
                                value = sex,
                                expanded = sexExpanded,
                                onExpandedChange = { sexExpanded = it },
                                options = sexOptions,
                                onSelect = { sex = it; sexExpanded = false },
                                icon = AppIcons.Gender
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    ResponsiveDropdownField(
                        label = stringResource(R.string.activity_level_label),
                        value = activity,
                        expanded = actExpanded,
                        onExpandedChange = { actExpanded = it },
                        options = activityLevels,
                        onSelect = { activity = it; actExpanded = false },
                        icon = AppIcons.Activity
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    ResponsiveDropdownField(
                        label = stringResource(R.string.environment_dropdown_label),
                        value = environment,
                        expanded = envExpanded,
                        onExpandedChange = { envExpanded = it },
                        options = weatherOptions,
                        onSelect = { environment = it; envExpanded = false },
                        icon = AppIcons.Weather
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (weight.isBlank() || height.isBlank() || age.isBlank() || 
                                sex == selectionPlaceholder || activity == selectionPlaceholder || environment == selectionPlaceholder) {
                                showError = true
                                errorMessage = "Please fill in all fields and selections."
                            } else {
                                showError = false
                                isLoadingGoal = true
                                scope.launch {
                                    val goal = coach.calculateHydrationGoal(weight, height, age, sex, activity, environment)
                                    isLoadingGoal = false
                                    if (goal != null) {
                                        showGoalDialog = goal
                                    } else {
                                        onComplete(
                                            UserRecord(weight, height, age, sex, activity, environment),
                                            3000
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        enabled = !isLoadingGoal
                    ) {
                        if (isLoadingGoal) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(R.string.finish_setup), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            onComplete(UserRecord(), 3000)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Skip for now",
                            color = MutedForeground,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ResponsiveEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            placeholder = { 
                Text(
                    text = placeholder, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp
                ) 
            },
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (isPassword) {
                    val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedContainerColor = Color.White
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
    icon: ImageVector
) {
    Column {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color.White
                ),
                leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp)) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .exposedDropdownSize()
                    .heightIn(max = 280.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = option, 
                                fontSize = 16.sp,
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis 
                            ) 
                        },
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}
