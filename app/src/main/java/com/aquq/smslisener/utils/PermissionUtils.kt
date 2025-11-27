package com.aquq.smslisener.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat


object PermissionUtil {
    const val REQUEST_CODE_PERMISSIONS: Int = 100


    /**
     * Check if multiple permissions are granted, if not request them.
     *
     * @param activity calling activity which needs permissions.
     * @param permissions one or more permissions, such as [android.Manifest.permission.CAMERA].
     * @return true if all permissions are granted, false if at least one is not granted yet.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun checkAndRequestPermissions(activity: Activity, vararg permissions: String): Boolean {
        val permissionsList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionState = activity.checkSelfPermission(permission)
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                permissionsList.add(permission)
            }
        }
        if (!permissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsList.toTypedArray<String>(),
                REQUEST_CODE_PERMISSIONS
            )
            return false
        }

        return true
    }


    /**
     * Handle the result of permission request, should be called from the calling [Activity]'s
     * [ActivityCompat.OnRequestPermissionsResultCallback.onRequestPermissionsResult]
     *
     * @param activity calling activity which needs permissions.
     * @param requestCode code used for requesting permission.
     * @param permissions permissions which were requested.
     * @param grantResults results of request.
     * @param callBack Callback interface to receive the result of permission request.
     */
    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        callBack: PermissionsCallBack?
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.size > 0) {
            val permissionsList: MutableList<String> = ArrayList()
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionsList.add(permissions[i])
                }
            }

            if (permissionsList.isEmpty() && callBack != null) {
                callBack.permissionsGranted()
            } else {
                var showRationale = false
                for (permission in permissionsList) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            permission
                        )
                    ) {
                        showRationale = true
                        break
                    }
                }

                if (showRationale) {
                    showAlertDialog(activity,
                        { dialogInterface, i ->
                            checkAndRequestPermissions(
                                activity,
                                *permissionsList.toTypedArray<String>()
                            )
                        },
                        { dialogInterface, i -> callBack?.permissionsDenied() })
                }
            }
        }
    }

    /**
     * Show alert if any permission is denied and ask again for it.
     *
     * @param context
     * @param okListener
     * @param cancelListener
     */
    private fun showAlertDialog(
        context: Context,
        okListener: DialogInterface.OnClickListener,
        cancelListener: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(context)
            .setMessage("Some permissions are not granted. Application may not work as expected. Do you want to grant them?")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", cancelListener)
            .create()
            .show()
    }

    interface PermissionsCallBack {
        fun permissionsGranted()

        fun permissionsDenied()
    }
}
