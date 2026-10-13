package com.hmaidi.budgetflow;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CategoryBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(CategoryBudget budget);

    @Delete
    void delete(CategoryBudget budget);

    @Query("SELECT * FROM category_budgets ORDER BY categoryName ASC")
    LiveData<List<CategoryBudget>> getAllCategoryBudgets();

    @Query("SELECT * FROM category_budgets ORDER BY categoryName ASC")
    List<CategoryBudget> getAllCategoryBudgetsSync();

    @Query("SELECT * FROM category_budgets WHERE categoryName = :name LIMIT 1")
    CategoryBudget getCategoryBudgetSync(String name);

    @Query("DELETE FROM category_budgets WHERE categoryName = :name")
    void deleteByName(String name);

    @Query("DELETE FROM category_budgets")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryBudget> budgets);
}
