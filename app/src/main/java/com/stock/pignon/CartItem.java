// CartItem.java
package com.stock.pignon;

public class CartItem {
    private final String name;
    private final int minPrice;
    private final int maxPrice;
    private int quantity;
    private final String imageFile; // Image name without extension

    public CartItem(String name, int minPrice, int maxPrice, int quantity, String imageFile) {
        this.name = name;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.quantity = quantity;
        this.imageFile = imageFile;
    }

    public String getName() { return name; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getTotalMin() { return minPrice * quantity; }
    public int getTotalMax() { return maxPrice * quantity; }
    public String getImageFile() { return imageFile; }
}
