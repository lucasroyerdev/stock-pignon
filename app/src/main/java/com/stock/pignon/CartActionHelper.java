// CartActionHelper.java
package com.stock.pignon;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action on shopping cart: validation, clearing, and persistence
 */
public class CartActionHelper {

    private static final String TAG = "CartActionHelper";

    /**
     * Resets the cart data and refreshes UI
     */
    public static void emptyCart(LinearLayout cartList, Context activity) {
        CartManager.clear();
        CartViewHelper.updateCartView(cartList, activity);

        if (activity instanceof MainActivity) {
            GridLayout grid = ((MainActivity) activity).findViewById(R.id.gridPieces);
            refreshGridQuantities(grid);
        }
    }

    /**
     * Handles checkout process: shows summary, saves to disk on confirmation
     * and redirects the user after a success message
     */
    public static void validateCart(LinearLayout cartList, Context activity) {
        List<CartItem> items = CartManager.getItems();
        if (items.isEmpty()) return;

        // Build a readable HTML summary for the confirmation dialog
        StringBuilder recap = new StringBuilder();

        // Show a line for each item
        for (CartItem item : items) {
            recap.append(activity.getString(R.string.popup_item,
                    item.getQuantity(),
                    item.getName(),
                    item.getTotalMin(),
                    item.getTotalMax()));
        }

        // Show total price range
        recap.append(activity.getString(R.string.popup_total,
                CartManager.getGlobalTotalMin(),
                CartManager.getGlobalTotalMax()));

        // Build dialog with HTML summary and buttons
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.popup_name))
                .setMessage(Html.fromHtml(recap.toString()))
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Save to storage
                    saveCartToExternalFile(items);

                    // Clear current session
                    CartManager.clear();
                    CartViewHelper.updateCartView(cartList, activity);

                    // Show success message and close
                    showSuccessAndRedirect(activity);
                })
                .setNegativeButton("Non", null)
                .show();
    }

    /**
     * Displays a thank you popup and returns to the main menu after 2 seconds.
     */
    private static void showSuccessAndRedirect(Context activity) {
        AlertDialog merciDialog = new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.popup_end))
                // Info popup, not cancelable
                .setCancelable(false)
                .create();
        merciDialog.show();


        new Handler().postDelayed(() -> {
            // Close dialog
            merciDialog.dismiss();
            // Go to home if not already
            if (!(activity instanceof MainActivity)) {
                Intent intent = new Intent(activity, MainActivity.class);
                // Clear the backstack so the user can't "go back" to a validated cart
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            }
        }, 2000);
    }

    /**
     * Merges current cart items with the existing stock file on the SD Card.
     */
    private static void saveCartToExternalFile(List<CartItem> cartItems) {
        File dir = new File(Environment.getExternalStorageDirectory(), Config.EXTERNAL_DIR_NAME);
        File stockFile = new File(dir, Config.STOCK_FILE_NAME);

        Gson gson = new Gson();
        Map<String, Integer> stockMap;

        // Load data into a Map (Key: Item Name, Value: Total Quantity)
        stockMap = loadExistingStock(stockFile, gson);

        // Merge with new items from current cart
        for (CartItem item : cartItems) {
            // Get previous quantity
            Integer qtyObj = stockMap.get(item.getName());
            int currentQty = (qtyObj != null) ? qtyObj : 0;
            // Add
            stockMap.put(item.getName(), currentQty + item.getQuantity());
        }

        // Overwrite the file with updated data
        // Needs WRITE_EXTERNAL_STORAGE permission
        try (FileOutputStream fos = new FileOutputStream(stockFile);
             @SuppressWarnings("CharsetObjectCanBeUsed")
             OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {

            // Writing directly to the stream
            gson.toJson(stockMap, writer);
            Log.i(TAG, "Stock updated successfully at: " + stockFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Failed to write stock file", e);
        }
    }

    /**
     * Reads the current stock file. If the file is missing or corrupted, returns an empty map.
     */
    private static Map<String, Integer> loadExistingStock(File stockFile, Gson gson) {
        if (!stockFile.exists()) return new HashMap<>();

        try (FileInputStream fis = new FileInputStream(stockFile);
             @SuppressWarnings("CharsetObjectCanBeUsed")
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {

            // Type definition for Map required by GSON : <String, Integer>
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            // Read and fill map
            Map<String, Integer> result = gson.fromJson(reader, type);

            return (result != null) ? result : new HashMap<>();

        } catch (Exception e) {
            Log.e(TAG, "Error reading existing stock, starting fresh", e);
            return new HashMap<>();
        }
    }

    /**
     * Updates the UI grid to reflect quantities.
     */
    public static void refreshGridQuantities(GridLayout grid) {
        if (grid == null) return;

        // Iterate through all child views in the grid
        for (int i = 0; i < grid.getChildCount(); i++) {
            View itemView = grid.getChildAt(i);

            // Locate the name and quantity views inside the current item layout
            TextView nameView = itemView.findViewById(R.id.itemName);
            TextView qtyView = itemView.findViewById(R.id.qtyView);


            if (nameView != null && qtyView != null) {
                // Match names
                String itemName = nameView.getText().toString();

                // Fetch current quantity from centralized CartManager
                CartItem cartItem = CartManager.getItemByName(itemName);
                int quantity = (cartItem != null) ? cartItem.getQuantity() : 0;

                // Update the visual counter
                qtyView.setText(String.valueOf(quantity));
            }
        }
    }
}