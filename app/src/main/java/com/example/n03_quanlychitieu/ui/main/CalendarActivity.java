package com.example.n03_quanlychitieu.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.adapter.CalendarGridAdapter;
import com.example.n03_quanlychitieu.adapter.TransactionAdapter;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Transaction;
import com.example.n03_quanlychitieu.ui.expense.UpdateExpenseActivity;
import com.example.n03_quanlychitieu.ui.income.UpdateIncomeActivity;
import com.example.n03_quanlychitieu.ui.user.UserProfileActivity;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;
import com.example.n03_quanlychitieu.utils.ExpandableHeightGridView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class CalendarActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private TextView tvTitle, tvMonthYear, tvTotalIncome, tvTotalExpense, tvTotalAll;
    private ImageView ivPrevMonth, ivNextMonth;
    private ExpandableHeightGridView gvCalendar;
    private RecyclerView rvTransactions;
    private Calendar currentCalendar;
    private DatabaseHelper dbHelper;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        dbHelper = new DatabaseHelper(this);
        userId = AuthenticationManager.getInstance(this).getCurrentUser().getUser_id();
        currentCalendar = Calendar.getInstance();

        initViews();
        setupListeners();
        loadMonthData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null && userId != null) {
            loadMonthData();
        }
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_nav);
        tvTitle = findViewById(R.id.tv_title);

        tvMonthYear = findViewById(R.id.tv_month_year);
        ivPrevMonth = findViewById(R.id.iv_prev_month);
        ivNextMonth = findViewById(R.id.iv_next_month);
        gvCalendar = findViewById(R.id.gv_calendar);
        gvCalendar.setExpanded(true);
        rvTransactions = findViewById(R.id.rv_transactions);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvTotalAll = findViewById(R.id.tv_total_all);

        android.widget.ImageButton btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(CalendarActivity.this, com.example.n03_quanlychitieu.ui.sign.notification_user.class);
                startActivity(intent);
            });
        }

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        bottomNav.setSelectedItemId(R.id.nav_calendar);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_input) {
                startActivity(new Intent(CalendarActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(CalendarActivity.this, com.example.n03_quanlychitieu.ui.sign.ReportTransaction.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_budget) {
                  startActivity(new Intent(CalendarActivity.this, com.example.n03_quanlychitieu.ui.budget.BudgetActivity.class));
                  overridePendingTransition(0, 0);
                  finish();
                  return true;
            } else if (itemId == R.id.nav_more) {
                Intent intent = new Intent(CalendarActivity.this, MoreActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return true;
        });
    }

    private void setupListeners() {
        ivPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            loadMonthData();
        });
        ivNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            loadMonthData();
        });
    }

    private void loadMonthData() {
        SimpleDateFormat sdfInfo = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        tvMonthYear.setText(sdfInfo.format(currentCalendar.getTime()));

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Setup dates start and end strings
        String startOfMonth = String.format(Locale.getDefault(), "%04d-%02d-%02dT00:00:00",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, 1);
        String endOfMonth = String.format(Locale.getDefault(), "%04d-%02d-%02dT23:59:59",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, daysInMonth);

        List<Transaction> monthTransactions = dbHelper.getTransactionsByDateRange(userId, startOfMonth, endOfMonth);

        // Prepopulate empty cells for start of month offset (Monday = first)
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sun, 2 = Mon
        int offset = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2;

        List<String> days = new ArrayList<>();
        List<Double> sums = new ArrayList<>();
        List<Boolean> isCurrentMonth = new ArrayList<>();
        
        Calendar prevMonthCal = (Calendar) currentCalendar.clone();
        prevMonthCal.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < offset; i++) {
            days.add(0, String.valueOf(daysInPrevMonth - i));
            sums.add(0.0);
            isCurrentMonth.add(0, false);
        }

        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : monthTransactions) {
            if (t.type.equals("income")) {
                totalIncome += t.amount;
            } else {
                totalExpense += t.amount;
            }
        }

        for (int i = 1; i <= daysInMonth; i++) {
            days.add(String.valueOf(i));
            double daySum = 0;

            // Calculate day sum
            for (Transaction t : monthTransactions) {
                // assume date format is yyyy-MM-dd
                if (t.date.length() >= 10) {
                    try {
                        int tDay = Integer.parseInt(t.date.substring(8, 10));
                        if (tDay == i) {
                            if (t.type.equals("income")) {
                                daySum += t.amount;
                            } else {
                                daySum -= t.amount;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            sums.add(daySum);
            isCurrentMonth.add(true);
        }

        int remainingCells = 42 - days.size();
        if (remainingCells >= 7 && days.size() <= 35) {
            remainingCells -= 7; // If only 5 rows needed, don't show the 6th row padding
        }
        for (int i = 1; i <= remainingCells; i++) {
            days.add(String.valueOf(i));
            sums.add(0.0);
            isCurrentMonth.add(false);
        }

        gvCalendar.setAdapter(new CalendarGridAdapter(this, days, sums, isCurrentMonth));
        Map<String, List<Transaction>> groupedByDate = new java.util.HashMap<>();
        for (Transaction t : monthTransactions) {
            String dayStr = t.date != null && t.date.length() >= 10 ? t.date.substring(0, 10) : "";
            if (!groupedByDate.containsKey(dayStr)) {
                groupedByDate.put(dayStr, new ArrayList<>());
            }
            groupedByDate.get(dayStr).add(t);
        }

        List<TransactionAdapter.ListItem> flattenedList = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(groupedByDate.keySet());
        sortedDates.sort((a, b) -> b.compareTo(a));

        for (String dateKey : sortedDates) {
            List<Transaction> dailyTrans = groupedByDate.get(dateKey);
            double dailyTotal = 0;
            for (Transaction t : dailyTrans) {
                if (t.type.equals("income")) dailyTotal += t.amount;
                else dailyTotal -= t.amount;
            }

            String headerTitle = dateKey;
            if (dateKey.length() == 10) {
                headerTitle = dateKey.substring(8, 10) + "/" + dateKey.substring(5, 7);
            }
            flattenedList.add(new TransactionAdapter.HeaderItem(headerTitle, dailyTotal));

            for (Transaction t : dailyTrans) {
                flattenedList.add(new TransactionAdapter.TransactionItem(t));
            }
        }

        rvTransactions.setAdapter(new TransactionAdapter(flattenedList, transaction -> {

            if (transaction.fixedId != null) {
                Intent intent = new Intent(CalendarActivity.this, FixedTransactionActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("transactionId", transaction.fixedId);
                startActivity(intent);
                return;
            }

            if (transaction.type.equals("expense")) {
                Intent intent = new Intent(CalendarActivity.this, UpdateExpenseActivity.class);
                intent.putExtra("expenseID", transaction.id);
                intent.putExtra("date", transaction.date);
                intent.putExtra("amount", String.valueOf(transaction.amount));
                intent.putExtra("categoryId", transaction.categoryId);
                intent.putExtra("description", transaction.description);
                if (transaction.budgetId != null) {
                    intent.putExtra("budget", transaction.budgetId);
                }
                startActivity(intent);
            } else if (transaction.type.equals("income")) {
                Intent intent = new Intent(CalendarActivity.this, UpdateIncomeActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("incomeID", transaction.id);
                intent.putExtra("categoryId", transaction.categoryId);
                intent.putExtra("amount", String.valueOf(transaction.amount));
                intent.putExtra("description", transaction.description);
                intent.putExtra("date", transaction.date);
                startActivity(intent);
            }
        }));

        tvTotalIncome.setText(String.format("+%,.0fđ", totalIncome));
        tvTotalExpense.setText(String.format("-%,.0fđ", totalExpense));
        tvTotalAll.setText(String.format("%,.0fđ", totalIncome - totalExpense));
    }
}
