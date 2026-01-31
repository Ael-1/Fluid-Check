package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.BuildConfig
import com.example.fluidcheck.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

@Composable
fun AICoachScreen() {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    
    // Initialize the Gemini model using the API key from BuildConfig
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemma-3-12b-it",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
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

            // 6.1 Smart Goal Setter Card
            SmartGoalSetterCard(generativeModel)

            Spacer(modifier = Modifier.height(24.dp))

            // 6.2 AI Recommendations Card
            AIRecommendationsCard(generativeModel)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SmartGoalSetterCard(generativeModel: GenerativeModel) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var environment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resultMl by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Adjust,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Personalized Goals",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Text(
                text = "AI-driven hydration planning.",
                fontSize = 14.sp,
                color = MutedForeground,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                CoachTextField(
                    value = weight, 
                    onValueChange = { weight = it }, 
                    label = "Weight (kg)", 
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                )
                Spacer(modifier = Modifier.width(12.dp))
                CoachTextField(
                    value = height, 
                    onValueChange = { height = it }, 
                    label = "Height (cm)", 
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CoachTextField(
                    value = age, 
                    onValueChange = { age = it }, 
                    label = "Age", 
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                )
                Spacer(modifier = Modifier.width(12.dp))
                CoachTextField(
                    value = gender, 
                    onValueChange = { gender = it }, 
                    label = "Gender", 
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CoachTextField(
                    value = activity, 
                    onValueChange = { activity = it }, 
                    label = "Activity", 
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                )
                Spacer(modifier = Modifier.width(12.dp))
                CoachTextField(
                    value = environment, 
                    onValueChange = { environment = it }, 
                    label = "Environment", 
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (resultMl != null) {
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
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    // Removed the buggy validation check that was blocking legitimate API keys
                    isLoading = true
                    scope.launch {
                        try {
                            val prompt = "Based on: Weight $weight kg, Height $height cm, Age $age, Gender $gender, Activity level $activity, and Environment $environment. Calculate the ideal daily water intake in ml. Return ONLY the number followed by 'ml'."
                            val response = generativeModel.generateContent(prompt)
                            resultMl = response.text?.trim() ?: "Could not calculate"
                        } catch (e: Exception) {
                            resultMl = "Error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculate Ideal Intake", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AIRecommendationsCard(generativeModel: GenerativeModel) {
    var preferences by remember { mutableStateOf("") }
    var habits by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "AI Hydration Coach",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Get a smart suggestion on what to drink next.",
                fontSize = 14.sp,
                color = MutedForeground,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Your drink preferences (optional)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            CoachTextArea(
                value = preferences,
                onValueChange = { preferences = it },
                placeholder = "e.g., I like sparkling water, I dislike sugary drinks...",
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Your drinking habits (optional)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            CoachTextArea(
                value = habits,
                onValueChange = { habits = it },
                placeholder = "e.g., I usually drink coffee in the morning, I forget to drink water in the afternoon...",
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (recommendation != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = recommendation!!,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    // Removed the buggy validation check that was blocking legitimate API keys
                    isLoading = true
                    scope.launch {
                        try {
                            val prompt = "As a hydration coach, give a short, personalized recommendation (max 2 sentences) based on these preferences: '$preferences' and habits: '$habits'. Make it encouraging."
                            val response = generativeModel.generateContent(prompt)
                            recommendation = response.text?.trim() ?: "Could not get recommendation"
                        } catch (e: Exception) {
                            recommendation = "Error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Recommendation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CoachTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, fontSize = 12.sp, color = MutedForeground) },
        modifier = modifier
            .height(52.dp)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            disabledContainerColor = Color(0xFFF8FAFC),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions
    )
}

@Composable
fun CoachTextArea(
    value: String, 
    onValueChange: (String) -> Unit, 
    placeholder: String,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = MutedForeground) },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            disabledContainerColor = Color(0xFFF8FAFC),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions
    )
}
