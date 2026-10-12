package com.hmaidi.budgetflow;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SavingsGoalAdapter extends RecyclerView.Adapter<SavingsGoalAdapter.GoalViewHolder> {

    public interface OnGoalClickListener {
        void onGoalClick(SavingsGoal goal);
    }

    private Context context;
    private List<SavingsGoal> goalList;
    private OnGoalClickListener listener;

    public SavingsGoalAdapter(Context context, List<SavingsGoal> goalList, OnGoalClickListener listener) {
        this.context = context;
        this.goalList = goalList;
        this.listener = listener;
    }

    public void updateList(List<SavingsGoal> newList) {
        this.goalList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_savings_goal, parent, false);
        return new GoalViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        SavingsGoal goal = goalList.get(position);
        holder.tvTitle.setText(goal.getTitle());

        int percent = goal.getTargetAmount() > 0
                ? (int) Math.round((goal.getSavedAmount() / goal.getTargetAmount()) * 100)
                : 0;

        holder.tvPercent.setText(percent + "%");
        holder.pbGoal.setProgress(Math.min(100, Math.max(0, percent)));

        holder.tvAmountInfo.setText(String.format("%s / %s",
                CurrencyHelper.format(context, goal.getSavedAmount()),
                CurrencyHelper.format(context, goal.getTargetAmount())));

        if (goal.getColor() != null && !goal.getColor().isEmpty()) {
            try {
                holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(goal.getColor())));
            } catch (Exception ignored) {}
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGoalClick(goal);
        });
    }

    @Override
    public int getItemCount() {
        return goalList != null ? goalList.size() : 0;
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvPercent, tvTitle, tvAmountInfo;
        ProgressBar pbGoal;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_goal_icon);
            tvPercent = itemView.findViewById(R.id.tv_goal_percent);
            tvTitle = itemView.findViewById(R.id.tv_goal_title);
            tvAmountInfo = itemView.findViewById(R.id.tv_goal_amount_info);
            pbGoal = itemView.findViewById(R.id.pb_goal);
        }
    }
}
