package com.example.n03_quanlychitieu.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.n03_quanlychitieu.model.Budgets;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.model.Expenses;
import com.example.n03_quanlychitieu.model.FixedTransaction;
import com.example.n03_quanlychitieu.model.Incomes;
import com.example.n03_quanlychitieu.model.Users;
import com.example.n03_quanlychitieu.model.Transaction;
import com.example.n03_quanlychitieu.ui.expense.ViewExpenseActivity;
import com.example.n03_quanlychitieu.ui.income.ViewIncomeActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "fin_manager.db";
    private static final int DATABASE_VERSION = 9;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
        Log.d(TAG, "Foreign key constraints enabled");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseContract.Users.CREATE_TABLE);
        db.execSQL(DatabaseContract.Categories.CREATE_TABLE);
        db.execSQL(DatabaseContract.Notifications.CREATE_TABLE);
        db.execSQL(DatabaseContract.Budgets.CREATE_TABLE);
        db.execSQL(DatabaseContract.Incomes.CREATE_TABLE);
        db.execSQL(DatabaseContract.Expenses.CREATE_TABLE);
        db.execSQL(DatabaseContract.FixedTransactions.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 7) {
            // Xóa bảng cũ
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.Expenses.TABLE_NAME);
            // Tạo lại bảng mới với cấu trúc đã cập nhật
            db.execSQL(DatabaseContract.Expenses.CREATE_TABLE);

            // Add warning threshold to budgets
            try {
                db.execSQL("ALTER TABLE " + DatabaseContract.Budgets.TABLE_NAME + " ADD COLUMN " + DatabaseContract.Budgets.COLUMN_WARNING_THRESHOLD + " INTEGER DEFAULT 90");
            } catch (Exception e) {
                Log.e(TAG, "Error adding warning_threshold: " + e.getMessage());
            }
        }
        if (oldVersion < 8) {
            db.execSQL(DatabaseContract.FixedTransactions.CREATE_TABLE);
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE " + DatabaseContract.Incomes.TABLE_NAME + " ADD COLUMN " + DatabaseContract.Incomes.COLUMN_FIXED_ID + " TEXT");
                db.execSQL("ALTER TABLE " + DatabaseContract.Expenses.TABLE_NAME + " ADD COLUMN " + DatabaseContract.Expenses.COLUMN_FIXED_ID + " TEXT");
            } catch (Exception e) {
                Log.e(TAG, "Error adding fixed_id column: " + e.getMessage());
            }
        }
    }

    // Callback interfaces
    public interface SimpleCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface UserCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface UserCheckCallback {
        void onUsernameExists();
        void onEmailExists();
        void onAvailable();
        void onError(String message);
    }

    public interface EmailCallback {
        void onSuccess(boolean emailExists);
        void onError(String errorMessage);
    }

    public interface ResetPasswordCallback {
        void onSuccess(int rowsAffected);
        void onError(String errorMessage);
    }

    public interface GetUserByEmailCallback {
        void onUserLoaded(Users user);
        void onUserNotFound();
        void onError(String errorMessage);
    }

    public interface GetUserCallback {
        void onUserLoaded(Users user);
        void onUserNotFound();
        void onError(String errorMessage);
    }

    // Helper methods
    private boolean checkCategoryExists(SQLiteDatabase db, String categoryId, String userId) {
        if (categoryId == null || userId == null) {
            Log.e(TAG, "checkCategoryExists: categoryId or userId is null");
            return false;
        }
        String query = "SELECT 1 FROM " + DatabaseContract.Categories.TABLE_NAME +
                " WHERE " + DatabaseContract.Categories.COLUMN_CATEGORY_ID + " = ? AND " +
                DatabaseContract.Categories.COLUMN_USER_ID + " = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, new String[]{categoryId, userId})) {
            boolean exists = cursor != null && cursor.moveToFirst();
            Log.d(TAG, "checkCategoryExists: categoryId=" + categoryId + ", userId=" + userId + ", exists=" + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "checkCategoryExists error: " + e.getMessage());
            return false;
        }
    }

    private boolean checkBudgetExists(SQLiteDatabase db, String budgetId) {
        if (budgetId == null) {
            Log.e(TAG, "checkBudgetExists: budgetId is null");
            return false;
        }
        String query = "SELECT 1 FROM " + DatabaseContract.Budgets.TABLE_NAME +
                " WHERE " + DatabaseContract.Budgets.COLUMN_BUDGET_ID + " = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, new String[]{budgetId})) {
            boolean exists = cursor != null && cursor.moveToFirst();
            Log.d(TAG, "checkBudgetExists: budgetId=" + budgetId + ", exists=" + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "checkBudgetExists error: " + e.getMessage());
            return false;
        }
    }

    private boolean checkIncomeExists(SQLiteDatabase db, String incomeId, String userId) {
        if (incomeId == null || userId == null) {
            Log.e(TAG, "checkIncomeExists: incomeId or userId is null");
            return false;
        }
        String query = "SELECT 1 FROM " + DatabaseContract.Incomes.TABLE_NAME +
                " WHERE " + DatabaseContract.Incomes.COLUMN_INCOME_ID + " = ? AND " +
                DatabaseContract.Incomes.COLUMN_USER_ID + " = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, new String[]{incomeId, userId})) {
            boolean exists = cursor != null && cursor.moveToFirst();
            Log.d(TAG, "checkIncomeExists: incomeId=" + incomeId + ", userId=" + userId + ", exists=" + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "checkIncomeExists error: " + e.getMessage());
            return false;
        }
    }

    private boolean checkFieldExists(SQLiteDatabase db, String column, String value) {
        Cursor cursor = db.query(
                DatabaseContract.Users.TABLE_NAME,
                new String[]{column},
                column + " = ?",
                new String[]{value},
                null, null, null
        );
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    private boolean checkFieldExistsForUpdate(SQLiteDatabase db, String column, String value, String userId) {
        Cursor cursor = db.query(
                DatabaseContract.Users.TABLE_NAME,
                new String[]{column},
                column + " = ? AND " + DatabaseContract.Users.COLUMN_USER_ID + " != ?",
                new String[]{value, userId},
                null, null, null
        );
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    private boolean checkEmailExists(SQLiteDatabase db, String email) {
        String query = "SELECT 1 FROM " + DatabaseContract.Users.TABLE_NAME +
                " WHERE " + DatabaseContract.Users.COLUMN_EMAIL + " = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, new String[]{email})) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // Category management
    public List<Categories> getCategoriesByUserIdAndType(String userId, String type) {
        List<Categories> categories = new ArrayList<>();
        if (userId == null || userId.isEmpty() || type == null || type.isEmpty()) {
            Log.e(TAG, "getCategoriesByUserIdAndType: Invalid userId or type");
            return categories;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(DatabaseContract.Categories.TABLE_NAME, null,
                DatabaseContract.Categories.COLUMN_USER_ID + " = ? AND " +
                        DatabaseContract.Categories.COLUMN_TYPE + " = ?",
                new String[]{userId, type}, null, null, null);
        while (cursor != null && cursor.moveToNext()) {
            Categories category = new Categories(
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_CATEGORY_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_ICON)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_COLOR)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_USER_ID))
            );
            categories.add(category);
        }
        if (cursor != null) cursor.close();
        Log.d(TAG, "getCategoriesByUserIdAndType: userId=" + userId + ", type=" + type + ", count=" + categories.size());
        return categories;
    }

    public String getCategoryNameById(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            Log.e(TAG, "getCategoryNameById: categoryId is null or empty");
            return "Không xác định";
        }
        SQLiteDatabase db = this.getReadableDatabase();
        String categoryName = "Không xác định";
        Cursor cursor = db.query(DatabaseContract.Categories.TABLE_NAME,
                new String[]{DatabaseContract.Categories.COLUMN_NAME},
                DatabaseContract.Categories.COLUMN_CATEGORY_ID + " = ?",
                new String[]{categoryId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Categories.COLUMN_NAME));
            cursor.close();
        }
        Log.d(TAG, "getCategoryNameById: categoryId=" + categoryId + ", name=" + categoryName);
        return categoryName;
    }


    // Income management
    public void addIncomeAsync(String userId, String amount, String categoryId, String description, String date, SimpleCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (userId == null || userId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("User ID is missing"));
                return;
            }
            try {
                double amountValue = Double.parseDouble(amount);
                SQLiteDatabase db = this.getWritableDatabase();
                if (!checkCategoryExists(db, categoryId, userId)) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Danh mục không tồn tại cho user: " + categoryId));
                    db.close();
                    return;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put("income_id", UUID.randomUUID().toString());
                contentValues.put("user_id", userId);
                contentValues.put("amount", amountValue);
                contentValues.put("category_id", categoryId);
                contentValues.put("description", description != null ? description : "");
                contentValues.put("create_at", date);
                long result = db.insert(DatabaseContract.Incomes.TABLE_NAME, null, contentValues);
                db.close();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result != -1) callback.onSuccess();
                    else callback.onError("Không thể thêm thu nhập");
                });
            } catch (NumberFormatException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Số tiền không hợp lệ: " + e.getMessage()));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi: " + e.getMessage()));
            }
        });
    }

    public void updateIncomeAsync(String incomeId, String userId, String amount, String categoryId, String description, String date, SimpleCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (userId == null || userId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("User ID is missing"));
                return;
            }
            if (incomeId == null || incomeId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Income ID is missing"));
                return;
            }
            try {
                double amountValue = Double.parseDouble(amount);
                SQLiteDatabase db = this.getWritableDatabase();
                if (!checkIncomeExists(db, incomeId, userId)) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Thu nhập không tồn tại: " + incomeId));
                    db.close();
                    return;
                }
                if (!checkCategoryExists(db, categoryId, userId)) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Danh mục không tồn tại cho user: " + categoryId));
                    db.close();
                    return;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put("amount", amountValue);
                contentValues.put("category_id", categoryId);
                contentValues.put("description", description != null ? description : "");
                contentValues.put("create_at", date != null ? date : "");
                int result = db.update(DatabaseContract.Incomes.TABLE_NAME, contentValues,
                        "income_id = ? AND user_id = ?", new String[]{incomeId, userId});
                db.close();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result > 0) {
                        Log.d(TAG, "Income updated successfully: incomeId=" + incomeId);
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "No rows updated for incomeId=" + incomeId);
                        callback.onError("Không thể cập nhật thu nhập: Không tìm thấy bản ghi");
                    }
                });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid amount format: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Số tiền không hợp lệ: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Update income error: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi: " + e.getMessage()));
            }
        });
    }

    public void deleteIncomeAsync(String incomeId, String userId, SimpleCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (userId == null || userId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("User ID is missing"));
                return;
            }
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                int result = db.delete(DatabaseContract.Incomes.TABLE_NAME,
                        "income_id = ? AND user_id = ?", new String[]{incomeId, userId});
                db.close();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result > 0) callback.onSuccess();
                    else callback.onError("Không thể xóa thu nhập");
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi: " + e.getMessage()));
            }
        });
    }

    public List<ViewIncomeActivity.Income> getIncomesByUserId(String userId) {
        List<ViewIncomeActivity.Income> list = new ArrayList<>();
        if (userId == null || userId.isEmpty()) return list;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(DatabaseContract.Incomes.TABLE_NAME, null,
                "user_id = ?", new String[]{userId}, null, null, "create_at DESC");
        while (cursor != null && cursor.moveToNext()) {
            String income_id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_INCOME_ID));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_AMOUNT));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_DESCRIPTION));
            String create_at = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_CREATE_AT));
            String user_id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_USER_ID));
            String category_id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_CATEGORY_ID));
            list.add(new ViewIncomeActivity.Income(income_id, amount, description, create_at, user_id, category_id));
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public List<Transaction> getTransactionsByDateRange(String userId, String startDate, String endDate) {
        List<Transaction> list = new ArrayList<>();
        if (userId == null) return list;
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT e.expense_id as id, e.amount, e.description, e.create_at, 'expense' as type, c.name, c.icon, c.color, e.category_id, e.budget_id, e.fixed_id " +
                       "FROM Expenses e LEFT JOIN Categories c ON e.category_id = c.category_id WHERE e.user_id = ? AND e.create_at >= ? AND e.create_at <= ? " +
                       "UNION ALL " +
                       "SELECT i.income_id as id, i.amount, i.description, i.create_at, 'income' as type, c.name, c.icon, c.color, i.category_id, NULL as budget_id, i.fixed_id " +
                       "FROM Incomes i LEFT JOIN Categories c ON i.category_id = c.category_id WHERE i.user_id = ? AND i.create_at >= ? AND i.create_at <= ? " +
                       "ORDER BY create_at DESC";

        Cursor cursor = db.rawQuery(query, new String[]{userId, startDate, endDate, userId, startDate, endDate});
        while (cursor != null && cursor.moveToNext()) {
            list.add(new Transaction(
                cursor.getString(0),
                cursor.getDouble(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5), // category name
                cursor.getString(6), // category icon
                cursor.getString(7), // category color
                cursor.getString(8), // category id
                cursor.getString(9), // budget id
                cursor.getString(10) // fixed id
            ));
        }
        if (cursor != null) cursor.close();


        return list;
    }

    // Expense management
    public void addExpenseAsync(String userId, String amount, String categoryId, String description, String date, String budgetId, SimpleCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (userId == null || userId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("User ID is missing"));
                return;
            }
            try {
                double amountValue = Double.parseDouble(amount);
                SQLiteDatabase db = this.getWritableDatabase();

                // Kiểm tra danh mục (categoryId)
                if (!checkCategoryExists(db, categoryId, userId)) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Danh mục không tồn tại cho user: " + categoryId));
                    db.close();
                    return;
                }

                // Chuẩn bị dữ liệu để lưu vào cơ sở dữ liệu
                ContentValues contentValues = new ContentValues();
                contentValues.put("expense_id", UUID.randomUUID().toString());
                contentValues.put("user_id", userId);
                contentValues.put("amount", amountValue);
                contentValues.put("category_id", categoryId);
                contentValues.put("description", description != null ? description : "");
                contentValues.put("create_at", date);

                // Chỉ thêm budget_id nếu không null
                if (budgetId != null) {
                    contentValues.put("budget_id", budgetId);
                }

                // Thực hiện chèn dữ liệu
                long result = db.insert(DatabaseContract.Expenses.TABLE_NAME, null, contentValues);
                db.close();
                // Xử lý kết quả chèn
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result != -1) {
                        Log.d(TAG, "addExpenseAsync: Chi tiêu được thêm thành công");
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "addExpenseAsync: Lỗi khi thêm chi tiêu");
                        callback.onError("Không thể thêm chi tiêu");
                    }
                });
            } catch (NumberFormatException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Số tiền không hợp lệ: " + e.getMessage()));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi: " + e.getMessage()));
            }
        });
    }
    public void updateExpenseAsync(String expenseId, String userId, String amount, String categoryId,
                                   String description, String date, String budgetId, SimpleCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Kiểm tra userId và expenseId
            if (userId == null || userId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("User ID is missing")
                );
                return;
            }
            if (expenseId == null || expenseId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Expense ID is missing")
                );
                return;
            }

            // 2. Parse amount
            double amountValue;
            try {
                amountValue = Double.parseDouble(amount);
            } catch (NumberFormatException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Số tiền không hợp lệ: " + e.getMessage())
                );
                return;
            }

            SQLiteDatabase db = this.getWritableDatabase();
            try {
                // 3. Kiểm tra category tồn tại
                Log.d("DB", "Checking category: categoryId=" + categoryId + ", userId=" + userId);
                if (!checkCategoryExists(db, categoryId, userId)) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Danh mục không tồn tại: " + categoryId)
                    );
                    return;
                }

                // 4. Chỉ kiểm tra budget nếu budgetId không null và khác "none"
                String finalBudgetId = (budgetId != null && !"none".equals(budgetId)) ? budgetId : null;
                if (finalBudgetId != null) {
                    Log.d("DB", "Checking budget: budgetId=" + finalBudgetId);
                    if (!checkBudgetExists(db, finalBudgetId)) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Ngân sách không tồn tại: " + finalBudgetId)
                        );
                        return;
                    }
                }

                // 5. Chuẩn bị ContentValues
                ContentValues cv = new ContentValues();
                cv.put("amount", amountValue);
                cv.put("category_id", categoryId);
                cv.put("description", description != null ? description : "");
                cv.put("create_at", date);
                if (finalBudgetId != null) {
                    cv.put("budget_id", finalBudgetId);
                } else {
                    cv.putNull("budget_id");
                }
                Log.d("DB", "Preparing update: expenseId=" + expenseId + ", userId=" + userId);

                // 6. Thực hiện update
                int rows = db.update(
                        DatabaseContract.Expenses.TABLE_NAME,
                        cv,
                        "expense_id = ? AND user_id = ?",
                        new String[]{expenseId, userId}
                );

                Log.d("DB", "Rows updated: " + rows);

                // 7. Gọi callback trên main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (rows > 0) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Không tìm thấy chi tiêu để cập nhật");
                    }
                });
            } catch (Exception e) {
                Log.e("DB", "Error updating expense: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Lỗi khi cập nhật: " + e.getMessage())
                );
            } finally {
                db.close();
            }
        });
    }

    public void deleteExpenseAsync(String expenseId, String userId, SimpleCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (userId == null || userId.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("User ID is missing"));
                return;
            }
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                int result = db.delete(DatabaseContract.Expenses.TABLE_NAME,
                        "expense_id = ? AND user_id = ?", new String[]{expenseId, userId});
                db.close();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result > 0) callback.onSuccess();
                    else callback.onError("Không thể xóa chi tiêu");
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi: " + e.getMessage()));
            }
        });
    }

    public List<ViewExpenseActivity.Expense> getExpensesByUserId(String userId) {
        List<ViewExpenseActivity.Expense> list = new ArrayList<>();
        if (userId == null || userId.isEmpty()) return list;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseContract.Expenses.TABLE_NAME,
                null,
                "user_id = ?",
                new String[]{userId},
                null,
                null,
                "create_at DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String expenseId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Expenses.COLUMN_EXPENSE_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Expenses.COLUMN_AMOUNT));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Expenses.COLUMN_DESCRIPTION));
                String createAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Expenses.COLUMN_CREATE_AT));
                String categoryId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Expenses.COLUMN_CATEGORY_ID));
                String budgetId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Expenses.COLUMN_BUDGET_ID));

                // Thêm đối tượng Expense vào danh sách
                list.add(new ViewExpenseActivity.Expense(expenseId, amount, categoryId, description, createAt));
            }
            cursor.close();
        }
        return list;
    }

    // User management
    public void addUserAsync(String userID, String username, String email, String hashPassword, UserCallback callback) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues contentValues = new ContentValues();
                contentValues.put(DatabaseContract.Users.COLUMN_USER_ID, userID);
                contentValues.put(DatabaseContract.Users.COLUMN_USERNAME, username);
                contentValues.put(DatabaseContract.Users.COLUMN_EMAIL, email);
                contentValues.put(DatabaseContract.Users.COLUMN_PASSWORD, hashPassword);

                long result = db.insert(DatabaseContract.Users.TABLE_NAME, null, contentValues);

                if (result != -1) {
                    insertDefaultCategories(db, userID);
                }

                db.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result != -1) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Failed to insert user");
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void checkUser(String username, String email, UserCheckCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                boolean usernameExists = checkFieldExists(db, DatabaseContract.Users.COLUMN_USERNAME, username);
                boolean emailExists = checkFieldExists(db, DatabaseContract.Users.COLUMN_EMAIL, email);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (usernameExists) {
                        callback.onUsernameExists();
                    } else if (emailExists) {
                        callback.onEmailExists();
                    } else {
                        callback.onAvailable();
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi hệ thống: " + e.getMessage()));
            }
        });
    }

    public void checkEmail(String email, EmailCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                boolean emailExists = checkEmailExists(db, email);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(emailExists));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void resetPassword(String email, String hashPassword, ResetPasswordCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (SQLiteDatabase db = this.getWritableDatabase()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(DatabaseContract.Users.COLUMN_PASSWORD, hashPassword);
                int rowsAffected = db.update(
                        DatabaseContract.Users.TABLE_NAME,
                        contentValues,
                        DatabaseContract.Users.COLUMN_EMAIL + " = ?",
                        new String[]{email}
                );
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(rowsAffected));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    @SuppressLint("Range")
    public void getUserByEmail(String email, GetUserByEmailCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                Cursor cursor = db.query(
                        DatabaseContract.Users.TABLE_NAME,
                        null,
                        DatabaseContract.Users.COLUMN_EMAIL + " = ?",
                        new String[]{email},
                        null, null, null
                );

                Users user;
                if (cursor != null && cursor.moveToFirst()) {
                    user = new Users();
                    user.setUser_id(cursor.getString(cursor.getColumnIndex(DatabaseContract.Users.COLUMN_USER_ID)));
                    user.setUsername(cursor.getString(cursor.getColumnIndex(DatabaseContract.Users.COLUMN_USERNAME)));
                    user.setEmail(cursor.getString(cursor.getColumnIndex(DatabaseContract.Users.COLUMN_EMAIL)));
                    user.setPassword(cursor.getString(cursor.getColumnIndex(DatabaseContract.Users.COLUMN_PASSWORD)));
                    user.setAvatar_url(cursor.getString(cursor.getColumnIndex(DatabaseContract.Users.COLUMN_AVATAR_URL)));
                    user.setCreated_at(cursor.getString(cursor.getColumnIndex(DatabaseContract.Users.COLUMN_CREATED_AT)));
                } else {
                    user = null;
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (user != null) {
                        callback.onUserLoaded(user);
                    } else {
                        callback.onUserNotFound();
                    }
                });

                if (cursor != null) cursor.close();
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Database error: " + e.getMessage()));
            }
        });
    }

    @SuppressLint("Range")
    public void getUserAsync(String usernameOrEmail, String rawPassword, GetUserCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                Cursor c = db.query(
                        DatabaseContract.Users.TABLE_NAME,
                        null,
                        DatabaseContract.Users.COLUMN_USERNAME + " = ? OR " +
                                DatabaseContract.Users.COLUMN_EMAIL + " = ?",
                        new String[]{usernameOrEmail, usernameOrEmail},
                        null, null, null
                );

                Users user;
                boolean passwordMatch;

                if (c != null && c.moveToFirst()) {
                    user = new Users();
                    user.setUser_id(c.getString(c.getColumnIndex(DatabaseContract.Users.COLUMN_USER_ID)));
                    user.setUsername(c.getString(c.getColumnIndex(DatabaseContract.Users.COLUMN_USERNAME)));
                    user.setEmail(c.getString(c.getColumnIndex(DatabaseContract.Users.COLUMN_EMAIL)));
                    String storedHash = c.getString(c.getColumnIndex(DatabaseContract.Users.COLUMN_PASSWORD));
                    user.setPassword(null);
                    user.setAvatar_url(c.getString(c.getColumnIndex(DatabaseContract.Users.COLUMN_AVATAR_URL)));
                    passwordMatch = BCrypt.verifyer().verify(rawPassword.toCharArray(), storedHash).verified;
                } else {
                    passwordMatch = false;
                    user = null;
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (user != null && passwordMatch) {
                        callback.onUserLoaded(user);
                    } else {
                        callback.onUserNotFound();
                    }
                });

                if (c != null) c.close();
            } catch (Exception e) {
                Log.e(TAG, "Error opening/querying DB", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Database error: " + e.getMessage()));
            }
        });
    }

    public Users getUserById(String userId) {
        if (userId == null || userId.isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Users user = null;
        Cursor cursor = db.query(
                DatabaseContract.Users.TABLE_NAME,
                null,
                "user_id = ?",
                new String[]{userId},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            user = new Users(
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_EMAIL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_PASSWORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_AVATAR_URL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_CREATED_AT))
            );
            cursor.close();
        }
        db.close();
        return user;
    }

    public String getPasswordForUser(String userId) {
        if (userId == null || userId.isEmpty()) return null;
        String stored = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT password FROM " + DatabaseContract.Users.TABLE_NAME + " WHERE user_id = ?";
            cursor = db.rawQuery(sql, new String[]{userId});
            if (cursor.moveToFirst()) {
                stored = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Users.COLUMN_PASSWORD));
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return stored;
    }

    public Boolean updateUser(String id, String name, String gmail) {
        if (id == null || id.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseContract.Users.COLUMN_USERNAME, name);
        contentValues.put(DatabaseContract.Users.COLUMN_EMAIL, gmail);
        long result = db.update(DatabaseContract.Users.TABLE_NAME, contentValues, "user_id = ?", new String[]{id});
        db.close();
        return result != -1;
    }

    public void checkUserForUpdate(String userId, String username, String email, UserCheckCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                boolean usernameExists = checkFieldExistsForUpdate(db, DatabaseContract.Users.COLUMN_USERNAME, username, userId);
                boolean emailExists = checkFieldExistsForUpdate(db, DatabaseContract.Users.COLUMN_EMAIL, email, userId);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (usernameExists) {
                        callback.onUsernameExists();
                    } else if (emailExists) {
                        callback.onEmailExists();
                    } else {
                        callback.onAvailable();
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Lỗi hệ thống: " + e.getMessage()));
            }
        });
    }

    public void addGoogleUserAsync(String userID, String username, String email, String avatarUrl, UserCallback callback) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseContract.Users.COLUMN_USER_ID, userID);
                values.put(DatabaseContract.Users.COLUMN_USERNAME, username);
                values.put(DatabaseContract.Users.COLUMN_EMAIL, email);
                values.put(DatabaseContract.Users.COLUMN_PASSWORD, "");
                values.put(DatabaseContract.Users.COLUMN_AVATAR_URL, avatarUrl);

                long result = db.insert(DatabaseContract.Users.TABLE_NAME, null, values);

                if (result != -1) {
                    insertDefaultCategories(db, userID);
                }

                db.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result != -1) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Failed to insert user");
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private void insertDefaultCategories(SQLiteDatabase db, String userId) {
        String[][] defaultExpenses = {
            {"Ăn uống", "ic_outline_restaurant", "#FF9800"},
            {"Chi tiêu hằng ngày", "ic_outline_market", "#4CAF50"},
            {"Quần áo", "ic_outline_clothes", "#2196F3"},
            {"Mỹ phẩm", "ic_outline_florist", "#E91E63"},
            {"Phí giao lưu", "ic_outline_cafe", "#FFC107"},
            {"Y tế", "ic_outline_hospital", "#4CAF50"},
            {"Giáo dục", "ic_outline_school", "#F44336"},
            {"Tiền điện", "ic_outline_device", "#03A9F4"},
            {"Đi lại", "ic_outline_car", "#795548"},
            {"Phí liên lạc", "ic_outline_smartphone", "#607D8B"},
            {"Tiền nhà", "ic_outline_home", "#FF5722"}
        };

        for (String[] cat : defaultExpenses) {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.Categories.COLUMN_CATEGORY_ID, UUID.randomUUID().toString());
            cv.put(DatabaseContract.Categories.COLUMN_NAME, cat[0]);
            cv.put(DatabaseContract.Categories.COLUMN_ICON, cat[1]);
            cv.put(DatabaseContract.Categories.COLUMN_COLOR, cat[2]);
            cv.put(DatabaseContract.Categories.COLUMN_TYPE, "expense");
            cv.put(DatabaseContract.Categories.COLUMN_USER_ID, userId);
            db.insert(DatabaseContract.Categories.TABLE_NAME, null, cv);
        }

        // Danh sách danh mục thu nhập mặc định
        String[][] defaultIncomeCategories = {
            {"Tiền lương", "ic_outline_wallet", "#4CAF50"},
            {"Tiền thưởng", "ic_outline_gift", "#FF9800"}
        };
        for (String[] cat : defaultIncomeCategories) {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.Categories.COLUMN_CATEGORY_ID, UUID.randomUUID().toString());
            cv.put(DatabaseContract.Categories.COLUMN_NAME, cat[0]);
            cv.put(DatabaseContract.Categories.COLUMN_ICON, cat[1]);
            cv.put(DatabaseContract.Categories.COLUMN_COLOR, cat[2]);
            cv.put(DatabaseContract.Categories.COLUMN_TYPE, "income");
            cv.put(DatabaseContract.Categories.COLUMN_USER_ID, userId);
            db.insert(DatabaseContract.Categories.TABLE_NAME, null, cv);
        }
    }

    //hàm lấy tất cả budget theo user - Quỳnh Trang
    public List<Budgets> getAllBudgets(String userId){
        SQLiteDatabase db = this.getReadableDatabase();
        List<Budgets> budgets = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT budget_id, description FROM Budgets WHERE user_id = ?", new String[]{userId});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                budgets.add(new Budgets(cursor.getString(0), cursor.getString(1)));
            }
            cursor.close();
        }
        return budgets;
    }

    public List<Categories> getAllCategories(String userId, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Categories> categories = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT category_id, name FROM Categories WHERE user_id = ? AND type = ?",
                new String[]{userId, type}
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                categories.add(new Categories(cursor.getString(0), cursor.getString(1)));
            }
            cursor.close();
        }
        return categories;
    }

    public List<Incomes> getAllIncomesById(String Id) {
        List<Incomes> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Incomes WHERE user_id = ?", new String[]{Id});
        while (cursor != null && cursor.moveToNext()) {
            list.add(new Incomes(
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_INCOME_ID)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_CREATE_AT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Incomes.COLUMN_CATEGORY_ID))
            ));
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public List<FixedTransaction> getFixedTransactions(String userId) {
        List<FixedTransaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT f.id, f.user_id, f.type, f.amount, f.category_id, c.name, c.icon, c.color, f.description, f.frequency, f.start_date, f.end_date " +
                       "FROM " + DatabaseContract.FixedTransactions.TABLE_NAME + " f " +
                       "LEFT JOIN " + DatabaseContract.Categories.TABLE_NAME + " c ON f.category_id = c.category_id " +
                       "WHERE f.user_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        while (cursor != null && cursor.moveToNext()) {
            list.add(new FixedTransaction(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8),
                    cursor.getString(9),
                    cursor.getString(10),
                    cursor.getString(11)
            ));
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public FixedTransaction getFixedTransactionById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT f.id, f.user_id, f.type, f.amount, f.category_id, c.name, c.icon, c.color, f.description, f.frequency, f.start_date, f.end_date " +
                       "FROM " + DatabaseContract.FixedTransactions.TABLE_NAME + " f " +
                       "LEFT JOIN " + DatabaseContract.Categories.TABLE_NAME + " c ON f.category_id = c.category_id " +
                       "WHERE f.id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{id});
        FixedTransaction transaction = null;
        if (cursor != null && cursor.moveToFirst()) {
            transaction = new FixedTransaction(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8),
                    cursor.getString(9),
                    cursor.getString(10),
                    cursor.getString(11)
            );
        }
        if (cursor != null) cursor.close();
        return transaction;
    }

    public void deleteFixedTransaction(String transactionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DatabaseContract.Expenses.TABLE_NAME, DatabaseContract.Expenses.COLUMN_FIXED_ID + "=?", new String[]{transactionId});
        db.delete(DatabaseContract.Incomes.TABLE_NAME, DatabaseContract.Incomes.COLUMN_FIXED_ID + "=?", new String[]{transactionId});
        db.delete(DatabaseContract.FixedTransactions.TABLE_NAME, DatabaseContract.FixedTransactions.COLUMN_ID + "=?", new String[]{transactionId});
        db.close();
    }
}
