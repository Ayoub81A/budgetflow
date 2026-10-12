package com.hmaidi.budgetflow;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    void insert(ExpenseModel expense);

    @Update
    void update(ExpenseModel expense);

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    LiveData<List<ExpenseModel>> getAllExpenses();

    @Query("SELECT * FROM expenses WHERE walletId = :walletId ORDER BY id DESC")
    LiveData<List<ExpenseModel>> getExpensesByWallet(int walletId);

    @Query("SELECT SUM(amount) FROM expenses WHERE isExpense = 1")
    LiveData<Double> getTotalExpenses();

    @Query("SELECT SUM(amount) FROM expenses WHERE isExpense = 0")
    LiveData<Double> getTotalIncome();

    @Query("SELECT SUM(amount) FROM expenses WHERE walletId = :walletId AND isExpense = 0")
    LiveData<Double> getIncomeByWallet(int walletId);

    @Query("SELECT SUM(amount) FROM expenses WHERE walletId = :walletId AND isExpense = 1")
    LiveData<Double> getExpensesByWalletTotal(int walletId);

    // Get all recurring expenses that are due
    @Query("SELECT * FROM expenses WHERE isRecurring = 1 AND nextDueDate <= :now")
    List<ExpenseModel> getDueRecurringExpenses(long now);

    // Get all recurring expenses for display
    @Query("SELECT * FROM expenses WHERE isRecurring = 1 ORDER BY nextDueDate ASC")
    LiveData<List<ExpenseModel>> getAllRecurringExpenses();

    @Delete
    void delete(ExpenseModel expense);

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    List<ExpenseModel> getAllExpensesSync();

    @Insert
    void insertAll(List<ExpenseModel> expenses);

    @Query("DELETE FROM expenses")
    void deleteAll();
}