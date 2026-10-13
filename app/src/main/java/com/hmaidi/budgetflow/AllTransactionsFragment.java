package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllTransactionsFragment extends Fragment {

    private ExpenseAdapter adapter;
    private ExpenseViewModel viewModel;
    private WalletViewModel walletViewModel;
    private List<ExpenseModel> allTransactions = new ArrayList<>();
    private Map<Integer, String> walletNameMap = new HashMap<>();

    private TransactionFilterCriteria criteria = new TransactionFilterCriteria();

    private TextView tvResultsCount;
    private LinearLayout emptyState;
    private RecyclerView recyclerView;
    private ImageView btnFilterDialog;
    private View viewFilterActiveDot;
    private HorizontalScrollView scrollActiveFilters;
    private LinearLayout layoutActiveFilterChips;

    private Button btnFilterAll, btnFilterExpense, btnFilterIncome, btnFilterTransfer, btnQuickFilterMore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_transactions, container, false);

        tvResultsCount = view.findViewById(R.id.tv_results_count);
        emptyState = view.findViewById(R.id.empty_filter_state);
        recyclerView = view.findViewById(R.id.rv_all_transactions);
        btnFilterDialog = view.findViewById(R.id.btn_filter_dialog);
        viewFilterActiveDot = view.findViewById(R.id.view_filter_active_dot);
        scrollActiveFilters = view.findViewById(R.id.scroll_active_filters);
        layoutActiveFilterChips = view.findViewById(R.id.layout_active_filter_chips);

        btnFilterAll = view.findViewById(R.id.filter_all);
        btnFilterExpense = view.findViewById(R.id.filter_expense);
        btnFilterIncome = view.findViewById(R.id.filter_income);
        btnFilterTransfer = view.findViewById(R.id.filter_transfer);
        btnQuickFilterMore = view.findViewById(R.id.btn_quick_filter_more);

        // Back button
        ImageView btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Set up adapter
        adapter = new ExpenseAdapter(getContext(), new ArrayList<>(), expense -> {
            ExpenseDetailFragment detail = ExpenseDetailFragment.newInstance(expense);
            detail.show(getParentFragmentManager(), "ExpenseDetail");
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Search
        TextInputEditText etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                criteria.searchQuery = s.toString();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Quick type buttons
        btnFilterAll.setOnClickListener(v -> setQuickType("all"));
        btnFilterExpense.setOnClickListener(v -> setQuickType("expense"));
        btnFilterIncome.setOnClickListener(v -> setQuickType("income"));
        btnFilterTransfer.setOnClickListener(v -> setQuickType("transfer"));

        // Filter dialog trigger
        View.OnClickListener openFilterAction = v -> showFilterBottomSheet();
        btnFilterDialog.setOnClickListener(openFilterAction);
        btnQuickFilterMore.setOnClickListener(openFilterAction);

        // Observe wallets for label mapping
        walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);
        walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            walletNameMap.clear();
            if (wallets != null) {
                for (Wallet w : wallets) {
                    walletNameMap.put(w.getId(), w.getName());
                }
            }
            updateActiveFilterChips();
        });

        // Observe all transactions
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allTransactions = expenses != null ? expenses : new ArrayList<>();
            applyFilter();
        });

        return view;
    }

    private void setQuickType(String type) {
        criteria.type = type;
        applyFilter();
    }

    private void showFilterBottomSheet() {
        TransactionFilterBottomSheet sheet = TransactionFilterBottomSheet.newInstance(criteria, updatedCriteria -> {
            criteria = updatedCriteria;
            applyFilter();
        });
        sheet.show(getChildFragmentManager(), "FilterBottomSheet");
    }

    private void updateTypeButtonStyles() {
        if (getContext() == null) return;
        int activeBg = ContextCompat.getColor(requireContext(), R.color.purple_main);
        int inactiveBg = ContextCompat.getColor(requireContext(), R.color.card_dark);
        int activeText = Color.WHITE;
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        Button[] buttons = {btnFilterAll, btnFilterExpense, btnFilterIncome, btnFilterTransfer};
        String[] types = {"all", "expense", "income", "transfer"};

        for (int i = 0; i < buttons.length; i++) {
            boolean isActive = types[i].equals(criteria.type);
            buttons[i].setBackgroundTintList(ColorStateList.valueOf(isActive ? activeBg : inactiveBg));
            buttons[i].setTextColor(isActive ? activeText : inactiveText);
        }
    }

    private void applyFilter() {
        if (getContext() == null) return;

        updateTypeButtonStyles();
        viewFilterActiveDot.setVisibility(criteria.hasActiveFilters() ? View.VISIBLE : View.GONE);
        updateActiveFilterChips();

        List<ExpenseModel> filtered = new ArrayList<>();
        double totalAmount = 0.0;

        for (ExpenseModel e : allTransactions) {
            if (criteria.matches(e)) {
                filtered.add(e);
                totalAmount += e.getAmount();
            }
        }

        // Sorting
        Collections.sort(filtered, (e1, e2) -> {
            switch (criteria.sortBy) {
                case "date_asc":
                    return compareDates(e1.getDate(), e2.getDate());
                case "amount_desc":
                    return Double.compare(e2.getAmount(), e1.getAmount());
                case "amount_asc":
                    return Double.compare(e1.getAmount(), e2.getAmount());
                case "category_asc": {
                    String c1 = e1.getCategory() != null ? e1.getCategory() : "";
                    String c2 = e2.getCategory() != null ? e2.getCategory() : "";
                    int cmp = c1.compareToIgnoreCase(c2);
                    return cmp != 0 ? cmp : compareDates(e2.getDate(), e1.getDate());
                }
                case "wallet_asc": {
                    String w1 = walletNameMap.getOrDefault(e1.getWalletId(), "");
                    String w2 = walletNameMap.getOrDefault(e2.getWalletId(), "");
                    int cmp = w1.compareToIgnoreCase(w2);
                    return cmp != 0 ? cmp : compareDates(e2.getDate(), e1.getDate());
                }
                case "date_desc":
                default:
                    int dateComp = compareDates(e2.getDate(), e1.getDate());
                    if (dateComp != 0) return dateComp;
                    return Integer.compare(e2.getId(), e1.getId());
            }
        });

        adapter.updateList(filtered);

        String countText = filtered.size() + (filtered.size() == 1 ? " transaction" : " transactions");
        if (!filtered.isEmpty()) {
            countText += "  •  Total: " + CurrencyHelper.format(getContext(), totalAmount);
        }
        tvResultsCount.setText(countText);

        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void updateActiveFilterChips() {
        if (getContext() == null || layoutActiveFilterChips == null) return;
        layoutActiveFilterChips.removeAllViews();

        if (!criteria.hasActiveFilters()) {
            scrollActiveFilters.setVisibility(View.GONE);
            return;
        }

        scrollActiveFilters.setVisibility(View.VISIBLE);
        float density = getResources().getDisplayMetrics().density;

        // Wallet chip
        if (criteria.walletId != null) {
            String wName = walletNameMap.getOrDefault(criteria.walletId, "Wallet #" + criteria.walletId);
            addFilterChip("Wallet: " + wName, () -> {
                criteria.walletId = null;
                applyFilter();
            });
        }

        // Category chip
        if (criteria.category != null && !criteria.category.isEmpty()) {
            addFilterChip("Category: " + criteria.category, () -> {
                criteria.category = null;
                applyFilter();
            });
        }

        // Date range chip
        if (!"all".equals(criteria.dateRange)) {
            String label = criteria.dateRange.replace("_", " ");
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
            addFilterChip("Period: " + label, () -> {
                criteria.dateRange = "all";
                applyFilter();
            });
        }

        // Sort chip
        if (!"date_desc".equals(criteria.sortBy)) {
            String sortLabel = "Sort: " + criteria.sortBy.replace("_", " ");
            if ("amount_desc".equals(criteria.sortBy)) sortLabel = "Sort: High to Low";
            else if ("amount_asc".equals(criteria.sortBy)) sortLabel = "Sort: Low to High";
            else if ("date_asc".equals(criteria.sortBy)) sortLabel = "Sort: Oldest First";
            else if ("category_asc".equals(criteria.sortBy)) sortLabel = "Sort: Category (A-Z)";
            else if ("wallet_asc".equals(criteria.sortBy)) sortLabel = "Sort: Wallet (A-Z)";

            addFilterChip(sortLabel, () -> {
                criteria.sortBy = "date_desc";
                applyFilter();
            });
        }

        // Amount min/max
        if (criteria.minAmount != null || criteria.maxAmount != null) {
            String amtStr = "Amount: ";
            if (criteria.minAmount != null && criteria.maxAmount != null) {
                amtStr += CurrencyHelper.format(getContext(), criteria.minAmount) + " - " + CurrencyHelper.format(getContext(), criteria.maxAmount);
            } else if (criteria.minAmount != null) {
                amtStr += ">= " + CurrencyHelper.format(getContext(), criteria.minAmount);
            } else {
                amtStr += "<= " + CurrencyHelper.format(getContext(), criteria.maxAmount);
            }
            addFilterChip(amtStr, () -> {
                criteria.minAmount = null;
                criteria.maxAmount = null;
                applyFilter();
            });
        }

        // Clear All Chip
        TextView tvClear = new TextView(requireContext());
        tvClear.setText("Clear All ✕");
        tvClear.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        tvClear.setTextSize(12f);
        tvClear.setTypeface(Typeface.DEFAULT_BOLD);
        tvClear.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
        tvClear.setOnClickListener(v -> {
            criteria.reset();
            applyFilter();
        });
        layoutActiveFilterChips.addView(tvClear);
    }

    private void addFilterChip(String text, Runnable onRemove) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout chip = new LinearLayout(requireContext());
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd((int) (6 * density));
        chip.setLayoutParams(params);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(12 * density);
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.card_dark));
        bg.setStroke((int) (1 * density), ContextCompat.getColor(requireContext(), R.color.purple_light));
        chip.setBackground(bg);

        TextView tv = new TextView(requireContext());
        tv.setText(text + " ✕");
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tv.setTextSize(11f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        chip.addView(tv);

        chip.setOnClickListener(v -> onRemove.run());
        layoutActiveFilterChips.addView(chip);
    }

    private int compareDates(String d1, String d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        String[] p1 = d1.trim().split("/");
        String[] p2 = d2.trim().split("/");
        if (p1.length == 3 && p2.length == 3) {
            try {
                int y1 = Integer.parseInt(p1[2]), y2 = Integer.parseInt(p2[2]);
                if (y1 != y2) return Integer.compare(y1, y2);
                int m1 = Integer.parseInt(p1[1]), m2 = Integer.parseInt(p2[1]);
                if (m1 != m2) return Integer.compare(m1, m2);
                int day1 = Integer.parseInt(p1[0]), day2 = Integer.parseInt(p2[0]);
                return Integer.compare(day1, day2);
            } catch (Exception ignored) {}
        }
        return d1.compareTo(d2);
    }
}