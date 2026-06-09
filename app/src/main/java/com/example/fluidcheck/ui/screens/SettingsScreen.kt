package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.*

import com.example.fluidcheck.util.MeasurementUtils
import kotlinx.coroutines.launch



@Composable
fun SettingsScreen(
    userId: String = "",
    username: String = "",
    email: String = "",
    streak: Int = 0,
    isDatabaseAdmin: Boolean = false,
    isAdminMode: Boolean = false,
    userRole: String = "FREE USER",
    isConnected: Boolean = true,
    hasPendingWrites: Boolean = false,
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onVerifyAccount: () -> Unit = {},
    onAboutDeveloper: () -> Unit = {},
    onToggleRole: () -> Unit = {},
    notificationsEnabled: Boolean = false,
    reminderFrequency: String = "Every 1 hour",
    onToggleNotifications: (Boolean) -> Unit = {},
    onFrequencyChanged: (String) -> Unit = {},
    profilePictureUrl: String = "",
    onRequestLocationPermission: () -> Unit = {},
    locationAccessEnabled: Boolean = false,
    dynamicWeatherEnabled: Boolean = false,
    onToggleLocationAccess: (Boolean) -> Unit = {},
    onToggleWeatherGoalAdjustment: (Boolean) -> Unit = {},
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences = com.example.fluidcheck.util.MeasurementPreferences(),
    onVolumeUnitChanged: (com.example.fluidcheck.util.VolumeUnit) -> Unit = {},
    onWeightUnitChanged: (com.example.fluidcheck.util.WeightUnit) -> Unit = {},
    onHeightUnitChanged: (com.example.fluidcheck.util.HeightUnit) -> Unit = {},
    smartRemindersEnabled: Boolean = true,
    onToggleSmartReminders: (Boolean) -> Unit = {},
    onSubscribe: (String) -> Unit = {},
    onRestorePurchases: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Check if this is the core Admin account
    val isPrimaryAdmin = userRole == "ADMIN"

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = stringResource(R.string.sign_out), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.sign_out_confirm_msg)) },
            confirmButton = {
                @Suppress("DEPRECATION")
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.sign_out), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    var showPremiumDialog by remember { mutableStateOf(false) }
    
    if (showPremiumDialog) {
        com.example.fluidcheck.ui.screens.PremiumPaywallDialog(
            onDismiss = { showPremiumDialog = false },
            onSubscribeMonthly = {
                showPremiumDialog = false
                onSubscribe("monthly")
            },
            onSubscribeYearly = {
                showPremiumDialog = false
                onSubscribe("yearly")
            }
        )
    }
    
    var showRestoreDialog by remember { mutableStateOf(false) }
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(text = "Restore Purchases", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to demote your account back to Free User?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        onRestorePurchases()
                    }
                ) {
                    Text("Restore", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Header
        if (isAdminMode) {
            AdminProfileHeader(
                username = username,
                userRole = userRole,
                profilePictureUrl = profilePictureUrl,
                onEditProfile = onEditProfile
            )
        } else {
            ProfileHeader(
                userId = userId,
                username = username,
                streak = streak,
                profilePictureUrl = profilePictureUrl,
                onEditProfile = onEditProfile,
                onVerifyAccount = onVerifyAccount
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Upgrade Button
        if (!isAdminMode && userRole == "FREE USER") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clickable { showPremiumDialog = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF60A5FA),
                                    Color(0xFF2563EB)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Premium,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upgrade to Premium",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlock AI coaching, advanced analytics, weather features, and more!",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Restore Purchases Button
        if (!isAdminMode && userRole == "PREMIUM USER") {
            OutlinedButton(
                onClick = { showRestoreDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Icon(AppIcons.Save, contentDescription = null, tint = TextDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restore Purchases",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextDark
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (!isAdminMode) {
            // Measurement System Section
            MeasurementSystemSection(
                prefs = measurementPreferences,
                onVolumeUnitChanged = onVolumeUnitChanged,
                onWeightUnitChanged = onWeightUnitChanged,
                onHeightUnitChanged = onHeightUnitChanged
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Smart Reminders Section
            SmartRemindersSection(
                userId = userId,
                notificationsEnabled = notificationsEnabled,
                frequency = reminderFrequency,
                onToggleNotifications = onToggleNotifications,
                onFrequencyChanged = onFrequencyChanged,
                onRequestLocationPermission = onRequestLocationPermission,
                locationAccessEnabled = locationAccessEnabled,
                dynamicWeatherEnabled = dynamicWeatherEnabled,
                onToggleLocationAccess = onToggleLocationAccess,
                onToggleWeatherGoalAdjustment = { enabled ->
                    if (userRole == "FREE USER") {
                        showPremiumDialog = true
                    } else {
                        onToggleWeatherGoalAdjustment(enabled)
                    }
                },
                smartRemindersEnabled = smartRemindersEnabled,
                onToggleSmartReminders = { enabled ->
                    if (userRole == "FREE USER") {
                        showPremiumDialog = true
                    } else {
                        onToggleSmartReminders(enabled)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // System Status Section
        SystemStatusSection(
            isConnected = isConnected,
            hasPendingWrites = hasPendingWrites,
            isGuest = userId == "GUEST"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // About Developer Button
        OutlinedButton(
            onClick = onAboutDeveloper,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
        ) {
            Icon(AppIcons.Info, contentDescription = null, tint = TextDark)
            Spacer(modifier = Modifier.width(8.dp))
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.about_developer),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Role Toggle Button for promoted Admins (Primary Admin excluded)
        if (isDatabaseAdmin && !isPrimaryAdmin) {
            Button(
                onClick = onToggleRole,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.1f),
                    contentColor = PrimaryBlue
                ),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (isAdminMode) AppIcons.PersonOutline else AppIcons.Badge,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                val switchText = if (isAdminMode) "Switch to User Mode" else if (userRole == "MODERATOR") "Switch to Moderator Mode" else "Switch to Admin Mode"
                @Suppress("DEPRECATION")
                Text(
                    text = switchText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Logout Section
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.sign_out),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer
        @Suppress("DEPRECATION")
        Text(
            text = stringResource(R.string.rights_reserved),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MutedForeground,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ProfileHeader(
    userId: String,
    username: String,
    streak: Int,
    profilePictureUrl: String = "",
    onEditProfile: () -> Unit,
    onVerifyAccount: () -> Unit
) {
    val displayName = username.ifEmpty { "User" }.replaceFirstChar { it.uppercase() }
    val isGuest = userId.equals("GUEST", ignoreCase = true) || username.equals("Guest", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue.copy(alpha = 0.12f))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val photoModel = remember(profilePictureUrl) {
                                when {
                                    profilePictureUrl.startsWith("http") -> profilePictureUrl
                                    profilePictureUrl.isNotEmpty() && !profilePictureUrl.startsWith("data:") -> java.io.File(profilePictureUrl)
                                    else -> null
                                }
                            }

                            if (photoModel != null) {
                                coil.compose.AsyncImage(
                                    model = photoModel,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = AppIcons.PersonOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = PrimaryBlue
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    @Suppress("DEPRECATION")
                    Text(
                        text = displayName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StreakBadge(streak = streak)
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.edit_profile),
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isGuest) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onVerifyAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(
                            text = "Verify Account",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProfileHeader(
    username: String,
    userRole: String = "ADMIN",
    profilePictureUrl: String = "",
    onEditProfile: () -> Unit
) {
    val displayName = username.ifEmpty { "Admin" }.replaceFirstChar { it.uppercase() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue.copy(alpha = 0.12f))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val photoModel = remember(profilePictureUrl) {
                                when {
                                    profilePictureUrl.startsWith("http") -> profilePictureUrl
                                    profilePictureUrl.isNotEmpty() && !profilePictureUrl.startsWith("data:") -> java.io.File(profilePictureUrl)
                                    else -> null
                                }
                            }

                            if (photoModel != null) {
                                coil.compose.AsyncImage(
                                    model = photoModel,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = AppIcons.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = PrimaryBlue
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    @Suppress("DEPRECATION")
                    Text(
                        text = displayName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val displayRole = if (userRole == "MODERATOR") "MODERATOR" else "ADMINISTRATOR"
                        Text(
                            text = displayRole,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.edit_profile),
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StreakBadge(streak: Int) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Streak,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                @Suppress("DEPRECATION")
                Text(
                    text = "STREAK", 
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedForeground
                )
                @Suppress("DEPRECATION")
                Text(
                    text = "$streak Days", 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRemindersSection(
    userId: String,
    notificationsEnabled: Boolean,
    frequency: String,
    onToggleNotifications: (Boolean) -> Unit,
    onFrequencyChanged: (String) -> Unit,
    onRequestLocationPermission: () -> Unit = {},
    locationAccessEnabled: Boolean,
    dynamicWeatherEnabled: Boolean,
    onToggleLocationAccess: (Boolean) -> Unit,
    onToggleWeatherGoalAdjustment: (Boolean) -> Unit,
    smartRemindersEnabled: Boolean,
    onToggleSmartReminders: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Permissions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.reminders_subtitle),
                fontSize = 14.sp,
                color = MutedForeground,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column {
                    // 1. Location Access for Weather
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Location,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Location Access for Weather",
                            fontSize = 16.sp,
                            color = TextDark,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = locationAccessEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    onRequestLocationPermission()
                                } else {
                                    onToggleLocationAccess(false)
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

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    // 2. Enable Notifications
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Notifications,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = "Enable Notifications",
                            fontSize = 16.sp,
                            color = TextDark,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { onToggleNotifications(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    // 3. Enable Smart Reminders (sub-feature)
                    Row(
                        modifier = Modifier
                            .padding(start = 36.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Lightbulb,
                            contentDescription = null,
                            tint = if (notificationsEnabled) PrimaryBlue else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = "Enable Smart Reminders",
                            fontSize = 15.sp,
                            color = if (notificationsEnabled) TextDark else MutedForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = smartRemindersEnabled && notificationsEnabled,
                            onToggleSmartReminders,
                            enabled = notificationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    // 4. Dynamic Weather Goal Adjustment (sub-feature)
                    Row(
                        modifier = Modifier
                            .padding(start = 36.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Weather,
                            contentDescription = null,
                            tint = if (notificationsEnabled) PrimaryBlue else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dynamic Weather Goal Adjustment",
                            fontSize = 15.sp,
                            color = if (notificationsEnabled) TextDark else MutedForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = dynamicWeatherEnabled && notificationsEnabled,
                            onCheckedChange = { enabled ->
                                onToggleWeatherGoalAdjustment(enabled)
                            },
                            enabled = notificationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.reminder_frequency),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF1F5F9),
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Every 30 mins", "Every 1 hour", "Every 2 hours", "Every 4 hours").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onFrequencyChanged(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementSystemSection(
    prefs: com.example.fluidcheck.util.MeasurementPreferences,
    onVolumeUnitChanged: (com.example.fluidcheck.util.VolumeUnit) -> Unit,
    onWeightUnitChanged: (com.example.fluidcheck.util.WeightUnit) -> Unit,
    onHeightUnitChanged: (com.example.fluidcheck.util.HeightUnit) -> Unit
) {
    var pendingVolume by remember(prefs.volume) { mutableStateOf(prefs.volume) }
    var pendingWeight by remember(prefs.weight) { mutableStateOf(prefs.weight) }
    var pendingHeight by remember(prefs.height) { mutableStateOf(prefs.height) }

    var expandedVolume by remember { mutableStateOf(false) }
    var expandedWeight by remember { mutableStateOf(false) }
    var expandedHeight by remember { mutableStateOf(false) }
    
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val hasChanges = pendingVolume != prefs.volume || pendingWeight != prefs.weight || pendingHeight != prefs.height

    if (isSaving) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* Prevent dismissal */ }, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Preferences",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            @Suppress("DEPRECATION")
            Text(
                text = "Customize the units of measurement used throughout the app.",
                fontSize = 14.sp,
                color = MutedForeground,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Liquids
            Text("Liquids", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedVolume,
                onExpandedChange = { expandedVolume = !expandedVolume },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (pendingVolume == com.example.fluidcheck.util.VolumeUnit.METRIC) "ml" else "oz",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVolume) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF1F5F9),
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedVolume,
                    onDismissRequest = { expandedVolume = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("ml") },
                        onClick = {
                            pendingVolume = com.example.fluidcheck.util.VolumeUnit.METRIC
                            expandedVolume = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("oz") },
                        onClick = {
                            pendingVolume = com.example.fluidcheck.util.VolumeUnit.IMPERIAL
                            expandedVolume = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weight
            Text("Weight", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedWeight,
                onExpandedChange = { expandedWeight = !expandedWeight },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (pendingWeight == com.example.fluidcheck.util.WeightUnit.METRIC) "kg" else "lbs",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeight) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF1F5F9),
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedWeight,
                    onDismissRequest = { expandedWeight = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("kg") },
                        onClick = {
                            pendingWeight = com.example.fluidcheck.util.WeightUnit.METRIC
                            expandedWeight = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("lbs") },
                        onClick = {
                            pendingWeight = com.example.fluidcheck.util.WeightUnit.IMPERIAL
                            expandedWeight = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Height
            Text("Height", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedHeight,
                onExpandedChange = { expandedHeight = !expandedHeight },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (pendingHeight == com.example.fluidcheck.util.HeightUnit.METRIC) "cm" else "ft/in",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHeight) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF1F5F9),
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedHeight,
                    onDismissRequest = { expandedHeight = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("cm") },
                        onClick = {
                            pendingHeight = com.example.fluidcheck.util.HeightUnit.METRIC
                            expandedHeight = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ft/in") },
                        onClick = {
                            pendingHeight = com.example.fluidcheck.util.HeightUnit.IMPERIAL
                            expandedHeight = false
                        }
                    )
                }
            }
            
            if (hasChanges) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            onVolumeUnitChanged(pendingVolume)
                            onWeightUnitChanged(pendingWeight)
                            onHeightUnitChanged(pendingHeight)
                            
                            // Simulate saving delay to give UI feedback and ensure flow collection completes
                            kotlinx.coroutines.delay(1000)
                            
                            isSaving = false
                            android.widget.Toast.makeText(context, "Preferences saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(
                        text = "Save",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SystemStatusSection(
    isConnected: Boolean = true,
    hasPendingWrites: Boolean = false,
    isGuest: Boolean = false
) {
    // Determine the sync indicator color.
    // Muted: Guest user (feature inactive)
    // Green: Connected and fully synced
    // Amber: Connected but has pending writes (syncing/stuck)
    // Red: Device is offline
    val syncColor = when {
        isGuest -> MutedForeground // Feature inactive for guests
        !isConnected -> ErrorRed
        hasPendingWrites -> WarningAmber
        else -> SuccessGreen
    }

    // Determine the cloud backup status text
    val backupStatusText = when {
        isGuest -> stringResource(R.string.disabled)
        hasPendingWrites -> stringResource(R.string.syncing)
        else -> stringResource(R.string.enabled)
    }
    val backupStatusColor = when {
        isGuest -> MutedForeground
        hasPendingWrites -> WarningAmber
        else -> MutedForeground
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Notifications,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.system_status),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                @Suppress("DEPRECATION")
                Text(text = stringResource(R.string.auto_sync), fontSize = 16.sp, color = TextDark)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(syncColor, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                @Suppress("DEPRECATION")
                Text(text = stringResource(R.string.cloud_backup), fontSize = 16.sp, color = TextDark)
                @Suppress("DEPRECATION")
                Text(text = backupStatusText, fontSize = 14.sp, color = backupStatusColor)
            }
        }
    }
}
