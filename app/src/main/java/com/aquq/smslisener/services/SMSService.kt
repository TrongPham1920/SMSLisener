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
import android.os.PowerManager
import android.provider.MediaStore
import android.app.AlarmManager
import android.app.PendingIntent
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aquq.smslisener.MainActivity
import com.aquq.smslisener.api.ApiHelper
import com.aquq.smslisener.utils.PreferenceManager
import kotlinx.coroutines.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class SmsService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        scheduleKeepAlive()
        Log.d(TAG, "Service onCreate - đã khởi tạo wake lock và keep-alive")
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        Log.d(TAG, "Service onDestroy - đã release wake lock")
        // Auto-restart service
        restartService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "Service onTaskRemoved - app bị kill, đang restart...")
        restartService()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand được gọi")

        val sender = intent?.getStringExtra("sender")
        val message = intent?.getStringExtra("message")

        Log.d(TAG, "Sender: $sender, Message: $message")

        // Đảm bảo wake lock và keep-alive luôn active
        if (wakeLock == null || !wakeLock!!.isHeld) {
            acquireWakeLock()
        }
        scheduleKeepAlive()

        try {
            startForeground(NOTIFICATION_ID, buildNotification(sender, message))
            Log.d(TAG, "startForeground đã được gọi thành công")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi startForeground", e)
        }

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

    private fun buildNotification(sender: String?, message: String?): Notification {
        Log.d(TAG, "buildNotification được gọi - sender: $sender, message: $message")

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (sender != null && message != null)
            "SMS từ $sender: $message"
        else
            "Đang chạy nền để lắng nghe SMS"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Listener đang chạy")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        Log.d(TAG, "Notification đã được build: title=${notification.extras?.getCharSequence(Notification.EXTRA_TITLE)}")
        return notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SMS Listener Service",
                NotificationManager.IMPORTANCE_DEFAULT  // Tăng từ LOW lên DEFAULT để hiển thị
            ).apply {
                description = "Thông báo khi SMS Listener đang chạy nền"
                setShowBadge(true)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
            Log.d(TAG, "Notification channel đã được tạo: $CHANNEL_ID với importance DEFAULT")
        }
    }


    private fun getPhoneNumber(): String? {
        val subscriptionManager =
            getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val infoList = subscriptionManager.activeSubscriptionInfoList ?: return null

        return infoList.firstOrNull()?.number
    }

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

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmsService::WakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L) // 10 giờ
            }
            Log.d(TAG, "Wake lock đã được acquire")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock đã được release")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi release wake lock", e)
        }
    }

    private fun scheduleKeepAlive() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, SmsService::class.java)
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    this,
                    KEEP_ALIVE_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getService(
                    this,
                    KEEP_ALIVE_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val interval = 5 * 60 * 1000L // 5 phút

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval,
                    interval,
                    pendingIntent
                )
            }
            Log.d(TAG, "Keep-alive đã được schedule mỗi 5 phút")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi schedule keep-alive", e)
        }
    }

    private fun restartService() {
        try {
            val intent = Intent(this, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "Service đã được restart")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi restart service", e)
        }
    }

    companion object {
        private const val TAG = "SmsService"
        private const val CHANNEL_ID = "SmsServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val KEEP_ALIVE_REQUEST_CODE = 1001
        private val FILE_LOCK = Any()
    }
}
