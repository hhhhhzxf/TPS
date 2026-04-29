package com.tps.ui.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketEmptyState
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    onNavigateToChat: (Long) -> Unit,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("交易消息", fontWeight = FontWeight.Bold) }) }) { padding ->
        MarketBackground {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.conversations.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MarketHeroCard("聊一聊再交易", "所有会话都会保留记录，确认价格和地点更安心。")
                MarketEmptyState("暂无消息", "去首页找一件好物，联系卖家开始交易。")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { MarketHeroCard("交易会话", "按商品聚合聊天，未读消息会优先提醒。") }
                items(uiState.conversations) { conv ->
                    MarketCard(Modifier.clickable { onNavigateToChat(conv.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFFFF0E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MarketOrange)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("商品 #${conv.productId}", fontWeight = FontWeight.Bold, color = Color(0xFF241713))
                                Text(conv.lastMessage ?: "暂无消息，主动问问成色和交易地点", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                conv.updatedAt?.let {
                                    val time = if (it.length >= 16) it.substring(11, 16) else it
                                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val unread = conv.unreadCount ?: ((conv.unreadBuyer ?: 0) + (conv.unreadSeller ?: 0))
                                if (unread > 0) Badge(containerColor = MarketOrange) { Text(unread.toString()) }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
