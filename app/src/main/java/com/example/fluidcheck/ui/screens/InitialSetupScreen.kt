package com.example.fluidcheck.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.fluidcheck.BuildConfig
import com.example.fluidcheck.R
import com.example.fluidcheck.ai.GeminiCoach
import com.example.fluidcheck.model.DEFAULT_QUICK_ADD_CONFIGS
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.ui.theme.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

import com.example.fluidcheck.util.MeasurementPreferences
import com.example.fluidcheck.util.VolumeUnit
import com.example.fluidcheck.util.WeightUnit
import com.example.fluidcheck.util.HeightUnit
import com.example.fluidcheck.util.MeasurementUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupScreen(
    onComplete: (UserRecord, Int?, MeasurementPreferences) -> Unit
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

    var selectedVolumeUnit by remember { mutableStateOf(VolumeUnit.METRIC) }
    var selectedWeightUnit by remember { mutableStateOf(WeightUnit.METRIC) }
    var selectedHeightUnit by remember { mutableStateOf(HeightUnit.METRIC) }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val sexOptions = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy", "Humid", "Hot", "Cold", "Dry")
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    // Task 11.8: Custom Focus Requesters
    val weightFocus = remember { FocusRequester() }
    val heightFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }

    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
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

                    Text(
                        text = "Measurement System",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MeasurementSystemCard(
                            title = "Metric",
                            subtitle = "ml, kg, cm",
                            selected = selectedVolumeUnit == VolumeUnit.METRIC && selectedWeightUnit == WeightUnit.METRIC && selectedHeightUnit == HeightUnit.METRIC,
                            onClick = { 
                                selectedVolumeUnit = VolumeUnit.METRIC
                                selectedWeightUnit = WeightUnit.METRIC
                                selectedHeightUnit = HeightUnit.METRIC
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        MeasurementSystemCard(
                            title = "Imperial",
                            subtitle = "oz, lbs, ft",
                            selected = selectedVolumeUnit == VolumeUnit.IMPERIAL && selectedWeightUnit == WeightUnit.IMPERIAL && selectedHeightUnit == HeightUnit.IMPERIAL,
                            onClick = { 
                                selectedVolumeUnit = VolumeUnit.IMPERIAL
                                selectedWeightUnit = WeightUnit.IMPERIAL
                                selectedHeightUnit = HeightUnit.IMPERIAL
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

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
                            ResponsiveEditField(
                                label = MeasurementUtils.weightLabel(context, selectedWeightUnit), 
                                value = weight, 
                                onValueChange = { weight = it }, 
                                icon = AppIcons.Scale, 
                                placeholder = inputPlaceholder,
                                modifier = Modifier.focusRequester(weightFocus),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ResponsiveEditField(
                                label = MeasurementUtils.heightLabel(context, selectedHeightUnit), 
                                value = height, 
                                onValueChange = { height = it }, 
                                icon = AppIcons.Height, 
                                placeholder = inputPlaceholder,
                                modifier = Modifier.focusRequester(heightFocus),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { ageFocus.requestFocus() })
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ResponsiveEditField(
                                label = stringResource(R.string.age_label), 
                                value = age, 
                                onValueChange = { age = it }, 
                                icon = AppIcons.Age, 
                                placeholder = inputPlaceholder,
                                modifier = Modifier.focusRequester(ageFocus),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
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

                    val isFormEmpty = weight.isBlank() && height.isBlank() && age.isBlank() && 
                                      sex == selectionPlaceholder && activity == selectionPlaceholder && environment == selectionPlaceholder

                    Button(
                        onClick = {
                            onComplete(
                                UserRecord(
                                    weight = MeasurementUtils.convertWeightToKg(weight.trim(), selectedWeightUnit), 
                                    height = MeasurementUtils.convertHeightToCm(height.trim(), selectedHeightUnit), 
                                    age = age.trim(), 
                                    sex = if (sex == selectionPlaceholder) "" else sex, 
                                    activity = if (activity == selectionPlaceholder) "" else activity, 
                                    environment = if (environment == selectionPlaceholder) "" else environment, 
                                    setupCompleted = true,
                                    quickAddConfig = DEFAULT_QUICK_ADD_CONFIGS
                                ),
                                3000,
                                MeasurementPreferences(selectedVolumeUnit, selectedWeightUnit, selectedHeightUnit)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        enabled = !isFormEmpty
                    ) {
                        Text(stringResource(R.string.finish_setup), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            // Skip for now should still proceed with default, 
                            // but we can decide if it saves current inputs.
                            // Based on Turn 285, "if there are inputs... it is stored" applied to the finish path.
                            // We'll keep Skip for now as the pure default path.
                            onComplete(UserRecord(setupCompleted = true, quickAddConfig = DEFAULT_QUICK_ADD_CONFIGS), 3000, MeasurementPreferences())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        @Suppress("DEPRECATION")
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
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
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
            modifier = modifier.fillMaxWidth().height(58.dp),
            placeholder = { 
                @Suppress("DEPRECATION")
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
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
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
                            @Suppress("DEPRECATION")
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

@Composable
fun MeasurementSystemCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PrimaryBlue.copy(alpha = 0.1f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) PrimaryBlue else Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, color = if (selected) PrimaryBlue else TextDark)
            Text(text = subtitle, fontSize = 12.sp, color = MutedForeground)
        }
    }
}
