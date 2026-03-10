package com.example.fluidcheck.ui.admin

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
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.screens.EditField
import com.example.fluidcheck.ui.screens.PersonalRecordsContainer
import com.example.fluidcheck.ui.screens.TimeRangeTabs
import com.example.fluidcheck.ui.screens.DateNavigationBar
import com.example.fluidcheck.ui.screens.HydrationLineChart
import com.example.fluidcheck.ui.screens.getChartDataForRange
import com.example.fluidcheck.ui.screens.generateYLabels
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

enum class SortColumn { NONE, USER, ROLE, CREATED_AT }
enum class SortState { DEFAULT, PRESS_1, PRESS_2 }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminDashboard(
    firestoreRepository: FirestoreRepository = remember { FirestoreRepository() },
    currentUserRole: String = "USER",
    currentUserId: String = ""
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForEdit by remember { mutableStateOf<UserRecord?>(null) }
    
    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showBulkDeletePasswordConfirm by remember { mutableStateOf(false) }
    
    // Status dialog state: (IsSuccess, Message)
    var statusDialogData by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val usersFlow = remember { firestoreRepository.getAllUsersFlow() }
    val users by usersFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    val canModifyUsers = currentUserRole == "ADMIN"

    // Stats calculation
    val totalUsers = users.size
    val moderatorsCount = users.count { it.role == "MODERATOR" }
    val avgGoal = if (totalUsers > 0) users.sumOf { it.dailyGoal ?: 3000 }.toFloat() / totalUsers else 3000f
    val totalRings = users.sumOf { it.totalRingsClosed }
    val totalStreak = users.sumOf { it.streak }
    val avgStreak = if (totalUsers > 0) "%.1fd".format(totalStreak.toFloat() / totalUsers) else "0d"

    val filteredUsers = users.filter { 
        !it.deleted && (it.username.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true))
    }

    var activeSortColumn by remember { mutableStateOf(SortColumn.NONE) }
    var activeSortState by remember { mutableStateOf(SortState.DEFAULT) }

    val roleWeight = { role: String ->
        when (role.uppercase()) {
            "ADMIN" -> 3
            "MODERATOR" -> 2
            "USER" -> 1
            else -> 0
        }
    }

    val sortedUsers = remember(filteredUsers, activeSortColumn, activeSortState) {
        if (activeSortState == SortState.DEFAULT || activeSortColumn == SortColumn.NONE) {
            filteredUsers.sortedWith(
                compareByDescending<UserRecord> { roleWeight(it.role) }
                    .thenBy { it.username.lowercase() }
            )
        } else {
            when (activeSortColumn) {
                SortColumn.USER -> {
                    if (activeSortState == SortState.PRESS_1) {
                        filteredUsers.sortedBy { it.username.lowercase() }
                    } else {
                        filteredUsers.sortedByDescending { it.username.lowercase() }
                    }
                }
                SortColumn.ROLE -> {
                    if (activeSortState == SortState.PRESS_1) {
                        filteredUsers.sortedWith(
                            compareByDescending<UserRecord> { roleWeight(it.role) }
                                .thenBy { it.username.lowercase() }
                        )
                    } else {
                        filteredUsers.sortedWith(
                            compareBy<UserRecord> { roleWeight(it.role) }
                                .thenBy { it.username.lowercase() }
                        )
                    }
                }
                SortColumn.CREATED_AT -> {
                    if (activeSortState == SortState.PRESS_1) {
                        filteredUsers.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    } else {
                        filteredUsers.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                    }
                }
                else -> filteredUsers
            }
        }
    }

    // Exit selection mode when no users are selected
    LaunchedEffect(selectedUserIds) {
        if (selectedUserIds.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    if (selectedUserForEdit != null) {
        EditUserDetailedDialog(
            user = selectedUserForEdit!!,
            canEdit = canModifyUsers,
            canDelete = canModifyUsers,
            firestoreRepository = firestoreRepository,
            currentUserId = currentUserId,
            onDismiss = { selectedUserForEdit = null },
            onSave = { updatedUser ->
                scope.launch {
                    try {
                        isLoading = true
                        if (updatedUser.username != selectedUserForEdit!!.username) {
                            val usernameResult = firestoreRepository.updateUsername(updatedUser.uid, selectedUserForEdit!!.username, updatedUser.username)
                            if (usernameResult.isFailure) {
                                val error = usernameResult.exceptionOrNull()?.message ?: "Error updating username"
                                isLoading = false
                                selectedUserForEdit = null
                                statusDialogData = false to error
                                return@launch
                            }
                        }
                        
                        val saveResult = firestoreRepository.saveUserRecord(updatedUser.uid, updatedUser)
                        if (saveResult.isSuccess) {
                            isLoading = false
                            selectedUserForEdit = null
                            statusDialogData = true to "User details updated successfully!"
                        } else {
                            val error = saveResult.exceptionOrNull()?.message ?: "Error saving user record"
                            isLoading = false
                            selectedUserForEdit = null
                            statusDialogData = false to error
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        selectedUserForEdit = null
                        statusDialogData = false to (e.message ?: "An unexpected error occurred.")
                    }
                }
            },
            onDelete = {
                scope.launch {
                    try {
                        val result = firestoreRepository.softDeleteUser(selectedUserForEdit!!.uid)
                        if (result.isSuccess) {
                            selectedUserForEdit = null
                            statusDialogData = true to "User has been successfully deleted."
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Unknown error"
                            statusDialogData = false to "Failed to delete user: $error"
                        }
                    } catch (e: Exception) {
                        statusDialogData = false to (e.message ?: "An unexpected error occurred.")
                    }
                }
            }
        )
    }

    // Bulk Delete - First confirmation
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(text = "Delete Selected Users", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Are you sure you want to delete ${selectedUserIds.size} selected user(s)? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteConfirm = false
                        showBulkDeletePasswordConfirm = true
                    }
                ) {
                    @Suppress("DEPRECATION")
                    Text("Confirm", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Bulk Delete - Second confirmation with password
    if (showBulkDeletePasswordConfirm) {
        PasswordConfirmationDialog(
            userCount = selectedUserIds.size,
            onDismiss = { showBulkDeletePasswordConfirm = false },
            onConfirm = {
                scope.launch {
                    try {
                        val count = selectedUserIds.size
                        val result = firestoreRepository.softDeleteUsers(selectedUserIds)
                        if (result.isSuccess) {
                            selectedUserIds = emptySet()
                            isSelectionMode = false
                            statusDialogData = true to "$count user(s) have been successfully deleted."
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Unknown error"
                            statusDialogData = false to "Failed to delete users: $error"
                        }
                        showBulkDeletePasswordConfirm = false
                    } catch (e: Exception) {
                        showBulkDeletePasswordConfirm = false
                        statusDialogData = false to "Unexpected error during bulk delete: ${e.message}"
                    }
                }
            }
        )
    }

    // Status Dialog
    statusDialogData?.let { (isSuccess, message) ->
        AlertDialog(
            onDismissRequest = { statusDialogData = null },
            title = { 
                @Suppress("DEPRECATION")
                Text(
                    text = if (isSuccess) "Success" else "Update Failed",
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = { statusDialogData = null }
                ) {
                    Text("OK", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            AnalyticsGrid(
                totalUsers = totalUsers.toString(),
                moderators = moderatorsCount.toString(),
                avgGoal = "%.0f ml".format(avgGoal),
                totalLogs = totalRings.toString(),
                avgStreak = avgStreak
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = { Text(stringResource(R.string.search_users_placeholder), color = Color.Gray) },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedBorderColor = PrimaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true
            )
        }

        item {
            UserDirectoryHeader(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedUserIds.size,
                onDeleteSelected = { showBulkDeleteConfirm = true },
                onCancelSelection = {
                    isSelectionMode = false
                    selectedUserIds = emptySet()
                },
                canDelete = canModifyUsers,
                activeSortColumn = activeSortColumn,
                activeSortState = activeSortState,
                onSortColumnClick = { column ->
                    if (activeSortColumn == column) {
                        activeSortState = when (activeSortState) {
                            SortState.DEFAULT -> SortState.PRESS_1
                            SortState.PRESS_1 -> SortState.PRESS_2
                            SortState.PRESS_2 -> SortState.DEFAULT
                        }
                        if (activeSortState == SortState.DEFAULT) {
                            activeSortColumn = SortColumn.NONE
                        }
                    } else {
                        activeSortColumn = column
                        activeSortState = SortState.PRESS_1
                    }
                }
            )
        }

        items(sortedUsers) { user ->
            val isSelected = selectedUserIds.contains(user.uid)
            UserRow(
                user = user,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onClick = {
                    if (isSelectionMode) {
                        selectedUserIds = if (isSelected) {
                            selectedUserIds - user.uid
                        } else {
                            selectedUserIds + user.uid
                        }
                    } else {
                        selectedUserForEdit = user
                    }
                },
                onLongClick = {
                    if (canModifyUsers && !isSelectionMode) {
                        isSelectionMode = true
                        selectedUserIds = setOf(user.uid)
                    }
                }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDetailedDialog(
    user: UserRecord,
    canEdit: Boolean,
    canDelete: Boolean,
    firestoreRepository: FirestoreRepository,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (UserRecord) -> Unit,
    onDelete: () -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var role by remember { mutableStateOf(user.role) }
    var dailyGoal by remember { mutableStateOf(user.dailyGoal?.toString() ?: "3000") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Personal Records States
    var weight by remember { mutableStateOf(user.weight) }
    var height by remember { mutableStateOf(user.height) }
    var age by remember { mutableStateOf(user.age) }
    var sex by remember { mutableStateOf(user.sex.ifEmpty { "Please select..." }) }
    var activity by remember { mutableStateOf(user.activity.ifEmpty { "Please select..." }) }
    var environment by remember { mutableStateOf(user.environment.ifEmpty { "Please select..." }) }
    
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
            title = { Text("Discard Changes?", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = { 
                    showDiscardConfirm = false
                    onDismiss()
                }) {
                    Text("DISCARD", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("KEEP EDITING", color = TextDark)
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
                userLogs.value = firestoreRepository.getFluidLogs(user.uid)
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
            title = { Text(text = "Delete User", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = { 
                Text(
                    "Are you sure you want to delete \"${user.username.ifEmpty { user.email }}\"? This action cannot be undone.",
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
                    @Suppress("DEPRECATION")
                    Text("Confirm", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showDeleteFirstConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = TextDark)
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
                            text = if (isSuccess) "Success" else "Validation Error",
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { internalStatusDialogData = null }) {
                            Text("OK", color = PrimaryBlue, fontWeight = FontWeight.Bold)
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
                        text = if (canEdit) "Edit User Details" else "User Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
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
                InfoSectionHeader("ACCOUNT INFO")
                ReadOnlyField("Email", user.email)
                ReadOnlyField("Email Status", if (user.emailVerified) "Verified" else "Unverified")
                ReadOnlyField("User ID", user.uid)
                ReadOnlyField("Created At", createdAtString)
                
                Spacer(modifier = Modifier.height(24.dp))

                InfoSectionHeader("ACCOUNT SETTINGS")
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
                    ReadOnlyField("Username", username)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (canEdit && user.role != "ADMIN") {
                    Text("Role", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
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
                    ReadOnlyField("Role", role)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (user.role != "ADMIN") {
                    InfoSectionHeader("HYDRATION STATISTICS")
                    if (canEdit) {
                        EditField(
                            label = "Daily Goal (ml)",
                            value = dailyGoal,
                            onValueChange = { if (it.all { c -> c.isDigit() }) dailyGoal = it },
                            icon = AppIcons.Goal,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                        )
                    } else {
                        ReadOnlyField("Daily Goal (ml)", dailyGoal)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField("Current Streak", "${user.streak} days")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField("Highest Streak", "${user.highestStreak} days")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField("Rings Closed", "${user.totalRingsClosed}")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            ReadOnlyField("Total Drank", "${user.totalFluidDrankAllTime} ml")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress Graph Section (Task 11)
                    InfoSectionHeader("PROGRESS GRAPH")
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
                                    text = if (showProgressGraph) "Hide Progress Graph" else "Show Progress Graph",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
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

                    InfoSectionHeader("PERSONAL RECORDS")
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
                            // Task 1.10: Enhanced Username Validation
                            val uErr = com.example.fluidcheck.util.ValidationUtils.validateUsername(username)
                            
                            // Task 12.2: Numeric Range Validation
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
                            Text("Save User Details", fontWeight = FontWeight.Bold)
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
                        Text("Delete User", fontWeight = FontWeight.Bold)
                    }
                } else if (canDelete && user.uid == currentUserId) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You cannot delete your own account from the dashboard.",
                        fontSize = 12.sp,
                        color = Color.Gray,
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

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { 
            Column {
                Text(
                    text = "🔒 Security Verification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your admin password to delete ${if (userCount == 1) "this user" else "$userCount users"}.",
                    fontSize = 13.sp,
                    color = Color.Gray
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
                    placeholder = { Text("Enter your password", fontSize = 14.sp) },
                    leadingIcon = { Icon(AppIcons.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
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
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
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
                        errorMessage = "Password cannot be empty"
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
                            errorMessage = "Incorrect password. Please try again."
                        }
                    }
                },
                enabled = !isLoading
            ) {
                @Suppress("DEPRECATION")
                Text("DELETE", color = if (isLoading) Color.Gray else Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            @Suppress("DEPRECATION")
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel), color = TextDark)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun UserProgressChart(
    allLogs: List<FluidLog>,
    dailyGoal: Int,
    accountCreatedAt: com.google.firebase.Timestamp? = null
) {
    var selectedTab by remember { mutableStateOf("Week") }
    var navOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        navOffset = 0
    }

    val (navLabel, chartData) = remember(selectedTab, navOffset, allLogs, dailyGoal) {
        getChartDataForRange(selectedTab, navOffset, allLogs, dailyGoal)
    }

    val creationDate = remember(accountCreatedAt) {
        accountCreatedAt?.toDate()?.toInstant()?.atZone(PST_ZONE)?.toLocalDate()
            ?: LocalDate.now(PST_ZONE)
    }

    val canGoNext = navOffset < 0
    val canGoPrevious = remember(selectedTab, navOffset, creationDate) {
        val today = LocalDate.now(PST_ZONE)
        when (selectedTab) {
            "Day" -> {
                val currentTargetDate = today.plusDays(navOffset.toLong())
                currentTargetDate.minusDays(1) >= creationDate
            }
            "Week" -> {
                val currentTargetWeek = today.plusWeeks(navOffset.toLong())
                val currentWeekStart = currentTargetWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                currentWeekStart.minusDays(1) >= creationDate
            }
            "Month" -> {
                val currentTargetMonth = today.plusMonths(navOffset.toLong())
                val currentMonthStart = currentTargetMonth.with(TemporalAdjusters.firstDayOfMonth())
                currentMonthStart.minusDays(1) >= creationDate
            }
            "Year" -> {
                val currentTargetYear = today.plusYears(navOffset.toLong())
                val currentYearStart = currentTargetYear.with(TemporalAdjusters.firstDayOfYear())
                currentYearStart.minusDays(1) >= creationDate
            }
            else -> true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimeRangeTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DateNavigationBar(
                label = navLabel,
                onPrevious = { navOffset-- },
                onNext = { navOffset++ },
                isPreviousEnabled = canGoPrevious,
                isNextEnabled = canGoNext
            )

            Spacer(modifier = Modifier.height(24.dp))

            HydrationLineChart(
                dataPoints = chartData.points,
                xOffsets = chartData.xOffsets,
                xLabels = chartData.xLabels,
                yLabels = chartData.yLabels,
                maxValue = chartData.maxValue,
                rangeType = selectedTab
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun InfoSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(16.dp),
                color = MutedForeground,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AnalyticsGrid(
    totalUsers: String,
    moderators: String,
    avgGoal: String,
    totalLogs: String,
    avgStreak: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Row 1: Users | Moderators
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.total_users),
                value = totalUsers,
                icon = AppIcons.Group,
                iconColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = "MODERATORS",
                value = moderators,
                icon = AppIcons.Security,
                iconColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 2: Avg. Goal | Total Logs (Rings Closed)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = "AVG. GOAL",
                value = avgGoal,
                icon = AppIcons.Goal,
                iconColor = Color(0xFF22C55E),
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = "TOTAL RINGS CLOSED",
                value = totalLogs,
                icon = AppIcons.History,
                iconColor = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 3: Avg. Streak (full width)
        AnalyticsCard(
            title = stringResource(R.string.avg_streak),
            value = avgStreak,
            icon = AppIcons.Progress,
            iconColor = Color(0xFFA855F7),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
        }
    }
}

@Composable
fun UserDirectoryHeader(
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onDeleteSelected: () -> Unit = {},
    onCancelSelection: () -> Unit = {},
    canDelete: Boolean = false,
    activeSortColumn: SortColumn = SortColumn.NONE,
    activeSortState: SortState = SortState.DEFAULT,
    onSortColumnClick: (SortColumn) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        if (isSelectionMode && canDelete) {
            // Selection mode header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$selectedCount selected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Delete button
                    FilledTonalButton(
                        onClick = onDeleteSelected,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                            contentColor = Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    // Cancel button
                    TextButton(onClick = onCancelSelection) {
                        Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        } else {
            // Normal header
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.user_directory),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.user_directory_subtitle),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                // Add space for checkbox
                Spacer(modifier = Modifier.width(40.dp))
            }
            SortableColumnHeader(
                title = stringResource(R.string.user_column),
                columnWeight = 2f,
                column = SortColumn.USER,
                activeSortColumn = activeSortColumn,
                activeSortState = activeSortState,
                onClick = { onSortColumnClick(SortColumn.USER) }
            )
            SortableColumnHeader(
                title = stringResource(R.string.role_column),
                columnWeight = 1f,
                column = SortColumn.ROLE,
                activeSortColumn = activeSortColumn,
                activeSortState = activeSortState,
                onClick = { onSortColumnClick(SortColumn.ROLE) }
            )
            SortableColumnHeader(
                title = "Created At",
                columnWeight = 1.2f,
                column = SortColumn.CREATED_AT,
                activeSortColumn = activeSortColumn,
                activeSortState = activeSortState,
                onClick = { onSortColumnClick(SortColumn.CREATED_AT) }
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
    }
}

@Composable
fun RowScope.SortableColumnHeader(
    title: String,
    columnWeight: Float,
    column: SortColumn,
    activeSortColumn: SortColumn,
    activeSortState: SortState,
    onClick: () -> Unit
) {
    val isActive = column == activeSortColumn && activeSortState != SortState.DEFAULT
    Row(
        modifier = Modifier
            .weight(columnWeight)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title, 
            fontSize = 14.sp, 
            color = if (isActive) PrimaryBlue else Color.Gray,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
        if (isActive) {
            val isDown = when (column) {
                SortColumn.USER -> activeSortState == SortState.PRESS_1 // User Press 1 = ASC (Down), Press 2 = DESC (Up)
                SortColumn.ROLE -> activeSortState == SortState.PRESS_1 // Role Press 1 = DESC (Down), Press 2 = ASC (Up)
                SortColumn.CREATED_AT -> activeSortState == SortState.PRESS_1 // Created At Press 1 = DESC (Down), Press 2 = ASC (Up)
                else -> false
            }
            Icon(
                imageVector = if (isDown) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = PrimaryBlue
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserRow(
    user: UserRecord,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val selectionColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200)
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(selectionColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = 16.dp, horizontal = if (isSelectionMode) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(32.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryBlue,
                        uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(2f)) {
                Text(user.username.ifEmpty { "No Username" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
            }
            
            Box(modifier = Modifier.weight(1f)) {
                val roleColor = when (user.role) {
                    "ADMIN" -> Color(0xFFDBEAFE)
                    "MODERATOR" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFF1F5F9)
                }
                val roleTextColor = when (user.role) {
                    "ADMIN" -> PrimaryBlue
                    "MODERATOR" -> Color(0xFFD97706)
                    else -> Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = roleColor,
                    contentColor = roleTextColor
                ) {
                    Text(
                        text = user.role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.CenterEnd) {
                val createdAtDate = user.createdAt?.toDate()
                val dateString = if (createdAtDate != null) {
                    SimpleDateFormat("MMM dd yyyy", Locale.US).format(createdAtDate)
                } else {
                    "---"
                }
                Text(
                    text = dateString,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
    }
}
