package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // Category Budgets
    private CategoryBudgetViewModel categoryBudgetViewModel;
    private CategoryViewModel categoryViewModel;
    private CardView cardCategoryBudgetEmpty;
    private LinearLayout layoutCategoryBudgetItems;
    private List<CategoryBudget> currentCategoryBudgets = new ArrayList<>();
    private List<CategoryModel> availableCategories = new ArrayList<>();

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

        // Category Budgets Setup
        cardCategoryBudgetEmpty = view.findViewById(R.id.card_category_budget_empty);
        layoutCategoryBudgetItems = view.findViewById(R.id.layout_category_budget_items);
        TextView tvManageCategoryBudgets = view.findViewById(R.id.tv_manage_category_budgets);

        if (cardCategoryBudgetEmpty != null) {
            cardCategoryBudgetEmpty.setOnClickListener(v -> showManageCategoryBudgetsDialog());
        }
        if (tvManageCategoryBudgets != null) {
            tvManageCategoryBudgets.setOnClickListener(v -> showManageCategoryBudgetsDialog());
        }

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

        // See All button & Search button
        TextView tvSeeAll = view.findViewById(R.id.tv_see_all);
        View.OnClickListener openAllAction = v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AllTransactionsFragment())
                        .addToBackStack(null)
                        .commit();

        tvSeeAll.setOnClickListener(openAllAction);
        ImageView btnHomeSearch = view.findViewById(R.id.btn_home_search);
        if (btnHomeSearch != null) {
            btnHomeSearch.setOnClickListener(openAllAction);
        }

        // Recurring Subscriptions Banner
        androidx.cardview.widget.CardView cardHomeRecurring = view.findViewById(R.id.card_home_recurring_banner);
        TextView tvHomeRecurringSummary = view.findViewById(R.id.tv_home_recurring_summary);

        if (cardHomeRecurring != null) {
            cardHomeRecurring.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new RecurringBillsFragment())
                            .addToBackStack(null)
                            .commit()
            );
        }

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
            updateCategoryBudgets();
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

        // Observe Category Budgets
        categoryBudgetViewModel = new ViewModelProvider(requireActivity()).get(CategoryBudgetViewModel.class);
        categoryBudgetViewModel.getAllCategoryBudgets().observe(getViewLifecycleOwner(), budgets -> {
            currentCategoryBudgets = budgets != null ? budgets : new ArrayList<>();
            updateCategoryBudgets();
        });

        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            availableCategories = categories != null ? categories : new ArrayList<>();
        });

        // Observe recurring bills for Home banner
        viewModel.getAllRecurringExpenses().observe(getViewLifecycleOwner(), items -> {
            if (cardHomeRecurring == null || tvHomeRecurringSummary == null || getContext() == null) return;
            if (items == null || items.isEmpty()) {
                cardHomeRecurring.setVisibility(View.GONE);
            } else {
                cardHomeRecurring.setVisibility(View.VISIBLE);
                double monthlySum = 0;
                for (ExpenseModel item : items) {
                    if (item.isExpense()) {
                        String interval = item.getRecurringInterval() != null ? item.getRecurringInterval().toLowerCase() : "monthly";
                        if ("daily".equals(interval)) monthlySum += item.getAmount() * 30;
                        else if ("weekly".equals(interval)) monthlySum += item.getAmount() * 4.33;
                        else if ("yearly".equals(interval)) monthlySum += item.getAmount() / 12;
                        else monthlySum += item.getAmount();
                    }
                }
                tvHomeRecurringSummary.setText(items.size() + (items.size() == 1 ? " Subscription • " : " Subscriptions • ") + CurrencyHelper.format(getContext(), monthlySum) + "/mo");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBudgetProgress();
        updateCategoryBudgets();
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
            if (e.isExpense()
                    && !"Transfer".equalsIgnoreCase(e.getCategory())
                    && isDateInMonth(e.getDate(), currentMonth, currentYear)) {
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
                            SavingsGoal goal = new SavingsGoal(title, target, 0, "ic_savings", "#6366F1", "");
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

    private void updateCategoryBudgets() {
        if (getContext() == null || layoutCategoryBudgetItems == null || cardCategoryBudgetEmpty == null) return;
        layoutCategoryBudgetItems.removeAllViews();

        if (currentCategoryBudgets.isEmpty()) {
            cardCategoryBudgetEmpty.setVisibility(View.VISIBLE);
            layoutCategoryBudgetItems.setVisibility(View.GONE);
            return;
        }

        cardCategoryBudgetEmpty.setVisibility(View.GONE);
        layoutCategoryBudgetItems.setVisibility(View.VISIBLE);

        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);

        Map<String, Double> spentMap = new HashMap<>();
        for (ExpenseModel e : currentExpensesList) {
            if (e.isExpense()
                    && !"Transfer".equalsIgnoreCase(e.getCategory())
                    && isDateInMonth(e.getDate(), currentMonth, currentYear)) {
                String cat = e.getCategory() != null ? e.getCategory() : "Uncategorized";
                spentMap.put(cat, spentMap.getOrDefault(cat, 0.0) + e.getAmount());
            }
        }

        float density = getResources().getDisplayMetrics().density;

        for (CategoryBudget cb : currentCategoryBudgets) {
            double spent = spentMap.getOrDefault(cb.getCategoryName(), 0.0);
            double limit = cb.getMonthlyLimit();
            int percent = limit > 0 ? (int) Math.round((spent / limit) * 100) : 0;
            double remaining = limit - spent;

            CardView card = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = (int) (10 * density);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * density);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_dark));
            card.setCardElevation(0f);
            card.setClickable(true);
            card.setFocusable(true);

            android.util.TypedValue outValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            card.setForeground(ContextCompat.getDrawable(requireContext(), outValue.resourceId));

            card.setOnClickListener(v -> showSetCategoryBudgetDialog(cb.getCategoryName(), cb.getMonthlyLimit()));

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (14 * density);
            inner.setPadding(pad, pad, pad, pad);

            // Top row: Category name & percentage badge
            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(requireContext());
            tvName.setText(cb.getCategoryName());
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvName.setTextSize(14f);
            tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameParams);

            TextView tvPct = new TextView(requireContext());
            tvPct.setText(percent + "%");
            tvPct.setTextSize(13f);
            tvPct.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            int statusColor;
            if (percent >= 100) {
                statusColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
            } else if (percent >= 75) {
                statusColor = Color.parseColor("#F59E0B");
            } else {
                statusColor = ContextCompat.getColor(requireContext(), R.color.income_green);
            }
            tvPct.setTextColor(statusColor);

            topRow.addView(tvName);
            topRow.addView(tvPct);

            // Progress bar
            ProgressBar pb = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (8 * density));
            pbParams.topMargin = (int) (8 * density);
            pbParams.bottomMargin = (int) (8 * density);
            pb.setLayoutParams(pbParams);
            pb.setMax(100);
            pb.setProgress(Math.min(100, Math.max(0, percent)));
            pb.setProgressTintList(ColorStateList.valueOf(statusColor));
            pb.setProgressBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.input_bg)));

            // Bottom info row: Spent vs limit + left/exceeded
            LinearLayout bottomRow = new LinearLayout(requireContext());
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvSpent = new TextView(requireContext());
            tvSpent.setText("Spent: " + CurrencyHelper.format(getContext(), spent) + " / " + CurrencyHelper.format(getContext(), limit));
            tvSpent.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvSpent.setTextSize(12f);
            LinearLayout.LayoutParams spentParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvSpent.setLayoutParams(spentParams);

            TextView tvRem = new TextView(requireContext());
            if (remaining >= 0) {
                tvRem.setText(CurrencyHelper.format(getContext(), remaining) + " left");
                tvRem.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
            } else {
                tvRem.setText("Over by " + CurrencyHelper.format(getContext(), Math.abs(remaining)));
                tvRem.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
            }
            tvRem.setTextSize(12f);
            tvRem.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            bottomRow.addView(tvSpent);
            bottomRow.addView(tvRem);

            inner.addView(topRow);
            inner.addView(pb);
            inner.addView(bottomRow);
            card.addView(inner);

            layoutCategoryBudgetItems.addView(card);
        }
    }

    private void showManageCategoryBudgetsDialog() {
        if (getContext() == null) return;

        Set<String> categorySet = new LinkedHashSet<>();

        // Add from current expense categories in DB
        if (availableCategories != null) {
            for (CategoryModel c : availableCategories) {
                if (c.isExpense() && c.getName() != null && !c.getName().trim().isEmpty() && !"Transfer".equalsIgnoreCase(c.getName())) {
                    categorySet.add(c.getName().trim());
                }
            }
        }

        // Add from existing transactions
        for (ExpenseModel e : currentExpensesList) {
            if (e.isExpense() && e.getCategory() != null && !e.getCategory().trim().isEmpty() && !"Transfer".equalsIgnoreCase(e.getCategory())) {
                categorySet.add(e.getCategory().trim());
            }
        }

        // Add standard categories if list is short
        String[] defaults = {"Food", "Transport", "Shopping", "Entertainment", "Bills", "Health", "Education", "Travel", "Groceries"};
        for (String d : defaults) {
            categorySet.add(d);
        }

        Map<String, Double> currentLimits = new HashMap<>();
        for (CategoryBudget cb : currentCategoryBudgets) {
            currentLimits.put(cb.getCategoryName(), cb.getMonthlyLimit());
            categorySet.add(cb.getCategoryName());
        }

        List<String> sortedCats = new ArrayList<>(categorySet);
        java.util.Collections.sort(sortedCats, String.CASE_INSENSITIVE_ORDER);

        String[] displayItems = new String[sortedCats.size()];
        for (int i = 0; i < sortedCats.size(); i++) {
            String name = sortedCats.get(i);
            if (currentLimits.containsKey(name)) {
                displayItems[i] = name + "  •  Limit: " + CurrencyHelper.format(getContext(), currentLimits.get(name));
            } else {
                displayItems[i] = name;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Category")
                .setItems(displayItems, (dialog, which) -> {
                    String selectedCat = sortedCats.get(which);
                    double existingLimit = currentLimits.getOrDefault(selectedCat, 0.0);
                    showSetCategoryBudgetDialog(selectedCat, existingLimit);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSetCategoryBudgetDialog(String categoryName, double currentLimit) {
        if (getContext() == null) return;

        EditText etLimit = new EditText(getContext());
        etLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etLimit.setHint("Monthly limit in " + CurrencyHelper.getCode(getContext()));
        etLimit.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        if (currentLimit > 0) {
            etLimit.setText(String.valueOf(currentLimit));
        }

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin / 2, margin, 0);
        etLimit.setLayoutParams(params);
        container.addView(etLimit);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(categoryName + " Budget Limit")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = etLimit.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            double limit = Double.parseDouble(val);
                            if (limit > 0) {
                                categoryBudgetViewModel.insertOrUpdate(new CategoryBudget(categoryName, limit));
                                Toast.makeText(getContext(), categoryName + " budget set!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .setNegativeButton("Cancel", null);

        if (currentLimit > 0) {
            builder.setNeutralButton("Remove", (dialog, which) -> {
                categoryBudgetViewModel.deleteByName(categoryName);
                Toast.makeText(getContext(), categoryName + " budget removed", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }
}