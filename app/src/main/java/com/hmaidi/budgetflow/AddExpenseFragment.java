package com.hmaidi.budgetflow;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

    private static final String ARG_PRESELECT_RECURRING = "arg_preselect_recurring";
    private java.util.List<Wallet> availableWallets = new java.util.ArrayList<>();

    String selectedCategory = "";
    boolean isExpense = true;
    int selectedWalletId = -1;
    boolean isRecurring = false;
    String selectedInterval = "monthly";

    public static AddExpenseFragment newInstance(boolean preselectRecurring) {
        AddExpenseFragment fragment = new AddExpenseFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PRESELECT_RECURRING, preselectRecurring);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        TextInputEditText etTitle = view.findViewById(R.id.et_title);
        TextInputEditText etAmount = view.findViewById(R.id.et_amount);
        TextInputEditText etDate = view.findViewById(R.id.et_date);
        TextInputEditText etSource = view.findViewById(R.id.et_source);
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

        // Wallet picker & auto-selection
        WalletViewModel walletViewModel = new ViewModelProvider(requireActivity())
                .get(WalletViewModel.class);

        walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            availableWallets = wallets != null ? wallets : new java.util.ArrayList<>();
            // Auto-select the first wallet if none selected yet
            if (selectedWalletId == -1 && !availableWallets.isEmpty() && getContext() != null) {
                Wallet first = availableWallets.get(0);
                selectedWalletId = first.getId();
                tvSelectedWallet.setText(first.getName());
                tvSelectedWallet.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        });

        tvSelectedWallet.setOnClickListener(v -> {
            if (getContext() == null) return;
            if (availableWallets.isEmpty()) {
                Toast.makeText(getContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] walletNames = new String[availableWallets.size()];
            for (int i = 0; i < availableWallets.size(); i++) {
                walletNames[i] = availableWallets.get(i).getName();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Wallet")
                    .setItems(walletNames, (dialog, which) -> {
                        Wallet chosen = availableWallets.get(which);
                        selectedWalletId = chosen.getId();
                        tvSelectedWallet.setText(chosen.getName());
                        tvSelectedWallet.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.text_primary));
                    })
                    .show();
        });

        // Toggle buttons
        btnExpense.setOnClickListener(v -> {
            isExpense = true;
            categoryContainer.setVisibility(View.VISIBLE);
            layoutSource.setVisibility(View.GONE);
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.expense_red)));
            btnExpense.setTextColor(Color.WHITE);
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.input_bg)));
            btnIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        });

        btnIncome.setOnClickListener(v -> {
            isExpense = false;
            categoryContainer.setVisibility(View.GONE);
            layoutSource.setVisibility(View.VISIBLE);
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.income_green)));
            btnIncome.setTextColor(Color.WHITE);
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.input_bg)));
            btnExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        });

        // Category selection
        LinearLayout categoryRow = view.findViewById(R.id.category_row);
        CategoryViewModel categoryViewModel = new ViewModelProvider(requireActivity())
                .get(CategoryViewModel.class);

        categoryViewModel.getExpenseCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories == null || getContext() == null) return;
            categoryRow.removeAllViews();

            float density = getResources().getDisplayMetrics().density;
            int widthPx = (int) (64 * density);
            int marginPx = (int) (8 * density);
            int iconSizePx = (int) (32 * density);
            int iconPadPx = (int) (6 * density);

            for (CategoryModel cat : categories) {
                LinearLayout catLayout = new LinearLayout(getContext());
                catLayout.setOrientation(LinearLayout.VERTICAL);
                catLayout.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT);
                params.setMarginEnd(marginPx);
                catLayout.setLayoutParams(params);

                ImageView icon = new ImageView(getContext());
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSizePx, iconSizePx);
                icon.setLayoutParams(iconParams);
                icon.setImageResource(resolveIconRes(cat.getIcon()));
                icon.setBackgroundResource(R.drawable.circle_bg);
                icon.setPadding(iconPadPx, iconPadPx, iconPadPx, iconPadPx);

                TextView label = new TextView(getContext());
                label.setText(cat.getName());
                label.setTextSize(9);
                label.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                textParams.topMargin = (int) (2 * density);
                label.setLayoutParams(textParams);

                boolean isSelected = cat.getName().equalsIgnoreCase(selectedCategory);
                applyCategorySelectionStyle(icon, label, isSelected);

                catLayout.addView(icon);
                catLayout.addView(label);

                catLayout.setOnClickListener(v -> {
                    selectedCategory = cat.getName();
                    updateCategorySelectionVisuals(categoryRow);
                });

                categoryRow.addView(catLayout);
            }

            // Quick "+ New" Category button
            LinearLayout addCatLayout = new LinearLayout(getContext());
            addCatLayout.setOrientation(LinearLayout.VERTICAL);
            addCatLayout.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT);
            addCatLayout.setLayoutParams(addParams);

            ImageView addIcon = new ImageView(getContext());
            LinearLayout.LayoutParams addIconParams = new LinearLayout.LayoutParams(iconSizePx, iconSizePx);
            addIcon.setLayoutParams(addIconParams);
            addIcon.setImageResource(R.drawable.ic_add);
            addIcon.setBackgroundResource(R.drawable.circle_bg);
            addIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.input_bg)));
            addIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_light)));
            addIcon.setPadding(iconPadPx, iconPadPx, iconPadPx, iconPadPx);

            TextView addLabel = new TextView(getContext());
            addLabel.setText("+ New");
            addLabel.setTextSize(9);
            addLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_light));
            LinearLayout.LayoutParams addTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            addTextParams.topMargin = (int) (2 * density);
            addLabel.setLayoutParams(addTextParams);

            addCatLayout.addView(addIcon);
            addCatLayout.addView(addLabel);

            addCatLayout.setOnClickListener(v -> showQuickAddCategoryDialog(categoryViewModel));
            categoryRow.addView(addCatLayout);
        });

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

        boolean preselectRecurring = false;
        if (getArguments() != null) {
            preselectRecurring = getArguments().getBoolean(ARG_PRESELECT_RECURRING, false);
        }

        switchRecurring.setOnCheckedChangeListener((btn, isChecked) -> {
            isRecurring = isChecked;
            layoutInterval.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        if (preselectRecurring) {
            switchRecurring.setChecked(true);
            isRecurring = true;
            layoutInterval.setVisibility(View.VISIBLE);
        }

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

            String cat = selectedCategory;
            if (!isExpense) {
                String sourceText = etSource.getText() != null ? etSource.getText().toString().trim() : "";
                cat = sourceText.isEmpty() ? "Income" : sourceText;
            }

            ExpenseModel expense = new ExpenseModel(
                    title,
                    date,
                    Double.parseDouble(amount),
                    cat,
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

    private void applyCategorySelectionStyle(ImageView icon, TextView label, boolean isSelected) {
        if (getContext() == null) return;
        if (isSelected) {
            icon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.purple_main)));
            icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_main));
        } else {
            icon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.input_bg)));
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void updateCategorySelectionVisuals(LinearLayout categoryRow) {
        for (int i = 0; i < categoryRow.getChildCount() - 1; i++) {
            View child = categoryRow.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout item = (LinearLayout) child;
                ImageView icon = (ImageView) item.getChildAt(0);
                TextView label = (TextView) item.getChildAt(1);
                boolean isSelected = label.getText().toString().equalsIgnoreCase(selectedCategory);
                applyCategorySelectionStyle(icon, label, isSelected);
            }
        }
    }

    private void showQuickAddCategoryDialog(CategoryViewModel categoryViewModel) {
        if (getContext() == null) return;
        CategoryEditorHelper.showEditorDialog(requireContext(), categoryViewModel, null, newCat -> {
            selectedCategory = newCat.getName();
        });
    }

    private int resolveIconRes(String iconName) {
        if (iconName == null) return R.drawable.ic_wallet;
        switch (iconName.toLowerCase(java.util.Locale.ROOT)) {
            case "food":
            case "ic_food": return R.drawable.ic_food;
            case "transport":
            case "ic_transport": return R.drawable.ic_transport;
            case "shopping":
            case "ic_shopping": return R.drawable.ic_shopping;
            case "health":
            case "ic_health": return R.drawable.ic_health;
            case "entertainment":
            case "ic_entertainment": return R.drawable.ic_entertainment;
            case "card":
            case "ic_card": return R.drawable.ic_card;
            case "savings":
            case "ic_savings": return R.drawable.ic_savings;
            default: return R.drawable.ic_wallet;
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }

    @Override
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
            android.widget.FrameLayout bottomSheet =
                    bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void highlightInterval(View root, Button selected) {
        int[] ids = {R.id.interval_daily, R.id.interval_weekly,
                R.id.interval_monthly, R.id.interval_yearly};
        for (int id : ids) {
            Button btn = root.findViewById(id);
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.input_bg)));
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
        selected.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.purple_main)));
        selected.setTextColor(Color.WHITE);
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