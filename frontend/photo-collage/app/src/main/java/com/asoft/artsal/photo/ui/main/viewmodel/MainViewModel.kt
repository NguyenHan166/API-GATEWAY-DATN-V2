package com.asoft.artsal.photo.ui.main.viewmodel

import android.app.Application
import com.asoft.artsal.photo.base.BaseViewModel
import com.asoft.artsal.photo.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    override val app: Application,
) : BaseViewModel(app) {

    val statusFetchRemoteConfig = SingleLiveEvent<Boolean>()

}
