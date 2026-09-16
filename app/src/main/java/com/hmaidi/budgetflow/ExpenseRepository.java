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

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        expenseDao = db.expenseDao();
        walletDao = db.walletDao();
        allExpenses = expenseDao.getAllExpenses();
        totalExpenses = expenseDao.getTotalExpenses();
        totalIncome = expenseDao.getTotalIncome();
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
        });
    }

    public void delete(ExpenseModel expense) {
        executor.execute(() -> {
            expenseDao.delete(expense);
            Wallet wallet = walletDao.getWalletByIdSync(expense.getWalletId());
            if (wallet != null) {
                if (expense.isExpense()) {
                    wallet.setBalance(wallet.getBalance() + expense.getAmount());
                } else {
                    wallet.setBalance(wallet.getBalance() - expense.getAmount());
                }
                walletDao.update(wallet);
            }
        });
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
}