package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.LessonSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingSessionService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

    private var tvTimer: TextView? = null
    private var tvTeacher: TextView? = null
    private var actionPanel: LinearLayout? = null
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        createFloatingView()
        observeSession()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "smartboard_lesson_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SmartBoard 86 Dars Sessiyasi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Faol dars taymeri bildirishnomasi"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SmartBoard 86 - Dars faol")
            .setContentText("45 daqiqalik dars sessiyasi davom etmoqda")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 30
            y = 120
        }

        // Bosh konteyner
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                setColor(Color.parseColor("#E60B132B")) // Dark semi-transparent
                setStroke(3, Color.parseColor("#00F0FF")) // Neon cyan stroke
            }
            background = bg
            setPadding(28, 16, 28, 16)
            elevation = 20f
        }

        // Yuqori qism: Ikonka + Taymer + O'qituvchi
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Pulsatsiyalanuvchi yashil nuqta
        val liveDot = View(this).apply {
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#10B981"))
            }
            background = dotBg
            layoutParams = LinearLayout.LayoutParams(20, 20).apply {
                rightMargin = 16
            }
        }

        tvTimer = TextView(this).apply {
            text = "45:00"
            textSize = 22f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#00F0FF"))
            letterSpacing = 0.08f
        }

        tvTeacher = TextView(this).apply {
            text = " • O'qituvchi"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 12
            }
        }

        headerRow.addView(liveDot)
        headerRow.addView(tvTimer)
        headerRow.addView(tvTeacher)
        rootLayout.addView(headerRow)

        // Qo'shimcha boshqaruv paneli (bosganda kengayadi/yig'iladi)
        actionPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
        }

        // +10 daqiqa tugmasi
        val btnExtend = Button(this).apply {
            text = "+10m"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B132B"))
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#F59E0B")) // Neon gold
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(140, 72).apply {
                rightMargin = 10
            }
            setOnClickListener {
                LessonSessionManager.onExtendMinutes?.invoke(10)
            }
        }

        // Doska (Chizish) tugmasi
        val btnWhiteboard = Button(this).apply {
            text = "Doska"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B132B"))
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#00F0FF"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(160, 72).apply {
                rightMargin = 10
            }
            setOnClickListener {
                openMainActivity(openWhiteboard = true)
            }
        }

        // Ilovani ochish tugmasi
        val btnOpenApp = Button(this).apply {
            text = "Ilova"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#1E293B"))
                setStroke(2, Color.parseColor("#94A3B8"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(140, 72).apply {
                rightMargin = 10
            }
            setOnClickListener {
                openMainActivity(openWhiteboard = false)
            }
        }

        // Darsni tugatish (Qulflash) tugmasi
        val btnLock = Button(this).apply {
            text = "Tugatish"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#EF4444")) // Red
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(170, 72)
            setOnClickListener {
                LessonSessionManager.onFinishLesson?.invoke()
                stopSelf()
            }
        }

        actionPanel?.addView(btnExtend)
        actionPanel?.addView(btnWhiteboard)
        actionPanel?.addView(btnOpenApp)
        actionPanel?.addView(btnLock)
        rootLayout.addView(actionPanel)

        // Surish (Drag and Drop) funksiyasi
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        rootLayout.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    // Right gravity: subtraction from rawX
                    p.x = initialX - dx
                    p.y = initialY + dy
                    windowManager?.updateViewLayout(rootLayout, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        // Bir bosishda kengayadi yoki qisqaradi
                        isExpanded = !isExpanded
                        actionPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = rootLayout
        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeSession() {
        updateJob = serviceScope.launch {
            launch {
                LessonSessionManager.activeSeconds.collect { secs ->
                    val mm = secs / 60
                    val ss = secs % 60
                    tvTimer?.text = String.format("%02d:%02d", mm, ss)

                    // Vaqt tugaganda avtomatik yopiladi
                    if (secs <= 0) {
                        stopSelf()
                    }
                }
            }

            launch {
                LessonSessionManager.teacherName.collect { name ->
                    tvTeacher?.text = " • $name"
                }
            }

            launch {
                LessonSessionManager.isLocked.collect { locked ->
                    if (locked) {
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun openMainActivity(openWhiteboard: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_OPEN_WHITEBOARD", openWhiteboard)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FloatingSessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingSessionService::class.java)
            context.stopService(intent)
        }
    }
}
