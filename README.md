# 📱 SMS Listener App

App Android lắng nghe tin nhắn SMS và tự động gửi đến API endpoint theo cấu hình.

## ✨ Tính năng

1. **Lắng nghe tin nhắn SMS** tự động
2. **UI Jetpack Compose** hiện đại với Material Design 3
3. **Cấp quyền dễ dàng** - Chỉ cần 1 nút bấm
4. **Cấu hình API linh hoạt**:
   - Tùy chỉnh domain/endpoint
   - Tùy chỉnh format body JSON
   - Sử dụng placeholders động
5. **Tự động gửi API** khi nhận tin nhắn

## 🚀 Cài đặt

### Bước 1: Build APK

```bash
cd /Users/huynguyen/Documents/project_code/Android/SMSLisener
./gradlew assembleDebug
```

APK sẽ được tạo tại: `app/build/outputs/apk/debug/app-debug.apk`

### Bước 2: Cài đặt lên thiết bị

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📖 Hướng dẫn sử dụng

### 1. Cấp quyền

Khi mở app lần đầu:
1. Nhấn nút **"Cấp quyền"**
2. Cho phép tất cả quyền:
   - 📩 Nhận SMS (RECEIVE_SMS)
   - 📖 Đọc SMS (READ_SMS)
   - 📱 Đọc trạng thái điện thoại (READ_PHONE_STATE)
   - 📞 Đọc số điện thoại (READ_PHONE_NUMBERS)

### 2. Cấu hình API

Sau khi cấp quyền, form cấu hình sẽ hiện ra:

#### **API Domain**
Nhập URL endpoint của bạn:
```
https://api.example.com/webhook/sms
```

#### **Body Format**
Nhập định dạng JSON với placeholders:
```json
{
  "sender": "{sender}",
  "message": "{message}",
  "receiver": "{receiver}",
  "timestamp": "auto"
}
```

#### **Placeholders hỗ trợ:**
- `{sender}` → Số điện thoại người gửi
- `{message}` → Nội dung tin nhắn
- `{receiver}` → Số điện thoại máy này

### 3. Lưu cấu hình

Nhấn nút **"Lưu cấu hình"** để lưu thiết lập.

### 4. Nhận tin nhắn

Khi có tin nhắn SMS đến:
- App tự động nhận và xử lý
- Thay thế placeholders bằng giá trị thực
- Gửi POST request đến API endpoint
- Log kết quả trong Logcat

## 📝 Ví dụ

### Input:
**API Domain:**
```
https://api.myserver.com/sms/webhook
```

**Body Format:**
```json
{
  "from": "{sender}",
  "content": "{message}",
  "to": "{receiver}",
  "device": "Samsung Galaxy"
}
```

### Khi nhận SMS:
- **Sender:** +84901234567
- **Message:** "Ma xac nhan cua ban la 123456"
- **Receiver:** +84987654321

### Request gửi đi:
```http
POST https://api.myserver.com/sms/webhook
Content-Type: application/json

{
  "from": "+84901234567",
  "content": "Ma xac nhan cua ban la 123456",
  "to": "+84987654321",
  "device": "Samsung Galaxy"
}
```

## 🔧 Cấu trúc Project

```
app/src/main/java/com/aquq/smslisener/
├── MainActivity.kt              # UI chính với Compose
├── api/
│   └── ApiHelper.kt            # Xử lý HTTP requests
├── services/
│   ├── SMSReceiver.kt          # BroadcastReceiver lắng nghe SMS
│   └── SMSService.kt           # Service xử lý SMS và call API
├── utils/
│   ├── PreferenceManager.kt    # Lưu/đọc cấu hình
│   └── PermissionUtils.kt      # Utilities cho permissions
└── ui/theme/
    └── Theme.kt                # Compose theme
```

## 🐛 Debug

### Xem logs:
```bash
adb logcat -s SmsReceiver:D SmsService:D ApiHelper:D
```

### Log tags:
- `SmsReceiver` - Logs khi nhận SMS
- `SmsService` - Logs xử lý service
- `ApiHelper` - Logs HTTP requests/responses

### Clear app data:
```bash
adb shell pm clear com.aquq.smslisener
```

## 📦 Dependencies

- **Jetpack Compose** - Modern UI toolkit
- **Material3** - Material Design components
- **OkHttp** - HTTP client
- **AndroidX Core KTX** - Kotlin extensions

## ⚙️ Requirements

- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 34

## 🔐 Permissions

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS"/>
```

## 📱 Tính năng bổ sung

App cũng tự động lưu tin nhắn vào file CSV tại:
```
/Documents/sms_log.csv
```

Format CSV:
```
ROWID,MessageDate,Sender,Receiver,Content
1,2024-11-13 23:30:00,+84901234567,+84987654321,Ma xac nhan...
```

## ⚠️ Lưu ý

1. **Cần cấp đủ quyền** để app hoạt động
2. **API domain** phải bắt đầu bằng `http://` hoặc `https://`
3. **Body format** phải là JSON hợp lệ
4. **Test trên thiết bị thật** để nhận SMS
5. App chạy ngầm, không cần mở để nhận SMS

## 🎨 Screenshots

### Màn hình chính - Chưa cấp quyền
- Hiển thị status quyền
- Nút cấp quyền

### Màn hình chính - Đã cấp quyền
- Form cấu hình API domain
- Form cấu hình body format
- Hướng dẫn sử dụng placeholders
- Ví dụ cấu hình
- Nút lưu cấu hình

## 🛠 Troubleshooting

### App không nhận SMS?
1. Kiểm tra đã cấp đủ quyền chưa
2. Kiểm tra app không bị tối ưu pin
3. Xem logs để debug

### API không được gọi?
1. Kiểm tra đã lưu cấu hình chưa
2. Kiểm tra domain và format có đúng không
3. Xem logs ApiHelper để debug

### Build failed?
```bash
./gradlew clean
./gradlew assembleDebug
```

## 📄 License

MIT License - Free to use and modify

## 👨‍💻 Author

Created with ❤️ for SMS automation

