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

    private static final String[] WALLET_COLORS = {
            "#6366F1", // Indigo / Royal Violet
            "#10B981", // Emerald Mint
            "#2563EB", // Ocean Blue
            "#F43F5E", // Rose Coral
            "#F59E0B", // Amber Gold
            "#06B6D4", // Cyan Teal
            "#8B5CF6", // Vivid Purple
            "#475569"  // Slate Charcoal
    };

    String selectedIcon = "ic_wallet";
    String selectedColor = WALLET_COLORS[0];

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
        LinearLayout colorRow = view.findViewById(R.id.color_row);
        selectedColor = WALLET_COLORS[0];
        setupColorSwatches(view, colorRow);

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

    @Override
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
            android.widget.FrameLayout bottomSheet =
                    bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void selectIcon(View root, LinearLayout selected,
                            String iconName, int[] allIds) {
        if (getContext() == null) return;
        // Reset all icons
        for (int id : allIds) {
            LinearLayout layout = root.findViewById(id);
            if (layout != null) {
                ImageView icon = (ImageView) layout.getChildAt(0);
                TextView label = (TextView) layout.getChildAt(1);
                icon.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.bg_dark)));
                icon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)));
                label.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }

        // Highlight selected
        ImageView icon = (ImageView) selected.getChildAt(0);
        TextView label = (TextView) selected.getChildAt(1);
        icon.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(selectedColor)));
        icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_light));
        selectedIcon = iconName;
    }

    private void setupColorSwatches(View root, LinearLayout colorRow) {
        if (getContext() == null) return;
        colorRow.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int frameSize = (int) (42 * density);
        int circleSize = (int) (34 * density);
        int checkSize = (int) (18 * density);
        int marginEnd = (int) (10 * density);

        for (String colorHex : WALLET_COLORS) {
            android.widget.FrameLayout frame = new android.widget.FrameLayout(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(frameSize, frameSize);
            lp.setMarginEnd(marginEnd);
            frame.setLayoutParams(lp);

            View circle = new View(getContext());
            android.widget.FrameLayout.LayoutParams circleLp =
                    new android.widget.FrameLayout.LayoutParams(circleSize, circleSize);
            circleLp.gravity = android.view.Gravity.CENTER;
            circle.setLayoutParams(circleLp);
            circle.setBackgroundResource(R.drawable.circle_bg);
            circle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex)));

            ImageView check = new ImageView(getContext());
            android.widget.FrameLayout.LayoutParams checkLp =
                    new android.widget.FrameLayout.LayoutParams(checkSize, checkSize);
            checkLp.gravity = android.view.Gravity.CENTER;
            check.setLayoutParams(checkLp);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            check.setVisibility(colorHex.equalsIgnoreCase(selectedColor) ? View.VISIBLE : View.GONE);

            frame.addView(circle);
            frame.addView(check);

            frame.setOnClickListener(v -> {
                selectedColor = colorHex;
                // Update checks
                for (int i = 0; i < colorRow.getChildCount(); i++) {
                    android.widget.FrameLayout f = (android.widget.FrameLayout) colorRow.getChildAt(i);
                    ImageView c = (ImageView) f.getChildAt(1);
                    boolean isMatch = WALLET_COLORS[i].equalsIgnoreCase(selectedColor);
                    c.setVisibility(isMatch ? View.VISIBLE : View.GONE);
                }
                // Update selected icon tint
                int selectedIconLayoutId = getIconLayoutId(selectedIcon);
                if (selectedIconLayoutId != 0) {
                    LinearLayout layout = root.findViewById(selectedIconLayoutId);
                    if (layout != null) {
                        ImageView icon = (ImageView) layout.getChildAt(0);
                        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(selectedColor)));
                    }
                }
            });

            colorRow.addView(frame);
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