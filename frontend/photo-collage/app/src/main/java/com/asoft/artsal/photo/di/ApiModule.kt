package com.asoft.artsal.photo.di

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.artsal.photo.editor.collage.maker.BuildConfig
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.base.network.UnauthorizedInterceptor
import com.asoft.artsal.photo.data.datasource.ImageService
import com.asoft.artsal.photo.utils.SharePreference
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LoggingRequest

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiRequest

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun providesFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            // increase the number of fetches available per hour during development.
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        // Set default Remote Config parameter values. An app uses the in-app default values, and
        // when you need to adjust those defaults, you set an updated value for only the values you
        // want to change in the Firebase console.
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        return remoteConfig
    }

    @Singleton
    @Provides
    @ApiRequest
    fun provideRetrofit(
        sharePreference: SharePreference
    ): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE)
        val httpClient: OkHttpClient = OkHttpClient().newBuilder()
            .readTimeout(240, TimeUnit.SECONDS)
            .connectTimeout(240, TimeUnit.SECONDS)
            .addInterceptor { chain: Interceptor.Chain ->
                val request: Request = chain.request()
                val newRequestBuilder = request.newBuilder()
                
                // Don't override Content-Type for multipart requests
                // Retrofit will automatically set multipart/form-data with boundary
                // Only add Accept header
                newRequestBuilder.addHeader("Accept", "application/json")
                
                chain.proceed(newRequestBuilder.build())
            }
            .cache(null)
            .addInterceptor(UnauthorizedInterceptor())
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_DOMAIN)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideImageService(
        @ApiRequest retrofit: Retrofit
    ): ImageService {
        return retrofit.create(ImageService::class.java)
    }

}