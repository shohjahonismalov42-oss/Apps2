package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.model.LessonSessionManager
import com.example.service.FloatingSessionService
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BoardScreenMode
import com.example.viewmodel.SmartBoardViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val boardViewModel: SmartBoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 86" Doska hech qachon o'chib qolmasligi uchun ekran doim yoqiq turadi
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Tizim panellarini yashirish (Immersive Kiosk Mode)
        applyKioskImmersive()

        // LessonSessionManager dan ilovani qayta ochish buyrug'i
        LessonSessionManager.onOpenApp = {
            val reopenIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(reopenIntent)
        }

        // Qulflangan / ochilgan holatni kuzatish va Kiosk Lock Taskni boshqarish
        lifecycleScope.launch {
            boardViewModel.uiState.collect { state ->
                if (state.isLocked) {
                    FloatingSessionService.stop(this@MainActivity)
                    applyKioskImmersive()
                    try {
                        startLockTask()
                    } catch (_: Exception) {
                        // Kiosk Screen Pinning qo'llab-quvvatlanmagan qurilmalarda xatosiz davom etadi
                    }
                } else {
                    try {
                        stopLockTask()
                    } catch (_: Exception) {}
                }
            }
        }

        // Telegram bot orqali dars ochilganda:
        // Ilova avtomatik ravishda desktopga chiqib ketadi va chetda suzuvchi 45 D lik sessiya ochiladi!
        lifecycleScope.launch {
            boardViewModel.shouldMinimizeToDesktop.collect {
                // 1. Agar boshqa ilovalar ustida ko'rsatish (Overlay) ruxsati bo'lsa, suzuvchi oynani boshlaymiz
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    FloatingSessionService.start(this@MainActivity)
                }

                // 2. Ilova o'zidan-o'zi chiqib ketib (Home Launcher / Desktopga o'tadi)
                try {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                } catch (_: Exception) {}
            }
        }

        setContent {
            MyApplicationTheme {
                MainScreen(boardViewModel = boardViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_WHITEBOARD", false)) {
            boardViewModel.setScreenMode(BoardScreenMode.WHITEBOARD_CANVAS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (boardViewModel.uiState.value.isLocked) {
            applyKioskImmersive()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (boardViewModel.uiState.value.isLocked) {
                applyKioskImmersive()
            }
        } else if (boardViewModel.uiState.value.isLocked) {
            // Agar doska qulflangan bo'lsa va tizim bildirishnomalari pastga tortilsa, uni darhol yopish
            try {
                @Suppress("DEPRECATION")
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (_: Exception) {}
        }
    }

    /**
     * Agar doska qulflangan bo'lsa, foydalanuvchi ilovadan chiqib keta olmaydi.
     * Agar dars faol bo'lsa, desktopga o'tganda chetda suzuvchi taymer xizmati ishga tushadi.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (boardViewModel.uiState.value.isLocked) {
            val reopenIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(reopenIntent)
        } else {
            if (Settings.canDrawOverlays(this)) {
                FloatingSessionService.start(this)
            }
        }
    }

    /**
     * Klaviaturadagi yoki doska tugmalaridagi Back, Home va boshqa tugmalarni qulflangan paytda to'sish
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (boardViewModel.uiState.value.isLocked) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_ESCAPE -> {
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun applyKioskImmersive() {
        val w = window ?: return
        val insetsController = WindowCompat.getInsetsController(w, w.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
