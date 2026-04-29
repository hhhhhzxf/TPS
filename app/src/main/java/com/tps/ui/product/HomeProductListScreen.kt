package com.tps.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tps.ui.theme.AppAsyncImage
import com.tps.data.remote.dto.ProductDto
import com.tps.util.resolveMediaUrl

private val categoryTabs = listOf("全部", "数码", "教材", "生活", "运动", "服饰")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeProductListScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜手机、教材、耳机、自行车", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp).height(50.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF7F7F7),
                            unfocusedContainerColor = Color(0xFFF7F7F7),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchQuery) })
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                HomeHero(
                    productCount = uiState.products.size
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHero(
    productCount: Int
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
                        listOf(Color(0xFFFF6A1A), Color(0xFFFFB000), Color(0xFFFFF0D6))
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("校园淘好物", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("像逛集市一样发现同校闲置", color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp)
                    }
                    AssistChip(
                        onClick = {},
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
        color = Color(0xFF7A2D00),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.72f))
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
                    selectedContainerColor = Color(0xFFFFE1D2),
                    selectedLabelColor = Color(0xFFFF5A1F),
                    containerColor = Color.White,
                    labelColor = Color(0xFF6D5247)
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
        Text("猜你喜欢", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF241A16))
        Spacer(Modifier.weight(1f))
        Text("${count} 件", fontSize = 12.sp, color = Color(0xFF8B6D60))
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                    color = Color(0xFF241A16)
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
                        color = Color(0xFFFF5A1F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFE1D2))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("¥", color = Color(0xFFE93600), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        trimPrice(product.price),
                        color = Color(0xFFE93600),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    AnimatedVisibility(product.favorited) {
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF5A1F), modifier = Modifier.size(17.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Verified, null, tint = Color(0xFF1F8A70), modifier = Modifier.size(14.dp))
                    Text(product.sellerNickname, fontSize = 11.sp, color = Color(0xFF6D5247), maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFB08A78), modifier = Modifier.size(13.dp))
                    Text(product.location ?: "校内面交", fontSize = 11.sp, color = Color(0xFF9A8074), maxLines = 1)
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
        color = if (isOnSale) Color(0xFFFF5A1F) else Color(0xFF6F6F6F),
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
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFF0E6)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Color(0xFFFF5A1F), modifier = Modifier.size(22.dp))
                    }
                    Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), color = Color(0xFF241A16))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.drop(4).forEach { (label, icon) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCategorySelected(if (label == "全新" || label == "信用") "全部" else label) }) {
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFF7F7F7)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Color(0xFF6D5247), modifier = Modifier.size(22.dp))
                    }
                    Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), color = Color(0xFF241A16))
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
