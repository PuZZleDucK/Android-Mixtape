package com.example.androidmixtape.diagnostics

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.SessionInfo
import androidx.media3.session.MediaSession
import java.io.File
import java.security.MessageDigest

object AndroidAutoDiagnostics {
    const val TAG = "MixtapeAutoDiag"
    const val DIAGNOSTICS_FILE_NAME = "android-auto-diagnostics.log"
    private const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"
    private const val MAX_DIAGNOSTICS_FILE_BYTES = 4L * 1024L * 1024L

    fun logPhoneEnvironment(context: Context, reason: String) {
        runCatching {
            val packageManager = context.packageManager
            val appInfo = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS,
            )
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            Log.i(TAG, "phone_environment_begin reason=$reason")
            Log.i(
                TAG,
                "app package=${context.packageName} versionName=${appInfo.versionName} " +
                    "versionCode=${versionCode(appInfo)} installer=${installer(packageManager, context.packageName)} " +
                    "debuggable=${appInfo.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0}",
            )
            Log.i(
                TAG,
                "device manufacturer=${Build.MANUFACTURER} brand=${Build.BRAND} model=${Build.MODEL} " +
                    "device=${Build.DEVICE} product=${Build.PRODUCT} hardware=${Build.HARDWARE}",
            )
            Log.i(
                TAG,
                "android sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE} " +
                    "securityPatch=${Build.VERSION.SECURITY_PATCH} incremental=${Build.VERSION.INCREMENTAL} " +
                    "abis=${Build.SUPPORTED_ABIS.joinToString()} fingerprint=${Build.FINGERPRINT}",
            )
            Log.i(
                TAG,
                "mode currentModeType=${uiModeManager?.currentModeType} " +
                    "configurationUiMode=${context.resources.configuration.uiMode} " +
                    "automotive=${packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)} " +
                    "templatesHost=${packageManager.hasSystemFeature("android.software.car.templates_host")} " +
                    "mediaTemplatesHost=${packageManager.hasSystemFeature("android.software.car.templates_host.media")}",
            )
            Log.i(
                TAG,
                "permissions readMediaAudio=${permission(context, Manifest.permission.READ_MEDIA_AUDIO)} " +
                    "readExternalStorage=${permission(context, Manifest.permission.READ_EXTERNAL_STORAGE)} " +
                    "mediaTemplatesRequested=${appInfo.requestedPermissions?.contains("androidx.car.app.MEDIA_TEMPLATES")} " +
                    "mediaTemplatesGranted=${permission(context, "androidx.car.app.MEDIA_TEMPLATES")}",
            )
            logPackage(context, ANDROID_AUTO_PACKAGE, "android_auto")
            val carServices = packageManager.queryIntentServices(Intent(CarAppService.SERVICE_INTERFACE), PackageManager.MATCH_ALL)
            Log.i(
                TAG,
                "discovered_car_app_services count=${carServices.size} services=" +
                    carServices.joinToString { "${it.serviceInfo.packageName}/${it.serviceInfo.name}" },
            )
            Log.i(TAG, "phone_environment_end reason=$reason")
        }.onFailure { error ->
            Log.e(TAG, "phone_environment_failed reason=$reason", error)
        }
    }

    fun logCarHost(carContext: CarContext, sessionInfo: SessionInfo?, intent: Intent) {
        runCatching {
            val hostInfo = carContext.hostInfo
            val apiLevel = carContext.carAppApiLevel
            val configuration = carContext.resources.configuration
            val metrics = carContext.resources.displayMetrics
            log(
                carContext,
                "car_host_snapshot apiLevel=$apiLevel hostPackage=${hostInfo?.packageName} hostUid=${hostInfo?.uid} " +
                    "sessionInfo=$sessionInfo supportedTemplates=${supportedTemplates(sessionInfo, apiLevel)} " +
                    "display=${metrics.widthPixels}x${metrics.heightPixels}@${metrics.density} " +
                    "orientation=${configuration.orientation} uiMode=${configuration.uiMode} " +
                    "intentAction=${intent.action} intentFlags=0x${intent.flags.toString(16)} " +
                    "intentExtras=${intent.extras?.keySet()?.sorted()}",
            )
            Log.i(TAG, "car_host_begin")
            Log.i(
                TAG,
                "car_host apiLevel=$apiLevel hostPackage=${hostInfo?.packageName} hostUid=${hostInfo?.uid} " +
                    "callingComponent=${carContext.callingComponent}",
            )
            Log.i(
                TAG,
                "car_session info=$sessionInfo displayType=${sessionInfo?.displayType} sessionId=${sessionInfo?.sessionId} " +
                    "supportedTemplates=${supportedTemplates(sessionInfo, apiLevel)}",
            )
            Log.i(
                TAG,
                "car_display widthPx=${metrics.widthPixels} heightPx=${metrics.heightPixels} density=${metrics.density} " +
                    "orientation=${configuration.orientation} uiMode=${configuration.uiMode} " +
                    "nightMode=${configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK}",
            )
            Log.i(
                TAG,
                "car_intent action=${intent.action} flags=0x${intent.flags.toString(16)} " +
                    "categories=${intent.categories} dataScheme=${intent.data?.scheme} extras=${intent.extras?.keySet()?.sorted()}",
            )
            hostInfo?.packageName?.let { logPackage(carContext, it, "car_host_package") }
            Log.i(TAG, "car_host_end")
        }.onFailure { error ->
            Log.e(TAG, "car_host_failed", error)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun controller(controller: MediaSession.ControllerInfo): String =
        "package=${controller.packageName} uid=${controller.uid} trusted=${controller.isTrusted} " +
            "controllerVersion=${controller.controllerVersion} interfaceVersion=${controller.interfaceVersion} " +
            "maxMediaItemCommands=${controller.maxCommandsForMediaItems} " +
            "connectionHintKeys=${controller.connectionHints.keySet().sorted()}"

    fun log(context: Context, message: String) {
        Log.i(TAG, message)
        appendToDiagnosticsFile(context, "INFO $message")
    }

    fun logError(context: Context, message: String, error: Throwable) {
        Log.e(TAG, message, error)
        appendToDiagnosticsFile(
            context,
            "ERROR $message ${error.javaClass.name}:${error.message}\n${Log.getStackTraceString(error)}",
        )
    }

    private fun appendToDiagnosticsFile(context: Context, message: String) {
        runCatching {
            synchronized(this) {
                val file = File(context.filesDir, DIAGNOSTICS_FILE_NAME)
                if (file.length() >= MAX_DIAGNOSTICS_FILE_BYTES) {
                    file.writeText("${System.currentTimeMillis()} diagnostics_file_rotated\n")
                }
                file.appendText(
                    "${System.currentTimeMillis()} pid=${android.os.Process.myPid()} thread=${Thread.currentThread().name} $message\n",
                )
            }
        }.onFailure { error ->
            Log.w(TAG, "diagnostics_file_append_failed", error)
        }
    }

    private fun permission(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun supportedTemplates(sessionInfo: SessionInfo?, apiLevel: Int): String = runCatching {
        sessionInfo?.getSupportedTemplates(apiLevel)?.map { it.name }?.sorted()?.joinToString().orEmpty()
    }.getOrElse { "error:${it.javaClass.simpleName}:${it.message}" }

    private fun logPackage(context: Context, packageName: String, label: String) {
        val packageManager = context.packageManager
        runCatching {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val flags = info.applicationInfo?.flags ?: 0
            val signingDigests = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
                    ?.map { certificate -> sha256(certificate.toByteArray()) }
                    ?.joinToString()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.map { certificate -> sha256(certificate.toByteArray()) }?.joinToString()
            }
            Log.i(
                TAG,
                "$label package=$packageName versionName=${info.versionName} versionCode=${versionCode(info)} " +
                    "enabled=${info.applicationInfo?.enabled} system=${flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0} " +
                    "installer=${installer(packageManager, packageName)} signingSha256=$signingDigests",
            )
        }.onFailure { error ->
            Log.w(TAG, "$label package_unavailable=$packageName error=${error.javaClass.simpleName}:${error.message}")
        }
    }

    private fun versionCode(info: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

    private fun installer(packageManager: PackageManager, packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val source = packageManager.getInstallSourceInfo(packageName)
            listOfNotNull(source.installingPackageName, source.initiatingPackageName, source.originatingPackageName)
                .distinct()
                .joinToString()
                .ifEmpty { null }
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }
}
