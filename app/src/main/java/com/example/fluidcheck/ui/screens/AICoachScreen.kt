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
import com.example.fluidcheck.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AICoachScreen(onSetGoal: (Int) -> Unit) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val coach = remember { GeminiCoach(BuildConfig.GEMINI_API_KEY) }

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

            SmartGoalSetterCard(coach, onSetGoal)

            Spacer(modifier = Modifier.height(24.dp))

            AIRecommendationsCard(coach)

            AIDisclaimer()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartGoalSetterCard(coach: GeminiCoach, onSetGoal: (Int) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    
    val selectionPlaceholder = "Select..."
    val inputPlaceholder = "Input..."
    
    var sex by remember { mutableStateOf(selectionPlaceholder) }
    var activity by remember { mutableStateOf(selectionPlaceholder) }
    var environment by remember { mutableStateOf(selectionPlaceholder) }
    
    var isLoading by remember { mutableStateOf(false) }
    var resultMl by remember { mutableStateOf<String?>(null) }
    
    var sexExpanded by remember { mutableStateOf(false) }
    var actExpanded by remember { mutableStateOf(false) }
    var envExpanded by remember { mutableStateOf(false) }
    
    val sexOptions = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy", "Humid", "Hot", "Cold", "Dry")
    
    var showError by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                        onValueChange = { weight = it }, 
                        label = stringResource(R.string.weight_label), 
                        placeholder = inputPlaceholder,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    CoachTextField(
                        value = height, 
                        onValueChange = { height = it }, 
                        label = stringResource(R.string.height_label), 
                        placeholder = inputPlaceholder,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    CoachTextField(
                        value = age, 
                        onValueChange = { age = it }, 
                        label = stringResource(R.string.age_label), 
                        placeholder = inputPlaceholder,
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
                        onSelect = { sex = it; sexExpanded = false },
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
                        onSelect = { activity = it; actExpanded = false },
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
                        onSelect = { environment = it; envExpanded = false },
                        icon = null,
                        isCoachStyle = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (resultMl != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your Ideal Daily Intake: $resultMl",
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
                    if (weight.isBlank() || height.isBlank() || age.isBlank() || 
                        sex == selectionPlaceholder || activity == selectionPlaceholder || environment == selectionPlaceholder) {
                        showError = true
                    } else {
                        showError = false
                        focusManager.clearFocus()
                        isLoading = true
                        scope.launch {
                            val result = coach.calculateHydrationGoal(weight, height, age, sex, activity, environment)
                            resultMl = result ?: "Could not calculate."
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(AppIcons.AICoach, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.calculate_goal), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun AIRecommendationsCard(coach: GeminiCoach) {
    var preferences by remember { mutableStateOf("") }
    var habits by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                onValueChange = { preferences = it },
                placeholder = "e.g., I like sparkling water...",
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.habits_prompt), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            CoachTextArea(
                value = habits,
                onValueChange = { habits = it },
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
                            text = recommendation!!,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true
                    scope.launch {
                        val result = coach.getRecommendation(preferences, habits)
                        recommendation = result ?: "Could not get recommendation."
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
            unfocusedContainerColor = Color(0xFFF8FAFC),
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
                    unfocusedContainerColor = Color(0xFFF8FAFC),
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
