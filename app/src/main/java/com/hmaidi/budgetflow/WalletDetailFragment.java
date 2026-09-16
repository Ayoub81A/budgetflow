package com.hmaidi.budgetflow;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;

public class WalletDetailFragment extends BottomSheetDialogFragment {

    private Wallet wallet;

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

        // Set wallet info
        tvName.setText(wallet.getName());
        tvBalance.setText("Balance: " + CurrencyHelper.format(getContext(), wallet.getBalance()));

        try {
            ivIcon.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor(wallet.getColor())));
        } catch (Exception e) {
            ivIcon.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#7B5EA7")));
        }

        // Set up transactions list
        ExpenseAdapter adapter = new ExpenseAdapter(
                getContext(), new ArrayList<>(), expense -> {
            ExpenseDetailFragment detail = ExpenseDetailFragment.newInstance(expense);
            detail.show(getParentFragmentManager(), "ExpenseDetail");
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Observe transactions for this wallet
        WalletViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(WalletViewModel.class);

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

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }
}