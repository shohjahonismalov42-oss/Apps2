package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.SmartBoardConfig
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceLightDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AdminPinDialog(
    correctPin: String,
    onSuccessUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.width(380.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Admin / O'qituvchi PIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Doskani qo'lda ochish yoki sozlamalarga kirish uchun PIN kiriting (Standart: 1234)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // PIN dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val display = enteredPin.padEnd(4, '•').take(4)
                    Text(
                        text = display.map { "$it " }.joinToString(""),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isError) Color(0xFFEF4444) else NeonCyan,
                        letterSpacing = 6.sp
                    )
                }

                if (isError) {
                    Text(
                        text = "Noto'g'ri PIN kod!",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numpad for 86" Touch Screen
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                numRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { digit ->
                            Button(
                                onClick = {
                                    isError = false
                                    when (digit) {
                                        "C" -> enteredPin = ""
                                        "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        else -> {
                                            if (enteredPin.length < 6) {
                                                enteredPin += digit
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (digit in listOf("C", "⌫")) Color(0xFF334155) else Color(0xFF1E293B),
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .size(72.dp, 48.dp)
                            ) {
                                if (digit == "⌫") {
                                    Icon(imageVector = Icons.Default.Backspace, contentDescription = null, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(digit, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Unlock or Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bekor qilish", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (enteredPin == correctPin) {
                                onSuccessUnlock()
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkNavyBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_pin_confirm")
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ochish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (enteredPin == correctPin) {
                            onOpenSettings()
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tizim sozlamalari", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AdminSettingsDialog(
    currentConfig: SmartBoardConfig,
    onSaveConfig: (SmartBoardConfig) -> Unit,
    onTestToken: suspend (String) -> Pair<Boolean, String>,
    onDismiss: () -> Unit
) {
    var botToken by remember { mutableStateOf(currentConfig.botToken) }
    var botUsername by remember { mutableStateOf(currentConfig.botUsername) }
    var boardId by remember { mutableStateOf(currentConfig.boardId) }
    var adminPin by remember { mutableStateOf(currentConfig.adminPin) }
    var wallpaperUrl by remember { mutableStateOf(currentConfig.wallpaperUrl) }

    var testStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .width(520.dp)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "86\" Doska & Telegram Sozlamalari",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Telegram Bot Token
                Text(
                    text = "Telegram Bot Token:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "@BotFather orqali olingan tokenni kiriting (masalan: 123456789:ABC...)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = botToken,
                    onValueChange = {
                        botToken = it
                        testStatus = null
                    },
                    singleLine = true,
                    placeholder = { Text("123456789:AAH...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceLightDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkNavyBg,
                        unfocusedContainerColor = DarkNavyBg
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_bot_token")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test Token Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            isTesting = true
                            testStatus = null
                            scope.launch {
                                testStatus = onTestToken(botToken.trim())
                                isTesting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Aloqani tekshirish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (testStatus != null) {
                        val (success, msg) = testStatus!!
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (success) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (success) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bot Username
                Text("Bot Username (Telegram):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                OutlinedTextField(
                    value = botUsername,
                    onValueChange = { botUsername = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceLightDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkNavyBg,
                        unfocusedContainerColor = DarkNavyBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Board ID & Admin PIN
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Doska ID:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        OutlinedTextField(
                            value = boardId,
                            onValueChange = { boardId = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceLightDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkNavyBg,
                                unfocusedContainerColor = DarkNavyBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Admin PIN:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        OutlinedTextField(
                            value = adminPin,
                            onValueChange = { adminPin = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceLightDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkNavyBg,
                                unfocusedContainerColor = DarkNavyBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wallpaper URL
                Text("Jonli fon (Wallpaper) URL:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                OutlinedTextField(
                    value = wallpaperUrl,
                    onValueChange = { wallpaperUrl = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceLightDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkNavyBg,
                        unfocusedContainerColor = DarkNavyBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save / Cancel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yopish")
                    }

                    Button(
                        onClick = {
                            val updated = currentConfig.copy(
                                botToken = botToken.trim(),
                                botUsername = botUsername.trim().removePrefix("@"),
                                boardId = boardId.trim(),
                                adminPin = adminPin.trim(),
                                wallpaperUrl = wallpaperUrl.trim()
                            )
                            onSaveConfig(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkNavyBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Saqlash", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
