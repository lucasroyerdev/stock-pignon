// MainActivity.java
package com.stock.pignon;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Main Activity: Central Dashboard for the application.
 * Manages category navigation and item selection within a single screen.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI components
    private LinearLayout cartList;
    private LinearLayout homeLayout;
    private LinearLayout categoryItemsLayout;
    private LinearLayout categoriesLayout;
    private GridLayout gridPieces;
    private ImageView mainImage;

    // Server component
    private ControlServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide Android bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // View Binding
        cartList = findViewById(R.id.cartList);
        homeLayout = findViewById(R.id.homeLayout);
        categoryItemsLayout = findViewById(R.id.categoryItemsLayout);
        categoriesLayout = findViewById(R.id.categoriesLayout);
        gridPieces = findViewById(R.id.gridPieces);
        mainImage = findViewById(R.id.mainImage);

        // Setup Back Button
        Button backBtn = findViewById(R.id.backToHomeBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showHome());
        }

        // Copy assets to sd card if not founded
        copyAssetsIfEmpty();
        // Get data from sd card
        DataLoader.loadData();

        // Initial UI Setup
        setupCategoriesMenu();
        loadMainImage();

        // Initial cart visual sync
        CartViewHelper.updateCartView(cartList, this);

        // Action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(" 🚲 Atelier du Pignon - Gestion du stock à prix libre");
        }

        // Launch server
        server = new ControlServer(8080);
        try {
            server.start();
            String url = "http://" + getDeviceIP() + ":8080";

            // On met l'URL directement dans le sous-titre de l'ActionBar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("🟢 Serveur actif : " + url);
            }
            Log.i(TAG, "Serveur démarré sur : " + url);
        } catch (IOException e) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("🔴 Erreur serveur : Port 8080 occupé");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure cart is up to date if returning to the activity
        CartViewHelper.updateCartView(cartList, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            Log.i(TAG, "Serveur arrêté.");
        }
    }

    /**
     * Builds the left sidebar with category buttons using DataLoader's cache.
     */
    private void setupCategoriesMenu() {
        List<Category> categories = DataLoader.getCategories();
        if (categories == null || categories.isEmpty()) {
            Log.w(TAG, "No categories found to display.");
            return;
        }

        categoriesLayout.removeAllViews();
        for (Category category : categories) {
            Button btn = createCategoryButton(category);
            // On click, we show the items for this specific category
            btn.setOnClickListener(v -> showCategory(category.getName()));
            categoriesLayout.addView(btn);
        }
    }

    /**
     * Styles category buttons based on the new Category getters.
     */
    private Button createCategoryButton(Category category) {
        Button btn = new Button(this);
        btn.setText(category.getName());
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

        float density = getResources().getDisplayMetrics().density;
        float radiusPx = 3 * density;

        // Apply background color with transparency (70% alpha)
        if (category.getBgColor() != null) {
            try {
                int color = Color.parseColor(category.getBgColor());
                int alpha = (int) (0.7 * 255);
                color = (color & 0x00FFFFFF) | (alpha << 24);

                GradientDrawable shape = new GradientDrawable();
                shape.setColor(color);
                shape.setCornerRadius(radiusPx);
                btn.setBackground(shape);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing bgColor for: " + category.getName());
            }
        }

        // Apply text color
        if (category.getTextColor() != null) {
            try {
                btn.setTextColor(Color.parseColor(category.getTextColor()));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing textColor for: " + category.getName());
            }
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 5, 0, 5);
        btn.setLayoutParams(params);

        return btn;
    }

    /**
     * Switches center view to display the grid of items.
     * Uses DataLoader.getItemsForCategory() to get the merged list.
     */
    private void showCategory(String categoryName) {
        homeLayout.setVisibility(View.GONE);
        categoryItemsLayout.setVisibility(View.VISIBLE);

        gridPieces.removeAllViews();

        // Calculate item width for a 4-column grid
        int gridWidth = getResources().getDisplayMetrics().widthPixels - dpToPx(370);
        int itemWidth = gridWidth / 4;

        // Fetch the PRE-MERGED list (globals + specifics) from DataLoader
        List<Item> itemsToDisplay = DataLoader.getItemsForCategory(categoryName);

        for (Item item : itemsToDisplay) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_piece, gridPieces, false);

            // Apply width to the inflated view
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) itemView.getLayoutParams();
            if (params == null) {
                params = new GridLayout.LayoutParams();
            }
            params.width = itemWidth;
            itemView.setLayoutParams(params);

            bindItemToView(itemView, item);
            gridPieces.addView(itemView);
        }
    }

    /**
     * Connects Item data to the inflated layout piece.
     */
    private void bindItemToView(View itemView, Item item) {
        TextView nom = itemView.findViewById(R.id.itemName);
        TextView prix = itemView.findViewById(R.id.itemPrice);
        ImageView img = itemView.findViewById(R.id.itemImage);
        TextView qtyView = itemView.findViewById(R.id.qtyView);

        nom.setText(item.getName());
        prix.setText(getString(R.string.price_range, item.getMinPrice(), item.getMaxPrice(), "€"));

        // Use your existing ImageLoader helper
        ImageLoader.loadImage(img, item.getImage(),200,200);

        // Sync visual quantity with CartManager
        CartItem existing = CartManager.getItemByName(item.getName());
        int currentQty = (existing != null) ? existing.getQuantity() : 0;
        qtyView.setText(String.valueOf(currentQty));

        // Listeners for + and - buttons
        itemView.findViewById(R.id.plusBtn).setOnClickListener(v -> updateItemQty(item, qtyView, 1));
        itemView.findViewById(R.id.minusBtn).setOnClickListener(v -> updateItemQty(item, qtyView, -1));
    }

    /**
     * Updates the local counter and the global cart state simultaneously.
     */
    private void updateItemQty(Item item, TextView qtyView, int delta) {
        int current = Integer.parseInt(qtyView.getText().toString());
        int next = Math.max(0, current + delta);

        qtyView.setText(String.valueOf(next));

        // Central data update
        CartManager.addOrUpdateItem(item.getName(), item.getMinPrice(), item.getMaxPrice(), next, item.getImage());

        // Refresh sidebar cart list
        CartViewHelper.updateCartView(cartList, this);
    }

    private void showHome() {
        categoryItemsLayout.setVisibility(View.GONE);
        homeLayout.setVisibility(View.VISIBLE);
    }

    private void loadMainImage() {
        // Fallback for home screen image
        ImageLoader.loadImage(mainImage, "_velo", 800,800);
    }

    // --- Button Actions (linked via android:onClick in XML) ---

    public void emptyCart(View view) {
        CartActionHelper.emptyCart(cartList, this);
    }

    public void validateCart(View view) {
        CartActionHelper.validateCart(cartList, this);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void copyAssetsIfEmpty() {
        File folder = new File(Environment.getExternalStorageDirectory(), Config.EXTERNAL_DIR_NAME);

        // First safety : is folder already in sdcard ?
        if (!folder.exists()) {
            // Second safety : are we able to create folder ?
            if (folder.mkdirs()) {
                // Copy JSON file
                copyFileFromAssets(Config.INPUT_JSON_NAME, new File(folder, Config.INPUT_JSON_NAME));

                // Copy images subfolder
                File imgFolder = new File(folder, Config.IMAGES_SUBDIR_NAME);
                if (imgFolder.mkdirs()) {
                    copyFolderFromAssets(Config.IMAGES_SUBDIR_NAME, imgFolder);
                }
            }
        }
    }

    private void copyFolderFromAssets(String assetDirName, File destDir) {
        try {
            String[] files = getAssets().list(assetDirName);
            if (files != null) {
                // For every image
                for (String fileName : files) {
                    // Create source path
                    String assetPath = assetDirName + "/" + fileName;
                    // Create dest path
                    File destFile = new File(destDir, fileName);
                    // Copy
                    copyFileFromAssets(assetPath, destFile);
                }
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Erreur listing assets: " + assetDirName, e);
        }
    }

    private void copyFileFromAssets(String assetName, File destFile) {
        // Optimized read
        // Try-with-resources ensures streams are automatically closed, avoid memory leaks
        try (InputStream in = getAssets().open(assetName);
             OutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            Log.d("MainActivity", "Succès : " + assetName + " copié.");

        } catch (IOException e) {
            Log.e("MainActivity", "Erreur copie asset: " + assetName, e);
        }
    }

    /**
     * Get device IP to print it to users for easy remote control
     */
    private String getDeviceIP() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur IP", e);
        }
        return "127.0.0.1";
    }
}