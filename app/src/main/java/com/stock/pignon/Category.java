// Category.java
package com.stock.pignon;

import java.util.ArrayList;
import java.util.List;

public class Category {
    @SuppressWarnings("unused")
    private String name;
    @SuppressWarnings("unused")
    private String bgColor;
    @SuppressWarnings("unused")
    private String textColor;
    @SuppressWarnings("unused")
    private List<Item> items;

    // Empty constructor for GSON
    public Category() {}

    // Full constructor for online editor
    public Category(String name, List<Item> items) {
        this.name = name;
        this.items = items;
        // Default colors
        this.bgColor = "#0049AF";
        this.textColor = "#FFFFFF";
    }

    // Getters
    public String getName() { return name; }
    public String getBgColor() { return bgColor; }
    public String getTextColor() { return textColor; }
    public List<Item> getItems() { return items != null ? items : new ArrayList<>(); }
}