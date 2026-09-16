package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    ExpenseAdapter adapter;
    ExpenseViewModel viewModel;
    TextView tvBalance, tvIncome, tvExpense;
    LinearLayout emptyState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rv_expenses);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        emptyState = view.findViewById(R.id.empty_state);

        // Dynamic greeting
        TextView tvGreeting = view.findViewById(R.id.tv_greeting);
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning 👋";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon 👋";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening 👋";
        } else {
            greeting = "Good Night 🌙";
        }
        tvGreeting.setText(greeting);

        // See All button
        TextView tvSeeAll = view.findViewById(R.id.tv_see_all);
        tvSeeAll.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AllTransactionsFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // Set up RecyclerView
        adapter = new ExpenseAdapter(getContext(), new ArrayList<>(), expense -> {
            ExpenseDetailFragment detail = ExpenseDetailFragment.newInstance(expense);
            detail.show(getParentFragmentManager(), "ExpenseDetail");
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Set up ViewModel
        viewModel = new ViewModelProvider(requireActivity())
                .get(ExpenseViewModel.class);

        // Observe expenses list
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.updateList(expenses);
            if (expenses == null || expenses.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
            }
        });

        // Observe total expenses
        viewModel.getTotalExpenses().observe(getViewLifecycleOwner(), total -> {
            double amount = total != null ? total : 0;
            tvExpense.setText(CurrencyHelper.format(getContext(), amount));
            updateBalance();

            // Budget warning
            double budget = CurrencyHelper.getBudget(getContext());
            if (budget > 0 && amount > budget) {
                tvExpense.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.expense_red));
                Toast.makeText(getContext(),
                        "⚠️ You exceeded your monthly budget!",
                        Toast.LENGTH_LONG).show();
            } else {
                tvExpense.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.text_primary));
            }
        });

        // Observe total income
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            double amount = total != null ? total : 0;
            tvIncome.setText(CurrencyHelper.format(getContext(), amount));
            updateBalance();
        });

        return view;
    }

    private void updateBalance() {
        try {
            String incomeStr = tvIncome.getText().toString()
                    .replaceAll("[^\\d.]", "");
            String expenseStr = tvExpense.getText().toString()
                    .replaceAll("[^\\d.]", "");

            double income = incomeStr.isEmpty() ? 0 : Double.parseDouble(incomeStr);
            double expense = expenseStr.isEmpty() ? 0 : Double.parseDouble(expenseStr);

            double balance = income - expense;
            tvBalance.setText(CurrencyHelper.format(getContext(), balance));
        } catch (NumberFormatException e) {
            tvBalance.setText(CurrencyHelper.format(getContext(), 0));
        }
    }
}