package com.example.fluidcheck.ui.components.admin

import com.example.fluidcheck.ui.admin.*
import com.example.fluidcheck.model.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.fluidcheck.R
import com.example.fluidcheck.model.ChartData
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.util.*
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.screens.TimeRangeTabs
import com.example.fluidcheck.ui.screens.DateNavigationBar
import com.example.fluidcheck.ui.screens.HydrationLineChart
import com.example.fluidcheck.ui.screens.getChartDataForRange
import com.example.fluidcheck.ui.screens.generateYLabels
import com.example.fluidcheck.ui.components.profile.EditField
import com.example.fluidcheck.ui.components.profile.PersonalRecordsContainer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp

private val PST_ZONE = ZoneId.of("GMT+8")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDetailedDialog(
    user: UserRecord,
    canEdit: Boolean,
    canDelete: Boolean,
    adminRepository: com.example.fluidcheck.repository.AdminRepository,
    userRepository: com.example.fluidcheck.repository.UserRepository,
    fluidLogRepository: com.example.fluidcheck.repository.FluidLogRepository,
    currentUserId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserRecord) -> Unit,
    onDelete: () -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var role by remember { mutableStateOf(user.role) }
    var dailyGoal by remember { mutableStateOf(user.dailyGoal?.toString() ?: "3000") }
    
    // Personal Records States
    var weight by remember { mutableStateOf(user.weight) }
    var height by remember { mutableStateOf(user.height) }
    var age by remember { mutableStateOf(user.age) }
    val selectionPlaceholder = stringResource(R.string.selection_placeholder)
    var sex by remember { mutableStateOf(user.sex.ifEmpty { selectionPlaceholder }) }
    var activity by remember { mutableStateOf(user.activity.ifEmpty { selectionPlaceholder }) }
    var environment by remember { mutableStateOf(user.environment.ifEmpty { selectionPlaceholder }) }
    
    // Internal status dialog for validation
    var internalStatusDialogData by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    
    val context = LocalContext.current
    var showDiscardConfirm by remember { mutableStateOf(false) }

    fun hasChanges(): Boolean {
        return username != user.username ||
               role != user.role ||
               dailyGoal != (user.dailyGoal?.toString() ?: "3000") ||
               weight != user.weight ||
               height != user.height ||
               age != user.age ||
               sex != (user.sex.ifEmpty { "Please select..." }) ||
               activity != (user.activity.ifEmpty { "Please select..." }) ||
               environment != (user.environment.ifEmpty { "Please select..." })
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(R.string.discard_changes_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = { Text(stringResource(R.string.unsaved_changes_discard), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { 
                    showDiscardConfirm = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.discard), color = ErrorRed, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(stringResource(R.string.keep_editing), color = TextDark, style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Delete confirmation states
    var showDeleteFirstConfirm by remember { mutableStateOf(false) }
    var showDeletePasswordConfirm by remember { mutableStateOf(false) }

    // Progress graph states
    var showProgressGraph by remember { mutableStateOf(false) }
    val userLogs = remember { mutableStateOf<List<FluidLog>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Load user's fluid logs only when the graph is expanded
    LaunchedEffect(user.uid, showProgressGraph) {
        if (showProgressGraph && userLogs.value.isEmpty()) {
            scope.launch {
                val res = fluidLogRepository.getFluidLogs(user.uid)
                if (res is com.example.fluidcheck.util.DataResult.Success) {
                    userLogs.value = res.data
                }
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val createdAtString = remember(user.createdAt) {
        user.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"
    }

    // First delete confirmation
    if (showDeleteFirstConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteFirstConfirm = false },
            title = { Text(text = stringResource(R.string.delete_user_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = ErrorRed) },
            text = { 
                Text(
                    stringResource(R.string.delete_user_confirm_msg, user.username.ifEmpty { user.email }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteFirstConfirm = false
                        showDeletePasswordConfirm = true
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = ErrorRed, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFirstConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = TextDark, style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Second delete confirmation with password
    if (showDeletePasswordConfirm) {
        PasswordConfirmationDialog(
            userCount = 1,
            onDismiss = { showDeletePasswordConfirm = false },
            onConfirm = {
                showDeletePasswordConfirm = false
                onDelete()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            if (internalStatusDialogData != null) {
                val (isSuccess, message) = internalStatusDialogData!!
                AlertDialog(
                    onDismissRequest = { internalStatusDialogData = null },
                    title = { 
                        Text(
                            text = if (isSuccess) stringResource(R.string.success) else stringResource(R.string.validation_error),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSuccess) SuccessGreen else ErrorRed
                        )
                    },
                    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(onClick = { internalStatusDialogData = null }) {
                            Text(stringResource(R.string.ok), color = PrimaryBlue, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (canEdit) stringResource(R.string.edit_user_details_title) else stringResource(R.string.user_details_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    IconButton(onClick = {
                        if (hasChanges()) {
                            showDiscardConfirm = true
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(AppIcons.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Profile Photo (Task 1.15)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue.copy(alpha = 0.3f))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.profilePictureUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "User Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(AppIcons.PersonOutline)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Basic Info Section
                InfoSectionHeader(stringResource(R.string.account_info_header))
                ReadOnlyField(stringResource(R.string.email_label), user.email)
                ReadOnlyField(stringResource(R.string.email_status_label), if (user.emailVerified) stringResource(R.string.verified) else stringResource(R.string.unverified))
                ReadOnlyField(stringResource(R.string.user_id_label), user.uid)
                ReadOnlyField(stringResource(R.string.created_at_label), createdAtString)
                
                Spacer(modifier = Modifier.height(24.dp))

                InfoSectionHeader(stringResource(R.string.account_settings_header))
                if (canEdit) {
                    EditField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it },
                        icon = AppIcons.PersonOutline,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                } else {
                    ReadOnlyField(stringResource(R.string.username_label), username)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (canEdit && user.role != "ADMIN") {
                    Text(stringResource(R.string.role_label), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = TextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    var roleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = !roleExpanded }
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            listOf("USER", "MODERATOR").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        role = option
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    ReadOnlyField(stringResource(R.string.role_label), role)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (user.role != "ADMIN") {
                    InfoSectionHeader(stringResource(R.string.hydration_statistics_header))
                    if (canEdit) {
                        EditField(
                            label = stringResource(R.string.daily_goal_field_label),
                            value = dailyGoal,
                            onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) dailyGoal = it },
                            icon = AppIcons.Goal,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                        )
                    } else {
                        ReadOnlyField(stringResource(R.string.daily_goal_field_label), dailyGoal)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField(stringResource(R.string.current_streak_label), context.getString(R.string.streak_days_count, user.streak))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField(stringResource(R.string.highest_streak_label), context.getString(R.string.streak_days_count, user.highestStreak))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField(stringResource(R.string.rings_closed_label), "${user.totalRingsClosed}")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField(stringResource(R.string.total_drank_label), "${user.totalFluidDrankAllTime} ml")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress Graph Section (Task 11)
                    InfoSectionHeader(stringResource(R.string.progress_graph_header))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProgressGraph = !showProgressGraph },
                        shape = RoundedCornerShape(16.dp),
                        color = if (showProgressGraph) PrimaryBlue.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (showProgressGraph) PrimaryBlue.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    AppIcons.Progress,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                 Text(
                                    text = if (showProgressGraph) stringResource(R.string.hide_progress_graph) else stringResource(R.string.show_progress_graph),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextDark
                                )
                            }
                            Icon(
                                if (showProgressGraph) AppIcons.ArrowDown else AppIcons.ArrowRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showProgressGraph) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UserProgressChart(
                            allLogs = userLogs.value,
                            dailyGoal = user.dailyGoal ?: 3000,
                            accountCreatedAt = user.createdAt
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    InfoSectionHeader(stringResource(R.string.personal_records_title))
                    PersonalRecordsContainer(
                        weight = weight, onWeightChange = { weight = it },
                        height = height, onHeightChange = { height = it },
                        age = age, onAgeChange = { age = it },
                        sex = sex, onSexChange = { sex = it },
                        activity = activity, onActivityChange = { activity = it },
                        environment = environment, onEnvironmentChange = { environment = it },
                        enabled = canEdit
                    )
                }

                if (canEdit) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            // Username format validation
                            val uErr = com.example.fluidcheck.util.ValidationUtils.validateUsername(username)
                            
                            // Range validation for numeric inputs
                            val wErr = com.example.fluidcheck.util.ValidationUtils.validateWeight(weight.toFloatOrNull())
                            val hErr = com.example.fluidcheck.util.ValidationUtils.validateHeight(height.toFloatOrNull())
                            val aErr = com.example.fluidcheck.util.ValidationUtils.validateAge(age.toIntOrNull())
                            val gErr = com.example.fluidcheck.util.ValidationUtils.validateDailyGoal(dailyGoal.toIntOrNull())

                            val firstErr = uErr ?: wErr ?: hErr ?: aErr ?: gErr
                            if (firstErr != null) {
                                internalStatusDialogData = false to firstErr
                                return@Button
                            }

                            onSave(user.copy(
                                username = username.trim(),
                                role = role,
                                dailyGoal = dailyGoal.trim().toIntOrNull() ?: 3000,
                                weight = weight.trim(),
                                height = height.trim(),
                                age = age.trim(),
                                sex = sex.replace("Please select...", ""),
                                activity = activity.replace("Please select...", ""),
                                environment = environment.replace("Please select...", "")
                            ))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(R.string.save_user_details), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Delete User Button (Task 12.1 - Self Guard)
                if (canDelete && user.uid != currentUserId) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { showDeleteFirstConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_user_title), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                } else if (canDelete && user.uid == currentUserId) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.self_delete_guard_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedForeground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PasswordConfirmationDialog(
    userCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { 
            Column {
                Text(
                    text = stringResource(R.string.security_verification_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                val targetText = if (userCount == 1) stringResource(R.string.this_user) else stringResource(R.string.multiple_users, userCount)
                Text(
                    text = stringResource(R.string.security_verification_subtitle, targetText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedForeground
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.enter_password_placeholder), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(AppIcons.Lock, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (password.isNotBlank()) {
                            // The logic is in confirmButton, so we'll leave it to the user to press the button or we can trigger it.
                            // But usually onDone should trigger the main action. 
                            // Since refactoring a whole block into a function might be risky here, I'll just keep it simple.
                        }
                    }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        errorBorderColor = Color(0xFFEF4444)
                    ),
                    isError = errorMessage != null,
                    singleLine = true
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = context.getString(R.string.error_empty_password)
                        return@TextButton
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = authRepository.reauthenticate(password)
                        isLoading = false
                        if (result.isSuccess) {
                            onConfirm()
                        } else {
                            errorMessage = context.getString(R.string.error_invalid_password)
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.delete), color = if (isLoading) Color.Gray else ErrorRed, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            @Suppress("DEPRECATION")
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel), color = TextDark, style = MaterialTheme.typography.labelLarge)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}
