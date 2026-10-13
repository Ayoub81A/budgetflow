package com.hmaidi.budgetflow;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class UserFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;
    ImageView ivProfile;

    private ActivityResultLauncher<Intent> exportCsvLauncher;
    private ActivityResultLauncher<Intent> exportJsonLauncher;
    private ActivityResultLauncher<Intent> importFileLauncher;
    private ActivityResultLauncher<Intent> exportStatementLauncher;
    private String pendingStatementHtml;
    private Integer targetCsvWalletId = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exportStatementLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && pendingStatementHtml != null) {
                            FinancialReportHelper.saveHtmlToUri(getContext(), uri, pendingStatementHtml);
                        }
                    }
                });

        exportCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri, true);
                        }
                    }
                });

        exportJsonLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri, false);
                        }
                    }
                });

        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performImport(uri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Views
        ivProfile = view.findViewById(R.id.iv_profile);
        ImageView ivEditPhoto = view.findViewById(R.id.iv_edit_photo);
        TextView tvProfileName = view.findViewById(R.id.tv_profile_name);
        TextView tvCurrencyDisplay = view.findViewById(R.id.tv_currency_display);
        TextView tvCurrentCurrency = view.findViewById(R.id.tv_current_currency);
        TextView tvBudgetValue = view.findViewById(R.id.tv_budget_value);
        Button btnEditName = view.findViewById(R.id.btn_edit_name);
        LinearLayout rowCurrency = view.findViewById(R.id.row_currency);
        LinearLayout rowBudget = view.findViewById(R.id.row_budget);
        LinearLayout rowExportData = view.findViewById(R.id.row_export_data);
        LinearLayout rowImportData = view.findViewById(R.id.row_import_data);
        LinearLayout rowManageCategories = view.findViewById(R.id.row_manage_categories);
        LinearLayout rowTrashBin = view.findViewById(R.id.row_trash_bin);
        TextView tvTrashCount = view.findViewById(R.id.tv_trash_count);
        LinearLayout rowClearData = view.findViewById(R.id.row_clear_data);
        SwitchCompat switchNotifications = view.findViewById(R.id.switch_notifications);
        SwitchCompat switchTheme = view.findViewById(R.id.switch_theme);

        LinearLayout rowRecurringBills = view.findViewById(R.id.row_recurring_bills);
        TextView tvRecurringBillsCount = view.findViewById(R.id.tv_recurring_bills_count);
        LinearLayout rowFinancialStatement = view.findViewById(R.id.row_financial_statement);

        ExpenseViewModel expenseViewModel = new ViewModelProvider(requireActivity())
                .get(ExpenseViewModel.class);
        expenseViewModel.getTrashedExpenses().observe(getViewLifecycleOwner(), trashed -> {
            if (trashed == null || trashed.isEmpty()) {
                tvTrashCount.setText("Empty");
            } else {
                tvTrashCount.setText(trashed.size() + (trashed.size() == 1 ? " item" : " items"));
            }
        });

        expenseViewModel.getAllRecurringExpenses().observe(getViewLifecycleOwner(), items -> {
            if (tvRecurringBillsCount != null) {
                if (items == null || items.isEmpty()) {
                    tvRecurringBillsCount.setText("None");
                } else {
                    tvRecurringBillsCount.setText(items.size() + " active");
                }
            }
        });

        if (rowRecurringBills != null) {
            rowRecurringBills.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RecurringBillsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (rowFinancialStatement != null) {
            rowFinancialStatement.setOnClickListener(v -> showFinancialStatementDialog());
        }

        rowTrashBin.setOnClickListener(v -> showTrashDialog());
        rowManageCategories.setOnClickListener(v -> showCategoriesDialog());

        // Load saved values
        tvProfileName.setText(CurrencyHelper.getName(getContext()));
        updateCurrencyDisplay(tvCurrencyDisplay, tvCurrentCurrency);
        updateBudgetDisplay(tvBudgetValue);
        switchNotifications.setChecked(CurrencyHelper.getNotifications(getContext()));
        switchTheme.setChecked(CurrencyHelper.isDarkTheme(getContext()));

        // Edit photo
        ivEditPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        // Edit name
        btnEditName.setOnClickListener(v -> {
            EditText etName = new EditText(getContext());
            etName.setText(CurrencyHelper.getName(getContext()));
            etName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            etName.setHint("Enter your name");

            FrameLayout dialogContainer = new FrameLayout(requireContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (20 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin / 2, margin, 0);
            etName.setLayoutParams(params);
            dialogContainer.addView(etName);

            new AlertDialog.Builder(getContext())
                    .setTitle("Edit Name")
                    .setView(dialogContainer)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String name = etName.getText().toString().trim();
                        if (!name.isEmpty()) {
                            CurrencyHelper.saveName(getContext(), name);
                            tvProfileName.setText(name);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Currency picker
        rowCurrency.setOnClickListener(v ->
                showCurrencyPicker(tvCurrencyDisplay, tvCurrentCurrency));

        // Budget limit
        rowBudget.setOnClickListener(v -> {
            EditText etBudget = new EditText(getContext());
            etBudget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            double current = CurrencyHelper.getBudget(getContext());
            if (current > 0) etBudget.setText(String.valueOf(current));
            etBudget.setHint("Enter monthly budget");
            etBudget.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

            FrameLayout dialogContainer = new FrameLayout(requireContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (20 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin / 2, margin, 0);
            etBudget.setLayoutParams(params);
            dialogContainer.addView(etBudget);

            new AlertDialog.Builder(getContext())
                    .setTitle("Monthly Budget Limit")
                    .setView(dialogContainer)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String val = etBudget.getText().toString().trim();
                        if (!val.isEmpty()) {
                            CurrencyHelper.saveBudget(getContext(),
                                    Double.parseDouble(val));
                            updateBudgetDisplay(tvBudgetValue);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) ->
                CurrencyHelper.saveNotifications(getContext(), isChecked));

        // Theme toggle
        switchTheme.setOnCheckedChangeListener((btn, isChecked) -> {
            CurrencyHelper.saveTheme(getContext(), isChecked);
            if (getActivity() != null) {
                getActivity().getIntent().putExtra("start_tab", "user");
            }
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Export Data
        rowExportData.setOnClickListener(v -> showExportDialog());

        // Import Data
        rowImportData.setOnClickListener(v -> showImportOptionsDialog());

        // Clear all data
        rowClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Clear All Data")
                    .setMessage("This will permanently delete all transactions and wallets. This cannot be undone!")
                    .setPositiveButton("Clear Everything", (dialog, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(requireContext());
                            db.expenseDao().deleteAll();
                            db.walletDao().deleteAll();
                            Wallet defaultW = new Wallet("Main Wallet", "ic_wallet", "#6366F1");
                            db.walletDao().insert(defaultW);

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "All data cleared", Toast.LENGTH_SHORT).show());
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }

    private void showFinancialStatementDialog() {
        if (getContext() == null) return;

        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);

        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Statement Month")
                .setSingleChoiceItems(monthNames, currentMonth - 1, (monthDialog, selectedIndex) -> {
                    monthDialog.dismiss();
                    int targetMonth = selectedIndex + 1;
                    showStatementActionDialog(targetMonth, currentYear, monthNames[selectedIndex]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStatementActionDialog(int month, int year, String monthName) {
        if (getContext() == null) return;

        String[] actions = {"Print / Save as PDF 📄", "Share HTML Statement 📤", "Save File to Storage 💾"};

        new AlertDialog.Builder(requireContext())
                .setTitle(monthName + " " + year + " Statement")
                .setItems(actions, (dialog, which) -> {
                    Toast.makeText(getContext(), "Generating statement...", Toast.LENGTH_SHORT).show();

                    Executors.newSingleThreadExecutor().execute(() -> {
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
                                String filename = "BudgetFlow_Statement_" + monthName + "_" + year;
                                if (which == 0) {
                                    FinancialReportHelper.printOrExportPdf(requireActivity(), html, filename);
                                } else if (which == 1) {
                                    FinancialReportHelper.shareHtmlReport(requireContext(), html, filename + ".html");
                                } else {
                                    pendingStatementHtml = html;
                                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                                    intent.setType("text/html");
                                    intent.putExtra(Intent.EXTRA_TITLE, filename + ".html");
                                    exportStatementLauncher.launch(intent);
                                }
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to generate statement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showExportDialog() {
        String[] options = {"CSV (Excel / Spreadsheets)", "JSON (Full Backup)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Export Data")
                .setItems(options, (dialog, which) -> {
                    String dateStamp = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("text/csv");
                        intent.putExtra(Intent.EXTRA_TITLE, "budgetflow_export_" + dateStamp + ".csv");
                        exportCsvLauncher.launch(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/json");
                        intent.putExtra(Intent.EXTRA_TITLE, "budgetflow_backup_" + dateStamp + ".json");
                        exportJsonLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performExport(Uri uri, boolean isCsv) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Exporting data...", Toast.LENGTH_SHORT).show();
        DataExportImportHelper.exportAllData(getContext(), isCsv, new DataExportImportHelper.Callback<String>() {
            @Override
            public void onSuccess(String content) {
                if (getContext() == null) return;
                DataExportImportHelper.writeToUri(getContext(), uri, content, new DataExportImportHelper.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Exported successfully!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showImportOptionsDialog() {
        if (getContext() == null) return;
        String[] options = {"Import Bank Statement (CSV)", "Import Full Backup (JSON)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Import Data")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        promptSelectTargetWalletForCsv();
                    } else {
                        targetCsvWalletId = null;
                        startImportPicker();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptSelectTargetWalletForCsv() {
        if (getContext() == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<Wallet> wallets = db.walletDao().getAllWalletsSync();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (wallets == null || wallets.isEmpty()) {
                    targetCsvWalletId = null;
                    startImportPicker();
                    return;
                }
                String[] walletOptions = new String[wallets.size() + 1];
                walletOptions[0] = "Auto-detect / Use CSV Wallets";
                for (int i = 0; i < wallets.size(); i++) {
                    walletOptions[i + 1] = wallets.get(i).getName();
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Select Target Wallet for Statement")
                        .setItems(walletOptions, (d, sel) -> {
                            if (sel == 0) {
                                targetCsvWalletId = null;
                            } else {
                                targetCsvWalletId = wallets.get(sel - 1).getId();
                            }
                            startImportPicker();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void startImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/json", "text/plain", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importFileLauncher.launch(intent);
    }

    private void performImport(Uri uri) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Reading file...", Toast.LENGTH_SHORT).show();
        DataExportImportHelper.readFromUri(getContext(), uri, new DataExportImportHelper.Callback<String>() {
            @Override
            public void onSuccess(String content) {
                if (getContext() == null || content == null || content.trim().isEmpty()) {
                    Toast.makeText(getContext(), "Selected file is empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                String trimmed = content.trim();
                boolean isJson = trimmed.startsWith("{");

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(getContext());
                        List<Wallet> existingWallets = db.walletDao().getAllWalletsSync();

                        DataExportImportHelper.BackupData data;
                        if (isJson) {
                            data = DataExportImportHelper.parseJson(trimmed);
                        } else {
                            data = DataExportImportHelper.parseCsv(trimmed, existingWallets, targetCsvWalletId);
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> showImportConfirmDialog(data));
                        }
                    } catch (Exception e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Failed to parse file: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to read file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showImportConfirmDialog(DataExportImportHelper.BackupData data) {
        if (getContext() == null) return;
        int expenseCount = data.expenses != null ? data.expenses.size() : 0;
        int walletCount = data.wallets != null ? data.wallets.size() : 0;

        if (data.currencyCode == null && walletCount == 0) {
            // This is a CSV bank statement import
            new AlertDialog.Builder(requireContext())
                    .setTitle("Import Bank Statement")
                    .setMessage("Found " + expenseCount + " transaction(s) in this CSV statement.\n\nImport them into your records now?")
                    .setPositiveButton("Import", (dialog, which) -> executeImport(data, false))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        String msg = "Found " + expenseCount + " transaction(s) and " + walletCount + " wallet(s).\n\n"
                + "Choose how to import:\n"
                + "• Merge: Keep current data and add these transactions.\n"
                + "• Replace: Clear current data and restore this backup.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Import")
                .setMessage(msg)
                .setPositiveButton("Merge", (dialog, which) -> executeImport(data, false))
                .setNeutralButton("Replace", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Warning")
                            .setMessage("Replacing data will delete all existing transactions and wallets. Continue?")
                            .setPositiveButton("Yes, Replace", (d2, w2) -> executeImport(data, true))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeImport(DataExportImportHelper.BackupData data, boolean replaceExisting) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Importing data...", Toast.LENGTH_SHORT).show();
        DataExportImportHelper.importBackupData(getContext(), data, replaceExisting, new DataExportImportHelper.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Successfully imported " + count + " transactions!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK
                && data != null) {
            Uri imageUri = data.getData();
            ivProfile.setImageURI(imageUri);
            ivProfile.setPadding(0, 0, 0, 0);
            ivProfile.setBackgroundTintList(null);
            ivProfile.setColorFilter(null);
        }
    }

    private void updateCurrencyDisplay(TextView tvDisplay, TextView tvCurrent) {
        String code = CurrencyHelper.getCode(getContext());
        String symbol = CurrencyHelper.getSymbol(getContext());
        tvDisplay.setText(code + " — " + symbol);
        tvCurrent.setText(code);
    }

    private void updateBudgetDisplay(TextView tvBudget) {
        double budget = CurrencyHelper.getBudget(getContext());
        if (budget > 0) {
            tvBudget.setText(CurrencyHelper.format(getContext(), budget));
        } else {
            tvBudget.setText("Not set");
        }
    }

    private void showCurrencyPicker(TextView tvDisplay, TextView tvCurrent) {
        List<CurrencyData.Currency> currencies = CurrencyData.getCurrencies();
        String[] options = new String[currencies.size()];
        for (int i = 0; i < currencies.size(); i++) {
            options[i] = currencies.get(i).toString();
        }

        String currentCode = CurrencyHelper.getCode(getContext());
        int currentIndex = 0;
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).code.equals(currentCode)) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Select Currency")
                .setSingleChoiceItems(options, currentIndex, null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    androidx.appcompat.app.AlertDialog d = (androidx.appcompat.app.AlertDialog) dialog;
                    int selected = d.getListView().getCheckedItemPosition();
                    if (selected >= 0 && selected < currencies.size()) {
                        CurrencyData.Currency currency = currencies.get(selected);
                        CurrencyHelper.saveCurrency(
                                getContext(), currency.code, currency.symbol);
                        updateCurrencyDisplay(tvDisplay, tvCurrent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTrashDialog() {
        ExpenseViewModel expenseViewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        List<ExpenseModel> trashedList = expenseViewModel.getTrashedExpenses().getValue();
        if (trashedList == null || trashedList.isEmpty()) {
            Toast.makeText(getContext(), "Recycle Bin is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itemTitles = new String[trashedList.size()];
        for (int i = 0; i < trashedList.size(); i++) {
            ExpenseModel e = trashedList.get(i);
            String sign = e.isExpense() ? "-" : "+";
            itemTitles[i] = e.getTitle() + " (" + sign + CurrencyHelper.format(getContext(), e.getAmount()) + ")\n" + e.getDate() + " • " + e.getCategory();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Recycle Bin (" + trashedList.size() + " items)")
                .setItems(itemTitles, (dialog, which) -> {
                    ExpenseModel selected = trashedList.get(which);
                    new AlertDialog.Builder(requireContext())
                            .setTitle(selected.getTitle())
                            .setMessage("Amount: " + CurrencyHelper.formatSigned(getContext(), selected.getAmount(), selected.isExpense()) + "\nDate: " + selected.getDate() + "\nCategory: " + selected.getCategory())
                            .setPositiveButton("Restore", (d, w) -> {
                                expenseViewModel.restoreFromTrash(selected);
                                Toast.makeText(getContext(), "Transaction restored!", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Delete Forever", (d, w) -> {
                                expenseViewModel.permanentDelete(selected);
                                Toast.makeText(getContext(), "Permanently deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNeutralButton("Cancel", null)
                            .show();
                })
                .setPositiveButton("Empty Bin", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Empty Recycle Bin?")
                            .setMessage("All " + trashedList.size() + " items will be permanently erased. This cannot be undone.")
                            .setPositiveButton("Empty All", (d, w) -> {
                                expenseViewModel.emptyTrash();
                                Toast.makeText(getContext(), "Recycle Bin emptied", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCategoriesDialog() {
        if (getContext() == null) return;
        CategoryViewModel categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        androidx.lifecycle.Observer<List<CategoryModel>> observer = new androidx.lifecycle.Observer<List<CategoryModel>>() {
            @Override
            public void onChanged(List<CategoryModel> categories) {
                categoryViewModel.getExpenseCategories().removeObserver(this);
                if (getContext() == null || categories == null) return;
                showCategoriesListDialog(categoryViewModel, categories);
            }
        };
        categoryViewModel.getExpenseCategories().observe(getViewLifecycleOwner(), observer);
    }

    private void showCategoriesListDialog(CategoryViewModel categoryViewModel, List<CategoryModel> categories) {
        String[] catNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            CategoryModel c = categories.get(i);
            catNames[i] = c.getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Custom Categories (" + categories.size() + ")")
                .setItems(catNames, (dialog, which) -> {
                    CategoryModel selectedCat = categories.get(which);
                    String[] actions = {"Edit Category", "Delete Category"};
                    new AlertDialog.Builder(requireContext())
                            .setTitle(selectedCat.getName())
                            .setItems(actions, (d, actIdx) -> {
                                if (actIdx == 0) {
                                    CategoryEditorHelper.showEditorDialog(requireContext(), categoryViewModel, selectedCat, null);
                                } else {
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("Delete Category")
                                            .setMessage("Delete '" + selectedCat.getName() + "'?\n\nAll existing transactions with this category will automatically be moved to 'Uncategorized'.")
                                            .setPositiveButton("Delete", (d2, w2) -> {
                                                categoryViewModel.deleteCategoryAndReassign(selectedCat, "Uncategorized");
                                                Toast.makeText(getContext(), "'" + selectedCat.getName() + "' deleted. Transactions moved to Uncategorized.", Toast.LENGTH_SHORT).show();
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setPositiveButton("+ Add Category", (dialog, which) -> {
                    CategoryEditorHelper.showEditorDialog(requireContext(), categoryViewModel, null, null);
                })
                .setNegativeButton("Close", null)
                .show();
    }
}