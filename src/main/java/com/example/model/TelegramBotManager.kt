package com.example.model

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramBotManager(
    private val onUnlockRequested: (teacherName: String, durationMinutes: Int) -> Unit,
    private val onLockRequested: () -> Unit,
    private val onExtendRequested: (additionalMinutes: Int) -> Unit,
    private val getStatusInfo: () -> String
) {
    private val tag = "TelegramBotManager"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var pollingJob: Job? = null
    private var lastUpdateId = 0L

    var isPollingActive = false
        private set

    var lastStatusMessage = "Kutilmoqda"
        private set

    fun startPolling(token: String, scope: CoroutineScope) {
        stopPolling()
        if (token.isBlank()) {
            lastStatusMessage = "Token kiritilmagan"
            return
        }

        pollingJob = scope.launch(Dispatchers.IO) {
            isPollingActive = true
            lastStatusMessage = "Telegram botga ulanmoqda..."
            Log.d(tag, "Starting Telegram polling...")

            var backoffDelay = 2000L

            while (isActive) {
                try {
                    val url = "https://api.telegram.org/bot$token/getUpdates?offset=${lastUpdateId + 1}&timeout=15"
                    val request = Request.Builder().url(url).build()

                    val response = client.newCall(request).execute()
                    val bodyString = response.body?.string()

                    if (response.isSuccessful && bodyString != null) {
                        backoffDelay = 2000L
                        lastStatusMessage = "Bot faol va buyruqlarni qabul qilmoqda"
                        val json = JSONObject(bodyString)
                        if (json.optBoolean("ok")) {
                            val result = json.optJSONArray("result") ?: JSONArray()
                            for (i in 0 until result.length()) {
                                val update = result.getJSONObject(i)
                                val updateId = update.getLong("update_id")
                                if (updateId > lastUpdateId) {
                                    lastUpdateId = updateId
                                }
                                processUpdate(token, update)
                            }
                        }
                    } else {
                        val code = response.code
                        lastStatusMessage = if (code == 401 || code == 404) {
                            "Bot tokeni noto'g'ri (Error $code)"
                        } else {
                            "Tarmoq xatosi: $code"
                        }
                        delay(backoffDelay)
                        backoffDelay = (backoffDelay * 2).coerceAtMost(30000L)
                    }
                } catch (e: Exception) {
                    if (e is IOException) {
                        lastStatusMessage = "Aloqa uzildi, qayta ulanmoqda..."
                    } else {
                        lastStatusMessage = "Xatolik: ${e.message}"
                    }
                    Log.e(tag, "Polling error", e)
                    delay(backoffDelay)
                    backoffDelay = (backoffDelay * 2).coerceAtMost(30000L)
                }
            }
            isPollingActive = false
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        isPollingActive = false
    }

    private suspend fun processUpdate(token: String, update: JSONObject) {
        val message = update.optJSONObject("message") ?: update.optJSONObject("callback_query")?.optJSONObject("message")
        val callbackQuery = update.optJSONObject("callback_query")

        val fromObj = update.optJSONObject("message")?.optJSONObject("from")
            ?: callbackQuery?.optJSONObject("from")

        val teacherName = buildString {
            val first = fromObj?.optString("first_name", "") ?: ""
            val last = fromObj?.optString("last_name", "") ?: ""
            val user = fromObj?.optString("username", "") ?: ""
            if (first.isNotBlank() || last.isNotBlank()) {
                append("$first $last".trim())
            } else if (user.isNotBlank()) {
                append("@$user")
            } else {
                append("O'qituvchi")
            }
        }

        var text = update.optJSONObject("message")?.optString("text", "") ?: ""
        val callbackData = callbackQuery?.optString("data", "") ?: ""
        if (callbackData.isNotBlank()) {
            text = callbackData
        }

        val chatId = message?.optJSONObject("chat")?.optLong("id")
            ?: callbackQuery?.optJSONObject("from")?.optLong("id")
            ?: return

        text = text.trim()
        Log.d(tag, "Received command: '$text' from $teacherName ($chatId)")

        when {
            text.startsWith("/start") || text.startsWith("/help") || text.equals("yordam", ignoreCase = true) -> {
                sendWelcomeMenu(token, chatId, teacherName)
            }
            text.equals("🔓 45 daqiqalik darsni boshlash", ignoreCase = true)
                    || text.startsWith("/unlock")
                    || text == "cmd_unlock" -> {
                withContext(Dispatchers.Main) {
                    onUnlockRequested(teacherName, 45)
                }
                sendMessage(
                    token = token,
                    chatId = chatId,
                    text = """
                        ✅ <b>86" Interaktiv Doska ochildi!</b>
                        
                        👨‍🏫 O'qituvchi: <b>$teacherName</b>
                        ⏳ Dars davomiyligi: <b>45 daqiqa</b>
                        
                        🎯 <i>Doskadan erkin foydalanishingiz mumkin. Chizish, veb-brauzer yoki HDMI ulanishlar faol.</i>
                    """.trimIndent()
                )
            }
            text.equals("🔒 Doskani qulflash", ignoreCase = true)
                    || text.startsWith("/lock")
                    || text == "cmd_lock" -> {
                withContext(Dispatchers.Main) {
                    onLockRequested()
                }
                sendMessage(
                    token = token,
                    chatId = chatId,
                    text = """
                        🔒 <b>Doska qulflanadi!</b>
                        
                        Kutish ekrani va jonli fon faollashtirildi. Ekranga ruxsatsiz teginishlar cheklandi.
                    """.trimIndent()
                )
            }
            text.equals("➕ 10 daqiqa qo'shish", ignoreCase = true)
                    || text.startsWith("/extend")
                    || text == "cmd_extend" -> {
                withContext(Dispatchers.Main) {
                    onExtendRequested(10)
                }
                sendMessage(
                    token = token,
                    chatId = chatId,
                    text = "➕ <b>Dars vaqti 10 daqiqaga uzaytirildi!</b>"
                )
            }
            text.equals("⏱ Qolgan vaqtni bilish", ignoreCase = true)
                    || text.startsWith("/status")
                    || text == "cmd_status" -> {
                val statusText = withContext(Dispatchers.Main) { getStatusInfo() }
                sendMessage(
                    token = token,
                    chatId = chatId,
                    text = "📊 <b>Doska holati:</b>\n$statusText"
                )
            }
            else -> {
                sendWelcomeMenu(token, chatId, teacherName)
            }
        }
    }

    private fun sendWelcomeMenu(token: String, chatId: Long, teacherName: String) {
        val welcomeText = """
            👋 <b>Assalomu alaykum, $teacherName!</b>
            
            Bu <b>86" 4K UHD Interaktiv Doska</b> boshqaruv boti.
            
            Darsni boshlash uchun quyidagi tugmani bosing:
        """.trimIndent()

        // Send reply keyboard with convenient quick action buttons
        val keyboard = JSONObject().apply {
            put("keyboard", JSONArray().apply {
                put(JSONArray().apply {
                    put("🔓 45 daqiqalik darsni boshlash")
                })
                put(JSONArray().apply {
                    put("⏱ Qolgan vaqtni bilish")
                    put("➕ 10 daqiqa qo'shish")
                })
                put(JSONArray().apply {
                    put("🔒 Doskani qulflash")
                })
            })
            put("resize_keyboard", true)
            put("one_time_keyboard", false)
        }

        sendMessage(token, chatId, welcomeText, keyboard)
    }

    fun sendMessage(token: String, chatId: Long, text: String, replyMarkup: JSONObject? = null) {
        if (token.isBlank() || chatId == 0L) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", text)
                    put("parse_mode", "HTML")
                    if (replyMarkup != null) {
                        put("reply_markup", replyMarkup)
                    }
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = payload.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$token/sendMessage")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(tag, "Failed to send message: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error sending telegram message", e)
            }
        }
    }

    suspend fun testToken(token: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Pair(false, "Token bo'sh bo'lmasligi kerak")
        try {
            val url = "https://api.telegram.org/bot$token/getMe"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    val json = JSONObject(body)
                    if (json.optBoolean("ok")) {
                        val result = json.optJSONObject("result")
                        val botUsername = result?.optString("username", "bot") ?: "bot"
                        val botFirstName = result?.optString("first_name", "") ?: ""
                        return@withContext Pair(true, "@$botUsername ($botFirstName)")
                    }
                }
                return@withContext Pair(false, "Token noto'g'ri yoki bot topilmadi (HTTP ${resp.code})")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Tarmoq xatosi: ${e.localizedMessage}")
        }
    }
}
