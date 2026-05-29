package com.example.music_wyy.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 自动附加 Cookie 到每个请求。
 */
class CookieInterceptor(
    private val cookieProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val cookie = cookieProvider() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .addHeader("Cookie", cookie)
            .build()
        return chain.proceed(request)
    }
}
