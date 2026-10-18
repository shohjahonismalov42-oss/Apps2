package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SmartBoardUiState
import kotlinx.coroutines.delay

@Composable
fun StandbyLockScreen(
    state: SmartBoardUiState,
    onQuickUnlockTest: () -> Unit,
    onOpenAdminPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shows a temporary banner when student/teacher taps the locked screen
    var showTapHint by remember { mutableStateOf(false) }

    LaunchedEffect(showTapHint) {
        if (showTapHint) {
            delay(4500L)
            showTapHint = false
        }
    }

    // Fullscreen transparent interceptor (preserves full view of live wallpaper)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping on the screen reveals a brief friendly hint
                showTapHint = true
            }
    ) {
        // Discreet, sleek top-right status pill (does not block wallpaper clock or logo)
        Surface(
            color = SurfaceDark.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Lock dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFCA5A5),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Qulflangan • @${state.config.botUsername}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))

                // Small PIN button
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenAdminPin)
                        .testTag("btn_admin_pin")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PIN",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Animated bottom hint banner that appears ONLY when someone taps the locked screen
        AnimatedVisibility(
            visible = showTapHint,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                color = SurfaceDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "Doska qulflangan. Darsni ochish uchun Telegram botga (@${state.config.botUsername}) kiring",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Quick test button in the tap hint
                    Button(
                        onClick = onQuickUnlockTest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DarkNavyBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .testTag("btn_quick_unlock_test")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Darsni ochish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
