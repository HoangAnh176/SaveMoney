package com.example.n03_quanlychitieu.ui.income;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.dao.BudgetDAO;
import com.example.n03_quanlychitieu.dao.NotificationDAO;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Budgets;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.model.Notifications;
import com.example.n03_quanlychitieu.ui.category.AddCategoryActivity;
import com.example.n03_quanlychitieu.ui.sign.LogIn;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddIncomeActivity extends AppCompatActivity {
    private static final String TAG = "AddIncomeActivity";
    private TextInputEditText etAmount, etSource, etDate;
    private TextInputLayout tilAmount, tilSource, tilDate;
    private Button btnSave;
    private ImageButton btnBack;
    private Spinner spinnerCategory;
    private TextInputLayout tilCategory;
    private DatabaseHelper databaseHelper;
    private AuthenticationManager auth;
    private String userId;
    private Map<String, String> categoryNameToIdMap; // Ánh xạ giữa tên danh mục và categoryId


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
            Log.d(TAG, "onCreate called at " + time);
        }

        databaseHelper = new DatabaseHelper(this);
        auth = AuthenticationManager.getInstance(this);

        if (!auth.isUserLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LogIn.class));
            finish();
            return;
        }

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUser_id() : null;
        }
        if (userId == null) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show();
            auth.logout();
            startActivity(new Intent(this, LogIn.class));
            finish();
            return;
        }
        Log.d(TAG, "Received userId: " + userId);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tilAmount = findViewById(R.id.tilAmount);
        tilSource = findViewById(R.id.tilSource);
        tilDate = findViewById(R.id.tilDate);
        tilCategory = findViewById(R.id.tilCategory);
        etAmount = findViewById(R.id.etAmount);
        etSource = findViewById(R.id.etSource);
        etDate = findViewById(R.id.etDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        categoryNameToIdMap = new HashMap<>();

        if (savedInstanceState != null) {
            etAmount.setText(savedInstanceState.getString("amount"));
            etSource.setText(savedInstanceState.getString("source"));
            etDate.setText(savedInstanceState.getString("date"));
            Log.d(TAG, "Restored state: amount=" + etAmount.getText().toString() +
                    ", source=" + etSource.getText().toString() + ", date=" + etDate.getText().toString());
        }

        loadCategoriesIncome();

        etDate.setOnClickListener(v -> showDatePicker());
        Log.d(TAG, "Date picker listener set");

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            setResult(RESULT_CANCELED);
            finish();
        });

        btnSave.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String time2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                Log.d(TAG, "Save button clicked at " + time2);
            }

            tilAmount.setError(null);
            tilSource.setError(null);
            tilDate.setError(null);
            tilCategory.setError(null);

            String amount = etAmount.getText().toString().trim();
            String source = etSource.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String selectedCategoryName = spinnerCategory.getSelectedItem() != null ?
                    spinnerCategory.getSelectedItem().toString() : null;
            String categoryId = selectedCategoryName != null ? categoryNameToIdMap.get(selectedCategoryName) : null;
            Log.d(TAG, "Input values: amount=" + amount + ", source=" + source + ", date=" + date + ", categoryId=" + categoryId);

            if (amount.isEmpty()) {
                tilAmount.setError("Vui lòng nhập số tiền");
                return;
            }
            if (source.isEmpty()) {
                tilSource.setError("Vui lòng nhập nguồn thu nhập");
                return;
            }
            if (date.isEmpty()) {
                tilDate.setError("Vui lòng chọn ngày");
                return;
            }
            if (categoryId == null || categoryId.isEmpty()) {
                tilCategory.setError("Vui lòng chọn danh mục");
                return;
            }

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

                databaseHelper.addIncomeAsync(
                        userId,
                        String.valueOf(amountValue),
                        categoryId,
                        source,
                        formattedDate,
                        new DatabaseHelper.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("amount", amount);
                                resultIntent.putExtra("source", source);
                                resultIntent.putExtra("date", date);
                                setResult(RESULT_OK, resultIntent);
                                Toast.makeText(AddIncomeActivity.this, "Thêm thu nhập thành công", Toast.LENGTH_SHORT).show();

                                finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                tilAmount.setError("Lỗi: " + errorMessage);
                                btnBack.setEnabled(true);
                                btnSave.setEnabled(true);
                                Log.e(TAG, "Error adding income: " + errorMessage);
                            }
                        }
                );
            } catch (NumberFormatException e) {
                tilAmount.setError("Số tiền phải là một số hợp lệ");
                Log.e(TAG, "NumberFormatException: " + e.getMessage());
            } catch (java.text.ParseException e) {
                tilDate.setError("Định dạng ngày không hợp lệ (dd/MM/yyyy)");
                Log.e(TAG, "ParseException: " + e.getMessage());
            } catch (Exception e) {
                tilAmount.setError("Lỗi không xác định: " + e.getMessage());
                btnBack.setEnabled(true);
                btnSave.setEnabled(true);
                Log.e(TAG, "Unexpected error: " + e.getMessage());
            }
        });
        Log.d(TAG, "Save button listener set");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null && databaseHelper != null) {
            loadCategoriesIncome();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("amount", etAmount.getText().toString().trim());
        outState.putString("source", etSource.getText().toString().trim());
        outState.putString("date", etDate.getText().toString().trim());
        Log.d(TAG, "Saved instance state: amount=" + etAmount.getText().toString() +
                ", source=" + etSource.getText().toString() + ", date=" + etDate.getText().toString());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etDate.setText(sdf.format(selectedDate.getTime()));
                    Log.d(TAG, "Selected date: " + sdf.format(selectedDate.getTime()));
                },
                year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadCategoriesIncome() {
        List<String> categoryNames = new ArrayList<>();
        try {
            List<Categories> categoryList = databaseHelper.getCategoriesByUserIdAndType(userId, "income");
            if (categoryList.isEmpty()) {
                Toast.makeText(this, "Không có danh mục thu nhập. Vui lòng thêm danh mục tại AddCategoryActivity.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, com.example.n03_quanlychitieu.ui.category.AddCategoryActivity.class);
                intent.putExtra("type", "income");
                intent.putExtra("userId", userId);
                startActivity(intent);
                return;
            }
            for (Categories cat : categoryList) {
                categoryNames.add(cat.getName());
                categoryNameToIdMap.put(cat.getName(), cat.getCategory_id());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading categories: " + e.getMessage());
            Toast.makeText(this, "Lỗi tải danh mục. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
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