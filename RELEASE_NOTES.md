# 📦 SMS Listener App - Release APK

## ✅ Build Successful!

**Version:** 1.0
**Build Date:** November 14, 2025
**Build Type:** Release (Signed with debug keystore)

---

## 📍 APK Locations

### 1. Release APK (Original)
```
app/build/outputs/apk/release/app-release.apk
```

### 2. Release APK (Copy)
```
SMSListener-v1.0-release.apk (in project root)
```

---

## 📊 Build Information

```
✅ BUILD SUCCESSFUL in 12s
✅ 46 actionable tasks: 45 executed, 1 up-to-date
✅ Signed with debug keystore
✅ Ready to install
```

### Build Configuration:
- **Application ID:** com.aquq.smslisener
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Version Code:** 1
- **Version Name:** 1.0
- **Minify Enabled:** No (for easier debugging)

---

## 🚀 Installation

### Option 1: ADB Install
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

Or from project root:
```bash
adb install SMSListener-v1.0-release.apk
```

### Option 2: Manual Install
1. Transfer APK to phone
2. Open APK file
3. Allow "Install from Unknown Sources" if needed
4. Click Install

### Option 3: Direct Install (if phone connected)
```bash
cd /Users/huynguyen/Documents/project_code/Android/SMSLisener
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🔐 Signing Information

**⚠️ Important:** This APK is signed with **debug keystore** for testing purposes.

### Current Signing:
- **Keystore:** `~/.android/debug.keystore`
- **Store Password:** android
- **Key Alias:** androiddebugkey
- **Key Password:** android

### For Production Release:

If you want to publish to Play Store or distribute publicly, you need to:

1. **Create a production keystore:**
```bash
keytool -genkey -v -keystore release.keystore -alias release-key \
  -keyalg RSA -keysize 2048 -validity 10000
```

2. **Update build.gradle.kts:**
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/release.keystore")
        storePassword = "your-password"
        keyAlias = "release-key"
        keyPassword = "your-key-password"
    }
}
```

3. **Rebuild:**
```bash
./gradlew clean assembleRelease
```

---

## ✨ Features in This Release

### Core Features:
- ✅ SMS listening and auto-forwarding
- ✅ Jetpack Compose UI with Material Design 3
- ✅ Runtime permissions handling
- ✅ Configurable API endpoint
- ✅ Configurable JSON body format
- ✅ Placeholder replacement: {sender}, {message}, {receiver}
- ✅ HTTP & HTTPS support
- ✅ Network security config included
- ✅ Default configuration preset

### Technical:
- ✅ OkHttp for networking
- ✅ SharedPreferences for config storage
- ✅ BroadcastReceiver for SMS
- ✅ Service for background processing
- ✅ Logging for debugging

---

## 📱 First Launch Guide

1. **Install APK**
2. **Open App**
3. **Grant Permissions:**
   - Receive SMS
   - Read SMS
   - Read Phone State
   - Read Phone Numbers
4. **Configure (or use defaults):**
   - API Domain: `https://webhook.site/your-unique-id`
   - Body Format: JSON with placeholders
5. **Save Configuration**
6. **Test:** Send SMS to the phone → Check webhook

---

## 🧪 Testing

### Quick Test:
```bash
# Install
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch
adb shell am start -n com.aquq.smslisener/.MainActivity

# Send test SMS (from another phone or service)
# Check logs
adb logcat -s ApiHelper:D SmsReceiver:D SmsService:D
```

### Webhook Testing:
1. Open https://webhook.site
2. Copy your unique URL
3. Paste into app as API Domain
4. Send SMS to your phone
5. Refresh webhook.site to see the POST request

---

## 📝 APK Verification

To verify the APK signature:
```bash
# Check signing info
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# View certificate
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

---

## 🔄 Rebuild Instructions

### Clean Build:
```bash
./gradlew clean assembleRelease
```

### Quick Build (incremental):
```bash
./gradlew assembleRelease
```

### Build All Variants:
```bash
./gradlew build
```

### Debug Build:
```bash
./gradlew assembleDebug
```

---

## 📦 Release Checklist

For future releases:

- [ ] Update version code in `build.gradle.kts`
- [ ] Update version name
- [ ] Test all features
- [ ] Run lint checks
- [ ] Create production keystore
- [ ] Sign with production key
- [ ] Test on multiple devices
- [ ] Create release notes
- [ ] Tag git commit
- [ ] Archive APK

---

## 🐛 Troubleshooting

### Installation Failed?
```bash
# Uninstall old version first
adb uninstall com.aquq.smslisener

# Then install
adb install app/build/outputs/apk/release/app-release.apk
```

### Signature Mismatch?
If you have an older version with different signature:
```bash
# Force reinstall
adb install -r -d app/build/outputs/apk/release/app-release.apk
```

### App Not Working?
1. Check permissions granted
2. Check Logcat for errors
3. Verify API endpoint is correct
4. Test network connectivity

---

## 📊 Build Outputs

```
app/build/outputs/
├── apk/
│   ├── debug/
│   │   └── app-debug.apk
│   └── release/
│       ├── app-release.apk ✅ (SIGNED)
│       └── output-metadata.json
└── logs/
    └── manifest-merger-release-report.txt
```

---

## 🎉 Success!

Your SMS Listener app is now built and ready to deploy!

**Next Steps:**
1. Install on your Android device
2. Grant permissions
3. Configure API endpoint
4. Start receiving and forwarding SMS

For support or issues, check the logs:
```bash
adb logcat | grep -E "SmsReceiver|SmsService|ApiHelper"
```

---

**Built with ❤️ using Jetpack Compose and Kotlin**

