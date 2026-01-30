// CartManager.java
package com.stock.pignon;

import java.util.ArrayList;
import java.util.List;

public class CartManager {
    // Unified and global list for whole application
    private static final List<CartItem> items = new ArrayList<>();

    /**
     * Private constructor: utility class, it should not be instantiated
     */
    private CartManager() {}

    /**
     * Returns the direct reference to the list to save memory on older devices
     */
    public static List<CartItem> getItems() {
        return items;
    }

    public static CartItem getItemByName(String name) {
        if (name == null) return null;
        for (CartItem item : items) {
            // if item.getName() is null, avoid crash
            if (item != null && name.equals(item.getName())) return item;
        }
        return null;
    }

    /**
     * Logic for adding, updating or removing items based on quantity, prevents duplicate entries
     */
    public static void addOrUpdateItem(String name, int minPrice, int maxPrice, int quantity, String imageFile) {
        CartItem current = getItemByName(name);

        if (current != null) {
            if (quantity <= 0) {
                items.remove(current);
            } else {
                current.setQuantity(quantity);
            }
        } else if (quantity > 0) {
            items.add(new CartItem(name, minPrice, maxPrice, quantity, imageFile));
        }
    }

    /**
     * Range-based pricing
     */
    public static int getGlobalTotalMin() {
        int total = 0;
        for (CartItem item : items) total += item.getTotalMin();
        return total;
    }

    /**
     * Range-based pricing
     */
    public static int getGlobalTotalMax() {
        int total = 0;
        for (CartItem item : items) total += item.getTotalMax();
        return total;
    }

    /**
     * Remove item from cart
     */
    public static void clear() {
        items.clear();
    }
}
