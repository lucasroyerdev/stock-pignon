// ImageLoader.java
package com.stock.pignon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.ImageView;
import java.io.File;

public class ImageLoader {

    /**
     * Load image from /sdcard/Pignon/, priority JPG then PNG
     * If image not found, default fallback
     */
    public static void loadImage(ImageView imageView, String fileName, int reqWidth, int reqHeight) {
        File storageRoot = Environment.getExternalStorageDirectory();
        File baseDir = new File(storageRoot, Config.EXTERNAL_DIR_NAME);
        File dir = new File(baseDir, Config.IMAGES_SUBDIR_NAME);

        File imgFileJpg = new File(dir, fileName + ".jpg");
        File imgFilePng = new File(dir, fileName + ".png");
        File imgFile = imgFileJpg.exists() ? imgFileJpg : (imgFilePng.exists() ? imgFilePng : null);

        if (imgFile != null && imgFile.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();

            // Check image size without loading
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

            // Compute size reduction
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Load reduce size
            options.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

            if (bmp != null) {
                imageView.setImageBitmap(bmp);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            // Fallback Android
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            final int halfHeight = options.outHeight / 2;
            final int halfWidth = options.outWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
