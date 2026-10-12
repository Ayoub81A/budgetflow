# BudgetFlow 💰📱

A modern, offline-first personal finance and expense tracking Android application built with native Java, Android Jetpack (Room, ViewModel, LiveData, WorkManager), and Material Design 3.

---

## ✨ Features

### 📊 Dashboard & Smart Budget Tracking
- **Real-Time Balance Overview**: Instantly view Total Balance, Total Income, and Total Expenses with high-contrast, legible cards.
- **Monthly Budget Progress Tracker**:
  - Live progress bar with color-coded safety indicators:
    - 🟢 **Purple / Green** (< 75%): Spending within healthy limits.
    - 🟡 **Amber / Orange** (75%–99%): Approaching budget limit.
    - 🔴 **Red** (≥ 100%): Budget exceeded alert.
  - Accurate current-calendar-month calculation.
  - One-tap quick budget adjustment directly from the Home screen.
- **Dynamic Greetings**: Time-aware personalized greeting (Good Morning, Good Afternoon, Good Evening, Good Night).

### 💳 Multi-Wallet Management
- **Custom Wallets**: Create Cash, Bank, Savings, and Custom wallets.
- **Interactive Swatch Selector**: Color-code wallets with interactive scaling preview animations.
- **Per-Wallet Tracking**: Filter and view transaction histories isolated to specific wallets.
- **Balance Auto-Sync**: Wallet balances automatically update on transaction additions and deletions.

### 💸 Income & Expense Management
- **Custom Categories**: Create, manage, and pick custom Expense and Income categories dynamically, including an inline `+ New` button directly within the entry modal.
- **Pill-Segmented Type Toggle**: High-contrast, intuitive toggling between Expense and Income.
- **Date Picker**: Built-in calendar picker for backdating or forward-planning entries.
- **Transaction Edit & Modification History**:
  - Edit title, amount, and category with automatic wallet balance recalculations.
  - View a complete timestamped audit trail of past revisions.
  - **1-Tap Reversion**: Instantly rollback any transaction to an earlier state.
- **Recycle Bin (Trash Area)**:
  - Soft-delete prevents accidental data loss; transactions sit in the Recycle Bin before permanent deletion.
  - Live item count badge, full inspection, 1-tap **Restore** (re-syncs wallet balance), and **Delete Forever**.
  - **Empty Bin** button and automatic 30-day retention cleanup.
- **Search & Filter**: Real-time keyword search and category filtering chips with dedicated empty states.

### 🎯 Savings Goals / Pockets
- **Visual Target Progress**: Set goal titles and target amounts with visual progress indicators and percentage completion.
- **Deposit Funds**: Add money directly to savings goals with one tap.
- **Goal Management**: Track savings milestones directly from the Home dashboard.

### 🔔 Smart Budget Notifications & Thresholds
- **80% Spending Warning**: Notifies you when this month's spending approaches 80% of your monthly budget.
- **100% Budget Exceeded**: Alerts you immediately when monthly expenses surpass your budget limit.
- **Smart Deduplication**: Avoids notification spam by alerting once per threshold per month.
- **Android 13+ Support**: Runtime notification permissions on modern Android versions.

### 🔄 Recurring Transactions & Automation
- **Flexible Frequencies**: Set recurring expenses on **Daily**, **Weekly**, **Monthly**, or **Yearly** schedules.
- **WorkManager Background Engine**: Automatically processes due recurring transactions every 24 hours.
- **Instant App-Startup Check**: Immediate catch-up processing upon launching the app if a due date passed while the device was off.
- **Local Notifications**: Automated reminders when a recurring transaction is recorded (respects notification settings).
- **Transaction Inspection**: View recurring recurrence intervals and next scheduled payment dates directly in transaction details.

### 📈 Analytics & Visual Charts
- **Time Range Filtering**: Seamlessly toggle between **This Week**, **This Month**, **This Year**, and **All Time** to view isolated financial insights.
- **Interactive Pie Chart**: Visual breakdown of expenses by category powered by MPAndroidChart.
- **Theme-Aware Charts**: Automatically adapts slice styling, text colors, and backgrounds between Light and Dark themes.
- **Dynamic Period Totals**: Displays period-specific Income, Expenses, and Net Savings (`Income - Expenses`).

### 💾 Data Export & Backup (CSV / JSON)
- **CSV Export**: Export all transactions to a cleanly formatted `.csv` file with Excel-compatible UTF-8 BOM, ready for Google Sheets or Microsoft Excel.
- **JSON Full Backup**: Back up complete database states including wallets, balances, and recurring transaction schedules into portable `.json` files.
- **Smart Data Import**:
  - Automatically identifies whether an imported file is CSV or JSON.
  - **Merge Mode**: Adds imported transactions and wallets to existing data without losing anything.
  - **Replace Mode**: Cleanly restores the database to match the backup file.
- **Modern Storage Access Framework (SAF)**: Uses native Android file pickers with zero unnecessary storage permissions required.

### ⚙️ Personalization & Settings
- **Dark Mode**: Complete system-wide Dark Theme support with high-contrast surfaces and persistent state.
- **Multi-Currency Support**: Switch between USD (`$`), EUR (`€`), GBP (`£`), TND (`DT`), CAD (`$`), JPY (`¥`), and more.
- **Profile Customization**: Set user profile name and manage notification preferences.
- **Custom Categories & Recycle Bin Access**: Quick access directly in Settings.
- **Data Management**: Clear all transactions and wallets with one-click data reset.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android (Min SDK: 26, Target SDK: 36)
- **Language**: Java 17
- **Design & UI**: Material Design 3, ConstraintLayout, CardView, Custom Vector Drawables
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: SQLite via Android Room Persistence Library
- **Reactive State**: Android Architecture Components (`LiveData`, `ViewModel`)
- **Background Processing**: Android Jetpack WorkManager (`PeriodicWorkRequest`, `OneTimeWorkRequest`)
- **Charting**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- **Persistence**: SharedPreferences for currency, user preferences, and theme state

---

## 📁 Project Structure

```text
com.hmaidi.budgetflow/
├── data/
│   ├── AppDatabase.java            # Room Database singleton
│   ├── ExpenseDao.java              # DAO queries for expenses & recurring rules
│   ├── ExpenseModel.java            # Room Entity for transactions & recurring data
│   ├── ExpenseRepository.java       # Repository mediating Room & Executors
│   ├── Wallet.java                  # Room Entity for wallets
│   ├── WalletDao.java               # DAO queries for wallets
│   └── WalletRepository.java        # Repository for wallet management
├── ui/
│   ├── MainActivity.java            # Host activity, bottom nav, WorkManager scheduler
│   ├── HomeFragment.java            # Dashboard, balance, & monthly budget tracker
│   ├── StatsFragment.java           # MPAndroidChart category breakdown & net stats
│   ├── WalletFragment.java          # Wallets grid & total assets
│   ├── WalletDetailFragment.java    # Per-wallet ledger & statistics
│   ├── AddExpenseFragment.java      # Modal sheet for new expenses & recurring rules
│   ├── AddWalletFragment.java       # Modal sheet for new wallet creation
│   ├── AllTransactionsFragment.java  # Search, filter chips, & transaction list
│   ├── ExpenseDetailFragment.java   # Transaction inspection & deletion sheet
│   └── UserFragment.java            # Profile, theme toggle, currency, & budget settings
├── adapter/
│   ├── ExpenseAdapter.java          # RecyclerView adapter for transaction items
│   └── WalletAdapter.java           # RecyclerView adapter for wallet cards
├── worker/
│   └── RecurringWorker.java         # WorkManager background task for recurring payments
└── utils/
    ├── CurrencyHelper.java          # Multi-currency formatting & SharedPreferences
    └── CurrencyData.java            # Currency codes and symbol definitions
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Android Studio Ladybug (or newer recommended)
- **JDK**: Java 17
- **Android Device / Emulator**: Running Android 8.0 (API level 26) or higher

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/Ayoub81A/budgetflow.git
   cd budgetflow
   ```

2. Open the project in Android Studio.

3. Let Gradle sync project dependencies.

4. Run the project:
   - Select an emulator or physical device.
   - Click **Run ▶** (or press `Shift + F10`).

### Command Line Build
- Build debug APK:
  ```powershell
  .\gradlew assembleDebug
  ```
- Run unit tests:
  ```powershell
  .\gradlew test
  ```

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
