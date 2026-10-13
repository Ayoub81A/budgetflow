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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout layoutChartLegend;
    private MaterialButton btnToggleCategories, btnToggleTrends;
    private LinearLayout layoutCategoriesView, layoutTrendsView;
    private String currentChartMode = "category";

    // Ratio & Metric Views (Option A + C)
    private TextView tvTrendsIncomeRatio, tvTrendsExpenseRatio;
    private View viewRatioIncome, viewRatioExpense;
    private TextView tvTrendPeakAmount, tvTrendPeakPeriod;
    private TextView tvTrendDailyAvg, tvTrendDailyAvgSub;
    private TextView tvTrendNetSaved, tvTrendSavingsRate;

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
        barChart = view.findViewById(R.id.bar_chart);
        layoutChartLegend = view.findViewById(R.id.layout_chart_legend);
        tvIncome = view.findViewById(R.id.tv_stats_income);
        tvExpense = view.findViewById(R.id.tv_stats_expense);
        tvBalance = view.findViewById(R.id.tv_stats_balance);

        btnToggleCategories = view.findViewById(R.id.btn_toggle_categories);
        btnToggleTrends = view.findViewById(R.id.btn_toggle_trends);
        layoutCategoriesView = view.findViewById(R.id.layout_categories_view);
        layoutTrendsView = view.findViewById(R.id.layout_trends_view);

        btnToggleCategories.setOnClickListener(v -> setChartMode("category"));
        btnToggleTrends.setOnClickListener(v -> setChartMode("trends"));

        tvTrendsIncomeRatio = view.findViewById(R.id.tv_trends_income_ratio);
        tvTrendsExpenseRatio = view.findViewById(R.id.tv_trends_expense_ratio);
        viewRatioIncome = view.findViewById(R.id.view_ratio_income);
        viewRatioExpense = view.findViewById(R.id.view_ratio_expense);
        tvTrendPeakAmount = view.findViewById(R.id.tv_trend_peak_amount);
        tvTrendPeakPeriod = view.findViewById(R.id.tv_trend_peak_period);
        tvTrendDailyAvg = view.findViewById(R.id.tv_trend_daily_avg);
        tvTrendDailyAvgSub = view.findViewById(R.id.tv_trend_daily_avg_sub);
        tvTrendNetSaved = view.findViewById(R.id.tv_trend_net_saved);
        tvTrendSavingsRate = view.findViewById(R.id.tv_trend_savings_rate);

        btnRangeWeek = view.findViewById(R.id.btn_range_week);
        btnRangeMonth = view.findViewById(R.id.btn_range_month);
        btnRangeYear = view.findViewById(R.id.btn_range_year);
        btnRangeAll = view.findViewById(R.id.btn_range_all);

        btnRangeWeek.setOnClickListener(v -> selectRange("week"));
        btnRangeMonth.setOnClickListener(v -> selectRange("month"));
        btnRangeYear.setOnClickListener(v -> selectRange("year"));
        btnRangeAll.setOnClickListener(v -> selectRange("all"));

        ImageView btnExportStatement = view.findViewById(R.id.btn_stats_export_statement);
        if (btnExportStatement != null) {
            btnExportStatement.setOnClickListener(v -> showStatementExportDialog());
        }

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

    private void setChartMode(String mode) {
        if (getContext() == null) return;
        currentChartMode = mode;
        int activeBg = ContextCompat.getColor(requireContext(), R.color.purple_main);
        int inactiveBg = ContextCompat.getColor(requireContext(), R.color.input_bg);
        int activeText = Color.WHITE;
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        if ("category".equals(mode)) {
            layoutCategoriesView.setVisibility(View.VISIBLE);
            layoutTrendsView.setVisibility(View.GONE);

            btnToggleCategories.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnToggleCategories.setTextColor(activeText);

            btnToggleTrends.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btnToggleTrends.setTextColor(inactiveText);
        } else {
            layoutCategoriesView.setVisibility(View.GONE);
            layoutTrendsView.setVisibility(View.VISIBLE);

            btnToggleTrends.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btnToggleTrends.setTextColor(activeText);

            btnToggleCategories.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btnToggleCategories.setTextColor(inactiveText);

            if (barChart != null && barChart.getData() != null) {
                barChart.animateY(500);
            }
        }
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
            if ("Transfer".equalsIgnoreCase(item.getCategory())) {
                continue;
            }
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
        buildBarChart(filtered, periodIncome, periodExpense);
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

    private void buildBarChart(List<ExpenseModel> transactions, double periodIncome, double periodExpense) {
        if (getContext() == null || barChart == null) return;

        List<String> labels = new ArrayList<>();
        List<Double> incomeList = new ArrayList<>();
        List<Double> expenseList = new ArrayList<>();

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;

        if ("week".equals(selectedRange)) {
            Calendar startOfWeek = Calendar.getInstance();
            startOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
            startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
            startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
            startOfWeek.set(Calendar.MINUTE, 0);
            startOfWeek.set(Calendar.SECOND, 0);
            startOfWeek.set(Calendar.MILLISECOND, 0);

            String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (int i = 0; i < 7; i++) {
                labels.add(dayNames[i]);
                Calendar dayCal = (Calendar) startOfWeek.clone();
                dayCal.add(Calendar.DAY_OF_YEAR, i);
                int targetD = dayCal.get(Calendar.DAY_OF_MONTH);
                int targetM = dayCal.get(Calendar.MONTH) + 1;
                int targetY = dayCal.get(Calendar.YEAR);

                double dayInc = 0;
                double dayExp = 0;
                for (ExpenseModel item : transactions) {
                    if ("Transfer".equalsIgnoreCase(item.getCategory())) continue;
                    String dStr = item.getDate();
                    if (dStr == null) continue;
                    String[] parts = dStr.trim().split("/");
                    if (parts.length != 3) continue;
                    try {
                        int d = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        if (d == targetD && m == targetM && y == targetY) {
                            if (item.isExpense()) {
                                dayExp += item.getAmount();
                            } else {
                                dayInc += item.getAmount();
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
                incomeList.add(dayInc);
                expenseList.add(dayExp);
            }
        } else if ("month".equals(selectedRange)) {
            int maxDaysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);
            int groupCount = (maxDaysInMonth > 28) ? 5 : 4;
            String[] weekLabels = {"W1", "W2", "W3", "W4", "W5"};

            for (int i = 0; i < groupCount; i++) {
                labels.add(weekLabels[i]);
                int startDay = i * 7 + 1;
                int endDay = (i == 4) ? maxDaysInMonth : Math.min((i + 1) * 7, maxDaysInMonth);

                double wInc = 0;
                double wExp = 0;
                for (ExpenseModel item : transactions) {
                    if ("Transfer".equalsIgnoreCase(item.getCategory())) continue;
                    String dStr = item.getDate();
                    if (dStr == null) continue;
                    String[] parts = dStr.trim().split("/");
                    if (parts.length != 3) continue;
                    try {
                        int d = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        if (y == currentYear && m == currentMonth && d >= startDay && d <= endDay) {
                            if (item.isExpense()) {
                                wExp += item.getAmount();
                            } else {
                                wInc += item.getAmount();
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
                incomeList.add(wInc);
                expenseList.add(wExp);
            }
        } else if ("year".equals(selectedRange)) {
            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            for (int i = 0; i < 12; i++) {
                labels.add(monthNames[i]);
                int targetMonth = i + 1;

                double mInc = 0;
                double mExp = 0;
                for (ExpenseModel item : transactions) {
                    if ("Transfer".equalsIgnoreCase(item.getCategory())) continue;
                    String dStr = item.getDate();
                    if (dStr == null) continue;
                    String[] parts = dStr.trim().split("/");
                    if (parts.length != 3) continue;
                    try {
                        int d = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        if (y == currentYear && m == targetMonth) {
                            if (item.isExpense()) {
                                mExp += item.getAmount();
                            } else {
                                mInc += item.getAmount();
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
                incomeList.add(mInc);
                expenseList.add(mExp);
            }
        } else {
            java.util.TreeSet<Integer> yearsSet = new java.util.TreeSet<>();
            for (ExpenseModel item : transactions) {
                if ("Transfer".equalsIgnoreCase(item.getCategory())) continue;
                String dStr = item.getDate();
                if (dStr == null) continue;
                String[] parts = dStr.trim().split("/");
                if (parts.length == 3) {
                    try {
                        yearsSet.add(Integer.parseInt(parts[2]));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (yearsSet.isEmpty()) {
                yearsSet.add(currentYear);
            }
            if (yearsSet.size() == 1) {
                int singleYear = yearsSet.first();
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                for (int i = 0; i < 12; i++) {
                    labels.add(monthNames[i]);
                    int targetMonth = i + 1;
                    double mInc = 0;
                    double mExp = 0;
                    for (ExpenseModel item : transactions) {
                        if ("Transfer".equalsIgnoreCase(item.getCategory())) continue;
                        String dStr = item.getDate();
                        if (dStr == null) continue;
                        String[] parts = dStr.trim().split("/");
                        if (parts.length == 3) {
                            try {
                                int m = Integer.parseInt(parts[1]);
                                int y = Integer.parseInt(parts[2]);
                                if (y == singleYear && m == targetMonth) {
                                    if (item.isExpense()) mExp += item.getAmount();
                                    else mInc += item.getAmount();
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    incomeList.add(mInc);
                    expenseList.add(mExp);
                }
            } else {
                for (int y : yearsSet) {
                    labels.add(String.valueOf(y));
                    double yInc = 0;
                    double yExp = 0;
                    for (ExpenseModel item : transactions) {
                        if ("Transfer".equalsIgnoreCase(item.getCategory())) continue;
                        String dStr = item.getDate();
                        if (dStr == null) continue;
                        String[] parts = dStr.trim().split("/");
                        if (parts.length == 3) {
                            try {
                                if (Integer.parseInt(parts[2]) == y) {
                                    if (item.isExpense()) yExp += item.getAmount();
                                    else yInc += item.getAmount();
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    incomeList.add(yInc);
                    expenseList.add(yExp);
                }
            }
        }

        boolean hasData = false;
        for (int i = 0; i < incomeList.size(); i++) {
            if (incomeList.get(i) > 0 || expenseList.get(i) > 0) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {
            barChart.clear();
            barChart.setNoDataText("No transactions in this period");
            barChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            barChart.invalidate();
            updateTrendMetricsAndRatio(0, 0, labels, expenseList, now);
            return;
        }

        int groupCount = labels.size();
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            incomeEntries.add(new BarEntry(i, incomeList.get(i).floatValue()));
            expenseEntries.add(new BarEntry(i, expenseList.get(i).floatValue()));
        }

        BarDataSet setIncome = new BarDataSet(incomeEntries, "Income");
        setIncome.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        setIncome.setDrawValues(false);

        BarDataSet setExpense = new BarDataSet(expenseEntries, "Expenses");
        setExpense.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        setExpense.setDrawValues(false);

        BarData barData = new BarData(setIncome, setExpense);
        float barWidth = 0.32f;
        float barSpace = 0.04f;
        float groupSpace = 0.28f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(groupCount);
        xAxis.setLabelCount(groupCount, false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        xAxis.setTextSize(11f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        leftAxis.setTextSize(10f);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.border_subtle));
        leftAxis.setDrawZeroLine(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000) {
                    return String.format(Locale.US, "%.1fk", value / 1000f);
                }
                return String.format(Locale.US, "%.0f", value);
            }
        });

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8f);
        legend.setXEntrySpace(14f);

        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);

        final List<String> finalLabels = labels;
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int groupIdx = (int) Math.floor(e.getX());
                if (groupIdx >= 0 && groupIdx < finalLabels.size()) {
                    String label = finalLabels.get(groupIdx);
                    String type = (h.getDataSetIndex() == 0) ? "Income" : "Expense";
                    String msg = label + " " + type + ": " + CurrencyHelper.format(getContext(), e.getY());
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.animateY(600);
        barChart.invalidate();

        updateTrendMetricsAndRatio(periodIncome, periodExpense, labels, expenseList, now);
    }

    private void updateTrendMetricsAndRatio(double periodIncome, double periodExpense,
                                            List<String> labels, List<Double> expenseList, Calendar now) {
        if (getContext() == null || tvTrendsIncomeRatio == null) return;

        // 1. Ratio Bar calculation
        double totalFlow = periodIncome + periodExpense;
        if (totalFlow > 0) {
            float incPct = (float) ((periodIncome / totalFlow) * 100.0);
            float expPct = (float) ((periodExpense / totalFlow) * 100.0);
            tvTrendsIncomeRatio.setText(String.format(Locale.US, "Income: %.0f%% (+%s)", incPct, CurrencyHelper.format(getContext(), periodIncome)));
            tvTrendsExpenseRatio.setText(String.format(Locale.US, "Expense: %.0f%% (-%s)", expPct, CurrencyHelper.format(getContext(), periodExpense)));

            if (incPct <= 0) {
                viewRatioIncome.setVisibility(View.GONE);
            } else {
                viewRatioIncome.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams incParams = (LinearLayout.LayoutParams) viewRatioIncome.getLayoutParams();
                incParams.weight = incPct;
                viewRatioIncome.setLayoutParams(incParams);
            }

            if (expPct <= 0) {
                viewRatioExpense.setVisibility(View.GONE);
            } else {
                viewRatioExpense.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams expParams = (LinearLayout.LayoutParams) viewRatioExpense.getLayoutParams();
                expParams.weight = expPct;
                viewRatioExpense.setLayoutParams(expParams);
            }
        } else {
            tvTrendsIncomeRatio.setText("Income: 0%");
            tvTrendsExpenseRatio.setText("Expense: 0%");
            viewRatioIncome.setVisibility(View.VISIBLE);
            viewRatioExpense.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams incParams = (LinearLayout.LayoutParams) viewRatioIncome.getLayoutParams();
            incParams.weight = 50f;
            viewRatioIncome.setLayoutParams(incParams);
            LinearLayout.LayoutParams expParams = (LinearLayout.LayoutParams) viewRatioExpense.getLayoutParams();
            expParams.weight = 50f;
            viewRatioExpense.setLayoutParams(expParams);
        }

        // 2. Peak Expense Tile
        double maxExpenseVal = 0;
        String maxExpensePeriod = "None";
        if (expenseList != null && labels != null) {
            for (int i = 0; i < expenseList.size(); i++) {
                if (expenseList.get(i) > maxExpenseVal) {
                    maxExpenseVal = expenseList.get(i);
                    maxExpensePeriod = (i < labels.size()) ? labels.get(i) : "P" + (i + 1);
                }
            }
        }
        if (maxExpenseVal > 0) {
            tvTrendPeakAmount.setText("-" + CurrencyHelper.format(getContext(), maxExpenseVal));
            tvTrendPeakPeriod.setText(maxExpensePeriod);
        } else {
            tvTrendPeakAmount.setText(CurrencyHelper.format(getContext(), 0));
            tvTrendPeakPeriod.setText("None");
        }

        // 3. Daily Average Tile
        int daysCount = 7;
        String avgSub = "per day";
        if ("week".equals(selectedRange)) {
            daysCount = 7;
            avgSub = "per day";
        } else if ("month".equals(selectedRange)) {
            daysCount = now.get(Calendar.DAY_OF_MONTH);
            if (daysCount < 1) daysCount = 1;
            avgSub = "per day";
        } else if ("year".equals(selectedRange)) {
            int currentDayOfYear = now.get(Calendar.DAY_OF_YEAR);
            daysCount = Math.max(1, currentDayOfYear);
            avgSub = "per day";
        } else {
            daysCount = 30;
            avgSub = "monthly avg";
        }
        double dailyAvg = (daysCount > 0) ? (periodExpense / daysCount) : 0;
        tvTrendDailyAvg.setText(CurrencyHelper.format(getContext(), dailyAvg));
        tvTrendDailyAvgSub.setText(avgSub);

        // 4. Net Saved / Rate Tile
        double net = periodIncome - periodExpense;
        tvTrendNetSaved.setText((net >= 0 ? "+" : "-") + CurrencyHelper.format(getContext(), Math.abs(net)));
        int netColor = ContextCompat.getColor(requireContext(), net >= 0 ? R.color.income_green : R.color.expense_red);
        tvTrendNetSaved.setTextColor(netColor);

        if (periodIncome > 0 && net > 0) {
            double rate = (net / periodIncome) * 100.0;
            tvTrendSavingsRate.setText(String.format(Locale.US, "%.1f%% saved", rate));
            tvTrendSavingsRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else if (net < 0) {
            tvTrendSavingsRate.setText("Deficit");
            tvTrendSavingsRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else {
            tvTrendSavingsRate.setText("0% saved");
            tvTrendSavingsRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void showStatementExportDialog() {
        if (getContext() == null) return;
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Export Financial Statement")
                .setSingleChoiceItems(monthNames, currentMonth - 1, (dialog, which) -> {
                    dialog.dismiss();
                    int targetMonth = which + 1;
                    String monthName = monthNames[which];
                    generateAndExportStatement(targetMonth, currentYear, monthName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAndExportStatement(int month, int year, String monthName) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Generating statement...", Toast.LENGTH_SHORT).show();

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Wallet> wallets = db.walletDao().getAllWalletsSync();
                List<ExpenseModel> expenses = db.expenseDao().getAllExpensesSync();
                List<CategoryBudget> categoryBudgets = db.categoryBudgetDao().getAllCategoryBudgetsSync();
                double monthlyBudget = CurrencyHelper.getBudget(getContext());
                String userName = CurrencyHelper.getName(getContext());

                String html = FinancialReportHelper.generateHtmlReport(
                        requireContext(),
                        year,
                        month,
                        expenses,
                        wallets,
                        categoryBudgets,
                        monthlyBudget,
                        userName
                );

                requireActivity().runOnUiThread(() -> {
                    String[] actions = {"Print / Save as PDF 📄", "Share HTML Statement 📤"};
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(monthName + " " + year + " Statement")
                            .setItems(actions, (d, actionIdx) -> {
                                String filename = "BudgetFlow_Statement_" + monthName + "_" + year;
                                if (actionIdx == 0) {
                                    FinancialReportHelper.printOrExportPdf(requireActivity(), html, filename);
                                } else {
                                    FinancialReportHelper.shareHtmlReport(requireContext(), html, filename + ".html");
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to export: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}