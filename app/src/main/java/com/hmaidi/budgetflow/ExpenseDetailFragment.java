package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ExpenseDetailFragment extends BottomSheetDialogFragment {

    private ExpenseModel expense;

    public static ExpenseDetailFragment newInstance(ExpenseModel expense) {
        ExpenseDetailFragment fragment = new ExpenseDetailFragment();
        Bundle args = new Bundle();
        args.putInt("id", expense.getId());
        args.putString("title", expense.getTitle());
        args.putString("date", expense.getDate());
        args.putDouble("amount", expense.getAmount());
        args.putString("category", expense.getCategory());
        args.putBoolean("isExpense", expense.isExpense());
        args.putInt("walletId", expense.getWalletId());
        args.putBoolean("isRecurring", expense.isRecurring());
        args.putString("recurringInterval", expense.getRecurringInterval());
        args.putLong("nextDueDate", expense.getNextDueDate());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense_detail, container, false);

        Bundle args = getArguments();
        expense = new ExpenseModel(
                args.getString("title"),
                args.getString("date"),
                args.getDouble("amount"),
                args.getString("category"),
                args.getBoolean("isExpense"),
                args.getInt("walletId")
        );
        expense.setId(args.getInt("id"));
        expense.setRecurring(args.getBoolean("isRecurring", false));
        expense.setRecurringInterval(args.getString("recurringInterval", ""));
        expense.setNextDueDate(args.getLong("nextDueDate", 0));

        ImageView ivDetailIcon = view.findViewById(R.id.iv_detail_icon);
        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        TextView tvCategory = view.findViewById(R.id.tv_detail_category);
        TextView tvAmount = view.findViewById(R.id.tv_detail_amount);
        TextView tvDate = view.findViewById(R.id.tv_detail_date);
        TextView tvType = view.findViewById(R.id.tv_detail_type);
        View rowRecurring = view.findViewById(R.id.row_detail_recurring);
        TextView tvRecurring = view.findViewById(R.id.tv_detail_recurring);
        Button btnDelete = view.findViewById(R.id.btn_delete);

        ivDetailIcon.setImageResource(getCategoryIcon(expense.getCategory(), expense.isExpense()));
        tvTitle.setText(expense.getTitle());
        tvCategory.setText(expense.getCategory() != null && !expense.getCategory().isEmpty() ? expense.getCategory() : (expense.isExpense() ? "General" : "Income"));
        tvDate.setText(expense.getDate());

        if (expense.isRecurring()) {
            rowRecurring.setVisibility(View.VISIBLE);
            String interval = expense.getRecurringInterval();
            String intervalCap = interval != null && !interval.isEmpty()
                    ? interval.substring(0, 1).toUpperCase() + interval.substring(1)
                    : "Recurring";
            if (expense.getNextDueDate() > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                String nextDate = sdf.format(new java.util.Date(expense.getNextDueDate()));
                tvRecurring.setText("🔄 " + intervalCap + " (Next: " + nextDate + ")");
            } else {
                tvRecurring.setText("🔄 " + intervalCap);
            }
        } else {
            rowRecurring.setVisibility(View.GONE);
        }

        if (expense.isExpense()) {
            tvAmount.setText(
                    CurrencyHelper.formatSigned(getContext(), expense.getAmount(), true));
            tvAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.expense_red));
            tvType.setText("Expense");
            tvType.setTextColor(ContextCompat.getColor(getContext(), R.color.expense_red));
        } else {
            tvAmount.setText(
                    CurrencyHelper.formatSigned(getContext(), expense.getAmount(), false));
            tvAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.income_green));
            tvType.setText("Income");
            tvType.setTextColor(ContextCompat.getColor(getContext(), R.color.income_green));
        }

        btnDelete.setOnClickListener(v -> {
            ExpenseViewModel viewModel = new ViewModelProvider(requireActivity())
                    .get(ExpenseViewModel.class);
            viewModel.delete(expense);
            dismiss();
        });

        return view;
    }

    private int getCategoryIcon(String category, boolean isExpense) {
        if (!isExpense) return R.drawable.ic_wallet;
        if (category == null) return R.drawable.ic_wallet;
        switch (category) {
            case "Food": return R.drawable.ic_food;
            case "Transport": return R.drawable.ic_transport;
            case "Shopping": return R.drawable.ic_shopping;
            case "Health": return R.drawable.ic_health;
            case "Entertainment": return R.drawable.ic_entertainment;
            default: return R.drawable.ic_wallet;
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }
}