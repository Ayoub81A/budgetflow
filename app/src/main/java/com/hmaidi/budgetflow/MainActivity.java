package com.hmaidi.budgetflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    FloatingActionButton fabAdd;
    String currentTab = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (CurrencyHelper.isDarkTheme(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        fabAdd = findViewById(R.id.fab_add);

        // Restore last tab after theme change recreation
        String startTab = getIntent().getStringExtra("start_tab");
        if (startTab != null && startTab.equals("user")) {
            currentTab = "user";
            fabAdd.setVisibility(View.GONE);
            loadFragment(new UserFragment());
            bottomNav.setSelectedItemId(R.id.nav_user);
        } else {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                currentTab = "home";
                fabAdd.setVisibility(View.VISIBLE);
                loadFragment(new HomeFragment());
            } else if (id == R.id.nav_stats) {
                currentTab = "stats";
                fabAdd.setVisibility(View.GONE);
                loadFragment(new StatsFragment());
            } else if (id == R.id.nav_wallet) {
                currentTab = "wallet";
                fabAdd.setVisibility(View.VISIBLE);
                loadFragment(new WalletFragment());
            } else if (id == R.id.nav_user) {
                currentTab = "user";
                fabAdd.setVisibility(View.GONE);
                loadFragment(new UserFragment());
            }
            return true;
        });

        fabAdd.setOnClickListener(v -> {
            if (currentTab.equals("wallet")) {
                AddWalletFragment sheet = new AddWalletFragment();
                sheet.show(getSupportFragmentManager(), "AddWallet");
            } else {
                AddExpenseFragment sheet = new AddExpenseFragment();
                sheet.show(getSupportFragmentManager(), "AddExpense");
            }
        });

        // Schedule recurring check every 24 hours
        PeriodicWorkRequest recurringWork =
                new PeriodicWorkRequest.Builder(RecurringWorker.class, 24, TimeUnit.HOURS)
                        .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recurring_check",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                recurringWork);

        // Also perform an immediate check on startup for any due recurring transactions
        OneTimeWorkRequest instantCheck =
                new OneTimeWorkRequest.Builder(RecurringWorker.class).build();
        WorkManager.getInstance(this).enqueue(instantCheck);

        // Request notification permission for Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SecurityHelper.shouldPromptLock(this)) {
            Intent lockIntent = new Intent(this, AppLockActivity.class);
            startActivity(lockIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SecurityHelper.onAppPaused(this);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}