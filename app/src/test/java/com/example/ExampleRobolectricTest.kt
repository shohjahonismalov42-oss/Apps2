package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SmartBoardConfig
import com.example.model.SmartBoardPreferences
import com.example.viewmodel.BoardScreenMode
import com.example.viewmodel.SmartBoardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SmartBoard 86 Locker", appName)
    }

    @Test
    fun `smartboard preferences save and load config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SmartBoardPreferences(context)
        val initialConfig = prefs.loadConfig()
        assertTrue(initialConfig.keepScreenAwake)
        assertEquals("1234", initialConfig.adminPin)
        assertEquals(45, initialConfig.defaultSessionMinutes)

        val updatedConfig = initialConfig.copy(
            botToken = "123456:ABC-DEF",
            botUsername = "my_smartboard_bot",
            boardId = "ROOM-101"
        )
        prefs.saveConfig(updatedConfig)

        val reloaded = prefs.loadConfig()
        assertEquals("123456:ABC-DEF", reloaded.botToken)
        assertEquals("my_smartboard_bot", reloaded.botUsername)
        assertEquals("ROOM-101", reloaded.boardId)
    }

    @Test
    fun `smartboard viewmodel starts locked and unlocks with session`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = SmartBoardViewModel(app)

        val initial = viewModel.uiState.value
        assertTrue("Initially board must be locked", initial.isLocked)
        assertEquals(BoardScreenMode.STANDBY_LOCK, initial.currentMode)

        // Start session
        viewModel.startLessonSession("O'qituvchi Test", 45)
        val unlocked = viewModel.uiState.value
        assertFalse("Board must be unlocked after starting session", unlocked.isLocked)
        assertEquals(BoardScreenMode.UNLOCKED_DESKTOP, unlocked.currentMode)
        assertEquals("O'qituvchi Test", unlocked.teacherName)
        assertEquals(45 * 60, unlocked.activeSecondsRemaining)

        // Lock again
        viewModel.lockBoard()
        val reLocked = viewModel.uiState.value
        assertTrue("Board must be locked after locking", reLocked.isLocked)
        assertEquals(BoardScreenMode.STANDBY_LOCK, reLocked.currentMode)
    }
}
