package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class WalletDetailFragment extends BottomSheetDialogFragment {

    private Wallet wallet;
    private List<Wallet> allWallets = new ArrayList<>();

    public static WalletDetailFragment newInstance(Wallet wallet) {
        WalletDetailFragment fragment = new WalletDetailFragment();
        Bundle args = new Bundle();
        args.putInt("id", wallet.getId());
        args.putString("name", wallet.getName());
        args.putString("icon", wallet.getIcon());
        args.putString("color", wallet.getColor());
        args.putDouble("balance", wallet.getBalance());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet_detail, container, false);

        // Rebuild wallet from args
        Bundle args = getArguments();
        wallet = new Wallet(
                args.getString("name"),
                args.getString("icon"),
                args.getString("color")
        );
        wallet.setId(args.getInt("id"));
        wallet.setBalance(args.getDouble("balance"));

        // Find views
        ImageView ivIcon = view.findViewById(R.id.iv_wallet_detail_icon);
        TextView tvName = view.findViewById(R.id.tv_wallet_detail_name);
        TextView tvBalance = view.findViewById(R.id.tv_wallet_detail_balance);
        TextView tvIncome = view.findViewById(R.id.tv_wallet_income);
        TextView tvExpense = view.findViewById(R.id.tv_wallet_expense);
        TextView tvNoTransactions = view.findViewById(R.id.tv_no_transactions);
        RecyclerView recyclerView = view.findViewById(R.id.rv_wallet_transactions);
        View btnTransfer = view.findViewById(R.id.btn_wallet_detail_transfer);
        View btnEdit = view.findViewById(R.id.btn_wallet_detail_edit);
        View btnDelete = view.findViewById(R.id.btn_wallet_detail_delete);

        bindWalletHeader(ivIcon, tvName, tvBalance, wallet);

        // Set up transactions list
        ExpenseAdapter adapter = new ExpenseAdapter(
                getContext(), new ArrayList<>(), expense -> {
            ExpenseDetailFragment detail = ExpenseDetailFragment.newInstance(expense);
            detail.show(getParentFragmentManager(), "ExpenseDetail");
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        WalletViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(WalletViewModel.class);

        // Live-observe this wallet so balance/name/color updates immediately after edits or transfers
        viewModel.getWalletById(wallet.getId()).observe(getViewLifecycleOwner(), liveWallet -> {
            if (liveWallet != null) {
                wallet = liveWallet;
                bindWalletHeader(ivIcon, tvName, tvBalance, wallet);
            }
        });

        viewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            allWallets = wallets != null ? wallets : new ArrayList<>();
        });

        if (btnTransfer != null) {
            btnTransfer.setOnClickListener(v ->
                    WalletTransferDialogHelper.showTransferDialog(
                            requireContext(), viewModel, allWallets, wallet.getId()));
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                AddWalletFragment editSheet = AddWalletFragment.newInstanceForEdit(wallet);
                editSheet.show(getParentFragmentManager(), "EditWallet");
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Wallet")
                        .setMessage("Delete \"" + wallet.getName() + "\"? This won't delete its transactions.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            viewModel.delete(wallet);
                            dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        viewModel.getExpensesByWallet(wallet.getId())
                .observe(getViewLifecycleOwner(), expenses -> {
                    if (expenses == null || expenses.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvNoTransactions.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvNoTransactions.setVisibility(View.GONE);
                        adapter.updateList(expenses);
                    }
                });

        // Observe income for this wallet
        viewModel.getIncomeByWallet(wallet.getId())
                .observe(getViewLifecycleOwner(), total -> {
                    double amount = total != null ? total : 0;
                    tvIncome.setText("+" + CurrencyHelper.format(getContext(), amount));
                });

        // Observe expenses for this wallet
        viewModel.getExpensesByWalletTotal(wallet.getId())
                .observe(getViewLifecycleOwner(), total -> {
                    double amount = total != null ? total : 0;
                    tvExpense.setText("-" + CurrencyHelper.format(getContext(), amount));
                });

        return view;
    }

    private void bindWalletHeader(ImageView ivIcon, TextView tvName, TextView tvBalance, Wallet w) {
        if (getContext() == null || w == null) return;
        tvName.setText(w.getName());
        tvBalance.setText("Balance: " + CurrencyHelper.format(getContext(), w.getBalance()));

        ivIcon.setImageResource(getIconResource(w.getIcon()));
        ivIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        try {
            ivIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(w.getColor())));
        } catch (Exception e) {
            ivIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        }
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

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }
}