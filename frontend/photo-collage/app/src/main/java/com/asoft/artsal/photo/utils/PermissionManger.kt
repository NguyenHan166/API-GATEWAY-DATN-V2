package com.asoft.artsal.photo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.extensions.showAndHideNav
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference

class PermissionManager {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionNoRequestAgainLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var pickImagesLauncher: ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    private var weakContext: WeakReference<Context>? = null
    private var weakActivity: WeakReference<Activity>? = null
    private var onGrantedCallback: ((Boolean) -> Unit)? = null
    private var alertDialogBuilder: AlertDialog? = null
    private var currentRequestedPermissions: Array<String> = emptyArray()
    private var currentRequestedPermission: String = ""

    private var titleRequestPermission: String = ""
    private var txtRationale = ""
    private var txtPermanentlyDenied = ""

    companion object {
        private const val TAG = "PermissionManager"
    }

    fun initializePermissionLauncher(context: Context, activity: Activity, fragment: Fragment) {
        weakContext = WeakReference(context)
        weakActivity = WeakReference(activity)

        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                permissionLauncher = fragment.registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val hasAnyPermission = permissions.any { it.value }
                    if (hasAnyPermission) {
                        onGrantedCallback?.invoke(true)
                        return@registerForActivityResult
                    }

                    val act = weakActivity?.get() ?: return@registerForActivityResult
                    val ctx = weakContext?.get() ?: return@registerForActivityResult
                    if (permissions.any { shouldShowRequestPermissionRationale(act, it.key) }) {
                        val listRequest = permissions.map { it.key }.toTypedArray()
                        showRequestAgainDialog(listRequest, ctx)
                    } else {
                        showSettingsDialog(ctx)
                    }
                }

                permissionNoRequestAgainLauncher = fragment.registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val hasAnyPermission = permissions.any { it.value }
                    if (hasAnyPermission) {
                        onGrantedCallback?.invoke(true)
                        return@registerForActivityResult
                    }
                }

                pickImagesLauncher = fragment.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    handleChooseImageResult(result)
                }

                settingsLauncher = fragment.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (currentRequestedPermission.isNotEmpty() && hasPermission(
                            currentRequestedPermission
                        )
                    ) {
                        onGrantedCallback?.invoke(true)
                    } else if (currentRequestedPermissions.isNotEmpty() && hasPermissions(
                            currentRequestedPermissions
                        )
                    ) {
                        onGrantedCallback?.invoke(true)
                    } else {
                        onGrantedCallback?.invoke(false)
                    }
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                alertDialogBuilder?.dismiss()
                alertDialogBuilder = null
                weakContext = null
                weakActivity = null
                onGrantedCallback = null
            }
        })
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            weakContext?.get() ?: return false,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    fun requestPermissions(
        permissions: Array<String>,
        title: String,
        txtRationale: String,
        txtPermanentlyDenied: String,
        onGranted: (Boolean) -> Unit,
    ) {
        this.titleRequestPermission = title
        this.txtRationale = txtRationale
        this.txtPermanentlyDenied = txtPermanentlyDenied
        this.currentRequestedPermissions = permissions
        this.onGrantedCallback = onGranted

        if (hasPermissions(permissions)) {
            onGranted(true)
            return
        }

        permissionLauncher.launch(permissions)
    }

    fun requestPermissionsNoRequestAgain(
        permissions: Array<String>,
        onGranted: (Boolean) -> Unit,
    ) {
        this.onGrantedCallback = onGranted
        if (hasPermissions(permissions)) {
            onGranted(true)
            return
        }
        permissionNoRequestAgainLauncher.launch(permissions)
    }

    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                && ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_DENIED
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        try {
            settingsLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback if launcher isn't initialized
            context.startActivity(intent)
        }
    }

    private fun showRequestAgainDialog(permissions: Array<String>, context: Context) {
        MaterialAlertDialogBuilder(weakContext?.get() ?: return, R.style.MaterialAlertDialogCustom)
            .setTitle(titleRequestPermission)
            .setMessage(txtRationale)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.agree)) { dialog, _ ->
                dialog.dismiss()
                permissionLauncher.launch(permissions)
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                onGrantedCallback?.invoke(false)
            }
            .showAndHideNav()

    }

    private fun showSettingsDialog(context: Context) {
        alertDialogBuilder = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialogCustom)
            .setTitle(titleRequestPermission)
            .setMessage(txtPermanentlyDenied)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.go_settings)) { dialog, _ ->
                dialog.dismiss()
                openAppSettings(context)
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                onGrantedCallback?.invoke(false)
            }
            .show()
    }

    fun requestPermissionAgain(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }

    fun getRequireImagePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }


    fun handleChooseImageResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                try {
                    val context = weakContext?.get() ?: return@let
                    val uris = mutableListOf<Uri>()

                    intent.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i)?.uri?.let { uri ->
                                uris.add(uri)
                            }
                        }
                    }

                    intent.data?.let { uri ->
                        uris.add(uri)
                    }

                    for (uri in uris) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                    }

                    onGrantedCallback?.invoke(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onGrantedCallback?.invoke(false)
                }
            } ?: run {
                onGrantedCallback?.invoke(true)
            }
        } else {
            onGrantedCallback?.invoke(false)
        }
    }

    fun hasImagePermissions(context: Context): Boolean {
        return getRequireImagePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}