    package com.tps.ui.product

/**
 * 文件说明：商品模块界面，负责商品浏览、详情或发布流程的 Compose 展示。
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.ui.theme.AppAsyncImage
import com.tps.data.remote.dto.ProductDto
import com.tps.ui.theme.MarketGreen
import com.tps.ui.theme.MarketInk
import com.tps.ui.theme.MarketLine
import com.tps.ui.theme.MarketMuted
import com.tps.ui.theme.MarketOrange
import com.tps.ui.theme.MarketSurfaceSoft
import com.tps.util.resolveMediaUrl
import kotlinx.coroutines.delay

private val categoryTabs = listOf("全部", "数码", "教材", "生活", "运动", "服饰")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeProductListScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var pullDistance by remember { mutableStateOf(0f) }
    var refreshPinned by remember { mutableStateOf(false) }
    var refreshStarted by remember { mutableStateOf(false) }
    val refreshThreshold = 96f
    val pullProgress = (pullDistance / refreshThreshold).coerceIn(0f, 1f)
    val isPullRefreshing = refreshPinned && uiState.isLoading
    val refreshIndicatorVisible = pullProgress > 0f || refreshPinned || isPullRefreshing
    val refreshLabel = when {
        refreshPinned || isPullRefreshing -> "正在刷新校园好物"
        pullProgress >= 1f -> "松手刷新"
        else -> "继续下拉刷新"
    }
    val triggerRefresh = {
        if (!uiState.isLoading && !refreshPinned) {
            refreshPinned = true
            refreshStarted = true
            pullDistance = refreshThreshold
            viewModel.loadProducts()
        }
    }

    LaunchedEffect(refreshPinned, uiState.isLoading) {
        if (refreshPinned && uiState.isLoading) {
            refreshStarted = true
        }
        if (refreshPinned && refreshStarted && !uiState.isLoading) {
            delay(260)
            refreshPinned = false
            refreshStarted = false
            pullDistance = 0f
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    val refreshConnection = remember(uiState.isLoading, refreshPinned) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < 0 && pullDistance > 0f && !refreshPinned) {
                    pullDistance = (pullDistance + available.y).coerceAtLeast(0f)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0 && !uiState.isLoading && !refreshPinned) {
                    pullDistance = (pullDistance + available.y * 0.45f).coerceAtMost(refreshThreshold)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance >= refreshThreshold && !uiState.isLoading && !refreshPinned) {
                    triggerRefresh()
                } else if (!refreshPinned) {
                    pullDistance = 0f
                }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            HomeSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearch = { viewModel.search(searchQuery) },
                pullProgress = pullProgress,
                isRefreshing = refreshPinned || isPullRefreshing,
                onRefresh = triggerRefresh
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(refreshConnection),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp
        ) {
            if (refreshIndicatorVisible) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    PullRefreshIndicator(
                        label = refreshLabel,
                        progress = pullProgress,
                        refreshing = refreshPinned || isPullRefreshing
                    )
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                HomeHero(
                    productCount = uiState.products.size,
                    onRefresh = triggerRefresh
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                QuickAccessGrid { category ->
                    selectedCategory = category
                    viewModel.filterCategory(category.takeUnless { it == "全部" })
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                PromoCardsRow(
                    onLowPrice = { viewModel.filterLowPrice() },
                    onNewItems = { viewModel.search("") }
                )
            }

        item(span = StaggeredGridItemSpan.FullLine) {
            CategoryStrip(
                selectedCategory = selectedCategory,
                onSelect = { category ->
                    selectedCategory = category
                    viewModel.filterCategory(category.takeUnless { it == "全部" })
                }
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            FeedHeader(uiState.products.size)
        }

        when {
            uiState.isLoading && uiState.products.isEmpty() -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            !uiState.isLoading && uiState.products.isEmpty() -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    EmptyGoodsCard()
                }
            }
            else -> {
                items(uiState.products) { product ->
                    ProductCard(product = product, onClick = { onNavigateToDetail(product.id) })
                }
                if (uiState.hasMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                        Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun HomeSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    pullProgress: Float,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Surface(color = Color(0xFFF5F7F6), tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜手机、教材、耳机、自行车", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MarketMuted) },
                modifier = Modifier.weight(1f).height(50.dp),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MarketLine,
                    unfocusedBorderColor = MarketLine,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            RefreshSpinnerButton(
                progress = pullProgress,
                refreshing = isRefreshing,
                onClick = onRefresh
            )
        }
    }
}

@Composable
private fun RefreshSpinnerButton(progress: Float, refreshing: Boolean, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "homeRefresh")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(720, easing = LinearEasing)),
        label = "homeRefreshRotation"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (refreshing) 0.72f else progress.coerceIn(0.12f, 1f) },
                modifier = Modifier.size(23.dp).rotate(if (refreshing) rotation else progress * 180f),
                color = MarketOrange,
                trackColor = MarketGreen.copy(alpha = 0.22f),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun PullRefreshIndicator(label: String, progress: Float, refreshing: Boolean) {
    val infinite = rememberInfiniteTransition(label = "pullRefresh")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(720, easing = LinearEasing)),
        label = "pullRefreshRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { if (refreshing) 0.72f else progress.coerceIn(0.12f, 1f) },
            modifier = Modifier
                .size(22.dp)
                .rotate(if (refreshing) rotation else progress * 180f),
            color = MarketOrange,
            trackColor = MarketGreen.copy(alpha = 0.20f),
            strokeWidth = 3.dp
        )
        Text(
            text = label,
            color = MarketGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHero(
    productCount: Int,
    onRefresh: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF20302A), MarketGreen, MarketOrange)
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("今天看看谁在出闲置", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        Text("同校面交 · 信用优先 · 好物实时刷新", color = Color.White.copy(alpha = 0.84f), fontSize = 13.sp)
                    }
                    AssistChip(
                        onClick = onRefresh,
                        label = { Text("${productCount} 件在逛") },
                        leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            labelColor = Color.White,
                            leadingIconContentColor = Color.White
                        ),
                        border = null
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PromoPill("校内面交")
                    PromoPill("信用优先")
                    PromoPill("低价捡漏")
                }
            }
        }
    }
}

@Composable
private fun PromoPill(text: String) {
    Text(
        text = text,
        color = MarketGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.86f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryStrip(selectedCategory: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categoryTabs) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelect(category) },
                label = { Text(category) },
                leadingIcon = {
                    if (category == "全部") Icon(Icons.Default.Category, null, Modifier.size(16.dp))
                },
                shape = RoundedCornerShape(999.dp),
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE5F4EE),
                    selectedLabelColor = MarketGreen,
                    containerColor = Color.White,
                    labelColor = MarketMuted
                )
            )
        }
    }
}

@Composable
private fun FeedHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("猜你喜欢", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MarketInk)
        Spacer(Modifier.weight(1f))
        Text("${count} 件", fontSize = 12.sp, color = MarketGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyGoodsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("暂时没找到", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("换个关键词，或者先发布你的第一件闲置。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProductCard(product: ProductDto, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = 1f, label = "cardScale")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                val imageHeight = remember(product.id) { if (product.id % 2L == 0L) 178.dp else 142.dp }
                AppAsyncImage(
                    url = resolveMediaUrl(product.imageUrls.firstOrNull()),
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxWidth().height(imageHeight),
                    contentScale = ContentScale.Crop
                )
                StatusTag(product.status)
            }
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    product.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    fontSize = 14.sp,
                    color = MarketInk
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val conditionLabel = when (product.condition) {
                        "NEW" -> "全新"
                        "LIKE_NEW" -> "几乎全新"
                        "GOOD" -> "成色好"
                        "FAIR" -> "有使用痕迹"
                        else -> "信用极好"
                    }
                    Text(
                        text = conditionLabel,
                        color = MarketGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE5F4EE))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("¥", color = Color(0xFFDF4A12), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        trimPrice(product.price),
                        color = Color(0xFFDF4A12),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    AnimatedVisibility(product.favorited) {
                        Icon(Icons.Default.Favorite, null, tint = MarketOrange, modifier = Modifier.size(17.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Verified, null, tint = MarketGreen, modifier = Modifier.size(14.dp))
                    Text(product.sellerNickname, fontSize = 11.sp, color = MarketMuted, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = MarketMuted, modifier = Modifier.size(13.dp))
                    Text(product.location ?: "校内面交", fontSize = 11.sp, color = MarketMuted, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: String) {
    val isOnSale = status == "ON_SALE" || status == "AVAILABLE"
    Text(
        text = if (isOnSale) "在售" else status,
        color = if (isOnSale) MarketGreen else Color(0xFF6F6F6F),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun trimPrice(price: Double): String {
    return if (price % 1.0 == 0.0) price.toInt().toString() else String.format("%.2f", price)
}

@Composable
private fun QuickAccessGrid(onCategorySelected: (String) -> Unit) {
    val items: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
        "数码" to Icons.Default.Computer,
        "教材" to Icons.Default.MenuBook,
        "宿舍" to Icons.Default.Home,
        "运动" to Icons.Default.SportsBasketball,
        "低价" to Icons.Default.LocalOffer,
        "全新" to Icons.Default.Star,
        "附近" to Icons.Default.NearMe,
        "信用" to Icons.Default.VerifiedUser
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.take(4).forEach { (label, icon) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCategorySelected(label) }) {
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFE5F4EE)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MarketGreen, modifier = Modifier.size(22.dp))
                    }
                    Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), color = MarketInk)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.drop(4).forEach { (label, icon) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCategorySelected(if (label == "全新" || label == "信用") "全部" else label) }) {
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(MarketSurfaceSoft), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MarketMuted, modifier = Modifier.size(22.dp))
                    }
                    Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), color = MarketInk)
                }
            }
        }
    }
}

@Composable
private fun PromoCardsRow(onLowPrice: () -> Unit, onNewItems: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        item { PromoCard("今日捡漏", "好物低至1折", Color(0xFFFFF4F0), Color(0xFFFF5A1F), onLowPrice) }
        item { PromoCard("同校新品", "刚上架的新鲜货", Color(0xFFF0F9F6), Color(0xFF168765), onNewItems) }
        item { PromoCard("低于50元", "学生党福利", Color(0xFFFFF9EE), Color(0xFFFFB000), onLowPrice) }
    }
}

@Composable
private fun PromoCard(title: String, subtitle: String, bgColor: Color, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(width = 110.dp, height = 56.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF241A16))
            Text(subtitle, fontSize = 10.sp, color = accentColor)
        }
    }
}
