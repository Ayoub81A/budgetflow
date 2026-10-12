package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

public class AddWalletFragment extends BottomSheetDialogFragment {

    String selectedIcon = "ic_wallet";
    String selectedColor = "#7B5EA7";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_wallet, container, false);

        TextInputEditText etName = view.findViewById(R.id.et_wallet_name);

        // Icon selection
        int[] iconIds = {R.id.icon_wallet, R.id.icon_bank,
                R.id.icon_card, R.id.icon_savings, R.id.icon_other};
        String[] iconNames = {"ic_wallet", "ic_bank", "ic_card", "ic_savings", "ic_other"};

        for (int i = 0; i < iconIds.length; i++) {
            LinearLayout iconLayout = view.findViewById(iconIds[i]);
            String iconName = iconNames[i];
            iconLayout.setOnClickListener(v ->
                    selectIcon(view, iconLayout, iconName, iconIds));
        }

        // Select first icon by default
        selectIcon(view, view.findViewById(R.id.icon_wallet),
                "ic_wallet", iconIds);

        // Color selection
        int[] colorIds = {R.id.color_purple, R.id.color_blue,
                R.id.color_green, R.id.color_red, R.id.color_orange};
        String[] colorValues = {"#7B5EA7", "#2196F3",
                "#4CAF50", "#F44336", "#FF9800"};

        for (int i = 0; i < colorIds.length; i++) {
            View colorView = view.findViewById(colorIds[i]);
            String colorValue = colorValues[i];
            colorView.setOnClickListener(v ->
                    selectColor(view, colorView, colorValue, colorIds));
        }

        // Select first color by default
        selectColor(view, view.findViewById(R.id.color_purple),
                "#7B5EA7", colorIds);

        // Save button
        Button btnSave = view.findViewById(R.id.btn_save_wallet);
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getContext(),
                        "Please enter a wallet name", Toast.LENGTH_SHORT).show();
                return;
            }

            Wallet wallet = new Wallet(name, selectedIcon, selectedColor);

            WalletViewModel viewModel = new ViewModelProvider(requireActivity())
                    .get(WalletViewModel.class);
            viewModel.insert(wallet);

            dismiss();
        });

        return view;
    }

    private void selectIcon(View root, LinearLayout selected,
                            String iconName, int[] allIds) {
        // Reset all icons
        for (int id : allIds) {
            LinearLayout layout = root.findViewById(id);
            ImageView icon = (ImageView) layout.getChildAt(0);
            TextView label = (TextView) layout.getChildAt(1);
            icon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.card_dark)));
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.text_secondary)));
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        }

        // Highlight selected
        ImageView icon = (ImageView) selected.getChildAt(0);
        TextView label = (TextView) selected.getChildAt(1);
        icon.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(selectedColor)));
        icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.purple_light));
        selectedIcon = iconName;
    }

    private void selectColor(View root, View selected,
                             String colorValue, int[] allIds) {
        // Reset all
        for (int id : allIds) {
            View colorView = root.findViewById(id);
            colorView.setScaleX(1f);
            colorView.setScaleY(1f);
            colorView.setAlpha(0.45f);
        }
        // Highlight selected
        selected.setScaleX(1.2f);
        selected.setScaleY(1.2f);
        selected.setAlpha(1f);
        selectedColor = colorValue;

        // Update currently selected icon background immediately
        int selectedIconLayoutId = getIconLayoutId(selectedIcon);
        if (selectedIconLayoutId != 0) {
            LinearLayout layout = root.findViewById(selectedIconLayoutId);
            if (layout != null) {
                ImageView icon = (ImageView) layout.getChildAt(0);
                icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(selectedColor)));
            }
        }
    }

    private int getIconLayoutId(String iconName) {
        if (iconName == null) return R.id.icon_wallet;
        switch (iconName) {
            case "ic_wallet": return R.id.icon_wallet;
            case "ic_bank": return R.id.icon_bank;
            case "ic_card": return R.id.icon_card;
            case "ic_savings": return R.id.icon_savings;
            case "ic_other": return R.id.icon_other;
            default: return R.id.icon_wallet;
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }
}