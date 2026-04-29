package com.tps.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.ColorPainter
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import android.util.Log
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

val MarketOrange = Color(0xFFFF5A1F)
val MarketGold = Color(0xFFFFB000)
val MarketGreen = Color(0xFF168765)
val MarketInk = Color(0xFF241713)
val MarketMuted = Color(0xFF8B6D60)
val MarketBgTop = Color(0xFFFFEFE5)
val MarketBgBottom = Color(0xFFF7F7F7)

@Composable
fun MarketBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MarketBgTop, Color(0xFFFFFAF6), MarketBgBottom)))
    ) {
        content()
    }
}

@Composable
fun MarketHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(MarketOrange, Color(0xFFFF8A00), MarketGold)))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp, lineHeight = 19.sp)
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun MarketCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun MarketEmptyState(title: String, subtitle: String) {
    MarketCard(Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = MarketInk, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color = MarketOrange) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}


fun resolveMediaUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("content://") || url.startsWith("file://") || url.startsWith("android.resource://")) return url
    val base = com.tps.data.remote.NetworkEndpointConfig.primaryApiBaseUrl.toString()
    return if (url.startsWith("/")) base + url.drop(1) else base + url
}

@Composable
fun AppAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolveMediaUrl(url))
            .listener(
                onError = { _, result ->
                    Log.e("AppAsyncImage", "Image load failed: $url, resolved: ${resolveMediaUrl(url)}", result.throwable)
                }
            )
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = ColorPainter(Color(0xFFEEEEEE)),
        error = ColorPainter(Color(0xFFFFE1D2))
    )
}
