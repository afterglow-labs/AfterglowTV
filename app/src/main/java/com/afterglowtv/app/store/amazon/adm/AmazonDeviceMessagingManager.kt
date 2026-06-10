package com.afterglowtv.app.store.amazon.adm

import android.content.Context
import android.util.Log
import com.afterglowtv.app.BuildConfig

object AmazonDeviceMessagingManager {
    private const val TAG = "AfterglowADM"
    private const val PREFS_NAME = "afterglow_adm"
    private const val KEY_REGISTRATION_ID = "registration_id"
    private const val KEY_LAST_ERROR = "last_error"

    fun registerIfAvailable(context: Context) {
        if (!BuildConfig.ENABLE_AMAZON_DEVICE_MESSAGING) return

        val applicationContext = context.applicationContext
        runCatching {
            val admClass = Class.forName("com.amazon.device.messaging.ADM")
            val adm = admClass.getConstructor(Context::class.java).newInstance(applicationContext)
            val isSupported = admClass.getMethod("isSupported").invoke(adm) as? Boolean == true
            if (!isSupported) {
                Log.i(TAG, "Amazon Device Messaging is not supported on this device")
                return
            }

            val existingRegistrationId = admClass.getMethod("getRegistrationId").invoke(adm) as? String
            if (!existingRegistrationId.isNullOrBlank()) {
                persistRegistrationId(applicationContext, existingRegistrationId)
                Log.i(TAG, "ADM registration already available")
                return
            }

            Log.i(TAG, "Requesting ADM registration")
            admClass.getMethod("startRegister").invoke(adm)
        }.onFailure { error ->
            Log.w(TAG, "Unable to initialize Amazon Device Messaging", error)
        }
    }

    fun registrationId(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REGISTRATION_ID, null)

    internal fun persistRegistrationId(context: Context, registrationId: String) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REGISTRATION_ID, registrationId)
            .remove(KEY_LAST_ERROR)
            .apply()
        Log.i(TAG, "ADM registered: ${registrationId.take(8)}...")
    }

    internal fun clearRegistrationId(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REGISTRATION_ID)
            .apply()
        Log.i(TAG, "ADM registration cleared")
    }

    internal fun persistRegistrationError(context: Context, errorId: String) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_ERROR, errorId)
            .apply()
        Log.w(TAG, "ADM registration failed: $errorId")
    }
}
