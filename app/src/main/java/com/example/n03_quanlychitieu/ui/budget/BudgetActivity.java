package com.example.n03_quanlychitieu.ui.budget;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.adapter.BudgetOverviewAdapter;
import com.example.n03_quanlychitieu.dao.BudgetDAO;
import com.example.n03_quanlychitieu.dao.CategoryDAO;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Budgets;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.model.Users;
import com.example.n03_quanlychitieu.ui.main.CalendarActivity;
import com.example.n03_quanlychitieu.ui.main.MainActivity;
import com.example.n03_quanlychitieu.ui.sign.ReportTransaction;
import com.example.n03_quanlychitieu.ui.sign.SetBudgets;
import com.example.n03_quanlychitieu.ui.user.UserProfileActivity;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
public class BudgetActivity extends AppCompatActivity {
    private RecyclerView rvBudget;
    private BottomNavigationView bottomNav;
    private ImageView ivPrevMonth, ivNextMonth;
    private TextView tvMonth;
    private DatabaseHelper dbHelper;
    private BudgetDAO budgetDAO;
    private CategoryDAO categoryDAO;
    private AuthenticationManager auth;
    private String userId;
    private Calendar currentCal;
    private BudgetOverviewAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);
        auth = AuthenticationManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);
        budgetDAO = new BudgetDAO(dbHelper.getReadableDatabase());
        categoryDAO = new CategoryDAO(dbHelper.getReadableDatabase());
        Users currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUser_id();
        }
        currentCal = Calendar.getInstance();
        initViews();
        setupListeners();
        updateDateRange();
    }
    private void initViews() {
        rvBudget = findViewById(R.id.rv_budget);
        bottomNav = findViewById(R.id.bottom_nav);
        ivPrevMonth = findViewById(R.id.iv_prev_month);
        ivNextMonth = findViewById(R.id.iv_next_month);
        tvMonth = findViewById(R.id.tv_month);
        rvBudget.setLayoutManager(new LinearLayoutManager(this));

        android.widget.ImageButton btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(BudgetActivity.this, com.example.n03_quanlychitieu.ui.sign.notification_user.class);
                startActivity(intent);
            });
        }

        bottomNav.setSelectedItemId(R.id.nav_budget);
    }
    private void updateDateRange() {
        Calendar cal = (Calendar) currentCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String title = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(cal.getTime());
        String mm = new SimpleDateFormat("MM", Locale.getDefault()).format(cal.getTime());
        tvMonth.setText(title + " (01/" + mm + " - " + maxDay + "/" + mm + ")");
        loadBudgets();
    }

    private double getTotalSpentForCategory(String categoryId, String startDate, String endDate) {
        String query = "SELECT SUM(amount) FROM expenses WHERE user_id = ? " +
                "AND category_id = ? " +
                "AND substr(create_at, 1, 10) BETWEEN ? AND ?";
        android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(query, new String[]{userId, categoryId, startDate, endDate});
        double total = 0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        return total;
    }

    private double getTotalSpentAll(String startDate, String endDate) {
        String query = "SELECT SUM(amount) FROM expenses WHERE user_id = ? " +
                "AND substr(create_at, 1, 10) BETWEEN ? AND ?";
        android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(query, new String[]{userId, startDate, endDate});
        double total = 0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        return total;
    }
    private void setupListeners() {
        ivPrevMonth.setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, -1);
            updateDateRange();
        });
        ivNextMonth.setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, 1);
            updateDateRange();
        });
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_input) {
                startActivity(new Intent(BudgetActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_calendar) {
                startActivity(new Intent(BudgetActivity.this, CalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_report) {
                startActivity(new Intent(BudgetActivity.this, ReportTransaction.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_more) {
                Intent intent = new Intent(BudgetActivity.this, UserProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == R.id.nav_budget) {
                 return true;
            }
            return false;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadBudgets();
    }
    private void loadBudgets() {
        if (userId != null) {
            List<Budgets> allBudgets = budgetDAO.getBudgetsByUser(userId);
            List<Categories> allCategories = categoryDAO.getAllCategories(userId);
            List<Categories> expenseCategories = new ArrayList<>();
            for (Categories cat : allCategories) {
                if ("expense".equals(cat.getType())) {
                    expenseCategories.add(cat);
                }
            }

            Calendar cal = (Calendar) currentCal.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String strStartDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String strEndDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            List<BudgetOverviewAdapter.BudgetItem> items = new ArrayList<>();
            // add total
            Budgets totalBudget = null;
            for (Budgets b : allBudgets) {
                if (b.getCategory_id() == null || b.getCategory_id().isEmpty() || "none".equals(b.getCategory_id())) {
                    // THÊM LỌC: Kiểm tra xem ngân sách này có hiệu lực trong tháng đang xem không
                    if (b.getStart_date() != null && b.getEnd_date() != null &&
                            b.getStart_date().compareTo(strEndDate) <= 0 &&
                            b.getEnd_date().compareTo(strStartDate) >= 0) {
                        totalBudget = b;
                        break;
                    }
                }
            }

            if (totalBudget == null) {
                double computedTotal = 0;
                for (Budgets b : allBudgets) {
                    if (b.getCategory_id() != null && !b.getCategory_id().isEmpty() && !"none".equals(b.getCategory_id())) {
                        // THÊM LỌC tương tự cho việc tính tổng
                        if (b.getStart_date() != null && b.getEnd_date() != null &&
                                b.getStart_date().compareTo(strEndDate) <= 0 &&
                                b.getEnd_date().compareTo(strStartDate) >= 0) {
                            computedTotal += b.getAmount();
                        }
                    }
                }
                if (computedTotal > 0) {
                    totalBudget = new Budgets(null, computedTotal, strStartDate, strEndDate, "Tổng ngân sách", userId, "none");
                }
            }
            double totalSpentAll = getTotalSpentAll(strStartDate, strEndDate);
            items.add(new BudgetOverviewAdapter.BudgetItem(null, totalBudget, true, totalSpentAll));

            for (Categories cat : expenseCategories) {
                Budgets found = null;
                for (Budgets b : allBudgets) {
                    if (cat.getCategory_id().equals(b.getCategory_id())) {
                        // THÊM LỌC: Chỉ lấy ngân sách của danh mục này nếu nó nằm trong khoảng thời gian của tháng
                        if (b.getStart_date() != null && b.getEnd_date() != null &&
                                b.getStart_date().compareTo(strEndDate) <= 0 &&
                                b.getEnd_date().compareTo(strStartDate) >= 0) {
                            found = b;
                            break;
                        }
                    }
                }
                double catSpent = getTotalSpentForCategory(cat.getCategory_id(), strStartDate, strEndDate);
                items.add(new BudgetOverviewAdapter.BudgetItem(cat, found, false, catSpent));
            }
            adapter = new BudgetOverviewAdapter(this, items, budgetDAO, currentCal.getTimeInMillis());
            rvBudget.setAdapter(adapter);
        }
    }
}
