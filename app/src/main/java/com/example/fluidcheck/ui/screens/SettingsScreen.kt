package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.*

private const val ADMIN_EMAIL = "admin@fluidcheck.ai"

@Composable
fun SettingsScreen(
    userId: String = "",
    username: String = "",
    email: String = "",
    streak: Int = 0,
    isDatabaseAdmin: Boolean = false,
    isAdminMode: Boolean = false,
    userRole: String = "USER",
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onVerifyAccount: () -> Unit = {},
    onAboutDeveloper: () -> Unit = {},
    onToggleRole: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Check if this is the core Admin account
    val isPrimaryAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)

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
                onEditProfile = onEditProfile
            )
        } else {
            ProfileHeader(
                userId = userId,
                username = username,
                streak = streak,
                onEditProfile = onEditProfile,
                onVerifyAccount = onVerifyAccount
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isAdminMode) {
            // Smart Reminders Section
            SmartRemindersSection()
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // System Status Section
        SystemStatusSection()

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
                            Icon(
                                imageVector = AppIcons.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = PrimaryBlue
                            )
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
                            Icon(
                                imageVector = AppIcons.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = PrimaryBlue
                            )
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
fun SmartRemindersSection() {
    var remindersEnabled by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("Every 1 hour") }
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.smart_reminders),
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
                        text = stringResource(R.string.enable_reminders),
                        fontSize = 16.sp,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it },
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
                                frequency = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStatusSection() {
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
                        .background(SuccessGreen, CircleShape)
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
                Text(text = stringResource(R.string.enabled), fontSize = 14.sp, color = MutedForeground)
            }
        }
    }
}
