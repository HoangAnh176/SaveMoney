package com.example.n03_quanlychitieu.ui.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.n03_quanlychitieu.R;
import com.example.n03_quanlychitieu.model.FixedTransaction;

import java.text.DecimalFormat;
import java.util.List;

public class FixedTransactionAdapter extends RecyclerView.Adapter<FixedTransactionAdapter.ViewHolder> {

    private List<FixedTransaction> list;
    private Context context;
    private DecimalFormat df = new DecimalFormat("#,###đ");

    public FixedTransactionAdapter(Context context, List<FixedTransaction> list) {
        this.context = context;
        this.list = list;
    }

    public void setList(List<FixedTransaction> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    // I need notifyDataSetChanged method
    public void updateData(List<FixedTransaction> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fixed_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FixedTransaction transaction = list.get(position);

        holder.tvTitle.setText(transaction.getDescription() != null && !transaction.getDescription().isEmpty() ? transaction.getDescription() : "Chưa đặt tên");
        holder.tvDesc.setText((transaction.getCategoryName() != null ? transaction.getCategoryName() : "Không có danh mục") + " · " + transaction.getFrequency());
        holder.tvAmount.setText(df.format(transaction.getAmount()));

        if ("income".equals(transaction.getType())) {
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#F44336")); // Red
        }

        try {
            if (transaction.getCategoryIcon() != null) {
                int iconResId = context.getResources().getIdentifier(transaction.getCategoryIcon(), "drawable", context.getPackageName());
                if (iconResId != 0) {
                    holder.ivIcon.setImageResource(iconResId);
                }
            }
            if (transaction.getCategoryColor() != null) {
                holder.ivIcon.setColorFilter(Color.parseColor(transaction.getCategoryColor()));
            }
        } catch (Exception e) {}

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FixedTransactionActivity.class);
            intent.putExtra("userId", transaction.getUserId());
            intent.putExtra("transactionId", transaction.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvAmount;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            ivIcon = itemView.findViewById(R.id.iv_cat_icon);
        }
    }
}

