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
import java.util.ArrayList;
import java.util.List;

/**
 * Action on shopping cart: validation, clearing, and persistence
 */
public class CartActionHelper {

    private static final String TAG = "CartActionHelper";

    /**
     * Data structure for JSON and CSV
     */
    private static class StockEntry {
        String date;
        String name;
        int qty;

        StockEntry(String date, String name, int qty) {
            this.date = date;
            this.name = name;
            this.qty = qty;
        }
    }

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
     * Merges current cart items with the existing stock file (json and csv) on the SD Card.
     */
    private static void saveCartToExternalFile(List<CartItem> cartItems) {
        File dir = new File(Environment.getExternalStorageDirectory(), Config.EXTERNAL_DIR_NAME);
        File stockFile = new File(dir, Config.OUPUT_JSON_NAME);
        File csvFile = new File(dir, Config.OUPUT_CSV_NAME);

        String today = DateHelper.getTodayIso();
        Gson gson = new Gson();

        // Load previous list
        List<StockEntry> history = loadHistory(stockFile, gson);

        // Merge current cart items in previous list
        for (CartItem cartItem : cartItems) {
            boolean merged = false;
            for (StockEntry entry : history) {
                // Same date same name ? Add it
                if (entry.date.equals(today) && entry.name.equals(cartItem.getName())) {
                    entry.qty += cartItem.getQuantity();
                    merged = true;
                    break;
                }
            }
            // Not found on the same date ? Create it
            if (!merged) {
                history.add(new StockEntry(today, cartItem.getName(), cartItem.getQuantity()));
            }
        }
        // Save to JSON
        try (FileOutputStream fos = new FileOutputStream(stockFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
            gson.newBuilder().setPrettyPrinting().create().toJson(history, writer);
        } catch (Exception e) {
            Log.e(TAG, "Error writing JSON", e);
        }

        // Save to CSV, french format with ";"
        try (FileOutputStream fos = new FileOutputStream(csvFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
            writer.write('\ufeff');
            writer.write("Date;Article;Quantité\n");
            for (StockEntry entry : history) {
                writer.write(entry.date + ";" + entry.name.replace(";", ",") + ";" + entry.qty + "\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing CSV", e);
        }
    }

    /**
     * Load JSON history
     */
    private static List<StockEntry> loadHistory(File file, Gson gson) {
        if (!file.exists()) return new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            Type type = new TypeToken<List<StockEntry>>(){}.getType();
            List<StockEntry> result = gson.fromJson(reader, type);
            return (result != null) ? result : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error reading json history", e);
            return new ArrayList<>();
        }
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

            if (activity instanceof MainActivity) {
                MainActivity main = (MainActivity) activity;
                // Go to home if not already
                main.showHome();
                // Clear the backstack so the user can't "go back" to a validated cart
                GridLayout grid = main.findViewById(R.id.gridPieces);
                refreshGridQuantities(grid);
            }
        }, 2000);
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