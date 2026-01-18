package com.asoft.artsal.photo.base.network

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class UnauthorizedInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            Timber.d("UnauthorizedEvent")
        }
        return response
    }
}