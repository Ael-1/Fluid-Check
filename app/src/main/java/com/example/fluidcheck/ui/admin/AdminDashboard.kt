package com.example.fluidcheck.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluidcheck.R
import com.example.fluidcheck.model.UserCredentials
import com.example.fluidcheck.ui.theme.*

@Composable
fun AdminDashboard() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForEdit by remember { mutableStateOf<UserCredentials?>(null) }
    var selectedUserForDelete by remember { mutableStateOf<UserCredentials?>(null) }
    
    val users = remember { mutableStateListOf(
        UserCredentials("Ael", "ael@example.com", "USER", "12d"),
        UserCredentials("Admin User", "admin@fluidcheck.ai", "ADMIN", "45d"),
        UserCredentials("Sarah Connor", "sarah@resistance.com", "USER", "5d"),
        UserCredentials("John Doe", "john@gmail.com", "USER", "0d"),
        UserCredentials("James Bond", "007@mi6.gov.uk", "USER", "7d")
    )}

    if (selectedUserForEdit != null) {
        EditUserDialog(
            user = selectedUserForEdit!!,
            onDismiss = { selectedUserForEdit = null },
            onSave = { updatedUser ->
                val index = users.indexOfFirst { it.email == selectedUserForEdit?.email }
                if (index != -1) {
                    users[index] = updatedUser
                }
                selectedUserForEdit = null
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
                        users.remove(selectedUserForDelete)
                        selectedUserForDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
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
            AnalyticsGrid()
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

        items(users.filter { it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true) }) { user ->
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

@Composable
fun EditUserDialog(
    user: UserCredentials,
    onDismiss: () -> Unit,
    onSave: (UserCredentials) -> Unit
) {
    var displayName by remember { mutableStateOf(user.name) }
    var emailAddress by remember { mutableStateOf(user.email) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.Done, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.edit_profile), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(AppIcons.Close, contentDescription = stringResource(R.string.close), tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
                
                Text(
                    stringResource(R.string.modify_user_details),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(stringResource(R.string.display_name_label).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(stringResource(R.string.email_label).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = emailAddress,
                        modifier = Modifier.padding(16.dp),
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Icon(AppIcons.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Button(
                        onClick = { onSave(user.copy(name = displayName, email = emailAddress)) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.total_users),
                value = "5",
                icon = AppIcons.Group,
                iconColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = stringResource(R.string.downloads),
                value = "1,284",
                icon = AppIcons.Download,
                iconColor = Color(0xFF22C55E),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.active_now),
                value = "42",
                icon = AppIcons.Goal, // Use appropriate icon from AppIcons
                iconColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = stringResource(R.string.avg_streak),
                value = "14.2d",
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
        Text(
            text = stringResource(R.string.user_directory),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
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
            Text(stringResource(R.string.user_column), modifier = Modifier.weight(2f), fontSize = 14.sp, color = Color.Gray)
            Text(stringResource(R.string.role_column), modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Gray)
            Text(stringResource(R.string.streak_column), modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Gray)
            Text(stringResource(R.string.actions_column), modifier = Modifier.width(60.dp), fontSize = 14.sp, color = Color.Gray)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
    }
}

@Composable
fun UserRow(
    user: UserCredentials,
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
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
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
                Text(user.streak, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
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

@Preview(showBackground = true, backgroundColor = 0xFFF0F9FF)
@Composable
fun PreviewAdminDashboard() {
    AdminDashboard()
}
