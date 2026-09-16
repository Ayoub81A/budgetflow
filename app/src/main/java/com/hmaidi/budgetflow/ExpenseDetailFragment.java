package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        args.putInt("walletId", expense.getWalletId()); // ← added
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
                args.getInt("walletId") // ← added
        );
        expense.setId(args.getInt("id"));

        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        TextView tvCategory = view.findViewById(R.id.tv_detail_category);
        TextView tvAmount = view.findViewById(R.id.tv_detail_amount);
        TextView tvDate = view.findViewById(R.id.tv_detail_date);
        TextView tvType = view.findViewById(R.id.tv_detail_type);
        Button btnDelete = view.findViewById(R.id.btn_delete);

        tvTitle.setText(expense.getTitle());
        tvCategory.setText(expense.getCategory());
        tvDate.setText(expense.getDate());

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

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }
}