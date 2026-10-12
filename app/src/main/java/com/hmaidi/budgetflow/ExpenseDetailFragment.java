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
        Button btnEdit = view.findViewById(R.id.btn_edit);
        Button btnHistory = view.findViewById(R.id.btn_history);
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

        btnEdit.setOnClickListener(v -> showEditDialog());

        btnHistory.setOnClickListener(v -> showHistoryDialog());

        btnDelete.setOnClickListener(v -> {
            ExpenseViewModel viewModel = new ViewModelProvider(requireActivity())
                    .get(ExpenseViewModel.class);
            viewModel.delete(expense);
            android.widget.Toast.makeText(getContext(), "Moved to Trash Bin", android.widget.Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return view;
    }

    private void showEditDialog() {
        if (getContext() == null) return;
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        android.widget.EditText etTitle = new android.widget.EditText(getContext());
        etTitle.setHint("Title");
        etTitle.setText(expense.getTitle());
        etTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        layout.addView(etTitle);

        android.widget.EditText etAmount = new android.widget.EditText(getContext());
        etAmount.setHint("Amount");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setText(String.valueOf(expense.getAmount()));
        etAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        layout.addView(etAmount);

        android.widget.EditText etCategory = new android.widget.EditText(getContext());
        etCategory.setHint("Category");
        etCategory.setText(expense.getCategory());
        etCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        layout.addView(etCategory);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Transaction")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String cat = etCategory.getText().toString().trim();
                    if (!title.isEmpty() && !amtStr.isEmpty()) {
                        try {
                            double newAmt = Double.parseDouble(amtStr);
                            expense.setTitle(title);
                            expense.setAmount(newAmt);
                            if (!cat.isEmpty()) {
                                expense.setCategory(cat);
                            }
                            ExpenseViewModel viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
                            viewModel.update(expense);
                            android.widget.Toast.makeText(getContext(), "Transaction updated!", android.widget.Toast.LENGTH_SHORT).show();
                            dismiss();
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHistoryDialog() {
        if (getContext() == null) return;
        ExpenseViewModel viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        viewModel.getHistoryForTransaction(expense.getId()).observe(getViewLifecycleOwner(), histories -> {
            if (histories == null || histories.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Modification History")
                        .setMessage("No previous modifications recorded for this transaction.")
                        .setPositiveButton("Close", null)
                        .show();
                return;
            }

            String[] items = new String[histories.size()];
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            for (int i = 0; i < histories.size(); i++) {
                TransactionHistory h = histories.get(i);
                String date = sdf.format(new java.util.Date(h.getTimestamp()));
                items[i] = h.getActionType() + " at " + date;
            }

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("History & Reversion")
                    .setItems(items, (dialog, which) -> {
                        TransactionHistory selectedHistory = histories.get(which);
                        if (selectedHistory.getPreviousStateJson() != null && !selectedHistory.getPreviousStateJson().isEmpty()) {
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Revert Change")
                                    .setMessage("Revert this transaction to its state before this edit?")
                                    .setPositiveButton("Revert", (d2, w2) -> {
                                        viewModel.revertHistoryChange(selectedHistory);
                                        android.widget.Toast.makeText(getContext(), "Reverted to previous state!", android.widget.Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            android.widget.Toast.makeText(getContext(), "No earlier state available for this record.", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });
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