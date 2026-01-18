package com.asoft.artsal.photo.utils

import android.os.CountDownTimer

class WrapperCountDownTimer(
    private val leftTimeMillis: Long, private val timerListener: TimerListener?
) {

    private val countDownTimer: CountDownTimer = object : CountDownTimer(leftTimeMillis, 1000L) {
        override fun onTick(millisUntilFinished: Long) {
            timerListener?.onTick(millisUntilFinished)
        }

        override fun onFinish() {
            timerListener?.onFinish()
        }
    }

    /**
     * Stop
     */
    fun stop() {
        countDownTimer.cancel()
        timerListener?.onStop()
    }

    /**
     * Start
     */
    fun start() {
        timerListener?.onStart(leftTimeMillis)
        countDownTimer.start()
    }
}

interface TimerListener {
    fun onStart(startTime: Long)
    fun onTick(millisUntilFinished: Long)
    fun onStop()
    fun onFinish()
}