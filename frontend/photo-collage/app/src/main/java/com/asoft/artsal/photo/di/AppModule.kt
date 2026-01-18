package com.asoft.artsal.photo.di

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.minsap.ad.consent.ConsentUMP
import com.asoft.artsal.photo.utils.PermissionManager
import com.asoft.artsal.photo.utils.SharePreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceIdQualifier

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun sharedPreference(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(SharePreference.NAME, SharePreference.MODE)

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Singleton
    @Provides
    fun provideConnectManager(@ApplicationContext context: Context): ConnectivityManager? {
        return getSystemService(context, ConnectivityManager::class.java)
    }

    @Provides
    @Singleton
    fun providesConnectivityManager(@ApplicationContext context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun providesGGCoroutineDispatchers(): GGCoroutineDispatchers {
        return GGCoroutineDispatchers(
            io = Dispatchers.IO,
            computation = Dispatchers.Default,
            main = Dispatchers.Main,
        )
    }

    @Provides
    fun providePermissionManager(): PermissionManager {
        return PermissionManager()
    }


    @Provides
    fun provideConsentUMP(): ConsentUMP {
        return ConsentUMP()
    }
}