package com.example.fluidcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.model.MissionPool

@Composable
fun ActiveMissionTracker(
    activeMissions: List<Map<String, Any>>,
    userRole: String,
    onNavigateToProgress: () -> Unit
) {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    if (userRole == "FREE USER") return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = "Active Missions",
            style = MaterialTheme.typography.titleSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (activeMissions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToProgress() },
                shape = RoundedCornerShape(16.dp),
                color = themeColors.cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder)
            ) {
                Text(
                    text = "No active missions. Tap to view the board.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeMissions) { missionData ->
                    val missionId = missionData["missionId"] as? String ?: return@items
                    val progress = (missionData["progress"] as? Number)?.toInt() ?: 0
                    val missionDef = MissionPool.getMission(missionId) ?: return@items
                    
                    Surface(
                        modifier = Modifier.width(160.dp).clickable { onNavigateToProgress() },
                        shape = RoundedCornerShape(16.dp),
                        color = themeColors.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(8.dp).background(
                                        Color(missionDef.difficulty.badgeRarity.color), CircleShape
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = missionDef.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val isFailed = (missionData["failed"] as? Boolean) == true
                            if (isFailed) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Failed ✗",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { if (missionDef.targetValue > 0) progress.toFloat() / missionDef.targetValue else 0f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(missionDef.difficulty.badgeRarity.color),
                                    trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$progress / ${missionDef.targetValue}",
                                    fontSize = 10.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
