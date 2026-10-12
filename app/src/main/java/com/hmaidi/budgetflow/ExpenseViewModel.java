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
    public void delete(ExpenseModel expense) { repository.delete(expense); }
    public LiveData<List<ExpenseModel>> getAllExpenses() { return allExpenses; }
    public LiveData<Double> getTotalExpenses() { return totalExpenses; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<List<ExpenseModel>> getAllRecurringExpenses() { return repository.getAllRecurringExpenses(); }
}
