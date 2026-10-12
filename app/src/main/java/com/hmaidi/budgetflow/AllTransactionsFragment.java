package com.hmaidi.budgetflow;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class AllTransactionsFragment extends Fragment {

    private ExpenseAdapter adapter;
    private ExpenseViewModel viewModel;
    private List<ExpenseModel> allTransactions = new ArrayList<>();
    private String currentFilter = "all";
    private String searchQuery = "";
    private TextView tvResultsCount;
    private LinearLayout emptyState;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_transactions,
                container, false);

        tvResultsCount = view.findViewById(R.id.tv_results_count);
        emptyState = view.findViewById(R.id.empty_filter_state);
        recyclerView = view.findViewById(R.id.rv_all_transactions);

        // Back button
        ImageView btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

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
                searchQuery = s.toString().toLowerCase();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter buttons
        int[] filterIds = {R.id.filter_all, R.id.filter_expense, R.id.filter_income,
                R.id.filter_food, R.id.filter_transport, R.id.filter_shopping,
                R.id.filter_health, R.id.filter_entertainment};
        String[] filterValues = {"all", "expense", "income",
                "Food", "Transport", "Shopping", "Health", "Entertainment"};

        for (int i = 0; i < filterIds.length; i++) {
            Button btn = view.findViewById(filterIds[i]);
            String filter = filterValues[i];
            btn.setOnClickListener(v -> {
                currentFilter = filter;
                updateFilterButtons(view, filterIds, filterValues, filter);
                applyFilter();
            });
        }

        // Observe all transactions
        viewModel = new ViewModelProvider(requireActivity())
                .get(ExpenseViewModel.class);
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allTransactions = expenses != null ? expenses : new ArrayList<>();
            applyFilter();
        });

        return view;
    }

    private void applyFilter() {
        List<ExpenseModel> filtered = new ArrayList<>();

        for (ExpenseModel e : allTransactions) {
            // Apply type/category filter
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all": matchesFilter = true; break;
                case "expense": matchesFilter = e.isExpense(); break;
                case "income": matchesFilter = !e.isExpense(); break;
                default: matchesFilter = currentFilter.equals(e.getCategory()); break;
            }

            // Apply search filter
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (e.getTitle() != null && e.getTitle().toLowerCase().contains(searchQuery)) ||
                    (e.getCategory() != null && e.getCategory().toLowerCase().contains(searchQuery)) ||
                    (e.getDate() != null && e.getDate().contains(searchQuery));

            if (matchesFilter && matchesSearch) {
                filtered.add(e);
            }
        }

        adapter.updateList(filtered);
        tvResultsCount.setText(filtered.size() + " transactions");

        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void updateFilterButtons(View root, int[] ids,
                                     String[] values, String selected) {
        for (int i = 0; i < ids.length; i++) {
            Button btn = root.findViewById(ids[i]);
            if (values[i].equals(selected)) {
                btn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(getContext(), R.color.purple_main)));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(getContext(), R.color.card_dark)));
                btn.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
        }
    }
}