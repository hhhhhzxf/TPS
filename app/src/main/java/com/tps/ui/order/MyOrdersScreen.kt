package com.tps.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketEmptyState
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange
import com.tps.ui.theme.StatusPill

val ORDER_STATUS_LABEL = mapOf(
    "PENDING" to "待付款",
    "PAID" to "待发货",
    "SHIPPED" to "待收货",
    "DONE" to "已完成",
    "CANCELLED" to "已取消"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(initialRole: String? = null, viewModel: OrderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("buyer" to "我买的", "seller" to "我卖的")
    
    LaunchedEffect(initialRole) {
        if (initialRole != null) {
            viewModel.switchRole(initialRole)
        }
    }
    
    val selectedIndex = if (uiState.role == "buyer") 0 else 1

    uiState.error?.let { err ->
        LaunchedEffect(err) {
            viewModel.clearError()
        }
    }

    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("我的订单", fontWeight = FontWeight.Bold) }) }) { padding ->
        MarketBackground {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MarketHeroCard(
                title = "交易订单",
                subtitle = "付款、发货、收货状态集中管理，减少线下交易纠纷。",
                modifier = Modifier.padding(12.dp)
            )
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = MarketOrange
            ) {
                tabs.forEachIndexed { index, (role, label) ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.switchRole(role) },
                        text = { Text(label) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.orders.isEmpty()) {
                MarketEmptyState("暂无订单", "看中商品后点击立即购买，就会出现在这里。")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            isBuyer = uiState.role == "buyer",
                            onPay = { viewModel.pay(order.id) },
                            onShip = { viewModel.ship(order.id) },
                            onConfirm = { viewModel.confirmReceived(order.id) },
                            onCancel = { viewModel.cancel(order.id) }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun OrderCard(
    order: com.tps.data.remote.dto.OrderDto,
    isBuyer: Boolean,
    onPay: () -> Unit,
    onShip: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val statusLabel = ORDER_STATUS_LABEL[order.status] ?: order.status
    val statusColor = when (order.status) {
        "PENDING" -> MaterialTheme.colorScheme.error
        "PAID", "SHIPPED" -> MaterialTheme.colorScheme.primary
        "DONE" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.productTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusPill(statusLabel, statusColor)
            }
            Text("¥%.2f".format(order.price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE93600))
            Text("订单号：${order.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 操作按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isBuyer && order.status == "PENDING") {
                    Button(onClick = onPay, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)) { Text("立即付款") }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消订单") }
                }
                if (isBuyer && order.status == "SHIPPED") {
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("确认收货") }
                }
                if (!isBuyer && order.status == "PENDING") {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消订单") }
                }
                if (!isBuyer && order.status == "PAID") {
                    Button(onClick = onShip, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)) { Text("确认发货") }
                }
            }
        }
    }
}
