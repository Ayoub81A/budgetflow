package com.hmaidi.budgetflow;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.biometric.BiometricManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SecurityHelper {

    private static final String PREF_SECURITY = "security_prefs";
    private static final String KEY_LOCK_ENABLED = "app_lock_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_LAST_PAUSE_TIME = "last_pause_time";

    private static boolean sessionUnlocked = false;
    private static final long LOCK_TIMEOUT_MS = 60 * 1000; // Lock if backgrounded for > 60 seconds

    public static boolean isLockEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOCK_ENABLED, false);
    }

    public static void setLockEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply();
        if (!enabled) {
            sessionUnlocked = true;
        }
    }

    public static boolean hasPin(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        return prefs.contains(KEY_PIN_HASH);
    }

    public static void setPin(Context context, String pin) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String hash = hashPin(pin);
        prefs.edit().putString(KEY_PIN_HASH, hash).apply();
    }

    public static boolean verifyPin(Context context, String enteredPin) {
        if (context == null || enteredPin == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String storedHash = prefs.getString(KEY_PIN_HASH, null);
        if (storedHash == null) return false;
        return storedHash.equals(hashPin(enteredPin));
    }

    public static boolean isBiometricEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true);
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static boolean isBiometricHardwareAvailable(Context context) {
        if (context == null) return false;
        BiometricManager bm = BiometricManager.from(context);
        int canAuth = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isSessionUnlocked() {
        return sessionUnlocked;
    }

    public static void setSessionUnlocked(boolean unlocked) {
        sessionUnlocked = unlocked;
    }

    public static void onAppPaused(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_PAUSE_TIME, System.currentTimeMillis()).apply();
    }

    public static boolean shouldPromptLock(Context context) {
        if (!isLockEnabled(context)) return false;
        if (!sessionUnlocked) return true;

        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        long lastPause = prefs.getLong(KEY_LAST_PAUSE_TIME, 0);
        if (lastPause > 0 && (System.currentTimeMillis() - lastPause) > LOCK_TIMEOUT_MS) {
            sessionUnlocked = false;
            return true;
        }
        return false;
    }

    public static String hashPin(String pin) {
        if (pin == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return pin;
        }
    }
}
