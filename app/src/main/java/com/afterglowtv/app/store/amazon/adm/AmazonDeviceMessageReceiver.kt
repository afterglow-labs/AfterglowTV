package com.afterglowtv.app.store.amazon.adm

import com.amazon.device.messaging.ADMMessageReceiver

class AmazonDeviceMessageReceiver : ADMMessageReceiver(AmazonDeviceMessageHandler::class.java)
