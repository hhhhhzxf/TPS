package com.tps.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tps.ui.theme.MarketBackground
import com.tps.ui.theme.MarketHeroCard
import com.tps.ui.theme.MarketOrange

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    MarketBackground {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MarketHeroCard("加入校园淘", "注册后即可发布闲置、收藏好物、联系同校卖家。")
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                TextButton(onClick = {
                    viewModel.sendCode(phone)
                    codeSent = true
                }, enabled = phone.length == 11) {
                    Text(if (codeSent) "已发送" else "发送验证码")
                }
            }
        )

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("验证码（固定1234）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("昵称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        OutlinedTextField(
            value = studentId,
            onValueChange = { input -> studentId = input.filter { it.isDigit() } },
            label = { Text("学号认证") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码（6-20位）") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        if (uiState.error != null) {
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { viewModel.register(phone, password, code, studentId, nickname) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !uiState.isLoading && studentId.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MarketOrange)
        ) {
            if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("注册")
        }

        TextButton(onClick = onBack) {
            Text("已有账号？去登录")
        }
        }
        }
    }
    }
}
