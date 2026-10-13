package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class TransactionFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(TransactionFilterCriteria criteria);
    }

    private TransactionFilterCriteria criteria;
    private OnFilterAppliedListener listener;
    private List<Wallet> walletsList = new ArrayList<>();
    private List<CategoryModel> categoriesList = new ArrayList<>();

    public static TransactionFilterBottomSheet newInstance(TransactionFilterCriteria criteria,
                                                            OnFilterAppliedListener listener) {
        TransactionFilterBottomSheet sheet = new TransactionFilterBottomSheet();
        sheet.criteria = criteria;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_filter_bottom_sheet, container, false);

        if (criteria == null) {
            criteria = new TransactionFilterCriteria();
        }

        // Views
        TextView tvReset = view.findViewById(R.id.tv_btn_reset_filters);
        ChipGroup chipGroupType = view.findViewById(R.id.chip_group_type);
        ChipGroup chipGroupWallets = view.findViewById(R.id.chip_group_wallets);
        ChipGroup chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        ChipGroup chipGroupDate = view.findViewById(R.id.chip_group_date_range);
        ChipGroup chipGroupSort = view.findViewById(R.id.chip_group_sort);
        TextInputEditText etMin = view.findViewById(R.id.et_filter_min_amount);
        TextInputEditText etMax = view.findViewById(R.id.et_filter_max_amount);
        MaterialButton btnApply = view.findViewById(R.id.btn_apply_filters);

        // Pre-fill type
        if ("expense".equals(criteria.type)) {
            chipGroupType.check(R.id.chip_type_expense);
        } else if ("income".equals(criteria.type)) {
            chipGroupType.check(R.id.chip_type_income);
        } else if ("transfer".equals(criteria.type)) {
            chipGroupType.check(R.id.chip_type_transfer);
        } else {
            chipGroupType.check(R.id.chip_type_all);
        }

        // Pre-fill date range
        if ("today".equals(criteria.dateRange)) {
            chipGroupDate.check(R.id.chip_date_today);
        } else if ("week".equals(criteria.dateRange)) {
            chipGroupDate.check(R.id.chip_date_week);
        } else if ("month".equals(criteria.dateRange)) {
            chipGroupDate.check(R.id.chip_date_month);
        } else if ("last_month".equals(criteria.dateRange)) {
            chipGroupDate.check(R.id.chip_date_last_month);
        } else {
            chipGroupDate.check(R.id.chip_date_all);
        }

        // Pre-fill sort
        if ("date_asc".equals(criteria.sortBy)) {
            chipGroupSort.check(R.id.chip_sort_date_asc);
        } else if ("amount_desc".equals(criteria.sortBy)) {
            chipGroupSort.check(R.id.chip_sort_amount_desc);
        } else if ("amount_asc".equals(criteria.sortBy)) {
            chipGroupSort.check(R.id.chip_sort_amount_asc);
        } else {
            chipGroupSort.check(R.id.chip_sort_date_desc);
        }

        // Pre-fill amount
        if (criteria.minAmount != null) etMin.setText(String.valueOf(criteria.minAmount));
        if (criteria.maxAmount != null) etMax.setText(String.valueOf(criteria.maxAmount));

        // Load wallets dynamically
        WalletViewModel walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);
        walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            walletsList = wallets != null ? wallets : new ArrayList<>();
            setupWalletChips(chipGroupWallets);
        });

        // Load categories dynamically
        CategoryViewModel categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            categoriesList = categories != null ? categories : new ArrayList<>();
            setupCategoryChips(chipGroupCategories);
        });

        // Reset click
        tvReset.setOnClickListener(v -> {
            criteria.reset();
            chipGroupType.check(R.id.chip_type_all);
            chipGroupDate.check(R.id.chip_date_all);
            chipGroupSort.check(R.id.chip_sort_date_desc);
            etMin.setText("");
            etMax.setText("");
            setupWalletChips(chipGroupWallets);
            setupCategoryChips(chipGroupCategories);
        });

        // Apply click
        btnApply.setOnClickListener(v -> {
            // Type
            int typeId = chipGroupType.getCheckedChipId();
            if (typeId == R.id.chip_type_expense) {
                criteria.type = "expense";
            } else if (typeId == R.id.chip_type_income) {
                criteria.type = "income";
            } else if (typeId == R.id.chip_type_transfer) {
                criteria.type = "transfer";
            } else {
                criteria.type = "all";
            }

            // Date
            int dateId = chipGroupDate.getCheckedChipId();
            if (dateId == R.id.chip_date_today) {
                criteria.dateRange = "today";
            } else if (dateId == R.id.chip_date_week) {
                criteria.dateRange = "week";
            } else if (dateId == R.id.chip_date_month) {
                criteria.dateRange = "month";
            } else if (dateId == R.id.chip_date_last_month) {
                criteria.dateRange = "last_month";
            } else {
                criteria.dateRange = "all";
            }

            // Sort
            int sortId = chipGroupSort.getCheckedChipId();
            if (sortId == R.id.chip_sort_date_asc) {
                criteria.sortBy = "date_asc";
            } else if (sortId == R.id.chip_sort_amount_desc) {
                criteria.sortBy = "amount_desc";
            } else if (sortId == R.id.chip_sort_amount_asc) {
                criteria.sortBy = "amount_asc";
            } else {
                criteria.sortBy = "date_desc";
            }

            // Min & Max
            String minStr = etMin.getText() != null ? etMin.getText().toString().trim() : "";
            String maxStr = etMax.getText() != null ? etMax.getText().toString().trim() : "";
            try {
                criteria.minAmount = minStr.isEmpty() ? null : Double.parseDouble(minStr);
            } catch (NumberFormatException ignored) {
                criteria.minAmount = null;
            }
            try {
                criteria.maxAmount = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);
            } catch (NumberFormatException ignored) {
                criteria.maxAmount = null;
            }

            if (listener != null) {
                listener.onFilterApplied(criteria);
            }
            dismiss();
        });

        return view;
    }

    private void setupWalletChips(ChipGroup group) {
        if (getContext() == null || group == null) return;
        group.removeAllViews();

        // "All Wallets" chip
        Chip allChip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice);
        allChip.setText("All Wallets");
        allChip.setCheckable(true);
        allChip.setChecked(criteria.walletId == null);
        allChip.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) criteria.walletId = null;
        });
        group.addView(allChip);

        for (Wallet w : walletsList) {
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice);
            chip.setText(w.getName());
            chip.setCheckable(true);
            chip.setChecked(criteria.walletId != null && criteria.walletId == w.getId());
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) criteria.walletId = w.getId();
            });
            group.addView(chip);
        }
    }

    private void setupCategoryChips(ChipGroup group) {
        if (getContext() == null || group == null) return;
        group.removeAllViews();

        // "All Categories" chip
        Chip allChip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice);
        allChip.setText("All Categories");
        allChip.setCheckable(true);
        allChip.setChecked(criteria.category == null);
        allChip.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) criteria.category = null;
        });
        group.addView(allChip);

        for (CategoryModel c : categoriesList) {
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice);
            chip.setText(c.getName());
            chip.setCheckable(true);
            chip.setChecked(criteria.category != null && criteria.category.equalsIgnoreCase(c.getName()));
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) criteria.category = c.getName();
            });
            group.addView(chip);
        }
    }
}
