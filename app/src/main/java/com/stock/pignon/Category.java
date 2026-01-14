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

    public String getName() { return name; }
    public String getBgColor() { return bgColor; }
    public String getTextColor() { return textColor; }

    // Avoid crash if json isn't readable
    public List<Item> getItems() {
        return items != null ? items : new ArrayList<>();
    }
}