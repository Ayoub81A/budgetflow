package com.hmaidi.budgetflow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class WalletFragment extends Fragment {

    WalletAdapter adapter;
    WalletViewModel walletViewModel;
    TextView tvTotalBalance;
    LinearLayout emptyState;
    private java.util.List<Wallet> currentWallets = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        emptyState = view.findViewById(R.id.wallet_empty_state);
        RecyclerView recyclerView = view.findViewById(R.id.rv_wallets);
        View btnTransfer = view.findViewById(R.id.btn_transfer_wallets);

        // Set up ViewModel
        walletViewModel = new ViewModelProvider(requireActivity())
                .get(WalletViewModel.class);

        if (btnTransfer != null) {
            btnTransfer.setOnClickListener(v ->
                    WalletTransferDialogHelper.showTransferDialog(
                            requireContext(), walletViewModel, currentWallets, -1));
        }

        // Set up adapter
        adapter = new WalletAdapter(getContext(), new ArrayList<>(), new WalletAdapter.OnWalletClickListener() {
            @Override
            public void onWalletClick(Wallet wallet) {
                // Open wallet detail
                WalletDetailFragment detail = WalletDetailFragment.newInstance(wallet);
                detail.show(getParentFragmentManager(), "WalletDetail");
            }

            @Override
            public void onWalletLongClick(Wallet wallet) {
                // Show delete confirmation
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Wallet")
                        .setMessage("Delete \"" + wallet.getName() + "\"? This won't delete its transactions.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            walletViewModel.delete(wallet);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Observe wallets
        walletViewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            currentWallets = wallets != null ? wallets : new ArrayList<>();
            adapter.updateList(currentWallets);
            if (currentWallets.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                tvTotalBalance.setText(CurrencyHelper.format(getContext(), 0));
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                // Calculate total balance across all wallets
                double total = 0;
                for (Wallet w : currentWallets) total += w.getBalance();
                tvTotalBalance.setText(CurrencyHelper.format(getContext(), total));
            }
        });

        return view;
    }
}