package com.tps.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange
import com.tps.ui.theme.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("用户管理", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("用户治理", "查看信用与账号状态，快速封禁异常用户。") }
            items(uiState.users) { user ->
                MarketCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(user.nickname, fontWeight = FontWeight.Bold)
                            Text("信用分：${user.creditScore}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(user.status, if (user.status == "BANNED") MaterialTheme.colorScheme.error else MarketOrange)
                        IconButton(onClick = { viewModel.banUser(user.id, user.status == "BANNED") }) {
                            Icon(
                                if (user.status == "BANNED") Icons.Default.Block else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (user.status == "BANNED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("商品审核", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("商品治理", "处理举报、下架违规商品，维护校园交易环境。") }
            items(uiState.reportedProducts) { report ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("商品ID：${report.productId}", style = MaterialTheme.typography.titleMedium)
                        Text("举报原因：${report.reason ?: "无"}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.takeDownProduct(report.productId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("下架")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("订单管理", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("订单看板", "集中查看平台交易状态和退款风险。") }
            items(uiState.orders) { order ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("订单 #${order.id}", style = MaterialTheme.typography.labelSmall)
                        Text("商品ID：${order.productId}", style = MaterialTheme.typography.titleMedium)
                        Text("状态：${order.status} | ¥${order.price}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("数据统计", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MarketHeroCard("运营数据", "用户、商品、订单和交易额一屏掌握。")
            StatCard("总用户数", uiState.stats?.totalUsers?.toString() ?: "-")
            StatCard("总商品数", uiState.stats?.totalProducts?.toString() ?: "-")
            StatCard("总订单数", uiState.stats?.totalOrders?.toString() ?: "-")
            StatCard("总交易额", uiState.stats?.totalAmount?.let { "¥$it" } ?: "-")
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MarketOrange, fontWeight = FontWeight.ExtraBold)
        }
    }
}
