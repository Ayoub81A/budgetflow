# BudgetFlow 💰📱

[![Latest Release](https://img.shields.io/github/v/release/Ayoub81A/budgetflow?color=10B981&label=Download%20APK&logo=android)](https://github.com/Ayoub81A/budgetflow/releases/latest)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue?logo=android)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-green?logo=android)](https://developer.android.com)
[![Java 17](https://img.shields.io/badge/Java-17-orange?logo=java)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A modern, privacy-first, offline-first personal finance and expense tracking Android application built with native Java, Android Jetpack (Room, ViewModel, LiveData, WorkManager), MPAndroidChart, and Material Design 3.

---

## 📥 Download & Install

Get the latest installable release APK directly from GitHub Releases:

👉 **[Download BudgetFlow v1.0 (APK)](https://github.com/Ayoub81A/budgetflow/releases/latest/download/BudgetFlow-v1.0.apk)**

> [!NOTE]
> BudgetFlow is completely offline and private — no analytics, no external servers, no account required. Your financial data stays 100% on your device.

---

## ✨ Features

### 📊 Dashboard & Smart Budget Tracking
- **Real-Time Financial Health**: Instantly view Total Balance, Total Income, and Total Expenses with high-contrast cards.
- **Monthly Budget Progress Tracker**:
  - Live progress bar with color-coded safety indicators:
    - 🟢 **Purple / Green** (< 75%): Spending within healthy limits.
    - 🟡 **Amber / Orange** (75%–99%): Approaching monthly budget limit.
    - 🔴 **Red** (≥ 100%): Budget exceeded alert.
  - Accurate current-calendar-month calculation and one-tap quick budget adjustment directly from the Home screen.
- **Category-Specific Budgets**:
  - Set custom monthly spending thresholds per category (e.g. Groceries, Dining, Entertainment).
  - Progress bars with real-time percentage indicators and remaining allowance calculations.
- **Dynamic Greetings**: Time-aware personalized greeting (Good Morning, Good Afternoon, Good Evening, Good Night).

### 💳 Multi-Wallet Management & Balance Transfers
- **Multiple Wallets**: Create Cash, Bank, Savings, Credit, and Custom wallets with custom icons and color swatches.
- **Inter-Wallet Transfers**: Move funds seamlessly between wallets (e.g. Bank ➔ Cash) with automatic balance recalculation and history auditing.
- **Per-Wallet Ledgers**: Filter and view transaction histories isolated to specific accounts.

### 💸 Income & Expense Management
- **Custom Categories**: Create, manage, and pick custom Expense and Income categories dynamically with dedicated icons.
- **Notes & Tagging**: Add freeform notes and `#hashtag` categorization to any transaction.
- **Receipt Photo Attachments**: Attach photos of physical receipts directly to transactions with full-screen pinch-to-zoom inspection.
- **Swipe-to-Delete with Undo**: Fluid swipe gestures to move transactions to trash, paired with immediate `Snackbar` Undo recovery.
- **Recycle Bin (Trash Area)**:
  - Soft-delete protection against accidental data loss.
  - Full inspection, 1-tap **Restore** (re-syncs wallet balance), and **Delete Forever**.
  - Automatic 30-day retention cleanup.
- **Transaction Edit & Modification History**:
  - Edit title, amount, date, wallet, and category with automatic balance recalculations.
  - Complete timestamped audit trail of past revisions with 1-tap reversion.

### 📈 Visual Charts & Spending Analytics
- **Spending Trend Bar Chart**: Side-by-side Income vs. Expense comparison grouped by day or month powered by MPAndroidChart.
- **Interactive Pie Chart**: Visual breakdown of expenses by category with interactive slice selection.
- **Flexible Time Ranges**: Seamlessly switch between **This Week**, **This Month**, **This Year**, and **All Time**.
- **Net Savings Overview**: Visual breakdown of Period Income, Period Expenses, and Net Savings (`Income - Expenses`).

### 🔒 Biometric & PIN Security
- **App Lock**: Optional biometric authentication (Fingerprint / Face Unlock via AndroidX Biometric).
- **4-Digit Secure PIN**: Custom tactile numpad fallback with SHA-256 secure hashing.
- **Smart Session Timeout**: Automatically locks if the app remains in the background for more than 60 seconds.

### 📳 Tactile Micro-Interactions & UX Polish
- **Haptic Feedback**: Safe, multi-level tactile feedback (`vibrateTick`, `vibrateConfirm`, `vibrateError`) integrated into the PIN numpad, expense/income toggles, category chips, and swipe actions.
- **Branded Splash Screen**: Android 12+ splash screen (`androidx.core:core-splashscreen`) with custom brand mark and dark navy background `#283549`.
- **Adaptive App Icon**: Custom high-res adaptive icon with safe-zone compliant foreground across all screen densities (`mdpi` through `xxxhdpi`).

### 🔔 Smart Budget Notifications & Thresholds
- **80% Spending Warning**: Local notification alerts you when current-month spending reaches 80% of your budget limit.
- **100% Budget Exceeded**: Instant alert when monthly expenses surpass your budget limit.
- **Smart Deduplication**: Avoids notification spam by alerting once per threshold per calendar month.
- **Android 13+ Support**: Runtime notification permissions on modern Android versions.

### 🔄 Recurring Transactions & Automation
- **Flexible Frequencies**: Set recurring expenses on **Daily**, **Weekly**, **Monthly**, or **Yearly** schedules.
- **WorkManager Background Engine**: Automatically processes due recurring transactions every 24 hours.
- **Instant App-Startup Catch-Up**: Immediate processing upon launching the app if a due date passed while the device was off.

### 💾 Data Export, Import & Bank Statement Parsing
- **CSV Export**: Export all transactions to a cleanly formatted `.csv` with Excel-compatible UTF-8 BOM.
- **Universal Bank Statement CSV Import**: Ingest raw bank statements with automatic column detection, handling negative amounts, currency symbols, and commas.
- **Full JSON Backup & Restore**: Backup and restore complete database states including wallets, balances, category budgets, savings goals, and recurring rules.
- **Merge & Replace Modes**: Choose between appending imported records or cleanly restoring a backup.
- **Storage Access Framework (SAF)**: Uses native Android file pickers with zero unnecessary storage permissions.

### ⚙️ Personalization & Theming
- **Dark Mode**: System-wide Dark Theme support with high-contrast surfaces and persistent state.
- **Multi-Currency Support**: Switch between USD (`$`), EUR (`€`), GBP (`£`), TND (`DT`), CAD (`$`), JPY (`¥`), and custom symbols.
- **Profile Customization**: Set personalized user profile name and manage notification preferences.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Minimum SDK** | Android 7.0 (API 24) |
| **Target SDK** | Android 15 / 16 Preview (API 36) |
| **Architecture** | MVVM (Model-View-ViewModel) + Repository Pattern |
| **Database** | SQLite via Android Room Persistence Library (`AppDatabase`, DAOs) |
| **Reactive State** | Android Architecture Components (`LiveData`, `ViewModel`) |
| **Background Processing** | Android Jetpack WorkManager (`PeriodicWorkRequest`) |
| **Charting Engine** | [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) |
| **Security** | AndroidX Biometric (`BiometricPrompt`), SHA-256 PIN hashing |
| **Splash Screen** | `androidx.core:core-splashscreen` |
| **Design System** | Google Material Design 3, ConstraintLayout, CardView |

---

## 📁 Project Structure

```text
com.hmaidi.budgetflow/
├── data/
│   ├── AppDatabase.java            # Room Database singleton (v5)
│   ├── ExpenseDao.java              # DAO queries for expenses & recurring rules
│   ├── ExpenseModel.java            # Room Entity for transactions & receipts
│   ├── ExpenseRepository.java       # Repository mediating Room & background Executors
│   ├── Wallet.java                  # Room Entity for multi-wallet accounts
│   ├── WalletDao.java               # DAO queries for wallets
│   ├── WalletRepository.java        # Repository for wallet management
│   ├── CategoryBudget.java          # Room Entity for per-category budgets
│   ├── CategoryBudgetDao.java       # DAO queries for category budgets
│   └── SavingsGoal.java             # Room Entity for savings targets
├── ui/
│   ├── MainActivity.java            # Host activity, bottom nav, WorkManager scheduler
│   ├── HomeFragment.java            # Dashboard, balance cards, & budget tracker
│   ├── StatsFragment.java           # MPAndroidChart Bar & Pie charts
│   ├── WalletFragment.java          # Wallets grid & inter-wallet transfer entry
│   ├── WalletDetailFragment.java    # Per-wallet ledger & statistics
│   ├── AddExpenseFragment.java      # Modal sheet for new expenses, receipts & tags
│   ├── AddWalletFragment.java       # Modal sheet for new wallet creation
│   ├── TransferDialogFragment.java  # Inter-wallet transfer dialog
│   ├── AllTransactionsFragment.java  # Search, filter chips, & transaction list
│   ├── ExpenseDetailFragment.java   # Transaction inspection & deletion sheet
│   ├── UserFragment.java            # Profile, theme toggle, currency & security lock
│   └── LockActivity.java            # PIN & Biometric authentication screen
├── adapter/
│   ├── ExpenseAdapter.java          # RecyclerView adapter for transaction items
│   ├── WalletAdapter.java           # RecyclerView adapter for wallet cards
│   └── CategoryAdapter.java         # RecyclerView adapter for category chips
├── worker/
│   └── RecurringWorker.java         # WorkManager background task for recurring payments
└── utils/
    ├── CurrencyHelper.java          # Multi-currency formatting & SharedPreferences
    ├── DataExportImportHelper.java  # CSV & JSON backup/restore & bank statement parser
    ├── HapticHelper.java            # Multi-level tactile vibration feedback
    ├── NotificationHelper.java      # Budget alert push notification dispatcher
    ├── SecurityHelper.java          # App Lock state & SHA-256 PIN management
    └── SwipeToDeleteCallback.java   # Canvas swipe-to-delete gesture callback
```

---

## 🚀 Building From Source

### Prerequisites
- **Android Studio**: Android Studio Ladybug (or newer recommended)
- **JDK**: Java 17
- **Android SDK**: API level 24 minimum, API level 36 compile SDK

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/Ayoub81A/budgetflow.git
   cd budgetflow
   ```

2. Open the project in Android Studio.

3. Let Gradle sync project dependencies.

4. Build & install via Command Line:
   ```powershell
   # Build installable release APK
   .\gradlew assembleRelease

   # Run automated unit test suite
   .\gradlew testDebugUnitTest
   ```

5. Find the built APK at:
   `app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
