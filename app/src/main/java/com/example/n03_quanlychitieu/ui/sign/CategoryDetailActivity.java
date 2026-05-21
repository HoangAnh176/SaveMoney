package com.example.n03_quanlychitieu.ui.sign;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.adapter.CategoryDetailAdapter;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.ui.main.FixedTransactionActivity;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryDetailActivity extends AppCompatActivity {
    private TextView tvTitle;
    private BarChart barChart;
    private TextView tvTotalSummary, tvAverageSummary;
    private RecyclerView rvDetails;
    private ImageView ivBack;

    private String categoryId;
    private String categoryName;
    private String colorStr;
    private boolean isExpense;
    private boolean isYearlyMode;
    private long currentStartDate;
    private long currentEndDate;

    private AuthenticationManager authManager;
    private DatabaseHelper dbHelper;

    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        categoryId = getIntent().getStringExtra("categoryId");
        categoryName = getIntent().getStringExtra("categoryName");
        colorStr = getIntent().getStringExtra("color");
        isExpense = getIntent().getBooleanExtra("isExpense", true);
        isYearlyMode = getIntent().getBooleanExtra("isYearlyMode", false);
        currentStartDate = getIntent().getLongExtra("currentStartDate", 0);
        currentEndDate = getIntent().getLongExtra("currentEndDate", 0);

        authManager = AuthenticationManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);

        initViews();
        setupChart();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInitialLoad) {
            setupChart();
            loadData();
        }
        isInitialLoad = false;
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        barChart = findViewById(R.id.barChart);
        tvTotalSummary = findViewById(R.id.tv_total_summary);
        tvAverageSummary = findViewById(R.id.tv_average_summary);
        rvDetails = findViewById(R.id.rv_details);
        ivBack = findViewById(R.id.iv_back);

        ivBack.setOnClickListener(v -> finish());
        rvDetails.setLayoutManager(new LinearLayoutManager(this));

        // Set Title
        String periodText = "";
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentStartDate);
        if (isYearlyMode) {
            String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.getTime());
            periodText = "(Năm " + year + ")";
        } else {
            String month = new SimpleDateFormat("MM", Locale.getDefault()).format(cal.getTime());
            periodText = "(T" + month + ")";
        }
        tvTitle.setText(categoryName + " " + periodText);
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return NumberFormat.getNumberInstance(Locale.US).format(Math.round(value));
            }
        });
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.setNoDataText("Không có dữ liệu");
        barChart.setExtraOffsets(0, 0, 0, 10);
    }

    private void loadData() {
        new Thread(() -> {
            String userId = authManager.getCurrentUser() != null ? authManager.getCurrentUser().getUser_id() : "";
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String table = isExpense ? "expenses" : "incomes";
            String idVal = isExpense ? "expense_id" : "income_id";

            String query = "SELECT e.amount, e.create_at, c.icon, e." + idVal + ", e.description, e.category_id, e.fixed_id FROM " + table + " e " +
                    "JOIN categories c ON e.category_id = c.category_id " +
                    "WHERE e.user_id = ? AND e.category_id = ?";
            Cursor c = db.rawQuery(query, new String[]{userId, categoryId});

            List<CategoryDetailAdapter.DetailItem> items = new ArrayList<>();
            Map<Integer, Double> chartYearlyMap = new HashMap<>(); // For 12 months in the year
            Map<String, Double> chartMonthlyMap = new HashMap<>(); // Cho monthly (trend 4-6 tháng)
            double listTotalAmount = 0;
            double chartTotalAmount = 0;

            Date sDate = new Date(currentStartDate);
            Date eDate = new Date(currentEndDate);

            Calendar sCal = Calendar.getInstance();
            sCal.setTime(sDate);
            int currentYear = sCal.get(Calendar.YEAR);
            int currentMonth = sCal.get(Calendar.MONTH) + 1;

            android.util.Log.d("CategoryDetail", "User: " + userId + ", categoryId: " + categoryId + ", table: " + table);
            android.util.Log.d("CategoryDetail", "sDate: " + sDate + ", eDate: " + eDate);

            while (c.moveToNext()) {
                double amount = c.getDouble(0);
                String dateStr = c.getString(1);
                String icon = c.getString(2);
                String transId = c.getString(3);
                String desc = c.getString(4);
                String catId = c.getString(5);
                String fixedId = c.getString(6);

                Date transDate = parseDate(dateStr);
                if (transDate == null) continue;

                Calendar tCal = Calendar.getInstance();
                tCal.setTime(transDate);
                int tYear = tCal.get(Calendar.YEAR);
                int tMonth = tCal.get(Calendar.MONTH) + 1;

                // Group data for the chart
                if (isYearlyMode) {
                    if (tYear == currentYear) {
                        chartYearlyMap.put(tMonth, chartYearlyMap.getOrDefault(tMonth, 0.0) + amount);
                        chartTotalAmount += amount;
                    }
                } else {
                    // Monthly mode chart: show trend for the current year up to the selected month, 
                    // or last 4-6 months. Let's just group by month for the current year.
                    if (tYear == currentYear && tMonth <= currentMonth) {
                        chartYearlyMap.put(tMonth, chartYearlyMap.getOrDefault(tMonth, 0.0) + amount);
                    }
                }

                // Filter for List logic
                if (!transDate.before(sDate) && !transDate.after(eDate)) {
                    listTotalAmount += amount;
                    
                    if (!isYearlyMode) {
                       String listDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(transDate);
                       items.add(new CategoryDetailAdapter.DetailItem(listDateStr, amount, categoryName, icon, transId, desc, catId, dateStr, fixedId));
                    }
                }
            }
            c.close();

            // Prepare list and chart
            List<CategoryDetailAdapter.DetailItem> finalItems = new ArrayList<>();
            List<BarEntry> entries = new ArrayList<>();
            List<String> xLabels = new ArrayList<>();
            
            // Build Chart Data
            if (isYearlyMode) {
                for (int m = 1; m <= 12; m++) {
                    double amt = chartYearlyMap.getOrDefault(m, 0.0);
                    finalItems.add(new CategoryDetailAdapter.DetailItem(String.valueOf(m), amt));
                    entries.add(new BarEntry(m - 1, (float)amt));
                    xLabels.add("T" + m);
                }
            } else {
                // Monthly mode: building chart data
                // In Monthly Mode, we show from Jan to current Month (e.g. T1, T2, T3, T4)
                // If it's Jan, maybe just show T1, or show previous months. The image showed 4 columns.
                // Let's just show from month 1 to currentMonth if currentMonth <= 4, otherwise showing last 4-6 months.
                int startM = Math.max(1, currentMonth - 5); // show at most 6 months
                int mIndex = 0;
                for (int m = startM; m <= currentMonth; m++) {
                    double amt = chartYearlyMap.getOrDefault(m, 0.0);
                    entries.add(new BarEntry(mIndex, (float)amt));
                    if (m == startM) {
                        xLabels.add(String.format(Locale.getDefault(), "%02d/%d", m, currentYear));
                    } else {
                        xLabels.add("T" + m);
                    }
                    mIndex++;
                }

                // Sắp xếp transactions
                items.sort((i1, i2) -> i2.dateStr.compareTo(i1.dateStr));
                
                // Gom nhóm theo ngày cho List Items
                Map<String, Double> dayMap = new HashMap<>();
                for (CategoryDetailAdapter.DetailItem it : items) {
                    dayMap.put(it.dateStr, dayMap.getOrDefault(it.dateStr, 0.0) + it.amount);
                    // The image shows the list contains daily sum? Or individual items?
                    // Actually the second image's list has items with Category icon, name, and total. 
                    // Let's assume the items is what we need to show. The first image shows individual grouped transactions or daily totals.
                    // Image 1: 12/04 (50,000) header, under it `Ăn uống -50,000`. So it might be grouped by day!
                    // Currently our CategoryDetailAdapter in Monthly mode just shows a list of transactions!
                }
                
                finalItems.addAll(items);
            }

            int listCount = isYearlyMode ? 12 : finalItems.size();
            final double fTotal = listTotalAmount;
            final double fAvg = listCount > 0 ? (listTotalAmount / listCount) : 0;
            
            new Handler(Looper.getMainLooper()).post(() -> {
                NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                tvTotalSummary.setText(fmt.format(fTotal));
                tvAverageSummary.setText(fmt.format(fAvg));

                if (!entries.isEmpty()) {
                    BarDataSet dataSet = new BarDataSet(entries, "");
                    try {
                        dataSet.setColor(Color.parseColor(colorStr));
                    } catch (Exception e) {
                        dataSet.setColor(Color.parseColor("#FF9800"));
                    }
                    dataSet.setDrawValues(true);
                    dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                        @Override
                        public String getBarLabel(BarEntry barEntry) {
                            if (barEntry.getY() == 0) return "0đ";
                            return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(barEntry.getY()) + "đ";
                        }
                    });
                    try { dataSet.setValueTextColor(Color.parseColor(colorStr)); } catch (Exception e){}
                    dataSet.setValueTextSize(10f);

                    BarData data = new BarData(dataSet);
                    // data.setBarWidth(0.5f);
                    barChart.setData(data);
                    barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
                    barChart.getXAxis().setLabelCount(xLabels.size());
                    barChart.invalidate();
                }

                CategoryDetailAdapter adapter = new CategoryDetailAdapter(this, finalItems, isYearlyMode, colorStr);
                adapter.setOnItemClickListener(new CategoryDetailAdapter.OnItemClickListener() {
                    @Override
                    public void onMonthItemClick(int month) {
                        if (!isYearlyMode) return;
                        Calendar clickCal = Calendar.getInstance();
                        clickCal.setTimeInMillis(currentStartDate);
                        
                        clickCal.set(Calendar.MONTH, month - 1);
                        clickCal.set(Calendar.DAY_OF_MONTH, 1);
                        clickCal.set(Calendar.HOUR_OF_DAY, 0);
                        clickCal.set(Calendar.MINUTE, 0);
                        clickCal.set(Calendar.SECOND, 0);
                        long newStart = clickCal.getTimeInMillis();

                        clickCal.set(Calendar.DAY_OF_MONTH, clickCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                        clickCal.set(Calendar.HOUR_OF_DAY, 23);
                        clickCal.set(Calendar.MINUTE, 59);
                        clickCal.set(Calendar.SECOND, 59);
                        long newEnd = clickCal.getTimeInMillis();

                        Intent intent = new Intent(CategoryDetailActivity.this, CategoryDetailActivity.class);
                        intent.putExtra("categoryId", categoryId);
                        intent.putExtra("categoryName", categoryName);
                        intent.putExtra("color", colorStr);
                        intent.putExtra("isExpense", isExpense);
                        intent.putExtra("isYearlyMode", false);
                        intent.putExtra("currentStartDate", newStart);
                        intent.putExtra("currentEndDate", newEnd);
                        startActivity(intent);
                    }

                    @Override
                    public void onTransactionClick(CategoryDetailAdapter.DetailItem item) {
                        if (!isYearlyMode && item.id != null) {

                            if (item.fixedId != null) {
                                Intent intent = new Intent(CategoryDetailActivity.this, FixedTransactionActivity.class);
                                String uid = AuthenticationManager.getInstance(CategoryDetailActivity.this).getCurrentUser().getUser_id();
                                intent.putExtra("userId", uid);
                                intent.putExtra("transactionId", item.fixedId);
                                startActivity(intent);
                                return;
                            }

                            Intent intent;
                            if (isExpense) {
                                intent = new Intent(CategoryDetailActivity.this, com.example.n03_quanlychitieu.ui.expense.UpdateExpenseActivity.class);
                                intent.putExtra("expenseID", item.id);
                            } else {
                                intent = new Intent(CategoryDetailActivity.this, com.example.n03_quanlychitieu.ui.income.UpdateIncomeActivity.class);
                                intent.putExtra("incomeID", item.id);
                            }
                            intent.putExtra("amount", String.valueOf((int)item.amount));
                            intent.putExtra("date", item.rawDate != null ? item.rawDate : item.dateStr);
                            intent.putExtra("categoryId", item.catId);
                            intent.putExtra("description", item.desc);
                            intent.putExtra("position", -1);
                            startActivityForResult(intent, 100);
                        }
                    }
                });
                rvDetails.setAdapter(adapter);
            });

        }).start();
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            if (dateStr.contains("T")) {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr);
            } else {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Already handled by onResume
        }
    }
}
