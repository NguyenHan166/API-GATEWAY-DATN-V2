package com.asoft.artsal.photo.component

internal interface OnBottomListener {

    fun onBottom()

    fun onScroll(firstVisible: Int, lastVisible: Int, dx: Int, dy: Int)

    fun onShow()

    fun onHide()
}