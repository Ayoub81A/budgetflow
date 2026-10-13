package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    private StringBuilder enteredPin = new StringBuilder();
    private View dot1, dot2, dot3, dot4;
    private TextView tvSubtitle;
    private ImageView btnBiometric;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent preview in recent apps switcher while locked
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_app_lock);

        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
        tvSubtitle = findViewById(R.id.tv_lock_subtitle);
        btnBiometric = findViewById(R.id.btn_key_biometric);

        setupKeypad();
        setupBiometrics();

        // Handle back press - send app to background instead of bypassing lock
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        // Trigger biometrics automatically if available and enabled
        if (SecurityHelper.isBiometricEnabled(this) && SecurityHelper.isBiometricHardwareAvailable(this)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::showBiometricPrompt, 250);
        }
    }

    private void setupKeypad() {
        int[] digitIds = {
                R.id.btn_key_0, R.id.btn_key_1, R.id.btn_key_2,
                R.id.btn_key_3, R.id.btn_key_4, R.id.btn_key_5,
                R.id.btn_key_6, R.id.btn_key_7, R.id.btn_key_8, R.id.btn_key_9
        };

        for (int id : digitIds) {
            TextView btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    HapticHelper.vibrateTick(v);
                    onDigitPressed(btn.getText().toString());
                });
            }
        }

        ImageView btnDelete = findViewById(R.id.btn_key_delete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                HapticHelper.vibrateTick(v);
                onDeletePressed();
            });
        }

        if (btnBiometric != null) {
            if (SecurityHelper.isBiometricHardwareAvailable(this) && SecurityHelper.isBiometricEnabled(this)) {
                btnBiometric.setVisibility(View.VISIBLE);
                btnBiometric.setOnClickListener(v -> {
                    HapticHelper.vibrateTick(v);
                    showBiometricPrompt();
                });
            } else {
                btnBiometric.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void onDigitPressed(String digit) {
        if (enteredPin.length() < 4) {
            enteredPin.append(digit);
            updateDots();
            if (enteredPin.length() == 4) {
                verifyEnteredPin();
            }
        }
    }

    private void onDeletePressed() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updateDots();
        }
    }

    private void updateDots() {
        int len = enteredPin.length();
        dot1.setBackgroundResource(len >= 1 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        dot2.setBackgroundResource(len >= 2 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        dot3.setBackgroundResource(len >= 3 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        dot4.setBackgroundResource(len >= 4 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
    }

    private void verifyEnteredPin() {
        String pin = enteredPin.toString();
        if (SecurityHelper.verifyPin(this, pin)) {
            unlockApp();
        } else {
            HapticHelper.vibrateError(tvSubtitle);
            tvSubtitle.setText("Incorrect PIN. Please try again.");
            tvSubtitle.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
            enteredPin.setLength(0);
            updateDots();
        }
    }

    private void unlockApp() {
        HapticHelper.vibrateConfirm(findViewById(android.R.id.content));
        SecurityHelper.setSessionUnlocked(true);
        finish();
    }

    private void setupBiometrics() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                unlockApp();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // User cancelled or opted for PIN; let them enter PIN
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(AppLockActivity.this, "Authentication failed. Try again or enter PIN.", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock BudgetFlow")
                .setSubtitle("Authenticate using your biometric credentials")
                .setNegativeButtonText("Use PIN")
                .build();
    }

    private void showBiometricPrompt() {
        if (biometricPrompt != null && promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        }
    }
}
