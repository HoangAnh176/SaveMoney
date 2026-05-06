package com.example.n03_quanlychitieu.ui.expense;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.dao.BudgetDAO;
import com.example.n03_quanlychitieu.dao.NotificationDAO;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Budgets;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.model.Notifications;
import com.example.n03_quanlychitieu.model.Users;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddExpenseActivity extends AppCompatActivity {
    private static final String TAG = "AddExpenseActivity";
    private TextInputEditText etDate, etAmount, etDescription;
    private TextInputLayout tilDate, tilAmount, tilDescription;
    private Button btnSave;
    private ImageButton btnBack;
    private Spinner spinnerCategory, spinnerBudget;
    private DatabaseHelper databaseHelper;
    Users currentUser;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);
        Log.d(TAG, "onCreate called");

        databaseHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tilDate = findViewById(R.id.tilDate);
        tilAmount = findViewById(R.id.tilAmount);
        tilDescription = findViewById(R.id.tilDescription);
        etDate = findViewById(R.id.etDate);
        etAmount = findViewById(R.id.etAmount);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerBudget = findViewById(R.id.spinnerBudget);
        etDescription = findViewById(R.id.etDescription);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        currentUser = AuthenticationManager.getInstance(this).getCurrentUser();
        userId = currentUser.getUser_id();

        loadBudgets();
        loadCategories();

//        if (savedInstanceState != null) {
//            etDate.setText(savedInstanceState.getString("date"));
//            etAmount.setText(savedInstanceState.getString("amount"));
//            etCategory.setText(savedInstanceState.getString("category"));
//            etDescription.setText(savedInstanceState.getString("description"));
//            Log.d(TAG, "Restored state: date=" + etDate.getText().toString() + ", amount=" + etAmount.getText().toString() +
//                    ", category=" + etCategory.getText().toString() + ", description=" + etDescription.getText().toString());
//        }

        etDate.setOnClickListener(v -> showDatePicker());
        Log.d(TAG, "Date picker listener set");

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            setResult(RESULT_CANCELED);
            finish();
        });

        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");

            tilDate.setError(null);
            tilAmount.setError(null);
            tilDescription.setError(null);

            String date = etDate.getText().toString().trim();
            String amount = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            // Lấy đối tượng Categories từ Spinner
            Categories selectedCategory = (Categories) spinnerCategory.getSelectedItem();
            String categoryId = selectedCategory != null ? selectedCategory.getCategory_id() : null;

            // Lấy đối tượng Budgets từ Spinner
            Budgets selectedBudget = (Budgets) spinnerBudget.getSelectedItem();
            String budgetId = selectedBudget != null ? selectedBudget.getBudget_id() : null;
            String categoryName = selectedCategory != null ? selectedCategory.getName() : "Không xác định";

            String logBudgetId = selectedBudget != null ? selectedBudget.getBudget_id() : "null";
            Log.d(TAG, "Input values: date=" + date + ", amount=" + amount + ", category=" + (selectedCategory != null ? selectedCategory.getName() : "null") + ", description=" + description + ", budget = " + logBudgetId );

            if (date.isEmpty()) {
                Log.w(TAG, "Validation failed: Date is empty");
                tilDate.setError("Vui lòng chọn ngày");
                return;
            }
            if (amount.isEmpty()) {
                Log.w(TAG, "Validation failed: Amount is empty");
                tilAmount.setError("Vui lòng nhập số tiền");
                return;
            }
            if (categoryId == null || categoryId.equals("none")) {
                Toast.makeText(this, "Vui lòng chọn danh mục hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (description.isEmpty()) {
                Log.w(TAG, "Validation failed: Description is empty");
                tilDescription.setError("Vui lòng nhập mô tả");
                return;
            }

            // Xử lý budgetId
            String finalBudgetId = budgetId != null && budgetId.equals("none") ? null : budgetId;

            // Tự động tìm ngân sách cho danh mục nếu người dùng không chọn ngân sách nào
            try {
                if (finalBudgetId == null && categoryId != null && !date.isEmpty()) {
                    SimpleDateFormat inFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat outFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String dbDateStr = outFmt.format(inFmt.parse(date)); // Chỉ lấy phần yyyy-MM-dd

                    BudgetDAO bDao = new BudgetDAO(databaseHelper.getReadableDatabase());
                    List<Budgets> userBudgets = bDao.getBudgetsByUser(userId);
                    Budgets matchedGlobal = null;

                    for (Budgets b : userBudgets) {
                        if (b.getStart_date() != null && b.getEnd_date() != null) {
                            // Cắt 10 ký tự đầu (yyyy-MM-dd) để tránh lỗi nếu database có dính chuỗi giờ 'T'
                            String bStart = b.getStart_date().length() > 10 ? b.getStart_date().substring(0, 10) : b.getStart_date();
                            String bEnd = b.getEnd_date().length() > 10 ? b.getEnd_date().substring(0, 10) : b.getEnd_date();

                            if (bStart.compareTo(dbDateStr) <= 0 && bEnd.compareTo(dbDateStr) >= 0) {
                                // Ưu tiên ngân sách cấu hình riêng cho danh mục này
                                if (categoryId.equals(b.getCategory_id())) {
                                    finalBudgetId = b.getBudget_id();
                                    break;
                                }
                                // Nếu không có, lưu tạm ngân sách tổng (Tổng ngân sách ID nhóm là "none")
                                else if ("none".equals(b.getCategory_id())) {
                                    matchedGlobal = b;
                                }
                            }
                        }
                    }

                    // Nếu không có ngân sách ưu tiên cho danh mục, thì gán ngân sách "Tổng"
                    if (finalBudgetId == null && matchedGlobal != null) {
                        finalBudgetId = matchedGlobal.getBudget_id();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tự động tìm ngân sách: " + e.getMessage());
            }

            final String effectiveBudgetId = finalBudgetId;

            try {
                double amountValue = Double.parseDouble(amount);
                if (amountValue <= 0) {
                    tilAmount.setError("Số tiền phải lớn hơn 0");
                    return;
                }

                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                inputFormat.setLenient(false);
                java.util.Date parsedDate = inputFormat.parse(date);
                String formattedDate = outputFormat.format(parsedDate);

                btnBack.setEnabled(false);
                btnSave.setEnabled(false);

                databaseHelper.addExpenseAsync(
                        userId,
                        String.valueOf(amountValue),
                        categoryId,
                        description,
                        formattedDate,
                        effectiveBudgetId,
                        new DatabaseHelper.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "addExpenseAsync: Success");

                                runOnUiThread(() -> {
                                    String warningMsg = checkBudgetWarningAfterExpense(effectiveBudgetId);
                                    if (warningMsg != null && !warningMsg.isEmpty()) {
                                        new AlertDialog.Builder(AddExpenseActivity.this)
                                            .setTitle("Cảnh báo Ngân sách")
                                            .setMessage(warningMsg)
                                            .setPositiveButton("Đã hiểu", (dialog, which) -> {
                                                finishWithResult(date, amount, categoryName, description, effectiveBudgetId);
                                            })
                                            .setCancelable(false)
                                            .show();
                                    } else {
                                        finishWithResult(date, amount, categoryName, description, effectiveBudgetId);
                                    }
                                });
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "addExpenseAsync: Error - " + errorMessage);
                                tilAmount.setError("Lỗi: " + errorMessage);
                                btnBack.setEnabled(true);
                                btnSave.setEnabled(true);
                            }
                        }
                );
            } catch (NumberFormatException e) {
                tilAmount.setError("Số tiền phải là một số hợp lệ");
            } catch (java.text.ParseException e) {
                tilDate.setError("Định dạng ngày không hợp lệ (dd/MM/yyyy)");
            } catch (Exception e) {
                tilAmount.setError("Lỗi không xác định: " + e.getMessage());
                btnBack.setEnabled(true);
                btnSave.setEnabled(true);
            }
        });
        Log.d(TAG, "Save button listener set");
    }

    private void loadBudgets(){
        List<Budgets> budgets = databaseHelper.getAllBudgets(userId);
        if (budgets.isEmpty()) {
            budgets.add(new Budgets("none", "Không có ngân sách"));
        } else {
            budgets.add(0, new Budgets("none", "Chọn ngân sách"));
        }
        ArrayAdapter<Budgets> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, budgets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudget.setAdapter(adapter);
    }

    private void loadCategories() {
        List<Categories> categoryList = databaseHelper.getAllCategories(userId, "expense");

        if (categoryList.isEmpty()) {
            categoryList.add(new Categories("none", "Không có danh mục"));
        } else {
            categoryList.add(0, new Categories("none", "Chọn danh mục"));
        }

        ArrayAdapter<Categories> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }



//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putString("date", etDate.getText().toString().trim());
//        outState.putString("amount", etAmount.getText().toString().trim());
//        outState.putString("category", etCategory.getText().toString().trim());
//        outState.putString("description", etDescription.getText().toString().trim());
//    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year1);
                    etDate.setText(formattedDate);
                    tilDate.setError(null);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void finishWithResult(String date, String amount, String categoryName, String description, String finalBudgetId) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("amount", amount);
        resultIntent.putExtra("category", categoryName);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("date", date);
        resultIntent.putExtra("budget", finalBudgetId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private String checkBudgetWarningAfterExpense(String budgetId) {
        Log.d(TAG, "checkBudgetWarningAfterExpense: called with budgetId=" + budgetId);
        if (budgetId == null || "none".equals(budgetId)) {
            Log.d(TAG, "checkBudgetWarningAfterExpense: budgetId is null or none, returning null");
            return null;
        }

        BudgetDAO budgetDAO = new BudgetDAO(databaseHelper.getReadableDatabase());
        NotificationDAO notificationDAO = new NotificationDAO(databaseHelper.getWritableDatabase());
        Budgets budget = budgetDAO.getBudgetById(budgetId);

        if (budget == null) {
            Log.d(TAG, "checkBudgetWarningAfterExpense: budget not found in DB");
            return null;
        }

        double spent = budgetDAO.getTotalSpentForBudget(budgetId);
        double budgetAmount = budget.getAmount();
        String budgetDesc = budget.getDescription() != null && !budget.getDescription().isEmpty() ? budget.getDescription() : "chưa rõ";

        Log.d(TAG, "checkBudgetWarningAfterExpense: spent=" + spent + " / budgetAmount=" + budgetAmount);

        int warningThreshold = budget.getWarning_threshold();
        double warningRatio = warningThreshold / 100.0;

        if (spent > budgetAmount) {
            Log.d(TAG, "checkBudgetWarningAfterExpense: over budget. msg will show.");
            String msg = "Bạn đã vượt ngân sách " + budgetDesc + " (Đã chi " + spent + " / " + budgetAmount + ").";
            Notifications notification = new Notifications(UUID.randomUUID().toString(), msg, false, null, "warn", userId);
            notificationDAO.insert(notification);
            return msg;
        } else if (spent >= budgetAmount * warningRatio) {
            Log.d(TAG, "checkBudgetWarningAfterExpense: warning ratio met. msg will show.");
            String msg = "Bạn sắp vượt ngân sách " + budgetDesc + " (Đã chi " + spent + " / " + budgetAmount + ")";
            Notifications notification = new Notifications(UUID.randomUUID().toString(), msg, false, null, "warn", userId);
            notificationDAO.insert(notification);
            return msg;
        }

        Log.d(TAG, "checkBudgetWarningAfterExpense: spent is safe. returning null.");
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_screens, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            Intent intent = new Intent(this, com.example.n03_quanlychitieu.ui.sign.notification_user.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}