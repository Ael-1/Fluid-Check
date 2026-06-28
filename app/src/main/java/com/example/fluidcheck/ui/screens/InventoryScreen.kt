package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.model.BadgeDefinition
import com.example.fluidcheck.model.BadgeRarity
import com.example.fluidcheck.model.MissionPool
import com.example.fluidcheck.model.UserRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    userRecord: UserRecord,
    onBack: () -> Unit,
    onToggleAutoShield: (Boolean) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedBadge by remember { mutableStateOf<BadgeDefinition?>(null) }
    var selectedConsumable by remember { mutableStateOf<String?>(null) } // "shield" or "fragment"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory & Badges") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Consumables") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Badge Gallery") }
                )
            }

            if (selectedTabIndex == 0) {
                ConsumablesTab(
                    shields = userRecord.streakShields,
                    fragments = userRecord.shieldFragments,
                    onShieldClick = { selectedConsumable = "shield" },
                    onFragmentClick = { selectedConsumable = "fragment" }
                )
            } else {
                BadgeGalleryTab(
                    earnedBadges = userRecord.earnedBadges,
                    onBadgeClick = { selectedBadge = it }
                )
            }
        }
    }

    if (selectedBadge != null) {
        BadgeDetailsDialog(
            badge = selectedBadge!!,
            count = userRecord.earnedBadges[selectedBadge!!.id] ?: 0,
            onDismiss = { selectedBadge = null }
        )
    }

    if (selectedConsumable != null) {
        ConsumableDetailsDialog(
            type = selectedConsumable!!,
            shields = userRecord.streakShields,
            fragments = userRecord.shieldFragments,
            autoShieldEnabled = userRecord.autoShieldEnabled,
            onToggleAutoShield = onToggleAutoShield,
            onDismiss = { selectedConsumable = null }
        )
    }
}

@Composable
fun ConsumablesTab(
    shields: Int,
    fragments: Int,
    onShieldClick: () -> Unit,
    onFragmentClick: () -> Unit
) {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        Text("Your Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Shield Item
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onShieldClick() },
            shape = RoundedCornerShape(16.dp),
            color = themeColors.cardBackground,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("🛡️", fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Streak Shield", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Protects your streak if you miss your goal.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text("x$shields", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fragments Item
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onFragmentClick() },
            shape = RoundedCornerShape(16.dp),
            color = themeColors.cardBackground,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).background(Color(0xFF8B5CF6).copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("🧩", fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shield Fragments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Collect 10 to automatically craft a full Streak Shield.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fragments / 10f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF8B5CF6),
                        trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("$fragments/10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
            }
        }
    }
}

@Composable
fun BadgeGalleryTab(
    earnedBadges: Map<String, Int>,
    onBadgeClick: (BadgeDefinition) -> Unit
) {
    val missionBadges = MissionPool.missionBadges
    val milestoneBadges = MissionPool.milestoneBadges
    var selectedFilter by remember { mutableStateOf<BadgeRarity?>(null) }
    
    val filteredMissionBadges = if (selectedFilter == null) missionBadges else missionBadges.filter { it.rarity == selectedFilter }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    "Mission Badges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All") }
                        )
                    }
                    items(BadgeRarity.values().filter { it != BadgeRarity.MILESTONE }) { rarity ->
                        FilterChip(
                            selected = selectedFilter == rarity,
                            onClick = { selectedFilter = rarity },
                            label = { Text(rarity.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(rarity.color).copy(alpha = 0.2f),
                                selectedLabelColor = Color(rarity.color)
                            )
                        )
                    }
                }
            }
        }
        
        items(filteredMissionBadges) { badge ->
            BadgeItem(
                badge = badge,
                count = earnedBadges[badge.id] ?: 0,
                onClick = { onBadgeClick(badge) }
            )
        }
        
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Milestone Badges",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 8.dp)
            )
        }
        
        items(milestoneBadges) { badge ->
            BadgeItem(
                badge = badge,
                count = earnedBadges[badge.id] ?: 0,
                onClick = { onBadgeClick(badge) }
            )
        }
    }
}

@Composable
fun BadgeItem(
    badge: BadgeDefinition,
    count: Int,
    onClick: () -> Unit
) {
    val isEarned = count > 0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    if (isEarned) Color(badge.rarity.color).copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f),
                    CircleShape
                )
                .border(
                    2.dp,
                    if (isEarned) Color(badge.rarity.color) else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconEmoji = when(badge.rarity) {
                BadgeRarity.COMMON -> "⚪"
                BadgeRarity.UNCOMMON -> "🔵"
                BadgeRarity.RARE -> "🟣"
                BadgeRarity.EPIC -> "🟡"
                BadgeRarity.MILESTONE -> "🟢"
            }
            Text(
                text = if (isEarned) iconEmoji else "🔒",
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            
            if (count > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("x$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = badge.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEarned) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else Color.Gray,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun BadgeDetailsDialog(badge: BadgeDefinition, count: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(badge.rarity.color).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge.rarity.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(badge.rarity.color),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(badge.name, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(badge.description, textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                if (count > 0) {
                    Text("Earned $count time${if (count > 1) "s" else ""}", fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                } else {
                    Text("Not yet earned", color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ConsumableDetailsDialog(
    type: String,
    shields: Int,
    fragments: Int,
    autoShieldEnabled: Boolean,
    onToggleAutoShield: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "shield") "Streak Shield" else "Shield Fragments") },
        text = {
            if (type == "shield") {
                Column {
                    Text("A Streak Shield automatically protects your streak from resetting if you miss your daily goal.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You currently have $shields shield(s).")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Auto-use Shield")
                        Switch(
                            checked = autoShieldEnabled,
                            onCheckedChange = { onToggleAutoShield(it) }
                        )
                    }
                    Text("If enabled, a shield will be consumed automatically at midnight if you missed your goal.", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                Column {
                    Text("Earn Shield Fragments by completing daily missions.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You currently have $fragments/10 fragments.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Collect 10 fragments to automatically craft a full Streak Shield.", color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
