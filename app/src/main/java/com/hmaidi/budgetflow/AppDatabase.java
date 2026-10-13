package com.hmaidi.budgetflow;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ExpenseModel.class, Wallet.class, CategoryModel.class, TransactionHistory.class, SavingsGoal.class, CategoryBudget.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ExpenseDao expenseDao();
    public abstract WalletDao walletDao();
    public abstract CategoryDao categoryDao();
    public abstract TransactionHistoryDao transactionHistoryDao();
    public abstract SavingsGoalDao savingsGoalDao();
    public abstract CategoryBudgetDao categoryBudgetDao();

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `category_budgets` (`categoryName` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, PRIMARY KEY(`categoryName`))");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "expense_database")
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}