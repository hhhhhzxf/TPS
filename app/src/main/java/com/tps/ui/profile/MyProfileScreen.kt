package com.tps.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tps.ui.theme.AppAsyncImage
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange
import com.tps.ui.theme.StatusPill
import com.tps.util.resolveMediaUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onLogout: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToOrders: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.updateAvatar(it) }
    }

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLogout()
    }

    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            showEditDialog = false
            viewModel.clearUpdateSuccess()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑资料")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录")
                    }
                }
            )
        }
    ) { padding ->
        MarketBackground {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MarketHeroCard("我的校园淘", "管理资料、信用分和账号状态，让交易更可信。")
                MarketCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF0E6))
                                .clickable { avatarPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            AppAsyncImage(
                                url = resolveMediaUrl(uiState.profile?.avatarUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            if (uiState.profile?.avatarUrl.isNullOrBlank()) {
                                Text("头像", color = MarketOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(uiState.profile?.nickname ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        StatusPill("信用分 ${uiState.profile?.creditScore ?: 0}", MarketOrange)
                        uiState.profile?.studentId?.let { if (it.isNotBlank()) Text("学号：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        uiState.profile?.bio?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        uiState.profile?.location?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        uiState.profile?.shippingAddress?.let { if (it.isNotBlank()) Text("收货地址：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                
                MarketCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToMyProducts() }) {
                            Text(uiState.profile?.productCount?.toString() ?: "0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF241713))
                            Text("我发布的", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToOrders("seller") }) {
                            Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF241713))
                            Text("我卖出的", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToOrders("buyer") }) {
                            Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF241713))
                            Text("我买到的", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToFavorites() }) {
                            Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF241713))
                            Text("我收藏的", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                MarketCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onNavigateToHistory() }) {
                            Icon(Icons.Default.Edit, null, tint = MarketOrange)
                            Text("浏览历史", modifier = Modifier.padding(start = 12.dp).weight(1f), fontSize = 15.sp)
                            Text(">", color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onNavigateToFeedback() }) {
                            Icon(Icons.Default.ExitToApp, null, tint = MarketOrange)
                            Text("客服/反馈", modifier = Modifier.padding(start = 12.dp).weight(1f), fontSize = 15.sp)
                            Text(">", color = Color.Gray)
                        }
                    }
                }

                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF241713))
                ) {
                    Text("退出登录")
                }
            }
        }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确认退出登录？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            current = uiState.profile,
            onDismiss = { showEditDialog = false },
            onConfirm = { nickname, bio, location, shippingAddress ->
                viewModel.updateProfile(nickname, bio, location, shippingAddress)
            }
        )
    }
}

@Composable
private fun EditProfileDialog(
    current: com.tps.data.remote.dto.UserProfile?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var nickname by remember { mutableStateOf(current?.nickname ?: "") }
    var bio by remember { mutableStateOf(current?.bio ?: "") }
    var location by remember { mutableStateOf(current?.location ?: "") }
    var shippingAddress by remember { mutableStateOf(current?.shippingAddress ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("简介") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("位置") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                OutlinedTextField(
                    value = shippingAddress,
                    onValueChange = { shippingAddress = it },
                    label = { Text("收货地址") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nickname, bio, location, shippingAddress) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
