package com.hmaidi.budgetflow;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecurringBillAdapter extends RecyclerView.Adapter<RecurringBillAdapter.ViewHolder> {

    public interface RecurringActionListener {
        void onPayNow(ExpenseModel item);
        void onCancelRecurring(ExpenseModel item);
        void onItemClick(ExpenseModel item);
    }

    private final Context context;
    private List<ExpenseModel> items;
    private Map<Integer, String> walletNames = new HashMap<>();
    private final RecurringActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

    public RecurringBillAdapter(Context context, List<ExpenseModel> items, RecurringActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<ExpenseModel> newItems, Map<Integer, String> newWallets) {
        this.items = newItems;
        if (newWallets != null) {
            this.walletNames = newWallets;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_recurring_bill, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseModel item = items.get(position);

        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Recurring Item");

        String wName = walletNames.getOrDefault(item.getWalletId(), "Default Wallet");
        holder.tvWallet.setText(wName);

        String cat = item.getCategory() != null ? item.getCategory() : "General";
        holder.tvCategory.setText(cat);

        holder.tvAmount.setText(CurrencyHelper.format(context, item.getAmount()));
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, item.isExpense() ? R.color.expense_red : R.color.income_green));

        String interval = item.getRecurringInterval() != null ? item.getRecurringInterval() : "Monthly";
        holder.tvInterval.setText(capitalize(interval));

        // Icon based on category
        int iconRes = getCategoryIcon(cat);
        holder.ivIcon.setImageResource(iconRes);

        // Calculate due date
        long nextDue = item.getNextDueDate();
        if (nextDue > 0) {
            long now = System.currentTimeMillis();
            long diffMs = nextDue - now;
            long diffDays = diffMs / (1000 * 60 * 60 * 24);

            String formattedDate = dateFormat.format(new Date(nextDue));
            if (diffDays < 0) {
                holder.tvNextDue.setText("⚠️ Overdue (" + formattedDate + ")");
                holder.tvNextDue.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
            } else if (diffDays == 0) {
                holder.tvNextDue.setText("⏰ Due Today (" + formattedDate + ")");
                holder.tvNextDue.setTextColor(Color.parseColor("#FFA000"));
            } else if (diffDays == 1) {
                holder.tvNextDue.setText("Due Tomorrow (" + formattedDate + ")");
                holder.tvNextDue.setTextColor(Color.parseColor("#FFA000"));
            } else {
                holder.tvNextDue.setText("Next: In " + diffDays + " days (" + formattedDate + ")");
                holder.tvNextDue.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        } else {
            holder.tvNextDue.setText("Active (" + capitalize(interval) + ")");
            holder.tvNextDue.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        holder.btnPayNow.setOnClickListener(v -> {
            if (listener != null) listener.onPayNow(item);
        });

        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancelRecurring(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private int getCategoryIcon(String cat) {
        if (cat == null) return R.drawable.ic_repeat;
        switch (cat.toLowerCase()) {
            case "food": return R.drawable.ic_food;
            case "transport": return R.drawable.ic_transport;
            case "shopping": return R.drawable.ic_shopping;
            case "health": return R.drawable.ic_health;
            case "entertainment": return R.drawable.ic_entertainment;
            case "bank": return R.drawable.ic_bank;
            default: return R.drawable.ic_repeat;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvWallet, tvCategory, tvAmount, tvInterval, tvNextDue;
        TextView btnPayNow, btnCancel;
        ImageView ivIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_sub_title);
            tvWallet = itemView.findViewById(R.id.tv_sub_wallet);
            tvCategory = itemView.findViewById(R.id.tv_sub_category);
            tvAmount = itemView.findViewById(R.id.tv_sub_amount);
            tvInterval = itemView.findViewById(R.id.tv_sub_interval);
            tvNextDue = itemView.findViewById(R.id.tv_sub_next_due);
            btnPayNow = itemView.findViewById(R.id.btn_sub_pay_now);
            btnCancel = itemView.findViewById(R.id.btn_sub_cancel);
            ivIcon = itemView.findViewById(R.id.iv_sub_icon);
        }
    }
}
