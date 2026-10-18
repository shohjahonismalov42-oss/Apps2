package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.viewmodel.SmartBoardUiState

/**
 * Tanaffus / Standby rejimi:
 * Ekran 100% toza turadi. Foydalanuvchi soatni ekranning xohlagan joyiga
 * erkin surib (drag) boshqarishi mumkin.
 *
 * O'qituvchi yoki Admin uchun:
 * 1. Telegram bot orqali 45 daqiqalik darsni ochish.
 * 2. O'ng yuqori burchakdagi tishli g'ildirakcha yoki burchak maydoni bosilganda Admin PIN oynasi ochiladi.
 */
@Composable
fun StandbyLockScreen(
    state: SmartBoardUiState,
    onQuickUnlockTest: () -> Unit,
    onOpenAdminPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shaffof konteyner - sensor teginishlarni to'smaydi, soat bemalol suriladi
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Faqat o'ng yuqori burchakdagi (tishli g'ildirakcha joylashgan) kichik shaffof sezgir maydon (70x70 dp)
        // Bu ekranning qolgan qismiga xalaqit bermaydi, soat butun ekran bo'ylab bemalol siljiydi
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(72.dp)
                .padding(8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onOpenAdminPin()
                }
                .testTag("secret_admin_hotspot")
        )
    }
}
