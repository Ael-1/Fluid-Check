package com.example.fluidcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.fluidcheck.model.MissionPool

@Composable
fun MissionBoardSection(
    userRole: String,
    boardMissionIds: List<String>,
    activeMissions: List<Map<String, Any>>,
    completedMissionsToday: List<String>,
    abortedMissionsToday: List<String>,
    isConnected: Boolean,
    onAcceptMission: suspend (String) -> Unit,
    onAbortMission: suspend (String) -> Unit,
    onCompleteMission: suspend (String) -> Unit,
    onNavigateToInventory: () -> Unit
) {
    if (userRole == "FREE USER") return
    
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    var missionToAccept by remember { mutableStateOf<String?>(null) }
    var missionToAbort by remember { mutableStateOf<String?>(null) }
    var showOfflineError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        color = themeColors.cardBackground,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Missions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Active: ${activeMissions.size}/5",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (boardMissionIds.isEmpty()) {
                    if (isConnected) {
                        // Skeleton load screen
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("Retrieving your missions...", color = Color.Gray)
                        }
                    } else {
                        // Offline and unable to retrieve missions
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Check your connection please try again.", color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { /* Force a recompose or just let the user retry manually when online */ }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                // Sort missions: Active, completed, or aborted missions first
                val sortedMissionIds = boardMissionIds.sortedByDescending { missionId ->
                    val isActive = activeMissions.any { it["missionId"] == missionId }
                    val isCompleted = completedMissionsToday.contains(missionId)
                    val isAborted = abortedMissionsToday.contains(missionId)
                    if (isActive || isCompleted || isAborted) 1 else 0
                }

                sortedMissionIds.forEachIndexed { index, missionId ->
                    val missionDef = MissionPool.getMission(missionId) ?: return@forEachIndexed
                    val isActive = activeMissions.any { it["missionId"] == missionId }
                    val isCompleted = completedMissionsToday.contains(missionId)
                    val isAborted = abortedMissionsToday.contains(missionId)
                    
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(missionDef.difficulty.badgeRarity.color).copy(alpha = 0.15f)
                            ) {
                                val activeMissionData = activeMissions.find { it["missionId"] == missionId }
                                val acceptedAt = (activeMissionData?.get("acceptedAt") as? Number)?.toLong() ?: 0L
                                val timeLeftStr = if (isActive && acceptedAt > 0) {
                                    val expiration = com.example.fluidcheck.model.getMissionExpirationTime(acceptedAt, missionDef)
                                    val msLeft = expiration - System.currentTimeMillis()
                                    if (msLeft <= 0) {
                                        " • Expired"
                                    } else {
                                        val hoursLeft = msLeft / (1000 * 60 * 60)
                                        if (hoursLeft < 24) {
                                            if (hoursLeft < 1) {
                                                val minsLeft = msLeft / (1000 * 60)
                                                " • ${minsLeft}m left"
                                            } else {
                                                " • ${hoursLeft}h left"
                                            }
                                        } else {
                                            val daysLeft = Math.ceil(msLeft / (24.0 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                                            " • ${daysLeft}d left"
                                        }
                                    }
                                } else if (missionDef.difficulty.durationDays > 1) {
                                    " • ${missionDef.difficulty.durationDays}d"
                                } else ""

                                Text(
                                    text = "${missionDef.difficulty.label}$timeLeftStr",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(missionDef.difficulty.badgeRarity.color),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            // Reward preview
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(Color(missionDef.difficulty.badgeRarity.color), CircleShape))
                                if (missionDef.difficulty.shieldFragmentChance > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🛡️", fontSize = 10.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = missionDef.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = missionDef.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isActive) {
                            val activeData = activeMissions.find { it["missionId"] == missionId }
                            val progress = (activeData?.get("progress") as? Number)?.toInt() ?: 0
                            val isFailed = (activeData?.get("failed") as? Boolean) == true
                            if (isFailed) {
                                Button(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = Color.Red.copy(alpha = 0.6f))
                                ) {
                                    Text("Failed ✗")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Abort",
                                        fontSize = 12.sp,
                                        color = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.clickable { missionToAbort = missionId }.padding(4.dp)
                                    )
                                }
                            } else if (progress >= missionDef.targetValue) {
                                Button(
                                    onClick = { scope.launch { onCompleteMission(missionId) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                                ) {
                                    Text("Claim Reward")
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(
                                        progress = { if (missionDef.targetValue > 0) progress.toFloat() / missionDef.targetValue else 0f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = Color(missionDef.difficulty.badgeRarity.color),
                                        trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("$progress / ${missionDef.targetValue}", fontSize = 12.sp, color = Color.Gray)
                                        Text(
                                            "Abort",
                                            fontSize = 12.sp,
                                            color = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.clickable { missionToAbort = missionId }.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        } else if (isCompleted) {
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = Color(0xFF22C55E))
                            ) {
                                Text("Completed ✓")
                            }
                        } else if (isAborted) {
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = Color.Red.copy(alpha = 0.5f))
                            ) {
                                Text("Aborted ✗")
                            }
                        } else {
                            Button(
                                onClick = { 
                                    if (isConnected) {
                                        missionToAccept = missionId 
                                    } else {
                                        showOfflineError = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
                                enabled = activeMissions.size < 5
                            ) {
                                Text(if (activeMissions.size >= 5) "Board Full" else "Accept Mission")
                            }
                        }
                    }
                    if (index < sortedMissionIds.size - 1) {
                        val nextMissionId = sortedMissionIds[index + 1]
                        val nextIsActiveOrFinished = activeMissions.any { it["missionId"] == nextMissionId } || completedMissionsToday.contains(nextMissionId) || abortedMissionsToday.contains(nextMissionId)
                        val currentIsActiveOrFinished = isActive || isCompleted || isAborted
                        
                        if (currentIsActiveOrFinished && !nextIsActiveOrFinished) {
                            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.6f), thickness = 4.dp)
                        } else {
                            HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 2.dp)
                        }
                    }
                }
            }
        }
        
        if (showOfflineError) {
            AlertDialog(
                onDismissRequest = { showOfflineError = false },
                title = { Text("Offline") },
                text = { Text("You need an internet connection to accept a mission. Please try again later.") },
                confirmButton = {
                    Button(onClick = { showOfflineError = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        missionToAbort?.let { missionId ->
            val missionDef = MissionPool.getMission(missionId)
            if (missionDef != null) {
                AlertDialog(
                    onDismissRequest = { missionToAbort = null },
                    title = { Text("Abort Mission") },
                    text = { 
                        Text("Are you sure you want to abort '${missionDef.title}'? You won't be able to attempt this mission again today.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch { onAbortMission(missionId) }
                                missionToAbort = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            Text("Abort", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { missionToAbort = null }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
        
        var isAccepting by remember { mutableStateOf(false) }

        missionToAccept?.let { missionId ->
            val missionDef = MissionPool.getMission(missionId)
            if (missionDef != null) {
                AlertDialog(
                    onDismissRequest = { if (!isAccepting) missionToAccept = null },
                    title = { Text("Accept Mission") },
                    text = {
                        Column {
                            Text(missionDef.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(missionDef.difficulty.badgeRarity.color).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = missionDef.difficulty.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(missionDef.difficulty.badgeRarity.color),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(missionDef.description, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Text("Possible Rewards:", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(Color(missionDef.difficulty.badgeRarity.color), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mission completion badge")
                            }
                            if (missionDef.difficulty.shieldFragmentChance > 0) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛡️", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val chance = missionDef.difficulty.shieldFragmentChance
                                    val probabilityText = when {
                                        chance <= 0.25 -> "low"
                                        chance <= 0.50 -> "fair"
                                        else -> "high"
                                    }
                                    Text("A $probabilityText chance for a Streak Shield Fragment")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    isAccepting = true
                                    onAcceptMission(missionId)
                                    isAccepting = false
                                    missionToAccept = null
                                }
                            },
                            enabled = !isAccepting,
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        ) {
                            if (isAccepting) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Accept")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { missionToAccept = null }, enabled = !isAccepting) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}
