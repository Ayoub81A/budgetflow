package com.hmaidi.budgetflow;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataExportImportHelper {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Handler mainHandler;

    private static void postToMain(Runnable r) {
        try {
            if (mainHandler == null && Looper.getMainLooper() != null) {
                mainHandler = new Handler(Looper.getMainLooper());
            }
            if (mainHandler != null) {
                mainHandler.post(r);
                return;
            }
        } catch (Throwable ignored) {}
        r.run();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class BackupData {
        public List<Wallet> wallets = new ArrayList<>();
        public List<ExpenseModel> expenses = new ArrayList<>();
        public List<CategoryModel> categories = new ArrayList<>();
        public List<SavingsGoal> savingsGoals = new ArrayList<>();
        public List<CategoryBudget> categoryBudgets = new ArrayList<>();
        public String currencyCode = null;
        public String currencySymbol = null;
        public Double monthlyBudget = null;
        public String userName = null;
    }

    // --- CSV GENERATION ---

    public static String generateCsv(List<Wallet> wallets, List<ExpenseModel> expenses) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        sb.append("\uFEFF");

        // Map wallet ID to name
        Map<Integer, String> walletMap = new HashMap<>();
        if (wallets != null) {
            for (Wallet w : wallets) {
                walletMap.put(w.getId(), w.getName());
            }
        }

        // CSV Header
        sb.append("Date,Title,Amount,Type,Category,Wallet,Recurring,Interval\n");

        if (expenses != null) {
            for (ExpenseModel e : expenses) {
                String walletName = walletMap.getOrDefault(e.getWalletId(), "Default");
                String typeStr = e.isExpense() ? "Expense" : "Income";
                String recurringStr = e.isRecurring() ? "Yes" : "No";
                String intervalStr = e.isRecurring() && e.getRecurringInterval() != null ? e.getRecurringInterval() : "";

                sb.append(escapeCsv(e.getDate())).append(",");
                sb.append(escapeCsv(e.getTitle())).append(",");
                sb.append(String.format(Locale.US, "%.2f", e.getAmount())).append(",");
                sb.append(escapeCsv(typeStr)).append(",");
                sb.append(escapeCsv(e.getCategory())).append(",");
                sb.append(escapeCsv(walletName)).append(",");
                sb.append(escapeCsv(recurringStr)).append(",");
                sb.append(escapeCsv(intervalStr)).append("\n");
            }
        }

        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // --- JSON GENERATION ---

    public static String generateJson(Context context,
                                      List<Wallet> wallets,
                                      List<ExpenseModel> expenses,
                                      List<CategoryModel> categories,
                                      List<SavingsGoal> savingsGoals) {
        return generateJson(context, wallets, expenses, categories, savingsGoals, null);
    }

    public static String generateJson(Context context,
                                      List<Wallet> wallets,
                                      List<ExpenseModel> expenses,
                                      List<CategoryModel> categories,
                                      List<SavingsGoal> savingsGoals,
                                      List<CategoryBudget> categoryBudgets) {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "BudgetFlow");
            root.put("version", 2);
            root.put("exportDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));

            if (context != null) {
                JSONObject settingsObj = new JSONObject();
                settingsObj.put("userName", CurrencyHelper.getName(context));
                settingsObj.put("currencyCode", CurrencyHelper.getCode(context));
                settingsObj.put("currencySymbol", CurrencyHelper.getSymbol(context));
                settingsObj.put("monthlyBudget", CurrencyHelper.getBudget(context));
                root.put("settings", settingsObj);
            }

            // Wallets array
            JSONArray walletsArr = new JSONArray();
            if (wallets != null) {
                for (Wallet w : wallets) {
                    JSONObject wObj = new JSONObject();
                    wObj.put("id", w.getId());
                    wObj.put("name", w.getName());
                    wObj.put("icon", w.getIcon());
                    wObj.put("color", w.getColor());
                    wObj.put("balance", w.getBalance());
                    walletsArr.put(wObj);
                }
            }
            root.put("wallets", walletsArr);

            // Categories array
            JSONArray categoriesArr = new JSONArray();
            if (categories != null) {
                for (CategoryModel c : categories) {
                    JSONObject cObj = new JSONObject();
                    cObj.put("name", c.getName());
                    cObj.put("icon", c.getIcon());
                    cObj.put("color", c.getColor());
                    cObj.put("isExpense", c.isExpense());
                    categoriesArr.put(cObj);
                }
            }
            root.put("categories", categoriesArr);

            // Savings Goals array
            JSONArray goalsArr = new JSONArray();
            if (savingsGoals != null) {
                for (SavingsGoal g : savingsGoals) {
                    JSONObject gObj = new JSONObject();
                    gObj.put("title", g.getTitle());
                    gObj.put("targetAmount", g.getTargetAmount());
                    gObj.put("savedAmount", g.getSavedAmount());
                    gObj.put("icon", g.getIcon());
                    gObj.put("color", g.getColor());
                    gObj.put("targetDate", g.getTargetDate());
                    goalsArr.put(gObj);
                }
            }
            root.put("savingsGoals", goalsArr);

            // Category Budgets array
            JSONArray cbArr = new JSONArray();
            if (categoryBudgets != null) {
                for (CategoryBudget cb : categoryBudgets) {
                    JSONObject cbObj = new JSONObject();
                    cbObj.put("categoryName", cb.getCategoryName());
                    cbObj.put("monthlyLimit", cb.getMonthlyLimit());
                    cbArr.put(cbObj);
                }
            }
            root.put("categoryBudgets", cbArr);

            // Expenses array
            JSONArray expensesArr = new JSONArray();
            if (expenses != null) {
                for (ExpenseModel e : expenses) {
                    JSONObject eObj = new JSONObject();
                    eObj.put("id", e.getId());
                    eObj.put("title", e.getTitle());
                    eObj.put("date", e.getDate());
                    eObj.put("amount", e.getAmount());
                    eObj.put("category", e.getCategory());
                    eObj.put("isExpense", e.isExpense());
                    eObj.put("walletId", e.getWalletId());
                    eObj.put("isRecurring", e.isRecurring());
                    eObj.put("recurringInterval", e.getRecurringInterval());
                    eObj.put("nextDueDate", e.getNextDueDate());
                    expensesArr.put(eObj);
                }
            }
            root.put("expenses", expensesArr);

            return root.toString(2);
        } catch (Exception e) {
            return "";
        }
    }

    // --- PARSING JSON ---

    public static BackupData parseJson(String jsonString) throws Exception {
        BackupData data = new BackupData();
        JSONObject root = new JSONObject(jsonString);

        if (root.has("settings")) {
            JSONObject sObj = root.optJSONObject("settings");
            if (sObj != null) {
                if (sObj.has("userName")) data.userName = sObj.optString("userName", null);
                if (sObj.has("currencyCode")) data.currencyCode = sObj.optString("currencyCode", null);
                if (sObj.has("currencySymbol")) data.currencySymbol = sObj.optString("currencySymbol", null);
                if (sObj.has("monthlyBudget")) data.monthlyBudget = sObj.optDouble("monthlyBudget", 0.0);
            }
        }

        if (root.has("wallets")) {
            JSONArray walletsArr = root.getJSONArray("wallets");
            for (int i = 0; i < walletsArr.length(); i++) {
                JSONObject wObj = walletsArr.getJSONObject(i);
                Wallet w = new Wallet(
                        wObj.optString("name", "Wallet"),
                        wObj.optString("icon", "ic_wallet"),
                        wObj.optString("color", "#10B981")
                );
                w.setId(wObj.optInt("id", 0));
                w.setBalance(wObj.optDouble("balance", 0.0));
                data.wallets.add(w);
            }
        }

        if (root.has("categories")) {
            JSONArray catArr = root.getJSONArray("categories");
            for (int i = 0; i < catArr.length(); i++) {
                JSONObject cObj = catArr.getJSONObject(i);
                CategoryModel c = new CategoryModel(
                        cObj.optString("name", "General"),
                        cObj.optString("icon", "ic_wallet"),
                        cObj.optString("color", "#10B981"),
                        cObj.optBoolean("isExpense", true)
                );
                if (c.isExpense()) {
                    data.categories.add(c);
                }
            }
        }

        if (root.has("savingsGoals")) {
            JSONArray goalsArr = root.getJSONArray("savingsGoals");
            for (int i = 0; i < goalsArr.length(); i++) {
                JSONObject gObj = goalsArr.getJSONObject(i);
                SavingsGoal g = new SavingsGoal(
                        gObj.optString("title", "Goal"),
                        gObj.optDouble("targetAmount", 0.0),
                        gObj.optDouble("savedAmount", 0.0),
                        gObj.optString("icon", "ic_savings"),
                        gObj.optString("color", "#10B981"),
                        gObj.optString("targetDate", "")
                );
                data.savingsGoals.add(g);
            }
        }

        if (root.has("categoryBudgets")) {
            JSONArray cbArr = root.getJSONArray("categoryBudgets");
            for (int i = 0; i < cbArr.length(); i++) {
                JSONObject cbObj = cbArr.getJSONObject(i);
                CategoryBudget cb = new CategoryBudget(
                        cbObj.optString("categoryName", ""),
                        cbObj.optDouble("monthlyLimit", 0.0)
                );
                if (!cb.getCategoryName().isEmpty() && cb.getMonthlyLimit() > 0) {
                    data.categoryBudgets.add(cb);
                }
            }
        }

        if (root.has("expenses")) {
            JSONArray expensesArr = root.getJSONArray("expenses");
            for (int i = 0; i < expensesArr.length(); i++) {
                JSONObject eObj = expensesArr.getJSONObject(i);
                ExpenseModel expense = new ExpenseModel(
                        eObj.optString("title", "Expense"),
                        eObj.optString("date", "1/1/2026"),
                        eObj.optDouble("amount", 0.0),
                        eObj.optString("category", "General"),
                        eObj.optBoolean("isExpense", true),
                        eObj.optInt("walletId", 1)
                );
                expense.setId(eObj.optInt("id", 0));
                expense.setRecurring(eObj.optBoolean("isRecurring", false));
                expense.setRecurringInterval(eObj.optString("recurringInterval", ""));
                expense.setNextDueDate(eObj.optLong("nextDueDate", 0));
                data.expenses.add(expense);
            }
        }

        return data;
    }

    // --- PARSING CSV ---

    public static BackupData parseCsv(String csvString, List<Wallet> existingWallets) throws Exception {
        BackupData data = new BackupData();
        List<String[]> rows = parseCsvRows(csvString);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        // Header mapping
        String[] header = rows.get(0);
        int dateCol = -1, titleCol = -1, amountCol = -1, typeCol = -1,
                categoryCol = -1, walletCol = -1, recurringCol = -1, intervalCol = -1;

        for (int i = 0; i < header.length; i++) {
            String col = header[i].trim().toLowerCase().replace("\uFEFF", "");
            if (col.contains("date")) dateCol = i;
            else if (col.contains("title") || col.contains("name") || col.contains("description")) titleCol = i;
            else if (col.contains("amount") || col.contains("cost") || col.contains("price")) amountCol = i;
            else if (col.contains("type")) typeCol = i;
            else if (col.contains("category")) categoryCol = i;
            else if (col.contains("wallet")) walletCol = i;
            else if (col.contains("recurring")) recurringCol = i;
            else if (col.contains("interval")) intervalCol = i;
        }

        // Fallbacks if header names don't match exactly
        if (dateCol == -1) dateCol = 0;
        if (titleCol == -1) titleCol = 1;
        if (amountCol == -1) amountCol = 2;
        if (typeCol == -1) typeCol = 3;
        if (categoryCol == -1) categoryCol = 4;
        if (walletCol == -1 && header.length > 5) walletCol = 5;

        // Map of wallet name (lowercased) to Wallet
        Map<String, Wallet> walletNameMap = new HashMap<>();
        if (existingWallets != null) {
            for (Wallet w : existingWallets) {
                walletNameMap.put(w.getName().toLowerCase(), w);
            }
        }

        int defaultWalletId = (existingWallets != null && !existingWallets.isEmpty())
                ? existingWallets.get(0).getId() : 1;

        // Process data rows
        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            if (row.length == 0 || (row.length == 1 && row[0].trim().isEmpty())) {
                continue;
            }

            String date = getCol(row, dateCol, "1/1/2026");
            String title = getCol(row, titleCol, "Transaction");
            String amountStr = getCol(row, amountCol, "0")
                    .replaceAll("[^0-9.]", "");
            double amount = 0;
            try {
                if (!amountStr.isEmpty()) amount = Double.parseDouble(amountStr);
            } catch (Exception ignored) {}

            String typeStr = getCol(row, typeCol, "Expense");
            boolean isExpense = !typeStr.equalsIgnoreCase("income");

            String category = getCol(row, categoryCol, isExpense ? "General" : "Income");
            String walletName = getCol(row, walletCol, "Default");

            int assignedWalletId = defaultWalletId;
            if (!walletName.isEmpty()) {
                String key = walletName.toLowerCase();
                if (walletNameMap.containsKey(key)) {
                    assignedWalletId = walletNameMap.get(key).getId();
                } else {
                    // Create wallet if it doesn't exist
                    Wallet newWallet = new Wallet(walletName, "ic_wallet", "#6366F1");
                    data.wallets.add(newWallet);
                    walletNameMap.put(key, newWallet);
                }
            }

            String recurringStr = getCol(row, recurringCol, "No");
            boolean isRecurring = recurringStr.equalsIgnoreCase("yes") || recurringStr.equalsIgnoreCase("true");
            String intervalStr = getCol(row, intervalCol, "").toLowerCase();

            ExpenseModel expense = new ExpenseModel(title, date, amount, category, isExpense, assignedWalletId);
            if (isRecurring && !intervalStr.isEmpty()) {
                expense.setRecurring(true);
                expense.setRecurringInterval(intervalStr);
            }
            data.expenses.add(expense);
        }

        return data;
    }

    private static String getCol(String[] row, int index, String fallback) {
        if (index >= 0 && index < row.length) {
            String val = row[index].trim();
            return val.isEmpty() ? fallback : val;
        }
        return fallback;
    }

    private static List<String[]> parseCsvRows(String csv) {
        List<String[]> list = new ArrayList<>();
        if (csv == null) return list;

        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    if (!currentRow.isEmpty()) {
                        list.add(currentRow.toArray(new String[0]));
                        currentRow = new ArrayList<>();
                    }
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                        i++; // skip CRLF
                    }
                } else {
                    currentField.append(c);
                }
            }
        }

        if (currentField.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentField.toString());
            list.add(currentRow.toArray(new String[0]));
        }

        return list;
    }

    // --- FILE I/O ---

    public static void writeToUri(Context context, Uri uri, String text, Callback<Void> callback) {
        executor.execute(() -> {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("Cannot open output stream");
                out.write(text.getBytes(StandardCharsets.UTF_8));
                out.flush();
                postToMain(() -> callback.onSuccess(null));
            } catch (Exception e) {
                postToMain(() -> callback.onError(e));
            }
        });
    }

    public static void readFromUri(Context context, Uri uri, Callback<String> callback) {
        executor.execute(() -> {
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                if (in == null) throw new IllegalStateException("Cannot open input stream");
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                postToMain(() -> callback.onSuccess(sb.toString()));
            } catch (Exception e) {
                postToMain(() -> callback.onError(e));
            }
        });
    }

    // --- REPOSITORY / DATABASE EXPORT & IMPORT ---

    public static void exportAllData(Context context, boolean isCsv, Callback<String> callback) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<Wallet> wallets = db.walletDao().getAllWalletsSync();
                List<ExpenseModel> expenses = db.expenseDao().getAllExpensesSync();
                List<CategoryModel> categories = db.categoryDao().getAllCategoriesSync();
                List<SavingsGoal> goals = db.savingsGoalDao().getAllGoalsSync();
                List<CategoryBudget> categoryBudgets = db.categoryBudgetDao().getAllCategoryBudgetsSync();

                String output = isCsv
                        ? generateCsv(wallets, expenses)
                        : generateJson(context, wallets, expenses, categories, goals, categoryBudgets);
                postToMain(() -> callback.onSuccess(output));
            } catch (Exception e) {
                postToMain(() -> callback.onError(e));
            }
        });
    }

    public static void importBackupData(Context context, BackupData data, boolean replaceExisting, Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);

                if (replaceExisting) {
                    db.expenseDao().deleteAll();
                    db.walletDao().deleteAll();
                    if (data.categories != null && !data.categories.isEmpty()) {
                        db.categoryDao().deleteAll();
                    }
                    if (data.savingsGoals != null && !data.savingsGoals.isEmpty()) {
                        db.savingsGoalDao().deleteAll();
                    }
                    if (data.categoryBudgets != null && !data.categoryBudgets.isEmpty()) {
                        db.categoryBudgetDao().deleteAll();
                    }
                }

                // Restore user settings if present in JSON backup
                if (context != null) {
                    if (data.userName != null && !data.userName.isEmpty()) {
                        CurrencyHelper.saveName(context, data.userName);
                    }
                    if (data.currencyCode != null && data.currencySymbol != null) {
                        CurrencyHelper.saveCurrency(context, data.currencyCode, data.currencySymbol);
                    }
                    if (data.monthlyBudget != null && data.monthlyBudget >= 0) {
                        CurrencyHelper.saveBudget(context, data.monthlyBudget);
                    }
                }

                // Restore custom categories
                if (data.categories != null && !data.categories.isEmpty()) {
                    List<CategoryModel> existingCats = db.categoryDao().getAllCategoriesSync();
                    Map<String, Boolean> existingCatNames = new HashMap<>();
                    for (CategoryModel c : existingCats) {
                        existingCatNames.put(c.getName().toLowerCase(), true);
                    }
                    for (CategoryModel c : data.categories) {
                        if (c.isExpense() && !existingCatNames.containsKey(c.getName().toLowerCase())) {
                            db.categoryDao().insert(new CategoryModel(c.getName(), c.getIcon(), c.getColor(), true));
                            existingCatNames.put(c.getName().toLowerCase(), true);
                        }
                    }
                }

                // Restore savings goals
                if (data.savingsGoals != null && !data.savingsGoals.isEmpty()) {
                    List<SavingsGoal> existingGoals = db.savingsGoalDao().getAllGoalsSync();
                    Map<String, Boolean> existingGoalTitles = new HashMap<>();
                    for (SavingsGoal g : existingGoals) {
                        existingGoalTitles.put(g.getTitle().toLowerCase(), true);
                    }
                    for (SavingsGoal g : data.savingsGoals) {
                        if (!existingGoalTitles.containsKey(g.getTitle().toLowerCase())) {
                            db.savingsGoalDao().insert(new SavingsGoal(
                                    g.getTitle(), g.getTargetAmount(), g.getSavedAmount(),
                                    g.getIcon(), g.getColor(), g.getTargetDate()
                            ));
                            existingGoalTitles.put(g.getTitle().toLowerCase(), true);
                        }
                    }
                }

                // Restore category budgets
                if (data.categoryBudgets != null && !data.categoryBudgets.isEmpty()) {
                    for (CategoryBudget cb : data.categoryBudgets) {
                        db.categoryBudgetDao().insertOrUpdate(cb);
                    }
                }

                List<Wallet> existingWallets = db.walletDao().getAllWalletsSync();
                Map<String, Integer> walletNameToId = new HashMap<>();
                for (Wallet w : existingWallets) {
                    walletNameToId.put(w.getName().toLowerCase(), w.getId());
                }

                // Insert or reuse wallets
                Map<Integer, Integer> oldToNewWalletId = new HashMap<>();
                if (data.wallets != null) {
                    for (Wallet w : data.wallets) {
                        String nameKey = w.getName().toLowerCase();
                        if (walletNameToId.containsKey(nameKey)) {
                            int existingId = walletNameToId.get(nameKey);
                            oldToNewWalletId.put(w.getId(), existingId);
                        } else {
                            // Insert wallet
                            Wallet insertW = new Wallet(w.getName(), w.getIcon(), w.getColor());
                            insertW.setBalance(w.getBalance());
                            long newId = db.walletDao().insert(insertW);
                            walletNameToId.put(nameKey, (int) newId);
                            oldToNewWalletId.put(w.getId(), (int) newId);
                        }
                    }
                }

                // If no wallets at all, ensure at least one default wallet
                if (walletNameToId.isEmpty()) {
                    Wallet defaultW = new Wallet("Main Wallet", "ic_wallet", "#10B981");
                    long defaultId = db.walletDao().insert(defaultW);
                    walletNameToId.put("main wallet", (int) defaultId);
                }

                int fallbackWalletId = walletNameToId.values().iterator().next();

                // Insert expenses
                int importedCount = 0;
                if (data.expenses != null) {
                    for (ExpenseModel e : data.expenses) {
                        int mappedWalletId = oldToNewWalletId.getOrDefault(e.getWalletId(), fallbackWalletId);
                        ExpenseModel toInsert = new ExpenseModel(
                                e.getTitle(),
                                e.getDate(),
                                e.getAmount(),
                                e.getCategory(),
                                e.isExpense(),
                                mappedWalletId
                        );
                        toInsert.setRecurring(e.isRecurring());
                        toInsert.setRecurringInterval(e.getRecurringInterval());
                        toInsert.setNextDueDate(e.getNextDueDate());

                        db.expenseDao().insert(toInsert);

                        // Update wallet balance if merging without initial pre-calculated balance
                        if (!replaceExisting) {
                            Wallet w = db.walletDao().getWalletByIdSync(mappedWalletId);
                            if (w != null) {
                                if (toInsert.isExpense()) {
                                    w.setBalance(w.getBalance() - toInsert.getAmount());
                                } else {
                                    w.setBalance(w.getBalance() + toInsert.getAmount());
                                }
                                db.walletDao().update(w);
                            }
                        }
                        importedCount++;
                    }
                }

                final int count = importedCount;
                postToMain(() -> callback.onSuccess(count));
            } catch (Exception e) {
                postToMain(() -> callback.onError(e));
            }
        });
    }
}
