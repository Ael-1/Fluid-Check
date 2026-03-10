package com.example.fluidcheck.ui.components.admin
 
import com.example.fluidcheck.ui.theme.*

import com.example.fluidcheck.ui.admin.*
import com.example.fluidcheck.model.*

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.example.fluidcheck.util.*
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.screens.TimeRangeTabs
import com.example.fluidcheck.ui.screens.DateNavigationBar
import com.example.fluidcheck.ui.screens.HydrationLineChart
import com.example.fluidcheck.ui.screens.getChartDataForRange
import com.example.fluidcheck.ui.screens.generateYLabels
import com.example.fluidcheck.ui.components.profile.EditField
import com.example.fluidcheck.ui.components.profile.PersonalRecordsContainer
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
                        text = stringResource(R.string.selected_count_format, selectedCount),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Delete button
                    FilledTonalButton(
                        onClick = onDeleteSelected,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f),
                            contentColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    // Cancel button
                    TextButton(onClick = onCancelSelection) {
                        Text(stringResource(R.string.cancel), color = MutedForeground, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        } else {
            // Normal header
            Text(
                text = stringResource(R.string.user_directory),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
            Text(
                text = stringResource(R.string.user_directory_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground
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
                title = stringResource(R.string.created_at_label),
                columnWeight = 1.2f,
                column = SortColumn.CREATED_AT,
                activeSortColumn = activeSortColumn,
                activeSortState = activeSortState,
                onClick = { onSortColumnClick(SortColumn.CREATED_AT) }
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = CardBorder.copy(alpha = 0.5f))
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
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) PrimaryBlue else MutedForeground,
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
                        uncheckedColor = MutedForeground.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(2f)) {
                Text(user.username.ifEmpty { "No Username" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextDark)
                Text(user.email, style = MaterialTheme.typography.labelSmall, color = MutedForeground)
            }
            
            Box(modifier = Modifier.weight(1f)) {
                val roleColor = when (user.role) {
                    "ADMIN" -> Color(0xFFDBEAFE)
                    "MODERATOR" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFF1F5F9)
                }
                val roleTextColor = when (user.role) {
                    "ADMIN" -> PrimaryBlue
                    "MODERATOR" -> WarningOrange
                    else -> MutedForeground
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = roleColor,
                    contentColor = roleTextColor
                ) {
                    Text(
                        text = user.role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MutedForeground
                )
            }
        }
        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
    }
}
