package com.asoft.artsal.photo.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourcesProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharePreference: SharePreference
) {
//    fun getString(@StringRes stringResId: Int): String {
//        val locale = sharePreference.get<Language>(SharePreference.LANGUAGE_APP)?.code
//        val ctx = context.setAppLocale(locale.orEmpty())
//        return ctx.getString(stringResId)
//    }
}