package com.example.n03_quanlychitieu.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.n03_quanlychitieu.db.DatabaseContract;
import com.example.n03_quanlychitieu.model.Budgets;

import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {
    private SQLiteDatabase db;

    public BudgetDAO(SQLiteDatabase db) {
        this.db = db;
    }

    // CHuyển đổi Budget thành ContentValues
    private ContentValues toContentValues(Budgets budget) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Budgets.COLUMN_BUDGET_ID, budget.getBudget_id());
        values.put(DatabaseContract.Budgets.COLUMN_AMOUNT, budget.getAmount());
        values.put(DatabaseContract.Budgets.COLUMN_START_DATE, budget.getStart_date());
        values.put(DatabaseContract.Budgets.COLUMN_END_DATE, budget.getEnd_date());
        values.put(DatabaseContract.Budgets.COLUMN_DESCRIPTION, budget.getDescription());
        values.put(DatabaseContract.Budgets.COLUMN_USER_ID, budget.getUser_id());
        values.put(DatabaseContract.Budgets.COLUMN_CATEGORY_ID, budget.getCategory_id());
        values.put(DatabaseContract.Budgets.COLUMN_WARNING_THRESHOLD, budget.getWarning_threshold());
        return values;
    }

    // Them giới hạn chi tiêu
    public long insert(Budgets budget) {
        if (budget == null) {
            return -1;
        }
        ContentValues values = toContentValues(budget);
        return db.insert(DatabaseContract.Budgets.TABLE_NAME, null, values);
    }

    // Cap nhat gioi han chi tieu
    public int update(Budgets budget) {
        if (budget == null || budget.getBudget_id() == null) {
            return -1;
        }
        ContentValues values = toContentValues(budget);
        String selection = DatabaseContract.Budgets.COLUMN_BUDGET_ID + " = ?";
        String[] selectionArgs = {budget.getBudget_id()};
        return db.update(DatabaseContract.Budgets.TABLE_NAME, values, selection, selectionArgs);
    }

    // Xóa giới hạn chi tiêu
    public int delete(String budgetId) {
        if (budgetId == null) {
            return -1;
        }
        String selection = DatabaseContract.Budgets.COLUMN_BUDGET_ID + " = ?";
        String[] selectionArgs = {budgetId};
        return db.delete(DatabaseContract.Budgets.TABLE_NAME, selection, selectionArgs);
    }

    // Lấy tất cả giới hạn chi tiêu của người dùng
    public List<Budgets> getBudgetsByUser(String userId) {
        List<Budgets> budgets = new ArrayList<>();
        String selection = DatabaseContract.Budgets.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {userId};
        String orderBy = DatabaseContract.Budgets.COLUMN_START_DATE + " DESC";

        Cursor cursor = db.query(
                DatabaseContract.Budgets.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );
        if (cursor.moveToFirst()) {
            do {
                Budgets budget = new Budgets(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_BUDGET_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_START_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_END_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_CATEGORY_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_WARNING_THRESHOLD))
                );
                budgets.add(budget);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return budgets;
    }

    // Lấy giới hạn chi tiêu theo ID
    public Budgets getBudgetById(String budgetId) {
        String selection = DatabaseContract.Budgets.COLUMN_BUDGET_ID + " = ?";
        String[] selectionArgs = {budgetId};

        Cursor cursor = db.query(
                DatabaseContract.Budgets.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {

            Budgets budget = new Budgets(
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_BUDGET_ID)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_START_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_END_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_CATEGORY_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_WARNING_THRESHOLD))
            );
            cursor.close();
            return budget;
        }
        cursor.close();
        return null;
    }

    // Lấy giới hạn chi tiêu theo danh mục
    public List<Budgets> getBudgetsByCategory(String categoryId, String userId) {
        List<Budgets> budgets = new ArrayList<>();
        String selection = DatabaseContract.Budgets.COLUMN_CATEGORY_ID + " = ? AND " + DatabaseContract.Budgets.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {categoryId, userId};

        Cursor cursor = db.query(
                DatabaseContract.Budgets.TABLE_NAME,
                null,

                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                Budgets budget = new Budgets(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_BUDGET_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_START_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_END_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_CATEGORY_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Budgets.COLUMN_WARNING_THRESHOLD))
                );
                budgets.add(budget);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return budgets;
    }

    // Kiểm tra giới hạn chi tiêu có tồn tại không
    public boolean isBudgetExists(String budgetId) {
        String selection = DatabaseContract.Budgets.COLUMN_BUDGET_ID + " = ?";
        String[] selectionArgs = {budgetId};

        Cursor cursor = db.query(
                DatabaseContract.Budgets.TABLE_NAME,
                new String[]{DatabaseContract.Budgets.COLUMN_BUDGET_ID},
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1" // Giới hạn chỉ 1 bản ghi
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // Tính tổng tiền đã chi tiêu so với giới hạn
    public double getTotalSpentForBudget(String budgetId) {
        Budgets budget = getBudgetById(budgetId);
        if (budget == null) return 0;

        String categoryCondition = "";
        String[] selectionArgs;

        if ("none".equals(budget.getCategory_id())) {
            // Ngân sách tổng: lấy tất cả các khoản chi chưa gán ngân sách con hoặc gán vào chính nó
            categoryCondition = "1=1"; // Có thể filter thêm nếu cần
            selectionArgs = new String[]{budget.getUser_id(), budget.getStart_date(), budget.getEnd_date()};
        } else {
            categoryCondition = DatabaseContract.Expenses.COLUMN_CATEGORY_ID + " = ?";
            selectionArgs = new String[]{budget.getUser_id(), budget.getCategory_id(), budget.getStart_date(), budget.getEnd_date()};
        }

        String query = "SELECT SUM(" + DatabaseContract.Expenses.COLUMN_AMOUNT + ") " +
                "FROM " + DatabaseContract.Expenses.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.Expenses.COLUMN_USER_ID + " = ? AND " +
                categoryCondition + " AND " +
                "substr(" + DatabaseContract.Expenses.COLUMN_CREATE_AT + ", 1, 10) BETWEEN ? AND ?";

        Cursor cursor = db.rawQuery(query, selectionArgs);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

}
