# 🔧 Android App Configuration Fix

## Problem Solved
**Error:** "This site can't be reached" (ERR_ADDRESS_UNREACHABLE)

**Cause:** App was using `127.0.0.1` (localhost) which doesn't work on physical devices.

---

## ✅ Solution Applied

### 1. Created Centralized Configuration
**File:** `app/src/main/java/com/healthpipeline/config/ApiConfig.kt`

All API URLs are now in ONE place - easy to update!

### 2. Updated Files
- ✅ `ApiClient.kt` - Health API calls
- ✅ `AuthViewModel.kt` - OAuth token exchange
- ✅ `MainActivity.kt` - OAuth authorization

---

## 🚀 How to Use

### For Physical Device (Current Setup):
```kotlin
// In ApiConfig.kt
private const val HOST = "192.168.31.175"  // ✅ Your computer's IP
```

### For Android Emulator:
```kotlin
// In ApiConfig.kt
private const val HOST = "10.0.2.2"  // Maps to host's localhost
```

### For Production:
```kotlin
// In ApiConfig.kt
private const val HOST = "api.yourapp.com"
```

---

## 📱 Testing Steps

1. **Make sure both servers are running:**
   ```bash
   # Terminal 1: IAM Server
   cd /home/mi/Desktop/AI_projects/nextjs-iam
   pnpm dev
   
   # Terminal 2: Health API
   cd /home/mi/Desktop/AI_projects/PHIA_app
   docker-compose up
   ```

2. **Rebuild the Android app:**
   ```bash
   cd /home/mi/Desktop/AI_projects/PHIA_app
   ./gradlew clean assembleDebug
   ./gradlew installDebug
   ```

3. **Test on your phone:**
   - Open PHIA app
   - Click "Sign In" or "Create Account"
   - Browser should open successfully ✅
   - Complete authentication
   - Should redirect back to app ✅

---

## 🔍 How to Find Your Computer's IP

### Linux:
```bash
hostname -I | awk '{print $1}'
```

### Windows:
```cmd
ipconfig
```
Look for "IPv4 Address" under your active network adapter.

### Mac:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

---

## ⚠️ Important Notes

1. **Your phone and computer must be on the SAME WiFi network**
2. **Firewall must allow connections on ports 5000 and 8000**
3. **IP address may change if you reconnect to WiFi**

---

## 🔥 Quick Fix if IP Changes

Just update ONE file:
```kotlin
// app/src/main/java/com/healthpipeline/config/ApiConfig.kt
private const val HOST = "YOUR_NEW_IP"  // Update this line only
```

Then rebuild and reinstall the app.

---

## 🆘 Troubleshooting

### Still getting "can't be reached"?

1. **Check servers are running:**
   ```bash
   curl http://192.168.31.175:5000/health
   curl http://192.168.31.175:8000/health
   ```

2. **Check firewall:**
   ```bash
   sudo ufw allow 5000
   sudo ufw allow 8000
   ```

3. **Verify IP is correct:**
   ```bash
   hostname -I
   ```

4. **Check phone is on same WiFi:**
   - Phone WiFi settings → Should show same network name

---

## ✅ Current Configuration

```
Health API:  http://192.168.31.175:8000/
IAM Server:  http://192.168.31.175:5000/
OAuth:       http://192.168.31.175:5000/api/auth/oauth2/authorize
Client ID:   phia-mobile-app
Redirect:    phia://auth/callback
```

---

**Your app should now work!** 🎉

If you still have issues, check the Android Logcat for detailed error messages.
