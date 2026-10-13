package com.hmaidi.budgetflow;

import java.io.Serializable;
import java.util.Calendar;

public class TransactionFilterCriteria implements Serializable {
    public String searchQuery = "";
    public String type = "all"; // "all", "expense", "income", "transfer"
    public Integer walletId = null; // null = all wallets
    public String category = null; // null = all categories
    public String dateRange = "all"; // "all", "today", "week", "month", "last_month"
    public String sortBy = "date_desc"; // "date_desc", "date_asc", "amount_desc", "amount_asc"
    public Double minAmount = null;
    public Double maxAmount = null;

    public boolean hasActiveFilters() {
        return (!"all".equals(type))
                || (walletId != null)
                || (category != null && !category.isEmpty())
                || (!"all".equals(dateRange))
                || (!"date_desc".equals(sortBy))
                || (minAmount != null)
                || (maxAmount != null);
    }

    public void reset() {
        searchQuery = "";
        type = "all";
        walletId = null;
        category = null;
        dateRange = "all";
        sortBy = "date_desc";
        minAmount = null;
        maxAmount = null;
    }

    public boolean matches(ExpenseModel e) {
        if (e == null || e.isDeleted()) return false;

        // 1. Type
        boolean isTransfer = "Transfer".equalsIgnoreCase(e.getCategory());
        if ("expense".equals(type)) {
            if (!e.isExpense() || isTransfer) return false;
        } else if ("income".equals(type)) {
            if (e.isExpense() || isTransfer) return false;
        } else if ("transfer".equals(type)) {
            if (!isTransfer) return false;
        }

        // 2. Wallet
        if (walletId != null && e.getWalletId() != walletId) {
            return false;
        }

        // 3. Category
        if (category != null && !category.isEmpty()) {
            if (e.getCategory() == null || !e.getCategory().equalsIgnoreCase(category)) {
                return false;
            }
        }

        // 4. Amount Range
        if (minAmount != null && e.getAmount() < minAmount) {
            return false;
        }
        if (maxAmount != null && e.getAmount() > maxAmount) {
            return false;
        }

        // 5. Date Range
        if (!"all".equals(dateRange)) {
            if (!matchesDateRange(e.getDate(), dateRange)) {
                return false;
            }
        }

        // 6. Search Query
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = searchQuery.toLowerCase().trim();
            boolean inTitle = e.getTitle() != null && e.getTitle().toLowerCase().contains(q);
            boolean inCategory = e.getCategory() != null && e.getCategory().toLowerCase().contains(q);
            boolean inDate = e.getDate() != null && e.getDate().contains(q);
            boolean inAmount = String.valueOf(e.getAmount()).contains(q);
            if (!inTitle && !inCategory && !inDate && !inAmount) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesDateRange(String dateStr, String range) {
        if (dateStr == null) return false;
        String[] parts = dateStr.trim().split("/");
        if (parts.length != 3) return false;

        try {
            int d = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);

            Calendar now = Calendar.getInstance();
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            int currentMonth = now.get(Calendar.MONTH) + 1;
            int currentYear = now.get(Calendar.YEAR);

            if ("today".equals(range)) {
                return d == currentDay && m == currentMonth && y == currentYear;
            } else if ("week".equals(range)) {
                Calendar startOfWeek = Calendar.getInstance();
                startOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
                startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
                startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
                startOfWeek.set(Calendar.MINUTE, 0);
                startOfWeek.set(Calendar.SECOND, 0);
                startOfWeek.set(Calendar.MILLISECOND, 0);

                Calendar endOfWeek = (Calendar) startOfWeek.clone();
                endOfWeek.add(Calendar.DAY_OF_YEAR, 7);

                Calendar itemCal = Calendar.getInstance();
                itemCal.set(y, m - 1, d, 12, 0, 0);
                long itemTime = itemCal.getTimeInMillis();
                return itemTime >= startOfWeek.getTimeInMillis() && itemTime < endOfWeek.getTimeInMillis();
            } else if ("month".equals(range)) {
                return m == currentMonth && y == currentYear;
            } else if ("last_month".equals(range)) {
                Calendar lastMonthCal = Calendar.getInstance();
                lastMonthCal.add(Calendar.MONTH, -1);
                int lastM = lastMonthCal.get(Calendar.MONTH) + 1;
                int lastY = lastMonthCal.get(Calendar.YEAR);
                return m == lastM && y == lastY;
            }
        } catch (NumberFormatException ignored) {}

        return true;
    }
}
