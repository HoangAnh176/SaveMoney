package com.example.n03_quanlychitieu.ui.category;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.dao.CategoryDAO;
import com.example.n03_quanlychitieu.db.DatabaseHelper;
import com.example.n03_quanlychitieu.model.Categories;
import com.example.n03_quanlychitieu.ui.main.MainActivity;
import com.example.n03_quanlychitieu.utils.AuthenticationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddCategoryActivity extends AppCompatActivity implements EditCategoryActivity.OnCategoryUpdatedListener {
    private DatabaseHelper db  = new DatabaseHelper(this);
    private RecyclerView.Adapter<MyViewHolder> adapter;
    private RecyclerView recyclerView;
    private CategoryDAO categoryDAO;

    private List<Categories> list = new ArrayList<>(); // Khởi tạo danh sách rỗng
    private List<Categories> filteredList = new ArrayList<>(); // Khởi tạo danh sách rỗng
    private ImageButton btnThem, btnQuayLai;

    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        userId = AuthenticationManager.getInstance(this).getCurrentUser().getUser_id();

        categoryDAO = new CategoryDAO(db.getWritableDatabase());
        khoitao();
//        addSampleData();
        recyclerViewHandle();
        btnThemSuaClick();

        String typeToShow = "expense";
        if (getIntent() != null && getIntent().hasExtra("type")) {
            typeToShow = getIntent().getStringExtra("type");
        }

        if (typeToShow.equals("income")) {
            TextView title = findViewById(R.id.textView);
            if(title != null) title.setText("Danh Mục Thu");
            filterCategories("income");
        } else {
             TextView title = findViewById(R.id.textView);
            if(title != null) title.setText("Danh Mục Chi");
            filterCategories("expense");
        }

    }
    public void khoitao(){
        recyclerView = findViewById(R.id.recyclerView);
        btnThem = findViewById(R.id.btnThem);
        btnQuayLai = findViewById(R.id.btnQuayLai);
    }
    private void filterCategories(String type) {
        if (filteredList == null) {
            filteredList = new ArrayList<>();
        }
        filteredList.clear();

        if (list != null && !list.isEmpty()) {
            for (Categories c : list) {
                if (c.getType() != null && c.getType().equals(type)) {
                    filteredList.add(c);
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    private void addSampleData() {
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("INSERT INTO Categories (category_id, name, icon, color, type) VALUES ('1', 'Danh mục 1', 'icon1', '#FF0000', 'income')");
        database.execSQL("INSERT INTO Categories (category_id, name, icon, color, type) VALUES ('2', 'Danh mục 2', 'icon2', '#00FF00', 'expense')");
    }
    public void btnThemSuaClick(){
        btnThem.setOnClickListener(view -> {
            String currentType = "expense";
            if (getIntent() != null && getIntent().hasExtra("type")) {
                currentType = getIntent().getStringExtra("type");
            }
            EditCategoryActivity dialog = EditCategoryActivity.newInstance(null, "","",currentType,"");
            dialog.show(getSupportFragmentManager(), "EditCategoryActivity");
        });
        btnQuayLai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    public void recyclerViewHandle() {
        list = categoryDAO.getAllCategories(userId); // Lấy danh sách từ cơ sở dữ liệu
        if (list == null) list = new ArrayList<>();

        filteredList.clear();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<MyViewHolder>() {
            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
                return new MyViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
                Categories c = filteredList.get(position);
                if (c != null) {
                    int iconResId = getResources().getIdentifier(c.getIcon(), "drawable", getPackageName());
                    if (iconResId != 0) {
                        android.graphics.drawable.Drawable iconDrawable = getResources().getDrawable(iconResId, null);
                        if (iconDrawable != null) {
                            int sizeInPx = (int) (48 * getResources().getDisplayMetrics().density);
                            iconDrawable.setBounds(0, 0, sizeInPx, sizeInPx);
                            holder.txtNguonTien.setCompoundDrawables(iconDrawable, null, null, null);
                            holder.txtNguonTien.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
                        }
                    } else {
                        holder.txtNguonTien.setCompoundDrawables(null, null, null, null);
                    }

                    try {
                        holder.txtNguonTien.setTextColor(android.graphics.Color.parseColor(c.getColor()));
                    } catch (Exception e) {
                        holder.txtNguonTien.setTextColor(android.graphics.Color.BLACK);
                    }

                    holder.txtNguonTien.setText(c.getName());
                    holder.btnSua.setOnClickListener(view -> {
                        EditCategoryActivity dialog = EditCategoryActivity.newInstance(c.getCategory_id(), c.getName(), c.getColor(), c.getType(), c.getIcon());
                        dialog.show(((AppCompatActivity) view.getContext()).getSupportFragmentManager(), "EditCategoryActivity");
                    });
                    holder.btnXoa.setOnClickListener(view -> {
                        new AlertDialog.Builder(view.getContext())
                                .setTitle("Xác nhận xóa")
                                .setMessage("Bạn có chắc chắn muốn xóa danh mục này không?")
                                .setPositiveButton("Xóa", (dialog, which) -> {
                                    categoryDAO.delete(c.getCategory_id());
                                    list = categoryDAO.getAllCategories(userId);
                                    if (list == null) list = new ArrayList<>();
                                    String currentType = "expense";
                                    if (getIntent() != null && getIntent().hasExtra("type")) {
                                        currentType = getIntent().getStringExtra("type");
                                    }
                                    filterCategories(currentType);
                                    Toast.makeText(view.getContext(), "Xóa danh mục thành công", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    });
                } else {
                    holder.txtNguonTien.setText("Không có dữ liệu");
                }
            }

            @Override
            public int getItemCount() {
                return filteredList.size();
            }
        };
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onCategorySaved(String id, String name, String color, String type, String icon) {
        if (id == null) {
            Categories c = new Categories(UUID.randomUUID().toString(), name, icon, color, type, userId);
            c.setUser_id(userId); // Gán user_id
            categoryDAO.insert(c);
        } else {
            Categories c = new Categories(id, name, icon, color, type, userId);
            c.setUser_id(userId); // Gán user_id
            categoryDAO.update(c);
        }

        list = categoryDAO.getAllCategories(userId); // Lấy danh mục theo user_id
        if (list == null || list.isEmpty()) {
            Toast.makeText(this, "Không có danh mục", Toast.LENGTH_SHORT).show();
        } else {
            String currentType = "expense";
            if (getIntent() != null && getIntent().hasExtra("type")) {
                currentType = getIntent().getStringExtra("type");
            }
            filterCategories(currentType);
        }
        adapter.notifyDataSetChanged();
    }
    
    class MyViewHolder extends RecyclerView.ViewHolder{
        TextView txtNguonTien;
        ImageButton btnSua, btnXoa;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNguonTien = itemView.findViewById(R.id.txtNguonTien);
            btnSua = itemView.findViewById(R.id.btnSua);
            btnXoa = itemView.findViewById(R.id.btnXoa);
        }
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
