package com.hmaidi.budgetflow;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryViewModel extends AndroidViewModel {

    private CategoryDao categoryDao;
    private LiveData<List<CategoryModel>> allCategories;
    private LiveData<List<CategoryModel>> expenseCategories;
    private LiveData<List<CategoryModel>> incomeCategories;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public CategoryViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        allCategories = categoryDao.getAllCategories();
        expenseCategories = categoryDao.getCategoriesByType(true);
        incomeCategories = categoryDao.getCategoriesByType(false);
        initDefaultCategoriesIfEmpty();
    }

    private void initDefaultCategoriesIfEmpty() {
        executor.execute(() -> {
            List<CategoryModel> existing = categoryDao.getAllCategoriesSync();
            if (existing == null || existing.isEmpty()) {
                categoryDao.insert(new CategoryModel("Food", "ic_food", "#FF6B6B", true));
                categoryDao.insert(new CategoryModel("Transport", "ic_transport", "#4D96FF", true));
                categoryDao.insert(new CategoryModel("Shopping", "ic_shopping", "#FFD93D", true));
                categoryDao.insert(new CategoryModel("Health", "ic_health", "#6BCB77", true));
                categoryDao.insert(new CategoryModel("Entertainment", "ic_entertainment", "#9D4EDD", true));
                categoryDao.insert(new CategoryModel("Salary", "ic_wallet", "#00B4D8", false));
                categoryDao.insert(new CategoryModel("Freelance", "ic_card", "#7209B7", false));
                categoryDao.insert(new CategoryModel("Investment", "ic_savings", "#38B000", false));
            }
        });
    }

    public LiveData<List<CategoryModel>> getAllCategories() { return allCategories; }
    public LiveData<List<CategoryModel>> getExpenseCategories() { return expenseCategories; }
    public LiveData<List<CategoryModel>> getIncomeCategories() { return incomeCategories; }

    public void insert(CategoryModel category) {
        executor.execute(() -> categoryDao.insert(category));
    }

    public void update(CategoryModel category) {
        executor.execute(() -> categoryDao.update(category));
    }

    public void updateCategoryAndRenameExpenses(CategoryModel category, String oldName) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            if (!category.getName().equals(oldName)) {
                db.expenseDao().reassignCategory(oldName, category.getName());
            }
            categoryDao.update(category);
        });
    }

    public void delete(CategoryModel category) {
        deleteCategoryAndReassign(category, "Uncategorized");
    }

    public void deleteCategoryAndReassign(CategoryModel category, String fallbackCategory) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            // Reassign any transactions that used this category to fallbackCategory
            db.expenseDao().reassignCategory(category.getName(), fallbackCategory);
            categoryDao.delete(category);
        });
    }
}
