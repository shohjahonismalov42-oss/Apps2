package com.example.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.LessonSessionManager
import com.example.model.SmartBoardConfig
import com.example.model.SmartBoardPreferences
import com.example.model.TelegramBotManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class BoardScreenMode {
    STANDBY_LOCK,       // 🔒 Locked screen showing live wallpaper, clock, QR code
    UNLOCKED_DESKTOP,   // 🔓 Unlocked home with floating timer & system apps
    WHITEBOARD_CANVAS   // 🖊 Fullscreen interactive drawing whiteboard
}

data class SmartBoardUiState(
    val isLocked: Boolean = true,
    val currentMode: BoardScreenMode = BoardScreenMode.STANDBY_LOCK,
    val activeSecondsRemaining: Int = 45 * 60,
    val sessionTotalSeconds: Int = 45 * 60,
    val teacherName: String = "",
    val isTimerPaused: Boolean = false,
    val botStatusText: String = "Kutilmoqda",
    val config: SmartBoardConfig = SmartBoardConfig(),
    val showAdminPinDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val lockMessageNotice: String? = null
)

class SmartBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SmartBoardPreferences(application)
    private val _uiState = MutableStateFlow(
        SmartBoardUiState(config = prefs.loadConfig())
    )
    val uiState: StateFlow<SmartBoardUiState> = _uiState.asStateFlow()

    private val _shouldMinimizeToDesktop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shouldMinimizeToDesktop = _shouldMinimizeToDesktop.asSharedFlow()

    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    private val botManager = TelegramBotManager(
        onUnlockRequested = { teacher, minutes ->
            startLessonSession(teacher, minutes, autoMinimize = true)
        },
        onLockRequested = {
            lockBoard()
        },
        onExtendRequested = { additionalMinutes ->
            extendSession(additionalMinutes)
        },
        getStatusInfo = {
            val state = _uiState.value
            if (state.isLocked) {
                "🔒 Doska qulflangan (Kutish rejimi)\nQurilma ID: ${state.config.boardId}"
            } else {
                val mm = state.activeSecondsRemaining / 60
                val ss = state.activeSecondsRemaining % 60
                val timeStr = String.format("%02d:%02d", mm, ss)
                "🔓 Doska ochiq!\n👨‍🏫 O'qituvchi: ${state.teacherName}\n⏳ Qolgan vaqt: $timeStr"
            }
        }
    )

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Exception) {}

        // Connect LessonSessionManager callbacks
        LessonSessionManager.onExtendMinutes = { addMin -> extendSession(addMin) }
        LessonSessionManager.onTogglePause = { toggleTimerPause() }
        LessonSessionManager.onFinishLesson = { lockBoard() }
        LessonSessionManager.onOpenWhiteboard = { setScreenMode(BoardScreenMode.WHITEBOARD_CANVAS) }

        startTelegramPolling()
    }

    private fun startTelegramPolling() {
        val token = _uiState.value.config.botToken
        if (token.isNotBlank()) {
            botManager.startPolling(token, viewModelScope)
            _uiState.value = _uiState.value.copy(
                botStatusText = "Bot faol va ulandi"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                botStatusText = "Bot tokeni sozlanmagan"
            )
        }
    }

    fun startLessonSession(
        teacherName: String = "O'qituvchi",
        durationMinutes: Int = 45,
        autoMinimize: Boolean = true
    ) {
        val totalSecs = durationMinutes * 60
        val resolvedTeacher = teacherName.ifBlank { "O'qituvchi" }

        // Sync with LessonSessionManager for Floating Service
        LessonSessionManager.isLocked.value = false
        LessonSessionManager.activeSeconds.value = totalSecs
        LessonSessionManager.sessionTotalSeconds.value = totalSecs
        LessonSessionManager.teacherName.value = resolvedTeacher
        LessonSessionManager.isPaused.value = false

        _uiState.value = _uiState.value.copy(
            isLocked = false,
            currentMode = BoardScreenMode.UNLOCKED_DESKTOP,
            activeSecondsRemaining = totalSecs,
            sessionTotalSeconds = totalSecs,
            teacherName = resolvedTeacher,
            isTimerPaused = false,
            lockMessageNotice = null
        )

        playBeepTone(ToneGenerator.TONE_PROP_BEEP2)
        startCountdownTimer()

        if (autoMinimize) {
            viewModelScope.launch {
                delay(350L)
                _shouldMinimizeToDesktop.emit(Unit)
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && !_uiState.value.isLocked) {
                delay(1000L)
                if (!_uiState.value.isTimerPaused) {
                    val remaining = _uiState.value.activeSecondsRemaining - 1
                    LessonSessionManager.activeSeconds.value = remaining.coerceAtLeast(0)

                    if (remaining <= 0) {
                        _uiState.value = _uiState.value.copy(activeSecondsRemaining = 0)
                        playBeepTone(ToneGenerator.TONE_CDMA_HIGH_L)
                        lockBoard()
                        break
                    } else {
                        // Warn at 5 minutes
                        if (remaining == 5 * 60) {
                            playBeepTone(ToneGenerator.TONE_PROP_BEEP)
                        }
                        _uiState.value = _uiState.value.copy(activeSecondsRemaining = remaining)
                    }
                }
            }
        }
    }

    fun lockBoard() {
        timerJob?.cancel()
        timerJob = null
        LessonSessionManager.isLocked.value = true

        _uiState.value = _uiState.value.copy(
            isLocked = true,
            currentMode = BoardScreenMode.STANDBY_LOCK,
            isTimerPaused = false
        )
        playBeepTone(ToneGenerator.TONE_PROP_PROMPT)
    }

    fun extendSession(additionalMinutes: Int = 10) {
        val additionalSecs = additionalMinutes * 60
        val currentRemaining = _uiState.value.activeSecondsRemaining
        val newRemaining = currentRemaining + additionalSecs
        val newTotal = _uiState.value.sessionTotalSeconds + additionalSecs

        LessonSessionManager.activeSeconds.value = newRemaining
        LessonSessionManager.sessionTotalSeconds.value = newTotal

        _uiState.value = _uiState.value.copy(
            activeSecondsRemaining = newRemaining,
            sessionTotalSeconds = newTotal
        )
        playBeepTone(ToneGenerator.TONE_PROP_ACK)
    }

    fun toggleTimerPause() {
        val nextPaused = !_uiState.value.isTimerPaused
        LessonSessionManager.isPaused.value = nextPaused
        _uiState.value = _uiState.value.copy(
            isTimerPaused = nextPaused
        )
    }

    fun setScreenMode(mode: BoardScreenMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode)
    }

    fun showNotice(msg: String) {
        _uiState.value = _uiState.value.copy(lockMessageNotice = msg)
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(lockMessageNotice = null)
    }

    fun openAdminPinDialog() {
        _uiState.value = _uiState.value.copy(showAdminPinDialog = true)
    }

    fun closeAdminPinDialog() {
        _uiState.value = _uiState.value.copy(showAdminPinDialog = false)
    }

    fun openSettingsDialog() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = true)
    }

    fun closeSettingsDialog() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
    }

    fun verifyAdminPin(enteredPin: String): Boolean {
        return enteredPin == _uiState.value.config.adminPin
    }

    fun updateConfig(newConfig: SmartBoardConfig) {
        prefs.saveConfig(newConfig)
        _uiState.value = _uiState.value.copy(config = newConfig)
        startTelegramPolling()
    }

    suspend fun testTelegramToken(token: String): Pair<Boolean, String> {
        return botManager.testToken(token)
    }

    private fun playBeepTone(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 400)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        botManager.stopPolling()
        timerJob?.cancel()
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
    }
}
