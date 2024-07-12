package com.lizongying.mytv0

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import com.google.gson.Gson
import com.lizongying.mytv0.requests.TimeResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    private var between: Long = 0

    fun getDateFormat(format: String): String {
        return SimpleDateFormat(
            format,
            Locale.CHINA
        ).format(Date(System.currentTimeMillis() - between))
    }

    fun getDateTimestamp(): Long {
        return (System.currentTimeMillis() - between) / 1000
    }

    suspend fun init() {
        try {
            val currentTimeMillis = getTimestampFromServer()
            if (currentTimeMillis > 0) {
                between = System.currentTimeMillis() - currentTimeMillis
            }
        } catch (e: Exception) {
            println("Failed to retrieve timestamp from server: ${e.message}")
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            init()
        }
    }

    /**
     * 从服务器获取时间戳
     * @return Long 时间戳
     */
    private suspend fun getTimestampFromServer(): Long {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder().build()
            val request = okhttp3.Request.Builder()
                .url("https://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val string = response.body()?.string()
                    Gson().fromJson(string, TimeResponse::class.java).data.t.toLong()
                }
            } catch (e: IOException) {
                // Handle network errors
                throw IOException("Error during network request", e)
            }
        }
    }

    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun isTmallDevice() = Build.MANUFACTURER.equals("Tmall", ignoreCase = true)

    fun formatUrl(url: String): String {
        // Check if the URL already starts with "http://" or "https://"
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
            return url
        }

        // Check if the URL starts with "//"
        if (url.startsWith("//")) {
            return "http://$url"
        }

        // Otherwise, add "http://" to the beginning of the URL
        return "http://${url}"
    }
}