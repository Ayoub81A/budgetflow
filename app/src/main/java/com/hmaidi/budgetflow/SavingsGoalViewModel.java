package com.hmaidi.budgetflow;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavingsGoalViewModel extends AndroidViewModel {

    private SavingsGoalDao savingsGoalDao;
    private LiveData<List<SavingsGoal>> allGoals;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public SavingsGoalViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        savingsGoalDao = db.savingsGoalDao();
        allGoals = savingsGoalDao.getAllGoals();
    }

    public LiveData<List<SavingsGoal>> getAllGoals() {
        return allGoals;
    }

    public void insert(SavingsGoal goal) {
        executor.execute(() -> savingsGoalDao.insert(goal));
    }

    public void update(SavingsGoal goal) {
        executor.execute(() -> savingsGoalDao.update(goal));
    }

    public void delete(SavingsGoal goal) {
        executor.execute(() -> savingsGoalDao.delete(goal));
    }

    public void addFunds(int goalId, double amount) {
        executor.execute(() -> {
            SavingsGoal goal = savingsGoalDao.getGoalByIdSync(goalId);
            if (goal != null) {
                goal.setSavedAmount(goal.getSavedAmount() + amount);
                savingsGoalDao.update(goal);
            }
        });
    }
}
