package com.hmaidi.budgetflow;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Calendar;
import java.util.List;

public class RecurringWorker extends Worker {

    private static final String CHANNEL_ID = "recurring_channel";

    public RecurringWorker(@NonNull Context context,
                           @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        ExpenseDao expenseDao = db.expenseDao();
        WalletDao walletDao = db.walletDao();

        long now = System.currentTimeMillis();
        List<ExpenseModel> dueExpenses = expenseDao.getDueRecurringExpenses(now);

        for (ExpenseModel template : dueExpenses) {
            // Create new expense from template
            ExpenseModel newExpense = new ExpenseModel(
                    template.getTitle(),
                    getCurrentDate(),
                    template.getAmount(),
                    template.getCategory(),
                    template.isExpense(),
                    template.getWalletId()
            );
            expenseDao.insert(newExpense);

            // Update wallet balance
            Wallet wallet = walletDao.getWalletByIdSync(template.getWalletId());
            if (wallet != null) {
                if (template.isExpense()) {
                    wallet.setBalance(wallet.getBalance() - template.getAmount());
                } else {
                    wallet.setBalance(wallet.getBalance() + template.getAmount());
                }
                walletDao.update(wallet);
            }

            // Update next due date
            template.setNextDueDate(getNextDueDate(
                    template.getNextDueDate(),
                    template.getRecurringInterval()));
            expenseDao.update(template);

            // Show notification if enabled in settings
            if (CurrencyHelper.getNotifications(getApplicationContext())) {
                showNotification(template);
            }
        }

        if (!dueExpenses.isEmpty()) {
            BudgetNotificationHelper.checkAllBudgetsAsync(getApplicationContext());
        }

        // Check for upcoming bills due in the next 48 hours and send reminder notifications
        if (CurrencyHelper.getNotifications(getApplicationContext())) {
            checkUpcomingBillReminders(expenseDao, now);
        }

        return Result.success();
    }

    private String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_MONTH) + "/" +
                (cal.get(Calendar.MONTH) + 1) + "/" +
                cal.get(Calendar.YEAR);
    }

    private long getNextDueDate(long lastDue, String interval) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastDue);
        switch (interval) {
            case "daily":   cal.add(Calendar.DAY_OF_YEAR, 1); break;
            case "weekly":  cal.add(Calendar.WEEK_OF_YEAR, 1); break;
            case "monthly": cal.add(Calendar.MONTH, 1); break;
            case "yearly":  cal.add(Calendar.YEAR, 1); break;
        }
        return cal.getTimeInMillis();
    }

    private void showNotification(ExpenseModel expense) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Recurring Transactions",
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        String type = expense.isExpense() ? "Expense" : "Income";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wallet)
                .setContentTitle("Recurring " + type + " Added")
                .setContentText(expense.getTitle() + " — " +
                        CurrencyHelper.format(getApplicationContext(), expense.getAmount()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void checkUpcomingBillReminders(ExpenseDao expenseDao, long now) {
        Context context = getApplicationContext();
        android.content.SharedPreferences prefs =
                context.getSharedPreferences("bill_reminders_prefs", Context.MODE_PRIVATE);

        long in48Hours = now + (48L * 60 * 60 * 1000);
        List<ExpenseModel> upcoming = expenseDao.getUpcomingRecurringExpenses(now, in48Hours);

        if (upcoming != null) {
            for (ExpenseModel item : upcoming) {
                String key = "reminded_" + item.getId() + "_" + item.getNextDueDate();
                if (!prefs.getBoolean(key, false)) {
                    long diffMs = item.getNextDueDate() - now;
                    long diffHours = diffMs / (1000 * 60 * 60);

                    String timing;
                    if (diffHours <= 24) {
                        timing = "is due tomorrow (" + CurrencyHelper.format(context, item.getAmount()) + ")";
                    } else {
                        timing = "is due in 2 days (" + CurrencyHelper.format(context, item.getAmount()) + ")";
                    }

                    showUpcomingReminderNotification(item, timing);
                    prefs.edit().putBoolean(key, true).apply();
                }
            }
        }
    }

    private void showUpcomingReminderNotification(ExpenseModel item, String timing) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Recurring Transactions",
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        String type = item.isExpense() ? "Bill" : "Income";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_repeat)
                .setContentTitle("Upcoming " + type + " Reminder")
                .setContentText(item.getTitle() + " " + timing)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Reminder: Your recurring " + (item.isExpense() ? "bill" : "income")
                                + " '" + item.getTitle() + "' " + timing + "."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) (item.getId() + 100000), builder.build());
    }
}