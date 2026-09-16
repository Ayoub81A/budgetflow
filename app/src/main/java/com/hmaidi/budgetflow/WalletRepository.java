package com.hmaidi.budgetflow;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WalletRepository {

    private WalletDao walletDao;
    private ExpenseDao expenseDao;
    private LiveData<List<Wallet>> allWallets;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public WalletRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        walletDao = db.walletDao();
        expenseDao = db.expenseDao();
        allWallets = walletDao.getAllWallets();
    }

    public void insert(Wallet wallet) {
        executor.execute(() -> walletDao.insert(wallet));
    }

    public void update(Wallet wallet) {
        executor.execute(() -> walletDao.update(wallet));
    }

    public void delete(Wallet wallet) {
        executor.execute(() -> walletDao.delete(wallet));
    }

    public LiveData<List<Wallet>> getAllWallets() { return allWallets; }

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
