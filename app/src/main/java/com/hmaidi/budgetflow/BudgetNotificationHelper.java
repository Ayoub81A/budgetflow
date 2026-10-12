package com.hmaidi.budgetflow;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class BudgetNotificationHelper {

    private static final String CHANNEL_ID = "budget_alerts_channel";
    private static final String PREF_NAME = "budget_alert_prefs";

    public static void checkAndNotifyBudget(Context context, double monthlySpent, double monthlyBudget) {
        if (context == null || monthlyBudget <= 0) return;
        if (!CurrencyHelper.getNotifications(context)) return;

        double percent = (monthlySpent / monthlyBudget) * 100.0;
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);
        String monthKey = currentYear + "_" + currentMonth;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean notified80 = prefs.getBoolean("notified_80_" + monthKey, false);
        boolean notified100 = prefs.getBoolean("notified_100_" + monthKey, false);

        if (percent >= 100.0 && !notified100) {
            sendNotification(
                    context,
                    1001,
                    "⚠️ Monthly Budget Exceeded!",
                    "You've spent " + CurrencyHelper.format(context, monthlySpent) +
                            " of your " + CurrencyHelper.format(context, monthlyBudget) + " budget."
            );
            prefs.edit().putBoolean("notified_100_" + monthKey, true).apply();
        } else if (percent >= 80.0 && percent < 100.0 && !notified80) {
            sendNotification(
                    context,
                    1002,
                    "⚡ Approaching Budget Limit (80%)",
                    "You've spent " + String.format(java.util.Locale.US, "%.0f%%", percent) +
                            " (" + CurrencyHelper.format(context, monthlySpent) +
                            " of " + CurrencyHelper.format(context, monthlyBudget) + ")."
            );
            prefs.edit().putBoolean("notified_80_" + monthKey, true).apply();
        }
    }

    private static void sendNotification(Context context, int id, String title, String message) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when nearing or exceeding monthly budget limits");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_budget)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(id, builder.build());
    }
}
