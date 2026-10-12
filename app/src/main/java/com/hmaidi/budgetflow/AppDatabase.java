package com.hmaidi.budgetflow;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ExpenseModel.class, Wallet.class, CategoryModel.class, TransactionHistory.class, SavingsGoal.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ExpenseDao expenseDao();
    public abstract WalletDao walletDao();
    public abstract CategoryDao categoryDao();
    public abstract TransactionHistoryDao transactionHistoryDao();
    public abstract SavingsGoalDao savingsGoalDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "expense_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}