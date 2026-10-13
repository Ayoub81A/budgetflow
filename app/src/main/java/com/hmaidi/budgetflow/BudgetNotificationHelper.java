package com.hmaidi.budgetflow;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BudgetNotificationHelper {

    private static final String CHANNEL_ID = "budget_alerts_channel";
    private static final String PREF_NAME = "budget_alert_prefs";

    /**
     * Asynchronously evaluates overall budget and category budgets against
     * the current month's spending and triggers push notifications if thresholds are reached.
     */
    public static void checkAllBudgetsAsync(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        if (!CurrencyHelper.getNotifications(appContext)) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                List<ExpenseModel> allExpenses = db.expenseDao().getAllExpensesSync();
                if (allExpenses == null) return;

                Calendar now = Calendar.getInstance();
                int currentMonth = now.get(Calendar.MONTH) + 1;
                int currentYear = now.get(Calendar.YEAR);

                double monthlySpent = 0.0;
                Map<String, Double> categorySpentMap = new HashMap<>();

                for (ExpenseModel e : allExpenses) {
                    if (e.isExpense() && !e.isDeleted()
                            && !"Transfer".equalsIgnoreCase(e.getCategory())
                            && isDateInMonth(e.getDate(), currentMonth, currentYear)) {
                        monthlySpent += e.getAmount();
                        String cat = e.getCategory() != null ? e.getCategory() : "General";
                        categorySpentMap.put(cat, categorySpentMap.getOrDefault(cat, 0.0) + e.getAmount());
                    }
                }

                // 1. Overall monthly budget check
                double monthlyBudget = CurrencyHelper.getBudget(appContext);
                if (monthlyBudget > 0) {
                    checkAndNotifyBudget(appContext, monthlySpent, monthlyBudget);
                }

                // 2. Category budget checks
                List<CategoryBudget> catBudgets = db.categoryBudgetDao().getAllCategoryBudgetsSync();
                if (catBudgets != null) {
                    for (CategoryBudget cb : catBudgets) {
                        double limit = cb.getMonthlyLimit();
                        if (limit > 0) {
                            double catSpent = categorySpentMap.getOrDefault(cb.getCategoryName(), 0.0);
                            checkAndNotifyCategoryBudget(appContext, cb.getCategoryName(), catSpent, limit);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public static void checkAndNotifyBudget(Context context, double monthlySpent, double monthlyBudget) {
        if (context == null || monthlyBudget <= 0) return;
        if (!CurrencyHelper.getNotifications(context)) return;

        double percent = (monthlySpent / monthlyBudget) * 100.0;
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);
        String monthKey = currentYear + "_" + currentMonth;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean notified50 = prefs.getBoolean("notified_50_" + monthKey, false);
        boolean notified80 = prefs.getBoolean("notified_80_" + monthKey, false);
        boolean notified100 = prefs.getBoolean("notified_100_" + monthKey, false);
        boolean notified120 = prefs.getBoolean("notified_120_" + monthKey, false);

        if (percent >= 120.0 && !notified120) {
            sendNotification(
                    context,
                    1120,
                    "🛑 Critical Budget Alert (120%)",
                    "Warning: You've exceeded your monthly budget by 20% (" +
                            CurrencyHelper.format(context, monthlySpent) + " of " +
                            CurrencyHelper.format(context, monthlyBudget) + ")!"
            );
            prefs.edit().putBoolean("notified_120_" + monthKey, true).apply();
        } else if (percent >= 100.0 && !notified100) {
            sendNotification(
                    context,
                    1100,
                    "⚠️ Monthly Budget Limit Reached!",
                    "You've hit 100% of your monthly budget (" +
                            CurrencyHelper.format(context, monthlySpent) + " of " +
                            CurrencyHelper.format(context, monthlyBudget) + "). Any further spending will be over budget!"
            );
            prefs.edit().putBoolean("notified_100_" + monthKey, true).apply();
        } else if (percent >= 80.0 && percent < 100.0 && !notified80) {
            sendNotification(
                    context,
                    1080,
                    "⚡ 80% Budget Warning",
                    "Caution: You've reached 80% of your monthly budget (" +
                            CurrencyHelper.format(context, monthlySpent) + " of " +
                            CurrencyHelper.format(context, monthlyBudget) + "). Pace your spending for the rest of the month!"
            );
            prefs.edit().putBoolean("notified_80_" + monthKey, true).apply();
        } else if (percent >= 50.0 && percent < 80.0 && !notified50) {
            sendNotification(
                    context,
                    1050,
                    "📊 Budget Halfway Milestone (50%)",
                    "You've used 50% of your monthly budget (" +
                            CurrencyHelper.format(context, monthlySpent) + " of " +
                            CurrencyHelper.format(context, monthlyBudget) + ")."
            );
            prefs.edit().putBoolean("notified_50_" + monthKey, true).apply();
        }
    }

    public static void checkAndNotifyCategoryBudget(Context context, String categoryName, double categorySpent, double categoryLimit) {
        if (context == null || categoryLimit <= 0 || categoryName == null) return;
        if (!CurrencyHelper.getNotifications(context)) return;

        double percent = (categorySpent / categoryLimit) * 100.0;
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);
        String monthKey = currentYear + "_" + currentMonth;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean notifiedCat80 = prefs.getBoolean("notified_cat_80_" + categoryName + "_" + monthKey, false);
        boolean notifiedCat100 = prefs.getBoolean("notified_cat_100_" + categoryName + "_" + monthKey, false);

        int baseId = 2000 + Math.abs(categoryName.hashCode() % 1000);

        if (percent >= 100.0 && !notifiedCat100) {
            sendNotification(
                    context,
                    baseId + 1,
                    "🚨 " + categoryName + " Budget Exceeded!",
                    "You've spent " + CurrencyHelper.format(context, categorySpent) +
                            " of your " + CurrencyHelper.format(context, categoryLimit) + " limit for " + categoryName + "!"
            );
            prefs.edit().putBoolean("notified_cat_100_" + categoryName + "_" + monthKey, true).apply();
        } else if (percent >= 80.0 && percent < 100.0 && !notifiedCat80) {
            sendNotification(
                    context,
                    baseId,
                    "⚡ " + categoryName + " Budget Alert (80%)",
                    "You've used 80% of your " + categoryName + " budget (" +
                            CurrencyHelper.format(context, categorySpent) + " of " +
                            CurrencyHelper.format(context, categoryLimit) + ")."
            );
            prefs.edit().putBoolean("notified_cat_80_" + categoryName + "_" + monthKey, true).apply();
        }
    }

    private static boolean isDateInMonth(String dateStr, int targetMonth, int targetYear) {
        if (dateStr == null) return false;
        String[] parts = dateStr.trim().split("/");
        if (parts.length != 3) return false;
        try {
            int m = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            return m == targetMonth && y == targetYear;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void sendNotification(Context context, int id, String title, String message) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Budget & Spending Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when reaching or exceeding monthly and category budget limits");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_budget)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(id, builder.build());
    }
}
