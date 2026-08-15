package com.example.androidmixtape.permissions

import android.Manifest

object AudioPermissionPolicy {
    fun requiredPermission(sdkInt: Int): String =
        if (sdkInt >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun requiredRuntimePermission(sdkInt: Int): String? =
        if (sdkInt >= 23) requiredPermission(sdkInt) else null

    fun status(isGranted: Boolean): AudioPermissionStatus =
        if (isGranted) AudioPermissionStatus.Granted else AudioPermissionStatus.PermissionRequired
}

enum class AudioPermissionStatus {
    PermissionRequired,
    Granted,
}
