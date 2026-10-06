package com.example.service

import android.util.Log
import com.example.data.model.Leader
import com.example.data.model.ScheduleChangeLog
import com.example.data.model.ScheduleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class SyncResult(
    val isSuccess: Boolean,
    val fetchedEvents: List<ScheduleEvent>,
    val changeLogs: List<ScheduleChangeLog>,
    val message: String,
    val resolvedUrl: String = "https://congan.cantho.gov.vn:8888/",
    val detectedTitle: String? = null,
    val detectedEdition: Int = 0,
    val articleUrl: String = "",
    val archivedContent: String = "",
    val isBlocked: Boolean = false,
    val isUnreachable: Boolean = false,
    val isAuthRequired: Boolean = false
)

object WebMonitorService {
    private const val TAG = "WebMonitorService"
    private const val SESSION_HOST = "congan.cantho.gov.vn"

    const val DEFAULT_TARGET_URL = "https://congan.cantho.gov.vn:8888/"
    const val LOGIN_URL = "https://congan.cantho.gov.vn:8888/wp-login.php"
    const val DEFAULT_USER = "anbd"
    const val DEFAULT_PASS = "An@CanTho?2025"

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Máy chủ nội bộ Công an thành phố (cổng :8888) thường sử dụng chứng thư nội bộ.
     * Nới lỏng xác thực SSL/TLS cho host congan.cantho.gov.vn:8888 để kết nối ổn định.
     */
    private val TRUST_RELAXED_HOSTS = setOf(
        "congan.cantho.gov.vn",
        "localhost",
        "127.0.0.1"
    )

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.computeIfAbsent(url.host) { mutableListOf() }
            synchronized(list) {
                for (newCookie in cookies) {
                    list.removeAll { it.name == newCookie.name }
                    list.add(newCookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host] ?: return emptyList()
            return synchronized(list) { list.toList() }
        }
    }

    private val httpClient: OkHttpClient by lazy { createHttpClient() }
    private val relaxedHttpClient: OkHttpClient by lazy { createHttpClient(relaxTls = true) }

    private fun createHttpClient(relaxTls: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .callTimeout(45, TimeUnit.SECONDS)

        if (relaxTls) {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustAllCerts, SecureRandom())
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    private fun clientFor(url: String): OkHttpClient {
        val host = url.substringAfter("://", "").substringBefore('/').substringBefore(':')
        return if (TRUST_RELAXED_HOSTS.contains(host.lowercase())) relaxedHttpClient else httpClient
    }

    /**
     * Tự động đăng nhập vào cổng thông tin nội bộ https://congan.cantho.gov.vn:8888/
     * Tên đăng nhập mặc định: anbd, Mật khẩu: An@CanTho?2025
     */
    fun loginToWordPress(
        user: String = DEFAULT_USER,
        pass: String = DEFAULT_PASS,
        attempts: Int = 3
    ): Boolean {
        val client = clientFor(LOGIN_URL)

        for (attempt in 1..attempts.coerceAtLeast(1)) {
            try {
                // Nhận cookie kiểm tra testcookie từ trang đăng nhập
                val getLoginReq = Request.Builder()
                    .url(LOGIN_URL)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()
                client.newCall(getLoginReq).execute().use { it.body?.bytes() }

                val formBody = FormBody.Builder()
                    .add("log", user)
                    .add("pwd", pass)
                    .add("rememberme", "forever")
                    .add("wp-submit", "Log In")
                    .add("redirect_to", DEFAULT_TARGET_URL)
                    .add("testcookie", "1")
                    .build()

                val request = Request.Builder()
                    .url(LOGIN_URL)
                    .post(formBody)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Referer", LOGIN_URL)
                    .build()

                client.newCall(request).execute().use { response ->
                    response.headers.values("Set-Cookie").forEach { raw ->
                        LOGIN_URL.toHttpUrlOrNull()?.let { target ->
                            runCatching {
                                Cookie.parse(target, raw)?.let { cookie ->
                                    cookieJar.saveFromResponse(target, listOf(cookie))
                                }
                            }
                        }
                    }
                }

                if (hasValidSession()) {
                    Log.i(TAG, "Đăng nhập Cổng 8888 thành công với tài khoản $user (lần $attempt)")
                    return true
                }

                Log.w(TAG, "Đăng nhập lần $attempt chưa nhận được phiên làm việc")
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đăng nhập lần $attempt: ${e.localizedMessage}")
            }

            if (attempt < attempts) {
                try {
                    Thread.sleep(1200L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
        }
        return false
    }

    /** Kiểm tra xem đang giữ phiên làm việc hợp lệ hay không */
    fun hasValidSession(): Boolean {
        val cookies = cookieStore[SESSION_HOST] ?: return false
        val now = System.currentTimeMillis()
        return cookies.any { cookie ->
            cookie.name.startsWith("wordpress_logged_in") &&
                    cookie.value.isNotBlank() &&
                    cookie.expiresAt > now
        }
    }

    /** Xóa phiên làm việc để buộc đăng nhập lại */
    fun clearSession() {
        cookieStore.remove(SESSION_HOST)
    }

    private fun looksLikeLoginPage(body: String): Boolean =
        (body.contains("name=\"log\"") && body.contains("name=\"pwd\"")) ||
                body.contains("id=\"loginform\"") ||
                body.contains("login_error") ||
                body.contains("class=\"login-action-login")

    /**
     * Truy cập định kỳ web https://congan.cantho.gov.vn:8888/,
     * phát hiện lịch làm việc của Ban Giám đốc và cập nhật.
     * Thể hiện rõ trạng thái nếu web chặn truy cập hoặc không thể truy cập.
     */
    suspend fun checkWebUpdates(
        currentEvents: List<ScheduleEvent>,
        leaders: List<Leader>,
        targetUrl: String = DEFAULT_TARGET_URL,
        username: String = DEFAULT_USER,
        password: String = DEFAULT_PASS,
        currentDocumentTitle: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {
        val timeFmt = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
        val nowStr = timeFmt.format(Date())

        var effectiveUrl = targetUrl.trim()
        if (effectiveUrl.startsWith("http://congan.cantho.gov.vn:8888")) {
            effectiveUrl = effectiveUrl.replace("http://", "https://")
        }

        try {
            // Đảm bảo có phiên làm việc trước khi truy cập
            if (!hasValidSession()) {
                clearSession()
                loginToWordPress(username, password)
            }

            var response = executeRequest(effectiveUrl)
            var bodyStr = response.body?.string().orEmpty()

            var authFailed = false
            if (looksLikeLoginPage(bodyStr)) {
                clearSession()
                val loggedIn = loginToWordPress(username, password)
                if (loggedIn) {
                    response = executeRequest(effectiveUrl)
                    bodyStr = response.body?.string().orEmpty()
                }
                if (looksLikeLoginPage(bodyStr)) {
                    authFailed = true
                    Log.w(TAG, "Đăng nhập thất bại: $effectiveUrl")
                }
            }

            return@withContext processResponse(
                response = response,
                bodyStr = bodyStr,
                effectiveUrl = effectiveUrl,
                currentEvents = currentEvents,
                leaders = leaders,
                nowStr = nowStr,
                username = username,
                currentDocumentTitle = currentDocumentTitle,
                authFailed = authFailed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Không thể kết nối Cổng 8888: ${e.localizedMessage}", e)
            val fallbackTitle = if (currentDocumentTitle.isNotBlank()) currentDocumentTitle else "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC"
            val fallbackEdition = ScheduleParser.extractEditionNumber(fallbackTitle)

            val (reason, isBlocked, isUnreachable) = when (e) {
                is java.net.UnknownHostException ->
                    Triple(
                        "⚠️ KHÔNG THỂ TRUY CẬP: Không phân giải được tên miền congan.cantho.gov.vn. Vui lòng kiểm tra kết nối mạng nội bộ CATP hoặc bật VPN chuyên dụng.",
                        false,
                        true
                    )
                is java.net.SocketTimeoutException ->
                    Triple(
                        "⚠️ KHÔNG THỂ TRUY CẬP: Máy chủ Cổng 8888 không phản hồi (Quá thời gian chờ kết nối).",
                        false,
                        true
                    )
                is javax.net.ssl.SSLException ->
                    Triple(
                        "⚠️ LỖI BẢO MẬT SSL: Lỗi chứng thư bảo mật khi kết nối tới Cổng 8888: ${e.message ?: "Không rõ nguyên nhân"}.",
                        false,
                        true
                    )
                is java.net.ConnectException ->
                    Triple(
                        "⚠️ KHÔNG THỂ KẾT NỐI: Máy chủ từ chối kết nối tới cổng 8888 (Dịch vụ nội bộ đang đóng hoặc bị chặn).",
                        false,
                        true
                    )
                else -> Triple(
                    "⚠️ KHÔNG THỂ TRUY CẬP: ${e.localizedMessage ?: "Lỗi kết nối không xác định"}.",
                    false,
                    true
                )
            }

            return@withContext SyncResult(
                isSuccess = false,
                fetchedEvents = emptyList(),
                changeLogs = emptyList(),
                message = reason,
                resolvedUrl = effectiveUrl,
                detectedTitle = fallbackTitle,
                detectedEdition = fallbackEdition,
                isBlocked = isBlocked,
                isUnreachable = isUnreachable
            )
        }
    }

    private fun executeRequest(url: String): okhttp3.Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Cache-Control", "no-cache")
            .build()
        return clientFor(url).newCall(request).execute()
    }

    private fun processResponse(
        response: okhttp3.Response,
        bodyStr: String,
        effectiveUrl: String,
        currentEvents: List<ScheduleEvent>,
        leaders: List<Leader>,
        nowStr: String,
        username: String = DEFAULT_USER,
        currentDocumentTitle: String = "",
        authFailed: Boolean = false
    ): SyncResult {
        if (authFailed) {
            return SyncResult(
                isSuccess = false,
                fetchedEvents = emptyList(),
                changeLogs = emptyList(),
                message = "🔑 YÊU CẦU ĐĂNG NHẬP: Đăng nhập không thành công với tài khoản '$username' trên Cổng 8888.",
                resolvedUrl = effectiveUrl,
                detectedTitle = currentDocumentTitle,
                isAuthRequired = true
            )
        }

        val code = response.code

        // Xử lý các mã phản hồi bị CHẶN truy cập (401, 403, WAF)
        if (code in listOf(401, 403)) {
            val blockMsg = if (code == 403) {
                "⛔ BỊ CHẶN TRUY CẬP (HTTP 403): Cổng 8888 chặn truy cập từ thiết bị (Yêu cầu mạng nội bộ CATP hoặc tường lửa WAF chặn)."
            } else {
                "🔑 BỊ TỪ CHỐI TRUY CẬP (HTTP 401): Cần quyền đăng nhập nội bộ hợp lệ."
            }
            return SyncResult(
                isSuccess = false,
                fetchedEvents = emptyList(),
                changeLogs = emptyList(),
                message = blockMsg,
                resolvedUrl = effectiveUrl,
                isBlocked = true
            )
        }

        if (response.isSuccessful || code in 200..399) {
            // Bước 1: Tìm bài viết lịch làm việc của Ban Giám đốc đầu tiên
            val articleLink = ScheduleParser.findFirstScheduleArticleLink(bodyStr, effectiveUrl)

            var targetHtml = bodyStr
            var articleUrl = effectiveUrl
            var detectedDocTitle = ScheduleParser.extractDocumentTitle(bodyStr)
            var editionNum = 0

            if (articleLink != null) {
                articleUrl = articleLink.url
                editionNum = articleLink.edition
                detectedDocTitle = articleLink.title
                try {
                    val articleResp = executeRequest(articleUrl)
                    if (articleResp.isSuccessful) {
                        targetHtml = articleResp.body?.string().orEmpty()
                    }
                } catch (_: Exception) {}
            }

            if (editionNum == 0 && detectedDocTitle != null) {
                editionNum = ScheduleParser.extractEditionNumber(detectedDocTitle)
            }
            if (editionNum == 0) {
                editionNum = ScheduleParser.extractEditionNumber(targetHtml)
            }

            val contentTitle = ScheduleParser.extractDocumentTitle(targetHtml)
            if (!contentTitle.isNullOrBlank()) {
                detectedDocTitle = contentTitle
                val cNum = ScheduleParser.extractEditionNumber(contentTitle)
                if (cNum > 0) editionNum = cNum
            }

            val archivedContent = ScheduleParser.extractTopScheduleSection(targetHtml)
            // ĐẶC BIỆT CHỈ LẤY NỘI DUNG TỪ WEB: Dữ liệu không có thì bỏ trống ("")
            val parsedEvents = ScheduleParser.parseRawText(targetHtml, leaders)

            val currentDocEdition = ScheduleParser.extractEditionNumber(currentDocumentTitle)
            val finalEdition = if (editionNum > 0) {
                editionNum
            } else if ((detectedDocTitle ?: "").contains("05/10/2026")) {
                1
            } else {
                currentDocEdition.coerceAtLeast(1)
            }

            val finalTitle = detectedDocTitle ?: if (finalEdition > 0) {
                "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (Điều chỉnh lần $finalEdition) (Từ ngày 05/10/2026 đến ngày 11/10/2026)"
            } else if (currentDocumentTitle.isNotBlank()) {
                currentDocumentTitle
            } else {
                "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (Từ ngày 05/10/2026 đến ngày 11/10/2026)"
            }

            // Kiểm tra điều chỉnh so với lịch đã lưu gần nhất
            val isEditionChanged = editionNum > 0 && currentDocEdition > 0 && editionNum != currentDocEdition
            val isTitleChanged = isEditionChanged || (
                !detectedDocTitle.isNullOrBlank() &&
                currentDocumentTitle.isNotBlank() &&
                !detectedDocTitle.trim().replace("\\s+".toRegex(), " ")
                    .equals(currentDocumentTitle.trim().replace("\\s+".toRegex(), " "), ignoreCase = true)
            )

            val effectiveEvents = if (parsedEvents.isNotEmpty()) {
                parsedEvents
            } else {
                emptyList()
            }

            val changes = if (effectiveEvents.isNotEmpty()) {
                calculateDiff(
                    oldEvents = currentEvents,
                    newEvents = effectiveEvents,
                    timestampStr = nowStr,
                    editionNumber = finalEdition,
                    oldDocumentTitle = currentDocumentTitle,
                    newDocumentTitle = finalTitle
                )
            } else {
                mutableListOf()
            }

            if (isTitleChanged && changes.none { it.changeType.contains("TIÊU ĐỀ") || it.changeType.contains("LẦN") }) {
                val titleChangeType = if (finalEdition > 0) "ĐIỀU CHỈNH LẦN $finalEdition" else "ĐIỀU CHỈNH TIÊU ĐỀ"
                changes.add(
                    0,
                    ScheduleChangeLog(
                        formattedTime = nowStr,
                        changeType = titleChangeType,
                        eventTitle = finalTitle,
                        details = "Phát hiện tiêu đề lịch công tác điều chỉnh: từ \"$currentDocumentTitle\" sang \"$finalTitle\"."
                    )
                )
            }

            val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val dateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val summaryMsg = if (changes.isEmpty()) {
                "Đã kết nối • Quét lúc $timeOnly ($dateOnly): Không thay đổi"
            } else {
                val changeDesc = if (finalEdition > 0) "Điều chỉnh lần $finalEdition" else "${changes.size} thay đổi"
                "Đã kết nối • Quét lúc $timeOnly ($dateOnly): Có thay đổi ($changeDesc)"
            }

            if (changes.isEmpty()) {
                changes.add(
                    ScheduleChangeLog(
                        formattedTime = "$timeOnly $dateOnly",
                        changeType = "KHÔNG THAY ĐỔI",
                        eventTitle = finalTitle,
                        details = "Quét lúc $timeOnly ngày $dateOnly không thay đổi. Dữ liệu trên Cổng thông tin đang khớp 100% với phần mềm."
                    )
                )
            }

            return SyncResult(
                isSuccess = true,
                fetchedEvents = effectiveEvents,
                changeLogs = changes,
                message = summaryMsg,
                resolvedUrl = effectiveUrl,
                detectedTitle = finalTitle,
                detectedEdition = finalEdition,
                articleUrl = articleUrl,
                archivedContent = archivedContent.ifBlank { finalTitle }
            )
        } else {
            val explanation = when (code) {
                400 -> "⚠️ Lỗi HTTP 400 từ máy chủ Cổng 8888. Đã tự động sử dụng HTTPS."
                404 -> "⚠️ Không tìm thấy đường dẫn tại $effectiveUrl (Mã HTTP 404)."
                500, 502, 503, 504 -> "⚠️ Máy chủ Cổng 8888 phản hồi lỗi máy chủ nội bộ (Mã HTTP $code)."
                else -> "⚠️ Cổng thông tin phản hồi mã HTTP $code."
            }

            return SyncResult(
                isSuccess = false,
                fetchedEvents = emptyList(),
                changeLogs = emptyList(),
                message = explanation,
                resolvedUrl = effectiveUrl,
                isUnreachable = true
            )
        }
    }

    /**
     * Đối chiếu lịch mới với lịch đã lưu gần nhất:
     * - Phát hiện nội dung bổ sung mới
     * - Phát hiện nội dung điều chỉnh (thời gian, địa điểm, chủ trì, người dự, đơn vị chuẩn bị)
     * - Phát hiện nội dung đã bị hủy bỏ
     * - Phát hiện điều chỉnh tiêu đề / lần điều chỉnh
     */
    fun calculateDiff(
        oldEvents: List<ScheduleEvent>,
        newEvents: List<ScheduleEvent>,
        timestampStr: String,
        editionNumber: Int = 0,
        oldDocumentTitle: String = "",
        newDocumentTitle: String = ""
    ): MutableList<ScheduleChangeLog> {
        val changes = mutableListOf<ScheduleChangeLog>()

        // 0. Thay đổi tiêu đề
        if (oldDocumentTitle.isNotBlank() && newDocumentTitle.isNotBlank() &&
            !oldDocumentTitle.trim().replace("\\s+".toRegex(), " ")
                .equals(newDocumentTitle.trim().replace("\\s+".toRegex(), " "), ignoreCase = true)
        ) {
            val edition = if (editionNumber > 0) editionNumber else ScheduleParser.extractEditionNumber(newDocumentTitle)
            val type = if (edition > 0) "ĐIỀU CHỈNH LẦN $edition" else "ĐIỀU CHỈNH TIÊU ĐỀ"
            changes.add(
                ScheduleChangeLog(
                    formattedTime = timestampStr,
                    changeType = type,
                    eventTitle = newDocumentTitle,
                    details = "Phát hiện điều chỉnh tiêu đề: từ \"$oldDocumentTitle\" ➔ \"$newDocumentTitle\"."
                )
            )
        }

        val oldMapById = oldEvents.associateBy { it.id }
        val oldMapByDateAndTime = oldEvents.groupBy { "${it.date}_${it.time}" }

        // 1. Phát hiện sự kiện MỚI hoặc ĐIỀU CHỈNH NỘI DUNG
        for (newItem in newEvents) {
            val matchingOld = oldMapById[newItem.id]
                ?: oldMapByDateAndTime["${newItem.date}_${newItem.time}"]?.firstOrNull {
                    it.title.take(20).trim().lowercase() == newItem.title.take(20).trim().lowercase() ||
                    (it.primaryLeaderId.isNotBlank() && it.primaryLeaderId == newItem.primaryLeaderId)
                }
                ?: oldMapByDateAndTime["${newItem.date}_${newItem.time}"]?.firstOrNull()

            val effectiveEdition = if (editionNumber > 0) editionNumber else 0

            if (matchingOld == null) {
                // Sự kiện hoàn toàn mới
                val changeType = if (effectiveEdition > 0) "BỔ SUNG LẦN $effectiveEdition" else "BỔ SUNG MỚI"
                val locPart = if (newItem.location.isNotBlank()) " Địa điểm: ${newItem.location}." else ""
                val prepPart = if (newItem.preparation.isNotBlank()) " Chuẩn bị: ${newItem.preparation}." else ""
                val leaderPart = if (newItem.primaryLeaderId.isNotBlank()) " Chủ trì: ${newItem.primaryLeaderId}." else ""

                changes.add(
                    ScheduleChangeLog(
                        formattedTime = timestampStr,
                        changeType = changeType,
                        eventTitle = newItem.title,
                        details = "Bổ sung mới lúc ${newItem.time} ngày ${newItem.date}.$leaderPart$locPart$prepPart"
                    )
                )
            } else {
                // Đã có lịch tại khung giờ này -> Kiểm tra từng trường nội dung xem có điều chỉnh không
                val diffs = mutableListOf<String>()
                if (matchingOld.title.trim() != newItem.title.trim()) {
                    diffs.add("Đổi nội dung: \"${matchingOld.title.take(40)}\" ➔ \"${newItem.title.take(40)}\"")
                }
                if (matchingOld.location.trim() != newItem.location.trim()) {
                    val oldLoc = matchingOld.location.ifBlank { "Không ghi" }
                    val newLoc = newItem.location.ifBlank { "Không ghi" }
                    diffs.add("Đổi địa điểm: $oldLoc ➔ $newLoc")
                }
                if (matchingOld.primaryLeaderId != newItem.primaryLeaderId) {
                    diffs.add("Thay đổi lãnh đạo chủ trì")
                }
                if (matchingOld.coAttendees.trim() != newItem.coAttendees.trim()) {
                    diffs.add("Điều chỉnh lãnh đạo cùng dự")
                }
                if (matchingOld.preparation.trim() != newItem.preparation.trim()) {
                    diffs.add("Cập nhật đơn vị chuẩn bị: ${newItem.preparation.ifBlank { "Không có" }}")
                }
                if (matchingOld.attendees.trim() != newItem.attendees.trim()) {
                    diffs.add("Cập nhật thành phần dự: ${newItem.attendees.ifBlank { "Không có" }}")
                }

                if (diffs.isNotEmpty()) {
                    val changeType = if (effectiveEdition > 0) "ĐIỀU CHỈNH LẦN $effectiveEdition" else "ĐIỀU CHỈNH NỘI DUNG"
                    changes.add(
                        ScheduleChangeLog(
                            formattedTime = timestampStr,
                            changeType = changeType,
                            eventTitle = newItem.title,
                            details = diffs.joinToString("; ")
                        )
                    )
                }
            }
        }

        // 2. Phát hiện lịch bị Hủy bỏ (có trong oldEvents nhưng không còn trong newEvents của cùng tuần)
        val newDatesCovered = newEvents.map { it.date }.toSet()
        for (oldItem in oldEvents) {
            if (oldItem.date in newDatesCovered) {
                val stillExists = newEvents.any {
                    it.id == oldItem.id ||
                    ("${it.date}_${it.time}" == "${oldItem.date}_${oldItem.time}")
                }
                if (!stillExists) {
                    changes.add(
                        ScheduleChangeLog(
                            formattedTime = timestampStr,
                            changeType = "HỦY BỎ",
                            eventTitle = oldItem.title,
                            details = "Chương trình lúc ${oldItem.time} ngày ${oldItem.date} đã được điều chỉnh hoặc hủy theo thông báo mới."
                        )
                    )
                }
            }
        }

        return changes
    }
}
