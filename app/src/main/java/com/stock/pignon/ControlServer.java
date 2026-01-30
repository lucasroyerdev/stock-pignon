// ControlServer.java
package com.stock.pignon;

import android.content.Context;
import android.os.Environment;

import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Scanner;

/**
 * Create a web server to remote management of app assets.
 */
// Inherit what is needed to build a web server
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
     * Main method which receive remote request
     */
    @Override
    public Response serve(IHTTPSession session) {
        // Get requested address
        String uri = session.getUri();

        if (session.getMethod() == Method.GET) {
            switch (uri) {
                case "/download_input":
                    return downloadFile(Config.INPUT_JSON_NAME);
                case "/output_json":
                    return viewFile(Config.OUTPUT_JSON_NAME);
                case "/download_output_json":
                    return downloadFile(Config.OUTPUT_JSON_NAME);
                case "/output_csv":
                    return viewFile(Config.OUTPUT_CSV_NAME);
                case "/download_output_csv":
                    return downloadFile(Config.OUTPUT_CSV_NAME);
                case "/":
                    return newFixedLengthResponse(getHome());
            }
        }

        if (session.getMethod() == Method.POST) {
            switch (uri) {
                case "/upload_json":
                    return handleJsonUpload(session);
                case "/upload_images":
                    return handleImagesUpload(session);
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "❌ Page non trouvée");
    }

    /**
     * HTML UI for users
     */
    private String getHome() {
        try {
            InputStream is = context.getAssets().open("index.html");
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            String html = s.hasNext() ? s.next() : "";
            is.close();
            return html;
        } catch (IOException e) {
            return "<html><body>❌ Erreur de chargement du template</body></html>";
        }
    }

    /**
     * Manage JSON sent by remote.
     */
    private Response handleJsonUpload(IHTTPSession session) {
        Map<String, String> tmpFiles = new HashMap<>();
        try {
            session.parseBody(tmpFiles);
            // NanoHTTPD stocke le fichier avec le nom du champ HTML (json_file)
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

            // On parcourt les fichiers temporaires créés par NanoHTTPD
            for (Map.Entry<String, String> entry : tmpFiles.entrySet()) {
                // NanoHTTPD indexe les envois multiples (images, images1, images2...)
                if (entry.getKey().startsWith("images")) {
                    String tmpPath = entry.getValue();

                    // Récupération sécurisée des paramètres pour obtenir le nom original
                    List<String> params = session.getParameters().get(entry.getKey());

                    if (params != null && !params.isEmpty()) {
                        // Nettoyage du nom de fichier (pour ne garder que "image.jpg" sans le chemin PC)
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
}