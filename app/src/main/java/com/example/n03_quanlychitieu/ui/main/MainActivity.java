package com.example.n03_quanlychitieu.ui.main;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.adapter.CategoryGridAdapter;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.model.Users;
import com.example.n03_quanlychitieu.ui.category.AddCategoryActivity;
import com.example.n03_quanlychitieu.ui.sign.LogIn;
import com.example.n03_quanlychitieu.ui.user.UserProfileActivity;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AlertDialog;
import com.example.n03_quanlychitieu.dao.BudgetDAO;
import com.example.n03_quanlychitieu.dao.NotificationDAO;
import com.example.n03_quanlychitieu.model.Budgets;
import com.example.n03_quanlychitieu.model.Notifications;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthenticationManager auth;
    private String userId;
    private RadioGroup rgType;
    private RadioButton rbExpense, rbIncome;
    private TextView tvDate, tvAmountLabel;
    private ImageView tvPrevDate, tvNextDate;
    private EditText etNote, etAmount;
    private RecyclerView rvCategories;
    private Button btnSubmit;
    private BottomNavigationView bottomNav;
    private CategoryGridAdapter categoryAdapter;
    private List<Categories> currentCategories;
    private Categories selectedCategory = null;
    private String currentType = "expense"; // "expense" or "income"
    private Calendar selectedCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = AuthenticationManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);
        if (!auth.isUserLoggedIn()) {
            startActivity(new Intent(this, LogIn.class));
            finish();
            return;
        }
        Users currentUser = auth.getCurrentUser();
        userId = currentUser != null ? currentUser.getUser_id() : null;
        initViews();
        setupListeners();
        loadCategories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth != null && auth.isUserLoggedIn()) {
            loadCategories();
        }
    }

    private void initViews() {
        rgType = findViewById(R.id.rg_type);
        rbExpense = findViewById(R.id.rb_expense);
        rbIncome = findViewById(R.id.rb_income);
        tvDate = findViewById(R.id.tv_date);
        tvAmountLabel = findViewById(R.id.tv_amount_label);
        etNote = findViewById(R.id.et_note);
        etAmount = findViewById(R.id.et_amount);
        rvCategories = findViewById(R.id.rv_categories);
        btnSubmit = findViewById(R.id.btn_submit);
        bottomNav = findViewById(R.id.bottom_nav);
        tvPrevDate = findViewById(R.id.tv_prev_date);
        tvNextDate = findViewById(R.id.tv_next_date);

        ImageView btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, com.example.n03_quanlychitieu.ui.sign.notification_user.class);
                startActivity(intent);
            });
        }

        // Set Current Date
        updateDateDisplay();

        // Setup Recycler
        rvCategories.setLayoutManager(new GridLayoutManager(this, 3));
    }
    private void setupListeners() {
        tvPrevDate.setOnClickListener(v -> {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
        });

        tvNextDate.setOnClickListener(v -> {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
        });

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(MainActivity.this, (view, year, month, dayOfMonth) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateDisplay();
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_calendar) {
                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_report) {
                Intent intent = new Intent(MainActivity.this, com.example.n03_quanlychitieu.ui.sign.ReportTransaction.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
              } else if (item.getItemId() == R.id.nav_budget) {
                  Intent intent = new Intent(MainActivity.this, com.example.n03_quanlychitieu.ui.budget.BudgetActivity.class);
                  startActivity(intent);
                  overridePendingTransition(0, 0);
                  finish();
                  return true;
              } else if (item.getItemId() == R.id.nav_more) {
                Intent intent = new Intent(MainActivity.this, MoreActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return true;
        });

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_expense) {
                currentType = "expense";
                tvAmountLabel.setText("Tiền chi");
                btnSubmit.setText("Nhập khoản chi");
            } else {
                currentType = "income";
                tvAmountLabel.setText("Tiền thu");
                btnSubmit.setText("Nhập khoản thu");
            }
            selectedCategory = null;
            loadCategories();
        });
        btnSubmit.setOnClickListener(v -> saveTransaction());
    }

    private void updateDateDisplay() {
        String currentDateStr = new SimpleDateFormat("dd/MM/yyyy (E)", new Locale("vi", "VN")).format(selectedCalendar.getTime());
        tvDate.setText(currentDateStr);
    }

    private void loadCategories() {
        new Thread(() -> {
            currentCategories = dbHelper.getCategoriesByUserIdAndType(userId, currentType);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryGridAdapter(this, currentCategories, new CategoryGridAdapter.OnItemClickListener() {
                        @Override
                        public void onCategoryClick(Categories category) {
                            selectedCategory = category;
                        }
                        @Override
                        public void onEditClick() {
                            Intent intent = new Intent(MainActivity.this, AddCategoryActivity.class);
                            intent.putExtra("type", currentType);
                            intent.putExtra("userId", userId);
                            startActivity(intent);
                        }
                    });
                    rvCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.setList(currentCategories);
                }
            });
        }).start();
    }
    private void saveTransaction() {
        String amount = etAmount.getText().toString();
        String note = etNote.getText().toString();
        if (amount.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDateIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(selectedCalendar.getTime());
        String dbDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.getTime());

        if (currentType.equals("expense")) {
            String autoBudgetId = null;
            try {
                BudgetDAO bDao = new BudgetDAO(dbHelper.getReadableDatabase());
                List<Budgets> userBudgets = bDao.getBudgetsByUser(userId);
                Budgets matchedGlobal = null;
                for (Budgets b : userBudgets) {
                    if (b.getStart_date() != null && b.getEnd_date() != null) {
                        String bStart = b.getStart_date().length() > 10 ? b.getStart_date().substring(0, 10) : b.getStart_date();
                        String bEnd = b.getEnd_date().length() > 10 ? b.getEnd_date().substring(0, 10) : b.getEnd_date();
                        if (bStart.compareTo(dbDateStr) <= 0 && bEnd.compareTo(dbDateStr) >= 0) {
                            if (selectedCategory.getCategory_id().equals(b.getCategory_id())) {
                                autoBudgetId = b.getBudget_id();
                                break;
                            } else if ("none".equals(b.getCategory_id())) {
                                matchedGlobal = b;
                            }
                        }
                    }
                }
                if (autoBudgetId == null && matchedGlobal != null) {
                    autoBudgetId = matchedGlobal.getBudget_id();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalBudgetId = autoBudgetId;

            dbHelper.addExpenseAsync(userId, amount, selectedCategory.getCategory_id(), note, currentDateIso, finalBudgetId, new DatabaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        String warningMsg = checkBudgetWarningAfterExpense(finalBudgetId);
                        if (warningMsg != null && !warningMsg.isEmpty()) {
                            new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Cảnh báo Ngân sách")
                                .setMessage(warningMsg)
                                .setPositiveButton("Đã hiểu", (dialog, which) -> {
                                    Toast.makeText(MainActivity.this, "Đã thêm chi tiêu!", Toast.LENGTH_SHORT).show();
                                    resetForm();
                                })
                                .setCancelable(false)
                                .show();
                        } else {
                            Toast.makeText(MainActivity.this, "Đã thêm chi tiêu!", Toast.LENGTH_SHORT).show();
                            resetForm();
                        }
                    });
                }
                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            dbHelper.addIncomeAsync(userId, amount, selectedCategory.getCategory_id(), note, currentDateIso, new DatabaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Đã thêm thu nhập!", Toast.LENGTH_SHORT).show();
                    resetForm();
                }
                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String checkBudgetWarningAfterExpense(String budgetId) {
        if (budgetId == null || "none".equals(budgetId)) {
            return null;
        }

        BudgetDAO budgetDAO = new BudgetDAO(dbHelper.getReadableDatabase());
        NotificationDAO notificationDAO = new NotificationDAO(dbHelper.getWritableDatabase());
        Budgets budget = budgetDAO.getBudgetById(budgetId);

        if (budget == null) {
            return null;
        }

        double spent = budgetDAO.getTotalSpentForBudget(budgetId);
        double budgetAmount = budget.getAmount();
        String budgetDesc = budget.getDescription() != null && !budget.getDescription().isEmpty() ? budget.getDescription() : "chưa rõ";

        int warningThreshold = budget.getWarning_threshold();
        double warningRatio = warningThreshold / 100.0;

        if (spent > budgetAmount) {
            String msg = "Bạn đã vượt ngân sách " + budgetDesc + " (Đã chi " + spent + " / " + budgetAmount + ").";
            Notifications notification = new Notifications(UUID.randomUUID().toString(), msg, false, null, "warn", userId);
            notificationDAO.insert(notification);
            return msg;
        } else if (spent >= budgetAmount * warningRatio) {
            String msg = "Bạn sắp vượt ngân sách " + budgetDesc + " (Đã chi " + spent + " / " + budgetAmount + ")";
            Notifications notification = new Notifications(UUID.randomUUID().toString(), msg, false, null, "warn", userId);
            notificationDAO.insert(notification);
            return msg;
        }

        return null;
    }

    private void resetForm() {
        etAmount.setText("");
        etNote.setText("");
        selectedCategory = null;
        selectedCalendar = Calendar.getInstance();
        updateDateDisplay();
        if(categoryAdapter != null) categoryAdapter.notifyDataSetChanged();
    }
}
