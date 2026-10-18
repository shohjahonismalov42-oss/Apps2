package com.example.ui

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.DarkNavyBg
import com.example.viewmodel.BoardScreenMode
import com.example.viewmodel.SmartBoardViewModel

@Composable
fun MainScreen(
    boardViewModel: SmartBoardViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by boardViewModel.uiState.collectAsState()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Keep screen awake for 86" Smart Display (never sleeps during operation)
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Fullscreen Immersive Kiosk Mode
    LaunchedEffect(uiState.isLocked) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Intercept Back button:
    // When locked: students cannot exit the app!
    // When unlocked: closes whiteboard or minimizes
    BackHandler {
        if (uiState.isLocked) {
            // Tanaffus vaqtida ekranda ortiqcha xabarlar chiqmasligi uchun hech nima ko'rsatmaydi
        } else {
            if (uiState.currentMode == BoardScreenMode.WHITEBOARD_CANVAS) {
                boardViewModel.setScreenMode(BoardScreenMode.UNLOCKED_DESKTOP)
            } else {
                // If on desktop, teacher can choose to lock or minimize
                boardViewModel.lockBoard()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        // 1. Background Live Wallpaper Effect (Always running in standby & desktop)
        SamWebView(
            url = uiState.config.wallpaperUrl,
            isDesktopMode = true,
            touchDragEnabled = true, // Har doim soatni surish va boshqarish imkoniyati
            zoomScale = 1.0f,
            onOpenAdminPin = {
                boardViewModel.openAdminPinDialog()
            },
            onLoadingChanged = {},
            onProgressChanged = {},
            onErrorOccurred = {},
            onWebViewCreated = { webView -> webViewInstance = webView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. State-based UI Layer
        if (uiState.isLocked) {
            // STANDBY / KIOSK LOCK SCREEN
            StandbyLockScreen(
                state = uiState,
                onQuickUnlockTest = {
                    boardViewModel.startLessonSession("O'qituvchi (Sinov)", 45)
                },
                onOpenAdminPin = {
                    boardViewModel.openAdminPinDialog()
                }
            )
        } else {
            // UNLOCKED LESSON SESSION ACTIVE
            when (uiState.currentMode) {
                BoardScreenMode.WHITEBOARD_CANVAS -> {
                    WhiteboardCanvas(
                        onCloseWhiteboard = {
                            boardViewModel.setScreenMode(BoardScreenMode.UNLOCKED_DESKTOP)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                BoardScreenMode.UNLOCKED_DESKTOP -> {
                    // Desktop view: live wallpaper in background with teacher tools
                }
                BoardScreenMode.STANDBY_LOCK -> {
                    // Handled above
                }
            }

            // Floating Active 45-Minute Lesson Overlay
            ActiveLessonOverlay(
                state = uiState,
                onOpenWhiteboard = {
                    boardViewModel.setScreenMode(
                        if (uiState.currentMode == BoardScreenMode.WHITEBOARD_CANVAS) {
                            BoardScreenMode.UNLOCKED_DESKTOP
                        } else {
                            BoardScreenMode.WHITEBOARD_CANVAS
                        }
                    )
                },
                onExtendMinutes = { additional ->
                    boardViewModel.extendSession(additional)
                },
                onTogglePause = {
                    boardViewModel.toggleTimerPause()
                },
                onFinishLesson = {
                    boardViewModel.lockBoard()
                },
                onMinimizeToDesktop = {
                    // Allow teacher to go to Android home launcher to launch external apps
                    try {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                    } catch (_: Exception) {}
                }
            )
        }

        // 3. Admin / PIN Dialogs
        if (uiState.showAdminPinDialog) {
            AdminPinDialog(
                correctPin = uiState.config.adminPin,
                onSuccessUnlock = {
                    boardViewModel.closeAdminPinDialog()
                    boardViewModel.startLessonSession("O'qituvchi (PIN orqali)", 45)
                },
                onOpenSettings = {
                    boardViewModel.closeAdminPinDialog()
                    boardViewModel.openSettingsDialog()
                },
                onDismiss = {
                    boardViewModel.closeAdminPinDialog()
                }
            )
        }

        if (uiState.showSettingsDialog) {
            AdminSettingsDialog(
                currentConfig = uiState.config,
                onSaveConfig = { updated ->
                    boardViewModel.updateConfig(updated)
                },
                onTestToken = { token ->
                    boardViewModel.testTelegramToken(token)
                },
                onDismiss = {
                    boardViewModel.closeSettingsDialog()
                }
            )
        }
    }
}
