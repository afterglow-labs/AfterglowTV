package com.afterglowtv.app.store.amazon.adm

import android.content.Intent
import android.util.Log
import com.amazon.device.messaging.ADMMessageHandlerBase

class AmazonDeviceMessageHandler :
    ADMMessageHandlerBase(AmazonDeviceMessageHandler::class.java.name) {

    override fun onMessage(intent: Intent) {
        AmazonDeviceMessagePayload.handle(this, intent)
    }

    override fun onRegistered(registrationId: String) {
        AmazonDeviceMessagingManager.persistRegistrationId(this, registrationId)
    }

    override fun onUnregistered(registrationId: String) {
        AmazonDeviceMessagingManager.clearRegistrationId(this)
    }

    override fun onRegistrationError(errorId: String) {
        AmazonDeviceMessagingManager.persistRegistrationError(this, errorId)
    }

    override fun onSubscribe(topic: String) {
        Log.i(TAG, "ADM subscribed: $topic")
    }

    override fun onSubscribeError(topic: String, errorId: String) {
        Log.w(TAG, "ADM subscribe failed: topic=$topic error=$errorId")
    }

    override fun onUnsubscribe(topic: String) {
        Log.i(TAG, "ADM unsubscribed: $topic")
    }

    override fun onUnsubscribeError(topic: String, errorId: String) {
        Log.w(TAG, "ADM unsubscribe failed: topic=$topic error=$errorId")
    }

    private companion object {
        private const val TAG = "AfterglowADM"
    }
}
