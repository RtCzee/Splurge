package com.example.splurge.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Small permission helper so reminders can gate notification access cleanly.
 */
object NotificationPermissionChecker {
    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
