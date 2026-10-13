package com.hmaidi.budgetflow;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ReceiptHelper {

    public static File getReceiptsDirectory(Context context) {
        File dir = new File(context.getFilesDir(), "receipts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File createTempReceiptFile(Context context) {
        File dir = getReceiptsDirectory(context);
        return new File(dir, "receipt_" + System.currentTimeMillis() + ".jpg");
    }

    public static Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }

    public static String copyImageUriToInternalReceipt(Context context, Uri sourceUri) {
        if (context == null || sourceUri == null) return null;
        try {
            // First decode bounds
            InputStream isBounds = context.getContentResolver().openInputStream(sourceUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(isBounds, null, options);
            if (isBounds != null) isBounds.close();

            int maxDimension = 2048;
            int sampleSize = 1;
            while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
                sampleSize *= 2;
            }

            // Decode scaled bitmap
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            InputStream isDecode = context.getContentResolver().openInputStream(sourceUri);
            Bitmap bitmap = BitmapFactory.decodeStream(isDecode, null, decodeOptions);
            if (isDecode != null) isDecode.close();

            if (bitmap == null) return null;

            File destFile = createTempReceiptFile(context);
            FileOutputStream fos = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteReceiptFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) return false;
        try {
            File f = new File(filePath);
            if (f.exists()) {
                return f.delete();
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void loadReceiptIntoView(String filePath, ImageView targetView) {
        if (filePath == null || targetView == null) return;
        try {
            File f = new File(filePath);
            if (!f.exists()) return;

            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), boundsOptions);

            int targetDim = 600;
            int sampleSize = 1;
            while ((boundsOptions.outWidth / sampleSize) > targetDim || (boundsOptions.outHeight / sampleSize) > targetDim) {
                sampleSize *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            if (bmp != null) {
                targetView.setImageBitmap(bmp);
            }
        } catch (Exception ignored) {}
    }

    public static void showFullscreenReceipt(Context context, String filePath, String title) {
        if (context == null || filePath == null) return;
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(context, "Receipt photo file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.argb(230, 10, 10, 15)));
        }

        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOrientation(LinearLayout.VERTICAL);

        // Header toolbar
        LinearLayout header = new LinearLayout(context);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (64 * density)));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) (16 * density);
        header.setPadding(pad, 0, pad, 0);

        // Close button
        TextView btnClose = new TextView(context);
        btnClose.setText("✕");
        btnClose.setTextSize(22);
        btnClose.setTextColor(Color.WHITE);
        btnClose.setPadding((int) (8 * density), (int) (8 * density), (int) (8 * density), (int) (8 * density));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        header.addView(btnClose);

        // Title
        TextView tvTitle = new TextView(context);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart((int) (12 * density));
        tvTitle.setLayoutParams(titleParams);
        tvTitle.setText(title != null && !title.isEmpty() ? title + " Receipt" : "Receipt Photo");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(tvTitle);

        // Share button
        TextView btnShare = new TextView(context);
        btnShare.setText("Share");
        btnShare.setTextSize(14);
        btnShare.setTextColor(ContextCompat.getColor(context, R.color.purple_light));
        btnShare.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnShare.setPadding((int) (12 * density), (int) (8 * density), (int) (8 * density), (int) (8 * density));
        btnShare.setOnClickListener(v -> {
            try {
                Uri contentUri = getFileUri(context, file);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
            } catch (Exception e) {
                Toast.makeText(context, "Failed to share receipt", Toast.LENGTH_SHORT).show();
            }
        });
        header.addView(btnShare);

        root.addView(header);

        // Image container
        FrameLayout imgContainer = new FrameLayout(context);
        imgContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        int imgPad = (int) (12 * density);
        imgContainer.setPadding(imgPad, imgPad, imgPad, (int) (24 * density));

        ImageView ivPhoto = new ImageView(context);
        ivPhoto.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivPhoto.setAdjustViewBounds(true);

        try {
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bmp != null) {
                ivPhoto.setImageBitmap(bmp);
            }
        } catch (Exception ignored) {}

        imgContainer.addView(ivPhoto);
        root.addView(imgContainer);

        dialog.setContentView(root);
        dialog.show();
    }
}
