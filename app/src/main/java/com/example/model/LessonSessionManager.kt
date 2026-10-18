package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doska dars sessiyasi holati - ViewModel va Floating Overlay Service o'rtasida
 * ma'lumotlarni sinxronlashtirish uchun yagona boshqaruvchi obyekt.
 */
object LessonSessionManager {
    val activeSeconds = MutableStateFlow(45 * 60)
    val sessionTotalSeconds = MutableStateFlow(45 * 60)
    val isLocked = MutableStateFlow(true)
    val isPaused = MutableStateFlow(false)
    val teacherName = MutableStateFlow("O'qituvchi")

    // Overlay boshqaruv harakatlari
    var onExtendMinutes: ((Int) -> Unit)? = null
    var onTogglePause: (() -> Unit)? = null
    var onFinishLesson: (() -> Unit)? = null
    var onOpenApp: (() -> Unit)? = null
    var onOpenWhiteboard: (() -> Unit)? = null
}
