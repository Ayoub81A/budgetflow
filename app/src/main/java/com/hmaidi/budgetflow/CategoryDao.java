package com.hmaidi.budgetflow;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(CategoryModel category);

    @Update
    void update(CategoryModel category);

    @Delete
    void delete(CategoryModel category);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<CategoryModel>> getAllCategories();

    @Query("SELECT * FROM categories WHERE isExpense = :isExpense ORDER BY name ASC")
    LiveData<List<CategoryModel>> getCategoriesByType(boolean isExpense);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryModel> getAllCategoriesSync();

    @Insert
    void insertAll(List<CategoryModel> categories);

    @Query("DELETE FROM categories WHERE isExpense = 0")
    void deleteIncomeCategories();

    @Query("DELETE FROM categories")
    void deleteAll();
}
