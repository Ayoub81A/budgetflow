package com.hmaidi.budgetflow;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private List<Wallet> walletList;
    private Context context;
    private OnWalletClickListener listener;

    public WalletAdapter(Context context, List<Wallet> walletList,
                         OnWalletClickListener listener) {
        this.context = context;
        this.walletList = walletList;
        this.listener = listener;
    }

    @Override
    public WalletViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_wallet, parent, false);
        return new WalletViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WalletViewHolder holder, int position) {
        Wallet wallet = walletList.get(position);

        holder.tvName.setText(wallet.getName());
        holder.tvBalance.setText(CurrencyHelper.format(context, wallet.getBalance()));

        // Set icon based on wallet icon name
        holder.ivIcon.setImageResource(getIconResource(wallet.getIcon()));
        holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        holder.tvTransactions.setVisibility(View.GONE);

        // Set icon background color
        try {
            holder.ivIcon.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor(wallet.getColor())));
        } catch (Exception e) {
            holder.ivIcon.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#7B5EA7")));
        }

        // Click to open wallet detail
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onWalletClick(wallet);
        });

        // Long press to delete
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onWalletLongClick(wallet);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return walletList.size();
    }

    private int getIconResource(String iconName) {
        if (iconName == null) return R.drawable.ic_wallet;
        switch (iconName) {
            case "ic_wallet": return R.drawable.ic_wallet;
            case "ic_bank": return R.drawable.ic_bank;
            case "ic_card": return R.drawable.ic_card;
            case "ic_savings": return R.drawable.ic_savings;
            case "ic_other": return R.drawable.ic_other;
            default: return R.drawable.ic_wallet;
        }
    }

    public void updateList(List<Wallet> newList) {
        walletList = newList;
        notifyDataSetChanged();
    }

    public static class WalletViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBalance, tvTransactions;
        ImageView ivIcon;

        public WalletViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_wallet_name);
            tvBalance = itemView.findViewById(R.id.tv_wallet_balance);
            tvTransactions = itemView.findViewById(R.id.tv_wallet_transactions);
            ivIcon = itemView.findViewById(R.id.iv_wallet_icon);
        }
    }

    public interface OnWalletClickListener {
        void onWalletClick(Wallet wallet);
        void onWalletLongClick(Wallet wallet);
    }
}