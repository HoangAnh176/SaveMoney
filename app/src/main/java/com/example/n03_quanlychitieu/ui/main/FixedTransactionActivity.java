package com.example.n03_quanlychitieu.ui.main;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // Đã thêm import này
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.app.DatePickerDialog;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.db.DatabaseContract;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.model.FixedTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.Calendar;

public class FixedTransactionActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private String userId;
    private String transactionId;
    private String currentType = "expense";
    private Spinner spCategory;
    private TextView tvStartDate, tvEndDate;
    private EditText etAmount, etDesc;
    private Spinner spFreq;
    private boolean isEditMode = false;
    private RadioGroup rgType;
    private RadioButton rbExpense, rbIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixed_transaction);
        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getStringExtra("userId");
        transactionId = getIntent().getStringExtra("transactionId");

        if (transactionId != null && !transactionId.isEmpty()) {
            isEditMode = true;
        }

        rgType = findViewById(R.id.rg_type);
        rbExpense = findViewById(R.id.rb_expense);
        rbIncome = findViewById(R.id.rb_income);

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_income) {
                currentType = "income";
            } else {
                currentType = "expense";
            }
            loadCategories();
        });

        etAmount = findViewById(R.id.et_amount);
        etDesc = findViewById(R.id.et_desc);
        spFreq = findViewById(R.id.sp_frequency);
        spCategory = findViewById(R.id.sp_category);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);

        TextView btnSave = findViewById(R.id.btn_save_fixed);
        // Sửa TextView thành LinearLayout cho khớp với XML
        LinearLayout btnBack = findViewById(R.id.btn_back_form);
        View llDelete = findViewById(R.id.ll_delete_container);
        Button btnDelete = findViewById(R.id.btn_delete_fixed);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (isEditMode) {
            llDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                dbHelper.deleteFixedTransaction(transactionId);
                Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        String[] frequencies = {"Không bao giờ", "Hàng ngày", "Ngày trong tuần", "Hàng tuần", "2 tuần 1 lần", "3 tuần 1 lần", "Hàng tháng", "2 tháng 1 lần", "3 tháng 1 lần", "4 tháng 1 lần", "5 tháng 1 lần", "6 tháng 1 lần", "Hàng năm"};
        spFreq.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, frequencies));

        // Default start date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvStartDate.setText(sdf.format(new Date()));

        tvStartDate.setOnClickListener(v -> showDatePicker(tvStartDate, false));
        tvEndDate.setOnClickListener(v -> showDatePicker(tvEndDate, true));

        loadCategories();

        if (isEditMode) {
            populateData();
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String amount = etAmount.getText().toString();
                String desc = etDesc.getText().toString();
                String freq = spFreq.getSelectedItem().toString();
                String startDate = tvStartDate.getText().toString();
                String endDate = tvEndDate.getText().toString();

                if(amount.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                    return;
                }

                Categories selectedCategory = (Categories) spCategory.getSelectedItem();
                String catId = selectedCategory != null ? selectedCategory.getCategory_id() : "none";

                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    if (!isEditMode) {
                        transactionId = UUID.randomUUID().toString();
                    }
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_ID, transactionId);
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_USER_ID, userId != null ? userId : "");
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_TYPE, currentType);
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_AMOUNT, Double.parseDouble(amount));

                    cv.put(DatabaseContract.FixedTransactions.COLUMN_CATEGORY_ID, catId);
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_DESCRIPTION, desc);
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_FREQUENCY, freq);

                    // Format dates to standard format
                    SimpleDateFormat inFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                    Date sDate = inFormat.parse(startDate);
                    cv.put(DatabaseContract.FixedTransactions.COLUMN_START_DATE, outFormat.format(sDate));

                    Date eDate = null;
                    if (!endDate.equals("Không") && !endDate.isEmpty()) {
                        eDate = inFormat.parse(endDate);
                        cv.put(DatabaseContract.FixedTransactions.COLUMN_END_DATE, outFormat.format(eDate));
                    } else {
                        cv.putNull(DatabaseContract.FixedTransactions.COLUMN_END_DATE);
                    }

                    if (isEditMode) {
                        db.update(DatabaseContract.FixedTransactions.TABLE_NAME, cv, DatabaseContract.FixedTransactions.COLUMN_ID + "=?", new String[]{transactionId});
                    } else {
                        db.insert(DatabaseContract.FixedTransactions.TABLE_NAME, null, cv);
                    }

                    generateActualTransactions(db, transactionId, currentType, Double.parseDouble(amount), catId, desc, freq, sDate, eDate);

                    db.close();

                    Toast.makeText(this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } catch(Exception e) {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void populateData() {
        FixedTransaction transaction = dbHelper.getFixedTransactionById(transactionId);
        if (transaction != null) {
            currentType = transaction.getType();
            if ("income".equals(currentType)) {
                rbIncome.setChecked(true);
            } else {
                rbExpense.setChecked(true);
            }
            loadCategories();

            etAmount.setText(String.valueOf((int)transaction.getAmount()));
            etDesc.setText(transaction.getDescription());

            // set frequency
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spFreq.getAdapter();
            for (int i=0; i<adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(transaction.getFrequency())) {
                    spFreq.setSelection(i);
                    break;
                }
            }

            try {
                SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat inFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                if (transaction.getStartDate() != null && !transaction.getStartDate().isEmpty()) {
                    Date sd = outFormat.parse(transaction.getStartDate());
                    tvStartDate.setText(inFormat.format(sd));
                }

                if (transaction.getEndDate() != null && !transaction.getEndDate().isEmpty()) {
                    Date ed = outFormat.parse(transaction.getEndDate());
                    tvEndDate.setText(inFormat.format(ed));
                } else {
                    tvEndDate.setText("Không");
                }
            } catch (Exception e) {}

            // Wait for categories to load, then select the right one
            spCategory.post(() -> {
                ArrayAdapter<Categories> catAdapter = (ArrayAdapter<Categories>) spCategory.getAdapter();
                if (catAdapter != null) {
                    for (int i=0; i<catAdapter.getCount(); i++) {
                        if (catAdapter.getItem(i).getCategory_id().equals(transaction.getCategoryId())) {
                            spCategory.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void generateActualTransactions(SQLiteDatabase db, String fixedId, String type, double amount, String categoryId, String description, String freq, Date startDate, Date endDate) {
        // Clear all previous auto-generated records for this fixed transaction
        db.delete(DatabaseContract.Expenses.TABLE_NAME, "fixed_id = ?", new String[]{fixedId});
        db.delete(DatabaseContract.Incomes.TABLE_NAME, "fixed_id = ?", new String[]{fixedId});

        if ("Không bao giờ".equalsIgnoreCase(freq) || "none".equals(freq) || freq == null || freq.isEmpty()) {
            return;
        }

        Date targetEndDate = endDate;
        if (targetEndDate == null) {
            targetEndDate = Calendar.getInstance().getTime();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

        while (cal.getTime().compareTo(targetEndDate) <= 0) {
            String dateStr = outFormat.format(cal.getTime());
            ContentValues cv = new ContentValues();

            if (type.equals("income")) {
                cv.put(DatabaseContract.Incomes.COLUMN_INCOME_ID, UUID.randomUUID().toString());
                cv.put(DatabaseContract.Incomes.COLUMN_AMOUNT, amount);
                cv.put(DatabaseContract.Incomes.COLUMN_DESCRIPTION, (description != null && !description.isEmpty()) ? description : "Thu nhập cố định");
                cv.put(DatabaseContract.Incomes.COLUMN_CREATE_AT, dateStr);
                cv.put(DatabaseContract.Incomes.COLUMN_USER_ID, userId);
                cv.put(DatabaseContract.Incomes.COLUMN_CATEGORY_ID, categoryId);
                cv.put(DatabaseContract.Incomes.COLUMN_FIXED_ID, fixedId);
                db.insert(DatabaseContract.Incomes.TABLE_NAME, null, cv);
            } else {
                cv.put(DatabaseContract.Expenses.COLUMN_EXPENSE_ID, UUID.randomUUID().toString());
                cv.put(DatabaseContract.Expenses.COLUMN_AMOUNT, amount);
                cv.put(DatabaseContract.Expenses.COLUMN_DESCRIPTION, (description != null && !description.isEmpty()) ? description : "Chi phí cố định");
                cv.put(DatabaseContract.Expenses.COLUMN_CREATE_AT, dateStr);
                cv.put(DatabaseContract.Expenses.COLUMN_USER_ID, userId);
                cv.put(DatabaseContract.Expenses.COLUMN_CATEGORY_ID, categoryId);
                cv.put(DatabaseContract.Expenses.COLUMN_FIXED_ID, fixedId);
                db.insert(DatabaseContract.Expenses.TABLE_NAME, null, cv);
            }

            if ("Hàng ngày".equalsIgnoreCase(freq)) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            } else if ("Ngày trong tuần".equalsIgnoreCase(freq)) {
                do {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
            } else if ("Hàng tuần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            } else if ("2 tuần 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.WEEK_OF_YEAR, 2);
            } else if ("3 tuần 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.WEEK_OF_YEAR, 3);
            } else if ("Hàng tháng".equalsIgnoreCase(freq)) {
                cal.add(Calendar.MONTH, 1);
            } else if ("2 tháng 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.MONTH, 2);
            } else if ("3 tháng 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.MONTH, 3);
            } else if ("4 tháng 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.MONTH, 4);
            } else if ("5 tháng 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.MONTH, 5);
            } else if ("6 tháng 1 lần".equalsIgnoreCase(freq)) {
                cal.add(Calendar.MONTH, 6);
            } else if ("Hàng năm".equalsIgnoreCase(freq)) {
                cal.add(Calendar.YEAR, 1);
            } else {
                break;
            }
        }
    }

    private void loadCategories() {
        if (userId == null) return;
        List<Categories> categoryList = dbHelper.getAllCategories(userId, currentType);
        if (categoryList.isEmpty()) {
            categoryList.add(new Categories("none", "Không có danh mục"));
        } else {
            categoryList.add(0, new Categories("none", "Chọn danh mục"));
        }
        ArrayAdapter<Categories> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
    }

    private void showDatePicker(TextView target, boolean isEndDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    target.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        if (isEndDate) {
            datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Không", (dialog, which) -> {
                target.setText("Không");
            });
        }

        datePickerDialog.show();
    }
}