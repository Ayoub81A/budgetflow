package com.hmaidi.budgetflow;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PremortemSafetyTest {

    @Test
    public void testFilterCriteria_nullSafetyAndTags() {
        TransactionFilterCriteria criteria = new TransactionFilterCriteria();

        // 1. Completely empty/null item should not crash
        ExpenseModel emptyExpense = new ExpenseModel(null, null, 0, null, true, 1);
        emptyExpense.setNotes(null);
        emptyExpense.setTags(null);
        assertTrue(criteria.matches(emptyExpense));

        // 2. Trashed item should never match
        emptyExpense.setDeleted(true);
        assertFalse(criteria.matches(emptyExpense));
        emptyExpense.setDeleted(false);

        // 3. Search query match in notes
        ExpenseModel noteExpense = new ExpenseModel("Lunch", "13/10/2026", 15.0, "Food", true, 1);
        noteExpense.setNotes("Business lunch with client");
        noteExpense.setTags("#work, #dining");

        criteria.searchQuery = "client";
        assertTrue("Search query in notes should match", criteria.matches(noteExpense));

        criteria.searchQuery = "#dining";
        assertTrue("Search query for hashtag should match", criteria.matches(noteExpense));

        criteria.searchQuery = "work";
        assertTrue("Search query for tag substring should match", criteria.matches(noteExpense));

        criteria.searchQuery = "nonexistent_query";
        assertFalse("Search query with no match should not match", criteria.matches(noteExpense));
    }

    @Test
    public void testNotesAndTagsJsonBackupRoundtrip() throws Exception {
        List<Wallet> wallets = new ArrayList<>();
        Wallet w = new Wallet("Main Wallet", "ic_wallet", "#10B981");
        w.setId(1);
        wallets.add(w);

        List<ExpenseModel> expenses = new ArrayList<>();
        ExpenseModel e = new ExpenseModel("Hardware Store", "13/10/2026", 124.99, "Home", true, 1);
        e.setId(42);
        e.setNotes("Bought drill bits and screws for garage shelf");
        e.setTags("#home, #tools, #diy");
        e.setReceiptPath("/storage/emulated/0/receipts/rec_123.jpg");
        expenses.add(e);

        String json = DataExportImportHelper.generateJson(null, wallets, expenses, new ArrayList<>(), new ArrayList<>());
        assertNotNull(json);
        assertTrue(json.contains("drill bits"));
        assertTrue(json.contains("#tools"));

        DataExportImportHelper.BackupData restored = DataExportImportHelper.parseJson(json);
        assertNotNull(restored);
        assertEquals(1, restored.expenses.size());

        ExpenseModel restoredExpense = restored.expenses.get(0);
        assertEquals("Hardware Store", restoredExpense.getTitle());
        assertEquals(124.99, restoredExpense.getAmount(), 0.001);
        assertEquals("Bought drill bits and screws for garage shelf", restoredExpense.getNotes());
        assertEquals("#home, #tools, #diy", restoredExpense.getTags());
        assertEquals("/storage/emulated/0/receipts/rec_123.jpg", restoredExpense.getReceiptPath());
    }

    @Test
    public void testNotesAndTagsCsvRoundtrip() throws Exception {
        List<Wallet> wallets = new ArrayList<>();
        Wallet w = new Wallet("Cash Wallet", "ic_wallet", "#10B981");
        w.setId(1);
        wallets.add(w);

        List<ExpenseModel> expenses = new ArrayList<>();
        ExpenseModel e = new ExpenseModel("Flight Booking", "15/10/2026", 450.00, "Travel", true, 1);
        e.setId(99);
        e.setNotes("Trip to Berlin for tech conference");
        e.setTags("#travel, #conference");
        expenses.add(e);

        String csv = DataExportImportHelper.generateCsv(wallets, expenses);
        assertNotNull(csv);
        assertTrue(csv.contains("Trip to Berlin"));
        assertTrue(csv.contains("#conference"));

        DataExportImportHelper.BackupData parsed = DataExportImportHelper.parseCsv(csv, wallets);
        assertNotNull(parsed);
        assertEquals(1, parsed.expenses.size());

        ExpenseModel parsedE = parsed.expenses.get(0);
        assertEquals("Flight Booking", parsedE.getTitle());
        assertEquals(450.00, parsedE.getAmount(), 0.001);
        assertEquals("Trip to Berlin for tech conference", parsedE.getNotes());
        assertEquals("#travel, #conference", parsedE.getTags());
    }

    @Test
    public void testCsvImportEdgeCases() throws Exception {
        // Test bank statement with currency symbols, negative formatting, commas
        String bankCsv = "Date,Description,Amount\n"
                + "2026-10-10,Supermarket Store,\"$ 85.50\"\n"
                + "11/10/2026,Coffee Shop,\"-€4.25\"\n"
                + "2026/10/12,Direct Deposit Payroll,\"+£2,400.00\"\n"
                + "   \n"; // blank line

        List<Wallet> wallets = new ArrayList<>();
        Wallet w = new Wallet("Checking", "ic_wallet", "#10B981");
        w.setId(1);
        wallets.add(w);

        DataExportImportHelper.BackupData parsed = DataExportImportHelper.parseCsv(bankCsv, wallets);
        assertEquals(3, parsed.expenses.size());

        // Expense 1: 85.50
        assertEquals("Supermarket Store", parsed.expenses.get(0).getTitle());
        assertEquals(85.50, parsed.expenses.get(0).getAmount(), 0.001);
        assertTrue(parsed.expenses.get(0).isExpense());

        // Expense 2: 4.25 negative -> expense
        assertEquals("Coffee Shop", parsed.expenses.get(1).getTitle());
        assertEquals(4.25, parsed.expenses.get(1).getAmount(), 0.001);
        assertTrue(parsed.expenses.get(1).isExpense());

        // Expense 3: 2400.00 positive -> income
        assertEquals("Direct Deposit Payroll", parsed.expenses.get(2).getTitle());
        assertEquals(2400.00, parsed.expenses.get(2).getAmount(), 0.001);
        assertFalse(parsed.expenses.get(2).isExpense());
    }

    @Test
    public void testSecurityPinHashVerification() {
        String pin = "1234";
        String hash = SecurityHelper.hashPin(pin);
        assertNotNull(hash);
        assertNotEquals(pin, hash);

        // Verification logic matches SHA-256
        String verifyHash = SecurityHelper.hashPin("1234");
        assertEquals(hash, verifyHash);

        String wrongHash = SecurityHelper.hashPin("4321");
        assertNotEquals(hash, wrongHash);

        // Null pin safety
        assertEquals("", SecurityHelper.hashPin(null));
    }

    @Test
    public void testCategoryBudgetZeroLimitSafety() {
        CategoryBudget cb = new CategoryBudget("Dining", 0.0);
        assertEquals("Dining", cb.getCategoryName());
        assertEquals(0.0, cb.getMonthlyLimit(), 0.001);

        cb.setMonthlyLimit(250.0);
        assertEquals(250.0, cb.getMonthlyLimit(), 0.001);
    }
}
