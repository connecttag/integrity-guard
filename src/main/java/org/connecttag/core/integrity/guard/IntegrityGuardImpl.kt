package org.connecttag.core.integrity.guard

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class IntegrityGuardImpl @Inject constructor() : IIntegrityGuard {

    override fun isAppSignatureValid(context: Context, expectedSignature: String): Boolean {
        return try {
            val packageName = context.packageName
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            } ?: return false

            signatures.any { signature ->
                val digest = MessageDigest.getInstance("SHA-256")
                val currentHash = digest.digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                currentHash.equals(expectedSignature, ignoreCase = true)
            }
        } catch (e: Exception) {
            Timber.e(e, "Signature validation failed")
            false
        }
    }

    override suspend fun isDeviceRooted(): Boolean = withContext(Dispatchers.IO) {
        checkRootFiles() || checkTestKeys() || checkSuBinary() || checkMagiskManager()
    }

    override fun isRunningOnEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.HARDWARE.contains("vbox86")
                || Build.BOARD.contains("goldfish")
                || (Build.HOST.startsWith("Build") && Build.USER.startsWith("android-build")))
    }

    override fun isAppBeingDebugged(context: Context): Boolean {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return isDebuggable || Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    override suspend fun isHookingDetected(): Boolean = withContext(Dispatchers.IO) {
        isXposedDetected() || isFridaDetected()
    }

    override fun isApkModified(context: Context): Boolean {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val apkFile = File(applicationInfo.sourceDir)
            // Just basic existence check here, could be expanded to CRC check if needed
            !apkFile.exists() || apkFile.length() == 0L
        } catch (e: Exception) {
            true
        }
    }

    private fun checkRootFiles(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/vendor/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinary(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }

    private fun checkMagiskManager(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("pm list packages com.topjohnwu.magisk")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()?.contains("com.topjohnwu.magisk") == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isXposedDetected(): Boolean {
        val xposedFiles = arrayOf(
            "/data/app/de.robv.android.xposed.installer",
            "/system/framework/XposedBridge.jar"
        )
        if (xposedFiles.any { File(it).exists() }) return true
        
        val classpath = System.getenv("CLASSPATH")
        return classpath != null && classpath.contains("XposedBridge")
    }

    private fun isFridaDetected(): Boolean {
        val fridaPorts = intArrayOf(27042, 27043)
        try {
            val process = Runtime.getRuntime().exec("netstat -an")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                for (port in fridaPorts) {
                    if (line!!.contains("LISTEN") && line!!.contains(":$port")) {
                        process.destroy()
                        return true
                    }
                }
            }
            process.destroy()
        } catch (e: Exception) {}

        try {
            val process = Runtime.getRuntime().exec("ps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("frida-server") || line!!.contains("frida-gadget")) {
                    process.destroy()
                    return true
                }
            }
            process.destroy()
        } catch (e: Exception) {}
        return false
    }
}
