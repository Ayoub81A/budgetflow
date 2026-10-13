package com.hmaidi.budgetflow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinancialReportHelper {

    public static String generateHtmlReport(Context context,
                                            int year,
                                            int month,
                                            List<ExpenseModel> allExpenses,
                                            List<Wallet> wallets,
                                            List<CategoryBudget> categoryBudgets,
                                            Double monthlyOverallBudget,
                                            String userName) {

        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        String monthName = (month >= 1 && month <= 12) ? monthNames[month - 1] : "Month " + month;
        String periodLabel = monthName + " " + year;

        // Map wallets
        Map<Integer, String> walletMap = new HashMap<>();
        if (wallets != null) {
            for (Wallet w : wallets) {
                walletMap.put(w.getId(), w.getName());
            }
        }

        // Map category budgets
        Map<String, Double> budgetMap = new HashMap<>();
        if (categoryBudgets != null) {
            for (CategoryBudget cb : categoryBudgets) {
                budgetMap.put(cb.getCategoryName(), cb.getMonthlyLimit());
            }
        }

        // Filter transactions for this month and year
        List<ExpenseModel> monthTransactions = new ArrayList<>();
        double totalIncome = 0.0;
        double totalExpense = 0.0;
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<Integer, Double> walletIncome = new HashMap<>();
        Map<Integer, Double> walletExpense = new HashMap<>();

        if (allExpenses != null) {
            for (ExpenseModel e : allExpenses) {
                if (e.isDeleted()) continue;
                String dStr = e.getDate();
                if (dStr == null) continue;
                String[] parts = dStr.trim().split("/");
                if (parts.length != 3) continue;

                try {
                    int m = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    if (m == month && y == year) {
                        monthTransactions.add(e);

                        if ("Transfer".equalsIgnoreCase(e.getCategory())) {
                            continue;
                        }

                        if (e.isExpense()) {
                            totalExpense += e.getAmount();
                            String cat = e.getCategory() != null ? e.getCategory() : "Uncategorized";
                            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + e.getAmount());
                            walletExpense.put(e.getWalletId(), walletExpense.getOrDefault(e.getWalletId(), 0.0) + e.getAmount());
                        } else {
                            totalIncome += e.getAmount();
                            walletIncome.put(e.getWalletId(), walletIncome.getOrDefault(e.getWalletId(), 0.0) + e.getAmount());
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        double netSaved = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (netSaved / totalIncome) * 100.0 : 0.0;

        // Sort transactions newest to oldest
        Collections.sort(monthTransactions, (e1, e2) -> {
            int comp = compareDateStrings(e2.getDate(), e1.getDate());
            if (comp != 0) return comp;
            return Integer.compare(e2.getId(), e1.getId());
        });

        SimpleDateFormat genFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String generatedAt = genFormat.format(new Date());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>BudgetFlow Statement - ").append(periodLabel).append("</title>\n");
        html.append("<style>\n");
        html.append("  * { box-sizing: border-box; }\n");
        html.append("  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 24px; color: #1E293B; background: #F8FAFC; }\n");
        html.append("  .container { max-width: 850px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; padding: 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }\n");
        html.append("  .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #E2E8F0; padding-bottom: 20px; margin-bottom: 24px; }\n");
        html.append("  .logo { font-size: 24px; font-weight: 800; color: #4338CA; letter-spacing: -0.5px; }\n");
        html.append("  .badge { background: #EEF2FF; color: #4338CA; font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 4px; }\n");
        html.append("  .meta { text-align: right; font-size: 13px; color: #64748B; }\n");
        html.append("  .meta strong { color: #0F172A; font-size: 14px; }\n");
        html.append("  h2 { font-size: 16px; font-weight: 700; color: #0F172A; margin: 28px 0 14px; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        html.append("  .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px; }\n");
        html.append("  .card { background: #F1F5F9; border-radius: 12px; padding: 14px; border: 1px solid #E2E8F0; }\n");
        html.append("  .card-label { font-size: 11px; font-weight: 700; color: #64748B; text-transform: uppercase; margin-bottom: 6px; }\n");
        html.append("  .card-value { font-size: 20px; font-weight: 800; }\n");
        html.append("  .text-green { color: #059669; }\n");
        html.append("  .text-red { color: #DC2626; }\n");
        html.append("  .text-indigo { color: #4338CA; }\n");
        html.append("  table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 13px; }\n");
        html.append("  th { background: #F8FAFC; text-align: left; padding: 10px 12px; font-size: 11px; text-transform: uppercase; color: #64748B; border-bottom: 2px solid #E2E8F0; }\n");
        html.append("  td { padding: 10px 12px; border-bottom: 1px solid #F1F5F9; vertical-align: middle; }\n");
        html.append("  tr:last-child td { border-bottom: none; }\n");
        html.append("  .tag { display: inline-block; padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; }\n");
        html.append("  .tag-inc { background: #ECFDF5; color: #059669; }\n");
        html.append("  .tag-exp { background: #FEF2F2; color: #DC2626; }\n");
        html.append("  .tag-trans { background: #F1F5F9; color: #64748B; }\n");
        html.append("  .footer { margin-top: 36px; padding-top: 16px; border-top: 1px solid #E2E8F0; font-size: 12px; color: #94A3B8; text-align: center; }\n");
        html.append("  @media print {\n");
        html.append("    body { background: #FFF; padding: 0; }\n");
        html.append("    .container { box-shadow: none; border-radius: 0; padding: 0; }\n");
        html.append("    tr { page-break-inside: avoid; }\n");
        html.append("  }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<div class=\"container\">\n");

        // Header
        html.append("  <div class=\"header\">\n");
        html.append("    <div>\n");
        html.append("      <div class=\"logo\">BudgetFlow</div>\n");
        html.append("      <div class=\"badge\">FINANCIAL STATEMENT</div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"meta\">\n");
        html.append("      <div>Statement Period: <strong>").append(periodLabel).append("</strong></div>\n");
        html.append("      <div>Account: <strong>").append(userName != null && !userName.isEmpty() ? userName : "BudgetFlow User").append("</strong></div>\n");
        html.append("      <div>Generated: ").append(generatedAt).append("</div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        // Executive Summary Cards
        html.append("  <h2>Monthly Overview</h2>\n");
        html.append("  <div class=\"grid-4\">\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <div class=\"card-label\">Total Income</div>\n");
        html.append("      <div class=\"card-value text-green\">+").append(CurrencyHelper.format(context, totalIncome)).append("</div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <div class=\"card-label\">Total Expenses</div>\n");
        html.append("      <div class=\"card-value text-red\">-").append(CurrencyHelper.format(context, totalExpense)).append("</div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <div class=\"card-label\">Net Saved</div>\n");
        String netClass = netSaved >= 0 ? "text-green" : "text-red";
        String netPrefix = netSaved >= 0 ? "+" : "-";
        html.append("      <div class=\"card-value ").append(netClass).append("\">").append(netPrefix).append(CurrencyHelper.format(context, Math.abs(netSaved))).append("</div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <div class=\"card-label\">Savings Rate</div>\n");
        html.append("      <div class=\"card-value text-indigo\">").append(String.format(Locale.US, "%.1f%%", savingsRate)).append("</div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        // Category Breakdown Table
        html.append("  <h2>Spending by Category</h2>\n");
        if (categoryTotals.isEmpty()) {
            html.append("  <p style=\"color: #64748B; font-size: 13px;\">No expenses recorded for this period.</p>\n");
        } else {
            html.append("  <table>\n");
            html.append("    <thead><tr><th>Category</th><th>Amount Spent</th><th>Share</th><th>Budget Limit</th><th>Status</th></tr></thead>\n");
            html.append("    <tbody>\n");

            // Sort categories by amount descending
            List<Map.Entry<String, Double>> sortedCats = new ArrayList<>(categoryTotals.entrySet());
            sortedCats.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Double> entry : sortedCats) {
                String catName = entry.getKey();
                double spent = entry.getValue();
                double share = totalExpense > 0 ? (spent / totalExpense) * 100.0 : 0.0;
                Double limit = budgetMap.get(catName);

                String statusBadge;
                String limitText;
                if (limit != null && limit > 0) {
                    limitText = CurrencyHelper.format(context, limit);
                    if (spent > limit) {
                        statusBadge = "<span class=\"tag tag-exp\">Over by " + CurrencyHelper.format(context, spent - limit) + "</span>";
                    } else {
                        statusBadge = "<span class=\"tag tag-inc\">Within Limit</span>";
                    }
                } else {
                    limitText = "No limit";
                    statusBadge = "<span class=\"tag tag-trans\">Standard</span>";
                }

                html.append("    <tr>\n");
                html.append("      <td><strong>").append(catName).append("</strong></td>\n");
                html.append("      <td>").append(CurrencyHelper.format(context, spent)).append("</td>\n");
                html.append("      <td>").append(String.format(Locale.US, "%.1f%%", share)).append("</td>\n");
                html.append("      <td>").append(limitText).append("</td>\n");
                html.append("      <td>").append(statusBadge).append("</td>\n");
                html.append("    </tr>\n");
            }
            html.append("    </tbody>\n  </table>\n");
        }

        // Wallet Activity Table
        if (wallets != null && !wallets.isEmpty()) {
            html.append("  <h2>Wallet Activity</h2>\n");
            html.append("  <table>\n");
            html.append("    <thead><tr><th>Wallet</th><th>Income</th><th>Expense</th><th>Net Flow</th></tr></thead>\n");
            html.append("    <tbody>\n");
            for (Wallet w : wallets) {
                double inc = walletIncome.getOrDefault(w.getId(), 0.0);
                double exp = walletExpense.getOrDefault(w.getId(), 0.0);
                double flow = inc - exp;
                String flowClass = flow >= 0 ? "text-green" : "text-red";
                String flowPrefix = flow >= 0 ? "+" : "-";

                html.append("    <tr>\n");
                html.append("      <td><strong>").append(w.getName()).append("</strong></td>\n");
                html.append("      <td class=\"text-green\">+").append(CurrencyHelper.format(context, inc)).append("</td>\n");
                html.append("      <td class=\"text-red\">-").append(CurrencyHelper.format(context, exp)).append("</td>\n");
                html.append("      <td class=\"").append(flowClass).append("\"><strong>").append(flowPrefix).append(CurrencyHelper.format(context, Math.abs(flow))).append("</strong></td>\n");
                html.append("    </tr>\n");
            }
            html.append("    </tbody>\n  </table>\n");
        }

        // Itemized Transaction Ledger
        html.append("  <h2>Itemized Transaction Ledger (").append(monthTransactions.size()).append(")</h2>\n");
        if (monthTransactions.isEmpty()) {
            html.append("  <p style=\"color: #64748B; font-size: 13px;\">No transactions found for this period.</p>\n");
        } else {
            html.append("  <table>\n");
            html.append("    <thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Wallet</th><th>Type</th><th style=\"text-align: right;\">Amount</th></tr></thead>\n");
            html.append("    <tbody>\n");
            for (ExpenseModel t : monthTransactions) {
                boolean isTransfer = "Transfer".equalsIgnoreCase(t.getCategory());
                String typeTag;
                String amtColor;
                String amtPrefix;

                if (isTransfer) {
                    typeTag = "<span class=\"tag tag-trans\">Transfer</span>";
                    amtColor = "#64748B";
                    amtPrefix = "";
                } else if (t.isExpense()) {
                    typeTag = "<span class=\"tag tag-exp\">Expense</span>";
                    amtColor = "#DC2626";
                    amtPrefix = "-";
                } else {
                    typeTag = "<span class=\"tag tag-inc\">Income</span>";
                    amtColor = "#059669";
                    amtPrefix = "+";
                }

                String wName = walletMap.getOrDefault(t.getWalletId(), "Default");

                html.append("    <tr>\n");
                html.append("      <td>").append(t.getDate() != null ? t.getDate() : "").append("</td>\n");
                html.append("      <td><strong>").append(t.getTitle() != null ? t.getTitle() : "").append("</strong></td>\n");
                html.append("      <td>").append(t.getCategory() != null ? t.getCategory() : "").append("</td>\n");
                html.append("      <td>").append(wName).append("</td>\n");
                html.append("      <td>").append(typeTag).append("</td>\n");
                html.append("      <td style=\"text-align: right; color: ").append(amtColor).append("; font-weight: 700;\">")
                        .append(amtPrefix).append(CurrencyHelper.format(context, t.getAmount())).append("</td>\n");
                html.append("    </tr>\n");
            }
            html.append("    </tbody>\n  </table>\n");
        }

        // Footer
        html.append("  <div class=\"footer\">\n");
        html.append("    BudgetFlow Personal Finance Manager • Confidential Financial Record\n");
        html.append("  </div>\n");

        html.append("</div>\n</body>\n</html>");

        return html.toString();
    }

    public static void printOrExportPdf(Activity activity, String htmlContent, String jobName) {
        if (activity == null || htmlContent == null) return;

        WebView webView = new WebView(activity);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                if (printManager != null) {
                    PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
                    PrintAttributes attributes = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();
                    printManager.print(jobName, printAdapter, attributes);
                }
            }
        });

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    public static void shareHtmlReport(Context context, String htmlContent, String filename) {
        if (context == null || htmlContent == null) return;

        try {
            File reportDir = new File(context.getCacheDir(), "reports");
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            File file = new File(reportDir, filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(htmlContent.getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/html");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "BudgetFlow Statement - " + filename);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Share Financial Statement"));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void saveHtmlToUri(Context context, Uri uri, String htmlContent) {
        if (context == null || uri == null || htmlContent == null) return;

        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(htmlContent.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(context, "Financial Statement exported successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static int compareDateStrings(String d1, String d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        String[] p1 = d1.trim().split("/");
        String[] p2 = d2.trim().split("/");
        if (p1.length == 3 && p2.length == 3) {
            try {
                int y1 = Integer.parseInt(p1[2]), y2 = Integer.parseInt(p2[2]);
                if (y1 != y2) return Integer.compare(y1, y2);
                int m1 = Integer.parseInt(p1[1]), m2 = Integer.parseInt(p2[1]);
                if (m1 != m2) return Integer.compare(m1, m2);
                int day1 = Integer.parseInt(p1[0]), day2 = Integer.parseInt(p2[0]);
                return Integer.compare(day1, day2);
            } catch (Exception ignored) {}
        }
        return d1.compareTo(d2);
    }
}
