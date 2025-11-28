package com.aquq.smslisener

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aquq.smslisener.ui.theme.SMSLisenerTheme
import com.aquq.smslisener.utils.PreferenceManager
import com.aquq.smslisener.services.SmsService

class MainActivity : ComponentActivity() {

    private val permissions = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.READ_PHONE_NUMBERS
    )

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        onPermissionResult?.invoke(allGranted)
    }

    // Request quyền POST_NOTIFICATIONS cho Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "POST_NOTIFICATIONS đã được cấp")
        } else {
            Log.w("MainActivity", "POST_NOTIFICATIONS bị từ chối - notification sẽ không hiển thị")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Khi mở app → yêu cầu tắt tối ưu pin ngay lập tức
        disableBatteryOptimizations()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SMSLisenerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onRequestPermissions = { callback ->
                            onPermissionResult = callback
                            requestPermissionLauncher.launch(permissions)
                        },
                        checkPermissions = {
                            permissions.all { permission ->
                                ContextCompat.checkSelfPermission(this, permission) ==
                                        PackageManager.PERMISSION_GRANTED
                            }
                        }
                    )
                }
            }
        }
    }


    //BẮT TẮT CHẾ ĐỘ TỐI ƯU PIN
    private fun disableBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        val packageName = packageName

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}

@Composable
fun MainScreen(
    onRequestPermissions: ((Boolean) -> Unit) -> Unit,
    checkPermissions: () -> Boolean
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkPermissions()) }
    var apiDomain by remember { mutableStateOf(PreferenceManager.getApiDomain(context)) }
    var bodyFormat by remember { mutableStateOf(PreferenceManager.getBodyFormat(context)) }

    //Auto start foreground service
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            Log.d("MainActivity", "Đã có quyền, đang start foreground service...")
            val serviceIntent = Intent(context, SmsService::class.java)
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
                Log.d("MainActivity", "Foreground service đã được start")
            } catch (e: Exception) {
                Log.e("MainActivity", "Lỗi khi start foreground service", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Permission Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermissions)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Trạng thái quyền",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasPermissions) "✓ Đã cấp quyền" else "✗ Chưa cấp quyền",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Button
        Button(
            onClick = {
                onRequestPermissions { granted ->
                    hasPermissions = granted
                    if (granted) {
                        Toast.makeText(context, "Đã cấp đủ quyền!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Cần cấp đủ quyền để app hoạt động", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasPermissions) "Kiểm tra lại quyền" else "Cấp quyền")
        }

        // Configuration Section - Only show when permissions are granted
        if (hasPermissions) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cấu hình API",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API Domain Input
            OutlinedTextField(
                value = apiDomain,
                onValueChange = { newValue -> apiDomain = newValue },
                label = { Text("API Domain") },
                placeholder = { Text("https://api.example.com/sms") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Format Input
            OutlinedTextField(
                value = bodyFormat,
                onValueChange = { newValue -> bodyFormat = newValue },
                label = { Text("Body Format (JSON)") },
                placeholder = { Text("{\"sender\":\"{sender}\",\"message\":\"{message}\",\"receiver\":\"{receiver}\"}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                minLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Helper Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "💡 Hướng dẫn:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• {sender} - Số điện thoại người gửi\n" +
                                "• {message} - Nội dung tin nhắn\n" +
                                "• {receiver} - Số điện thoại máy này",
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (apiDomain.isNotEmpty() && bodyFormat.isNotEmpty()) {
                        PreferenceManager.saveApiDomain(context, apiDomain)
                        PreferenceManager.saveBodyFormat(context, bodyFormat)
                        Toast.makeText(context, "Đã lưu cấu hình!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Lưu cấu hình", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Example Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📝 Ví dụ Body Format:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "{\n" +
                                "  \"sender\": \"{sender}\",\n" +
                                "  \"message\": \"{message}\",\n" +
                                "  \"receiver\": \"{receiver}\",\n" +
                                "  \"timestamp\": \"auto\"\n" +
                                "}",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
