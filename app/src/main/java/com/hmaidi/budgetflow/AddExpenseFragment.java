package com.hmaidi.budgetflow;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;
import androidx.appcompat.widget.SwitchCompat;

public class AddExpenseFragment extends BottomSheetDialogFragment {

    String selectedCategory = "";
    boolean isExpense = true;
    int selectedWalletId = -1; // ← moved to class level
    boolean isRecurring = false;
    String selectedInterval = "monthly";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        TextInputEditText etTitle = view.findViewById(R.id.et_title);
        TextInputEditText etAmount = view.findViewById(R.id.et_amount);
        TextInputEditText etDate = view.findViewById(R.id.et_date);
        Button btnExpense = view.findViewById(R.id.btn_expense);
        Button btnIncome = view.findViewById(R.id.btn_income);
        TextInputLayout layoutSource = view.findViewById(R.id.layout_source);
        LinearLayout categoryContainer = view.findViewById(R.id.category_container);
        TextView tvSelectedWallet = view.findViewById(R.id.tv_selected_wallet);

        // Auto-fill today's date
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        etDate.setText(day + "/" + (month + 1) + "/" + year);

        // Wallet picker
        WalletViewModel walletViewModel = new ViewModelProvider(requireActivity())
                .get(WalletViewModel.class);

        tvSelectedWallet.setOnClickListener(v -> {
            walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
                if (wallets == null || wallets.isEmpty()) {
                    Toast.makeText(getContext(),
                            "Please create a wallet first", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] walletNames = new String[wallets.size()];
                for (int i = 0; i < wallets.size(); i++) {
                    walletNames[i] = wallets.get(i).getName();
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Select Wallet")
                        .setItems(walletNames, (dialog, which) -> {
                            selectedWalletId = wallets.get(which).getId();
                            tvSelectedWallet.setText(wallets.get(which).getName());
                            tvSelectedWallet.setTextColor(
                                    ContextCompat.getColor(getContext(), R.color.text_primary));
                        })
                        .show();
            });
        });

        // Toggle buttons
        btnExpense.setOnClickListener(v -> {
            isExpense = true;
            categoryContainer.setVisibility(View.VISIBLE);
            layoutSource.setVisibility(View.GONE);
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.expense_red)));
            btnExpense.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.card_dark)));
            btnIncome.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        });

        btnIncome.setOnClickListener(v -> {
            isExpense = false;
            categoryContainer.setVisibility(View.GONE);
            layoutSource.setVisibility(View.VISIBLE);
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.income_green)));
            btnIncome.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.card_dark)));
            btnExpense.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        });

        // Category selection
        int[] categoryIds = {R.id.cat_food, R.id.cat_transport,
                R.id.cat_shopping, R.id.cat_health, R.id.cat_entertainment};
        String[] categoryNames = {"Food", "Transport", "Shopping", "Health", "Entertainment"};

        for (int i = 0; i < categoryIds.length; i++) {
            LinearLayout cat = view.findViewById(categoryIds[i]);
            String name = categoryNames[i];
            cat.setOnClickListener(v -> selectCategory(view, cat, name, categoryIds));
        }

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(getContext(), (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                etDate.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
            }, cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        SwitchCompat switchRecurring = view.findViewById(R.id.switch_recurring);
        LinearLayout layoutInterval = view.findViewById(R.id.layout_interval);

        Button btnDaily = view.findViewById(R.id.interval_daily);
        Button btnWeekly = view.findViewById(R.id.interval_weekly);
        Button btnMonthly = view.findViewById(R.id.interval_monthly);
        Button btnYearly = view.findViewById(R.id.interval_yearly);

        // Default monthly selected
        highlightInterval(view, btnMonthly);

        switchRecurring.setOnCheckedChangeListener((btn, isChecked) -> {
            isRecurring = isChecked;
            layoutInterval.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnDaily.setOnClickListener(v -> {
            selectedInterval = "daily";
            highlightInterval(view, btnDaily);
        });
        btnWeekly.setOnClickListener(v -> {
            selectedInterval = "weekly";
            highlightInterval(view, btnWeekly);
        });
        btnMonthly.setOnClickListener(v -> {
            selectedInterval = "monthly";
            highlightInterval(view, btnMonthly);
        });
        btnYearly.setOnClickListener(v -> {
            selectedInterval = "yearly";
            highlightInterval(view, btnYearly);
        });

        // Save button
        Button btnSave = view.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String amount = etAmount.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (title.isEmpty() || amount.isEmpty() || date.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isExpense && selectedCategory.isEmpty()) {
                Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedWalletId == -1) {
                Toast.makeText(getContext(), "Please select a wallet", Toast.LENGTH_SHORT).show();
                return;
            }

            ExpenseModel expense = new ExpenseModel(
                    title,
                    date,
                    Double.parseDouble(amount),
                    selectedCategory,
                    isExpense,
                    selectedWalletId
            );

            if (isRecurring) {
                expense.setRecurring(true);
                expense.setRecurringInterval(selectedInterval);
                expense.setNextDueDate(getFirstDueDate(selectedInterval));
            }

            ExpenseViewModel viewModel = new ViewModelProvider(requireActivity())
                    .get(ExpenseViewModel.class);
            viewModel.insert(expense);

            dismiss();
        });

        return view;
    }

    private void selectCategory(View root, LinearLayout selected,
                                String name, int[] allIds) {
        for (int id : allIds) {
            LinearLayout cat = root.findViewById(id);
            ImageView icon = (ImageView) cat.getChildAt(0);
            TextView label = (TextView) cat.getChildAt(1);
            icon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.card_dark)));
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.text_secondary)));
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        }

        ImageView icon = (ImageView) selected.getChildAt(0);
        TextView label = (TextView) selected.getChildAt(1);
        icon.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(getContext(), R.color.purple_main)));
        icon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(getContext(), R.color.text_primary)));
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.purple_light));
        selectedCategory = name;
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }

    private void highlightInterval(View root, Button selected) {
        int[] ids = {R.id.interval_daily, R.id.interval_weekly,
                R.id.interval_monthly, R.id.interval_yearly};
        for (int id : ids) {
            Button btn = root.findViewById(id);
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.card_dark)));
            btn.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        }
        selected.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(getContext(), R.color.purple_main)));
        selected.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
    }

    private long getFirstDueDate(String interval) {
        Calendar cal = Calendar.getInstance();
        switch (interval) {
            case "daily":   cal.add(Calendar.DAY_OF_YEAR, 1); break;
            case "weekly":  cal.add(Calendar.WEEK_OF_YEAR, 1); break;
            case "monthly": cal.add(Calendar.MONTH, 1); break;
            case "yearly":  cal.add(Calendar.YEAR, 1); break;
        }
        return cal.getTimeInMillis();
    }
}