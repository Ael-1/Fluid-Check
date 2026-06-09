package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.LocalDate
import java.time.ZoneId
import com.example.fluidcheck.repository.UserPreferencesRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AICoachScreen(
    userRecord: UserRecord?,
    onSetGoal: (Int) -> Unit,
    isConnected: Boolean = true,
    firestoreRepository: com.example.fluidcheck.repository.FirestoreRepository? = null,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences = com.example.fluidcheck.util.MeasurementPreferences(),
    // Hoisted states
    aiGoalWeight: String,
    onAiGoalWeightChange: (String) -> Unit,
    aiGoalHeight: String,
    onAiGoalHeightChange: (String) -> Unit,
    aiGoalAge: String,
    onAiGoalAgeChange: (String) -> Unit,
    aiGoalSex: String,
    onAiGoalSexChange: (String) -> Unit,
    aiGoalActivity: String,
    onAiGoalActivityChange: (String) -> Unit,
    aiGoalEnvironment: String,
    onAiGoalEnvironmentChange: (String) -> Unit,
    aiGoalIsLoading: Boolean,
    onAiGoalIsLoadingChange: (Boolean) -> Unit,
    aiGoalResultMl: String?,
    onAiGoalResultMlChange: (String?) -> Unit,
    aiAssessmentIsLoading: Boolean,
    onAiAssessmentIsLoadingChange: (Boolean) -> Unit,
    aiAssessmentResult: String?,
    onAiAssessmentResultChange: (String?) -> Unit,
    aiRecsPreferences: String,
    onAiRecsPreferencesChange: (String) -> Unit,
    aiRecsHabits: String,
    onAiRecsHabitsChange: (String) -> Unit,
    aiRecsIsLoading: Boolean,
    onAiRecsIsLoadingChange: (Boolean) -> Unit,
    aiRecsRecommendation: String?,
    onAiRecsRecommendationChange: (String?) -> Unit,
    mainScope: kotlinx.coroutines.CoroutineScope,
    locationAccessEnabled: Boolean = false,
    weatherGoalAdjustmentEnabled: Boolean = false,
    onToggleWeatherGoalAdjustment: (Boolean) -> Unit = {},
    userRole: String = "FREE USER",
    onPremiumFeatureClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val coach = remember { GeminiCoach(BuildConfig.GEMINI_API_KEY) }
    
    val allLogsFlow = remember(userRecord?.uid) {
        firestoreRepository?.getFluidLogsFlow(userRecord?.uid ?: "") ?: kotlinx.coroutines.flow.emptyFlow()
    }
    val allLogs by allLogsFlow.collectAsState(initial = emptyList())
    
    val logsLast14DaysStr = remember(allLogs) {
        val todayNow = LocalDate.now(ZoneId.of("GMT+8"))
        val datesList = (0..14).map { todayNow.minusDays(it.toLong()) }.reversed()
        val logsByDate = allLogs.groupBy { it.date }
        
        datesList.joinToString("\n") { date ->
            val dateStr = date.toString()
            val dayLogs = logsByDate[dateStr]
            if (dayLogs.isNullOrEmpty()) {
                "$dateStr: No logs recorded (User did not log any drinks today)"
            } else {
                dayLogs.joinToString("\n") { log ->
                    "${log.date} ${log.time}: ${com.example.fluidcheck.util.MeasurementUtils.formatVolume(context, log.amount, measurementPreferences.volume)} of ${log.type}"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SmartGoalSetterCard(
                coach = coach,
                userRecord = userRecord,
                onSetGoal = onSetGoal,
                isConnected = isConnected,
                weight = aiGoalWeight,
                onWeightChange = onAiGoalWeightChange,
                height = aiGoalHeight,
                onHeightChange = onAiGoalHeightChange,
                age = aiGoalAge,
                onAgeChange = onAiGoalAgeChange,
                sex = aiGoalSex,
                onSexChange = onAiGoalSexChange,
                activity = aiGoalActivity,
                onActivityChange = onAiGoalActivityChange,
                environment = aiGoalEnvironment,
                onEnvironmentChange = onAiGoalEnvironmentChange,
                isLoading = aiGoalIsLoading,
                onLoadingChange = onAiGoalIsLoadingChange,
                resultMl = aiGoalResultMl,
                onResultMlChange = onAiGoalResultMlChange,
                mainScope = mainScope,
                locationAccessEnabled = locationAccessEnabled,
                weatherGoalAdjustmentEnabled = weatherGoalAdjustmentEnabled,
                onToggleWeatherGoalAdjustment = onToggleWeatherGoalAdjustment,
                measurementPreferences = measurementPreferences,
                userRole = userRole,
                onPremiumFeatureClick = onPremiumFeatureClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            AIHydrationAssessmentCard(
                coach = coach,
                userRecord = userRecord,
                logsLast14DaysStr = logsLast14DaysStr,
                isConnected = isConnected,
                isLoading = aiAssessmentIsLoading,
                onLoadingChange = onAiAssessmentIsLoadingChange,
                assessmentResult = aiAssessmentResult,
                onAssessmentResultChange = onAiAssessmentResultChange,
                mainScope = mainScope,
                measurementPreferences = measurementPreferences,
                userRole = userRole,
                onPremiumFeatureClick = onPremiumFeatureClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            AIRecommendationsCard(
                coach = coach,
                isConnected = isConnected,
                preferences = aiRecsPreferences,
                onPreferencesChange = onAiRecsPreferencesChange,
                habits = aiRecsHabits,
                onHabitsChange = onAiRecsHabitsChange,
                isLoading = aiRecsIsLoading,
                onLoadingChange = onAiRecsIsLoadingChange,
                recommendation = aiRecsRecommendation,
                onRecommendationChange = onAiRecsRecommendationChange,
                mainScope = mainScope,
                measurementPreferences = measurementPreferences,
                userRole = userRole,
                onPremiumFeatureClick = onPremiumFeatureClick
            )

            AIDisclaimer()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartGoalSetterCard(
    coach: GeminiCoach,
    userRecord: UserRecord?,
    onSetGoal: (Int) -> Unit,
    isConnected: Boolean,
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    sex: String,
    onSexChange: (String) -> Unit,
    activity: String,
    onActivityChange: (String) -> Unit,
    environment: String,
    onEnvironmentChange: (String) -> Unit,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    resultMl: String?,
    onResultMlChange: (String?) -> Unit,
    mainScope: kotlinx.coroutines.CoroutineScope,
    locationAccessEnabled: Boolean,
    weatherGoalAdjustmentEnabled: Boolean,
    onToggleWeatherGoalAdjustment: (Boolean) -> Unit,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences = com.example.fluidcheck.util.MeasurementPreferences(),
    userRole: String = "FREE USER",
    onPremiumFeatureClick: () -> Unit = {}
) {
    val selectionPlaceholder = "Select..."
    val inputPlaceholder = "Input..."

    // Auto-populate from Firestore record
    LaunchedEffect(userRecord, measurementPreferences.volume) {
        userRecord?.let { record ->
            if (weight.isEmpty()) onWeightChange(com.example.fluidcheck.util.MeasurementUtils.convertWeightForDisplay(record.weight, measurementPreferences.weight))
            if (height.isEmpty()) onHeightChange(com.example.fluidcheck.util.MeasurementUtils.convertHeightForDisplay(record.height, measurementPreferences.height))
            if (age.isEmpty()) onAgeChange(record.age)
            if (sex == selectionPlaceholder && record.sex.isNotEmpty()) onSexChange(record.sex)
            if (activity == selectionPlaceholder && record.activity.isNotEmpty()) onActivityChange(record.activity)
            if (environment == selectionPlaceholder && record.environment.isNotEmpty()) onEnvironmentChange(record.environment)
        }
    }
    
    var sexExpanded by remember { mutableStateOf(false) }
    var actExpanded by remember { mutableStateOf(false) }
    var envExpanded by remember { mutableStateOf(false) }
    
    val sexOptions = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy", "Humid", "Hot", "Cold", "Dry")
    
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    
    // Task 11.8: Custom Focus Requesters
    val weightFocus = remember { FocusRequester() }
    val heightFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }
    
    val context = LocalContext.current

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Adjust,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.personalized_goals),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.ai_planning_subtitle),
                fontSize = 15.sp,
                color = MutedForeground,
                modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (showError) {
                Text(
                    text = "Please fill in all fields.",
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    CoachTextField(
                        value = weight, 
                        onValueChange = onWeightChange, 
                        label = com.example.fluidcheck.util.MeasurementUtils.weightLabel(context, measurementPreferences.weight), 
                        placeholder = inputPlaceholder,
                        modifier = Modifier.focusRequester(weightFocus),
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    CoachTextField(
                        value = height, 
                        onValueChange = onHeightChange, 
                        label = com.example.fluidcheck.util.MeasurementUtils.heightLabel(context, measurementPreferences.height), 
                        placeholder = inputPlaceholder,
                        modifier = Modifier.focusRequester(heightFocus),
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { ageFocus.requestFocus() })
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    CoachTextField(
                        value = age, 
                        onValueChange = onAgeChange, 
                        label = stringResource(R.string.age_label), 
                        placeholder = inputPlaceholder,
                        modifier = Modifier.focusRequester(ageFocus),
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    ResponsiveDropdownField(
                        label = stringResource(R.string.sex_label),
                        value = sex,
                        expanded = sexExpanded,
                        onExpandedChange = { sexExpanded = it },
                        options = sexOptions,
                        onSelect = { onSexChange(it); sexExpanded = false },
                        icon = null,
                        isCoachStyle = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ResponsiveDropdownField(
                        label = stringResource(R.string.activity_label),
                        value = activity,
                        expanded = actExpanded,
                        onExpandedChange = { actExpanded = it },
                        options = activityLevels,
                        onSelect = { onActivityChange(it); actExpanded = false },
                        icon = null,
                        isCoachStyle = true
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ResponsiveDropdownField(
                        label = stringResource(R.string.environment_label),
                        value = environment,
                        expanded = envExpanded,
                        onExpandedChange = { envExpanded = it },
                        options = weatherOptions,
                        onSelect = { onEnvironmentChange(it); envExpanded = false },
                        icon = null,
                        isCoachStyle = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (resultMl != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val formattedResult = try {
                        val parsed = resultMl.replace(" ml", "").trim().toInt()
                        com.example.fluidcheck.util.MeasurementUtils.formatVolume(context, parsed, measurementPreferences.volume)
                    } catch (e: Exception) { resultMl }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your Ideal Daily Intake: $formattedResult",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = {
                            val goal = resultMl?.filter { it.isDigit() }?.toIntOrNull()
                            if (goal != null) {
                                onSetGoal(goal)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Daily Goal", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (userRole == "FREE USER") {
                        onPremiumFeatureClick()
                        return@Button
                    }
                    if (!isConnected) {
                        showNoInternetDialog = true
                        return@Button
                    }
                    if (weight.isBlank() || height.isBlank() || age.isBlank() || 
                        sex == selectionPlaceholder || activity == selectionPlaceholder || environment == selectionPlaceholder) {
                        showError = true
                    } else {
                        showError = false
                        focusManager.clearFocus()
                        onLoadingChange(true)
                        mainScope.launch {
                            try {
                                val metricWeight = com.example.fluidcheck.util.MeasurementUtils.convertWeightToKg(weight.trim(), measurementPreferences.weight)
                                val metricHeight = com.example.fluidcheck.util.MeasurementUtils.convertHeightToCm(height.trim(), measurementPreferences.height)
                                val unitsInfo = "User uses ${measurementPreferences.volume.name} for volume, ${measurementPreferences.weight.name} for weight, ${measurementPreferences.height.name} for height."
                                val result = coach.calculateHydrationGoal(unitsInfo, metricWeight, metricHeight, age.trim(), sex, activity, environment)
                                onResultMlChange(result ?: "Could not calculate.")
                            } catch (e: Exception) {
                                onResultMlChange(null)
                                android.widget.Toast.makeText(context, "Error calculating goal: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                onLoadingChange(false)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(AppIcons.AICoach, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.calculate_goal), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider(color = Slate100, thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Clean: preferences loaded from hoisted parameters

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = AppIcons.Weather,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dynamic Weather Goal Adjustment",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Automatically adjust goals based on local temperature",
                            fontSize = 12.sp,
                            color = MutedForeground
                        )
                    }
                }
                Switch(
                    checked = weatherGoalAdjustmentEnabled,
                    onCheckedChange = { enabled ->
                        if (userRole == "FREE USER") {
                            onPremiumFeatureClick()
                        } else {
                            onToggleWeatherGoalAdjustment(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            
            if (!weatherGoalAdjustmentEnabled || !locationAccessEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calculations may be inaccurate if location services are turned off. You can enable them in Settings.",
                        fontSize = 13.sp,
                        color = ErrorRed,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AIHydrationAssessmentCard(
    coach: GeminiCoach,
    userRecord: UserRecord?,
    logsLast14DaysStr: String,
    isConnected: Boolean,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    assessmentResult: String?,
    onAssessmentResultChange: (String?) -> Unit,
    mainScope: kotlinx.coroutines.CoroutineScope,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences = com.example.fluidcheck.util.MeasurementPreferences(),
    userRole: String = "FREE USER",
    onPremiumFeatureClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showNoInternetDialog by remember { mutableStateOf(false) }

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.History,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Hydration Assessment",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Text(
                text = "Analyze your overall drink habits and get custom feedback",
                fontSize = 15.sp,
                color = MutedForeground,
                modifier = Modifier.padding(start = 40.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (assessmentResult != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = assessmentResult,
                        fontSize = 15.sp,
                        color = TextDark,
                        lineHeight = 22.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (userRole == "FREE USER") {
                        onPremiumFeatureClick()
                        return@Button
                    }
                    if (!isConnected) {
                        showNoInternetDialog = true
                        return@Button
                    }
                    if (logsLast14DaysStr.trim().isEmpty()) {
                        onAssessmentResultChange("You haven't logged any drinks in the last 14 days! Start logging your daily beverages on the Home screen so I can analyze your habits and provide personalized insights.")
                        return@Button
                    }
                    onLoadingChange(true)
                    val convertedWeight = com.example.fluidcheck.util.MeasurementUtils.convertWeightForDisplay(userRecord?.weight ?: "", measurementPreferences.weight)
                    val convertedHeight = com.example.fluidcheck.util.MeasurementUtils.convertHeightForDisplay(userRecord?.height ?: "", measurementPreferences.height)
                    val weightUnit = com.example.fluidcheck.util.MeasurementUtils.weightUnit(context, measurementPreferences.weight)
                    val heightUnit = com.example.fluidcheck.util.MeasurementUtils.heightUnit(context, measurementPreferences.height)
                    val profileStr = """
                        Username: ${userRecord?.username ?: "User"}
                        Weight: $convertedWeight $weightUnit
                        Height: $convertedHeight $heightUnit
                        Age: ${userRecord?.age ?: "Unknown"}
                        Sex: ${userRecord?.sex ?: "Unknown"}
                        Activity Level: ${userRecord?.activity ?: "Unknown"}
                        Environment: ${userRecord?.environment ?: "Unknown"}
                    """.trimIndent()
                    
                    mainScope.launch {
                        try {
                            val unitsInfo = "User uses ${measurementPreferences.volume.name} for volume, ${measurementPreferences.weight.name} for weight, ${measurementPreferences.height.name} for height."
                            val result = coach.analyzeHabitsAndRules(unitsInfo, logsLast14DaysStr, profileStr)
                            if (result != null) {
                                onAssessmentResultChange(result.first)
                                val repository = UserPreferencesRepository(context)
                                repository.setPredictiveReminders(userRecord?.uid ?: "", result.second)
                            } else {
                                onAssessmentResultChange("Habit analysis completed. Keep drinking water regularly to maintain consistency!")
                            }
                        } catch (e: Exception) {
                            onAssessmentResultChange("Unable to complete analysis. Let's aim to drink water throughout the day!")
                        } finally {
                            onLoadingChange(false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(AppIcons.AICoach, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze My Habits", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun AIRecommendationsCard(
    coach: GeminiCoach,
    isConnected: Boolean,
    preferences: String,
    onPreferencesChange: (String) -> Unit,
    habits: String,
    onHabitsChange: (String) -> Unit,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    recommendation: String?,
    onRecommendationChange: (String?) -> Unit,
    mainScope: kotlinx.coroutines.CoroutineScope,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences,
    userRole: String = "FREE USER",
    onPremiumFeatureClick: () -> Unit = {}
) {
    var showNoInternetDialog by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Psychology,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.ai_coach_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Text(
                text = stringResource(R.string.ai_recommendation_subtitle),
                fontSize = 15.sp,
                color = MutedForeground,
                modifier = Modifier.padding(start = 40.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(stringResource(R.string.preferences_prompt), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            CoachTextArea(
                value = preferences,
                onValueChange = onPreferencesChange,
                placeholder = "e.g., I like sparkling water...",
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.habits_prompt), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            CoachTextArea(
                value = habits,
                onValueChange = onHabitsChange,
                placeholder = "e.g., I usually drink coffee in the morning...",
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (recommendation != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row {
                        Icon(AppIcons.Lightbulb, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = recommendation,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (userRole == "FREE USER") {
                        onPremiumFeatureClick()
                        return@Button
                    }
                    if (!isConnected) {
                        showNoInternetDialog = true
                        return@Button
                    }
                    focusManager.clearFocus()
                    if (preferences.trim().isEmpty() && habits.trim().isEmpty()) {
                        onRecommendationChange("Please tell me a bit about your preferences or habits so I can give you personalized hydration tips! In the meantime, try to drink water consistently throughout the day and listen to your body's signals.")
                        return@Button
                    }
                    onLoadingChange(true)
                    mainScope.launch {
                        try {
                            val unitsInfo = "User uses ${measurementPreferences.volume.name} for volume, ${measurementPreferences.weight.name} for weight, ${measurementPreferences.height.name} for height."
                            val result = coach.getRecommendation(unitsInfo, preferences.trim(), habits.trim())
                            onRecommendationChange(result ?: "Could not get recommendation.")
                        } catch (e: Exception) {
                            onRecommendationChange(null)
                            android.widget.Toast.makeText(context, "Error getting recommendation: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        } finally {
                            onLoadingChange(false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(AppIcons.AICoach, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.get_recommendation), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun AIDisclaimer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = AppIcons.Info,
            contentDescription = null,
            tint = MutedForeground,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.ai_disclaimer),
            fontSize = 14.sp,
            color = MutedForeground,
            textAlign = TextAlign.Start,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun CoachTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.SemiBold, 
            color = TextDark,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 16.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
            keyboardActions = keyboardActions,
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }
}

@Composable
fun CoachTextArea(
    value: String, 
    onValueChange: (String) -> Unit, 
    placeholder: String,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 16.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            unfocusedContainerColor = Slate50,
            focusedContainerColor = Color.White
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
    )
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
    icon: ImageVector?,
    isCoachStyle: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
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
                    unfocusedContainerColor = Slate50,
                    focusedContainerColor = Color.White
                ),
                leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp)) } },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.exposedDropdownSize()
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
@Composable
fun NoInternetDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No Internet Connection", fontWeight = FontWeight.Bold) },
        text = { Text("AI Coach features require an active internet connection to process your requests. Please check your connectivity and try again.") },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}
