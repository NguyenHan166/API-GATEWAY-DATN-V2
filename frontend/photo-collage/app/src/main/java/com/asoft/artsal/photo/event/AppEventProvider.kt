package com.asoft.artsal.photo.event


sealed interface AppEventProvider {
    data object RedirectScreen : AppEventProvider
    data object RedirectSavedResultScreen : AppEventProvider
}