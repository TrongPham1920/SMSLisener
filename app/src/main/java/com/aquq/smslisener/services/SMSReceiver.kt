package com.aquq.smslisener.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.annotation.RequiresApi

class SmsReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle["pdus"] as Array<Any>?
            if (pdus != null) {
                for (pdu in pdus) {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = smsMessage.displayOriginatingAddress
                    val message = smsMessage.displayMessageBody
                    val receiver = smsMessage
                    Log.d("SmsReceiver", "Sender: $sender, Message: $message")

                    // Gửi tin nhắn đến Service để lưu
                    val svc = Intent(context, SmsService::class.java).apply {
                        putExtra("sender", sender)
                        putExtra("message", message)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(svc)
                    } else {
                        context.startService(svc)
                    }

                }
            }
        }
    }
}