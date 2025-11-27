package com.aquq.smslisener.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import androidx.core.app.NotificationManagerCompat
import com.aquq.smslisener.api.ApiHelper
import com.aquq.smslisener.utils.PreferenceManager
import com.aquq.smslisener.MainActivity
import com.aquq.smslisener.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SmsService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    private fun getPhoneNumber(): String? {
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfoList = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            listOf()
        } else {
            subscriptionManager.activeSubscriptionInfoList
        }
        return if (subscriptionInfoList.isNotEmpty()) {
            @Suppress("DEPRECATION")
            subscriptionInfoList[0].number
        } else {
            null
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val sender = intent.getStringExtra("sender")
        val message = intent.getStringExtra("message")

        Log.d(TAG, "Foreground service ping. sender=$sender message=$message")
        startForeground(NOTIFICATION_ID, buildNotification(sender, message))

        if (sender.isNullOrBlank() || message.isNullOrBlank()) {
            return START_STICKY
        }

        val receiver = getPhoneNumber() ?: "Unknown"
        saveSmsToCsv(sender, receiver, message)

        val domain = PreferenceManager.getApiDomain(this)
        val bodyFormat = PreferenceManager.getBodyFormat(this)

        if (domain.isNotEmpty() && bodyFormat.isNotEmpty()) {
            ApiHelper.sendSmsData(domain, bodyFormat, sender, message, receiver)
        } else {
            Log.w(TAG, "API domain or body format not configured")
        }

        return START_STICKY
    }

    private fun buildNotification(sender: String?, message: String?): Notification {
        val contentText = if (!sender.isNullOrBlank() && !message.isNullOrBlank()) {
            "Tin mới từ $sender"
        } else {
            getString(R.string.notification_content_idle)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (!message.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    private fun saveSmsToCsv(sender: String?, receiver: String?, content: String?) {
        val resolver = applicationContext.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf("sms_log.csv")

        val existingFileUri: Uri? = resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                ContentUris.withAppendedId(collection, id)
            } else {
                null
            }
        }

        val uri = existingFileUri ?: resolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "sms_log.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        })

        uri?.let {
            try {
                resolver.openOutputStream(it, if (existingFileUri == null) "wt" else "wa")?.use { outputStream ->
                    val writer = outputStream.bufferedWriter()

                    if (existingFileUri == null) {
                        // Write header if the file is new
                        writer.write("ROWID,MessageDate,Sender,Receiver,Content\n")
                    }

                    // Format the message date
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val messageDateFormatted = sdf.format(Date())

                    // Increment the ROWID (This is a simple increment, replace with actual logic if needed)
                    val rowId = (existingFileUri?.let { getExistingRowCount(it) } ?: 0) + 1

                    writer.write("$rowId,$messageDateFormatted,$sender,$receiver,$content\n")
                    writer.flush()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error writing to CSV file", e)
            }
        } ?: run {
            Log.e(TAG, "Failed to create or open file")
        }
    }

    private fun getExistingRowCount(uri: Uri): Int {
        val resolver = applicationContext.contentResolver
        return resolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().lineSequence().count { it.isNotBlank() } - 1 // Subtract 1 for header line
        } ?: 0
    }

    companion object {
        private const val TAG = "SmsService"
        private const val CHANNEL_ID = "sms_listener_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
