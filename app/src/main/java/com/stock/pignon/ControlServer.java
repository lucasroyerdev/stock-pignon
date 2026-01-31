// ControlServer.java
package com.stock.pignon;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Iterator;

/**
 * Create a web server to remote management of app assets.
 * Inherit what is needed to build a web server from NanoHTTPD
 */
public class ControlServer extends NanoHTTPD {

    private final Context context;

    // Assets folders
    private final File storageRoot = Environment.getExternalStorageDirectory();
    private final File baseDir = new File(storageRoot, Config.EXTERNAL_DIR_NAME);
    private final File imagesDir = new File(baseDir, Config.IMAGES_SUBDIR_NAME);

    public ControlServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    /**
     * Main method which manage requests
     */
    @Override
    public Response serve(IHTTPSession session) {
        // Get requested address
        String uri = session.getUri();

        if (uri.startsWith("/img_view/")) {
            return serveImage(uri.replace("/img_view/", ""));
        }

        if (session.getMethod() == Method.GET) {
            switch (uri) {
                // Soon deprecated with online editor
                case "/download_input":
                    return downloadFile(Config.INPUT_JSON_NAME);
                // Soon deprecated with online editor
                case "/output_json":
                    return viewFile(Config.OUTPUT_JSON_NAME);
                case "/download_output_json":
                    return downloadFile(Config.OUTPUT_JSON_NAME);
                case "/output_csv":
                    return viewFile(Config.OUTPUT_CSV_NAME);
                case "/download_output_csv":
                    return downloadFile(Config.OUTPUT_CSV_NAME);
                case "/edit":
                    return newFixedLengthResponse(fillEditor());
                case "/":
                    return newFixedLengthResponse(getHtml("index.html"));
            }
        }

        if (session.getMethod() == Method.POST) {
            switch (uri) {
                // Soon deprecated with online editor
                case "/upload_json":
                    return handleJsonUpload(session);
                // Soon deprecated with online editor
                case "/upload_images":
                    return handleImagesUpload(session);
                case "/save_editor":
                    return handleSaveEditor(session);
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "❌ Page non trouvée");
    }

    /**
     * Read from asset and return html file
     */
    private String getHtml(String file) {
        try {
            InputStream is = context.getAssets().open(file);
            // Scanner html file from the beginning with "\\A"
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            String html = s.hasNext() ? s.next() : "";
            is.close();
            return html;
        } catch (IOException e) {
            return "<html><body>❌ Erreur de chargement de la page</body></html>";
        }
    }

    /**
     * Read from asset and return html file
     */
    private String fillEditor() {
        // Get data for assets
        DataLoader.loadData();
        // Create an html string to fill the template
        StringBuilder html = new StringBuilder();
        // Save categories order
        List<String> orderList = new ArrayList<>();
        int index = 0;

        // Browse map : 'id' for categories name, 'items' for items list
        for (Map.Entry<String, List<Item>> section : DataLoader.getAllSections().entrySet()) {
            String technicalId = "cat_" + index;
            String displayName = section.getKey();
            List<Item> items = section.getValue();

            orderList.add(technicalId);

            // For each category, render a HTML card
            html.append(renderCategorySection(technicalId, displayName, items));
            index++;
        }
        String orderField = "<input type='hidden' name='cat_order_list' value='" +
                android.text.TextUtils.join(",", orderList) + "'>";

        return getHtml("editor.html").replace("{{GENERATED_CONTENT}}", html + orderField);
    }

    /**
     * Generate HTML section for each category
     */
    private String renderCategorySection(String techId, String displayName, List<Item> items) {
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='card'>");
        sb.append("<h2>");

        if ("global".equals(displayName)) {
            sb.append("Articles globaux");
            // Hidden input to mark cat for server
            sb.append("<input type='hidden' name='cat|").append(displayName).append("|name' value='global'>");
            techId = "global";
        } else {
            // Copy H2 theme
            sb.append("<input type='text' name='cat|").append(techId).append("|name' ")
                    .append("value=\"").append(displayName).append("\" class='input-h2'>");

            // Delete category button
            sb.append(" <button type='button' class='btn-del' style='vertical-align: middle; margin-left: 10px;' ")
                    .append("onclick=\"if(confirm('Supprimer cette catégorie et tous ses articles?')) this.closest('.card').remove()\">×</button>");
        }

        sb.append("</h2>");

        // Build table
        sb.append("<table id='table-").append(techId).append("'>");
        sb.append("<tr><th>Image</th><th>Nom</th><th>Prix Min</th><th>Prix Max</th></tr>");

        int rowIdx = 0;
        for (Item item : items) {
            sb.append(renderItemRow(techId, item, rowIdx++));
        }

        sb.append("</table>");

        // Button to add item in this category
        sb.append("<button type='button' class='btn-add' onclick=\"addRow('").append(techId).append("')\">+ Ajouter article</button>");
        sb.append("</div>");

        return sb.toString();
    }

    /**
     * Generate HTML row for each item
     */
    private String renderItemRow(String techId, Item item, int idx) {
        String prefix = "item|" + techId + "|" + idx;
        return "<tr>" +
                "<td><img src='/img_view/" + item.getImage() + "' class='img-p' onerror=\"this.src='https://placehold.co/60?text=?'\">" +
                "<br><input type='file' accept='.jpg,.png' style='font-size:10px; width:70px' onchange='uImg(this)'>" +
                "<input type='hidden' name='" + prefix + "|img' value='" + item.getImage() + "'></td>" +
                "<td><input type='text' name='" + prefix + "|name' value=\"" + item.getName() + "\"></td>" +
                "<td><input type='number' name='" + prefix + "|min' value='" + item.getMinPrice() + "'></td>" +
                "<td><input type='number' name='" + prefix + "|max' value='" + item.getMaxPrice() + "'></td>" +
                // Retro JS compatibility
                "<td><button type='button' class='btn-del' onclick='var r=this.parentNode.parentNode; r.parentNode.removeChild(r);'>×</button></td>" +
                "</tr>";
    }

    private Response serveImage(String filename) {
        File imgJpg = new File(imagesDir, filename + ".jpg");
        File imgPng = new File(imagesDir, filename + ".png");
        File file = imgJpg.exists() ? imgJpg : (imgPng.exists() ? imgPng : null);

        if (file == null || !file.exists()) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "No image");

        try {
            Response res = newChunkedResponse(Response.Status.OK, "image/jpeg", new FileInputStream(file));
            // Avoid cache if user change image
            res.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            res.addHeader("Pragma", "no-cache");
            res.addHeader("Expires", "0");
            return res;
        } catch (IOException e) {
            return newFixedLengthResponse("Error");
        }
    }

    /**
     * Receive JSON and update cache and storage catalog
     */
    private Response handleSaveEditor(IHTTPSession session) {
        try {
            // NanoHTTPD read request
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            String jsonStr = files.get("postData");
            if (jsonStr == null || jsonStr.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Données vides");
            }

            JSONObject fullJson = new JSONObject(jsonStr);
            // Get items
            JSONObject allFields = fullJson.getJSONObject("items");
            // Get categories order
            String[] orderedIds = fullJson.getString("cat_order_list").split(",");

            // LinkedHashMap to keep order
            Map<String, List<Item>> finalData = new LinkedHashMap<>();

            // For each category
            for (String techId : orderedIds) {
                // Create awaited label
                String catNameKey = "cat|" + techId + "|name";
                // Does it exist
                if (!allFields.has(catNameKey)) continue;

                // Get data
                String realCatName = allFields.getString(catNameKey);
                List<Item> itemsInCategory = new ArrayList<>();

                // Browse and create items
                int i = 0;
                while (true) {
                    String itemBase = "item|" + techId + "|" + i + "|";
                    // On vérifie si l'item suivant existe (via son champ name)
                    if (!allFields.has(itemBase + "name")) break;

                    itemsInCategory.add(new Item(
                        // Mandatory
                        allFields.getString(itemBase + "name"),
                        // Not mandatory
                        allFields.optString(itemBase + "img", ""),
                        parseSafely(allFields.optString(itemBase + "min", "0")),
                        parseSafely(allFields.optString(itemBase + "max", "0"))
                    ));
                    i++;
                }
                finalData.put(realCatName, itemsInCategory);
            }

            DataLoader.saveData(finalData);
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK");

        } catch (Exception e) {
            Log.e("ControlServer", "Erreur save", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur");
        }
    }

    private int parseSafely(String val) {
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // Soon deprecated with online editor
    /**
     * Manage JSON sent by remote.
     */
    private Response handleJsonUpload(IHTTPSession session) {
        Map<String, String> tmpFiles = new HashMap<>();
        try {
            // NanoHTTPD stores loaded file in temp file
            session.parseBody(tmpFiles);
            // tmpFiles map stores file name and path to tmp folder
            String tmpPath = tmpFiles.get("json_file");
            if (tmpPath == null) return newFixedLengthResponse("❌ Aucun fichier reçu.");

            File src = new File(tmpPath);
            File dest = new File(baseDir, Config.INPUT_JSON_NAME);

            copyFile(src, dest);
            return newFixedLengthResponse("✅ Catalogue mis à jour ! <a href='/'>Retour</a>");
        } catch (Exception e) {
            return newFixedLengthResponse("❌ Erreur JSON : " + e.getMessage());
        }
    }

    // Soon deprecated with online editor
    /**
     * Manage multiple images sent by remote.
     */
    private Response handleImagesUpload(IHTTPSession session) {
        Map<String, String> tmpFiles = new HashMap<>();
        try {
            session.parseBody(tmpFiles);

            if (!imagesDir.exists() && !imagesDir.mkdirs()) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "❌ Erreur : Impossible de créer le dossier des images sur la tablette.");
            }

            int count = 0;

            // Browse temp files created by NanoHTTPD
            for (Map.Entry<String, String> entry : tmpFiles.entrySet()) {
                // Manage multiple files uppload
                if (entry.getKey().startsWith("images")) {
                    String tmpPath = entry.getValue();

                    // Get file name
                    List<String> params = session.getParameters().get(entry.getKey());

                    if (params != null && !params.isEmpty()) {
                        // Clean file name to remove path, only keep image.jpg or image.png
                        String originalName = new File(params.get(0)).getName();

                        File src = new File(tmpPath);
                        File dest = new File(imagesDir, originalName);

                        copyFile(src, dest);
                        count++;
                    }
                }
            }

            return newFixedLengthResponse("✅ " + count + " images enregistrées ! <a href='/'>Retour</a>");

        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "❌ Erreur serveur : " + e.getMessage());
        }
    }

    // Soon deprecated with online editor
    /**
     * Utility method to copy file byte per byte.
     * Not always possible to simply move a file on Android.
     */
    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * Prepare file and offer as download
     */
    private Response downloadFile(String filename) {
        File file = new File(baseDir, filename);
        if (!file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "❌ Fichier non trouvé.");
        }
        try {
            InputStream is = new FileInputStream(file);

            // Force download
            Response res = newChunkedResponse(Response.Status.OK, "application/octet-stream", is);

            res.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            return res;
        } catch (IOException e) {
            return newFixedLengthResponse("❌ Erreur de lecture.");
        }
    }

    /**
     * Prepare file and print it in browser
     */
    private Response viewFile(String filename) {
        File file = new File(baseDir, filename);
        if (!file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "❌ Fichier non trouvé.");
        }
        try {
            InputStream is = new FileInputStream(file);

            // Force Mime type to print file inside browser
            String mimeType = "text/plain";

            Response res = newChunkedResponse(Response.Status.OK, mimeType + "; charset=utf-8", is);

            res.addHeader("Content-Disposition", "inline");
            return res;
        } catch (IOException e) {
            return newFixedLengthResponse("❌ Erreur de lecture.");
        }
    }
}