// Item.java
package com.stock.pignon;

public class Item {
    @SuppressWarnings("unused")
    private String name;
    @SuppressWarnings("unused")
    private int minPrice;
    @SuppressWarnings("unused")
    private int maxPrice;
    @SuppressWarnings("unused")
    private String image;

    // Empty constructor for GSON
    public Item() {}

    public String getName() { return name; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public String getImage() { return image; }
}

