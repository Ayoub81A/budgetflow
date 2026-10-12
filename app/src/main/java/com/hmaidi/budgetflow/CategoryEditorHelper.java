package com.hmaidi.budgetflow;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class CategoryEditorHelper {

    public static final String[] CATEGORY_ICONS = {
            "ic_food", "ic_transport", "ic_shopping", "ic_health",
            "ic_entertainment", "ic_wallet", "ic_card", "ic_bank",
            "ic_savings", "ic_other"
    };

    public static final String[] CATEGORY_COLORS = {
            "#6366F1", "#10B981", "#2563EB", "#F43F5E",
            "#F59E0B", "#06B6D4", "#8B5CF6", "#475569"
    };

    public interface OnCategorySavedListener {
        void onSaved(CategoryModel category);
    }

    public static void showEditorDialog(Context context,
                                        CategoryViewModel viewModel,
                                        @Nullable CategoryModel existingCategory,
                                        @Nullable OnCategorySavedListener listener) {
        boolean isEdit = existingCategory != null;
        String title = isEdit ? "Edit Category" : "New Category";

        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * density);
        layout.setPadding(pad, (int) (8 * density), pad, 0);

        // Name input
        EditText etName = new EditText(context);
        etName.setHint("Category Name (e.g. Groceries)");
        etName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        if (isEdit) etName.setText(existingCategory.getName());
        layout.addView(etName);

        // Icon Label
        TextView tvIconLabel = new TextView(context);
        tvIconLabel.setText("Select Icon:");
        tvIconLabel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        tvIconLabel.setTextSize(12);
        tvIconLabel.setPadding(0, (int) (8 * density), 0, 0);
        layout.addView(tvIconLabel);

        // Icon horizontal picker
        HorizontalScrollView iconScroll = new HorizontalScrollView(context);
        iconScroll.setScrollBarSize(0);
        iconScroll.setClipToPadding(false);
        LinearLayout iconRow = new LinearLayout(context);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER_VERTICAL);
        iconRow.setPadding(0, (int) (6 * density), 0, (int) (10 * density));
        iconScroll.addView(iconRow);
        layout.addView(iconScroll);

        final String[] selectedIcon = {isEdit ? existingCategory.getIcon() : "ic_food"};
        final String[] selectedColor = {isEdit ? existingCategory.getColor() : CATEGORY_COLORS[0]};

        int iconBoxSize = (int) (40 * density);
        int iconPadding = (int) (8 * density);
        int iconMargin = (int) (8 * density);

        Runnable refreshIcons = () -> {
            for (int i = 0; i < iconRow.getChildCount(); i++) {
                ImageView iv = (ImageView) iconRow.getChildAt(i);
                String ic = CATEGORY_ICONS[i];
                boolean sel = ic.equalsIgnoreCase(selectedIcon[0]);
                if (sel) {
                    iv.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(selectedColor[0])));
                    iv.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                } else {
                    iv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.input_bg)));
                    iv.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary)));
                }
            }
        };

        for (String ic : CATEGORY_ICONS) {
            ImageView iv = new ImageView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconBoxSize, iconBoxSize);
            lp.setMarginEnd(iconMargin);
            iv.setLayoutParams(lp);
            iv.setBackgroundResource(R.drawable.circle_bg);
            iv.setImageResource(getIconDrawableRes(ic));
            iv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
            iv.setOnClickListener(v -> {
                selectedIcon[0] = ic;
                refreshIcons.run();
            });
            iconRow.addView(iv);
        }
        refreshIcons.run();

        // Color Label
        TextView tvColorLabel = new TextView(context);
        tvColorLabel.setText("Select Color:");
        tvColorLabel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        tvColorLabel.setTextSize(12);
        layout.addView(tvColorLabel);

        // Color horizontal picker
        HorizontalScrollView colorScroll = new HorizontalScrollView(context);
        colorScroll.setScrollBarSize(0);
        colorScroll.setClipToPadding(false);
        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER_VERTICAL);
        colorRow.setPadding(0, (int) (6 * density), 0, (int) (12 * density));
        colorScroll.addView(colorRow);
        layout.addView(colorScroll);

        int colorFrameSize = (int) (38 * density);
        int colorCircleSize = (int) (32 * density);
        int checkSize = (int) (16 * density);
        int colorMargin = (int) (8 * density);

        Runnable refreshColors = () -> {
            for (int i = 0; i < colorRow.getChildCount(); i++) {
                FrameLayout frame = (FrameLayout) colorRow.getChildAt(i);
                ImageView check = (ImageView) frame.getChildAt(1);
                String c = CATEGORY_COLORS[i];
                check.setVisibility(c.equalsIgnoreCase(selectedColor[0]) ? View.VISIBLE : View.GONE);
            }
            refreshIcons.run();
        };

        for (String cHex : CATEGORY_COLORS) {
            FrameLayout frame = new FrameLayout(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(colorFrameSize, colorFrameSize);
            lp.setMarginEnd(colorMargin);
            frame.setLayoutParams(lp);

            View circle = new View(context);
            FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(colorCircleSize, colorCircleSize);
            cLp.gravity = Gravity.CENTER;
            circle.setLayoutParams(cLp);
            circle.setBackgroundResource(R.drawable.circle_bg);
            circle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(cHex)));

            ImageView check = new ImageView(context);
            FrameLayout.LayoutParams chkLp = new FrameLayout.LayoutParams(checkSize, checkSize);
            chkLp.gravity = Gravity.CENTER;
            check.setLayoutParams(chkLp);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            check.setVisibility(cHex.equalsIgnoreCase(selectedColor[0]) ? View.VISIBLE : View.GONE);

            frame.addView(circle);
            frame.addView(check);

            frame.setOnClickListener(v -> {
                selectedColor[0] = cHex;
                refreshColors.run();
            });
            colorRow.addView(frame);
        }

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isEdit) {
                        String oldName = existingCategory.getName();
                        existingCategory.setName(name);
                        existingCategory.setIcon(selectedIcon[0]);
                        existingCategory.setColor(selectedColor[0]);
                        existingCategory.setExpense(true);
                        viewModel.updateCategoryAndRenameExpenses(existingCategory, oldName);
                        Toast.makeText(context, "Category updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onSaved(existingCategory);
                    } else {
                        CategoryModel newCat = new CategoryModel(name, selectedIcon[0], selectedColor[0], true);
                        viewModel.insert(newCat);
                        Toast.makeText(context, "Category created!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onSaved(newCat);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static int getIconDrawableRes(String iconName) {
        if (iconName == null) return R.drawable.ic_other;
        switch (iconName.toLowerCase(java.util.Locale.ROOT)) {
            case "food":
            case "ic_food": return R.drawable.ic_food;
            case "transport":
            case "ic_transport": return R.drawable.ic_transport;
            case "shopping":
            case "ic_shopping": return R.drawable.ic_shopping;
            case "health":
            case "ic_health": return R.drawable.ic_health;
            case "entertainment":
            case "ic_entertainment": return R.drawable.ic_entertainment;
            case "wallet":
            case "ic_wallet": return R.drawable.ic_wallet;
            case "card":
            case "ic_card": return R.drawable.ic_card;
            case "bank":
            case "ic_bank": return R.drawable.ic_bank;
            case "savings":
            case "ic_savings": return R.drawable.ic_savings;
            default: return R.drawable.ic_other;
        }
    }
}
