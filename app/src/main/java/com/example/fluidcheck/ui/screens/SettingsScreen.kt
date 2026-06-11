package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.components.ProgressMeterStyle
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
    onRestorePurchases: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    autoShieldEnabled: Boolean = false,
    onToggleAutoShield: (Boolean) -> Unit = {},
    appTheme: String = "LIGHT",
    onAppThemeChanged: (String) -> Unit = {},
    appIconBackground: String = "BLUE",
    progressMeterStyle: String = "RING",
    onProgressMeterStyleChanged: (String) -> Unit = {},
    appIcon: String = "DEFAULT",
    onAppIconConfigurationChanged: (String, String) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
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
                    Text(stringResource(R.string.cancel), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
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
                    Text("Cancel", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
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
                userRole = userRole,
                onEditProfile = onEditProfile,
                onVerifyAccount = onVerifyAccount,
                onNavigateToInventory = onNavigateToInventory
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
                Icon(AppIcons.Save, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restore Purchases",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
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

        // Auto-Shield Section
        if (!isAdminMode && userRole != "FREE USER") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = themeColors.cardBackground,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, themeColors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Gamification",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-use Streak Shield", fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Automatically consume a shield at midnight if you missed your goal to preserve your streak.",
                                fontSize = 12.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = autoShieldEnabled,
                            onCheckedChange = { onToggleAutoShield(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Customization Hub
        val isPremium = userRole != "FREE USER" && userRole != "GUEST"

        var showThemeDialog by remember { mutableStateOf(false) }
        var showAppIconDialog by remember { mutableStateOf(false) }
        var showMeterDialog by remember { mutableStateOf(false) }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select App Theme") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AppThemeId.values().forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showThemeDialog = false
                                        onAppThemeChanged(theme.name)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appTheme == theme.name,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(theme.displayName)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
            )
        }

        if (showAppIconDialog) {
            var activeTab by remember { mutableStateOf("Icon") }
            var pendingIcon by remember { mutableStateOf(appIcon) }
            var pendingBg by remember { mutableStateOf(appIconBackground) }
            AlertDialog(
                onDismissRequest = { showAppIconDialog = false },
                title = {
                    Text(
                        text = "App Icon Customization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Segmented Tab bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Icon", "Background").forEach { tab ->
                                val isSelected = activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { activeTab = tab }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            if (activeTab == "Icon") {
                                val icons = listOf(
                                    "DEFAULT", "BLUSH_PINK", "BRONZE", "CHAPAGNE", "CREAM_YELLOW", "CYBER_BLUE", 
                                    "ELECTRIC_PURPLE", "GUNMETAL", "HOT_PINK", "LAVENDER_BLUE", "LIME_GREEN", 
                                    "NEON_ORANGE", "PEACH_FUZZ", "PLATINUM_SILVER", "ROSE_GOLD", "SAGE_GREEN", "SOFT_MINT"
                                )
                                icons.chunked(3).forEach { rowIcons ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowIcons.forEach { iconVariant ->
                                            val isSelected = pendingIcon == iconVariant
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        BorderStroke(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        pendingIcon = iconVariant
                                                    }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Preview Box
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    val bgResId = context.resources.getIdentifier(
                                                        "ic_launcher_bg_${pendingBg.lowercase()}",
                                                        "drawable",
                                                        context.packageName
                                                    )
                                                    if (bgResId != 0) {
                                                        Image(
                                                            painter = painterResource(id = bgResId),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    val fgResId = context.resources.getIdentifier(
                                                        "${iconVariant.lowercase()}_icon",
                                                        "drawable",
                                                        context.packageName
                                                    )
                                                    if (fgResId != 0) {
                                                        Image(
                                                            painter = painterResource(id = fgResId),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = iconVariant.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (rowIcons.size < 3) {
                                            repeat(3 - rowIcons.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Note: Changing the app icon may take a moment to update on your home screen. The app will no longer force-close to apply changes.",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                val backgrounds = listOf(
                                    "NONE", "BLUE", "DARK", "ORANGE", "CYBERPUNK", "MINIMAL",
                                    "DEEP_SPACE", "NORDIC_SLATE", "WARM_SAND", "SAGE_GARDEN", "BURGUNDY",
                                    "AURORA", "SUNSET", "OCEAN", "GRID", "STRIPES"
                                )
                                backgrounds.chunked(3).forEach { rowBgs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowBgs.forEach { bgVariant ->
                                            val isSelected = pendingBg == bgVariant
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        BorderStroke(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        pendingBg = bgVariant
                                                    }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Preview Box
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    val bgResId = context.resources.getIdentifier(
                                                        "ic_launcher_bg_${bgVariant.lowercase()}",
                                                        "drawable",
                                                        context.packageName
                                                    )
                                                    if (bgResId != 0) {
                                                        Image(
                                                            painter = painterResource(id = bgResId),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    val fgResId = context.resources.getIdentifier(
                                                        "${pendingIcon.lowercase()}_icon",
                                                        "drawable",
                                                        context.packageName
                                                    )
                                                    if (fgResId != 0) {
                                                        Image(
                                                            painter = painterResource(id = fgResId),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = bgVariant.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (rowBgs.size < 3) {
                                            repeat(3 - rowBgs.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Note: Changing the app icon may take a moment to update on your home screen. This process might cause the app to close to apply the changes safely.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAppIconDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                showAppIconDialog = false
                                onAppIconConfigurationChanged(pendingIcon, pendingBg)
                            }
                        ) {
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        if (showMeterDialog) {
            AlertDialog(
                onDismissRequest = { showMeterDialog = false },
                title = { Text("Select Progress Meter") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ProgressMeterStyle.values().forEach { meter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMeterDialog = false
                                        onProgressMeterStyleChanged(meter.name)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = progressMeterStyle == meter.name,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(meter.displayName)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showMeterDialog = false }) { Text("Close") } }
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = themeColors.cardBackground,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, themeColors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Customization",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    if (!isPremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Gold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber700
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Theme Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPremium) showThemeDialog = true else showPremiumDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.FormatPaint, contentDescription = null, tint = if(isPremium) androidx.compose.material3.MaterialTheme.colorScheme.primary else MutedForeground)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("App Theme", fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            val themeDisplayName = remember(appTheme) {
                                try {
                                    if (appTheme == "AMOLED_BLACK") AppThemeId.DARK.displayName
                                    else AppThemeId.valueOf(appTheme).displayName
                                } catch (e: Exception) {
                                    AppThemeId.LIGHT.displayName
                                }
                            }
                            Text(themeDisplayName, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = Slate50)

                // Progress Meter Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPremium) showMeterDialog = true else showPremiumDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.PieChart, contentDescription = null, tint = if(isPremium) androidx.compose.material3.MaterialTheme.colorScheme.primary else MutedForeground)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Progress Meter Style", fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            Text(ProgressMeterStyle.valueOf(progressMeterStyle).displayName, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = Slate50)

                // App Icon Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPremium) showAppIconDialog = true else showPremiumDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Goal, contentDescription = null, tint = if(isPremium) androidx.compose.material3.MaterialTheme.colorScheme.primary else MutedForeground)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("App Icon", fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            val iconName = appIcon.lowercase().replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                            val bgName = appIconBackground.lowercase().replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                            Text("$iconName ($bgName Background)", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

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
            Icon(AppIcons.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(8.dp))
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.about_developer),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
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
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
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
    userRole: String = "FREE USER",
    onEditProfile: () -> Unit,
    onVerifyAccount: () -> Unit,
    onNavigateToInventory: () -> Unit = {}
) {
    val displayName = username.ifEmpty { "User" }.replaceFirstChar { it.uppercase() }
    val isGuest = userId.equals("GUEST", ignoreCase = true) || username.equals("Guest", ignoreCase = true)
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = themeColors.cardBackground,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = themeColors.cardBackground,
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
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
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
                if (userRole != "FREE USER") {
                    Button(
                        onClick = onNavigateToInventory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "My Inventory & Badges",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
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
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
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
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = themeColors.cardBackground,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = themeColors.cardBackground,
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
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StreakBadge(streak: Int) {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    Surface(
        color = themeColors.cardBackground,
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                @Suppress("DEPRECATION")
                Text(
                    text = "$streak Days", 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
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

    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = themeColors.cardBackground,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Permissions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.reminders_subtitle),
                fontSize = 14.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = themeColors.cardBackground,
                border = BorderStroke(1.dp, themeColors.cardBorder)
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
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Location Access for Weather",
                            fontSize = 16.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
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
                                checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                    // 2. Enable Notifications
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Notifications,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = "Enable Notifications",
                            fontSize = 16.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { onToggleNotifications(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                            tint = if (notificationsEnabled) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = "Enable Smart Reminders",
                            fontSize = 15.sp,
                            color = if (notificationsEnabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else MutedForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = smartRemindersEnabled && notificationsEnabled,
                            onToggleSmartReminders,
                            enabled = notificationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                            tint = if (notificationsEnabled) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dynamic Weather Goal Adjustment",
                            fontSize = 15.sp,
                            color = if (notificationsEnabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else MutedForeground,
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
                                checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
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
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        }
    }

    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = themeColors.cardBackground,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Preferences",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            @Suppress("DEPRECATION")
            Text(
                text = "Customize the units of measurement used throughout the app.",
                fontSize = 14.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Liquids
            Text("Liquids", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
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
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            Text("Weight", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
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
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            Text("Height", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
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
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
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

    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = themeColors.cardBackground,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Notifications,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.system_status),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                @Suppress("DEPRECATION")
                Text(text = stringResource(R.string.auto_sync), fontSize = 16.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
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
                Text(text = stringResource(R.string.cloud_backup), fontSize = 16.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                @Suppress("DEPRECATION")
                Text(text = backupStatusText, fontSize = 14.sp, color = backupStatusColor)
            }
        }
    }
}
