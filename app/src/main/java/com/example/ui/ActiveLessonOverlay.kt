package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceLightDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BoardScreenMode
import com.example.viewmodel.SmartBoardUiState
import kotlin.math.roundToInt

@Composable
fun ActiveLessonOverlay(
    state: SmartBoardUiState,
    onOpenWhiteboard: () -> Unit,
    onExtendMinutes: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onFinishLesson: () -> Unit,
    onMinimizeToDesktop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(20f) }
    var isCompactMode by remember { mutableStateOf(false) }

    val remainingSeconds = state.activeSecondsRemaining
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    // Color indicator: Green -> Gold (<10 min) -> Red (<3 min)
    val timerColor = when {
        remainingSeconds <= 180 -> Color(0xFFEF4444) // Urgent red
        remainingSeconds <= 600 -> NeonGold          // Warning amber
        else -> NeonCyan                             // Normal cyan
    }

    Box(modifier = modifier) {
        Surface(
            color = SurfaceDark.copy(alpha = 0.95f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, timerColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceAtLeast(10f)
                        offsetY = (offsetY + dragAmount.y).coerceAtLeast(10f)
                    }
                }
        ) {
            if (isCompactMode) {
                // Minimized floating pill on 86" screen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = timerColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = timerColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { isCompactMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = timerColor, contentColor = DarkNavyBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Kengaytirish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Full Expanded Lesson Control Bar
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.width(620.dp)
                    ) {
                        // Drag handle and teacher badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Dars sessiyasi faol",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "👨‍🏫 ${state.teacherName}",
                                    fontSize = 12.sp,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Compact minimize toggle
                        IconButton(
                            onClick = { isCompactMode = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                text = "—",
                                color = TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timer Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.width(620.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isTimerPaused) NeonGold else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = timerColor,
                                letterSpacing = 2.sp
                            )
                            if (state.isTimerPaused) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(Pauza)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGold
                                )
                            }
                        }

                        // Action Buttons: Whiteboard, Apps/Desktop, Extend, Lock
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Whiteboard toggle button
                            Button(
                                onClick = onOpenWhiteboard,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = DarkNavyBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("btn_open_whiteboard")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Doska", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Desktop / External Android Apps
                            OutlinedButton(
                                onClick = onMinimizeToDesktop,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("btn_minimize_desktop")
                            ) {
                                Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ilovalar", fontSize = 12.sp)
                            }

                            // +10 min extension
                            OutlinedButton(
                                onClick = { onExtendMinutes(10) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("10m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Pause / Resume
                            IconButton(
                                onClick = onTogglePause,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(
                                    imageVector = if (state.isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Finish Lesson / Lock
                            Button(
                                onClick = onFinishLesson,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("btn_finish_lesson")
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Qulflash", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
