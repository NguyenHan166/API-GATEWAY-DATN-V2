package com.asoft.artsal.photo.utils

import android.view.View
import timber.log.Timber
private const val SAFE_INTERVAL = 750

class SafeOnClickListener(
    private val isSafe: Boolean,
    private val action: (view: View) -> Unit
) : View.OnClickListener {
    private var lastClickTimestamp = 0L

    override fun onClick(clickedView: View) {
        if (!isSafe) {
            action(clickedView)
            return
        }
        val currentTimestamp = System.currentTimeMillis()
        Timber.tag("SafeOnClickListener").d("CurrentTimesStamp ==> $currentTimestamp")
        if (lastClickTimestamp == 0L || currentTimestamp - lastClickTimestamp > SAFE_INTERVAL) {
            action(clickedView)
            lastClickTimestamp = currentTimestamp
        }
    }
}