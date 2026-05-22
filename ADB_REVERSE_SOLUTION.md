# ✅ SOLUTION: ADB Reverse Tunnel

## Problem Identified:
Your phone and computer are on the same WiFi but **cannot communicate** due to:
- AP Isolation on router
- Network firewall blocking device-to-device communication
- Guest network mode

**Proof:** `ping 192.168.31.175` from phone = 100% packet loss

---

## Solution Applied: ADB Reverse

Created USB tunnels that forward phone's localhost to computer's ports:

```bash
adb reverse tcp:5000 tcp:5000  # IAM Server
adb reverse tcp:8000 tcp:8000  # Health API
```

**Status:** ✅ Tunnels active

---

## Changes Made:

1. ✅ **ApiConfig.kt** - Changed back to `127.0.0.1`
2. ✅ **auth.ts** - Changed back to `http://127.0.0.1:5000`
3. ✅ **ADB tunnels** - Created and verified
4. ✅ **App rebuilt** - Installed on device

---

## How It Works:

```
Phone App (127.0.0.1:5000)
    ↓ (via USB cable)
ADB Reverse Tunnel
    ↓
Computer (localhost:5000)
    ↓
IAM Server
```

---

## Testing Steps:

1. **Make sure USB cable is connected**
2. **Verify tunnels are active:**
   ```bash
   adb reverse --list
   ```
   Should show:
   ```
   tcp:5000 tcp:5000
   tcp:8000 tcp:8000
   ```

3. **Open PHIA app on phone**
4. **Click "Sign In"**
5. **Browser should open with IAM page** ✅

---

## Important Notes:

⚠️ **Tunnels reset when:**
- Phone is disconnected from USB
- Phone is rebooted
- ADB server restarts

**To recreate tunnels:**
```bash
adb reverse tcp:5000 tcp:5000
adb reverse tcp:8000 tcp:8000
```

---

## Alternative Solutions (If ADB Doesn't Work):

### Option 1: USB Tethering
1. Enable USB tethering on phone
2. Find new IP: `ip addr show`
3. Update ApiConfig.kt with new IP

### Option 2: Fix Router Settings
1. Access router admin panel
2. Disable "AP Isolation" or "Client Isolation"
3. Use network IP (192.168.31.175)

### Option 3: Use Mobile Hotspot
1. Enable hotspot on phone
2. Connect computer to phone's hotspot
3. Use phone's hotspot IP

---

## Current Configuration:

```
IAM Server:    http://127.0.0.1:5000 (via ADB tunnel)
Health API:    http://127.0.0.1:8000 (via ADB tunnel)
OAuth Client:  phia-mobile-app (public, PKCE enabled)
Redirect URI:  phia://auth/callback
```

---

**The app is ready to test!** 🚀

**IMPORTANT:** Keep the USB cable connected while testing.

Try clicking "Sign In" now!
