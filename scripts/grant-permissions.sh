#!/bin/bash
# Grant all common runtime permissions for a specified Android package via ADB.
# Usage: ./grant-permissions.sh [package_name]
# If no package name is provided, it auto-detects the current foreground app.

set -e

# Resolve package name
if [ -n "$1" ]; then
    PKG="$1"
else
    PKG=$(adb shell dumpsys activity activities 2>/dev/null | grep "mResumedActivity" | head -1 | sed 's/.*u0 \([^/]*\)\/.*/\1/')
    if [ -z "$PKG" ]; then
        echo "Error: cannot detect foreground app. Please specify package name."
        echo "Usage: $0 <package_name>"
        exit 1
    fi
fi

echo "Granting permissions for: $PKG"
echo "---"

PERMISSIONS=(
    android.permission.ACCESS_FINE_LOCATION
    android.permission.ACCESS_COARSE_LOCATION
    android.permission.ACCESS_BACKGROUND_LOCATION
    android.permission.CAMERA
    android.permission.RECORD_AUDIO
    android.permission.READ_CONTACTS
    android.permission.CALL_PHONE
    android.permission.READ_PHONE_STATE
    android.permission.WRITE_CALL_LOG
    android.permission.READ_EXTERNAL_STORAGE
    android.permission.WRITE_EXTERNAL_STORAGE
    android.permission.READ_MEDIA_IMAGES
    android.permission.READ_MEDIA_VIDEO
    android.permission.READ_MEDIA_AUDIO
    android.permission.READ_MEDIA_VISUAL_USER_SELECTED
    android.permission.READ_CALENDAR
    android.permission.WRITE_CALENDAR
    android.permission.BLUETOOTH_CONNECT
    android.permission.BLUETOOTH_SCAN
    android.permission.POST_NOTIFICATIONS
)

# Grant all permissions
for perm in "${PERMISSIONS[@]}"; do
    adb shell pm grant "$PKG" "$perm" 2>/dev/null || true
done

# Special permission via appops
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow 2>/dev/null || true

# Verify by reading back actual status
echo "Verification:"
ok=0
fail=0
for perm in "${PERMISSIONS[@]}"; do
    status=$(adb shell dumpsys package "$PKG" | grep "$perm:" | head -1 | grep -o "granted=[a-z]*" | cut -d= -f2)
    if [ "$status" = "true" ]; then
        echo "  [OK] $perm"
        ok=$((ok + 1))
    else
        echo "  [FAIL] $perm (granted=$status)"
        fail=$((fail + 1))
    fi
done

# Verify SYSTEM_ALERT_WINDOW via appops
alert_status=$(adb shell appops get "$PKG" SYSTEM_ALERT_WINDOW 2>/dev/null | head -1 | grep -o "allow")
if [ "$alert_status" = "allow" ]; then
    echo "  [OK] SYSTEM_ALERT_WINDOW"
    ok=$((ok + 1))
else
    echo "  [FAIL] SYSTEM_ALERT_WINDOW"
    fail=$((fail + 1))
fi

echo "---"
echo "Done: $ok granted, $fail failed."
