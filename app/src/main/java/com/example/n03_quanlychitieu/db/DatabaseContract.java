package com.example.n03_quanlychitieu.db;

public class DatabaseContract {
    private DatabaseContract() {}
    public static class Database {
        public static final String DATABASE_NAME = "fin_manager.db";
        public static final int DATABASE_VERSION = 6;
    }
    public static class Users {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_AVATAR_URL = "avatar_url";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_PASSWORD + " TEXT NOT NULL, " +
                        COLUMN_AVATAR_URL + " TEXT, " +
                        COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
    }
    public static class Categories {
        public static final String TABLE_NAME = "categories";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ICON = "icon";
        public static final String COLUMN_COLOR = "color";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_CATEGORY_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_NAME + " TEXT NOT NULL, " +
                        COLUMN_ICON + " TEXT, " +
                        COLUMN_COLOR + " TEXT, " +
                        COLUMN_TYPE + " TEXT NOT NULL CHECK(" + COLUMN_TYPE + " IN ('income','expense'))," +
                        COLUMN_USER_ID + " TEXT NOT NULL, " +
                        "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " +
                        Users.TABLE_NAME + "(" + Users.COLUMN_USER_ID + ") ON DELETE CASCADE" +
                        ")";
    }
    public static class Notifications {
        public static final String TABLE_NAME = "notifications";
        public static final String COLUMN_NOTIFICATION_ID = "notification_id";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_IS_READ = "is_read";
        public static final String COLUMN_CREATE_AT = "create_at";
        public static final String COLUMN_NOTIFICATION_TYPE = "notification_type";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_NOTIFICATION_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_CONTENT + " TEXT NOT NULL, " +
                        COLUMN_IS_READ + " BOOLEAN NOT NULL DEFAULT 0, " +
                        COLUMN_CREATE_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        COLUMN_NOTIFICATION_TYPE + " TEXT NOT NULL CHECK(" + COLUMN_NOTIFICATION_TYPE + " IN ('warn','info')), " +
                        COLUMN_USER_ID + " TEXT NOT NULL, " +
                        "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " +
                        Users.TABLE_NAME + "(" + Users.COLUMN_USER_ID + ") ON DELETE CASCADE" +
                        ")";
    }
    public static class Budgets {
        public static final String TABLE_NAME = "budgets";
        public static final String COLUMN_BUDGET_ID = "budget_id";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_START_DATE = "start_date";
        public static final String COLUMN_END_DATE = "end_date";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_WARNING_THRESHOLD = "warning_threshold";
        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_BUDGET_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_AMOUNT + " REAL NOT NULL, " +
                        COLUMN_START_DATE + " DATE NOT NULL, " +
                        COLUMN_END_DATE + " DATE NOT NULL, " +
                        COLUMN_DESCRIPTION + " TEXT, " +
                        COLUMN_USER_ID + " TEXT NOT NULL, " +
                        COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +
                        COLUMN_WARNING_THRESHOLD + " INTEGER DEFAULT 90, " +
                        "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " +
                        Users.TABLE_NAME + "(" + Users.COLUMN_USER_ID + ") ON DELETE CASCADE, " +
                        "FOREIGN KEY (" + COLUMN_CATEGORY_ID + ") REFERENCES " +
                        Categories.TABLE_NAME + "(" + Categories.COLUMN_CATEGORY_ID + ") ON DELETE CASCADE" +
                        ")";
    }
    public static class Incomes {
        public static final String TABLE_NAME = "incomes";
        public static final String COLUMN_INCOME_ID = "income_id";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_CREATE_AT = "create_at";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_INCOME_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_AMOUNT + " REAL NOT NULL, " +
                        COLUMN_DESCRIPTION + " TEXT, " +
                        COLUMN_CREATE_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        COLUMN_USER_ID + " TEXT NOT NULL, " +
                        COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +
                        "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " +
                        Users.TABLE_NAME + "(" + Users.COLUMN_USER_ID + ") ON DELETE CASCADE, " +
                        "FOREIGN KEY (" + COLUMN_CATEGORY_ID + ") REFERENCES " +
                        Categories.TABLE_NAME + "(" + Categories.COLUMN_CATEGORY_ID + ") ON DELETE CASCADE" +
                        ")";
    }
    public static class Expenses {
        public static final String TABLE_NAME = "expenses";
        public static final String COLUMN_EXPENSE_ID = "expense_id";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_CREATE_AT = "create_at";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_BUDGET_ID = "budget_id";
        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_EXPENSE_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_AMOUNT + " REAL NOT NULL, " +
                        COLUMN_DESCRIPTION + " TEXT, " +
                        COLUMN_CREATE_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        COLUMN_USER_ID + " TEXT NOT NULL, " +
                        COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +
                        COLUMN_BUDGET_ID + " TEXT, " +
                        "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " +
                        Users.TABLE_NAME + "(" + Users.COLUMN_USER_ID + ") ON DELETE CASCADE, " +
                        "FOREIGN KEY (" + COLUMN_CATEGORY_ID + ") REFERENCES " +
                        Categories.TABLE_NAME + "(" + Categories.COLUMN_CATEGORY_ID + ") ON DELETE CASCADE, " +
                        "FOREIGN KEY (" + COLUMN_BUDGET_ID + ") REFERENCES " +
                        Budgets.TABLE_NAME + "(" + Budgets.COLUMN_BUDGET_ID + ") ON DELETE SET NULL" +
                        ")";
    }
}
