package com.example.music_wyy.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求间隔控制，防止 429 限流（≥ 300ms）。
 */
class RateLimitInterceptor(
    private val minIntervalMs: Long = 500,
) : Interceptor {

    @Volatile
    private var lastRequestTime: Long = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < minIntervalMs) {
            Thread.sleep(minIntervalMs - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
        return chain.proceed(chain.request())
    }
}
