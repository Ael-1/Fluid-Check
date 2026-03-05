package com.example.fluidcheck.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fluidcheck.R
import com.example.fluidcheck.model.ALL_FLUID_TYPES
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.NavigationItem
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.model.getIconForFluidType
import com.example.fluidcheck.ui.navigation.NavRoutes
import com.example.fluidcheck.ui.screens.AICoachScreen
import com.example.fluidcheck.ui.screens.HomeScreen
import com.example.fluidcheck.ui.screens.ProgressScreen
import com.example.fluidcheck.ui.screens.SettingsScreen
import com.example.fluidcheck.ui.screens.EditProfileScreen
import com.example.fluidcheck.ui.screens.AboutDeveloperScreen
import com.example.fluidcheck.ui.auth.SignUpScreen
import com.example.fluidcheck.ui.auth.VerifyAccountScreen
import com.example.fluidcheck.ui.admin.AdminDashboard
import com.example.fluidcheck.ui.theme.AppBackground
import com.example.fluidcheck.ui.theme.AppIcons
import com.example.fluidcheck.ui.theme.PrimaryBlue
import com.example.fluidcheck.ui.theme.TextDark
import com.example.fluidcheck.util.NetworkMonitor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val ADMIN_EMAIL = "admin@fluidcheck.ai"

@Composable
fun MainScreen(
    userId: String,
    username: String = "",
    isAdmin: Boolean = false, // Kept for compatibility
    hasAdminPrivileges: Boolean = false, // Kept for compatibility
    onLogout: () -> Unit = {},
    onToggleRole: () -> Unit = {},
    onVerifyAccount: (UserRecord, String, String, String) -> Unit = { _, _, _, _ -> },
    onGoogleVerifyAccount: (UserRecord) -> Unit = { _ -> },
    isAuthInProgress: Boolean = false,
    isGoogleAvailable: Boolean = false,
    onGoogleSignInClick: () -> Unit = {},
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    firestoreRepository: FirestoreRepository = run {
        val context = LocalContext.current
        remember(context) { FirestoreRepository(context) }
    }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showLogSheet by remember { mutableStateOf(false) }
    var logToEdit by remember { mutableStateOf<FluidLog?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember { UserPreferencesRepository(context) }

    // Use userId (UID) for synchronization as document ID
    val userRecordFlow = remember(userId) { firestoreRepository.getUserRecordFlow(userId) }
    val userRecord by userRecordFlow.collectAsState(initial = null)
    
    // Core administrative check from database - Now includes MODERATOR
    val userRole = userRecord?.role ?: "USER"
    val isDatabaseAdmin = userRole == "ADMIN" || userRole == "MODERATOR"
    val isPrimaryAdmin = userRecord?.email?.equals(ADMIN_EMAIL, ignoreCase = true) == true
    
    // Last session's mode from DataStore
    val adminModePrefFlow = remember(userId) { repository.getAdminModeFlow(userId) }
    val adminModeState by adminModePrefFlow.collectAsState(initial = "LOADING")
    
    // Determine if we are still waiting for critical data
    val isLoading = userRecord == null || adminModeState == "LOADING" || userId.isEmpty()

    // Notification states
    // Default to system permission status if user hasn't made an explicit choice yet.
    val notificationsEnabled = userRecord?.notificationsEnabled ?: hasNotificationPermission
    val reminderFrequency = userRecord?.reminderFrequency ?: "60"

    // Sync WorkManager when settings change
    LaunchedEffect(notificationsEnabled, reminderFrequency, hasNotificationPermission, userId) {
        if (notificationsEnabled && hasNotificationPermission) {
            val freqInt = reminderFrequency.toIntOrNull() ?: 60
            com.example.fluidcheck.util.NotificationScheduler.scheduleReminders(context, userId, freqInt)
            com.example.fluidcheck.util.NotificationScheduler.scheduleSmartReminders(context, userId)
            
            // Also sync to local repository for worker offline access
            scope.launch {
                repository.setNotificationsEnabled(userId, true)
                repository.setReminderFrequency(userId, reminderFrequency)
            }
        } else {
            com.example.fluidcheck.util.NotificationScheduler.cancelAllReminders(context)
            // If explicitly disabled, sync to local repo
            if (!notificationsEnabled && userId.isNotEmpty()) {
                scope.launch {
                    repository.setNotificationsEnabled(userId, false)
                }
            }
        }
    }

    // UI state for switching between User and Admin views
    // Initialized ONLY when loading is done to prevent flicker
    var isAdminMode by rememberSaveable(userId, isPrimaryAdmin, isDatabaseAdmin, adminModeState) { 
        val prefValue = adminModeState as? Boolean
        mutableStateOf(if (isPrimaryAdmin) true else prefValue ?: isDatabaseAdmin) 
    }
    
    val todayLogsFlow = remember(userId) { firestoreRepository.getTodayFluidLogsFlow(userId) }
    val todayLogs by todayLogsFlow.collectAsState(initial = emptyList())
    
    val currentGoal = userRecord?.dailyGoal ?: 3000
    val totalIntake = todayLogs.sumOf { it.amount }
    val currentStreak = userRecord?.streak ?: 0
    val quickAddConfigs = userRecord?.quickAddConfig ?: emptyList()

    // Network and sync state for System Status section
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState(initial = true)
    val hasPendingWritesFlow = remember(userId) { firestoreRepository.hasPendingWritesFlow(userId) }
    val hasPendingWrites by hasPendingWritesFlow.collectAsState(initial = true)

    // Background Profile Photo Sync
    val hasPendingUpload by repository.hasPendingPhotoUpload(userId).collectAsState(initial = false)
    LaunchedEffect(isConnected, hasPendingUpload) {
        if (isConnected && hasPendingUpload) {
            val result = firestoreRepository.syncPendingProfilePhoto(userId)
            if (result.isSuccess) {
                repository.setPendingPhotoUpload(userId, false)
                android.widget.Toast.makeText(context, "Profile photo synced to cloud!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle Streak and Logic
    LaunchedEffect(totalIntake, currentGoal) {
        if (!isAdminMode && totalIntake >= currentGoal && currentGoal > 0 && userRecord != null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")
            }.format(Date())
            if (userRecord?.lastRingClosedDate != today) {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("GMT+8")
                }.format(calendar.time)
                
                firestoreRepository.markGoalAchievedToday(userId, today, yesterday)
            }
        }
    }

    if (showLogSheet) {
        LogNewDrinkSheet(
            onDismiss = { showLogSheet = false },
            onConfirm = { type, amount ->
                scope.launch {
                    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("GMT+8")
                    }
                    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("GMT+8")
                    }
                    val now = Date()
                    val time = sdfTime.format(now)
                    val date = sdfDate.format(now)
                    
                    val newLog = FluidLog(type = type, time = time, amount = amount, date = date)
                    
                    val result = firestoreRepository.saveFluidLog(userId, newLog)
                    if (result.isSuccess) {
                        showLogSheet = false
                        snackbarHostState.showSnackbar("Logged $amount ml of $type")
                    } else {
                        snackbarHostState.showSnackbar("Error logging drink")
                    }
                }
            }
        )
    }

    if (logToEdit != null) {
        EditLogSheet(
            log = logToEdit!!,
            onDismiss = { logToEdit = null },
            onSave = { updatedLog ->
                scope.launch {
                    val result = firestoreRepository.updateFluidLog(userId, logToEdit!!, updatedLog)
                    if (result.isSuccess) {
                        logToEdit = null
                        snackbarHostState.showSnackbar("Log updated")
                    } else {
                        snackbarHostState.showSnackbar("Error updating log")
                    }
                }
            },
            onDelete = { _ ->
                scope.launch {
                    logToEdit?.let { log ->
                        val result = firestoreRepository.deleteFluidLog(userId, log)
                        if (result.isSuccess) {
                            logToEdit = null
                            snackbarHostState.showSnackbar("Log deleted")
                        } else {
                            snackbarHostState.showSnackbar("Error deleting log")
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val hideBottomBarRoutes = listOf(NavRoutes.VerifyAccount.route, NavRoutes.EditProfile.route)
            if (!isLoading && currentDestination?.route !in hideBottomBarRoutes) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )
                    FluidBottomNavigation(
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        currentRoute = currentDestination?.route,
                        isAdmin = isAdminMode
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isLoading && !isAdminMode && currentDestination?.route == NavRoutes.Home.route) {
                FloatingActionButton(
                    onClick = { showLogSheet = true },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = stringResource(R.string.add_fluid),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppBackground)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = if (isAdminMode) NavRoutes.Admin.route else NavRoutes.Home.route
                ) {
                    composable(NavRoutes.Home.route) { 
                        HomeScreen(
                            userId = userId,
                            firestoreRepository = firestoreRepository,
                            dailyGoal = currentGoal,
                            totalIntake = totalIntake,
                            logs = todayLogs,
                            streakDays = currentStreak,
                            quickAddConfigs = quickAddConfigs,
                            onUpdateGoal = { newGoal ->
                                scope.launch {
                                    repository.saveDailyGoal(userId, newGoal)
                                    firestoreRepository.saveDailyGoal(userId, newGoal)
                                }
                            },
                            onEditLog = { log ->
                                logToEdit = log
                            },
                            onQuickAdd = { config ->
                                scope.launch {
                                    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getTimeZone("GMT+8")
                                    }
                                    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getTimeZone("GMT+8")
                                    }
                                    val now = Date()
                                    val time = sdfTime.format(now)
                                    val date = sdfDate.format(now)
                                    
                                    val newLog = FluidLog(type = config.type, time = time, amount = config.amount, date = date)
                                    val result = firestoreRepository.saveFluidLog(userId, newLog)
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Logged ${config.amount} ml of ${config.type}")
                                    }
                                }
                            },
                            onUpdateQuickAdd = { newConfigs ->
                                scope.launch {
                                    firestoreRepository.updateQuickAddConfig(userId, newConfigs)
                                }
                            }
                        )
                    }
                    composable(NavRoutes.Progress.route) { 
                        ProgressScreen(
                            userId = userId,
                            firestoreRepository = firestoreRepository,
                            dailyGoal = currentGoal,
                            accountCreatedAt = userRecord?.createdAt
                        )
                    }
                    composable(NavRoutes.AICoach.route) { 
                        AICoachScreen(
                            userRecord = userRecord,
                            isConnected = isConnected,
                            onSetGoal = { newGoal ->
                                scope.launch {
                                    repository.saveDailyGoal(userId, newGoal)
                                    val result = firestoreRepository.saveDailyGoal(userId, newGoal)
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Daily goal updated to ${newGoal}ml")
                                    }
                                }
                            }
                        )
                    }
                    composable(NavRoutes.Settings.route) {
                            val displayProfilePhoto = remember(userRecord?.profilePictureUrl, hasPendingUpload) {
                                if (hasPendingUpload && userId != "GUEST") {
                                    val localFile = com.example.fluidcheck.util.ProfilePhotoManager.getLocalPhotoFile(context)
                                    if (localFile != null && localFile.exists()) {
                                        localFile.absolutePath
                                    } else {
                                        userRecord?.profilePictureUrl ?: ""
                                    }
                                } else {
                                    userRecord?.profilePictureUrl ?: ""
                                }
                            }
                            SettingsScreen(
                                userId = userId,
                                username = userRecord?.username ?: username,
                                email = userRecord?.email ?: "",
                                streak = userRecord?.streak ?: 0,
                                isDatabaseAdmin = isDatabaseAdmin,
                                isAdminMode = isAdminMode,
                                userRole = userRole,
                                isConnected = isConnected,
                                hasPendingWrites = hasPendingWrites,
                                onLogout = onLogout,
                                onEditProfile = { navController.navigate(NavRoutes.EditProfile.route) },
                                onVerifyAccount = { navController.navigate(NavRoutes.VerifyAccount.route) },
                                onAboutDeveloper = { navController.navigate(NavRoutes.AboutDeveloper.route) },
                                notificationsEnabled = notificationsEnabled,
                                reminderFrequency = when (reminderFrequency) {
                                    "30" -> "Every 30 mins"
                                    "120" -> "Every 2 hours"
                                    "240" -> "Every 4 hours"
                                    else -> "Every 1 hour"
                                },
                                 onToggleNotifications = { enabled ->
                                    // Optimistically update the state so the toggle visually stays ON
                                    // while the permission dialog is showing.
                                    scope.launch {
                                        firestoreRepository.updateNotificationsEnabled(userId, enabled)
                                        repository.setNotificationsEnabled(userId, enabled)
                                    }
                                    
                                    // Request permission if not already granted and trying to enable
                                    if (enabled && !hasNotificationPermission) {
                                        onRequestNotificationPermission()
                                    }
                                },
                                onFrequencyChanged = { freqLabel ->
                                    val freqValue = when (freqLabel) {
                                        "Every 30 mins" -> "30"
                                        "Every 2 hours" -> "120"
                                        "Every 4 hours" -> "240"
                                        else -> "60"
                                    }
                                    scope.launch {
                                        firestoreRepository.updateReminderFrequency(userId, freqValue)
                                        repository.setReminderFrequency(userId, freqValue)
                                    }
                                },
                                onToggleRole = {
                                    isAdminMode = !isAdminMode
                                    scope.launch {
                                        repository.setAdminMode(userId, isAdminMode)
                                    }
                                    navController.navigate(if (isAdminMode) NavRoutes.Admin.route else NavRoutes.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                profilePictureUrl = displayProfilePhoto
                            )
                    }
                    composable(NavRoutes.EditProfile.route) {
                        EditProfileScreen(
                            userId = userId,
                            username = userRecord?.username ?: username,
                            isAdminMode = isAdminMode,
                            repository = repository,
                            firestoreRepository = firestoreRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(NavRoutes.VerifyAccount.route) {
                        VerifyAccountScreen(
                            onVerifySuccess = { u, e, p ->
                                // Pass current guest data to verify function
                                onVerifyAccount(userRecord ?: UserRecord(), u, e, p)
                            },
                            onCancel = { navController.popBackStack() },
                            isGoogleAvailable = isGoogleAvailable,
                            onGoogleVerifyClick = {
                                onGoogleVerifyAccount(userRecord ?: UserRecord())
                            },
                            isLoading = isAuthInProgress
                        )
                    }
                    composable(NavRoutes.AboutDeveloper.route) {
                        AboutDeveloperScreen(onBack = { navController.popBackStack() })
                    }
                    composable(NavRoutes.Admin.route) {
                        AdminDashboard(
                            firestoreRepository = firestoreRepository,
                            currentUserRole = userRole
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogNewDrinkSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf("Water") }
    var amountText by remember { mutableStateOf("250") }
    var isExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val fluidTypes = ALL_FLUID_TYPES

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(24.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.log_new_drink),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss) {
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.close), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showError) {
                Text(
                    text = "Please input a valid amount.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.fluid_type),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val icon = getIconForFluidType(selectedType)
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .menuAnchor(),
                    leadingIcon = {
                        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        Icon(AppIcons.ArrowDown, contentDescription = null)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                    modifier = Modifier
                        .exposedDropdownSize()
                        .heightIn(max = 280.dp)
                ) {
                    fluidTypes.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        type.icon,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = type.name, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                selectedType = type.name
                                isExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.amount_ml),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Please input...", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (amountText.isBlank()) {
                        showError = true
                    } else {
                        showError = false
                        val amount = amountText.toIntOrNull() ?: 0
                        onConfirm(selectedType, amount)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.confirm),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogSheet(
    log: FluidLog,
    onDismiss: () -> Unit,
    onSave: (FluidLog) -> Unit,
    onDelete: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(log.type) }
    var amountText by remember { mutableStateOf(log.amount.toString()) }
    var timeText by remember { mutableStateOf(log.time) }
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { @Suppress("DEPRECATION") Text(text = stringResource(R.string.delete_log_title), fontWeight = FontWeight.Bold) },
            text = { @Suppress("DEPRECATION") Text(text = stringResource(R.string.delete_log_msg)) },
            confirmButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(log.id)
                }) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.confirm), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showDeleteDialog = false }) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = Color.White
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(AppIcons.Delete, contentDescription = "Delete", tint = Color.Red)
                }
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.edit_log),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss) {
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.close), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showError) {
                @Suppress("DEPRECATION")
                Text(
                    text = "Please fill in all fields.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.fluid_type),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val icon = getIconForFluidType(selectedType)
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .menuAnchor(),
                    leadingIcon = {
                        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        Icon(AppIcons.ArrowDown, contentDescription = null)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                    modifier = Modifier
                        .exposedDropdownSize()
                        .heightIn(max = 280.dp)
                ) {
                    val fluidTypes = ALL_FLUID_TYPES
                    fluidTypes.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        type.icon,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = type.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                selectedType = type.name
                                isExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.amount_ml),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Please input...", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.time_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Please input...", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (amountText.isBlank() || timeText.isBlank()) {
                        showError = true
                    } else {
                        showError = false
                        val amount = amountText.toIntOrNull() ?: log.amount
                        onSave(log.copy(type = selectedType, amount = amount, time = timeText))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.save_changes),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun FluidBottomNavigation(
    onNavigate: (String) -> Unit,
    currentRoute: String?,
    isAdmin: Boolean = false
) {
    val items = if (isAdmin) {
        listOf(
            NavigationItem("Dashboard", NavRoutes.Admin.route, AppIcons.Group),
            @Suppress("DEPRECATION")
            NavigationItem(stringResource(R.string.settings), NavRoutes.Settings.route, AppIcons.Settings)
        )
    } else {
        listOf(
            @Suppress("DEPRECATION")
            NavigationItem(stringResource(R.string.home), NavRoutes.Home.route, AppIcons.Home),
            @Suppress("DEPRECATION")
            NavigationItem(stringResource(R.string.progress), NavRoutes.Progress.route, AppIcons.Progress),
            NavigationItem("AI Coach", NavRoutes.AICoach.route, AppIcons.AICoach),
            @Suppress("DEPRECATION")
            NavigationItem(stringResource(R.string.settings), NavRoutes.Settings.route, AppIcons.Settings)
        )
    }

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    @Suppress("DEPRECATION")
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                    unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
                    indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
                )
            )
        }
    }
}
