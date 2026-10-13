package com.hmaidi.budgetflow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnSwipeListener {
        void onSwipedToDelete(int position);
    }

    private final OnSwipeListener listener;
    private final GradientDrawable background;
    private final Drawable deleteIcon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;

    public SwipeToDeleteCallback(Context context, OnSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener = listener;

        background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(context, R.color.expense_red));
        float radius = 16f * context.getResources().getDisplayMetrics().density;
        background.setCornerRadius(radius);

        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        if (deleteIcon != null) {
            deleteIcon.setTint(Color.WHITE);
            int defaultSize = (int) (24 * context.getResources().getDisplayMetrics().density);
            intrinsicWidth = deleteIcon.getIntrinsicWidth() > 0 ? deleteIcon.getIntrinsicWidth() : defaultSize;
            intrinsicHeight = deleteIcon.getIntrinsicHeight() > 0 ? deleteIcon.getIntrinsicHeight() : defaultSize;
        } else {
            intrinsicWidth = 0;
            intrinsicHeight = 0;
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && listener != null) {
            listener.onSwipedToDelete(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        if (dX < 0) { // Swiping left
            int iconMargin = (itemView.getHeight() - intrinsicHeight) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + intrinsicHeight;
            int iconRight = itemView.getRight() - (int) (16 * itemView.getResources().getDisplayMetrics().density);
            int iconLeft = iconRight - intrinsicWidth;

            background.setBounds(
                    (int) (itemView.getRight() + dX),
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom()
            );
            background.draw(c);

            if (deleteIcon != null && Math.abs(dX) > (itemView.getRight() - iconLeft)) {
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
