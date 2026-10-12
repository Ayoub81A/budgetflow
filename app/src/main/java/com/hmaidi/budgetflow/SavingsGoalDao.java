package com.hmaidi.budgetflow;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SavingsGoalDao {

    @Insert
    long insert(SavingsGoal goal);

    @Update
    void update(SavingsGoal goal);

    @Delete
    void delete(SavingsGoal goal);

    @Query("SELECT * FROM savings_goals ORDER BY id DESC")
    LiveData<List<SavingsGoal>> getAllGoals();

    @Query("SELECT * FROM savings_goals ORDER BY id DESC")
    List<SavingsGoal> getAllGoalsSync();

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    SavingsGoal getGoalByIdSync(int id);

    @Query("DELETE FROM savings_goals")
    void deleteAll();
}
