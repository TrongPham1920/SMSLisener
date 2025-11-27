package com.aquq.smslisener.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.aquq.smslisener.MainActivity
import com.aquq.smslisener.api.ApiHelper
import com.aquq.smslisener.utils.PreferenceManager
import kotlinx.coroutines.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SmsService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val sender = intent?.getStringExtra("sender")
        val message = intent?.getStringExtra("message")

        // Gọi 1 lần startForeground với notification thực sự
        startForeground(NOTIFICATION_ID, buildNotification(sender, message))

        if (sender.isNullOrBlank() || message.isNullOrBlank()) {
            return START_STICKY
        }

        val receiver = getPhoneNumber() ?: "Unknown"

        CoroutineScope(Dispatchers.IO).launch {
            synchronized(FILE_LOCK) {
                saveSmsToCsv(sender, receiver, message)
            }

            val domain = PreferenceManager.getApiDomain(this@SmsService)
            val bodyFormat = PreferenceManager.getBodyFormat(this@SmsService)

            if (domain.isNotEmpty() && bodyFormat.isNotEmpty()) {
                ApiHelper.sendSmsData(domain, bodyFormat, sender, message, receiver)
            }
        }

        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null

    /** ============================
     *  Notification Foreground
     *  ============================ */
    private fun buildNotification(sender: String?, message: String?): Notification {

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (sender != null && message != null)
            "SMS từ $sender: $message"
        else
            "Đang chạy nền để lắng nghe SMS"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Listener is running")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SMS Listener Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }


    /** ============================
     *  Lấy số điện thoại SIM
     *  ============================ */
    private fun getPhoneNumber(): String? {
        val subscriptionManager =
            getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val infoList = subscriptionManager.activeSubscriptionInfoList ?: return null

        return infoList.firstOrNull()?.number
    }

    /** ============================
     *  Ghi CSV
     *  ============================ */
    private fun saveSmsToCsv(sender: String?, receiver: String?, content: String?) {
        val resolver = applicationContext.contentResolver

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else
                MediaStore.Files.getContentUri("external")

        val fileName = "sms_log.csv"

        val existingFileUri: Uri? = resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(fileName),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                ContentUris.withAppendedId(collection, id)
            } else null
        }

        val fileUri = existingFileUri ?: resolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }
        )

        fileUri?.let {
            try {
                val mode = if (existingFileUri == null) "w" else "wa"
                resolver.openOutputStream(it, mode)?.use { stream ->
                    val writer = stream.bufferedWriter()

                    if (existingFileUri == null) {
                        writer.write("ROWID,MessageDate,Sender,Receiver,Content\n")
                    }

                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                    val rowId = System.currentTimeMillis()

                    writer.write("$rowId,$date,$sender,$receiver,$content\n")
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing CSV", e)
            }
        }
    }

    private fun getExistingRowCount(uri: Uri): Int {
        val resolver = contentResolver
        return resolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readLines().size - 1
        } ?: 0
    }

    companion object {
        private const val TAG = "SmsService"
        private const val CHANNEL_ID = "SmsServiceChannel"
        private const val NOTIFICATION_ID = 1
        private val FILE_LOCK = Any()
    }
}
