package com.tps.ui.admin

/**
 * 文件说明：管理员模块界面，负责后台管理页面的 Compose 展示与交互。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tps.ui.home.TabItem
import com.tps.ui.profile.ProfileViewModel
import com.tps.ui.theme.MarketBgBottom
import com.tps.ui.theme.MarketBgTop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val profileState by profileViewModel.uiState.collectAsState()
    val adminState by adminViewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val tabs = listOf(
        TabItem("用户", Icons.Default.People, "admin_users"),
        TabItem("商品", Icons.Default.Inventory, "admin_products"),
        TabItem("订单", Icons.Default.Receipt, "admin_orders"),
        TabItem("反馈", Icons.Default.SupportAgent, "admin_feedback"),
        TabItem("统计", Icons.Default.BarChart, "admin_stats")
    )
    val currentRoute by navController.currentBackStackEntryAsState()

    LaunchedEffect(profileState.loggedOut) {
        if (profileState.loggedOut) onLogout()
    }

    LaunchedEffect(adminState.sessionExpired) {
        if (adminState.sessionExpired) onLogout()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("管理员后台", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White.copy(alpha = 0.94f), tonalElevation = 10.dp) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute?.destination?.route == tab.route,
                        onClick = { navController.navigate(tab.route) { launchSingleTop = true; popUpTo(navController.graph.startDestinationId) { saveState = true }; restoreState = true } },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController,
            startDestination = "admin_users",
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(MarketBgTop, Color.White, MarketBgBottom)))
                .padding(padding)
        ) {
            composable("admin_users") { AdminUsersScreen(adminViewModel) }
            composable("admin_products") { AdminProductsScreen(adminViewModel) }
            composable("admin_orders") { AdminOrdersScreen(adminViewModel) }
            composable("admin_feedback") { AdminFeedbackScreen(adminViewModel) }
            composable("admin_stats") { AdminStatsScreen(adminViewModel) }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确认退出管理员账号？") },
            confirmButton = {
                TextButton(onClick = { profileViewModel.logout() }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}
