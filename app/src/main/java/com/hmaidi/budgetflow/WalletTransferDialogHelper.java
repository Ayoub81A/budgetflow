package com.hmaidi.budgetflow;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WalletTransferDialogHelper {

    public static void showTransferDialog(Context context,
                                          WalletViewModel walletViewModel,
                                          List<Wallet> wallets,
                                          int preselectedFromWalletId) {
        if (context == null || walletViewModel == null) return;
        if (wallets == null || wallets.size() < 2) {
            Toast.makeText(context, "Create at least 2 wallets to transfer money between them", Toast.LENGTH_SHORT).show();
            return;
        }

        float density = context.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * density);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, (int) (12 * density), pad, (int) (4 * density));

        List<String> walletLabels = new ArrayList<>();
        int defaultFromIdx = 0;
        for (int i = 0; i < wallets.size(); i++) {
            Wallet w = wallets.get(i);
            walletLabels.add(w.getName() + " (" + CurrencyHelper.format(context, w.getBalance()) + ")");
            if (w.getId() == preselectedFromWalletId) {
                defaultFromIdx = i;
            }
        }
        int defaultToIdx = (defaultFromIdx == 0 && wallets.size() > 1) ? 1 : 0;

        // From Wallet Label & Spinner
        TextView tvFrom = new TextView(context);
        tvFrom.setText("From Wallet");
        tvFrom.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        tvFrom.setTextSize(13f);
        container.addView(tvFrom);

        Spinner spFrom = new Spinner(context);
        ArrayAdapter<String> adapterFrom = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, walletLabels);
        spFrom.setAdapter(adapterFrom);
        spFrom.setSelection(defaultFromIdx);
        LinearLayout.LayoutParams spFromParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spFromParams.bottomMargin = (int) (14 * density);
        spFrom.setLayoutParams(spFromParams);
        container.addView(spFrom);

        // To Wallet Label & Spinner
        TextView tvTo = new TextView(context);
        tvTo.setText("To Wallet");
        tvTo.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        tvTo.setTextSize(13f);
        container.addView(tvTo);

        Spinner spTo = new Spinner(context);
        ArrayAdapter<String> adapterTo = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, walletLabels);
        spTo.setAdapter(adapterTo);
        spTo.setSelection(defaultToIdx);
        LinearLayout.LayoutParams spToParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spToParams.bottomMargin = (int) (14 * density);
        spTo.setLayoutParams(spToParams);
        container.addView(spTo);

        // Amount Input
        EditText etAmount = new EditText(context);
        etAmount.setHint("Amount to transfer");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        etAmount.setHintTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        LinearLayout.LayoutParams amtParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amtParams.bottomMargin = (int) (10 * density);
        etAmount.setLayoutParams(amtParams);
        container.addView(etAmount);

        // Note Input
        EditText etNote = new EditText(context);
        etNote.setHint("Note (optional, e.g. ATM Withdrawal)");
        etNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etNote.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        etNote.setHintTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        container.addView(etNote);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Transfer Between Wallets")
                .setView(container)
                .setPositiveButton("Transfer", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int fromPos = spFrom.getSelectedItemPosition();
                int toPos = spTo.getSelectedItemPosition();
                if (fromPos == toPos) {
                    Toast.makeText(context, "Please select two different wallets", Toast.LENGTH_SHORT).show();
                    return;
                }

                String amtStr = etAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amtStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amount <= 0) {
                    Toast.makeText(context, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                Wallet fromWallet = wallets.get(fromPos);
                Wallet toWallet = wallets.get(toPos);
                String note = etNote.getText().toString().trim();

                Calendar cal = Calendar.getInstance();
                String today = cal.get(Calendar.DAY_OF_MONTH) + "/"
                        + (cal.get(Calendar.MONTH) + 1) + "/"
                        + cal.get(Calendar.YEAR);

                walletViewModel.transferBetweenWallets(
                        fromWallet.getId(), toWallet.getId(), amount, note, today);

                Toast.makeText(context,
                        "Transferred " + CurrencyHelper.format(context, amount)
                                + " from " + fromWallet.getName() + " to " + toWallet.getName(),
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
