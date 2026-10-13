package com.hmaidi.budgetflow;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryBudgetViewModel extends AndroidViewModel {

    private final CategoryBudgetDao dao;
    private final LiveData<List<CategoryBudget>> allBudgets;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CategoryBudgetViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.categoryBudgetDao();
        allBudgets = dao.getAllCategoryBudgets();
    }

    public LiveData<List<CategoryBudget>> getAllCategoryBudgets() {
        return allBudgets;
    }

    public void insertOrUpdate(CategoryBudget budget) {
        executor.execute(() -> dao.insertOrUpdate(budget));
    }

    public void delete(CategoryBudget budget) {
        executor.execute(() -> dao.delete(budget));
    }

    public void deleteByName(String categoryName) {
        executor.execute(() -> dao.deleteByName(categoryName));
    }
}
