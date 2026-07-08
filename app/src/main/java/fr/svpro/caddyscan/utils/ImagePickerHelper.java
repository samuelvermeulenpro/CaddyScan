package fr.svpro.caddyscan.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralise la logique de prise de photo et de sélection depuis la galerie.
 * Retourne une URI locale (stockée dans les fichiers privés de l'app).
 */
public class ImagePickerHelper {

    private static final String AUTHORITY_SUFFIX = ".fileprovider";
    private static final int    MAX_SIZE         = 1024; // px max côté long
    private static final int    JPEG_QUALITY     = 85;

    /** Crée un fichier temporaire pour la photo et retourne son URI FileProvider. */
    public static Uri createCameraImageUri(Context context) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile("PRODUCT_" + timestamp, ".jpg", storageDir);
        return FileProvider.getUriForFile(context,
                context.getPackageName() + AUTHORITY_SUFFIX, imageFile);
    }

    /** Intent pour ouvrir l'appareil photo avec l'URI de destination. */
    public static Intent cameraIntent(Uri outputUri) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return intent;
    }

    /** Intent pour choisir une image depuis la galerie / gestionnaire de fichiers. */
    public static Intent galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        return intent;
    }

    /**
     * Copie et compresse l'image source (URI) vers un fichier privé de l'app.
     * Retourne l'URI locale du fichier compressé, ou null en cas d'erreur.
     */
    public static Uri copyAndCompressImage(Context context, Uri sourceUri) {
        try {
            // Décode l'image source
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                BitmapFactory.decodeStream(is, null, opts);
            }

            // Calcul du sous-échantillonnage
            int inSampleSize = 1;
            if (opts.outWidth > MAX_SIZE || opts.outHeight > MAX_SIZE) {
                int halfW = opts.outWidth / 2, halfH = opts.outHeight / 2;
                while (halfW / inSampleSize >= MAX_SIZE && halfH / inSampleSize >= MAX_SIZE) {
                    inSampleSize *= 2;
                }
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;
            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                bitmap = BitmapFactory.decodeStream(is, null, opts);
            }
            if (bitmap == null) return null;

            // Sauvegarde dans le cache privé de l'app
            File cacheDir = new File(context.getCacheDir(), "images");
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
            String name = "product_" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(cacheDir, name);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
            }
            bitmap.recycle();

            return FileProvider.getUriForFile(context,
                    context.getPackageName() + AUTHORITY_SUFFIX, outFile);

        } catch (Exception e) {
            return null;
        }
    }
}
