package com.tps.ui.admin

/**
 * 文件说明：管理员模块界面，负责后台管理页面的 Compose 展示与交互。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.data.remote.dto.FeedbackDto
import com.tps.data.remote.dto.OrderDto
import com.tps.data.remote.dto.ProductDto
import com.tps.data.remote.dto.ReportDto
import com.tps.ui.theme.AppAsyncImage
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketEmptyState
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange
import com.tps.ui.theme.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var keyword by remember { mutableStateOf(uiState.userKeyword) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("全部" to null, "正常" to "ACTIVE", "已封禁" to "BANNED", "已注销" to "DEACTIVATED")
    val sortOptions = listOf(
        "创建时间新到旧" to ("createdAt" to "desc"),
        "昵称 A-Z" to ("nickname" to "asc"),
        "手机号升序" to ("phone" to "asc"),
        "信用分高到低" to ("creditScore" to "desc")
    )
    val currentStatusLabel = statusOptions.firstOrNull { it.second == uiState.userStatus }?.first ?: "全部"
    val currentSortLabel = sortOptions.firstOrNull { it.second.first == uiState.userSort && it.second.second == uiState.userDirection }?.first ?: "创建时间新到旧"

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("用户管理", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("用户治理", "管理禁言、禁发与账号封禁，控制异常行为的影响范围。") }
            item {
                MarketCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("搜索昵称 / 手机号 / 学号") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(
                                expanded = statusMenuExpanded,
                                onExpandedChange = { statusMenuExpanded = !statusMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = currentStatusLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("状态") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = statusMenuExpanded,
                                    onDismissRequest = { statusMenuExpanded = false }
                                ) {
                                    statusOptions.forEach { (label, value) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                statusMenuExpanded = false
                                                viewModel.loadUsers(keyword = keyword, status = value)
                                            }
                                        )
                                    }
                                }
                            }
                            ExposedDropdownMenuBox(
                                expanded = sortMenuExpanded,
                                onExpandedChange = { sortMenuExpanded = !sortMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = currentSortLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("排序") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    sortOptions.forEach { (label, sortPair) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                sortMenuExpanded = false
                                                viewModel.loadUsers(
                                                    keyword = keyword,
                                                    status = uiState.userStatus,
                                                    sort = sortPair.first,
                                                    direction = sortPair.second
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { viewModel.loadUsers(keyword = keyword) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
                        ) {
                            Text("搜索")
                        }
                    }
                }
            }
            item { SectionHeader("用户列表", "${uiState.users.size} 人") }
            items(uiState.users) { user ->
                MarketCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(user.nickname, fontWeight = FontWeight.Bold)
                                Text("手机号：${user.phone ?: "-"} | 学号：${user.studentId ?: "-"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("信用分：${user.creditScore} | 商品数：${user.productCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(statusLabel(user.status), if (user.status == "BANNED") MaterialTheme.colorScheme.error else MarketOrange)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            AssistChip(
                                onClick = { viewModel.setUserMuted(user.id, !user.muted) },
                                label = { Text(if (user.muted) "解除禁言" else "禁止发言") },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (user.muted) MarketOrange else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            AssistChip(
                                onClick = { viewModel.setUserPublishBanned(user.id, !user.publishBanned) },
                                label = { Text(if (user.publishBanned) "解除禁发" else "禁止发布") },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (user.publishBanned) MarketOrange else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            AssistChip(
                                onClick = { viewModel.banUser(user.id, user.status == "BANNED") },
                                leadingIcon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)) },
                                label = { Text(if (user.status == "BANNED") "解除封禁" else "封禁账号") },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (user.status == "BANNED") MarketOrange else MaterialTheme.colorScheme.error,
                                    leadingIconContentColor = if (user.status == "BANNED") MarketOrange else MaterialTheme.colorScheme.error
                                )
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (user.muted) StatusPill("已禁言", MaterialTheme.colorScheme.error)
                            if (user.publishBanned) StatusPill("已禁发", MaterialTheme.colorScheme.error)
                            if (user.role == "ADMIN") StatusPill("管理员", MarketOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: String): String {
    return when (status) {
        "ACTIVE" -> "正常"
        "BANNED" -> "已封禁"
        "DEACTIVATED" -> "已注销"
        else -> status
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingTakedown by remember { mutableStateOf<ProductDto?>(null) }
    var takedownReason by remember { mutableStateOf("") }
    var pendingReportTakedown by remember { mutableStateOf<ReportDto?>(null) }
    var reportTakedownReason by remember { mutableStateOf("") }
    var pendingReportReject by remember { mutableStateOf<ReportDto?>(null) }
    var reportRejectReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadListedProducts()
        viewModel.loadReportedProducts()
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    uiState.selectedProduct?.let { product ->
        AdminProductDetailDialog(
            product = product,
            operating = uiState.operatingProductId == product.id,
            onDismiss = viewModel::dismissProductDetail,
            onTakedown = {
                pendingTakedown = product
                takedownReason = product.takedownReason.orEmpty()
            }
        )
    }

    pendingTakedown?.let { product ->
        AlertDialog(
            onDismissRequest = {
                pendingTakedown = null
                takedownReason = ""
            },
            title = { Text("强制下架商品") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("商品：${product.title}")
                    OutlinedTextField(
                        value = takedownReason,
                        onValueChange = { takedownReason = it.take(255) },
                        label = { Text("下架原因") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${takedownReason.length}/255", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.takedownProduct(product.id, takedownReason)
                        pendingTakedown = null
                        takedownReason = ""
                    },
                    enabled = takedownReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认下架")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingTakedown = null
                    takedownReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
    pendingReportTakedown?.let { report ->
        AlertDialog(
            onDismissRequest = {
                pendingReportTakedown = null
                reportTakedownReason = ""
            },
            title = { Text("处理举报并下架") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(report.productTitle ?: "商品ID：${report.productId}")
                    Text("用户举报原因：${report.reason ?: "无"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = reportTakedownReason,
                        onValueChange = { reportTakedownReason = it.take(255) },
                        label = { Text("平台下架原因") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${reportTakedownReason.length}/255", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleReport(report.id, true, reportTakedownReason)
                        pendingReportTakedown = null
                        reportTakedownReason = ""
                    },
                    enabled = reportTakedownReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认下架")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingReportTakedown = null
                    reportTakedownReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
    pendingReportReject?.let { report ->
        AlertDialog(
            onDismissRequest = {
                pendingReportReject = null
                reportRejectReason = ""
            },
            title = { Text("驳回举报") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(report.productTitle ?: "商品ID：${report.productId}")
                    Text("用户举报原因：${report.reason ?: "无"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = reportRejectReason,
                        onValueChange = { reportRejectReason = it.take(255) },
                        label = { Text("驳回原因") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${reportRejectReason.length}/255", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleReport(report.id, false, reportRejectReason)
                        pendingReportReject = null
                        reportRejectReason = ""
                    },
                    enabled = reportRejectReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
                ) {
                    Text("确认驳回")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingReportReject = null
                    reportRejectReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("商品审核", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("商品治理", "查看上架商品，填写原因后强制下架违规内容。") }

            item {
                SectionHeader("上架中商品", "${uiState.listedProducts.size} 件")
            }

            if (uiState.listedProducts.isEmpty()) {
                item {
                    MarketCard {
                        Text("暂无上架中商品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.listedProducts, key = { it.id }) { product ->
                    AdminListedProductCard(
                        product = product,
                        operating = uiState.operatingProductId == product.id,
                        loadingDetail = uiState.loadingProductDetailId == product.id,
                        onViewDetail = { viewModel.loadProductDetail(product.id) },
                        onTakedown = {
                            pendingTakedown = product
                            takedownReason = ""
                        }
                    )
                }
            }

            item {
                SectionHeader("待处理举报", "${uiState.reportedProducts.size} 条")
            }

            items(uiState.reportedProducts) { report ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppAsyncImage(
                                url = report.productImageUrl,
                                contentDescription = report.productTitle,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFE1D2)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(report.productTitle ?: "商品ID：${report.productId}", style = MaterialTheme.typography.titleMedium)
                                Text("商品ID：${report.productId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("举报原因：${report.reason ?: "无"}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.loadProductDetail(report.productId) }) {
                                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("查看商品")
                            }
                            OutlinedButton(onClick = {
                                pendingReportReject = report
                                reportRejectReason = ""
                            }) {
                                Text("驳回举报")
                            }
                            Button(onClick = {
                                pendingReportTakedown = report
                                reportTakedownReason = ""
                            },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("处理并下架")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        StatusPill(count, MarketOrange)
    }
}

@Composable
private fun AdminListedProductCard(
    product: ProductDto,
    operating: Boolean,
    loadingDetail: Boolean,
    onViewDetail: () -> Unit,
    onTakedown: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAsyncImage(
                    url = product.imageUrls.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFE1D2)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(product.title, fontWeight = FontWeight.Bold)
                    Text("卖家：${product.sellerNickname} | ¥${product.price}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("商品ID：${product.id} | ${product.category ?: "未分类"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill("在售", MarketOrange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (operating || loadingDetail) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                }
                OutlinedButton(
                    onClick = onViewDetail,
                    enabled = !loadingDetail
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("详情")
                }
                Button(
                    onClick = onTakedown,
                    enabled = !operating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("强制下架")
                }
            }
        }
    }
}

@Composable
private fun AdminProductDetailDialog(
    product: ProductDto,
    operating: Boolean,
    onDismiss: () -> Unit,
    onTakedown: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (product.status != "SOLD") {
                Button(
                    onClick = onTakedown,
                    enabled = !operating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (product.status == "OFF") "更新下架原因" else "强制下架")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("关闭")
            }
        },
        title = { Text(product.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppAsyncImage(
                    url = product.imageUrls.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFE1D2)),
                    contentScale = ContentScale.Crop
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥${product.price}", fontWeight = FontWeight.ExtraBold, color = MarketOrange, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.weight(1f))
                    StatusPill(productStatusLabel(product.status), if (product.status == "OFF") MaterialTheme.colorScheme.error else MarketOrange)
                }
                Text(product.description ?: "暂无描述", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                DetailLine("卖家", "${product.sellerNickname}（ID ${product.userId}）")
                DetailLine("分类/成色", "${product.category ?: "未分类"} / ${product.condition ?: "未标注"}")
                DetailLine("地点", product.location ?: "未填写")
                DetailLine("浏览/收藏", "${product.viewCount} / ${product.favoriteCount}")
                DetailLine("创建时间", product.createdAt ?: "-")
                if (!product.takedownReason.isNullOrBlank()) {
                    Text(
                        "下架原因：${product.takedownReason}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF0F0))
                            .padding(10.dp)
                    )
                    DetailLine("下架管理员", product.takedownBy?.toString() ?: "-")
                    DetailLine("下架时间", product.takedownAt ?: "-")
                }
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun productStatusLabel(status: String): String {
    return when (status) {
        "ON_SALE", "AVAILABLE" -> "在售"
        "OFF" -> "已下架"
        "SOLD" -> "已售出"
        else -> status
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var rejectingOrder by remember { mutableStateOf<OrderDto?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    rejectingOrder?.let { order ->
        AlertDialog(
            onDismissRequest = {
                rejectingOrder = null
                rejectReason = ""
            },
            title = { Text("驳回退款申请") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("订单 #${order.id}，商品：${order.productTitle}")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it.take(255) },
                        label = { Text("驳回原因") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectRefund(order.id, rejectReason)
                        rejectingOrder = null
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认驳回")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    rejectingOrder = null
                    rejectReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("订单管理", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MarketHeroCard("订单看板", "集中查看平台交易状态和退款风险。") }
            items(uiState.orders) { order ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("订单 #${order.id}", style = MaterialTheme.typography.labelSmall)
                        Text(order.productTitle.ifBlank { "商品ID：${order.productId}" }, style = MaterialTheme.typography.titleMedium)
                        Text("买家：${order.buyerNickname ?: order.buyerId} | 卖家：${order.sellerNickname ?: order.sellerId}")
                        Text("状态：${order.status} | ¥${order.price}")
                        if (!order.remark.isNullOrBlank()) {
                            Text("备注：${order.remark}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (order.status == "REFUNDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.operatingOrderId == order.id) {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        rejectingOrder = order
                                        rejectReason = ""
                                    },
                                    enabled = uiState.operatingOrderId != order.id
                                ) {
                                    Text("驳回退款")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.approveRefund(order.id) },
                                    enabled = uiState.operatingOrderId != order.id
                                ) {
                                    Text("通过退款")
                                }
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
fun AdminFeedbackScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var replyingFeedback by remember { mutableStateOf<FeedbackDto?>(null) }
    var replyText by remember { mutableStateOf("") }
    val statusFilters = listOf<Pair<String, String?>>(
        "全部" to null,
        "待处理" to "PENDING",
        "处理中" to "PROCESSING",
        "已完成" to "DONE",
        "已关闭" to "CLOSED"
    )

    LaunchedEffect(Unit) {
        viewModel.loadFeedback(selectedStatus)
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    replyingFeedback?.let { feedback ->
        AlertDialog(
            onDismissRequest = {
                replyingFeedback = null
                replyText = ""
            },
            title = { Text("回复反馈") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(feedback.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it.take(1000) },
                        label = { Text("回复内容") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${replyText.length}/1000", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.replyFeedback(feedback.id, replyText)
                        replyingFeedback = null
                        replyText = ""
                    },
                    enabled = replyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
                ) {
                    Text("发送回复")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    replyingFeedback = null
                    replyText = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("反馈处理", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { MarketHeroCard("用户反馈", "查看问题和建议，回复处理结果，沉淀平台运营线索。") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusFilters.forEach { (label, status) ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = {
                                selectedStatus = status
                                viewModel.loadFeedback(status)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                SectionHeader("反馈列表", "${uiState.feedback.size} 条")
            }

            if (uiState.feedback.isEmpty()) {
                item {
                    MarketCard {
                        MarketEmptyState("暂无反馈", "当前筛选条件下没有用户反馈")
                    }
                }
            } else {
                items(uiState.feedback, key = { it.id }) { feedback ->
                    AdminFeedbackCard(
                        feedback = feedback,
                        operating = uiState.operatingFeedbackId == feedback.id,
                        onReply = {
                            replyingFeedback = feedback
                            replyText = feedback.reply.orEmpty()
                        },
                        onStatus = { status -> viewModel.updateFeedbackStatus(feedback.id, status) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminFeedbackCard(
    feedback: FeedbackDto,
    operating: Boolean,
    onReply: () -> Unit,
    onStatus: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(feedback.type, fontWeight = FontWeight.Bold, color = MarketOrange)
                    Text("用户ID：${feedback.userId}${feedback.userNickname?.let { " | $it" } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(feedback.status, feedbackStatusColor(feedback.status))
            }

            Text(feedback.content, style = MaterialTheme.typography.bodyMedium)

            if (!feedback.contact.isNullOrBlank()) {
                Text("联系方式：${feedback.contact}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!feedback.reply.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFFFF4ED),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("平台回复", fontWeight = FontWeight.Bold, color = MarketOrange)
                        Text(feedback.reply, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text(
                listOfNotNull(feedback.createdAt?.let { "提交：$it" }, feedback.updatedAt?.let { "更新：$it" }).joinToString("  "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (operating) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                }
                TextButton(onClick = { onStatus("PROCESSING") }, enabled = !operating && feedback.status != "PROCESSING") {
                    Text("处理中")
                }
                TextButton(onClick = { onStatus("DONE") }, enabled = !operating && feedback.status != "DONE") {
                    Text("完成")
                }
                TextButton(onClick = { onStatus("CLOSED") }, enabled = !operating && feedback.status != "CLOSED") {
                    Text("关闭")
                }
                Button(
                    onClick = onReply,
                    enabled = !operating,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
                ) {
                    Text("回复")
                }
            }
        }
    }
}

@Composable
private fun feedbackStatusColor(status: String): Color = when (status) {
    "DONE" -> Color(0xFF2E7D32)
    "CLOSED" -> MaterialTheme.colorScheme.outline
    "PROCESSING" -> Color(0xFF1976D2)
    else -> MarketOrange
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("数据统计", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MarketHeroCard("运营数据", "用户、商品、订单和交易额一屏掌握。")
            StatCard("总用户数", uiState.stats?.totalUsers?.toString() ?: "-")
            StatCard("活跃/封禁用户", uiState.stats?.let { "${it.activeUsers}/${it.bannedUsers}" } ?: "-")
            StatCard("总商品数", uiState.stats?.totalProducts?.toString() ?: "-")
            StatCard("在售/下架/售出", uiState.stats?.let { "${it.onSaleProducts}/${it.offProducts}/${it.soldProducts}" } ?: "-")
            StatCard("总订单数", uiState.stats?.totalOrders?.toString() ?: "-")
            StatCard("退款中订单", uiState.stats?.refundingOrders?.toString() ?: "-")
            StatCard("待处理举报", uiState.stats?.pendingReports?.toString() ?: "-")
            StatCard("今日订单数", uiState.stats?.todayOrders?.toString() ?: "-")
            StatCard("总交易额", uiState.stats?.totalAmount?.let { "¥$it" } ?: "-")
            StatCard("今日交易额", uiState.stats?.todayAmount?.let { "¥$it" } ?: "-")
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
