package com.hmaidi.budgetflow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<ExpenseModel> expenseList;
    private Context context;
    private OnExpenseClickListener listener;

    public ExpenseAdapter(Context context, List<ExpenseModel> expenseList,
                          OnExpenseClickListener listener) {
        this.context = context;
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @Override
    public ExpenseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        ExpenseModel expense = expenseList.get(position);

        holder.tvTitle.setText(expense.getTitle());
        holder.tvDate.setText(expense.getDate());

        // Set category icon
        holder.ivCategory.setImageResource(getCategoryIcon(expense.getCategory()));

        if (expense.isExpense()) {
            holder.tvAmount.setText(
                    CurrencyHelper.formatSigned(context, expense.getAmount(), true));
            holder.tvAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.expense_red));
        } else {
            holder.tvAmount.setText(
                    CurrencyHelper.formatSigned(context, expense.getAmount(), false));
            holder.tvAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.income_green));
        }

        // Show recurring badge
        if (expense.isRecurring()) {
            holder.tvRecurringBadge.setVisibility(View.VISIBLE);
            holder.tvRecurringBadge.setText("🔄 " + expense.getRecurringInterval());
        } else {
            holder.tvRecurringBadge.setVisibility(View.GONE);
        }

        // Show receipt indicator
        if (holder.ivReceiptIndicator != null) {
            boolean hasReceipt = expense.getReceiptPath() != null && !expense.getReceiptPath().trim().isEmpty();
            holder.ivReceiptIndicator.setVisibility(hasReceipt ? View.VISIBLE : View.GONE);
        }

        // Show tags
        if (holder.tvItemTags != null) {
            String tags = expense.getTags();
            if (tags != null && !tags.trim().isEmpty()) {
                holder.tvItemTags.setVisibility(View.VISIBLE);
                holder.tvItemTags.setText(tags.trim());
            } else {
                holder.tvItemTags.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvAmount, tvRecurringBadge, tvItemTags;
        ImageView ivCategory, ivReceiptIndicator;

        public ExpenseViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvRecurringBadge = itemView.findViewById(R.id.tv_recurring_badge);
            tvItemTags = itemView.findViewById(R.id.tv_item_tags);
            ivCategory = itemView.findViewById(R.id.iv_category);
            ivReceiptIndicator = itemView.findViewById(R.id.iv_receipt_indicator);
        }
    }

    public void updateList(List<ExpenseModel> newList) {
        expenseList = newList;
        notifyDataSetChanged();
    }

    public interface OnExpenseClickListener {
        void onExpenseClick(ExpenseModel expense);
    }

    private int getCategoryIcon(String category) {
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
}