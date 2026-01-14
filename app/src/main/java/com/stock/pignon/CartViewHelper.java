// CartViewHelper.java
package com.stock.pignon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * UI Helper to dynamically build and update cart's list
 */
public class CartViewHelper {

    /**
     * Refreshes the entire cart side-panel or list
     * Clears everything and rebuild on current CartManager data
     */
    public static void updateCartView(LinearLayout cartList, Context context) {
        if (cartList == null) return;

        // Clear and get current items
        cartList.removeAllViews();
        List<CartItem> cartItems = CartManager.getItems();

        // If empty cart, show it to user
        if (cartItems.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(context.getString(R.string.cart_empty));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 50, 0, 0);
            cartList.addView(empty);

            updateTotalDisplay(context, 0, 0);
            return;
        }

        // Build list and calculate total at the same time
        int totalMin = 0;
        int totalMax = 0;

        for (CartItem item : cartItems) {
            totalMin += item.getTotalMin();
            totalMax += item.getTotalMax();

            // Create and add the row view
            cartList.addView(createCartItemView(item, cartList, context));
        }

        // Display global price range at the end
        updateTotalDisplay(context, totalMin, totalMax);
    }

    /**
     * Updates the global price range at the bottom of the screen
     */
    private static void updateTotalDisplay(Context context, int min, int max) {
        TextView totalView = ((Activity) context).findViewById(R.id.totalView);
        if (totalView != null) {
            // Using string resources
            totalView.setText(context.getString(R.string.price_range, min, max, "€"));
        }
    }

    /**
     * Creates a horizontal row for a single cart item
     * Layout: [image] [name / [quantity / price]] [remove button]
     */
    private static View createCartItemView(CartItem item, LinearLayout cartList, Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Layout parameters
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 10, 0, 10);
        row.setLayoutParams(rowParams);

        // Image
        ImageView image = new ImageView(context);
        ImageLoader.loadImage(image, item.getImageFile(),200,200);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(100, 100);
        imgParams.setMargins(0, 0, 20, 0);
        image.setLayoutParams(imgParams);
        row.addView(image);

        // Sublayout for name + [quantity + price]
        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Name
        TextView nameView = new TextView(context);
        nameView.setText(item.getName());
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nameView.setTypeface(null, Typeface.BOLD);
        infoLayout.addView(nameView);

        // Quantity and price range
        TextView detailsView = new TextView(context);
        String details = context.getString(R.string.cart_item, item.getQuantity(), item.getTotalMin(), item.getTotalMax());
        detailsView.setText(details);
        detailsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        infoLayout.addView(detailsView);

        row.addView(infoLayout);

        // Remove button
        row.addView(createRemoveButton(item, cartList, context));

        return row;
    }

    /**
     * Creates the delete button
     */
    private static Button createRemoveButton(CartItem item, LinearLayout cartList, Context context) {
        Button btn = new Button(context);
        btn.setText("✖");
        btn.setTextSize(18);
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);

        // Conversion DP to PX for consistent size on all screens
        float scale = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (48 * scale + 0.5f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
        btn.setLayoutParams(params);

        // Background: grey rounded rectangle (API 17 fallback)
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#E53935")); // Reddish to indicate delete
        shape.setCornerRadius(4 * scale);
        btn.setBackground(shape);

        btn.setOnClickListener(v -> {
            // setting quantity to 0 removes the item
            CartManager.addOrUpdateItem(
                    item.getName(),
                    item.getMinPrice(),
                    item.getMaxPrice(),
                    0, // Setting quantity to 0 triggers removal
                    item.getImageFile()
            );

            // visual refresh of the cart list
            updateCartView(cartList, context);
        });

        return btn;
    }
}