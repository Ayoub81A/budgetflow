package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecurringBillsFragment extends Fragment implements RecurringBillAdapter.RecurringActionListener {

    private ExpenseViewModel expenseViewModel;
    private WalletViewModel walletViewModel;
    private RecurringBillAdapter adapter;
    private List<ExpenseModel> recurringList = new ArrayList<>();
    private Map<Integer, String> walletMap = new HashMap<>();

    private TextView tvTotalMonthly, tvActiveCount, tvYearlyProjection;
    private CardView cardUpcomingAlert;
    private TextView tvUpcomingText;
    private LinearLayout emptyState;
    private RecyclerView rvRecurring;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recurring_bills, container, false);

        // Header
        ImageView btnBack = view.findViewById(R.id.btn_back_recurring);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        TextView btnAdd = view.findViewById(R.id.btn_add_recurring_bill);
        btnAdd.setOnClickListener(v -> openAddRecurring());

        // Views
        tvTotalMonthly = view.findViewById(R.id.tv_recurring_total_monthly);
        tvActiveCount = view.findViewById(R.id.tv_recurring_active_count);
        tvYearlyProjection = view.findViewById(R.id.tv_recurring_yearly_projection);
        cardUpcomingAlert = view.findViewById(R.id.card_upcoming_bill_alert);
        tvUpcomingText = view.findViewById(R.id.tv_upcoming_bill_text);
        emptyState = view.findViewById(R.id.empty_recurring_state);
        rvRecurring = view.findViewById(R.id.rv_recurring_bills);

        MaterialButton btnEmptyAdd = view.findViewById(R.id.btn_empty_add_recurring);
        btnEmptyAdd.setOnClickListener(v -> openAddRecurring());

        // Setup Recycler
        adapter = new RecurringBillAdapter(getContext(), new ArrayList<>(), this);
        rvRecurring.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecurring.setAdapter(adapter);

        // ViewModels
        walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);
        walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            walletMap.clear();
            if (wallets != null) {
                for (Wallet w : wallets) {
                    walletMap.put(w.getId(), w.getName());
                }
            }
            if (adapter != null) {
                adapter.updateData(recurringList, walletMap);
            }
        });

        expenseViewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        expenseViewModel.getAllRecurringExpenses().observe(getViewLifecycleOwner(), items -> {
            recurringList = items != null ? items : new ArrayList<>();
            updateUi();
        });

        return view;
    }

    private void updateUi() {
        if (getContext() == null) return;

        adapter.updateData(recurringList, walletMap);

        if (recurringList.isEmpty()) {
            rvRecurring.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            cardUpcomingAlert.setVisibility(View.GONE);
            tvTotalMonthly.setText(CurrencyHelper.format(getContext(), 0) + " / mo");
            tvActiveCount.setText("0 Active Subscriptions");
            tvYearlyProjection.setText("Est. " + CurrencyHelper.format(getContext(), 0) + " / yr");
            return;
        }

        rvRecurring.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        // Calculate totals
        double monthlySum = 0.0;
        long now = System.currentTimeMillis();
        ExpenseModel nearestDueItem = null;
        long nearestDueDiff = Long.MAX_VALUE;

        for (ExpenseModel item : recurringList) {
            double amt = item.getAmount();
            String interval = item.getRecurringInterval() != null ? item.getRecurringInterval().toLowerCase() : "monthly";

            if (item.isExpense()) {
                switch (interval) {
                    case "daily":
                        monthlySum += amt * 30.0;
                        break;
                    case "weekly":
                        monthlySum += amt * 4.33;
                        break;
                    case "yearly":
                        monthlySum += amt / 12.0;
                        break;
                    case "monthly":
                    default:
                        monthlySum += amt;
                        break;
                }
            }

            long due = item.getNextDueDate();
            if (due > 0) {
                long diff = due - now;
                if (diff < nearestDueDiff) {
                    nearestDueDiff = diff;
                    nearestDueItem = item;
                }
            }
        }

        tvTotalMonthly.setText(CurrencyHelper.format(getContext(), monthlySum) + " / mo");
        tvActiveCount.setText(recurringList.size() + (recurringList.size() == 1 ? " Active Bill" : " Active Bills"));
        tvYearlyProjection.setText("Est. " + CurrencyHelper.format(getContext(), monthlySum * 12) + " / yr");

        // Nearest due alert
        if (nearestDueItem != null) {
            long diffDays = nearestDueDiff / (1000 * 60 * 60 * 24);
            if (diffDays <= 7) {
                cardUpcomingAlert.setVisibility(View.VISIBLE);
                String dueNotice;
                if (diffDays < 0) {
                    dueNotice = "⚠️ Overdue: " + nearestDueItem.getTitle() + " (" + CurrencyHelper.format(getContext(), nearestDueItem.getAmount()) + ")";
                } else if (diffDays == 0) {
                    dueNotice = "⏰ Due Today: " + nearestDueItem.getTitle() + " (" + CurrencyHelper.format(getContext(), nearestDueItem.getAmount()) + ")";
                } else if (diffDays == 1) {
                    dueNotice = "Due Tomorrow: " + nearestDueItem.getTitle() + " (" + CurrencyHelper.format(getContext(), nearestDueItem.getAmount()) + ")";
                } else {
                    dueNotice = "Due in " + diffDays + " days: " + nearestDueItem.getTitle() + " (" + CurrencyHelper.format(getContext(), nearestDueItem.getAmount()) + ")";
                }
                tvUpcomingText.setText(dueNotice);
            } else {
                cardUpcomingAlert.setVisibility(View.GONE);
            }
        } else {
            cardUpcomingAlert.setVisibility(View.GONE);
        }
    }

    private void openAddRecurring() {
        // Navigate to AddExpenseFragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AddExpenseFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onPayNow(ExpenseModel item) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Process Payment")
                .setMessage("Record a payment of " + CurrencyHelper.format(getContext(), item.getAmount()) + " for \"" + item.getTitle() + "\" today?")
                .setPositiveButton("Confirm Payment", (dialog, which) -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String todayStr = sdf.format(new Date());

                    // Create new executed transaction
                    ExpenseModel executed = new ExpenseModel(
                            item.getTitle(),
                            todayStr,
                            item.getAmount(),
                            item.getCategory(),
                            item.isExpense(),
                            item.getWalletId()
                    );
                    executed.setRecurring(false); // executed instance is a normal logged expense
                    expenseViewModel.insert(executed);

                    // Advance the next due date on the recurring template
                    long nextDue = calculateNextDueDate(item.getNextDueDate() > 0 ? item.getNextDueDate() : System.currentTimeMillis(), item.getRecurringInterval());
                    item.setNextDueDate(nextDue);
                    expenseViewModel.update(item);

                    Toast.makeText(getContext(), "Payment recorded for " + item.getTitle(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onCancelRecurring(ExpenseModel item) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Stop Subscription")
                .setMessage("Are you sure you want to stop recurring payments for \"" + item.getTitle() + "\"? Past transactions will be kept.")
                .setPositiveButton("Stop Recurring", (dialog, which) -> {
                    item.setRecurring(false);
                    expenseViewModel.update(item);
                    Toast.makeText(getContext(), item.getTitle() + " subscription paused", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Keep Active", null)
                .show();
    }

    @Override
    public void onItemClick(ExpenseModel item) {
        ExpenseDetailFragment detail = ExpenseDetailFragment.newInstance(item);
        detail.show(getParentFragmentManager(), "ExpenseDetail");
    }

    private long calculateNextDueDate(long fromTimestamp, String interval) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(fromTimestamp);

        if (interval == null) interval = "monthly";
        switch (interval.toLowerCase()) {
            case "daily":
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case "weekly":
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "yearly":
                cal.add(Calendar.YEAR, 1);
                break;
            case "monthly":
            default:
                cal.add(Calendar.MONTH, 1);
                break;
        }
        return cal.getTimeInMillis();
    }
}
