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

    // Savings Goals
    private RecyclerView rvSavingsGoals;
    private SavingsGoalAdapter goalsAdapter;
    private SavingsGoalViewModel goalsViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rv_expenses);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        emptyState = view.findViewById(R.id.empty_state);

        tvBalance.setText(CurrencyHelper.format(getContext(), 0));
        tvIncome.setText(CurrencyHelper.format(getContext(), 0));
        tvExpense.setText(CurrencyHelper.format(getContext(), 0));

        // Savings Goals setup
        rvSavingsGoals = view.findViewById(R.id.rv_savings_goals);
        TextView tvAddGoal = view.findViewById(R.id.tv_add_goal);
        goalsAdapter = new SavingsGoalAdapter(getContext(), new ArrayList<>(), goal -> showGoalActionDialog(goal));
        rvSavingsGoals.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSavingsGoals.setAdapter(goalsAdapter);
        if (tvAddGoal != null) {
            tvAddGoal.setOnClickListener(v -> showAddGoalDialog());
        }

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

        // Observe Savings Goals
        goalsViewModel = new ViewModelProvider(requireActivity()).get(SavingsGoalViewModel.class);
        goalsViewModel.getAllGoals().observe(getViewLifecycleOwner(), goals -> {
            goalsAdapter.updateList(goals != null ? goals : new ArrayList<>());
            if (goals == null || goals.isEmpty()) {
                rvSavingsGoals.setVisibility(View.GONE);
            } else {
                rvSavingsGoals.setVisibility(View.VISIBLE);
            }
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

        BudgetNotificationHelper.checkAndNotifyBudget(getContext(), thisMonthSpent, budget);
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

    private void showAddGoalDialog() {
        if (getContext() == null) return;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        EditText etTitle = new EditText(getContext());
        etTitle.setHint("Goal title (e.g. New Laptop)");
        etTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        layout.addView(etTitle);

        EditText etTarget = new EditText(getContext());
        etTarget.setHint("Target amount");
        etTarget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etTarget.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        layout.addView(etTarget);

        new AlertDialog.Builder(requireContext())
                .setTitle("Create Savings Goal")
                .setView(layout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String targetStr = etTarget.getText().toString().trim();
                    if (!title.isEmpty() && !targetStr.isEmpty()) {
                        try {
                            double target = Double.parseDouble(targetStr);
                            SavingsGoal goal = new SavingsGoal(title, target, 0, "ic_savings", "#7B5EA7", "");
                            goalsViewModel.insert(goal);
                            Toast.makeText(getContext(), "Goal created!", Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGoalActionDialog(SavingsGoal goal) {
        if (getContext() == null) return;
        String[] actions = {"Add Money to Goal", "Delete Goal"};
        new AlertDialog.Builder(requireContext())
                .setTitle(goal.getTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showAddFundsDialog(goal);
                    } else if (which == 1) {
                        goalsViewModel.delete(goal);
                        Toast.makeText(getContext(), "Goal deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAddFundsDialog(SavingsGoal goal) {
        if (getContext() == null) return;
        WalletViewModel walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);
        androidx.lifecycle.Observer<List<Wallet>> observer = new androidx.lifecycle.Observer<List<Wallet>>() {
            @Override
            public void onChanged(List<Wallet> wallets) {
                walletViewModel.getAllWallets().removeObserver(this);
                if (getContext() == null) return;
                if (wallets == null || wallets.isEmpty()) {
                    Toast.makeText(getContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDepositDialogWithWallets(goal, wallets);
            }
        };
        walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), observer);
    }

    private void showDepositDialogWithWallets(SavingsGoal goal, List<Wallet> wallets) {
        if (getContext() == null) return;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        EditText etAmount = new EditText(getContext());
        etAmount.setHint("Amount to deposit");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        layout.addView(etAmount);

        final int[] selectedWalletIndex = {0};
        TextView tvWallet = new TextView(getContext());
        tvWallet.setText("Deduct from: " + wallets.get(0).getName() + " (" + CurrencyHelper.format(getContext(), wallets.get(0).getBalance()) + ") ▾");
        tvWallet.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_light));
        tvWallet.setTextSize(13);
        tvWallet.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));

        String[] walletNames = new String[wallets.size()];
        for (int i = 0; i < wallets.size(); i++) {
            walletNames[i] = wallets.get(i).getName() + " (" + CurrencyHelper.format(getContext(), wallets.get(i).getBalance()) + ")";
        }

        tvWallet.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Source Wallet")
                    .setItems(walletNames, (d, which) -> {
                        selectedWalletIndex[0] = which;
                        tvWallet.setText("Deduct from: " + walletNames[which] + " ▾");
                    })
                    .show();
        });
        layout.addView(tvWallet);

        new AlertDialog.Builder(requireContext())
                .setTitle("Deposit to: " + goal.getTitle())
                .setView(layout)
                .setPositiveButton("Deposit", (dialog, which) -> {
                    String val = etAmount.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            double added = Double.parseDouble(val);
                            if (added <= 0) return;
                            Wallet chosenWallet = wallets.get(selectedWalletIndex[0]);

                            // 1. Add funds to savings goal
                            goalsViewModel.addFunds(goal.getId(), added);

                            // 2. Automatically record as an expense transaction and deduct from wallet
                            Calendar now = Calendar.getInstance();
                            String date = now.get(Calendar.DAY_OF_MONTH) + "/" + (now.get(Calendar.MONTH) + 1) + "/" + now.get(Calendar.YEAR);
                            ExpenseModel expense = new ExpenseModel(
                                    "Savings: " + goal.getTitle(),
                                    date,
                                    added,
                                    "Savings",
                                    true, // isExpense
                                    chosenWallet.getId()
                            );
                            viewModel.insert(expense);

                            Toast.makeText(getContext(), "Deposited " + CurrencyHelper.format(getContext(), added) + " from " + chosenWallet.getName() + "!", Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}