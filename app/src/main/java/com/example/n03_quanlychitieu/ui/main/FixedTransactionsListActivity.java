package com.example.n03_quanlychitieu.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.FixedTransaction;

import java.util.List;

public class FixedTransactionsListActivity extends AppCompatActivity {
    private String userId;
    private DatabaseHelper dbHelper;
    private RecyclerView rvList;
    private FixedTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixed_transactions_list);

        userId = getIntent().getStringExtra("userId");
        dbHelper = new DatabaseHelper(this);

        // Đã sửa: Chuyển TextView thành LinearLayout và ImageView để khớp với XML
        LinearLayout btnBack = findViewById(R.id.btn_back_list);
        ImageView btnAdd = findViewById(R.id.btn_add_fixed);
        rvList = findViewById(R.id.rv_fixed_transactions);

        rvList.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(FixedTransactionsListActivity.this, FixedTransactionActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (userId != null) {
            List<FixedTransaction> list = dbHelper.getFixedTransactions(userId);
            if (adapter == null) {
                adapter = new FixedTransactionAdapter(this, list);
                rvList.setAdapter(adapter);
            } else {
                adapter.updateData(list);
            }
        }
    }
}