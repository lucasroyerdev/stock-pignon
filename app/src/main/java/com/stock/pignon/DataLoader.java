// DataLoader.java
package com.stock.pignon;

import android.os.Environment;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

public class DataLoader {
    private static final String TAG = "DataLoader"; // For readable logs
    private static final String EXTERNAL_DIR = Config.EXTERNAL_DIR_NAME;
    private static final String PIECES_FILE = Config.INPUT_JSON_NAME;

    // Raw data
    private static List<Category> cachedCategories = new ArrayList<>();
    private static List<Item> cachedGlobals = new ArrayList<>();


    /**
     * Get data from input JSON PIECES_FILE
     */
    public static void loadData() {
        File dir = new File(Environment.getExternalStorageDirectory(), EXTERNAL_DIR);
        File jsonFile = new File(dir, PIECES_FILE);

        // Check if file exist
        if (!jsonFile.exists()) {
            Log.e(TAG, "File doesn't exist : " + jsonFile.getAbsolutePath());
            return;
        }

        // Optimized read
        // Try-with-resources ensures streams are automatically closed, avoid memory leaks
        try (FileInputStream fis = new FileInputStream(jsonFile);
             @SuppressWarnings("CharsetObjectCanBeUsed")
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {

            // From json to java object
            Gson gson = new Gson();
            // Internal class needed for Gson
            CategoriesWrapper wrapper = gson.fromJson(reader, CategoriesWrapper.class);

            // Extract lists from wrapper with null-safety
            if (wrapper != null) {
                cachedGlobals = wrapper.globalItems != null ? wrapper.globalItems : new ArrayList<>();
                cachedCategories = wrapper.categories != null ? wrapper.categories : new ArrayList<>();

            }
        } catch (java.io.FileNotFoundException e) {
            Log.e(TAG, "Can't find file", e);
        } catch (java.io.IOException e) {
            Log.e(TAG, "Read error (I/O)", e);
        } catch (Exception e) {
            Log.e(TAG, "Parsing JSON error", e);
        }
    }

    /**
     * Create a merge list with global items and items from a specified category for main activity
     * Compromise between CPU usage and memory usage : keep cache raw data and combinate global and specific items at call
     */
    public static List<Item> getItemsForCategory(String categoryName) {

        // Add global items
        List<Item> combinedList = new ArrayList<>(cachedGlobals);

        // Add specific items
        for (Category cat : cachedCategories) {
            if (cat.getName().equals(categoryName)) {
                combinedList.addAll(cat.getItems());
                break;
            }
        }
        return combinedList;
    }

    /**
     * To fill online editor, create a map with all items sorted by category
     */
    public static Map<String, List<Item>> getAllSections() {
        // LinkedHashMap remember item order instead of HashMap
        Map<String, List<Item>> sections = new LinkedHashMap<>();
        sections.put("global", cachedGlobals);
        for (Category cat : cachedCategories) {
            sections.put(cat.getName(), cat.getItems());
        }
        return sections;
    }

    /**
     * Create a web server to remote management of app assets.
     */
    public static List<Category> getCategories() {
        return cachedCategories;
    }

    /**
     * Internal class for GSON
     */
    private static class CategoriesWrapper {
        List<Item> globalItems;
        List<Category> categories;
    }

    /**
     * Write JSON from online editor data
     */
    public static void saveData(Map<String, List<Item>> sections) throws Exception {
        File dir = new File(Environment.getExternalStorageDirectory(), EXTERNAL_DIR);
        File jsonFile = new File(dir, PIECES_FILE);

        // To respect original format, we use the same format as GSON
        List<Item> globalList = sections.get("global");
        if (globalList == null) {
            globalList = new ArrayList<>();
        }

        CategoriesWrapper wrapper = new CategoriesWrapper();
        wrapper.globalItems = globalList;
        wrapper.categories = new ArrayList<>();

        // Fill each category
        for (Map.Entry<String, List<Item>> entry : sections.entrySet()) {
            if (!"global".equals(entry.getKey())) {
                wrapper.categories.add(new Category(entry.getKey(), entry.getValue()));
            }
        }

        // Convert to pretty JSON, human readable
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String jsonString = gson.toJson(wrapper);

        // Write to disk
        try (FileOutputStream fos = new FileOutputStream(jsonFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
            writer.write(jsonString);
            writer.flush();
        }

        // Update app cache
        cachedGlobals = wrapper.globalItems;
        cachedCategories = wrapper.categories;
    }
}