package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    ExpenseAdapter adapter;
    ExpenseViewModel viewModel;
    TextView tvBalance, tvIncome, tvExpense;
    LinearLayout emptyState;
    private double totalIncomeAmount = 0;
    private double totalExpenseAmount = 0;

    // Budget Tracker Views
    private CardView cardBudgetTracker;
    private LinearLayout layoutBudgetActive, layoutBudgetEmpty;
    private TextView tvBudgetPercent, tvBudgetSpentInfo, tvBudgetRemaining;
    private ProgressBar pbBudget;
    private List<ExpenseModel> currentExpensesList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rv_expenses);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        emptyState = view.findViewById(R.id.empty_state);

        // Budget Tracker Views
        cardBudgetTracker = view.findViewById(R.id.card_budget_tracker);
        layoutBudgetActive = view.findViewById(R.id.layout_budget_active);
        layoutBudgetEmpty = view.findViewById(R.id.layout_budget_empty);
        tvBudgetPercent = view.findViewById(R.id.tv_budget_percent);
        tvBudgetSpentInfo = view.findViewById(R.id.tv_budget_spent_info);
        tvBudgetRemaining = view.findViewById(R.id.tv_budget_remaining);
        pbBudget = view.findViewById(R.id.pb_budget);

        cardBudgetTracker.setOnClickListener(v -> showBudgetDialog());

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
            currentExpensesList = expenses != null ? expenses : new ArrayList<>();
            adapter.updateList(currentExpensesList);
            if (currentExpensesList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
            }
            updateBudgetProgress();
        });

        // Observe total expenses
        viewModel.getTotalExpenses().observe(getViewLifecycleOwner(), total -> {
            totalExpenseAmount = total != null ? total : 0;
            tvExpense.setText(CurrencyHelper.format(getContext(), totalExpenseAmount));
            updateBalance();
            updateBudgetProgress();
        });

        // Observe total income
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            totalIncomeAmount = total != null ? total : 0;
            tvIncome.setText(CurrencyHelper.format(getContext(), totalIncomeAmount));
            updateBalance();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBudgetProgress();
    }

    private void updateBalance() {
        double balance = totalIncomeAmount - totalExpenseAmount;
        tvBalance.setText(CurrencyHelper.format(getContext(), balance));
    }

    private void updateBudgetProgress() {
        if (getContext() == null || pbBudget == null) return;
        double budget = CurrencyHelper.getBudget(getContext());
        if (budget <= 0) {
            layoutBudgetActive.setVisibility(View.GONE);
            layoutBudgetEmpty.setVisibility(View.VISIBLE);
            return;
        }

        layoutBudgetActive.setVisibility(View.VISIBLE);
        layoutBudgetEmpty.setVisibility(View.GONE);

        double thisMonthSpent = calculateThisMonthExpenses(currentExpensesList);
        double remaining = budget - thisMonthSpent;
        int percent = (int) Math.round((thisMonthSpent / budget) * 100);

        tvBudgetPercent.setText(percent + "%");
        tvBudgetSpentInfo.setText(String.format("Spent: %s / %s",
                CurrencyHelper.format(getContext(), thisMonthSpent),
                CurrencyHelper.format(getContext(), budget)));

        if (remaining >= 0) {
            tvBudgetRemaining.setText(CurrencyHelper.format(getContext(), remaining) + " left");
            tvBudgetRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            tvBudgetRemaining.setText("Exceeded by " + CurrencyHelper.format(getContext(), Math.abs(remaining)));
            tvBudgetRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        pbBudget.setProgress(Math.min(100, Math.max(0, percent)));

        int progressColor;
        if (percent >= 100) {
            progressColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
            tvBudgetPercent.setTextColor(progressColor);
        } else if (percent >= 75) {
            progressColor = Color.parseColor("#FFA000"); // Amber warning
            tvBudgetPercent.setTextColor(progressColor);
        } else {
            progressColor = ContextCompat.getColor(requireContext(), R.color.purple_main);
            tvBudgetPercent.setTextColor(progressColor);
        }
        pbBudget.setProgressTintList(ColorStateList.valueOf(progressColor));
    }

    private double calculateThisMonthExpenses(List<ExpenseModel> expenses) {
        if (expenses == null) return 0;
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);

        double sum = 0;
        for (ExpenseModel e : expenses) {
            if (e.isExpense() && isDateInMonth(e.getDate(), currentMonth, currentYear)) {
                sum += e.getAmount();
            }
        }
        return sum;
    }

    private boolean isDateInMonth(String dateStr, int targetMonth, int targetYear) {
        if (dateStr == null) return false;
        String[] parts = dateStr.trim().split("/");
        if (parts.length == 3) {
            try {
                int m = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                return m == targetMonth && y == targetYear;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    private void showBudgetDialog() {
        EditText etBudget = new EditText(getContext());
        etBudget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        double current = CurrencyHelper.getBudget(getContext());
        if (current > 0) etBudget.setText(String.valueOf(current));
        etBudget.setHint("Enter monthly budget");
        etBudget.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        FrameLayout dialogContainer = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin / 2, margin, 0);
        etBudget.setLayoutParams(params);
        dialogContainer.addView(etBudget);

        new AlertDialog.Builder(requireContext())
                .setTitle("Monthly Budget Limit")
                .setView(dialogContainer)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = etBudget.getText().toString().trim();
                    if (!val.isEmpty()) {
                        CurrencyHelper.saveBudget(getContext(), Double.parseDouble(val));
                        updateBudgetProgress();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}