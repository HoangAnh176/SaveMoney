package com.example.n03_quanlychitieu.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.n03_quanlychitieu.R;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoryDetailAdapter extends RecyclerView.Adapter<CategoryDetailAdapter.ViewHolder> {
    private List<DetailItem> list;
    private Context context;
    private boolean isYearlyMode;
    private String colorStr;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onMonthItemClick(int month);
        void onTransactionClick(DetailItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public CategoryDetailAdapter(Context context, List<DetailItem> list, boolean isYearlyMode, String colorStr) {
        this.context = context;
        this.list = list;
        this.isYearlyMode = isYearlyMode;
        this.colorStr = colorStr;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetailItem item = list.get(position);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        if (isYearlyMode) {
            holder.llMonthItem.setVisibility(View.VISIBLE);
            holder.llDayItem.setVisibility(View.GONE);
            holder.tvMonthTitle.setText("Tháng " + item.monthOrDayStr);
            holder.tvMonthAmount.setText(formatter.format(item.amount));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    try {
                        listener.onMonthItemClick(Integer.parseInt(item.monthOrDayStr));
                    } catch (Exception e) {}
                }
            });
        } else {
            holder.llMonthItem.setVisibility(View.GONE);
            holder.llDayItem.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null);
            holder.tvCatName.setText(item.catName);

            // Format date
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.dateStr);
                String formattedDate = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(d);
                holder.tvDate.setText(formattedDate);
            } catch (Exception e) {
                holder.tvDate.setText(item.dateStr);
            }

            holder.tvDayAmount.setText(formatter.format(item.amount));

            try {
                holder.ivCatIcon.setColorFilter(Color.parseColor(colorStr));
            } catch (Exception e) {}

            if (item.icon != null && !item.icon.isEmpty()) {
                int resId = context.getResources().getIdentifier(item.icon, "drawable", context.getPackageName());
                if (resId != 0) {
                    holder.ivCatIcon.setImageResource(resId);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llMonthItem, llDayItem;
        TextView tvMonthTitle, tvMonthAmount;
        ImageView ivCatIcon;
        TextView tvCatName, tvDate, tvDayAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llMonthItem = itemView.findViewById(R.id.ll_month_item);
            llDayItem = itemView.findViewById(R.id.ll_day_item);
            tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
            tvMonthAmount = itemView.findViewById(R.id.tv_month_amount);
            ivCatIcon = itemView.findViewById(R.id.iv_cat_icon);
            tvCatName = itemView.findViewById(R.id.tv_cat_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDayAmount = itemView.findViewById(R.id.tv_day_amount);
        }
    }

    public static class DetailItem {
        public String monthOrDayStr;
        public String dateStr;
        public double amount;
        public String catName;
        public String icon;
        public String id;
        public String desc;
        public String catId;
        public String rawDate;
        public String fixedId;

        // Yearly
        public DetailItem(String dateStr, double amount) {
            this.monthOrDayStr = dateStr;
            this.dateStr = dateStr;
            this.amount = amount;
        }

        // Monthly (Daily) with extra details
        public DetailItem(String dateStr, double amount, String catName, String icon, String id, String desc, String catId, String rawDate, String fixedId) {
            this.dateStr = dateStr;
            this.amount = amount;
            this.catName = catName;
            this.icon = icon;
            this.id = id;
            this.desc = desc;
            this.catId = catId;
            this.rawDate = rawDate;
            this.fixedId = fixedId;
        }
    }
}
