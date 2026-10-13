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

    public LiveData<Wallet> getWalletById(int walletId) {
        return walletDao.getWalletById(walletId);
    }

    public LiveData<List<ExpenseModel>> getExpensesByWallet(int walletId) {
        return expenseDao.getExpensesByWallet(walletId);
    }

    public LiveData<Double> getIncomeByWallet(int walletId) {
        return expenseDao.getIncomeByWallet(walletId);
    }

    public LiveData<Double> getExpensesByWalletTotal(int walletId) {
        return expenseDao.getExpensesByWalletTotal(walletId);
    }

    public void transferBetweenWallets(int fromWalletId, int toWalletId, double amount, String note, String date) {
        executor.execute(() -> {
            if (fromWalletId == toWalletId || amount <= 0) return;
            Wallet fromWallet = walletDao.getWalletByIdSync(fromWalletId);
            Wallet toWallet = walletDao.getWalletByIdSync(toWalletId);
            if (fromWallet == null || toWallet == null) return;

            String cleanNote = (note != null && !note.trim().isEmpty()) ? note.trim() : null;
            String outTitle = cleanNote != null
                    ? cleanNote + " (→ " + toWallet.getName() + ")"
                    : "Transfer to " + toWallet.getName();
            String inTitle = cleanNote != null
                    ? cleanNote + " (← " + fromWallet.getName() + ")"
                    : "Transfer from " + fromWallet.getName();

            ExpenseModel outTx = new ExpenseModel(outTitle, date, amount, "Transfer", true, fromWalletId);
            ExpenseModel inTx = new ExpenseModel(inTitle, date, amount, "Transfer", false, toWalletId);

            expenseDao.insert(outTx);
            expenseDao.insert(inTx);

            fromWallet.setBalance(fromWallet.getBalance() - amount);
            toWallet.setBalance(toWallet.getBalance() + amount);
            walletDao.update(fromWallet);
            walletDao.update(toWallet);
        });
    }
}
