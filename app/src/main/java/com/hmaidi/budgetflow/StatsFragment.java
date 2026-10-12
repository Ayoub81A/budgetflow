package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private PieChart pieChart;
    private LinearLayout layoutChartLegend;
    private TextView tvIncome, tvExpense, tvBalance;
    private ExpenseViewModel viewModel;
    private Button btnRangeWeek, btnRangeMonth, btnRangeYear, btnRangeAll;
    private String selectedRange = "month";
    private List<ExpenseModel> allExpenses = new ArrayList<>();
    private final List<View> legendRowViews = new ArrayList<>();
    private final List<Integer> activeSliceColors = new ArrayList<>();
    private int currentHighlightedIndex = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pie_chart);
        layoutChartLegend = view.findViewById(R.id.layout_chart_legend);
        tvIncome = view.findViewById(R.id.tv_stats_income);
        tvExpense = view.findViewById(R.id.tv_stats_expense);
        tvBalance = view.findViewById(R.id.tv_stats_balance);

        btnRangeWeek = view.findViewById(R.id.btn_range_week);
        btnRangeMonth = view.findViewById(R.id.btn_range_month);
        btnRangeYear = view.findViewById(R.id.btn_range_year);
        btnRangeAll = view.findViewById(R.id.btn_range_all);

        btnRangeWeek.setOnClickListener(v -> selectRange("week"));
        btnRangeMonth.setOnClickListener(v -> selectRange("month"));
        btnRangeYear.setOnClickListener(v -> selectRange("year"));
        btnRangeAll.setOnClickListener(v -> selectRange("all"));

        applyTimeRangeFilter();

        viewModel = new ViewModelProvider(requireActivity())
                .get(ExpenseViewModel.class);

        // Observe all expenses
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allExpenses = expenses != null ? expenses : new ArrayList<>();
            applyTimeRangeFilter();
        });

        return view;
    }

    private void selectRange(String range) {
        selectedRange = range;
        int activeBg = ContextCompat.getColor(requireContext(), R.color.purple_main);
        int inactiveBg = ContextCompat.getColor(requireContext(), R.color.card_dark);
        int activeText = Color.WHITE;
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        Button[] buttons = {btnRangeWeek, btnRangeMonth, btnRangeYear, btnRangeAll};
        String[] ranges = {"week", "month", "year", "all"};

        for (int i = 0; i < buttons.length; i++) {
            boolean isActive = ranges[i].equals(range);
            buttons[i].setBackgroundTintList(ColorStateList.valueOf(isActive ? activeBg : inactiveBg));
            buttons[i].setTextColor(isActive ? activeText : inactiveText);
        }

        applyTimeRangeFilter();
    }

    private void applyTimeRangeFilter() {
        if (getContext() == null) return;
        List<ExpenseModel> filtered = filterExpensesByRange(allExpenses, selectedRange);

        double periodIncome = 0;
        double periodExpense = 0;
        List<ExpenseModel> expenseOnly = new ArrayList<>();

        for (ExpenseModel item : filtered) {
            if (item.isExpense()) {
                periodExpense += item.getAmount();
                expenseOnly.add(item);
            } else {
                periodIncome += item.getAmount();
            }
        }

        tvIncome.setText("+" + CurrencyHelper.format(getContext(), periodIncome));
        tvExpense.setText("-" + CurrencyHelper.format(getContext(), periodExpense));

        double net = periodIncome - periodExpense;
        tvBalance.setText(CurrencyHelper.format(getContext(), net));
        if (net < 0) {
            tvBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else {
            tvBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        }

        buildPieChart(expenseOnly);
    }

    private List<ExpenseModel> filterExpensesByRange(List<ExpenseModel> list, String range) {
        if (list == null) return new ArrayList<>();
        if ("all".equals(range)) return new ArrayList<>(list);

        List<ExpenseModel> result = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;

        // Current week calculation (Monday through Sunday)
        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        long startOfWeekMillis = startOfWeek.getTimeInMillis();

        Calendar endOfWeek = Calendar.getInstance();
        endOfWeek.setTimeInMillis(startOfWeekMillis);
        endOfWeek.add(Calendar.DAY_OF_YEAR, 7);
        long endOfWeekMillis = endOfWeek.getTimeInMillis();

        for (ExpenseModel item : list) {
            String dateStr = item.getDate();
            if (dateStr == null) continue;
            String[] parts = dateStr.trim().split("/");
            if (parts.length != 3) continue;

            try {
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);

                if ("year".equals(range)) {
                    if (y == currentYear) {
                        result.add(item);
                    }
                } else if ("month".equals(range)) {
                    if (y == currentYear && m == currentMonth) {
                        result.add(item);
                    }
                } else if ("week".equals(range)) {
                    Calendar itemCal = Calendar.getInstance();
                    itemCal.set(y, m - 1, d, 12, 0, 0);
                    long itemTime = itemCal.getTimeInMillis();
                    if (itemTime >= startOfWeekMillis && itemTime < endOfWeekMillis) {
                        result.add(item);
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private void buildPieChart(List<ExpenseModel> expenses) {
        Map<String, Float> categoryMap = new HashMap<>();
        float totalAmount = 0f;
        for (ExpenseModel e : expenses) {
            if (e.isExpense()) {
                String cat = e.getCategory() != null && !e.getCategory().trim().isEmpty()
                        ? e.getCategory() : "Uncategorized";
                float amt = (float) e.getAmount();
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0f) + amt);
                totalAmount += amt;
            }
        }

        layoutChartLegend.removeAllViews();
        legendRowViews.clear();
        activeSliceColors.clear();
        currentHighlightedIndex = -1;

        if (categoryMap.isEmpty() || totalAmount <= 0f) {
            pieChart.clear();
            pieChart.setNoDataText("No expenses in this period");
            pieChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            pieChart.invalidate();
            return;
        }

        // Sort categories from largest to smallest
        List<Map.Entry<String, Float>> sortedEntries = new ArrayList<>(categoryMap.entrySet());
        sortedEntries.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : sortedEntries) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        // High-contrast, easily distinguishable palette (no adjacent similar blues)
        int[] palette = new int[]{
                Color.parseColor("#10B981"), // Emerald Green
                Color.parseColor("#F59E0B"), // Warm Amber
                Color.parseColor("#3B82F6"), // Sky Blue
                Color.parseColor("#F43F5E"), // Coral Rose
                Color.parseColor("#A855F7"), // Vibrant Purple
                Color.parseColor("#14B8A6"), // Teal
                Color.parseColor("#F97316"), // Tangerine Orange
                Color.parseColor("#EC4899")  // Pink
        };

        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            int color = palette[i % palette.length];
            colors.add(color);
            activeSliceColors.add(color);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(7f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Hide text on very thin slices (< 5%) so numbers never overlap
                if (value < 5f) return "";
                return String.format(Locale.US, "%.0f%%", value);
            }
        });

        int cardColor = ContextCompat.getColor(requireContext(), R.color.card_dark);

        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false); // Category names are in the interactive legend below
        pieChart.setData(data);
        pieChart.setBackgroundColor(Color.TRANSPARENT);
        pieChart.setHoleColor(cardColor);
        pieChart.setHoleRadius(56f);
        pieChart.setTransparentCircleColor(cardColor);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false); // Replaced by custom interactive legend below
        pieChart.setDrawCenterText(true);

        final float finalTotal = totalAmount;
        updateCenterText(null, finalTotal, finalTotal, -1);

        // Build interactive legend rows below the chart
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < entries.size(); i++) {
            final int index = i;
            PieEntry entry = entries.get(i);
            int sliceColor = colors.get(i);
            float pct = (entry.getValue() / finalTotal) * 100f;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padH = (int) (12 * density);
            int padV = (int) (10 * density);
            row.setPadding(padH, padV, padH, padV);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = (int) (6 * density);
            row.setLayoutParams(rowParams);

            // Color dot
            View dot = new View(requireContext());
            int dotSize = (int) (12 * density);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.setMarginEnd((int) (10 * density));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(sliceColor);
            dot.setBackground(dotDrawable);

            // Category Name
            TextView tvCatName = new TextView(requireContext());
            tvCatName.setText(entry.getLabel());
            tvCatName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvCatName.setTextSize(14f);
            tvCatName.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvCatName.setLayoutParams(nameParams);

            // Percentage badge
            TextView tvPct = new TextView(requireContext());
            tvPct.setText(String.format(Locale.US, "%.1f%%", pct));
            tvPct.setTextColor(sliceColor);
            tvPct.setTextSize(13f);
            tvPct.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pctParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pctParams.setMarginEnd((int) (10 * density));
            tvPct.setLayoutParams(pctParams);

            // Amount
            TextView tvAmt = new TextView(requireContext());
            tvAmt.setText(CurrencyHelper.format(requireContext(), entry.getValue()));
            tvAmt.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvAmt.setTextSize(13f);

            row.addView(dot);
            row.addView(tvCatName);
            row.addView(tvPct);
            row.addView(tvAmt);

            styleLegendRow(row, sliceColor, false, false);

            // Tapping the legend row highlights or un-highlights the pie slice too!
            row.setOnClickListener(v -> {
                if (currentHighlightedIndex == index) {
                    pieChart.highlightValues(null);
                    onSliceSelected(-1, entries, finalTotal);
                } else {
                    pieChart.highlightValue(index, 0, false);
                    onSliceSelected(index, entries, finalTotal);
                }
            });

            legendRowViews.add(row);
            layoutChartLegend.addView(row);
        }

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) h.getX();
                onSliceSelected(index, entries, finalTotal);
            }

            @Override
            public void onNothingSelected() {
                onSliceSelected(-1, entries, finalTotal);
            }
        });

        pieChart.animateY(700);
        pieChart.invalidate();
    }

    private void onSliceSelected(int index, List<PieEntry> entries, float totalAmount) {
        currentHighlightedIndex = index;
        boolean hasSelection = index >= 0 && index < entries.size();

        if (hasSelection) {
            PieEntry selected = entries.get(index);
            int color = activeSliceColors.get(index);
            updateCenterText(selected.getLabel(), selected.getValue(), totalAmount, color);
        } else {
            updateCenterText(null, totalAmount, totalAmount, -1);
        }

        for (int i = 0; i < legendRowViews.size(); i++) {
            View row = legendRowViews.get(i);
            int color = activeSliceColors.get(i);
            boolean isSelected = (i == index);
            styleLegendRow(row, color, isSelected, hasSelection);
        }
    }

    private void styleLegendRow(View row, int sliceColor, boolean isSelected, boolean anySelected) {
        if (getContext() == null) return;
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * density);

        if (isSelected) {
            int alphaColor = Color.argb(38, Color.red(sliceColor), Color.green(sliceColor), Color.blue(sliceColor));
            bg.setColor(alphaColor);
            bg.setStroke((int) (1.5f * density), sliceColor);
            row.setAlpha(1f);
        } else {
            bg.setColor(ContextCompat.getColor(requireContext(), R.color.bg_dark));
            bg.setStroke((int) (1 * density), ContextCompat.getColor(requireContext(), R.color.border_subtle));
            row.setAlpha(anySelected ? 0.5f : 1f);
        }
        row.setBackground(bg);
    }

    private void updateCenterText(String categoryLabel, float amount, float totalAmount, int accentColor) {
        if (getContext() == null) return;
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        String title = categoryLabel != null ? categoryLabel : "Total Spent";
        String amountStr = CurrencyHelper.format(requireContext(), amount);
        String fullText;
        if (categoryLabel != null && totalAmount > 0f) {
            String pctStr = String.format(Locale.US, "%.1f%%", (amount / totalAmount) * 100f);
            fullText = title + "\n" + amountStr + "\n" + pctStr;
        } else {
            fullText = title + "\n" + amountStr;
        }

        SpannableString spannable = new SpannableString(fullText);
        int titleEnd = title.length();
        spannable.setSpan(new ForegroundColorSpan(secondaryColor), 0, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(0.85f), 0, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int amountStart = titleEnd + 1;
        int amountEnd = amountStart + amountStr.length();
        spannable.setSpan(new ForegroundColorSpan(primaryColor), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(1.15f), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (categoryLabel != null && amountEnd < fullText.length()) {
            int pctColor = accentColor != -1 ? accentColor : primaryColor;
            spannable.setSpan(new ForegroundColorSpan(pctColor), amountEnd + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), amountEnd + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.9f), amountEnd + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        pieChart.setCenterText(spannable);
        pieChart.setCenterTextSize(13f);
    }
}