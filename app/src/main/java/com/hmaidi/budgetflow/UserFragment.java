package com.hmaidi.budgetflow;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

public class UserFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;
    ImageView ivProfile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Views
        ivProfile = view.findViewById(R.id.iv_profile);
        ImageView ivEditPhoto = view.findViewById(R.id.iv_edit_photo);
        TextView tvProfileName = view.findViewById(R.id.tv_profile_name);
        TextView tvCurrencyDisplay = view.findViewById(R.id.tv_currency_display);
        TextView tvCurrentCurrency = view.findViewById(R.id.tv_current_currency);
        TextView tvBudgetValue = view.findViewById(R.id.tv_budget_value);
        Button btnEditName = view.findViewById(R.id.btn_edit_name);
        LinearLayout rowCurrency = view.findViewById(R.id.row_currency);
        LinearLayout rowBudget = view.findViewById(R.id.row_budget);
        LinearLayout rowClearData = view.findViewById(R.id.row_clear_data);
        SwitchCompat switchNotifications = view.findViewById(R.id.switch_notifications);
        SwitchCompat switchTheme = view.findViewById(R.id.switch_theme);

        // Load saved values
        tvProfileName.setText(CurrencyHelper.getName(getContext()));
        updateCurrencyDisplay(tvCurrencyDisplay, tvCurrentCurrency);
        updateBudgetDisplay(tvBudgetValue);
        switchNotifications.setChecked(CurrencyHelper.getNotifications(getContext()));
        switchTheme.setChecked(CurrencyHelper.isDarkTheme(getContext()));

        // Edit photo
        ivEditPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        // Edit name
        btnEditName.setOnClickListener(v -> {
            EditText etName = new EditText(getContext());
            etName.setText(CurrencyHelper.getName(getContext()));
            etName.setTextColor(getContext().getColor(R.color.text_primary));
            etName.setHint("Enter your name");

            new AlertDialog.Builder(getContext())
                    .setTitle("Edit Name")
                    .setView(etName)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String name = etName.getText().toString().trim();
                        if (!name.isEmpty()) {
                            CurrencyHelper.saveName(getContext(), name);
                            tvProfileName.setText(name);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Currency picker
        rowCurrency.setOnClickListener(v ->
                showCurrencyPicker(tvCurrencyDisplay, tvCurrentCurrency));

        // Budget limit
        rowBudget.setOnClickListener(v -> {
            EditText etBudget = new EditText(getContext());
            etBudget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            double current = CurrencyHelper.getBudget(getContext());
            if (current > 0) etBudget.setText(String.valueOf(current));
            etBudget.setHint("Enter monthly budget");
            etBudget.setTextColor(getContext().getColor(R.color.text_primary));

            new AlertDialog.Builder(getContext())
                    .setTitle("Monthly Budget Limit")
                    .setView(etBudget)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String val = etBudget.getText().toString().trim();
                        if (!val.isEmpty()) {
                            CurrencyHelper.saveBudget(getContext(),
                                    Double.parseDouble(val));
                            updateBudgetDisplay(tvBudgetValue);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) ->
                CurrencyHelper.saveNotifications(getContext(), isChecked));

        // Theme toggle
        switchTheme.setOnCheckedChangeListener((btn, isChecked) -> {
            CurrencyHelper.saveTheme(getContext(), isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Clear all data
        rowClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Clear All Data")
                    .setMessage("This will permanently delete all transactions and wallets. This cannot be undone!")
                    .setPositiveButton("Clear Everything", (dialog, which) -> {
                        ExpenseViewModel expenseViewModel = new ViewModelProvider(
                                requireActivity()).get(ExpenseViewModel.class);
                        WalletViewModel walletViewModel = new ViewModelProvider(
                                requireActivity()).get(WalletViewModel.class);

                        // Delete all expenses and wallets
                        expenseViewModel.getAllExpenses().observe(
                                getViewLifecycleOwner(), expenses -> {
                                    if (expenses != null) {
                                        for (ExpenseModel e : expenses) {
                                            expenseViewModel.delete(e);
                                        }
                                    }
                                });
                        walletViewModel.getAllWallets().observe(
                                getViewLifecycleOwner(), wallets -> {
                                    if (wallets != null) {
                                        for (Wallet w : wallets) {
                                            walletViewModel.delete(w);
                                        }
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK
                && data != null) {
            Uri imageUri = data.getData();
            ivProfile.setImageURI(imageUri);
            ivProfile.setPadding(0, 0, 0, 0);
            ivProfile.setBackgroundTintList(null);
            ivProfile.setColorFilter(null);
        }
    }

    private void updateCurrencyDisplay(TextView tvDisplay, TextView tvCurrent) {
        String code = CurrencyHelper.getCode(getContext());
        String symbol = CurrencyHelper.getSymbol(getContext());
        tvDisplay.setText(code + " — " + symbol);
        tvCurrent.setText(code);
    }

    private void updateBudgetDisplay(TextView tvBudget) {
        double budget = CurrencyHelper.getBudget(getContext());
        if (budget > 0) {
            tvBudget.setText(CurrencyHelper.format(getContext(), budget));
        } else {
            tvBudget.setText("Not set");
        }
    }

    private void showCurrencyPicker(TextView tvDisplay, TextView tvCurrent) {
        List<CurrencyData.Currency> currencies = CurrencyData.getCurrencies();
        String[] options = new String[currencies.size()];
        for (int i = 0; i < currencies.size(); i++) {
            options[i] = currencies.get(i).toString();
        }

        String currentCode = CurrencyHelper.getCode(getContext());
        int currentIndex = 0;
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).code.equals(currentCode)) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Select Currency")
                .setSingleChoiceItems(options, currentIndex, null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    android.app.AlertDialog d = (android.app.AlertDialog) dialog;
                    int selected = d.getListView().getCheckedItemPosition();
                    CurrencyData.Currency currency = currencies.get(selected);
                    CurrencyHelper.saveCurrency(
                            getContext(), currency.code, currency.symbol);
                    updateCurrencyDisplay(tvDisplay, tvCurrent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}