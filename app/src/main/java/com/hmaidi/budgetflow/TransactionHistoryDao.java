package com.hmaidi.budgetflow;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TransactionHistoryDao {

    @Insert
    long insert(TransactionHistory history);

    @Delete
    void delete(TransactionHistory history);

    @Query("SELECT * FROM transaction_history WHERE transactionId = :transactionId ORDER BY timestamp DESC")
    LiveData<List<TransactionHistory>> getHistoryForTransaction(int transactionId);

    @Query("SELECT * FROM transaction_history WHERE transactionId = :transactionId ORDER BY timestamp DESC")
    List<TransactionHistory> getHistoryForTransactionSync(int transactionId);

    @Query("DELETE FROM transaction_history WHERE transactionId = :transactionId")
    void deleteHistoryForTransaction(int transactionId);

    @Query("DELETE FROM transaction_history")
    void deleteAll();
}
