package com.example.fluidcheck.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fluidcheck.R
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.screens.EditField
import com.example.fluidcheck.ui.screens.PersonalRecordsContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboard(
    firestoreRepository: FirestoreRepository = remember { FirestoreRepository() }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForEdit by remember { mutableStateOf<UserRecord?>(null) }
    var selectedUserForDelete by remember { mutableStateOf<UserRecord?>(null) }
    
    val users by firestoreRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    // Stats calculation
    val totalUsers = users.size
    val activeUsers = users.count { it.streak > 0 }
    val totalStreak = users.sumOf { it.streak }
    val avgStreak = if (totalUsers > 0) "%.1fd".format(totalStreak.toFloat() / totalUsers) else "0d"

    if (selectedUserForEdit != null) {
        EditUserDetailedDialog(
            user = selectedUserForEdit!!,
            onDismiss = { selectedUserForEdit = null },
            onSave = { updatedUser ->
                scope.launch {
                    firestoreRepository.saveUserRecord(updatedUser.uid, updatedUser)
                    if (updatedUser.username != selectedUserForEdit!!.username) {
                        firestoreRepository.updateUsername(updatedUser.uid, selectedUserForEdit!!.username, updatedUser.username)
                    }
                    selectedUserForEdit = null
                }
            }
        )
    }

    if (selectedUserForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedUserForDelete = null },
            title = { Text(text = stringResource(R.string.confirm_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_delete_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            firestoreRepository.softDeleteUser(selectedUserForDelete!!.uid)
                            selectedUserForDelete = null
                        }
                    }
                ) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.delete), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { selectedUserForDelete = null }) {
                    Text(stringResource(R.string.cancel), color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            AnalyticsGrid(
                totalUsers = totalUsers.toString(),
                activeUsers = activeUsers.toString(),
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
                )
            )
        }

        item {
            UserDirectoryHeader()
        }

        items(users.filter { 
            !it.isDeleted && (it.username.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true))
        }) { user ->
            UserRow(
                user = user,
                onEdit = { selectedUserForEdit = user },
                onDelete = { selectedUserForDelete = user }
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
    onDismiss: () -> Unit,
    onSave: (UserRecord) -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var role by remember { mutableStateOf(user.role) }
    var dailyGoal by remember { mutableStateOf(user.dailyGoal?.toString() ?: "3000") }
    
    // Personal Records States
    var weight by remember { mutableStateOf(user.weight) }
    var height by remember { mutableStateOf(user.height) }
    var age by remember { mutableStateOf(user.age) }
    var sex by remember { mutableStateOf(user.sex.ifEmpty { "Please select..." }) }
    var activity by remember { mutableStateOf(user.activity.ifEmpty { "Please select..." }) }
    var environment by remember { mutableStateOf(user.environment.ifEmpty { "Please select..." }) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val createdAtString = remember(user.createdAt) {
        user.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"
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
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(AppIcons.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Basic Info Section
                InfoSectionHeader("ACCOUNT INFO")
                ReadOnlyField("Email", user.email)
                ReadOnlyField("User ID", user.uid)
                ReadOnlyField("Created At", createdAtString)
                
                Spacer(modifier = Modifier.height(24.dp))

                InfoSectionHeader("ACCOUNT SETTINGS")
                EditField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it },
                    icon = AppIcons.PersonOutline
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                        listOf("USER", "ADMIN").forEach { option ->
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

                Spacer(modifier = Modifier.height(24.dp))

                InfoSectionHeader("HYDRATION STATISTICS")
                EditField(
                    label = "Daily Goal (ml)",
                    value = dailyGoal,
                    onValueChange = { if (it.all { c -> c.isDigit() }) dailyGoal = it },
                    icon = AppIcons.Goal
                )
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

                InfoSectionHeader("PERSONAL RECORDS")
                PersonalRecordsContainer(
                    weight = weight, onWeightChange = { weight = it },
                    height = height, onHeightChange = { height = it },
                    age = age, onAgeChange = { age = it },
                    sex = sex, onSexChange = { sex = it },
                    activity = activity, onActivityChange = { activity = it },
                    environment = environment, onEnvironmentChange = { environment = it },
                    enabled = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        onSave(user.copy(
                            username = username,
                            role = role,
                            dailyGoal = dailyGoal.toIntOrNull() ?: 3000,
                            weight = weight,
                            height = height,
                            age = age,
                            sex = sex.replace("Please select...", ""),
                            activity = activity.replace("Please select...", ""),
                            environment = environment.replace("Please select...", "")
                        ))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save User Details", fontWeight = FontWeight.Bold)
                }
            }
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
    activeUsers: String,
    avgStreak: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.total_users),
                value = totalUsers,
                icon = AppIcons.Group,
                iconColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = stringResource(R.string.downloads),
                value = totalUsers, // Using total users as proxy for now
                icon = AppIcons.Download,
                iconColor = Color(0xFF22C55E),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.active_now),
                value = activeUsers,
                icon = AppIcons.Goal,
                iconColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = stringResource(R.string.avg_streak),
                value = avgStreak,
                icon = AppIcons.Progress,
                iconColor = Color(0xFFA855F7),
                modifier = Modifier.weight(1f)
            )
        }
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
fun UserDirectoryHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
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
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            @Suppress("DEPRECATION")
            Text(stringResource(R.string.user_column), modifier = Modifier.weight(2f), fontSize = 14.sp, color = Color.Gray)
            @Suppress("DEPRECATION")
            Text(stringResource(R.string.role_column), modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Gray)
            @Suppress("DEPRECATION")
            Text(stringResource(R.string.streak_column), modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Gray)
            @Suppress("DEPRECATION")
            Text(stringResource(R.string.actions_column), modifier = Modifier.width(60.dp), fontSize = 14.sp, color = Color.Gray)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
    }
}

@Composable
fun UserRow(
    user: UserRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Text(user.username.ifEmpty { "No Username" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
            }
            
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (user.role == "ADMIN") Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                    contentColor = if (user.role == "ADMIN") PrimaryBlue else Color.Gray
                ) {
                    Text(
                        text = user.role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${user.streak}d", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
            }

            Row(modifier = Modifier.width(60.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(
                    AppIcons.Edit, 
                    contentDescription = stringResource(R.string.edit), 
                    modifier = Modifier.size(18.dp).clickable { onEdit() }, 
                    tint = Color.Gray
                )
                Icon(
                    AppIcons.Delete,
                    contentDescription = "Delete", 
                    modifier = Modifier.size(18.dp).clickable { onDelete() }, 
                    tint = Color.Gray
                )
            }
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
    }
}
