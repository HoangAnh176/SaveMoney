package com.example.n03_quanlychitieu.ui.sign;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.adapter.ReportCategoryAdapter;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.ui.main.CalendarActivity;
import com.example.n03_quanlychitieu.ui.main.MainActivity;
import com.example.n03_quanlychitieu.ui.user.UserProfileActivity;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class ReportTransaction extends AppCompatActivity {
    private TextView tvTotalIncome, tvTotalExpense, tvBalance, tvDateRange;
    private PieChart pieChart;
    private RecyclerView rvCategories;
    private TabLayout tabLayout;
    private RadioGroup rgReportType;
    private BottomNavigationView bottomNav;
    private AuthenticationManager authManager;
    private DatabaseHelper dbHelper;
    private Calendar currentCal = Calendar.getInstance();
    private boolean isYearlyMode = false;
    private boolean isExpenseTab = true;
    private Date currentStartDate;
    private Date currentEndDate;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial_report);
        authManager = AuthenticationManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);
        initViews();
        setupBottomNav();
        setupPieChart();
        setupListeners();
        updateDateRange(); // Tính toán currentStartDate và currentEndDate
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInitialLoad) {
            refreshData();
        }
        isInitialLoad = false;
    }

    private void initViews() {
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvBalance = findViewById(R.id.tv_balance);
        tvDateRange = findViewById(R.id.tv_date_range);
        pieChart = findViewById(R.id.pieChart);
        rvCategories = findViewById(R.id.rv_report_categories);
        tabLayout = findViewById(R.id.tab_layout);
        rgReportType = findViewById(R.id.rg_report_type);
        bottomNav = findViewById(R.id.bottom_nav);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        android.widget.ImageButton btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(ReportTransaction.this, notification_user.class);
                startActivity(intent);
            });
        }
    }
    private void setupListeners() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isExpenseTab = tab.getPosition() == 0;
                refreshData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        rgReportType.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rbMonthly = findViewById(R.id.rb_monthly);
            RadioButton rbYearly = findViewById(R.id.rb_yearly);
            if (checkedId == R.id.rb_monthly) {
                rbMonthly.setBackgroundColor(Color.parseColor("#FF9800"));
                rbMonthly.setTextColor(Color.WHITE);
                rbYearly.setBackgroundColor(Color.parseColor("#F5F5F5"));
                rbYearly.setTextColor(Color.parseColor("#FF9800"));
            } else {
                rbYearly.setBackgroundColor(Color.parseColor("#FF9800"));
                rbYearly.setTextColor(Color.WHITE);
                rbMonthly.setBackgroundColor(Color.parseColor("#F5F5F5"));
                rbMonthly.setTextColor(Color.parseColor("#FF9800"));
            }
            isYearlyMode = (checkedId == R.id.rb_yearly);
            updateDateRange();
        });
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (isYearlyMode) currentCal.add(Calendar.YEAR, -1);
            else currentCal.add(Calendar.MONTH, -1);
            updateDateRange();
        });
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (isYearlyMode) currentCal.add(Calendar.YEAR, 1);
            else currentCal.add(Calendar.MONTH, 1);
            updateDateRange();
        });

    }
    private void updateDateRange() {
        Calendar cal = (Calendar) currentCal.clone();
        if (isYearlyMode) {
            String yearStr = new SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.getTime());
            tvDateRange.setText("Năm " + yearStr);
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            currentStartDate = cal.getTime();
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            currentEndDate = cal.getTime();
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            currentStartDate = cal.getTime();
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String title = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(cal.getTime());
            tvDateRange.setText(title + " (01/" + new SimpleDateFormat("MM", Locale.getDefault()).format(cal.getTime()) + " - " + maxDay + "/" + new SimpleDateFormat("MM", Locale.getDefault()).format(cal.getTime()) + ")");
            cal.set(Calendar.DAY_OF_MONTH, maxDay);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            currentEndDate = cal.getTime();
        }
        refreshData();
    }
    private void refreshData() {
        new Thread(() -> {
            String userId = authManager.getCurrentUser() != null ? authManager.getCurrentUser().getUser_id() : "";
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            // Tổng Chi
            Map<String, CategorySum> expenseMap = getAggregated(db, userId, "expenses");
            double totalExpense = 0;
            for (CategorySum cs : expenseMap.values()) totalExpense += cs.amount;
            // Tổng Thu
            Map<String, CategorySum> incomeMap = getAggregated(db, userId, "incomes");
            double totalIncome = 0;
            for (CategorySum cs : incomeMap.values()) totalIncome += cs.amount;
            double balance = totalIncome - totalExpense;
            // Lấy data cho List/Chart dựa trên Tab hiện tại
            Map<String, CategorySum> activeMap = isExpenseTab ? expenseMap : incomeMap;
            double activeTotal = isExpenseTab ? totalExpense : totalIncome;
            List<CategorySum> results = new ArrayList<>(activeMap.values());
            results.sort((a,b) -> Double.compare(b.amount, a.amount));
            final double fExp = totalExpense;
            final double fInc = totalIncome;
            final double fBal = balance;
            new Handler(Looper.getMainLooper()).post(() -> {
                NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                tvTotalExpense.setText("-" + fmt.format(fExp).replace("-",""));
                tvTotalIncome.setText("+" + fmt.format(fInc).replace("+",""));
                String balStr = fmt.format(Math.abs(fBal));
                tvBalance.setText((fBal < 0 ? "-" : (fBal > 0 ? "+" : "")) + balStr);
                updateChartAndList(results, activeTotal);
            });
        }).start();
    }
    private Map<String, CategorySum> getAggregated(SQLiteDatabase db, String userId, String table) {
        Map<String, CategorySum> map = new HashMap<>();
        String query = "SELECT c.category_id, c.name, e.amount, e.create_at, c.color, c.icon FROM " + table + " e " +
                       "JOIN categories c ON e.category_id = c.category_id " +
                       "WHERE e.user_id = ?";
        Cursor c = db.rawQuery(query, new String[]{userId});
        while (c.moveToNext()) {
            String catId = c.getString(0);
            String name = c.getString(1);
            double amount = c.getDouble(2);
            String dateStr = c.getString(3);
            String color = c.getString(4);
            String icon = c.getString(5);
            if (color == null || color.isEmpty()) color = (table.equals("expenses") ? "#E67E22" : "#3498DB");

            if (isDateInRange(dateStr)) {
                CategorySum cs = map.getOrDefault(name, new CategorySum(catId, name, 0, color, icon));
                cs.amount += amount;
                map.put(name, cs);
            }
        }
        c.close();
        return map;
    }

    private boolean isDateInRange(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            Date d;
            if (dateStr.contains("T")) {
                d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr);
            } else {
                d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            }
            if (d == null) return false;
            return !d.before(currentStartDate) && !d.after(currentEndDate);
        } catch (Exception e) {
            return false;
        }
    }
    private void updateChartAndList(List<CategorySum> results, double total) {
        List<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        List<ReportCategoryAdapter.CategoryReportItem> items = new ArrayList<>();
        for (CategorySum cs : results) {
            String shortName = cs.name;
            if (shortName != null && shortName.length() > 10) {
                shortName = shortName.substring(0, 9) + "...";
            }
            entries.add(new PieEntry((float)cs.amount, shortName, cs.name));
            try { colors.add(Color.parseColor(cs.color)); }
            catch(Exception e) { colors.add(Color.GRAY); }
            double percent = total > 0 ? (cs.amount / total * 100) : 0;
            items.add(new ReportCategoryAdapter.CategoryReportItem(cs.catId, cs.name, cs.amount, percent, cs.color, cs.icon));
        }
        if (entries.isEmpty()) {
            pieChart.setData(null);
            pieChart.invalidate();
        } else {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setSliceSpace(2f);
            dataSet.setDrawValues(false);
            dataSet.setSelectionShift(0f); // Tắt hiệu ứng zoom to khi bấm

            PieData data = new PieData(dataSet);
            pieChart.setData(data);

            // Cài đặt Tooltip
            CustomMarkerView mv = new CustomMarkerView(this, R.layout.custom_marker_view, total);
            mv.setChartView(pieChart);
            pieChart.setMarker(mv);

            pieChart.invalidate();
        }
        ReportCategoryAdapter adapter = new ReportCategoryAdapter(this, items);
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(ReportTransaction.this, CategoryDetailActivity.class);
            intent.putExtra("categoryId", item.categoryId);
            intent.putExtra("categoryName", item.name);
            intent.putExtra("color", item.color);
            intent.putExtra("isExpense", isExpenseTab);
            intent.putExtra("isYearlyMode", isYearlyMode);
            intent.putExtra("currentStartDate", currentStartDate.getTime());
            intent.putExtra("currentEndDate", currentEndDate.getTime());
            startActivity(intent);
        });
        rvCategories.setAdapter(adapter);
    }
    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_report);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_input) {
                startActivity(new Intent(ReportTransaction.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_calendar) {
                startActivity(new Intent(ReportTransaction.this, CalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
              } else if (item.getItemId() == R.id.nav_budget) {
                  Intent intent = new Intent(ReportTransaction.this, com.example.n03_quanlychitieu.ui.budget.BudgetActivity.class);
                  startActivity(intent);
                  overridePendingTransition(0, 0);
                  finish();
                  return true;
              } else if (item.getItemId() == R.id.nav_more) {
                String userId = authManager.getCurrentUser() != null ? authManager.getCurrentUser().getUser_id() : null;
                Intent intent = new Intent(ReportTransaction.this, UserProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                return true;
            }
            return true;
        });
    }
    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleRadius(50f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false); 
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(14f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTypeface(Typeface.DEFAULT);
        pieChart.setExtraOffsets(5f, 5f, 5f, 5f);
        pieChart.setNoDataText("Không có dữ liệu trong khoảng thời gian này");
    }
    class CategorySum {
        String catId;
        String name;
        double amount;
        String color;
        String icon;
        CategorySum(String id, String n, double a, String c, String i) { catId = id; name = n; amount = a; color = c; icon = i; }
    }
}
