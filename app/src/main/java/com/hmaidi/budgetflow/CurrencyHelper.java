package com.hmaidi.budgetflow;

import android.content.Context;
import android.content.SharedPreferences;

public class CurrencyHelper {

    private static final String PREF_NAME = "currency_prefs";
    private static final String KEY_CODE = "currency_code";
    private static final String KEY_SYMBOL = "currency_symbol";

    // Save selected currency
    public static void saveCurrency(Context context, String code, String symbol) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CODE, code)
                .putString(KEY_SYMBOL, symbol)
                .apply();
    }

    // Get currency symbol — falls back to code if no symbol
    public static String getSymbol(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String symbol = prefs.getString(KEY_SYMBOL, "");
        String code = prefs.getString(KEY_CODE, "TND");
        return symbol.isEmpty() ? code : symbol;
    }

    // Get currency code
    public static String getCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CODE, "TND");
    }

    // Format an amount with the current currency
    public static String format(Context context, double amount) {
        String symbol = getSymbol(context);
        return String.format("%s %,.2f", symbol, amount);
    }

    // Format with + or - sign
    public static String formatSigned(Context context, double amount, boolean isExpense) {
        String symbol = getSymbol(context);
        if (isExpense) {
            return String.format("-%s %,.2f", symbol, amount);
        } else {
            return String.format("+%s %,.2f", symbol, amount);
        }
    }

    private static final String KEY_NAME = "user_name";
    private static final String KEY_BUDGET = "budget_limit";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_THEME = "dark_theme";

    public static void saveName(Context context, String name) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_NAME, name).apply();
    }

    public static String getName(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NAME, "My Account");
    }

    public static void saveBudget(Context context, double budget) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putFloat(KEY_BUDGET, (float) budget).apply();
    }

    public static double getBudget(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getFloat(KEY_BUDGET, 0f);
    }

    public static void saveNotifications(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public static boolean getNotifications(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATIONS, true);
    }

    public static void saveTheme(Context context, boolean isDark) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_THEME, isDark).apply();
    }

    public static boolean isDarkTheme(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_THEME, true);
    }

}