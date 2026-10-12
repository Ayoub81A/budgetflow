package com.hmaidi.budgetflow;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WalletDao {

    @Insert
    long insert(Wallet wallet);

    @Update
    void update(Wallet wallet);

    @Delete
    void delete(Wallet wallet);

    @Query("SELECT * FROM wallets ORDER BY name ASC")
    LiveData<List<Wallet>> getAllWallets();

    @Query("SELECT * FROM wallets WHERE id = :walletId")
    LiveData<Wallet> getWalletById(int walletId);

    @Query("SELECT * FROM wallets WHERE id = :walletId")
    Wallet getWalletByIdSync(int walletId);

    @Query("SELECT * FROM wallets ORDER BY id ASC")
    List<Wallet> getAllWalletsSync();

    @Insert
    void insertAll(List<Wallet> wallets);

    @Query("DELETE FROM wallets")
    void deleteAll();
}
