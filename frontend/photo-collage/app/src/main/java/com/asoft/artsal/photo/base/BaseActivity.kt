package com.asoft.artsal.photo.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.asoft.artsal.photo.extensions.hideNavigationBar
import com.google.gson.GsonBuilder
import com.asoft.artsal.photo.extensions.setAppLocale
import com.asoft.artsal.photo.extensions.setStatusBarStyle
import com.asoft.artsal.photo.utils.SharePreference
import timber.log.Timber

abstract class BaseActivity<B : ViewBinding> : AppCompatActivity() {
    private var _binding: B? = null
    protected val binding: B?
        get() = _binding

    protected open val isInsets = false

    override fun attachBaseContext(newBase: Context) {
        try {
            val sharePreference = SharePreference((newBase).getSharedPreferences(SharePreference.NAME, SharePreference.MODE), GsonBuilder().create())
            val locale = sharePreference.get<String>(SharePreference.LANGUAGE_APP, "en")
            super.attachBaseContext(newBase.setAppLocale(locale.orEmpty()))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag("[ACTIVITY_DEBUG]").d("${this@BaseActivity}::onCreate")
        hideNavigationBar()
        enableEdgeToEdge()
        val binding = onInflateView(layoutInflater)
        _binding = binding
        setContentView(binding.root)
        if (isInsets) {
            ViewCompat.setOnApplyWindowInsetsListener(_binding?.root ?: return) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        setupUi()
        renderUi()
    }

    override fun onStart() {
        super.onStart()
        Timber.tag("[ACTIVITY_DEBUG]").d("${this@BaseActivity}::onStart")
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("[ACTIVITY_DEBUG]").d("${this@BaseActivity}::onResume")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("[ACTIVITY_DEBUG]").d("${this@BaseActivity}::onPause")
    }

    override fun onStop() {
        super.onStop()
        Timber.tag("[ACTIVITY_DEBUG]").d("${this@BaseActivity}::onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("[ACTIVITY_DEBUG]").d("${this@BaseActivity}::onDestroy")
        _binding = null
    }

    protected abstract fun onInflateView(inflater: LayoutInflater): B

    protected abstract fun setupUi()

    protected abstract fun renderUi()

}