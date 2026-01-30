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

    // Full constructor for online editor
    public Item(String name, String image, int minPrice, int maxPrice) {
        this.name = name;
        this.image = image;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    // Getters
    public String getName() { return name; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public String getImage() { return image; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setMinPrice(int minPrice) { this.minPrice = minPrice; }
    public void setMaxPrice(int maxPrice) { this.maxPrice = maxPrice; }
    public void setImage(String image) { this.image = image; }
}

