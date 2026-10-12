package com.hmaidi.budgetflow;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DataExportImportTest {

    @Test
    public void testGenerateCsvAndParse() throws Exception {
        List<Wallet> wallets = new ArrayList<>();
        Wallet w1 = new Wallet("Main Wallet", "ic_wallet", "#7B5EA7");
        w1.setId(1);
        wallets.add(w1);

        List<ExpenseModel> expenses = new ArrayList<>();
        ExpenseModel e1 = new ExpenseModel("Groceries, fruits", "12/10/2026", 45.50, "Food", true, 1);
        e1.setId(10);
        expenses.add(e1);

        ExpenseModel e2 = new ExpenseModel("Salary", "01/10/2026", 2500.00, "Income", false, 1);
        e2.setId(11);
        expenses.add(e2);

        String csv = DataExportImportHelper.generateCsv(wallets, expenses);
        assertNotNull(csv);
        assertTrue(csv.contains("Date,Title,Amount,Type,Category,Wallet,Recurring,Interval"));
        assertTrue(csv.contains("\"Groceries, fruits\""));
        assertTrue(csv.contains("Salary"));

        // Now test parseCsv
        DataExportImportHelper.BackupData parsed = DataExportImportHelper.parseCsv(csv, wallets);
        assertNotNull(parsed);
        assertEquals(2, parsed.expenses.size());

        ExpenseModel parsedE1 = parsed.expenses.get(0);
        assertEquals("Groceries, fruits", parsedE1.getTitle());
        assertEquals(45.50, parsedE1.getAmount(), 0.001);
        assertTrue(parsedE1.isExpense());
        assertEquals("Food", parsedE1.getCategory());

        ExpenseModel parsedE2 = parsed.expenses.get(1);
        assertEquals("Salary", parsedE2.getTitle());
        assertEquals(2500.00, parsedE2.getAmount(), 0.001);
        assertFalse(parsedE2.isExpense());
    }

    @Test
    public void testParseCsvWithNewWallet() throws Exception {
        String csv = "Date,Title,Amount,Type,Category,Wallet,Recurring,Interval\n"
                + "10/10/2026,Freelance,500.00,Income,Income,Crypto Wallet,No,\n";

        List<Wallet> existing = new ArrayList<>();
        DataExportImportHelper.BackupData parsed = DataExportImportHelper.parseCsv(csv, existing);

        assertEquals(1, parsed.expenses.size());
        assertEquals(1, parsed.wallets.size());
        assertEquals("Crypto Wallet", parsed.wallets.get(0).getName());
    }
}
