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
}