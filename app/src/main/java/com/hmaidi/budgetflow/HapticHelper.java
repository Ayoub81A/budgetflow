package com.hmaidi.budgetflow;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticHelper {

    public static void vibrateTick(View view) {
        if (view != null) {
            boolean handled = view.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
            if (!handled) {
                vibrateMillis(view.getContext(), 15);
            }
        }
    }

    public static void vibrateConfirm(View view) {
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                boolean handled = view.performHapticFeedback(
                        HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                );
                if (handled) return;
            }
            vibrateMillis(view.getContext(), 35);
        }
    }

    public static void vibrateError(View view) {
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                boolean handled = view.performHapticFeedback(
                        HapticFeedbackConstants.REJECT,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                );
                if (handled) return;
            }
            vibratePattern(view.getContext(), new long[]{0, 40, 60, 40});
        }
    }

    private static void vibrateMillis(Context context, long millis) {
        if (context == null) return;
        try {
            Vibrator vibrator = getVibrator(context);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(millis);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void vibratePattern(Context context, long[] pattern) {
        if (context == null) return;
        try {
            Vibrator vibrator = getVibrator(context);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (manager != null) {
                return manager.getDefaultVibrator();
            }
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
