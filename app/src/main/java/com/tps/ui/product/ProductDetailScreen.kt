package com.tps.ui.product

/**
 * 文件说明：商品模块界面，负责商品浏览、详情或发布流程的 Compose 展示。
 */

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.data.remote.dto.ProductCommentDto
import com.tps.ui.theme.AppAsyncImage
import com.tps.ui.theme.MarketBottomActions
import com.tps.ui.theme.MarketGreen
import com.tps.ui.theme.MarketInk
import com.tps.ui.theme.MarketOrange
import com.tps.util.resolveMediaUrl
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onBack: () -> Unit,
    onChat: (Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showReportSheet by remember { mutableStateOf(false) }

    LaunchedEffect(productId) { viewModel.load(productId) }
    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { 
            onChat(it)
            viewModel.consumeNavigateToChat()
        }
    }
    LaunchedEffect(uiState.orderCreated) {
        if (uiState.orderCreated) snackbarHostState.showSnackbar("下单成功！")
    }
    LaunchedEffect(uiState.orderError) {
        uiState.orderError?.let { snackbarHostState.showSnackbar("下单失败：$it") }
    }
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeActionSuccess()
        }
    }
    LaunchedEffect(uiState.reportSubmitted) {
        if (uiState.reportSubmitted) {
            showReportSheet = false
            snackbarHostState.showSnackbar("举报已提交，管理员会尽快审核")
            viewModel.consumeReportSubmitted()
        }
    }

    if (showReportSheet) {
        ReportProductSheet(
            productTitle = uiState.product?.title.orEmpty(),
            isSubmitting = uiState.isReporting,
            onDismiss = { if (!uiState.isReporting) showReportSheet = false },
            onSubmit = { reason, evidenceUris ->
                viewModel.reportProduct(productId, reason, evidenceUris)
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF5F7F6),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            uiState.product?.let { product ->
                var showMenu by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("下架商品") },
                        text = { Text("确定要下架该商品吗？下架后可以重新上架。") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.deleteProduct(product.id) }) { Text("下架", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                        }
                    )
                }

                TopAppBar(
                    title = { Text("商品详情", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                            Icon(
                                if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null
                            )
                        }
                        if (uiState.isOwner) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多操作")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("下架商品", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; showDeleteDialog = true }
                                )
                            }
                        } else {
                            IconButton(onClick = { showReportSheet = true }) {
                                Icon(Icons.Default.Report, "举报商品")
                            }
                        }
                    }
                )
            } ?: TopAppBar(
                title = { Text("商品详情", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                        Icon(
                            if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null
                        )
                    }
                }
            )
        },
        bottomBar = {
            uiState.product?.let { product ->
                var showBuyDialog by remember { mutableStateOf(false) }
                if (showBuyDialog) {
                    AlertDialog(
                        onDismissRequest = { showBuyDialog = false },
                        title = { Text("确认购买") },
                        text = { Text("商品名称：${product.title}\n购买价格：¥${product.price}") },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.createOrder(product.id)
                                showBuyDialog = false
                            }, colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)) { Text("确认下单") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBuyDialog = false }) { Text("取消") }
                        }
                    )
                }
                if (uiState.isOwner) {
                    when (product.status) {
                        "ON_SALE", "AVAILABLE" -> {
                            MarketBottomActions(
                                primaryText = "下架商品",
                                onPrimaryClick = { viewModel.updateStatus(product.id, "OFF") },
                                secondaryText = "擦亮商品",
                                onSecondaryClick = { viewModel.bumpProduct(product.id) }
                            )
                        }
                        "OFF" -> {
                            MarketBottomActions(
                                primaryText = "重新上架",
                                onPrimaryClick = { viewModel.updateStatus(product.id, "ON_SALE") }
                            )
                        }
                        "SOLD" -> {
                            Surface(tonalElevation = 8.dp, color = Color.White) {
                                Text(
                                    text = "商品已成交，不能重新上架",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    MarketBottomActions(
                        primaryText = "立即购买",
                        onPrimaryClick = { showBuyDialog = true },
                        primaryEnabled = product.status == "ON_SALE" || product.status == "AVAILABLE",
                        secondaryText = "联系卖家",
                        onSecondaryClick = { viewModel.startChat(product.userId, product.id) }
                    )
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            uiState.product?.let { product ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { product.imageUrls.size })
                        var showFullScreenImage by remember { mutableStateOf<String?>(null) }

                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF5F7F6))
                                    .clickable { showFullScreenImage = product.imageUrls[page] }
                            ) {
                                AppAsyncImage(
                                    url = resolveMediaUrl(product.imageUrls[page]),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        if (product.imageUrls.isNotEmpty()) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${product.imageUrls.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (showFullScreenImage != null) {
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { showFullScreenImage = null },
                                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .clickable { showFullScreenImage = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    var scale by remember { mutableStateOf(1f) }
                                    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                                    AppAsyncImage(
                                        url = resolveMediaUrl(showFullScreenImage),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .transformable(
                                                state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, panChange, _ ->
                                                    scale = (scale * zoomChange).coerceIn(1f, 3f)
                                                    offset += panChange
                                                }
                                            )
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offset.x,
                                                translationY = offset.y
                                            ),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                val priceStr = product.price.toString()
                                val parts = priceStr.split(".")
                                val integerPart = parts[0]
                                val decimalPart = if (parts.size > 1 && parts[1] != "0") ".${parts[1]}" else ""

                                Text("¥", fontSize = 16.sp, color = Color(0xFFDF4A12), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                Text(integerPart, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDF4A12))
                                Text(decimalPart, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDF4A12), modifier = Modifier.padding(bottom = 4.dp))
                                Spacer(Modifier.weight(1f))
                                Text(
                                    when (product.status) {
                                        "ON_SALE", "AVAILABLE" -> "在售"
                                        "OFF" -> "已下架"
                                        "SOLD" -> "已售出"
                                        else -> product.status
                                    },
                                    color = MarketGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).background(Color(0xFFE5F4EE)).padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(product.title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = MarketInk, lineHeight = 27.sp)
                            product.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp) }
                            if (uiState.isOwner && product.status == "OFF" && !product.takedownReason.isNullOrBlank()) {
                                Text(
                                    text = "平台下架原因：${product.takedownReason}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFF0F0))
                                        .padding(12.dp)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFDCE5E0))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoChip("${product.category ?: "其他"}")
                                InfoChip("成色 ${product.condition ?: "未标注"}")
                                InfoChip(product.location ?: "校内面交")
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).background(
                                    Brush.linearGradient(listOf(MarketGreen, MarketOrange))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(product.sellerNickname.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(product.sellerNickname, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "信用分 ${product.sellerCreditScore ?: 100} · ${product.sellerReviewCount} 条评价",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.Verified, null, tint = MarketGreen)
                        }
                    }
                    ProductCommentsCard(
                        comments = uiState.comments,
                        loading = uiState.commentsLoading,
                        submitting = uiState.commentSubmitting,
                        onSubmit = viewModel::submitComment,
                        onDelete = viewModel::deleteComment,
                        onRefresh = viewModel::refreshComments,
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCommentsCard(
    comments: List<ProductCommentDto>,
    loading: Boolean,
    submitting: Boolean,
    onSubmit: (String, List<Uri>) -> Unit,
    onDelete: (Long) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var commentImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            commentImageUris = (commentImageUris + uri).take(3)
        }
        pendingCameraUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        commentImageUris = (commentImageUris + uris).distinct().take(3)
    }
    val launchCameraCapture = {
        try {
            val uri = createCameraImageUri(context, "comment-image-")
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            pendingCameraUri = null
            Toast.makeText(context, "无法打开相机：${e.message ?: "请检查权限"}", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    if (showCommentSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showCommentSheet = false
                commentText = ""
                commentImageUris = emptyList()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("添加评论", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MarketInk)
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it.take(500) },
                    label = { Text("评论内容") },
                    placeholder = { Text("询问尺寸、成色、交易方式等") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${commentText.length}/500") }
                )
                Text("评论图片（最多3张）", fontWeight = FontWeight.Bold, color = MarketInk)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(commentImageUris) { uri ->
                        Box {
                            AppAsyncImage(
                                url = uri.toString(),
                                contentDescription = null,
                                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { commentImageUris = commentImageUris.filterNot { it == uri } },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                    }
                    if (commentImageUris.size < 3) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        launchCameraCapture()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.height(76.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(6.dp))
                                Text("拍照")
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.height(76.dp)
                            ) {
                                Icon(Icons.Default.Image, null)
                                Spacer(Modifier.width(6.dp))
                                Text("相册")
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showCommentSheet = false
                            commentText = ""
                            commentImageUris = emptyList()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onSubmit(commentText, commentImageUris)
                            showCommentSheet = false
                            commentText = ""
                            commentImageUris = emptyList()
                        },
                        enabled = commentText.isNotBlank() && !submitting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MarketGreen)
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("发布")
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("商品评论", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MarketInk)
                    Text("向卖家提问，或查看其他同学的留言", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh, enabled = !loading) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新评论", tint = MarketGreen)
                }
            }

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MarketGreen)
                    }
                }
                comments.isEmpty() -> {
                    Text(
                        text = "还没有评论，先问问商品细节。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F7F6))
                            .padding(14.dp)
                    )
                }
                else -> {
                    comments.take(20).forEachIndexed { index, comment ->
                        ProductCommentRow(comment = comment, onDelete = onDelete)
                        if (index < comments.take(20).lastIndex) {
                            HorizontalDivider(color = Color(0xFFE8EEE9))
                        }
                    }
                }
            }

            Button(
                onClick = { showCommentSheet = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MarketGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加评论")
            }
        }
    }
}

@Composable
private fun ProductCommentRow(
    comment: ProductCommentDto,
    onDelete: (Long) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MarketGreen, MarketOrange))),
            contentAlignment = Alignment.Center
        ) {
            val nickname = comment.userNickname?.takeIf { it.isNotBlank() } ?: "同学"
            Text(nickname.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userNickname?.takeIf { it.isNotBlank() } ?: "匿名同学",
                    fontWeight = FontWeight.Bold,
                    color = MarketInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(formatCommentTime(comment.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(comment.content, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            if (comment.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(comment.imageUrls) { imageUrl ->
                        AppAsyncImage(
                            url = resolveMediaUrl(imageUrl),
                            contentDescription = null,
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        if (comment.mine) {
            IconButton(onClick = { onDelete(comment.id) }, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除评论",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatCommentTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val normalized = value.replace("T", " ")
    return when {
        normalized.length >= 16 -> normalized.substring(0, 16)
        else -> normalized
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportProductSheet(
    productTitle: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, List<Uri>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var reason by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var evidenceUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            evidenceUris = (evidenceUris + uri).take(3)
        }
        pendingCameraUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        evidenceUris = (evidenceUris + uris).distinct().take(3)
    }
    val launchCameraCapture = {
        try {
            val uri = createCameraImageUri(context, "report-evidence-")
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            pendingCameraUri = null
            Toast.makeText(context, "无法打开相机：${e.message ?: "请检查权限"}", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("举报商品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(productTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("举报原因") },
                placeholder = { Text("例如：疑似违禁品、虚假描述、价格欺诈") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(18.dp)
            )
            Text("举报凭证（最多3张）", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(evidenceUris) { uri ->
                    Box {
                        AppAsyncImage(
                            url = uri.toString(),
                            contentDescription = null,
                            modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { evidenceUris = evidenceUris.filterNot { it == uri } },
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }
                if (evidenceUris.size < 3) {
                    item {
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    launchCameraCapture()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.height(76.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(6.dp))
                            Text("拍照")
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.height(76.dp)
                        ) {
                            Icon(Icons.Default.Image, null)
                            Spacer(Modifier.width(6.dp))
                            Text("相册")
                        }
                    }
                }
            }
            Button(
                onClick = { onSubmit(reason.trim(), evidenceUris) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isSubmitting && reason.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A1F))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("提交举报")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun createCameraImageUri(context: Context, prefix: String): Uri {
    val imageFile = File.createTempFile(prefix, ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MarketGreen,
        modifier = Modifier.clip(CircleShape).background(Color(0xFFE5F4EE)).padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
