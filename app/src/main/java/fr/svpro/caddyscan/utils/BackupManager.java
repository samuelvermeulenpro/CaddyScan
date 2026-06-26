package fr.svpro.caddyscan.utils;

import android.content.Context;
import android.net.Uri;

import fr.svpro.caddyscan.models.Product;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Gère l'export et l'import de la liste de produits au format JSON.
 * Utilise le Storage Access Framework (SAF) — aucune permission requise.
 */
public class BackupManager {

    private static final String BACKUP_VERSION = "1";
    private static final String KEY_VERSION    = "version";
    private static final String KEY_EXPORTED_AT= "exported_at";
    private static final String KEY_COUNT      = "count";
    private static final String KEY_PRODUCTS   = "products";

    public interface ExportCallback {
        void onSuccess(int count);
        void onError(String message);
    }

    public interface ImportCallback {
        void onSuccess(int imported, int skipped);
        void onError(String message);
    }

    // ─── Export ──────────────────────────────────────────────────────────────

    public static void exportToUri(Context context, Uri uri,
                                   List<Product> products, ExportCallback callback) {
        new Thread(() -> {
            try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                 OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {

                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                JsonObject root = new JsonObject();
                root.addProperty(KEY_VERSION, BACKUP_VERSION);
                root.addProperty(KEY_EXPORTED_AT,
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                .format(new Date()));
                root.addProperty(KEY_COUNT, products.size());

                JsonArray array = new JsonArray();
                for (Product p : products) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("barcode",     p.getBarcode());
                    obj.addProperty("name",        p.getName());
                    obj.addProperty("brand",       p.getBrand());
                    obj.addProperty("quantity",    p.getQuantity());
                    obj.addProperty("imageUrl",    p.getImageUrl());
                    obj.addProperty("categories",  p.getCategories());
                    obj.addProperty("nutriscore",  p.getNutriscore());
                    obj.addProperty("ingredients", p.getIngredients());
                    obj.addProperty("scannedAt",   p.getScannedAt());
                    array.add(obj);
                }
                root.add(KEY_PRODUCTS, array);

                writer.write(gson.toJson(root));
                writer.flush();

                callback.onSuccess(products.size());

            } catch (IOException e) {
                callback.onError("Erreur d'écriture : " + e.getMessage());
            }
        }).start();
    }

    // ─── Import ──────────────────────────────────────────────────────────────

    public static void importFromUri(Context context, Uri uri,
                                     List<String> existingBarcodes, ImportCallback callback) {
        new Thread(() -> {
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is, StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();

                if (!root.has(KEY_PRODUCTS)) {
                    callback.onError("Fichier de sauvegarde invalide");
                    return;
                }

                JsonArray array = root.getAsJsonArray(KEY_PRODUCTS);
                List<Product> toInsert = new ArrayList<>();
                int skipped = 0;

                for (JsonElement el : array) {
                    JsonObject obj = el.getAsJsonObject();
                    String barcode = getString(obj, "barcode");
                    if (barcode == null || barcode.isEmpty()) continue;

                    if (existingBarcodes.contains(barcode)) {
                        skipped++;
                        continue;
                    }

                    Product p = new Product(barcode);
                    p.setName(getString(obj, "name"));
                    p.setBrand(getString(obj, "brand"));
                    p.setQuantity(getString(obj, "quantity"));
                    p.setImageUrl(getString(obj, "imageUrl"));
                    p.setCategories(getString(obj, "categories"));
                    p.setNutriscore(getString(obj, "nutriscore"));
                    p.setIngredients(getString(obj, "ingredients"));
                    if (obj.has("scannedAt") && !obj.get("scannedAt").isJsonNull()) {
                        p.setScannedAt(obj.get("scannedAt").getAsLong());
                    }
                    toInsert.add(p);
                }

                callback.onSuccess(toInsert.size(), skipped);
                // Note: actual DB insert is done in MainActivity to stay off main thread

            } catch (Exception e) {
                callback.onError("Fichier invalide ou corrompu : " + e.getMessage());
            }
        }).start();
    }

    /**
     * Variante qui retourne directement la liste des produits à insérer.
     */
    public static void parseImport(Context context, Uri uri, ParseCallback callback) {
        new Thread(() -> {
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is, StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();
                if (!root.has(KEY_PRODUCTS)) {
                    callback.onError("Fichier de sauvegarde invalide");
                    return;
                }

                JsonArray array = root.getAsJsonArray(KEY_PRODUCTS);
                List<Product> products = new ArrayList<>();

                for (JsonElement el : array) {
                    JsonObject obj = el.getAsJsonObject();
                    String barcode = getString(obj, "barcode");
                    if (barcode == null || barcode.isEmpty()) continue;

                    Product p = new Product(barcode);
                    p.setName(getString(obj, "name"));
                    p.setBrand(getString(obj, "brand"));
                    p.setQuantity(getString(obj, "quantity"));
                    p.setImageUrl(getString(obj, "imageUrl"));
                    p.setCategories(getString(obj, "categories"));
                    p.setNutriscore(getString(obj, "nutriscore"));
                    p.setIngredients(getString(obj, "ingredients"));
                    if (obj.has("scannedAt") && !obj.get("scannedAt").isJsonNull()) {
                        p.setScannedAt(obj.get("scannedAt").getAsLong());
                    }
                    products.add(p);
                }

                callback.onSuccess(products);

            } catch (Exception e) {
                callback.onError("Fichier invalide ou corrompu : " + e.getMessage());
            }
        }).start();
    }

    public interface ParseCallback {
        void onSuccess(List<Product> products);
        void onError(String message);
    }

    private static String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    /** Génère un nom de fichier horodaté pour l'export */
    public static String generateFileName() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "scanner_backup_" + ts + ".json";
    }
}
