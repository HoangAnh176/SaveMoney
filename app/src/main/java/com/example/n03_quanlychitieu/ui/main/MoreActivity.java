package com.example.n03_quanlychitieu.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.ui.user.UserProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MoreActivity extends AppCompatActivity {
    private String userId;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            com.example.n03_quanlychitieu.utils.AuthenticationManager auth = com.example.n03_quanlychitieu.utils.AuthenticationManager.getInstance(this);
            if (auth.getCurrentUser() != null) {
                userId = auth.getCurrentUser().getUser_id();
            }
        }

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_more);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_input) {
                startActivity(new Intent(MoreActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(MoreActivity.this, CalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(MoreActivity.this, com.example.n03_quanlychitieu.ui.sign.ReportTransaction.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_budget) {
                startActivity(new Intent(MoreActivity.this, com.example.n03_quanlychitieu.ui.budget.BudgetActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_more) {
                return true;
            }
            return false;
        });

        View btnPersonalPage = findViewById(R.id.btn_personal_page);
        btnPersonalPage.setOnClickListener(v -> {
            Intent intent = new Intent(MoreActivity.this, UserProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        View btnFixedTransaction = findViewById(R.id.btn_fixed_transaction);
        btnFixedTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(MoreActivity.this, FixedTransactionsListActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }
}
