package com.hmaidi.budgetflow;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsFragment extends Fragment {

    PieChart pieChart;
    TextView tvIncome, tvExpense, tvBalance;
    ExpenseViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pie_chart);
        tvIncome = view.findViewById(R.id.tv_stats_income);
        tvExpense = view.findViewById(R.id.tv_stats_expense);
        tvBalance = view.findViewById(R.id.tv_stats_balance);

        viewModel = new ViewModelProvider(requireActivity())
                .get(ExpenseViewModel.class);

        // Observe all expenses to build chart
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses == null || expenses.isEmpty()) {
                pieChart.setNoDataText("No expenses yet");
                pieChart.setNoDataTextColor(Color.WHITE);
                return;
            }
            buildPieChart(expenses);
        });

        // Observe totals for summary
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            double amount = total != null ? total : 0;
            tvIncome.setText("+" + CurrencyHelper.format(getContext(), amount));
            updateBalance();
        });

        viewModel.getTotalExpenses().observe(getViewLifecycleOwner(), total -> {
            double amount = total != null ? total : 0;
            tvExpense.setText("-" + CurrencyHelper.format(getContext(), amount));
            updateBalance();
        });

        return view;
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
        pieChart.setBackgroundColor(Color.parseColor("#1E1B2E"));
        pieChart.setHoleColor(Color.parseColor("#1E1B2E"));
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setTextSize(12f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void updateBalance() {
        try {
            String incomeStr = tvIncome.getText().toString()
                    .replaceAll("[^\\d.]", ""); // removes everything except digits and dot
            String expenseStr = tvExpense.getText().toString()
                    .replaceAll("[^\\d.]", "");

            double income = incomeStr.isEmpty() ? 0 : Double.parseDouble(incomeStr);
            double expense = expenseStr.isEmpty() ? 0 : Double.parseDouble(expenseStr);

            double balance = income - expense;
            tvBalance.setText(CurrencyHelper.format(getContext(), balance));
        } catch (NumberFormatException e) {
            tvBalance.setText(CurrencyHelper.format(getContext(), 0));
        }
    }
}