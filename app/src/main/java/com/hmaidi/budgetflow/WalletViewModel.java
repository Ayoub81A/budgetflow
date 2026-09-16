package com.hmaidi.budgetflow;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class WalletViewModel extends AndroidViewModel {

    private WalletRepository repository;
    private LiveData<List<Wallet>> allWallets;

    public WalletViewModel(Application application) {
        super(application);
        repository = new WalletRepository(application);
        allWallets = repository.getAllWallets();
    }

    public void insert(Wallet wallet) { repository.insert(wallet); }
    public void update(Wallet wallet) { repository.update(wallet); }
    public void delete(Wallet wallet) { repository.delete(wallet); }
    public LiveData<List<Wallet>> getAllWallets() { return allWallets; }

    public LiveData<List<ExpenseModel>> getExpensesByWallet(int walletId) {
        return repository.getExpensesByWallet(walletId);
    }

    public LiveData<Double> getIncomeByWallet(int walletId) {
        return repository.getIncomeByWallet(walletId);
    }

    public LiveData<Double> getExpensesByWalletTotal(int walletId) {
        return repository.getExpensesByWalletTotal(walletId);
    }
}
