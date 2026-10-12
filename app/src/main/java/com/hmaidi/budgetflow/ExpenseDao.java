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

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY id DESC")
    LiveData<List<ExpenseModel>> getAllExpenses();

    @Query("SELECT * FROM expenses WHERE walletId = :walletId AND isDeleted = 0 ORDER BY id DESC")
    LiveData<List<ExpenseModel>> getExpensesByWallet(int walletId);

    @Query("SELECT SUM(amount) FROM expenses WHERE isExpense = 1 AND isDeleted = 0")
    LiveData<Double> getTotalExpenses();

    @Query("SELECT SUM(amount) FROM expenses WHERE isExpense = 0 AND isDeleted = 0")
    LiveData<Double> getTotalIncome();

    @Query("SELECT SUM(amount) FROM expenses WHERE walletId = :walletId AND isExpense = 0 AND isDeleted = 0")
    LiveData<Double> getIncomeByWallet(int walletId);

    @Query("SELECT SUM(amount) FROM expenses WHERE walletId = :walletId AND isExpense = 1 AND isDeleted = 0")
    LiveData<Double> getExpensesByWalletTotal(int walletId);

    // Get all recurring expenses that are due
    @Query("SELECT * FROM expenses WHERE isRecurring = 1 AND nextDueDate <= :now AND isDeleted = 0")
    List<ExpenseModel> getDueRecurringExpenses(long now);

    // Get all recurring expenses for display
    @Query("SELECT * FROM expenses WHERE isRecurring = 1 AND isDeleted = 0 ORDER BY nextDueDate ASC")
    LiveData<List<ExpenseModel>> getAllRecurringExpenses();

    @Query("SELECT * FROM expenses WHERE id = :id")
    ExpenseModel getExpenseByIdSync(int id);

    // Trash Bin Queries
    @Query("SELECT * FROM expenses WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    LiveData<List<ExpenseModel>> getTrashedExpenses();

    @Query("SELECT * FROM expenses WHERE isDeleted = 1 AND deletedTimestamp <= :cutoff")
    List<ExpenseModel> getExpiredTrashedExpenses(long cutoff);

    @Query("DELETE FROM expenses WHERE isDeleted = 1")
    void emptyTrash();

    @Query("UPDATE expenses SET category = :newCategory WHERE category = :oldCategory")
    void reassignCategory(String oldCategory, String newCategory);

    @Delete
    void delete(ExpenseModel expense);

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY id ASC")
    List<ExpenseModel> getAllExpensesSync();

    @Insert
    void insertAll(List<ExpenseModel> expenses);

    @Query("DELETE FROM expenses")
    void deleteAll();
}