package com.tps.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketCard
import com.tps.ui.theme.MarketEmptyState
import com.tps.ui.theme.MarketOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Submit, 1 = History

    LaunchedEffect(Unit) {
        viewModel.loadMyFeedback()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            snackbarHostState.showSnackbar("反馈提交成功")
            selectedTab = 1
            viewModel.resetSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("客服/反馈", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F7F7))
            )
        }
    ) { padding ->
        MarketBackground {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF7F7F7),
                    contentColor = MarketOrange
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("提交反馈") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("反馈记录") })
                }
                
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (selectedTab == 0) {
                        FeedbackSubmitForm(onSubmit = { type, content, contact ->
                            viewModel.submitFeedback(type, content, contact)
                        })
                    } else {
                        FeedbackHistoryList(uiState.feedbackList)
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackSubmitForm(onSubmit: (String, String, String) -> Unit) {
    var type by remember { mutableStateOf("功能建议") }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    val types = listOf("功能建议", "遇到Bug", "违规举报", "其他问题")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("反馈类型", fontWeight = FontWeight.Bold)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(types) { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(t) }
                )
            }
        }
        
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("详细描述你的问题或建议...") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(12.dp)
        )
        
        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            placeholder = { Text("选填：手机号/微信，方便我们联系你") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Button(
            onClick = { onSubmit(type, content, contact) },
            modifier = Modifier.fillMaxWidth(),
            enabled = content.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
        ) {
            Text("提交")
        }
    }
}

@Composable
fun FeedbackHistoryList(list: List<com.tps.data.remote.dto.FeedbackDto>) {
    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MarketEmptyState("暂无记录", "你还没有提交过反馈")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list) { feedback ->
                MarketCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row {
                            Text(feedback.type, fontWeight = FontWeight.Bold, color = MarketOrange)
                            Spacer(Modifier.weight(1f))
                            Text(feedback.status, fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(feedback.content, fontSize = 14.sp)
                        feedback.reply?.let {
                            Surface(color = Color(0xFFF7F7F7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("回复：$it", fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                            }
                        }
                        Text(feedback.createdAt ?: "", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
