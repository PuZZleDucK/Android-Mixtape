package com.example.androidmixtape.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPermissionPolicyTest {
    @Test
    fun sdk33AndNewerRequestsReadMediaAudio() {
        assertEquals(Manifest.permission.READ_MEDIA_AUDIO, AudioPermissionPolicy.requiredPermission(33))
        assertEquals(Manifest.permission.READ_MEDIA_AUDIO, AudioPermissionPolicy.requiredPermission(35))
    }

    @Test
    fun sdk23To32RequestsLegacyReadExternalStorage() {
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, AudioPermissionPolicy.requiredPermission(23))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, AudioPermissionPolicy.requiredPermission(24))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, AudioPermissionPolicy.requiredPermission(32))
    }

    @Test
    fun sdk19To22UsesInstallTimeStoragePermissionWithoutRuntimePrompt() {
        assertEquals(null, invokeOptionalRuntimePermission(19))
        assertEquals(null, invokeOptionalRuntimePermission(22))
    }

    @Test
    fun sdk23AndNewerExposeRuntimePermissionToRequest() {
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, invokeOptionalRuntimePermission(23))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, invokeOptionalRuntimePermission(32))
        assertEquals(Manifest.permission.READ_MEDIA_AUDIO, invokeOptionalRuntimePermission(33))
    }

    @Test
    fun permissionStatusReflectsGrantState() {
        assertEquals(AudioPermissionStatus.Granted, AudioPermissionPolicy.status(isGranted = true))
        assertEquals(AudioPermissionStatus.PermissionRequired, AudioPermissionPolicy.status(isGranted = false))
    }

    private fun invokeOptionalRuntimePermission(sdkInt: Int): String? {
        val method = AudioPermissionPolicy::class.java.methods.singleOrNull {
            it.name == "requiredRuntimePermission" && it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
        }
        org.junit.Assert.assertNotNull(
            "Add AudioPermissionPolicy.requiredRuntimePermission(sdkInt): String? so API 19/22 can skip runtime permission UI while API 23+ still requests storage/audio access.",
            method,
        )
        return method!!.invoke(AudioPermissionPolicy, sdkInt) as String?
    }
}
