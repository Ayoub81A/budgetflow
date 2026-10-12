package com.hmaidi.budgetflow;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvIncome, tvExpense, tvBalance;
    private ExpenseViewModel viewModel;
    private Button btnRangeWeek, btnRangeMonth, btnRangeYear, btnRangeAll;
    private String selectedRange = "month";
    private List<ExpenseModel> allExpenses = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pie_chart);
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
        // Group expenses by category
        Map<String, Float> categoryMap = new HashMap<>();
        for (ExpenseModel e : expenses) {
            if (e.isExpense()) {
                String cat = e.getCategory();
                categoryMap.put(cat,
                        categoryMap.getOrDefault(cat, 0f) + (float) e.getAmount());
            }
        }

        if (categoryMap.isEmpty()) {
            pieChart.setNoDataText("No expenses yet");
            return;
        }

        // Build pie entries
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        // Colors for each slice
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#7B5EA7")); // purple
        colors.add(Color.parseColor("#F44336")); // red
        colors.add(Color.parseColor("#4CAF50")); // green
        colors.add(Color.parseColor("#2196F3")); // blue
        colors.add(Color.parseColor("#FF9800")); // orange

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Style the chart
        int cardColor = ContextCompat.getColor(requireContext(), R.color.card_dark);
        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);

        pieChart.setBackgroundColor(Color.TRANSPARENT);
        pieChart.setHoleColor(cardColor);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleColor(cardColor);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setTextColor(textPrimary);
        pieChart.getLegend().setTextSize(12f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.animateY(800);
        pieChart.invalidate();
    }
}