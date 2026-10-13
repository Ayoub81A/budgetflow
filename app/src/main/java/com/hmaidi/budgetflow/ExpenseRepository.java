package com.hmaidi.budgetflow;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {

    private ExpenseDao expenseDao;
    private WalletDao walletDao;
    private LiveData<List<ExpenseModel>> allExpenses;
    private LiveData<Double> totalExpenses;
    private LiveData<Double> totalIncome;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private TransactionHistoryDao historyDao;

    private Application application;

    public ExpenseRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getInstance(application);
        expenseDao = db.expenseDao();
        walletDao = db.walletDao();
        historyDao = db.transactionHistoryDao();
        allExpenses = expenseDao.getAllExpenses();
        totalExpenses = expenseDao.getTotalExpenses();
        totalIncome = expenseDao.getTotalIncome();
        cleanupOldTrash();
    }

    public void insert(ExpenseModel expense) {
        executor.execute(() -> {
            expenseDao.insert(expense);
            Wallet wallet = walletDao.getWalletByIdSync(expense.getWalletId());
            if (wallet != null) {
                if (expense.isExpense()) {
                    wallet.setBalance(wallet.getBalance() - expense.getAmount());
                } else {
                    wallet.setBalance(wallet.getBalance() + expense.getAmount());
                }
                walletDao.update(wallet);
            }
            if (expense.isExpense()) {
                BudgetNotificationHelper.checkAllBudgetsAsync(application);
            }
        });
    }

    public void update(ExpenseModel newExpense) {
        executor.execute(() -> {
            ExpenseModel oldExpense = expenseDao.getExpenseByIdSync(newExpense.getId());
            if (oldExpense == null) return;

            // Log history record
            String oldJson = toJson(oldExpense);
            String newJson = toJson(newExpense);
            TransactionHistory history = new TransactionHistory(
                    newExpense.getId(),
                    System.currentTimeMillis(),
                    "EDIT",
                    oldJson,
                    newJson
            );
            historyDao.insert(history);

            // Reconcile wallet balances
            if (oldExpense.getWalletId() == newExpense.getWalletId()) {
                Wallet wallet = walletDao.getWalletByIdSync(newExpense.getWalletId());
                if (wallet != null) {
                    double oldImpact = oldExpense.isExpense() ? -oldExpense.getAmount() : oldExpense.getAmount();
                    double newImpact = newExpense.isExpense() ? -newExpense.getAmount() : newExpense.getAmount();
                    wallet.setBalance(wallet.getBalance() - oldImpact + newImpact);
                    walletDao.update(wallet);
                }
            } else {
                // Wallet changed
                Wallet oldWallet = walletDao.getWalletByIdSync(oldExpense.getWalletId());
                if (oldWallet != null) {
                    if (oldExpense.isExpense()) {
                        oldWallet.setBalance(oldWallet.getBalance() + oldExpense.getAmount());
                    } else {
                        oldWallet.setBalance(oldWallet.getBalance() - oldExpense.getAmount());
                    }
                    walletDao.update(oldWallet);
                }
                Wallet newWallet = walletDao.getWalletByIdSync(newExpense.getWalletId());
                if (newWallet != null) {
                    if (newExpense.isExpense()) {
                        newWallet.setBalance(newWallet.getBalance() - newExpense.getAmount());
                    } else {
                        newWallet.setBalance(newWallet.getBalance() + newExpense.getAmount());
                    }
                    walletDao.update(newWallet);
                }
            }

            expenseDao.update(newExpense);
            if (newExpense.isExpense()) {
                BudgetNotificationHelper.checkAllBudgetsAsync(application);
            }
        });
    }

    public void moveToTrash(ExpenseModel expense) {
        executor.execute(() -> {
            expense.setDeleted(true);
            expense.setDeletedTimestamp(System.currentTimeMillis());
            expenseDao.update(expense);

            // Revert impact on wallet balance while in trash
            Wallet wallet = walletDao.getWalletByIdSync(expense.getWalletId());
            if (wallet != null) {
                if (expense.isExpense()) {
                    wallet.setBalance(wallet.getBalance() + expense.getAmount());
                } else {
                    wallet.setBalance(wallet.getBalance() - expense.getAmount());
                }
                walletDao.update(wallet);
            }

            // Log history record
            TransactionHistory history = new TransactionHistory(
                    expense.getId(),
                    System.currentTimeMillis(),
                    "TRASH",
                    toJson(expense),
                    ""
            );
            historyDao.insert(history);
        });
    }

    public void restoreFromTrash(ExpenseModel expense) {
        executor.execute(() -> {
            expense.setDeleted(false);
            expense.setDeletedTimestamp(0);
            expenseDao.update(expense);

            // Re-apply impact on wallet balance
            Wallet wallet = walletDao.getWalletByIdSync(expense.getWalletId());
            if (wallet != null) {
                if (expense.isExpense()) {
                    wallet.setBalance(wallet.getBalance() - expense.getAmount());
                } else {
                    wallet.setBalance(wallet.getBalance() + expense.getAmount());
                }
                walletDao.update(wallet);
            }

            // Log history record
            TransactionHistory history = new TransactionHistory(
                    expense.getId(),
                    System.currentTimeMillis(),
                    "RESTORE",
                    "",
                    toJson(expense)
            );
            historyDao.insert(history);
        });
    }

    public void permanentDelete(ExpenseModel expense) {
        executor.execute(() -> {
            expenseDao.delete(expense);
            historyDao.deleteHistoryForTransaction(expense.getId());
        });
    }

    public void emptyTrash() {
        executor.execute(() -> {
            expenseDao.emptyTrash();
        });
    }

    public void cleanupOldTrash() {
        executor.execute(() -> {
            // Automatically purge items in trash older than 30 days
            long cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            List<ExpenseModel> expired = expenseDao.getExpiredTrashedExpenses(cutoff);
            if (expired != null) {
                for (ExpenseModel exp : expired) {
                    expenseDao.delete(exp);
                    historyDao.deleteHistoryForTransaction(exp.getId());
                }
            }
        });
    }

    public void revertHistoryChange(TransactionHistory historyRecord) {
        executor.execute(() -> {
            try {
                if (historyRecord.getPreviousStateJson() != null && !historyRecord.getPreviousStateJson().isEmpty()) {
                    org.json.JSONObject obj = new org.json.JSONObject(historyRecord.getPreviousStateJson());
                    ExpenseModel current = expenseDao.getExpenseByIdSync(historyRecord.getTransactionId());
                    if (current != null) {
                        ExpenseModel restored = new ExpenseModel(
                                obj.getString("title"),
                                obj.getString("date"),
                                obj.getDouble("amount"),
                                obj.getString("category"),
                                obj.getBoolean("isExpense"),
                                obj.getInt("walletId")
                        );
                        restored.setId(current.getId());
                        restored.setRecurring(obj.optBoolean("isRecurring", false));
                        restored.setRecurringInterval(obj.optString("recurringInterval", ""));
                        restored.setNextDueDate(obj.optLong("nextDueDate", 0));
                        restored.setDeleted(current.isDeleted());
                        restored.setDeletedTimestamp(current.getDeletedTimestamp());

                        update(restored);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void delete(ExpenseModel expense) {
        // Move to trash bin by default
        moveToTrash(expense);
    }

    public LiveData<List<ExpenseModel>> getTrashedExpenses() {
        return expenseDao.getTrashedExpenses();
    }

    public LiveData<List<TransactionHistory>> getHistoryForTransaction(int transactionId) {
        return historyDao.getHistoryForTransaction(transactionId);
    }

    public LiveData<List<ExpenseModel>> getAllExpenses() { return allExpenses; }
    public LiveData<Double> getTotalExpenses() { return totalExpenses; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }

    public LiveData<List<ExpenseModel>> getExpensesByWallet(int walletId) {
        return expenseDao.getExpensesByWallet(walletId);
    }

    public LiveData<Double> getIncomeByWallet(int walletId) {
        return expenseDao.getIncomeByWallet(walletId);
    }

    public LiveData<Double> getExpensesByWalletTotal(int walletId) {
        return expenseDao.getExpensesByWalletTotal(walletId);
    }

    public LiveData<List<ExpenseModel>> getAllRecurringExpenses() {
        return expenseDao.getAllRecurringExpenses();
    }

    private static String toJson(ExpenseModel e) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("id", e.getId());
            obj.put("title", e.getTitle());
            obj.put("date", e.getDate());
            obj.put("amount", e.getAmount());
            obj.put("category", e.getCategory());
            obj.put("isExpense", e.isExpense());
            obj.put("walletId", e.getWalletId());
            obj.put("isRecurring", e.isRecurring());
            obj.put("recurringInterval", e.getRecurringInterval());
            obj.put("nextDueDate", e.getNextDueDate());
            return obj.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}