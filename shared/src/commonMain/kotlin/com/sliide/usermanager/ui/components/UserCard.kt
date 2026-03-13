package com.sliide.usermanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.ui.util.toRelativeTimeString
import kotlin.math.abs

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val r: Float; val g: Float; val b: Float
    when {
        h < 60f  -> { r = c; g = x; b = 0f }
        h < 120f -> { r = x; g = c; b = 0f }
        h < 180f -> { r = 0f; g = c; b = x }
        h < 240f -> { r = 0f; g = x; b = c }
        h < 300f -> { r = x; g = 0f; b = c }
        else     -> { r = c; g = 0f; b = x }
    }
    return Color(r + m, g + m, b + m)
}

private fun avatarColor(name: String): Color {
    val hue = (name.hashCode().and(0x7FFFFFFF) % 360).toFloat()
    return hsvToColor(hue, 0.45f, 0.85f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserCard(
    user: User,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val poshSurface = Color(0xFF1E293B)
    val poshAccent  = Color(0xFFF59E0B) 

    // Box wrapper to fix "weird corners" caused by SwipeDismiss background leaking
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.Transparent)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = poshSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier          = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier           = Modifier.size(56.dp).clip(CircleShape).background(avatarColor(user.name)),
                    contentAlignment   = Alignment.Center
                ) {
                    Text(
                        text       = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text      = user.name,
                        style     = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color     = Color.White,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis
                    )
                    
                    // FIXED: Increased vertical spacing between Name and Email
                    Spacer(Modifier.height(6.dp))

                    Text(
                        text     = user.email.lowercase(),
                        style    = MaterialTheme.typography.bodySmall.copy(
                            letterSpacing = 0.25.sp
                        ),
                        color    = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text     = user.createdAtEpochMs.toRelativeTimeString(),
                        style    = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color    = poshAccent,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
