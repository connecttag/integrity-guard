package org.connecttag.core.integrity.guard

import android.content.Context

public interface IIntegrityGuard {
    public fun isAppSignatureValid(context: Context, expectedSignature: String): Boolean
    public suspend fun isDeviceRooted(): Boolean
    public fun isRunningOnEmulator(): Boolean
    public fun isAppBeingDebugged(context: Context): Boolean
    public suspend fun isHookingDetected(): Boolean
    public fun isApkModified(context: Context): Boolean
}
