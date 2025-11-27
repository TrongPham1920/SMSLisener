package com.aquq.smslisener.services

import android.Manifest
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
import com.aquq.smslisener.api.ApiHelper
import com.aquq.smslisener.utils.PreferenceManager
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SmsService : Service() {
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
        val sender = intent.getStringExtra("sender") ?: ""
        val message = intent.getStringExtra("message") ?: ""
        Log.e(TAG, "Show listen sms $sender - $message")

        val receiver = getPhoneNumber() ?: "Unknown"

        saveSmsToCsv(sender, receiver, message)

        // Call API with configured domain and body format
        val domain = PreferenceManager.getApiDomain(this)
        val bodyFormat = PreferenceManager.getBodyFormat(this)

        if (domain.isNotEmpty() && bodyFormat.isNotEmpty()) {
            ApiHelper.sendSmsData(domain, bodyFormat, sender, message, receiver)
        } else {
            Log.w(TAG, "API domain or body format not configured")
        }

        return START_NOT_STICKY
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
    }
}
