package com.example.fluidcheck.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.model.getIconForFluidType
import com.example.fluidcheck.ui.admin.AdminDashboard
import com.example.fluidcheck.ui.navigation.NavRoutes
import com.example.fluidcheck.ui.screens.AICoachScreen
import com.example.fluidcheck.ui.screens.HomeScreen
import com.example.fluidcheck.ui.screens.ProgressScreen
import com.example.fluidcheck.ui.screens.SettingsScreen
import com.example.fluidcheck.ui.screens.EditProfileScreen
import com.example.fluidcheck.ui.screens.AboutDeveloperScreen
import com.example.fluidcheck.ui.auth.SignUpScreen
import com.example.fluidcheck.ui.theme.AppBackground
import com.example.fluidcheck.ui.theme.AppIcons
import com.example.fluidcheck.ui.theme.PrimaryBlue
import com.example.fluidcheck.ui.theme.TextDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    username: String = "",
    isAdmin: Boolean = false,
    onLogout: () -> Unit = {}
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

    // Observe daily goal from repository
    val currentGoal by repository.getDailyGoal(username).collectAsState(initial = 3000)
    
    // State for user logs (logs are still in-memory for now per project scope)
    var userLogsMap by remember { mutableStateOf(mapOf<String, List<FluidLog>>()) }
    
    val currentLogs = userLogsMap[username] ?: emptyList()
    val totalIntake = currentLogs.sumOf { it.amount }

    if (showLogSheet) {
        LogNewDrinkSheet(
            onDismiss = { showLogSheet = false },
            onConfirm = { type, amount ->
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val icon = getIconForFluidType(type)
                val newLog = FluidLog(type = type, time = time, amount = amount, icon = icon)
                
                val updatedLogs = currentLogs + newLog
                val newMap = userLogsMap.toMutableMap()
                newMap[username] = updatedLogs
                userLogsMap = newMap
                
                showLogSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("Logged $amount ml of $type")
                }
            }
        )
    }

    if (logToEdit != null) {
        EditLogSheet(
            log = logToEdit!!,
            onDismiss = { logToEdit = null },
            onSave = { updatedLog ->
                val updatedLogs = currentLogs.map { if (it.id == updatedLog.id) updatedLog else it }
                val newMap = userLogsMap.toMutableMap()
                newMap[username] = updatedLogs
                userLogsMap = newMap
                logToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Log updated")
                }
            },
            onDelete = { logId ->
                val updatedLogs = currentLogs.filter { it.id != logId }
                val newMap = userLogsMap.toMutableMap()
                newMap[username] = updatedLogs
                userLogsMap = newMap
                logToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Log deleted")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TransparentHeader()
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.3f)
                )
                FluidBottomNavigation(
                    isAdmin = isAdmin,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    currentRoute = currentDestination?.route
                )
            }
        },
        floatingActionButton = {
            if (currentDestination?.route == NavRoutes.Home.route) {
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
            NavHost(
                navController = navController,
                startDestination = if (isAdmin) NavRoutes.Admin.route else NavRoutes.Home.route
            ) {
                composable(NavRoutes.Home.route) { 
                    HomeScreen(
                        dailyGoal = currentGoal,
                        totalIntake = totalIntake,
                        logs = currentLogs,
                        onUpdateGoal = { newGoal ->
                            scope.launch {
                                repository.saveDailyGoal(username, newGoal)
                            }
                        },
                        onEditLog = { log ->
                            logToEdit = log
                        }
                    )
                }
                composable(NavRoutes.Progress.route) { ProgressScreen() }
                composable(NavRoutes.AICoach.route) { 
                    AICoachScreen(
                        onSetGoal = { newGoal ->
                            scope.launch {
                                repository.saveDailyGoal(username, newGoal)
                                snackbarHostState.showSnackbar("Daily goal updated to ${newGoal}ml")
                            }
                        }
                    )
                }
                composable(NavRoutes.Settings.route) { 
                    SettingsScreen(
                        isAdmin = isAdmin, 
                        onLogout = onLogout,
                        onEditProfile = { navController.navigate(NavRoutes.EditProfile.route) },
                        onAboutDeveloper = { navController.navigate(NavRoutes.AboutDeveloper.route) }
                    )
                }
                composable(NavRoutes.EditProfile.route) {
                    EditProfileScreen(
                        username = username,
                        repository = repository,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.AboutDeveloper.route) {
                    AboutDeveloperScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.SignUp.route) { 
                    SignUpScreen(
                        onSignUpSuccess = { 
                            // SignUp logic
                        },
                        onBackToLogin = { 
                            // Back to login logic
                        } 
                    )
                }
                if (isAdmin) {
                    composable(NavRoutes.Admin.route) { AdminDashboard() }
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
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .menuAnchor(),
                    leadingIcon = {
                        val icon = getIconForFluidType(selectedType)
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
            title = { Text(text = stringResource(R.string.delete_log_title), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.delete_log_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(log.id)
                }) {
                    Text(stringResource(R.string.confirm), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
                Text(
                    text = "Please fill in all fields.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

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
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .menuAnchor(),
                    leadingIcon = {
                        val icon = getIconForFluidType(selectedType)
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
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        val icon = getIconForFluidType(selectedType)
                        onSave(log.copy(type = selectedType, amount = amount, time = timeText, icon = icon))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
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
fun TransparentHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = AppIcons.AppLogo),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FluidBottomNavigation(
    isAdmin: Boolean = false,
    onNavigate: (String) -> Unit,
    currentRoute: String?
) {
    val items = if (isAdmin) {
        listOf(
            NavigationItem(stringResource(R.string.analytics), NavRoutes.Admin.route, AppIcons.Admin),
            NavigationItem(stringResource(R.string.account), NavRoutes.Settings.route, AppIcons.Settings)
        )
    } else {
        listOf(
            NavigationItem(stringResource(R.string.home), NavRoutes.Home.route, AppIcons.Home),
            NavigationItem(stringResource(R.string.history), NavRoutes.Progress.route, AppIcons.Progress),
            NavigationItem("AI Coach", NavRoutes.AICoach.route, AppIcons.AICoach),
            NavigationItem(stringResource(R.string.account), NavRoutes.Settings.route, AppIcons.Settings)
        )
    }

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
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
