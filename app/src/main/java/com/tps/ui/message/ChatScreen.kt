package com.tps.ui.message

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.tps.ui.theme.AppAsyncImage
import com.tps.data.remote.dto.ProductDto

import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.data.remote.websocket.ChatMessage
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketOrange
import com.tps.util.resolveMediaUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val title by viewModel.title.collectAsState()
    val product by viewModel.product.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val quickPhrases = listOf("还在吗？", "能便宜点吗？", "在哪里面交？", "可以看看细节图吗？")

    LaunchedEffect(conversationId) {
        viewModel.init(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F7F7))
        )
        product?.let { prod ->
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetail(prod.id) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppAsyncImage(
                        url = resolveMediaUrl(prod.imageUrls.firstOrNull()),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prod.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("¥${prod.price}", color = Color(0xFFE93600), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (prod.status == "ON_SALE" || prod.status == "AVAILABLE") {
                                Text("在售", color = MarketOrange, fontSize = 10.sp, modifier = Modifier.background(Color(0xFFFFE1D2), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                            } else {
                                Text(prod.status, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        MarketBackground(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg, viewModel.myUserId, product)
                }
            }
        }
        Surface(
            color = Color(0xFFF7F7F7),
            tonalElevation = 8.dp,
            modifier = Modifier.imePadding().navigationBarsPadding()
        ) {
            Column {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPhrases) { phrase ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.clickable { input = phrase }
                        ) {
                            Text(phrase, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = Color(0xFF241A16))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("想跟Ta说点什么...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MarketOrange),
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, myUserId: Long, product: ProductDto?) {
    val isMe = msg.senderId == myUserId
    // A simplified avatar logic: we just use the first letter of buyer/seller. 
    // In a real app we'd fetch the UserProfile or it would be included in the conversation data.
    val avatarLetter = if (isMe) "我" else "Ta" 
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(avatarLetter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isMe) 18.dp else 4.dp,
                    topEnd = if (isMe) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (isMe) Color(0xFFFFE1D2) else Color.White,
                tonalElevation = 1.dp,
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isMe) Color(0xFF7B361B) else Color(0xFF241A16),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
        
        if (isMe) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(MarketOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(avatarLetter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
