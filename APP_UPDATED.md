# ✅ Android App Updated & Installed

## What Was Done:

1. ✅ **Created centralized API configuration** (`ApiConfig.kt`)
2. ✅ **Updated all hardcoded URLs** from `127.0.0.1` to `192.168.31.175`
3. ✅ **Rebuilt the app** with clean build
4. ✅ **Installed on your device** (motorola edge 60 pro)

---

## 🧪 Testing Steps:

### 1. Make Sure IAM Server is Running
```bash
cd /home/mi/Desktop/AI_projects/nextjs-iam
pnpm dev
```

You should see:
```
- Local:    http://localhost:5000
- Network:  http://0.0.0.0:5000
```

### 2. Test from Your Phone's Browser First
Open Chrome on your phone and visit:
```
http://192.168.31.175:5000
```

**Expected:** You should see the IAM login page ✅

**If it doesn't load:**
- Check your phone is on the same WiFi as your computer
- Check firewall: `sudo ufw allow 5000`

### 3. Test the PHIA App
1. Open PHIA app on your phone
2. Click "Sign In" or "Create Account"
3. Browser should open with the IAM login page
4. Complete authentication
5. Should redirect back to app

---

## 🔍 If Still Not Working:

### Check Android Logs:
```bash
adb logcat | grep -E "(OAuth|ApiClient|MainActivity)"
```

Look for:
- ✅ "Generated Auth URL with PKCE: http://192.168.31.175:5000..."
- ❌ Any connection errors

### Verify Configuration:
```bash
# Check the compiled APK has the right URL
cd /home/mi/Desktop/AI_projects/PHIA_app
./gradlew :app:dependencies | grep -i "192.168"
```

### Test Network Connectivity:
From your phone's browser, test both servers:
- IAM: `http://192.168.31.175:5000/health`
- Health API: `http://192.168.31.175:8000/health`

---

## 📱 What Should Happen Now:

### Before (❌):
```
Click "Sign In" → Browser opens → "This site can't be reached"
```

### After (✅):
```
Click "Sign In" → Browser opens → IAM login page loads
→ Enter credentials → Authenticate → Redirect to app → Success!
```

---

## ⚠️ Common Issues:

### 1. "Site can't be reached" still appears
**Solution:** 
- Verify both devices on same WiFi
- Restart IAM server
- Check firewall: `sudo ufw status`

### 2. Browser opens but shows 404
**Solution:**
- IAM server might not be running
- Check: `curl http://192.168.31.175:5000/health`

### 3. "Invalid client" error
**Solution:**
- OAuth client not configured correctly
- Run: `cd /home/mi/Desktop/AI_projects/nextjs-iam && pnpm oauth:test`

---

## 🎯 Next Steps:

1. **Test the app now** - Click "Sign In" button
2. **Check if browser opens** with IAM page
3. **If it works:** Complete authentication flow
4. **If it doesn't:** Share the error message or screenshot

---

**The app has been updated and installed on your device!** 🎉

Try clicking "Sign In" now and let me know what happens.
