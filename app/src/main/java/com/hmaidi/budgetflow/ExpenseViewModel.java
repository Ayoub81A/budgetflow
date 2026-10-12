package com.hmaidi.budgetflow;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {

    private ExpenseRepository repository;
    private LiveData<List<ExpenseModel>> allExpenses;
    private LiveData<Double> totalExpenses;
    private LiveData<Double> totalIncome;

    public ExpenseViewModel(Application application) {
        super(application);
        repository = new ExpenseRepository(application);
        allExpenses = repository.getAllExpenses();
        totalExpenses = repository.getTotalExpenses();
        totalIncome = repository.getTotalIncome();
    }

    public void insert(ExpenseModel expense) { repository.insert(expense); }
    public void update(ExpenseModel expense) { repository.update(expense); }
    public void delete(ExpenseModel expense) { repository.delete(expense); }
    public void moveToTrash(ExpenseModel expense) { repository.moveToTrash(expense); }
    public void restoreFromTrash(ExpenseModel expense) { repository.restoreFromTrash(expense); }
    public void permanentDelete(ExpenseModel expense) { repository.permanentDelete(expense); }
    public void emptyTrash() { repository.emptyTrash(); }
    public void cleanupOldTrash() { repository.cleanupOldTrash(); }
    public void revertHistoryChange(TransactionHistory history) { repository.revertHistoryChange(history); }

    public LiveData<List<ExpenseModel>> getTrashedExpenses() { return repository.getTrashedExpenses(); }
    public LiveData<List<TransactionHistory>> getHistoryForTransaction(int transactionId) { return repository.getHistoryForTransaction(transactionId); }

    public LiveData<List<ExpenseModel>> getAllExpenses() { return allExpenses; }
    public LiveData<Double> getTotalExpenses() { return totalExpenses; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<List<ExpenseModel>> getAllRecurringExpenses() { return repository.getAllRecurringExpenses(); }
}
