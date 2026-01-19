// ControlServer.java
package com.stock.pignon;

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

/**
 * Create a web server to remote management of app assets.
 */
// Inherit what is needed to build a web server
public class ControlServer extends NanoHTTPD {

    // Assets folders
    private final File storageRoot = Environment.getExternalStorageDirectory();
    private final File baseDir = new File(storageRoot, Config.EXTERNAL_DIR_NAME);
    private final File imagesDir = new File(baseDir, Config.IMAGES_SUBDIR_NAME);

    public ControlServer(int port) {
        super(port);
    }

    /**
     * Main method which receive remote request
     */
    @Override
    public Response serve(IHTTPSession session) {
        // Get requested address
        String uri = session.getUri();

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
            case "/index.html":
                return newFixedLengthResponse(getHtmlResponse());
        }

        // File upload management
        if (session.getMethod() == Method.POST) {
            if (uri.equals("/upload_json")) {
                return handleJsonUpload(session);
            } else if (uri.equals("/upload_images")) {
                return handleImagesUpload(session);
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "❌ Page non trouvée");
    }

    /**
     * HTML UI for users
     */
    private String getHtmlResponse() {
        return "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                "<style>" +
                "body { font-family: sans-serif; line-height: 1.6; padding: 20px; color: #333; max-width: 800px; margin: auto; }" +
                "h1 { color: #0049AF; border-bottom: 2px solid #0049AF; }" +
                "h2 { color: #555; margin-top: 30px; }" +
                ".card { background: #f4f4f4; padding: 15px; border-radius: 8px; margin-bottom: 20px; border: 1px solid #ddd; }" +
                ".btn { display: inline-block; width: 280px; height: 45px; line-height: 45px; text-align: center; background: #0049AF; color: white; text-decoration: none; border-radius: 4px; border: none; cursor: pointer; font-weight: bold; box-sizing: border-box; padding: 0; -webkit-appearance: none; font-family: inherit; font-size: 14px; font-style: normal; letter-spacing: normal; margin: 10px 5px; vertical-align: middle;}" +
                ".btn-download { background: #2E7D32; }" +
                "input[type='file'] { margin: 10px 0; }" +
                ".status { font-weight: bold; color: green; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<h1>Application : stock-pignon</h1>" +

                "<h2>Gestion des stocks</h2>" +
                "<div class='card'>" +
                "<h3>Récupérer les sacoches des adhérent·es</h3>" +
                "<p>Télécharger le décompte des pièces sorties de l'atelier.</p>" +
                "<a href='/output_json' class='btn'>Voir stock.json</a>" +
                "<a href='/output_csv' class='btn'>Voir stock.csv</a>" +
                "<a href='/download_output_json' class='btn'>Télécharger stock.json</a>" +
                "<a href='/download_output_csv' class='btn'>Télécharger stock.csv</a>" +
                "</div>" +

                "<h2>Modifier le catalogue</h2>" +
                "<div class='card'>" +
                "<h3>Gestion du fichier JSON</h3>" +
                "<p>Catalogue actuel : <a href='/download_input' class='btn'>Télécharger pieces.json</a></p>" +
                "<hr>" +
                "<form action='/upload_json' method='post' enctype='multipart/form-data'>" +
                "<p><strong>Envoyer un nouveau catalogue :</strong></p>" +
                "<input type='file' name='json_file' accept='.json'><br>" +
                "<input type='submit' value='Remplacer pieces.json' class='btn'>" +
                "</form>" +
                "</div>" +

                "<div class='card'>" +
                "<h3>Gestion des images</h3>" +
                "<form action='/upload_images' method='post' enctype='multipart/form-data'>" +
                "<p><strong>Ajouter/Modifier des images (PNG/JPG) :</strong></p>" +
                "<input type='file' name='images' accept='image/*' multiple><br>" +
                "<input type='submit' value='Envoyer les images' class='btn'>" +
                "</form>" +
                "</div>" +

                "</body>" +
                "</html>";
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