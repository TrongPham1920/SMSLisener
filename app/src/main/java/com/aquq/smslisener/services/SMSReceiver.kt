package com.aquq.smslisener.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle["pdus"] as Array<Any>?
            if (pdus != null) {
                for (pdu in pdus) {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = smsMessage.displayOriginatingAddress
                    val message = smsMessage.displayMessageBody
                    Log.d("SmsReceiver", "Sender: $sender, Message: $message")

                    // Gửi tin nhắn đến Service để lưu
                    val serviceIntent = Intent(
                        context,
                        SmsService::class.java
                    )
                    serviceIntent.putExtra("sender", sender)
                    serviceIntent.putExtra("message", message)
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        }
    }
}